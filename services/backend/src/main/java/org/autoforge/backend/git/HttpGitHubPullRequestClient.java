package org.autoforge.backend.git;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class HttpGitHubPullRequestClient implements GitHubPullRequestClient {

  private final HttpClient httpClient;
  private final GitHubRepositoryCoordinates repositoryCoordinates;
  private final String githubToken;

  public HttpGitHubPullRequestClient(
    GitHubRepositoryCoordinates repositoryCoordinates,
    @Value("${github.token:}") String githubToken
  ) {
    this.httpClient = HttpClient.newHttpClient();
    this.repositoryCoordinates = repositoryCoordinates;
    this.githubToken = githubToken;
  }

  @Override
  public GitHubPullRequestResponse createPullRequest(CreatePullRequestRequest request) {
    ensureToken();

    String payload = """
      {
        "title": "%s",
        "head": "%s",
        "base": "%s",
        "body": "%s"
      }
      """.formatted(
      escapeJson(request.pullRequestTitle()),
      escapeJson(request.branchName()),
      escapeJson(request.baseBranch()),
      escapeJson(request.pullRequestBody() == null ? "" : request.pullRequestBody())
    );

    HttpRequest httpRequest = HttpRequest.newBuilder()
      .uri(URI.create("https://api.github.com/repos/%s/%s/pulls".formatted(repositoryCoordinates.owner(), repositoryCoordinates.repository())))
      .header("Authorization", "Bearer " + githubToken)
      .header("Accept", "application/vnd.github+json")
      .header("Content-Type", "application/json")
      .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
      .build();

    try {
      HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new GitBrokerException("GitHub PR creation failed: " + response.statusCode() + " " + response.body());
      }

      String prUrl = extractJsonString(response.body(), "html_url");
      if (prUrl == null || prUrl.isBlank()) {
        throw new GitBrokerException("GitHub PR creation response missing html_url");
      }

      return new GitHubPullRequestResponse(prUrl);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new GitBrokerException("GitHub PR creation interrupted", exception);
    } catch (IOException exception) {
      throw new GitBrokerException("GitHub PR creation failed", exception);
    }
  }

  @Override
  public void pushBranch(CreatePullRequestRequest request, GitRepositoryPreparationService.GitRepositoryWorkspace workspace) {
    ensureToken();
    GitProcessRunner.run(
      workspace.repositoryDirectory(),
      "git",
      "push",
      "origin",
      "HEAD:" + request.branchName()
    );
  }

  private void ensureToken() {
    if (githubToken == null || githubToken.isBlank()) {
      throw new GitBrokerException("Missing GitHub token configuration");
    }
  }

  private String escapeJson(String value) {
    return value.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  private String extractJsonString(String body, String key) {
    String token = "\"" + key + "\":";
    int index = body.indexOf(token);
    if (index < 0) {
      return null;
    }

    int start = body.indexOf('"', index + token.length());
    int end = body.indexOf('"', start + 1);
    if (start < 0 || end < 0) {
      return null;
    }

    return body.substring(start + 1, end);
  }
}
