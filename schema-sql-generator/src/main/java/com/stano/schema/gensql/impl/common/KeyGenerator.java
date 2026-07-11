package com.stano.schema.gensql.impl.common;

import com.stano.schema.model.Key;
import com.stano.schema.model.KeyType;
import com.stano.schema.model.Table;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class KeyGenerator extends BaseGenerator {
  private static final String PK_PREFIX = "pk_";
  private static final String AK_PREFIX = "ak_";

  protected KeyGenerator(SQLGenerator sqlGenerator) {
    super(sqlGenerator);
  }

  public List<String> getKeyConstraints(Table table) {
    boolean disallowDuplicateUniqueKeys = !disallowDuplicateUniqueKeyConstraints();
    Set<String> keysAlreadyGenerated = new HashSet<>();
    int nextUniqueKeyNo = 1;

    List<String> keyConstraints = new ArrayList<>();

    for (int keyIndex = 0; keyIndex < table.getKeys().size(); keyIndex++) {
      Key key = table.getKeys().get(keyIndex);

      if (disallowDuplicateUniqueKeys && keysAlreadyGenerated.contains(key.getColumnsAsString())) {
        continue;
      }

      keysAlreadyGenerated.add(key.getColumnsAsString());

      String keyName = null;

      if (key.getType() == KeyType.PRIMARY) {
        keyName = buildKeyName(PK_PREFIX, table.getName(), "");
      } else if (key.getType() == KeyType.UNIQUE) {
        keyName = buildKeyName(AK_PREFIX, table.getName(), String.valueOf(nextUniqueKeyNo));
        nextUniqueKeyNo++;
      }

      keyConstraints.add(generateKey(keyName, key));
    }

    return keyConstraints;
  }

  protected boolean disallowDuplicateUniqueKeyConstraints() {
    return false;
  }

  protected String generateKey(String keyName, Key key) {
    if (key.getType() == KeyType.PRIMARY) {
      return generatePrimaryKey(key, keyName);
    }

    if (key.getType() == KeyType.UNIQUE) {
      return generateUniqueKey(key, keyName);
    }

    return null;
  }

  private String generatePrimaryKey(Key key, String keyName) {
    String keyClusterSql = getKeyClusterSql(key);
    String keyCompressionSql = getKeyCompressSql(key);

    if (keyClusterSql != null && keyCompressionSql != null) {
      return String.format(
          "   constraint %s primary key %s (%s) %s",
          keyName.toLowerCase(), keyClusterSql, key.getColumnsAsString(), keyCompressionSql);
    }

    if (keyClusterSql != null) {
      return String.format(
          "   constraint %s primary key %s (%s)",
          keyName.toLowerCase(), keyClusterSql, key.getColumnsAsString());
    }

    if (keyCompressionSql != null) {
      return String.format(
          "   constraint %s primary key (%s) %s",
          keyName.toLowerCase(), key.getColumnsAsString(), keyCompressionSql);
    }

    return String.format(
        "   constraint %s primary key (%s)", keyName.toLowerCase(), key.getColumnsAsString());
  }

  private String generateUniqueKey(Key key, String keyName) {
    String keyClusterSql = getKeyClusterSql(key);
    String keyCompressionSql = getKeyCompressSql(key);

    if (keyClusterSql != null && keyCompressionSql != null) {
      return String.format(
          "   constraint %s unique %s (%s) %s",
          keyName.toLowerCase(), keyClusterSql, key.getColumnsAsString(), keyCompressionSql);
    }

    if (keyClusterSql != null) {
      return String.format(
          "   constraint %s unique %s (%s)",
          keyName.toLowerCase(), keyClusterSql, key.getColumnsAsString());
    }

    if (keyCompressionSql != null) {
      return String.format(
          "   constraint %s unique (%s) %s",
          keyName.toLowerCase(), key.getColumnsAsString(), keyCompressionSql);
    }

    return String.format(
        "   constraint %s unique (%s)", keyName.toLowerCase(), key.getColumnsAsString());
  }

  protected String getKeyClusterSql(Key key) {
    return null;
  }

  protected String getKeyCompressSql(Key key) {
    return null;
  }
}
