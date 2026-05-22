package org.autoforge.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.autoforge.backend.config.BackendMvpProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class HttpPromptDraftClarifierTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final AtomicReference<String> sessionAuth = new AtomicReference<>();
  private static final AtomicReference<String> sessionBody = new AtomicReference<>();
  private static final AtomicReference<String> messageAuth = new AtomicReference<>();
  private static final AtomicReference<String> messageBody = new AtomicReference<>();
  private static HttpServer server;
  private static int port;

  @BeforeAll
  static void startServer() throws IOException {
    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext("/session", HttpPromptDraftClarifierTest::handleSession);
    server.createContext("/session/session-clarify/message", HttpPromptDraftClarifierTest::handleMessage);
    server.start();
    port = server.getAddress().getPort();
  }

  @AfterAll
  static void stopServer() {
    server.stop(0);
  }

  @Test
  void createsOpenCodeSessionAndParsesClarificationResponse() {
    HttpPromptDraftClarifier clarifier = new HttpPromptDraftClarifier(
      new BackendMvpProperties(null, new BackendMvpProperties.Opencode(
        "http://127.0.0.1:%d".formatted(port),
        "opencode",
        "secret",
        "autoforge-openrouter/google/gemma-4-26b-a4b-it:free",
        "test-api-key"
      )),
      new ObjectMapper()
    );

    PromptDraftClarificationResult result = clarifier.clarify(new PromptDraftClarificationRequest("Audit the workflow"));

    assertThat(result.readyForApproval()).isFalse();
    assertThat(result.questions()).containsExactly("Which repository should this apply to?");
    assertThat(result.message()).isEqualTo("Which repository should this apply to?");
    assertThat(sessionAuth.get()).isEqualTo(basicAuth("opencode", "secret"));
    assertThat(messageAuth.get()).isEqualTo(basicAuth("opencode", "secret"));
    assertThat(sessionBody.get()).contains("Autoforge prompt draft clarification");
    assertThat(sessionBody.get()).contains("test-api-key");
    assertThat(messageBody.get()).contains("autoforge-openrouter/google/gemma-4-26b-a4b-it:free");
    assertThat(messageBody.get()).contains("readyForApproval");
    assertThat(messageBody.get()).contains("Audit the workflow");
  }

  private static void handleSession(HttpExchange exchange) throws IOException {
    sessionAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
    sessionBody.set(readBody(exchange));
    writeJson(exchange, 200, OBJECT_MAPPER.writeValueAsString(Map.of("id", "session-clarify")));
  }

  private static void handleMessage(HttpExchange exchange) throws IOException {
    messageAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
    messageBody.set(readBody(exchange));
    String assistantText = OBJECT_MAPPER.writeValueAsString(Map.of(
      "readyForApproval", false,
      "questions", List.of("Which repository should this apply to?"),
      "message", "Which repository should this apply to?"
    ));
    writeJson(exchange, 200, OBJECT_MAPPER.writeValueAsString(Map.of(
      "parts", List.of(Map.of("type", "text", "text", assistantText))
    )));
  }

  private static String readBody(HttpExchange exchange) throws IOException {
    return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
  }

  private static void writeJson(HttpExchange exchange, int statusCode, String body) throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().add("Content-Type", "application/json");
    exchange.sendResponseHeaders(statusCode, bytes.length);
    try (OutputStream output = exchange.getResponseBody()) {
      output.write(bytes);
    }
  }

  private static String basicAuth(String username, String password) {
    String credentials = username + ":" + password;
    return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
  }
}
