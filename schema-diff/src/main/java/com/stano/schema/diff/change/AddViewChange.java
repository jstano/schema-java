package com.stano.schema.diff.change;

import com.stano.schema.diff.SchemaChange;
import com.stano.schema.model.View;

/**
 * Represents a view that must be created because it is new in the new schema, or whose SQL
 * definition differs from the old schema's version (in which case a matching {@code DropViewChange}
 * for the old definition precedes this change). Carries the full {@link View} definition, including
 * its name and SQL, to create.
 */
public final class AddViewChange implements SchemaChange {
  private final View view;

  /**
   * Creates a change describing a view to add.
   *
   * @param view the definition of the view to create
   */
  public AddViewChange(View view) {
    this.view = view;
  }

  /** Returns the definition of the view to create. */
  public View getView() {
    return view;
  }
}
