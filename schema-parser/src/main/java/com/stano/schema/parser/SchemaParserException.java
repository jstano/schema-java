package com.stano.schema.parser;

/**
 * Unchecked exception thrown by {@link SchemaParser} when an XML schema resource cannot be read.
 *
 * <p>Specifically, it is thrown by {@link SchemaParser#parseSchema(java.net.URL)} when the stream
 * for the given schema URL cannot be opened, wrapping the underlying {@link java.io.IOException} as
 * the cause.
 */
public class SchemaParserException extends RuntimeException {
  /**
   * Creates a new exception wrapping the given cause.
   *
   * @param cause the underlying exception that caused the schema resource to be unreadable
   */
  public SchemaParserException(Throwable cause) {
    super(cause);
  }
}
