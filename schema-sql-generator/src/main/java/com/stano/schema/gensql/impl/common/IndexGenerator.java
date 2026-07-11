package com.stano.schema.gensql.impl.common;

import com.stano.schema.model.Key;
import com.stano.schema.model.KeyType;
import com.stano.schema.model.Table;
import org.apache.commons.text.StringEscapeUtils;

public class IndexGenerator extends BaseGenerator {
  private static final String IX_PREFIX = "ix_";

  protected IndexGenerator(SQLGenerator sqlGenerator) {
    super(sqlGenerator);
  }

  public void outputIndexes() {
    schema.getTables().forEach(this::outputIndexes);
  }

  public void outputIndexes(Table table) {
    if (!table.getIndexes().isEmpty()) {
      for (int keyIndex = 0; keyIndex < table.getIndexes().size(); keyIndex++) {
        Key key = table.getIndexes().get(keyIndex);

        if (key.getType() != KeyType.INDEX) {
          continue;
        }

        String keyName = buildKeyName(IX_PREFIX, table.getName(), String.valueOf(keyIndex + 1));

        outputIndex(table, keyName.toLowerCase(), key);
      }

      sqlWriter.println();
    }
  }

  private void outputIndex(Table table, String keyName, Key key) {
    if (key.getType() == KeyType.INDEX) {
      String indexOptions = getIndexOptions(key);
      String indexColumns =
          String.join(
              ",",
              key.getColumns().stream()
                  .map(it -> StringEscapeUtils.unescapeXml(it.getName()))
                  .toArray(String[]::new));

      if (indexOptions == null) {
        sqlWriter.println(
            String.format(
                "create %sindex %s on %s (%s)%s",
                key.isUnique() ? "unique " : "",
                keyName.toLowerCase(),
                getFullyQualifiedTableName(table),
                indexColumns,
                statementSeparator));
      } else {
        sqlWriter.println(
            String.format(
                "create %sindex %s on %s (%s) %s%s",
                key.isUnique() ? "unique " : "",
                keyName.toLowerCase(),
                getFullyQualifiedTableName(table),
                indexColumns,
                indexOptions,
                statementSeparator));
      }
    }
  }

  protected String getIndexOptions(Key key) {
    return null;
  }
}
