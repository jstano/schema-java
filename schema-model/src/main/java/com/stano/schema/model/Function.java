package com.stano.schema.model;

/**
 * A stored function definition corresponding to a {@code <function>} top-level element. Functions
 * are always database-specific, with the raw SQL body supplied per {@link DatabaseType}.
 */
public class Function {
  private final String schemaName;
  private final String name;
  private final DatabaseType databaseType;
  private final String sql;

  /**
   * Creates a function definition.
   *
   * @param schemaName the name of the containing schema namespace, or {@code null} if none
   * @param name the function's name
   * @param databaseType the database type this function's SQL body is written for
   * @param sql the raw SQL {@code CREATE FUNCTION} statement/body
   */
  public Function(String schemaName, String name, DatabaseType databaseType, String sql) {
    this.schemaName = schemaName;
    this.name = name;
    this.databaseType = databaseType;
    this.sql = sql;
  }

  /** Returns the name of the containing schema namespace, or {@code null} if none. */
  public String getSchemaName() {
    return schemaName;
  }

  /** Returns the function's name. */
  public String getName() {
    return name;
  }

  /** Returns the database type this function's SQL body is written for. */
  public DatabaseType getDatabaseType() {
    return databaseType;
  }

  /** Returns the raw SQL {@code CREATE FUNCTION} statement/body. */
  public String getSql() {
    return sql;
  }
}
