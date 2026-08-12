package com.stano.schema.migrations;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Checks whether a given table exists by querying JDBC {@link java.sql.DatabaseMetaData}.
 *
 * <p>Only objects of JDBC type {@code "TABLE"} are considered, so views and other object types with
 * the same name do not count as a match. The table name is normalized to the casing convention
 * reported by the driver (see {@link MigrationUtil#normalizeIdentifierCase}) before being used to
 * look up metadata, so callers do not need to worry about the casing conventions of the underlying
 * database.
 *
 * <p>Typically used inside a hand-written Flyway or Liquibase Java migration to conditionally
 * create a table only if it does not already exist, e.g. {@code if (!new
 * TableExistsMigration(connection, "my_table").tableExists()) { ... }}.
 */
public class TableExistsMigration {
  private final Connection connection;
  private final String tableName;

  /**
   * Creates a migration helper that checks for the existence of a table named {@code tableName}.
   *
   * @param connection the JDBC connection to the database being migrated; used to obtain {@link
   *     java.sql.DatabaseMetaData}
   * @param tableName the name of the table to check
   */
  public TableExistsMigration(Connection connection, String tableName) {
    this.connection = connection;
    this.tableName = tableName;
  }

  /**
   * Determines whether the table passed to the constructor exists.
   *
   * @return {@code true} if {@link java.sql.DatabaseMetaData#getTables} returns at least one
   *     matching row of type {@code "TABLE"} for the (normalized) table name, {@code false}
   *     otherwise
   * @throws MigrationException if the underlying metadata query throws a {@link SQLException}
   */
  public boolean tableExists() {
    try (ResultSet rs =
        connection
            .getMetaData()
            .getTables(null, null, getAdjustedTableName(), new String[] {"TABLE"})) {
      return rs.next();
    } catch (SQLException x) {
      throw new MigrationException(x);
    }
  }

  /**
   * Normalizes the casing of the configured table name to match the casing convention reported by
   * the database driver.
   *
   * @return the table name adjusted to the driver's identifier storage case
   */
  private String getAdjustedTableName() {
    return MigrationUtil.normalizeIdentifierCase(connection, tableName);
  }
}
