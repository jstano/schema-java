package com.stano.schema.gensql.impl.h2;

import com.stano.schema.gensql.impl.common.ColumnTypeGenerator;
import com.stano.schema.gensql.impl.common.ColumnTypeMapper;
import com.stano.schema.gensql.impl.common.SQLGenerator;
import com.stano.schema.model.BooleanMode;
import com.stano.schema.model.Column;
import com.stano.schema.model.Schema;
import com.stano.schema.model.Table;

class H2ColumnTypeGenerator extends ColumnTypeGenerator {
  H2ColumnTypeGenerator(SQLGenerator sqlGenerator) {
    super(sqlGenerator);
  }

  @Override
  protected ColumnTypeMapper createMapper(BooleanMode booleanMode, Schema schema) {
    return new H2ColumnTypeMapper(booleanMode, schema);
  }

  /** H2 computes the value on read, so it has no {@code stored} keyword. */
  @Override
  public String getGeneratedColumnSql(Table table, Column column, String expression) {
    return String.format(
        "%s generated always as (%s)", getColumnTypeSql(table, column), expression);
  }

  @Override
  protected String getUUIDDefaultValueSql(Schema schema) {
    return "random_uuid(7)";
  }
}
