package com.stano.schema.model;

/**
 * The referential-integrity behavior of a {@link Relation}, matching the {@code type} attribute of
 * a {@code <relation>} element ({@code cascade}, {@code enforce}, {@code setnull}, or {@code
 * donothing}).
 */
public enum RelationType {
  CASCADE,
  ENFORCE,
  SETNULL,
  DONOTHING
}
