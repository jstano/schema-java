package com.stano.schema.model;

/**
 * A grouping column used by an {@link Aggregation} to bucket source rows when rolling them up into
 * the destination table.
 */
public class AggregationGroup {
  private final String source;
  private final String sourceDerivedFrom;
  private final String destination;

  /**
   * Creates an aggregation grouping column mapping.
   *
   * @param source the name of the grouping column in the source table
   * @param sourceDerivedFrom the name of the source column this grouping value is derived from, if
   *     not read directly from {@code source}
   * @param destination the name of the grouping column in the destination table
   */
  public AggregationGroup(String source, String sourceDerivedFrom, String destination) {
    this.source = source;
    this.sourceDerivedFrom = sourceDerivedFrom;
    this.destination = destination;
  }

  /** Returns the name of the grouping column in the source table. */
  public String getSource() {
    return source;
  }

  /** Returns the name of the grouping column in the destination table. */
  public String getDestination() {
    return destination;
  }

  /** Returns the name of the source column this grouping value is derived from, if any. */
  public String getSourceDerivedFrom() {
    return sourceDerivedFrom;
  }
}
