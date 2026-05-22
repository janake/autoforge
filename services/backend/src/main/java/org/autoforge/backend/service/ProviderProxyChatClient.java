package org.autoforge.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.autoforge.backend.config.BackendMvpProperties;

final class ProviderProxyChatClient {

  private final HttpClient httpClient;
  private final BackendMvpProperties.ProviderProxy providerProxy;
  private final ObjectMapper objectMapper;

  ProviderProxyChatClient(BackendMvpProperties properties, ObjectMapper objectMapper) {
    this.httpClient = HttpClient.newHttpClient();
    this.providerProxy = properties.providerProxy();
    this.objectMapper = objectMapper;
  }

  boolean isConfigured() {
    return providerProxy != null
      && providerProxy.baseUrl() != null
      && !providerProxy.baseUrl().isBlank()
      && (providerProxy.baseUrl().startsWith("http://") || providerProxy.baseUrl().startsWith("https://"))
      && providerProxy.model() != null
      && !providerProxy.model().isBlank();
  }

  String complete(String systemPrompt, String userPrompt, String errorMessage) {
    ensureConfigured();
    HttpRequest request = HttpRequest.newBuilder()
      .uri(URI.create(baseUrl() + "/chat/completions"))
      .header("Accept", "application/json")
      .header("Content-Type", "application/json")
      .POST(HttpRequest.BodyPublishers.ofString(chatPayload(systemPrompt, userPrompt), StandardCharsets.UTF_8))
      .build();

    JsonNode response = sendJson(request, errorMessage);
    String content = extractMessageContent(response);
    if (content == null || content.isBlank()) {
      throw new ProviderProxyClientException(errorMessage + ": provider response did not include assistant content");
    }
    return content;
  }

  private void ensureConfigured() {
    if (providerProxy == null) {
      throw new ProviderProxyClientException("AI provider proxy is unavailable because backend has no provider-proxy configuration");
    }
    if (providerProxy.baseUrl() == null || providerProxy.baseUrl().isBlank()) {
      throw new ProviderProxyClientException("AI provider proxy is unavailable because AI_PROVIDER_PROXY_BASE_URL is not configured");
    }
    if (!providerProxy.baseUrl().startsWith("http://") && !providerProxy.baseUrl().startsWith("https://")) {
      throw new ProviderProxyClientException("AI provider proxy is unavailable because AI_PROVIDER_PROXY_BASE_URL is invalid");
    }
    if (providerProxy.model() == null || providerProxy.model().isBlank()) {
      throw new ProviderProxyClientException("AI provider proxy is unavailable because AI_PROVIDER_MODEL is not configured");
    }
  }

  private String chatPayload(String systemPrompt, String userPrompt) {
    try {
      return objectMapper.writeValueAsString(Map.of(
        "model", providerProxy.model(),
        "messages", List.of(
          Map.of("role", "system", "content", systemPrompt),
          Map.of("role", "user", "content", userPrompt)
        )
      ));
    } catch (IOException exception) {
      throw new ProviderProxyClientException("Failed to build provider proxy chat payload", exception);
    }
  }

  private JsonNode sendJson(HttpRequest request, String errorMessage) {
    try {
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new ProviderProxyClientException(errorMessage + ": provider proxy returned HTTP " + response.statusCode());
      }
      return objectMapper.readTree(response.body());
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new ProviderProxyClientException(errorMessage + " (interrupted)", exception);
    } catch (IOException exception) {
      throw new ProviderProxyClientException(errorMessage, exception);
    }
  }

  private String extractMessageContent(JsonNode response) {
    JsonNode choices = response == null ? null : response.get("choices");
    if (choices != null && choices.isArray() && !choices.isEmpty()) {
      JsonNode message = choices.get(0).get("message");
      JsonNode content = message == null ? null : message.get("content");
      if (content != null && !content.isNull()) {
        return content.asText();
      }
    }
    JsonNode directContent = response == null ? null : response.get("content");
    return directContent == null || directContent.isNull() ? null : directContent.asText();
  }

  private String baseUrl() {
    return providerProxy.baseUrl().replaceAll("/+$", "");
  }
}
