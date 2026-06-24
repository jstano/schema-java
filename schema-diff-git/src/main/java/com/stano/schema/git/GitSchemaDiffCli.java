package com.stano.schema.git;

import com.stano.schema.diff.ChangeSet;
import com.stano.schema.diff.ChangeSetWriter;
import com.stano.schema.diff.SchemaDiffEngine;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class GitSchemaDiffCli {
  private static final Options OPTIONS =
      new Options()
          .addOption(
              Option.builder("o")
                  .longOpt("output")
                  .hasArg()
                  .argName("file")
                  .desc("write changeset XML to file (default: stdout)")
                  .build())
          .addOption(Option.builder("h").longOpt("help").desc("print this help").build());

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

      var schemaFile = Path.of(cmd.getArgs()[0]);
      var versions = new GitSchemaReader().readSchemas(schemaFile);
      var changeSet =
          new SchemaDiffEngine().diff(versions.getCommittedSchema(), versions.getCurrentSchema());

      if (cmd.hasOption("o")) {
        writeToFile(changeSet, Path.of(cmd.getOptionValue("o")));
      } else {
        writeToStdout(changeSet);
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

  private static void writeToStdout(ChangeSet changeSet) {
    try (var writer = new PrintWriter(System.out)) {
      new ChangeSetWriter().write(changeSet, writer);
    }
  }

  private static void writeToFile(ChangeSet changeSet, Path outputPath) throws IOException {
    try (var writer = new PrintWriter(outputPath.toFile())) {
      new ChangeSetWriter().write(changeSet, writer);
    }
  }

  private static void printHelp() {
    var formatter = new HelpFormatter();
    formatter.printHelp("schema-diff-git <schema-file> [options]", OPTIONS);
  }
}
