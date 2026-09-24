package com.stano.schema.gensql.impl.sqlserver;

import com.stano.schema.gensql.impl.common.SQLGenerator;
import com.stano.schema.model.Aggregation;
import com.stano.schema.model.AggregationColumn;
import com.stano.schema.model.AggregationGroup;
import com.stano.schema.model.Column;
import com.stano.schema.model.Relation;
import com.stano.schema.model.RelationType;
import com.stano.schema.model.Table;
import com.stano.schema.model.TriggerType;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

class SQLServerDeleteTriggerGenerator extends SQLServerBaseTriggerGenerator {

  SQLServerDeleteTriggerGenerator(SQLGenerator sqlGenerator) {

    super(sqlGenerator);
  }

  void outputDeleteTrigger(Table table, List relations) {

    List<String> pkColumns = table.getPrimaryKeyColumns();
    Column column = table.getColumn(pkColumns.get(0));

    if (column == null) {
      return;
    }

    String triggerName = table.getName().toLowerCase() + "_delete";
    String schemaName =
        table.getSchemaName().equalsIgnoreCase("public") ? "dbo" : table.getSchemaName();
    String fullyQualifiedTrigger = schemaName + "." + triggerName;

    sqlWriter.println(String.format("/* %s */", triggerName));
    sqlWriter.println(
        "if object_id('" + escapeSqlLiteral(fullyQualifiedTrigger) + "', 'TR') is not null");
    sqlWriter.println("   drop trigger " + fullyQualifiedTrigger + statementSeparator);
    sqlWriter.println();

    sqlWriter.println(
        "create trigger "
            + triggerName
            + " on "
            + getFullyQualifiedTableName(table)
            + " for delete as");
    sqlWriter.println("if (select count(*) from deleted) > 0");
    sqlWriter.println("BEGIN");

    outputDeleteEnforceStatements(relations, column);

    outputDeleteSetNullStatements(relations, column);

    outputDeleteCascadeStatements(relations, column);

    outputDeleteAggregationStatements(table);

    outputAdditionalTriggerStatements(table, TriggerType.DELETE);

    sqlWriter.println("END" + statementSeparator);

    outputPrimingDeleteTriggerStatements(table);

    sqlWriter.println();
  }

  /**
   * Joins every column pair of a (reverse) relation into a {@code leftAlias.col = rightAlias.col}
   * join predicate, used for the composite-relation join-based trigger SQL that replaces the
   * single-column {@code in (select col from deleted)} idiom, which has no direct multi-column
   * equivalent.
   */
  private String joinCondition(Relation relation, String leftAlias, String rightAlias) {

    return relation.getColumnPairs().stream()
        .map(
            pair ->
                leftAlias
                    + "."
                    + pair.getToColumnName()
                    + " = "
                    + rightAlias
                    + "."
                    + pair.getFromColumnName())
        .collect(Collectors.joining(" and "));
  }

  private void outputDeleteEnforceStatements(List relations, Column column) {

    Iterator relationIterator = relations.iterator();

    boolean firstRelation = true;
    while (relationIterator.hasNext()) {
      Relation relation = (Relation) relationIterator.next();

      if (relation.getType() == RelationType.ENFORCE) {
        if (firstRelation) {
          sqlWriter.println("   declare @msg varchar(2000)");
        }

        firstRelation = false;

        String childTable = getFullyQualifiedTableName(schema.getTable(relation.getToTableName()));

        if (relation.isComposite()) {
          sqlWriter.println(
              "   if exists (select 1 from "
                  + childTable
                  + " c inner join deleted d on "
                  + joinCondition(relation, "c", "d")
                  + ")");
          sqlWriter.println("   begin");
          sqlWriter.println(
              "      select @msg = 'The row in "
                  + getFullyQualifiedTableName(schema.getTable(relation.getFromTableName()))
                  + " cannot be deleted. It is being used by a row in the "
                  + childTable
                  + " table.'");
          sqlWriter.println("      rollback transaction");
          sqlWriter.println("      raiserror (@msg, 16, 1)");
          sqlWriter.println("      return");
          sqlWriter.println("   end;");
          sqlWriter.println();
        } else {
          sqlWriter.println(
              "   if (select count(*) from "
                  + childTable
                  + " where "
                  + relation.getToColumnName()
                  + " in (select "
                  + column.getName()
                  + " from deleted)) > 0");
          sqlWriter.println("   begin");
          sqlWriter.println(
              "      select @msg = 'The "
                  + getFullyQualifiedTableName(schema.getTable(relation.getFromTableName()))
                  + " ' + (select top 1 convert(varchar, "
                  + column.getName()
                  + ") from deleted where "
                  + column.getName()
                  + " in (select "
                  + relation.getToColumnName()
                  + " from "
                  + childTable
                  + ")) + ' cannot be deleted. It is being used by a row in the "
                  + childTable
                  + " table.'");
          sqlWriter.println("      rollback transaction");
          sqlWriter.println("      raiserror (@msg, 16, 1)");
          sqlWriter.println("      return");
          sqlWriter.println("   end;");
          sqlWriter.println();
        }
      }
    }
  }

  private void outputDeleteSetNullStatements(List relations, Column column) {

    Iterator relationIterator;
    relationIterator = relations.iterator();

    while (relationIterator.hasNext()) {
      Relation relation = (Relation) relationIterator.next();

      if (relation.getType() == RelationType.SETNULL) {
        String childTable = getFullyQualifiedTableName(schema.getTable(relation.getToTableName()));

        if (relation.isComposite()) {
          String setClause =
              relation.getColumnPairs().stream()
                  .map(pair -> pair.getToColumnName() + " = null")
                  .collect(Collectors.joining(", "));
          sqlWriter.println(
              "   update "
                  + childTable
                  + " set "
                  + setClause
                  + " from "
                  + childTable
                  + " inner join deleted on "
                  + joinCondition(relation, childTable, "deleted")
                  + ";");
        } else {
          sqlWriter.println(
              "   update "
                  + childTable
                  + " set "
                  + relation.getToColumnName()
                  + " = null where "
                  + relation.getToColumnName()
                  + " in (select "
                  + column.getName()
                  + " from deleted);");
        }
      }
    }
  }

  private void outputDeleteCascadeStatements(List relations, Column column) {

    Iterator relationIterator;
    relationIterator = relations.iterator();

    while (relationIterator.hasNext()) {
      Relation relation = (Relation) relationIterator.next();

      if (relation.getType() == RelationType.CASCADE) {
        String childTable = getFullyQualifiedTableName(schema.getTable(relation.getToTableName()));

        if (relation.isComposite()) {
          sqlWriter.println(
              "   delete "
                  + childTable
                  + " from "
                  + childTable
                  + " inner join deleted on "
                  + joinCondition(relation, childTable, "deleted")
                  + ";");
        } else {
          sqlWriter.println(
              "   delete from "
                  + childTable
                  + " where "
                  + relation.getToColumnName()
                  + " in (select "
                  + column.getName()
                  + " from deleted);");
        }
      }
    }
  }

  private void outputDeleteAggregationStatements(Table table) {

    if (!table.getAggregations().isEmpty()) {
      sqlWriter.println(";");

      for (Aggregation aggregation : table.getAggregations()) {
        sqlWriter.println(getAggregationDeletionText(aggregation));
      }
    }
  }

  private void outputPrimingDeleteTriggerStatements(Table table) {

    if (!table.getAggregations().isEmpty()) {
      Iterator aggregationIterator = table.getAggregations().iterator();

      String sp_name = "usp_primeAggregates_" + table.getName();
      sqlWriter.println();
      sqlWriter.println(
          "if exists (select * from sys.objects where object_id = Object_ID('"
              + sp_name
              + "') and type in ('P','PC'))\n  drop procedure "
              + sp_name
              + statementSeparator);
      sqlWriter.println();
      sqlWriter.println("create proc " + sp_name + " as");
      sqlWriter.println("begin");

      while (aggregationIterator.hasNext()) {
        Aggregation aggregation = (Aggregation) aggregationIterator.next();
        sqlWriter.println(getAggregationPrimingSP(aggregation, table.getName()));
      }

      sqlWriter.println("end" + statementSeparator);
    }
  }

  private String getAggregationDeletionText(Aggregation aggregation) {

    return "  with "
        + createSourceCTE(aggregation, "deleted", "D")
        + ", "
        + createDeleteAggregatedCTE(aggregation)
        + createUpdateCTE(aggregation)
        + ";";
  }

  private String createDeleteAggregatedCTE(Aggregation aggregation) {

    StringBuilder s = new StringBuilder("aggregated as (select ");
    StringBuilder g = new StringBuilder();

    for (AggregationColumn column : aggregation.getAggregationColumns()) {
      switch (column.getAggregationType()) {
        case SUM:
          s.append("SUM(");
          break;
        case COUNT:
          s.append("COUNT(*");
          break;
      }
      if (column.getSourceColumn() != null && !column.getSourceColumn().trim().isEmpty()) {
        s.append(column.getDestinationColumn());
      }
      s.append(") as ").append(column.getDestinationColumn()).append(", ");
    }

    for (AggregationGroup group : aggregation.getAggregationGroups()) {
      s.append(group.getDestination()).append(", ");
    }

    s.append(aggregation.getDateColumn()).append(" from D group by ");

    for (AggregationGroup group : aggregation.getAggregationGroups()) {
      s.append(group.getDestination()).append(", ");
    }

    return s.append(aggregation.getDateColumn()).append(") ").toString();
  }

  private String createUpdateCTE(Aggregation aggregation) {

    StringBuilder s = new StringBuilder("update dest set ");

    for (AggregationColumn column : aggregation.getAggregationColumns()) {
      s.append("dest.")
          .append(column.getDestinationColumn())
          .append(" = dest.")
          .append(column.getDestinationColumn())
          .append(" - aggregated.")
          .append(column.getDestinationColumn())
          .append(", ");
    }

    s.setLength(s.length() - 2);
    s.append(" from ")
        .append(aggregation.getDestinationTable())
        .append(" as dest join aggregated on ");

    for (AggregationGroup group : aggregation.getAggregationGroups()) {
      s.append("dest.")
          .append(group.getDestination())
          .append(" = aggregated.")
          .append(group.getDestination())
          .append(" and ");
    }

    return s.append("dest.")
        .append(aggregation.getDateColumn())
        .append(" = aggregated.")
        .append(aggregation.getDateColumn())
        .toString();
  }

  private String getAggregationPrimingSP(Aggregation aggregation, String forTableName) {

    StringBuilder s = new StringBuilder("  delete from ");

    s.append(aggregation.getDestinationTable())
        .append(";\n")
        .append("  with source as (select getDate() as ")
        .append(aggregation.getTimeStampColumn())
        .append(", ");

    s.append(createAggregationFrequencyConversion(aggregation))
        .append(" as ")
        .append(aggregation.getDateColumn())
        .append(", ");

    for (AggregationGroup group : aggregation.getAggregationGroups()) {
      s.append(group.getSource()).append(" as ").append(group.getDestination()).append(", ");
    }

    for (AggregationColumn column : aggregation.getAggregationColumns()) {
      if (column.getSourceColumn() != null && !column.getSourceColumn().trim().isEmpty()) {
        s.append(column.getSourceColumn()).append(", ");
      }
    }

    s.setLength(s.length() - 2);
    s.append(" from ").append(forTableName);

    if (aggregation.getCriteria() != null && !aggregation.getCriteria().trim().isEmpty()) {
      s.append(" where ").append(aggregation.getCriteria());
    }

    s.append(")\n  insert into ")
        .append(aggregation.getDestinationTable())
        .append(" (")
        .append(aggregation.getTimeStampColumn())
        .append(", ")
        .append(aggregation.getDateColumn())
        .append(", ");

    for (AggregationGroup group : aggregation.getAggregationGroups()) {
      s.append(group.getDestination()).append(", ");
    }

    for (AggregationColumn column : aggregation.getAggregationColumns()) {
      s.append(column.getDestinationColumn()).append(", ");
    }

    s.setLength(s.length() - 2);
    s.append(") select getDate(), ").append(aggregation.getDateColumn()).append(", ");

    for (AggregationGroup group : aggregation.getAggregationGroups()) {
      s.append(group.getDestination()).append(", ");
    }

    for (AggregationColumn column : aggregation.getAggregationColumns()) {
      switch (column.getAggregationType()) {
        case SUM:
          s.append("SUM(");
          break;
        case COUNT:
          s.append("COUNT(*");
          break;
      }
      if (column.getSourceColumn() != null && !column.getSourceColumn().trim().isEmpty()) {
        s.append("COALESCE(").append(column.getSourceColumn()).append(", 0)), ");
      } else {
        s.append("), ");
      }
    }

    s.setLength(s.length() - 2);
    s.append(" from source group by ");

    for (AggregationGroup group : aggregation.getAggregationGroups()) {
      s.append(group.getDestination()).append(", ");
    }

    return s.append(aggregation.getDateColumn()).toString();
  }
}
