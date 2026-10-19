package com.stano.schema.gensql.impl.sqlserver;

import static com.stano.schema.gensql.impl.common.AggregationSql.criteriaPredicate;
import static com.stano.schema.gensql.impl.common.AggregationSql.destinationTableName;
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
 * Maintains an aggregation's destination table from statement-level trigger events on its source
 * table.
 *
 * <p>The destination holds one row per (group columns, date bucket of {@code dateColumn}). Each
 * trigger turns the {@code inserted}/{@code deleted} pseudo-tables into per-bucket deltas and
 * applies them with a {@code merge} (insert/update triggers) or an {@code update} (delete trigger).
 * A group's {@code source} is a column or SQL expression over the source row; its value lands in
 * the group's {@code destination} column. Week buckets follow the session's {@code DATEFIRST}.
 */
final class SQLServerAggregationSql {

  private SQLServerAggregationSql() {}

  /** The schema-qualified destination, mapping the {@code public} schema to {@code dbo}. */
  static String destinationTable(Table table, Aggregation aggregation) {

    String schemaName =
        table.getSchemaName().equalsIgnoreCase("public") ? "dbo" : table.getSchemaName();
    return destinationTableName(schemaName, aggregation);
  }

  /** The date bucket of the aggregation's date column, as a {@code date}. */
  static String dateBucket(Aggregation aggregation) {

    String d = aggregation.getDateColumn();
    return switch (aggregation.getAggregationFrequency()) {
      case DAILY -> "cast(" + d + " as date)";
      case WEEKLY -> "cast(dateadd(d, -datepart(dw, " + d + ") + 1, " + d + ") as date)";
      case MONTHLY -> "cast(dateadd(d, -datepart(d, " + d + ") + 1, " + d + ") as date)";
      case YEARLY -> "cast(dateadd(d, -datepart(dy, " + d + ") + 1, " + d + ") as date)";
    };
  }

  /**
   * A CTE {@code alias} projecting each row of {@code pseudoTable} ({@code inserted}/{@code
   * deleted}) into the destination's shape: one value per aggregation column, the group values, and
   * the bucket.
   */
  private static String sourceCte(Aggregation aggregation, String pseudoTable, String alias) {

    List<String> items = new ArrayList<>();
    for (AggregationColumn column : aggregation.getAggregationColumns()) {
      items.add(rowValue(column) + " as " + column.getDestinationColumn());
    }
    for (AggregationGroup group : aggregation.getAggregationGroups()) {
      items.add(group.getSource() + " as " + group.getDestination());
    }
    items.add(dateBucket(aggregation) + " as " + aggregation.getDateColumn());

    String sql = alias + " as (select " + String.join(", ", items) + " from " + pseudoTable;
    String criteria = criteriaPredicate(aggregation);
    return (criteria == null ? sql : sql + " where " + criteria) + ")";
  }

  private static List<String> groupAndDateColumns(Aggregation aggregation) {

    List<String> columns = new ArrayList<>();
    for (AggregationGroup group : aggregation.getAggregationGroups()) {
      columns.add(group.getDestination());
    }
    columns.add(aggregation.getDateColumn());
    return columns;
  }

  private static String joinOn(Aggregation aggregation, String left, String right) {

    return groupAndDateColumns(aggregation).stream()
        .map(c -> left + "." + c + " = " + right + "." + c)
        .collect(Collectors.joining(" and "));
  }

  /** Statements for the body of the {@code for insert, update} trigger. */
  static String upsertAggregationSql(Table table, Aggregation aggregation) {

    String timestamp = aggregation.getTimeStampColumn();
    List<String> keys = groupAndDateColumns(aggregation);
    List<String> columns =
        aggregation.getAggregationColumns().stream()
            .map(AggregationColumn::getDestinationColumn)
            .toList();

    // Inserted rows count positively, deleted rows negatively.
    String deltaCte =
        "delta as ("
            + deltaBranch(columns, keys, "", "I")
            + " union all "
            + deltaBranch(columns, keys, "-", "D")
            + ")";

    List<String> aggregatedItems = new ArrayList<>();
    for (String c : columns) {
      aggregatedItems.add("sum(coalesce(" + c + ", 0)) as " + c);
    }
    aggregatedItems.addAll(keys);
    String aggregatedCte =
        "aggregated as (select "
            + String.join(", ", aggregatedItems)
            + " from delta group by "
            + String.join(", ", keys)
            + ")";

    List<String> matchedItems = new ArrayList<>();
    for (String c : columns) {
      matchedItems.add("target." + c + " = target." + c + " + source." + c);
    }
    matchedItems.add("target." + timestamp + " = getdate()");

    List<String> insertColumns = new ArrayList<>(List.of(timestamp));
    List<String> insertValues = new ArrayList<>(List.of("getdate()"));
    for (String key : keys) {
      insertColumns.add(key);
      insertValues.add("source." + key);
    }
    for (String c : columns) {
      insertColumns.add(c);
      insertValues.add("source." + c);
    }

    StringBuilder sql = new StringBuilder("   ;\n");
    List<String> watched = watchedColumns(aggregation);
    if (watched != null) {
      sql.append("   if ")
          .append(
              watched.stream().map(c -> "update(" + c + ")").collect(Collectors.joining(" or ")))
          .append("\n");
    }
    sql.append("   with ")
        .append(sourceCte(aggregation, "deleted", "D"))
        .append(",\n      ")
        .append(sourceCte(aggregation, "inserted", "I"))
        .append(",\n      ")
        .append(deltaCte)
        .append(",\n      ")
        .append(aggregatedCte)
        .append("\n   merge ")
        .append(destinationTable(table, aggregation))
        .append(" as target using aggregated as source on (")
        .append(joinOn(aggregation, "target", "source"))
        .append(")\n   when matched then update set\n      ")
        .append(String.join(",\n      ", matchedItems))
        .append("\n   when not matched then insert (")
        .append(String.join(", ", insertColumns))
        .append(")\n      values (")
        .append(String.join(", ", insertValues))
        .append(");");
    return sql.toString();
  }

  private static String deltaBranch(
      List<String> columns, List<String> keys, String sign, String alias) {

    List<String> items = new ArrayList<>();
    for (String c : columns) {
      items.add(sign + c + " as " + c);
    }
    items.addAll(keys);
    return "select " + String.join(", ", items) + " from " + alias;
  }

  /** Statements for the body of the {@code for delete} trigger. */
  static String deleteAggregationSql(Table table, Aggregation aggregation) {

    List<String> keys = groupAndDateColumns(aggregation);

    List<String> aggregatedItems = new ArrayList<>();
    List<String> setItems = new ArrayList<>();
    for (AggregationColumn column : aggregation.getAggregationColumns()) {
      String name = column.getDestinationColumn();
      aggregatedItems.add("sum(" + name + ") as " + name);
      setItems.add("dest." + name + " = dest." + name + " - aggregated." + name);
    }
    aggregatedItems.addAll(keys);
    setItems.add("dest." + aggregation.getTimeStampColumn() + " = getdate()");

    return "   ;\n   with "
        + sourceCte(aggregation, "deleted", "D")
        + ",\n      aggregated as (select "
        + String.join(", ", aggregatedItems)
        + " from D group by "
        + String.join(", ", keys)
        + ")\n   update dest set\n      "
        + String.join(",\n      ", setItems)
        + "\n   from "
        + destinationTable(table, aggregation)
        + " as dest join aggregated on "
        + joinOn(aggregation, "dest", "aggregated")
        + ";";
  }
}
