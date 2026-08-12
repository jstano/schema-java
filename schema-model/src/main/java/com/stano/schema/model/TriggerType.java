package com.stano.schema.model;

/**
 * The database operation a {@link Trigger} fires on, matching the {@code <update>} and {@code
 * <delete>} elements within a table's {@code <triggers>} block.
 */
public enum TriggerType {
  UPDATE,
  DELETE
}
