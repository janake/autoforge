package org.autoforge.backend.jira;

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
public class HttpJiraIssueClient implements JiraIssueClient {

  private final HttpClient httpClient;
  private final String baseUrl;
  private final String email;
  private final String token;

  public HttpJiraIssueClient(
    @Value("${autoforge.mvp.jira.base-url:}") String baseUrl,
    @Value("${autoforge.mvp.jira.email:}") String email,
    @Value("${autoforge.mvp.jira.api-token:}") String token
  ) {
    this.httpClient = HttpClient.newHttpClient();
    this.baseUrl = baseUrl;
    this.email = email;
    this.token = token;
  }

  @Override
  public CreateJiraIssueResponse createIssue(CreateJiraIssueRequest request) {
    ensureConfigured();

    String payload = """
      {
        "fields": {
          "project": { "key": "%s" },
          "issuetype": { "name": "%s" },
          "summary": "%s",
          "description": "%s",
          "labels": %s
        }
      }
      """.formatted(
      escapeJson(request.projectKey()),
      escapeJson(request.issueType()),
      escapeJson(request.summary()),
      escapeJson(request.description() == null ? "" : request.description()),
      toJsonArray(request.labels())
    );

    HttpRequest httpRequest = HttpRequest.newBuilder()
      .uri(URI.create(baseUrl.replaceAll("/+$", "") + "/rest/api/3/issue"))
      .header("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString((email + ":" + token).getBytes(StandardCharsets.UTF_8)))
      .header("Accept", "application/json")
      .header("Content-Type", "application/json")
      .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
      .build();

    try {
      HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new JiraIssueClientException("Jira issue creation failed: " + response.statusCode() + " " + response.body());
      }

      String issueKey = extractJsonString(response.body(), "key");
      String issueUrl = extractJsonString(response.body(), "self");
      if (issueKey == null || issueKey.isBlank()) {
        throw new JiraIssueClientException("Jira issue creation response missing key");
      }

      return new CreateJiraIssueResponse(issueKey, issueUrl == null ? baseUrl.replaceAll("/+$", "") + "/browse/" + issueKey : issueUrl);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new JiraIssueClientException("Jira issue creation interrupted", exception);
    } catch (IOException exception) {
      throw new JiraIssueClientException("Jira issue creation failed", exception);
    }
  }

  private void ensureConfigured() {
    if (baseUrl == null || baseUrl.isBlank()) {
      throw new JiraIssueClientException("Missing autoforge.mvp.jira.base-url configuration");
    }
    if (email == null || email.isBlank()) {
      throw new JiraIssueClientException("Missing autoforge.mvp.jira.email configuration");
    }
    if (token == null || token.isBlank()) {
      throw new JiraIssueClientException("Missing autoforge.mvp.jira.api-token configuration");
    }
  }

  private String toJsonArray(java.util.List<String> values) {
    if (values == null || values.isEmpty()) {
      return "[]";
    }
    return values.stream().map(this::escapeJson).map(value -> "\"" + value + "\"").reduce((left, right) -> left + "," + right).map(valuesString -> "[" + valuesString + "]").orElse("[]");
  }

  private String escapeJson(String value) {
    return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
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
