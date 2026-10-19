package com.stano.schema.diff.change;

import com.stano.schema.diff.SchemaChange;
import com.stano.schema.model.InitialData;

/**
 * Represents an initial-data statement that no longer exists on a table. Generators only emit a
 * review comment for it, since the rows it inserted may have been changed since.
 */
public final class DropInitialDataChange implements SchemaChange {
  private final String tableName;
  private final InitialData initialData;

  /**
   * Creates the change.
   *
   * @param tableName the name of the table the data belonged to
   * @param initialData the removed statement
   */
  public DropInitialDataChange(String tableName, InitialData initialData) {
    this.tableName = tableName;
    this.initialData = initialData;
  }

  /** Returns the name of the table the data belonged to. */
  public String getTableName() {
    return tableName;
  }

  /** Returns the removed statement. */
  public InitialData getInitialData() {
    return initialData;
  }
}
