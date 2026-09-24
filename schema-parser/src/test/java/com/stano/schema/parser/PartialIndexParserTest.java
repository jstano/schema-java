package com.stano.schema.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stano.schema.model.Key;
import com.stano.schema.model.KeyType;
import com.stano.schema.model.Schema;
import com.stano.schema.model.Table;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Partial/filtered unique index parsing")
class PartialIndexParserTest {

  private Schema parse(String tableXml) throws Exception {
    String xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<database xmlns=\"http://stano.com/database\">\n"
            + tableXml
            + "\n</database>\n";

    URL url = URI.create("file:///test-schema.xml").toURL();
    InputStream inputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    return new SchemaParser().parseSchema(url, inputStream);
  }

  @Test
  @DisplayName("<primary where=...> throws")
  void primaryWithWhereThrows() {
    String tableXml =
        "<table name=\"T\">\n"
            + "  <columns><column name=\"ID\" type=\"sequence\" required=\"true\"/></columns>\n"
            + "  <keys>\n"
            + "    <primary where=\"ID is not null\"><column name=\"ID\"/></primary>\n"
            + "  </keys>\n"
            + "</table>";

    assertThrows(IllegalStateException.class, () -> parse(tableXml));
  }

  @Test
  @DisplayName("clustered <unique where=...> throws")
  void clusteredUniqueWithWhereThrows() {
    String tableXml =
        "<table name=\"T\">\n"
            + "  <columns>\n"
            + "    <column name=\"ID\" type=\"sequence\" required=\"true\"/>\n"
            + "    <column name=\"ParentId\" type=\"int\"/>\n"
            + "  </columns>\n"
            + "  <keys>\n"
            + "    <primary><column name=\"ID\"/></primary>\n"
            + "    <unique cluster=\"true\" where=\"ParentId is not null\">\n"
            + "      <column name=\"ParentId\"/>\n"
            + "    </unique>\n"
            + "  </keys>\n"
            + "</table>";

    assertThrows(IllegalStateException.class, () -> parse(tableXml));
  }

  @Test
  @DisplayName("<unique where=...> promotes to a filtered unique index")
  void uniqueWithWherePromotesToFilteredIndex() throws Exception {
    String tableXml =
        "<table name=\"T\">\n"
            + "  <columns>\n"
            + "    <column name=\"ID\" type=\"sequence\" required=\"true\"/>\n"
            + "    <column name=\"ParentId\" type=\"int\"/>\n"
            + "  </columns>\n"
            + "  <keys>\n"
            + "    <primary><column name=\"ID\"/></primary>\n"
            + "    <unique where=\"ParentId is not null\">\n"
            + "      <column name=\"ParentId\"/>\n"
            + "    </unique>\n"
            + "  </keys>\n"
            + "</table>";

    Schema schema = parse(tableXml);
    Table table = schema.getTable("T");

    assertEquals(0, (int) table.getKeys().stream().filter(k -> k.getType() == KeyType.UNIQUE).count());
    assertEquals(1, table.getIndexes().size());

    Key index = table.getIndexes().get(0);
    assertEquals(KeyType.INDEX, index.getType());
    assertTrue(index.isUnique());
    assertEquals("ParentId is not null", index.getFilter());
  }

  @Test
  @DisplayName("<index where=...> parses its predicate")
  void indexWithWhereParsesPredicate() throws Exception {
    String tableXml =
        "<table name=\"T\">\n"
            + "  <columns>\n"
            + "    <column name=\"ID\" type=\"sequence\" required=\"true\"/>\n"
            + "    <column name=\"ParentId\" type=\"int\"/>\n"
            + "  </columns>\n"
            + "  <keys>\n"
            + "    <primary><column name=\"ID\"/></primary>\n"
            + "    <index where=\"ParentId is not null\">\n"
            + "      <column name=\"ParentId\"/>\n"
            + "    </index>\n"
            + "  </keys>\n"
            + "</table>";

    Schema schema = parse(tableXml);
    Table table = schema.getTable("T");

    assertEquals(1, table.getIndexes().size());
    Key index = table.getIndexes().get(0);
    assertEquals("ParentId is not null", index.getFilter());
    assertTrue(!index.isUnique());
  }

  @Test
  @DisplayName("<index> without where has a null filter")
  void indexWithoutWhereHasNullFilter() throws Exception {
    String tableXml =
        "<table name=\"T\">\n"
            + "  <columns>\n"
            + "    <column name=\"ID\" type=\"sequence\" required=\"true\"/>\n"
            + "    <column name=\"ParentId\" type=\"int\"/>\n"
            + "  </columns>\n"
            + "  <keys>\n"
            + "    <primary><column name=\"ID\"/></primary>\n"
            + "    <index>\n"
            + "      <column name=\"ParentId\"/>\n"
            + "    </index>\n"
            + "  </keys>\n"
            + "</table>";

    Schema schema = parse(tableXml);
    Table table = schema.getTable("T");

    assertNull(table.getIndexes().get(0).getFilter());
  }
}
