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
import java.util.concurrent.atomic.AtomicReference;
import org.autoforge.backend.config.BackendMvpProperties;
import org.autoforge.backend.domain.Job;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class HttpOpenCodeAIPatchGeneratorTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static HttpServer server;
  private static int port;
  private static final AtomicReference<String> sessionAuth = new AtomicReference<>();
  private static final AtomicReference<String> sessionBody = new AtomicReference<>();
  private static final AtomicReference<String> messageAuth = new AtomicReference<>();
  private static final AtomicReference<String> messageBody = new AtomicReference<>();

  @BeforeAll
  static void startServer() throws IOException {
    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext("/session", HttpOpenCodeAIPatchGeneratorTest::handleSession);
    server.createContext("/session/session-123/message", HttpOpenCodeAIPatchGeneratorTest::handleMessage);
    server.start();
    port = server.getAddress().getPort();
  }

  @AfterAll
  static void stopServer() {
    server.stop(0);
  }

  @Test
  void createsSessionAndParsesGeneratedPatchJson() throws Exception {
    HttpOpenCodeAIPatchGenerator generator = new HttpOpenCodeAIPatchGenerator(
      new BackendMvpProperties(null, new BackendMvpProperties.Opencode(
        "http://127.0.0.1:%d".formatted(port),
        "opencode",
        "secret",
        "autoforge-openrouter/qwen/qwen3.6-plus",
        null
      )),
      new ObjectMapper()
    );

    var response = generator.generatePatch(Job.createQueued(
      "AUTO-321",
      "Process queued job",
      "janake/autoforge",
      "main"
    ));

    assertThat(response.summary()).isEqualTo("Add processed line");
    assertThat(response.changedFiles()).containsExactly("README.md");
    assertThat(response.patch()).contains("diff --git a/README.md b/README.md");
    assertThat(response.patch()).contains("+processed");

    assertThat(sessionAuth.get()).isEqualTo(basicAuth("opencode", "secret"));
    assertThat(messageAuth.get()).isEqualTo(basicAuth("opencode", "secret"));
    assertThat(sessionBody.get()).contains("Autoforge AUTO-321");
    assertThat(messageBody.get()).contains("autoforge-openrouter/qwen/qwen3.6-plus");
    assertThat(messageBody.get()).contains("Return JSON only.");
    assertThat(messageBody.get()).contains("Process queued job");
  }

  private static void handleSession(HttpExchange exchange) throws IOException {
    sessionAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
    sessionBody.set(readBody(exchange));
    writeJson(exchange, 200, OBJECT_MAPPER.writeValueAsString(java.util.Map.of("id", "session-123")));
  }

  private static void handleMessage(HttpExchange exchange) throws IOException {
    messageAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
    messageBody.set(readBody(exchange));

    String patchJson = OBJECT_MAPPER.writeValueAsString(java.util.Map.of(
      "patch",
      "diff --git a/README.md b/README.md\n--- a/README.md\n+++ b/README.md\n@@ -1 +1,2 @@\n initial\n+processed\n",
      "summary",
      "Add processed line",
      "changedFiles",
      java.util.List.of("README.md")
    ));
    String assistantText = "```json\n" + patchJson + "\n```";

    writeJson(exchange, 200, OBJECT_MAPPER.writeValueAsString(java.util.Map.of(
      "parts",
      java.util.List.of(java.util.Map.of("type", "text", "text", assistantText))
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
