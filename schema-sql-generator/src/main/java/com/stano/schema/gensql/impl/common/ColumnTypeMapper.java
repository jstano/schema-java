package com.stano.schema.gensql.impl.common;

import com.stano.schema.model.BooleanMode;
import com.stano.schema.model.Column;
import com.stano.schema.model.ColumnType;
import com.stano.schema.model.EnumType;
import com.stano.schema.model.EnumValue;
import com.stano.schema.model.Schema;

public abstract class ColumnTypeMapper {
  protected final BooleanMode booleanMode;
  protected final Schema schema;

  protected ColumnTypeMapper(BooleanMode booleanMode, Schema schema) {
    this.booleanMode = booleanMode;
    this.schema = schema;
  }

  public String toSqlType(Column column) {
    ColumnType columnType = column.getType();

    if (columnType == ColumnType.SEQUENCE) return getSequenceSql();
    if (columnType == ColumnType.LONGSEQUENCE) return getLongSequenceSql();
    if (columnType == ColumnType.BYTE) return getByteSql();
    if (columnType == ColumnType.SHORT) return getShortSql();
    if (columnType == ColumnType.INT) return getIntSql();
    if (columnType == ColumnType.LONG) return getLongSql();
    if (columnType == ColumnType.FLOAT) return getFloatSql();
    if (columnType == ColumnType.DOUBLE) return getDoubleSql();
    if (columnType == ColumnType.DECIMAL) return getDecimalSql(column);
    if (columnType == ColumnType.BOOLEAN) return getBooleanSql();
    if (columnType == ColumnType.DATE) return getDateSql();
    if (columnType == ColumnType.DATETIME || columnType == ColumnType.TIMESTAMP)
      return getDateTimeSql();
    if (columnType == ColumnType.TIME) return getTimeSql();
    if (columnType == ColumnType.TIMESTAMPTZ) return getTimestampTZSql();
    if (columnType == ColumnType.CHAR) return getCharSql(column);
    if (columnType == ColumnType.VARCHAR) return getVarcharSql(column);
    if (columnType == ColumnType.TEXT) return getTextSql(column);
    if (columnType == ColumnType.CITEXT) return getCitextSql();
    if (columnType == ColumnType.CSTEXT) return getCstextSql();
    if (columnType == ColumnType.BINARY) return getBinarySql();
    if (columnType == ColumnType.UUID) return getUUIDSql(column);
    if (columnType == ColumnType.JSON) return getJsonSql();
    if (columnType == ColumnType.ENUM) return getEnumSql(column);
    if (columnType == ColumnType.ARRAY) return getArraySql(column);

    throw new IllegalArgumentException("Unsupported column type: " + columnType);
  }

  protected abstract String getSequenceSql();

  protected abstract String getLongSequenceSql();

  protected abstract String getTextSql(Column column);

  protected abstract String getCitextSql();

  protected abstract String getCstextSql();

  protected abstract String getBinarySql();

  protected abstract String getArraySql(Column column);

  protected String getByteSql() {
    return "tinyint";
  }

  protected String getShortSql() {
    return "smallint";
  }

  protected String getIntSql() {
    return "integer";
  }

  protected String getLongSql() {
    return "bigint";
  }

  protected String getFloatSql() {
    return "real";
  }

  protected String getDoubleSql() {
    return "double precision";
  }

  protected String getDecimalSql(Column column) {
    int length = column.getLength();
    int scale = column.getScale();
    if (length == 0 && scale == 0) return "decimal";
    if (scale == 0) return "decimal(" + length + ")";
    return "decimal(" + length + "," + scale + ")";
  }

  protected String getBooleanSql() {
    if (booleanMode == BooleanMode.YES_NO) return "varchar(3)";
    if (booleanMode == BooleanMode.YN) return "char(1)";
    return getNativeBooleanSql();
  }

  protected String getDateSql() {
    return "date";
  }

  protected String getDateTimeSql() {
    return "timestamp";
  }

  protected String getTimeSql() {
    return "time";
  }

  protected String getTimestampTZSql() {
    return "timestamp";
  }

  protected String getCharSql(Column column) {
    return "char(" + column.getLength() + ")";
  }

  protected String getVarcharSql(Column column) {
    return "varchar(" + column.getLength() + ")";
  }

  protected String getUUIDSql(Column column) {
    return "varchar(36)";
  }

  protected String getJsonSql() {
    return getTextSql(null);
  }

  protected String getEnumSql(Column column) {
    if (schema == null) {
      return "varchar(255)";
    }
    EnumType enumType = schema.getEnumType(column.getEnumType());
    int minLength = Integer.MAX_VALUE;
    int maxLength = 0;
    for (EnumValue enumValue : enumType.getValues()) {
      String code = enumValue.getCode();
      minLength = Math.min(minLength, code.length());
      maxLength = Math.max(maxLength, code.length());
    }
    if (minLength != maxLength) return "varchar(" + maxLength + ")";
    return "char(" + maxLength + ")";
  }

  protected String getNativeBooleanSql() {
    return "boolean";
  }
}
