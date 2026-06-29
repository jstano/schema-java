package com.stano.schema.diff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stano.schema.diff.change.AddConstraintChange;
import com.stano.schema.diff.change.AddFunctionChange;
import com.stano.schema.diff.change.AddProcedureChange;
import com.stano.schema.diff.change.AddTableChange;
import com.stano.schema.diff.change.DropConstraintChange;
import com.stano.schema.diff.change.DropFunctionChange;
import com.stano.schema.diff.change.DropKeyChange;
import com.stano.schema.diff.change.DropProcedureChange;
import com.stano.schema.diff.change.DropTableChange;
import com.stano.schema.diff.change.DropViewChange;
import com.stano.schema.model.Column;
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
import com.stano.schema.model.Table;
import com.stano.schema.model.View;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SchemaDiffEngine")
class SchemaDiffEngineTest {

  private static final URL TEST_URL = SchemaDiffEngineTest.class.getResource("/");

  @Test
  @DisplayName("detects added table")
  void detectsAddedTable() {
    Schema oldSchema = new Schema(TEST_URL);
    Schema newSchema = new Schema(TEST_URL);

    Table newTable = new Table(newSchema, null, "users", null, null, false);
    newSchema.addTable(newTable);

    SchemaDiffEngine engine = new SchemaDiffEngine();
    ChangeSet changeSet = engine.diff(oldSchema, newSchema);

    assertEquals(1, changeSet.getChanges().size());
    assertTrue(changeSet.getChanges().get(0) instanceof AddTableChange);
    assertEquals("users", ((AddTableChange) changeSet.getChanges().get(0)).getTableName());
  }

  @Test
  @DisplayName("detects dropped table")
  void detectsDroppedTable() {
    Schema oldSchema = new Schema(TEST_URL);
    Schema newSchema = new Schema(TEST_URL);

    Table oldTable = new Table(oldSchema, null, "users", null, null, false);
    oldSchema.addTable(oldTable);

    SchemaDiffEngine engine = new SchemaDiffEngine();
    ChangeSet changeSet = engine.diff(oldSchema, newSchema);

    assertEquals(1, changeSet.getChanges().size());
    assertTrue(changeSet.getChanges().get(0) instanceof DropTableChange);
    assertEquals("users", ((DropTableChange) changeSet.getChanges().get(0)).getTableName());
  }

  @Test
  @DisplayName("detects added column")
  void detectsAddedColumn() {
    Schema oldSchema = new Schema(TEST_URL);
    Schema newSchema = new Schema(TEST_URL);

    Table oldTable = new Table(oldSchema, null, "users", null, null, false);
    oldSchema.addTable(oldTable);

    Table newTable = new Table(newSchema, null, "users", null, null, false);
    Column email = new Column("email", ColumnType.VARCHAR, 255, false);
    newTable.getColumns().add(email);
    newSchema.addTable(newTable);

    SchemaDiffEngine engine = new SchemaDiffEngine();
    ChangeSet changeSet = engine.diff(oldSchema, newSchema);

    assertEquals(1, changeSet.getChanges().size());
    assertTrue(
        changeSet.getChanges().get(0) instanceof com.stano.schema.diff.change.AddColumnChange);
  }

  @Test
  @DisplayName("detects dropped column")
  void detectsDroppedColumn() {
    Schema oldSchema = new Schema(TEST_URL);
    Schema newSchema = new Schema(TEST_URL);

    Table oldTable = new Table(oldSchema, null, "users", null, null, false);
    Column email = new Column("email", ColumnType.VARCHAR, 255, false);
    oldTable.getColumns().add(email);
    oldSchema.addTable(oldTable);

    Table newTable = new Table(newSchema, null, "users", null, null, false);
    newSchema.addTable(newTable);

    SchemaDiffEngine engine = new SchemaDiffEngine();
    ChangeSet changeSet = engine.diff(oldSchema, newSchema);

    assertEquals(1, changeSet.getChanges().size());
    assertTrue(
        changeSet.getChanges().get(0) instanceof com.stano.schema.diff.change.DropColumnChange);
  }

  @Test
  @DisplayName("detects modified column type")
  void detectsModifiedColumnType() {
    Schema oldSchema = new Schema(TEST_URL);
    Schema newSchema = new Schema(TEST_URL);

    Table oldTable = new Table(oldSchema, null, "users", null, null, false);
    Column oldEmail = new Column("email", ColumnType.VARCHAR, 100, false);
    oldTable.getColumns().add(oldEmail);
    oldSchema.addTable(oldTable);

    Table newTable = new Table(newSchema, null, "users", null, null, false);
    Column newEmail = new Column("email", ColumnType.VARCHAR, 255, false);
    newTable.getColumns().add(newEmail);
    newSchema.addTable(newTable);

    SchemaDiffEngine engine = new SchemaDiffEngine();
    ChangeSet changeSet = engine.diff(oldSchema, newSchema);

    assertEquals(1, changeSet.getChanges().size());
    assertTrue(
        changeSet.getChanges().get(0) instanceof com.stano.schema.diff.change.ModifyColumnChange);
  }

  @Test
  @DisplayName("detects added primary key")
  void detectsAddedPrimaryKey() {
    Schema oldSchema = new Schema(TEST_URL);
    Schema newSchema = new Schema(TEST_URL);

    Table oldTable = new Table(oldSchema, null, "users", null, null, false);
    Column id = new Column("id", ColumnType.INT, 0, true);
    oldTable.getColumns().add(id);
    oldSchema.addTable(oldTable);

    Table newTable = new Table(newSchema, null, "users", null, null, false);
    newTable.getColumns().add(id);
    List<KeyColumn> pkCols = new ArrayList<>();
    pkCols.add(new KeyColumn("id"));
    Key pk = new Key(KeyType.PRIMARY, pkCols);
    newTable.getKeys().add(pk);
    newSchema.addTable(newTable);

    SchemaDiffEngine engine = new SchemaDiffEngine();
    ChangeSet changeSet = engine.diff(oldSchema, newSchema);

    assertEquals(1, changeSet.getChanges().size());
    assertTrue(changeSet.getChanges().get(0) instanceof com.stano.schema.diff.change.AddKeyChange);
  }

  @Test
  @DisplayName("detects added constraint")
  void detectsAddedConstraint() {
    Schema oldSchema = new Schema(TEST_URL);
    Schema newSchema = new Schema(TEST_URL);

    Table oldTable = new Table(oldSchema, null, "users", null, null, false);
    Column email = new Column("email", ColumnType.VARCHAR, 255, false);
    oldTable.getColumns().add(email);
    oldSchema.addTable(oldTable);

    Table newTable = new Table(newSchema, null, "users", null, null, false);
    newTable.getColumns().add(email);
    Constraint chk = new Constraint("chk_email", "email like '%@%'", null);
    newTable.getConstraints().add(chk);
    newSchema.addTable(newTable);

    SchemaDiffEngine engine = new SchemaDiffEngine();
    ChangeSet changeSet = engine.diff(oldSchema, newSchema);

    assertEquals(1, changeSet.getChanges().size());
    assertTrue(
        changeSet.getChanges().get(0) instanceof com.stano.schema.diff.change.AddConstraintChange);
  }

  @Test
  @DisplayName("detects added relation")
  void detectsAddedRelation() {
    Schema oldSchema = new Schema(TEST_URL);
    Schema newSchema = new Schema(TEST_URL);

    Table oldOrdersTable = new Table(oldSchema, null, "orders", null, null, false);
    oldSchema.addTable(oldOrdersTable);
    Table oldUsersTable = new Table(oldSchema, null, "users", null, null, false);
    oldSchema.addTable(oldUsersTable);

    Table newOrdersTable = new Table(newSchema, null, "orders", null, null, false);
    Relation fk = new Relation("orders", "user_id", "users", "id", RelationType.CASCADE, false);
    newOrdersTable.getRelations().add(fk);
    newSchema.addTable(newOrdersTable);
    Table newUsersTable = new Table(newSchema, null, "users", null, null, false);
    newSchema.addTable(newUsersTable);

    SchemaDiffEngine engine = new SchemaDiffEngine();
    ChangeSet changeSet = engine.diff(oldSchema, newSchema);

    assertEquals(1, changeSet.getChanges().size());
    assertTrue(
        changeSet.getChanges().get(0) instanceof com.stano.schema.diff.change.AddRelationChange);
  }

  @Test
  @DisplayName("detects added view")
  void detectsAddedView() {
    Schema oldSchema = new Schema(TEST_URL);
    Schema newSchema = new Schema(TEST_URL);

    View newView = new View(null, "user_summary", "SELECT id, email FROM users", null);
    newSchema.addView(newView);

    SchemaDiffEngine engine = new SchemaDiffEngine();
    ChangeSet changeSet = engine.diff(oldSchema, newSchema);

    assertEquals(1, changeSet.getChanges().size());
    assertTrue(changeSet.getChanges().get(0) instanceof com.stano.schema.diff.change.AddViewChange);
  }

  @Test
  @DisplayName("detects dropped view")
  void detectsDroppedView() {
    Schema oldSchema = new Schema(TEST_URL);
    View view = new View(null, "user_summary", "SELECT id FROM users", null);
    oldSchema.addView(view);

    Schema newSchema = new Schema(TEST_URL);

    SchemaDiffEngine engine = new SchemaDiffEngine();
    ChangeSet changeSet = engine.diff(oldSchema, newSchema);

    assertEquals(1, changeSet.getChanges().size());
    assertTrue(changeSet.getChanges().get(0) instanceof DropViewChange);
  }

  @Test
  @DisplayName("detects dropped key")
  void detectsDroppedKey() {
    Schema oldSchema = new Schema(TEST_URL);
    Table oldTable = new Table(oldSchema, null, "users", null, null, false);
    Column id = new Column("id", ColumnType.INT, 0, true);
    oldTable.getColumns().add(id);
    List<KeyColumn> pkCols = new ArrayList<>();
    pkCols.add(new KeyColumn("id"));
    Key pk = new Key(KeyType.PRIMARY, pkCols);
    oldTable.getKeys().add(pk);
    oldSchema.addTable(oldTable);

    Schema newSchema = new Schema(TEST_URL);
    Table newTable = new Table(newSchema, null, "users", null, null, false);
    newTable.getColumns().add(id);
    newSchema.addTable(newTable);

    SchemaDiffEngine engine = new SchemaDiffEngine();
    ChangeSet changeSet = engine.diff(oldSchema, newSchema);

    assertEquals(1, changeSet.getChanges().size());
    assertTrue(changeSet.getChanges().get(0) instanceof DropKeyChange);
  }

  @Test
  @DisplayName("detects dropped constraint")
  void detectsDroppedConstraint() {
    Schema oldSchema = new Schema(TEST_URL);
    Table oldTable = new Table(oldSchema, null, "users", null, null, false);
    Constraint chk = new Constraint("chk_email", "email like '%@%'", null);
    oldTable.getConstraints().add(chk);
    oldSchema.addTable(oldTable);

    Schema newSchema = new Schema(TEST_URL);
    Table newTable = new Table(newSchema, null, "users", null, null, false);
    newSchema.addTable(newTable);

    SchemaDiffEngine engine = new SchemaDiffEngine();
    ChangeSet changeSet = engine.diff(oldSchema, newSchema);

    assertEquals(1, changeSet.getChanges().size());
    assertTrue(changeSet.getChanges().get(0) instanceof DropConstraintChange);
  }

  @Test
  @DisplayName("detects constraint SQL change as drop and re-add")
  void detectsConstraintSqlChange() {
    Schema oldSchema = new Schema(TEST_URL);
    Table oldTable = new Table(oldSchema, null, "users", null, null, false);
    oldTable.getConstraints().add(new Constraint("chk_email", "email like '%@%'", null));
    oldSchema.addTable(oldTable);

    Schema newSchema = new Schema(TEST_URL);
    Table newTable = new Table(newSchema, null, "users", null, null, false);
    newTable.getConstraints().add(new Constraint("chk_email", "email like '%@%.%'", null));
    newSchema.addTable(newTable);

    SchemaDiffEngine engine = new SchemaDiffEngine();
    ChangeSet changeSet = engine.diff(oldSchema, newSchema);

    assertEquals(2, changeSet.getChanges().size());
    assertTrue(changeSet.getChanges().get(0) instanceof DropConstraintChange);
    assertTrue(changeSet.getChanges().get(1) instanceof AddConstraintChange);
  }

  @Test
  @DisplayName("change order is drop before add")
  void changeOrderIsDropBeforeAdd() {
    Schema oldSchema = new Schema(TEST_URL);
    Schema newSchema = new Schema(TEST_URL);

    // Old schema has table1 → table2 (with FK)
    Table oldTable1 = new Table(oldSchema, null, "table1", null, null, false);
    oldSchema.addTable(oldTable1);
    Table oldTable2 = new Table(oldSchema, null, "table2", null, null, false);
    Relation oldFk = new Relation("table2", "t1_id", "table1", "id", RelationType.CASCADE, false);
    oldTable2.getRelations().add(oldFk);
    oldSchema.addTable(oldTable2);

    // New schema has only table2 (different structure)
    Table newTable2 = new Table(newSchema, null, "table2", null, null, false);
    newSchema.addTable(newTable2);

    SchemaDiffEngine engine = new SchemaDiffEngine();
    ChangeSet changeSet = engine.diff(oldSchema, newSchema);

    // Should drop relation first, then table1
    List<SchemaChange> changes = changeSet.getChanges();
    assertFalse(changes.isEmpty());

    // Find indices of relation drop and table drop
    int relationDropIdx = -1;
    int tableDropIdx = -1;
    for (int i = 0; i < changes.size(); i++) {
      if (changes.get(i) instanceof com.stano.schema.diff.change.DropRelationChange) {
        relationDropIdx = i;
      }
      if (changes.get(i) instanceof DropTableChange
          && ((DropTableChange) changes.get(i)).getTableName().equals("table1")) {
        tableDropIdx = i;
      }
    }

    assertTrue(relationDropIdx >= 0, "Should have relation drop");
    assertTrue(tableDropIdx >= 0, "Should have table1 drop");
    assertTrue(relationDropIdx < tableDropIdx, "Relations should be dropped before tables");
  }

  @Test
  @DisplayName("detects added function")
  void detectsAddedFunction() {
    Schema oldSchema = new Schema(TEST_URL);
    Schema newSchema = new Schema(TEST_URL);
    newSchema.addFunction(
        new Function(null, "fn_greet", DatabaseType.POSTGRESQL, "CREATE FUNCTION fn_greet()"));

    SchemaDiffEngine engine = new SchemaDiffEngine();
    ChangeSet changeSet = engine.diff(oldSchema, newSchema);

    assertEquals(1, changeSet.getChanges().size());
    assertTrue(changeSet.getChanges().get(0) instanceof AddFunctionChange);
    assertEquals(
        "fn_greet", ((AddFunctionChange) changeSet.getChanges().get(0)).getFunction().getName());
  }

  @Test
  @DisplayName("detects dropped function")
  void detectsDroppedFunction() {
    Schema oldSchema = new Schema(TEST_URL);
    oldSchema.addFunction(
        new Function(null, "fn_greet", DatabaseType.POSTGRESQL, "CREATE FUNCTION fn_greet()"));
    Schema newSchema = new Schema(TEST_URL);

    SchemaDiffEngine engine = new SchemaDiffEngine();
    ChangeSet changeSet = engine.diff(oldSchema, newSchema);

    assertEquals(1, changeSet.getChanges().size());
    assertTrue(changeSet.getChanges().get(0) instanceof DropFunctionChange);
    assertEquals(
        "fn_greet", ((DropFunctionChange) changeSet.getChanges().get(0)).getFunctionName());
  }

  @Test
  @DisplayName("detects modified function as drop then add")
  void detectsModifiedFunction() {
    Schema oldSchema = new Schema(TEST_URL);
    oldSchema.addFunction(
        new Function(null, "fn_greet", DatabaseType.POSTGRESQL, "CREATE FUNCTION fn_greet() v1"));
    Schema newSchema = new Schema(TEST_URL);
    newSchema.addFunction(
        new Function(null, "fn_greet", DatabaseType.POSTGRESQL, "CREATE FUNCTION fn_greet() v2"));

    SchemaDiffEngine engine = new SchemaDiffEngine();
    ChangeSet changeSet = engine.diff(oldSchema, newSchema);

    assertEquals(2, changeSet.getChanges().size());
    assertTrue(changeSet.getChanges().get(0) instanceof DropFunctionChange);
    assertTrue(changeSet.getChanges().get(1) instanceof AddFunctionChange);
  }

  @Test
  @DisplayName("detects added procedure")
  void detectsAddedProcedure() {
    Schema oldSchema = new Schema(TEST_URL);
    Schema newSchema = new Schema(TEST_URL);
    newSchema.addProcedure(
        new Procedure(null, "sp_audit", DatabaseType.POSTGRESQL, "CREATE PROCEDURE sp_audit()"));

    SchemaDiffEngine engine = new SchemaDiffEngine();
    ChangeSet changeSet = engine.diff(oldSchema, newSchema);

    assertEquals(1, changeSet.getChanges().size());
    assertTrue(changeSet.getChanges().get(0) instanceof AddProcedureChange);
    assertEquals(
        "sp_audit", ((AddProcedureChange) changeSet.getChanges().get(0)).getProcedure().getName());
  }

  @Test
  @DisplayName("detects dropped procedure")
  void detectsDroppedProcedure() {
    Schema oldSchema = new Schema(TEST_URL);
    oldSchema.addProcedure(
        new Procedure(null, "sp_audit", DatabaseType.POSTGRESQL, "CREATE PROCEDURE sp_audit()"));
    Schema newSchema = new Schema(TEST_URL);

    SchemaDiffEngine engine = new SchemaDiffEngine();
    ChangeSet changeSet = engine.diff(oldSchema, newSchema);

    assertEquals(1, changeSet.getChanges().size());
    assertTrue(changeSet.getChanges().get(0) instanceof DropProcedureChange);
    assertEquals(
        "sp_audit", ((DropProcedureChange) changeSet.getChanges().get(0)).getProcedureName());
  }

  @Test
  @DisplayName("detects modified procedure as drop then add")
  void detectsModifiedProcedure() {
    Schema oldSchema = new Schema(TEST_URL);
    oldSchema.addProcedure(
        new Procedure(null, "sp_audit", DatabaseType.POSTGRESQL, "CREATE PROCEDURE sp_audit() v1"));
    Schema newSchema = new Schema(TEST_URL);
    newSchema.addProcedure(
        new Procedure(null, "sp_audit", DatabaseType.POSTGRESQL, "CREATE PROCEDURE sp_audit() v2"));

    SchemaDiffEngine engine = new SchemaDiffEngine();
    ChangeSet changeSet = engine.diff(oldSchema, newSchema);

    assertEquals(2, changeSet.getChanges().size());
    assertTrue(changeSet.getChanges().get(0) instanceof DropProcedureChange);
    assertTrue(changeSet.getChanges().get(1) instanceof AddProcedureChange);
  }

  @Test
  @DisplayName("no changes returns empty changeset")
  void noChangesReturnsEmptyChangeSet() {
    Schema oldSchema = new Schema(TEST_URL);
    Table oldTable = new Table(oldSchema, null, "users", null, null, false);
    oldSchema.addTable(oldTable);

    Schema newSchema = new Schema(TEST_URL);
    Table newTable = new Table(newSchema, null, "users", null, null, false);
    newSchema.addTable(newTable);

    SchemaDiffEngine engine = new SchemaDiffEngine();
    ChangeSet changeSet = engine.diff(oldSchema, newSchema);

    assertTrue(changeSet.isEmpty());
  }
}
