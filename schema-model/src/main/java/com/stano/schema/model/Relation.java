package com.stano.schema.model;

/**
 * A foreign-key relationship between two tables, corresponding to a {@code <relation>} element
 * within a table's {@code <relations>} block. Describes the source (child) column, the referenced
 * (parent) table and column, and the {@link RelationType} behavior applied when the parent row is
 * deleted or updated. Reverse relations (parent-to-child) are also represented by this class; see
 * {@link Table#getReverseRelations()}.
 */
public class Relation {
  private final String fromTableName;
  private final String fromColumnName;
  private final String toTableName;
  private final String toColumnName;
  private final RelationType type;
  private final boolean disableUsageChecking;

  /**
   * Creates a relation.
   *
   * @param fromTableName the name of the table declaring the foreign key (the source/child table)
   * @param fromColumnName the name of the source column holding the foreign key value
   * @param toTableName the name of the referenced (parent) table
   * @param toColumnName the name of the referenced (parent) column
   * @param type the behavior applied on delete/update of the referenced row
   * @param disableUsageChecking whether usage/referential checks should be skipped for this
   *     relation
   */
  public Relation(
      String fromTableName,
      String fromColumnName,
      String toTableName,
      String toColumnName,
      RelationType type,
      boolean disableUsageChecking) {
    this.fromTableName = fromTableName;
    this.fromColumnName = fromColumnName;
    this.toTableName = toTableName;
    this.toColumnName = toColumnName;
    this.type = type;
    this.disableUsageChecking = disableUsageChecking;
  }

  /** Returns the name of the table declaring the foreign key (the source/child table). */
  public String getFromTableName() {
    return fromTableName;
  }

  /** Returns the name of the source column holding the foreign key value. */
  public String getFromColumnName() {
    return fromColumnName;
  }

  /** Returns the name of the referenced (parent) table. */
  public String getToTableName() {
    return toTableName;
  }

  /** Returns the name of the referenced (parent) column. */
  public String getToColumnName() {
    return toColumnName;
  }

  /** Returns the behavior applied on delete/update of the referenced row. */
  public RelationType getType() {
    return type;
  }

  /** Returns whether usage/referential checks should be skipped for this relation. */
  public boolean isDisableUsageChecking() {
    return disableUsageChecking;
  }
}
