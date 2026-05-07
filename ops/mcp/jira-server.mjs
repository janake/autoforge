#!/usr/bin/env node
import { execFileSync } from "node:child_process";
import readline from "node:readline";

const tools = [
  {
    name: "jira_search",
    description: "Search for Jira issues using JQL.",
    inputSchema: {
      type: "object",
      properties: {
        jql: { type: "string", description: "JQL query, for example: project = AUTO" },
        maxResults: { type: "number", description: "Maximum result count. Default: 50" },
      },
      required: ["jql"],
    },
  },
  {
    name: "jira_get_issue",
    description: "Get details of a specific Jira issue by key.",
    inputSchema: {
      type: "object",
      properties: {
        issueKey: { type: "string", description: "Jira issue key, for example AUTO-5" },
      },
      required: ["issueKey"],
    },
  },
  {
    name: "jira_update_issue",
    description: "Update fields of a Jira issue using Jira REST API v2 field names.",
    inputSchema: {
      type: "object",
      properties: {
        issueKey: { type: "string", description: "Jira issue key, for example AUTO-5" },
        fields: { type: "object", description: "Jira fields payload, for example { summary: 'New title' }" },
      },
      required: ["issueKey", "fields"],
    },
  },
];

function getSecret(name) {
  if (process.env[name]) return process.env[name];

  const ocid = process.env[`OCI_${name}_SECRET_OCID`];
  if (ocid) return getOciSecretValue(ocid);

  const raw = execFileSync("oci", [
    "search",
    "resource",
    "free-text-search",
    "--text",
    name,
    "--output",
    "json",
  ], { encoding: "utf8", stdio: ["ignore", "pipe", "pipe"] });

  const matches = (JSON.parse(raw).data?.items || [])
    .filter((item) => item["resource-type"] === "VaultSecret")
    .filter((item) => item["display-name"] === name)
    .filter((item) => item["lifecycle-state"] === "ACTIVE");

  if (matches.length !== 1) {
    throw new Error(`Expected exactly one ACTIVE OCI Vault secret named ${name}, found ${matches.length}`);
  }

  return getOciSecretValue(matches[0].identifier);
}

function getOciSecretValue(secretId) {
  const encoded = execFileSync("oci", [
    "secrets",
    "secret-bundle",
    "get",
    "--secret-id",
    secretId,
    "--query",
    'data."secret-bundle-content".content',
    "--raw-output",
  ], { encoding: "utf8", stdio: ["ignore", "pipe", "pipe"] }).trim();

  return Buffer.from(encoded, "base64").toString("utf8").trim();
}

function jiraConfig() {
  const baseUrl = getSecret("JIRA_BASE_URL").replace(/\/+$/, "");
  const email = getSecret("JIRA_EMAIL");
  const token = getSecret("JIRA_API_TOKEN");
  const auth = Buffer.from(`${email}:${token}`).toString("base64");

  return { baseUrl, auth };
}

async function jiraRequest(method, apiPath, body = null) {
  const { baseUrl, auth } = jiraConfig();
  const response = await fetch(`${baseUrl}${apiPath}`, {
    method,
    headers: {
      Authorization: `Basic ${auth}`,
      Accept: "application/json",
      "Content-Type": "application/json",
    },
    body: body ? JSON.stringify(body) : undefined,
  });

  const text = await response.text();
  const data = text ? JSON.parse(text) : null;
  if (!response.ok) {
    const message = [
      ...(data?.errorMessages || []),
      ...(data?.errors ? Object.entries(data.errors).map(([field, error]) => `${field}: ${error}`) : []),
      response.statusText,
    ].filter(Boolean).join("; ");
    throw new Error(`Jira ${method} ${apiPath} failed (${response.status}): ${message}`);
  }

  return data;
}

function send(id, result, error = null) {
  const message = error
    ? { jsonrpc: "2.0", id, error: { code: -32000, message: error.message || String(error) } }
    : { jsonrpc: "2.0", id, result };

  process.stdout.write(`${JSON.stringify(message)}\n`);
}

function textResult(text) {
  return { content: [{ type: "text", text }] };
}

async function callTool(name, args = {}) {
  if (name === "jira_search") {
    const maxResults = args.maxResults || 50;
    const data = await jiraRequest("GET", `/rest/api/2/search?jql=${encodeURIComponent(args.jql)}&maxResults=${maxResults}`);
    const issues = (data.issues || []).map((issue) => ({
      key: issue.key,
      summary: issue.fields.summary,
      status: issue.fields.status?.name,
    }));
    return textResult(JSON.stringify(issues, null, 2));
  }

  if (name === "jira_get_issue") {
    const data = await jiraRequest("GET", `/rest/api/2/issue/${encodeURIComponent(args.issueKey)}`);
    return textResult(JSON.stringify({ key: data.key, fields: data.fields }, null, 2));
  }

  if (name === "jira_update_issue") {
    await jiraRequest("PUT", `/rest/api/2/issue/${encodeURIComponent(args.issueKey)}`, { fields: args.fields });
    return textResult(`Issue ${args.issueKey} updated successfully.`);
  }

  throw new Error(`Unknown tool: ${name}`);
}

async function handle(request) {
  if (!request.id && request.method?.startsWith("notifications/")) return;

  try {
    if (request.method === "initialize") {
      send(request.id, {
        protocolVersion: request.params?.protocolVersion || "2024-11-05",
        capabilities: { tools: {} },
        serverInfo: { name: "autoforge-jira-mcp", version: "0.1.0" },
      });
      return;
    }

    if (request.method === "tools/list") {
      send(request.id, { tools });
      return;
    }

    if (request.method === "tools/call") {
      send(request.id, await callTool(request.params?.name, request.params?.arguments || {}));
      return;
    }

    send(request.id, null, new Error(`Unsupported method: ${request.method}`));
  } catch (error) {
    send(request.id, null, error);
  }
}

const rl = readline.createInterface({ input: process.stdin });
rl.on("line", (line) => {
  if (!line.trim()) return;
  try {
    void handle(JSON.parse(line));
  } catch (error) {
    send(null, null, error);
  }
});

process.stderr.write("Jira MCP Server running on stdio\n");
