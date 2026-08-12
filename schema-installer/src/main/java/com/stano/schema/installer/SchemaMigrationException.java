package com.stano.schema.installer;

/**
 * Thrown by {@link SchemaInstaller} when installing, migrating, or inspecting a schema fails,
 * typically because of an I/O error while reading or generating SQL, or a {@link
 * java.sql.SQLException} raised while communicating with the database.
 */
public class SchemaMigrationException extends RuntimeException {
  /**
   * Creates a new exception wrapping the given cause.
   *
   * @param x the underlying cause of the failure
   */
  public SchemaMigrationException(Throwable x) {
    super(x);
  }
}
