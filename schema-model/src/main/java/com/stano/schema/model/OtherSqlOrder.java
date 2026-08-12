package com.stano.schema.model;

/**
 * Where an {@link OtherSql} fragment should be placed relative to the rest of the generated SQL
 * script, corresponding to the {@code order} attribute (e.g. {@code "top"} or {@code "bottom"}) on
 * an {@code <otherSql>} element.
 */
public enum OtherSqlOrder {
  BOTTOM,
  TOP
}
