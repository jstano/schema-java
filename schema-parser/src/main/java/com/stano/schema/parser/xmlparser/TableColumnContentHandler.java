package com.stano.schema.parser.xmlparser;

import com.stano.schema.model.Column;
import com.stano.schema.model.ColumnType;
import com.stano.schema.model.Schema;
import com.stano.schema.model.Table;
import org.apache.commons.lang3.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class TableColumnContentHandler extends AbstractContentHandler {
  private final Table table;
  private final TableContentHandler tableContentHandler;

  private String name;
  private ColumnType type;
  private String length;
  private String scale;
  private boolean required;
  private String defaultConstraint;
  private String checkConstraint;
  private String generated;
  private String enumType;
  private ColumnType elementType;
  private String minValue;
  private String maxValue;

  protected TableColumnContentHandler(
      Schema schema, Table table, TableContentHandler tableContentHandler) {
    super(null, schema);

    this.table = table;
    this.tableContentHandler = tableContentHandler;
  }

  @Override
  public void startElement(String namespaceURI, String localName, String qName, Attributes atts)
      throws SAXException {
    if (localName.equals("column")) {
      name = atts.getValue("name");
      type = ColumnType.getColumnType(atts.getValue("type"));
      length = atts.getValue("length");
      scale = atts.getValue("scale");
      required = Boolean.parseBoolean(atts.getValue("required"));
      defaultConstraint = atts.getValue("default");
      generated = atts.getValue("generated");
      enumType = atts.getValue("enumType");
      elementType =
          atts.getValue("elementType") != null
              ? ColumnType.getColumnType(atts.getValue("elementType"))
              : null;
      minValue = atts.getValue("minValue");
      maxValue = atts.getValue("maxValue");
      checkConstraint = null;
    } else if (localName.equals("check")) {
      initContentStorage();
    }
  }

  @Override
  public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
    switch (localName) {
      case "columns" -> tableContentHandler.contentHandler = null;
      case "column" ->
          table
              .getColumns()
              .add(
                  new Column(
                      name,
                      type,
                      length == null ? 0 : Integer.parseInt(length),
                      scale == null ? 0 : Integer.parseInt(scale),
                      required,
                      checkConstraint,
                      defaultConstraint,
                      generated,
                      minValue,
                      maxValue,
                      enumType,
                      elementType));
      case "check" -> {
        String content = getContent();

        if (StringUtils.isNotBlank(content)) {
          checkConstraint = "check(" + content + ")";
        }
      }
    }
  }
}
