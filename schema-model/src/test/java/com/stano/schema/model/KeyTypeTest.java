package com.stano.schema.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.HashSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class KeyTypeTest {
  @Test
  @DisplayName("enum should contain exactly the expected values in order")
  void testEnumValues() {
    KeyType[] values = KeyType.values();
    assertEquals(3, values.length);
    assertEquals("PRIMARY", values[0].name());
    assertEquals("UNIQUE", values[1].name());
    assertEquals("INDEX", values[2].name());
  }

  @ParameterizedTest
  @MethodSource("valueOfProvider")
  @DisplayName("valueOf should return the correct enum constant for each name")
  void testValueOf(String name, KeyType constant) {
    assertEquals(constant, KeyType.valueOf(name));
    assertEquals(name, constant.name());
  }

  static Stream<Arguments> valueOfProvider() {
    return Stream.of(
        Arguments.of("PRIMARY", KeyType.PRIMARY),
        Arguments.of("UNIQUE", KeyType.UNIQUE),
        Arguments.of("INDEX", KeyType.INDEX));
  }

  @Test
  @DisplayName("all enum values should be unique")
  void testEnumValuesUnique() {
    KeyType[] values = KeyType.values();
    assertEquals(new HashSet<>(java.util.Arrays.asList(values)).size(), values.length);
  }

  @Test
  @DisplayName(
      "integration: Keys created with each KeyType reflect their type and Table.getPrimaryKey uses"
          + " PRIMARY")
  void testTablePrimaryKey() throws MalformedURLException {
    Schema schema = new Schema(URI.create("https://example.com/schema.json").toURL());
    Table table = new Table(schema, "public", "customer", null, LockEscalation.AUTO, false);

    table
        .getColumns()
        .addAll(
            java.util.Arrays.asList(
                new Column("id", ColumnType.SEQUENCE, 0, true),
                new Column("code", ColumnType.VARCHAR, 50, false)));

    Key idx = new Key(KeyType.INDEX, java.util.Arrays.asList(new KeyColumn("code")));
    Key uq = new Key(KeyType.UNIQUE, java.util.Arrays.asList(new KeyColumn("code")));
    Key pk = new Key(KeyType.PRIMARY, java.util.Arrays.asList(new KeyColumn("id")));

    table.getKeys().addAll(java.util.Arrays.asList(idx, uq, pk));

    assertEquals(KeyType.INDEX, idx.getType());
    assertEquals(KeyType.UNIQUE, uq.getType());
    assertEquals(KeyType.PRIMARY, pk.getType());

    assertSame(pk, table.getPrimaryKey());
  }
}
