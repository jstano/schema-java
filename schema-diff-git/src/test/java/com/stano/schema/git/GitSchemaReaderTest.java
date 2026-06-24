package com.stano.schema.git;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("GitSchemaReader")
class GitSchemaReaderTest {
  private static final String SCHEMA_V1 =
      """
      <?xml version="1.0" encoding="UTF-8"?>
      <database xmlns="http://stano.com/database"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="http://stano.com/database http://stano.com/database">
        <table name="User">
          <columns>
            <column name="ID" type="longsequence" required="true"/>
            <column name="Name" type="varchar" length="100" required="true"/>
          </columns>
          <keys>
            <key type="primary">
              <key-column name="ID"/>
            </key>
          </keys>
        </table>
      </database>
      """;

  private static final String SCHEMA_V2 =
      """
      <?xml version="1.0" encoding="UTF-8"?>
      <database xmlns="http://stano.com/database"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="http://stano.com/database http://stano.com/database">
        <table name="User">
          <columns>
            <column name="ID" type="longsequence" required="true"/>
            <column name="Name" type="varchar" length="100" required="true"/>
            <column name="Email" type="varchar" length="255"/>
          </columns>
          <keys>
            <key type="primary">
              <key-column name="ID"/>
            </key>
          </keys>
        </table>
      </database>
      """;

  @Test
  @DisplayName("reads committed schema from HEAD and current schema from working tree")
  void readsCommittedAndCurrentSchemas(@TempDir Path tempDir) throws IOException, GitAPIException {
    var schemaFile = tempDir.resolve("schema.xml");
    Files.writeString(schemaFile, SCHEMA_V1);

    try (var git = Git.init().setDirectory(tempDir.toFile()).call()) {
      git.add().addFilepattern("schema.xml").call();
      git.commit().setMessage("initial schema").setAuthor("test", "test@test.com").call();

      Files.writeString(schemaFile, SCHEMA_V2);

      SchemaVersions versions = new GitSchemaReader().readSchemas(schemaFile);

      assertEquals(
          2,
          versions.getCommittedSchema().getTable("User").getColumns().size(),
          "committed schema should have 2 columns");
      assertEquals(
          3,
          versions.getCurrentSchema().getTable("User").getColumns().size(),
          "current schema should have 3 columns");
    }
  }

  @Test
  @DisplayName("throws when schema file has not been committed")
  void throwsWhenFileNotCommitted(@TempDir Path tempDir) throws IOException, GitAPIException {
    var schemaFile = tempDir.resolve("schema.xml");
    Files.writeString(schemaFile, SCHEMA_V1);

    try (var git = Git.init().setDirectory(tempDir.toFile()).call()) {
      git.add().addFilepattern("schema.xml").call();
      git.commit().setMessage("empty").setAuthor("test", "test@test.com").call();

      var uncommitted = tempDir.resolve("new-schema.xml");
      Files.writeString(uncommitted, SCHEMA_V1);

      assertThrows(
          IllegalStateException.class,
          () -> new GitSchemaReader().readSchemas(uncommitted),
          "should throw when file is not in HEAD tree");
    }
  }
}
