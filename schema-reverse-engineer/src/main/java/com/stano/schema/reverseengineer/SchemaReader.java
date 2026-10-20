package com.stano.schema.reverseengineer;

import com.stano.schema.model.Column;
import com.stano.schema.model.ColumnPair;
import com.stano.schema.model.ColumnType;
import com.stano.schema.model.Constraint;
import com.stano.schema.model.DatabaseType;
import com.stano.schema.model.Function;
import com.stano.schema.model.Key;
import com.stano.schema.model.KeyColumn;
import com.stano.schema.model.KeyType;
import com.stano.schema.model.Procedure;
import com.stano.schema.model.Relation;
import com.stano.schema.model.RelationType;
import com.stano.schema.model.Schema;
import com.stano.schema.model.Table;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SchemaReader {
  private static final Logger LOGGER = LoggerFactory.getLogger(SchemaReader.class);

  /** The Postgres schema introspected when no schema name is given explicitly. */
  public static final String DEFAULT_SCHEMA = "public";

  /**
   * Reads the given database's metadata into a {@link Schema}, introspecting only the {@code
   * public} schema. Equivalent to {@link #readSchema(Connection, String)} with {@link
   * #DEFAULT_SCHEMA}.
   */
  public Schema readSchema(Connection connection) {
    return readSchema(connection, DEFAULT_SCHEMA);
  }

  /**
   * Reads the given database's metadata into a {@link Schema}, introspecting only the named schema
   * (rather than every schema visible on the connection).
   *
   * @param connection the JDBC connection to introspect
   * @param dbSchema the name of the database schema to introspect (e.g. {@code "public"})
   * @return the resulting in-memory schema model, with every table's {@code schemaName} set to
   *     {@code dbSchema}
   */
  public Schema readSchema(Connection connection, String dbSchema) {
    try {
      var schema = new Schema(null);
      var metaData = connection.getMetaData();

      String product = metaData.getDatabaseProductName();
      if (!"PostgreSQL".equalsIgnoreCase(product)) {
        throw new IllegalArgumentException(
            "reverse engineering supports PostgreSQL only, but the database is " + product);
      }

      populateTables(schema, metaData, dbSchema);
      populateColumns(schema, metaData, dbSchema);
      populatePrimaryKeys(schema, metaData, dbSchema);
      populateConstraints(schema, connection, dbSchema);
      populateKeys(schema, connection, dbSchema);
      populateImportedKeys(schema, metaData, dbSchema);
      populateRoutines(schema, connection, dbSchema);

      return schema;
    } catch (SQLException x) {
      throw new RuntimeException(x);
    }
  }

  /**
   * {@code getTables}/{@code getColumns} take a LIKE pattern, so escape {@code _} and {@code %}.
   */
  private static String schemaPattern(DatabaseMetaData metaData, String dbSchema)
      throws SQLException {
    String escape = metaData.getSearchStringEscape();
    if (escape == null || escape.isEmpty()) {
      return dbSchema;
    }
    return dbSchema
        .replace(escape, escape + escape)
        .replace("_", escape + "_")
        .replace("%", escape + "%");
  }

  void populateTables(Schema schema, DatabaseMetaData metaData, String dbSchema)
      throws SQLException {
    try (var resultSet =
        metaData.getTables(null, schemaPattern(metaData, dbSchema), null, new String[] {"TABLE"})) {
      while (resultSet.next()) {
        String tableName = resultSet.getString("TABLE_NAME");
        schema.addTable(new Table(schema, dbSchema, tableName, null, null, false));
      }
    }
  }

  private void populateColumns(Schema schema, DatabaseMetaData metaData, String dbSchema)
      throws SQLException {
    try (var resultSet = metaData.getColumns(null, schemaPattern(metaData, dbSchema), null, null)) {
      while (resultSet.next()) {
        String tableName = resultSet.getString("TABLE_NAME");
        String columnName = resultSet.getString("COLUMN_NAME");
        int dataType = resultSet.getInt("DATA_TYPE");
        String typeName = resultSet.getString("TYPE_NAME");
        String columnDef = resultSet.getString("COLUMN_DEF");
        int columnSize = resultSet.getInt("COLUMN_SIZE");
        int decimalDigits = resultSet.getInt("DECIMAL_DIGITS");
        String nullable = resultSet.getString("IS_NULLABLE");
        String autoIncrement = resultSet.getString("IS_AUTOINCREMENT");
        String generatedColumn = resultSet.getString("IS_GENERATEDCOLUMN");

        schema
            .getOptionalTable(tableName)
            .ifPresent(
                table -> {
                  ColumnType columnType =
                      getColumnType(dataType, typeName, autoIncrement.equals("YES"), columnSize);
                  ColumnType elementType = getElementType(dataType, typeName);
                  if (columnType == ColumnType.ARRAY && elementType == null) {
                    Warnings.warn(
                        LOGGER,
                        "Column {}.{} has array type '{}', which the schema model can't"
                            + " represent - falling back to text",
                        tableName,
                        columnName,
                        typeName);
                    columnType = ColumnType.TEXT;
                  }

                  table
                      .getColumns()
                      .add(
                          new Column(
                              columnName,
                              columnType,
                              getColumnSize(columnType, columnSize),
                              getDecimalDigits(columnType, decimalDigits),
                              nullable.equals("NO"),
                              null,
                              generatedColumn.equals("YES") ? null : columnDef,
                              generatedColumn.equals("YES") ? columnDef : null,
                              null,
                              null,
                              null,
                              elementType));
                });
      }
    }
  }

  private void populatePrimaryKeys(Schema schema, DatabaseMetaData metaData, String dbSchema)
      throws SQLException {
    var primaryKeyData = new HashMap<String, List<PrimaryKeyData>>();

    try (var resultSet = metaData.getPrimaryKeys(null, dbSchema, null)) {
      while (resultSet.next()) {
        String tableName = resultSet.getString("TABLE_NAME");
        String columnName = resultSet.getString("COLUMN_NAME");
        int keySequence = resultSet.getInt("KEY_SEQ");

        primaryKeyData.putIfAbsent(tableName, new ArrayList<>());
        primaryKeyData
            .get(tableName)
            .add(new PrimaryKeyData(tableName, columnName, null, keySequence));
      }
    }

    for (String tableName : primaryKeyData.keySet()) {
      schema
          .getOptionalTable(tableName)
          .ifPresent(
              table -> {
                table
                    .getKeys()
                    .add(
                        new Key(
                            KeyType.PRIMARY,
                            primaryKeyData.get(tableName).stream()
                                .sorted()
                                .map(it -> new KeyColumn(it.columnName()))
                                .toList()));
              });
    }
  }

  private void populateConstraints(Schema schema, Connection connection, String dbSchema)
      throws SQLException {
    try (var statement =
        connection.prepareStatement(
            "SELECT c.relname AS tablename, con.conname, con.contype::text AS contype,"
                + " pg_get_constraintdef(con.oid) AS definition"
                + " FROM pg_constraint con"
                + " JOIN pg_class c ON c.oid = con.conrelid"
                + " JOIN pg_namespace n ON n.oid = c.relnamespace"
                + " WHERE n.nspname = ? AND con.contype IN ('c', 'x')"
                + " ORDER BY c.relname, con.conname")) {
      statement.setString(1, dbSchema);
      try (var rs = statement.executeQuery()) {
        while (rs.next()) {
          String tableName = rs.getString("tablename");
          String constraintName = rs.getString("conname");
          String definition = rs.getString("definition");

          // Primary and unique keys come from the key queries; only check and exclusion
          // constraints have no other representation.
          schema
              .getOptionalTable(tableName)
              .ifPresent(
                  table ->
                      table
                          .getConstraints()
                          .add(
                              new Constraint(constraintName, definition, DatabaseType.POSTGRESQL)));
        }
      }
    }
  }

  /**
   * One column slot of one index, as returned by the {@code pg_index} query. {@code columnName} is
   * {@code null} for an expression slot (an index on {@code lower(name)}, say).
   */
  record IndexRow(
      String tableName,
      String indexName,
      boolean unique,
      boolean primary,
      String columnName,
      boolean include,
      String predicate) {}

  private void populateKeys(Schema schema, Connection connection, String dbSchema)
      throws SQLException {
    var indexRows = new ArrayList<IndexRow>();

    try (var statement = connection.prepareStatement(INDEX_QUERY)) {
      statement.setString(1, dbSchema);
      try (var rs = statement.executeQuery()) {
        while (rs.next()) {
          indexRows.add(
              new IndexRow(
                  rs.getString("table_name"),
                  rs.getString("index_name"),
                  rs.getBoolean("is_unique"),
                  rs.getBoolean("is_primary"),
                  rs.getString("column_name"),
                  rs.getBoolean("is_include"),
                  rs.getString("predicate")));
        }
      }
    }

    applyIndexRows(schema, indexRows);

    for (Table table : schema.getTables()) {
      promoteLoneUniqueKey(table);
    }
  }

  private static final String INDEX_QUERY =
      "SELECT t.relname AS table_name, i.relname AS index_name, ix.indisunique AS is_unique,"
          + " ix.indisprimary AS is_primary, a.attname AS column_name,"
          + " (k.ord > ix.indnkeyatts) AS is_include,"
          + " pg_get_expr(ix.indpred, ix.indrelid) AS predicate"
          + " FROM pg_index ix"
          + " JOIN pg_class t ON t.oid = ix.indrelid"
          + " JOIN pg_class i ON i.oid = ix.indexrelid"
          + " JOIN pg_namespace n ON n.oid = t.relnamespace"
          + " CROSS JOIN LATERAL unnest(ix.indkey::int2[]) WITH ORDINALITY AS k(attnum, ord)"
          + " LEFT JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = k.attnum"
          + " WHERE n.nspname = ? AND t.relkind IN ('r', 'p')"
          + " ORDER BY t.relname, i.relname, k.ord";

  /**
   * Folds index rows (already ordered by table, index, then column position) into {@link Key}s on
   * the tables of {@code schema}. Partial indexes keep their predicate and covering indexes their
   * {@code INCLUDE} columns; a unique index with either is kept as an {@code <index unique>}
   * because {@code <unique>} can't carry them. The index backing the primary key and indexes behind
   * exclusion constraints (already captured as constraints) are skipped, as are expression indexes,
   * which the model can't represent.
   */
  static void applyIndexRows(Schema schema, List<IndexRow> rows) {
    record Group(
        IndexRow first, List<String> columns, List<String> include, boolean[] hasExpression) {}

    var groups = new ArrayList<Group>();
    for (IndexRow row : rows) {
      Group last = groups.isEmpty() ? null : groups.getLast();
      if (last == null
          || !last.first().tableName().equals(row.tableName())
          || !last.first().indexName().equals(row.indexName())) {
        last = new Group(row, new ArrayList<>(), new ArrayList<>(), new boolean[1]);
        groups.add(last);
      }
      if (row.columnName() == null) {
        last.hasExpression()[0] = true;
      } else if (row.include()) {
        last.include().add(row.columnName());
      } else {
        last.columns().add(row.columnName());
      }
    }

    for (Group group : groups) {
      IndexRow first = group.first();
      if (first.primary()) {
        continue;
      }
      if (group.hasExpression()[0]) {
        Warnings.warn(
            LOGGER,
            "Index {}.{} is an expression index, which the schema model can't represent -"
                + " skipping it",
            first.tableName(),
            first.indexName());
        continue;
      }

      var table = schema.getOptionalTable(first.tableName());
      if (table.isEmpty()) {
        continue;
      }
      // An index that merely backs an exclusion constraint is captured as a Constraint already.
      if (table.get().getConstraints().stream()
          .anyMatch(it -> it.getName().equals(first.indexName()))) {
        continue;
      }

      var columns = group.columns().stream().map(KeyColumn::new).toList();
      String include = group.include().isEmpty() ? null : String.join(",", group.include());
      String filter = first.predicate();

      if (first.unique() && include == null && filter == null) {
        table.get().getKeys().add(new Key(KeyType.UNIQUE, columns));
      } else {
        table
            .get()
            .getKeys()
            .add(new Key(KeyType.INDEX, columns, false, false, first.unique(), include, filter));
      }
    }
  }

  /**
   * A table with no primary key but exactly one unique key gets that key promoted to primary. Only
   * done when every key column is already {@code NOT NULL}: promoting a nullable column would
   * invent a {@code NOT NULL} the database doesn't have.
   */
  void promoteLoneUniqueKey(Table table) {
    if (table.getPrimaryKey() != null) {
      return;
    }

    var uniqueKeys = table.getKeys().stream().filter(it -> it.getType() == KeyType.UNIQUE).toList();
    if (uniqueKeys.size() != 1) {
      return;
    }

    Key unique = uniqueKeys.getFirst();
    boolean allRequired =
        unique.getColumns().stream()
            .allMatch(
                keyColumn ->
                    table.getColumns().stream()
                        .anyMatch(
                            c ->
                                c.getName().equalsIgnoreCase(keyColumn.getName())
                                    && c.isRequired()));
    if (allRequired) {
      table.getKeys().remove(unique);
      table.getKeys().add(new Key(KeyType.PRIMARY, unique.getColumns()));
    }
  }

  /** The uuid generator the PostgreSQL SQL generator emits itself; exporting it would duplicate. */
  private static final String GENERATED_UUID_FUNCTION = "generate_uuid";

  /**
   * Reads plain functions and procedures of {@code dbSchema} as {@code create or replace ...}
   * statements. Aggregates and window functions are skipped ({@code pg_get_functiondef} rejects
   * aggregates), as are functions owned by an extension.
   */
  private void populateRoutines(Schema schema, Connection connection, String dbSchema)
      throws SQLException {
    try (var statement =
        connection.prepareStatement(
            "SELECT p.proname::text AS name, p.prokind::text AS kind,"
                + " pg_get_functiondef(p.oid) AS definition"
                + " FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace"
                + " WHERE n.nspname = ? AND p.prokind IN ('f', 'p')"
                + " AND NOT EXISTS (SELECT 1 FROM pg_depend d WHERE d.objid = p.oid"
                + " AND d.deptype = 'e')"
                + " ORDER BY p.proname, p.oid")) {
      statement.setString(1, dbSchema);
      try (var rs = statement.executeQuery()) {
        while (rs.next()) {
          String name = rs.getString("name");
          String sql = trimRoutineDefinition(rs.getString("definition"));

          if (rs.getString("kind").equals("p")) {
            schema.addProcedure(new Procedure(dbSchema, name, DatabaseType.POSTGRESQL, sql));
          } else if (name.equals(GENERATED_UUID_FUNCTION)) {
            LOGGER.info(
                "Function {} is created by the PostgreSQL generator itself - not exporting it",
                name);
          } else {
            schema.addFunction(new Function(dbSchema, name, DatabaseType.POSTGRESQL, sql));
          }
        }
      }
    }
  }

  static String trimRoutineDefinition(String definition) {
    String sql = definition.strip();
    while (sql.endsWith(";")) {
      sql = sql.substring(0, sql.length() - 1).stripTrailing();
    }
    return sql;
  }

  void populateImportedKeys(Schema schema, DatabaseMetaData metaData, String dbSchema)
      throws SQLException {
    for (Table table : schema.getTables()) {
      var foreignKeys = new ArrayList<ForeignKeyData>();

      try (var resultSet = metaData.getImportedKeys(null, dbSchema, table.getName())) {
        while (resultSet.next()) {
          String fkName = resultSet.getString("FK_NAME");
          String pkSchemaName = resultSet.getString("PKTABLE_SCHEM");
          String pkTableName = resultSet.getString("PKTABLE_NAME");
          String pkColumnName = resultSet.getString("PKCOLUMN_NAME");
          String fkTableName = resultSet.getString("FKTABLE_NAME");
          String fkColumnName = resultSet.getString("FKCOLUMN_NAME");
          int keySeq = resultSet.getInt("KEY_SEQ");
          String updateRule =
              resultSet.getString(
                  "UPDATE_RULE"); // importedNoAction,importedKeyCascade,importedKeySetNull,importedKeySetDefault,importedKeyRestrict
          String deleteRule =
              resultSet.getString(
                  "DELETE_RULE"); // importedNoAction,importedKeyCascade,importedKeySetNull,importedKeySetDefault,importedKeyRestrict

          foreignKeys.add(
              new ForeignKeyData(
                  fkName,
                  pkSchemaName,
                  pkTableName,
                  pkColumnName,
                  fkTableName,
                  fkColumnName,
                  keySeq,
                  updateRule,
                  deleteRule));
        }
      }

      table
          .getRelations()
          .addAll(groupForeignKeys(withoutCrossSchemaTargets(foreignKeys, dbSchema)));
    }
  }

  /**
   * Drops foreign keys whose referenced table lives in a different schema. The model resolves
   * relation targets by table name within a single schema, so such a relation could not be resolved
   * when the generated XML is parsed back; it is skipped with a warning instead.
   */
  static List<ForeignKeyData> withoutCrossSchemaTargets(
      List<ForeignKeyData> foreignKeys, String dbSchema) {
    var kept = new ArrayList<ForeignKeyData>();
    for (var foreignKey : foreignKeys) {
      String pkSchema = foreignKey.pkSchemaName();
      if (dbSchema != null && pkSchema != null && !pkSchema.equalsIgnoreCase(dbSchema)) {
        Warnings.warn(
            LOGGER,
            "Foreign key {}.{} references {}.{} in another schema, which the schema model can't"
                + " represent - skipping it",
            foreignKey.fkTableName(),
            foreignKey.fkName(),
            pkSchema,
            foreignKey.pkTableName());
        continue;
      }
      kept.add(foreignKey);
    }
    return kept;
  }

  /**
   * Groups per-column {@link ForeignKeyData} rows (as returned by {@link
   * DatabaseMetaData#getImportedKeys}, one row per FK column) by {@code fkName} into one {@link
   * Relation} per constraint, ordered by {@code keySeq} within each group. A single-row group
   * produces a single-column {@link Relation}; a multi-row group produces a composite {@link
   * Relation} via {@link Relation#composite}.
   */
  static List<Relation> groupForeignKeys(List<ForeignKeyData> foreignKeys) {
    var byConstraint = new LinkedHashMap<String, List<ForeignKeyData>>();
    for (var foreignKey : foreignKeys) {
      byConstraint.computeIfAbsent(foreignKey.fkName(), k -> new ArrayList<>()).add(foreignKey);
    }

    var relations = new ArrayList<Relation>();
    for (var rows : byConstraint.values()) {
      var sortedRows = rows.stream().sorted().toList();
      var first = sortedRows.get(0);
      var relationType = mapDeleteRule(first.deleteRule());

      if (sortedRows.size() == 1) {
        relations.add(
            new Relation(
                first.fkTableName(),
                first.fkColumnName(),
                first.pkTableName(),
                first.pkColumnName(),
                relationType,
                false));
      } else {
        var columnPairs =
            sortedRows.stream()
                .map(row -> new ColumnPair(row.fkColumnName(), row.pkColumnName()))
                .toList();
        relations.add(
            Relation.composite(
                first.fkTableName(), first.pkTableName(), columnPairs, relationType, false));
      }
    }

    return relations;
  }

  /**
   * Maps a JDBC {@code DELETE_RULE} to a {@link RelationType}. {@code SET DEFAULT} has no
   * equivalent (mapping it to {@code SETNULL} would null the column instead of restoring its
   * default), so it and any unknown rule are rejected. {@code NO ACTION} maps to {@code ENFORCE}:
   * {@code DONOTHING} means no foreign key constraint is generated at all.
   *
   * <p>Real drivers report the rule as the {@link DatabaseMetaData} integer constant (as a string);
   * the constant names are accepted too.
   */
  static RelationType mapDeleteRule(String deleteRule) {
    String rule = deleteRule == null ? "" : deleteRule;
    return switch (rule) {
      case "importedKeyCascade", "0" -> RelationType.CASCADE;
      case "importedKeyRestrict", "1", "importedNoAction", "3" -> RelationType.ENFORCE;
      case "importedKeySetNull", "2" -> RelationType.SETNULL;
      default ->
          throw new IllegalStateException(
              "unsupported ON DELETE rule '"
                  + rule
                  + "' (supported: CASCADE, SET NULL, RESTRICT, NO ACTION)");
    };
  }

  ColumnType getColumnType(int dataType, String typeName, boolean autoIncrement, int columnSize) {
    if (dataType == Types.INTEGER) {
      if (autoIncrement) {
        return ColumnType.SEQUENCE;
      }

      return ColumnType.INT;
    }
    if (dataType == Types.BIGINT) {
      if (autoIncrement) {
        return ColumnType.LONGSEQUENCE;
      }

      return ColumnType.LONG;
    }
    if (dataType == Types.SMALLINT) {
      return ColumnType.SHORT;
    }
    if (dataType == Types.TINYINT) {
      return ColumnType.BYTE;
    }
    if (dataType == Types.FLOAT) {
      return ColumnType.FLOAT;
    }
    if (dataType == Types.DOUBLE) {
      return ColumnType.DOUBLE;
    }
    if (dataType == Types.DECIMAL) {
      return ColumnType.DECIMAL;
    }
    if (dataType == Types.VARCHAR || dataType == Types.NVARCHAR) {
      if (typeName.equalsIgnoreCase("text") || columnSize == Integer.MAX_VALUE) {
        return ColumnType.TEXT;
      }
      return ColumnType.VARCHAR;
    }
    if (dataType == Types.CHAR || dataType == Types.NCHAR) {
      return ColumnType.CHAR;
    }
    if (dataType == Types.LONGVARCHAR || dataType == Types.LONGNVARCHAR) {
      return ColumnType.TEXT;
    }
    if (dataType == Types.BINARY
        || dataType == Types.VARBINARY
        || dataType == Types.LONGVARBINARY) {
      return ColumnType.BINARY;
    }
    if (dataType == Types.DATE) {
      return ColumnType.DATE;
    }
    if (dataType == Types.TIME) {
      return ColumnType.TIME;
    }
    if (dataType == Types.TIME_WITH_TIMEZONE) {
      return ColumnType.TIMESTAMPTZ;
    }
    if (dataType == Types.TIMESTAMP) {
      return ColumnType.DATETIME;
    }
    if (dataType == Types.BOOLEAN || dataType == Types.BIT) {
      return ColumnType.BOOLEAN;
    }
    if (dataType == Types.ARRAY) {
      return ColumnType.ARRAY;
    }
    if (typeName.equals("json")) {
      return ColumnType.JSON;
    }
    if (typeName.equals("numeric")) {
      return ColumnType.DECIMAL;
    }
    if (dataType == Types.OTHER) {
      if (typeName.equalsIgnoreCase("uuid")) {
        return ColumnType.UUID;
      }
      if (typeName.equalsIgnoreCase("jsonb")) {
        return ColumnType.JSON;
      }
      return ColumnType.VARCHAR;
    }

    // An unsupported/unmapped type falls back to TEXT with a warning rather than aborting the
    // whole run - losing one column's exact type is far better than producing no output at all
    // for the rest of the database.
    Warnings.warn(
        LOGGER,
        "Unknown data type '{}' (typeName='{}') - falling back to text",
        dataType,
        typeName);
    return ColumnType.TEXT;
  }

  /**
   * The element type of a Postgres array column, or {@code null} for a non-array column or an
   * element type the schema model has no array support for. Array columns carry no usable length,
   * so character arrays map to {@code text}.
   */
  private ColumnType getElementType(int dataType, String typeName) {
    if (dataType != Types.ARRAY) {
      return null;
    }

    return switch (typeName) {
      case "_text", "_varchar", "_bpchar" -> ColumnType.TEXT;
      case "_int2" -> ColumnType.SHORT;
      case "_int4" -> ColumnType.INT;
      case "_int8" -> ColumnType.LONG;
      default -> null;
    };
  }

  private int getColumnSize(ColumnType columnType, int columnSize) {
    if (columnType == ColumnType.VARCHAR
        || columnType == ColumnType.CHAR
        || columnType == ColumnType.DECIMAL) {
      return columnSize;
    }

    return 0;
  }

  private int getDecimalDigits(ColumnType columnType, int decimalDigits) {
    if (columnType == ColumnType.DECIMAL) {
      return decimalDigits;
    }

    return 0;
  }
}
