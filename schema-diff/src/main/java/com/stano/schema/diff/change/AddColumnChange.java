package com.stano.schema.diff.change;

import com.stano.schema.diff.SchemaChange;
import com.stano.schema.model.Column;

/**
 * Represents a column that exists in the new schema but not in the old one, and so must be added to
 * an existing table. Carries the owning table's name and the full {@link Column} definition to add.
 */
public final class AddColumnChange implements SchemaChange {
  private final String tableName;
  private final Column column;

  /**
   * Creates a change describing a column to add.
   *
   * @param tableName the name of the table the column is added to
   * @param column the definition of the new column
   */
  public AddColumnChange(String tableName, Column column) {
    this.tableName = tableName;
    this.column = column;
  }

  /** Returns the name of the table the column is added to. */
  public String getTableName() {
    return tableName;
  }

  /** Returns the definition of the new column. */
  public Column getColumn() {
    return column;
  }
}
