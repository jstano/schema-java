package com.stano.schema.model;

/**
 * A named, raw-SQL table constraint corresponding to a {@code <constraint>} element within a
 * table's {@code <constraints>} block, optionally restricted to a specific database type.
 */
public class Constraint {
  private final String name;
  private final String sql;
  private final DatabaseType databaseType;

  /**
   * Creates a constraint definition.
   *
   * @param name the constraint's SQL name
   * @param sql the raw SQL constraint expression/body
   * @param databaseType the database type this constraint applies to, or {@code null} if it applies
   *     to all database types
   */
  public Constraint(String name, String sql, DatabaseType databaseType) {
    this.name = name;
    this.sql = sql;
    this.databaseType = databaseType;
  }

  /** Returns the constraint's SQL name. */
  public String getName() {
    return name;
  }

  /** Returns the raw SQL constraint expression/body. */
  public String getSql() {
    return sql;
  }

  /** Returns the database type this constraint applies to, or {@code null} for all types. */
  public DatabaseType getDatabaseType() {
    return databaseType;
  }
}
