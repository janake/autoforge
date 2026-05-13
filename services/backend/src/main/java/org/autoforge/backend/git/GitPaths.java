package org.autoforge.backend.git;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class GitPaths {

  private GitPaths() {
  }

  static void deleteRecursively(Path root) {
    if (root == null || !Files.exists(root)) {
      return;
    }

    try (var paths = Files.walk(root)) {
      paths.sorted((left, right) -> right.compareTo(left)).forEach(path -> {
        try {
          Files.deleteIfExists(path);
        } catch (IOException exception) {
          throw new GitBrokerException("Failed to delete git workspace: " + path, exception);
        }
      });
    } catch (IOException exception) {
      throw new GitBrokerException("Failed to traverse git workspace", exception);
    }
  }
}
