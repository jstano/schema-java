package com.stano.schema.diff.change;

import com.stano.schema.diff.SchemaChange;

/**
 * Represents a column that was renamed within the same table, carrying the table's name along with
 * the column's old and new names.
 *
 * <p>Note: {@link com.stano.schema.diff.SchemaDiffEngine} does not currently construct this change
 * itself; a removed and a differently-named added column are instead reported as a separate {@code
 * DropColumnChange} (with the new column's name surfaced only as a rename candidate) and {@code
 * AddColumnChange}. This type exists for callers that want to model or apply an explicit rename,
 * e.g. after resolving a {@code DropColumnChange}'s rename candidates.
 */
public final class RenameColumnChange implements SchemaChange {
  private final String tableName;
  private final String oldName;
  private final String newName;

  /**
   * Creates a change describing a column rename.
   *
   * @param tableName the name of the table the column belongs to
   * @param oldName the column's name before the rename
   * @param newName the column's name after the rename
   */
  public RenameColumnChange(String tableName, String oldName, String newName) {
    this.tableName = tableName;
    this.oldName = oldName;
    this.newName = newName;
  }

  /** Returns the name of the table the column belongs to. */
  public String getTableName() {
    return tableName;
  }

  /** Returns the column's name before the rename. */
  public String getOldName() {
    return oldName;
  }

  /** Returns the column's name after the rename. */
  public String getNewName() {
    return newName;
  }
}
