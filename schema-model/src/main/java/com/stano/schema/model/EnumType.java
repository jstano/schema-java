package com.stano.schema.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A named enumeration type defined by an {@code <enum>} top-level element, made up of a list of
 * {@link EnumValue}s. Referenced by columns declared with {@code type="enum"} via the column's
 * {@code enumType} attribute.
 */
public class EnumType {
  private final String name;
  private final List<EnumValue> values = new ArrayList<>();

  /**
   * Creates an enum type with no values.
   *
   * @param name the enum type's name
   */
  public EnumType(String name) {
    this.name = name;
  }

  /** Returns the enum type's name. */
  public String getName() {
    return name;
  }

  /** Returns the enum type's values, in declaration order. */
  public List<EnumValue> getValues() {
    return values;
  }

  /**
   * Appends a value to this enum type.
   *
   * @param value the value to add
   */
  public void addValue(EnumValue value) {
    values.add(value);
  }
}
