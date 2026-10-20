package com.stano.schema.reverseengineer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.stano.schema.model.Column;
import com.stano.schema.model.ColumnType;
import com.stano.schema.model.Constraint;
import com.stano.schema.model.Key;
import com.stano.schema.model.KeyColumn;
import com.stano.schema.model.KeyType;
import com.stano.schema.model.RelationType;
import com.stano.schema.model.Schema;
import com.stano.schema.model.Table;
import java.sql.Connection;
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
          + " DELETE_RULE (NO_ACTION, which is ENFORCE)")
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
        RelationType.ENFORCE,
        relationType,
        "FK with DELETE_RULE=NO_ACTION and UPDATE_RULE=CASCADE should map to ENFORCE (delete rule"
            + " wins); DONOTHING would drop the constraint on regeneration");
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

  @Test
  @DisplayName("mapDeleteRule rejects SET DEFAULT instead of mapping it to SETNULL")
  void testSetDefaultIsRejected() {
    var byName =
        assertThrows(
            IllegalStateException.class, () -> SchemaReader.mapDeleteRule("importedKeySetDefault"));
    assertTrue(byName.getMessage().contains("importedKeySetDefault"), byName.getMessage());
    assertThrows(IllegalStateException.class, () -> SchemaReader.mapDeleteRule("4"));
    assertThrows(IllegalStateException.class, () -> SchemaReader.mapDeleteRule("bogus"));
    assertThrows(IllegalStateException.class, () -> SchemaReader.mapDeleteRule(null));
  }

  @Test
  @DisplayName("mapDeleteRule accepts the numeric DatabaseMetaData constants real drivers report")
  void testMapDeleteRuleNumericConstants() {
    assertEquals(RelationType.CASCADE, SchemaReader.mapDeleteRule("0"));
    assertEquals(RelationType.ENFORCE, SchemaReader.mapDeleteRule("1"));
    assertEquals(RelationType.SETNULL, SchemaReader.mapDeleteRule("2"));
    assertEquals(RelationType.ENFORCE, SchemaReader.mapDeleteRule("3"));
  }

  @Test
  @DisplayName("groupForeignKeys fails on a SET DEFAULT foreign key")
  void testGroupForeignKeysRejectsSetDefault() {
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
                "importedKeySetDefault"));

    assertThrows(IllegalStateException.class, () -> SchemaReader.groupForeignKeys(rows));
  }

  @Test
  @DisplayName("withoutCrossSchemaTargets drops foreign keys that reference another schema")
  void testCrossSchemaForeignKeysAreSkipped() {
    var sameSchema =
        new ForeignKeyData(
            "fk_a", "public", "parent_a", "id", "child", "a_id", 1, "importedNoAction", "0");
    var otherSchema =
        new ForeignKeyData(
            "fk_x", "other", "parent_x", "id", "child", "x_id", 1, "importedNoAction", "0");
    var unknownSchema =
        new ForeignKeyData("fk_b", "parent_b", "id", "child", "b_id", 1, "importedNoAction", "0");

    var kept =
        SchemaReader.withoutCrossSchemaTargets(
            List.of(sameSchema, otherSchema, unknownSchema), "public");

    assertEquals(List.of(sameSchema, unknownSchema), kept);
  }

  private static Schema schemaWithTable() {
    Schema schema = new Schema(null);
    schema.addTable(new Table(schema, "public", "t", null, null, false));
    return schema;
  }

  private static SchemaReader.IndexRow row(
      String index, boolean unique, String column, boolean include, String predicate) {
    return new SchemaReader.IndexRow("t", index, unique, false, column, include, predicate);
  }

  @Test
  @DisplayName("applyIndexRows keeps a plain unique index as a unique key")
  void testPlainUniqueIndex() {
    Schema schema = schemaWithTable();

    SchemaReader.applyIndexRows(schema, List.of(row("u", true, "a", false, null)));

    var keys = schema.getTable("t").getKeys();
    assertEquals(1, keys.size());
    assertEquals(KeyType.UNIQUE, keys.get(0).getType());
  }

  @Test
  @DisplayName("applyIndexRows keeps a partial unique index's predicate and writes it as an index")
  void testPartialUniqueIndexKeepsFilter() {
    Schema schema = schemaWithTable();

    SchemaReader.applyIndexRows(schema, List.of(row("u", true, "a", false, "(b IS NOT NULL)")));

    var keys = schema.getTable("t").getKeys();
    assertEquals(1, keys.size());
    assertEquals(KeyType.INDEX, keys.get(0).getType());
    assertTrue(keys.get(0).isUnique());
    assertEquals("a", keys.get(0).getColumnsAsString());
    assertEquals("(b IS NOT NULL)", keys.get(0).getFilter());
  }

  @Test
  @DisplayName("applyIndexRows separates INCLUDE columns from key columns")
  void testCoveringIndexKeepsIncludeColumns() {
    Schema schema = schemaWithTable();

    SchemaReader.applyIndexRows(
        schema, List.of(row("i", false, "a", false, null), row("i", false, "b", true, null)));

    var key = schema.getTable("t").getKeys().get(0);
    assertEquals(KeyType.INDEX, key.getType());
    assertEquals("a", key.getColumnsAsString());
    assertEquals("b", key.getInclude());
  }

  @Test
  @DisplayName("applyIndexRows skips expression indexes but keeps the others")
  void testExpressionIndexIsSkipped() {
    Schema schema = schemaWithTable();

    SchemaReader.applyIndexRows(
        schema, List.of(row("e", false, null, false, null), row("ok", false, "a", false, null)));

    var keys = schema.getTable("t").getKeys();
    assertEquals(1, keys.size());
    assertEquals("a", keys.get(0).getColumnsAsString());
  }

  @Test
  @DisplayName("applyIndexRows skips the primary-key index and indexes behind a constraint")
  void testPrimaryAndConstraintBackedIndexesAreSkipped() {
    Schema schema = schemaWithTable();
    schema.getTable("t").getConstraints().add(new Constraint("excl", "exclude using gist", null));

    SchemaReader.applyIndexRows(
        schema,
        List.of(
            new SchemaReader.IndexRow("t", "t_pkey", true, true, "id", false, null),
            row("excl", false, "a", false, null)));

    assertTrue(schema.getTable("t").getKeys().isEmpty());
  }

  @Test
  @DisplayName("promoteLoneUniqueKey promotes only when every key column is NOT NULL")
  void testLoneUniqueKeyPromotion() {
    Schema schema = new Schema(null);
    Table required = new Table(schema, "public", "req", null, null, false);
    required.getColumns().add(column("a", true));
    required.getKeys().add(new Key(KeyType.UNIQUE, List.of(new KeyColumn("a"))));
    Table nullable = new Table(schema, "public", "nul", null, null, false);
    nullable.getColumns().add(column("a", false));
    nullable.getKeys().add(new Key(KeyType.UNIQUE, List.of(new KeyColumn("a"))));

    reader.promoteLoneUniqueKey(required);
    reader.promoteLoneUniqueKey(nullable);

    assertEquals(KeyType.PRIMARY, required.getKeys().get(0).getType());
    assertEquals(KeyType.UNIQUE, nullable.getKeys().get(0).getType());
    assertEquals(null, nullable.getPrimaryKey());
  }

  private static Column column(String name, boolean required) {
    return new Column(
        name, ColumnType.INT, 0, 0, required, null, null, null, null, null, null, null);
  }

  @Test
  @DisplayName("trimRoutineDefinition drops surrounding whitespace and trailing semicolons")
  void testTrimRoutineDefinition() {
    assertEquals(
        "create or replace function f() returns int as $$ select 1 $$",
        SchemaReader.trimRoutineDefinition(
            "\ncreate or replace function f() returns int as $$ select 1 $$;\n"));
  }

  @Test
  @DisplayName("readSchema rejects a database that is not PostgreSQL")
  void readSchemaRejectsNonPostgres() throws SQLException {
    Connection connection = mock(Connection.class);
    DatabaseMetaData metaData = mock(DatabaseMetaData.class);
    when(connection.getMetaData()).thenReturn(metaData);
    when(metaData.getDatabaseProductName()).thenReturn("H2");

    var x = assertThrows(IllegalArgumentException.class, () -> reader.readSchema(connection));

    assertTrue(x.getMessage().contains("PostgreSQL only"), x.getMessage());
    assertTrue(x.getMessage().contains("H2"), x.getMessage());
  }

  @Test
  @DisplayName("the schema name is escaped before it is used as a LIKE pattern")
  void schemaNameIsEscapedAsLikePattern() throws SQLException {
    DatabaseMetaData metaData = mock(DatabaseMetaData.class);
    ResultSet empty = mock(ResultSet.class);
    when(metaData.getSearchStringEscape()).thenReturn("\\");
    when(metaData.getTables(null, "my\\_schema", null, new String[] {"TABLE"})).thenReturn(empty);

    reader.populateTables(new Schema(null), metaData, "my_schema");

    org.mockito.Mockito.verify(metaData)
        .getTables(null, "my\\_schema", null, new String[] {"TABLE"});
  }
}
