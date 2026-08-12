package com.stano.schema.model;

/**
 * A raw SQL fragment injected verbatim into generated output, corresponding to a top-level {@code
 * <otherSql>} element. Can be restricted to a specific database type and positioned at the top or
 * bottom of the generated script via {@link OtherSqlOrder}.
 */
public class OtherSql {
  private final DatabaseType databaseType;
  private final OtherSqlOrder order;
  private final String sql;

  /**
   * Creates an other-SQL fragment.
   *
   * @param databaseType the database type this SQL applies to, or {@code null} for all types
   * @param order where in the generated output this SQL should be placed
   * @param sql the raw SQL text
   */
  public OtherSql(DatabaseType databaseType, OtherSqlOrder order, String sql) {
    this.databaseType = databaseType;
    this.order = order;
    this.sql = sql;
  }

  /** Returns the database type this SQL applies to, or {@code null} for all types. */
  public DatabaseType getDatabaseType() {
    return databaseType;
  }

  /** Returns where in the generated output this SQL should be placed. */
  public OtherSqlOrder getOrder() {
    return order;
  }

  /** Returns the raw SQL text. */
  public String getSql() {
    return sql;
  }
}
