# Schema Diff & Migration Generator — Implementation Checklist

## Context

Implement two new modules (`schema-diff` and `schema-migration-generator`) as specified in `docs/plans/schema-migrator.md`. These enable a state-based migration workflow: diff two Schema XMLs → produce editable changeset XML → generate dialect-specific ALTER/DROP/CREATE SQL.

Natural stopping points after each phase. Each phase ends with a working build.

---

## Phase 1: Scaffold (build system wiring)

- [x] Add `include("schema-diff")` and `include("schema-migration-generator")` to `settings.gradle.kts` (alphabetical order)
- [x] Create `schema-diff/build.gradle.kts` (see plan doc for exact content — api: schema-model, testImpl: schema-parser)
- [x] Create `schema-migration-generator/build.gradle.kts` (api: schema-diff, impl: schema-model, testImpl: schema-parser)
- [x] Add `api(project(":schema-diff"))` and `api(project(":schema-migration-generator"))` to `schema-bom/build.gradle.kts`
- [x] Create empty placeholder source directories (`src/main/java/.gitkeep`) so Gradle can resolve the modules
- [x] **Verify:** `./gradlew :schema-diff:build :schema-migration-generator:build` (empty modules compile)

---

## Phase 2: `schema-diff` — Change Model

Package: `com.stano.schema.diff` and `com.stano.schema.diff.change`

- [x] `SchemaChange.java` — empty marker interface
- [x] `ChangeSet.java` — wraps `List<SchemaChange>` with `addChange()`, `getChanges()`, `isEmpty()`
- [x] `change/AddTableChange.java` — `final`, immutable, `String tableName`
- [x] `change/DropTableChange.java` — `String tableName`
- [x] `change/RenameTableChange.java` — `String oldName, newName`
- [x] `change/AddColumnChange.java` — `String tableName`, full `Column column`
- [x] `change/DropColumnChange.java` — `String tableName, columnName`
- [x] `change/RenameColumnChange.java` — `String tableName, oldName, newName`
- [x] `change/ModifyColumnChange.java` — `String tableName`, `Column oldColumn, newColumn`
- [x] `change/AddKeyChange.java` — `String tableName`, `Key key`
- [x] `change/DropKeyChange.java` — `String tableName`, `Key key`
- [x] `change/AddConstraintChange.java` — `String tableName`, `Constraint constraint`
- [x] `change/DropConstraintChange.java` — `String tableName, constraintName`
- [x] `change/AddRelationChange.java` — `Relation relation`
- [x] `change/DropRelationChange.java` — `Relation relation`
- [x] `change/AddViewChange.java` — `View view`
- [x] `change/DropViewChange.java` — `String viewName`
- [x] **Verify:** `./gradlew :schema-diff:compileJava`

---

## Phase 3: `schema-diff` — SchemaDiffEngine

- [x] `SchemaDiffEngine.java` — `public ChangeSet diff(Schema oldSchema, Schema newSchema)`
  - Drop ordering: views → relations → keys → constraints → columns → tables
  - Add ordering: tables → columns → modify columns → keys → constraints → relations → views
  - Table diff: by name (missing in new = Drop, missing in old = Add)
  - Column diff: per-table by name; detect added/dropped/modified (type, length, scale, required, default)
  - Key diff: per-table by `key.getType()` + `key.getColumnsAsString()`
  - Constraint diff: per-table by `constraint.getName()`
  - Relation diff: by `fromTable+fromColumn+toTable+toColumn`
  - View diff: by `view.getName()`
- [x] **Verify:** `./gradlew :schema-diff:compileJava`

---

## Phase 4: `schema-diff` — ChangeSetWriter and Parser

- [x] `ChangeSetWriter.java` — `write(ChangeSet, PrintWriter)` → emits XML per plan doc format
  - Column attributes: name, type (lowercased), length (if >0), scale (if >0), required, default (if non-null)
  - Key columns: use `key.getColumnsAsString()`
- [x] `ChangeSetParserException.java` — `RuntimeException` wrapping constructor
- [x] `ChangeSetParser.java` — `parse(URL)` and `parse(InputStream)` via JAXP SAX
  - Single `DefaultHandler` inner class, `startElement` switch on `localName`
  - Reconstruct `Column` from attributes using full constructor
  - Reconstruct `Key` with `KeyType.valueOf(type.toUpperCase())`, split `columns` on comma → `List<KeyColumn>`
  - Reconstruct `Relation` from from-table/from-column/to-table/to-column/type attributes
  - Support both `<rename-table>` / `<rename-column>` and drop+add forms
- [x] **Verify:** `./gradlew :schema-diff:compileJava`

---

## Phase 5: `schema-diff` — Tests

- [x] `SchemaDiffEngineTest.java` — one test per change type (build schemas programmatically), plus `changeOrderIsDropBeforeAdd()` ✓ (11 tests passing)
- [x] `ChangeSetWriterTest.java` — one of each change type → assert XML output string ✓ (9 tests passing)
- [x] `ChangeSetParserTest.java` — parse known XML string → assert change object fields; `parsesManualRenameEdit()` for `<rename-column>` ✓ (6 tests passing - SAX localName/qName fix)
- [x] Create test fixture XMLs: `old-schema.xml`, `new-schema.xml` in `schema-diff/src/test/resources/` ✓
- [x] `SchemaDiffIntegrationTest.java` (JUnit) — load fixtures via SchemaParser, diff → write → parse → assert structural equivalence ✓ (3 integration tests)
- [x] **Verify:** `./gradlew :schema-diff:test` ✓ (38/38 tests passing)

---

## Phase 6: `schema-migration-generator` — Core Infrastructure

Package: `com.stano.schema.genmigration`

- [x] `MigrationGeneratorOptions.java` (in `impl/common`) — fields: `ChangeSet, PrintWriter, DatabaseType, String statementSeparator`; two constructors (explicit separator, and derive from `databaseType.getStatementSeparator()`)
- [x] `MigrationGenerator.java` (abstract, in `impl/common`) — `generate()` walks changes, dispatches via Java 21 `switch (change) { case AddTableChange c -> ... }`, closes writer in `finally`; one `protected abstract void generateXxx(XxxChange)` per change type
- [x] `MigrationGeneratorFactory.java` (in `impl/common`) — switch on `DatabaseType` → return dialect impl
- [x] `GenMigration.java` — public `migrationGeneratorFactory` field (not private, matches `GenSQL`); two `generateMigrationSQL()` overloads
- [x] **Verify:** `./gradlew :schema-migration-generator:compileJava`

---

## Phase 7: `schema-migration-generator` — Dialect Implementations

FK constraint name convention: `fk_<fromTable>_<fromColumn>` (truncate to `databaseType.getMaxKeyNameLength()`).

- [x] `impl/postgresql/PostgreSQLMigrationGenerator.java`:
  - `ADD TABLE` → `CREATE TABLE name ()`
  - `DROP TABLE` → `DROP TABLE IF EXISTS name`
  - `RENAME TABLE` → `ALTER TABLE old RENAME TO new`
  - `ADD COLUMN` → `ALTER TABLE t ADD COLUMN name type [NOT NULL] [DEFAULT val]`
  - `DROP COLUMN` → `ALTER TABLE t DROP COLUMN name`
  - `RENAME COLUMN` → `ALTER TABLE t RENAME COLUMN old TO new`
  - `MODIFY COLUMN` → `ALTER TABLE t ALTER COLUMN name TYPE newtype` + SET/DROP NOT NULL
  - `ADD KEY` (primary) → `ALTER TABLE t ADD PRIMARY KEY (cols)`
  - `ADD KEY` (unique/index) → `CREATE [UNIQUE] INDEX idx_... ON t (cols)`
  - `DROP KEY` → `ALTER TABLE t DROP CONSTRAINT ...` or `DROP INDEX ...`
  - `ADD CONSTRAINT` → `ALTER TABLE t ADD CONSTRAINT name CHECK (sql)`
  - `DROP CONSTRAINT` → `ALTER TABLE t DROP CONSTRAINT name`
  - `ADD RELATION` → `ALTER TABLE t ADD CONSTRAINT fk_... FOREIGN KEY (col) REFERENCES other(col) [ON DELETE CASCADE]`
  - `DROP RELATION` → `ALTER TABLE t DROP CONSTRAINT fk_...`
  - `ADD VIEW` → `CREATE OR REPLACE VIEW name AS sql`
  - `DROP VIEW` → `DROP VIEW IF EXISTS name`
  - Private `toSqlType(ColumnType)` helper
- [x] `impl/h2/H2MigrationGenerator.java` — same as PostgreSQL except:
  - `RENAME COLUMN` → `ALTER TABLE t ALTER COLUMN old RENAME TO new`
  - `MODIFY COLUMN` → drop + re-add (H2 lacks ALTER COLUMN TYPE)
- [x] `impl/sqlserver/SQLServerMigrationGenerator.java` — same as PostgreSQL except:
  - `RENAME TABLE` → `EXEC sp_rename 'old', 'new'`
  - `RENAME COLUMN` → `EXEC sp_rename 't.old', 'new', 'COLUMN'`
  - `MODIFY COLUMN` → `ALTER TABLE t ALTER COLUMN name newtype [NOT NULL]`
- [x] **Verify:** `./gradlew :schema-migration-generator:compileJava`

---

## Phase 8: `schema-migration-generator` — Tests

- [x] `PostgreSQLMigrationGeneratorTest.java` — one test per change type, generate into `StringWriter`, assert SQL ✓ (8 tests)
- [x] `H2MigrationGeneratorTest.java` — same pattern, H2-specific assertions for rename/modify ✓ (3 tests)
- [x] `SQLServerMigrationGeneratorTest.java` — same pattern, SQL Server-specific assertions ✓ (3 tests)
- [x] `MigrationGeneratorIntegrationTest.java` (JUnit) — load changeset, run `GenMigration` for each dialect, assert output ✓ (3 integration tests)
- [x] **Verify:** `./gradlew :schema-migration-generator:test` ✓ (17 tests passing)

---

## Phase 9: Full Build Sign-off

- [x] `./gradlew build` — ✓ All 119 actionable tasks passing
- [x] `./gradlew check` — ✓ Spotless formatting + all tests pass
