package com.stano.schema.gensql.impl.postgresql;

import com.stano.schema.gensql.impl.common.BaseGenerator;
import com.stano.schema.gensql.impl.common.SQLGenerator;
import com.stano.schema.model.Aggregation;
import com.stano.schema.model.AggregationColumn;
import com.stano.schema.model.AggregationGroup;
import com.stano.schema.model.Column;
import com.stano.schema.model.DatabaseType;
import com.stano.schema.model.ForeignKeyMode;
import com.stano.schema.model.Relation;
import com.stano.schema.model.RelationType;
import com.stano.schema.model.Table;
import com.stano.schema.model.Trigger;
import com.stano.schema.model.TriggerType;
import java.util.List;

public class PostgreSQLDeleteTriggerGenerator extends BaseGenerator {

  public PostgreSQLDeleteTriggerGenerator(SQLGenerator sqlGenerator) {

    super(sqlGenerator);
  }

  public void outputDeleteTrigger(Table table, List<Relation> relations) {

    String deleteFunctionName = table.getName().toLowerCase() + "_delete";
    String deleteTriggerName = table.getName().toLowerCase() + "_delete";

    List<String> pkColumns = table.getPrimaryKeyColumns();
    Column column = table.getColumn(pkColumns.get(0));

    if (column == null) {
      return;
    }

    outputDeleteFunction(table, relations, deleteFunctionName, column);

    outputDeleteTrigger(table, deleteTriggerName, deleteFunctionName);
  }

  private String getFullyQualifiedFunctionName(Table table, String functionName) {

    return table.getSchemaName() + "." + functionName;
  }

  private void outputDeleteFunction(
      Table table, List<Relation> relations, String delFunctionName, Column column) {

    sqlWriter.println(
        String.format("/* %s */", getFullyQualifiedFunctionName(table, delFunctionName)));
    sqlWriter.println(
        "create or replace function "
            + getFullyQualifiedFunctionName(table, delFunctionName)
            + "() returns trigger as $BODY$");
    sqlWriter.println("begin");

    if (foreignKeyMode == ForeignKeyMode.TRIGGERS) {
      outputDeleteEnforceStatements(relations, column);

      outputDeleteSetNullStatements(relations);

      outputDeleteCascadeStatements(relations);
    }

    outputDeleteAggregationStatements(table);

    outputDeleteAdditionalTriggerStatements(table);

    sqlWriter.println("   return null;");
    sqlWriter.println("end;");
    sqlWriter.println("$BODY$ language plpgsql" + statementSeparator);
    sqlWriter.println();
  }

  private void outputDeleteTrigger(
      Table table, String deleteTriggerName, String deleteFunctionName) {

    sqlWriter.println(
        "drop trigger if exists "
            + deleteTriggerName
            + " on "
            + getFullyQualifiedTableName(table)
            + " cascade"
            + statementSeparator);
    sqlWriter.println(
        "create trigger "
            + deleteTriggerName
            + " after delete on "
            + getFullyQualifiedTableName(table));
    sqlWriter.println(
        "   for each row execute procedure "
            + getFullyQualifiedFunctionName(table, deleteFunctionName)
            + "()"
            + statementSeparator);
    sqlWriter.println();
  }

  private void outputDeleteEnforceStatements(List<Relation> relations, Column column) {

    relations.stream()
        .filter(relation -> relation.getType() == RelationType.ENFORCE)
        .forEach(
            relation -> {
              sqlWriter.println(
                  "   if (select count(*) from "
                      + getFullyQualifiedTableName(schema.getTable(relation.getToTableName()))
                      + " where "
                      + relation.getToColumnName()
                      + " = OLD."
                      + column.getName()
                      + ") > 0 then");
              sqlWriter.println(
                  "      raise exception 'The row in "
                      + getFullyQualifiedTableName(schema.getTable(relation.getFromTableName()))
                      + " cannot be deleted. It is being used by a row in the "
                      + getFullyQualifiedTableName(schema.getTable(relation.getToTableName()))
                      + " table.';");
              sqlWriter.println("   end if;");
              sqlWriter.println();
            });
  }

  private void outputDeleteSetNullStatements(List<Relation> relations) {

    relations.stream()
        .filter(relation -> relation.getType() == RelationType.SETNULL)
        .forEach(
            relation -> {
              sqlWriter.println(
                  "   update "
                      + getFullyQualifiedTableName(schema.getTable(relation.getToTableName()))
                      + " set "
                      + relation.getToColumnName()
                      + " = null where "
                      + relation.getToColumnName()
                      + " = OLD."
                      + relation.getFromColumnName()
                      + ";");
              sqlWriter.println();
            });
  }

  private void outputDeleteCascadeStatements(List<Relation> relations) {

    relations.stream()
        .filter(relation -> relation.getType() == RelationType.CASCADE)
        .forEach(
            relation -> {
              sqlWriter.println(
                  "   delete from "
                      + getFullyQualifiedTableName(schema.getTable(relation.getToTableName()))
                      + " where "
                      + relation.getToColumnName()
                      + " = OLD."
                      + relation.getFromColumnName()
                      + ";");
              sqlWriter.println();
            });
  }

  private void outputDeleteAggregationStatements(Table table) {

    table
        .getAggregations()
        .forEach(
            aggregation -> {
              sqlWriter.println(getAggregationDeletionText(aggregation, table));
            });
  }

  private void outputDeleteAdditionalTriggerStatements(Table table) {

    if (!table.getTriggers().isEmpty()) {
      for (Trigger trigger : table.getTriggers()) {
        if (trigger.getDatabaseType() == DatabaseType.POSTGRESQL
            && trigger.getTriggerType() == TriggerType.DELETE) {
          sqlWriter.println(trigger.getTriggerText());
        }
      }
    }
  }

  private String getAggregationDeletionText(Aggregation aggregation, Table table) {

    return String.format(
        "   update %s set %s\n   where %s;",
        aggregation.getDestinationTable(),
        createDeleteUpdateColumns(aggregation),
        createDeleteAggGroup(aggregation));
  }

  private String createDeleteAggGroup(Aggregation aggregation) {

    StringBuilder s = new StringBuilder();

    for (AggregationGroup group : aggregation.getAggregationGroups()) {
      if (group.getSource().equals(group.getDestination())) {
        s.append(aggregation.getDestinationTable())
            .append(".")
            .append(group.getSource())
            .append(" = old.")
            .append(group.getSource())
            .append(" and ");
      } else {
        s.append(aggregation.getDestinationTable())
            .append(".")
            .append(group.getSource())
            .append(" = old.")
            .append(group.getDestination())
            .append(" and ");
      }
    }

    s.append(" ())");
    return s.substring(0, (s.indexOf("()") - 6)).toString();
  }

  private String createDeleteUpdateColumns(Aggregation aggregation) {

    StringBuilder s = new StringBuilder();

    for (AggregationColumn column : aggregation.getAggregationColumns()) {
      s.append("\n      ")
          .append(
              column.getSourceColumn() != null && !column.getSourceColumn().trim().isEmpty()
                  ? column.getSourceColumn()
                  : "1")
          .append(" = ")
          .append(aggregation.getDestinationTable())
          .append(".")
          .append(column.getDestinationColumn())
          .append(" - coalesce(old.")
          .append(column.getDestinationColumn())
          .append(",0),");
    }
    s.append("()");
    return s.substring(0, (s.indexOf("()") - 1)).toString();
  }
}
