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
        projectKey: { type: "string", description: "Optional Jira project key when resolving fixVersion/fixVersions names." },
        fields: { type: "object", description: "Jira fields payload, for example { summary: 'New title', fixVersion: '0.1.66' }" },
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
    name: "jira_create_issue",
    description: "Create a Jira issue or subtask. Supports Task, Story, Bug, Epic, Spike, and Subtask by issue type name.",
    inputSchema: {
      type: "object",
      properties: {
        projectKey: { type: "string", description: "Jira project key, for example AUTO. Defaults to JIRA_PROJECT_KEY." },
        issueType: { type: "string", description: "Issue type name, for example Task, Story, Bug, Epic, Spike, or Subtask." },
        summary: { type: "string", description: "Issue summary." },
        description: { type: "string", description: "Plain text description." },
        parentKey: { type: "string", description: "Parent issue key for subtasks or team-managed epic/story hierarchy." },
        labels: { type: "array", items: { type: "string" }, description: "Labels to apply." },
        fields: { type: "object", description: "Additional Jira fields payload to merge into the create request. Supports fixVersion/fixVersions shorthand and auto-creates missing versions." },
      },
      required: ["issueType", "summary"],
    },
  },
  {
    name: "jira_list_boards",
    description: "List Jira Agile boards, optionally filtered by project key or board type.",
    inputSchema: {
      type: "object",
      properties: {
        projectKey: { type: "string", description: "Optional project key filter." },
        type: { type: "string", description: "Optional board type filter, for example scrum or kanban." },
        maxResults: { type: "number", description: "Maximum board count. Default: 50." },
      },
    },
  },
  {
    name: "jira_list_sprints",
    description: "List sprints for a Jira Agile board.",
    inputSchema: {
      type: "object",
      properties: {
        boardId: { type: "number", description: "Jira Agile board id." },
        state: { type: "string", description: "Sprint states, for example active,future,closed. Default: active,future." },
        maxResults: { type: "number", description: "Maximum sprint count. Default: 50." },
      },
      required: ["boardId"],
    },
  },
  {
    name: "jira_create_sprint",
    description: "Create a Jira sprint on an Agile board.",
    inputSchema: {
      type: "object",
      properties: {
        boardId: { type: "number", description: "Origin Jira Agile board id." },
        name: { type: "string", description: "Sprint name." },
        startDate: { type: "string", description: "Optional ISO start date." },
        endDate: { type: "string", description: "Optional ISO end date." },
        goal: { type: "string", description: "Optional sprint goal." },
      },
      required: ["boardId", "name"],
    },
  },
  {
    name: "jira_update_sprint",
    description: "Update sprint name, dates, goal, or state. Use state active to start and closed to close.",
    inputSchema: {
      type: "object",
      properties: {
        sprintId: { type: "number", description: "Sprint id." },
        name: { type: "string", description: "Optional sprint name." },
        state: { type: "string", description: "Optional sprint state: future, active, or closed." },
        startDate: { type: "string", description: "Optional ISO start date." },
        endDate: { type: "string", description: "Optional ISO end date." },
        goal: { type: "string", description: "Optional sprint goal." },
      },
      required: ["sprintId"],
    },
  },
  {
    name: "jira_get_sprint_issues",
    description: "List issues assigned to a Jira sprint.",
    inputSchema: {
      type: "object",
      properties: {
        sprintId: { type: "number", description: "Sprint id." },
        jql: { type: "string", description: "Optional additional JQL filter." },
        maxIssues: { type: "number", description: "Optional safety limit. Default: all returned by Jira pages." },
        pageSize: { type: "number", description: "Page size. Default: 100." },
      },
      required: ["sprintId"],
    },
  },
  {
    name: "jira_add_issues_to_sprint",
    description: "Add Jira issues to a sprint.",
    inputSchema: {
      type: "object",
      properties: {
        sprintId: { type: "number", description: "Sprint id." },
        issueKeys: { type: "array", items: { type: "string" }, description: "Issue keys to add." },
      },
      required: ["sprintId", "issueKeys"],
    },
  },
  {
    name: "jira_move_issues_to_backlog",
    description: "Move Jira issues from sprint or board planning into the backlog.",
    inputSchema: {
      type: "object",
      properties: {
        issueKeys: { type: "array", items: { type: "string" }, description: "Issue keys to move to backlog." },
      },
      required: ["issueKeys"],
    },
  },
  {
    name: "jira_rank_issues",
    description: "Rank Jira backlog or sprint issues before or after another issue.",
    inputSchema: {
      type: "object",
      properties: {
        issueKeys: { type: "array", items: { type: "string" }, description: "Issue keys to rank." },
        rankBeforeIssue: { type: "string", description: "Issue key to rank before." },
        rankAfterIssue: { type: "string", description: "Issue key to rank after." },
      },
      required: ["issueKeys"],
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
  {
    name: "jira_export_project",
    description: "Export project planning state: project metadata, boards, sprints, issues, subtasks, comments, links, labels, parents, and custom fields.",
    inputSchema: {
      type: "object",
      properties: {
        projectKey: { type: "string", description: "Jira project key. Defaults to JIRA_PROJECT_KEY." },
        includeClosedSprints: { type: "boolean", description: "Include closed sprints. Default: true." },
        includeComments: { type: "boolean", description: "Include issue comments. Default: true." },
        maxIssues: { type: "number", description: "Optional safety limit for exported issues. Default: all matching issues." },
        pageSize: { type: "number", description: "Jira page size per request. Default: 100." },
      },
    },
  },
];

const secretCache = new Map();
let jiraConfigCache = null;

function getSecret(name) {
  if (secretCache.has(name)) return secretCache.get(name);

  if (process.env[name]) {
    secretCache.set(name, process.env[name]);
    return process.env[name];
  }

  const ocid = process.env[`OCI_${name}_SECRET_OCID`];
  if (ocid) {
    const value = getOciSecretValue(ocid);
    secretCache.set(name, value);
    return value;
  }

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

  const value = getOciSecretValue(matches[0].identifier);
  secretCache.set(name, value);
  return value;
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
  if (jiraConfigCache) return jiraConfigCache;

  const baseUrl = getSecret("JIRA_BASE_URL").replace(/\/+$/, "");
  const email = getSecret("JIRA_EMAIL");
  const token = getSecret("JIRA_API_TOKEN");
  const auth = Buffer.from(`${email}:${token}`).toString("base64");

  jiraConfigCache = { baseUrl, auth };
  return jiraConfigCache;
}

function defaultProjectKey() {
  return getSecret("JIRA_PROJECT_KEY");
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

const projectCache = new Map();
const projectVersionsCache = new Map();

async function jiraProject(projectKey) {
  if (projectCache.has(projectKey)) {
    return projectCache.get(projectKey);
  }

  const project = await jiraRequest("GET", `/rest/api/3/project/${encodeURIComponent(projectKey)}`);
  projectCache.set(projectKey, project);
  return project;
}

async function jiraProjectVersions(projectKey) {
  if (projectVersionsCache.has(projectKey)) {
    return projectVersionsCache.get(projectKey);
  }

  const versions = await jiraRequest("GET", `/rest/api/3/project/${encodeURIComponent(projectKey)}/versions`);
  projectVersionsCache.set(projectKey, versions || []);
  return versions || [];
}

function requestedVersions(value) {
  if (value === undefined || value === null) {
    return [];
  }

  return Array.isArray(value) ? value : [value];
}

function versionName(value) {
  if (typeof value === "string") {
    return value.trim();
  }

  if (value && typeof value === "object" && typeof value.name === "string") {
    return value.name.trim();
  }

  return "";
}

function versionId(value) {
  if (value && typeof value === "object" && value.id !== undefined && value.id !== null) {
    return String(value.id).trim();
  }

  return "";
}

async function resolveFixVersions(projectKey, fields) {
  const requested = fields.fixVersions ?? fields.fixVersion;
  if (requested === undefined || requested === null) {
    return fields;
  }

  const versions = [];
  const known = new Map();
  const projectVersions = await jiraProjectVersions(projectKey);

  for (const entry of requestedVersions(requested)) {
    const id = versionId(entry);
    if (id) {
      if (!known.has(id)) {
        known.set(id, true);
        versions.push({ id });
      }
      continue;
    }

    const name = versionName(entry);
    if (!name) {
      continue;
    }

    const existing = projectVersions.find((candidate) => candidate.name?.toLowerCase() === name.toLowerCase());
    if (existing) {
      if (!known.has(String(existing.id))) {
        known.set(String(existing.id), true);
        versions.push({ id: existing.id });
      }
      continue;
    }

    const project = await jiraProject(projectKey);
    const created = await jiraRequest("POST", "/rest/api/3/version", {
      projectId: project.id,
      name,
    });
    projectVersions.push(created);
    projectVersionsCache.set(projectKey, projectVersions);

    if (!known.has(String(created.id))) {
      known.set(String(created.id), true);
      versions.push({ id: created.id });
    }
  }

  return compactObject({
    ...Object.fromEntries(Object.entries(fields).filter(([key]) => key !== "fixVersion")),
    fixVersions: versions,
  });
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

function clampPageSize(value, defaultValue = 100, maxValue = 100) {
  return Math.min(Math.max(value || defaultValue, 1), maxValue);
}

function compactObject(value) {
  return Object.fromEntries(Object.entries(value).filter(([, entry]) => entry !== undefined));
}

function normalizeUser(user) {
  if (!user) return null;
  return {
    accountId: user.accountId || null,
    displayName: user.displayName || null,
    emailAddress: user.emailAddress || null,
    active: user.active ?? null,
  };
}

function fieldName(value) {
  return value?.name || null;
}

function fieldNames(values) {
  return (values || []).map((value) => value.name).filter(Boolean);
}

function normalizeBoard(board) {
  return {
    id: board.id,
    name: board.name,
    type: board.type,
    self: board.self || null,
    location: board.location || null,
  };
}

function normalizeSprint(sprint) {
  return {
    id: sprint.id,
    self: sprint.self || null,
    state: sprint.state || null,
    name: sprint.name || null,
    startDate: sprint.startDate || null,
    endDate: sprint.endDate || null,
    completeDate: sprint.completeDate || null,
    originBoardId: sprint.originBoardId || null,
    goal: sprint.goal || null,
  };
}

function normalizeComment(comment) {
  return {
    id: comment.id,
    author: normalizeUser(comment.author),
    updateAuthor: normalizeUser(comment.updateAuthor),
    created: comment.created || null,
    updated: comment.updated || null,
    body: comment.body || null,
  };
}

function normalizeIssue(issue, baseUrl) {
  const fields = issue.fields || {};
  const issueType = fields.issuetype || {};
  const parent = fields.parent || null;
  const customFields = Object.fromEntries(
    Object.entries(fields)
      .filter(([key, value]) => key.startsWith("customfield_") && value !== null && value !== undefined)
      .sort(([left], [right]) => left.localeCompare(right))
  );
  const sprintValues = [fields.sprint, fields.closedSprints, ...Object.values(customFields)]
    .flat()
    .filter((value) => value && typeof value === "object" && "id" in value && "state" in value && "boardId" in value);
  return {
    id: issue.id,
    key: issue.key,
    url: issueUrl(baseUrl, issue.key),
    type: issueType.name || null,
    isSubtask: Boolean(issueType.subtask),
    summary: fields.summary || null,
    status: fieldName(fields.status),
    priority: fieldName(fields.priority),
    assignee: normalizeUser(fields.assignee),
    reporter: normalizeUser(fields.reporter),
    labels: fields.labels || [],
    components: fieldNames(fields.components),
    fixVersions: fieldNames(fields.fixVersions),
    sprintIds: [...new Set(sprintValues.map((sprint) => sprint.id).filter(Boolean))],
    parentKey: parent?.key || null,
    parentSummary: parent?.fields?.summary || null,
    created: fields.created || null,
    updated: fields.updated || null,
    description: fields.description || null,
    issueLinks: (fields.issuelinks || []).map((link) => ({
      id: link.id,
      type: link.type?.name || null,
      inward: link.type?.inward || null,
      outward: link.type?.outward || null,
      inwardIssueKey: link.inwardIssue?.key || null,
      outwardIssueKey: link.outwardIssue?.key || null,
    })),
    customFields,
    comments: (issue.comments || []).map(normalizeComment),
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
        "issuelinks",
        "sprint",
        "closedSprints",
        "*all",
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

async function fetchAllValues(path, query = {}, valueKey = "values", pageSize = 50, maxItems = null) {
  const values = [];
  let startAt = 0;

  while (maxItems === null || values.length < maxItems) {
    const currentPageSize = Math.min(pageSize, maxItems ? maxItems - values.length : pageSize);
    const params = new URLSearchParams({
      ...Object.fromEntries(Object.entries(query).filter(([, value]) => value !== undefined && value !== null && value !== "")),
      startAt: String(startAt),
      maxResults: String(currentPageSize),
    });
    const data = await jiraRequest("GET", `${path}?${params.toString()}`);
    const pageValues = data[valueKey] || [];
    values.push(...pageValues);

    if (data.isLast || pageValues.length === 0) break;
    startAt += pageValues.length;
  }

  return maxItems ? values.slice(0, maxItems) : values;
}

async function fetchIssueComments(issueKey, pageSize = 100) {
  return fetchAllValues(
    `/rest/api/3/issue/${encodeURIComponent(issueKey)}/comment`,
    { orderBy: "created" },
    "comments",
    pageSize
  );
}

async function listBoards(args = {}) {
  return fetchAllValues("/rest/agile/1.0/board", {
    projectKeyOrId: args.projectKey,
    type: args.type,
  }, "values", clampPageSize(args.maxResults || 50, 50, 50), args.maxResults || null);
}

async function listSprints(boardId, state = "active,future", maxResults = 50) {
  return fetchAllValues(
    `/rest/agile/1.0/board/${encodeURIComponent(boardId)}/sprint`,
    { state },
    "values",
    clampPageSize(maxResults, 50, 50),
    maxResults || null
  );
}

async function sprintIssues(sprintId, args = {}) {
  const jql = args.jql ? `Sprint = ${Number(sprintId)} AND (${args.jql}) ORDER BY key ASC` : `Sprint = ${Number(sprintId)} ORDER BY key ASC`;
  return searchAllIssues(jql, clampPageSize(args.pageSize), args.maxIssues || null);
}

async function exportProject(args = {}) {
  const projectKey = args.projectKey || defaultProjectKey();
  const pageSize = clampPageSize(args.pageSize);
  const { baseUrl } = jiraConfig();
  const [project, fields, statuses, issueTypes, boards] = await Promise.all([
    jiraRequest("GET", `/rest/api/3/project/${encodeURIComponent(projectKey)}`),
    jiraRequest("GET", "/rest/api/3/field"),
    jiraRequest("GET", `/rest/api/3/project/${encodeURIComponent(projectKey)}/statuses`),
    jiraRequest("GET", "/rest/api/3/issuetype"),
    listBoards({ projectKey, maxResults: 100 }),
  ]);
  const sprintState = args.includeClosedSprints === false ? "active,future" : "active,future,closed";
  const boardExports = [];

  for (const board of boards) {
    const sprints = board.type === "scrum" || board.type === "simple" ? await listSprints(board.id, sprintState, 1000) : [];
    boardExports.push({ ...normalizeBoard(board), sprints: sprints.map(normalizeSprint) });
  }

  const issues = await searchAllIssues(`project = ${projectKey} ORDER BY key ASC`, pageSize, args.maxIssues || null);
  if (args.includeComments !== false) {
    for (const issue of issues) {
      issue.comments = await fetchIssueComments(issue.key, pageSize);
    }
  }

  const grouped = groupExportedIssues(issues, baseUrl);
  return {
    exportedAt: new Date().toISOString(),
    project: {
      id: project.id,
      key: project.key,
      name: project.name,
      projectTypeKey: project.projectTypeKey,
      simplified: project.simplified,
      style: project.style,
      url: `${baseUrl}/jira/software/c/projects/${project.key}`,
    },
    fields: fields
      .map((field) => ({ id: field.id, key: field.key || null, name: field.name, custom: field.custom, schema: field.schema || null }))
      .sort((left, right) => left.id.localeCompare(right.id)),
    statuses,
    issueTypes: issueTypes
      .map((issueType) => ({ id: issueType.id, name: issueType.name, subtask: issueType.subtask, hierarchyLevel: issueType.hierarchyLevel ?? null }))
      .sort((left, right) => left.id.localeCompare(right.id)),
    boards: boardExports.sort((left, right) => left.id - right.id),
    issueCount: grouped.allIssues.length,
    epicCount: grouped.epics.length,
    standardIssueCount: grouped.issues.length,
    subtaskCount: grouped.subtasks.length,
    epics: grouped.epics,
    issues: grouped.issues,
    subtasks: grouped.subtasks,
    allIssues: grouped.allIssues,
  };
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
    const projectKey = args.projectKey || args.issueKey.split("-")[0];
    const needsVersionOverride = Boolean(args.fields && (Object.prototype.hasOwnProperty.call(args.fields, "fixVersion") || Object.prototype.hasOwnProperty.call(args.fields, "fixVersions")));
    const fields = await resolveFixVersions(projectKey, args.fields || {});
    const updatePath = needsVersionOverride
      ? `/rest/api/3/issue/${encodeURIComponent(args.issueKey)}?overrideScreenSecurity=true&notifyUsers=false`
      : `/rest/api/3/issue/${encodeURIComponent(args.issueKey)}`;
    await jiraRequest("PUT", updatePath, { fields });
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

  if (name === "jira_create_issue") {
    const projectKey = args.projectKey || defaultProjectKey();
    const versionedFields = await resolveFixVersions(projectKey, compactObject(args.fields || {}));
    const fields = compactObject({
      project: { key: projectKey },
      issuetype: { name: args.issueType },
      summary: args.summary,
      description: args.description ? adfText(args.description) : undefined,
      parent: args.parentKey ? { key: args.parentKey } : undefined,
      labels: args.labels,
      ...versionedFields,
    });
    const data = await jiraRequest("POST", "/rest/api/3/issue", { fields });
    const { baseUrl } = jiraConfig();
    return textResult(JSON.stringify({ key: data.key, id: data.id, url: issueUrl(baseUrl, data.key) }, null, 2));
  }

  if (name === "jira_list_boards") {
    const boards = await listBoards(args);
    return textResult(JSON.stringify(boards.map(normalizeBoard), null, 2));
  }

  if (name === "jira_list_sprints") {
    const sprints = await listSprints(args.boardId, args.state || "active,future", args.maxResults || 50);
    return textResult(JSON.stringify(sprints.map(normalizeSprint), null, 2));
  }

  if (name === "jira_create_sprint") {
    const sprint = await jiraRequest("POST", "/rest/agile/1.0/sprint", compactObject({
      name: args.name,
      originBoardId: args.boardId,
      startDate: args.startDate,
      endDate: args.endDate,
      goal: args.goal,
    }));
    return textResult(JSON.stringify(normalizeSprint(sprint), null, 2));
  }

  if (name === "jira_update_sprint") {
    const current = await jiraRequest("GET", `/rest/agile/1.0/sprint/${encodeURIComponent(args.sprintId)}`);
    const sprint = await jiraRequest("PUT", `/rest/agile/1.0/sprint/${encodeURIComponent(args.sprintId)}`, compactObject({
      name: args.name || current.name,
      state: args.state || current.state,
      startDate: args.startDate || current.startDate,
      endDate: args.endDate || current.endDate,
      goal: args.goal ?? current.goal,
    }));
    return textResult(JSON.stringify(normalizeSprint(sprint), null, 2));
  }

  if (name === "jira_get_sprint_issues") {
    const issues = await sprintIssues(args.sprintId, args);
    const { baseUrl } = jiraConfig();
    return textResult(JSON.stringify(issues.map((issue) => normalizeIssue(issue, baseUrl)), null, 2));
  }

  if (name === "jira_add_issues_to_sprint") {
    await jiraRequest("POST", `/rest/agile/1.0/sprint/${encodeURIComponent(args.sprintId)}/issue`, {
      issues: args.issueKeys,
    });
    return textResult(`Added ${args.issueKeys.length} issue(s) to sprint ${args.sprintId}.`);
  }

  if (name === "jira_move_issues_to_backlog") {
    await jiraRequest("POST", "/rest/agile/1.0/backlog/issue", { issues: args.issueKeys });
    return textResult(`Moved ${args.issueKeys.length} issue(s) to backlog.`);
  }

  if (name === "jira_rank_issues") {
    if (!args.rankBeforeIssue && !args.rankAfterIssue) {
      throw new Error("Either rankBeforeIssue or rankAfterIssue is required.");
    }
    if (args.rankBeforeIssue && args.rankAfterIssue) {
      throw new Error("Use only one of rankBeforeIssue or rankAfterIssue.");
    }
    await jiraRequest("PUT", "/rest/agile/1.0/issue/rank", compactObject({
      issues: args.issueKeys,
      rankBeforeIssue: args.rankBeforeIssue,
      rankAfterIssue: args.rankAfterIssue,
    }));
    return textResult(`Ranked ${args.issueKeys.length} issue(s).`);
  }

  if (name === "jira_export_issues") {
    const projectKey = args.projectKey || defaultProjectKey();
    const jql = args.jql || `project = ${projectKey} ORDER BY key ASC`;
    const pageSize = clampPageSize(args.pageSize);
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

  if (name === "jira_export_project") {
    return textResult(JSON.stringify(await exportProject(args), null, 2));
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
