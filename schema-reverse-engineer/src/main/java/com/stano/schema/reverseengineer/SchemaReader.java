package com.stano.schema.reverseengineer;

import com.stano.schema.model.Column;
import com.stano.schema.model.ColumnType;
import com.stano.schema.model.Constraint;
import com.stano.schema.model.Key;
import com.stano.schema.model.KeyColumn;
import com.stano.schema.model.KeyType;
import com.stano.schema.model.Relation;
import com.stano.schema.model.RelationType;
import com.stano.schema.model.Schema;
import com.stano.schema.model.Table;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import org.apache.commons.text.StringEscapeUtils;

public class SchemaReader {
  public Schema readSchema(Connection connection) {
    try {
      var schema = new Schema(null);
      var metaData = connection.getMetaData();

      populateTables(schema, metaData);
      populateColumns(schema, metaData);
      populatePrimaryKeys(schema, metaData);
      populateConstraints(schema, connection);
      populateKeys(schema, metaData);
      populateImportedKeys(schema, metaData);

      return schema;
    } catch (SQLException x) {
      throw new RuntimeException(x);
    }
  }

  private void populateTables(Schema schema, DatabaseMetaData metaData) throws SQLException {
    try (var resultSet = metaData.getTables(null, null, null, new String[] {"TABLE"})) {
      while (resultSet.next()) {
        String tableName = resultSet.getString("TABLE_NAME");
        schema.addTable(new Table(schema, "", tableName, null, null, false));
      }
    }
  }

  private void populateColumns(Schema schema, DatabaseMetaData metaData) throws SQLException {
    try (var resultSet = metaData.getColumns(null, null, null, null)) {
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
                              generatedColumn.equals("YES")
                                  ? "generated always as " + columnDef + " stored"
                                  : null,
                              null,
                              null,
                              null,
                              getElementType(dataType, typeName, columnSize)));
                });
      }
    }
  }

  private void populatePrimaryKeys(Schema schema, DatabaseMetaData metaData) throws SQLException {
    var primaryKeyData = new HashMap<String, List<PrimaryKeyData>>();

    try (var resultSet = metaData.getPrimaryKeys(null, null, null)) {
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

  private void populateConstraints(Schema schema, Connection connection) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      try (ResultSet rs =
          statement.executeQuery(
              "SELECT conrelid::regclass AS tablename, conname, contype, pg_get_constraintdef(oid)"
                  + " AS definition"
                  + " FROM"
                  + " pg_constraint;")) { // WHERE conrelid =
        // 'classificationrulecriteriaconfig'::regclass
        while (rs.next()) {
          String tableName = rs.getString("tablename");
          String constraintName = rs.getString("conname");
          String constraintType = rs.getString("contype");
          String definition = rs.getString("definition");

          if (constraintType.equals("p")) {
          } else if (constraintType.equals("u")) {
          } else if (constraintType.equals("x") || constraintType.equals("c")) {
            schema
                .getOptionalTable(tableName)
                .ifPresent(
                    table -> {
                      table.getConstraints().add(new Constraint(constraintName, definition, null));
                    });
          }
        }
      }
    }
  }

  private void populateKeys(Schema schema, DatabaseMetaData metaData) throws SQLException {
    for (Table table : schema.getTables()) {
      var primaryKeysResultSet = metaData.getIndexInfo(null, null, table.getName(), false, false);
      var keys = new LinkedHashMap<String, List<KeyDataColumn>>();

      while (primaryKeysResultSet.next()) {
        String indexName = primaryKeysResultSet.getString("INDEX_NAME");
        boolean nonUnique = primaryKeysResultSet.getBoolean("NON_UNIQUE");
        String columnName = primaryKeysResultSet.getString("COLUMN_NAME");
        int ordinalPosition = primaryKeysResultSet.getInt("ORDINAL_POSITION");

        keys.putIfAbsent(indexName + ":::" + nonUnique, new ArrayList<>());
        keys.get(indexName + ":::" + nonUnique).add(new KeyDataColumn(columnName, ordinalPosition));
      }

      keys.forEach(
          (indexName, columns) -> {
            if (indexName.contains(":::true")) {
              table
                  .getKeys()
                  .add(
                      new Key(
                          KeyType.INDEX,
                          columns.stream()
                              .sorted()
                              .map(it -> escapeColumnName(it.columnName()))
                              .map(KeyColumn::new)
                              .toList()));
            } else {
              if (showIncludeKey(table, indexName, columns)) {
                table
                    .getKeys()
                    .add(
                        new Key(
                            KeyType.UNIQUE,
                            columns.stream()
                                .sorted()
                                .map(it -> escapeColumnName(it.columnName()))
                                .map(KeyColumn::new)
                                .toList()));
              }
            }
          });

      if (table.getPrimaryKey() == null) {
        var uniqueKeys =
            table.getKeys().stream().filter(it -> it.getType() == KeyType.UNIQUE).toList();

        if (uniqueKeys.size() == 1) {
          table.getKeys().removeIf(it -> it.getType() == KeyType.UNIQUE);
          table.getKeys().add(new Key(KeyType.PRIMARY, uniqueKeys.getFirst().getColumns()));
        }
      }
    }
  }

  private String escapeColumnName(String columnName) {
    return StringEscapeUtils.escapeXml11(columnName.replace("\n", ""));
  }

  private boolean showIncludeKey(Table table, String indexName, List<KeyDataColumn> columns) {
    if (table.getConstraints().stream().anyMatch(it -> it.getName().equals(indexName))) {
      return false;
    }

    if (table.getPrimaryKey() != null) {
      List<String> pkColumnNames =
          table.getPrimaryKey().getColumns().stream().map(KeyColumn::getName).toList();
      List<String> keyColumnNames =
          columns.stream().sorted().map(KeyDataColumn::columnName).toList();

      return !pkColumnNames.equals(keyColumnNames);
    }

    return true;
  }

  void populateImportedKeys(Schema schema, DatabaseMetaData metaData) throws SQLException {
    for (Table table : schema.getTables()) {
      var foreignKeys = new ArrayList<ForeignKeyData>();

      try (var resultSet = metaData.getImportedKeys(null, null, table.getName())) {
        while (resultSet.next()) {
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
                  pkTableName,
                  pkColumnName,
                  fkTableName,
                  fkColumnName,
                  keySeq,
                  updateRule,
                  deleteRule));
        }
      }

      for (var foreignKey : foreignKeys) {
        var deleteRule = foreignKey.deleteRule();
        var relationType =
            switch (deleteRule) {
              case "importedNoAction" -> RelationType.DONOTHING;
              case "importedKeyCascade" -> RelationType.CASCADE;
              case "importedKeyRestrict" -> RelationType.ENFORCE;
              case "importedKeySetNull" -> RelationType.SETNULL;
              case "importedKeySetDefault" -> RelationType.SETNULL;
              default -> RelationType.DONOTHING;
            };

        table
            .getRelations()
            .add(
                new Relation(
                    foreignKey.fkTableName(),
                    foreignKey.fkColumnName(),
                    foreignKey.pkTableName(),
                    foreignKey.pkColumnName(),
                    relationType,
                    false));
      }
    }
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

    throw new IllegalArgumentException("Unknown data type: " + dataType);
  }

  private ColumnType getElementType(int dataType, String typeName, int columnSize) {
    if (dataType != Types.ARRAY) {
      return null;
    }

    return switch (typeName) {
      case "_text" -> ColumnType.TEXT;
      case "_int2" -> ColumnType.SHORT;
      case "_int4" -> ColumnType.INT;
      case "_int8" -> ColumnType.LONG;
      default -> ColumnType.ARRAY;
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
