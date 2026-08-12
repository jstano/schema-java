package com.stano.schema.reverseengineer;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.DriverManager;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 * Command-line entry point that reverse-engineers a live database into an XML schema file.
 *
 * <p>It opens a JDBC connection using the {@code --database}, {@code --username}, and {@code
 * --password} command-line options, reads the connected database's metadata into a {@link
 * com.stano.schema.model.Schema} using {@link SchemaReader}, and then serializes that model as XML
 * to the file identified by the {@code --file} option using {@link SchemaWriter}.
 */
public class SchemaReverseEngineer {

  /**
   * Parses the command-line arguments, connects to the target database via JDBC, reads its schema
   * metadata, and writes the resulting schema to the requested output file as XML.
   *
   * <p>Required options are {@code --database} (JDBC URL), {@code --username}, {@code --password},
   * and {@code --file} (path of the XML file to write). Any failure while parsing arguments,
   * connecting to the database, reading the schema, or writing the file is caught and its stack
   * trace is printed to standard error.
   *
   * @param args the command-line arguments, parsed as {@code --database}, {@code --username},
   *     {@code --password}, and {@code --file} options
   */
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
