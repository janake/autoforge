package org.autoforge.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.autoforge.backend.config.BackendMvpProperties;
import org.autoforge.backend.domain.Job;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class HttpProviderProxyAIPatchGeneratorTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static HttpServer server;
  private static int port;
  private static final AtomicReference<String> chatPath = new AtomicReference<>();
  private static final AtomicReference<String> chatBody = new AtomicReference<>();

  @BeforeAll
  static void startServer() throws IOException {
    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext("/v1/chat/completions", HttpProviderProxyAIPatchGeneratorTest::handleChatCompletion);
    server.start();
    port = server.getAddress().getPort();
  }

  @AfterAll
  static void stopServer() {
    server.stop(0);
  }

  @Test
  void callsProviderProxyAndParsesGeneratedPatchJson() throws Exception {
    HttpProviderProxyAIPatchGenerator generator = new HttpProviderProxyAIPatchGenerator(
      new BackendMvpProperties(null, new BackendMvpProperties.ProviderProxy(
        "http://127.0.0.1:%d/v1".formatted(port),
        "google/gemma-4-26b-a4b-it:free"
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

    assertThat(chatPath.get()).isEqualTo("/v1/chat/completions");
    assertThat(chatBody.get()).contains("google/gemma-4-26b-a4b-it:free");
    assertThat(chatBody.get()).contains("Return JSON only.");
    assertThat(chatBody.get()).contains("Process queued job");
  }

  private static void handleChatCompletion(HttpExchange exchange) throws IOException {
    chatPath.set(exchange.getRequestURI().getPath());
    chatBody.set(readBody(exchange));

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
      "choices",
      List.of(Map.of("message", Map.of("content", assistantText)))
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

}
