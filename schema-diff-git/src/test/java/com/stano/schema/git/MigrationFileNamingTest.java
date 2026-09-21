package com.stano.schema.git;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MigrationFileNaming")
class MigrationFileNamingTest {
  private static ZonedDateTime sampleTimestamp() {
    return ZonedDateTime.of(2024, 1, 15, 14, 30, 22, 0, ZoneOffset.UTC);
  }

  @Test
  @DisplayName("generates filename without description")
  void generatesFilenameWithoutDescription() {
    assertEquals(
        "V20240115143022.sql", MigrationFileNaming.generateFilename(sampleTimestamp(), null));
    assertEquals(
        "V20240115143022.sql", MigrationFileNaming.generateFilename(sampleTimestamp(), ""));
  }

  @Test
  @DisplayName("generates filename with description")
  void generatesFilenameWithDescription() {
    assertEquals(
        "V20240115143022__add_users_table.sql",
        MigrationFileNaming.generateFilename(sampleTimestamp(), "add users table"));
  }

  @Test
  @DisplayName("derives directory from schema file path")
  void derivesDirectoryFromSchemaFilePath() {
    assertEquals(
        Path.of("migrations/V20240115143022.sql"),
        MigrationFileNaming.generatePath("migrations/schema.xml", sampleTimestamp(), null));

    assertEquals(
        Path.of("V20240115143022__add_users_table.sql"),
        MigrationFileNaming.generatePath("schema.xml", sampleTimestamp(), "add users table"));
  }
}
