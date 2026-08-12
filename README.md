# java-schema

A Java library for defining relational database schemas in vendor-neutral XML and either generating SQL DDL for multiple database dialects or installing schemas directly into a live database.

**Supported databases:** PostgreSQL, Microsoft SQL Server, H2

## What It Does

java-schema lets you:

1. **Define your schema once in XML** — independent of any specific database dialect
2. **Generate SQL DDL** for any supported database (PostgreSQL, SQL Server, H2)
3. **Install schemas directly** into a live database using Flyway or Liquibase
4. **Import existing schemas** from a live database into XML format (reverse engineering)
5. **Introspect the schema model** — use the parsed schema for code generation, validation, ORM mapping, etc.

## Quick Start

### 1. Define a Schema in XML

Create `src/main/resources/db/my-schema.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<database xmlns="http://stano.com/database"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://stano.com/database http://schema.stano.com/schema.xsd"
          foreignKeyMode="relations"
          booleanMode="native">

  <enum name="StatusType">
    <value name="ACTIVE" code="A"/>
    <value name="INACTIVE" code="I"/>
  </enum>

  <table name="Customer">
    <columns>
      <column name="ID"     type="sequence" required="true"/>
      <column name="Name"   type="varchar"  length="100" required="true"/>
      <column name="Status" type="enum"     enumType="StatusType" required="true"/>
    </columns>
    <keys>
      <primary><column name="ID"/></primary>
      <unique><column name="Name"/></unique>
    </keys>
  </table>

  <table name="Order">
    <columns>
      <column name="ID"         type="sequence" required="true"/>
      <column name="CustomerID" type="int"       required="true"/>
      <column name="OrderDate"  type="date"      required="true"/>
    </columns>
    <keys>
      <primary><column name="ID"/></primary>
    </keys>
    <relations>
      <relation src="CustomerID" table="Customer" column="ID" type="cascade"/>
    </relations>
  </table>
</database>
```

### 2. Parse the Schema

```java
Schema schema = new SchemaParser().parseSchema(
    MyApp.class.getResource("/db/my-schema.xml")
);
```

### 3. Generate SQL

```java
new GenSQL().generateSQL(
    DatabaseType.POSTGRESQL,
    schema,
    new PrintWriter(new FileWriter("schema-postgres.sql")),
    ForeignKeyMode.RELATIONS,
    BooleanMode.NATIVE,
    ";"
);
```

Or use the CLI:
```bash
java -cp ... com.stano.schema.gensql.GenSQL PGSQL,MSSQL my-schema.xml
```

### 4. Install into a Database

```java
SchemaInstaller installer = new FlywaySchemaInstaller();
installer.installSchema(dataSource, new FileSchemaContext(new File("my-schema.xml")));
```

## Dependency Management

Add to `build.gradle.kts`:

```kotlin
dependencies {
    implementation(platform("com.stano:schema-bom:0.54.0"))
    implementation("com.stano:schema-parser")
    implementation("com.stano:schema-sql-generator")
}
```

Or use a specific installer:

```kotlin
dependencies {
    implementation("com.stano:schema-installer-flyway:0.54.0")
    implementation("com.stano:schema-installer-liquibase:0.54.0")
}
```

## Module Overview

| Module | Purpose |
|--------|---------|
| **schema-model** | Core domain model (Schema, Table, Column, etc.) — no dependencies beyond Commons/SLF4J |
| **schema-parser** | Parses XML schema files into the model |
| **schema-sql-generator** | Generates SQL DDL for PostgreSQL, SQL Server, H2 |
| **schema-installer** | Abstract base for installing schemas into a live database |
| **schema-installer-flyway** | Concrete installer using Flyway (with SQL Server support) |
| **schema-installer-liquibase** | Concrete installer using Liquibase |
| **schema-migrations** | JDBC utility helpers for migration scripts (existence checks, safe drops, etc.) |
| **schema-reverse-engineer** | Reverse-engineers an existing database into XML schema format |
| **schema-diagram-generator** | Generates ER diagrams in Mermaid and PlantUML formats |
| **schema-diff** | Compares two `Schema` objects and produces a `ChangeSet` describing structural differences (added/dropped/renamed tables, columns, keys, constraints, relations, views, functions, procedures) |
| **schema-diff-git** | CLI (`GitSchemaDiffCli`) that reads and diffs schema XML across git revisions/tags |
| **schema-migration-generator** | Generates dialect-specific `ALTER`/`CREATE`/`DROP` migration SQL from a `ChangeSet` (PostgreSQL, SQL Server, H2) |
| **schema-bom** | Bill of Materials for version-aligned dependency management |

## Schema Diff & Migration

For an existing database whose schema needs to evolve to match a new XML definition, `schema-diff` and `schema-migration-generator` provide a state-based migration workflow: compare two schema versions in memory, then generate dialect-specific migration SQL from the resulting changeset.

### 1. Compare two schema versions

```java
Schema oldSchema = new SchemaParser().parseSchema(new File("schema-v1.xml").toURI().toURL());
Schema newSchema = new SchemaParser().parseSchema(new File("schema-v2.xml").toURI().toURL());

ChangeSet changeSet = new SchemaDiffEngine().diff(oldSchema, newSchema);
```

### 2. Generate migration SQL from the changeset

```java
new GenMigration().generateMigrationSQL(
    DatabaseType.POSTGRESQL, changeSet, new PrintWriter(new FileWriter("migration-postgres.sql")));
```

The diff engine always emits a drop+add pair for column renames; when it detects a plausible rename target it adds a `-- TODO: possible rename?` comment with the equivalent `ALTER TABLE ... RENAME COLUMN` above the drop+add in the generated SQL, for you to resolve manually.

### Diffing straight from git history

`schema-diff-git` combines both steps into one CLI command — it compares the version of a schema file at the last git commit against its current working-tree contents and writes migration SQL directly:

```bash
java -cp ... com.stano.schema.git.GitSchemaDiffCli my-schema.xml -d postgresql -o migration.sql
```

## XML Schema Format

The root element is `<database>`.

### Supported Column Types

`sequence`, `longsequence`, `byte`, `short`, `int`, `long`, `float`, `double`, `decimal` (with `length`+`scale`), `boolean`, `date`, `datetime`, `time`, `timestamp`, `timestamptz`, `char`, `varchar`, `enum`, `text`, `citext`, `cstext`, `binary`, `uuid`, `json`, `array` (with `elementType`)

### Top-Level Elements

- **`<table name="...">...</table>`** — relational table with columns, keys, relations, triggers, constraints, and initial data
- **`<enum name="...">...</enum>`** — enumeration type (used by `type="enum"` columns)
- **`<view name="...">...</view>`** — view definition (can be database-specific with `databaseType="postgres"`)
- **`<function name="...">...</function>`** — stored function (always database-specific)
- **`<procedure name="...">...</procedure>`** — stored procedure (always database-specific)
- **`<otherSql>...</otherSql>`** — raw SQL injected into generated output (can specify `order="top"` or `order="bottom"`)
- **`<schema name="...">...</schema>`** — groups tables/views/functions under a named schema namespace (e.g., for PostgreSQL schema namespaces)

### Table Definition

```xml
<table name="Employee" data="true" compress="true">
  <columns>
    <column name="ID" type="sequence" required="true"/>
    <column name="Name" type="varchar" length="100" required="true"/>
    <column name="Age" type="short" minValue="0" maxValue="150"/>
    <column name="Score" type="decimal" length="19" scale="4"/>
    <column name="Active" type="boolean" required="true" default="true"/>
    <column name="Status" type="enum" enumType="StatusType" required="true"/>
    <column name="Notes" type="varchar" length="200">
      <check>Notes like '%valid%'</check>
    </column>
  </columns>
  <keys>
    <primary>
      <column name="ID"/>
    </primary>
    <unique cluster="true">
      <column name="Name"/>
    </unique>
    <index compress="true" include="Name">
      <column name="Age"/>
    </index>
  </keys>
  <relations>
    <relation src="DeptID" table="Department" column="ID" type="cascade"/>
    <!-- type: cascade | enforce | setnull | donothing -->
  </relations>
  <constraints>
    <constraint name="chk_custom">... raw SQL constraint ...</constraint>
  </constraints>
  <initialData>
    <sql>INSERT INTO Employee (Name) VALUES ('Admin')</sql>
    <sql databaseType="postgres">INSERT INTO Employee (Name) VALUES ('Admin')</sql>
  </initialData>
  <triggers>
    <delete databaseType="postgres">... PL/pgSQL body ...</delete>
    <update databaseType="sqlserver">... T-SQL body ...</update>
  </triggers>
</table>
```

### Enums

```xml
<enum name="StatusType">
  <value name="ACTIVE" code="A"/>
  <value name="INACTIVE" code="I"/>
</enum>
```

### Views, Functions, Procedures

```xml
<view name="ActiveEmployees">
  SELECT * FROM Employee WHERE Active = TRUE
</view>

<function name="myFunc">
  <sql databaseType="postgres">CREATE OR REPLACE FUNCTION myFunc() RETURNS ... AS ... END;</sql>
  <sql databaseType="sqlserver">CREATE FUNCTION myFunc() RETURNS ... AS BEGIN ... END;</sql>
</function>

<procedure name="myProc">
  <sql databaseType="postgres">CREATE OR REPLACE PROCEDURE myProc() AS ... END;</sql>
  <sql databaseType="sqlserver">CREATE PROCEDURE myProc() AS BEGIN ... END;</sql>
</procedure>
```
## CLI Tools

### GenSQL — Generate SQL DDL

```bash
java -cp ... com.stano.schema.gensql.GenSQL <database-types> <schema-file> [options]

# Example: generate for PostgreSQL and SQL Server
java -cp ... com.stano.schema.gensql.GenSQL postgres,sqlserver schema.xml \
    --foreign-key-mode=relations \
    --boolean-mode=native \
    --output-indexes-only
```

**Database types:** `h2`, `postgres`, `sqlserver` (comma-separated, case-insensitive)

**Options:**
- `--foreign-key-mode` — `none`, `relations`, `triggers` (default: from XML)
- `--boolean-mode` — `native`, `yes_no`, `yn` (default: from XML or `native`)
- `--output-indexes-only` — generate only index DDL
- `--output-triggers-only` — generate only trigger DDL
- `--postgresql-version` — target PostgreSQL major version (e.g. `17`, `18`)

Produces files like `schema-postgres.sql`, `schema-sqlserver.sql`, `schema-h2.sql` alongside the input file.

## XSD Schema Reference

The XML schema (XSD) is located at:
```
http://schema.stano.com/schema.xsd
```

Use this URL in the `xsi:schemaLocation` attribute for XML IDE validation and autocompletion.

## License

MIT
