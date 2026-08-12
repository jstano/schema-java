package com.stano.schema.installer.liquibase;

/**
 * Thrown when a Liquibase-based operation performed by {@link LiquibaseSchemaInstaller} (or its
 * supporting classes) fails, wrapping the underlying cause.
 */
public class LiquibaseRuntimeException extends RuntimeException {
  /**
   * Creates a new exception wrapping the given cause.
   *
   * @param x the underlying cause of the failure
   */
  public LiquibaseRuntimeException(Throwable x) {
    super(x);
  }
}
