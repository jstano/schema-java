package com.stano.schema.genmigration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stano.schema.diff.ChangeSet;
import com.stano.schema.diff.SchemaChange;
import com.stano.schema.diff.change.AddColumnChange;
import com.stano.schema.diff.change.AddConstraintChange;
import com.stano.schema.diff.change.AddRelationChange;
import com.stano.schema.diff.change.DropColumnChange;
import com.stano.schema.diff.change.DropRelationChange;
import com.stano.schema.diff.change.ModifyColumnChange;
import com.stano.schema.genmigration.impl.common.MigrationGeneratorFactory;
import com.stano.schema.genmigration.impl.common.MigrationGeneratorOptions;
import com.stano.schema.model.Column;
import com.stano.schema.model.ColumnType;
import com.stano.schema.model.Constraint;
import com.stano.schema.model.DatabaseType;
import com.stano.schema.model.Key;
import com.stano.schema.model.KeyColumn;
import com.stano.schema.model.KeyType;
import com.stano.schema.model.LockEscalation;
import com.stano.schema.model.Relation;
import com.stano.schema.model.RelationType;
import com.stano.schema.model.Schema;
import com.stano.schema.model.Table;
import com.stano.schema.model.View;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("migration generator fixes ported from schema-rs (group B)")
class GroupBMigrationTest {

  private static String run(DatabaseType type, Schema schema, SchemaChange... changes) {
    ChangeSet changeSet = new ChangeSet();
    for (SchemaChange change : changes) {
      changeSet.addChange(change);
    }
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    new MigrationGeneratorFactory()
        .createGenerator(new MigrationGeneratorOptions(changeSet, pw, type, schema))
        .generate();
    pw.flush();
    return sw.toString();
  }

  private static Column column(
      String name, ColumnType type, boolean required, String defaultValue, String generated) {
    return new Column(
        name, type, 0, 0, required, null, defaultValue, generated, null, null, null, null);
  }

  private static Column intColumn(String name, boolean required) {
    return column(name, ColumnType.INT, required, null, null);
  }

  private static Table table(Schema schema, String name, Column... columns) {
    Table table = new Table(schema, "dbo", name, null, LockEscalation.AUTO, false);
    table.getColumns().addAll(List.of(columns));
    schema.addTable(table);
    return table;
  }

  // ---- M25: NOT NULL add-column guard ------------------------------------------------------

  @Test
  @DisplayName("M25: a required column with no default is added nullable, halts, then tightened")
  void requiredColumnWithoutDefaultHalts() {
    var change = new AddColumnChange("users", intColumn("age", true));

    String pg = run(DatabaseType.POSTGRESQL, null, change);
    int add = pg.indexOf("ADD COLUMN IF NOT EXISTS age");
    int halt = pg.indexOf("RAISE EXCEPTION");
    int tighten = pg.indexOf("ALTER COLUMN age SET NOT NULL");
    assertTrue(add >= 0 && add < halt && halt < tighten, pg);
    assertFalse(pg.substring(add, pg.indexOf("--", add)).contains("NOT NULL"), pg);
    assertTrue(pg.contains("-- WARNING"), pg);

    String ss = run(DatabaseType.SQL_SERVER, null, change);
    int ssAdd = ss.indexOf("ADD age");
    int ssHalt = ss.indexOf("THROW 50000");
    int ssTighten = ss.indexOf("ALTER COLUMN age");
    assertTrue(ssAdd >= 0 && ssAdd < ssHalt && ssHalt < ssTighten, ss);
    assertTrue(ss.substring(ssAdd, ssHalt).contains(" NULL"), ss);
    assertTrue(ss.substring(ssTighten).contains("NOT NULL"), ss);
  }

  @Test
  @DisplayName("M25: a required column that has a default adds in one step with no halt")
  void requiredColumnWithDefaultDoesNotHalt() {
    var change = new AddColumnChange("users", column("age", ColumnType.INT, true, "0", null));

    assertFalse(run(DatabaseType.POSTGRESQL, null, change).contains("RAISE EXCEPTION"));
    String ss = run(DatabaseType.SQL_SERVER, null, change);
    assertFalse(ss.contains("THROW"), ss);
    assertTrue(ss.contains("NOT NULL CONSTRAINT df_users_age_"), ss);
    assertTrue(ss.contains("DEFAULT 0"), ss);
  }

  // ---- M2 migration half: computed columns ---------------------------------------------------

  @Test
  @DisplayName("M2: a generated column is added as a computed column, with no default or halt")
  void generatedColumnIsAddedComputed() {
    var change =
        new AddColumnChange("items", column("total", ColumnType.INT, true, null, "price * qty"));

    String pg = run(DatabaseType.POSTGRESQL, null, change);
    assertTrue(pg.contains("total integer GENERATED ALWAYS AS (price * qty) STORED NOT NULL"), pg);
    assertFalse(pg.contains("RAISE EXCEPTION"), pg);

    String ss = run(DatabaseType.SQL_SERVER, null, change);
    assertTrue(ss.contains("ADD total AS (price * qty) PERSISTED NOT NULL"), ss);
    assertFalse(ss.contains("THROW"), ss);

    String h2 = run(DatabaseType.H2, null, change);
    assertTrue(h2.contains("GENERATED ALWAYS AS (price * qty)"), h2);
    assertFalse(h2.contains("STORED"), h2);
  }

  @Test
  @DisplayName("M2: changing a column's generated expression halts instead of guessing")
  void generatedChangeHalts() {
    var change =
        new ModifyColumnChange(
            "items",
            column("total", ColumnType.INT, false, null, "price * qty"),
            column("total", ColumnType.INT, false, null, "price * qty * 2"));

    assertTrue(run(DatabaseType.POSTGRESQL, null, change).contains("RAISE EXCEPTION"));
    assertTrue(run(DatabaseType.SQL_SERVER, null, change).contains("THROW 50000"));
  }

  // ---- M26: no double CHECK -----------------------------------------------------------------

  @Test
  @DisplayName("M26: AddConstraint writes the constraint body as-is, without re-wrapping in CHECK")
  void addConstraintIsNotDoubleWrapped() {
    var change =
        new AddConstraintChange("orders", new Constraint("ck_amount", "check (amount >= 0)", null));

    for (DatabaseType type :
        List.of(DatabaseType.POSTGRESQL, DatabaseType.SQL_SERVER, DatabaseType.H2)) {
      String sql = run(type, null, change);
      assertTrue(sql.contains("check (amount >= 0)"), sql);
      assertFalse(sql.toLowerCase().contains("check (check"), type + ": " + sql);
    }
  }

  // ---- M24: ALTER COLUMN TYPE ---------------------------------------------------------------

  @Test
  @DisplayName("M24: a Postgres type change uses USING")
  void postgresTypeChangeUsesUsing() {
    String sql =
        run(
            DatabaseType.POSTGRESQL,
            null,
            new ModifyColumnChange(
                "t", column("n", ColumnType.VARCHAR, false, null, null), intColumn("n", false)));

    assertTrue(sql.contains("ALTER COLUMN n TYPE integer USING n::integer"), sql);
  }

  @Test
  @DisplayName("M24: a sequence column is altered to its underlying type, with a note")
  void postgresSequenceTypeChange() {
    String sql =
        run(
            DatabaseType.POSTGRESQL,
            null,
            new ModifyColumnChange(
                "t",
                intColumn("id", true),
                column("id", ColumnType.LONGSEQUENCE, true, null, null)));

    assertTrue(sql.contains("ALTER COLUMN id TYPE bigint USING id::bigint"), sql);
    assertFalse(sql.contains("TYPE bigserial"), sql);
    assertTrue(sql.contains("-- NOTE"), sql);
  }

  @Test
  @DisplayName("M24: an unchanged type emits no ALTER COLUMN TYPE")
  void postgresUnchangedTypeEmitsNoTypeChange() {
    String sql =
        run(
            DatabaseType.POSTGRESQL,
            null,
            new ModifyColumnChange("t", intColumn("n", false), intColumn("n", true)));

    assertFalse(sql.contains(" TYPE "), sql);
    assertTrue(sql.contains("SET NOT NULL"), sql);
  }

  // ---- M37: dependent views -----------------------------------------------------------------

  private static Schema schemaWithViews() {
    Schema schema = new Schema(null);
    table(schema, "orders", intColumn("id", true), intColumn("amount", false));
    schema.addView(
        new View(null, "v_amounts", "select id, amount from orders", DatabaseType.POSTGRESQL));
    schema.addView(new View(null, "v_top", "select * from v_amounts where amount > 10", null));
    schema.addView(new View(null, "v_unrelated", "select id from other_table", null));
    return schema;
  }

  @Test
  @DisplayName("M37: a type change drops and recreates dependent views, transitively, in order")
  void typeChangeCyclesDependentViews() {
    String sql =
        run(
            DatabaseType.POSTGRESQL,
            schemaWithViews(),
            new ModifyColumnChange(
                "orders",
                column("amount", ColumnType.INT, false, null, null),
                column("amount", ColumnType.LONG, false, null, null)));

    int dropTop = sql.indexOf("DROP VIEW IF EXISTS v_top");
    int dropAmounts = sql.indexOf("DROP VIEW IF EXISTS v_amounts");
    int alter = sql.indexOf("ALTER COLUMN amount TYPE");
    int createAmounts = sql.indexOf("CREATE OR REPLACE VIEW v_amounts");
    int createTop = sql.indexOf("CREATE OR REPLACE VIEW v_top");
    assertTrue(
        dropTop >= 0
            && dropTop < dropAmounts
            && dropAmounts < alter
            && alter < createAmounts
            && createAmounts < createTop,
        sql);
    assertFalse(sql.contains("v_unrelated"), sql);
  }

  @Test
  @DisplayName("a view is recreated once, after every column change in the run")
  void viewsAreRecreatedAfterTheWholeRun() {
    Schema schema = schemaWithViews();
    String sql =
        run(
            DatabaseType.POSTGRESQL,
            schema,
            new ModifyColumnChange(
                "orders",
                column("amount", ColumnType.INT, false, null, null),
                column("amount", ColumnType.LONG, false, null, null)),
            new ModifyColumnChange(
                "orders",
                column("id", ColumnType.INT, true, null, null),
                column("id", ColumnType.LONG, true, null, null)));

    int lastAlter = sql.lastIndexOf("ALTER COLUMN");
    int firstCreate = sql.indexOf("CREATE OR REPLACE VIEW v_amounts");
    assertTrue(lastAlter < firstCreate, sql);
    assertEquals(
        sql.indexOf("CREATE OR REPLACE VIEW v_amounts"),
        sql.lastIndexOf("CREATE OR REPLACE VIEW v_amounts"),
        sql);
  }

  @Test
  @DisplayName("M37: dropping a column drops and recreates the views that read it")
  void dropColumnCyclesDependentViews() {
    String sql =
        run(DatabaseType.POSTGRESQL, schemaWithViews(), new DropColumnChange("orders", "amount"));

    int drop = sql.indexOf("DROP VIEW IF EXISTS v_amounts");
    int dropColumn = sql.indexOf("DROP COLUMN IF EXISTS amount");
    int create = sql.indexOf("CREATE OR REPLACE VIEW v_amounts");
    assertTrue(drop >= 0 && drop < dropColumn && dropColumn < create, sql);
  }

  @Test
  @DisplayName("M37: with no schema, or no dependent view, no views are touched")
  void noViewsWithoutDependents() {
    var change =
        new ModifyColumnChange(
            "orders",
            column("note", ColumnType.INT, false, null, null),
            column("note", ColumnType.LONG, false, null, null));

    assertFalse(run(DatabaseType.POSTGRESQL, null, change).contains("VIEW"));
    assertFalse(run(DatabaseType.POSTGRESQL, schemaWithViews(), change).contains("v_amounts"));
  }

  // ---- M27: SQL Server ModifyColumn ----------------------------------------------------------

  @Test
  @DisplayName("M27: SQL Server ModifyColumn keeps the default, and a sequence stays a bare int")
  void sqlServerModifyKeepsDefaultAndStripsIdentity() {
    String sql =
        run(
            DatabaseType.SQL_SERVER,
            null,
            new ModifyColumnChange(
                "t",
                column("n", ColumnType.INT, true, "1", null),
                column("n", ColumnType.LONG, true, "2", null)));

    int dropDefault = sql.indexOf("sys.default_constraints");
    int alter = sql.indexOf("ALTER COLUMN n bigint NOT NULL");
    int addDefault = sql.indexOf("DEFAULT 2 FOR n");
    assertTrue(dropDefault >= 0 && dropDefault < alter && alter < addDefault, sql);

    String identity =
        run(
            DatabaseType.SQL_SERVER,
            null,
            new ModifyColumnChange(
                "t",
                column("id", ColumnType.SEQUENCE, true, null, null),
                column("id", ColumnType.SEQUENCE, false, null, null)));
    assertTrue(identity.contains("ALTER COLUMN id integer NULL"), identity);
    assertFalse(identity.toLowerCase().contains("identity("), identity);
    assertTrue(identity.contains("-- NOTE"), identity);
  }

  @Test
  @DisplayName("M27: SQL Server ModifyColumn cycles the keys and foreign keys around the column")
  void sqlServerModifyCyclesDependents() {
    Schema schema = new Schema(null);
    Table parent = table(schema, "parent", intColumn("id", true));
    parent.getKeys().add(new Key(KeyType.PRIMARY, List.of(new KeyColumn("id"))));
    Table child = table(schema, "child", intColumn("parent_id", true), intColumn("code", true));
    child.getKeys().add(new Key(KeyType.UNIQUE, List.of(new KeyColumn("code"))));
    child
        .getRelations()
        .add(new Relation("child", "parent_id", "parent", "id", RelationType.CASCADE, false));
    child
        .getRelations()
        .add(new Relation("child", "code", "parent", "id", RelationType.DONOTHING, false));

    // Altering parent.id: the child FK that references it (and parent's own PK) must come off
    // first and go back on after. The DONOTHING relation has no constraint, so is never touched.
    String sql =
        run(
            DatabaseType.SQL_SERVER,
            schema,
            new ModifyColumnChange(
                "parent",
                column("id", ColumnType.INT, true, null, null),
                column("id", ColumnType.LONG, true, null, null)));

    int dropFk = sql.indexOf("DROP CONSTRAINT fk_child1");
    int dropPk = sql.indexOf("DROP CONSTRAINT pk_parent");
    int alter = sql.indexOf("ALTER COLUMN id bigint NOT NULL");
    int addPk = sql.indexOf("ADD CONSTRAINT pk_parent");
    int addFk = sql.indexOf("ADD CONSTRAINT fk_child1");
    assertTrue(
        dropFk >= 0 && dropFk < dropPk && dropPk < alter && alter < addPk && addPk < addFk, sql);
    assertFalse(sql.contains("fk_child2"), sql);
    assertFalse(sql.contains("ak_child"), "unrelated unique key must be left alone: " + sql);
  }

  @Test
  @DisplayName("SQL Server: a foreign key goes back only after both of its columns are altered")
  void sqlServerForeignKeyReaddedAfterBothSidesChange() {
    Schema schema = new Schema(null);
    Table parent = table(schema, "parent", intColumn("id", true));
    parent.getKeys().add(new Key(KeyType.PRIMARY, List.of(new KeyColumn("id"))));
    Table child = table(schema, "child", intColumn("parent_id", true));
    child
        .getRelations()
        .add(new Relation("child", "parent_id", "parent", "id", RelationType.CASCADE, false));

    String sql =
        run(
            DatabaseType.SQL_SERVER,
            schema,
            new ModifyColumnChange(
                "parent",
                column("id", ColumnType.INT, true, null, null),
                column("id", ColumnType.LONG, true, null, null)),
            new ModifyColumnChange(
                "child",
                column("parent_id", ColumnType.INT, true, null, null),
                column("parent_id", ColumnType.LONG, true, null, null)));

    int alterParent = sql.indexOf("ALTER TABLE parent ALTER COLUMN id bigint");
    int alterChild = sql.indexOf("ALTER TABLE child ALTER COLUMN parent_id bigint");
    int addFk = sql.indexOf("ADD CONSTRAINT fk_child1");
    assertTrue(alterParent >= 0 && alterChild >= 0, sql);
    assertTrue(addFk > alterParent && addFk > alterChild, sql);
    assertTrue(sql.indexOf("ADD CONSTRAINT fk_child1", addFk + 1) < 0, "re-added once: " + sql);
  }

  @Test
  @DisplayName("M27: SQL Server DropColumn drops the column's default and check constraints first")
  void sqlServerDropColumnDropsConstraints() {
    String sql = run(DatabaseType.SQL_SERVER, null, new DropColumnChange("t", "n"));

    int dropDefault = sql.indexOf("DROP CONSTRAINT df_t_n_");
    int dropCheck = sql.indexOf("DROP CONSTRAINT ck_t_n_");
    int dropColumn = sql.indexOf("DROP COLUMN n");
    assertTrue(dropDefault >= 0 && dropDefault < dropColumn, sql);
    assertTrue(dropCheck >= 0 && dropCheck < dropColumn, sql);
  }

  // ---- M34 / L4: ON DELETE ------------------------------------------------------------------

  private static AddRelationChange add(RelationType type) {
    return new AddRelationChange(new Relation("child", "p", "parent", "id", type, false), 1);
  }

  @Test
  @DisplayName("M34: relations emit the ON DELETE action for their type, in every dialect")
  void onDeleteClauseFollowsType() {
    for (DatabaseType type :
        List.of(DatabaseType.POSTGRESQL, DatabaseType.SQL_SERVER, DatabaseType.H2)) {
      assertTrue(
          run(type, null, add(RelationType.CASCADE)).contains("ON DELETE CASCADE"), type.name());
      assertTrue(
          run(type, null, add(RelationType.SETNULL)).contains("ON DELETE SET NULL"), type.name());
      String enforce = run(type, null, add(RelationType.ENFORCE));
      assertTrue(enforce.contains("FOREIGN KEY"), type.name());
      assertFalse(enforce.contains("ON DELETE"), type.name());
    }
  }

  @Test
  @DisplayName("L4: DONOTHING relations get no foreign key to add or drop")
  void doNothingRelationsAreSkipped() {
    Relation relation = new Relation("child", "p", "parent", "id", RelationType.DONOTHING, false);
    for (DatabaseType type :
        List.of(DatabaseType.POSTGRESQL, DatabaseType.SQL_SERVER, DatabaseType.H2)) {
      String sql =
          run(type, null, new AddRelationChange(relation, 1), new DropRelationChange(relation, 1));
      assertEquals("", sql.strip(), type + ": " + sql);
    }
  }
}
