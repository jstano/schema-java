package com.stano.schema.gensql.impl.sqlserver;

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

@DisplayName("SQLServerGenerator integration")
class SQLServerGeneratorTest {

  private Schema schema;

  @BeforeEach
  void setUp() throws Exception {
    URL schemaURL = getClass().getResource("/test-schema.xml");
    assertNotNull(schemaURL, "test-schema.xml must be on the classpath");
    schema = new SchemaParser().parseSchema(schemaURL);
  }

  private String generate(ForeignKeyMode fkMode, BooleanMode boolMode) {
    StringWriter sw = new StringWriter();
    new SQLServerGenerator(
            new SQLGeneratorOptions(
                schema,
                new PrintWriter(sw),
                DatabaseType.SQL_SERVER,
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
    assertTrue(sql.contains("create table dbo.ParentTable"), "should create ParentTable");
    assertTrue(sql.contains("create table dbo.ChildTable"), "should create ChildTable");
    assertTrue(
        sql.contains("create table dbo.ColumnTesterTable"), "should create ColumnTesterTable");
  }

  @Test
  @DisplayName("generates DROP TABLE IF EXISTS guard before each CREATE TABLE")
  void generatesDropTableIfExistsGuardBeforeEachCreateTable() {
    String sql = generate(ForeignKeyMode.RELATIONS, BooleanMode.NATIVE);
    assertTrue(
        sql.contains("if exists (select name from dbo.sysobjects"),
        "should include IF EXISTS guard");
    assertTrue(sql.contains("drop table"), "should include DROP TABLE");
  }

  @Test
  @DisplayName("generates NONCLUSTERED primary key constraint")
  void generatesNonclusteredPrimaryKeyConstraint() {
    String sql = generate(ForeignKeyMode.RELATIONS, BooleanMode.NATIVE);
    assertTrue(
        sql.contains("primary key nonclustered"),
        "should include nonclustered keyword for SQL Server PK");
  }

  @Test
  @DisplayName("generates CLUSTERED unique constraint for cluster=true")
  void generatesClusteredUniqueConstraintForClusterKeys() {
    String sql = generate(ForeignKeyMode.RELATIONS, BooleanMode.NATIVE);
    assertTrue(
        sql.contains("constraint ak_parenttable1 unique clustered"),
        "clustered unique key should have clustered keyword");
  }

  @Test
  @DisplayName("generates inline unique constraints in CREATE TABLE DDL")
  void generatesInlineUniqueConstraints() {
    String sql = generate(ForeignKeyMode.RELATIONS, BooleanMode.NATIVE);
    assertTrue(
        sql.contains("constraint ak_childtable1 unique"),
        "should generate unique constraints inline");
  }

  @Test
  @DisplayName("generates CREATE INDEX for non-unique indexes")
  void generatesCreateIndexForNonUniqueIndexes() {
    String sql = generate(ForeignKeyMode.RELATIONS, BooleanMode.NATIVE);
    assertTrue(
        sql.contains("create index ix_parenttable1 on dbo.ParentTable"),
        "should generate non-unique indexes");
  }

  @Test
  @DisplayName("generates FOREIGN KEY constraints with RELATIONS mode")
  void generatesForeignKeyConstraintsWithRelationsMode() {
    String sql = generate(ForeignKeyMode.RELATIONS, BooleanMode.NATIVE);
    assertTrue(
        sql.contains("foreign key"), "RELATIONS mode should produce foreign key constraints");
    assertTrue(sql.contains("on delete cascade"), "cascade relation should use ON DELETE CASCADE");
  }

  @Test
  @DisplayName("generates VIEW for views")
  void generatesViewForViews() {
    String sql = generate(ForeignKeyMode.RELATIONS, BooleanMode.NATIVE);
    assertTrue(sql.contains("TestView1"), "should include TestView1 definition");
  }

  @Test
  @DisplayName("generates custom function SQL for sqlserver dialect")
  void generatesCustomFunctionSqlForSqlServerDialect() {
    String sql = generate(ForeignKeyMode.RELATIONS, BooleanMode.NATIVE);
    assertTrue(
        sql.contains("custom function sql for mssql 1"), "should include sqlserver function SQL");
    assertFalse(
        sql.contains("custom function sql for pgsql 1"),
        "should not include postgres function SQL");
  }

  @Test
  @DisplayName("generates otherSql top and bottom blocks for sqlserver dialect")
  void generatesOtherSqlForSqlServerDialect() {
    String sql = generate(ForeignKeyMode.RELATIONS, BooleanMode.NATIVE);
    assertTrue(sql.contains("other top sql for mssql 1"), "should include sqlserver top otherSql");
    assertTrue(
        sql.contains("other bottom sql for mssql 1"), "should include sqlserver bottom otherSql");
    assertFalse(sql.contains("other top sql for pgsql 1"), "should not include postgres otherSql");
  }

  @Test
  @DisplayName("generates database-specific initial data for sqlserver")
  void generatesDatabaseSpecificInitialDataForSqlServer() {
    String sql = generate(ForeignKeyMode.RELATIONS, BooleanMode.NATIVE);
    assertTrue(sql.contains("'MSSQL'"), "should include sqlserver-specific initial data row");
    assertFalse(sql.contains("'PGSQL'"), "should not include postgres-specific initial data row");
  }

  @Test
  @DisplayName("generates delete trigger SQL for sqlserver tables with delete triggers defined")
  void generatesDeleteTriggerSqlForTablesWithDeleteTriggersDefined() {
    String sql = generate(ForeignKeyMode.TRIGGERS, BooleanMode.NATIVE);
    assertTrue(
        sql.contains("delete from mssql"),
        "should include table-level sqlserver delete trigger body");
  }

  @Test
  @DisplayName("generates delete and update trigger definitions with TRIGGERS mode")
  void generatesTriggerDefinitionsWithTriggersMode() {
    String sql = generate(ForeignKeyMode.TRIGGERS, BooleanMode.NATIVE);
    assertTrue(
        sql.contains("create trigger parenttable_delete on dbo.ParentTable"),
        "should create delete trigger");
    assertTrue(
        sql.contains("create trigger parenttable_update on dbo.ParentTable"),
        "should create update trigger");
  }

  @ParameterizedTest
  @EnumSource(value = ForeignKeyMode.class)
  @DisplayName("generates valid SQL for all foreign key modes")
  void generatesValidSqlForAllForeignKeyModes(ForeignKeyMode fkMode) {
    String sql = generate(fkMode, BooleanMode.NATIVE);
    assertTrue(
        sql.contains("create table dbo.ParentTable"),
        "should always create ParentTable regardless of FK mode");
  }
}
