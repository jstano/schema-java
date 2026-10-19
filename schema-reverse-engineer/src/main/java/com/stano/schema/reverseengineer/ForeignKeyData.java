package com.stano.schema.reverseengineer;

public record ForeignKeyData(
    String fkName,
    String pkSchemaName,
    String pkTableName,
    String pkColumnName,
    String fkTableName,
    String fkColumnName,
    int keySeq,
    String updateRule,
    String deleteRule)
    implements Comparable<ForeignKeyData> {

  /** Creates a row whose referenced table is in the schema being introspected. */
  public ForeignKeyData(
      String fkName,
      String pkTableName,
      String pkColumnName,
      String fkTableName,
      String fkColumnName,
      int keySeq,
      String updateRule,
      String deleteRule) {
    this(
        fkName,
        null,
        pkTableName,
        pkColumnName,
        fkTableName,
        fkColumnName,
        keySeq,
        updateRule,
        deleteRule);
  }

  @Override
  public int compareTo(ForeignKeyData other) {
    return keySeq - other.keySeq;
  }
}
