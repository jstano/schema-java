package com.stano.schema.diff.change;

import com.stano.schema.diff.SchemaChange;

/**
 * Represents a view that must be dropped because it no longer exists in the new schema, or because
 * its SQL definition changed (in which case an {@code AddViewChange} with the new definition
 * follows this change). Carries only the name of the dropped view.
 */
public final class DropViewChange implements SchemaChange {
  private final String viewName;

  /**
   * Creates a change describing a view to drop.
   *
   * @param viewName the name of the dropped view
   */
  public DropViewChange(String viewName) {
    this.viewName = viewName;
  }

  /** Returns the name of the dropped view. */
  public String getViewName() {
    return viewName;
  }
}
