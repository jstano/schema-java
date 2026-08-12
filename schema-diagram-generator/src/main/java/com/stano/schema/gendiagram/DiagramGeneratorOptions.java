package com.stano.schema.gendiagram;

import com.stano.schema.model.Schema;
import java.io.PrintWriter;

/**
 * Immutable bundle of configuration passed to a {@link DiagramGenerator}: the schema to diagram,
 * the writer to render output to, and the desired output format.
 */
public class DiagramGeneratorOptions {
  private final Schema schema;
  private final PrintWriter writer;
  private final DiagramFormat format;

  /**
   * Creates a new options bundle.
   *
   * @param schema the schema to generate a diagram for
   * @param writer the writer that the generated diagram is written to
   * @param format the output format to generate
   */
  public DiagramGeneratorOptions(Schema schema, PrintWriter writer, DiagramFormat format) {
    this.schema = schema;
    this.writer = writer;
    this.format = format;
  }

  /**
   * Returns the schema to generate a diagram for.
   *
   * @return the schema
   */
  public Schema getSchema() {
    return schema;
  }

  /**
   * Returns the writer that the generated diagram is written to.
   *
   * @return the output writer
   */
  public PrintWriter getWriter() {
    return writer;
  }

  /**
   * Returns the output format to generate.
   *
   * @return the diagram format
   */
  public DiagramFormat getFormat() {
    return format;
  }
}
