package org.autoforge.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SecurityBaselineTest {

  @Test
  void backendDockerfileRunsAsNonRoot() throws Exception {
    String dockerfile = Files.readString(Path.of("Dockerfile"));

    assertThat(dockerfile).contains("USER 1000");
  }

  @Test
  void composeFilesDoNotMountDockerSocket() throws Exception {
    String compose = Files.readString(Path.of("..", "..", "docker-compose.yml"));
    String localCompose = Files.readString(Path.of("..", "..", "infra", "compose", "docker-compose.local.yml"));

    assertThat(compose).doesNotContain("docker.sock");
    assertThat(localCompose).doesNotContain("docker.sock");
  }
}
