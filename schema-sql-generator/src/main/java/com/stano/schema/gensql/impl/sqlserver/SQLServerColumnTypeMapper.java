package com.stano.schema.gensql.impl.sqlserver;

import com.stano.schema.gensql.impl.common.ColumnTypeMapper;
import com.stano.schema.model.BooleanMode;
import com.stano.schema.model.Column;
import com.stano.schema.model.EnumType;
import com.stano.schema.model.EnumValue;
import com.stano.schema.model.Schema;

public class SQLServerColumnTypeMapper extends ColumnTypeMapper {
  public SQLServerColumnTypeMapper(BooleanMode booleanMode, Schema schema) {
    super(booleanMode, schema);
  }

  @Override
  protected String getSequenceSql() {
    return "integer identity(1,1)";
  }

  @Override
  protected String getLongSequenceSql() {
    return "bigint identity(1,1)";
  }

  @Override
  protected String getNativeBooleanSql() {
    return "bit";
  }

  @Override
  protected String getDateSql() {
    return "datetime";
  }

  @Override
  protected String getDateTimeSql() {
    return "datetime";
  }

  @Override
  protected String getTimeSql() {
    return "datetime";
  }

  @Override
  protected String getTimestampTZSql() {
    return "datetimeoffset";
  }

  @Override
  protected String getCharSql(Column column) {
    return "char(" + column.getLength() + ")";
  }

  @Override
  protected String getVarcharSql(Column column) {
    return "nvarchar(" + (column.getLength() == -1 ? "max" : column.getLength()) + ")";
  }

  @Override
  protected String getTextSql(Column column) {
    return "nvarchar(max)";
  }

  @Override
  protected String getCitextSql() {
    return "nvarchar(max)";
  }

  @Override
  protected String getCstextSql() {
    return "nvarchar(max)";
  }

  @Override
  protected String getBinarySql() {
    return "varbinary(max)";
  }

  @Override
  protected String getBooleanSql() {
    if (booleanMode == BooleanMode.YES_NO) return "nvarchar(3)";
    if (booleanMode == BooleanMode.YN) return "nchar(1)";
    return getNativeBooleanSql();
  }

  @Override
  protected String getEnumSql(Column column) {
    if (schema == null) {
      return "nvarchar(255)";
    }
    EnumType enumType = schema.getEnumType(column.getEnumType());
    int minLength = Integer.MAX_VALUE;
    int maxLength = 0;
    for (EnumValue enumValue : enumType.getValues()) {
      String code = enumValue.getCode();
      minLength = Math.min(minLength, code.length());
      maxLength = Math.max(maxLength, code.length());
    }
    if (minLength != maxLength) return "nvarchar(" + maxLength + ")";
    return "nchar(" + maxLength + ")";
  }

  @Override
  protected String getUUIDSql(Column column) {
    return "uniqueidentifier";
  }

  @Override
  protected String getArraySql(Column column) {
    throw new UnsupportedOperationException("SQL Server does not support arrays");
  }
}
