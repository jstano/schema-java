package com.stano.schema.gensql.impl.common;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stano.schema.model.BooleanMode;
import com.stano.schema.model.Column;
import com.stano.schema.model.ColumnType;
import com.stano.schema.model.DatabaseType;
import com.stano.schema.model.ForeignKeyMode;
import com.stano.schema.model.Schema;
import com.stano.schema.model.Table;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SQLGenerator with an invalid schema")
class InvalidSchemaTest {

  @Test
  @DisplayName("generate throws with the validation errors instead of writing an empty file")
  void generateThrowsWhenSchemaIsInvalid() throws Exception {
    Schema schema = new Schema(java.net.URI.create("file:///schema.xml").toURL());
    Table t = new Table(null, "app", "t", null, null, false);
    t.getColumns().add(new Column("a", ColumnType.SEQUENCE, 0, true));
    t.getColumns().add(new Column("b", ColumnType.SEQUENCE, 0, true));
    schema.addTable(t);

    StringWriter sw = new StringWriter();
    SQLGenerator generator =
        new SQLGeneratorFactory()
            .createSQLGenerator(
                new SQLGeneratorOptions(
                    schema,
                    new PrintWriter(sw),
                    DatabaseType.POSTGRESQL,
                    ForeignKeyMode.RELATIONS,
                    BooleanMode.NATIVE,
                    OutputMode.ALL));

    IllegalArgumentException x = assertThrows(IllegalArgumentException.class, generator::generate);

    assertTrue(x.getMessage().contains("more than one identity column"), x.getMessage());
    assertTrue(sw.toString().isEmpty());
  }
}
