package com.stano.schema.model;

import java.util.stream.Stream;

/**
 * The set of column data types supported by the XML schema format's {@code type} attribute on
 * {@code <column>} elements (e.g. {@code sequence}, {@code varchar}, {@code decimal}, {@code enum},
 * {@code array}). Used both for a column's own type and, for {@code array} columns, its element
 * type.
 */
public enum ColumnType {
  SEQUENCE,
  LONGSEQUENCE,
  BYTE,
  SHORT,
  INT,
  LONG,
  FLOAT,
  DOUBLE,
  DECIMAL,
  BOOLEAN,
  DATE,
  DATETIME,
  TIME,
  TIMESTAMPTZ,
  TIMESTAMP,
  CHAR,
  VARCHAR,
  ENUM,
  TEXT,
  CITEXT,
  CSTEXT,
  BINARY,
  UUID,
  JSON,
  ARRAY;

  /** Returns whether this type is a textual/character-based type. */
  public boolean isText() {
    return this == CHAR
        || this == VARCHAR
        || this == ENUM
        || this == TEXT
        || this == CITEXT
        || this == CSTEXT
        || this == JSON
        || this == UUID;
  }

  /** Returns whether this type is a numeric type. */
  public boolean isNumeric() {
    return this == SEQUENCE
        || this == LONGSEQUENCE
        || this == BYTE
        || this == SHORT
        || this == INT
        || this == LONG
        || this == FLOAT
        || this == DOUBLE
        || this == DECIMAL;
  }

  /**
   * Looks up the {@code ColumnType} whose name matches the given XML {@code type} attribute value,
   * case-insensitively.
   *
   * @param typeName the type name as it appears in the XML schema (e.g. {@code "varchar"})
   * @return the matching {@code ColumnType}
   * @throws IllegalArgumentException if no type matches {@code typeName}
   */
  public static ColumnType getColumnType(String typeName) {
    return Stream.of(values())
        .filter(it -> it.name().equals(typeName.toUpperCase()))
        .findFirst()
        .orElseThrow(
            () -> new IllegalArgumentException("The type '" + typeName + "' is not valid."));
  }
}
