package com.stano.schema.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

class LockEscalationTest {
  @Test
  @DisplayName("enum should contain exactly the expected values in order")
  void testEnumValues() {
    LockEscalation[] values = LockEscalation.values();
    assertEquals(3, values.length);
    assertEquals("DISABLE", values[0].name());
    assertEquals("AUTO", values[1].name());
    assertEquals("TABLE", values[2].name());
  }

  @ParameterizedTest
  @MethodSource("valueOfProvider")
  @DisplayName("valueOf should return the correct enum constant for each name")
  void testValueOf(String name, LockEscalation constant) {
    assertEquals(constant, LockEscalation.valueOf(name));
    assertEquals(name, constant.name());
  }

  static Stream<Arguments> valueOfProvider() {
    return Stream.of(
        Arguments.of("DISABLE", LockEscalation.DISABLE),
        Arguments.of("AUTO", LockEscalation.AUTO),
        Arguments.of("TABLE", LockEscalation.TABLE));
  }

  @Test
  @DisplayName("all enum values should be unique")
  void testEnumValuesUnique() {
    LockEscalation[] values = LockEscalation.values();
    assertEquals(new HashSet<>(java.util.Arrays.asList(values)).size(), values.length);
  }

  @Test
  @DisplayName("Table should store and return LockEscalation via getter")
  void testTableLockEscalation() throws MalformedURLException {
    Schema schema = new Schema(URI.create("https://example.com/schema.json").toURL());

    Table tblDisable = new Table(schema, "public", "t1", null, LockEscalation.DISABLE, false);
    Table tblAuto = new Table(schema, "public", "t2", "exported", LockEscalation.AUTO, true);
    Table tblTable = new Table(schema, "public", "t3", null, LockEscalation.TABLE, false);

    assertEquals(LockEscalation.DISABLE, tblDisable.getLockEscalation());
    assertEquals(LockEscalation.AUTO, tblAuto.getLockEscalation());
    assertEquals(LockEscalation.TABLE, tblTable.getLockEscalation());

    assertEquals("public", tblAuto.getSchemaName());
    assertEquals("t2", tblAuto.getName());
    assertEquals("exported", tblAuto.getExportDateColumn());
    assertTrue(tblAuto.isNoExport());
  }
}
