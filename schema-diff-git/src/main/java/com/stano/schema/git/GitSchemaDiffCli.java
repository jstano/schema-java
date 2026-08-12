package com.stano.schema.git;

import com.stano.schema.diff.ChangeSet;
import com.stano.schema.diff.SchemaDiffEngine;
import com.stano.schema.genmigration.GenMigration;
import com.stano.schema.model.DatabaseType;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Command-line entry point that diffs a schema XML file against its own git history and emits
 * migration SQL for the detected changes.
 *
 * <p>Given a single schema file argument, this CLI compares the version of that file committed at
 * {@code HEAD} against the current working-tree contents (via {@link GitSchemaReader}), computes
 * the resulting {@link ChangeSet} (via {@link SchemaDiffEngine}), and generates dialect-specific
 * migration SQL for that changeset (via {@link GenMigration}), writing the output either to a file
 * or to standard output.
 *
 * <p>Usage: {@code java -cp ... com.stano.schema.git.GitSchemaDiffCli <schema-file> [options]}
 *
 * <p>Supported options:
 *
 * <ul>
 *   <li>{@code -o, --output <file>} — write migration SQL to the given file; defaults to standard
 *       output when omitted.
 *   <li>{@code -d, --database-type <type>} — target database dialect, one of {@code postgresql},
 *       {@code h2}, or {@code sql_server} (case-insensitive); defaults to {@code postgresql} when
 *       omitted.
 *   <li>{@code -h, --help} — print usage information and exit.
 * </ul>
 *
 * <p>The process exits with status 0 on success or when help is explicitly requested, and with
 * status 1 on any argument parsing error, missing/extra arguments, an unrecognized database type,
 * or any other failure encountered while reading or diffing the schema.
 */
public class GitSchemaDiffCli {
  private static final Options OPTIONS =
      new Options()
          .addOption(
              Option.builder("o")
                  .longOpt("output")
                  .hasArg()
                  .argName("file")
                  .desc("write migration SQL to file (default: stdout)")
                  .build())
          .addOption(
              Option.builder("d")
                  .longOpt("database-type")
                  .hasArg()
                  .argName("type")
                  .desc("database type: postgresql, h2, sql_server (default: postgresql)")
                  .build())
          .addOption(Option.builder("h").longOpt("help").desc("print this help").build());

  /**
   * Parses command-line arguments, reads the committed and current versions of the given schema
   * file, diffs them, and writes the resulting migration SQL to the requested destination.
   *
   * <p>Prints an error message to standard error and terminates the JVM (via {@link
   * System#exit(int)}) with a non-zero status on any parsing failure, invalid/missing arguments,
   * unknown database type, or other exception raised while processing the schema.
   *
   * @param args the command-line arguments: the schema file path plus any of the supported {@code
   *     -o}, {@code -d}, or {@code -h} options
   */
  public static void main(String[] args) {
    var parser = new DefaultParser();
    try {
      var cmd = parser.parse(OPTIONS, args);

      if (cmd.hasOption("h") || cmd.getArgs().length == 0) {
        printHelp();
        System.exit(cmd.hasOption("h") ? 0 : 1);
      }

      if (cmd.getArgs().length > 1) {
        System.err.println("Error: too many arguments");
        printHelp();
        System.exit(1);
      }

      DatabaseType databaseType = DatabaseType.POSTGRESQL;
      if (cmd.hasOption("d")) {
        try {
          databaseType = DatabaseType.valueOf(cmd.getOptionValue("d").toUpperCase());
        } catch (IllegalArgumentException e) {
          System.err.println("Error: unknown database type: " + cmd.getOptionValue("d"));
          System.err.println("Valid types: postgresql, h2, sql_server");
          System.exit(1);
        }
      }

      var schemaFile = Path.of(cmd.getArgs()[0]);
      var versions = new GitSchemaReader().readSchemas(schemaFile);
      var changeSet =
          new SchemaDiffEngine().diff(versions.getCommittedSchema(), versions.getCurrentSchema());

      if (cmd.hasOption("o")) {
        writeToFile(changeSet, databaseType, Path.of(cmd.getOptionValue("o")));
      } else {
        writeToStdout(changeSet, databaseType);
      }
    } catch (ParseException e) {
      System.err.println("Error: " + e.getMessage());
      printHelp();
      System.exit(1);
    } catch (Exception e) {
      System.err.println("Error: " + e.getMessage());
      System.exit(1);
    }
  }

  private static void writeToStdout(ChangeSet changeSet, DatabaseType databaseType) {
    try (var writer = new PrintWriter(System.out)) {
      new GenMigration().generateMigrationSQL(databaseType, changeSet, writer);
    }
  }

  private static void writeToFile(ChangeSet changeSet, DatabaseType databaseType, Path outputPath)
      throws IOException {
    try (var writer = new PrintWriter(outputPath.toFile())) {
      new GenMigration().generateMigrationSQL(databaseType, changeSet, writer);
    }
  }

  private static void printHelp() {
    var formatter = new HelpFormatter();
    formatter.printHelp("schema-diff-git <schema-file> [options]", OPTIONS);
  }
}
