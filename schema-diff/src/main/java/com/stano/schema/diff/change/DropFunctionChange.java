package com.stano.schema.diff.change;

import com.stano.schema.diff.SchemaChange;
import com.stano.schema.model.DatabaseType;

public final class DropFunctionChange implements SchemaChange {
  private final String functionName;
  private final DatabaseType databaseType;

  public DropFunctionChange(String functionName, DatabaseType databaseType) {
    this.functionName = functionName;
    this.databaseType = databaseType;
  }

  public String getFunctionName() {
    return functionName;
  }

  public DatabaseType getDatabaseType() {
    return databaseType;
  }
}
