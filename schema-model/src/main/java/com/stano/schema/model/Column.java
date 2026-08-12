package com.stano.schema.model;

/**
 * A single column definition within a {@link Table}, corresponding to a {@code <column>} element in
 * the XML schema. Captures the column's SQL name, type, size, nullability, default/check/enum
 * constraints, and (for {@code array} columns) the element type.
 */
public class Column {
  private final String name;
  private final ColumnType type;
  private final int length;
  private final int scale;
  private final boolean required;
  private final String checkConstraint;
  private final String defaultConstraint;
  private final String generated;
  private final String minValue;
  private final String maxValue;
  private final String enumType;
  private final ColumnType elementType;

  /**
   * Creates a column with no length/scale-related constraints, check constraint, default value, or
   * enum type.
   *
   * @param name the column's SQL name
   * @param type the column's data type
   * @param length the column's length (for sized types such as {@code varchar} or {@code char})
   * @param required whether the column is {@code NOT NULL}
   */
  public Column(String name, ColumnType type, int length, boolean required) {
    this.name = name;
    this.type = type;
    this.length = length;
    this.scale = 0;
    this.required = required;
    this.checkConstraint = null;
    this.defaultConstraint = null;
    this.generated = null;
    this.minValue = null;
    this.maxValue = null;
    this.enumType = null;
    this.elementType = null;
  }

  /**
   * Creates a column with a raw SQL check constraint.
   *
   * @param name the column's SQL name
   * @param type the column's data type
   * @param length the column's length (for sized types such as {@code varchar} or {@code char})
   * @param required whether the column is {@code NOT NULL}
   * @param checkConstraint a raw SQL check-constraint expression applied to the column
   */
  public Column(
      String name, ColumnType type, int length, boolean required, String checkConstraint) {
    this.name = name;
    this.type = type;
    this.length = length;
    this.scale = 0;
    this.required = required;
    this.checkConstraint = checkConstraint;
    this.defaultConstraint = null;
    this.generated = null;
    this.minValue = null;
    this.maxValue = null;
    this.enumType = null;
    this.elementType = null;
  }

  /**
   * Creates a fully-specified column.
   *
   * @param name the column's SQL name
   * @param type the column's data type
   * @param length the column's length (for sized types such as {@code varchar} or {@code char})
   * @param scale the column's decimal scale (for {@code decimal} columns)
   * @param required whether the column is {@code NOT NULL}
   * @param checkConstraint a raw SQL check-constraint expression applied to the column
   * @param defaultConstraint the column's default value expression, or {@code null} for none
   * @param generated the generated-column expression, or {@code null} if not a generated column
   * @param minValue the minimum allowed value, enforced via a generated check constraint
   * @param maxValue the maximum allowed value, enforced via a generated check constraint
   * @param enumType the name of the {@link EnumType} this column's values are restricted to, for
   *     {@code enum} columns
   * @param elementType the element type of the column, for {@code array} columns
   */
  public Column(
      String name,
      ColumnType type,
      int length,
      int scale,
      boolean required,
      String checkConstraint,
      String defaultConstraint,
      String generated,
      String minValue,
      String maxValue,
      String enumType,
      ColumnType elementType) {
    this.name = name;
    this.type = type;
    this.length = length;
    this.scale = scale;
    this.required = required;
    this.checkConstraint = checkConstraint;
    this.defaultConstraint = defaultConstraint;
    this.generated = generated;
    this.minValue = minValue;
    this.maxValue = maxValue;
    this.enumType = enumType;
    this.elementType = elementType;
  }

  /** Returns the column's SQL name. */
  public String getName() {
    return name;
  }

  /** Returns the column's data type. */
  public ColumnType getType() {
    return type;
  }

  /** Returns the column's length, for sized types such as {@code varchar} or {@code char}. */
  public int getLength() {
    return length;
  }

  /** Returns the column's decimal scale, for {@code decimal} columns. */
  public int getScale() {
    return scale;
  }

  /** Returns whether the column is {@code NOT NULL}. */
  public boolean isRequired() {
    return required;
  }

  /** Returns the raw SQL check-constraint expression applied to the column, if any. */
  public String getCheckConstraint() {
    return checkConstraint;
  }

  /** Returns the column's default value expression, or {@code null} for none. */
  public String getDefaultConstraint() {
    return defaultConstraint;
  }

  /** Returns the generated-column expression, or {@code null} if not a generated column. */
  public String getGenerated() {
    return generated;
  }

  /** Returns the minimum allowed value, enforced via a generated check constraint. */
  public String getMinValue() {
    return minValue;
  }

  /** Returns the maximum allowed value, enforced via a generated check constraint. */
  public String getMaxValue() {
    return maxValue;
  }

  /** Returns the name of the {@link EnumType} this column's values are restricted to. */
  public String getEnumType() {
    return enumType;
  }

  /** Returns the element type of the column, for {@code array} columns. */
  public ColumnType getElementType() {
    return elementType;
  }

  /**
   * Determines whether this column needs a generated SQL check constraint, based on its check
   * expression, min/max values, enum type restriction, or (when the given {@link BooleanMode} is
   * not {@link BooleanMode#NATIVE}) its being a boolean column.
   *
   * @param booleanMode the boolean representation mode in effect for SQL generation
   * @return {@code true} if a check constraint must be generated for this column
   */
  public boolean needsCheckConstraints(BooleanMode booleanMode) {
    return checkConstraint != null
        || minValue != null
        || maxValue != null
        || enumType != null
        || (type == ColumnType.BOOLEAN && booleanMode != BooleanMode.NATIVE);
  }

  /** Returns whether this column has a minimum and/or maximum value restriction. */
  public boolean hasMinOrMaxValue() {
    return minValue != null || maxValue != null;
  }
}
