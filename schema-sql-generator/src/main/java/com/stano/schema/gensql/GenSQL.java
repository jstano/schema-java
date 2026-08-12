package com.stano.schema.gensql;

import com.stano.schema.gensql.impl.common.OutputMode;
import com.stano.schema.gensql.impl.common.SQLGenerator;
import com.stano.schema.gensql.impl.common.SQLGeneratorFactory;
import com.stano.schema.gensql.impl.common.SQLGeneratorOptions;
import com.stano.schema.model.BooleanMode;
import com.stano.schema.model.DatabaseType;
import com.stano.schema.model.ForeignKeyMode;
import com.stano.schema.model.Schema;
import com.stano.schema.parser.SchemaParser;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import org.apache.commons.io.IOUtils;

/**
 * Main entry point for generating SQL DDL from a parsed {@link Schema}.
 *
 * <p>This class can be used as a plain library class, by calling one of the {@code
 * generateSQL(...)} overloads directly with an already-parsed {@link Schema}, or run as a
 * command-line tool via {@link #main(String[])}:
 *
 * <pre>{@code
 * java -cp ... com.stano.schema.gensql.GenSQL <target-database> <schema-filename>
 *     [--foreign-key-mode=mode] [--boolean-mode=mode] [--output-indexes-only]
 *     [--output-triggers-only] [--postgresql-version=N]
 * }</pre>
 *
 * <p>where {@code <target-database>} is a comma-separated list of one or more of {@code H2}, {@code
 * POSTGRESQL}, or {@code SQL_SERVER}. For each target database, an output file named {@code
 * <schema-filename-without-extension>-<database-name>.sql} is written alongside the input schema
 * file.
 */
public class GenSQL {

  /** Factory used to obtain the {@link SQLGenerator} implementation for a given database type. */
  public SQLGeneratorFactory sqlGeneratorFactory = new SQLGeneratorFactory();

  /**
   * Generates SQL DDL for the given schema and writes it to the supplied writer, using the default
   * target PostgreSQL version.
   *
   * @param databaseType the database dialect to generate SQL for
   * @param schema the parsed schema to generate SQL from
   * @param writer the writer to which the generated SQL is written; it is closed once generation
   *     completes (successfully or not)
   * @param foreignKeyMode how foreign keys should be expressed in the generated SQL
   * @param booleanMode how boolean columns should be expressed in the generated SQL
   * @param outputMode which parts of the schema should be output
   * @param statementSeparator the separator written after each generated SQL statement
   */
  public void generateSQL(
      DatabaseType databaseType,
      Schema schema,
      PrintWriter writer,
      ForeignKeyMode foreignKeyMode,
      BooleanMode booleanMode,
      OutputMode outputMode,
      String statementSeparator) {
    try {
      SQLGenerator sqlGenerator =
          sqlGeneratorFactory.createSQLGenerator(
              new SQLGeneratorOptions(
                  schema,
                  writer,
                  databaseType,
                  foreignKeyMode,
                  booleanMode,
                  outputMode,
                  statementSeparator));

      sqlGenerator.generate();
    } finally {
      IOUtils.closeQuietly(writer);
    }
  }

  /**
   * Generates SQL DDL for the given schema and writes it to the supplied writer, outputting all
   * parts of the schema (equivalent to passing {@link OutputMode#ALL}).
   *
   * @param databaseType the database dialect to generate SQL for
   * @param schema the parsed schema to generate SQL from
   * @param writer the writer to which the generated SQL is written; it is closed once generation
   *     completes (successfully or not)
   * @param foreignKeyMode how foreign keys should be expressed in the generated SQL
   * @param booleanMode how boolean columns should be expressed in the generated SQL
   * @param statementSeparator the separator written after each generated SQL statement
   */
  public void generateSQL(
      DatabaseType databaseType,
      Schema schema,
      PrintWriter writer,
      ForeignKeyMode foreignKeyMode,
      BooleanMode booleanMode,
      String statementSeparator) {
    generateSQL(
        databaseType,
        schema,
        writer,
        foreignKeyMode,
        booleanMode,
        OutputMode.ALL,
        statementSeparator);
  }

  /**
   * Generates SQL DDL for the given schema and writes it to the supplied writer, using the database
   * type's default statement separator and the default target PostgreSQL version.
   *
   * @param databaseType the database dialect to generate SQL for
   * @param schema the parsed schema to generate SQL from
   * @param writer the writer to which the generated SQL is written; it is closed once generation
   *     completes (successfully or not)
   * @param foreignKeyMode how foreign keys should be expressed in the generated SQL
   * @param booleanMode how boolean columns should be expressed in the generated SQL
   * @param outputMode which parts of the schema should be output
   */
  public void generateSQL(
      DatabaseType databaseType,
      Schema schema,
      PrintWriter writer,
      ForeignKeyMode foreignKeyMode,
      BooleanMode booleanMode,
      OutputMode outputMode) {
    generateSQL(
        databaseType,
        schema,
        writer,
        foreignKeyMode,
        booleanMode,
        outputMode,
        databaseType.getStatementSeparator(),
        0);
  }

  /**
   * Generates SQL DDL for the given schema and writes it to the supplied writer.
   *
   * @param databaseType the database dialect to generate SQL for
   * @param schema the parsed schema to generate SQL from
   * @param writer the writer to which the generated SQL is written; it is closed once generation
   *     completes (successfully or not)
   * @param foreignKeyMode how foreign keys should be expressed in the generated SQL
   * @param booleanMode how boolean columns should be expressed in the generated SQL
   * @param outputMode which parts of the schema should be output
   * @param statementSeparator the separator written after each generated SQL statement
   * @param targetPostgresVersion the target PostgreSQL major version (e.g. 17, 18), or {@code 0} to
   *     use the default
   */
  public void generateSQL(
      DatabaseType databaseType,
      Schema schema,
      PrintWriter writer,
      ForeignKeyMode foreignKeyMode,
      BooleanMode booleanMode,
      OutputMode outputMode,
      String statementSeparator,
      int targetPostgresVersion) {
    try {
      SQLGenerator sqlGenerator =
          sqlGeneratorFactory.createSQLGenerator(
              new SQLGeneratorOptions(
                  schema,
                  writer,
                  databaseType,
                  foreignKeyMode,
                  booleanMode,
                  outputMode,
                  statementSeparator,
                  targetPostgresVersion));

      sqlGenerator.generate();
    } finally {
      IOUtils.closeQuietly(writer);
    }
  }

  private static File createOutputFile(String schemaFilename, DatabaseType databaseType) {
    String baseFilename = schemaFilename.substring(0, schemaFilename.lastIndexOf('.'));

    return new File(String.format("%s-%s.sql", baseFilename, databaseType.name().toLowerCase()));
  }

  private static String getSchemaFileName(String schemaFileName) {
    if (schemaFileName.matches("^[a-zA-Z]:.*$")) {
      return "/" + schemaFileName;
    }

    return schemaFileName;
  }

  /**
   * Command-line entry point that parses an XML schema file and generates SQL DDL for one or more
   * target databases.
   *
   * <p>Expected usage: {@code GenSQL <target-database> <schema-filename> [--foreign-key-mode=mode]
   * [--boolean-mode=mode] [--output-indexes-only] [--output-triggers-only]
   * [--postgresql-version=N]}
   *
   * <ul>
   *   <li>{@code <target-database>} — one or more of {@code H2}, {@code POSTGRESQL}, {@code
   *       SQL_SERVER}, separated by commas
   *   <li>{@code --foreign-key-mode=mode} — one of {@code none}, {@code relations}, {@code
   *       triggers}; defaults to the mode declared in the schema file
   *   <li>{@code --boolean-mode=mode} — one of {@code native}, {@code yes_no}, {@code yn}; defaults
   *       to the mode declared in the schema file
   *   <li>{@code --output-indexes-only} — output only index DDL
   *   <li>{@code --output-triggers-only} — output only trigger DDL
   *   <li>{@code --postgresql-version=N} — the target PostgreSQL major version (e.g. 17, 18)
   * </ul>
   *
   * <p>If fewer than two arguments are supplied, a usage message is printed and the JVM exits with
   * status {@code 1}. Any exception raised during processing is printed to standard error.
   *
   * @param args the command-line arguments, as described above
   */
  public static void main(String[] args) {
    try {
      if (args.length < 2) {
        System.out.println(
            "USAGE: GenSQL <target-database> <schema-filename> [--foreign-key-mode=mode]"
                + " [--boolean-mode=mode] [--output-indexes-only] [--output-triggers-only]"
                + " [--postgresql-version=N]");
        System.out.println(
            "   where <target-database> is one or more of: [H2,POSTGRESQL,SQL_SERVER] separated by"
                + " commas");
        System.out.println(
            "   and   <foreign-key-mode> is one of: none,relations,triggers (default is"
                + " relations)");
        System.out.println(
            "   and   <boolean-mode> is one of: native,yes_no,yn (default is native)");
        System.out.println("   and   <output-indexes-only> causes only indexes to be output");
        System.out.println("   and   <output-triggers-only> causes only triggers to be output");
        System.out.println(
            "   and   <postgresql-version> is the target PostgreSQL major version (e.g., 17,"
                + " 18)");
        System.exit(1);
      }

      String targetDatabases = args[0];
      String schemaFilename = getSchemaFileName(args[1]);
      URL schemaURL = new URI("file://" + schemaFilename).toURL();
      Schema schema = new SchemaParser().parseSchema(schemaURL);
      ForeignKeyMode foreignKeyMode = schema.getForeignKeyMode();
      BooleanMode booleanMode = schema.getBooleanMode();
      OutputMode outputMode = OutputMode.ALL;
      int targetPostgresVersion = 0;

      for (String arg : args) {
        if (arg.startsWith("--foreign-key-mode=")) {
          foreignKeyMode =
              ForeignKeyMode.valueOf(arg.substring("--foreign-key-mode=".length()).toUpperCase());
        } else if (arg.startsWith("--boolean-mode=")) {
          booleanMode =
              BooleanMode.valueOf(arg.substring("--boolean-mode=".length()).toUpperCase());
        } else if (arg.equals("--output-indexes-only")) {
          outputMode = OutputMode.INDEXES_ONLY;
        } else if (arg.equals("--output-triggers-only")) {
          outputMode = OutputMode.TRIGGERS_ONLY;
        } else if (arg.startsWith("--postgresql-version=")) {
          targetPostgresVersion = Integer.parseInt(arg.substring("--postgresql-version=".length()));
        }
      }

      GenSQL genSQL = new GenSQL();

      for (DatabaseType databaseType : DatabaseType.getDatabaseTypes(targetDatabases)) {
        genSQL.generateSQL(
            databaseType,
            schema,
            new PrintWriter(new FileWriter(createOutputFile(schemaFilename, databaseType))),
            foreignKeyMode,
            booleanMode,
            outputMode,
            databaseType.getStatementSeparator(),
            targetPostgresVersion);
      }
    } catch (Throwable x) {
      x.printStackTrace();
    }
  }
}
