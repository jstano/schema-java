package com.stano.schema.reverseengineer;

import com.stano.schema.model.Column;
import com.stano.schema.model.ColumnType;
import com.stano.schema.model.Constraint;
import com.stano.schema.model.DatabaseType;
import com.stano.schema.model.Function;
import com.stano.schema.model.Key;
import com.stano.schema.model.KeyColumn;
import com.stano.schema.model.KeyType;
import com.stano.schema.model.Procedure;
import com.stano.schema.model.Schema;
import com.stano.schema.model.View;
import com.stano.schema.reverseengineer.SchemaReader.IndexRow;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The SQL Server half of {@link SchemaReader}: columns, check constraints, indexes, routines and
 * views are read from the {@code sys.*} catalog, because JDBC metadata hides identity seeds,
 * computed columns and filtered/covering indexes. Tables, primary keys and foreign keys come from
 * {@link SchemaReader}'s shared JDBC metadata code.
 */
final class SqlServerSchemaReader {
  private static final Logger LOGGER = LoggerFactory.getLogger(SqlServerSchemaReader.class);

  /** The SQL Server schema introspected when no schema name is given explicitly. */
  static final String DEFAULT_SCHEMA = "dbo";

  /** {@code sys.indexes.type} values that are ordinary B-tree indexes. */
  private static final int CLUSTERED = 1;

  private static final int NONCLUSTERED = 2;

  /** One row of the {@code sys.columns} query. {@code maxLength} is bytes; {@code -1} is max. */
  record ColumnRow(
      String tableName,
      String columnName,
      String typeName,
      int maxLength,
      int precision,
      int scale,
      boolean nullable,
      boolean identity,
      Long identitySeed,
      Long identityIncrement,
      String computedDefinition,
      boolean persisted,
      String defaultDefinition) {}

  void populate(Schema schema, Connection connection, String dbSchema) throws SQLException {
    populateColumns(schema, connection, dbSchema);
    populateConstraints(schema, connection, dbSchema);
    populateKeys(schema, connection, dbSchema);
    populateRoutines(schema, connection, dbSchema);
    populateViews(schema, connection, dbSchema);
  }

  private void populateColumns(Schema schema, Connection connection, String dbSchema)
      throws SQLException {
    try (var statement = connection.prepareStatement(COLUMN_QUERY)) {
      statement.setString(1, dbSchema);
      try (var rs = statement.executeQuery()) {
        while (rs.next()) {
          var row =
              new ColumnRow(
                  rs.getString("table_name"),
                  rs.getString("column_name"),
                  rs.getString("type_name"),
                  rs.getInt("max_length"),
                  rs.getInt("precision"),
                  rs.getInt("scale"),
                  rs.getInt("is_nullable") != 0,
                  rs.getInt("is_identity") != 0,
                  parseLong(rs.getString("identity_seed")),
                  parseLong(rs.getString("identity_increment")),
                  rs.getString("computed_definition"),
                  rs.getInt("is_persisted") != 0,
                  rs.getString("default_definition"));
          schema
              .getOptionalTable(row.tableName())
              .ifPresent(table -> table.getColumns().add(buildColumn(row)));
        }
      }
    }
  }

  private static final String COLUMN_QUERY =
      "SELECT t.name AS table_name, c.name AS column_name,"
          + " CASE WHEN ty.is_user_defined = 1 THEN bt.name ELSE ty.name END AS type_name,"
          + " CAST(c.max_length AS int) AS max_length, CAST(c.precision AS int) AS precision,"
          + " CAST(c.scale AS int) AS scale, CAST(c.is_nullable AS int) AS is_nullable,"
          + " CAST(c.is_identity AS int) AS is_identity,"
          + " CAST(CAST(ic.seed_value AS bigint) AS nvarchar(40)) AS identity_seed,"
          + " CAST(CAST(ic.increment_value AS bigint) AS nvarchar(40)) AS identity_increment,"
          + " cc.definition AS computed_definition,"
          + " CAST(ISNULL(cc.is_persisted, 0) AS int) AS is_persisted,"
          + " dc.definition AS default_definition"
          + " FROM sys.columns c"
          + " JOIN sys.tables t ON t.object_id = c.object_id"
          + " JOIN sys.types ty ON ty.user_type_id = c.user_type_id"
          + " LEFT JOIN sys.types bt ON bt.user_type_id = c.system_type_id"
          + " LEFT JOIN sys.identity_columns ic"
          + " ON ic.object_id = c.object_id AND ic.column_id = c.column_id"
          + " LEFT JOIN sys.computed_columns cc"
          + " ON cc.object_id = c.object_id AND cc.column_id = c.column_id"
          + " LEFT JOIN sys.default_constraints dc ON dc.object_id = c.default_object_id"
          + " WHERE t.schema_id = SCHEMA_ID(?) AND t.is_ms_shipped = 0"
          + " ORDER BY t.name, c.column_id";

  private static Long parseLong(String value) {
    if (value == null) {
      return null;
    }
    try {
      return Long.parseLong(value.trim());
    } catch (NumberFormatException x) {
      return null;
    }
  }

  static Column buildColumn(ColumnRow row) {
    String location = row.tableName() + "." + row.columnName();
    String typeName = row.typeName().toLowerCase(Locale.ROOT);

    ColumnType columnType = mapColumnType(typeName, row.precision());
    if (columnType == null) {
      // One exotic column (xml, geography, sql_variant, ...) shouldn't abort the run.
      Warnings.warn(
          LOGGER, "Column {} has unsupported type '{}' - falling back to text", location, typeName);
      columnType = ColumnType.TEXT;
    }
    if (typeName.equals("money") || typeName.equals("smallmoney")) {
      Warnings.warn(LOGGER, "Column {} is {} - exported as decimal", location, typeName);
    }

    if (row.identity()) {
      if (columnType == ColumnType.INT) {
        columnType = ColumnType.SEQUENCE;
      } else if (columnType == ColumnType.LONG) {
        columnType = ColumnType.LONGSEQUENCE;
      } else {
        Warnings.warn(
            LOGGER,
            "Column {} is an identity column of type {}, which the model can't represent -"
                + " identity dropped",
            location,
            typeName);
      }
      long seed = row.identitySeed() == null ? 1 : row.identitySeed();
      long increment = row.identityIncrement() == null ? 1 : row.identityIncrement();
      if ((columnType == ColumnType.SEQUENCE || columnType == ColumnType.LONGSEQUENCE)
          && (seed != 1 || increment != 1)) {
        Warnings.warn(
            LOGGER,
            "Column {} is identity({},{}) - exported as identity(1,1)",
            location,
            seed,
            increment);
      }
    }

    // varchar(max) / nvarchar(max) report max_length -1; the model's unbounded text is TEXT.
    boolean unicode = typeName.startsWith("n");
    int charLength = row.maxLength() == -1 ? -1 : unicode ? row.maxLength() / 2 : row.maxLength();
    if ((columnType == ColumnType.CHAR || columnType == ColumnType.VARCHAR) && charLength == -1) {
      columnType = ColumnType.TEXT;
    }

    int length =
        switch (columnType) {
          case CHAR, VARCHAR -> charLength;
          case BINARY -> row.maxLength() > 0 ? row.maxLength() : 0;
          case DECIMAL ->
              switch (typeName) {
                case "money" -> 19;
                case "smallmoney" -> 10;
                default -> row.precision();
              };
          default -> 0;
        };
    int scale =
        columnType == ColumnType.DECIMAL
            ? (typeName.equals("money") || typeName.equals("smallmoney") ? 4 : row.scale())
            : 0;

    String generated = null;
    if (row.computedDefinition() != null) {
      if (!row.persisted()) {
        Warnings.warn(
            LOGGER,
            "Column {} is a non-persisted computed column - exported as persisted",
            location);
      }
      generated = stripOuterParens(row.computedDefinition());
    }

    // A computed column's own default is meaningless, and the model rejects the combination.
    String defaultConstraint =
        generated != null || row.identity() || row.defaultDefinition() == null
            ? null
            : stripOuterParens(row.defaultDefinition());

    return new Column(
        row.columnName(),
        columnType,
        length,
        scale,
        !row.nullable(),
        null,
        defaultConstraint,
        generated,
        null,
        null,
        null,
        null);
  }

  static ColumnType mapColumnType(String typeName, int precision) {
    return switch (typeName) {
      case "int" -> ColumnType.INT;
      case "bigint" -> ColumnType.LONG;
      case "smallint" -> ColumnType.SHORT;
      case "tinyint" -> ColumnType.BYTE;
      case "bit" -> ColumnType.BOOLEAN;
      case "real" -> ColumnType.FLOAT;
      // float(n) with n <= 24 is stored as a 4-byte real.
      case "float" -> precision <= 24 ? ColumnType.FLOAT : ColumnType.DOUBLE;
      case "decimal", "numeric", "money", "smallmoney" -> ColumnType.DECIMAL;
      case "char", "nchar" -> ColumnType.CHAR;
      case "varchar", "nvarchar" -> ColumnType.VARCHAR;
      case "text", "ntext" -> ColumnType.TEXT;
      case "binary", "varbinary", "image", "rowversion", "timestamp" -> ColumnType.BINARY;
      case "date" -> ColumnType.DATE;
      case "time" -> ColumnType.TIME;
      case "datetime", "smalldatetime" -> ColumnType.DATETIME;
      case "datetime2" -> ColumnType.TIMESTAMP;
      case "datetimeoffset" -> ColumnType.TIMESTAMPTZ;
      case "uniqueidentifier" -> ColumnType.UUID;
      case "json" -> ColumnType.JSON;
      default -> null;
    };
  }

  /**
   * Strips every pair of parentheses that wraps the whole expression. SQL Server stores column
   * defaults and computed-column / check expressions wrapped ({@code ((0))}, {@code (getdate())},
   * {@code ([a]*[b])}); the generators add their own punctuation, so the bare expression is kept.
   */
  static String stripOuterParens(String expression) {
    String current = expression.trim();
    while (current.startsWith("(") && current.endsWith(")") && wrapsWhole(current)) {
      current = current.substring(1, current.length() - 1).trim();
    }
    return current;
  }

  /** True when the {@code (} at the start matches the {@code )} at the very end. */
  private static boolean wrapsWhole(String expression) {
    int depth = 0;
    boolean inString = false;
    for (int i = 0; i < expression.length(); i++) {
      char ch = expression.charAt(i);
      if (ch == '\'') {
        if (inString && i + 1 < expression.length() && expression.charAt(i + 1) == '\'') {
          i++;
        } else {
          inString = !inString;
        }
      } else if (!inString && ch == '(') {
        depth++;
      } else if (!inString && ch == ')') {
        depth--;
        if (depth == 0 && i != expression.length() - 1) {
          return false;
        }
      }
    }
    return depth == 0;
  }

  private void populateConstraints(Schema schema, Connection connection, String dbSchema)
      throws SQLException {
    // Primary and unique keys are read as keys and defaults live on the column, so only check
    // constraints have no other representation.
    try (var statement =
        connection.prepareStatement(
            "SELECT t.name AS table_name, cc.name AS name, cc.definition AS definition"
                + " FROM sys.check_constraints cc"
                + " JOIN sys.tables t ON t.object_id = cc.parent_object_id"
                + " WHERE t.schema_id = SCHEMA_ID(?) AND t.is_ms_shipped = 0"
                + " ORDER BY t.name, cc.name")) {
      statement.setString(1, dbSchema);
      try (var rs = statement.executeQuery()) {
        while (rs.next()) {
          String name = rs.getString("name");
          String sql = checkSql(rs.getString("definition"));
          schema
              .getOptionalTable(rs.getString("table_name"))
              .ifPresent(
                  table ->
                      table
                          .getConstraints()
                          .add(new Constraint(name, sql, DatabaseType.SQL_SERVER)));
        }
      }
    }
  }

  /**
   * {@code sys.check_constraints.definition} is just the parenthesised predicate; the generator
   * emits {@code constraint <name> <sql>}, so the {@code check} keyword has to be part of the SQL.
   */
  static String checkSql(String definition) {
    return "check (" + stripOuterParens(definition) + ")";
  }

  private void populateKeys(Schema schema, Connection connection, String dbSchema)
      throws SQLException {
    var rows = new ArrayList<IndexRow>();
    // Rows of one index are skipped as a whole; remember which ones already had their warning.
    var skipped = new java.util.HashSet<String>();

    try (var statement = connection.prepareStatement(INDEX_QUERY)) {
      statement.setString(1, dbSchema);
      try (var rs = statement.executeQuery()) {
        while (rs.next()) {
          String table = rs.getString("table_name");
          String index = rs.getString("index_name");
          int type = rs.getInt("index_type");
          if (type != CLUSTERED && type != NONCLUSTERED) {
            if (skipped.add(table + "." + index)) {
              Warnings.warn(
                  LOGGER,
                  "Index {}.{} is a {} index, which the schema model can't represent - skipping"
                      + " it",
                  table,
                  index,
                  rs.getString("type_desc").toLowerCase(Locale.ROOT).replace('_', ' '));
            }
            continue;
          }
          rows.add(
              new IndexRow(
                  table,
                  index,
                  rs.getInt("is_unique") != 0,
                  rs.getInt("is_primary") != 0,
                  rs.getString("column_name"),
                  rs.getInt("is_include") != 0,
                  rs.getString("filter")));
        }
      }
    }

    applyPrimaryKeys(schema, rows);
    SchemaReader.applyIndexRows(schema, rows);
  }

  /**
   * Adds each table's primary key from the index rows (ordered by key position within an index).
   * The mssql driver's {@code getPrimaryKeys} returns nothing when called without a table name, so
   * the shared JDBC primary-key code isn't used for SQL Server.
   */
  static void applyPrimaryKeys(Schema schema, List<IndexRow> rows) {
    var columnsByTable = new LinkedHashMap<String, List<KeyColumn>>();
    for (IndexRow row : rows) {
      if (row.primary() && !row.include() && row.columnName() != null) {
        columnsByTable
            .computeIfAbsent(row.tableName(), k -> new ArrayList<>())
            .add(new KeyColumn(row.columnName()));
      }
    }
    columnsByTable.forEach(
        (tableName, columns) ->
            schema
                .getOptionalTable(tableName)
                .ifPresent(table -> table.getKeys().add(new Key(KeyType.PRIMARY, columns))));
  }

  private static final String INDEX_QUERY =
      "SELECT t.name AS table_name, i.name AS index_name, CAST(i.type AS int) AS index_type,"
          + " i.type_desc AS type_desc, CAST(i.is_unique AS int) AS is_unique,"
          + " CAST(i.is_primary_key AS int) AS is_primary, c.name AS column_name,"
          + " CAST(ic.is_included_column AS int) AS is_include,"
          + " i.filter_definition AS filter"
          + " FROM sys.indexes i"
          + " JOIN sys.tables t ON t.object_id = i.object_id"
          + " JOIN sys.index_columns ic ON ic.object_id = i.object_id AND ic.index_id = i.index_id"
          + " JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id"
          + " WHERE t.schema_id = SCHEMA_ID(?) AND t.is_ms_shipped = 0"
          + " AND i.is_hypothetical = 0 AND i.type > 0"
          + " ORDER BY t.name, i.index_id, ic.is_included_column, ic.key_ordinal,"
          + " ic.index_column_id";

  /**
   * Reads T-SQL functions (scalar, inline and multi-statement table-valued) and stored procedures
   * as their full {@code CREATE ...} text from {@code sys.sql_modules}. CLR routines are skipped.
   */
  private void populateRoutines(Schema schema, Connection connection, String dbSchema)
      throws SQLException {
    try (var statement =
        connection.prepareStatement(
            "SELECT o.name AS name, RTRIM(o.type) AS kind, m.definition AS definition"
                + " FROM sys.objects o"
                + " JOIN sys.sql_modules m ON m.object_id = o.object_id"
                + " WHERE o.schema_id = SCHEMA_ID(?) AND o.is_ms_shipped = 0"
                + " AND o.type IN ('FN', 'IF', 'TF', 'P')"
                + " ORDER BY o.name")) {
      statement.setString(1, dbSchema);
      try (var rs = statement.executeQuery()) {
        while (rs.next()) {
          String name = rs.getString("name");
          String definition = rs.getString("definition");
          if (definition == null) {
            Warnings.warn(
                LOGGER, "Definition of {} is not visible (encrypted?) - skipping it", name);
            continue;
          }
          String sql = definition.strip();
          if (rs.getString("kind").equals("P")) {
            schema.addProcedure(new Procedure(dbSchema, name, DatabaseType.SQL_SERVER, sql));
          } else {
            schema.addFunction(new Function(dbSchema, name, DatabaseType.SQL_SERVER, sql));
          }
        }
      }
    }
  }

  private void populateViews(Schema schema, Connection connection, String dbSchema)
      throws SQLException {
    try (var statement =
        connection.prepareStatement(
            "SELECT v.name AS name, m.definition AS definition"
                + " FROM sys.views v"
                + " JOIN sys.sql_modules m ON m.object_id = v.object_id"
                + " WHERE v.schema_id = SCHEMA_ID(?) AND v.is_ms_shipped = 0"
                + " ORDER BY v.name")) {
      statement.setString(1, dbSchema);
      try (var rs = statement.executeQuery()) {
        while (rs.next()) {
          String name = rs.getString("name");
          String definition = rs.getString("definition");
          if (definition == null) {
            Warnings.warn(
                LOGGER, "Definition of view {} is not visible (encrypted?) - skipping it", name);
            continue;
          }
          String body = viewBody(name, definition);
          if (body == null) {
            Warnings.warn(
                LOGGER,
                "Couldn't find the SELECT in the definition of view {} - skipping it",
                name);
            continue;
          }
          schema.addView(new View(dbSchema, name, body, DatabaseType.SQL_SERVER));
        }
      }
    }
  }

  /**
   * The query of a view's {@code CREATE VIEW <name> [(columns)] [WITH ...] AS <query>}, or {@code
   * null} when there is none. The model stores only the query (the generator writes the {@code
   * create view} header), so a column list or {@code WITH SCHEMABINDING} between the name and
   * {@code AS} is dropped with a warning.
   */
  static String viewBody(String viewName, String definition) {
    int asAt = findTopLevelAs(definition);
    if (asAt < 0) {
      return null;
    }
    if (headerHasExtras(definition.substring(0, asAt))) {
      Warnings.warn(
          LOGGER,
          "View {} has a column list or WITH options that the model can't represent -"
              + " exporting the query only",
          viewName);
    }
    String body = definition.substring(asAt + 2).strip();
    return body.isEmpty() ? null : body;
  }

  /** True when the header ({@code CREATE VIEW <name> ...} up to {@code AS}) holds more. */
  private static boolean headerHasExtras(String header) {
    var tokens = new ArrayList<>(java.util.Arrays.asList(header.trim().split("\\s+")));
    while (!tokens.isEmpty()
        && (tokens.getFirst().equalsIgnoreCase("create")
            || tokens.getFirst().equalsIgnoreCase("alter"))) {
      tokens.removeFirst();
    }
    // the VIEW keyword and the name
    return tokens.size() > 2;
  }

  /** Offset of the first {@code AS} keyword outside parentheses, quotes and brackets. */
  private static int findTopLevelAs(String sql) {
    int depth = 0;
    char quote = 0;
    for (int i = 0; i < sql.length(); i++) {
      char ch = sql.charAt(i);
      if (quote != 0) {
        if (ch == quote) {
          quote = 0;
        }
        continue;
      }
      switch (ch) {
        case '\'', '"' -> quote = ch;
        case '[' -> quote = ']';
        case '(' -> depth++;
        case ')' -> depth--;
        case 'a', 'A' -> {
          if (depth == 0
              && i + 1 < sql.length()
              && (sql.charAt(i + 1) == 's' || sql.charAt(i + 1) == 'S')
              && (i == 0 || !isIdentifierChar(sql.charAt(i - 1)))
              && (i + 2 >= sql.length() || !isIdentifierChar(sql.charAt(i + 2)))) {
            return i;
          }
        }
        default -> {}
      }
    }
    return -1;
  }

  private static boolean isIdentifierChar(char ch) {
    return Character.isLetterOrDigit(ch) || ch == '_' || ch == '@' || ch == '#' || ch == '$';
  }
}
