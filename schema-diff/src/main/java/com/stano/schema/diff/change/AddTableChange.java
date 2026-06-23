package com.stano.schema.diff.change;

import com.stano.schema.diff.SchemaChange;

public final class AddTableChange implements SchemaChange {
  private final String tableName;

  public AddTableChange(String tableName) {
    this.tableName = tableName;
  }

  public String getTableName() {
    return tableName;
  }
}
