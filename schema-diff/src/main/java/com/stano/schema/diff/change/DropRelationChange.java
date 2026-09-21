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
  private final int ordinal;

  /**
   * Creates a change describing a relation to drop.
   *
   * @param relation the definition of the dropped relation
   * @param ordinal the 1-based position of this relation among the owning table's own relations —
   *     matches the name the SQL generator's create path would have given this relation (e.g.
   *     {@code fk_<table><ordinal>})
   */
  public DropRelationChange(Relation relation, int ordinal) {
    this.relation = relation;
    this.ordinal = ordinal;
  }

  /** Returns the definition of the dropped relation. */
  public Relation getRelation() {
    return relation;
  }

  /** Returns the 1-based position of this relation among the owning table's own relations. */
  public int getOrdinal() {
    return ordinal;
  }
}
