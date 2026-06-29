package com.stano.schema.diff;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stano.schema.diff.change.AddColumnChange;
import com.stano.schema.diff.change.AddKeyChange;
import com.stano.schema.diff.change.AddViewChange;
import com.stano.schema.diff.change.DropColumnChange;
import com.stano.schema.diff.change.ModifyColumnChange;
import com.stano.schema.model.Column;
import com.stano.schema.model.ColumnType;
import com.stano.schema.model.Schema;
import com.stano.schema.model.Table;
import com.stano.schema.parser.SchemaParser;
import java.net.URL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SchemaDiffIntegration")
class SchemaDiffIntegrationTest {

  private static final URL TEST_URL = SchemaDiffIntegrationTest.class.getResource("/");

  @Test
  @DisplayName("detects expected changes between old and new schema")
  void detectsExpectedChanges() throws Exception {
    URL oldSchemaUrl = getClass().getResource("/old-schema.xml");
    URL newSchemaUrl = getClass().getResource("/new-schema.xml");
    assertNotNull(oldSchemaUrl);
    assertNotNull(newSchemaUrl);

    SchemaParser parser = new SchemaParser();
    Schema oldSchema = parser.parseSchema(oldSchemaUrl);
    Schema newSchema = parser.parseSchema(newSchemaUrl);

    SchemaDiffEngine engine = new SchemaDiffEngine();
    ChangeSet changeSet = engine.diff(oldSchema, newSchema);

    assertFalse(changeSet.isEmpty(), "Changeset should not be empty");

    boolean hasDropColumn = false;
    boolean hasModifyColumn = false;
    boolean hasAddColumn = false;
    boolean hasAddKey = false;
    boolean hasAddView = false;

    for (SchemaChange change : changeSet.getChanges()) {
      if (change instanceof DropColumnChange) hasDropColumn = true;
      if (change instanceof ModifyColumnChange) hasModifyColumn = true;
      if (change instanceof AddColumnChange) hasAddColumn = true;
      if (change instanceof AddKeyChange) hasAddKey = true;
      if (change instanceof AddViewChange) hasAddView = true;
    }

    assertTrue(
        hasAddColumn || hasDropColumn || hasModifyColumn || hasAddKey || hasAddView,
        "Should have at least one type of change");
  }

  @Test
  @DisplayName(
      "flags rename candidates when column dropped and same-type column added to same table")
  void flagsRenameCandidates() {
    Schema oldSchema = new Schema(TEST_URL);
    Table oldTable = new Table(oldSchema, null, "users", null, null, false);
    oldTable.getColumns().add(new Column("first_name", ColumnType.VARCHAR, 255, false));
    oldSchema.addTable(oldTable);

    Schema newSchema = new Schema(TEST_URL);
    Table newTable = new Table(newSchema, null, "users", null, null, false);
    newTable.getColumns().add(new Column("full_name", ColumnType.VARCHAR, 255, false));
    newSchema.addTable(newTable);

    SchemaDiffEngine engine = new SchemaDiffEngine();
    ChangeSet changeSet = engine.diff(oldSchema, newSchema);

    assertFalse(changeSet.isEmpty());
    DropColumnChange drop =
        changeSet.getChanges().stream()
            .filter(c -> c instanceof DropColumnChange)
            .map(c -> (DropColumnChange) c)
            .findFirst()
            .orElse(null);
    assertNotNull(drop, "Should have a DropColumnChange");
    assertFalse(drop.getRenameCandidates().isEmpty(), "Should flag full_name as rename candidate");
    assertTrue(drop.getRenameCandidates().contains("full_name"));
  }

  @Test
  @DisplayName("does not flag rename candidates when types differ")
  void doesNotFlagRenameCandidatesWhenTypesDiffer() {
    Schema oldSchema = new Schema(TEST_URL);
    Table oldTable = new Table(oldSchema, null, "users", null, null, false);
    oldTable.getColumns().add(new Column("code", ColumnType.INT, 0, false));
    oldSchema.addTable(oldTable);

    Schema newSchema = new Schema(TEST_URL);
    Table newTable = new Table(newSchema, null, "users", null, null, false);
    newTable.getColumns().add(new Column("label", ColumnType.VARCHAR, 100, false));
    newSchema.addTable(newTable);

    SchemaDiffEngine engine = new SchemaDiffEngine();
    ChangeSet changeSet = engine.diff(oldSchema, newSchema);

    DropColumnChange drop =
        changeSet.getChanges().stream()
            .filter(c -> c instanceof DropColumnChange)
            .map(c -> (DropColumnChange) c)
            .findFirst()
            .orElse(null);
    assertNotNull(drop);
    assertTrue(
        drop.getRenameCandidates().isEmpty(), "Should not flag candidates when types differ");
  }
}
