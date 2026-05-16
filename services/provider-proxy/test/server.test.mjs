import assert from "node:assert/strict";
import { after, before, test } from "node:test";
import http from "node:http";
import { createServer, readConfig } from "../src/server.mjs";

function listen(server) {
  return new Promise((resolve) => {
    server.listen(0, "127.0.0.1", () => resolve(server.address().port));
  });
}

function close(server) {
  return new Promise((resolve, reject) => server.close((error) => error ? reject(error) : resolve()));
}

let upstreamAuthorization = "";
let upstreamBody = null;
let upstreamServer;
let upstreamPort;

before(async () => {
  upstreamServer = http.createServer(async (request, response) => {
    upstreamAuthorization = request.headers.authorization || "";
    const chunks = [];
    for await (const chunk of request) chunks.push(chunk);
    upstreamBody = JSON.parse(Buffer.concat(chunks).toString("utf8"));
    response.writeHead(200, { "content-type": "application/json" });
    response.end(JSON.stringify({ id: "chatcmpl-test", choices: [] }));
  });
  upstreamPort = await listen(upstreamServer);
});

after(async () => {
  await close(upstreamServer);
});

test("forwards allowed chat completion requests with the proxy-held API key", async () => {
  const proxy = createServer(readConfig({
    OPENROUTER_API_KEY: "real-openrouter-key",
    OPENROUTER_BASE_URL: `http://127.0.0.1:${upstreamPort}/v1`,
    OPENROUTER_DEFAULT_MODEL: "deepseek/deepseek-chat",
    OPENROUTER_ALLOWED_MODELS: "deepseek/deepseek-chat",
  }));
  const proxyPort = await listen(proxy);

  try {
    const response = await fetch(`http://127.0.0.1:${proxyPort}/v1/chat/completions`, {
      method: "POST",
      headers: {
        authorization: "Bearer dummy-opencode-token",
        "content-type": "application/json",
      },
      body: JSON.stringify({ messages: [{ role: "user", content: "hello" }] }),
    });

    assert.equal(response.status, 200);
    assert.equal(upstreamAuthorization, "Bearer real-openrouter-key");
    assert.equal(upstreamBody.model, "deepseek/deepseek-chat");
  } finally {
    await close(proxy);
  }
});

test("rejects models outside the allowlist", async () => {
  const proxy = createServer(readConfig({
    OPENROUTER_API_KEY: "real-openrouter-key",
    OPENROUTER_BASE_URL: `http://127.0.0.1:${upstreamPort}/v1`,
    OPENROUTER_DEFAULT_MODEL: "deepseek/deepseek-chat",
    OPENROUTER_ALLOWED_MODELS: "deepseek/deepseek-chat",
  }));
  const proxyPort = await listen(proxy);

  try {
    const response = await fetch(`http://127.0.0.1:${proxyPort}/v1/chat/completions`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ model: "openai/gpt-4.1", messages: [] }),
    });

    assert.equal(response.status, 400);
    const body = await response.json();
    assert.match(body.error.message, /not allowed/);
  } finally {
    await close(proxy);
  }
});
