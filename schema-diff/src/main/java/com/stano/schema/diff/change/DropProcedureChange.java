package com.stano.schema.diff.change;

import com.stano.schema.diff.SchemaChange;
import com.stano.schema.model.DatabaseType;

public final class DropProcedureChange implements SchemaChange {
  private final String procedureName;
  private final DatabaseType databaseType;

  public DropProcedureChange(String procedureName, DatabaseType databaseType) {
    this.procedureName = procedureName;
    this.databaseType = databaseType;
  }

  public String getProcedureName() {
    return procedureName;
  }

  public DatabaseType getDatabaseType() {
    return databaseType;
  }
}
