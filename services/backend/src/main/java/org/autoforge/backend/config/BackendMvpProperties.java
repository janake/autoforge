package org.autoforge.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "autoforge.mvp")
public record BackendMvpProperties(
  Github github,
  ProviderProxy providerProxy
) {

  public record Github(
    String token,
    String owner,
    String repo,
    String baseBranch
  ) {
  }

  public record ProviderProxy(
    String baseUrl,
    String model
  ) {
  }
}
