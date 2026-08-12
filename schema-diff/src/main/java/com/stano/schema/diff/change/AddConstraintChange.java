package com.stano.schema.diff.change;

import com.stano.schema.diff.SchemaChange;
import com.stano.schema.model.Constraint;

/**
 * Represents a constraint that exists on a table in the new schema but not in the old one, and so
 * must be added. Carries the owning table's name and the full {@link Constraint} definition to add.
 */
public final class AddConstraintChange implements SchemaChange {
  private final String tableName;
  private final Constraint constraint;

  /**
   * Creates a change describing a constraint to add.
   *
   * @param tableName the name of the table the constraint is added to
   * @param constraint the definition of the new constraint
   */
  public AddConstraintChange(String tableName, Constraint constraint) {
    this.tableName = tableName;
    this.constraint = constraint;
  }

  /** Returns the name of the table the constraint is added to. */
  public String getTableName() {
    return tableName;
  }

  /** Returns the definition of the new constraint. */
  public Constraint getConstraint() {
    return constraint;
  }
}
