package com.stano.schema.gensql;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stano.schema.gensql.impl.common.OutputMode;
import com.stano.schema.gensql.impl.common.SQLGenerator;
import com.stano.schema.gensql.impl.common.SQLGeneratorFactory;
import com.stano.schema.gensql.impl.common.SQLGeneratorOptions;
import com.stano.schema.model.BooleanMode;
import com.stano.schema.model.DatabaseType;
import com.stano.schema.model.ForeignKeyMode;
import com.stano.schema.model.Schema;
import java.io.File;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("GenSQL")
class GenSQLTest {

  @Mock private Schema mockSchema;

  @Mock private PrintWriter mockWriter;

  @Mock private SQLGenerator mockSQLGenerator;

  @Mock private SQLGeneratorFactory mockSQLGeneratorFactory;

  @Test
  @DisplayName(
      "should be able to generate the sql from the schema using the full generateSQL method")
  void shouldBeAbleToGenerateTheSqlFromTheSchemaUsingTheFullGenerateSQLMethod() {
    doAnswer(
            inv -> {
              // Generator called, no op
              return null;
            })
        .when(mockSQLGenerator)
        .generate();

    when(mockSQLGeneratorFactory.createSQLGenerator(any()))
        .thenAnswer(
            inv -> {
              var options = (SQLGeneratorOptions) inv.getArgument(0);
              if (options.getSchema() == mockSchema
                  && options.getSqlWriter() == mockWriter
                  && options.getDatabaseType() == DatabaseType.POSTGRESQL
                  && options.getForeignKeyMode() == ForeignKeyMode.RELATIONS
                  && options.getBooleanMode() == BooleanMode.NATIVE
                  && options.getOutputMode() == OutputMode.ALL
                  && options.getStatementSeparator().equals(";")) {
                return mockSQLGenerator;
              }
              return null;
            });

    GenSQL genSql = new GenSQL();
    genSql.sqlGeneratorFactory = mockSQLGeneratorFactory;

    genSql.generateSQL(
        DatabaseType.POSTGRESQL,
        mockSchema,
        mockWriter,
        ForeignKeyMode.RELATIONS,
        BooleanMode.NATIVE,
        OutputMode.ALL,
        ";");

    verify(mockSQLGenerator).generate();
    verify(mockWriter).close();
  }

  @Test
  @DisplayName(
      "should be able to generate the sql from the schema using the generateSQL method without the"
          + " OutputMode")
  void shouldBeAbleToGenerateTheSqlFromTheSchemaUsingTheGenerateSQLMethodWithoutTheOutputMode() {
    doAnswer(
            inv -> {
              // Generator called, no op
              return null;
            })
        .when(mockSQLGenerator)
        .generate();

    when(mockSQLGeneratorFactory.createSQLGenerator(any()))
        .thenAnswer(
            inv -> {
              var options = (SQLGeneratorOptions) inv.getArgument(0);
              if (options.getSchema() == mockSchema
                  && options.getSqlWriter() == mockWriter
                  && options.getDatabaseType() == DatabaseType.POSTGRESQL
                  && options.getForeignKeyMode() == ForeignKeyMode.RELATIONS
                  && options.getBooleanMode() == BooleanMode.NATIVE
                  && options.getOutputMode() == OutputMode.ALL
                  && options.getStatementSeparator().equals(";")) {
                return mockSQLGenerator;
              }
              return null;
            });

    GenSQL genSql = new GenSQL();
    genSql.sqlGeneratorFactory = mockSQLGeneratorFactory;

    genSql.generateSQL(
        DatabaseType.POSTGRESQL,
        mockSchema,
        mockWriter,
        ForeignKeyMode.RELATIONS,
        BooleanMode.NATIVE,
        ";");

    verify(mockSQLGenerator).generate();
    verify(mockWriter).close();
  }

  @Test
  @DisplayName(
      "should be able to generate the sql from the schema using the middle generateSQL method")
  void shouldBeAbleToGenerateTheSqlFromTheSchemaUsingTheMiddleGenerateSQLMethod() {
    doAnswer(
            inv -> {
              // Generator called, no op
              return null;
            })
        .when(mockSQLGenerator)
        .generate();

    when(mockSQLGeneratorFactory.createSQLGenerator(any()))
        .thenAnswer(
            inv -> {
              var options = (SQLGeneratorOptions) inv.getArgument(0);
              if (options.getSchema() == mockSchema
                  && options.getSqlWriter() == mockWriter
                  && options.getDatabaseType() == DatabaseType.POSTGRESQL
                  && options.getForeignKeyMode() == ForeignKeyMode.RELATIONS
                  && options.getBooleanMode() == BooleanMode.NATIVE
                  && options.getOutputMode() == OutputMode.ALL
                  && options
                      .getStatementSeparator()
                      .equals(DatabaseType.POSTGRESQL.getStatementSeparator())) {
                return mockSQLGenerator;
              }
              return null;
            });

    GenSQL genSql = new GenSQL();
    genSql.sqlGeneratorFactory = mockSQLGeneratorFactory;

    genSql.generateSQL(
        DatabaseType.POSTGRESQL,
        mockSchema,
        mockWriter,
        ForeignKeyMode.RELATIONS,
        BooleanMode.NATIVE,
        OutputMode.ALL);

    verify(mockSQLGenerator).generate();
    verify(mockWriter).close();
  }

  @Test
  @DisplayName("should pass targetPostgresVersion through to SQLGeneratorOptions")
  void shouldPassTargetPostgresVersionThroughToSQLGeneratorOptions() {
    doAnswer(
            inv -> {
              return null;
            })
        .when(mockSQLGenerator)
        .generate();

    when(mockSQLGeneratorFactory.createSQLGenerator(any()))
        .thenAnswer(
            inv -> {
              var options = (SQLGeneratorOptions) inv.getArgument(0);
              if (options.getSchema() == mockSchema
                  && options.getSqlWriter() == mockWriter
                  && options.getDatabaseType() == DatabaseType.POSTGRESQL
                  && options.getForeignKeyMode() == ForeignKeyMode.RELATIONS
                  && options.getBooleanMode() == BooleanMode.NATIVE
                  && options.getOutputMode() == OutputMode.ALL
                  && options.getStatementSeparator().equals(";")
                  && options.getTargetPostgresVersion() == 18) {
                return mockSQLGenerator;
              }
              return null;
            });

    GenSQL genSql = new GenSQL();
    genSql.sqlGeneratorFactory = mockSQLGeneratorFactory;

    genSql.generateSQL(
        DatabaseType.POSTGRESQL,
        mockSchema,
        mockWriter,
        ForeignKeyMode.RELATIONS,
        BooleanMode.NATIVE,
        OutputMode.ALL,
        ";",
        18);

    verify(mockSQLGenerator).generate();
    verify(mockWriter).close();
  }

  private static String testSchemaPath() throws URISyntaxException {
    return new File(GenSQLTest.class.getResource("/test-schema.xml").toURI()).getPath();
  }

  @Test
  void runRejectsNonNumericPostgresVersion(@TempDir Path tempDir) throws Exception {
    File out = tempDir.resolve("out.sql").toFile();

    var x =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                GenSQL.run(
                    new String[] {
                      "POSTGRESQL",
                      testSchemaPath(),
                      "--postgresql-version=abc",
                      "--output-file=" + out
                    }));

    assertTrue(x.getMessage().contains("--postgresql-version"));
    assertFalse(out.exists());
  }

  @Test
  void runRejectsUnknownOption(@TempDir Path tempDir) {
    var x =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                GenSQL.run(
                    new String[] {
                      "POSTGRESQL",
                      testSchemaPath(),
                      "--output-index-only",
                      "--output-file=" + tempDir.resolve("out.sql")
                    }));

    assertTrue(x.getMessage().contains("--output-index-only"));
  }

  @Test
  void runRejectsUnknownForeignKeyMode(@TempDir Path tempDir) throws Exception {
    var x =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                GenSQL.run(
                    new String[] {
                      "POSTGRESQL",
                      testSchemaPath(),
                      "--foreign-key-mode=bogus",
                      "--output-file=" + tempDir.resolve("out.sql")
                    }));

    assertTrue(x.getMessage().contains("--foreign-key-mode"));
  }

  @Test
  void runRejectsOutputFileWithMultipleDatabases() throws Exception {
    var x =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                GenSQL.run(
                    new String[] {"H2,POSTGRESQL", testSchemaPath(), "--output-file=x.sql"}));

    assertTrue(x.getMessage().contains("--output-file"));
  }

  @Test
  void runThrowsWhenSchemaFileIsMissing(@TempDir Path tempDir) {
    assertThrows(
        Exception.class,
        () -> GenSQL.run(new String[] {"POSTGRESQL", tempDir.resolve("missing.xml").toString()}));
  }

  @Test
  @DisplayName("run accepts a schema path containing spaces")
  void runAcceptsSchemaPathWithSpaces(@TempDir Path tempDir) throws Exception {
    Path dir = java.nio.file.Files.createDirectory(tempDir.resolve("my schemas"));
    Path copy = dir.resolve("test schema.xml");
    java.nio.file.Files.copy(Path.of(testSchemaPath()), copy);
    Path out = tempDir.resolve("out.sql");

    GenSQL.run(new String[] {"POSTGRESQL", copy.toString(), "--output-file=" + out});

    assertTrue(java.nio.file.Files.size(out) > 0);
  }
}
