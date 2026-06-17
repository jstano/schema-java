package com.stano.schema.model;

import java.util.HashSet;
import java.util.Set;

public enum DatabaseType {
  H2(";", 64, false),
  POSTGRESQL(";", 63, true),
  SQL_SERVER("\nGO", 32, true);

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

  public String getStatementSeparator() {
    return statementSeparator;
  }

  public int getMaxKeyNameLength() {
    return maxKeyNameLength;
  }

  public boolean isSupportsTriggers() {
    return supportsTriggers;
  }

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
