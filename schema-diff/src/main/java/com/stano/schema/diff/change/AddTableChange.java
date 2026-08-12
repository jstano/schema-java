package com.stano.schema.diff.change;

import com.stano.schema.diff.SchemaChange;

/**
 * Represents a table that exists in the new schema but not in the old one, and so must be created.
 * Carries only the name of the table to add; the table's columns, keys, constraints, and relations
 * are reported separately as their own {@code Add*Change} instances.
 */
public final class AddTableChange implements SchemaChange {
  private final String tableName;

  /**
   * Creates a change describing a table to add.
   *
   * @param tableName the name of the new table
   */
  public AddTableChange(String tableName) {
    this.tableName = tableName;
  }

  /** Returns the name of the new table. */
  public String getTableName() {
    return tableName;
  }
}
