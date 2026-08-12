package com.stano.schema.model;

/**
 * A single column reference within a {@link Key}, corresponding to a {@code <column name="..."/>}
 * element inside a {@code <primary>}, {@code <unique>}, or {@code <index>} block.
 */
public class KeyColumn {
  private final String name;

  /**
   * Creates a key column reference.
   *
   * @param name the referenced column's name
   */
  public KeyColumn(String name) {
    this.name = name;
  }

  /** Returns the referenced column's name. */
  public String getName() {
    return name;
  }
}
