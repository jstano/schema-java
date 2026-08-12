package com.stano.schema.migrations;

/**
 * Unchecked exception used throughout this package to wrap checked {@link java.sql.SQLException}
 * failures encountered while performing migration helper operations (existence checks, DDL
 * execution, etc.).
 *
 * <p>Wrapping the checked {@code SQLException} in an unchecked exception allows the helper classes
 * in this package to be used from lambda-style call sites (such as {@link StatementAction#execute})
 * and from hand-written Flyway or Liquibase migration methods without forcing every call site to
 * declare or catch {@code SQLException}.
 */
public class MigrationException extends RuntimeException {
  /**
   * Creates a new migration exception wrapping the given cause.
   *
   * @param x the underlying cause of the failure, typically a {@link java.sql.SQLException}
   */
  public MigrationException(Throwable x) {
    super(x);
  }
}
