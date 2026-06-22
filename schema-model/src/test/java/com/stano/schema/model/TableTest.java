package com.stano.schema.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Table")
class TableTest {

  @Test
  @DisplayName("constructor should store fields and toString returns the table name")
  void constructorShouldStoreFieldsAndToStringReturnsTheTableName() throws MalformedURLException {
    Schema schema = new Schema(URI.create("https://example.com/schema.json").toURL());

    Table table = new Table(schema, "public", "users", "exported_at", LockEscalation.AUTO, true);

    assertEquals(table.getSchema(), schema);
    assertEquals(table.getSchemaName(), "public");
    assertEquals(table.getName(), "users");
    assertEquals(table.getExportDateColumn(), "exported_at");
    assertEquals(table.getLockEscalation(), LockEscalation.AUTO);
    assertTrue(table.isNoExport());
    assertEquals(table.toString(), "users");
  }

  @Test
  @DisplayName("getColumn should populate lazy map and be case-insensitive; hasColumn mirrors that")
  void getColumnShouldPopulateLazyMapAndBeCaseInsensitive() throws MalformedURLException {
    Schema schema = new Schema(URI.create("https://example.com/schema.json").toURL());
    Table table = new Table(schema, "public", "accounts", null, LockEscalation.AUTO, false);

    table
        .getColumns()
        .addAll(
            List.of(
                new Column("Id", ColumnType.SEQUENCE, 0, true),
                new Column("userName", ColumnType.VARCHAR, 50, false)));

    assertTrue(table.hasColumn("id"));
    assertTrue(table.hasColumn("USERNAME"));
    assertFalse(table.hasColumn("missing"));

    assertEquals(table.getColumn("ID").getName(), "Id");
    assertEquals(table.getColumn("username").getName(), "userName");
  }

  @Test
  @DisplayName("getIdentityColumn returns first SEQUENCE or LONGSEQUENCE if present")
  void getIdentityColumnReturnsFirstSequenceOrLongsequenceIfPresent() throws MalformedURLException {
    Schema schema = new Schema(URI.create("https://example.com/schema.json").toURL());

    Table t1 = new Table(schema, "public", "t1", null, LockEscalation.AUTO, false);
    t1.getColumns()
        .addAll(
            List.of(
                new Column("code", ColumnType.VARCHAR, 20, false),
                new Column("id", ColumnType.SEQUENCE, 0, true),
                new Column("id2", ColumnType.LONGSEQUENCE, 0, true)));

    Table t2 = new Table(schema, "public", "t2", null, LockEscalation.AUTO, false);
    t2.getColumns()
        .addAll(
            List.of(
                new Column("code", ColumnType.VARCHAR, 20, false),
                new Column("id2", ColumnType.LONGSEQUENCE, 0, true)));

    Table t3 = new Table(schema, "public", "t3", null, LockEscalation.AUTO, false);
    t3.getColumns().add(new Column("code", ColumnType.VARCHAR, 20, false));

    assertEquals(t1.getIdentityColumn().getName(), "id");
    assertEquals(t2.getIdentityColumn().getName(), "id2");
    assertNull(t3.getIdentityColumn());
  }

  @Test
  @DisplayName("getPrimaryKey and getPrimaryKeyColumns return correct info or null when absent")
  void getPrimaryKeyAndGetPrimaryKeyColumnsReturnCorrectInfoOrNullWhenAbsent()
      throws MalformedURLException {
    Schema schema = new Schema(URI.create("https://example.com/schema.json").toURL());
    Table table = new Table(schema, "public", "orders", null, LockEscalation.AUTO, false);

    table
        .getColumns()
        .addAll(
            List.of(
                new Column("id", ColumnType.SEQUENCE, 0, true),
                new Column("tenant_id", ColumnType.INT, 0, true),
                new Column("code", ColumnType.VARCHAR, 50, false)));

    table.getKeys().add(new Key(KeyType.INDEX, List.of(new KeyColumn("code"))));
    Key pk = new Key(KeyType.PRIMARY, List.of(new KeyColumn("id"), new KeyColumn("tenant_id")));
    table.getKeys().add(pk);

    assertEquals(table.getPrimaryKey(), pk);
    assertEquals(table.getPrimaryKeyColumns(), List.of("id", "tenant_id"));

    table.getKeys().clear();

    assertNull(table.getPrimaryKey());
    assertNull(table.getPrimaryKeyColumns());
  }

  @Test
  @DisplayName("hasOption uses identity semantics against the live options list")
  void hasOptionUsesIdentitySemantics() throws MalformedURLException {
    Schema schema = new Schema(URI.create("https://example.com/schema.json").toURL());
    Table table = new Table(schema, "public", "items", null, LockEscalation.AUTO, false);

    assertTrue(table.getOptions().isEmpty());
    assertFalse(table.hasOption(TableOption.DATA));

    table.getOptions().add(TableOption.DATA);

    assertTrue(table.hasOption(TableOption.DATA));
    assertFalse(table.hasOption(TableOption.NO_EXPORT));
  }

  @Test
  @DisplayName(
      "hasColumnConstraints and getColumnsWithCheckConstraints depend on BooleanMode and explicit"
          + " constraints")
  void hasColumnConstraintsAndGetColumnsWithCheckConstraintsDependOnBooleanMode()
      throws MalformedURLException {
    Schema schema = new Schema(URI.create("https://example.com/schema.json").toURL());
    Table table = new Table(schema, "public", "orders", null, LockEscalation.AUTO, false);

    Column boolCol = new Column("flag", ColumnType.BOOLEAN, 0, false);
    Column checked = new Column("amt", ColumnType.INT, 0, false, "amt > 0");
    Column ranged =
        new Column(
            "price", ColumnType.DECIMAL, 10, 2, false, null, null, null, "0", "100", null, null);
    table.getColumns().addAll(List.of(boolCol, checked, ranged));

    assertTrue(table.hasColumnConstraints(BooleanMode.NATIVE));
    var nativeConstraints = table.getColumnsWithCheckConstraints(BooleanMode.NATIVE);
    assertEquals(nativeConstraints.stream().map(Column::getName).toList(), List.of("amt", "price"));

    assertTrue(table.hasColumnConstraints(BooleanMode.YN));
    var ynConstraints = table.getColumnsWithCheckConstraints(BooleanMode.YN);
    assertEquals(
        ynConstraints.stream().map(Column::getName).toList(), List.of("flag", "amt", "price"));
  }

  @Test
  @DisplayName(
      "getColumnRelation should return matching relation by from-column name case-insensitively or"
          + " null")
  void getColumnRelationShouldReturnMatchingRelation() throws MalformedURLException {
    Schema schema = new Schema(URI.create("https://example.com/schema.json").toURL());
    Table table = new Table(schema, "public", "child", null, LockEscalation.AUTO, false);

    table
        .getColumns()
        .addAll(
            List.of(
                new Column("parent_id", ColumnType.INT, 0, false),
                new Column("other", ColumnType.VARCHAR, 10, false)));

    table
        .getRelations()
        .add(new Relation("child", "Parent_Id", "parent", "id", RelationType.CASCADE, false));

    assertEquals(
        table.getColumnRelation(new Column("PARENT_id", ColumnType.INT, 0, false)).getType(),
        RelationType.CASCADE);
    assertNull(table.getColumnRelation(new Column("missing", ColumnType.INT, 0, false)));
  }

  @Test
  @DisplayName("getIndexes should expose a live mutable list and preserve insertion order")
  void getIndexesShouldExposeLiveMutableList() throws MalformedURLException {
    Schema schema = new Schema(URI.create("https://example.com/schema.json").toURL());
    Table table = new Table(schema, "public", "idx_test", null, LockEscalation.AUTO, false);

    assertTrue(table.getIndexes().isEmpty());

    Key k1 = new Key(KeyType.INDEX, List.of(new KeyColumn("code")));
    Key k2 = new Key(KeyType.INDEX, List.of(new KeyColumn("tenant_id"), new KeyColumn("code")));
    table.getIndexes().add(k1);
    table.getIndexes().add(k2);

    assertEquals(table.getIndexes().get(0), k1);
    assertEquals(table.getIndexes().get(1), k2);

    table.getIndexes().remove(0);

    assertEquals(table.getIndexes().size(), 1);
    assertEquals(table.getIndexes().get(0), k2);
  }

  @Test
  @DisplayName("getConstraints should expose a live mutable list that reflects changes")
  void getConstraintsShouldExposeLiveMutableList() throws MalformedURLException {
    Schema schema = new Schema(URI.create("https://example.com/schema.json").toURL());
    Table table = new Table(schema, "public", "con_test", null, LockEscalation.AUTO, false);

    assertTrue(table.getConstraints().isEmpty());

    Constraint c1 = new Constraint("ck_positive", "amount > 0", DatabaseType.POSTGRESQL);
    Constraint c2 = new Constraint("ck_not_null", "col is not null", DatabaseType.SQL_SERVER);
    table.getConstraints().addAll(List.of(c1, c2));

    assertEquals(
        table.getConstraints().stream().map(Constraint::getName).toList(),
        List.of("ck_positive", "ck_not_null"));
    assertEquals(
        table.getConstraints().stream().map(Constraint::getSql).toList(),
        List.of("amount > 0", "col is not null"));

    table.getConstraints().remove(0);

    assertEquals(
        table.getConstraints().stream().map(Constraint::getName).toList(), List.of("ck_not_null"));
  }

  @Test
  @DisplayName(
      "getAggregations should expose a live list and store Aggregation instances correctly")
  void getAggregationsShouldExposeLiveList() throws MalformedURLException {
    Schema schema = new Schema(URI.create("https://example.com/schema.json").toURL());
    Table table = new Table(schema, "public", "agg_test", null, LockEscalation.AUTO, false);

    var cols = List.of(new AggregationColumn(AggregationType.SUM, "amount", "total_amount"));
    var groups = List.of(new AggregationGroup("country", null, "country"));
    Aggregation agg =
        new Aggregation(
            "agg_sales",
            "sale_date",
            "status='CONFIRMED'",
            "updated_at",
            AggregationFrequency.MONTHLY,
            cols,
            groups);

    table.getAggregations().add(agg);

    assertEquals(table.getAggregations().size(), 1);
    assertEquals(table.getAggregations().get(0).getDestinationTable(), "agg_sales");
    assertEquals(table.getAggregations().get(0).getDateColumn(), "sale_date");
    assertEquals(table.getAggregations().get(0).getTimeStampColumn(), "updated_at");
    assertEquals(
        table.getAggregations().get(0).getAggregationFrequency(), AggregationFrequency.MONTHLY);
    assertEquals(table.getAggregations().get(0).getAggregationColumns().size(), 1);
    assertEquals(
        table.getAggregations().get(0).getAggregationColumns().get(0).getAggregationType(),
        AggregationType.SUM);
    assertEquals(
        table.getAggregations().get(0).getAggregationGroups().stream()
            .map(AggregationGroup::getDestination)
            .toList(),
        List.of("country"));

    table.getAggregations().remove(0);

    assertTrue(table.getAggregations().isEmpty());
  }

  @Test
  @DisplayName(
      "hasColumnConstraints returns false when no columns require constraints (false path)")
  void hasColumnConstraintsReturnsFalseWhenNoColumnsRequireConstraints()
      throws MalformedURLException {
    Schema schema = new Schema(URI.create("https://example.com/schema.json").toURL());
    Table t1 = new Table(schema, "public", "no_checks_native", null, LockEscalation.AUTO, false);
    Table t2 = new Table(schema, "public", "no_checks_any", null, LockEscalation.AUTO, false);

    t1.getColumns()
        .addAll(
            List.of(
                new Column("flag", ColumnType.BOOLEAN, 0, false),
                new Column("code", ColumnType.VARCHAR, 50, false)));

    t2.getColumns()
        .addAll(
            List.of(
                new Column("a", ColumnType.INT, 0, false),
                new Column("b", ColumnType.VARCHAR, 10, false)));

    assertFalse(t1.hasColumnConstraints(BooleanMode.NATIVE));
    assertFalse(t2.hasColumnConstraints(BooleanMode.NATIVE));
    assertFalse(t2.hasColumnConstraints(BooleanMode.YN));
  }
}
