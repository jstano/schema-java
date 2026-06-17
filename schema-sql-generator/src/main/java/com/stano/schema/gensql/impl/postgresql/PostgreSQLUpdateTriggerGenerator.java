package com.stano.schema.gensql.impl.postgresql;

import com.stano.schema.gensql.impl.common.BaseGenerator;
import com.stano.schema.gensql.impl.common.SQLGenerator;
import com.stano.schema.model.Aggregation;
import com.stano.schema.model.AggregationColumn;
import com.stano.schema.model.AggregationGroup;
import com.stano.schema.model.DatabaseType;
import com.stano.schema.model.ForeignKeyMode;
import com.stano.schema.model.Relation;
import com.stano.schema.model.RelationType;
import com.stano.schema.model.Table;
import com.stano.schema.model.Trigger;
import com.stano.schema.model.TriggerType;
import java.util.List;
import java.util.stream.Collectors;

public class PostgreSQLUpdateTriggerGenerator extends BaseGenerator {

  public PostgreSQLUpdateTriggerGenerator(SQLGenerator sqlGenerator) {

    super(sqlGenerator);
  }

  public void outputUpdateTrigger(Table table, List<Relation> relations) {

    String updateTriggerName = table.getName().toLowerCase() + "_update";
    String updateFunctionName = table.getName().toLowerCase() + "_update";

    generateUpdateFunction(table, relations, updateFunctionName);

    outputUpdateTrigger(table, updateTriggerName, updateFunctionName);
  }

  private String getFullyQualifiedFunctionName(Table table, String functionName) {

    return table.getSchemaName() + "." + functionName;
  }

  private void generateUpdateFunction(
      Table table, List<Relation> relations, String updateFunctionName) {

    sqlWriter.println(
        String.format("/* %s */", getFullyQualifiedFunctionName(table, updateFunctionName)));
    sqlWriter.println(
        "create or replace function "
            + getFullyQualifiedFunctionName(table, updateFunctionName)
            + "() returns trigger as $BODY$");
    sqlWriter.println("begin");

    if (foreignKeyMode == ForeignKeyMode.TRIGGERS) {
      for (Relation relation : relations) {
        if (relation.getType() == RelationType.ENFORCE) {
          sqlWriter.println("   if new." + relation.getFromColumnName() + " is not null then");
          sqlWriter.println(
              "      if (select count(*) from "
                  + getFullyQualifiedTableName(schema.getTable(relation.getToTableName()))
                  + " where "
                  + relation.getToColumnName()
                  + " = new."
                  + relation.getFromColumnName()
                  + ") = 0 then");
          sqlWriter.println(
              "         raise exception 'The value of "
                  + relation.getFromColumnName()
                  + " was not found in the "
                  + getFullyQualifiedTableName(schema.getTable(relation.getToTableName()))
                  + " table.';");
          sqlWriter.println("      end if;");
          sqlWriter.println("   end if;");
          sqlWriter.println();
        } else if (relation.getType() == RelationType.SETNULL) {
          sqlWriter.println("   if new." + relation.getFromColumnName() + " is not null then");
          sqlWriter.println(
              "      if (select count(*) from "
                  + getFullyQualifiedTableName(schema.getTable(relation.getToTableName()))
                  + " where "
                  + relation.getToColumnName()
                  + " = new."
                  + relation.getFromColumnName()
                  + ") = 0 then");
          sqlWriter.println(
              "         raise exception 'The value of "
                  + relation.getFromColumnName()
                  + " was not found in the "
                  + getFullyQualifiedTableName(schema.getTable(relation.getToTableName()))
                  + " table.';");
          sqlWriter.println("      end if;");
          sqlWriter.println("   end if;");
          sqlWriter.println();
        } else if (relation.getType() == RelationType.CASCADE) {
          sqlWriter.println("   if new." + relation.getFromColumnName() + " is not null then");
          sqlWriter.println(
              "      if (select count(*) from "
                  + getFullyQualifiedTableName(schema.getTable(relation.getToTableName()))
                  + " where "
                  + relation.getToColumnName()
                  + " = new."
                  + relation.getFromColumnName()
                  + ") = 0 then");
          sqlWriter.println(
              "         raise exception 'The value of "
                  + relation.getFromColumnName()
                  + " was not found in the "
                  + getFullyQualifiedTableName(schema.getTable(relation.getToTableName()))
                  + " table.';");
          sqlWriter.println("      end if;");
          sqlWriter.println("   end if;");
          sqlWriter.println();
        }
      }
    }

    table
        .getAggregations()
        .forEach(
            aggregation -> {
              sqlWriter.println(getAggregationSql(aggregation));
              sqlWriter.println();
            });

    outputAdditionalUpdateTriggerStatements(table);

    sqlWriter.println("   return new;");
    sqlWriter.println("end;");
    sqlWriter.println("$BODY$ language plpgsql" + statementSeparator);
    sqlWriter.println();
  }

  private void outputUpdateTrigger(
      Table table, String updateTriggerName, String updateFunctionName) {

    sqlWriter.println(
        "drop trigger if exists "
            + updateTriggerName
            + " on "
            + getFullyQualifiedTableName(table)
            + " cascade"
            + statementSeparator);
    sqlWriter.println(
        "create trigger "
            + updateTriggerName
            + " after insert or update on "
            + getFullyQualifiedTableName(table));
    sqlWriter.println(
        "   for each row execute procedure "
            + getFullyQualifiedFunctionName(table, updateFunctionName)
            + "()"
            + statementSeparator);
    sqlWriter.println();
  }

  private void outputAdditionalUpdateTriggerStatements(Table table) {

    if (!table.getTriggers().isEmpty()) {
      for (Trigger trigger : table.getTriggers()) {
        if (trigger.getDatabaseType() == DatabaseType.POSTGRESQL
            && trigger.getTriggerType() == TriggerType.UPDATE) {
          sqlWriter.println(trigger.getTriggerText());
        }
      }
    }
  }

  private String getAggregationSql(Aggregation aggregation) {

    StringBuilder sql = new StringBuilder();

    sql.append("   if tg_op = 'UPDATE' then\n")
        .append(getAggregationUpdateSql(aggregation))
        .append("\n   end if;\n\n")
        .append(getAggregationInsertSql(aggregation));

    //      sql.append("   if tg_op = 'INSERT' then\n")
    //         .append("\n   else\n")
    //         .append(getAggregationUpdateSql(aggregation))
    //         .append("\n   end if;\n");

    return sql.toString();
  }

  private String getAggregationInsertSql(Aggregation aggregation) {

    return String.format(
        "   insert into %s (%s)\n"
            + "      values (%s)\n"
            + "      on conflict (%s) do update set%s\n"
            + "      where %s;",
        aggregation.getDestinationTable(),
        createInsertColumns(aggregation),
        createInsertNewColumns(aggregation),
        createUpsertOnConflictColumns(aggregation),
        createUpdateColumnSettersInsertMode(aggregation),
        createUpsertAggGroup(aggregation, false));
  }

  private String getAggregationUpdateSql(Aggregation aggregation) {

    return String.format(
        "      update %s set%s\n      where %s;",
        aggregation.getDestinationTable(),
        createUpdateColumnSettersUpdateMode(aggregation),
        createUpsertAggGroup(aggregation, true));
  }

  private String createUpsertOnConflictColumns(Aggregation aggregation) {

    return aggregation.getAggregationGroups().stream()
        .map(AggregationGroup::getDestination)
        .collect(Collectors.joining(","));
  }

  private String createUpdateColumnSettersInsertMode(Aggregation aggregation) {

    StringBuilder s = new StringBuilder();

    for (AggregationColumn column : aggregation.getAggregationColumns()) {
      s.append("\n         ")
          .append(
              column.getSourceColumn() != null && !column.getSourceColumn().trim().isEmpty()
                  ? column.getSourceColumn()
                  : "1")
          .append(" = ")
          .append("coalesce(")
          .append(aggregation.getDestinationTable())
          .append(".")
          .append(column.getDestinationColumn())
          .append(",0)")
          .append(" + coalesce(new.")
          .append(column.getDestinationColumn())
          .append(",0),");
    }
    s.append("()");
    return s.substring(0, (s.indexOf("()") - 1));
  }

  private String createUpdateColumnSettersUpdateMode(Aggregation aggregation) {

    StringBuilder s = new StringBuilder();

    for (AggregationColumn column : aggregation.getAggregationColumns()) {
      s.append("\n         ")
          .append(
              column.getSourceColumn() != null && !column.getSourceColumn().trim().isEmpty()
                  ? column.getSourceColumn()
                  : "1")
          .append(" = ")
          .append("coalesce(")
          .append(aggregation.getDestinationTable())
          .append(".")
          .append(column.getDestinationColumn())
          .append(",0)")
          .append(" - coalesce(old.")
          .append(column.getDestinationColumn())
          .append(",0),");
    }
    s.append("()");
    return s.substring(0, (s.indexOf("()") - 1));
  }

  private String createUpsertAggGroup(Aggregation aggregation, boolean useOldValues) {

    StringBuilder s = new StringBuilder();

    for (AggregationGroup group : aggregation.getAggregationGroups()) {
      if (group.getSource().equals(group.getDestination())) {
        s.append(aggregation.getDestinationTable())
            .append(".")
            .append(group.getSource())
            .append(useOldValues ? " = old." : " = new.")
            .append(group.getSource())
            .append(" and ");
      } else {
        s.append(aggregation.getDestinationTable())
            .append(".")
            .append(group.getSource())
            .append(" = new.")
            .append(group.getDestination())
            .append(" and ");
      }
    }
    s.append(" ())");
    return s.substring(0, (s.indexOf("()") - 6));
  }

  private String createInsertColumns(Aggregation aggregation) {

    StringBuilder s = new StringBuilder();

    for (AggregationGroup group : aggregation.getAggregationGroups()) {
      if (group.getSource().equals(group.getDestination())) {
        s.append(group.getSource()).append(", ");
      } else {
        s.append(group.getSource()).append(", ");
      }
    }

    for (AggregationColumn column : aggregation.getAggregationColumns()) {
      s.append(
              column.getSourceColumn() != null && !column.getSourceColumn().trim().isEmpty()
                  ? column.getSourceColumn()
                  : "1")
          .append(", ");
    }
    s.append("()");
    return s.substring(0, (s.indexOf("()") - 2));
  }

  private String createInsertNewColumns(Aggregation aggregation) {

    StringBuilder s = new StringBuilder();

    for (AggregationGroup group : aggregation.getAggregationGroups()) {
      if (group.getSource().equals(group.getDestination())) {
        s.append("new.").append(group.getSource()).append(", ");
      } else {
        s.append("new.").append(group.getSource()).append(", ");
      }
    }

    for (AggregationColumn column : aggregation.getAggregationColumns()) {
      s.append("coalesce(new.").append(column.getDestinationColumn()).append(",0), ");
    }
    s.append("()");
    return s.substring(0, (s.indexOf("()") - 2));
  }
}
