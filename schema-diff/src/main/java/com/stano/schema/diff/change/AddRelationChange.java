package com.stano.schema.diff.change;

import com.stano.schema.diff.SchemaChange;
import com.stano.schema.model.Relation;

/**
 * Represents a foreign-key relation between two tables that exists in the new schema but not in the
 * old one (matched by from/to table and column), and so must be added. Carries the full {@link
 * Relation} definition to add.
 */
public final class AddRelationChange implements SchemaChange {
  private final Relation relation;
  private final int ordinal;

  /**
   * Creates a change describing a relation to add.
   *
   * @param relation the definition of the new relation
   * @param ordinal the 1-based position of this relation among the owning table's own relations —
   *     matches the name the SQL generator's create path would give this relation (e.g. {@code
   *     fk_<table><ordinal>})
   */
  public AddRelationChange(Relation relation, int ordinal) {
    this.relation = relation;
    this.ordinal = ordinal;
  }

  /** Returns the definition of the new relation. */
  public Relation getRelation() {
    return relation;
  }

  /** Returns the 1-based position of this relation among the owning table's own relations. */
  public int getOrdinal() {
    return ordinal;
  }
}
