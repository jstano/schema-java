package com.stano.schema.parser.xmlparser;

import com.stano.schema.model.ColumnPair;
import com.stano.schema.model.Relation;
import com.stano.schema.model.RelationType;
import com.stano.schema.model.Schema;
import com.stano.schema.model.Table;
import java.util.ArrayList;
import java.util.List;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class RelationContentHandler extends AbstractContentHandler {
  private final Table table;
  private final TableContentHandler tableContentHandler;

  private String compositeToTableName;
  private RelationType compositeType;
  private boolean compositeDisableUsageChecking;
  private final List<ColumnPair> compositeColumnPairs = new ArrayList<>();

  protected RelationContentHandler(
      Schema schema, Table table, TableContentHandler tableContentHandler) {
    super(null, schema);

    this.table = table;
    this.tableContentHandler = tableContentHandler;
  }

  @Override
  public void startElement(String namespaceURI, String localName, String qName, Attributes atts)
      throws SAXException {
    if (localName.equals("relation")) {
      table
          .getRelations()
          .add(
              new Relation(
                  table.getName(),
                  atts.getValue("src"),
                  atts.getValue("table"),
                  atts.getValue("column"),
                  RelationType.valueOf(atts.getValue("type").toUpperCase()),
                  Boolean.parseBoolean(atts.getValue("disableUsageChecking"))));
    } else if (localName.equals("compositeRelation")) {
      compositeToTableName = atts.getValue("table");
      compositeType = RelationType.valueOf(atts.getValue("type").toUpperCase());
      compositeDisableUsageChecking = Boolean.parseBoolean(atts.getValue("disableUsageChecking"));
      compositeColumnPairs.clear();
    } else if (localName.equals("column")) {
      compositeColumnPairs.add(new ColumnPair(atts.getValue("src"), atts.getValue("name")));
    }
  }

  @Override
  public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
    switch (localName) {
      case "relations" -> tableContentHandler.contentHandler = null;
      case "compositeRelation" ->
          table
              .getRelations()
              .add(
                  Relation.composite(
                      table.getName(),
                      compositeToTableName,
                      compositeColumnPairs,
                      compositeType,
                      compositeDisableUsageChecking));
    }
  }
}
