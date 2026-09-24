package com.stano.schema.genmigration.impl.h2;

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
import com.stano.schema.diff.change.ModifyColumnChange;
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

@DisplayName("H2MigrationGenerator")
class H2MigrationGeneratorTest {

  @Test
  @DisplayName("generates CREATE TABLE for add-table change")
  void generatesCreateTable() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(new AddTableChange("users"));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts = new MigrationGeneratorOptions(changeSet, pw, DatabaseType.H2);
    H2MigrationGenerator gen = new H2MigrationGenerator(opts);
    gen.generate();

    String sql = sw.toString();
    assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS users ()"));
  }

  @Test
  @DisplayName("generates DROP TABLE IF EXISTS for drop-table change")
  void generatesDropTable() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(new DropTableChange("users"));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts = new MigrationGeneratorOptions(changeSet, pw, DatabaseType.H2);
    H2MigrationGenerator gen = new H2MigrationGenerator(opts);
    gen.generate();

    String sql = sw.toString();
    assertTrue(sql.contains("DROP TABLE IF EXISTS users"));
  }

  @Test
  @DisplayName("generates RENAME TO for rename-table change")
  void generatesRenameTable() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(new RenameTableChange("customer", "customers"));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts = new MigrationGeneratorOptions(changeSet, pw, DatabaseType.H2);
    H2MigrationGenerator gen = new H2MigrationGenerator(opts);
    gen.generate();

    String sql = sw.toString();
    assertAll(() -> assertTrue(sql.contains("ALTER TABLE IF EXISTS customer RENAME TO customers")));
  }

  @Test
  @DisplayName("generates ALTER COLUMN RENAME TO for rename-column change")
  void generatesRenameColumn() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(new RenameColumnChange("customers", "name", "full_name"));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts = new MigrationGeneratorOptions(changeSet, pw, DatabaseType.H2);
    H2MigrationGenerator gen = new H2MigrationGenerator(opts);
    gen.generate();

    String sql = sw.toString();
    assertAll(
        () ->
            assertTrue(
                sql.contains(
                    "ALTER TABLE customers ALTER COLUMN IF EXISTS name RENAME TO full_name")));
  }

  @Test
  @DisplayName("drops and re-adds column for modify (H2 limitation)")
  void generatesDropAndReadd() {
    ChangeSet changeSet = new ChangeSet();
    Column oldCol =
        new Column(
            "email", ColumnType.VARCHAR, 100, 0, false, null, null, null, null, null, null, null);
    Column newCol =
        new Column(
            "email", ColumnType.VARCHAR, 255, 0, false, null, null, null, null, null, null, null);
    changeSet.addChange(new ModifyColumnChange("users", oldCol, newCol));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts = new MigrationGeneratorOptions(changeSet, pw, DatabaseType.H2);
    H2MigrationGenerator gen = new H2MigrationGenerator(opts);
    gen.generate();

    String sql = sw.toString();
    assertTrue(sql.contains("DROP COLUMN"));
    assertTrue(sql.contains("ADD COLUMN"));
  }

  @Test
  @DisplayName("generates function SQL for add-function change with H2 database type")
  void generatesAddFunction() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(
        new AddFunctionChange(
            new Function(null, "fn_greet", DatabaseType.H2, "CREATE ALIAS fn_greet AS $$")));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts = new MigrationGeneratorOptions(changeSet, pw, DatabaseType.H2);
    H2MigrationGenerator gen = new H2MigrationGenerator(opts);
    gen.generate();

    assertTrue(sw.toString().contains("CREATE ALIAS fn_greet AS $$"));
  }

  @Test
  @DisplayName("generates DROP FUNCTION IF EXISTS for drop-function change")
  void generatesDropFunction() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(new DropFunctionChange("fn_greet", DatabaseType.H2));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts = new MigrationGeneratorOptions(changeSet, pw, DatabaseType.H2);
    H2MigrationGenerator gen = new H2MigrationGenerator(opts);
    gen.generate();

    assertTrue(sw.toString().contains("DROP FUNCTION IF EXISTS fn_greet"));
  }

  @Test
  @DisplayName("generates procedure SQL for add-procedure change with H2 database type")
  void generatesAddProcedure() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(
        new AddProcedureChange(
            new Procedure(null, "sp_audit", DatabaseType.H2, "CREATE PROCEDURE sp_audit()")));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts = new MigrationGeneratorOptions(changeSet, pw, DatabaseType.H2);
    H2MigrationGenerator gen = new H2MigrationGenerator(opts);
    gen.generate();

    assertTrue(sw.toString().contains("CREATE PROCEDURE sp_audit()"));
  }

  @Test
  @DisplayName("generates DROP PROCEDURE IF EXISTS for drop-procedure change")
  void generatesDropProcedure() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(new DropProcedureChange("sp_audit", DatabaseType.H2));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts = new MigrationGeneratorOptions(changeSet, pw, DatabaseType.H2);
    H2MigrationGenerator gen = new H2MigrationGenerator(opts);
    gen.generate();

    assertTrue(sw.toString().contains("DROP PROCEDURE IF EXISTS sp_audit"));
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
        new MigrationGeneratorOptions(changeSet, pw, DatabaseType.H2, schema);
    H2MigrationGenerator gen = new H2MigrationGenerator(opts);
    gen.generate();

    String sql = sw.toString();
    assertTrue(
        sql.contains("ADD CONSTRAINT IF NOT EXISTS ck_product_price_")
            && sql.contains("check(price >= 0 and price <= 100)"),
        "expected a min/max CHECK constraint, got: " + sql);
  }

  @Test
  @DisplayName("guards ADD PRIMARY KEY with IF NOT EXISTS")
  void generatesAddPrimaryKeyIsGuarded() {
    ChangeSet changeSet = new ChangeSet();
    List<KeyColumn> cols = new ArrayList<>();
    cols.add(new KeyColumn("id"));
    Key key = new Key(KeyType.PRIMARY, cols);
    changeSet.addChange(new AddKeyChange("orders", key, 1));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts = new MigrationGeneratorOptions(changeSet, pw, DatabaseType.H2);
    H2MigrationGenerator gen = new H2MigrationGenerator(opts);
    gen.generate();

    String sql = sw.toString();
    assertTrue(
        sql.contains("ADD CONSTRAINT IF NOT EXISTS pk_orders PRIMARY KEY (id)"), "got: " + sql);
  }

  @Test
  @DisplayName("guards ADD unique index with IF NOT EXISTS")
  void generatesAddUniqueKeyIsGuarded() {
    ChangeSet changeSet = new ChangeSet();
    List<KeyColumn> cols = new ArrayList<>();
    cols.add(new KeyColumn("email"));
    Key key = new Key(KeyType.UNIQUE, cols);
    changeSet.addChange(new AddKeyChange("users", key, 1));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts = new MigrationGeneratorOptions(changeSet, pw, DatabaseType.H2);
    H2MigrationGenerator gen = new H2MigrationGenerator(opts);
    gen.generate();

    String sql = sw.toString();
    assertTrue(sql.contains("CREATE UNIQUE INDEX IF NOT EXISTS"), "got: " + sql);
  }

  @Test
  @DisplayName("generates a plain unique index and drops the filter (H2 has no partial-index syntax)")
  void generatesUniqueIndexWithoutFilter() {
    ChangeSet changeSet = new ChangeSet();
    List<KeyColumn> cols = new ArrayList<>();
    cols.add(new KeyColumn("parent_id"));
    Key key =
        new Key(KeyType.INDEX, cols, false, false, true, null, "parent_id is not null");
    changeSet.addChange(new AddKeyChange("users", key, 1));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts = new MigrationGeneratorOptions(changeSet, pw, DatabaseType.H2);
    H2MigrationGenerator gen = new H2MigrationGenerator(opts);
    gen.generate();

    String sql = sw.toString();
    assertAll(
        () -> assertTrue(sql.contains("CREATE UNIQUE INDEX IF NOT EXISTS"), "got: " + sql),
        () -> assertTrue(!sql.contains("WHERE"), "got: " + sql));
  }

  @Test
  @DisplayName("guards DROP PRIMARY KEY with IF EXISTS")
  void generatesDropPrimaryKeyIsGuarded() {
    ChangeSet changeSet = new ChangeSet();
    List<KeyColumn> cols = new ArrayList<>();
    cols.add(new KeyColumn("id"));
    Key key = new Key(KeyType.PRIMARY, cols);
    changeSet.addChange(new DropKeyChange("orders", key, 1));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts = new MigrationGeneratorOptions(changeSet, pw, DatabaseType.H2);
    H2MigrationGenerator gen = new H2MigrationGenerator(opts);
    gen.generate();

    String sql = sw.toString();
    assertTrue(sql.contains("DROP CONSTRAINT IF EXISTS pk_orders"), "got: " + sql);
  }

  @Test
  @DisplayName("guards ADD CONSTRAINT with IF NOT EXISTS")
  void generatesAddConstraintIsGuarded() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(
        new AddConstraintChange(
            "orders", new Constraint("ck_orders_total", "total >= 0", DatabaseType.H2)));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts = new MigrationGeneratorOptions(changeSet, pw, DatabaseType.H2);
    H2MigrationGenerator gen = new H2MigrationGenerator(opts);
    gen.generate();

    String sql = sw.toString();
    assertTrue(
        sql.contains("ADD CONSTRAINT IF NOT EXISTS ck_orders_total CHECK (total >= 0)"),
        "got: " + sql);
  }

  @Test
  @DisplayName("guards DROP CONSTRAINT with IF EXISTS")
  void generatesDropConstraintIsGuarded() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(new DropConstraintChange("orders", "ck_orders_total"));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts = new MigrationGeneratorOptions(changeSet, pw, DatabaseType.H2);
    H2MigrationGenerator gen = new H2MigrationGenerator(opts);
    gen.generate();

    String sql = sw.toString();
    assertTrue(sql.contains("DROP CONSTRAINT IF EXISTS ck_orders_total"), "got: " + sql);
  }

  @Test
  @DisplayName("guards ADD RELATION with IF NOT EXISTS")
  void generatesAddRelationIsGuarded() {
    ChangeSet changeSet = new ChangeSet();
    Relation rel =
        new Relation("orders", "customer_id", "customers", "id", RelationType.CASCADE, false);
    changeSet.addChange(new AddRelationChange(rel, 1));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts = new MigrationGeneratorOptions(changeSet, pw, DatabaseType.H2);
    H2MigrationGenerator gen = new H2MigrationGenerator(opts);
    gen.generate();

    String sql = sw.toString();
    assertAll(
        () -> assertTrue(sql.contains("ADD CONSTRAINT IF NOT EXISTS fk_orders1"), "got: " + sql),
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
    MigrationGeneratorOptions opts = new MigrationGeneratorOptions(changeSet, pw, DatabaseType.H2);
    H2MigrationGenerator gen = new H2MigrationGenerator(opts);
    gen.generate();

    String sql = sw.toString();
    assertAll(
        () ->
            assertTrue(
                sql.contains("FOREIGN KEY (parent_assignment_id, property_id)"), "got: " + sql),
        () -> assertTrue(sql.contains("REFERENCES properties(id, property_id)"), "got: " + sql));
  }

  @Test
  @DisplayName("guards DROP RELATION with IF EXISTS")
  void generatesDropRelationIsGuarded() {
    ChangeSet changeSet = new ChangeSet();
    Relation rel =
        new Relation("orders", "customer_id", "customers", "id", RelationType.CASCADE, false);
    changeSet.addChange(new DropRelationChange(rel, 1));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts = new MigrationGeneratorOptions(changeSet, pw, DatabaseType.H2);
    H2MigrationGenerator gen = new H2MigrationGenerator(opts);
    gen.generate();

    String sql = sw.toString();
    assertTrue(sql.contains("DROP CONSTRAINT IF EXISTS fk_orders1"), "got: " + sql);
  }

  @Test
  @DisplayName("guards DROP COLUMN with IF EXISTS")
  void generatesDropColumnIsGuarded() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addChange(new DropColumnChange("users", "legacy", List.of()));

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    MigrationGeneratorOptions opts = new MigrationGeneratorOptions(changeSet, pw, DatabaseType.H2);
    H2MigrationGenerator gen = new H2MigrationGenerator(opts);
    gen.generate();

    String sql = sw.toString();
    assertTrue(sql.contains("DROP COLUMN IF EXISTS legacy"), "got: " + sql);
  }
}
