/**
 * Generates entity-relationship diagrams from a {@link com.stano.schema.model.Schema}.
 *
 * <p>{@link com.stano.schema.gendiagram.GenDiagram} is the library and command-line entry point.
 * {@link com.stano.schema.gendiagram.DiagramGeneratorFactory} creates the format-specific {@link
 * com.stano.schema.gendiagram.DiagramGenerator} implementation (Mermaid or PlantUML, per {@link
 * com.stano.schema.gendiagram.DiagramFormat}) configured via {@link
 * com.stano.schema.gendiagram.DiagramGeneratorOptions}. Format-specific implementations live in the
 * {@code impl} sub-package.
 */
package com.stano.schema.gendiagram;
