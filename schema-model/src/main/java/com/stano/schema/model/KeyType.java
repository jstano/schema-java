package com.stano.schema.model;

/**
 * The kind of key a {@link Key} represents, mapping to the {@code <primary>}, {@code <unique>}, and
 * {@code <index>} elements of the XML schema's {@code <keys>} block.
 */
public enum KeyType {
  PRIMARY,
  UNIQUE,
  INDEX
}
