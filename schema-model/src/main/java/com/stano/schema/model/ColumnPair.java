package com.stano.schema.model;

/**
 * One column mapping within a composite {@link Relation}, pairing a source (child) column with the
 * referenced (parent) column it maps to.
 */
public class ColumnPair {
  private final String fromColumnName;
  private final String toColumnName;

  /**
   * Creates a column pair.
   *
   * @param fromColumnName the name of the source column holding the foreign key value
   * @param toColumnName the name of the referenced (parent) column
   */
  public ColumnPair(String fromColumnName, String toColumnName) {
    this.fromColumnName = fromColumnName;
    this.toColumnName = toColumnName;
  }

  /** Returns the name of the source column holding the foreign key value. */
  public String getFromColumnName() {
    return fromColumnName;
  }

  /** Returns the name of the referenced (parent) column. */
  public String getToColumnName() {
    return toColumnName;
  }
}
