package com.stano.schema.diff;

import com.stano.schema.diff.change.AddColumnChange;
import com.stano.schema.diff.change.AddConstraintChange;
import com.stano.schema.diff.change.AddKeyChange;
import com.stano.schema.diff.change.AddRelationChange;
import com.stano.schema.diff.change.AddTableChange;
import com.stano.schema.diff.change.AddViewChange;
import com.stano.schema.diff.change.DropColumnChange;
import com.stano.schema.diff.change.DropConstraintChange;
import com.stano.schema.diff.change.DropKeyChange;
import com.stano.schema.diff.change.DropRelationChange;
import com.stano.schema.diff.change.DropTableChange;
import com.stano.schema.diff.change.DropViewChange;
import com.stano.schema.diff.change.ModifyColumnChange;
import com.stano.schema.diff.change.RenameColumnChange;
import com.stano.schema.diff.change.RenameTableChange;
import com.stano.schema.model.Column;
import com.stano.schema.model.ColumnType;
import com.stano.schema.model.Constraint;
import com.stano.schema.model.Key;
import com.stano.schema.model.KeyColumn;
import com.stano.schema.model.KeyType;
import com.stano.schema.model.Relation;
import com.stano.schema.model.RelationType;
import com.stano.schema.model.View;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

public class ChangeSetParser {

  public ChangeSet parse(URL url) {
    try (InputStream stream = url.openStream()) {
      return parse(stream);
    } catch (ChangeSetParserException e) {
      throw e;
    } catch (Exception e) {
      throw new ChangeSetParserException("Failed to parse changeset from URL: " + url, e);
    }
  }

  public ChangeSet parse(InputStream inputStream) {
    try {
      SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
      Schema xsdSchema = schemaFactory.newSchema(getClass().getResource("/changeset.xsd"));

      SAXParserFactory factory = SAXParserFactory.newInstance();
      factory.setSchema(xsdSchema);
      factory.setNamespaceAware(true);
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
      factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

      SAXParser saxParser = factory.newSAXParser();
      XMLReader reader = saxParser.getXMLReader();
      ChangeSetHandler handler = new ChangeSetHandler();
      reader.setContentHandler(handler);
      reader.setErrorHandler(new DefaultHandler());
      reader.parse(new org.xml.sax.InputSource(inputStream));
      return handler.changeSet;
    } catch (ChangeSetParserException e) {
      throw e;
    } catch (Exception e) {
      throw new ChangeSetParserException("Failed to parse changeset", e);
    }
  }

  private static class ChangeSetHandler extends DefaultHandler {
    private final ChangeSet changeSet = new ChangeSet();

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
        throws SAXException {
      try {
        String elementName = localName != null && !localName.isEmpty() ? localName : qName;
        switch (elementName) {
          case "add-table" -> parseAddTable(attributes);
          case "drop-table" -> parseDropTable(attributes);
          case "rename-table" -> parseRenameTable(attributes);
          case "add-column" -> parseAddColumn(attributes);
          case "drop-column" -> parseDropColumn(attributes);
          case "rename-column" -> parseRenameColumn(attributes);
          case "modify-column" -> parseModifyColumn(attributes);
          case "add-key" -> parseAddKey(attributes);
          case "drop-key" -> parseDropKey(attributes);
          case "add-constraint" -> parseAddConstraint(attributes);
          case "drop-constraint" -> parseDropConstraint(attributes);
          case "add-relation" -> parseAddRelation(attributes);
          case "drop-relation" -> parseDropRelation(attributes);
          case "add-view" -> parseAddView(attributes);
          case "drop-view" -> parseDropView(attributes);
        }
      } catch (Exception e) {
        throw new SAXException("Error parsing element: " + localName + ": " + e.getMessage(), e);
      }
    }

    private void parseAddTable(Attributes atts) {
      String name = atts.getValue("name");
      changeSet.addChange(new AddTableChange(name));
    }

    private void parseDropTable(Attributes atts) {
      String name = atts.getValue("name");
      changeSet.addChange(new DropTableChange(name));
    }

    private void parseRenameTable(Attributes atts) {
      String oldName = atts.getValue("old-name");
      String newName = atts.getValue("new-name");
      changeSet.addChange(new RenameTableChange(oldName, newName));
    }

    private void parseAddColumn(Attributes atts) {
      String tableName = atts.getValue("table");
      String name = atts.getValue("name");
      String type = atts.getValue("type");
      int length = getIntAttribute(atts, "length", 0);
      int scale = getIntAttribute(atts, "scale", 0);
      boolean required = getBooleanAttribute(atts, "required", false);
      String defaultValue = atts.getValue("default");

      ColumnType columnType = ColumnType.valueOf(type.toUpperCase());
      Column column =
          new Column(
              name,
              columnType,
              length,
              scale,
              required,
              null,
              defaultValue,
              null,
              null,
              null,
              null,
              null);
      changeSet.addChange(new AddColumnChange(tableName, column));
    }

    private void parseDropColumn(Attributes atts) {
      String tableName = atts.getValue("table");
      String name = atts.getValue("name");
      changeSet.addChange(new DropColumnChange(tableName, name));
    }

    private void parseRenameColumn(Attributes atts) {
      String tableName = atts.getValue("table");
      String oldName = atts.getValue("old-name");
      String newName = atts.getValue("new-name");
      changeSet.addChange(new RenameColumnChange(tableName, oldName, newName));
    }

    private void parseModifyColumn(Attributes atts) {
      String tableName = atts.getValue("table");
      String name = atts.getValue("name");

      String oldType = atts.getValue("old-type");
      int oldLength = getIntAttribute(atts, "old-length", 0);
      int oldScale = getIntAttribute(atts, "old-scale", 0);
      boolean oldRequired = getBooleanAttribute(atts, "old-required", false);
      String oldDefault = atts.getValue("old-default");

      String newType = atts.getValue("new-type");
      int newLength = getIntAttribute(atts, "new-length", 0);
      int newScale = getIntAttribute(atts, "new-scale", 0);
      boolean newRequired = getBooleanAttribute(atts, "new-required", false);
      String newDefault = atts.getValue("new-default");

      ColumnType oldColumnType = ColumnType.valueOf(oldType.toUpperCase());
      Column oldColumn =
          new Column(
              name,
              oldColumnType,
              oldLength,
              oldScale,
              oldRequired,
              null,
              oldDefault,
              null,
              null,
              null,
              null,
              null);

      ColumnType newColumnType = ColumnType.valueOf(newType.toUpperCase());
      Column newColumn =
          new Column(
              name,
              newColumnType,
              newLength,
              newScale,
              newRequired,
              null,
              newDefault,
              null,
              null,
              null,
              null,
              null);

      changeSet.addChange(new ModifyColumnChange(tableName, oldColumn, newColumn));
    }

    private void parseAddKey(Attributes atts) {
      String tableName = atts.getValue("table");
      String type = atts.getValue("type");
      String columns = atts.getValue("columns");

      KeyType keyType = KeyType.valueOf(type.toUpperCase());
      List<KeyColumn> keyColumns = parseKeyColumns(columns);
      Key key = new Key(keyType, keyColumns);
      changeSet.addChange(new AddKeyChange(tableName, key));
    }

    private void parseDropKey(Attributes atts) {
      String tableName = atts.getValue("table");
      String type = atts.getValue("type");
      String columns = atts.getValue("columns");

      KeyType keyType = KeyType.valueOf(type.toUpperCase());
      List<KeyColumn> keyColumns = parseKeyColumns(columns);
      Key key = new Key(keyType, keyColumns);
      changeSet.addChange(new DropKeyChange(tableName, key));
    }

    private void parseAddConstraint(Attributes atts) {
      String tableName = atts.getValue("table");
      String name = atts.getValue("name");
      String sql = atts.getValue("sql");
      Constraint constraint = new Constraint(name, sql, null);
      changeSet.addChange(new AddConstraintChange(tableName, constraint));
    }

    private void parseDropConstraint(Attributes atts) {
      String tableName = atts.getValue("table");
      String name = atts.getValue("name");
      changeSet.addChange(new DropConstraintChange(tableName, name));
    }

    private void parseAddRelation(Attributes atts) {
      String fromTableName = atts.getValue("from-table");
      String fromColumnName = atts.getValue("from-column");
      String toTableName = atts.getValue("to-table");
      String toColumnName = atts.getValue("to-column");
      String type = atts.getValue("type");

      RelationType relationType = RelationType.valueOf(type.toUpperCase());
      Relation relation =
          new Relation(
              fromTableName, fromColumnName, toTableName, toColumnName, relationType, false);
      changeSet.addChange(new AddRelationChange(relation));
    }

    private void parseDropRelation(Attributes atts) {
      String fromTableName = atts.getValue("from-table");
      String fromColumnName = atts.getValue("from-column");
      String toTableName = atts.getValue("to-table");
      String toColumnName = atts.getValue("to-column");

      // <drop-relation> intentionally omits the type attribute; type is not needed to drop a FK
      // (the FK name is derived from from-table + from-column). CASCADE is used as a placeholder.
      Relation relation =
          new Relation(
              fromTableName,
              fromColumnName,
              toTableName,
              toColumnName,
              RelationType.CASCADE,
              false);
      changeSet.addChange(new DropRelationChange(relation));
    }

    private void parseAddView(Attributes atts) {
      String name = atts.getValue("name");
      String sql = atts.getValue("sql");
      View view = new View(null, name, sql, null);
      changeSet.addChange(new AddViewChange(view));
    }

    private void parseDropView(Attributes atts) {
      String name = atts.getValue("name");
      changeSet.addChange(new DropViewChange(name));
    }

    private List<KeyColumn> parseKeyColumns(String columnsStr) {
      List<KeyColumn> columns = new ArrayList<>();
      if (columnsStr != null && !columnsStr.isEmpty()) {
        String[] parts = columnsStr.split(",");
        for (String part : parts) {
          columns.add(new KeyColumn(part.trim()));
        }
      }
      return columns;
    }

    private int getIntAttribute(Attributes atts, String name, int defaultValue) {
      String value = atts.getValue(name);
      if (value == null || value.isEmpty()) {
        return defaultValue;
      }
      try {
        return Integer.parseInt(value);
      } catch (NumberFormatException e) {
        return defaultValue;
      }
    }

    private boolean getBooleanAttribute(Attributes atts, String name, boolean defaultValue) {
      String value = atts.getValue(name);
      if (value == null || value.isEmpty()) {
        return defaultValue;
      }
      return Boolean.parseBoolean(value);
    }
  }
}
