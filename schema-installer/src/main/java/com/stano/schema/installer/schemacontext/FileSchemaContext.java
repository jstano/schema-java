package com.stano.schema.installer.schemacontext;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Concrete {@link DefaultSchemaContext} that reads a schema definition from a local file on disk.
 *
 * <p>Unlike {@link DefaultSchemaContext}, which checks for a {@code databaseupgradelog} table to
 * decide whether a schema is already installed, this context always reports the schema as not yet
 * installed, so {@code SchemaInstaller.installSchema} always (re)installs it.
 */
public class FileSchemaContext extends DefaultSchemaContext {
  /**
   * Creates a context that installs the schema defined in the given file.
   *
   * @param schemaFile the local file containing the XML schema definition
   * @throws IllegalArgumentException if the file's location cannot be converted to a URL
   */
  public FileSchemaContext(File schemaFile) {
    super(toUrl(schemaFile), null);
  }

  /**
   * {@inheritDoc}
   *
   * @param connection unused
   * @return always {@code false}
   */
  @Override
  public boolean schemaIsInstalled(Connection connection) throws SQLException {
    return false;
  }

  private static URL toUrl(File schemaFile) {
    try {
      return schemaFile.toURI().toURL();
    } catch (MalformedURLException x) {
      throw new IllegalArgumentException(x);
    }
  }
}
