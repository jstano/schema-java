package com.stano.schema.diff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stano.schema.diff.change.AddEnumTypeChange;
import com.stano.schema.diff.change.AddInitialDataChange;
import com.stano.schema.diff.change.AddOtherSqlChange;
import com.stano.schema.diff.change.AddTriggerChange;
import com.stano.schema.diff.change.DropEnumTypeChange;
import com.stano.schema.diff.change.DropInitialDataChange;
import com.stano.schema.diff.change.DropOtherSqlChange;
import com.stano.schema.diff.change.DropTableChange;
import com.stano.schema.diff.change.DropTriggerChange;
import com.stano.schema.diff.change.ModifyEnumTypeChange;
import com.stano.schema.model.DatabaseType;
import com.stano.schema.model.EnumType;
import com.stano.schema.model.EnumValue;
import com.stano.schema.model.InitialData;
import com.stano.schema.model.OtherSql;
import com.stano.schema.model.OtherSqlOrder;
import com.stano.schema.model.Schema;
import com.stano.schema.model.Table;
import com.stano.schema.model.Trigger;
import com.stano.schema.model.TriggerType;
import java.net.URL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SchemaDiffEngine: enums, triggers, other_sql, initial data")
class SchemaDiffEngineGroupCTest {

  private static final URL TEST_URL = SchemaDiffEngineGroupCTest.class.getResource("/");

  private final SchemaDiffEngine engine = new SchemaDiffEngine();

  private static EnumType enumType(String name, String... codes) {
    EnumType type = new EnumType(name);
    for (String code : codes) {
      type.addValue(new EnumValue(code.toUpperCase(), code));
    }
    return type;
  }

  private static Table table(Schema schema, String name) {
    Table table = new Table(schema, null, name, null, null, false);
    schema.addTable(table);
    return table;
  }

  @Test
  void addedEnumTypeIsReported() {
    Schema oldSchema = new Schema(TEST_URL);
    Schema newSchema = new Schema(TEST_URL);
    newSchema.addEnumType(enumType("Gender", "m", "f"));

    var changes = engine.diff(oldSchema, newSchema).getChanges();

    assertEquals(1, changes.size());
    assertEquals("Gender", ((AddEnumTypeChange) changes.get(0)).getEnumType().getName());
  }

  @Test
  void droppedEnumTypeIsReportedAfterTableDrops() {
    Schema oldSchema = new Schema(TEST_URL);
    oldSchema.addEnumType(enumType("Gender", "m", "f"));
    table(oldSchema, "people");
    Schema newSchema = new Schema(TEST_URL);

    var changes = engine.diff(oldSchema, newSchema).getChanges();

    assertEquals(2, changes.size());
    assertTrue(changes.get(0) instanceof DropTableChange);
    assertEquals("Gender", ((DropEnumTypeChange) changes.get(1)).getEnumTypeName());
  }

  @Test
  void enumTypeWithAnAddedValueIsReportedAsModified() {
    Schema oldSchema = new Schema(TEST_URL);
    oldSchema.addEnumType(enumType("Gender", "m", "f"));
    Schema newSchema = new Schema(TEST_URL);
    newSchema.addEnumType(enumType("gender", "m", "f", "x"));

    var changes = engine.diff(oldSchema, newSchema).getChanges();

    assertEquals(1, changes.size());
    var modify = (ModifyEnumTypeChange) changes.get(0);
    assertEquals(2, modify.getOldEnumType().getValues().size());
    assertEquals(3, modify.getNewEnumType().getValues().size());
  }

  @Test
  void identicalEnumTypesProduceNoChange() {
    Schema oldSchema = new Schema(TEST_URL);
    oldSchema.addEnumType(enumType("Gender", "m", "f"));
    Schema newSchema = new Schema(TEST_URL);
    newSchema.addEnumType(enumType("Gender", "m", "f"));

    assertTrue(engine.diff(oldSchema, newSchema).isEmpty());
  }

  @Test
  void otherSqlIsAddedDroppedAndTrimInsensitive() {
    Schema oldSchema = new Schema(TEST_URL);
    oldSchema.addOtherSql(new OtherSql(DatabaseType.POSTGRESQL, OtherSqlOrder.TOP, "A"));
    oldSchema.addOtherSql(new OtherSql(DatabaseType.POSTGRESQL, OtherSqlOrder.TOP, "same"));
    Schema newSchema = new Schema(TEST_URL);
    newSchema.addOtherSql(new OtherSql(DatabaseType.POSTGRESQL, OtherSqlOrder.TOP, " same \n"));
    newSchema.addOtherSql(new OtherSql(DatabaseType.POSTGRESQL, OtherSqlOrder.BOTTOM, "B"));

    var changes = engine.diff(oldSchema, newSchema).getChanges();

    assertEquals(2, changes.size());
    assertEquals("A", ((DropOtherSqlChange) changes.get(0)).getOtherSql().getSql());
    assertEquals("B", ((AddOtherSqlChange) changes.get(1)).getOtherSql().getSql());
  }

  @Test
  void triggerAddedToExistingTableIsReported() {
    Schema oldSchema = new Schema(TEST_URL);
    table(oldSchema, "people");
    Schema newSchema = new Schema(TEST_URL);
    table(newSchema, "people")
        .getTriggers()
        .add(new Trigger("x := 1;", TriggerType.UPDATE, DatabaseType.POSTGRESQL));

    var changes = engine.diff(oldSchema, newSchema).getChanges();

    assertEquals(1, changes.size());
    assertEquals("people", ((AddTriggerChange) changes.get(0)).getTableName());
  }

  @Test
  void triggerOnBrandNewTableIsStillReported() {
    Schema oldSchema = new Schema(TEST_URL);
    Schema newSchema = new Schema(TEST_URL);
    table(newSchema, "people")
        .getTriggers()
        .add(new Trigger("x := 1;", TriggerType.UPDATE, DatabaseType.POSTGRESQL));

    var changes = engine.diff(oldSchema, newSchema).getChanges();

    assertEquals(2, changes.size());
    assertTrue(changes.get(1) instanceof AddTriggerChange);
  }

  @Test
  void changedTriggerIsReportedAsDropAndAdd() {
    Schema oldSchema = new Schema(TEST_URL);
    table(oldSchema, "people")
        .getTriggers()
        .add(new Trigger("x := 1;", TriggerType.UPDATE, DatabaseType.POSTGRESQL));
    Schema newSchema = new Schema(TEST_URL);
    table(newSchema, "people")
        .getTriggers()
        .add(new Trigger("x := 2;", TriggerType.UPDATE, DatabaseType.POSTGRESQL));

    var changes = engine.diff(oldSchema, newSchema).getChanges();

    assertEquals(2, changes.size());
    assertTrue(changes.get(0) instanceof DropTriggerChange);
    assertTrue(changes.get(1) instanceof AddTriggerChange);
  }

  @Test
  void initialDataAddedAndDroppedOnExistingTable() {
    Schema oldSchema = new Schema(TEST_URL);
    table(oldSchema, "people").getInitialData().add(new InitialData("insert 1", null));
    Schema newSchema = new Schema(TEST_URL);
    table(newSchema, "people").getInitialData().add(new InitialData("insert 2", null));

    var changes = engine.diff(oldSchema, newSchema).getChanges();

    assertEquals(2, changes.size());
    assertEquals("insert 1", ((DropInitialDataChange) changes.get(0)).getInitialData().getSql());
    assertEquals("insert 2", ((AddInitialDataChange) changes.get(1)).getInitialData().getSql());
  }

  @Test
  void initialDataOnBrandNewTableIsReportedLast() {
    Schema oldSchema = new Schema(TEST_URL);
    Schema newSchema = new Schema(TEST_URL);
    table(newSchema, "people").getInitialData().add(new InitialData("insert 1", null));

    var changes = engine.diff(oldSchema, newSchema).getChanges();

    assertEquals(2, changes.size());
    assertTrue(changes.get(1) instanceof AddInitialDataChange);
  }
}
