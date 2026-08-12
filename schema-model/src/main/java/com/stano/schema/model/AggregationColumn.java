package com.stano.schema.model;

/**
 * Maps a single source column to a destination column within an {@link Aggregation}, along with the
 * aggregate function (sum/count) applied to it.
 */
public class AggregationColumn {
  private final AggregationType aggregationType;
  private final String sourceColumn;
  private final String destinationColumn;

  /**
   * Creates an aggregation column mapping.
   *
   * @param aggregationType the aggregate function applied to the source column
   * @param sourceColumn the name of the column in the source table
   * @param destinationColumn the name of the column in the destination table
   */
  public AggregationColumn(
      AggregationType aggregationType, String sourceColumn, String destinationColumn) {
    this.aggregationType = aggregationType;
    this.sourceColumn = sourceColumn;
    this.destinationColumn = destinationColumn;
  }

  /** Returns the aggregate function applied to the source column. */
  public AggregationType getAggregationType() {
    return aggregationType;
  }

  /** Returns the name of the column in the source table. */
  public String getSourceColumn() {
    return sourceColumn;
  }

  /** Returns the name of the column in the destination table. */
  public String getDestinationColumn() {
    return destinationColumn;
  }
}
