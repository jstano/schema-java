package com.stano.schema.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    assertNull(schema.getForeignKeyMode());

    schema.setForeignKeyMode(ForeignKeyMode.RELATIONS);
    schema.setBooleanMode(BooleanMode.YN);

    assertEquals(schema.getForeignKeyMode(), ForeignKeyMode.RELATIONS);
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
}
