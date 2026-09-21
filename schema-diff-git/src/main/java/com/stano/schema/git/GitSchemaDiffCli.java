package com.stano.schema.git;

import com.stano.schema.diff.ChangeSet;
import com.stano.schema.diff.SchemaDiffEngine;
import com.stano.schema.genmigration.GenMigration;
import com.stano.schema.model.DatabaseType;
import com.stano.schema.model.Schema;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
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
 * <p>Alternatively, {@code --old}/{@code --new} may be given instead of the positional schema-file
 * argument, to diff two arbitrary schema sources: a plain file path, or a git {@code <rev>:<path>}
 * reference (e.g. {@code HEAD:schema.xml}, {@code v1.2.0:schema/schema.xml}).
 *
 * <p>Supported options:
 *
 * <ul>
 *   <li>{@code -o, --output <file>} — write migration SQL to the given file; defaults to standard
 *       output when omitted. Not usable together with {@code --auto-generate-name}.
 *   <li>{@code -d, --database-type <type>} — target database dialect, one of {@code postgresql},
 *       {@code h2}, or {@code sql_server} (case-insensitive); defaults to {@code postgresql} when
 *       omitted.
 *   <li>{@code --old <source>} — the old (current) schema source: a file path, or a git {@code
 *       <rev>:<path>} reference. Requires {@code --new}; conflicts with the positional schema-file
 *       argument.
 *   <li>{@code --new <source>} — the new (target) schema source, in the same form as {@code --old}.
 *   <li>{@code --auto-generate-name} — auto-names the output as {@code V{timestamp}
 *       [__{description}].sql} alongside the schema file, instead of using {@code --output}.
 *       Requires the positional schema-file argument (not usable with {@code --old}/{@code --new}).
 *   <li>{@code --description <text>} — optional description used by {@code --auto-generate-name}
 *       (e.g. {@code "add users table"}).
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
          .addOption(
              Option.builder()
                  .longOpt("old")
                  .hasArg()
                  .argName("source")
                  .desc(
                      "old (current) schema source: a file path, or <rev>:<path> (requires --new)")
                  .build())
          .addOption(
              Option.builder()
                  .longOpt("new")
                  .hasArg()
                  .argName("source")
                  .desc("new (target) schema source: a file path, or <rev>:<path> (requires --old)")
                  .build())
          .addOption(
              Option.builder()
                  .longOpt("auto-generate-name")
                  .desc(
                      "auto-name the output as V{timestamp}[__{description}].sql alongside the"
                          + " schema file (requires the positional schema-file argument)")
                  .build())
          .addOption(
              Option.builder()
                  .longOpt("description")
                  .hasArg()
                  .argName("text")
                  .desc("description used by --auto-generate-name, e.g. 'add users table'")
                  .build())
          .addOption(Option.builder("h").longOpt("help").desc("print this help").build());

  /**
   * Parses command-line arguments, reads the two schema versions to compare (either the committed
   * and current versions of the given schema file, or the sources given via {@code --old}/{@code
   * --new}), diffs them, and writes the resulting migration SQL to the requested destination.
   *
   * <p>Prints an error message to standard error and terminates the JVM (via {@link
   * System#exit(int)}) with a non-zero status on any parsing failure, invalid/missing/conflicting
   * arguments, unknown database type, or other exception raised while processing the schema.
   *
   * @param args the command-line arguments: either the schema file path, or {@code --old}/{@code
   *     --new}, plus any of the supported {@code -o}, {@code -d}, {@code --auto-generate-name},
   *     {@code --description}, or {@code -h} options
   */
  public static void main(String[] args) {
    var parser = new DefaultParser();
    try {
      var cmd = parser.parse(OPTIONS, args);

      if (cmd.hasOption("h")) {
        printHelp();
        System.exit(0);
      }

      boolean hasOld = cmd.hasOption("old");
      boolean hasNew = cmd.hasOption("new");
      boolean hasPositionalFile = cmd.getArgs().length > 0;

      if (hasOld != hasNew) {
        System.err.println("Error: --old and --new must be given together");
        printHelp();
        System.exit(1);
      }

      if ((hasOld || hasNew) && hasPositionalFile) {
        System.err.println("Error: --old/--new cannot be combined with a schema-file argument");
        printHelp();
        System.exit(1);
      }

      if (!hasOld && !hasPositionalFile) {
        printHelp();
        System.exit(1);
      }

      if (cmd.getArgs().length > 1) {
        System.err.println("Error: too many arguments");
        printHelp();
        System.exit(1);
      }

      boolean autoGenerateName = cmd.hasOption("auto-generate-name");

      if (autoGenerateName && !hasPositionalFile) {
        System.err.println("Error: --auto-generate-name requires a schema-file argument");
        printHelp();
        System.exit(1);
      }

      if (autoGenerateName && cmd.hasOption("o")) {
        System.err.println("Error: --auto-generate-name cannot be combined with --output");
        printHelp();
        System.exit(1);
      }

      if (cmd.hasOption("description") && !autoGenerateName) {
        System.err.println("Error: --description requires --auto-generate-name");
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

      Schema oldSchema;
      Schema newSchema;
      String schemaFileArg = hasPositionalFile ? cmd.getArgs()[0] : null;

      if (hasOld) {
        var sourceReader = new GitSchemaSourceReader();
        oldSchema = sourceReader.readSchema(cmd.getOptionValue("old"));
        newSchema = sourceReader.readSchema(cmd.getOptionValue("new"));
      } else {
        var versions = new GitSchemaReader().readSchemas(Path.of(schemaFileArg));
        oldSchema = versions.getCommittedSchema();
        newSchema = versions.getCurrentSchema();
      }

      var changeSet = new SchemaDiffEngine().diff(oldSchema, newSchema);

      if (autoGenerateName) {
        var outputPath =
            MigrationFileNaming.generatePath(
                schemaFileArg,
                ZonedDateTime.now(ZoneOffset.UTC),
                cmd.getOptionValue("description"));
        writeToFile(changeSet, databaseType, outputPath);
        System.out.println("Wrote migration to " + outputPath);
      } else if (cmd.hasOption("o")) {
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
