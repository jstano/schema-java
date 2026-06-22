package com.stano.schema.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Relation")
class RelationTest {

  @ParameterizedTest
  @MethodSource("provideRelationTestCases")
  @DisplayName(
      "constructor should set fields and getters should return them for different types and flags")
  void constructorShouldSetFields(
      String fromTable,
      String fromColumn,
      String toTable,
      String toColumn,
      RelationType type,
      boolean disable) {
    Relation rel = new Relation(fromTable, fromColumn, toTable, toColumn, type, disable);

    assertEquals(rel.getFromTableName(), fromTable);
    assertEquals(rel.getFromColumnName(), fromColumn);
    assertEquals(rel.getToTableName(), toTable);
    assertEquals(rel.getToColumnName(), toColumn);
    assertEquals(rel.getType(), type);
    assertEquals(rel.isDisableUsageChecking(), disable);
  }

  private static Stream<Object[]> provideRelationTestCases() {
    return Stream.of(
        new Object[] {"orders", "user_id", "users", "id", RelationType.CASCADE, false},
        new Object[] {"orders", "user_id", "users", "id", RelationType.ENFORCE, true},
        new Object[] {"orders", "user_id", "users", "id", RelationType.SETNULL, false},
        new Object[] {"orders", "user_id", "users", "id", RelationType.DONOTHING, true});
  }

  @Test
  @DisplayName(
      "Schema.buildReverseRelations should add inverse relation entries with"
          + " disableUsageChecking=false")
  void schemaBuildReverseRelationsShouldAddInverseRelationEntries() throws MalformedURLException {
    Schema schema = new Schema(URI.create("https://example.com/schema.json").toURL());
    Table parent = new Table(schema, "public", "users", null, LockEscalation.AUTO, false);
    Table child = new Table(schema, "public", "orders", null, LockEscalation.AUTO, false);

    parent.getColumns().add(new Column("id", ColumnType.SEQUENCE, 0, true));
    child.getColumns().add(new Column("user_id", ColumnType.INT, 0, false));

    child
        .getRelations()
        .add(new Relation("orders", "user_id", "users", "id", RelationType.CASCADE, true));

    schema.addTable(parent);
    schema.addTable(child);

    schema.buildReverseRelations();

    assertEquals(parent.getReverseRelations().size(), 1);
    assertEquals(parent.getReverseRelations().get(0).getFromTableName(), "users");
    assertEquals(parent.getReverseRelations().get(0).getFromColumnName(), "id");
    assertEquals(parent.getReverseRelations().get(0).getToTableName(), "orders");
    assertEquals(parent.getReverseRelations().get(0).getToColumnName(), "user_id");
    assertEquals(parent.getReverseRelations().get(0).getType(), RelationType.CASCADE);
    assertFalse(parent.getReverseRelations().get(0).isDisableUsageChecking());
  }

  @Test
  @DisplayName(
      "Schema.validate should flag SETNULL when from-column is required and ignore otherwise")
  void schemaValidateShouldFlagSETNULL() throws MalformedURLException {
    Schema schema = new Schema(URI.create("https://example.com/schema.json").toURL());

    Table parent = new Table(schema, "public", "parent", null, LockEscalation.AUTO, false);
    Table childRequired =
        new Table(schema, "public", "child_req", null, LockEscalation.AUTO, false);
    Table childOptional =
        new Table(schema, "public", "child_opt", null, LockEscalation.AUTO, false);

    parent.getColumns().add(new Column("id", ColumnType.SEQUENCE, 0, true));
    childRequired.getColumns().add(new Column("parent_id", ColumnType.INT, 0, true));
    childOptional.getColumns().add(new Column("parent_id", ColumnType.INT, 0, false));

    childRequired
        .getRelations()
        .add(new Relation("child_req", "parent_id", "parent", "id", RelationType.SETNULL, false));
    childOptional
        .getRelations()
        .add(new Relation("child_opt", "parent_id", "parent", "id", RelationType.SETNULL, false));

    schema.addTable(parent);
    schema.addTable(childRequired);
    schema.addTable(childOptional);

    var errors = schema.validate();

    assertEquals(errors.size(), 1);
    assertTrue(errors.get(0).contains("child_req.parent_id is required"));
    assertTrue(errors.get(0).contains("relation specifies setnull"));

    assertFalse(errors.stream().anyMatch(e -> e.contains("child_opt.parent_id")));
  }
}
