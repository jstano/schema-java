package com.stano.schema.diff.change;

import com.stano.schema.diff.SchemaChange;
import com.stano.schema.model.Column;

public final class AddColumnChange implements SchemaChange {
  private final String tableName;
  private final Column column;

  public AddColumnChange(String tableName, Column column) {
    this.tableName = tableName;
    this.column = column;
  }

  public String getTableName() {
    return tableName;
  }

  public Column getColumn() {
    return column;
  }
}
