package com.stano.schema.diff.change;

import com.stano.schema.diff.SchemaChange;
import com.stano.schema.model.Function;

/**
 * Represents a database function that must be created because it is new in the new schema, or whose
 * SQL body differs from the old schema's version (in which case a matching {@code
 * DropFunctionChange} for the old definition precedes this change). Carries the full {@link
 * Function} definition, including its name, database type, and SQL, to create.
 */
public final class AddFunctionChange implements SchemaChange {
  private final Function function;

  /**
   * Creates a change describing a function to add.
   *
   * @param function the definition of the function to create
   */
  public AddFunctionChange(Function function) {
    this.function = function;
  }

  /** Returns the definition of the function to create. */
  public Function getFunction() {
    return function;
  }
}
