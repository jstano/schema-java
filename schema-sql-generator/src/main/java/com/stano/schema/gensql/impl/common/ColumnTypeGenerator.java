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

  /**
   * Returns the definition tail for a generated (computed) column, given its bare expression. The
   * default is the PostgreSQL form, which keeps the column type; databases with different syntax
   * override it.
   */
  public String getGeneratedColumnSql(Table table, Column column, String expression) {
    return String.format(
        "%s generated always as (%s) stored", getColumnTypeSql(table, column), expression);
  }

  protected abstract String getUUIDDefaultValueSql(Schema schema);
}
