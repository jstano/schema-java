package com.stano.schema.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.MalformedURLException;
import java.net.URI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ColumnTest {
  @Test
  @DisplayName("constructor (name,type,length,required) should set fields and defaults")
  void testConstructorBasic() {
    Column col = new Column("code", ColumnType.VARCHAR, 50, true);

    assertEquals("code", col.getName());
    assertEquals(ColumnType.VARCHAR, col.getType());
    assertEquals(50, col.getLength());
    assertEquals(0, col.getScale());
    assertTrue(col.isRequired());
    assertNull(col.getCheckConstraint());
    assertNull(col.getDefaultConstraint());
    assertNull(col.getGenerated());
    assertNull(col.getMinValue());
    assertNull(col.getMaxValue());
    assertNull(col.getEnumType());
    assertNull(col.getElementType());

    assertFalse(col.needsCheckConstraints(BooleanMode.NATIVE));
    assertFalse(col.needsCheckConstraints(BooleanMode.YES_NO));
  }

  @Test
  @DisplayName("constructor with checkConstraint should set it and other defaults")
  void testConstructorWithCheckConstraint() {
    Column col = new Column("amount", ColumnType.INT, 0, false, "amount > 0");

    assertFalse(col.isRequired());
    assertEquals("amount > 0", col.getCheckConstraint());
    assertNull(col.getDefaultConstraint());
    assertNull(col.getGenerated());
    assertNull(col.getMinValue());
    assertNull(col.getMaxValue());
    assertNull(col.getEnumType());
    assertNull(col.getElementType());

    assertTrue(col.needsCheckConstraints(BooleanMode.NATIVE));
  }

  @Test
  @DisplayName("full constructor should assign all fields correctly")
  void testFullConstructor() {
    Column col =
        new Column(
            "prices",
            ColumnType.ARRAY,
            0,
            4,
            false,
            "json_valid(prices)",
            "DEFAULT '[]'",
            null,
            null,
            null,
            null,
            ColumnType.DECIMAL);

    assertEquals("prices", col.getName());
    assertEquals(ColumnType.ARRAY, col.getType());
    assertEquals(0, col.getLength());
    assertEquals(4, col.getScale());
    assertFalse(col.isRequired());
    assertEquals("json_valid(prices)", col.getCheckConstraint());
    assertEquals("DEFAULT '[]'", col.getDefaultConstraint());
    assertNull(col.getGenerated());
    assertNull(col.getMinValue());
    assertNull(col.getMaxValue());
    assertNull(col.getEnumType());
    assertEquals(ColumnType.DECIMAL, col.getElementType());

    assertTrue(col.needsCheckConstraints(BooleanMode.NATIVE));
  }

  @Test
  @DisplayName(
      "needsCheckConstraints for BOOLEAN depends on BooleanMode when no explicit constraints")
  void testNeedsCheckConstraintsBoolean() {
    Column col = new Column("active", ColumnType.BOOLEAN, 0, false);

    assertFalse(col.needsCheckConstraints(BooleanMode.NATIVE));
    assertTrue(col.needsCheckConstraints(BooleanMode.YES_NO));
    assertTrue(col.needsCheckConstraints(BooleanMode.YN));
  }

  @Test
  @DisplayName("hasMinOrMaxValue reflects presence of bounds")
  void testHasMinOrMaxValue() {
    assertFalse(new Column("x", ColumnType.INT, 0, false).hasMinOrMaxValue());

    assertTrue(
        new Column("y", ColumnType.DECIMAL, 10, 2, false, null, null, null, "0", null, null, null)
            .hasMinOrMaxValue());

    assertTrue(
        new Column("z", ColumnType.DECIMAL, 10, 2, false, null, null, null, null, "100", null, null)
            .hasMinOrMaxValue());
  }

  @Test
  @DisplayName(
      "Table integration: hasColumn is case-insensitive and getColumn caches names ignoring case")
  void testTableIntegrationHasColumn() throws MalformedURLException {
    Schema schema = new Schema(URI.create("https://example.com/schema.json").toURL());
    Table table = new Table(schema, "public", "users", null, LockEscalation.AUTO, false);

    table
        .getColumns()
        .addAll(
            java.util.Arrays.asList(
                new Column("Id", ColumnType.SEQUENCE, 0, true),
                new Column("userName", ColumnType.VARCHAR, 100, false)));

    assertTrue(table.hasColumn("id"));
    assertTrue(table.hasColumn("ID"));
    assertTrue(table.hasColumn("UserName"));
    assertFalse(table.hasColumn("missing"));

    assertEquals("Id", table.getColumn("id").getName());
    assertEquals("userName", table.getColumn("USERNAME").getName());
  }

  @Test
  @DisplayName("Table integration: getIdentityColumn returns first SEQUENCE or LONGSEQUENCE")
  void testTableIntegrationGetIdentityColumn() throws MalformedURLException {
    Schema schema = new Schema(URI.create("https://example.com/schema.json").toURL());
    Table t1 = new Table(schema, "public", "t1", null, LockEscalation.AUTO, false);

    t1.getColumns()
        .addAll(
            java.util.Arrays.asList(
                new Column("code", ColumnType.VARCHAR, 20, false),
                new Column("id", ColumnType.SEQUENCE, 0, true),
                new Column("id2", ColumnType.LONGSEQUENCE, 0, true)));

    assertEquals("id", t1.getIdentityColumn().getName());

    Table t2 = new Table(schema, "public", "t2", null, LockEscalation.AUTO, false);
    t2.getColumns()
        .addAll(
            java.util.Arrays.asList(
                new Column("code", ColumnType.VARCHAR, 20, false),
                new Column("id2", ColumnType.LONGSEQUENCE, 0, true)));

    assertEquals("id2", t2.getIdentityColumn().getName());

    Table t3 = new Table(schema, "public", "t3", null, LockEscalation.AUTO, false);
    t3.getColumns().add(new Column("code", ColumnType.VARCHAR, 20, false));

    assertNull(t3.getIdentityColumn());
  }

  @Test
  @DisplayName("Table integration: getColumnsWithCheckConstraints collects columns per BooleanMode")
  void testTableIntegrationGetColumnsWithCheckConstraints() throws MalformedURLException {
    Schema schema = new Schema(URI.create("https://example.com/schema.json").toURL());
    Table table = new Table(schema, "public", "orders", null, LockEscalation.AUTO, false);

    Column c1 = new Column("flag", ColumnType.BOOLEAN, 0, false);
    Column c2 = new Column("amt", ColumnType.INT, 0, false, "amt > 0");
    Column c3 =
        new Column(
            "price", ColumnType.DECIMAL, 10, 2, false, null, null, null, "0", "100", null, null);
    table.getColumns().addAll(java.util.Arrays.asList(c1, c2, c3));

    java.util.List<Column> nativeCols = table.getColumnsWithCheckConstraints(BooleanMode.NATIVE);
    java.util.List<Column> ynCols = table.getColumnsWithCheckConstraints(BooleanMode.YN);

    assertEquals(
        java.util.Arrays.asList("amt", "price"),
        nativeCols.stream().map(Column::getName).collect(java.util.stream.Collectors.toList()));
    assertEquals(
        java.util.Arrays.asList("flag", "amt", "price"),
        ynCols.stream().map(Column::getName).collect(java.util.stream.Collectors.toList()));
  }
}
