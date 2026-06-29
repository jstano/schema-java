package com.stano.schema.gensql.impl.common;

import com.stano.schema.model.BooleanMode;
import com.stano.schema.model.Column;
import com.stano.schema.model.Schema;
import com.stano.schema.model.Table;

public abstract class ColumnTypeGenerator extends BaseGenerator {
  private final ColumnTypeMapper mapper;

  protected ColumnTypeGenerator(SQLGenerator sqlGenerator) {
    super(sqlGenerator);
    this.mapper = createMapper(booleanMode, schema);
  }

  protected abstract ColumnTypeMapper createMapper(BooleanMode booleanMode, Schema schema);

  public String getColumnTypeSql(Table table, Column column) {
    return mapper.toSqlType(column);
  }

  protected abstract String getUUIDDefaultValueSql(Schema schema);
}
