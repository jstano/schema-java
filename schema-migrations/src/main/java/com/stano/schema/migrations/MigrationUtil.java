package com.stano.schema.migrations;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * Static helper methods shared by the migration helper classes in this package.
 *
 * <p>This is a non-instantiable utility class.
 */
public final class MigrationUtil {
  /**
   * Adjusts the case of a database identifier (table or column name) to match the identifier
   * storage convention reported by the JDBC driver, so that the identifier can be reliably used in
   * {@link DatabaseMetaData} lookups such as {@link DatabaseMetaData#getTables} or {@link
   * DatabaseMetaData#getColumns}.
   *
   * <p>If {@link DatabaseMetaData#storesLowerCaseIdentifiers()} returns {@code true}, the
   * identifier is lower-cased. Otherwise, if {@link DatabaseMetaData#storesUpperCaseIdentifiers()}
   * returns {@code true}, the identifier is upper-cased. Otherwise the identifier is returned
   * unchanged (mixed-case storage).
   *
   * @param connection the JDBC connection used to obtain {@link DatabaseMetaData}
   * @param identifier the identifier (table or column name) to normalize
   * @return the identifier adjusted to the driver's identifier storage case
   * @throws MigrationException if obtaining the metadata throws a {@link SQLException}
   */
  public static String normalizeIdentifierCase(Connection connection, String identifier) {
    try {
      DatabaseMetaData databaseMetaData = connection.getMetaData();

      if (databaseMetaData.storesLowerCaseIdentifiers()) {
        return identifier.toLowerCase();
      }

      if (databaseMetaData.storesUpperCaseIdentifiers()) {
        return identifier.toUpperCase();
      }

      return identifier;
    } catch (SQLException x) {
      throw new MigrationException(x);
    }
  }

  /** Private constructor to prevent instantiation of this utility class. */
  private MigrationUtil() {}
}
