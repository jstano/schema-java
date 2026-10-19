package com.stano.schema.genmigration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stano.schema.diff.ChangeSet;
import com.stano.schema.diff.SchemaChange;
import com.stano.schema.diff.change.AddEnumTypeChange;
import com.stano.schema.diff.change.AddInitialDataChange;
import com.stano.schema.diff.change.AddOtherSqlChange;
import com.stano.schema.diff.change.AddTriggerChange;
import com.stano.schema.diff.change.DropEnumTypeChange;
import com.stano.schema.diff.change.DropInitialDataChange;
import com.stano.schema.diff.change.DropOtherSqlChange;
import com.stano.schema.diff.change.DropTriggerChange;
import com.stano.schema.diff.change.ModifyEnumTypeChange;
import com.stano.schema.genmigration.impl.common.MigrationGeneratorFactory;
import com.stano.schema.genmigration.impl.common.MigrationGeneratorOptions;
import com.stano.schema.model.Column;
import com.stano.schema.model.ColumnType;
import com.stano.schema.model.DatabaseType;
import com.stano.schema.model.EnumType;
import com.stano.schema.model.EnumValue;
import com.stano.schema.model.InitialData;
import com.stano.schema.model.LockEscalation;
import com.stano.schema.model.OtherSql;
import com.stano.schema.model.OtherSqlOrder;
import com.stano.schema.model.Schema;
import com.stano.schema.model.Table;
import com.stano.schema.model.Trigger;
import com.stano.schema.model.TriggerType;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("migration generator: enums, triggers, other_sql, initial data (group C)")
class GroupCMigrationTest {

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

  private static EnumType enumType(String name, String... codes) {
    EnumType type = new EnumType(name);
    for (String code : codes) {
      type.addValue(new EnumValue(code.toUpperCase(), code));
    }
    return type;
  }

  private static Schema schemaWithEnumColumn(EnumType type) {
    Schema schema = new Schema(GroupCMigrationTest.class.getResource("/"));
    schema.addEnumType(type);
    Table table = new Table(schema, "dbo", "people", null, LockEscalation.AUTO, false);
    table
        .getColumns()
        .add(
            new Column(
                "gender",
                ColumnType.ENUM,
                0,
                0,
                true,
                null,
                null,
                null,
                null,
                null,
                type.getName(),
                null));
    schema.addTable(table);
    return schema;
  }

  // ---- enums -----------------------------------------------------------------------------

  @Test
  @DisplayName("Postgres: add enum type is guarded, drop is cascade")
  void postgresAddAndDropEnum() {
    String add =
        run(
            DatabaseType.POSTGRESQL,
            null,
            new AddEnumTypeChange(enumType("OrderStatus", "o", "it's")));
    assertTrue(add.contains("pg_type WHERE typname = 'order_status'"), add);
    assertTrue(add.contains("CREATE TYPE order_status AS ENUM ('o','it''s')"), add);

    String drop = run(DatabaseType.POSTGRESQL, null, new DropEnumTypeChange("OrderStatus"));
    assertTrue(drop.contains("DROP TYPE IF EXISTS order_status"), drop);
    assertFalse(drop.contains("CASCADE"), drop);
  }

  @Test
  @DisplayName("Postgres: modified enum adds new values and halts on removed ones")
  void postgresModifyEnum() {
    var change =
        new ModifyEnumTypeChange(enumType("Gender", "m", "f"), enumType("Gender", "m", "x"));

    String sql = run(DatabaseType.POSTGRESQL, null, change);

    assertTrue(sql.contains("ALTER TYPE gender ADD VALUE IF NOT EXISTS 'x'"), sql);
    assertFalse(sql.contains("ADD VALUE IF NOT EXISTS 'm'"), sql);
    assertTrue(sql.contains("-- WARNING: enum value 'f' was removed"), sql);
    assertTrue(
        sql.contains("RAISE EXCEPTION 'Migration halted: enum value ''f'' was removed"), sql);
  }

  @Test
  @DisplayName("Postgres: a code change is an add plus a removal, not silently ignored")
  void postgresCodeChangeIsDetected() {
    EnumType oldType = new EnumType("Gender");
    oldType.addValue(new EnumValue("MALE", "M"));
    EnumType newType = new EnumType("Gender");
    newType.addValue(new EnumValue("MALE", "m"));

    String sql = run(DatabaseType.POSTGRESQL, null, new ModifyEnumTypeChange(oldType, newType));

    assertTrue(sql.contains("ADD VALUE IF NOT EXISTS 'm'"), sql);
    assertTrue(sql.contains("RAISE EXCEPTION"), sql);
  }

  @Test
  @DisplayName("SQL Server: modified enum regenerates the check constraint on every column")
  void sqlServerModifyEnum() {
    EnumType newType = enumType("Gender", "m", "f", "x");
    Schema schema = schemaWithEnumColumn(newType);
    var change = new ModifyEnumTypeChange(enumType("Gender", "m", "f"), newType);

    String sql = run(DatabaseType.SQL_SERVER, schema, change);

    int drop = sql.indexOf("DROP CONSTRAINT");
    int add = sql.indexOf("ADD CONSTRAINT");
    assertTrue(drop >= 0 && drop < add, sql);
    assertTrue(sql.contains("'m','f','x'"), sql);
    assertFalse(sql.contains("THROW"), sql);
    assertTrue(
        sql.contains("IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('people')"),
        "only columns that already exist are cycled: " + sql);
  }

  @Test
  @DisplayName("SQL Server: a removed enum value halts before the constraints are regenerated")
  void sqlServerRemovedEnumValueHalts() {
    EnumType newType = enumType("Gender", "m");
    Schema schema = schemaWithEnumColumn(newType);
    var change = new ModifyEnumTypeChange(enumType("Gender", "m", "f"), newType);

    String sql = run(DatabaseType.SQL_SERVER, schema, change);

    assertTrue(sql.indexOf("THROW 50000") >= 0, sql);
    assertTrue(sql.indexOf("THROW 50000") < sql.indexOf("DROP CONSTRAINT"), sql);
  }

  @Test
  @DisplayName("SQL Server and H2 emit nothing for add/drop enum type")
  void noNativeEnumTypeOutsidePostgres() {
    var add = new AddEnumTypeChange(enumType("Gender", "m"));
    var drop = new DropEnumTypeChange("Gender");

    assertEquals("", run(DatabaseType.SQL_SERVER, null, add, drop));
    assertEquals("", run(DatabaseType.H2, null, add, drop));
  }

  @Test
  @DisplayName("H2: modified enum lists the columns to alter by hand")
  void h2ModifyEnumListsColumns() {
    EnumType newType = enumType("Gender", "m", "f", "x");
    Schema schema = schemaWithEnumColumn(newType);

    String sql =
        run(
            DatabaseType.H2,
            schema,
            new ModifyEnumTypeChange(enumType("Gender", "m", "f"), newType));

    assertTrue(sql.contains("-- TODO: enum type 'Gender' changed"), sql);
    assertTrue(sql.contains("--   people.gender"), sql);
  }

  // ---- other_sql / initial data --------------------------------------------------------------

  @Test
  @DisplayName("other_sql: added entry passes through for its own dialect only")
  void otherSqlAdd() {
    var change =
        new AddOtherSqlChange(
            new OtherSql(DatabaseType.POSTGRESQL, OtherSqlOrder.TOP, "create extension foo"));

    assertTrue(run(DatabaseType.POSTGRESQL, null, change).contains("create extension foo"));
    assertEquals("", run(DatabaseType.SQL_SERVER, null, change));
    assertEquals("", run(DatabaseType.H2, null, change));
  }

  @Test
  @DisplayName("other_sql: removed entry becomes a fully commented review note")
  void otherSqlDropIsCommented() {
    var change =
        new DropOtherSqlChange(
            new OtherSql(DatabaseType.POSTGRESQL, OtherSqlOrder.TOP, "line one;\nline two;"));

    String sql = run(DatabaseType.POSTGRESQL, null, change);

    assertTrue(sql.contains("-- TODO: other_sql entry removed, review manually:"), sql);
    assertTrue(sql.contains("--   line one;\n--   line two;"), sql.replace("\r", ""));
    assertEquals("", run(DatabaseType.SQL_SERVER, null, change));
  }

  @Test
  @DisplayName("initial data: dialect-neutral entry applies to every dialect")
  void initialDataApplies() {
    var neutral = new AddInitialDataChange("people", new InitialData("insert into people", null));
    var sqlServerOnly =
        new AddInitialDataChange("people", new InitialData("insert ss", DatabaseType.SQL_SERVER));

    for (DatabaseType type : DatabaseType.values()) {
      assertTrue(run(type, null, neutral).contains("insert into people"), type.name());
    }
    assertTrue(run(DatabaseType.SQL_SERVER, null, sqlServerOnly).contains("insert ss"));
    assertEquals("", run(DatabaseType.POSTGRESQL, null, sqlServerOnly));
  }

  @Test
  @DisplayName("initial data: removed entry becomes a review comment")
  void initialDataDropIsCommented() {
    var change = new DropInitialDataChange("people", new InitialData("insert old", null));

    String sql = run(DatabaseType.POSTGRESQL, null, change);

    assertTrue(sql.contains("-- TODO: initial_data entry removed, review manually:"), sql);
    assertTrue(sql.contains("--   insert old"), sql);
  }

  // ---- triggers --------------------------------------------------------------------------

  @Test
  @DisplayName("triggers: a table's trigger set is regenerated once, however many changed")
  void triggersRegeneratedOncePerTable() {
    Schema schema = new Schema(GroupCMigrationTest.class.getResource("/"));
    Table table = new Table(schema, "public", "people", null, LockEscalation.AUTO, false);
    Trigger trigger = new Trigger("x := 1;", TriggerType.UPDATE, DatabaseType.POSTGRESQL);
    table.getTriggers().add(trigger);
    schema.addTable(table);

    String sql =
        run(
            DatabaseType.POSTGRESQL,
            schema,
            new DropTriggerChange("people", trigger),
            new AddTriggerChange("people", trigger));

    assertTrue(sql.contains("people_update"), sql);
    assertTrue(sql.contains("x := 1;"), sql);
    assertEquals(
        1, sql.split("create or replace function public.people_update", -1).length - 1, sql);
  }

  @Test
  @DisplayName("triggers: a trigger for another dialect is ignored")
  void triggerForOtherDialectIgnored() {
    Schema schema = new Schema(GroupCMigrationTest.class.getResource("/"));
    Table table = new Table(schema, "public", "people", null, LockEscalation.AUTO, false);
    Trigger trigger = new Trigger("x := 1;", TriggerType.UPDATE, DatabaseType.SQL_SERVER);
    table.getTriggers().add(trigger);
    schema.addTable(table);

    assertEquals("", run(DatabaseType.POSTGRESQL, schema, new AddTriggerChange("people", trigger)));
  }

  @Test
  @DisplayName("triggers: removing a table's last trigger drops it")
  void removedLastTriggerIsDropped() {
    Schema schema = new Schema(GroupCMigrationTest.class.getResource("/"));
    Table table = new Table(schema, "public", "people", null, LockEscalation.AUTO, false);
    schema.addTable(table);
    Trigger removed = new Trigger("x := 1;", TriggerType.UPDATE, DatabaseType.POSTGRESQL);

    String pg = run(DatabaseType.POSTGRESQL, schema, new DropTriggerChange("people", removed));

    assertTrue(pg.contains("drop trigger if exists people_update on public.people"), pg);
    assertTrue(pg.contains("drop function if exists public.people_update()"), pg);
    assertTrue(pg.contains("drop trigger if exists people_delete on public.people"), pg);

    Trigger ssRemoved = new Trigger("x", TriggerType.UPDATE, DatabaseType.SQL_SERVER);
    String ss = run(DatabaseType.SQL_SERVER, schema, new DropTriggerChange("people", ssRemoved));

    assertTrue(ss.contains("drop trigger dbo.people_update"), ss);
  }

  @Test
  @DisplayName("triggers: a trigger that is still needed is regenerated, not dropped")
  void stillNeededTriggerIsNotDropped() {
    Schema schema = new Schema(GroupCMigrationTest.class.getResource("/"));
    Table table = new Table(schema, "public", "people", null, LockEscalation.AUTO, false);
    Trigger kept = new Trigger("x := 1;", TriggerType.UPDATE, DatabaseType.POSTGRESQL);
    table.getTriggers().add(kept);
    schema.addTable(table);

    String sql = run(DatabaseType.POSTGRESQL, schema, new DropTriggerChange("people", kept));

    assertTrue(sql.contains("create or replace function public.people_update"), sql);
    assertTrue(!sql.contains("drop function if exists public.people_update"), sql);
  }
}
