package com.stano.schema.diff.change;

import com.stano.schema.diff.SchemaChange;
import com.stano.schema.model.Column;

public final class ModifyColumnChange implements SchemaChange {
  private final String tableName;
  private final Column oldColumn;
  private final Column newColumn;

  public ModifyColumnChange(String tableName, Column oldColumn, Column newColumn) {
    this.tableName = tableName;
    this.oldColumn = oldColumn;
    this.newColumn = newColumn;
  }

  public String getTableName() {
    return tableName;
  }

  public Column getOldColumn() {
    return oldColumn;
  }

  public Column getNewColumn() {
    return newColumn;
  }
}
