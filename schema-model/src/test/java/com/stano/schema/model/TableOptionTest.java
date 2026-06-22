package com.stano.schema.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.HashSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TableOptionTest {
  @Test
  @DisplayName("enum should contain exactly the expected values in order")
  void testEnumValues() {
    TableOption[] values = TableOption.values();
    assertEquals(3, values.length);
    assertEquals("DATA", values[0].name());
    assertEquals("NO_EXPORT", values[1].name());
    assertEquals("COMPRESS", values[2].name());
  }

  @ParameterizedTest
  @MethodSource("valueOfProvider")
  @DisplayName("valueOf should return the correct enum constant for each name")
  void testValueOf(String name, TableOption constant) {
    assertEquals(constant, TableOption.valueOf(name));
    assertEquals(name, constant.name());
  }

  static Stream<Arguments> valueOfProvider() {
    return Stream.of(
        Arguments.of("DATA", TableOption.DATA),
        Arguments.of("NO_EXPORT", TableOption.NO_EXPORT),
        Arguments.of("COMPRESS", TableOption.COMPRESS));
  }

  @Test
  @DisplayName("all enum values should be unique")
  void testEnumValuesUnique() {
    TableOption[] values = TableOption.values();
    assertEquals(new HashSet<>(java.util.Arrays.asList(values)).size(), values.length);
  }

  @Test
  @DisplayName("integration with Table: getOptions is live list and hasOption checks identity")
  void testTableOptions() throws MalformedURLException {
    Schema schema = new Schema(URI.create("https://example.com/schema.json").toURL());
    Table table = new Table(schema, "public", "orders", null, LockEscalation.AUTO, false);

    assertTrue(table.getOptions().isEmpty());
    assertFalse(table.hasOption(TableOption.DATA));
    assertFalse(table.hasOption(TableOption.NO_EXPORT));
    assertFalse(table.hasOption(TableOption.COMPRESS));

    table.getOptions().add(TableOption.DATA);
    table.getOptions().add(TableOption.COMPRESS);

    assertEquals(2, table.getOptions().size());
    assertTrue(table.hasOption(TableOption.DATA));
    assertFalse(table.hasOption(TableOption.NO_EXPORT));
    assertTrue(table.hasOption(TableOption.COMPRESS));

    table.getOptions().remove(TableOption.DATA);

    assertEquals(1, table.getOptions().size());
    assertFalse(table.hasOption(TableOption.DATA));
    assertTrue(table.hasOption(TableOption.COMPRESS));
  }
}
