package com.stano.schema.importer;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.DriverManager;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class SchemaImporter {
  public static void main(String[] args) {
    try {
      var options = new Options();
      options.addOption(
          Option.builder()
              .longOpt("database")
              .hasArg()
              .required()
              .desc("database to connect to")
              .get());
      options.addOption(
          Option.builder()
              .longOpt("username")
              .hasArg()
              .required()
              .desc("username to connect with")
              .get());
      options.addOption(
          Option.builder()
              .longOpt("password")
              .hasArg()
              .required()
              .desc("password to connect with")
              .get());
      options.addOption(
          Option.builder()
              .longOpt("file")
              .hasArg()
              .required()
              .desc("file to write schema to")
              .get());

      var parser = new DefaultParser();
      var cmd = parser.parse(options, args);

      try (var connection =
          DriverManager.getConnection(
              cmd.getOptionValue("database"),
              cmd.getOptionValue("username"),
              cmd.getOptionValue("password"))) {
        var schemaFile = new File(cmd.getOptionValue("file"));
        var schemaReader = new SchemaReader();
        var schema = schemaReader.readSchema(connection);

        try (var writer = new PrintWriter(new FileWriter(schemaFile))) {
          var schemaWriter = new SchemaWriter(writer);
          schemaWriter.outputSchema(schema);
        }
      }
    } catch (Exception x) {
      x.printStackTrace();
    }
  }
}
