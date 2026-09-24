package com.stano.schema.genmigration.impl.sqlserver;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stano.schema.diff.ChangeSet;
import com.stano.schema.diff.change.AddColumnChange;
import com.stano.schema.diff.change.AddConstraintChange;
import com.stano.schema.diff.change.AddFunctionChange;
import com.stano.schema.diff.change.AddKeyChange;
import com.stano.schema.diff.change.AddProcedureChange;
import com.stano.schema.diff.change.AddRelationChange;
import com.stano.schema.diff.change.AddTableChange;
import com.stano.schema.diff.change.DropColumnChange;
import com.stano.schema.diff.change.DropConstraintChange;
import com.stano.schema.diff.change.DropFunctionChange;
import com.stano.schema.diff.change.DropKeyChange;
import com.stano.schema.diff.change.DropProcedureChange;
import com.stano.schema.diff.change.DropRelationChange;
import com.stano.schema.diff.change.DropTableChange;
import com.stano.schema.diff.change.RenameColumnChange;
import com.stano.schema.diff.change.RenameTableChange;
import com.stano.schema.genmigration.impl.common.MigrationGeneratorOptions;
import com.stano.schema.model.Column;
import com.stano.schema.model.ColumnPair;
import com.stano.schema.model.ColumnType;
import com.stano.schema.model.Constraint;
import com.stano.schema.model.DatabaseType;
import com.stano.schema.model.Function;
import com.stano.schema.model.Key;
import com.stano.schema.model.KeyColumn;
import com.stano.schema.model.KeyType;
import com.stano.schema.model.Procedure;
import com.stano.schema.model.Relation;
import com.stano.schema.model.RelationType;
import com.stano.schema.model.Schema;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SQLServerMigrationGenerator")
class SQLServerMigrationGeneratorTest {

  @Test
  @DisplayName("generates CREATE TABLE for add-table change")
  void generatesCreateTable() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(new AddTableChange("users"));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts =
        new MigrationGeneratorOptions(changeSet, pw, DatabaseType.SQL_SERVER);
    SQLServerMigrationGenerator gen = new SQLServerMigrationGenerator(opts);
    gen.generate();

    String sql = sw.toString();
    assertTrue(sql.contains("CREATE TABLE users ()"));
  }

  @Test
  @DisplayName("generates DROP TABLE IF EXISTS for drop-table change")
  void generatesDropTable() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(new DropTableChange("users"));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts =
        new MigrationGeneratorOptions(changeSet, pw, DatabaseType.SQL_SERVER);
    SQLServerMigrationGenerator gen = new SQLServerMigrationGenerator(opts);
    gen.generate();

    String sql = sw.toString();
    assertTrue(sql.contains("DROP TABLE IF EXISTS users"));
  }

  @Test
  @DisplayName("generates sp_rename for rename-table change")
  void generatesRenameTable() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(new RenameTableChange("customer", "customers"));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts =
        new MigrationGeneratorOptions(changeSet, pw, DatabaseType.SQL_SERVER);
    SQLServerMigrationGenerator gen = new SQLServerMigrationGenerator(opts);
    gen.generate();

    String sql = sw.toString();
    assertAll(
        () -> assertTrue(sql.contains("sp_rename")),
        () -> assertTrue(sql.contains("customer")),
        () -> assertTrue(sql.contains("customers")));
  }

  @Test
  @DisplayName("generates sp_rename with COLUMN for rename-column change")
  void generatesRenameColumn() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(new RenameColumnChange("customers", "name", "full_name"));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts =
        new MigrationGeneratorOptions(changeSet, pw, DatabaseType.SQL_SERVER);
    SQLServerMigrationGenerator gen = new SQLServerMigrationGenerator(opts);
    gen.generate();

    String sql = sw.toString();
    assertAll(
        () -> assertTrue(sql.contains("sp_rename")),
        () -> assertTrue(sql.contains("customers.name")),
        () -> assertTrue(sql.contains("full_name")),
        () -> assertTrue(sql.contains("'COLUMN'")));
  }

  @Test
  @DisplayName("uses GO statement separator for SQL Server")
  void useGoSeparator() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(new AddTableChange("users"));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts =
        new MigrationGeneratorOptions(changeSet, pw, DatabaseType.SQL_SERVER);
    SQLServerMigrationGenerator gen = new SQLServerMigrationGenerator(opts);
    gen.generate();

    String sql = sw.toString();
    assertTrue(sql.contains("GO"));
  }

  @Test
  @DisplayName("generates function SQL for add-function change with SQL Server database type")
  void generatesAddFunction() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(
        new AddFunctionChange(
            new Function(
                null, "fn_greet", DatabaseType.SQL_SERVER, "CREATE FUNCTION dbo.fn_greet()")));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts =
        new MigrationGeneratorOptions(changeSet, pw, DatabaseType.SQL_SERVER);
    SQLServerMigrationGenerator gen = new SQLServerMigrationGenerator(opts);
    gen.generate();

    assertTrue(sw.toString().contains("CREATE FUNCTION dbo.fn_greet()"));
  }

  @Test
  @DisplayName("generates DROP FUNCTION IF EXISTS dbo for drop-function change")
  void generatesDropFunction() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(new DropFunctionChange("fn_greet", DatabaseType.SQL_SERVER));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts =
        new MigrationGeneratorOptions(changeSet, pw, DatabaseType.SQL_SERVER);
    SQLServerMigrationGenerator gen = new SQLServerMigrationGenerator(opts);
    gen.generate();

    assertTrue(sw.toString().contains("DROP FUNCTION IF EXISTS dbo.fn_greet"));
  }

  @Test
  @DisplayName("generates procedure SQL for add-procedure change with SQL Server database type")
  void generatesAddProcedure() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(
        new AddProcedureChange(
            new Procedure(
                null, "sp_audit", DatabaseType.SQL_SERVER, "CREATE PROCEDURE dbo.sp_audit()")));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts =
        new MigrationGeneratorOptions(changeSet, pw, DatabaseType.SQL_SERVER);
    SQLServerMigrationGenerator gen = new SQLServerMigrationGenerator(opts);
    gen.generate();

    assertTrue(sw.toString().contains("CREATE PROCEDURE dbo.sp_audit()"));
  }

  @Test
  @DisplayName("generates DROP PROCEDURE IF EXISTS dbo for drop-procedure change")
  void generatesDropProcedure() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(new DropProcedureChange("sp_audit", DatabaseType.SQL_SERVER));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts =
        new MigrationGeneratorOptions(changeSet, pw, DatabaseType.SQL_SERVER);
    SQLServerMigrationGenerator gen = new SQLServerMigrationGenerator(opts);
    gen.generate();

    assertTrue(sw.toString().contains("DROP PROCEDURE IF EXISTS dbo.sp_audit"));
  }

  @Test
  @DisplayName("add-column emits a CHECK constraint matching min/max bounds")
  void generatesAddColumnMinMaxCheckConstraint() {
    ChangeSet changeSet = new ChangeSet();
    Column col =
        new Column("price", ColumnType.INT, 0, 0, false, null, null, null, "0", "100", null, null);
    changeSet.addChange(new AddColumnChange("product", col));

    Schema schema = new Schema(null);
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts =
        new MigrationGeneratorOptions(changeSet, pw, DatabaseType.SQL_SERVER, schema);
    SQLServerMigrationGenerator gen = new SQLServerMigrationGenerator(opts);
    gen.generate();

    String sql = sw.toString();
    assertTrue(
        sql.contains("ADD CONSTRAINT ck_product_price_")
            && sql.contains("check(price >= 0 and price <= 100)"),
        "expected a min/max CHECK constraint, got: " + sql);
  }

  @Test
  @DisplayName("guards CREATE TABLE with an OBJECT_ID existence check")
  void generatesCreateTableIsGuarded() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(new AddTableChange("items"));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts =
        new MigrationGeneratorOptions(changeSet, pw, DatabaseType.SQL_SERVER);
    SQLServerMigrationGenerator gen = new SQLServerMigrationGenerator(opts);
    gen.generate();

    String sql = sw.toString();
    assertTrue(
        sql.contains("IF OBJECT_ID('items', 'U') IS NULL CREATE TABLE items ()"), "got: " + sql);
  }

  @Test
  @DisplayName("guards ADD COLUMN with a sys.columns existence check")
  void generatesAddColumnIsGuarded() {
    ChangeSet changeSet = new ChangeSet();
    Column col =
        new Column(
            "email", ColumnType.VARCHAR, 100, 0, true, null, null, null, null, null, null, null);
    changeSet.addChange(new AddColumnChange("users", col));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts =
        new MigrationGeneratorOptions(changeSet, pw, DatabaseType.SQL_SERVER);
    SQLServerMigrationGenerator gen = new SQLServerMigrationGenerator(opts);
    gen.generate();

    String sql = sw.toString();
    assertTrue(
        sql.contains(
            "IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('users') AND name"
                + " = 'email')"),
        "got: " + sql);
  }

  @Test
  @DisplayName("guards DROP COLUMN with a sys.columns existence check")
  void generatesDropColumnIsGuarded() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(new DropColumnChange("users", "legacy", List.of()));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts =
        new MigrationGeneratorOptions(changeSet, pw, DatabaseType.SQL_SERVER);
    SQLServerMigrationGenerator gen = new SQLServerMigrationGenerator(opts);
    gen.generate();

    String sql = sw.toString();
    assertTrue(
        sql.contains(
            "IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('users') AND name ="
                + " 'legacy')"),
        "got: " + sql);
  }

  @Test
  @DisplayName("guards ADD PRIMARY KEY with a sys.key_constraints existence check")
  void generatesAddPrimaryKeyIsGuarded() {
    ChangeSet changeSet = new ChangeSet();
    List<KeyColumn> cols = new ArrayList<>();
    cols.add(new KeyColumn("id"));
    Key key = new Key(KeyType.PRIMARY, cols);
    changeSet.addChange(new AddKeyChange("orders", key, 1));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts =
        new MigrationGeneratorOptions(changeSet, pw, DatabaseType.SQL_SERVER);
    SQLServerMigrationGenerator gen = new SQLServerMigrationGenerator(opts);
    gen.generate();

    String sql = sw.toString();
    assertAll(
        () ->
            assertTrue(
                sql.contains("FROM sys.key_constraints WHERE name = 'pk_orders'"), "got: " + sql),
        () -> assertTrue(sql.contains("ADD CONSTRAINT pk_orders PRIMARY KEY (id)"), "got: " + sql));
  }

  @Test
  @DisplayName("guards ADD unique key with a sys.key_constraints existence check")
  void generatesAddUniqueKeyIsGuarded() {
    ChangeSet changeSet = new ChangeSet();
    List<KeyColumn> cols = new ArrayList<>();
    cols.add(new KeyColumn("email"));
    Key key = new Key(KeyType.UNIQUE, cols);
    changeSet.addChange(new AddKeyChange("users", key, 1));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts =
        new MigrationGeneratorOptions(changeSet, pw, DatabaseType.SQL_SERVER);
    SQLServerMigrationGenerator gen = new SQLServerMigrationGenerator(opts);
    gen.generate();

    String sql = sw.toString();
    assertAll(
        () ->
            assertTrue(
                sql.contains("FROM sys.key_constraints WHERE name = 'ak_users1'"), "got: " + sql),
        () -> assertTrue(sql.contains("ADD CONSTRAINT ak_users1 UNIQUE (email)"), "got: " + sql));
  }

  @Test
  @DisplayName("guards DROP unique key with a sys.key_constraints existence check")
  void generatesDropUniqueKeyIsGuarded() {
    ChangeSet changeSet = new ChangeSet();
    List<KeyColumn> cols = new ArrayList<>();
    cols.add(new KeyColumn("email"));
    Key key = new Key(KeyType.UNIQUE, cols);
    changeSet.addChange(new DropKeyChange("users", key, 1));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts =
        new MigrationGeneratorOptions(changeSet, pw, DatabaseType.SQL_SERVER);
    SQLServerMigrationGenerator gen = new SQLServerMigrationGenerator(opts);
    gen.generate();

    String sql = sw.toString();
    assertAll(
        () ->
            assertTrue(
                sql.contains("EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = 'ak_users1'"),
                "got: " + sql),
        () -> assertTrue(sql.contains("DROP CONSTRAINT ak_users1"), "got: " + sql));
  }

  @Test
  @DisplayName("generates unique filtered index for a promoted unique+where index key")
  void generatesUniqueFilteredIndex() {
    ChangeSet changeSet = new ChangeSet();
    List<KeyColumn> cols = new ArrayList<>();
    cols.add(new KeyColumn("parent_id"));
    Key key = new Key(KeyType.INDEX, cols, false, false, true, null, "parent_id is not null");
    changeSet.addChange(new AddKeyChange("users", key, 1));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts =
        new MigrationGeneratorOptions(changeSet, pw, DatabaseType.SQL_SERVER);
    SQLServerMigrationGenerator gen = new SQLServerMigrationGenerator(opts);
    gen.generate();

    String sql = sw.toString();
    assertAll(
        () -> assertTrue(sql.contains("CREATE UNIQUE INDEX"), "got: " + sql),
        () -> assertTrue(sql.contains("WHERE parent_id is not null"), "got: " + sql));
  }

  @Test
  @DisplayName("guards DROP PRIMARY KEY with a sys.key_constraints existence check")
  void generatesDropPrimaryKeyIsGuarded() {
    ChangeSet changeSet = new ChangeSet();
    List<KeyColumn> cols = new ArrayList<>();
    cols.add(new KeyColumn("id"));
    Key key = new Key(KeyType.PRIMARY, cols);
    changeSet.addChange(new DropKeyChange("orders", key, 1));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts =
        new MigrationGeneratorOptions(changeSet, pw, DatabaseType.SQL_SERVER);
    SQLServerMigrationGenerator gen = new SQLServerMigrationGenerator(opts);
    gen.generate();

    String sql = sw.toString();
    assertAll(
        () ->
            assertTrue(
                sql.contains("EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = 'pk_orders'"),
                "got: " + sql),
        () -> assertTrue(sql.contains("DROP CONSTRAINT pk_orders"), "got: " + sql));
  }

  @Test
  @DisplayName("guards ADD CONSTRAINT with a sys.check_constraints existence check")
  void generatesAddConstraintIsGuarded() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(
        new AddConstraintChange(
            "orders", new Constraint("ck_orders_total", "total >= 0", DatabaseType.SQL_SERVER)));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts =
        new MigrationGeneratorOptions(changeSet, pw, DatabaseType.SQL_SERVER);
    SQLServerMigrationGenerator gen = new SQLServerMigrationGenerator(opts);
    gen.generate();

    String sql = sw.toString();
    assertTrue(
        sql.contains("FROM sys.check_constraints WHERE name = 'ck_orders_total'"), "got: " + sql);
  }

  @Test
  @DisplayName("guards DROP CONSTRAINT with a sys.objects existence check")
  void generatesDropConstraintIsGuarded() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(new DropConstraintChange("orders", "ck_orders_total"));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts =
        new MigrationGeneratorOptions(changeSet, pw, DatabaseType.SQL_SERVER);
    SQLServerMigrationGenerator gen = new SQLServerMigrationGenerator(opts);
    gen.generate();

    String sql = sw.toString();
    assertTrue(
        sql.contains(
            "IF EXISTS (SELECT 1 FROM sys.objects WHERE name = 'ck_orders_total' AND"
                + " parent_object_id = OBJECT_ID('orders'))"),
        "got: " + sql);
  }

  @Test
  @DisplayName("guards ADD RELATION with a sys.foreign_keys existence check")
  void generatesAddRelationIsGuarded() {
    ChangeSet changeSet = new ChangeSet();
    Relation rel =
        new Relation("orders", "customer_id", "customers", "id", RelationType.CASCADE, false);
    changeSet.addChange(new AddRelationChange(rel, 1));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts =
        new MigrationGeneratorOptions(changeSet, pw, DatabaseType.SQL_SERVER);
    SQLServerMigrationGenerator gen = new SQLServerMigrationGenerator(opts);
    gen.generate();

    String sql = sw.toString();
    assertAll(
        () ->
            assertTrue(
                sql.contains("NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name ="),
                "got: " + sql),
        () ->
            assertTrue(
                sql.contains("FOREIGN KEY (customer_id) REFERENCES customers(id)"), "got: " + sql));
  }

  @Test
  @DisplayName("generates composite FOREIGN KEY listing all column pairs for add-relation change")
  void generatesAddCompositeRelationListsAllColumnPairs() {
    ChangeSet changeSet = new ChangeSet();
    Relation rel =
        Relation.composite(
            "assignments",
            "properties",
            List.of(
                new ColumnPair("parent_assignment_id", "id"),
                new ColumnPair("property_id", "property_id")),
            RelationType.CASCADE,
            false);
    changeSet.addChange(new AddRelationChange(rel, 1));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts =
        new MigrationGeneratorOptions(changeSet, pw, DatabaseType.SQL_SERVER);
    SQLServerMigrationGenerator gen = new SQLServerMigrationGenerator(opts);
    gen.generate();

    String sql = sw.toString();
    assertAll(
        () ->
            assertTrue(
                sql.contains("FOREIGN KEY (parent_assignment_id, property_id)"), "got: " + sql),
        () -> assertTrue(sql.contains("REFERENCES properties(id, property_id)"), "got: " + sql));
  }

  @Test
  @DisplayName("guards DROP RELATION with a sys.foreign_keys existence check")
  void generatesDropRelationIsGuarded() {
    ChangeSet changeSet = new ChangeSet();
    Relation rel =
        new Relation("orders", "customer_id", "customers", "id", RelationType.CASCADE, false);
    changeSet.addChange(new DropRelationChange(rel, 1));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts =
        new MigrationGeneratorOptions(changeSet, pw, DatabaseType.SQL_SERVER);
    SQLServerMigrationGenerator gen = new SQLServerMigrationGenerator(opts);
    gen.generate();

    String sql = sw.toString();
    assertTrue(sql.contains("EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name ="), "got: " + sql);
  }

  @Test
  @DisplayName("guards RENAME TABLE with an OBJECT_ID existence check")
  void generatesRenameTableIsGuarded() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(new RenameTableChange("orders", "sales_orders"));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts =
        new MigrationGeneratorOptions(changeSet, pw, DatabaseType.SQL_SERVER);
    SQLServerMigrationGenerator gen = new SQLServerMigrationGenerator(opts);
    gen.generate();

    String sql = sw.toString();
    assertTrue(
        sql.contains(
            "IF OBJECT_ID('orders', 'U') IS NOT NULL AND OBJECT_ID('sales_orders', 'U') IS NULL"),
        "got: " + sql);
  }

  @Test
  @DisplayName("guards RENAME COLUMN with a sys.columns existence check")
  void generatesRenameColumnIsGuarded() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(new RenameColumnChange("users", "first_name", "given_name"));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts =
        new MigrationGeneratorOptions(changeSet, pw, DatabaseType.SQL_SERVER);
    SQLServerMigrationGenerator gen = new SQLServerMigrationGenerator(opts);
    gen.generate();

    String sql = sw.toString();
    assertAll(
        () -> assertTrue(sql.contains("name = 'first_name') AND NOT EXISTS"), "got: " + sql),
        () ->
            assertTrue(
                sql.contains("sp_rename 'users.first_name', 'given_name', 'COLUMN'"),
                "got: " + sql));
  }
}
