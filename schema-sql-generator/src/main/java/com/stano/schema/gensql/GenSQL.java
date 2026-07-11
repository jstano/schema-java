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

public class GenSQL {
  public SQLGeneratorFactory sqlGeneratorFactory = new SQLGeneratorFactory();

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
