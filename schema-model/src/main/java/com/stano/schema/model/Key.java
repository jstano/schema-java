package com.stano.schema.model;

import java.util.List;

/**
 * A key definition on a {@link Table}, corresponding to a {@code <primary>}, {@code <unique>}, or
 * {@code <index>} element within the table's {@code <keys>} block. Carries the ordered list of
 * columns and database-specific options such as clustering, compression, uniqueness, and included
 * (covering) columns.
 */
public class Key {
  private final KeyType type;
  private final List<KeyColumn> columns;
  private final boolean cluster;
  private final boolean compress;
  private final boolean unique;
  private final String include;

  /**
   * Creates a fully-specified key.
   *
   * @param type whether this is a primary key, unique key, or index
   * @param columns the ordered columns making up the key
   * @param cluster whether the key should be created as a clustered index
   * @param compress whether the key should be created with data compression enabled
   * @param unique whether the key enforces uniqueness (for indexes)
   * @param include a comma-separated list of covering (included) columns, or {@code null}
   */
  public Key(
      KeyType type,
      List<KeyColumn> columns,
      boolean cluster,
      boolean compress,
      boolean unique,
      String include) {
    this.type = type;
    this.columns = List.copyOf(columns);
    this.cluster = cluster;
    this.compress = compress;
    this.unique = unique;
    this.include = include;
  }

  /**
   * Creates a key with default options: not clustered, not compressed, not unique, and no included
   * columns.
   *
   * @param type whether this is a primary key, unique key, or index
   * @param columns the ordered columns making up the key
   */
  public Key(KeyType type, List<KeyColumn> columns) {
    this.type = type;
    this.columns = List.copyOf(columns);
    this.cluster = false;
    this.compress = false;
    this.unique = false;
    this.include = null;
  }

  /** Returns whether this is a primary key, unique key, or index. */
  public KeyType getType() {
    return type;
  }

  /** Returns the ordered columns making up the key. */
  public List<KeyColumn> getColumns() {
    return columns;
  }

  /** Returns whether the key should be created as a clustered index. */
  public boolean isCluster() {
    return cluster;
  }

  /** Returns whether the key should be created with data compression enabled. */
  public boolean isCompress() {
    return compress;
  }

  /** Returns whether the key enforces uniqueness (for indexes). */
  public boolean isUnique() {
    return unique;
  }

  /** Returns the comma-separated list of covering (included) columns, or {@code null}. */
  public String getInclude() {
    return include;
  }

  /**
   * Determines whether this key includes a column with the given name.
   *
   * @param columnName the column name to look for (compared case-sensitively)
   * @return {@code true} if a column of the key matches {@code columnName}
   */
  public boolean containsColumn(String columnName) {
    return columns.stream().map(KeyColumn::getName).anyMatch(colName -> colName.equals(columnName));
  }

  /**
   * Builds a comma-separated string of this key's column names, in key order.
   *
   * @return the comma-joined column names
   */
  public String getColumnsAsString() {
    return String.join(",", columns.stream().map(KeyColumn::getName).toArray(String[]::new));
  }
}
