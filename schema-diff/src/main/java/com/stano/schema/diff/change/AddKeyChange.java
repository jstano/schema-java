package com.stano.schema.diff.change;

import com.stano.schema.diff.SchemaChange;
import com.stano.schema.model.Key;

public final class AddKeyChange implements SchemaChange {
  private final String tableName;
  private final Key key;

  public AddKeyChange(String tableName, Key key) {
    this.tableName = tableName;
    this.key = key;
  }

  public String getTableName() {
    return tableName;
  }

  public Key getKey() {
    return key;
  }
}
