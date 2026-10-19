package com.stano.schema.diff.change;

import com.stano.schema.diff.SchemaChange;
import com.stano.schema.model.Trigger;

/**
 * Represents a trigger that no longer exists on a table in the new schema. Triggers are generated
 * per table, so a generator regenerates the table's remaining trigger set.
 */
public final class DropTriggerChange implements SchemaChange {
  private final String tableName;
  private final Trigger trigger;

  /**
   * Creates the change.
   *
   * @param tableName the name of the table the trigger belonged to
   * @param trigger the trigger that was removed
   */
  public DropTriggerChange(String tableName, Trigger trigger) {
    this.tableName = tableName;
    this.trigger = trigger;
  }

  /** Returns the name of the table the trigger belonged to. */
  public String getTableName() {
    return tableName;
  }

  /** Returns the trigger that was removed. */
  public Trigger getTrigger() {
    return trigger;
  }
}
