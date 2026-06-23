package com.stano.schema.diff.change;

import com.stano.schema.diff.SchemaChange;

public final class DropConstraintChange implements SchemaChange {
  private final String tableName;
  private final String constraintName;

  public DropConstraintChange(String tableName, String constraintName) {
    this.tableName = tableName;
    this.constraintName = constraintName;
  }

  public String getTableName() {
    return tableName;
  }

  public String getConstraintName() {
    return constraintName;
  }
}
