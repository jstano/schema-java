package com.stano.schema.model;

import java.util.List;

/**
 * A foreign-key relationship between two tables, corresponding to a {@code <relation>} or {@code
 * <compositeRelation>} element within a table's {@code <relations>} block. Describes the source
 * (child) column(s), the referenced (parent) table and column(s), and the {@link RelationType}
 * behavior applied when the parent row is deleted or updated. Reverse relations (parent-to-child)
 * are also represented by this class; see {@link Table#getReverseRelations()}.
 *
 * <p>Most relations map a single source column to a single referenced column. A composite
 * (multi-column) relation, created via {@link #composite}, maps two or more column pairs at once;
 * {@link #getFromColumnName()} and {@link #getToColumnName()} return the first pair's columns as a
 * convenience for the (overwhelmingly common) single-column case.
 */
public class Relation {
  private final String fromTableName;
  private final String toTableName;
  private final List<ColumnPair> columnPairs;
  private final RelationType type;
  private final boolean disableUsageChecking;

  /**
   * Creates a single-column relation.
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
    this.toTableName = toTableName;
    this.columnPairs = List.of(new ColumnPair(fromColumnName, toColumnName));
    this.type = type;
    this.disableUsageChecking = disableUsageChecking;
  }

  /**
   * Creates a composite (multi-column) relation.
   *
   * @param fromTableName the name of the table declaring the foreign key (the source/child table)
   * @param toTableName the name of the referenced (parent) table
   * @param columnPairs the source-to-referenced column pairs making up the composite key; must
   *     contain at least two pairs
   * @param type the behavior applied on delete/update of the referenced row
   * @param disableUsageChecking whether usage/referential checks should be skipped for this
   *     relation
   * @throws IllegalArgumentException if fewer than two column pairs are given
   */
  public static Relation composite(
      String fromTableName,
      String toTableName,
      List<ColumnPair> columnPairs,
      RelationType type,
      boolean disableUsageChecking) {
    if (columnPairs == null || columnPairs.size() < 2) {
      throw new IllegalArgumentException(
          "composite relation requires at least 2 column pairs, got "
              + (columnPairs == null ? 0 : columnPairs.size()));
    }
    return new Relation(
        fromTableName, toTableName, List.copyOf(columnPairs), type, disableUsageChecking);
  }

  private Relation(
      String fromTableName,
      String toTableName,
      List<ColumnPair> columnPairs,
      RelationType type,
      boolean disableUsageChecking) {
    this.fromTableName = fromTableName;
    this.toTableName = toTableName;
    this.columnPairs = columnPairs;
    this.type = type;
    this.disableUsageChecking = disableUsageChecking;
  }

  /** Returns the name of the table declaring the foreign key (the source/child table). */
  public String getFromTableName() {
    return fromTableName;
  }

  /** Returns the name of the first pair's source column holding the foreign key value. */
  public String getFromColumnName() {
    return columnPairs.get(0).getFromColumnName();
  }

  /** Returns the name of the referenced (parent) table. */
  public String getToTableName() {
    return toTableName;
  }

  /** Returns the name of the first pair's referenced (parent) column. */
  public String getToColumnName() {
    return columnPairs.get(0).getToColumnName();
  }

  /** Returns the source-to-referenced column pairs making up this relation, in order. */
  public List<ColumnPair> getColumnPairs() {
    return columnPairs;
  }

  /** Returns whether this relation spans more than one column pair. */
  public boolean isComposite() {
    return columnPairs.size() > 1;
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
