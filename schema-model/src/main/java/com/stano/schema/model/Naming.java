package com.stano.schema.model;

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
   * Builds {@code prefix + tableName + suffix}, truncating the table-name portion (never the prefix
   * or suffix) so the result fits within {@link DatabaseType#getMaxKeyNameLength()}.
   *
   * <p>Exposed publicly (in addition to the {@code primaryKeyName}/{@code uniqueKeyName}/{@code
   * indexName}/{@code foreignKeyName} convenience methods above) so callers with a non-standard
   * prefix/suffix shape can still share this single truncation implementation.
   */
  public static String buildName(
      DatabaseType databaseType, String prefix, String tableName, String suffix) {
    int maxKeyNameLength = databaseType.getMaxKeyNameLength();
    String candidate = prefix + tableName + suffix;

    if (candidate.length() <= maxKeyNameLength) {
      return candidate;
    }

    int available = maxKeyNameLength - (prefix.length() + suffix.length());
    int truncateTo = Math.max(0, Math.min(available, tableName.length()));

    return prefix + tableName.substring(0, truncateTo) + suffix;
  }
}
