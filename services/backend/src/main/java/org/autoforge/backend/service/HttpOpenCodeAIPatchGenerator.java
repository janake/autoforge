package org.autoforge.backend.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import org.autoforge.backend.config.BackendMvpProperties;
import org.autoforge.backend.domain.Job;
import org.autoforge.backend.dto.GeneratedPatchResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class HttpOpenCodeAIPatchGenerator implements AIPatchGenerator {

  private final HttpClient httpClient;
  private final BackendMvpProperties.Opencode opencode;
  private final ObjectMapper objectMapper;

  public HttpOpenCodeAIPatchGenerator(BackendMvpProperties properties, ObjectMapper objectMapper) {
    this.httpClient = HttpClient.newHttpClient();
    this.opencode = properties.opencode();
    this.objectMapper = objectMapper;
  }

  @Override
  public GeneratedPatchResponse generatePatch(Job job) {
    Objects.requireNonNull(job, "job");
    ensureConfigured();

    String sessionId = createSession(job);
    String responseText = sendMessage(sessionId, job);
    return parseGeneratedPatch(responseText);
  }

  private String createSession(Job job) {
    HttpRequest request = HttpRequest.newBuilder()
      .uri(URI.create(baseUrl() + "/session"))
      .header("Authorization", basicAuth())
      .header("Accept", "application/json")
      .header("Content-Type", "application/json")
      .POST(HttpRequest.BodyPublishers.ofString(sessionPayload(job), StandardCharsets.UTF_8))
      .build();

    JsonNode response = sendJson(request, "OpenCode session creation failed");
    String sessionId = textValue(response, "id");
    if (sessionId == null || sessionId.isBlank()) {
      throw new OpenCodeClientException("OpenCode session creation response missing id");
    }
    return sessionId;
  }

  private String sendMessage(String sessionId, Job job) {
    HttpRequest request = HttpRequest.newBuilder()
      .uri(URI.create(baseUrl() + "/session/" + sessionId + "/message"))
      .header("Authorization", basicAuth())
      .header("Accept", "application/json")
      .header("Content-Type", "application/json")
      .POST(HttpRequest.BodyPublishers.ofString(messageBody(job), StandardCharsets.UTF_8))
      .build();

    JsonNode response = sendJson(request, "OpenCode message request failed");
    String responseText = extractMessageText(response);
    if (responseText == null || responseText.isBlank()) {
      throw new OpenCodeClientException("OpenCode response did not include assistant text");
    }
    return responseText;
  }

  private GeneratedPatchResponse parseGeneratedPatch(String responseText) {
    String jsonText = extractJsonObject(responseText);
    try {
      JsonNode response = objectMapper.readTree(jsonText);
      String patch = textValue(response, "patch");
      String summary = textValue(response, "summary");
      List<String> changedFiles = stringList(response.get("changedFiles"));

      if (patch == null || patch.isBlank()) {
        throw new OpenCodeClientException("OpenCode response missing patch");
      }
      if (summary == null || summary.isBlank()) {
        throw new OpenCodeClientException("OpenCode response missing summary");
      }

      return new GeneratedPatchResponse(patch, summary, changedFiles);
    } catch (IOException exception) {
      throw new OpenCodeClientException("Failed to parse OpenCode response", exception);
    }
  }

  private JsonNode sendJson(HttpRequest request, String errorMessage) {
    try {
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new OpenCodeClientException(errorMessage + ": " + response.statusCode() + " " + response.body());
      }

      return objectMapper.readTree(response.body());
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new OpenCodeClientException(errorMessage + " (interrupted)", exception);
    } catch (IOException exception) {
      throw new OpenCodeClientException(errorMessage, exception);
    }
  }

  private void ensureConfigured() {
    if (opencode == null) {
      throw new OpenCodeClientException("Missing opencode configuration");
    }
    if (opencode.serverUrl() == null || opencode.serverUrl().isBlank()) {
      throw new OpenCodeClientException("Missing autoforge.mvp.opencode.server-url configuration");
    }
    if (opencode.username() == null || opencode.username().isBlank()) {
      throw new OpenCodeClientException("Missing autoforge.mvp.opencode.username configuration");
    }
    if (opencode.password() == null || opencode.password().isBlank()) {
      throw new OpenCodeClientException("Missing autoforge.mvp.opencode.password configuration");
    }
    if (opencode.model() == null || opencode.model().isBlank()) {
      throw new OpenCodeClientException("Missing autoforge.mvp.opencode.model configuration");
    }
  }

  private String baseUrl() {
    return opencode.serverUrl().replaceAll("/+$", "");
  }

  private String basicAuth() {
    String credentials = opencode.username() + ":" + opencode.password();
    return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
  }

  private String sessionTitle(Job job) {
    String reference = job.getJiraIssueKey() == null || job.getJiraIssueKey().isBlank() ? job.getId() : job.getJiraIssueKey();
    return "Autoforge %s".formatted(reference);
  }

  private String messageBody(Job job) {
    String prompt = """
      You are Autoforge's code generator.
      Return a single JSON object with these keys only:
      - patch: a unified diff
      - summary: a short change summary
      - changedFiles: an array of modified file paths

      Rules:
      - Output JSON only.
      - Do not use markdown or code fences.
      - Do not mention secrets, API keys, or credentials.
      - Keep the patch focused on the requested task.

      Repository: %s
      Base branch: %s
      Jira issue: %s

      User request:
      %s
      """.formatted(
      job.getTargetRepository(),
      job.getBaseBranch(),
      job.getJiraIssueKey() == null || job.getJiraIssueKey().isBlank() ? "none" : job.getJiraIssueKey(),
      job.getPrompt()
    );

    try {
      return objectMapper.writeValueAsString(java.util.Map.of(
        "model", opencode.model(),
        "system", "Return JSON only.",
        "parts", List.of(java.util.Map.of("type", "text", "text", prompt))
      ));
    } catch (IOException exception) {
      throw new OpenCodeClientException("Failed to build OpenCode request payload", exception);
    }
  }

  private String sessionPayload(Job job) {
    try {
      return objectMapper.writeValueAsString(java.util.Map.of("title", sessionTitle(job)));
    } catch (IOException exception) {
      throw new OpenCodeClientException("Failed to build OpenCode session payload", exception);
    }
  }

  private String textValue(JsonNode node, String fieldName) {
    JsonNode value = node == null ? null : node.get(fieldName);
    return value == null || value.isNull() ? null : value.asText();
  }

  private List<String> stringList(JsonNode node) {
    if (node == null || !node.isArray()) {
      return List.of();
    }

    List<String> values = new ArrayList<>();
    for (JsonNode element : node) {
      if (element != null && !element.isNull()) {
        values.add(element.asText());
      }
    }
    return List.copyOf(values);
  }

  private String extractMessageText(JsonNode response) {
    if (response == null) {
      return null;
    }

    StringBuilder text = new StringBuilder();
    JsonNode parts = response.get("parts");
    if (parts != null && parts.isArray()) {
      for (JsonNode part : parts) {
        String partText = textValue(part, "text");
        if (partText != null) {
          text.append(partText);
        }
      }
    }

    if (text.length() > 0) {
      return text.toString();
    }

    String directText = textValue(response, "text");
    if (directText != null) {
      return directText;
    }

    JsonNode info = response.get("info");
    if (info != null) {
      String infoText = textValue(info, "text");
      if (infoText != null) {
        return infoText;
      }
    }

    return null;
  }

  private String extractJsonObject(String value) {
    String trimmed = value == null ? "" : value.trim();
    if (trimmed.startsWith("```")) {
      int firstNewline = trimmed.indexOf('\n');
      int lastFence = trimmed.lastIndexOf("```");
      if (firstNewline >= 0 && lastFence > firstNewline) {
        trimmed = trimmed.substring(firstNewline + 1, lastFence).trim();
      }
    }

    int start = trimmed.indexOf('{');
    int end = trimmed.lastIndexOf('}');
    if (start >= 0 && end > start) {
      return trimmed.substring(start, end + 1);
    }

    return trimmed;
  }
}
