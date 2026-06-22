package com.stano.schema.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class InitialDataTest {
  @ParameterizedTest
  @MethodSource("constructorProvider")
  @DisplayName(
      "constructor should set fields and getters should return them for various database types")
  void testConstructor(String sql, DatabaseType dbType) {
    InitialData init = new InitialData(sql, dbType);

    assertEquals(sql, init.getSql());
    assertEquals(dbType, init.getDatabaseType());
  }

  static Stream<Arguments> constructorProvider() {
    return Stream.of(
        Arguments.of("insert into t(a) values (1)", DatabaseType.POSTGRESQL),
        Arguments.of("INSERT INTO t(a) VALUES (42);", DatabaseType.SQL_SERVER));
  }

  @Test
  @DisplayName("supports null sql value and still returns correct fields")
  void testConstructorWithNullSql() {
    InitialData init = new InitialData(null, DatabaseType.H2);

    assertNull(init.getSql());
    assertEquals(DatabaseType.H2, init.getDatabaseType());
  }

  @Test
  @DisplayName("Table.getInitialData exposes a live mutable list (current contract)")
  void testTableInitialDataLiveList() throws MalformedURLException {
    Schema schema = new Schema(URI.create("https://example.com/schema.json").toURL());
    Table table = new Table(schema, "public", "orders", null, LockEscalation.AUTO, false);

    assertTrue(table.getInitialData().isEmpty());

    table
        .getInitialData()
        .add(new InitialData("insert into orders(id) values (1)", DatabaseType.POSTGRESQL));
    table
        .getInitialData()
        .add(new InitialData("insert into orders(id) values (2)", DatabaseType.POSTGRESQL));

    List<InitialData> data = table.getInitialData();
    assertEquals(2, data.size());
    assertEquals("insert into orders(id) values (1)", data.get(0).getSql());
    assertEquals("insert into orders(id) values (2)", data.get(1).getSql());

    table.getInitialData().remove(0);

    assertEquals(1, table.getInitialData().size());
    assertEquals("insert into orders(id) values (2)", table.getInitialData().get(0).getSql());
  }
}
