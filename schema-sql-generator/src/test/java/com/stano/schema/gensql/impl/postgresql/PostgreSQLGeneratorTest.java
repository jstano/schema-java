package com.stano.schema.gensql.impl.postgresql;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stano.schema.gensql.impl.common.OutputMode;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("PostgreSQLGenerator integration")
class PostgreSQLGeneratorTest {

  private Schema schema;

  @BeforeEach
  void setUp() throws Exception {
    URL schemaURL = getClass().getResource("/test-schema.xml");
    assertNotNull(schemaURL, "test-schema.xml must be on the classpath");
    schema = new SchemaParser().parseSchema(schemaURL);
  }

  private String generate(ForeignKeyMode fkMode, BooleanMode boolMode) {
    StringWriter sw = new StringWriter();
    new PostgreSQLGenerator(
            new SQLGeneratorOptions(
                schema,
                new PrintWriter(sw),
                DatabaseType.POSTGRESQL,
                fkMode,
                boolMode,
                OutputMode.ALL))
        .generate();
    return sw.toString();
  }

  @Test
  @DisplayName("generates CREATE TABLE for each table in the schema")
  void generatesCreateTableForEachTable() {
    String sql = generate(ForeignKeyMode.RELATIONS, BooleanMode.NATIVE);
    assertTrue(sql.contains("create table public.ParentTable"), "should create ParentTable");
    assertTrue(sql.contains("create table public.ChildTable"), "should create ChildTable");
    assertTrue(
        sql.contains("create table public.ColumnTesterTable"), "should create ColumnTesterTable");
  }

  @Test
  @DisplayName("generates CREATE TYPE AS ENUM for enum types")
  void generatesCreateTypeAsEnumForEnumTypes() {
    String sql = generate(ForeignKeyMode.RELATIONS, BooleanMode.NATIVE);
    assertTrue(sql.contains("create type gender_type as enum"), "should create gender_type enum");
    assertTrue(
        sql.contains("create type test_enum_type as enum"), "should create test_enum_type enum");
  }

  @Test
  @DisplayName("generates create extension block")
  void generatesCreateExtensionBlock() {
    String sql = generate(ForeignKeyMode.RELATIONS, BooleanMode.NATIVE);
    assertTrue(sql.contains("create extension if not exists"), "should include extension creation");
  }

  @Test
  @DisplayName("omits create extension block when emitPostgresExtensions is false")
  void omitsCreateExtensionBlockWhenDisabled() {
    StringWriter sw = new StringWriter();
    new PostgreSQLGenerator(
            new SQLGeneratorOptions(
                schema,
                new PrintWriter(sw),
                DatabaseType.POSTGRESQL,
                ForeignKeyMode.RELATIONS,
                BooleanMode.NATIVE,
                OutputMode.ALL,
                DatabaseType.POSTGRESQL.getStatementSeparator(),
                0,
                false))
        .generate();
    String sql = sw.toString();
    assertFalse(sql.contains("create extension"), "should not include extension creation");
  }

  @Test
  @DisplayName("checks CURRENT_USER's privilege by default")
  void checksCurrentUserByDefault() {
    String sql = generate(ForeignKeyMode.RELATIONS, BooleanMode.NATIVE);
    assertTrue(
        sql.contains("where usename = CURRENT_USER"), "should check CURRENT_USER by default");
  }

  @Test
  @DisplayName("checks the configured extensionCheckUser's privilege when set")
  void checksConfiguredExtensionUser() {
    StringWriter sw = new StringWriter();
    new PostgreSQLGenerator(
            new SQLGeneratorOptions(
                schema,
                new PrintWriter(sw),
                DatabaseType.POSTGRESQL,
                ForeignKeyMode.RELATIONS,
                BooleanMode.NATIVE,
                OutputMode.ALL,
                DatabaseType.POSTGRESQL.getStatementSeparator(),
                0,
                true,
                "schema_admin"))
        .generate();
    String sql = sw.toString();
    assertTrue(sql.contains("where usename = 'schema_admin'"), "should check the configured role");
    assertFalse(sql.contains("CURRENT_USER)"), "should not check CURRENT_USER");
  }

  @Test
  @DisplayName("generates CREATE INDEX for non-unique indexes")
  void generatesCreateIndexForNonUniqueIndexes() {
    String sql = generate(ForeignKeyMode.RELATIONS, BooleanMode.NATIVE);
    assertTrue(
        sql.contains("create index ix_parenttable1 on public.ParentTable"),
        "should create non-unique indexes");
  }

  @Test
  @DisplayName("generates inline UNIQUE constraints in CREATE TABLE DDL")
  void generatesInlineUniqueConstraints() {
    String sql = generate(ForeignKeyMode.RELATIONS, BooleanMode.NATIVE);
    assertTrue(
        sql.contains("constraint ak_parenttable1 unique"),
        "should generate unique constraints inline");
  }

  @Test
  @DisplayName("generates FOREIGN KEY constraints with RELATIONS mode")
  void generatesForeignKeyConstraintsWithRelationsMode() {
    String sql = generate(ForeignKeyMode.RELATIONS, BooleanMode.NATIVE);
    assertTrue(
        sql.contains("foreign key"), "RELATIONS mode should produce foreign key constraints");
    assertTrue(sql.contains("on delete cascade"), "cascade relation should use ON DELETE CASCADE");
    assertTrue(
        sql.contains("on delete set null"), "setnull relation should use ON DELETE SET NULL");
  }

  @Test
  @DisplayName("generates trigger-based FK enforcement with TRIGGERS mode")
  void generatesTriggerBasedFKEnforcementWithTriggersMode() {
    String sql = generate(ForeignKeyMode.TRIGGERS, BooleanMode.NATIVE);
    assertTrue(sql.contains("create trigger"), "TRIGGERS mode should produce trigger definitions");
    assertFalse(
        sql.contains("foreign key"), "TRIGGERS mode should not produce foreign key constraints");
  }

  @Test
  @DisplayName("generates CREATE OR REPLACE VIEW for views")
  void generatesCreateOrReplaceViewForViews() {
    String sql = generate(ForeignKeyMode.RELATIONS, BooleanMode.NATIVE);
    assertTrue(
        sql.contains("create or replace view test.TestView1"), "should create TestView1 view");
    assertTrue(
        sql.contains("create or replace view public.TestView2"),
        "should create postgres-specific TestView2 view");
  }

  @Test
  @DisplayName("generates custom function SQL for postgres dialect")
  void generatesCustomFunctionSqlForPostgresDialect() {
    String sql = generate(ForeignKeyMode.RELATIONS, BooleanMode.NATIVE);
    assertTrue(
        sql.contains("custom function sql for pgsql 1"), "should include postgres function SQL");
    assertFalse(
        sql.contains("custom function sql for mssql 1"),
        "should not include sqlserver function SQL");
  }

  @Test
  @DisplayName("generates custom procedure SQL for postgres dialect")
  void generatesCustomProcedureSqlForPostgresDialect() {
    String sql = generate(ForeignKeyMode.RELATIONS, BooleanMode.NATIVE);
    assertTrue(
        sql.contains("custom procedure sql for pgsql 1"), "should include postgres procedure SQL");
  }

  @Test
  @DisplayName("generates otherSql top and bottom blocks for postgres dialect")
  void generatesOtherSqlTopAndBottomForPostgresDialect() {
    String sql = generate(ForeignKeyMode.RELATIONS, BooleanMode.NATIVE);
    assertTrue(sql.contains("other top sql for pgsql 1"), "should include postgres top otherSql");
    assertTrue(
        sql.contains("other bottom sql for pgsql 1"), "should include postgres bottom otherSql");
    assertFalse(sql.contains("other top sql for mssql 1"), "should not include sqlserver otherSql");
  }

  @Test
  @DisplayName("generates database-specific initial data for postgres")
  void generatesDatabaseSpecificInitialDataForPostgres() {
    String sql = generate(ForeignKeyMode.RELATIONS, BooleanMode.NATIVE);
    assertTrue(sql.contains("'PGSQL'"), "should include postgres-specific initial data row");
    assertFalse(sql.contains("'MSSQL'"), "should not include sqlserver-specific initial data row");
  }

  @Test
  @DisplayName("generates CHAR column type for BOOLEAN in YN mode")
  void generatesCharColumnTypeForBooleanInYnMode() {
    String sql = generate(ForeignKeyMode.RELATIONS, BooleanMode.YN);
    assertTrue(sql.contains("char(1)"), "YN boolean mode should produce char(1)");
  }

  @ParameterizedTest
  @EnumSource(value = ForeignKeyMode.class)
  @DisplayName("generates valid SQL for all foreign key modes")
  void generatesValidSqlForAllForeignKeyModes(ForeignKeyMode fkMode) {
    String sql = generate(fkMode, BooleanMode.NATIVE);
    assertTrue(
        sql.contains("create table public.ParentTable"),
        "should always create ParentTable regardless of FK mode");
  }

  @Test
  @DisplayName("generates delete trigger SQL for postgres tables with delete triggers defined")
  void generatesDeleteTriggerSqlForTablesWithDeleteTriggersDefined() {
    String sql = generate(ForeignKeyMode.RELATIONS, BooleanMode.NATIVE);
    assertTrue(
        sql.contains("delete from pgsql"),
        "should include table-level postgres delete trigger body");
  }

  @Test
  @DisplayName("generates update trigger SQL for postgres tables with aggregations")
  void generatesUpdateTriggerSqlForTablesWithAggregations() {
    String sql = generate(ForeignKeyMode.RELATIONS, BooleanMode.NATIVE);
    assertTrue(
        sql.contains("create trigger parenttable_update"),
        "should generate update triggers for tables with aggregations");
  }
}
