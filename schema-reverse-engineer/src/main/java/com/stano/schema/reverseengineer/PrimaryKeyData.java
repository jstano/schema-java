package com.stano.schema.reverseengineer;

public record PrimaryKeyData(
    String tableName, String columnName, String expression, int keySequence)
    implements Comparable<PrimaryKeyData> {
  @Override
  public int compareTo(PrimaryKeyData other) {
    return keySequence - other.keySequence;
  }
}
