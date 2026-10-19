package com.stano.schema.gensql.impl.common;

import com.stano.schema.model.BooleanMode;
import com.stano.schema.model.Column;
import com.stano.schema.model.ColumnType;
import com.stano.schema.model.EnumValue;
import com.stano.schema.model.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ColumnConstraintGenerator extends BaseGenerator {
  private static final String CK_PREFIX = "ck_";

  protected ColumnConstraintGenerator(SQLGenerator sqlGenerator) {
    super(sqlGenerator);
  }

  public List<String> getColumnCheckConstraints(Table table) {
    List<Column> columns = table.getColumnsWithCheckConstraints(booleanMode);

    return columns.stream()
        .map(column -> generateConstraint(table, column))
        .filter(c -> c != null)
        .collect(Collectors.toList());
  }

  /**
   * Builds the {@code check(...)} clause for a column, or {@code null} if it needs none.
   *
   * <p>A column's boolean-mode restriction, user {@code <check>}, enum restriction and min/max
   * bounds are independent attributes, not alternatives, so every applicable one is collected and
   * AND'ed into a single clause (the hashed constraint name only has room for one constraint per
   * column). A lone clause is rendered without extra parentheses; two or more are each wrapped in
   * their own parentheses so a user check containing {@code or} can't change meaning.
   */
  public String getCheckConstraintSQL(Column column) {
    List<String> clauses = new ArrayList<>();

    if (column.getType() == ColumnType.BOOLEAN) {
      if (booleanMode == BooleanMode.YES_NO) {
        clauses.add(String.format("%s in ('Yes','No')", column.getName()));
      } else if (booleanMode == BooleanMode.YN) {
        clauses.add(String.format("%s in ('Y','N')", column.getName()));
      }
    }

    if (column.getCheckConstraint() != null) {
      clauses.add(stripCheckWrapper(column.getCheckConstraint()));
    }

    if (column.getType() == ColumnType.ENUM) {
      clauses.add(buildEnumCheckClause(column));
    }

    if (column.hasMinOrMaxValue()) {
      clauses.add(buildMinMaxCheckClause(column));
    }

    if (clauses.isEmpty()) {
      return null;
    }

    if (clauses.size() == 1) {
      return "check(" + clauses.get(0) + ")";
    }

    return "check("
        + clauses.stream().map(it -> "(" + it + ")").collect(Collectors.joining(" and "))
        + ")";
  }

  /**
   * Reduces a user-supplied check expression to a bare predicate: a {@code <check>} holds just the
   * expression, but the user may have written the {@code check(...)} wrapper themselves anyway.
   */
  private String stripCheckWrapper(String expression) {
    String trimmed = expression.trim();

    if (trimmed.toLowerCase().startsWith("check")) {
      String rest = trimmed.substring(5).trim();
      if (rest.startsWith("(") && rest.endsWith(")")) {
        return rest.substring(1, rest.length() - 1);
      }
    }

    return trimmed;
  }

  private String generateConstraint(Table table, Column column) {
    String checkSql = getCheckConstraintSQL(column);
    if (checkSql == null) {
      return null;
    }
    return String.format(
        "   constraint %s %s", getConstraintName(table.getName(), column.getName()), checkSql);
  }

  /**
   * Builds the {@code ck_<table>_<column>_<hash>} name this generator gives a column's check
   * constraint, so a caller outside a full {@code CREATE TABLE} generation (e.g.
   * schema-migration-generator's {@code AddColumn} handling) can produce the exact same name.
   */
  public String getConstraintName(String tableName, String columnName) {
    return constraintName(tableName, columnName);
  }

  /** Static form of {@link #getConstraintName}; it needs no schema, so it works without one. */
  public static String constraintName(String tableName, String columnName) {
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

    return CK_PREFIX + tableName + "_" + columnName + "_" + hashCode;
  }

  private String buildEnumCheckClause(Column column) {
    List<EnumValue> enumValues = schema.getEnumType(column.getEnumType()).getValues();

    return String.format(
        "%s in (%s)",
        column.getName(),
        enumValues.stream().map(it -> "'" + it.getCode() + "'").collect(Collectors.joining(",")));
  }

  private String buildMinMaxCheckClause(Column column) {
    List<String> bounds = new ArrayList<>();

    if (column.getMinValue() != null) {
      bounds.add(column.getName() + " >= " + column.getMinValue());
    }

    if (column.getMaxValue() != null) {
      bounds.add(column.getName() + " <= " + column.getMaxValue());
    }

    return String.join(" and ", bounds);
  }
}
