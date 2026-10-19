package com.stano.schema.gendiagram;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("GenDiagram CLI")
class GenDiagramTest {

  @Test
  void runRejectsUnknownFormat() {
    var x =
        assertThrows(
            IllegalArgumentException.class,
            () -> GenDiagram.run(new String[] {"BOGUS", "schema.xml"}));

    assertTrue(x.getMessage().contains("BOGUS"));
  }

  @Test
  void runRejectsSchemaFilenameWithoutExtension() {
    var x =
        assertThrows(
            IllegalArgumentException.class,
            () -> GenDiagram.run(new String[] {"MERMAID", "schema"}));

    assertTrue(x.getMessage().contains("extension"));
  }

  @Test
  void runThrowsWhenSchemaFileIsMissing(@TempDir Path tempDir) {
    assertThrows(
        Exception.class,
        () -> GenDiagram.run(new String[] {"MERMAID", tempDir.resolve("missing.xml").toString()}));
  }
}
