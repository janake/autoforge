package org.autoforge.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.autoforge.backend.config.BackendMvpProperties;
import org.autoforge.backend.domain.LearningContentGenerationType;
import org.autoforge.backend.dto.LearningContentSourceReference;
import org.springframework.stereotype.Component;

@Component
public class HttpLearningAiEngineProvider implements LearningAiEngineProvider {

  private static final String FALLBACK_REASON_UNCONFIGURED = "AI engine is not configured.";
  private static final String SYSTEM_PROMPT = """
    You are Autoforge's learning content generator.
    Generate teaching content in Hungarian based on the provided material.
    """.trim();

  private final ProviderProxyChatClient chatClient;
  private final ObjectMapper objectMapper;

  public HttpLearningAiEngineProvider(BackendMvpProperties properties, ObjectMapper objectMapper) {
    this.chatClient = new ProviderProxyChatClient(properties, objectMapper);
    this.objectMapper = objectMapper;
  }

  @Override
  public LearningGenerationResult generate(GenerationContext context) {
    Objects.requireNonNull(context, "context");
    if (!chatClient.isConfigured()) {
      return new LearningGenerationResult(null, null, List.of(), true, FALLBACK_REASON_UNCONFIGURED);
    }

    try {
      String responseText = chatClient.complete(SYSTEM_PROMPT, prompt(context), "Provider proxy learning generation failed");
      return parseResponse(responseText);
    } catch (Exception exception) {
      String reason = exception.getMessage() != null ? exception.getMessage() : exception.getClass().getSimpleName();
      return new LearningGenerationResult(null, null, List.of(), true, reason);
    }
  }

  private LearningGenerationResult parseResponse(String responseText) {
    String jsonText = extractJsonObject(responseText);
    try {
      JsonNode response = objectMapper.readTree(jsonText);
      String content = textValue(response, "content");
      String structuredContent = textValue(response, "structuredContent");
      List<LearningContentSourceReference> sources = parseSources(response.get("sources"));

      if (content == null || content.isBlank()) {
        throw new ProviderProxyClientException("AI response missing content");
      }

      return new LearningGenerationResult(content, structuredContent, sources, false, null);
    } catch (IOException exception) {
      throw new ProviderProxyClientException("Failed to parse AI response", exception);
    }
  }

  private String prompt(GenerationContext context) {
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
    } else if (context.generationType() == LearningContentGenerationType.LESSON) {
      text.append("""

        Generate a teacher-ready lesson in Hungarian based on the material.
        Return a single JSON object with keys:
        - "content": a human-readable lesson with title, sections, key concepts, practice goals, and short recap
        - "structuredContent": null
        - "sources": an array of { "chunkIndex": 0, "excerpt": "..." } referencing key points

        Rules:
        - Return JSON only, no markdown fences.
        - Lesson must be in Hungarian.
        - Keep it concise enough to review before publishing.
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

    return text.toString();
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
