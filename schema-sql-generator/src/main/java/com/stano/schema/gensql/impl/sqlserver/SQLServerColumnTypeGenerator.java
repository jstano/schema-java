package com.stano.schema.gensql.impl.sqlserver;

import com.stano.schema.gensql.impl.common.ColumnTypeGenerator;
import com.stano.schema.gensql.impl.common.ColumnTypeMapper;
import com.stano.schema.gensql.impl.common.SQLGenerator;
import com.stano.schema.model.BooleanMode;
import com.stano.schema.model.Schema;

class SQLServerColumnTypeGenerator extends ColumnTypeGenerator {
  SQLServerColumnTypeGenerator(SQLGenerator sqlGenerator) {
    super(sqlGenerator);
  }

  @Override
  protected ColumnTypeMapper createMapper(BooleanMode booleanMode, Schema schema) {
    return new SQLServerColumnTypeMapper(booleanMode, schema);
  }

  @Override
  protected String getUUIDDefaultValueSql(Schema schema) {
    return "newid()";
  }
}
