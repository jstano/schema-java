package com.stano.schema.gensql.impl.common;

import com.stano.schema.model.Aggregation;
import com.stano.schema.model.AggregationColumn;
import com.stano.schema.model.AggregationGroup;
import com.stano.schema.model.AggregationType;
import java.util.ArrayList;
import java.util.List;

/** Dialect-neutral pieces shared by the PostgreSQL and SQL Server aggregation trigger SQL. */
public final class AggregationSql {

  private AggregationSql() {}

  /**
   * Schema-qualifies an aggregation's destination table with {@code schemaName} when the XML names
   * it bare; a name that already contains a {@code .} is used as written.
   */
  public static String destinationTableName(String schemaName, Aggregation aggregation) {

    String destination = aggregation.getDestinationTable();
    return destination.contains(".") ? destination : schemaName + "." + destination;
  }

  /**
   * The value a source row contributes to an aggregation column: a count contributes {@code 1} per
   * row, a sum contributes the (null-as-zero) source column.
   */
  public static String rowValue(AggregationColumn column) {

    return column.getAggregationType() == AggregationType.COUNT
        ? "1"
        : "coalesce(" + column.getSourceColumn() + ", 0)";
  }

  /** The criteria as a parenthesised predicate, or {@code null} when absent or blank. */
  public static String criteriaPredicate(Aggregation aggregation) {

    String criteria = aggregation.getCriteria();
    return criteria == null || criteria.isBlank() ? null : "(" + criteria.trim() + ")";
  }

  /**
   * Source columns whose change can alter an aggregation's contribution, or {@code null} when that
   * can't be known (the free-form criteria may read any column), in which case an update must
   * always be re-aggregated. A group's {@code sourceDerivedFrom} names the column its (possibly
   * expression-valued) {@code source} is derived from.
   */
  public static List<String> watchedColumns(Aggregation aggregation) {

    if (criteriaPredicate(aggregation) != null) {
      return null;
    }

    List<String> columns = new ArrayList<>();
    for (AggregationColumn column : aggregation.getAggregationColumns()) {
      if (column.getAggregationType() == AggregationType.SUM) {
        columns.add(column.getSourceColumn());
      }
    }
    for (AggregationGroup group : aggregation.getAggregationGroups()) {
      String derived = group.getSourceDerivedFrom();
      columns.add(derived != null && !derived.isBlank() ? derived.trim() : group.getSource());
    }
    columns.add(aggregation.getDateColumn());
    return columns;
  }

  /** Indents every line of {@code sql} by {@code spaces}. */
  public static String indent(String sql, int spaces) {

    String pad = " ".repeat(spaces);
    return sql.lines().map(line -> pad + line).collect(java.util.stream.Collectors.joining("\n"));
  }
}
