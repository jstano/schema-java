package com.stano.schema.diff.change;

import com.stano.schema.diff.SchemaChange;
import com.stano.schema.model.View;

public final class AddViewChange implements SchemaChange {
  private final View view;

  public AddViewChange(View view) {
    this.view = view;
  }

  public View getView() {
    return view;
  }
}
