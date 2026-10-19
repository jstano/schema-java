package com.stano.schema.diff.change;

import com.stano.schema.diff.SchemaChange;

/**
 * Represents an enum type that exists in the old schema but not in the new one and so must be
 * dropped.
 */
public final class DropEnumTypeChange implements SchemaChange {
  private final String enumTypeName;

  /**
   * Creates the change.
   *
   * @param enumTypeName the name of the dropped enum type
   */
  public DropEnumTypeChange(String enumTypeName) {
    this.enumTypeName = enumTypeName;
  }

  /** Returns the name of the dropped enum type. */
  public String getEnumTypeName() {
    return enumTypeName;
  }
}
