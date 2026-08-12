package com.stano.schema.diff.change;

import com.stano.schema.diff.SchemaChange;
import com.stano.schema.model.DatabaseType;

/**
 * Represents a database function that must be dropped because it no longer exists in the new
 * schema, or because its SQL body changed (in which case an {@code AddFunctionChange} with the new
 * definition follows this change). Carries the function's name and the {@link DatabaseType} it
 * targets.
 */
public final class DropFunctionChange implements SchemaChange {
  private final String functionName;
  private final DatabaseType databaseType;

  /**
   * Creates a change describing a function to drop.
   *
   * @param functionName the name of the dropped function
   * @param databaseType the database type the function targets
   */
  public DropFunctionChange(String functionName, DatabaseType databaseType) {
    this.functionName = functionName;
    this.databaseType = databaseType;
  }

  /** Returns the name of the dropped function. */
  public String getFunctionName() {
    return functionName;
  }

  /** Returns the database type the dropped function targets. */
  public DatabaseType getDatabaseType() {
    return databaseType;
  }
}
