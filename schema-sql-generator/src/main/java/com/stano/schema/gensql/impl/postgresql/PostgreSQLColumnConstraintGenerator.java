package com.stano.schema.gensql.impl.postgresql;

import com.stano.schema.gensql.impl.common.ColumnConstraintGenerator;
import com.stano.schema.gensql.impl.common.SQLGenerator;
import com.stano.schema.model.Column;
import com.stano.schema.model.ColumnType;

public class PostgreSQLColumnConstraintGenerator extends ColumnConstraintGenerator {
  public PostgreSQLColumnConstraintGenerator(SQLGenerator sqlGenerator) {
    super(sqlGenerator);
  }

  @Override
  public String getCheckConstraintSQL(Column column) {
    if (column.getType() == ColumnType.ENUM) {
      return null;
    }

    if (column.getType() == ColumnType.VARCHAR) {
      return String.format("check(length(%s) <= %d)", column.getName(), column.getLength());
    }

    return super.getCheckConstraintSQL(column);
  }
}
