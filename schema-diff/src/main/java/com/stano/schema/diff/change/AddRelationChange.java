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

  /**
   * Creates a change describing a relation to add.
   *
   * @param relation the definition of the new relation
   */
  public AddRelationChange(Relation relation) {
    this.relation = relation;
  }

  /** Returns the definition of the new relation. */
  public Relation getRelation() {
    return relation;
  }
}
