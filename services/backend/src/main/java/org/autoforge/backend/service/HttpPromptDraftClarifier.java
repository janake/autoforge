package org.autoforge.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.autoforge.backend.config.BackendMvpProperties;
import org.springframework.stereotype.Component;

@Component
public class HttpPromptDraftClarifier implements PromptDraftClarifier {

  private static final String SYSTEM_PROMPT = """
    You are Autoforge's prompt intake assistant.
    Decide whether the user's implementation request is ready for approval.
    If details are missing, ask concise clarification questions.
    Return JSON only.
    """.trim();

  private final ProviderProxyChatClient chatClient;
  private final ObjectMapper objectMapper;

  public HttpPromptDraftClarifier(BackendMvpProperties properties, ObjectMapper objectMapper) {
    this.chatClient = new ProviderProxyChatClient(properties, objectMapper);
    this.objectMapper = objectMapper;
  }

  @Override
  public PromptDraftClarificationResult clarify(PromptDraftClarificationRequest request) {
    Objects.requireNonNull(request, "request");

    try {
      String responseText = chatClient.complete(SYSTEM_PROMPT, prompt(request), "Provider proxy prompt-draft clarification failed");
      return parseResponse(responseText);
    } catch (ProviderProxyClientException exception) {
      throw new PromptDraftAiUnavailableException(exception.getMessage(), exception);
    } catch (PromptDraftAiUnavailableException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new PromptDraftAiUnavailableException("Prompt draft AI clarification failed", exception);
    }
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
          throw new PromptDraftAiUnavailableException("Provider proxy prompt-draft response missing clarification questions");
        }
      }

      return new PromptDraftClarificationResult(readyForApproval, questions, message);
    } catch (IOException exception) {
      throw new PromptDraftAiUnavailableException("Failed to parse provider proxy prompt-draft clarification response", exception);
    }
  }

  private String prompt(PromptDraftClarificationRequest request) {
    return """
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
