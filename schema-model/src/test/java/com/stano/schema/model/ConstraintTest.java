package com.stano.schema.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ConstraintTest {
  @ParameterizedTest
  @MethodSource("constraintProvider")
  @DisplayName(
      "constructor should set fields and getters should return them for various database types")
  void testConstructor(String name, String sql, DatabaseType dbType) {
    Constraint constraint = new Constraint(name, sql, dbType);
    assertEquals(name, constraint.getName());
    assertEquals(sql, constraint.getSql());
    assertEquals(dbType, constraint.getDatabaseType());
  }

  static Stream<Arguments> constraintProvider() {
    return Stream.of(
        Arguments.of("ck_positive", "amount > 0", DatabaseType.SQL_SERVER),
        Arguments.of("ck_not_null", "col is not null", DatabaseType.POSTGRESQL));
  }

  @org.junit.jupiter.api.Test
  @DisplayName("supports null sql value and still returns correct fields")
  void testConstructorWithNullSql() {
    Constraint constraint = new Constraint("ck_empty", null, DatabaseType.H2);
    assertEquals("ck_empty", constraint.getName());
    assertNull(constraint.getSql());
    assertEquals(DatabaseType.H2, constraint.getDatabaseType());
  }
}
