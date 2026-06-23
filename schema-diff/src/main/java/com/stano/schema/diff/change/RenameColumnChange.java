package com.stano.schema.diff.change;

import com.stano.schema.diff.SchemaChange;

public final class RenameColumnChange implements SchemaChange {
  private final String tableName;
  private final String oldName;
  private final String newName;

  public RenameColumnChange(String tableName, String oldName, String newName) {
    this.tableName = tableName;
    this.oldName = oldName;
    this.newName = newName;
  }

  public String getTableName() {
    return tableName;
  }

  public String getOldName() {
    return oldName;
  }

  public String getNewName() {
    return newName;
  }
}
