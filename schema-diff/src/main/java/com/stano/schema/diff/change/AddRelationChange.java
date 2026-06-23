package com.stano.schema.diff.change;

import com.stano.schema.diff.SchemaChange;
import com.stano.schema.model.Relation;

public final class AddRelationChange implements SchemaChange {
  private final Relation relation;

  public AddRelationChange(Relation relation) {
    this.relation = relation;
  }

  public Relation getRelation() {
    return relation;
  }
}
