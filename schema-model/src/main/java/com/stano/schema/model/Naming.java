package com.stano.schema.model;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Single source of truth for generated key/index/constraint names ({@code pk_}, {@code ak_}, {@code
 * ix_}, {@code fk_}), shared between the SQL-generator's create path and any migration generator's
 * alter path. Keeping both paths on the same naming logic ensures a migration's {@code DROP
 * CONSTRAINT}/{@code DROP INDEX} always targets the exact name the create path would have produced
 * for that same object.
 */
public final class Naming {
  private static final String PK_PREFIX = "pk_";
  private static final String AK_PREFIX = "ak_";
  private static final String IX_PREFIX = "ix_";
  private static final String FK_PREFIX = "fk_";

  private Naming() {}

  /** Builds the primary key constraint name for a table, e.g. {@code pk_users}. */
  public static String primaryKeyName(DatabaseType databaseType, String tableName) {
    return buildName(databaseType, PK_PREFIX, tableName, "");
  }

  /**
   * Builds a unique key constraint name for a table, e.g. {@code ak_users1}.
   *
   * @param ordinal the 1-based position of this key among the table's unique keys
   */
  public static String uniqueKeyName(DatabaseType databaseType, String tableName, int ordinal) {
    return buildName(databaseType, AK_PREFIX, tableName, String.valueOf(ordinal));
  }

  /**
   * Builds an index name for a table, e.g. {@code ix_users1}.
   *
   * @param ordinal the 1-based position of this index among the table's indexes
   */
  public static String indexName(DatabaseType databaseType, String tableName, int ordinal) {
    return buildName(databaseType, IX_PREFIX, tableName, String.valueOf(ordinal));
  }

  /**
   * Builds a foreign key constraint name for a table, e.g. {@code fk_users1}.
   *
   * @param ordinal the 1-based position of this relation among the table's relations
   */
  public static String foreignKeyName(DatabaseType databaseType, String tableName, int ordinal) {
    return buildName(databaseType, FK_PREFIX, tableName, String.valueOf(ordinal));
  }

  /**
   * Builds {@code prefix + tableName + suffix}, lower-cased, truncating the table-name portion
   * (never the prefix or suffix) so the result fits within {@link
   * DatabaseType#getMaxKeyNameLength()}.
   *
   * <p>PostgreSQL's limit is in <em>bytes</em> (UTF-8); every other database counts characters.
   * Measuring in characters for PostgreSQL would let a multi-byte table name pass, after which
   * PostgreSQL silently truncates it further and sibling names that differ only in their (cut-off)
   * ordinal suffix collide. Lower-casing can change a character's width, so the measurement is
   * taken on the lower-cased result.
   *
   * <p>Exposed publicly (in addition to the {@code primaryKeyName}/{@code uniqueKeyName}/{@code
   * indexName}/{@code foreignKeyName} convenience methods above) so callers with a non-standard
   * prefix/suffix shape can still share this single truncation implementation.
   */
  public static String buildName(
      DatabaseType databaseType, String prefix, String tableName, String suffix) {
    int maxKeyNameLength = databaseType.getMaxKeyNameLength();
    String fullName = (prefix + tableName + suffix).toLowerCase(Locale.ROOT);

    if (measure(databaseType, fullName) <= maxKeyNameLength) {
      return fullName;
    }

    // Keep the suffix intact (it carries the ordinal that keeps sibling names distinct) and drop
    // table-name characters one code point at a time until the whole identifier fits.
    String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
    String lowerSuffix = suffix.toLowerCase(Locale.ROOT);
    StringBuilder name = new StringBuilder(tableName.toLowerCase(Locale.ROOT));

    while (name.length() > 0
        && measure(databaseType, lowerPrefix + name + lowerSuffix) > maxKeyNameLength) {
      name.setLength(name.offsetByCodePoints(name.length(), -1));
    }

    return lowerPrefix + name + lowerSuffix;
  }

  /**
   * Builds {@code prefix + table + "_" + column + "_" + hash} for check and default constraints:
   * the table and column parts are lower-cased and cut to 9 characters each, and the hash is the
   * upper-case hex of the untruncated {@code table_column} hash. The parts are further trimmed by
   * code point if multi-byte characters would push the name past PostgreSQL's 63-byte limit.
   */
  public static String hashedColumnName(String prefix, String tableName, String columnName) {
    String table = tableName.toLowerCase(Locale.ROOT);
    String column = columnName.toLowerCase(Locale.ROOT);
    String hash = Integer.toHexString((table + "_" + column).hashCode()).toUpperCase(Locale.ROOT);

    table =
        table.substring(
            0, table.offsetByCodePoints(0, Math.min(9, table.codePointCount(0, table.length()))));
    column =
        column.substring(
            0,
            column.offsetByCodePoints(0, Math.min(9, column.codePointCount(0, column.length()))));

    int maxBytes = DatabaseType.POSTGRESQL.getMaxKeyNameLength();
    while (bytes(prefix + table + "_" + column + "_" + hash) > maxBytes
        && (!table.isEmpty() || !column.isEmpty())) {
      if (table.length() >= column.length()) {
        table = table.substring(0, table.offsetByCodePoints(table.length(), -1));
      } else {
        column = column.substring(0, column.offsetByCodePoints(column.length(), -1));
      }
    }

    return prefix + table + "_" + column + "_" + hash;
  }

  private static int bytes(String name) {
    return name.getBytes(StandardCharsets.UTF_8).length;
  }

  private static int measure(DatabaseType databaseType, String name) {
    if (databaseType == DatabaseType.POSTGRESQL) {
      return name.getBytes(StandardCharsets.UTF_8).length;
    }

    return name.codePointCount(0, name.length());
  }
}
