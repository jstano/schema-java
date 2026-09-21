package com.stano.schema.gensql.impl.sqlserver;

import com.stano.schema.gensql.impl.common.SQLGenerator;
import com.stano.schema.gensql.impl.common.ViewGenerator;
import com.stano.schema.model.View;

class SQLServerViewGenerator extends ViewGenerator {

  SQLServerViewGenerator(SQLGenerator sqlGenerator) {

    super(sqlGenerator);
  }

  @Override
  protected String getFullyQualifiedViewName(View view) {

    String schemaName =
        view.getSchemaName().equalsIgnoreCase("public") ? "dbo" : view.getSchemaName();
    return schemaName + "." + view.getName();
  }

  @Override
  protected void outputView(View view) {

    String viewName = getFullyQualifiedViewName(view);

    sqlWriter.println(String.format("/* %s */", viewName));
    sqlWriter.println("if object_id('" + escapeSqlLiteral(viewName) + "', 'V') is not null");
    sqlWriter.println("   drop view " + viewName + statementSeparator);
    sqlWriter.println("create view " + viewName + " as");
    sqlWriter.println("   " + view.getSql() + statementSeparator);
    sqlWriter.println();
  }
}
