package org.autoforge.backend.git;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GitHubRepositoryCoordinates {

  private final String owner;
  private final String repository;

  public GitHubRepositoryCoordinates(
    @Value("${autoforge.mvp.github.owner}") String owner,
    @Value("${autoforge.mvp.github.repo}") String repository
  ) {
    this.owner = owner;
    this.repository = repository;
  }

  public String owner() {
    return owner;
  }

  public String repository() {
    return repository;
  }
}
