package com.stano.schema.git;

import com.stano.schema.model.Schema;
import com.stano.schema.parser.SchemaParser;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads both the git-committed and current working-tree versions of a schema XML file, parsing each
 * into a {@link Schema} so they can be diffed.
 *
 * <p>The enclosing git repository is located by walking up from the schema file's parent directory
 * (via JGit's {@code FileRepositoryBuilder}), so the file need not be at the repository root. The
 * committed version is read from the blob stored in the {@code HEAD} commit's tree, and the current
 * version is read directly from the file on disk.
 */
public class GitSchemaReader {
  private static final Logger log = LoggerFactory.getLogger(GitSchemaReader.class);

  /**
   * Reads the {@code HEAD}-committed and current working-tree versions of the given schema file and
   * parses each into a {@link Schema}.
   *
   * @param schemaFilePath the path to the schema XML file, which must live inside a git working
   *     tree
   * @return a {@link SchemaVersions} holding the committed schema and the current schema
   * @throws IOException if the git repository cannot be opened, the committed or current schema
   *     file cannot be read, or either schema fails to parse
   * @throws IllegalStateException if the repository has no commits yet, or if the schema file has
   *     never been committed (i.e. is not present in the {@code HEAD} tree)
   */
  public SchemaVersions readSchemas(Path schemaFilePath) throws IOException {
    var absolutePath = schemaFilePath.toAbsolutePath().normalize();

    try (var repository = openRepository(absolutePath)) {
      String relativePath = relativize(repository, absolutePath);
      log.debug("Reading schema versions for {} (git path: {})", absolutePath, relativePath);

      Schema committedSchema = readCommittedSchema(repository, relativePath, absolutePath);
      Schema currentSchema = readCurrentSchema(absolutePath);

      return new SchemaVersions(committedSchema, currentSchema);
    }
  }

  private Repository openRepository(Path absolutePath) throws IOException {
    return new FileRepositoryBuilder()
        .findGitDir(absolutePath.getParent().toFile())
        .readEnvironment()
        .build();
  }

  private String relativize(Repository repository, Path absolutePath) {
    var workTree = repository.getWorkTree().toPath().toAbsolutePath().normalize();
    return workTree.relativize(absolutePath).toString().replace('\\', '/');
  }

  private Schema readCommittedSchema(Repository repository, String relativePath, Path absolutePath)
      throws IOException {
    ObjectId headId = repository.resolve(Constants.HEAD);
    if (headId == null) {
      throw new IllegalStateException(
          "Repository has no commits yet; cannot read committed schema for: " + absolutePath);
    }

    try (var revWalk = new RevWalk(repository)) {
      RevCommit headCommit = revWalk.parseCommit(headId);

      try (var treeWalk = TreeWalk.forPath(repository, relativePath, headCommit.getTree())) {
        if (treeWalk == null) {
          throw new IllegalStateException(
              "Schema file has not been committed yet: " + relativePath);
        }

        ObjectLoader loader = repository.open(treeWalk.getObjectId(0));
        URL schemaURL = absolutePath.toUri().toURL();

        try (InputStream inputStream = loader.openStream()) {
          return new SchemaParser().parseSchema(schemaURL, inputStream);
        }
      }
    }
  }

  private Schema readCurrentSchema(Path absolutePath) throws IOException {
    return new SchemaParser().parseSchema(absolutePath.toUri().toURL());
  }
}
