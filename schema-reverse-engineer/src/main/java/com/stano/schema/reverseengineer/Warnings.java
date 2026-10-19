package com.stano.schema.reverseengineer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;
import org.slf4j.helpers.NOPLoggerFactory;

/**
 * Reports a warning through SLF4J, and to standard error when no SLF4J binding is installed - the
 * command-line tool ships without one, which would otherwise drop every warning silently.
 */
final class Warnings {

  private Warnings() {}

  static void warn(Logger logger, String format, Object... args) {
    if (LoggerFactory.getILoggerFactory() instanceof NOPLoggerFactory) {
      System.err.println("WARNING: " + MessageFormatter.arrayFormat(format, args).getMessage());
    } else {
      logger.warn(format, args);
    }
  }
}
