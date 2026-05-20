package org.autoforge.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "autoforge.mvp")
public record BackendMvpProperties(
  Github github,
  Opencode opencode
) {

  public record Github(
    String token,
    String owner,
    String repo,
    String baseBranch
  ) {
  }

  public record Opencode(
    String serverUrl,
    String username,
    String password,
    String model,
    String apiKey
  ) {
  }
}
