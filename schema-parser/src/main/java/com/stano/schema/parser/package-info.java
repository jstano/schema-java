/**
 * Parses vendor-neutral XML schema definition files into the {@code com.stano.schema.model} object
 * model.
 *
 * <p>The public entry point is {@link com.stano.schema.parser.SchemaParser}, which reads an XML
 * schema document (validated against the bundled {@code schema.xsd}) from a {@link java.net.URL} or
 * {@link java.io.InputStream} and produces a fully populated {@link com.stano.schema.model.Schema}
 * instance, ready for SQL generation, live installation, diagram generation, or other downstream
 * processing.
 *
 * <p>Parsing failures are surfaced as {@link com.stano.schema.parser.SchemaParserException} (when
 * the schema resource itself cannot be read) or {@link java.lang.IllegalStateException} (when the
 * XML content cannot be parsed or fails schema validation). The actual SAX-based parsing mechanics
 * live in the {@link com.stano.schema.parser.xmlparser} subpackage.
 */
package com.stano.schema.parser;
