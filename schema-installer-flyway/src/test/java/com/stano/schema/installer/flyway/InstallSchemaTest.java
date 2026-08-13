package com.stano.schema.installer.flyway;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stano.schema.migrations.MigrationServices;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("InstallSchema")
class InstallSchemaTest {
  private static final String SCHEMA_XML =
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
          + "<database xmlns=\"http://stano.com/database\">\n"
          + "  <table name=\"install_schema_test\">\n"
          + "    <columns>\n"
          + "      <column name=\"id\" type=\"int\" required=\"true\"/>\n"
          + "    </columns>\n"
          + "    <keys>\n"
          + "      <primary>\n"
          + "        <column name=\"id\"/>\n"
          + "      </primary>\n"
          + "    </keys>\n"
          + "  </table>\n"
          + "</database>\n";

  private Connection conn;
  private String jdbcUrl;
  private InstallSchema installSchema;
  private final MigrationServices migrationServices = new MigrationServices();

  @BeforeEach
  void setUp() throws SQLException {
    jdbcUrl = "jdbc:h2:mem:install_schema_test_" + System.nanoTime() + ";MODE=PostgreSQL";
    conn = DriverManager.getConnection(jdbcUrl, "sa", "");
    installSchema = new InstallSchema();
  }

  @AfterEach
  void tearDown() {
    if (conn != null) {
      try {
        conn.close();
      } catch (Exception e) {
        // ignore
      }
    }
  }

  @Test
  @DisplayName("installs the schema when it is not yet installed")
  void installsTheSchemaWhenItIsNotYetInstalled() throws IOException, SQLException {
    URL schemaUrl = writeSchemaFile();

    installSchema.installOrMigrate(jdbcUrl, "sa", "", schemaUrl, null);

    assertTrue(migrationServices.tableExists(conn, "install_schema_test"));
    assertTrue(migrationServices.tableExists(conn, "databaseupgradelog"));
  }

  @Test
  @DisplayName("does not fail when run again against an already-installed schema")
  void doesNotFailWhenRunAgainAgainstAnAlreadyInstalledSchema() throws IOException, SQLException {
    URL schemaUrl = writeSchemaFile();

    installSchema.installOrMigrate(jdbcUrl, "sa", "", schemaUrl, null);

    assertDoesNotThrow(() -> installSchema.installOrMigrate(jdbcUrl, "sa", "", schemaUrl, null));
  }

  private URL writeSchemaFile() throws IOException {
    File schemaFile = File.createTempFile("install_schema_test_" + System.nanoTime(), ".xml");
    schemaFile.deleteOnExit();
    try (FileWriter writer = new FileWriter(schemaFile)) {
      writer.write(SCHEMA_XML);
    }
    try {
      return schemaFile.toURI().toURL();
    } catch (MalformedURLException x) {
      throw new IllegalStateException(x);
    }
  }
}
