# java-schema

A Java library for defining relational database schemas in vendor-neutral XML and either generating SQL DDL for multiple database dialects or installing schemas directly into a live database.

**Supported databases:** PostgreSQL, MySQL, Microsoft SQL Server, H2

## What It Does

java-schema lets you:

1. **Define your schema once in XML** — independent of any specific database dialect
2. **Generate SQL DDL** for any supported database (PostgreSQL, MySQL, SQL Server, H2)
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
          xsi:schemaLocation="http://stano.com/database https://raw.githubusercontent.com/jstano/java-schema/refs/heads/main/schema-model/src/resources/schema.xsd"
          version="1.0"
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
    DatabaseType.POSTGRES,
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
    implementation(platform("com.stano:schema-bom:0.9.11"))
    implementation("com.stano:schema-parser")
    implementation("com.stano:schema-sql-generator")
}
```

Or use a specific installer:

```kotlin
dependencies {
    implementation("com.stano:schema-installer-flyway:0.9.11")
    implementation("com.stano:schema-installer-liquibase:0.9.11")
}
```

## Module Overview

| Module | Purpose |
|--------|---------|
| **schema-model** | Core domain model (Schema, Table, Column, etc.) — no dependencies beyond Commons/SLF4J |
| **schema-parser** | Parses XML schema files into the model |
| **schema-sql-generator** | Generates SQL DDL for PostgreSQL, MySQL, SQL Server, H2 |
| **schema-installer** | Abstract base for installing schemas into a live database |
| **schema-installer-flyway** | Concrete installer using Flyway (with SQL Server support) |
| **schema-installer-liquibase** | Concrete installer using Liquibase |
| **schema-migrations** | JDBC utility helpers for migration scripts (existence checks, safe drops, etc.) |
| **schema-importer** | Reverse-engineers an existing database into XML schema format |
| **schema-bom** | Bill of Materials for version-aligned dependency management |

## XML Schema Format

The root element is `<database>` with a `version` attribute (required, e.g., `"1.0"`, `"1.2.3"`).

### Supported Column Types

`sequence`, `longsequence`, `byte`, `short`, `int`, `long`, `float`, `double`, `decimal` (with `length`+`scale`), `boolean`, `date`, `datetime`, `time`, `timestamp`, `char`, `varchar`, `enum`, `text`, `binary`, `uuid`, `json`, `array` (with `elementType`)

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

## API Reference

### SchemaParser — Parse XML to Model

```java
Schema schema = new SchemaParser().parseSchema(URL schemaURL);
Schema schema = new SchemaParser().parseSchema(URL schemaURL, InputStream inputStream);
```

After parsing, `schema.sortTablesByName()` and `schema.buildReverseRelations()` are called automatically.

### Schema — Top-Level Model

```java
schema.getVersion()                    // Version(major, minor[, patch])
schema.getTables()                     // List<Table>
schema.getTable("Employee")            // Table (throws if not found)
schema.getOptionalTable("Employee")    // Optional<Table>
schema.getViews(DatabaseType.POSTGRES) // List<View> (merged generic + db-specific)
schema.getEnumTypes()                  // Collection<EnumType>
schema.getEnumType("StatusType")       // EnumType
schema.getFunctions()                  // List<Function>
schema.getProcedures()                 // List<Procedure>
schema.getOtherSql()                   // List<OtherSql>
schema.getForeignKeyMode()             // ForeignKeyMode or null
schema.getBooleanMode()                // BooleanMode (defaults to NATIVE)
schema.validate()                      // List<String> of validation errors
```

### Table — Per-Table Model

```java
table.getName()                // String
table.getSchemaName()          // String ("public" by default)
table.getColumns()             // List<Column>
table.getColumn("Name")        // Column (case-insensitive)
table.hasColumn("Name")        // boolean
table.getKeys()                // List<Key> (PRIMARY, UNIQUE, INDEX)
table.getPrimaryKey()          // Key or null
table.getPrimaryKeyColumns()   // List<String>
table.getIdentityColumn()      // Column with type SEQUENCE or LONGSEQUENCE
table.getRelations()           // List<Relation> (FK constraints on this table)
table.getReverseRelations()    // List<Relation> (populated after buildReverseRelations())
table.getTriggers()            // List<Trigger>
table.getConstraints()         // List<Constraint>
table.getInitialData()         // List<InitialData>
```

### Column — Per-Column Model

```java
column.getName()               // String
column.getType()               // ColumnType enum
column.getLength()             // int
column.getScale()              // int (for DECIMAL)
column.isRequired()            // boolean
column.getDefaultConstraint()  // String or null
column.getCheckConstraint()    // String or null
column.getMinValue()           // String or null
column.getMaxValue()           // String or null
column.getEnumType()           // String (enum name) or null
column.getElementType()        // ColumnType for ARRAY columns
column.getGenerated()          // String or null
```

### GenSQL — Generate SQL DDL

```java
GenSQL genSQL = new GenSQL();

genSQL.generateSQL(
    DatabaseType.POSTGRES,        // POSTGRES, MYSQL, SQL_SERVER, H2
    schema,                        // parsed Schema
    new PrintWriter(new FileWriter("output.sql")),
    ForeignKeyMode.RELATIONS,      // NONE | RELATIONS | TRIGGERS
    BooleanMode.NATIVE,            // NATIVE | YES_NO | YN
    OutputMode.ALL,                // ALL | INDEXES_ONLY | TRIGGERS_ONLY
    ";"                            // statement separator
);
```

### SchemaInstaller (Abstract) — Install Schema to Database

Concrete implementations: `LiquibaseSchemaInstaller` and `FlywaySchemaInstaller`.

```java
SchemaInstaller installer = new FlywaySchemaInstaller();
installer.installSchema(DataSource dataSource, SchemaContext context);
installer.installSchema(Connection connection, SchemaContext context);
installer.installSql(DataSource dataSource, SchemaContext context); // raw SQL files
```

Installation skips if `context.schemaIsInstalled(connection)` returns true.

### SchemaContext (Interface) — Configuration for Installation

```java
public interface SchemaContext {
    URL getSchemaUrl();
    ResourceLocator getMigrationScriptLocator(Connection);
    ResourceLocator getPostCreateScriptLocator(Connection);
    Version getSchemaVersion();
    Version getDatabaseVersion(Connection);
    BooleanMode getBooleanMode();
    ForeignKeyMode getForeignKeyMode();
    boolean schemaIsInstalled(Connection);
    void schemaInstalled(Connection);
}
```

**Common implementations:**

- **`FileSchemaContext`** — simplest; wraps a File and always treats schema as not installed (useful for dev/testing)
- **`DefaultSchemaContext`** — abstract base providing sensible defaults; subclass it for production

```java
// Simple usage
SchemaContext ctx = new FileSchemaContext(new File("my-schema.xml"));

// Production usage (extend DefaultSchemaContext)
public class MyAppSchemaContext extends DefaultSchemaContext {
    @Override
    public URL getSchemaUrl() {
        return getClass().getClassLoader().getResource("db/my-schema.xml");
    }

    @Override
    public ResourceLocator getMigrationScriptLocator(Connection conn) {
        return new ClasspathResourceLocator("db/migration");
    }
}
```

### MigrationServices — Helper Utilities

Use inside migration scripts to safely introspect and modify the database:

```java
// Existence checks
MigrationServices.tableExists(connection, "Customer")
MigrationServices.columnExists(connection, "Customer", "Name")
MigrationServices.indexExists(connection, "idx_customer_name")
MigrationServices.constraintExists(connection, "fk_order_customer")

// Safe drops
MigrationServices.dropIndex(connection, "Customer", "idx_customer_name")
MigrationServices.dropColumnCheckConstraint(connection, "Customer", "Age")
MigrationServices.dropColumnConstraints(connection, "Customer", "Score")
MigrationServices.dropTableConstraint(connection, "Customer", "my_constraint")
MigrationServices.dropAllTriggers(connection)

// Execution
MigrationServices.executeSQL(connection, "ALTER TABLE Customer ADD COLUMN Email VARCHAR(100)")

// Identifier normalization
String normalized = MigrationServices.normalizeIdentifierCase(connection, "MyColumn")
```

### SchemaImporter — Reverse-Engineer Database to XML

```bash
java -cp ... com.stano.schema.importer.SchemaImporter \
    --database jdbc:postgresql://localhost/mydb \
    --username user \
    --password pass \
    --file schema.xml
```

Produces an XML schema file from the live database structure. Currently optimized for PostgreSQL (introspects `pg_constraint` for check constraints).

## Full Workflow Example

```java
// 1. Parse schema from classpath
Schema schema = new SchemaParser().parseSchema(
    MyApp.class.getResource("/db/app-schema.xml")
);

// 2. Generate SQL for multiple databases
for (DatabaseType dbType : Arrays.asList(DatabaseType.POSTGRES, DatabaseType.MYSQL)) {
    new GenSQL().generateSQL(
        dbType,
        schema,
        new PrintWriter(new FileWriter("schema-" + dbType.name().toLowerCase() + ".sql")),
        ForeignKeyMode.RELATIONS,
        BooleanMode.NATIVE,
        ";"
    );
}

// 3. Install into a PostgreSQL database at runtime
DataSource ds = new HikariDataSource(config);
SchemaInstaller installer = new FlywaySchemaInstaller();
installer.installSchema(ds, new MyAppSchemaContext());

// 4. Introspect the model for code generation or validation
for (Table table : schema.getTables()) {
    System.out.println("Table: " + table.getName());
    for (Column col : table.getColumns()) {
        System.out.println("  - " + col.getName() + " : " + col.getType());
    }
    for (Relation rel : table.getRelations()) {
        System.out.println("  FK: " + rel.getFromColumnName()
            + " -> " + rel.getToTableName() + "." + rel.getToColumnName());
    }
}
```

## CLI Tools

### GenSQL — Generate SQL DDL

```bash
java -cp ... com.stano.schema.gensql.GenSQL <database-types> <schema-file> [options]

# Example: generate for PostgreSQL and MySQL
java -cp ... com.stano.schema.gensql.GenSQL PGSQL,MYSQL schema.xml \
    --foreign-key-mode=relations \
    --boolean-mode=native \
    --output-indexes-only
```

**Database types:** `H2`, `MYSQL`, `PGSQL`, `MSSQL` (comma-separated)

**Options:**
- `--foreign-key-mode` — `none`, `relations`, `triggers` (default: from XML)
- `--boolean-mode` — `native`, `yes_no`, `yn` (default: from XML or `native`)
- `--output-indexes-only` — generate only index DDL
- `--output-triggers-only` — generate only trigger DDL

Produces files like `schema-postgres.sql`, `schema-mysql.sql` alongside the input file.

### SchemaImporter — Reverse-Engineer Database

```bash
java -cp ... com.stano.schema.importer.SchemaImporter \
    --database <jdbc-url> \
    --username <user> \
    --password <pass> \
    --file <output.xml>
```

Introspects the live database and generates an equivalent XML schema file.

## XSD Schema Reference

The XML schema (XSD) is located at:
```
https://raw.githubusercontent.com/jstano/java-schema/refs/heads/main/schema-model/src/resources/schema.xsd
```

Use this URL in the `xsi:schemaLocation` attribute for XML IDE validation and autocompletion.

## License

MIT
