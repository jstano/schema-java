package com.stano.schema.parser;

import com.stano.schema.model.Schema;
import com.stano.schema.parser.xmlparser.SchemaContentHandler;
import com.stano.schema.parser.xmlparser.XMLSchemaParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * Main entry point for parsing an XML schema definition file into a {@link Schema} object.
 *
 * <p>This is the class consumers use to load a vendor-neutral XML schema document (validated
 * against the bundled {@code schema.xsd}) into the in-memory {@link Schema} model, for example as
 * shown in the README Quick Start:
 *
 * <pre>{@code
 * Schema schema = new SchemaParser().parseSchema(
 *     MyApp.class.getResource("/db/my-schema.xml")
 * );
 * }</pre>
 *
 * <p>Internally, parsing is delegated to {@link com.stano.schema.parser.xmlparser.XMLSchemaParser},
 * which drives a SAX parse of the XML document, feeding events to a {@link
 * com.stano.schema.parser.xmlparser.SchemaContentHandler} that populates the {@link Schema}.
 */
public class SchemaParser {
  /**
   * Parses the XML schema document located at the given URL into a {@link Schema}.
   *
   * <p>Opens a stream on {@code schemaURL} and delegates to {@link #parseSchema(URL, InputStream)}.
   *
   * @param schemaURL the URL of the XML schema document to read and parse
   * @return the fully parsed and post-processed {@link Schema}
   * @throws SchemaParserException if the stream for {@code schemaURL} cannot be opened
   * @throws IllegalStateException if the XML content cannot be read, parsed, or validated
   */
  public Schema parseSchema(URL schemaURL) {
    try {
      return parseSchema(schemaURL, schemaURL.openStream());
    } catch (IOException x) {
      throw new SchemaParserException(x);
    }
  }

  /**
   * Parses XML schema content from the given input stream into a {@link Schema}.
   *
   * <p>The stream is wrapped in a {@link BufferedReader} and parsed via {@link
   * com.stano.schema.parser.xmlparser.XMLSchemaParser}, whose SAX events populate a {@link
   * com.stano.schema.parser.xmlparser.SchemaContentHandler} bound to the returned {@link Schema}.
   * After parsing, the schema's tables are sorted by name and its reverse relations are built
   * before the schema is returned.
   *
   * @param schemaURL the URL associated with the schema being parsed, used to construct the {@link
   *     Schema} instance (e.g. as its origin/base URL)
   * @param inputStream the stream from which the raw XML content is read
   * @return the fully parsed and post-processed {@link Schema}
   * @throws IllegalStateException if the XML content cannot be read, parsed, or validated; the
   *     underlying cause (e.g. an I/O error or XML/SAX exception) is wrapped
   */
  public Schema parseSchema(URL schemaURL, InputStream inputStream) {
    try {
      Schema schema = new Schema(schemaURL);

      try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
        XMLSchemaParser parser = new XMLSchemaParser();

        parser.parse(reader, new SchemaContentHandler(schema, schemaURL));
      }

      schema.sortTablesByName();
      schema.buildReverseRelations();

      return schema;
    } catch (Exception x) {
      throw new IllegalStateException(x);
    }
  }
}
