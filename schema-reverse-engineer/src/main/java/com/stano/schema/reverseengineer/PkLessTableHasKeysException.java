package com.stano.schema.reverseengineer;

/**
 * Thrown when a table has unique or index keys but no primary key, which the XML schema format
 * cannot represent ({@code <keys>} requires a {@code <primary>} element). Writing such a table
 * anyway would silently produce XML that fails the XSD, so {@link SchemaWriter} raises this
 * instead.
 */
public class PkLessTableHasKeysException extends RuntimeException {

  /**
   * Creates the exception for the given table.
   *
   * @param tableName the name of the offending table
   */
  public PkLessTableHasKeysException(String tableName) {
    super(
        "Table '"
            + tableName
            + "' has unique or index keys but no primary key; the schema XML format requires a"
            + " <primary> key whenever <keys> is present");
  }
}
