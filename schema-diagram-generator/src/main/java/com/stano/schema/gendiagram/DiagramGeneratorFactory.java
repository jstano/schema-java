package com.stano.schema.gendiagram;

import com.stano.schema.gendiagram.impl.MermaidERDiagramGenerator;
import com.stano.schema.gendiagram.impl.PlantUMLERDiagramGenerator;

/**
 * Factory that creates the concrete {@link DiagramGenerator} implementation matching the {@link
 * DiagramFormat} requested in a {@link DiagramGeneratorOptions} instance.
 */
public class DiagramGeneratorFactory {

  /**
   * Creates a {@link DiagramGenerator} for the format specified in {@code options}.
   *
   * @param options the schema, output writer, and target format to generate the diagram for
   * @return a {@link com.stano.schema.gendiagram.impl.MermaidERDiagramGenerator} for {@link
   *     DiagramFormat#MERMAID}, or a {@link
   *     com.stano.schema.gendiagram.impl.PlantUMLERDiagramGenerator} for {@link
   *     DiagramFormat#PLANTUML}
   */
  public DiagramGenerator createDiagramGenerator(DiagramGeneratorOptions options) {
    return switch (options.getFormat()) {
      case MERMAID -> new MermaidERDiagramGenerator(options);
      case PLANTUML -> new PlantUMLERDiagramGenerator(options);
    };
  }
}
