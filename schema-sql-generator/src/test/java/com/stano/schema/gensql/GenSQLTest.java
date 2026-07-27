package com.stano.schema.gensql;

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
import java.io.PrintWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
}
