package com.stano.schema.diff.change;

import com.stano.schema.diff.SchemaChange;

/**
 * Represents a table that exists in the old schema but not in the new one, and so must be dropped.
 * Carries only the name of the table to drop; the table's columns, keys, constraints, and relations
 * are dropped implicitly along with it, and are also reported separately as their own {@code
 * Drop*Change} instances.
 */
public final class DropTableChange implements SchemaChange {
  private final String tableName;

  /**
   * Creates a change describing a table to drop.
   *
   * @param tableName the name of the dropped table
   */
  public DropTableChange(String tableName) {
    this.tableName = tableName;
  }

  /** Returns the name of the dropped table. */
  public String getTableName() {
    return tableName;
  }
}
