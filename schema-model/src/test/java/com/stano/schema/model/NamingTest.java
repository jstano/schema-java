package com.stano.schema.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Naming")
class NamingTest {

  @Test
  @DisplayName("short names are lower-cased and otherwise unchanged")
  void shortNameIsLowerCased() {
    assertEquals("ak_users1", Naming.uniqueKeyName(DatabaseType.POSTGRESQL, "Users", 1));
  }

  @Test
  @DisplayName("PostgreSQL limit is measured in bytes, not characters")
  void postgresLimitIsMeasuredInBytes() {
    String longTableName = "ж".repeat(70);

    for (int ordinal : new int[] {1, 2, 10}) {
      String name = Naming.uniqueKeyName(DatabaseType.POSTGRESQL, longTableName, ordinal);

      int bytes = name.getBytes(StandardCharsets.UTF_8).length;
      assertTrue(bytes <= 63, "name '" + name + "' is " + bytes + " bytes");
      assertTrue(name.startsWith("ak_"));
      assertTrue(name.endsWith(String.valueOf(ordinal)));
    }

    assertNotEquals(
        Naming.uniqueKeyName(DatabaseType.POSTGRESQL, longTableName, 1),
        Naming.uniqueKeyName(DatabaseType.POSTGRESQL, longTableName, 2));
  }

  @Test
  @DisplayName("non-PostgreSQL limits remain character based")
  void otherDatabasesStayCharacterBased() {
    String name = Naming.primaryKeyName(DatabaseType.H2, "ж".repeat(200));

    assertEquals(DatabaseType.H2.getMaxKeyNameLength(), name.codePointCount(0, name.length()));
  }

  @Test
  @DisplayName("check and default constraint names stay within PostgreSQL's byte limit")
  void hashedColumnNamesAreByteLimited() {
    String name = Naming.hashedColumnName("ck_", "表".repeat(20), "列".repeat(20));

    assertTrue(name.getBytes(StandardCharsets.UTF_8).length <= 63);
    assertEquals(
        "ck_orders_total_", Naming.hashedColumnName("ck_", "ORDERS", "Total").substring(0, 16));
  }

  @Test
  @DisplayName("lower-casing does not depend on the default locale")
  void lowerCasingIsLocaleIndependent() {
    java.util.Locale original = java.util.Locale.getDefault();
    try {
      java.util.Locale.setDefault(java.util.Locale.forLanguageTag("tr"));
      assertEquals("pk_items", Naming.primaryKeyName(DatabaseType.H2, "ITEMS"));
    } finally {
      java.util.Locale.setDefault(original);
    }
  }
}
