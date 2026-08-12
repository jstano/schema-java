package com.stano.schema.diff.change;

import com.stano.schema.diff.SchemaChange;
import com.stano.schema.model.Procedure;

/**
 * Represents a stored procedure that must be created because it is new in the new schema, or whose
 * SQL body differs from the old schema's version (in which case a matching {@code
 * DropProcedureChange} for the old definition precedes this change). Carries the full {@link
 * Procedure} definition, including its name, database type, and SQL, to create.
 */
public final class AddProcedureChange implements SchemaChange {
  private final Procedure procedure;

  /**
   * Creates a change describing a procedure to add.
   *
   * @param procedure the definition of the procedure to create
   */
  public AddProcedureChange(Procedure procedure) {
    this.procedure = procedure;
  }

  /** Returns the definition of the procedure to create. */
  public Procedure getProcedure() {
    return procedure;
  }
}
