package com.stano.schema.gensql.impl.postgresql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stano.schema.gensql.impl.common.OutputMode;
import com.stano.schema.gensql.impl.common.SQLGenerator;
import com.stano.schema.gensql.impl.common.SQLGeneratorOptions;
import com.stano.schema.model.BooleanMode;
import com.stano.schema.model.DatabaseType;
import com.stano.schema.model.ForeignKeyMode;
import com.stano.schema.model.Schema;
import com.stano.schema.parser.SchemaParser;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PostgreSQL UUID Version-Specific Generation")
class PostgreSQLUUIDVersionTest {

  private Schema schema;

  @BeforeEach
  void setUp() throws Exception {
    URL schemaURL = getClass().getResource("/test-schema.xml");
    assertNotNull(schemaURL, "test-schema.xml must be on the classpath");
    schema = new SchemaParser().parseSchema(schemaURL);
  }

  @Test
  @DisplayName("should generate generate_uuid() function for PG 17")
  void shouldGenerateGenerateUUIDFunctionForPG17() throws Exception {

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);

    PostgreSQLGenerator generator =
        new PostgreSQLGenerator(
            new SQLGeneratorOptions(
                schema,
                pw,
                DatabaseType.POSTGRESQL,
                ForeignKeyMode.RELATIONS,
                BooleanMode.NATIVE,
                OutputMode.ALL,
                ";",
                17));

    generator.generate();
    String output = sw.toString();

    assertTrue(output.contains("generate_uuid()"), "PG 17 should have generate_uuid() function");
    assertTrue(
        output.contains("create or replace function generate_uuid()"),
        "Should have explicit function definition");
  }

  @Test
  @DisplayName("should not generate generate_uuid() function for PG 18")
  void shouldNotGenerateGenerateUUIDFunctionForPG18() throws Exception {

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);

    PostgreSQLGenerator generator =
        new PostgreSQLGenerator(
            new SQLGeneratorOptions(
                schema,
                pw,
                DatabaseType.POSTGRESQL,
                ForeignKeyMode.RELATIONS,
                BooleanMode.NATIVE,
                OutputMode.ALL,
                ";",
                18));

    generator.generate();
    String output = sw.toString();

    assertFalse(
        output.contains("create or replace function generate_uuid()"),
        "PG 18 should not have generate_uuid() function definition");
  }

  @Test
  @DisplayName("should use uuidv7() for PG 18 UUID defaults")
  void shouldUseUuidv7ForPG18UUIDDefaults() throws Exception {
    PostgreSQLColumnTypeGenerator gen = new PostgreSQLColumnTypeGenerator(createMockGenerator(18));

    String defaultValue = gen.getUUIDDefaultValueSql(null);
    assertEquals("uuidv7()", defaultValue, "PG 18 should use uuidv7() for UUID defaults");
  }

  private SQLGenerator createMockGenerator(int pgVersion) {
    return new SQLGenerator(
        new SQLGeneratorOptions(
            null,
            null,
            DatabaseType.POSTGRESQL,
            ForeignKeyMode.RELATIONS,
            BooleanMode.NATIVE,
            OutputMode.ALL,
            ";",
            pgVersion)) {
      @Override
      protected void outputTables() {}

      @Override
      protected void outputRelations() {}

      @Override
      protected void outputIndexes() {}

      @Override
      protected void outputTriggers() {}

      @Override
      protected void outputFunctions() {}

      @Override
      protected void outputViews() {}

      @Override
      protected void outputProcedures() {}

      @Override
      protected void outputOtherSqlTop() {}

      @Override
      protected void outputOtherSqlBottom() {}
    };
  }

  @Test
  @DisplayName("should use generate_uuid() for PG 17 UUID defaults")
  void shouldUseGenerateUUIDForPG17UUIDDefaults() throws Exception {
    PostgreSQLColumnTypeGenerator gen = new PostgreSQLColumnTypeGenerator(createMockGenerator(17));

    String defaultValue = gen.getUUIDDefaultValueSql(null);
    assertEquals(
        "generate_uuid()", defaultValue, "PG 17 should use generate_uuid() for UUID defaults");
  }

  @Test
  @DisplayName("default version (0) should use generate_uuid() as pre-18")
  void shouldDefaultToPrePG18Behavior() throws Exception {

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);

    PostgreSQLGenerator generator =
        new PostgreSQLGenerator(
            new SQLGeneratorOptions(
                schema,
                pw,
                DatabaseType.POSTGRESQL,
                ForeignKeyMode.RELATIONS,
                BooleanMode.NATIVE,
                OutputMode.ALL,
                ";",
                0));

    generator.generate();
    String output = sw.toString();

    assertTrue(
        output.contains("create or replace function generate_uuid()"),
        "Default (version 0) should use generate_uuid() function");
    assertTrue(
        output.contains("generate_uuid()"),
        "Default (version 0) should use generate_uuid() for defaults");
  }

  @Test
  @DisplayName("default version (0) generates generate_uuid() in column defaults")
  void shouldUseGenerateUUIDForDefaultVersion() throws Exception {
    PostgreSQLColumnTypeGenerator gen = new PostgreSQLColumnTypeGenerator(createMockGenerator(0));

    String defaultValue = gen.getUUIDDefaultValueSql(null);
    assertEquals(
        "generate_uuid()",
        defaultValue,
        "Default version (0) should use generate_uuid() for UUID defaults");
  }
}
