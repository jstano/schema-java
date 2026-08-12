package com.stano.schema.installer.flyway;

/**
 * Thrown when a Flyway-based operation performed by {@link FlywaySchemaInstaller} (or its
 * supporting classes) fails, wrapping the underlying cause.
 */
public class FlywayRuntimeException extends RuntimeException {
  /**
   * Creates a new exception wrapping the given cause.
   *
   * @param cause the underlying cause of the failure
   */
  public FlywayRuntimeException(Throwable cause) {
    super(cause);
  }
}
