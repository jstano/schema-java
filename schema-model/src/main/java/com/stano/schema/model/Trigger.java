package com.stano.schema.model;

/**
 * A database trigger attached to a {@link Table}, corresponding to an {@code <update>} or {@code
 * <delete>} element within the table's {@code <triggers>} block. Triggers are always
 * database-specific, with the raw trigger body supplied per {@link DatabaseType}.
 */
public class Trigger {
  private final String triggerText;
  private final TriggerType triggerType;
  private final DatabaseType databaseType;

  /**
   * Creates a trigger definition.
   *
   * @param triggerText the raw SQL trigger body
   * @param triggerType the operation the trigger fires on (update or delete)
   * @param databaseType the database type this trigger's SQL body is written for
   */
  public Trigger(String triggerText, TriggerType triggerType, DatabaseType databaseType) {
    this.triggerText = triggerText;
    this.triggerType = triggerType;
    this.databaseType = databaseType;
  }

  /** Returns the raw SQL trigger body. */
  public String getTriggerText() {
    return triggerText;
  }

  /** Returns the operation the trigger fires on (update or delete). */
  public TriggerType getTriggerType() {
    return triggerType;
  }

  /** Returns the database type this trigger's SQL body is written for. */
  public DatabaseType getDatabaseType() {
    return databaseType;
  }
}
