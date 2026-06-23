package com.stano.schema.diff.change;

import com.stano.schema.diff.SchemaChange;

public final class DropViewChange implements SchemaChange {
  private final String viewName;

  public DropViewChange(String viewName) {
    this.viewName = viewName;
  }

  public String getViewName() {
    return viewName;
  }
}
