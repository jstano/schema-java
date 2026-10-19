package com.stano.schema.diff.change;

import com.stano.schema.diff.SchemaChange;
import com.stano.schema.model.Trigger;

/**
 * Represents a trigger that is new on a table in the new schema, or whose definition changed.
 * Triggers are generated per table, so a generator regenerates the table's full trigger set.
 */
public final class AddTriggerChange implements SchemaChange {
  private final String tableName;
  private final Trigger trigger;

  /**
   * Creates the change.
   *
   * @param tableName the name of the table the trigger belongs to
   * @param trigger the trigger that was added
   */
  public AddTriggerChange(String tableName, Trigger trigger) {
    this.tableName = tableName;
    this.trigger = trigger;
  }

  /** Returns the name of the table the trigger belongs to. */
  public String getTableName() {
    return tableName;
  }

  /** Returns the trigger that was added. */
  public Trigger getTrigger() {
    return trigger;
  }
}
