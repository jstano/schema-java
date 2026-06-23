package com.stano.schema.diff.change;

import com.stano.schema.diff.SchemaChange;
import com.stano.schema.model.Constraint;

public final class AddConstraintChange implements SchemaChange {
  private final String tableName;
  private final Constraint constraint;

  public AddConstraintChange(String tableName, Constraint constraint) {
    this.tableName = tableName;
    this.constraint = constraint;
  }

  public String getTableName() {
    return tableName;
  }

  public Constraint getConstraint() {
    return constraint;
  }
}
