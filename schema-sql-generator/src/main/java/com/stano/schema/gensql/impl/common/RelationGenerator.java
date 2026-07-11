package com.stano.schema.gensql.impl.common;

import com.stano.schema.model.Relation;
import com.stano.schema.model.RelationType;
import com.stano.schema.model.Table;

public class RelationGenerator extends BaseGenerator {

  private static final String FK_PREFIX = "fk_";

  protected RelationGenerator(SQLGenerator sqlGenerator) {

    super(sqlGenerator);
  }

  public void outputRelations() {

    if (schema.getTables().stream().anyMatch(table -> !table.getRelations().isEmpty())) {
      sqlWriter.println("/* relations */");
    }

    schema.getTables().stream()
        .filter(table -> !table.getRelations().isEmpty())
        .forEach(this::outputRelationsForTable);

    sqlWriter.println();
  }

  private void outputRelationsForTable(Table table) {

    for (int relationIndex = 0; relationIndex < table.getRelations().size(); relationIndex++) {
      Relation relation = table.getRelations().get(relationIndex);

      String relationName =
          buildKeyName(FK_PREFIX, table.getName(), String.valueOf(relationIndex + 1));

      outputRelation(table, relationName.toLowerCase(), relation);
    }
  }

  private void outputRelation(Table table, String relationName, Relation relation) {

    String operation = getRelationTypeOperation(relation.getType());

    sqlWriter.print("alter table " + getFullyQualifiedTableName(table));
    sqlWriter.print(" add constraint ");
    sqlWriter.print(relationName);
    sqlWriter.print(" foreign key (");
    sqlWriter.print(relation.getFromColumnName());
    sqlWriter.print(") references ");
    sqlWriter.print(getFullyQualifiedTableName(schema.getTable(relation.getToTableName())));
    sqlWriter.print("(");
    sqlWriter.print(relation.getToColumnName());
    sqlWriter.print(") on delete ");
    sqlWriter.print(operation);
    sqlWriter.println(statementSeparator);
  }

  private String getRelationTypeOperation(RelationType relationType) {

    if (relationType == RelationType.ENFORCE || relationType == RelationType.DONOTHING) {
      return "no action";
    }

    if (relationType == RelationType.CASCADE) {
      return "cascade";
    }

    if (relationType == RelationType.SETNULL) {
      return "set null";
    }

    throw new IllegalArgumentException("Bad relation type.");
  }
}
