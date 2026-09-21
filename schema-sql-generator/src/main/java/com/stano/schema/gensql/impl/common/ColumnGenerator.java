package com.stano.schema.gensql.impl.common;

import com.stano.schema.model.BooleanMode;
import com.stano.schema.model.Column;
import com.stano.schema.model.ColumnType;
import com.stano.schema.model.Table;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

public abstract class ColumnGenerator extends BaseGenerator {
  private static final String DF_PREFIX = "df_";

  protected ColumnGenerator(SQLGenerator sqlGenerator) {
    super(sqlGenerator);
  }

  public List<String> getColumnDefinitions(Table table) {
    return table.getColumns().stream()
        .map(column -> getColumnSql(table, column))
        .collect(Collectors.toList());
  }

  protected abstract ColumnTypeGenerator getColumnTypeGenerator();

  protected String getColumnSql(Table table, Column column) {
    String columnOptions = getColumnOptions(table, column);

    if (StringUtils.isNotBlank(columnOptions)) {
      return String.format(
          "   %s %s %s",
          column.getName(),
          getColumnTypeGenerator().getColumnTypeSql(table, column),
          columnOptions);
    }

    return String.format(
        "   %s %s", column.getName(), getColumnTypeGenerator().getColumnTypeSql(table, column));
  }

  protected String getColumnOptions(Table table, Column column) {
    StringBuilder columnOptions = new StringBuilder();

    if (column.isRequired()) {
      if (columnOptions.length() > 0) {
        columnOptions.append(' ');
      }

      columnOptions.append("not null");
    }

    String defaultValue = getDefaultValue(table, column);

    if (defaultValue != null) {
      if (!columnOptions.isEmpty()) {
        columnOptions.append(' ');
      }

      columnOptions.append(createDefaultConstraint(table, column, defaultValue));

      if (column.getGenerated() != null) {
        columnOptions.append(" ");
        columnOptions.append(column.getGenerated());
      }
    }

    return columnOptions.toString().trim();
  }

  protected String getDefaultValue(Table table, Column column) {
    String defaultValue = column.getDefaultConstraint();

    if (column.getType() == ColumnType.BOOLEAN) {
      if (defaultValue != null) {
        defaultValue =
            Column.parseBooleanDefault(defaultValue)
                .map(this::convertBooleanDefaultConstraint)
                .orElse(null);
      }
      // No `default` attribute at all means no default constraint — do not silently default
      // a nullable boolean column to `false`.
    } else if (column.getType() == ColumnType.UUID) {
      List<String> primaryKeyColumns = table.getPrimaryKeyColumns();

      if (column.isRequired()
          && primaryKeyColumns.size() == 1
          && primaryKeyColumns.contains(column.getName())
          && table.getColumnRelation(column) == null) {
        return getColumnTypeGenerator().getUUIDDefaultValueSql(table.getSchema());
      } else if (defaultValue != null && defaultValue.equalsIgnoreCase("generate_uuid()")) {
        return getColumnTypeGenerator().getUUIDDefaultValueSql(table.getSchema());
      }
    }

    return defaultValue;
  }

  protected String createDefaultConstraint(Table table, Column column, String defaultValue) {
    return String.format(
        "constraint %s default %s",
        buildDefaultConstraintName(table.getName(), column.getName()), defaultValue);
  }

  protected String convertBooleanDefaultConstraint(boolean defaultValue) {
    if (booleanMode == BooleanMode.YES_NO) {
      return defaultValue ? "'Yes'" : "'No'";
    }

    if (booleanMode == BooleanMode.YN) {
      return defaultValue ? "'Y'" : "'N'";
    }

    return defaultValue ? "true" : "false";
  }

  private String buildDefaultConstraintName(String tableName, String columnName) {
    tableName = tableName.toLowerCase();
    columnName = columnName.toLowerCase();

    String name = tableName + "_" + columnName;
    String hashCode = Integer.toHexString(name.hashCode()).toUpperCase();

    if (tableName.length() > 9) {
      tableName = tableName.substring(0, 9);
    }

    if (columnName.length() > 9) {
      columnName = columnName.substring(0, 9);
    }

    return DF_PREFIX + tableName + "_" + columnName + "_" + hashCode;
  }
}
