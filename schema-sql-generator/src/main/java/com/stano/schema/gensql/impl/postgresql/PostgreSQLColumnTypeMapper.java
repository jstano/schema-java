package com.stano.schema.gensql.impl.postgresql;

import com.stano.schema.gensql.impl.common.ColumnTypeMapper;
import com.stano.schema.model.BooleanMode;
import com.stano.schema.model.Column;
import com.stano.schema.model.ColumnType;
import com.stano.schema.model.Schema;

public class PostgreSQLColumnTypeMapper extends ColumnTypeMapper {
  public PostgreSQLColumnTypeMapper(BooleanMode booleanMode, Schema schema) {
    super(booleanMode, schema);
  }

  @Override
  protected String getSequenceSql() {
    return "serial";
  }

  @Override
  protected String getLongSequenceSql() {
    return "bigserial";
  }

  @Override
  protected String getByteSql() {
    return "smallint";
  }

  @Override
  protected String getNativeBooleanSql() {
    return "boolean";
  }

  @Override
  protected String getVarcharSql(Column column) {
    return "text";
  }

  @Override
  protected String getTextSql(Column column) {
    if (schema != null && !schema.isCaseSensitiveText()) {
      return "citext";
    }
    return "text";
  }

  @Override
  protected String getCitextSql() {
    return "citext";
  }

  @Override
  protected String getCstextSql() {
    return "text";
  }

  @Override
  protected String getBinarySql() {
    return "bytea";
  }

  @Override
  protected String getJsonSql() {
    return "jsonb";
  }

  @Override
  protected String getUUIDSql(Column column) {
    return "uuid";
  }

  @Override
  protected String getTimestampTZSql() {
    return "timestamptz";
  }

  @Override
  protected String getArraySql(Column column) {
    ColumnType elementType = column.getElementType();
    return switch (elementType) {
      case ColumnType.VARCHAR -> getVarcharSql(column) + "[]";
      case ColumnType.CHAR -> getCharSql(column) + "[]";
      case ColumnType.TEXT -> getTextSql(column) + "[]";
      case ColumnType.DECIMAL -> getDecimalSql(column) + "[]";
      case ColumnType.BYTE -> getByteSql() + "[]";
      case ColumnType.SHORT -> getShortSql() + "[]";
      case ColumnType.INT -> getIntSql() + "[]";
      case ColumnType.LONG -> getLongSql() + "[]";
      default ->
          throw new IllegalArgumentException("Unsupported array element type: " + elementType);
    };
  }

  @Override
  protected String getEnumSql(Column column) {
    if (schema == null) {
      return "varchar(255)";
    }
    String name = schema.getEnumType(column.getEnumType()).getName();
    return name.replaceAll("(?<=[a-z0-9])([A-Z])", "_$1").toLowerCase();
  }
}
