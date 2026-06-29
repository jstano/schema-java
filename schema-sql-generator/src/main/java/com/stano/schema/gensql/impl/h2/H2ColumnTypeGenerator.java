package com.stano.schema.gensql.impl.h2;

import com.stano.schema.gensql.impl.common.ColumnTypeGenerator;
import com.stano.schema.gensql.impl.common.ColumnTypeMapper;
import com.stano.schema.gensql.impl.common.SQLGenerator;
import com.stano.schema.model.BooleanMode;
import com.stano.schema.model.Schema;

class H2ColumnTypeGenerator extends ColumnTypeGenerator {
  H2ColumnTypeGenerator(SQLGenerator sqlGenerator) {
    super(sqlGenerator);
  }

  @Override
  protected ColumnTypeMapper createMapper(BooleanMode booleanMode, Schema schema) {
    return new H2ColumnTypeMapper(booleanMode, schema);
  }

  @Override
  protected String getUUIDDefaultValueSql(Schema schema) {
    return "random_uuid(7)";
  }
}
