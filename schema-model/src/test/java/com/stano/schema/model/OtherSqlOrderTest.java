package com.stano.schema.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class OtherSqlOrderTest {
  @Test
  @DisplayName("enum should contain exactly the expected values in order")
  void testEnumValues() {
    OtherSqlOrder[] values = OtherSqlOrder.values();
    assertEquals(2, values.length);
    assertEquals("BOTTOM", values[0].name());
    assertEquals("TOP", values[1].name());
  }

  @ParameterizedTest
  @MethodSource("valueOfProvider")
  @DisplayName("valueOf should return the correct enum constant for each name")
  void testValueOf(String name, OtherSqlOrder constant) {
    assertEquals(constant, OtherSqlOrder.valueOf(name));
    assertEquals(name, constant.name());
  }

  static Stream<Arguments> valueOfProvider() {
    return Stream.of(
        Arguments.of("BOTTOM", OtherSqlOrder.BOTTOM), Arguments.of("TOP", OtherSqlOrder.TOP));
  }

  @Test
  @DisplayName("all enum values should be unique")
  void testEnumValuesUnique() {
    OtherSqlOrder[] values = OtherSqlOrder.values();
    assertEquals(new HashSet<>(java.util.Arrays.asList(values)).size(), values.length);
  }

  @Test
  @DisplayName("integration: OtherSql should retain the provided OtherSqlOrder")
  void testOtherSqlIntegration() {
    OtherSql top = new OtherSql(DatabaseType.POSTGRESQL, OtherSqlOrder.TOP, "A;");
    OtherSql bottom = new OtherSql(DatabaseType.SQL_SERVER, OtherSqlOrder.BOTTOM, "B;");

    assertEquals(OtherSqlOrder.TOP, top.getOrder());
    assertEquals(OtherSqlOrder.BOTTOM, bottom.getOrder());
  }
}
