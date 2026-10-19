package com.stano.schema.gensql.impl.postgresql;

import static com.stano.schema.gensql.impl.common.AggregationSql.criteriaPredicate;
import static com.stano.schema.gensql.impl.common.AggregationSql.destinationTableName;
import static com.stano.schema.gensql.impl.common.AggregationSql.indent;
import static com.stano.schema.gensql.impl.common.AggregationSql.rowValue;
import static com.stano.schema.gensql.impl.common.AggregationSql.watchedColumns;

import com.stano.schema.model.Aggregation;
import com.stano.schema.model.AggregationColumn;
import com.stano.schema.model.AggregationGroup;
import com.stano.schema.model.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Maintains an aggregation's destination table from row-level trigger events on its source table.
 *
 * <p>The destination holds one row per (group columns, date bucket of {@code dateColumn}) and must
 * have a unique key over exactly those columns, which the insert's {@code on conflict} targets. A
 * group's {@code source} is a column or SQL expression over the source row; its value lands in the
 * group's {@code destination} column. Weeks start on Monday ({@code date_trunc}).
 */
final class PostgreSQLAggregationSql {

  private PostgreSQLAggregationSql() {}

  private static String dateBucket(Aggregation aggregation) {

    String dateColumn = aggregation.getDateColumn();
    return switch (aggregation.getAggregationFrequency()) {
      case DAILY -> "cast(" + dateColumn + " as date)";
      case WEEKLY -> "cast(date_trunc('week', " + dateColumn + ") as date)";
      case MONTHLY -> "cast(date_trunc('month', " + dateColumn + ") as date)";
      case YEARLY -> "cast(date_trunc('year', " + dateColumn + ") as date)";
    };
  }

  /**
   * A select over the trigger's {@code new}/{@code old} record yielding the destination-shaped
   * contribution of that single row, or no rows when the criteria rejects it.
   */
  private static String contributionSelect(Aggregation aggregation, String record) {

    List<String> items = new ArrayList<>();
    items.add(dateBucket(aggregation) + " as " + aggregation.getDateColumn());
    for (AggregationGroup group : aggregation.getAggregationGroups()) {
      items.add(group.getSource() + " as " + group.getDestination());
    }
    for (AggregationColumn column : aggregation.getAggregationColumns()) {
      items.add(rowValue(column) + " as " + column.getDestinationColumn());
    }

    String sql = "select " + String.join(", ", items) + " from (select " + record + ".*) as src";
    String criteria = criteriaPredicate(aggregation);
    return criteria == null ? sql : sql + " where " + criteria;
  }

  /** Statement adding the contribution of {@code new} to the destination (insert or increment). */
  private static String addNewStatement(Table table, Aggregation aggregation) {

    String destination = destinationTableName(table.getSchemaName(), aggregation);
    String timestamp = aggregation.getTimeStampColumn();
    String date = aggregation.getDateColumn();

    List<String> insertColumns = new ArrayList<>(List.of(timestamp, date));
    List<String> selectItems = new ArrayList<>(List.of("now()", "agg." + date));
    List<String> conflictColumns = new ArrayList<>();
    for (AggregationGroup group : aggregation.getAggregationGroups()) {
      insertColumns.add(group.getDestination());
      selectItems.add("agg." + group.getDestination());
      conflictColumns.add(group.getDestination());
    }
    conflictColumns.add(date);

    List<String> updateItems = new ArrayList<>();
    for (AggregationColumn column : aggregation.getAggregationColumns()) {
      String name = column.getDestinationColumn();
      insertColumns.add(name);
      selectItems.add("agg." + name);
      updateItems.add(name + " = coalesce(dest." + name + ", 0) + excluded." + name);
    }
    updateItems.add(timestamp + " = excluded." + timestamp);

    // "where true" keeps the parser from reading "on conflict" as a join's "on".
    return "   insert into "
        + destination
        + " as dest ("
        + String.join(", ", insertColumns)
        + ")\n   select "
        + String.join(", ", selectItems)
        + " from ("
        + contributionSelect(aggregation, "new")
        + ") as agg where true\n   on conflict ("
        + String.join(", ", conflictColumns)
        + ") do update set\n      "
        + String.join(",\n      ", updateItems)
        + ";";
  }

  /**
   * Statement subtracting the contribution of {@code record} ({@code old}) from the destination.
   */
  private static String subtractStatement(Table table, Aggregation aggregation, String record) {

    String destination = destinationTableName(table.getSchemaName(), aggregation);

    List<String> setItems = new ArrayList<>();
    for (AggregationColumn column : aggregation.getAggregationColumns()) {
      String name = column.getDestinationColumn();
      setItems.add(name + " = coalesce(dest." + name + ", 0) - agg." + name);
    }
    setItems.add(aggregation.getTimeStampColumn() + " = now()");

    List<String> joinItems = new ArrayList<>();
    for (AggregationGroup group : aggregation.getAggregationGroups()) {
      joinItems.add("dest." + group.getDestination() + " = agg." + group.getDestination());
    }
    String date = aggregation.getDateColumn();
    joinItems.add("dest." + date + " = agg." + date);

    return "   update "
        + destination
        + " as dest set\n      "
        + String.join(",\n      ", setItems)
        + "\n   from ("
        + contributionSelect(aggregation, record)
        + ") as agg\n   where "
        + String.join(" and ", joinItems)
        + ";";
  }

  /** Statements for the body of the {@code after insert or update} trigger function. */
  static String upsertAggregationSql(Table table, Aggregation aggregation) {

    List<String> watched = watchedColumns(aggregation);
    if (watched == null) {
      return "   if tg_op = 'UPDATE' then\n"
          + indent(subtractStatement(table, aggregation, "old"), 3)
          + "\n   end if;\n"
          + addNewStatement(table, aggregation);
    }

    // "old" is unassigned on insert, so it may only be read in the elsif branch.
    String changed =
        watched.stream()
            .map(column -> "new." + column + " is distinct from old." + column)
            .collect(Collectors.joining("\n         or "));
    String add = addNewStatement(table, aggregation);
    return "   if tg_op = 'INSERT' then\n"
        + indent(add, 3)
        + "\n   elsif "
        + changed
        + " then\n"
        + indent(subtractStatement(table, aggregation, "old"), 3)
        + "\n"
        + indent(add, 3)
        + "\n   end if;";
  }

  /** Statement for the body of the {@code after delete} trigger function. */
  static String deleteAggregationSql(Table table, Aggregation aggregation) {

    return subtractStatement(table, aggregation, "old");
  }
}
