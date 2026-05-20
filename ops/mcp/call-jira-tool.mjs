#!/usr/bin/env node
import { spawn } from "node:child_process";

const [, , toolName, rawArgs] = process.argv;

if (!toolName || toolName === "--help" || toolName === "-h") {
  process.stderr.write("Usage: node ops/mcp/call-jira-tool.mjs <tool-name|--list> '<json-args>'\n");
  process.stderr.write("Example: node ops/mcp/call-jira-tool.mjs jira_transition_issue '{\"issueKey\":\"AUTO-415\",\"transitionName\":\"Under Test\"}'\n");
  process.exit(toolName ? 0 : 1);
}

let args = {};
if (toolName !== "--list") {
  try {
    args = rawArgs ? JSON.parse(rawArgs) : {};
  } catch (error) {
    process.stderr.write(`Invalid JSON args: ${error instanceof Error ? error.message : String(error)}\n`);
    process.exit(1);
  }
}

const child = spawn(process.execPath, ["ops/mcp/jira-server.mjs"], {
  cwd: new URL("../..", import.meta.url),
  stdio: ["pipe", "pipe", "pipe"],
});

let stdout = "";
let stderr = "";

child.stdout.on("data", (chunk) => {
  stdout += chunk.toString("utf8");
});

child.stderr.on("data", (chunk) => {
  stderr += chunk.toString("utf8");
});

const requests = [
  { jsonrpc: "2.0", id: 1, method: "initialize", params: { protocolVersion: "2024-11-05", capabilities: {}, clientInfo: { name: "autoforge-jira-cli", version: "0.1.0" } } },
  toolName === "--list"
    ? { jsonrpc: "2.0", id: 2, method: "tools/list", params: {} }
    : { jsonrpc: "2.0", id: 2, method: "tools/call", params: { name: toolName, arguments: args } },
];

for (const request of requests) {
  child.stdin.write(`${JSON.stringify(request)}\n`);
}
child.stdin.end();

child.on("close", (code) => {
  if (code !== 0) {
    process.stderr.write(stderr);
    process.exit(code || 1);
  }

  const responses = stdout
    .split("\n")
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line) => JSON.parse(line));
  const response = responses.find((item) => item.id === 2);

  if (!response) {
    process.stderr.write(stderr || "No MCP response received.\n");
    process.exit(1);
  }

  if (response.error) {
    process.stderr.write(`${response.error.message}\n`);
    process.exit(1);
  }

  const content = response.result?.content;
  if (Array.isArray(content) && content.length > 0) {
    process.stdout.write(content.map((item) => item.text ?? JSON.stringify(item)).join("\n"));
    process.stdout.write("\n");
    return;
  }

  process.stdout.write(`${JSON.stringify(response.result, null, 2)}\n`);
});
