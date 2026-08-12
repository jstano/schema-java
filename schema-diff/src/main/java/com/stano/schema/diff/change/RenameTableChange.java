package com.stano.schema.diff.change;

import com.stano.schema.diff.SchemaChange;

/**
 * Represents a table that was renamed, carrying its old and new names.
 *
 * <p>Note: {@link com.stano.schema.diff.SchemaDiffEngine} does not currently construct this change
 * itself; a table missing under its old name and a table newly present under a different name are
 * instead reported as a separate {@code DropTableChange} and {@code AddTableChange}, with no rename
 * detection performed at the table level. This type exists for callers that want to model or apply
 * an explicit table rename.
 */
public final class RenameTableChange implements SchemaChange {
  private final String oldName;
  private final String newName;

  /**
   * Creates a change describing a table rename.
   *
   * @param oldName the table's name before the rename
   * @param newName the table's name after the rename
   */
  public RenameTableChange(String oldName, String newName) {
    this.oldName = oldName;
    this.newName = newName;
  }

  /** Returns the table's name before the rename. */
  public String getOldName() {
    return oldName;
  }

  /** Returns the table's name after the rename. */
  public String getNewName() {
    return newName;
  }
}
