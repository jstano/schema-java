package com.stano.schema.parser.xmlparser;

import java.io.BufferedReader;
import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * SAX-driving XML parser used internally by {@link com.stano.schema.parser.SchemaParser} to read
 * and validate schema XML documents.
 *
 * <p>Configures a JAXP {@link SAXParser} that validates incoming documents against the bundled
 * {@code /schema.xsd} classpath resource, is namespace-aware, and has secure processing features
 * enabled (disallowing {@code DOCTYPE} declarations and external general/parameter entities).
 * Parsing is driven via SAX, with content events dispatched to a caller-supplied {@link
 * SchemaContentHandler}.
 */
public class XMLSchemaParser {
  /**
   * Parses the XML content from the given reader, dispatching SAX events to the given handler.
   *
   * @param reader the source of the raw XML content to parse
   * @param schemaContentHandler the SAX content handler that receives parse events and builds up a
   *     {@link com.stano.schema.model.Schema} as content is parsed
   * @throws RuntimeException if the XML reader cannot be created, or if parsing or schema
   *     validation fails; the underlying cause is wrapped
   */
  public void parse(BufferedReader reader, SchemaContentHandler schemaContentHandler) {
    try {
      XMLReader parser = createXMLReader();

      parser.setContentHandler(schemaContentHandler);
      parser.parse(new InputSource(reader));
    } catch (Exception x) {
      throw new RuntimeException(x);
    }
  }

  private XMLReader createXMLReader() {
    try {
      // Load XSD schema from classpath
      SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
      Schema schema = schemaFactory.newSchema(getClass().getResource("/schema.xsd"));

      // Allocate and configure JAXP SAX parser factory
      SAXParserFactory factory = SAXParserFactory.newInstance();
      factory.setSchema(schema);
      factory.setNamespaceAware(true);
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
      factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

      // Allocate parser
      SAXParser parser = factory.newSAXParser();

      // Return configured SAX XMLReader
      XMLReader reader = parser.getXMLReader();
      reader.setErrorHandler(new DefaultHandler());

      return reader;
    } catch (Exception x) {
      throw new RuntimeException(x);
    }
  }
}
