package com.stano.schema.reverseengineer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.stano.schema.model.ColumnType;
import com.stano.schema.model.RelationType;
import com.stano.schema.model.Schema;
import com.stano.schema.model.Table;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SchemaReader")
public class SchemaReaderTest {
  private final SchemaReader reader = new SchemaReader();

  @Test
  @DisplayName("getColumnType maps Types.OTHER with typeName='uuid' to ColumnType.UUID")
  void testTypesOtherUuid() {
    ColumnType result = reader.getColumnType(Types.OTHER, "uuid", false, 0);
    assertEquals(ColumnType.UUID, result);
  }

  @Test
  @DisplayName("getColumnType maps Types.OTHER with typeName='jsonb' to ColumnType.JSON")
  void testTypesOtherJsonb() {
    ColumnType result = reader.getColumnType(Types.OTHER, "jsonb", false, 0);
    assertEquals(ColumnType.JSON, result);
  }

  @Test
  @DisplayName("getColumnType maps unknown Types.OTHER to ColumnType.VARCHAR (no crash)")
  void testTypesOtherUnknown() {
    ColumnType result = reader.getColumnType(Types.OTHER, "some_enum_type", false, 0);
    assertEquals(ColumnType.VARCHAR, result);
  }

  @Test
  @DisplayName("getColumnType falls back to TEXT for truly unknown JDBC type constant")
  void testUnknownJdbcType() {
    ColumnType result = reader.getColumnType(9999, "unknown", false, 0);
    assertEquals(ColumnType.TEXT, result);
  }

  @Test
  @DisplayName("populateImportedKeys uses DELETE_RULE, not UPDATE_RULE, for RelationType")
  void testImportedKeysUsesDeleteRule() throws SQLException {
    Schema schema = new Schema(null);
    Table table = new Table(schema, "", "child_table", null, null, false);
    schema.addTable(table);

    DatabaseMetaData metaData = mock(DatabaseMetaData.class);
    ResultSet importedKeysResultSet = mock(ResultSet.class);

    when(metaData.getImportedKeys(null, null, "child_table")).thenReturn(importedKeysResultSet);

    when(importedKeysResultSet.next()).thenReturn(true).thenReturn(false);
    when(importedKeysResultSet.getString("PKTABLE_NAME")).thenReturn("parent_table");
    when(importedKeysResultSet.getString("PKCOLUMN_NAME")).thenReturn("id");
    when(importedKeysResultSet.getString("FKTABLE_NAME")).thenReturn("child_table");
    when(importedKeysResultSet.getString("FKCOLUMN_NAME")).thenReturn("parent_id");
    when(importedKeysResultSet.getInt("KEY_SEQ")).thenReturn(1);
    when(importedKeysResultSet.getString("DELETE_RULE")).thenReturn("importedKeyCascade");
    when(importedKeysResultSet.getString("UPDATE_RULE")).thenReturn("importedNoAction");

    reader.populateImportedKeys(schema, metaData, null);

    assertEquals(1, table.getRelations().size());
    RelationType relationType = table.getRelations().get(0).getType();
    assertEquals(
        RelationType.CASCADE,
        relationType,
        "FK with DELETE_RULE=CASCADE and UPDATE_RULE=NO_ACTION should map to CASCADE");
  }

  @Test
  @DisplayName(
      "populateImportedKeys with UPDATE_RULE=CASCADE and DELETE_RULE=NO_ACTION still maps to"
          + " DELETE_RULE (NO_ACTION)")
  void testImportedKeysDeleteRuleOverrules() throws SQLException {
    Schema schema = new Schema(null);
    Table table = new Table(schema, "", "child_table", null, null, false);
    schema.addTable(table);

    DatabaseMetaData metaData = mock(DatabaseMetaData.class);
    ResultSet importedKeysResultSet = mock(ResultSet.class);

    when(metaData.getImportedKeys(null, null, "child_table")).thenReturn(importedKeysResultSet);

    when(importedKeysResultSet.next()).thenReturn(true).thenReturn(false);
    when(importedKeysResultSet.getString("PKTABLE_NAME")).thenReturn("parent_table");
    when(importedKeysResultSet.getString("PKCOLUMN_NAME")).thenReturn("id");
    when(importedKeysResultSet.getString("FKTABLE_NAME")).thenReturn("child_table");
    when(importedKeysResultSet.getString("FKCOLUMN_NAME")).thenReturn("parent_id");
    when(importedKeysResultSet.getInt("KEY_SEQ")).thenReturn(1);
    when(importedKeysResultSet.getString("DELETE_RULE")).thenReturn("importedNoAction");
    when(importedKeysResultSet.getString("UPDATE_RULE")).thenReturn("importedKeyCascade");

    reader.populateImportedKeys(schema, metaData, null);

    assertEquals(1, table.getRelations().size());
    RelationType relationType = table.getRelations().get(0).getType();
    assertEquals(
        RelationType.DONOTHING,
        relationType,
        "FK with DELETE_RULE=NO_ACTION and UPDATE_RULE=CASCADE should map to DONOTHING (delete rule"
            + " wins)");
  }

  @Test
  @DisplayName("groupForeignKeys passes single-column rows through as one relation per row")
  void testGroupForeignKeysSingleColumn() {
    var rows =
        List.of(
            new ForeignKeyData(
                "fk_a",
                "parent",
                "id",
                "child",
                "parent_id",
                1,
                "importedNoAction",
                "importedKeyCascade"));

    var relations = SchemaReader.groupForeignKeys(rows);

    assertEquals(1, relations.size());
    assertFalse(relations.get(0).isComposite());
    assertEquals("parent_id", relations.get(0).getFromColumnName());
    assertEquals("id", relations.get(0).getToColumnName());
    assertEquals(RelationType.CASCADE, relations.get(0).getType());
  }

  @Test
  @DisplayName("groupForeignKeys folds multiple rows sharing an fkName into one composite relation")
  void testGroupForeignKeysComposite() {
    var rows =
        List.of(
            new ForeignKeyData(
                "fk_assignment",
                "Assignment",
                "ID",
                "Assignment",
                "ParentAssignmentID",
                1,
                "importedNoAction",
                "importedKeyCascade"),
            new ForeignKeyData(
                "fk_assignment",
                "Assignment",
                "PropertyID",
                "Assignment",
                "PropertyID",
                2,
                "importedNoAction",
                "importedKeyCascade"));

    var relations = SchemaReader.groupForeignKeys(rows);

    assertEquals(1, relations.size());
    assertTrue(relations.get(0).isComposite());
    var columnPairs = relations.get(0).getColumnPairs();
    assertEquals(2, columnPairs.size());
    assertEquals("ParentAssignmentID", columnPairs.get(0).getFromColumnName());
    assertEquals("ID", columnPairs.get(0).getToColumnName());
    assertEquals("PropertyID", columnPairs.get(1).getFromColumnName());
    assertEquals("PropertyID", columnPairs.get(1).getToColumnName());
  }

  @Test
  @DisplayName("groupForeignKeys keeps rows from different constraints as separate relations")
  void testGroupForeignKeysDifferentConstraints() {
    var rows =
        List.of(
            new ForeignKeyData(
                "fk_a",
                "parent_a",
                "id",
                "child",
                "a_id",
                1,
                "importedNoAction",
                "importedKeyCascade"),
            new ForeignKeyData(
                "fk_b",
                "parent_b",
                "id",
                "child",
                "b_id",
                1,
                "importedNoAction",
                "importedKeySetNull"));

    var relations = SchemaReader.groupForeignKeys(rows);

    assertEquals(2, relations.size());
    assertFalse(relations.get(0).isComposite());
    assertFalse(relations.get(1).isComposite());
    assertEquals("parent_a", relations.get(0).getToTableName());
    assertEquals("parent_b", relations.get(1).getToTableName());
    assertEquals(RelationType.SETNULL, relations.get(1).getType());
  }
}
