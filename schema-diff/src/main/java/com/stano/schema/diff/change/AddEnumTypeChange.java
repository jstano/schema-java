package com.stano.schema.diff.change;

import com.stano.schema.diff.SchemaChange;
import com.stano.schema.model.EnumType;

/**
 * Represents an enum type that exists in the new schema but not in the old one and so must be
 * created.
 */
public final class AddEnumTypeChange implements SchemaChange {
  private final EnumType enumType;

  /**
   * Creates the change.
   *
   * @param enumType the enum type to create
   */
  public AddEnumTypeChange(EnumType enumType) {
    this.enumType = enumType;
  }

  /** Returns the enum type to create. */
  public EnumType getEnumType() {
    return enumType;
  }
}
