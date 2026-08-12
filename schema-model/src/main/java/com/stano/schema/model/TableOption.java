package com.stano.schema.model;

/**
 * Boolean-style options that may be set on a {@link Table}, corresponding to attributes such as
 * {@code data}, {@code compress}, and no-export flags on the {@code <table>} element.
 */
public enum TableOption {
  DATA,
  NO_EXPORT,
  COMPRESS
}
