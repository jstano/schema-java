package com.stano.schema.model;

/**
 * Controls a table's row-lock escalation behavior in databases that support it (e.g. SQL Server's
 * {@code LOCK_ESCALATION} table option): disabled, automatic, or escalate to a full table lock.
 */
public enum LockEscalation {
  DISABLE,
  AUTO,
  TABLE
}
