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
    assert.equal(upstreamBody.max_tokens, 2048);
  } finally {
    await close(proxy);
  }
});

test("reports non-secret health and guardrail configuration", async () => {
  const proxy = createServer(readConfig({
    OPENROUTER_API_KEY: "real-openrouter-key",
    OPENROUTER_DEFAULT_MODEL: "deepseek/deepseek-chat",
    OPENROUTER_ALLOWED_MODELS: "deepseek/deepseek-chat",
    OPENROUTER_MAX_COMPLETION_TOKENS: "512",
    OPENROUTER_MAX_REQUEST_BYTES: "4096",
  }));
  const proxyPort = await listen(proxy);

  try {
    const response = await fetch(`http://127.0.0.1:${proxyPort}/health`);
    const body = await response.json();

    assert.equal(response.status, 200);
    assert.equal(body.healthy, true);
    assert.equal(body.apiKeyConfigured, true);
    assert.deepEqual(body.allowedModels, ["deepseek/deepseek-chat"]);
    assert.equal(body.maxCompletionTokens, 512);
    assert.equal(body.maxRequestBytes, 4096);
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

test("rejects requests above the max token limit", async () => {
  const proxy = createServer(readConfig({
    OPENROUTER_API_KEY: "real-openrouter-key",
    OPENROUTER_BASE_URL: `http://127.0.0.1:${upstreamPort}/v1`,
    OPENROUTER_DEFAULT_MODEL: "deepseek/deepseek-chat",
    OPENROUTER_ALLOWED_MODELS: "deepseek/deepseek-chat",
    OPENROUTER_MAX_COMPLETION_TOKENS: "128",
  }));
  const proxyPort = await listen(proxy);

  try {
    const response = await fetch(`http://127.0.0.1:${proxyPort}/v1/chat/completions`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ messages: [], max_tokens: 129 }),
    });

    assert.equal(response.status, 400);
    const body = await response.json();
    assert.match(body.error.message, /max_tokens exceeds/);
  } finally {
    await close(proxy);
  }
});

test("rejects invalid max token values", async () => {
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
      body: JSON.stringify({ messages: [], max_tokens: 0 }),
    });

    assert.equal(response.status, 400);
    const body = await response.json();
    assert.match(body.error.message, /positive integer/);
  } finally {
    await close(proxy);
  }
});

test("rejects request bodies above the byte limit", async () => {
  const proxy = createServer(readConfig({
    OPENROUTER_API_KEY: "real-openrouter-key",
    OPENROUTER_BASE_URL: `http://127.0.0.1:${upstreamPort}/v1`,
    OPENROUTER_DEFAULT_MODEL: "deepseek/deepseek-chat",
    OPENROUTER_ALLOWED_MODELS: "deepseek/deepseek-chat",
    OPENROUTER_MAX_REQUEST_BYTES: "32",
  }));
  const proxyPort = await listen(proxy);

  try {
    const response = await fetch(`http://127.0.0.1:${proxyPort}/v1/chat/completions`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ messages: [{ role: "user", content: "this request is too large" }] }),
    });

    assert.equal(response.status, 413);
    const body = await response.json();
    assert.match(body.error.message, /exceeds 32 bytes/);
  } finally {
    await close(proxy);
  }
});
