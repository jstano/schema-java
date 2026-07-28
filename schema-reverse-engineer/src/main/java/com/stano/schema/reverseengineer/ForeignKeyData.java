package com.stano.schema.reverseengineer;

public record ForeignKeyData(
    String pkTableName,
    String pkColumnName,
    String fkTableName,
    String fkColumnName,
    int keySeq,
    String updateRule,
    String deleteRule)
    implements Comparable<ForeignKeyData> {
  @Override
  public int compareTo(ForeignKeyData other) {
    return keySeq - other.keySeq;
  }
}
