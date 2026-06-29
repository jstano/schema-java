package com.stano.schema.diff.change;

import com.stano.schema.diff.SchemaChange;
import com.stano.schema.model.Function;

public final class AddFunctionChange implements SchemaChange {
  private final Function function;

  public AddFunctionChange(Function function) {
    this.function = function;
  }

  public Function getFunction() {
    return function;
  }
}
