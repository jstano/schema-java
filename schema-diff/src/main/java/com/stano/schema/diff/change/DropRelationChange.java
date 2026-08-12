package com.stano.schema.diff.change;

import com.stano.schema.diff.SchemaChange;
import com.stano.schema.model.Relation;

/**
 * Represents a foreign-key relation between two tables that exists in the old schema but not in the
 * new one (matched by from/to table and column), and so must be dropped. Carries the full {@link
 * Relation} definition to drop.
 */
public final class DropRelationChange implements SchemaChange {
  private final Relation relation;

  /**
   * Creates a change describing a relation to drop.
   *
   * @param relation the definition of the dropped relation
   */
  public DropRelationChange(Relation relation) {
    this.relation = relation;
  }

  /** Returns the definition of the dropped relation. */
  public Relation getRelation() {
    return relation;
  }
}
