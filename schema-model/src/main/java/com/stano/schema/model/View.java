package com.stano.schema.model;

/**
 * A database view definition corresponding to a {@code <view>} top-level element. A view may be
 * database-agnostic ({@code databaseType} is {@code null}) or restricted to a specific {@link
 * DatabaseType}; see {@link Schema#getViews(DatabaseType)} for how these are resolved.
 */
public class View {
  private final String schemaName;
  private final String name;
  private final String sql;
  private final DatabaseType databaseType;

  /**
   * Creates a view definition.
   *
   * @param schemaName the name of the containing schema namespace, or {@code null} if none
   * @param name the view's name
   * @param sql the raw SQL {@code SELECT} statement/body defining the view
   * @param databaseType the database type this view is specific to, or {@code null} if it applies
   *     to all database types
   */
  public View(String schemaName, String name, String sql, DatabaseType databaseType) {
    this.schemaName = schemaName;
    this.name = name;
    this.sql = sql;
    this.databaseType = databaseType;
  }

  /** Returns the name of the containing schema namespace, or {@code null} if none. */
  public String getSchemaName() {
    return schemaName;
  }

  /** Returns the view's name. */
  public String getName() {
    return name;
  }

  /** Returns the raw SQL {@code SELECT} statement/body defining the view. */
  public String getSql() {
    return sql;
  }

  /** Returns the database type this view is specific to, or {@code null} for all types. */
  public DatabaseType getDatabaseType() {
    return databaseType;
  }
}
