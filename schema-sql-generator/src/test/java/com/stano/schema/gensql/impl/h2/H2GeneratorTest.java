package com.stano.schema.gensql.impl.h2;

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

@DisplayName("H2Generator integration")
class H2GeneratorTest {

  private Schema schema;

  @BeforeEach
  void setUp() throws Exception {
    URL schemaURL = getClass().getResource("/test-schema.xml");
    assertNotNull(schemaURL, "test-schema.xml must be on the classpath");
    schema = new SchemaParser().parseSchema(schemaURL);
  }

  private String generate(ForeignKeyMode fkMode) {
    StringWriter sw = new StringWriter();
    new H2Generator(
            new SQLGeneratorOptions(
                schema,
                new PrintWriter(sw),
                DatabaseType.H2,
                fkMode,
                BooleanMode.NATIVE,
                OutputMode.ALL))
        .generate();
    return sw.toString();
  }

  @Test
  @DisplayName("generates CREATE TABLE for each table in the schema")
  void generatesCreateTableForEachTable() {
    String sql = generate(ForeignKeyMode.RELATIONS);
    assertTrue(sql.contains("create table public.ParentTable"), "should create ParentTable");
    assertTrue(sql.contains("create table public.ChildTable"), "should create ChildTable");
    assertTrue(
        sql.contains("create table public.ColumnTesterTable"), "should create ColumnTesterTable");
  }

  @Test
  @DisplayName("generates inline PRIMARY KEY and UNIQUE constraints in CREATE TABLE DDL")
  void generatesInlinePrimaryKeyAndUniqueConstraints() {
    String sql = generate(ForeignKeyMode.RELATIONS);
    assertTrue(
        sql.contains("constraint pk_parenttable primary key"),
        "should generate primary key constraint");
    assertTrue(
        sql.contains("constraint ak_parenttable1 unique"), "should generate unique constraint");
  }

  @Test
  @DisplayName("generates CREATE INDEX for non-unique indexes")
  void generatesCreateIndexForNonUniqueIndexes() {
    String sql = generate(ForeignKeyMode.RELATIONS);
    assertTrue(
        sql.contains("create index ix_parenttable1 on public.ParentTable"),
        "should generate indexes");
  }

  @Test
  @DisplayName("generates FOREIGN KEY constraints with RELATIONS mode")
  void generatesForeignKeyConstraintsWithRelationsMode() {
    String sql = generate(ForeignKeyMode.RELATIONS);
    assertTrue(
        sql.contains("foreign key"), "RELATIONS mode should produce foreign key constraints");
    assertTrue(sql.contains("on delete cascade"), "cascade relation should use ON DELETE CASCADE");
    assertTrue(
        sql.contains("on delete set null"), "setnull relation should use ON DELETE SET NULL");
  }

  @Test
  @DisplayName("generates CREATE VIEW for views")
  void generatesCreateViewForViews() {
    String sql = generate(ForeignKeyMode.RELATIONS);
    assertTrue(sql.contains("TestView1"), "should include TestView1 view definition");
  }

  @Test
  @DisplayName("does not generate dialect-specific SQL for postgres or sqlserver")
  void doesNotGenerateDialectSpecificSqlForOtherDatabases() {
    String sql = generate(ForeignKeyMode.RELATIONS);
    assertFalse(
        sql.contains("custom function sql for pgsql 1"),
        "should not include postgres-specific function SQL");
    assertFalse(
        sql.contains("other top sql for mssql 1"),
        "should not include sqlserver-specific otherSql");
  }

  @Test
  @DisplayName("generates initial data that is not database-type specific")
  void generatesInitialDataThatIsNotDatabaseTypeSpecific() {
    String sql = generate(ForeignKeyMode.RELATIONS);
    assertTrue(sql.contains("'AAA'"), "should include non-dialect-specific initial data");
    assertFalse(sql.contains("'PGSQL'"), "should not include postgres-specific initial data");
    assertFalse(sql.contains("'MSSQL'"), "should not include sqlserver-specific initial data");
  }

  @Test
  @DisplayName(
      "TRIGGERS ForeignKeyMode falls back to RELATIONS for H2 since H2 does not support triggers")
  void triggersForeignKeyModeFallsBackToRelationsForH2() {
    String sql = generate(ForeignKeyMode.TRIGGERS);
    assertTrue(
        sql.contains("foreign key"),
        "H2 does not support triggers so FK mode should fall back to RELATIONS");
  }
}
