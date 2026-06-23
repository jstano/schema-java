package com.stano.schema.diff;

public class ChangeSetParserException extends RuntimeException {
  public ChangeSetParserException(String message) {
    super(message);
  }

  public ChangeSetParserException(String message, Throwable cause) {
    super(message, cause);
  }
}
