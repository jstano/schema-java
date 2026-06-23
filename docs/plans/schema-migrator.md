# Schema Diff and Migration Generation

## Context

schema-java already generates DDL from a schema definition. This plan adds a state-based migration workflow: compare two schema XML files, produce an editable changeset XML, then generate dialect-specific ALTER/DROP/CREATE SQL from that changeset.

The key architectural principle: **schema comparison produces a migration model, not SQL**. SQL is generated from the reviewed migration model as a separate step. This separates ambiguous structural differences (rename vs drop+add) from developer intent, and keeps comparison, intent, and generation cleanly decoupled.

---

## New Modules

### `schema-diff`

Compares two `Schema` objects and produces a `ChangeSet`. Also owns the `ChangeSet` model, XML writer, and XML parser.

**Package:** `com.stano.schema.diff`

**Classes:**

```
ChangeSet                          — list of SchemaChange objects
SchemaChange                       — marker interface
SchemaDiffEngine                   — diff(Schema old, Schema next) → ChangeSet
ChangeSetWriter                    — write(ChangeSet, PrintWriter) → XML
ChangeSetParser                    — parse(URL/InputStream) → ChangeSet
ChangeSetParserException           — RuntimeException wrapper
change/
  AddTableChange(tableName)
  DropTableChange(tableName)
  RenameTableChange(oldName, newName)
  AddColumnChange(tableName, Column)   — carries full Column object
  DropColumnChange(tableName, columnName)
  RenameColumnChange(tableName, oldName, newName)
  ModifyColumnChange(tableName, oldColumn, newColumn)
  AddKeyChange(tableName, Key)
  DropKeyChange(tableName, Key)
  AddConstraintChange(tableName, Constraint)
  DropConstraintChange(tableName, constraintName)
  AddRelationChange(Relation)
  DropRelationChange(Relation)
  AddViewChange(View)
  DropViewChange(viewName)
```

All change types are immutable (final fields, getters only).

**`SchemaDiffEngine` change ordering** (migration-safe):
1. Drop views → drop relations → drop keys → drop constraints → drop columns → drop tables
2. Add tables → add columns → modify columns → add keys → add constraints → add relations → add views

**Rename policy:** The diff engine always emits drop+add. Developers edit the changeset XML to replace a drop+add pair with `<rename-table>` or `<rename-column>`. The parser supports both forms.

**`AddColumnChange` carries a full `Column` object** (not flattened fields) because `Column` is an established value type with 10+ attributes. The writer serializes only the relevant attributes; the parser reconstructs a `Column` via its existing constructor.

**`ChangeSetParser`** uses SAX (matching the `SchemaParser` / `AbstractContentHandler` pattern). The changeset XML is flat — one level of nesting under `<changeset>` — so a single `DefaultHandler` subclass with a `startElement` switch is sufficient.

**Changeset XML format:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<changeset>
    <add-table name="orders"/>
    <drop-table name="old_table"/>
    <rename-table old-name="customer" new-name="customers"/>

    <add-column table="customers" name="email" type="varchar" length="255" required="false"/>
    <add-column table="orders" name="amount" type="decimal" length="10" scale="2" required="true" default="0"/>
    <drop-column table="old_table" name="legacy_col"/>
    <rename-column table="customers" old-name="name" new-name="full_name"/>
    <modify-column table="customers" name="email"
                   old-type="varchar" old-length="100" old-required="false"
                   new-type="varchar" new-length="255" new-required="true"/>

    <add-key table="customers" type="unique" columns="email"/>
    <add-key table="customers" type="index" columns="last_name,first_name"/>
    <drop-key table="customers" type="unique" columns="email"/>

    <add-constraint table="customers" name="chk_email" sql="check(email like '%@%')"/>
    <drop-constraint table="customers" name="chk_old"/>

    <add-relation from-table="orders" from-column="customer_id"
                  to-table="customers" to-column="id" type="cascade"/>
    <drop-relation from-table="orders" from-column="customer_id"
                   to-table="customers" to-column="id"/>

    <add-view name="customer_summary" sql="select id, full_name from customers"/>
    <drop-view name="old_view"/>
</changeset>
```

- `type` on `<add-column>` uses `ColumnType` enum values lowercased (`varchar`, `decimal`, `sequence`, etc.)
- `type` on `<add-key>` takes `primary`, `unique`, or `index`
- `columns` on key elements is comma-separated (matches existing `Key.getColumnsAsString()`)

**`build.gradle.kts` for `schema-diff`:**
```kotlin
import com.stano.buildlogic.configurePublishing

plugins { id("com.stano.java-library-convention") }

configurePublishing(
  name = "schema-diff",
  description = "Computes structural differences between two Schema models and produces a ChangeSet.",
  url = "https://github.com/jstano/schema-java"
)

dependencies {
  api(platform(project(":schema-platform-dependencies")))
  api(project(":schema-model"))
  implementation("org.slf4j:slf4j-api")

  testImplementation(project(":test-platform-dependencies"))
  testImplementation(project(":schema-parser"))   // test-only: load XML fixtures
}
```

---

### `schema-migration-generator`

Generates dialect-specific ALTER/CREATE/DROP SQL from a `ChangeSet`. Mirrors the Factory+Strategy+TemplateMethod pattern of `schema-sql-generator` exactly.

**Package:** `com.stano.schema.genmigration`

**Classes:**
```
GenMigration                                        — public facade
MigrationGeneratorOptions(changeSet, writer, databaseType, statementSeparator)
impl/common/
  MigrationGenerator (abstract)                    — template method; dispatch per change type
  MigrationGeneratorFactory                        — switch on DatabaseType → dialect impl
impl/postgresql/
  PostgreSQLMigrationGenerator extends MigrationGenerator
impl/h2/
  H2MigrationGenerator extends MigrationGenerator
impl/sqlserver/
  SQLServerMigrationGenerator extends MigrationGenerator
```

**`MigrationGenerator.generate()`** walks `changeSet.getChanges()`, dispatches to abstract `generate<ChangeType>(change)` methods using a Java 21 `switch` on instance types. Closes `PrintWriter` in a finally block (same pattern as `SQLGenerator`).

**`GenMigration`** (public facade, mirrors `GenSQL`):
```java
public class GenMigration {
    public MigrationGeneratorFactory migrationGeneratorFactory = new MigrationGeneratorFactory();

    public void generateMigrationSQL(DatabaseType databaseType, ChangeSet changeSet,
                                     PrintWriter writer, String statementSeparator) { ... }

    public void generateMigrationSQL(DatabaseType databaseType, ChangeSet changeSet,
                                     PrintWriter writer) { ... }
}
```

**Dialect-specific SQL highlights:**

| Change | PostgreSQL | H2 | SQL Server |
|---|---|---|---|
| Rename table | `ALTER TABLE x RENAME TO y` | `ALTER TABLE x RENAME TO y` | `sp_rename 'x', 'y'` |
| Rename column | `ALTER TABLE t RENAME COLUMN a TO b` | `ALTER TABLE t ALTER COLUMN a RENAME TO b` | `sp_rename 't.a', 'b', 'COLUMN'` |
| Modify type | `ALTER TABLE t ALTER COLUMN c TYPE newtype` | Drop + re-add (H2 doesn't support ALTER COLUMN TYPE) | `ALTER TABLE t ALTER COLUMN c newtype` |
| Drop table | `DROP TABLE IF EXISTS t` | `DROP TABLE IF EXISTS t` | `DROP TABLE IF EXISTS t` |
| Add FK | `ALTER TABLE t ADD CONSTRAINT fk_... FOREIGN KEY ...` | same | same |

Column type resolution (ColumnType → dialect SQL type string) is implemented inline in each dialect class as a private helper. If the logic grows unwieldy across dialects, factor out a `ColumnTypeResolver` in a follow-up.

**`build.gradle.kts` for `schema-migration-generator`:**
```kotlin
import com.stano.buildlogic.configurePublishing

plugins { id("com.stano.java-library-convention") }

configurePublishing(
  name = "schema-migration-generator",
  description = "Generates SQL migration scripts from a ChangeSet for multiple database dialects.",
  url = "https://github.com/jstano/schema-java"
)

dependencies {
  api(platform(project(":schema-platform-dependencies")))
  api(project(":schema-diff"))
  implementation(project(":schema-model"))
  implementation("org.slf4j:slf4j-api")

  testImplementation(project(":test-platform-dependencies"))
  testImplementation(project(":schema-parser"))
}
```

`schema-diff` is declared `api` so consumers get the changeset model transitively.

---

## Changes to Existing Files

**`settings.gradle.kts`** — add (alphabetical order after existing entries):
```kotlin
include("schema-diff")
include("schema-migration-generator")
```

**`schema-bom/build.gradle.kts`** — add to `dependencies` block:
```kotlin
api(project(":schema-diff"))
api(project(":schema-migration-generator"))
```

---

## Module Dependency Graph

```
schema-model
    ↑
schema-diff  (api: schema-model)
    ↑
schema-migration-generator  (api: schema-diff, impl: schema-model)
```

`schema-parser` is `testImplementation` only in both new modules (loads XML test fixtures; not a runtime dependency).

---

## Test Strategy

### `schema-diff`

**JUnit 5 unit tests (`SchemaDiffEngineTest`, `ChangeSetWriterTest`, `ChangeSetParserTest`)**
- Build schemas programmatically (no XML needed for unit tests)
- One test per change type: `detectsAddedTable()`, `detectsDroppedColumn()`, `detectsModifiedColumnType()`, etc.
- `changeOrderIsDropBeforeAdd()` — verifies safe migration ordering
- `ChangeSetWriterTest` — builds a ChangeSet with one of each change type, asserts XML output
- `ChangeSetParserTest` — parses a known XML string, asserts resulting change objects
- `parsesManualRenameEdit()` — drop+add manually replaced with `<rename-column>` in XML; asserts `RenameColumnChange` produced

**Spock integration tests (`SchemaDiffRoundTripSpec`, `SchemaDiffEngineSpec`)**
- Load `old-schema.xml` and `new-schema.xml` via `SchemaParser`
- Diff → write XML → parse XML back → assert structural equivalence (round-trip)
- End-to-end: diff two schemas, assert the detected change set matches expectations

### `schema-migration-generator`

**JUnit 5 unit tests per dialect (`PostgreSQLMigrationGeneratorTest`, `H2MigrationGeneratorTest`, `SQLServerMigrationGeneratorTest`)**
- One test per change type per dialect
- Build a minimal single-change `ChangeSet`, generate into a `StringWriter`, assert SQL output
- Cover column modifiers: required, default value, type changes, nullability changes

**Spock integration test (`MigrationGeneratorIntegrationSpec`)**
- Load a `test-changeset.xml`, run `GenMigration` for each `DatabaseType`
- Assert output SQL contains expected statements for each dialect

---

## Deferred to Future Phase

| Item | Reason |
|---|---|
| Rename detection heuristic | Ambiguous; developer review step already handles it |
| Function / procedure / trigger diffs | Bodies are opaque SQL; drop-and-recreate semantics vary by dialect |
| `ModifyViewChange` | SQL body diffing is non-trivial |
| Enum type changes | PostgreSQL `ALTER TYPE ... ADD VALUE` has edge cases; H2/SQL Server have no native enum |
| Changeset versioning / author metadata | Useful for audit; not needed for core workflow |
| `AddTableChange` carrying full column list | Needed if CREATE TABLE should include columns; currently emits bare `CREATE TABLE name ()` and relies on subsequent `AddColumnChange` entries |
| ColumnTypeResolver refactor | Only needed if per-dialect type mapping grows unwieldy |
