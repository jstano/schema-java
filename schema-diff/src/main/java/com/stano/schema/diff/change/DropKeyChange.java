package com.stano.schema.diff.change;

import com.stano.schema.diff.SchemaChange;
import com.stano.schema.model.Key;

/**
 * Represents a key (e.g. primary or unique key) that exists on a table in the old schema but not in
 * the new one, and so must be dropped. Carries the owning table's name and the full {@link Key}
 * definition to drop.
 */
public final class DropKeyChange implements SchemaChange {
  private final String tableName;
  private final Key key;

  /**
   * Creates a change describing a key to drop.
   *
   * @param tableName the name of the table the key is dropped from
   * @param key the definition of the dropped key
   */
  public DropKeyChange(String tableName, Key key) {
    this.tableName = tableName;
    this.key = key;
  }

  /** Returns the name of the table the key is dropped from. */
  public String getTableName() {
    return tableName;
  }

  /** Returns the definition of the dropped key. */
  public Key getKey() {
    return key;
  }
}
