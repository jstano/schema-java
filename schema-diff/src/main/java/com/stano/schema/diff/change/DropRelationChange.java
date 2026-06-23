package com.stano.schema.diff.change;

import com.stano.schema.diff.SchemaChange;
import com.stano.schema.model.Relation;

public final class DropRelationChange implements SchemaChange {
  private final Relation relation;

  public DropRelationChange(Relation relation) {
    this.relation = relation;
  }

  public Relation getRelation() {
    return relation;
  }
}
