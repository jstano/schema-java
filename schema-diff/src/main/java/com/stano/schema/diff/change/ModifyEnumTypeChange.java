package com.stano.schema.diff.change;

import com.stano.schema.diff.SchemaChange;
import com.stano.schema.model.EnumType;

/**
 * Represents an enum type present in both schemas whose value list (names or codes) differs.
 * Carries both definitions so a generator can work out which values were added and removed.
 */
public final class ModifyEnumTypeChange implements SchemaChange {
  private final EnumType oldEnumType;
  private final EnumType newEnumType;

  /**
   * Creates the change.
   *
   * @param oldEnumType the enum type as it was in the old schema
   * @param newEnumType the enum type as it is in the new schema
   */
  public ModifyEnumTypeChange(EnumType oldEnumType, EnumType newEnumType) {
    this.oldEnumType = oldEnumType;
    this.newEnumType = newEnumType;
  }

  /** Returns the enum type as it was in the old schema. */
  public EnumType getOldEnumType() {
    return oldEnumType;
  }

  /** Returns the enum type as it is in the new schema. */
  public EnumType getNewEnumType() {
    return newEnumType;
  }
}
