package com.stano.schema.gensql.impl.sqlserver;

import com.stano.schema.gensql.impl.common.ColumnTypeGenerator;
import com.stano.schema.gensql.impl.common.ColumnTypeMapper;
import com.stano.schema.gensql.impl.common.SQLGenerator;
import com.stano.schema.model.BooleanMode;
import com.stano.schema.model.Column;
import com.stano.schema.model.Schema;
import com.stano.schema.model.Table;

class SQLServerColumnTypeGenerator extends ColumnTypeGenerator {
  SQLServerColumnTypeGenerator(SQLGenerator sqlGenerator) {
    super(sqlGenerator);
  }

  @Override
  protected ColumnTypeMapper createMapper(BooleanMode booleanMode, Schema schema) {
    return new SQLServerColumnTypeMapper(
        booleanMode, schema, sqlGenerator.getSqlGeneratorOptions().getTargetSqlServerVersion());
  }

  /** SQL Server computed columns declare no data type; they are always persisted. */
  @Override
  public String getGeneratedColumnSql(Table table, Column column, String expression) {
    return String.format("as (%s) persisted", expression);
  }

  @Override
  protected String getUUIDDefaultValueSql(Schema schema) {
    return "newid()";
  }
}
