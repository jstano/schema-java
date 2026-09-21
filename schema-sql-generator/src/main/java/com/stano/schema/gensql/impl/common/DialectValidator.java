package com.stano.schema.gensql.impl.common;

import com.stano.schema.model.Column;
import com.stano.schema.model.ColumnType;
import com.stano.schema.model.DatabaseType;
import com.stano.schema.model.Procedure;
import com.stano.schema.model.Schema;
import com.stano.schema.model.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Dialect-specific checks {@link Schema#validate()} can't make on its own, since it has no notion
 * of which dialect will render the model — one schema.xml is meant to target all three databases.
 *
 * <p>Callers must run this in addition to {@link Schema#validate()} before generating: several
 * generator code paths ({@code getArraySql}, {@code getEnumSql}) throw on a violation here, on the
 * assumption this already ran.
 */
public final class DialectValidator {

  /**
   * Kept in sync by hand with {@code PostgreSQLColumnTypeMapper.getArraySql}'s supported element
   * types.
   */
  private static final Set<ColumnType> POSTGRES_ARRAY_ELEMENT_TYPES =
      Set.of(
          ColumnType.BYTE,
          ColumnType.SHORT,
          ColumnType.INT,
          ColumnType.LONG,
          ColumnType.DECIMAL,
          ColumnType.CHAR,
          ColumnType.VARCHAR,
          ColumnType.TEXT);

  private DialectValidator() {}

  public static List<String> validateForDialect(Schema schema, DatabaseType databaseType) {
    List<String> errors = new ArrayList<>();

    for (Table table : schema.getTables()) {
      for (Column column : table.getColumns()) {
        if (column.getType() != ColumnType.ARRAY) {
          continue;
        }

        if (databaseType == DatabaseType.H2) {
          continue; // H2 has native array support
        }

        if (databaseType == DatabaseType.SQL_SERVER) {
          errors.add(
              String.format(
                  "ERROR: %s.%s is an array column, but SQL Server does not support array types",
                  table.getName(), column.getName()));
        } else if (databaseType == DatabaseType.POSTGRESQL) {
          ColumnType elementType = column.getElementType();

          if (elementType != null && !POSTGRES_ARRAY_ELEMENT_TYPES.contains(elementType)) {
            errors.add(
                String.format(
                    "ERROR: %s.%s is an array column with elementType '%s', which PostgreSQL array"
                        + " generation does not support (supported: byte, short, int, long,"
                        + " decimal, char, varchar, text)",
                    table.getName(), column.getName(), elementType));
          }
        }
      }
    }

    if (databaseType == DatabaseType.H2) {
      for (Procedure procedure : schema.getProcedures()) {
        if (procedure.getDatabaseType() == DatabaseType.H2) {
          errors.add(
              String.format(
                  "ERROR: procedure '%s' targets H2, but H2 SQL generation does not support"
                      + " stored procedures",
                  procedure.getName()));
        }
      }
    }

    return errors;
  }
}
