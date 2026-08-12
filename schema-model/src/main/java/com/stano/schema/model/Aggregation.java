package com.stano.schema.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Defines a rollup of a source table's data into an aggregated destination table, grouped by date
 * (and optionally by other columns) at a configured {@link AggregationFrequency}.
 */
public class Aggregation {
  private final String destinationTable;
  private final String dateColumn;
  private final String criteria;
  private final String timeStampColumn;
  private final AggregationFrequency aggregationFrequency;
  private final List<AggregationColumn> aggregationColumns;
  private final List<AggregationGroup> aggregationGroups;

  /**
   * Creates an aggregation definition.
   *
   * @param destinationTable the name of the table the aggregated rows are written to
   * @param dateColumn the source column used to derive the aggregation period
   * @param criteria an optional SQL filter applied to source rows before aggregation
   * @param timeStampColumn the column used to record when a row was last aggregated
   * @param aggregationFrequency how often source data is rolled up (daily, weekly, etc.)
   * @param aggregationColumns the columns to aggregate (sum/count) into the destination table
   * @param aggregationGroups the columns used to group source rows in the destination table
   */
  public Aggregation(
      String destinationTable,
      String dateColumn,
      String criteria,
      String timeStampColumn,
      AggregationFrequency aggregationFrequency,
      List<AggregationColumn> aggregationColumns,
      List<AggregationGroup> aggregationGroups) {
    this.destinationTable = destinationTable;
    this.dateColumn = dateColumn;
    this.criteria = criteria;
    this.timeStampColumn = timeStampColumn;
    this.aggregationFrequency = aggregationFrequency;
    this.aggregationColumns = Collections.unmodifiableList(new ArrayList<>(aggregationColumns));
    this.aggregationGroups = Collections.unmodifiableList(new ArrayList<>(aggregationGroups));
  }

  /** Returns the name of the table the aggregated rows are written to. */
  public String getDestinationTable() {
    return destinationTable;
  }

  /** Returns the source column used to derive the aggregation period. */
  public String getDateColumn() {
    return dateColumn;
  }

  /** Returns the optional SQL filter applied to source rows before aggregation. */
  public String getCriteria() {
    return criteria;
  }

  /** Returns the column used to record when a row was last aggregated. */
  public String getTimeStampColumn() {
    return timeStampColumn;
  }

  /** Returns how often source data is rolled up. */
  public AggregationFrequency getAggregationFrequency() {
    return aggregationFrequency;
  }

  /** Returns the columns used to group source rows in the destination table. */
  public List<AggregationGroup> getAggregationGroups() {
    return aggregationGroups;
  }

  /** Returns the columns aggregated (sum/count) into the destination table. */
  public List<AggregationColumn> getAggregationColumns() {
    return aggregationColumns;
  }
}
