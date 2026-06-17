package com.stano.schema.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("View")
class ViewTest {

  @ParameterizedTest
  @MethodSource("provideViewTestCases")
  @DisplayName(
      "constructor should set fields and getters should return them for various database types")
  void constructorShouldSetFields(String schemaName, String name, String sql, DatabaseType dbType) {
    View view = new View(schemaName, name, sql, dbType);

    assertEquals(view.getSchemaName(), schemaName);
    assertEquals(view.getName(), name);
    assertEquals(view.getSql(), sql);
    assertEquals(view.getDatabaseType(), dbType);
  }

  private static Stream<Object[]> provideViewTestCases() {
    return Stream.of(
        new Object[] {"public", "v_orders", "select * from orders", DatabaseType.POSTGRESQL},
        new Object[] {"dbo", "v_users", "SELECT * FROM dbo.users", DatabaseType.SQL_SERVER});
  }

  @Test
  @DisplayName("supports null SQL and still returns correct fields")
  void supportsNullSQL() {
    View view = new View("util", "v_empty", null, DatabaseType.H2);

    assertEquals(view.getSchemaName(), "util");
    assertEquals(view.getName(), "v_empty");
    assertNull(view.getSql());
    assertEquals(view.getDatabaseType(), DatabaseType.H2);
  }

  @Test
  @DisplayName("Schema.getViews should return DB-specific when present, otherwise generic")
  void schemaGetViewsShouldReturnDBSpecificWhenPresent() throws MalformedURLException {
    Schema schema = new Schema(new URL("https://example.com/schema.json"));

    schema.addView(new View("public", "sales", "SELECT 1 -- generic", null));
    schema.addView(new View("public", "sales", "SELECT 1 -- pg", DatabaseType.POSTGRESQL));
    schema.addView(new View("dbo", "sales", "SELECT 1 -- mssql", DatabaseType.SQL_SERVER));
    schema.addView(new View("public", "inventory", "SELECT 2 -- generic", null));

    var pgViews = schema.getViews(DatabaseType.POSTGRESQL);
    var msViews = schema.getViews(DatabaseType.SQL_SERVER);
    var h2Views = schema.getViews(DatabaseType.H2);

    assertEquals(pgViews.stream().map(View::getName).toList(), List.of("sales", "inventory"));
    assertEquals(
        pgViews.stream().map(View::getSql).toList(),
        List.of("SELECT 1 -- pg", "SELECT 2 -- generic"));

    assertEquals(msViews.stream().map(View::getName).toList(), List.of("sales", "inventory"));
    assertEquals(
        msViews.stream().map(View::getSql).toList(),
        List.of("SELECT 1 -- mssql", "SELECT 2 -- generic"));

    assertEquals(h2Views.stream().map(View::getName).toList(), List.of("sales", "inventory"));
    assertEquals(
        h2Views.stream().map(View::getSql).toList(),
        List.of("SELECT 1 -- generic", "SELECT 2 -- generic"));
  }

  @Test
  @DisplayName(
      "Schema.getViews should treat view names case-insensitively and preserve distinct order")
  void schemaGetViewsShouldTreatViewNamesCaseInsensitively() throws MalformedURLException {
    Schema schema = new Schema(new URL("https://example.com/schema.json"));

    schema.addView(new View("public", "AView", "A generic", null));
    schema.addView(new View("public", "bview", "B generic", null));
    schema.addView(new View("public", "aview", "A pg", DatabaseType.POSTGRESQL));
    schema.addView(new View("public", "CView", "C generic", null));

    var pgViews = schema.getViews(DatabaseType.POSTGRESQL);
    var h2Views = schema.getViews(DatabaseType.H2);

    assertEquals(pgViews.stream().map(View::getName).toList(), List.of("aview", "bview", "CView"));
    assertEquals(h2Views.stream().map(View::getName).toList(), List.of("AView", "bview", "CView"));

    assertEquals(
        pgViews.stream().map(View::getSql).toList(), List.of("A pg", "B generic", "C generic"));
    assertEquals(
        h2Views.stream().map(View::getSql).toList(),
        List.of("A generic", "B generic", "C generic"));
  }

  @Test
  @DisplayName("Schema.getViews should return an unmodifiable list")
  void schemaGetViewsShouldReturnUnmodifiableList() throws MalformedURLException {
    Schema schema = new Schema(new URL("https://example.com/schema.json"));
    schema.addView(new View("public", "only", "SELECT 1", null));

    assertThrows(
        UnsupportedOperationException.class,
        () -> schema.getViews(DatabaseType.POSTGRESQL).add(new View("public", "x", "y", null)));
  }
}
