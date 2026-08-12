package com.stano.schema.model;

/**
 * Controls how foreign-key relationships declared via {@link Relation}s are enforced in generated
 * SQL: not at all, as native foreign key constraints, or via triggers.
 */
public enum ForeignKeyMode {
  NONE,
  RELATIONS,
  TRIGGERS
}
