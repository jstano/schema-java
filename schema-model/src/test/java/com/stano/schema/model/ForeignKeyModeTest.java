package com.stano.schema.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.HashSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ForeignKeyModeTest {
  @Test
  @DisplayName("enum should contain exactly the expected values in order")
  void testEnumValues() {
    ForeignKeyMode[] values = ForeignKeyMode.values();
    assertEquals(3, values.length);
    assertEquals("NONE", values[0].name());
    assertEquals("RELATIONS", values[1].name());
    assertEquals("TRIGGERS", values[2].name());
  }

  @ParameterizedTest
  @MethodSource("valueOfProvider")
  @DisplayName("valueOf should return the correct enum constant for each name")
  void testValueOf(String name, ForeignKeyMode constant) {
    assertEquals(constant, ForeignKeyMode.valueOf(name));
    assertEquals(name, constant.name());
  }

  static Stream<Arguments> valueOfProvider() {
    return Stream.of(
        Arguments.of("NONE", ForeignKeyMode.NONE),
        Arguments.of("RELATIONS", ForeignKeyMode.RELATIONS),
        Arguments.of("TRIGGERS", ForeignKeyMode.TRIGGERS));
  }

  @Test
  @DisplayName("all enum values should be unique")
  void testEnumValuesUnique() {
    ForeignKeyMode[] values = ForeignKeyMode.values();
    assertEquals(new HashSet<>(java.util.Arrays.asList(values)).size(), values.length);
  }

  @Test
  @DisplayName("Schema should store and return ForeignKeyMode via getter/setter")
  void testSchemaForeignKeyMode() throws MalformedURLException {
    Schema schema = new Schema(URI.create("https://example.com/schema.json").toURL());

    schema.setForeignKeyMode(ForeignKeyMode.RELATIONS);
    assertEquals(ForeignKeyMode.RELATIONS, schema.getForeignKeyMode());

    schema.setForeignKeyMode(ForeignKeyMode.TRIGGERS);
    assertEquals(ForeignKeyMode.TRIGGERS, schema.getForeignKeyMode());
  }
}
