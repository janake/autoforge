package org.autoforge.backend.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.autoforge.backend.config.BackendMvpProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class HttpPromptDraftClarifier implements PromptDraftClarifier {

  private static final String SYSTEM_PROMPT = """
    You are Autoforge's prompt intake assistant.
    Decide whether the user's implementation request is ready for approval.
    If details are missing, ask concise clarification questions.
    Return JSON only.
    """.trim();

  private final HttpClient httpClient;
  private final BackendMvpProperties.Opencode opencode;
  private final ObjectMapper objectMapper;

  public HttpPromptDraftClarifier(BackendMvpProperties properties, ObjectMapper objectMapper) {
    this.httpClient = HttpClient.newHttpClient();
    this.opencode = properties.opencode();
    this.objectMapper = objectMapper;
  }

  @Override
  public PromptDraftClarificationResult clarify(PromptDraftClarificationRequest request) {
    Objects.requireNonNull(request, "request");
    ensureConfigured();

    try {
      String sessionId = createSession();
      String responseText = sendPrompt(sessionId, request);
      return parseResponse(responseText);
    } catch (PromptDraftAiUnavailableException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new PromptDraftAiUnavailableException("Prompt draft AI clarification failed", exception);
    }
  }

  private String createSession() {
    HttpRequest request = HttpRequest.newBuilder()
      .uri(URI.create(baseUrl() + "/session"))
      .header("Authorization", basicAuth())
      .header("Accept", "application/json")
      .header("Content-Type", "application/json")
      .POST(HttpRequest.BodyPublishers.ofString(sessionPayload(), StandardCharsets.UTF_8))
      .build();

    JsonNode response = sendJson(request, "OpenCode prompt-draft session creation failed");
    String sessionId = textValue(response, "id");
    if (sessionId == null || sessionId.isBlank()) {
      throw new PromptDraftAiUnavailableException("OpenCode prompt-draft session response missing id");
    }
    return sessionId;
  }

  private String sendPrompt(String sessionId, PromptDraftClarificationRequest request) {
    HttpRequest requestMessage = HttpRequest.newBuilder()
      .uri(URI.create(baseUrl() + "/session/" + sessionId + "/message"))
      .header("Authorization", basicAuth())
      .header("Accept", "application/json")
      .header("Content-Type", "application/json")
      .POST(HttpRequest.BodyPublishers.ofString(messageBody(request), StandardCharsets.UTF_8))
      .build();

    JsonNode response = sendJson(requestMessage, "OpenCode prompt-draft clarification failed");
    String responseText = extractMessageText(response);
    if (responseText == null || responseText.isBlank()) {
      throw new PromptDraftAiUnavailableException("OpenCode prompt-draft response did not include assistant text");
    }
    return responseText;
  }

  private PromptDraftClarificationResult parseResponse(String responseText) {
    try {
      JsonNode response = objectMapper.readTree(extractJsonObject(responseText));
      boolean readyForApproval = response.path("readyForApproval").asBoolean(false);
      String message = textValue(response, "message");
      List<String> questions = stringList(response.get("questions"));

      if (!readyForApproval && questions.isEmpty()) {
        if (message != null && !message.isBlank()) {
          questions = List.of(message.trim());
        } else {
          throw new PromptDraftAiUnavailableException("OpenCode prompt-draft response missing clarification questions");
        }
      }

      return new PromptDraftClarificationResult(readyForApproval, questions, message);
    } catch (IOException exception) {
      throw new PromptDraftAiUnavailableException("Failed to parse OpenCode prompt-draft clarification response", exception);
    }
  }

  private String sessionPayload() {
    Map<String, Object> fields = new LinkedHashMap<>();
    fields.put("title", "Autoforge prompt draft clarification");
    String apiKey = opencode.apiKey();
    if (apiKey != null && !apiKey.isBlank()) {
      fields.put("apiKey", apiKey);
    }
    try {
      return objectMapper.writeValueAsString(fields);
    } catch (IOException exception) {
      throw new PromptDraftAiUnavailableException("Failed to build OpenCode prompt-draft session payload", exception);
    }
  }

  private String messageBody(PromptDraftClarificationRequest request) {
    String prompt = """
      Conversation so far:
      %s

      Return one JSON object with exactly these keys:
      - readyForApproval: boolean
      - questions: array of strings, empty only when readyForApproval is true
      - message: concise assistant text to show the user

      Approval criteria:
      - The repository or target system is clear.
      - The intended change is clear enough to create a Jira issue.
      - Success or acceptance criteria are clear enough for implementation.
      """.formatted(Objects.requireNonNullElse(request.conversationText(), "").trim());

    try {
      return objectMapper.writeValueAsString(Map.of(
        "model", opencode.model(),
        "system", SYSTEM_PROMPT,
        "parts", List.of(Map.of("type", "text", "text", prompt))
      ));
    } catch (IOException exception) {
      throw new PromptDraftAiUnavailableException("Failed to build OpenCode prompt-draft message payload", exception);
    }
  }

  private JsonNode sendJson(HttpRequest request, String errorMessage) {
    try {
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new PromptDraftAiUnavailableException(errorMessage + ": " + response.statusCode() + " " + response.body());
      }
      return objectMapper.readTree(response.body());
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new PromptDraftAiUnavailableException(errorMessage + " (interrupted)", exception);
    } catch (IOException exception) {
      throw new PromptDraftAiUnavailableException(errorMessage, exception);
    }
  }

  private void ensureConfigured() {
    if (opencode == null) {
      throw new PromptDraftAiUnavailableException("Prompt draft AI is unavailable because the backend has no opencode configuration");
    }
    if (opencode.serverUrl() == null || opencode.serverUrl().isBlank()) {
      throw new PromptDraftAiUnavailableException("Prompt draft AI is unavailable because OPENCODE_SERVER_URL is not configured");
    }
    if (!opencode.serverUrl().startsWith("http://") && !opencode.serverUrl().startsWith("https://")) {
      throw new PromptDraftAiUnavailableException("Prompt draft AI is unavailable because OPENCODE_SERVER_URL is invalid");
    }
    if (opencode.username() == null || opencode.username().isBlank()) {
      throw new PromptDraftAiUnavailableException("Prompt draft AI is unavailable because OPENCODE_SERVER_USERNAME is not configured");
    }
    if (opencode.password() == null || opencode.password().isBlank()) {
      throw new PromptDraftAiUnavailableException("Prompt draft AI is unavailable because OPENCODE_SERVER_PASSWORD is not configured");
    }
    if (opencode.model() == null || opencode.model().isBlank()) {
      throw new PromptDraftAiUnavailableException("Prompt draft AI is unavailable because OPENCODE_MODEL is not configured");
    }
  }

  private String baseUrl() {
    return opencode.serverUrl().replaceAll("/+$", "");
  }

  private String basicAuth() {
    String credentials = opencode.username() + ":" + opencode.password();
    return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
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
      if (element != null && !element.isNull() && !element.asText().isBlank()) {
        values.add(element.asText());
      }
    }
    return List.copyOf(values);
  }

  private String extractMessageText(JsonNode response) {
    if (response == null) return null;
    StringBuilder text = new StringBuilder();
    JsonNode parts = response.get("parts");
    if (parts != null && parts.isArray()) {
      for (JsonNode part : parts) {
        String partText = textValue(part, "text");
        if (partText != null) text.append(partText);
      }
    }
    if (text.length() > 0) return text.toString();
    String directText = textValue(response, "text");
    if (directText != null) return directText;
    JsonNode info = response.get("info");
    if (info != null) {
      String infoText = textValue(info, "text");
      if (infoText != null) return infoText;
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
