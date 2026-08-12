package com.stano.schema.gendiagram;

import com.stano.schema.model.Schema;
import com.stano.schema.parser.SchemaParser;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;

/**
 * Library and command-line entry point for generating an ER diagram from a {@link Schema}.
 *
 * <p>As a library, {@link #generateDiagram(Schema, DiagramFormat, PrintWriter)} renders a diagram
 * for an already-loaded schema. As a CLI ({@link #main(String[])}), it parses an XML schema file
 * with {@link SchemaParser} and writes the diagram to a sibling file whose extension matches the
 * requested format ({@code .mmd} for Mermaid, {@code .puml} for PlantUML).
 */
public class GenDiagram {
  private DiagramGeneratorFactory diagramGeneratorFactory = new DiagramGeneratorFactory();

  /**
   * Generates a diagram for the given schema in the given format, writing the output to {@code
   * writer} and closing it when generation completes (successfully or not).
   *
   * @param schema the schema to generate a diagram for
   * @param format the output format to generate
   * @param writer the writer that the generated diagram is written to; closed by this method
   */
  public void generateDiagram(Schema schema, DiagramFormat format, PrintWriter writer) {
    try {
      DiagramGenerator generator =
          diagramGeneratorFactory.createDiagramGenerator(
              new DiagramGeneratorOptions(schema, writer, format));

      generator.generate();
    } finally {
      writer.close();
    }
  }

  private static File createOutputFile(String schemaFilename, DiagramFormat format) {
    String baseFilename = schemaFilename.substring(0, schemaFilename.lastIndexOf('.'));
    String extension = format == DiagramFormat.MERMAID ? "mmd" : "puml";
    return new File(String.format("%s.%s", baseFilename, extension));
  }

  private static String getSchemaFileName(String schemaFileName) {
    if (schemaFileName.matches("^[a-zA-Z]:.*$")) {
      return "/" + schemaFileName;
    }
    return schemaFileName;
  }

  /**
   * Command-line entry point: parses an XML schema file and writes a generated diagram next to it.
   *
   * <p>Usage: {@code GenDiagram <format> <schema-filename>}, where {@code <format>} is one of
   * {@code MERMAID} or {@code PLANTUML} (case-insensitive). The schema is parsed from the given
   * file with {@link SchemaParser}, and the diagram is written to a file with the same base name as
   * the schema file, using the {@code .mmd} extension for Mermaid or {@code .puml} for PlantUML. If
   * fewer than two arguments are supplied, a usage message is printed and the process exits with
   * status 1. Any other failure has its stack trace printed to standard error.
   *
   * @param args the command-line arguments: the diagram format followed by the schema file path
   */
  public static void main(String[] args) {
    try {
      if (args.length < 2) {
        System.out.println("USAGE: GenDiagram <format> <schema-filename>");
        System.out.println("   where <format> is one of: MERMAID, PLANTUML");
        System.exit(1);
      }

      DiagramFormat format = DiagramFormat.valueOf(args[0].toUpperCase());
      String schemaFilename = getSchemaFileName(args[1]);
      URL schemaURL = new URI("file://" + schemaFilename).toURL();
      Schema schema = new SchemaParser().parseSchema(schemaURL);

      GenDiagram genDiagram = new GenDiagram();
      genDiagram.generateDiagram(
          schema,
          format,
          new PrintWriter(new FileWriter(createOutputFile(schemaFilename, format))));
    } catch (Throwable x) {
      x.printStackTrace();
    }
  }
}
