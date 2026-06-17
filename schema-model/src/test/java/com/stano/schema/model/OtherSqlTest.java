package com.stano.schema.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("OtherSql")
class OtherSqlTest {

  @ParameterizedTest
  @MethodSource("provideOtherSqlTestCases")
  @DisplayName(
      "constructor should set fields and getters should return them for various combinations")
  void constructorShouldSetFields(DatabaseType dbType, OtherSqlOrder ord, String sql) {
    OtherSql other = new OtherSql(dbType, ord, sql);

    assertEquals(other.getDatabaseType(), dbType);
    assertEquals(other.getOrder(), ord);
    assertEquals(other.getSql(), sql);
  }

  private static Stream<Object[]> provideOtherSqlTestCases() {
    return Stream.of(
        new Object[] {
          DatabaseType.POSTGRESQL, OtherSqlOrder.TOP, "CREATE EXTENSION IF NOT EXISTS uuid-ossp;"
        },
        new Object[] {DatabaseType.SQL_SERVER, OtherSqlOrder.BOTTOM, "PRINT 'Done';"},
        new Object[] {DatabaseType.H2, OtherSqlOrder.BOTTOM, "-- noop"});
  }

  @Test
  @DisplayName("supports null SQL and still returns correct fields")
  void supportsNullSQL() {
    OtherSql other = new OtherSql(DatabaseType.H2, OtherSqlOrder.TOP, null);

    assertEquals(other.getDatabaseType(), DatabaseType.H2);
    assertEquals(other.getOrder(), OtherSqlOrder.TOP);
    assertNull(other.getSql());
  }

  @Test
  @DisplayName("Schema should collect OtherSql entries and expose an unmodifiable copy")
  void schemaShouldCollectOtherSqlEntries() throws MalformedURLException {
    Schema schema = new Schema(new URL("https://example.com/schema.json"));
    OtherSql a = new OtherSql(DatabaseType.POSTGRESQL, OtherSqlOrder.TOP, "A;");
    OtherSql b = new OtherSql(DatabaseType.POSTGRESQL, OtherSqlOrder.BOTTOM, "B;");

    schema.addOtherSql(a);
    schema.addOtherSql(b);

    assertEquals(schema.getOtherSql().stream().map(OtherSql::getSql).toList(), List.of("A;", "B;"));

    assertThrows(
        UnsupportedOperationException.class,
        () -> schema.getOtherSql().add(new OtherSql(DatabaseType.H2, OtherSqlOrder.TOP, "C;")));
  }
}
