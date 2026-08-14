package com.stano.schema.model;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.collections4.map.CaseInsensitiveMap;

/**
 * The root of the in-memory schema model, corresponding to the {@code <database>} root element of
 * the XML schema format. Aggregates all top-level definitions parsed from a schema file — tables,
 * views, functions, procedures, enum types, and raw SQL fragments — along with schema-wide
 * generation settings such as {@link ForeignKeyMode} and {@link BooleanMode}.
 */
public class Schema {
  private final URL schemaURL;
  private final List<Table> tables = new ArrayList<>();
  private final List<View> views = new ArrayList<>();
  private final List<Function> functions = new ArrayList<>();
  private final List<Procedure> procedures = new ArrayList<>();
  private final List<OtherSql> otherSql = new ArrayList<>();
  private final Map<String, Table> tableMap = new CaseInsensitiveMap<>();
  private final Map<String, EnumType> enumTypes = new HashMap<>();
  private ForeignKeyMode foreignKeyMode = ForeignKeyMode.RELATIONS;
  private BooleanMode booleanMode = BooleanMode.NATIVE;
  private boolean caseSensitiveText = true;

  /**
   * Creates an empty schema backed by the given source URL.
   *
   * @param schemaURL the location the schema was (or will be) loaded from
   */
  public Schema(URL schemaURL) {
    this.schemaURL = schemaURL;
  }

  /** Returns the location the schema was loaded from. */
  public URL getSchemaURL() {
    return schemaURL;
  }

  /** Returns how foreign keys declared via relations should be enforced in generated SQL. */
  public ForeignKeyMode getForeignKeyMode() {
    return foreignKeyMode;
  }

  /** Sets how foreign keys declared via relations should be enforced in generated SQL. */
  public void setForeignKeyMode(ForeignKeyMode foreignKeyMode) {
    this.foreignKeyMode = foreignKeyMode;
  }

  /** Returns how boolean columns should be represented in generated SQL. */
  public BooleanMode getBooleanMode() {
    return booleanMode;
  }

  /** Sets how boolean columns should be represented in generated SQL. */
  public void setBooleanMode(BooleanMode booleanMode) {
    this.booleanMode = booleanMode;
  }

  /** Returns whether text columns/comparisons are treated as case-sensitive. */
  public boolean isCaseSensitiveText() {
    return caseSensitiveText;
  }

  /** Sets whether text columns/comparisons are treated as case-sensitive. */
  public void setCaseSensitiveText(boolean caseSensitiveText) {
    this.caseSensitiveText = caseSensitiveText;
  }

  /** Returns all tables defined in the schema. */
  public List<Table> getTables() {
    return Collections.unmodifiableList(tables);
  }

  /**
   * Looks up a table by name.
   *
   * @param name the table name
   * @return the matching table
   * @throws IllegalStateException if no table with the given name exists
   */
  public Table getTable(String name) {
    Table table = tableMap.get(name);

    if (table != null) {
      return table;
    }

    throw new IllegalStateException(
        String.format("Unable to locate a table with the name '%s'", name));
  }

  /**
   * Looks up a table by name without throwing if it does not exist.
   *
   * @param name the table name
   * @return the matching table, or an empty {@code Optional} if none exists
   */
  public Optional<Table> getOptionalTable(String name) {
    return Optional.ofNullable(tableMap.get(name));
  }

  /** Returns all views defined in the schema, regardless of database type. */
  public List<View> getViews() {
    return Collections.unmodifiableList(new ArrayList<>(views));
  }

  /**
   * Returns the views applicable to a given database type: for each distinct view name, prefers the
   * database-specific definition (matching {@code databaseType}) over a generic, database-agnostic
   * definition (declared with no {@code databaseType}).
   *
   * @param databaseType the database type to resolve views for
   * @return the resolved, de-duplicated list of views, in original declaration order
   */
  public List<View> getViews(DatabaseType databaseType) {
    Map<String, View> viewMap = new HashMap<>();

    views.stream()
        .filter(view -> view.getDatabaseType() == databaseType)
        .forEach(
            view -> {
              viewMap.put(view.getName().toLowerCase(), view);
            });

    views.stream()
        .filter(view -> view.getDatabaseType() == null)
        .forEach(
            view -> {
              String viewName = view.getName().toLowerCase();

              if (!viewMap.containsKey(viewName)) {
                viewMap.put(viewName, view);
              }
            });

    return Collections.unmodifiableList(
        views.stream()
            .map(view -> view.getName().toLowerCase())
            .distinct()
            .map(viewMap::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList()));
  }

  /** Returns all enum types defined in the schema. */
  public Collection<EnumType> getEnumTypes() {
    return enumTypes.values();
  }

  /**
   * Looks up an enum type by name.
   *
   * @param typeName the enum type's name
   * @return the matching enum type
   * @throws IllegalStateException if no enum type with the given name exists
   */
  public EnumType getEnumType(String typeName) {
    EnumType enumType = enumTypes.get(typeName);

    if (enumType != null) {
      return enumType;
    }

    throw new IllegalStateException(
        String.format("Unable to locate an enum type with name '%s'", typeName));
  }

  /**
   * Adds a table to the schema, indexing it by name for later lookup.
   *
   * @param table the table to add
   */
  public void addTable(Table table) {
    tables.add(table);
    tableMap.put(table.getName(), table);
  }

  /**
   * Adds a view to the schema.
   *
   * @param view the view to add
   */
  public void addView(View view) {
    views.add(view);
  }

  /**
   * Adds an enum type to the schema, indexing it by name for later lookup.
   *
   * @param enumType the enum type to add
   */
  public void addEnumType(EnumType enumType) {
    enumTypes.put(enumType.getName(), enumType);
  }

  /**
   * Adds a function to the schema.
   *
   * @param function the function to add
   */
  public void addFunction(Function function) {
    this.functions.add(function);
  }

  /** Returns all functions defined in the schema. */
  public List<Function> getFunctions() {
    return Collections.unmodifiableList(new ArrayList<>(functions));
  }

  /**
   * Adds a procedure to the schema.
   *
   * @param procedure the procedure to add
   */
  public void addProcedure(Procedure procedure) {
    this.procedures.add(procedure);
  }

  /** Returns all procedures defined in the schema. */
  public List<Procedure> getProcedures() {
    return Collections.unmodifiableList(new ArrayList<>(procedures));
  }

  /**
   * Adds a raw SQL fragment to the schema.
   *
   * @param otherSql the SQL fragment to add
   */
  public void addOtherSql(OtherSql otherSql) {
    this.otherSql.add(otherSql);
  }

  /** Returns all raw SQL fragments ({@code <otherSql>} elements) defined in the schema. */
  public List<OtherSql> getOtherSql() {
    return Collections.unmodifiableList(new ArrayList<>(otherSql));
  }

  /**
   * Validates cross-table integrity rules that cannot be checked while a single table is being
   * built. Currently checks that no {@link RelationType#SETNULL} relation targets a required
   * (non-nullable) column.
   *
   * @return a list of human-readable error messages; empty if the schema is valid
   */
  public List<String> validate() {
    List<String> errors = new ArrayList<String>();

    for (Table table : tables) {
      for (Relation relation : table.getRelations()) {
        if (relation.getType() == RelationType.SETNULL) {
          String fromTableName = relation.getFromTableName();
          String fromColumnName = relation.getFromColumnName();

          Table fromTable = getTable(fromTableName);

          if (fromTable.getColumn(fromColumnName).isRequired()) {
            errors.add(
                String.format(
                    "ERROR: %s.%s is required. The %s.%s relation specifies setnull, which is not"
                        + " allowed",
                    fromTableName,
                    fromColumnName,
                    relation.getToTableName(),
                    relation.getToColumnName()));
          }
        }
      }
    }

    return errors;
  }

  /** Sorts the schema's tables in place, alphabetically by table name. */
  public void sortTablesByName() {
    tables.sort(Comparator.comparing(Table::getName));
  }

  /**
   * Derives the reverse (parent-to-child) side of every relation and attaches it to the referenced
   * parent table's {@linkplain Table#getReverseRelations() reverse relations} list.
   */
  public void buildReverseRelations() {
    for (Table table : tables) {
      if (!table.getRelations().isEmpty()) {
        for (Relation relation : table.getRelations()) {
          String parentTableName = relation.getToTableName();
          Table parentTable = getTable(parentTableName);

          Relation reverseRelation =
              new Relation(
                  relation.getToTableName(),
                  relation.getToColumnName(),
                  relation.getFromTableName(),
                  relation.getFromColumnName(),
                  relation.getType(),
                  false);

          parentTable.getReverseRelations().add(reverseRelation);
        }
      }
    }
  }
}
