package com.stano.schema.diff.change;

import com.stano.schema.diff.SchemaChange;

/**
 * Represents a constraint that exists on a table in the old schema but not in the new one, and so
 * must be dropped. Carries the owning table's name and the dropped constraint's name.
 */
public final class DropConstraintChange implements SchemaChange {
  private final String tableName;
  private final String constraintName;

  /**
   * Creates a change describing a constraint to drop.
   *
   * @param tableName the name of the table the constraint is dropped from
   * @param constraintName the name of the dropped constraint
   */
  public DropConstraintChange(String tableName, String constraintName) {
    this.tableName = tableName;
    this.constraintName = constraintName;
  }

  /** Returns the name of the table the constraint is dropped from. */
  public String getTableName() {
    return tableName;
  }

  /** Returns the name of the dropped constraint. */
  public String getConstraintName() {
    return constraintName;
  }
}
