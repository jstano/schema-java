package com.stano.schema.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DatabaseTypeTest {
  @Test
  @DisplayName("if the values in the TargetData enum changes, we need to adjust these tests")
  void testEnumSize() {
    assertEquals(3, DatabaseType.values().length);
  }

  @ParameterizedTest
  @MethodSource("getDatabaseTypesProvider")
  @DisplayName("getDatabaseTypes should return the correct results")
  void testGetDatabaseTypes(String targetDatabasesStr, Set<DatabaseType> expected) {
    assertEquals(expected, DatabaseType.getDatabaseTypes(targetDatabasesStr));
  }

  static Stream<Arguments> getDatabaseTypesProvider() {
    return Stream.of(
        Arguments.of(null, new HashSet<>()),
        Arguments.of("", new HashSet<>()),
        Arguments.of("sqlserver", Set.of(DatabaseType.SQL_SERVER)),
        Arguments.of(
            "sqlserver,POSTGRESQL", Set.of(DatabaseType.SQL_SERVER, DatabaseType.POSTGRESQL)));
  }

  @ParameterizedTest
  @MethodSource("getMaxKeyNameLengthProvider")
  @DisplayName("getMaxKeyNameLength should return the correct value for each type")
  void testGetMaxKeyNameLength(DatabaseType targetDatabase, int maxKeyNameLength) {
    assertEquals(maxKeyNameLength, targetDatabase.getMaxKeyNameLength());
  }

  static Stream<Arguments> getMaxKeyNameLengthProvider() {
    return Stream.of(
        Arguments.of(DatabaseType.H2, 64),
        Arguments.of(DatabaseType.POSTGRESQL, 63),
        Arguments.of(DatabaseType.SQL_SERVER, 32));
  }

  @ParameterizedTest
  @MethodSource("getStatementSeparatorProvider")
  @DisplayName("statement separator should be correct for each DB")
  void testGetStatementSeparator(DatabaseType db, String sep) {
    assertEquals(sep, db.getStatementSeparator());
  }

  static Stream<Arguments> getStatementSeparatorProvider() {
    return Stream.of(
        Arguments.of(DatabaseType.H2, ";"),
        Arguments.of(DatabaseType.POSTGRESQL, ";"),
        Arguments.of(DatabaseType.SQL_SERVER, "\nGO"));
  }

  @ParameterizedTest
  @MethodSource("supportsTriggerProvider")
  @DisplayName("supportsTriggers flag should be correct for each DB")
  void testSupportsTriggers(DatabaseType db, boolean supports) {
    assertEquals(supports, db.isSupportsTriggers());
  }

  static Stream<Arguments> supportsTriggerProvider() {
    return Stream.of(
        Arguments.of(DatabaseType.H2, false),
        Arguments.of(DatabaseType.POSTGRESQL, true),
        Arguments.of(DatabaseType.SQL_SERVER, true));
  }

  @ParameterizedTest
  @MethodSource("valueOfProvider")
  @DisplayName("valueOf should resolve names case-sensitively (upper) and name() round-trips")
  void testValueOf(String name) {
    assertEquals(name, DatabaseType.valueOf(name).name());
  }

  static Stream<Arguments> valueOfProvider() {
    return Stream.of(DatabaseType.values()).map(db -> Arguments.of(db.name()));
  }
}
