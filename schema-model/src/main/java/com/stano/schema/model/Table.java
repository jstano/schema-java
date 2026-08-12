package com.stano.schema.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.map.CaseInsensitiveMap;

/**
 * A relational table definition corresponding to a {@code <table>} top-level element in the XML
 * schema. Aggregates the table's columns, keys/indexes, foreign-key relations, triggers,
 * constraints, initial data, options, and aggregation rollups.
 */
public class Table {
  private final Schema schema;
  private final String schemaName;
  private final String name;
  private final String exportDateColumn;
  private final LockEscalation lockEscalation;
  private final boolean noExport;

  private final List<Column> columns = new ArrayList<>();
  private final List<Key> keys = new ArrayList<>();
  private final List<Key> indexes = new ArrayList<>();
  private final List<Relation> relations = new ArrayList<>();
  private final List<Relation> reverseRelations = new ArrayList<>();
  private final List<Trigger> triggers = new ArrayList<>();
  private final List<Constraint> constraints = new ArrayList<>();
  private final List<InitialData> initialData = new ArrayList<>();
  private final List<TableOption> options = new ArrayList<>();
  private final List<Aggregation> aggregations = new ArrayList<>();
  private final Map<String, Column> columnMap = new CaseInsensitiveMap<>();

  /**
   * Creates a table with no columns, keys, relations, triggers, constraints, initial data, options,
   * or aggregations. These are added afterward via the corresponding mutable lists.
   *
   * @param schema the schema this table belongs to
   * @param schemaName the name of the containing schema namespace, or {@code null} if none
   * @param name the table's name
   * @param exportDateColumn the name of the column used to track export/change dates, or {@code
   *     null} if none
   * @param lockEscalation the row-lock escalation behavior for this table
   * @param noExport whether the table is excluded from data export
   */
  public Table(
      Schema schema,
      String schemaName,
      String name,
      String exportDateColumn,
      LockEscalation lockEscalation,
      boolean noExport) {
    this.schema = schema;
    this.schemaName = schemaName;
    this.name = name;
    this.exportDateColumn = exportDateColumn;
    this.lockEscalation = lockEscalation;
    this.noExport = noExport;
  }

  /** Returns the schema this table belongs to. */
  public Schema getSchema() {
    return schema;
  }

  /** Returns the name of the containing schema namespace, or {@code null} if none. */
  public String getSchemaName() {
    return schemaName;
  }

  /** Returns the table's name. */
  public String getName() {
    return name;
  }

  /** Returns the row-lock escalation behavior for this table. */
  public LockEscalation getLockEscalation() {
    return lockEscalation;
  }

  /** Returns whether the table is excluded from data export. */
  public boolean isNoExport() {
    return noExport;
  }

  /** Returns the table's columns, in declaration order. */
  public List<Column> getColumns() {
    return columns;
  }

  /** Returns the table's keys (primary and unique keys). */
  public List<Key> getKeys() {
    return keys;
  }

  /** Returns the table's indexes. */
  public List<Key> getIndexes() {
    return indexes;
  }

  /** Returns the foreign-key relations declared by this table (this table is the child side). */
  public List<Relation> getRelations() {
    return relations;
  }

  /**
   * Returns the reverse relations for this table (this table is the parent side), populated by
   * {@link Schema#buildReverseRelations()}.
   */
  public List<Relation> getReverseRelations() {
    return reverseRelations;
  }

  /** Returns the triggers defined on this table. */
  public List<Trigger> getTriggers() {
    return triggers;
  }

  /** Returns the raw SQL constraints defined on this table. */
  public List<Constraint> getConstraints() {
    return constraints;
  }

  /** Returns the initial-data SQL statements seeding this table. */
  public List<InitialData> getInitialData() {
    return initialData;
  }

  /** Returns the boolean-style options set on this table. */
  public List<TableOption> getOptions() {
    return options;
  }

  /** Returns the aggregation rollups sourced from this table. */
  public List<Aggregation> getAggregations() {
    return aggregations;
  }

  /**
   * Looks up a column by name, case-insensitively. Lazily builds and caches a name-to-column index
   * on first use.
   *
   * @param columnName the column name to look up
   * @return the matching column, or {@code null} if none exists
   */
  public Column getColumn(String columnName) {
    if (columnMap.isEmpty()) {
      for (Column column : columns) {
        columnMap.put(column.getName(), column);
      }
    }

    return columnMap.get(columnName);
  }

  /**
   * Finds this table's primary key.
   *
   * @return the primary key, or {@code null} if the table has none
   */
  public Key getPrimaryKey() {
    for (Key key : keys) {
      if (key.getType() == KeyType.PRIMARY) {
        return key;
      }
    }

    return null;
  }

  /**
   * Determines whether this table has a column with the given name, case-insensitively.
   *
   * @param columnName the column name to look for
   * @return {@code true} if a matching column exists
   */
  public boolean hasColumn(String columnName) {
    for (Column column : columns) {
      if (column.getName().equalsIgnoreCase(columnName)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Finds this table's identity/auto-generated column.
   *
   * @return the first column of type {@link ColumnType#SEQUENCE} or {@link
   *     ColumnType#LONGSEQUENCE}, or {@code null} if the table has none
   */
  public Column getIdentityColumn() {
    for (Column column : columns) {
      if (column.getType() == ColumnType.SEQUENCE || column.getType() == ColumnType.LONGSEQUENCE) {
        return column;
      }
    }

    return null;
  }

  /**
   * Returns the names of the columns making up this table's primary key, in key order.
   *
   * @return the primary key's column names, or {@code null} if the table has no primary key
   */
  public List<String> getPrimaryKeyColumns() {
    for (Key key : keys) {
      if (key.getType() == KeyType.PRIMARY) {
        return key.getColumns().stream().map(KeyColumn::getName).toList();
      }
    }

    return null;
  }

  /**
   * Determines whether this table has the given option set.
   *
   * @param option the option to check for
   * @return {@code true} if the option is set on this table
   */
  public boolean hasOption(TableOption option) {
    for (TableOption tableOption : options) {
      if (tableOption == option) {
        return true;
      }
    }

    return false;
  }

  /**
   * Determines whether any column in this table needs a generated SQL check constraint under the
   * given boolean representation mode.
   *
   * @param booleanMode the boolean representation mode in effect for SQL generation
   * @return {@code true} if at least one column needs a check constraint
   */
  public boolean hasColumnConstraints(BooleanMode booleanMode) {
    for (Column column : columns) {
      if (column.needsCheckConstraints(booleanMode)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Collects the columns in this table that need a generated SQL check constraint under the given
   * boolean representation mode.
   *
   * @param booleanMode the boolean representation mode in effect for SQL generation
   * @return the columns needing a check constraint
   */
  public List<Column> getColumnsWithCheckConstraints(BooleanMode booleanMode) {
    List<Column> cols = new ArrayList<Column>();

    for (Column column : columns) {
      if (column.needsCheckConstraints(booleanMode)) {
        cols.add(column);
      }
    }

    return cols;
  }

  /**
   * Finds the relation, if any, declared by the given column of this table.
   *
   * @param column the column to look up a relation for
   * @return the matching relation, or {@code null} if the column has no relation
   */
  public Relation getColumnRelation(Column column) {
    for (Relation relation : relations) {
      if (relation.getFromColumnName().equalsIgnoreCase(column.getName())) {
        return relation;
      }
    }

    return null;
  }

  /** Returns the name of the column used to track export/change dates, or {@code null}. */
  public String getExportDateColumn() {
    return exportDateColumn;
  }

  @Override
  public String toString() {
    return name;
  }
}
