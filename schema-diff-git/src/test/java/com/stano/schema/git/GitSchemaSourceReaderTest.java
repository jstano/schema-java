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

@DisplayName("GitSchemaSourceReader")
class GitSchemaSourceReaderTest {
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
  @DisplayName("reads a plain file path")
  void readsPlainFilePath(@TempDir Path tempDir) throws IOException {
    var schemaFile = tempDir.resolve("schema.xml");
    Files.writeString(schemaFile, SCHEMA_V1);

    var schema =
        new GitSchemaSourceReader(tempDir).readSchema(schemaFile.toAbsolutePath().toString());

    assertEquals(2, schema.getTable("User").getColumns().size());
  }

  @Test
  @DisplayName("reads a HEAD:<path> git reference")
  void readsHeadGitReference(@TempDir Path tempDir) throws IOException, GitAPIException {
    var schemaFile = tempDir.resolve("schema.xml");
    Files.writeString(schemaFile, SCHEMA_V1);

    try (var git = Git.init().setDirectory(tempDir.toFile()).call()) {
      git.add().addFilepattern("schema.xml").call();
      git.commit().setMessage("initial schema").setAuthor("test", "test@test.com").call();

      Files.writeString(schemaFile, SCHEMA_V2);

      var reader = new GitSchemaSourceReader(tempDir);
      var oldSchema = reader.readSchema("HEAD:schema.xml");
      var newSchema = reader.readSchema(schemaFile.toAbsolutePath().toString());

      assertEquals(2, oldSchema.getTable("User").getColumns().size());
      assertEquals(3, newSchema.getTable("User").getColumns().size());
    }
  }

  @Test
  @DisplayName("reads an older revision by its commit id")
  void readsOlderRevisionByCommitId(@TempDir Path tempDir) throws IOException, GitAPIException {
    var schemaFile = tempDir.resolve("schema.xml");
    Files.writeString(schemaFile, SCHEMA_V1);

    try (var git = Git.init().setDirectory(tempDir.toFile()).call()) {
      git.add().addFilepattern("schema.xml").call();
      var firstCommit =
          git.commit().setMessage("initial schema").setAuthor("test", "test@test.com").call();

      Files.writeString(schemaFile, SCHEMA_V2);
      git.add().addFilepattern("schema.xml").call();
      git.commit().setMessage("add email column").setAuthor("test", "test@test.com").call();

      var reader = new GitSchemaSourceReader(tempDir);
      var schema = reader.readSchema(firstCommit.getName() + ":schema.xml");

      assertEquals(2, schema.getTable("User").getColumns().size());
    }
  }

  @Test
  @DisplayName("treats a value with an unresolvable revision as a plain (missing) file path")
  void treatsUnresolvableRevisionAsPlainPath(@TempDir Path tempDir) {
    var reader = new GitSchemaSourceReader(tempDir);

    assertThrows(RuntimeException.class, () -> reader.readSchema("not-a-rev:schema.xml"));
  }
}
