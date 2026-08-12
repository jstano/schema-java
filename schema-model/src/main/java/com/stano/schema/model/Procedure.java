package com.stano.schema.model;

/**
 * A stored procedure definition corresponding to a {@code <procedure>} top-level element.
 * Procedures are always database-specific, with the raw SQL body supplied per {@link DatabaseType}.
 */
public class Procedure {
  private final String schemaName;
  private final String name;
  private final DatabaseType databaseType;
  private final String sql;

  /**
   * Creates a procedure definition.
   *
   * @param schemaName the name of the containing schema namespace, or {@code null} if none
   * @param name the procedure's name
   * @param databaseType the database type this procedure's SQL body is written for
   * @param sql the raw SQL {@code CREATE PROCEDURE} statement/body
   */
  public Procedure(String schemaName, String name, DatabaseType databaseType, String sql) {
    this.schemaName = schemaName;
    this.name = name;
    this.databaseType = databaseType;
    this.sql = sql;
  }

  /** Returns the name of the containing schema namespace, or {@code null} if none. */
  public String getSchemaName() {
    return schemaName;
  }

  /** Returns the procedure's name. */
  public String getName() {
    return name;
  }

  /** Returns the database type this procedure's SQL body is written for. */
  public DatabaseType getDatabaseType() {
    return databaseType;
  }

  /** Returns the raw SQL {@code CREATE PROCEDURE} statement/body. */
  public String getSql() {
    return sql;
  }
}
