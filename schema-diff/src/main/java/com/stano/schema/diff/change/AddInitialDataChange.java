package com.stano.schema.diff.change;

import com.stano.schema.diff.SchemaChange;
import com.stano.schema.model.InitialData;

/**
 * Represents an initial-data statement that is new on a table that exists in both schemas. (A
 * brand-new table's initial data is already part of its {@code AddTableChange}.)
 */
public final class AddInitialDataChange implements SchemaChange {
  private final String tableName;
  private final InitialData initialData;

  /**
   * Creates the change.
   *
   * @param tableName the name of the table the data belongs to
   * @param initialData the statement to run
   */
  public AddInitialDataChange(String tableName, InitialData initialData) {
    this.tableName = tableName;
    this.initialData = initialData;
  }

  /** Returns the name of the table the data belongs to. */
  public String getTableName() {
    return tableName;
  }

  /** Returns the statement to run. */
  public InitialData getInitialData() {
    return initialData;
  }
}
