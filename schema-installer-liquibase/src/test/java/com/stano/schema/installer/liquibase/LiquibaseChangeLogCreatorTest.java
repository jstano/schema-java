package com.stano.schema.installer.liquibase;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stano.schema.model.DatabaseType;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("LiquibaseChangeLogCreator")
class LiquibaseChangeLogCreatorTest {

  @TempDir Path tempDir;

  private LiquibaseChangeLogCreator creator;
  private File sqlFile;

  @BeforeEach
  void setUp() throws Exception {
    creator = new LiquibaseChangeLogCreator();
    sqlFile = tempDir.resolve("V1__install.sql").toFile();
    sqlFile.createNewFile();
  }

  @Test
  @DisplayName(
      "createTempChangeLogFile produces valid Liquibase XML with fixed install changeSet id")
  void createTempChangeLogFileProducesValidXmlWithCorrectChangeSetId() throws Exception {
    File changeLogFile = creator.createTempChangeLogFile(DatabaseType.H2, sqlFile, ";");

    String content = Files.readString(changeLogFile.toPath());
    assertTrue(content.contains("<?xml version=\"1.0\""), "should start with XML declaration");
    assertTrue(content.contains("databaseChangeLog"), "should contain databaseChangeLog element");
    assertTrue(content.contains("changeSet"), "should contain changeSet element");
    assertTrue(content.contains("id=\"install\""), "changeSet id should be 'install'");
  }

  @Test
  @DisplayName(
      "createTempChangeLogFile references the sql file by name with relativeToChangelogFile=true")
  void createTempChangeLogFileReferencesSqlFileByName() throws Exception {
    File changeLogFile = creator.createTempChangeLogFile(DatabaseType.H2, sqlFile, ";");

    String content = Files.readString(changeLogFile.toPath());
    assertTrue(content.contains(sqlFile.getName()), "should reference sql file by name");
    assertTrue(
        content.contains("relativeToChangelogFile=\"true\""),
        "should set relativeToChangelogFile=true");
  }

  @Test
  @DisplayName("createTempChangeLogFile includes the specified endDelimiter")
  void createTempChangeLogFileIncludesEndDelimiter() throws Exception {
    File changeLogFile = creator.createTempChangeLogFile(DatabaseType.H2, sqlFile, "GO");

    String content = Files.readString(changeLogFile.toPath());
    assertTrue(content.contains("endDelimiter=\"GO\""), "should include the end delimiter");
  }

  @Test
  @DisplayName("createTempChangeLogFile XML-escapes special characters in endDelimiter")
  void createTempChangeLogFileEscapesSpecialCharactersInEndDelimiter() throws Exception {
    File changeLogFile = creator.createTempChangeLogFile(DatabaseType.H2, sqlFile, "&<>\"");

    String content = Files.readString(changeLogFile.toPath());
    assertTrue(content.contains("&amp;"), "& should be escaped to &amp;");
    assertTrue(content.contains("&lt;"), "< should be escaped to &lt;");
    assertTrue(content.contains("&gt;"), "> should be escaped to &gt;");
    assertTrue(content.contains("&quot;"), "\" should be escaped to &quot;");
    assertFalse(
        content.contains("endDelimiter=\"&<>\"\""),
        "raw special chars should not appear unescaped");
  }

  @Test
  @DisplayName(
      "createTempChangeLogFile escapes newline in endDelimiter as \\n for Liquibase compatibility")
  void createTempChangeLogFileEscapesNewlineInEndDelimiter() throws Exception {
    File changeLogFile = creator.createTempChangeLogFile(DatabaseType.SQL_SERVER, sqlFile, "\nGO");

    String content = Files.readString(changeLogFile.toPath());
    assertTrue(
        content.contains("endDelimiter=\"\\nGO\""),
        "real newline should be escaped as \\n for Liquibase");
    assertFalse(
        content.contains("endDelimiter=\"\nGO\""),
        "raw newline byte should not appear in the attribute");
  }
}
