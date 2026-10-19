package com.stano.schema.gensql.impl.postgresql;

import com.stano.schema.gensql.impl.common.BaseGenerator;
import com.stano.schema.gensql.impl.common.SQLGenerator;
import com.stano.schema.model.ColumnPair;
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
        if (relation.getType() == RelationType.ENFORCE
            || relation.getType() == RelationType.SETNULL
            || relation.getType() == RelationType.CASCADE) {
          String notNullClause =
              relation.getColumnPairs().stream()
                  .map(pair -> "new." + pair.getFromColumnName() + " is not null")
                  .collect(Collectors.joining(" and "));
          String newMatchClause =
              relation.getColumnPairs().stream()
                  .map(pair -> pair.getToColumnName() + " = new." + pair.getFromColumnName())
                  .collect(Collectors.joining(" and "));
          String columnList =
              relation.getColumnPairs().stream()
                  .map(ColumnPair::getFromColumnName)
                  .collect(Collectors.joining(", "));
          sqlWriter.println("   if " + notNullClause + " then");
          sqlWriter.println(
              "      if (select count(*) from "
                  + getFullyQualifiedTableName(schema.getTable(relation.getToTableName()))
                  + " where "
                  + newMatchClause
                  + ") = 0 then");
          sqlWriter.println(
              "         raise exception 'The value of "
                  + columnList
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
              sqlWriter.println(PostgreSQLAggregationSql.upsertAggregationSql(table, aggregation));
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
}
