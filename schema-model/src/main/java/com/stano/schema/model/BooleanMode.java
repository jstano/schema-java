package com.stano.schema.model;

/**
 * Controls how {@code boolean} columns are represented in generated SQL: as a native boolean type,
 * or as textual {@code YES}/{@code NO} or {@code Y}/{@code N} values.
 */
public enum BooleanMode {
  NATIVE,
  YES_NO,
  YN
}
