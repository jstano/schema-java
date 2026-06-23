package com.stano.schema.diff.change;

import com.stano.schema.diff.SchemaChange;

public final class RenameTableChange implements SchemaChange {
  private final String oldName;
  private final String newName;

  public RenameTableChange(String oldName, String newName) {
    this.oldName = oldName;
    this.newName = newName;
  }

  public String getOldName() {
    return oldName;
  }

  public String getNewName() {
    return newName;
  }
}
