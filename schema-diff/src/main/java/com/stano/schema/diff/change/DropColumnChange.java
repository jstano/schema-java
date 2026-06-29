package com.stano.schema.diff.change;

import com.stano.schema.diff.SchemaChange;
import java.util.List;

public final class DropColumnChange implements SchemaChange {
  private final String tableName;
  private final String columnName;
  private final List<String> renameCandidates;

  public DropColumnChange(String tableName, String columnName) {
    this(tableName, columnName, List.of());
  }

  public DropColumnChange(String tableName, String columnName, List<String> renameCandidates) {
    this.tableName = tableName;
    this.columnName = columnName;
    this.renameCandidates = List.copyOf(renameCandidates);
  }

  public String getTableName() {
    return tableName;
  }

  public String getColumnName() {
    return columnName;
  }

  public List<String> getRenameCandidates() {
    return renameCandidates;
  }
}
