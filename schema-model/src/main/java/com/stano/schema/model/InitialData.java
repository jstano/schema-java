package com.stano.schema.model;

/**
 * A raw SQL statement seeding a table's initial data, corresponding to a {@code <sql>} element
 * within a table's {@code <initialData>} block, optionally restricted to a specific database type.
 */
public class InitialData {
  private final String sql;
  private final DatabaseType databaseType;

  /**
   * Creates an initial-data statement.
   *
   * @param sql the raw SQL statement (typically an {@code INSERT})
   * @param databaseType the database type this statement applies to, or {@code null} if it applies
   *     to all database types
   */
  public InitialData(String sql, DatabaseType databaseType) {
    this.sql = sql;
    this.databaseType = databaseType;
  }

  /** Returns the raw SQL statement. */
  public String getSql() {
    return sql;
  }

  /** Returns the database type this statement applies to, or {@code null} for all types. */
  public DatabaseType getDatabaseType() {
    return databaseType;
  }
}
