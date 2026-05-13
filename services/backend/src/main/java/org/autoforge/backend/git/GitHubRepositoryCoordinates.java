package org.autoforge.backend.git;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GitHubRepositoryCoordinates {

  private final String owner;
  private final String repository;

  public GitHubRepositoryCoordinates(
    @Value("${github.owner:janake}") String owner,
    @Value("${github.repository:autoforge}") String repository
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
