package com.stano.schema.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Schema")
class SchemaTest {

  @Test
  @DisplayName("constructor sets URL and default modes; setters update modes")
  void constructorSetsURLAndDefaultModes() throws MalformedURLException {
    URL url = URI.create("https://example.com/schema.json").toURL();
    Schema schema = new Schema(url);

    assertEquals(schema.getSchemaURL(), url);
    assertEquals(schema.getBooleanMode(), BooleanMode.NATIVE);
    assertEquals(schema.getForeignKeyMode(), ForeignKeyMode.RELATIONS);

    schema.setForeignKeyMode(ForeignKeyMode.NONE);
    schema.setBooleanMode(BooleanMode.YN);

    assertEquals(schema.getForeignKeyMode(), ForeignKeyMode.NONE);
    assertEquals(schema.getBooleanMode(), BooleanMode.YN);
  }

  @Test
  @DisplayName(
      "addTable populates tables and case-insensitive map; getTables unmodifiable; getTable throws"
          + " when missing")
  void addTablePopulatesTables() throws MalformedURLException {
    Schema schema = new Schema(URI.create("https://example.com/schema.json").toURL());
    Table tUsers = new Table(schema, "public", "Users", null, LockEscalation.AUTO, false);
    Table tOrders = new Table(schema, "sales", "orders", null, LockEscalation.AUTO, false);

    schema.addTable(tUsers);
    schema.addTable(tOrders);

    assertEquals(
        schema.getTables().stream().map(Table::getName).toList(), List.of("Users", "orders"));

    assertThrows(
        UnsupportedOperationException.class,
        () ->
            schema.getTables().add(new Table(schema, "x", "y", null, LockEscalation.AUTO, false)));

    assertEquals(schema.getTable("users"), tUsers);
    assertEquals(schema.getTable("ORDERS"), tOrders);

    assertFalse(schema.getOptionalTable("missing").isPresent());

    var ex = assertThrows(IllegalStateException.class, () -> schema.getTable("missing"));
    assertTrue(ex.getMessage().contains("Unable to locate a table with the name 'missing'"));
  }

  @Test
  @DisplayName(
      "addEnumType stores by name; getEnumType returns, getEnumTypes exposes values; throws when"
          + " missing")
  void addEnumTypeStoresByName() throws MalformedURLException {
    Schema schema = new Schema(URI.create("https://example.com/schema.json").toURL());
    EnumType etColor = new EnumType("Color");
    etColor.addValue(new EnumValue("RED", "R"));
    EnumType etStatus = new EnumType("Status");

    schema.addEnumType(etColor);
    schema.addEnumType(etStatus);

    assertEquals(schema.getEnumType("Color"), etColor);
    assertEquals(schema.getEnumType("Status"), etStatus);

    Set<EnumType> enumTypes = new HashSet<>(schema.getEnumTypes());
    assertEquals(enumTypes, new HashSet<>(List.of(etColor, etStatus)));

    var ex = assertThrows(IllegalStateException.class, () -> schema.getEnumType("Missing"));
    assertTrue(ex.getMessage().contains("Unable to locate an enum type with name 'Missing'"));
  }

  @Test
  @DisplayName("sortTablesByName orders tables lexicographically by getName()")
  void sortTablesByNameOrdersTablesLexicographically() throws MalformedURLException {
    Schema schema = new Schema(URI.create("https://example.com/schema.json").toURL());
    Table t3 = new Table(schema, "public", "zeta", null, LockEscalation.AUTO, false);
    Table t1 = new Table(schema, "public", "alpha", null, LockEscalation.AUTO, false);
    Table t2 = new Table(schema, "public", "Beta", null, LockEscalation.AUTO, false);

    schema.addTable(t3);
    schema.addTable(t1);
    schema.addTable(t2);

    assertEquals(
        schema.getTables().stream().map(Table::getName).toList(), List.of("zeta", "alpha", "Beta"));

    schema.sortTablesByName();

    assertEquals(
        schema.getTables().stream().map(Table::getName).toList(), List.of("Beta", "alpha", "zeta"));
  }

  @Test
  @DisplayName(
      "getViews returns unmodifiable list and preserves distinct-name order for simple case")
  void getViewsReturnsUnmodifiableList() throws MalformedURLException {
    Schema schema = new Schema(URI.create("https://example.com/schema.json").toURL());
    schema.addView(new View("public", "A", "ga", null));
    schema.addView(new View("public", "a", "pg", DatabaseType.POSTGRESQL));
    schema.addView(new View("public", "B", "gb", null));

    var pgViews = schema.getViews(DatabaseType.POSTGRESQL);
    var h2Views = schema.getViews(DatabaseType.H2);

    assertEquals(pgViews.stream().map(View::getName).toList(), List.of("a", "B"));
    assertEquals(h2Views.stream().map(View::getName).toList(), List.of("A", "B"));

    assertThrows(
        UnsupportedOperationException.class, () -> pgViews.add(new View("public", "C", "x", null)));
  }

  @Test
  @DisplayName("getViews prefers DB-specific over generic for same logical name")
  void getViewsPrefersDBSpecificOverGeneric() throws MalformedURLException {
    Schema schema = new Schema(URI.create("https://example.com/schema.json").toURL());
    schema.addView(new View("public", "sales", "generic", null));
    schema.addView(new View("public", "sales", "pgsql", DatabaseType.POSTGRESQL));
    schema.addView(new View("public", "sales", "mssql", DatabaseType.SQL_SERVER));

    var pg = schema.getViews(DatabaseType.POSTGRESQL);
    var ms = schema.getViews(DatabaseType.SQL_SERVER);
    var h2 = schema.getViews(DatabaseType.H2);

    assertEquals(pg.stream().map(View::getSql).toList(), List.of("pgsql"));
    assertEquals(ms.stream().map(View::getSql).toList(), List.of("mssql"));
    assertEquals(h2.stream().map(View::getSql).toList(), List.of("generic"));
  }

  @Test
  @DisplayName(
      "buildReverseRelations adds reverse on parent with disableUsageChecking=false (in"
          + " SchemaSpec)")
  void buildReverseRelationsAddsReverseOnParent() throws MalformedURLException {
    Schema schema = new Schema(URI.create("https://example.com/schema.json").toURL());
    Table parent = new Table(schema, "public", "users", null, LockEscalation.AUTO, false);
    Table child = new Table(schema, "public", "orders", null, LockEscalation.AUTO, false);
    parent.getColumns().add(new Column("id", ColumnType.SEQUENCE, 0, true));
    child.getColumns().add(new Column("user_id", ColumnType.INT, 0, false));
    child
        .getRelations()
        .add(new Relation("orders", "user_id", "users", "id", RelationType.CASCADE, true));
    schema.addTable(parent);
    schema.addTable(child);

    schema.buildReverseRelations();

    assertEquals(parent.getReverseRelations().size(), 1);
    assertEquals(parent.getReverseRelations().get(0).getFromTableName(), "users");
    assertEquals(parent.getReverseRelations().get(0).getFromColumnName(), "id");
    assertEquals(parent.getReverseRelations().get(0).getToTableName(), "orders");
    assertEquals(parent.getReverseRelations().get(0).getToColumnName(), "user_id");
    assertEquals(parent.getReverseRelations().get(0).getType(), RelationType.CASCADE);
    assertFalse(parent.getReverseRelations().get(0).isDisableUsageChecking());
  }

  @Test
  @DisplayName("buildReverseRelations preserves all column pairs of a composite relation")
  void buildReverseRelationsPreservesCompositeColumnPairs() throws MalformedURLException {
    Schema schema = new Schema(URI.create("https://example.com/schema.json").toURL());
    Table parent = new Table(schema, "public", "properties", null, LockEscalation.AUTO, false);
    Table child = new Table(schema, "public", "assignments", null, LockEscalation.AUTO, false);
    parent.getColumns().add(new Column("id", ColumnType.SEQUENCE, 0, true));
    parent.getColumns().add(new Column("org_id", ColumnType.INT, 0, true));
    child.getColumns().add(new Column("property_id", ColumnType.INT, 0, false));
    child.getColumns().add(new Column("org_id", ColumnType.INT, 0, false));
    child
        .getRelations()
        .add(
            Relation.composite(
                "assignments",
                "properties",
                List.of(new ColumnPair("property_id", "id"), new ColumnPair("org_id", "org_id")),
                RelationType.CASCADE,
                false));
    schema.addTable(parent);
    schema.addTable(child);

    schema.buildReverseRelations();

    assertEquals(1, parent.getReverseRelations().size());
    Relation reverse = parent.getReverseRelations().get(0);
    assertTrue(reverse.isComposite());
    assertEquals(reverse.getFromTableName(), "properties");
    assertEquals(reverse.getToTableName(), "assignments");
    List<ColumnPair> pairs = reverse.getColumnPairs();
    assertEquals(2, pairs.size());
    assertEquals(pairs.get(0).getFromColumnName(), "id");
    assertEquals(pairs.get(0).getToColumnName(), "property_id");
    assertEquals(pairs.get(1).getFromColumnName(), "org_id");
    assertEquals(pairs.get(1).getToColumnName(), "org_id");
  }

  @Test
  @DisplayName("validate flags SETNULL on required from-column (in SchemaSpec)")
  void validateFlagsSETNULLOnRequiredFromColumn() throws MalformedURLException {
    Schema schema = new Schema(URI.create("https://example.com/schema.json").toURL());
    Table parent = new Table(schema, "public", "parent", null, LockEscalation.AUTO, false);
    Table childReq = new Table(schema, "public", "child_req", null, LockEscalation.AUTO, false);
    parent.getColumns().add(new Column("id", ColumnType.SEQUENCE, 0, true));
    childReq.getColumns().add(new Column("parent_id", ColumnType.INT, 0, true));
    childReq
        .getRelations()
        .add(new Relation("child_req", "parent_id", "parent", "id", RelationType.SETNULL, false));
    schema.addTable(parent);
    schema.addTable(childReq);

    var errors = schema.validate();

    assertEquals(errors.size(), 1);
    assertTrue(errors.get(0).contains("child_req.parent_id is required"));
    assertTrue(errors.get(0).contains("relation specifies setnull"));
  }

  private static Table tableOf(Schema schema, String name, Column... columns) {
    Table table = new Table(schema, "public", name, null, LockEscalation.AUTO, false);
    table.getColumns().addAll(List.of(columns));
    schema.addTable(table);
    return table;
  }

  @Test
  @DisplayName("validate reports duplicate column names, case-insensitively")
  void validateReportsDuplicateColumnNames() {
    Schema schema = new Schema(null);
    tableOf(
        schema,
        "t",
        new Column("Name", ColumnType.INT, 0, false),
        new Column("name", ColumnType.INT, 0, false));

    List<String> errors = schema.validate();

    assertEquals(1, errors.size(), errors.toString());
    assertTrue(errors.get(0).contains("duplicate column name 'name'"), errors.get(0));
  }

  @Test
  @DisplayName("validate reports key and index columns that are not on the table")
  void validateReportsMissingKeyColumns() {
    Schema schema = new Schema(null);
    Table table = tableOf(schema, "t", new Column("id", ColumnType.INT, 0, true));
    table.getKeys().add(new Key(KeyType.PRIMARY, List.of(new KeyColumn("Idd"))));
    table.getIndexes().add(new Key(KeyType.INDEX, List.of(new KeyColumn("nope"))));

    List<String> errors = schema.validate();

    assertEquals(2, errors.size(), errors.toString());
    assertTrue(errors.get(0).contains("a key on column 'Idd'"), errors.get(0));
    assertTrue(errors.get(1).contains("an index on column 'nope'"), errors.get(1));
  }

  @Test
  @DisplayName("validate reports a missing source column for every relation type")
  void validateReportsMissingSourceColumnForAnyRelationType() {
    Schema schema = new Schema(null);
    tableOf(schema, "parent", new Column("id", ColumnType.INT, 0, true));
    Table child = tableOf(schema, "child", new Column("x", ColumnType.INT, 0, false));
    child
        .getRelations()
        .add(new Relation("child", "missing", "parent", "id", RelationType.CASCADE, false));

    List<String> errors = schema.validate();

    assertEquals(1, errors.size(), errors.toString());
    assertTrue(errors.get(0).contains("child.missing does not exist"), errors.get(0));
  }

  @Test
  @DisplayName("validate checks every column pair of a composite relation, including SETNULL")
  void validateChecksEveryCompositePair() {
    Schema schema = new Schema(null);
    tableOf(
        schema,
        "parent",
        new Column("a", ColumnType.INT, 0, true),
        new Column("b", ColumnType.INT, 0, true));
    Table child =
        tableOf(
            schema,
            "child",
            new Column("a", ColumnType.INT, 0, true),
            new Column("b", ColumnType.INT, 0, false));
    child
        .getRelations()
        .add(
            Relation.composite(
                "child",
                "parent",
                List.of(new ColumnPair("a", "a"), new ColumnPair("b", "b")),
                RelationType.SETNULL,
                false));

    List<String> errors = schema.validate();

    assertEquals(1, errors.size(), errors.toString());
    assertTrue(errors.get(0).contains("child.a is required"), errors.get(0));
  }

  @Test
  @DisplayName("validate reports a relation whose target table or target column does not exist")
  void validateReportsMissingRelationTarget() {
    Schema schema = new Schema(null);
    tableOf(schema, "parent", new Column("id", ColumnType.INT, 0, true));
    Table child = tableOf(schema, "child", new Column("parent_id", ColumnType.INT, 0, false));
    child
        .getRelations()
        .add(new Relation("child", "parent_id", "parent", "idd", RelationType.CASCADE, false));
    child
        .getRelations()
        .add(new Relation("child", "parent_id", "ghost", "id", RelationType.CASCADE, false));

    List<String> errors = schema.validate();

    assertEquals(2, errors.size(), errors.toString());
    assertTrue(errors.get(0).contains("parent has no column 'idd'"), errors.get(0));
    assertTrue(errors.get(1).contains("relation to 'ghost' which does not exist"), errors.get(1));
  }

  @Test
  @DisplayName("a well-formed schema still validates clean")
  void validSchemaHasNoErrors() {
    Schema schema = new Schema(null);
    Table parent = tableOf(schema, "parent", new Column("id", ColumnType.INT, 0, true));
    parent.getKeys().add(new Key(KeyType.PRIMARY, List.of(new KeyColumn("ID"))));
    Table child = tableOf(schema, "child", new Column("parent_id", ColumnType.INT, 0, false));
    child
        .getRelations()
        .add(new Relation("child", "parent_id", "parent", "id", RelationType.SETNULL, false));

    assertTrue(schema.validate().isEmpty(), schema.validate().toString());
  }

  @Test
  @DisplayName("a relation to a missing table is reported by validate instead of failing the build")
  void relationToMissingTableIsReportedByValidate() {
    Schema schema = new Schema(null);
    Table child = new Table(null, "app", "child", null, null, false);
    child.getColumns().add(new Column("parent_id", ColumnType.INT, 0, false));
    child
        .getRelations()
        .add(new Relation("child", "parent_id", "ghost", "id", RelationType.ENFORCE, false));
    schema.addTable(child);

    schema.buildReverseRelations();

    assertTrue(
        schema.validate().stream().anyMatch(e -> e.contains("relation to 'ghost' which does not")),
        schema.validate().toString());
  }

  @Test
  @DisplayName(
      "validate rejects a full generated clause but accepts names that merely start with it")
  void validateGeneratedPrefix() {
    Schema schema = new Schema(null);
    tableOf(
        schema,
        "t",
        new Column(
            "a",
            ColumnType.INT,
            0,
            0,
            false,
            null,
            null,
            "generated always as (1)",
            null,
            null,
            null,
            null),
        new Column(
            "b",
            ColumnType.INT,
            0,
            0,
            false,
            null,
            null,
            "generated_total * 2",
            null,
            null,
            null,
            null));

    List<String> errors = schema.validate();

    assertEquals(1, errors.size(), errors.toString());
    assertTrue(errors.get(0).contains("t.a"), errors.get(0));
  }
}
