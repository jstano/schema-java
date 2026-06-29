package com.stano.schema.diff.change;

import com.stano.schema.diff.SchemaChange;
import com.stano.schema.model.Procedure;

public final class AddProcedureChange implements SchemaChange {
  private final Procedure procedure;

  public AddProcedureChange(Procedure procedure) {
    this.procedure = procedure;
  }

  public Procedure getProcedure() {
    return procedure;
  }
}
