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

public class GitSchemaReader {
  private static final Logger log = LoggerFactory.getLogger(GitSchemaReader.class);

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
