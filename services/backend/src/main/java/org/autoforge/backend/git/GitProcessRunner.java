package org.autoforge.backend.git;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

final class GitProcessRunner {

  private GitProcessRunner() {
  }

  static GitProcessResult run(Path workingDirectory, String... command) {
    ProcessBuilder processBuilder = new ProcessBuilder(command);
    if (workingDirectory != null) {
      processBuilder.directory(workingDirectory.toFile());
    }
    processBuilder.redirectErrorStream(true);

    try {
      Process process = processBuilder.start();
      String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      int exitCode = process.waitFor();

      if (exitCode != 0) {
        throw new GitBrokerException("Git command failed: " + String.join(" ", command) + "\n" + output.trim());
      }

      return new GitProcessResult(output.trim(), exitCode);
    } catch (IOException exception) {
      throw new GitBrokerException("Failed to run git command: " + String.join(" ", command), exception);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new GitBrokerException("Git command interrupted: " + String.join(" ", command), exception);
    }
  }

  record GitProcessResult(String output, int exitCode) {
  }
}
