package com.stano.schema.model;

import java.util.HashSet;
import java.util.Set;

/**
 * The relational database vendors supported for SQL generation and installation. Also carries
 * per-database SQL generation traits: the statement separator, the maximum length allowed for
 * generated key/constraint names, and whether the database supports triggers.
 */
public enum DatabaseType {
  H2(";", 64, false),
  POSTGRESQL(";", 63, true),
  SQL_SERVER("\nGO", 32, true);

  /**
   * Parses a comma-separated list of database type names (e.g. from a {@code --database-types} CLI
   * option) into a set of {@code DatabaseType} values.
   *
   * @param targetDatabasesStr a comma-separated list of database type names, may be {@code null}
   * @return the parsed set of database types, empty if {@code targetDatabasesStr} is {@code null}
   *     or blank
   */
  public static Set<DatabaseType> getDatabaseTypes(String targetDatabasesStr) {
    Set<DatabaseType> databaseTypes = new HashSet<>();

    if (targetDatabasesStr != null) {
      for (String targetDatabase : targetDatabasesStr.split(",")) {
        if (!targetDatabase.trim().isEmpty()) {
          databaseTypes.add(DatabaseType.fromString(targetDatabase));
        }
      }
    }

    return databaseTypes;
  }

  private final String statementSeparator;
  private final int maxKeyNameLength;
  private final boolean supportsTriggers;

  /** Returns the token used to separate SQL statements when generating DDL for this database. */
  public String getStatementSeparator() {
    return statementSeparator;
  }

  /** Returns the maximum length allowed for generated key/constraint names on this database. */
  public int getMaxKeyNameLength() {
    return maxKeyNameLength;
  }

  /** Returns whether this database supports triggers. */
  public boolean isSupportsTriggers() {
    return supportsTriggers;
  }

  /**
   * Parses a single database type name (as used in the XML schema's {@code databaseType} attribute,
   * e.g. {@code "postgres"} or {@code "sqlserver"}) into a {@code DatabaseType}.
   *
   * @param databaseType the database type name, may be {@code null}
   * @return the matching {@code DatabaseType}, or {@code null} if {@code databaseType} is {@code
   *     null}
   * @throws IllegalArgumentException if {@code databaseType} does not match any known type
   */
  public static DatabaseType fromString(String databaseType) {
    if (databaseType == null) {
      return null;
    }

    if (databaseType.trim().equalsIgnoreCase("sqlserver")) {
      return SQL_SERVER;
    }

    if (databaseType.trim().equalsIgnoreCase("postgres")
        || databaseType.trim().equalsIgnoreCase("postgresql")) {
      return POSTGRESQL;
    }

    return valueOf(databaseType.toUpperCase());
  }

  DatabaseType(String statementSeparator, int maxKeyNameLength, boolean supportsTriggers) {
    this.statementSeparator = statementSeparator;
    this.maxKeyNameLength = maxKeyNameLength;
    this.supportsTriggers = supportsTriggers;
  }
}
