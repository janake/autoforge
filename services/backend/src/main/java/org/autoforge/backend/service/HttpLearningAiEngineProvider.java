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
import org.autoforge.backend.domain.LearningContentGenerationType;
import org.autoforge.backend.dto.LearningContentSourceReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class HttpLearningAiEngineProvider implements LearningAiEngineProvider {

  private static final String FALLBACK_REASON_UNCONFIGURED = "AI engine is not configured.";
  private static final String SYSTEM_PROMPT = """
    You are Autoforge's learning content generator.
    Generate teaching content in Hungarian based on the provided material.
    """.trim();

  private final HttpClient httpClient;
  private final BackendMvpProperties.Opencode opencode;
  private final ObjectMapper objectMapper;

  public HttpLearningAiEngineProvider(BackendMvpProperties properties, ObjectMapper objectMapper) {
    this.httpClient = HttpClient.newHttpClient();
    this.opencode = properties.opencode();
    this.objectMapper = objectMapper;
  }

  @Override
  public LearningGenerationResult generate(GenerationContext context) {
    Objects.requireNonNull(context, "context");
    if (!isConfigured()) {
      return new LearningGenerationResult(null, null, List.of(), true, FALLBACK_REASON_UNCONFIGURED);
    }

    try {
      String sessionId = createSession(context);
      String responseText = sendPrompt(sessionId, context);
      return parseResponse(responseText, context);
    } catch (Exception exception) {
      String reason = exception.getMessage() != null ? exception.getMessage() : exception.getClass().getSimpleName();
      return new LearningGenerationResult(null, null, List.of(), true, reason);
    }
  }

  private boolean isConfigured() {
    if (opencode == null) return false;
    String url = opencode.serverUrl();
    if (url == null || url.isBlank()) return false;
    if (!url.startsWith("http://") && !url.startsWith("https://")) return false;
    if (opencode.username() == null || opencode.username().isBlank()) return false;
    if (opencode.password() == null || opencode.password().isBlank()) return false;
    if (opencode.model() == null || opencode.model().isBlank()) return false;
    return true;
  }

  private String createSession(GenerationContext context) {
    HttpRequest request = HttpRequest.newBuilder()
      .uri(URI.create(baseUrl() + "/session"))
      .header("Authorization", basicAuth())
      .header("Accept", "application/json")
      .header("Content-Type", "application/json")
      .POST(HttpRequest.BodyPublishers.ofString(sessionPayload(context), StandardCharsets.UTF_8))
      .build();

    JsonNode response = sendJson(request, "OpenCode session creation failed");
    String sessionId = textValue(response, "id");
    if (sessionId == null || sessionId.isBlank()) {
      throw new OpenCodeClientException("OpenCode session creation response missing id");
    }
    return sessionId;
  }

  private String sendPrompt(String sessionId, GenerationContext context) {
    HttpRequest request = HttpRequest.newBuilder()
      .uri(URI.create(baseUrl() + "/session/" + sessionId + "/message"))
      .header("Authorization", basicAuth())
      .header("Accept", "application/json")
      .header("Content-Type", "application/json")
      .POST(HttpRequest.BodyPublishers.ofString(messageBody(context), StandardCharsets.UTF_8))
      .build();

    JsonNode response = sendJson(request, "OpenCode learning generation failed");
    String responseText = extractMessageText(response);
    if (responseText == null || responseText.isBlank()) {
      throw new OpenCodeClientException("OpenCode response did not include assistant text");
    }
    return responseText;
  }

  private LearningGenerationResult parseResponse(String responseText, GenerationContext context) {
    String jsonText = extractJsonObject(responseText);
    try {
      JsonNode response = objectMapper.readTree(jsonText);
      String content = textValue(response, "content");
      String structuredContent = textValue(response, "structuredContent");
      List<LearningContentSourceReference> sources = parseSources(response.get("sources"));

      if (content == null || content.isBlank()) {
        throw new OpenCodeClientException("AI response missing content");
      }

      return new LearningGenerationResult(content, structuredContent, sources, false, null);
    } catch (IOException exception) {
      throw new OpenCodeClientException("Failed to parse AI response", exception);
    }
  }

  private String sessionPayload(GenerationContext context) {
    String title = "Learning %s for %s".formatted(
      context.generationType() == LearningContentGenerationType.QUESTION_SET ? "questions" : "summary",
      context.title()
    );
    var fields = new java.util.LinkedHashMap<String, Object>();
    fields.put("title", title.length() > 200 ? title.substring(0, 200) : title);
    String apiKey = opencode.apiKey();
    if (apiKey != null && !apiKey.isBlank()) {
      fields.put("apiKey", apiKey);
    }
    try {
      return objectMapper.writeValueAsString(fields);
    } catch (IOException exception) {
      throw new OpenCodeClientException("Failed to build session payload", exception);
    }
  }

  private String messageBody(GenerationContext context) {
    StringBuilder text = new StringBuilder();
    text.append("Learning material:\n");
    text.append("Title: ").append(context.title()).append("\n");
    if (context.description() != null && !context.description().isBlank()) {
      text.append("Description: ").append(context.description()).append("\n");
    }
    text.append("\nContent:\n").append(context.materialText()).append("\n");
    if (context.optimizedImageUrl() != null) {
      text.append("\nReference image: ").append(context.optimizedImageUrl()).append("\n");
    }
    text.append("\nLearner context: ").append(context.retrievalContext()).append("\n");

    if (context.generationType() == LearningContentGenerationType.QUESTION_SET) {
      text.append("""

        Generate 3-5 multiple-choice questions in Hungarian based on the material.
        Return a single JSON object with keys:
        - "content": a human-readable text showing each question, its options, the correct answer, and an explanation
        - "structuredContent": the same data as a JSON object with fields: materialId, materialTitle, retrievalContext, questions (array)
        - "sources": an array of { "chunkIndex": 0, "excerpt": "..." } referencing key concepts

        Rules:
        - Return JSON only, no markdown fences.
        - Questions must be in Hungarian.
        - Each question must have 4 options.
        """);
    } else {
      text.append("""

        Generate a concise summary in Hungarian based on the material.
        Return a single JSON object with keys:
        - "content": a human-readable summary text in Hungarian
        - "structuredContent": null
        - "sources": an array of { "chunkIndex": 0, "excerpt": "..." } referencing key points

        Rules:
        - Return JSON only, no markdown fences.
        - Summary must be in Hungarian.
        """);
    }

    try {
      return objectMapper.writeValueAsString(java.util.Map.of(
        "model", opencode.model(),
        "system", SYSTEM_PROMPT,
        "parts", List.of(java.util.Map.of("type", "text", "text", text.toString()))
      ));
    } catch (IOException exception) {
      throw new OpenCodeClientException("Failed to build message payload", exception);
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

  private List<LearningContentSourceReference> parseSources(JsonNode sourcesNode) {
    if (sourcesNode == null || !sourcesNode.isArray()) {
      return List.of();
    }
    List<LearningContentSourceReference> result = new ArrayList<>();
    for (JsonNode element : sourcesNode) {
      if (element == null || element.isNull()) continue;
      Integer chunkIndex = element.has("chunkIndex") && !element.get("chunkIndex").isNull() ? element.get("chunkIndex").asInt() : null;
      String excerpt = textValue(element, "excerpt");
      if (chunkIndex != null && excerpt != null) {
        result.add(new LearningContentSourceReference(chunkIndex, excerpt));
      }
    }
    return List.copyOf(result);
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
