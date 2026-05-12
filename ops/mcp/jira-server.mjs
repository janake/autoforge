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
    description: "Update fields of a Jira issue using Jira REST API field names.",
    inputSchema: {
      type: "object",
      properties: {
        issueKey: { type: "string", description: "Jira issue key, for example AUTO-5" },
        fields: { type: "object", description: "Jira fields payload, for example { summary: 'New title' }" },
      },
      required: ["issueKey", "fields"],
    },
  },
  {
    name: "jira_list_transitions",
    description: "List available workflow transitions for a Jira issue.",
    inputSchema: {
      type: "object",
      properties: {
        issueKey: { type: "string", description: "Jira issue key, for example AUTO-5" },
      },
      required: ["issueKey"],
    },
  },
  {
    name: "jira_transition_issue",
    description: "Transition a Jira issue by transition name, for example Under Test.",
    inputSchema: {
      type: "object",
      properties: {
        issueKey: { type: "string", description: "Jira issue key, for example AUTO-5" },
        transitionName: { type: "string", description: "Transition name, for example Under Test" },
      },
      required: ["issueKey", "transitionName"],
    },
  },
  {
    name: "jira_add_comment",
    description: "Add a plain text comment to a Jira issue.",
    inputSchema: {
      type: "object",
      properties: {
        issueKey: { type: "string", description: "Jira issue key, for example AUTO-5" },
        body: { type: "string", description: "Plain text comment body." },
      },
      required: ["issueKey", "body"],
    },
  },
  {
    name: "jira_export_issues",
    description: "Export Jira epics, tasks/stories, bugs, and subtasks as structured JSON using a project key or JQL.",
    inputSchema: {
      type: "object",
      properties: {
        projectKey: { type: "string", description: "Jira project key, for example AUTO. Used when jql is omitted." },
        jql: { type: "string", description: "JQL query to export, for example: project = AUTO ORDER BY key ASC" },
        maxIssues: { type: "number", description: "Optional safety limit for exported issues. Default: all matching issues." },
        pageSize: { type: "number", description: "Jira page size per request. Default: 100." },
      },
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

function adfText(body) {
  return {
    type: "doc",
    version: 1,
    content: [
      {
        type: "paragraph",
        content: [{ type: "text", text: body }],
      },
    ],
  };
}

function issueUrl(baseUrl, issueKey) {
  return `${baseUrl}/browse/${issueKey}`;
}

function fieldName(value) {
  return value?.name || null;
}

function fieldNames(values) {
  return (values || []).map((value) => value.name).filter(Boolean);
}

function normalizeIssue(issue, baseUrl) {
  const fields = issue.fields || {};
  const issueType = fields.issuetype || {};
  const parent = fields.parent || null;
  return {
    id: issue.id,
    key: issue.key,
    url: issueUrl(baseUrl, issue.key),
    type: issueType.name || null,
    isSubtask: Boolean(issueType.subtask),
    summary: fields.summary || null,
    status: fieldName(fields.status),
    priority: fieldName(fields.priority),
    assignee: fields.assignee?.displayName || null,
    reporter: fields.reporter?.displayName || null,
    labels: fields.labels || [],
    components: fieldNames(fields.components),
    fixVersions: fieldNames(fields.fixVersions),
    parentKey: parent?.key || null,
    parentSummary: parent?.fields?.summary || null,
    created: fields.created || null,
    updated: fields.updated || null,
    description: fields.description || null,
    subtasks: (fields.subtasks || []).map((subtask) => ({
      key: subtask.key,
      summary: subtask.fields?.summary || null,
      status: fieldName(subtask.fields?.status),
      type: fieldName(subtask.fields?.issuetype),
    })),
  };
}

async function searchAllIssues(jql, pageSize, maxIssues) {
  const issues = [];
  let nextPageToken = null;

  do {
    const remaining = maxIssues ? maxIssues - issues.length : pageSize;
    const currentPageSize = Math.min(pageSize, remaining || pageSize);
    const params = new URLSearchParams({
      jql,
      maxResults: String(currentPageSize),
      fields: [
        "summary",
        "status",
        "issuetype",
        "parent",
        "priority",
        "assignee",
        "reporter",
        "labels",
        "components",
        "fixVersions",
        "created",
        "updated",
        "description",
        "subtasks",
      ].join(","),
    });
    if (nextPageToken) params.set("nextPageToken", nextPageToken);

    const data = await jiraRequest("GET", `/rest/api/3/search/jql?${params.toString()}`);
    issues.push(...(data.issues || []));
    nextPageToken = data.nextPageToken || null;
    if (data.isLast || (maxIssues && issues.length >= maxIssues)) break;
  } while (nextPageToken);

  return maxIssues ? issues.slice(0, maxIssues) : issues;
}

function groupExportedIssues(issues, baseUrl) {
  const normalized = issues.map((issue) => normalizeIssue(issue, baseUrl));
  const epics = normalized.filter((issue) => issue.type === "Epic");
  const subtasks = normalized.filter((issue) => issue.isSubtask);
  const standardIssues = normalized.filter((issue) => issue.type !== "Epic" && !issue.isSubtask);
  const childrenByParent = new Map();

  for (const issue of normalized) {
    if (!issue.parentKey) continue;
    const children = childrenByParent.get(issue.parentKey) || [];
    children.push(issue.key);
    childrenByParent.set(issue.parentKey, children);
  }

  return {
    epics: epics.map((issue) => ({ ...issue, childKeys: childrenByParent.get(issue.key) || [] })),
    issues: standardIssues.map((issue) => ({ ...issue, childKeys: childrenByParent.get(issue.key) || [] })),
    subtasks,
    allIssues: normalized,
  };
}

async function callTool(name, args = {}) {
  if (name === "jira_search") {
    const maxResults = args.maxResults || 50;
    const data = await jiraRequest("GET", `/rest/api/3/search/jql?jql=${encodeURIComponent(args.jql)}&maxResults=${maxResults}&fields=summary,status`);
    const issues = (data.issues || []).map((issue) => ({
      key: issue.key,
      summary: issue.fields.summary,
      status: issue.fields.status?.name,
    }));
    return textResult(JSON.stringify(issues, null, 2));
  }

  if (name === "jira_get_issue") {
    const data = await jiraRequest("GET", `/rest/api/3/issue/${encodeURIComponent(args.issueKey)}`);
    return textResult(JSON.stringify({ key: data.key, fields: data.fields }, null, 2));
  }

  if (name === "jira_update_issue") {
    await jiraRequest("PUT", `/rest/api/3/issue/${encodeURIComponent(args.issueKey)}`, { fields: args.fields });
    return textResult(`Issue ${args.issueKey} updated successfully.`);
  }

  if (name === "jira_list_transitions") {
    const data = await jiraRequest("GET", `/rest/api/3/issue/${encodeURIComponent(args.issueKey)}/transitions`);
    const transitions = (data.transitions || []).map((transition) => ({
      id: transition.id,
      name: transition.name,
      to: transition.to?.name,
    }));
    return textResult(JSON.stringify(transitions, null, 2));
  }

  if (name === "jira_transition_issue") {
    const data = await jiraRequest("GET", `/rest/api/3/issue/${encodeURIComponent(args.issueKey)}/transitions`);
    const transitions = data.transitions || [];
    const transition = transitions.find((candidate) => candidate.name.toLowerCase() === args.transitionName.toLowerCase());
    if (!transition) {
      const available = transitions.map((candidate) => candidate.name).join(", ") || "none";
      throw new Error(`Transition '${args.transitionName}' is not available for ${args.issueKey}. Available transitions: ${available}`);
    }

    await jiraRequest("POST", `/rest/api/3/issue/${encodeURIComponent(args.issueKey)}/transitions`, {
      transition: { id: transition.id },
    });
    return textResult(`Issue ${args.issueKey} transitioned with '${transition.name}'.`);
  }

  if (name === "jira_add_comment") {
    await jiraRequest("POST", `/rest/api/3/issue/${encodeURIComponent(args.issueKey)}/comment`, {
      body: adfText(args.body),
    });
    return textResult(`Comment added to ${args.issueKey}.`);
  }

  if (name === "jira_export_issues") {
    const jql = args.jql || (args.projectKey ? `project = ${args.projectKey} ORDER BY key ASC` : "project = AUTO ORDER BY key ASC");
    const pageSize = Math.min(Math.max(args.pageSize || 100, 1), 100);
    const issues = await searchAllIssues(jql, pageSize, args.maxIssues || null);
    const { baseUrl } = jiraConfig();
    const grouped = groupExportedIssues(issues, baseUrl);
    return textResult(JSON.stringify({
      exportedAt: new Date().toISOString(),
      jql,
      issueCount: grouped.allIssues.length,
      epicCount: grouped.epics.length,
      standardIssueCount: grouped.issues.length,
      subtaskCount: grouped.subtasks.length,
      epics: grouped.epics,
      issues: grouped.issues,
      subtasks: grouped.subtasks,
      allIssues: grouped.allIssues,
    }, null, 2));
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
