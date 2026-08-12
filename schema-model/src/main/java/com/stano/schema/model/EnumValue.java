package com.stano.schema.model;

/**
 * A single named value within an {@link EnumType}, corresponding to a {@code <value>} element with
 * {@code name} and optional {@code code} attributes.
 */
public class EnumValue {
  private final String name;
  private final String code;

  /**
   * Creates an enum value.
   *
   * @param name the value's name
   * @param code the value's stored code, or {@code null} to fall back to {@code name}
   */
  public EnumValue(String name, String code) {
    this.name = name;
    this.code = code;
  }

  /** Returns the value's name. */
  public String getName() {
    return name;
  }

  /**
   * Returns the value's stored code, falling back to the value's {@linkplain #getName() name} if no
   * explicit code was set.
   *
   * @return the code to persist for this value
   */
  public String getCode() {
    if (code != null) {
      return code;
    }

    return name;
  }
}
