package com.stano.schema.diff.change;

import com.stano.schema.diff.SchemaChange;
import com.stano.schema.model.Column;

/**
 * Represents a column that exists in both the old and new schema under the same name, but whose
 * definition (type, length, scale, required flag, or default constraint) differs between the two.
 * Carries the owning table's name along with the column's old and new definitions.
 */
public final class ModifyColumnChange implements SchemaChange {
  private final String tableName;
  private final Column oldColumn;
  private final Column newColumn;

  /**
   * Creates a change describing a column whose definition changed.
   *
   * @param tableName the name of the table the column belongs to
   * @param oldColumn the column's definition in the old schema
   * @param newColumn the column's definition in the new schema
   */
  public ModifyColumnChange(String tableName, Column oldColumn, Column newColumn) {
    this.tableName = tableName;
    this.oldColumn = oldColumn;
    this.newColumn = newColumn;
  }

  /** Returns the name of the table the column belongs to. */
  public String getTableName() {
    return tableName;
  }

  /** Returns the column's definition in the old schema. */
  public Column getOldColumn() {
    return oldColumn;
  }

  /** Returns the column's definition in the new schema. */
  public Column getNewColumn() {
    return newColumn;
  }
}
