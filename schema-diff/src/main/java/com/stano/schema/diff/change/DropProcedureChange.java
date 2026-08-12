package com.stano.schema.diff.change;

import com.stano.schema.diff.SchemaChange;
import com.stano.schema.model.DatabaseType;

/**
 * Represents a stored procedure that must be dropped because it no longer exists in the new schema,
 * or because its SQL body changed (in which case an {@code AddProcedureChange} with the new
 * definition follows this change). Carries the procedure's name and the {@link DatabaseType} it
 * targets.
 */
public final class DropProcedureChange implements SchemaChange {
  private final String procedureName;
  private final DatabaseType databaseType;

  /**
   * Creates a change describing a procedure to drop.
   *
   * @param procedureName the name of the dropped procedure
   * @param databaseType the database type the procedure targets
   */
  public DropProcedureChange(String procedureName, DatabaseType databaseType) {
    this.procedureName = procedureName;
    this.databaseType = databaseType;
  }

  /** Returns the name of the dropped procedure. */
  public String getProcedureName() {
    return procedureName;
  }

  /** Returns the database type the dropped procedure targets. */
  public DatabaseType getDatabaseType() {
    return databaseType;
  }
}
