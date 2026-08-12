package com.stano.schema.diff.change;

import com.stano.schema.diff.SchemaChange;
import com.stano.schema.model.Key;

/**
 * Represents a key (e.g. primary or unique key) that exists on a table in the new schema but not in
 * the old one, and so must be added. Carries the owning table's name and the full {@link Key}
 * definition to add.
 */
public final class AddKeyChange implements SchemaChange {
  private final String tableName;
  private final Key key;

  /**
   * Creates a change describing a key to add.
   *
   * @param tableName the name of the table the key is added to
   * @param key the definition of the new key
   */
  public AddKeyChange(String tableName, Key key) {
    this.tableName = tableName;
    this.key = key;
  }

  /** Returns the name of the table the key is added to. */
  public String getTableName() {
    return tableName;
  }

  /** Returns the definition of the new key. */
  public Key getKey() {
    return key;
  }
}
