package com.stano.schema.reverseengineer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.stano.schema.model.Column;
import com.stano.schema.model.ColumnType;
import com.stano.schema.model.Key;
import com.stano.schema.model.KeyColumn;
import com.stano.schema.model.Schema;
import com.stano.schema.model.Table;
import com.stano.schema.reverseengineer.SqlServerSchemaReader.ColumnRow;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SqlServerSchemaReader")
class SqlServerSchemaReaderTest {

  private static ColumnRow row(String typeName) {
    return new ColumnRow("t", "c", typeName, 4, 10, 0, true, false, null, null, null, true, null);
  }

  private static ColumnRow withLength(String typeName, int maxLength) {
    return new ColumnRow(
        "t", "c", typeName, maxLength, 10, 0, true, false, null, null, null, true, null);
  }

  private static ColumnRow identity(String typeName) {
    return new ColumnRow("t", "c", typeName, 8, 10, 0, false, true, 1L, 1L, null, true, null);
  }

  @Test
  @DisplayName("stripOuterParens removes every layer that wraps the whole expression")
  void stripsAllWrappingLayers() {
    assertEquals("0", SqlServerSchemaReader.stripOuterParens("((0))"));
    assertEquals("getdate()", SqlServerSchemaReader.stripOuterParens("(getdate())"));
    assertEquals("'A'", SqlServerSchemaReader.stripOuterParens("('A')"));
  }

  @Test
  @DisplayName("stripOuterParens keeps parentheses that do not wrap the whole expression")
  void keepsPartialParens() {
    assertEquals("(a)+(b)", SqlServerSchemaReader.stripOuterParens("(a)+(b)"));
    assertEquals("(a)+(b)", SqlServerSchemaReader.stripOuterParens("((a)+(b))"));
  }

  @Test
  @DisplayName("stripOuterParens ignores parentheses inside string literals")
  void ignoresParensInStrings() {
    assertEquals("')('", SqlServerSchemaReader.stripOuterParens("(')(')"));
    assertEquals("'it''s (x)'", SqlServerSchemaReader.stripOuterParens("('it''s (x)')"));
  }

  @Test
  @DisplayName("checkSql rewraps a stored predicate as a check clause")
  void checkSqlRewraps() {
    assertEquals("check ([amount]>=(0))", SqlServerSchemaReader.checkSql("([amount]>=(0))"));
    assertEquals("check ([a]>(0))", SqlServerSchemaReader.checkSql("(([a]>(0)))"));
  }

  @Test
  @DisplayName("identity int and bigint columns become sequences")
  void identityBecomesSequence() {
    assertEquals(ColumnType.SEQUENCE, SqlServerSchemaReader.buildColumn(identity("int")).getType());
    assertEquals(
        ColumnType.LONGSEQUENCE, SqlServerSchemaReader.buildColumn(identity("bigint")).getType());
  }

  @Test
  @DisplayName("nvarchar length is counted in characters, varchar in bytes")
  void varcharLengths() {
    Column nvarchar = SqlServerSchemaReader.buildColumn(withLength("nvarchar", 200));
    assertEquals(ColumnType.VARCHAR, nvarchar.getType());
    assertEquals(100, nvarchar.getLength());
    assertEquals(50, SqlServerSchemaReader.buildColumn(withLength("varchar", 50)).getLength());
  }

  @Test
  @DisplayName("varchar(max) becomes text with no length")
  void maxBecomesText() {
    Column column = SqlServerSchemaReader.buildColumn(withLength("varchar", -1));
    assertEquals(ColumnType.TEXT, column.getType());
    assertEquals(0, column.getLength());
  }

  @Test
  @DisplayName("decimal keeps precision and scale, money maps to decimal(19,4)")
  void decimals() {
    var decimal =
        new ColumnRow("t", "c", "decimal", 9, 12, 3, true, false, null, null, null, true, null);
    Column column = SqlServerSchemaReader.buildColumn(decimal);
    assertEquals(ColumnType.DECIMAL, column.getType());
    assertEquals(12, column.getLength());
    assertEquals(3, column.getScale());

    Column money = SqlServerSchemaReader.buildColumn(row("money"));
    assertEquals(ColumnType.DECIMAL, money.getType());
    assertEquals(19, money.getLength());
    assertEquals(4, money.getScale());
  }

  @Test
  @DisplayName("float(n) up to 24 is a float, wider is a double")
  void floats() {
    var narrow =
        new ColumnRow("t", "c", "float", 4, 24, 0, true, false, null, null, null, true, null);
    var wide =
        new ColumnRow("t", "c", "float", 8, 53, 0, true, false, null, null, null, true, null);
    assertEquals(ColumnType.FLOAT, SqlServerSchemaReader.buildColumn(narrow).getType());
    assertEquals(ColumnType.DOUBLE, SqlServerSchemaReader.buildColumn(wide).getType());
  }

  @Test
  @DisplayName("an unsupported type falls back to text")
  void unsupportedTypeFallsBack() {
    assertEquals(ColumnType.TEXT, SqlServerSchemaReader.buildColumn(row("xml")).getType());
  }

  @Test
  @DisplayName("a stored default loses its wrapping parentheses")
  void defaultIsUnwrapped() {
    var withDefault =
        new ColumnRow("t", "c", "int", 4, 10, 0, false, false, null, null, null, true, "((0))");
    assertEquals("0", SqlServerSchemaReader.buildColumn(withDefault).getDefaultConstraint());
  }

  @Test
  @DisplayName("a computed column keeps its expression and drops any default")
  void computedColumn() {
    var computed =
        new ColumnRow(
            "t", "c", "int", 4, 10, 0, true, false, null, null, "([a]*[b])", true, "((0))");
    Column column = SqlServerSchemaReader.buildColumn(computed);
    assertEquals("[a]*[b]", column.getGenerated());
    assertNull(column.getDefaultConstraint());
  }

  @Test
  @DisplayName("nullable and not-null columns map to required")
  void requiredFlag() {
    assertEquals(false, SqlServerSchemaReader.buildColumn(row("int")).isRequired());
    assertEquals(true, SqlServerSchemaReader.buildColumn(identity("int")).isRequired());
  }

  @Test
  @DisplayName("viewBody extracts the query after AS")
  void viewBodyAfterAs() {
    assertEquals(
        "SELECT a FROM t",
        SqlServerSchemaReader.viewBody("v", "CREATE VIEW [dbo].[v] AS\r\nSELECT a FROM t"));
  }

  @Test
  @DisplayName("an AS inside the name or the query is not the separator")
  void asInsideNameOrQuery() {
    assertEquals(
        "SELECT a AS b FROM t",
        SqlServerSchemaReader.viewBody("v", "CREATE VIEW [dbo].[as_view] AS SELECT a AS b FROM t"));
    assertEquals(
        "select 1", SqlServerSchemaReader.viewBody("v", "create view [has as in it] as select 1"));
  }

  @Test
  @DisplayName("WITH options and column lists are dropped from the view header")
  void headerExtrasDropped() {
    assertEquals(
        "SELECT a FROM dbo.t",
        SqlServerSchemaReader.viewBody(
            "v", "CREATE VIEW v WITH SCHEMABINDING AS SELECT a FROM dbo.t"));
    assertEquals(
        "SELECT a, b FROM t",
        SqlServerSchemaReader.viewBody("v", "CREATE VIEW v (x, y) AS SELECT a, b FROM t"));
  }

  @Test
  @DisplayName("a definition without AS has no view body")
  void noAsNoBody() {
    assertNull(SqlServerSchemaReader.viewBody("v", "CREATE VIEW v"));
  }

  @Test
  @DisplayName("applyPrimaryKeys builds a composite primary key in index order")
  void primaryKeyFromIndexRows() {
    Schema schema = new Schema(null);
    schema.addTable(new Table(schema, "dbo", "t", null, null, false));
    SqlServerSchemaReader.applyPrimaryKeys(
        schema,
        List.of(
            new SchemaReader.IndexRow("t", "pk_t", true, true, "b", false, null),
            new SchemaReader.IndexRow("t", "pk_t", true, true, "a", false, null),
            new SchemaReader.IndexRow("t", "ix", false, false, "c", false, null)));

    Key primary = schema.getTable("t").getPrimaryKey();
    assertEquals(List.of("b", "a"), primary.getColumns().stream().map(KeyColumn::getName).toList());
    assertEquals(1, schema.getTable("t").getKeys().size());
  }
}
