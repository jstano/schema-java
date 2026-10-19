package com.stano.schema.gensql.impl.common;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stano.schema.gensql.impl.h2.H2Generator;
import com.stano.schema.gensql.impl.postgresql.PostgreSQLGenerator;
import com.stano.schema.gensql.impl.sqlserver.SQLServerGenerator;
import com.stano.schema.model.BooleanMode;
import com.stano.schema.model.Column;
import com.stano.schema.model.ColumnType;
import com.stano.schema.model.DatabaseType;
import com.stano.schema.model.ForeignKeyMode;
import com.stano.schema.model.Schema;
import com.stano.schema.model.Table;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Generated (computed) columns")
class GeneratedColumnTest {

  private static URL schemaUrl() {
    try {
      return URI.create("file:///schema.xml").toURL();
    } catch (MalformedURLException e) {
      throw new IllegalStateException(e);
    }
  }

  private static Schema schemaWithGeneratedColumn() {
    Schema schema = new Schema(schemaUrl());

    Table widget = new Table(null, "public", "widget", null, null, false);
    widget.getColumns().add(new Column("a", ColumnType.INT, 0, true));
    widget.getColumns().add(new Column("b", ColumnType.INT, 0, true));
    widget
        .getColumns()
        .add(
            new Column(
                "total", ColumnType.INT, 0, 0, true, null, null, "a + b", null, null, null, null));
    schema.addTable(widget);

    return schema;
  }

  private static String generate(DatabaseType databaseType) {
    StringWriter sw = new StringWriter();
    SQLGeneratorOptions options =
        new SQLGeneratorOptions(
            schemaWithGeneratedColumn(),
            new PrintWriter(sw),
            databaseType,
            ForeignKeyMode.RELATIONS,
            BooleanMode.NATIVE,
            OutputMode.ALL);

    switch (databaseType) {
      case POSTGRESQL -> new PostgreSQLGenerator(options).generate();
      case SQL_SERVER -> new SQLServerGenerator(options).generate();
      case H2 -> new H2Generator(options).generate();
    }

    return sw.toString();
  }

  @Test
  @DisplayName("PostgreSQL keeps the type and stores the value")
  void postgresql() {
    String sql = generate(DatabaseType.POSTGRESQL);

    assertTrue(sql.contains("total integer generated always as (a + b) stored not null"), sql);
  }

  @Test
  @DisplayName("SQL Server declares no type and persists the value")
  void sqlServer() {
    String sql = generate(DatabaseType.SQL_SERVER);

    assertTrue(sql.contains("total as (a + b) persisted not null"), sql);
  }

  @Test
  @DisplayName("H2 keeps the type and has no stored keyword")
  void h2() {
    String sql = generate(DatabaseType.H2);

    assertTrue(sql.contains("generated always as (a + b) not null"), sql);
    assertFalse(sql.contains("stored"), sql);
  }

  @Test
  @DisplayName("validate rejects a full clause and a generated column with a default")
  void validateRejectsFullClauseAndDefault() {
    Schema schema = new Schema(null);
    Table widget = new Table(null, "public", "widget", null, null, false);
    widget
        .getColumns()
        .add(
            new Column(
                "a",
                ColumnType.INT,
                0,
                0,
                false,
                null,
                null,
                "generated always as (1) stored",
                null,
                null,
                null,
                null));
    widget
        .getColumns()
        .add(new Column("b", ColumnType.INT, 0, 0, false, null, "2", "1", null, null, null, null));
    schema.addTable(widget);

    List<String> errors = schema.validate();

    assertTrue(errors.size() == 2, errors.toString());
  }
}
