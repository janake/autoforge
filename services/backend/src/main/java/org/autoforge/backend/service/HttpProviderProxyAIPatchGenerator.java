package org.autoforge.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.autoforge.backend.config.BackendMvpProperties;
import org.autoforge.backend.domain.Job;
import org.autoforge.backend.dto.GeneratedPatchResponse;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class HttpProviderProxyAIPatchGenerator implements AIPatchGenerator {

  private final ProviderProxyChatClient chatClient;
  private final ObjectMapper objectMapper;

  public HttpProviderProxyAIPatchGenerator(BackendMvpProperties properties, ObjectMapper objectMapper) {
    this.chatClient = new ProviderProxyChatClient(properties, objectMapper);
    this.objectMapper = objectMapper;
  }

  @Override
  public GeneratedPatchResponse generatePatch(Job job) {
    Objects.requireNonNull(job, "job");
    String responseText = chatClient.complete("Return JSON only.", prompt(job), "Provider proxy patch generation failed");
    return parseGeneratedPatch(responseText);
  }

  private String prompt(Job job) {
    return """
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
  }

  private GeneratedPatchResponse parseGeneratedPatch(String responseText) {
    String jsonText = extractJsonObject(responseText);
    try {
      JsonNode response = objectMapper.readTree(jsonText);
      String patch = textValue(response, "patch");
      String summary = textValue(response, "summary");
      List<String> changedFiles = stringList(response.get("changedFiles"));

      if (patch == null || patch.isBlank()) {
        throw new ProviderProxyClientException("Provider proxy response missing patch");
      }
      if (summary == null || summary.isBlank()) {
        throw new ProviderProxyClientException("Provider proxy response missing summary");
      }

      return new GeneratedPatchResponse(patch, summary, changedFiles);
    } catch (IOException exception) {
      throw new ProviderProxyClientException("Failed to parse provider proxy patch response", exception);
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
