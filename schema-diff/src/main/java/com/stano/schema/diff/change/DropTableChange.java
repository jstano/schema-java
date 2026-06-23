package com.stano.schema.diff.change;

import com.stano.schema.diff.SchemaChange;

public final class DropTableChange implements SchemaChange {
  private final String tableName;

  public DropTableChange(String tableName) {
    this.tableName = tableName;
  }

  public String getTableName() {
    return tableName;
  }
}
