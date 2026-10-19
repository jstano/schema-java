package com.stano.schema.gendiagram.impl;

import com.stano.schema.model.Column;
import com.stano.schema.model.ColumnPair;
import com.stano.schema.model.Relation;
import com.stano.schema.model.Table;

/** Crow's-foot cardinality shared by the ER diagram generators. */
final class Cardinality {
  private Cardinality() {}

  /**
   * Returns the crow's-foot cardinality for a foreign key, derived from the nullability of its
   * local columns: the child side is always zero-or-many, and the parent side is exactly one only
   * when every FK column is required (otherwise the reference is optional). The delete action says
   * nothing about this, so it is deliberately not consulted.
   *
   * @param table the table declaring the relation
   * @param relation the relation to describe
   */
  static String of(Table table, Relation relation) {
    boolean allRequired = true;
    for (ColumnPair pair : relation.getColumnPairs()) {
      Column column = table.getColumn(pair.getFromColumnName());
      if (column == null || !column.isRequired()) {
        allRequired = false;
      }
    }
    return allRequired ? "}o--||" : "}o--o|";
  }
}
