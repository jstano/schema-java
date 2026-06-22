package com.stano.schema.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Key")
class KeyTest {

  @Test
  @DisplayName(
      "constructor with all parameters stores values and exposes unmodifiable, copied columns list")
  void constructorWithAllParametersStoresValues() {
    List<KeyColumn> inputCols =
        new ArrayList<>(List.of(new KeyColumn("id"), new KeyColumn("tenant_id")));

    Key key = new Key(KeyType.UNIQUE, inputCols, true, true, true, "included_col");

    assertEquals(key.getType(), KeyType.UNIQUE);
    assertTrue(key.isCluster());
    assertTrue(key.isCompress());
    assertTrue(key.isUnique());
    assertEquals(key.getInclude(), "included_col");

    assertEquals(
        key.getColumns().stream().map(KeyColumn::getName).toList(), List.of("id", "tenant_id"));

    inputCols.clear();

    assertEquals(
        key.getColumns().stream().map(KeyColumn::getName).toList(), List.of("id", "tenant_id"));

    assertThrows(
        UnsupportedOperationException.class, () -> key.getColumns().add(new KeyColumn("x")));
  }

  @Test
  @DisplayName("secondary constructor defaults flags to false and include to null")
  void secondaryConstructorDefaultsFlagsToFalse() {
    Key key = new Key(KeyType.INDEX, List.of(new KeyColumn("a")));

    assertEquals(key.getType(), KeyType.INDEX);
    assertFalse(key.isCluster());
    assertFalse(key.isCompress());
    assertFalse(key.isUnique());
    assertNull(key.getInclude());
    assertEquals(key.getColumns().stream().map(KeyColumn::getName).toList(), List.of("a"));
  }

  @ParameterizedTest
  @CsvSource({"Id,true", "code,true", "id,false", "missing,false"})
  @DisplayName(
      "containsColumn should return true only when the exact column name exists (case-sensitive)")
  void containsColumnShouldReturnTrueOnlyWhenExactColumnNameExists(
      String testName, boolean expected) {
    Key key = new Key(KeyType.PRIMARY, List.of(new KeyColumn("Id"), new KeyColumn("code")));

    assertEquals(key.containsColumn(testName), expected);
  }

  @Test
  @DisplayName("getColumnsAsString should join column names with commas in order")
  void getColumnsAsStringShouldJoinColumnNames() {
    Key key =
        new Key(
            KeyType.PRIMARY,
            List.of(new KeyColumn("id"), new KeyColumn("tenant_id"), new KeyColumn("code")));

    assertEquals(key.getColumnsAsString(), "id,tenant_id,code");
  }

  @Test
  @DisplayName("Table integration: getPrimaryKey and getPrimaryKeyColumns work as expected")
  void tableIntegrationGetPrimaryKey() throws MalformedURLException {
    Schema schema = new Schema(URI.create("https://example.com/schema.json").toURL());
    Table table = new Table(schema, "public", "users", null, LockEscalation.AUTO, false);

    table
        .getColumns()
        .addAll(
            List.of(
                new Column("id", ColumnType.SEQUENCE, 0, true),
                new Column("tenant_id", ColumnType.INT, 0, true),
                new Column("code", ColumnType.VARCHAR, 50, false)));
    table.getKeys().add(new Key(KeyType.INDEX, List.of(new KeyColumn("code"))));
    Key pk = new Key(KeyType.PRIMARY, List.of(new KeyColumn("id"), new KeyColumn("tenant_id")));
    table.getKeys().add(pk);

    assertEquals(table.getPrimaryKey(), pk);
    assertEquals(table.getPrimaryKeyColumns(), List.of("id", "tenant_id"));

    table.getKeys().clear();

    assertNull(table.getPrimaryKey());
    assertNull(table.getPrimaryKeyColumns());
  }
}
