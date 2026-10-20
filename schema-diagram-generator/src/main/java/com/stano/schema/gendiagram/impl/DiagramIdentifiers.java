package com.stano.schema.gendiagram.impl;

/** Turns schema names into identifiers the Mermaid and PlantUML grammars accept. */
final class DiagramIdentifiers {
  private DiagramIdentifiers() {}

  /** Replaces every character outside {@code [A-Za-z0-9_]} with an underscore. */
  static String of(String name) {
    StringBuilder id = new StringBuilder(name.length());
    name.codePoints()
        .forEach(
            cp ->
                id.append(
                    cp < 128 && (Character.isLetterOrDigit(cp) || cp == '_') ? (char) cp : '_'));
    return id.toString();
  }
}
