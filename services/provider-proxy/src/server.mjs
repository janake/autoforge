import http from "node:http";

const DEFAULT_PORT = 8080;
const DEFAULT_UPSTREAM = "https://openrouter.ai/api/v1";
const DEFAULT_MODEL = "deepseek/deepseek-chat";

export function readConfig(env = process.env) {
  const defaultModel = env.OPENROUTER_DEFAULT_MODEL || DEFAULT_MODEL;
  const allowedModels = (env.OPENROUTER_ALLOWED_MODELS || defaultModel)
    .split(",")
    .map((model) => model.trim())
    .filter(Boolean);

  return {
    port: Number(env.PROVIDER_PROXY_PORT || DEFAULT_PORT),
    apiKey: env.OPENROUTER_API_KEY || "",
    upstreamBaseUrl: (env.OPENROUTER_BASE_URL || DEFAULT_UPSTREAM).replace(/\/+$/, ""),
    defaultModel,
    allowedModels: new Set(allowedModels),
    siteUrl: env.OPENROUTER_SITE_URL || "",
    appName: env.OPENROUTER_APP_NAME || "Autoforge",
  };
}

function jsonResponse(response, statusCode, body) {
  response.writeHead(statusCode, {
    "content-type": "application/json; charset=utf-8",
    "cache-control": "no-store",
  });
  response.end(JSON.stringify(body));
}

function readBody(request) {
  return new Promise((resolve, reject) => {
    const chunks = [];
    request.on("data", (chunk) => chunks.push(chunk));
    request.on("end", () => resolve(Buffer.concat(chunks).toString("utf8")));
    request.on("error", reject);
  });
}

function normalizeModel(body, config) {
  const requestedModel = typeof body.model === "string" && body.model.trim()
    ? body.model.trim()
    : config.defaultModel;

  if (!config.allowedModels.has(requestedModel)) {
    const error = new Error(`Model '${requestedModel}' is not allowed.`);
    error.statusCode = 400;
    throw error;
  }

  return { ...body, model: requestedModel };
}

async function forwardToOpenRouter(body, config) {
  const headers = {
    authorization: `Bearer ${config.apiKey}`,
    accept: "application/json",
    "content-type": "application/json",
  };

  if (config.siteUrl) {
    headers["http-referer"] = config.siteUrl;
  }

  if (config.appName) {
    headers["x-title"] = config.appName;
  }

  return fetch(`${config.upstreamBaseUrl}/chat/completions`, {
    method: "POST",
    headers,
    body: JSON.stringify(body),
  });
}

export function createServer(config = readConfig()) {
  return http.createServer(async (request, response) => {
    try {
      if (request.method === "GET" && request.url === "/health") {
        jsonResponse(response, 200, {
          healthy: true,
          provider: "openrouter",
          allowedModels: [...config.allowedModels],
        });
        return;
      }

      if (request.method === "GET" && request.url === "/v1/models") {
        jsonResponse(response, 200, {
          object: "list",
          data: [...config.allowedModels].map((model) => ({ id: model, object: "model" })),
        });
        return;
      }

      if (request.method !== "POST" || request.url !== "/v1/chat/completions") {
        jsonResponse(response, 404, { error: { message: "Not found" } });
        return;
      }

      if (!config.apiKey) {
        jsonResponse(response, 503, { error: { message: "OpenRouter API key is not configured." } });
        return;
      }

      const rawBody = await readBody(request);
      const parsedBody = rawBody ? JSON.parse(rawBody) : {};
      const proxiedBody = normalizeModel(parsedBody, config);
      const upstream = await forwardToOpenRouter(proxiedBody, config);

      response.writeHead(upstream.status, {
        "content-type": upstream.headers.get("content-type") || "application/json; charset=utf-8",
        "cache-control": "no-store",
      });

      if (upstream.body) {
        for await (const chunk of upstream.body) {
          response.write(chunk);
        }
      }
      response.end();
    } catch (error) {
      const statusCode = error.statusCode || 500;
      jsonResponse(response, statusCode, { error: { message: error.message || "Provider proxy error." } });
    }
  });
}

if (import.meta.url === `file://${process.argv[1]}`) {
  const config = readConfig();
  createServer(config).listen(config.port, "0.0.0.0", () => {
    console.log(`OpenRouter provider proxy listening on ${config.port}`);
  });
}
