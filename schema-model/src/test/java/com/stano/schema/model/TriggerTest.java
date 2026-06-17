package com.stano.schema.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Trigger")
class TriggerTest {

  @ParameterizedTest
  @MethodSource("provideTriggerTestCases")
  @DisplayName(
      "constructor should set fields and getters should return them for various types and DBs")
  void constructorShouldSetFields(String text, TriggerType ttype, DatabaseType db) {
    Trigger trg = new Trigger(text, ttype, db);

    assertEquals(trg.getTriggerText(), text);
    assertEquals(trg.getTriggerType(), ttype);
    assertEquals(trg.getDatabaseType(), db);
  }

  private static Stream<Object[]> provideTriggerTestCases() {
    return Stream.of(
        new Object[] {"AFTER UPDATE SET x=1", TriggerType.UPDATE, DatabaseType.POSTGRESQL},
        new Object[] {"BEFORE DELETE FROM t", TriggerType.DELETE, DatabaseType.SQL_SERVER},
        new Object[] {"DROP TRIGGER IF EXISTS", TriggerType.DELETE, DatabaseType.H2});
  }

  @Test
  @DisplayName("supports null triggerText and still returns correct fields")
  void supportsNullTriggerText() {
    Trigger trg = new Trigger(null, TriggerType.UPDATE, DatabaseType.H2);

    assertNull(trg.getTriggerText());
    assertEquals(trg.getTriggerType(), TriggerType.UPDATE);
    assertEquals(trg.getDatabaseType(), DatabaseType.H2);
  }

  @Test
  @DisplayName("Table.getTriggers exposes a live mutable list (current contract)")
  void tableGetTriggersExposesLiveMutableList() throws MalformedURLException {
    Schema schema = new Schema(new URL("https://example.com/schema.json"));
    Table table = new Table(schema, "public", "orders", null, LockEscalation.AUTO, false);

    assertEquals(table.getTriggers().size(), 0);

    table
        .getTriggers()
        .add(new Trigger("AFTER UPDATE ON orders", TriggerType.UPDATE, DatabaseType.POSTGRESQL));
    table
        .getTriggers()
        .add(new Trigger("BEFORE DELETE ON orders", TriggerType.DELETE, DatabaseType.POSTGRESQL));

    assertEquals(
        table.getTriggers().stream().map(Trigger::getTriggerType).toList(),
        List.of(TriggerType.UPDATE, TriggerType.DELETE));
    assertEquals(
        table.getTriggers().stream().map(Trigger::getDatabaseType).toList(),
        List.of(DatabaseType.POSTGRESQL, DatabaseType.POSTGRESQL));

    table.getTriggers().remove(0);

    assertEquals(
        table.getTriggers().stream().map(Trigger::getTriggerType).toList(),
        List.of(TriggerType.DELETE));
  }
}
