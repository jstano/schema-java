package com.stano.schema.gensql.impl.common;

import com.stano.schema.model.InitialData;
import com.stano.schema.model.Relation;
import com.stano.schema.model.Table;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TableGenerator extends BaseGenerator {
  private static final Logger LOGGER = LoggerFactory.getLogger(TableGenerator.class);

  protected TableGenerator(SQLGenerator sqlGenerator) {
    super(sqlGenerator);
  }

  public void outputTables() {
    List<Table> dropOrder = tablesInDropOrder();

    if (!dropOrder.isEmpty()) {
      for (Table table : dropOrder) {
        outputTableDrop(table);
      }
      sqlWriter.println();
    }

    schema.getTables().forEach(this::outputTable);
  }

  /**
   * Orders the schema's tables so that every table appears before any table it has a foreign key to
   * (children before parents), via a topological sort of the foreign-key relation graph.
   * Self-referencing relations are ignored (a single drop handles both sides); if the relation
   * graph contains a cycle, the tables involved fall back to their original declaration order.
   *
   * <p>All drops are then emitted up front (in this order) before any table is created, so
   * rerunning the generated script against a populated database doesn't fail attempting to drop a
   * parent table while a child still references it (H17).
   */
  protected List<Table> tablesInDropOrder() {
    List<Table> tables = schema.getTables();
    int tableCount = tables.size();

    Map<Table, Integer> indexOfTable = new IdentityHashMap<>();
    for (int i = 0; i < tableCount; i++) {
      indexOfTable.put(tables.get(i), i);
    }

    int[] inDegree = new int[tableCount];
    List<List<Integer>> successors = new ArrayList<>();
    for (int i = 0; i < tableCount; i++) {
      successors.add(new ArrayList<>());
    }

    for (int childIndex = 0; childIndex < tableCount; childIndex++) {
      for (Relation relation : tables.get(childIndex).getRelations()) {
        Table parentTable = schema.getOptionalTable(relation.getToTableName()).orElse(null);

        if (parentTable == null) {
          continue;
        }

        Integer parentIndex = indexOfTable.get(parentTable);

        if (parentIndex == null || parentIndex == childIndex) {
          continue; // unknown table, or a self-referencing FK - a single drop handles both sides
        }

        successors.get(childIndex).add(parentIndex);
        inDegree[parentIndex]++;
      }
    }

    Deque<Integer> queue = new ArrayDeque<>();
    for (int i = 0; i < tableCount; i++) {
      if (inDegree[i] == 0) {
        queue.addLast(i);
      }
    }

    List<Integer> order = new ArrayList<>(tableCount);
    while (!queue.isEmpty()) {
      int index = queue.removeFirst();
      order.add(index);

      for (int successor : successors.get(index)) {
        inDegree[successor]--;

        if (inDegree[successor] == 0) {
          queue.addLast(successor);
        }
      }
    }

    if (order.size() < tableCount) {
      Set<Integer> alreadyOrdered = new HashSet<>(order);
      for (int i = 0; i < tableCount; i++) {
        if (!alreadyOrdered.contains(i)) {
          order.add(i);
        }
      }
    }

    List<Table> result = new ArrayList<>(tableCount);
    for (int index : order) {
      result.add(tables.get(index));
    }

    return result;
  }

  protected abstract ColumnGenerator getColumnGenerator();

  protected abstract KeyGenerator getKeyGenerator();

  protected abstract ColumnConstraintGenerator getColumnConstraintGenerator();

  protected abstract TableConstraintGenerator getTableConstraintGenerator();

  protected abstract IndexGenerator getIndexGenerator();

  protected void outputTable(Table table) {
    LOGGER.debug("Generating SQL for table " + getFullyQualifiedTableName(table));

    outputTableHeader(table);
    outputTableDefinition(table);
    outputTableFooter(table);
    outputIndexes(table);
    outputInitialData(table);
  }

  /**
   * Emits the {@code drop table} statement for a table (and a header comment), run up front for
   * every table before any table is created. The default implementation is a no-op; a dialect
   * override should emit its {@code drop table} statement here instead of in {@link
   * #outputTableHeader}.
   */
  protected void outputTableDrop(Table table) {
    // no-op by default
  }

  protected void outputTableHeader(Table table) {
    sqlWriter.println(String.format("/* %s */", getFullyQualifiedTableName(table)));
    sqlWriter.println("create table " + getFullyQualifiedTableName(table));
    sqlWriter.println("(");
  }

  protected void outputTableDefinition(Table table) {
    List<String> tableDefinitions =
        Stream.of(
                getColumnGenerator().getColumnDefinitions(table),
                getKeyGenerator().getKeyConstraints(table),
                getColumnConstraintGenerator().getColumnCheckConstraints(table),
                getTableConstraintGenerator().getTableCheckConstraints(table))
            .flatMap(Collection::stream)
            .toList();

    for (int i = 0; i < tableDefinitions.size(); i++) {
      String sql = tableDefinitions.get(i);

      sqlWriter.print(sql);

      if (i < tableDefinitions.size() - 1) {
        sqlWriter.print(",");
      }

      sqlWriter.println();
    }
  }

  protected void outputTableFooter(Table table) {
    sqlWriter.println(")" + statementSeparator);
    sqlWriter.println();
  }

  protected void outputIndexes(Table table) {
    getIndexGenerator().outputIndexes(table);
  }

  protected void outputInitialData(Table table) {
    List<InitialData> initialDataList =
        table.getInitialData().stream()
            .filter(it -> it.getDatabaseType() == null || it.getDatabaseType() == databaseType)
            .toList();

    if (!initialDataList.isEmpty()) {
      initialDataList.forEach(
          initialData -> {
            sqlWriter.println(initialData.getSql() + statementSeparator);
          });

      sqlWriter.println();
    }
  }
}
