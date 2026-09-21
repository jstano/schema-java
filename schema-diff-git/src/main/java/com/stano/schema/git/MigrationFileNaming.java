package com.stano.schema.git;

import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Builds a Flyway-style {@code V{version}__{description}.sql} filename from a timestamp and an
 * optional description. The description is omitted entirely (rather than left as a trailing empty
 * {@code __}) when not given.
 */
public class MigrationFileNaming {
  private static final DateTimeFormatter VERSION_FORMAT =
      DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

  private MigrationFileNaming() {}

  /**
   * Builds the migration filename for the given timestamp and optional description.
   *
   * @param timestamp the timestamp to encode as the migration version
   * @param description the migration description, or {@code null}/empty to omit it
   * @return {@code V{version}__{description}.sql}, or {@code V{version}.sql} when {@code
   *     description} is {@code null} or empty
   */
  public static String generateFilename(ZonedDateTime timestamp, String description) {
    String version = VERSION_FORMAT.format(timestamp);
    if (description != null && !description.isEmpty()) {
      return "V" + version + "__" + description.replace(' ', '_') + ".sql";
    }
    return "V" + version + ".sql";
  }

  /**
   * Builds the full output path for an auto-generated migration: the generated filename, placed
   * alongside the schema file it was diffed from.
   *
   * @param schemaFile the schema file the migration was diffed from
   * @param timestamp the timestamp to encode as the migration version
   * @param description the migration description, or {@code null}/empty to omit it
   * @return the schema file's parent directory joined with the generated filename, or the bare
   *     filename when the schema file has no parent directory
   */
  public static Path generatePath(String schemaFile, ZonedDateTime timestamp, String description) {
    Path dir = Path.of(schemaFile).getParent();
    String filename = generateFilename(timestamp, description);
    return dir != null ? dir.resolve(filename) : Path.of(filename);
  }
}
