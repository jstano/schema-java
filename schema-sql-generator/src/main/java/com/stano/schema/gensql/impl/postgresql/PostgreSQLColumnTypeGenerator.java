package com.stano.schema.gensql.impl.postgresql;

import com.stano.schema.gensql.impl.common.ColumnTypeGenerator;
import com.stano.schema.gensql.impl.common.ColumnTypeMapper;
import com.stano.schema.gensql.impl.common.SQLGenerator;
import com.stano.schema.model.BooleanMode;
import com.stano.schema.model.Schema;

public class PostgreSQLColumnTypeGenerator extends ColumnTypeGenerator {
  protected PostgreSQLColumnTypeGenerator(SQLGenerator sqlGenerator) {
    super(sqlGenerator);
  }

  @Override
  protected ColumnTypeMapper createMapper(BooleanMode booleanMode, Schema schema) {
    return new PostgreSQLColumnTypeMapper(booleanMode, schema);
  }

  @Override
  protected String getUUIDDefaultValueSql(Schema schema) {
    if (sqlGenerator.getSqlGeneratorOptions().getTargetPostgresVersion() >= 18) {
      return "uuidv7()";
    }
    return "generate_uuid()";
  }
}
