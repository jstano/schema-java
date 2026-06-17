package com.stano.schema.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class FunctionTest {
  @ParameterizedTest
  @MethodSource("constructorProvider")
  @DisplayName(
      "constructor should set fields and getters should return them for various database types")
  void testConstructor(String schemaName, String name, DatabaseType dbType, String sql) {
    Function fn = new Function(schemaName, name, dbType, sql);

    assertEquals(schemaName, fn.getSchemaName());
    assertEquals(name, fn.getName());
    assertEquals(dbType, fn.getDatabaseType());
    assertEquals(sql, fn.getSql());
  }

  static Stream<Arguments> constructorProvider() {
    return Stream.of(
        Arguments.of(
            "public",
            "fn_total",
            DatabaseType.POSTGRESQL,
            "create function fn_total() returns int as $$ select 1 $$;"),
        Arguments.of(
            "dbo",
            "fn_compute",
            DatabaseType.SQL_SERVER,
            "CREATE FUNCTION dbo.fn_compute() RETURNS INT AS BEGIN RETURN 42 END"));
  }

  @Test
  @DisplayName("supports null SQL and still returns correct fields")
  void testConstructorWithNullSql() {
    Function fn = new Function("util", "fn_empty", DatabaseType.H2, null);

    assertEquals("util", fn.getSchemaName());
    assertEquals("fn_empty", fn.getName());
    assertEquals(DatabaseType.H2, fn.getDatabaseType());
    assertNull(fn.getSql());
  }
}
