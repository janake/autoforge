package org.autoforge.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "autoforge.mvp")
public record BackendMvpProperties(
  Github github
) {

  public record Github(
    String token,
    String owner,
    String repo,
    String baseBranch
  ) {
  }
}
