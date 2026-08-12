package com.stano.schema.migrations;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Checks whether a given column exists on a given table by querying JDBC {@link
 * java.sql.DatabaseMetaData}.
 *
 * <p>The table and column names are normalized to the casing convention reported by the driver (see
 * {@link MigrationUtil#normalizeIdentifierCase}) before being used to look up metadata, so callers
 * do not need to worry about the casing conventions of the underlying database.
 *
 * <p>Typically used inside a hand-written Flyway or Liquibase Java migration to conditionally add a
 * column only if it does not already exist, e.g. {@code if (!new ColumnExistsMigration(connection,
 * "my_table", "my_column").columnExists()) { ... }}.
 */
public class ColumnExistsMigration {
  private final Connection connection;
  private final String tableName;
  private final String columnName;

  /**
   * Creates a migration helper that checks for the existence of {@code columnName} on {@code
   * tableName}.
   *
   * @param connection the JDBC connection to the database being migrated; used to obtain {@link
   *     java.sql.DatabaseMetaData}
   * @param tableName the name of the table to check
   * @param columnName the name of the column to check for
   */
  public ColumnExistsMigration(Connection connection, String tableName, String columnName) {
    this.connection = connection;
    this.tableName = tableName;
    this.columnName = columnName;
  }

  /**
   * Determines whether the column passed to the constructor exists on the table passed to the
   * constructor.
   *
   * @return {@code true} if {@link java.sql.DatabaseMetaData#getColumns} returns at least one
   *     matching row for the (normalized) table and column name, {@code false} otherwise
   * @throws MigrationException if the underlying metadata query throws a {@link SQLException}
   */
  public boolean columnExists() {
    try {
      try (ResultSet rs =
          connection
              .getMetaData()
              .getColumns(null, null, getAdjustedName(tableName), getAdjustedName(columnName))) {
        return rs.next();
      }
    } catch (SQLException x) {
      throw new MigrationException(x);
    }
  }

  /**
   * Normalizes the casing of the given identifier to match the casing convention reported by the
   * database driver.
   *
   * @param name the identifier (table or column name) to normalize
   * @return the identifier adjusted to the driver's identifier storage case
   */
  private String getAdjustedName(String name) {
    return MigrationUtil.normalizeIdentifierCase(connection, name);
  }
}
