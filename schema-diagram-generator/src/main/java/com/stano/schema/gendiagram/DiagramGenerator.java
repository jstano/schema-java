package com.stano.schema.gendiagram;

/**
 * Generates an entity-relationship diagram for a {@link com.stano.schema.model.Schema}.
 *
 * <p>Implementations render the diagram in a specific output format (Mermaid or PlantUML, see
 * {@link DiagramFormat}) using the schema and writer supplied via {@link DiagramGeneratorOptions}.
 * Instances are created by {@link DiagramGeneratorFactory}.
 */
public interface DiagramGenerator {

  /**
   * Generates the diagram, writing its output using the configured {@link DiagramGeneratorOptions}.
   */
  void generate();
}
