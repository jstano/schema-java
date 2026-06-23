package com.stano.schema.diff.change;

import com.stano.schema.diff.SchemaChange;

public final class DropColumnChange implements SchemaChange {
  private final String tableName;
  private final String columnName;

  public DropColumnChange(String tableName, String columnName) {
    this.tableName = tableName;
    this.columnName = columnName;
  }

  public String getTableName() {
    return tableName;
  }

  public String getColumnName() {
    return columnName;
  }
}
