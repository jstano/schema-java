package com.stano.schema.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TriggerTypeTest {
  @Test
  @DisplayName("enum should contain exactly the expected values in order")
  void testEnumValues() {
    TriggerType[] values = TriggerType.values();
    assertEquals(2, values.length);
    assertEquals("UPDATE", values[0].name());
    assertEquals("DELETE", values[1].name());
  }

  @ParameterizedTest
  @MethodSource("valueOfProvider")
  @DisplayName("valueOf should return the correct enum constant for each name")
  void testValueOf(String name, TriggerType constant) {
    assertEquals(constant, TriggerType.valueOf(name));
    assertEquals(name, constant.name());
  }

  static Stream<Arguments> valueOfProvider() {
    return Stream.of(
        Arguments.of("UPDATE", TriggerType.UPDATE), Arguments.of("DELETE", TriggerType.DELETE));
  }

  @Test
  @DisplayName("all enum values should be unique")
  void testEnumValuesUnique() {
    TriggerType[] values = TriggerType.values();
    assertEquals(new HashSet<>(java.util.Arrays.asList(values)).size(), values.length);
  }

  @Test
  @DisplayName("can be used in Trigger without errors")
  void testTriggerIntegration() {
    Trigger tUpd = new Trigger("AFTER UPDATE ON t", TriggerType.UPDATE, DatabaseType.POSTGRESQL);
    Trigger tDel = new Trigger("BEFORE DELETE ON t", TriggerType.DELETE, DatabaseType.SQL_SERVER);

    assertEquals(TriggerType.UPDATE, tUpd.getTriggerType());
    assertEquals(TriggerType.DELETE, tDel.getTriggerType());
  }
}
