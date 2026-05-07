#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import fs from "node:fs";
import path from "node:path";

const rootDir = process.cwd();
const defaultPlan = path.join(rootDir, "build", "jira-import-plan.json");
const secretNames = ["JIRA_BASE_URL", "JIRA_PROJECT_KEY", "JIRA_EMAIL", "JIRA_API_TOKEN"];

function parseArgs(argv) {
  const args = {
    plan: defaultPlan,
    apply: false,
    applyStatus: false,
    checkExisting: false,
    ociLookupByName: false,
    listProjects: false,
    limit: null,
    onlyExternalId: null,
    fallbackIssueType: "Task",
  };

  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];
    if (arg === "--plan") {
      args.plan = path.resolve(rootDir, argv[index + 1] || "");
      index += 1;
      continue;
    }
    if (arg === "--apply") {
      args.apply = true;
      continue;
    }
    if (arg === "--apply-status") {
      args.applyStatus = true;
      continue;
    }
    if (arg === "--check-existing") {
      args.checkExisting = true;
      continue;
    }
    if (arg === "--oci-lookup-by-name") {
      args.ociLookupByName = true;
      continue;
    }
    if (arg === "--list-projects") {
      args.listProjects = true;
      continue;
    }
    if (arg === "--limit") {
      args.limit = Number.parseInt(argv[index + 1] || "", 10);
      index += 1;
      continue;
    }
    if (arg === "--issue") {
      args.onlyExternalId = argv[index + 1] || "";
      index += 1;
      continue;
    }
    if (arg === "--fallback-issue-type") {
      args.fallbackIssueType = argv[index + 1] || "Task";
      index += 1;
      continue;
    }
    if (arg === "--help" || arg === "-h") {
      printHelp();
      process.exit(0);
    }
    throw new Error(`Unknown argument: ${arg}`);
  }

  if (args.applyStatus && !args.apply) {
    throw new Error("--apply-status requires --apply");
  }
  if (args.limit !== null && (!Number.isInteger(args.limit) || args.limit < 1)) {
    throw new Error("--limit must be a positive integer");
  }

  return args;
}

function printHelp() {
  console.log(`Usage: node ops/jira/import-plan.mjs [options]

Options:
  --plan <file>              Import plan JSON. Default: build/jira-import-plan.json
  --apply                    Create or update Jira issues. Without this, only prints a dry-run summary.
  --apply-status             Try to transition Jira issues to the planned status. Requires --apply.
  --check-existing           Read Jira and report whether issues already exist, without writing.
  --oci-lookup-by-name       Resolve JIRA_* secrets from OCI Vault by display name.
  --list-projects            List Jira projects visible to the configured credentials and exit.
  --limit <n>                Process only the first n issues.
  --issue <external-id>      Process one external ID, for example AUTO-29 or EPIC3-STORY-1.
  --fallback-issue-type <t>  Fallback issue type for create retry. Default: Task.

Credential sources:
  1. Direct env values: JIRA_BASE_URL, JIRA_PROJECT_KEY, JIRA_EMAIL, JIRA_API_TOKEN
  2. OCI OCID env values: OCI_JIRA_BASE_URL_SECRET_OCID, OCI_JIRA_PROJECT_KEY_SECRET_OCID,
     OCI_JIRA_EMAIL_SECRET_OCID, OCI_JIRA_API_TOKEN_SECRET_OCID
  3. Optional OCI name lookup: --oci-lookup-by-name for Vault secrets named JIRA_BASE_URL,
     JIRA_PROJECT_KEY, JIRA_EMAIL, JIRA_API_TOKEN

No Jira token or secret value is printed.`);
}

function readPlan(planPath) {
  if (!fs.existsSync(planPath)) {
    throw new Error(`Import plan does not exist: ${path.relative(rootDir, planPath)}. Run npm run jira:dry-run first.`);
  }
  return JSON.parse(fs.readFileSync(planPath, "utf8"));
}

async function main() {
  const args = parseArgs(process.argv.slice(2));
  const plan = readPlan(args.plan);
  const issues = selectIssues(plan.issues || [], args);

  if (!args.apply && !args.checkExisting && !args.listProjects) {
    printLocalDryRun(plan, issues, args);
    return;
  }

  const credentials = loadCredentials(args);
  const client = new JiraClient(credentials);

  if (args.listProjects) {
    await client.listProjects();
    return;
  }

    const results = [];

  for (const issue of issues) {
    const existing = await client.findIssueByExternalId(issue.externalId);
    const parentKey = issue.parentExternalId
      ? await client.findParentKey(issue.parentExternalId, issue.externalId)
      : null;

    if (!args.apply) {
      results.push({ externalId: issue.externalId, action: existing ? "exists" : "missing", key: existing?.key || null });
      continue;
    }

    const result = existing
      ? await client.updateIssue(existing.key, issue, parentKey)
      : await client.createIssue(issue, args.fallbackIssueType, parentKey);

    if (args.applyStatus) {
      await client.transitionIssue(result.key, issue.status);
    }

    results.push({ externalId: issue.externalId, action: existing ? "updated" : "created", key: result.key });
  }

  printResults(results, args);
}

function selectIssues(issues, args) {
  let selected = issues;
  if (args.onlyExternalId) {
    selected = selected.filter((issue) => issue.externalId === args.onlyExternalId);
  }
  if (args.limit !== null) {
    selected = selected.slice(0, args.limit);
  }
  if (selected.length === 0) {
    throw new Error("No issues selected from import plan.");
  }
  return selected;
}

function printLocalDryRun(plan, issues, args) {
  console.log("Jira import dry-run only. No Jira API calls were made.");
  console.log(`Plan: ${path.relative(rootDir, args.plan)}`);
  console.log(`Plan generated by: ${plan.metadata?.generator || "unknown"}`);
  console.log(`Selected issues: ${issues.length}`);
  console.log("Use --check-existing to compare against Jira, or --apply to create/update issues.");
}

function printResults(results, args) {
  const counts = results.reduce((acc, result) => {
    acc[result.action] = (acc[result.action] || 0) + 1;
    return acc;
  }, {});

  console.log(args.apply ? "Jira import completed." : "Jira existing issue check completed.");
  console.log(JSON.stringify({ counts, results }, null, 2));
}

function loadCredentials(args) {
  const values = {};
  for (const name of secretNames) {
    values[name] = process.env[name] || "";
  }

  for (const name of secretNames) {
    if (!values[name]) {
      const ocid = process.env[`OCI_${name}_SECRET_OCID`] || "";
      if (ocid) {
        values[name] = getOciSecretValue(ocid);
      }
    }
  }

  if (args.ociLookupByName) {
    for (const name of secretNames) {
      if (!values[name]) {
        values[name] = getOciSecretValue(findOciSecretIdByName(name));
      }
    }
  }

  const missing = secretNames.filter((name) => !values[name]);
  if (missing.length > 0) {
    throw new Error(`Missing Jira credentials: ${missing.join(", ")}`);
  }

  return {
    baseUrl: values.JIRA_BASE_URL.replace(/\/+$/, ""),
    projectKey: values.JIRA_PROJECT_KEY,
    email: values.JIRA_EMAIL,
    apiToken: values.JIRA_API_TOKEN,
  };
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

function findOciSecretIdByName(name) {
  const raw = execFileSync("oci", [
    "search",
    "resource",
    "free-text-search",
    "--text",
    name,
    "--output",
    "json",
  ], { encoding: "utf8", stdio: ["ignore", "pipe", "pipe"] });

  const data = JSON.parse(raw);
  const matches = (data.data?.items || [])
    .filter((item) => item["resource-type"] === "VaultSecret")
    .filter((item) => item["display-name"] === name)
    .filter((item) => item["lifecycle-state"] === "ACTIVE");

  if (matches.length !== 1) {
    throw new Error(`Expected exactly one ACTIVE OCI Vault secret named ${name}, found ${matches.length}`);
  }
  return matches[0].identifier;
}

class JiraClient {
  constructor(credentials) {
    this.credentials = credentials;
    this.auth = Buffer.from(`${credentials.email}:${credentials.apiToken}`).toString("base64");
  }

  async request(method, apiPath, body = null) {
    const response = await fetch(`${this.credentials.baseUrl}${apiPath}`, {
      method,
      headers: {
        Authorization: `Basic ${this.auth}`,
        Accept: "application/json",
        "Content-Type": "application/json",
      },
      body: body ? JSON.stringify(body) : undefined,
    });

    const text = await response.text();
    const data = text ? JSON.parse(text) : null;
    if (!response.ok) {
      const fieldErrors = data?.errors && typeof data.errors === "object"
        ? Object.entries(data.errors).map(([field, message]) => `${field}: ${message}`)
        : [];
      const message = [
        ...(data?.errorMessages || []),
        ...fieldErrors,
        data?.message,
        response.statusText,
      ].filter(Boolean).join("; ");
      throw new Error(`Jira ${method} ${apiPath} failed (${response.status}): ${message}`);
    }
    return data;
  }

  async findIssueByExternalId(externalId) {
    const label = externalIdLabel(externalId);
    const jql = `project = ${quoteJql(this.credentials.projectKey)} AND labels = ${quoteJql(label)}`;
    const params = new URLSearchParams({
      jql,
      maxResults: "2",
    });
    params.append("fields", "summary");
    params.append("fields", "labels");

    const data = await this.request("GET", `/rest/api/3/search/jql?${params.toString()}`);

    if ((data.issues || []).length > 1) {
      throw new Error(`Multiple Jira issues found for external ID ${externalId}`);
    }
    return data.issues?.[0] || null;
  }

  async findParentKey(parentExternalId, childExternalId) {
    const parent = await this.findIssueByExternalId(parentExternalId);
    if (!parent) {
      throw new Error(`Parent Jira issue not found for ${childExternalId}: ${parentExternalId}`);
    }
    return parent.key;
  }

  async listProjects() {
    const data = await this.request("GET", "/rest/api/3/project/search?maxResults=100");
    const projects = data.values || [];
    console.log(JSON.stringify({
      count: projects.length,
      projects: projects.map((project) => ({
        key: project.key,
        name: project.name,
        projectTypeKey: project.projectTypeKey,
        simplified: project.simplified,
      })),
    }, null, 2));
  }

  async createIssue(issue, fallbackIssueType, parentKey = null) {
    const payload = this.issuePayload(issue, issue.issueType, parentKey);
    try {
      const created = await this.request("POST", "/rest/api/2/issue", payload);
      return { key: created.key };
    } catch (error) {
      if (issue.issueType === fallbackIssueType) {
        throw error;
      }
      const fallback = this.issuePayload(issue, fallbackIssueType, parentKey);
      // For fallback description, we use the v3 format because we know description works with toAdf in v2 endpoint as well, 
      // but actually let's just use string to be safe.
      fallback.fields.description = `${adfPlainText(issue)}\n\nImport note: requested issue type ${issue.issueType} was retried as ${fallbackIssueType}.`;
      const created = await this.request("POST", "/rest/api/2/issue", fallback);
      return { key: created.key };
    }
  }

  async updateIssue(key, issue, parentKey = null) {
    await this.request("PUT", `/rest/api/2/issue/${encodeURIComponent(key)}`, {
      fields: issueFields(issue, parentKey),
    });
    return { key };
  }

  async transitionIssue(key, targetStatus) {
    const data = await this.request("GET", `/rest/api/2/issue/${encodeURIComponent(key)}/transitions`);
    const transition = (data.transitions || []).find((candidate) => {
      return [candidate.name, candidate.to?.name]
        .filter(Boolean)
        .some((name) => name.toLowerCase() === targetStatus.toLowerCase());
    });

    if (!transition) {
      console.warn(`No Jira transition found for ${key} -> ${targetStatus}`);
      return;
    }

    await this.request("POST", `/rest/api/2/issue/${encodeURIComponent(key)}/transitions`, {
      transition: { id: transition.id },
    });
  }

  issuePayload(issue, issueType, parentKey = null) {
    return {
      fields: {
        project: { key: this.credentials.projectKey },
        ...issueFields(issue, parentKey),
        issuetype: { name: issueType },
      },
    };
  }
}

function issueFields(issue, parentKey = null) {
  return {
    summary: issue.summary,
    description: adfPlainText(issue), // Using string for v2
    labels: labelsForIssue(issue),
    ...(parentKey ? { parent: { key: parentKey } } : {}),
    ...(issue.fields?.priority ? { priority: { name: issue.fields.priority } } : {}),
    customfield_10039: Array.isArray(issue.fields?.acceptanceCriteria) 
      ? issue.fields.acceptanceCriteria.map(ac => `- ${ac}`).join("\n")
      : (issue.fields?.acceptanceCriteria || ""),
    customfield_10040: issue.fields?.dependencies || "",
    customfield_10041: issue.fields?.output || "",
    customfield_10042: issue.fields?.goal || "",
    customfield_10043: issue.fields?.scope || "",
  };
}

function adfPlainText(issue) {
  const metadata = [
    `Autoforge External ID: ${issue.externalId}`,
    `Source file: ${issue.sourceFile}`,
    `Planned issue type: ${issue.issueType}`,
    `Planned status: ${issue.status}`,
    `Status reason: ${issue.statusReason}`,
    issue.fixVersion ? `Fix version: ${issue.fixVersion}` : null,
    issue.parentExternalId ? `Parent external ID: ${issue.parentExternalId}` : null,
  ].filter(Boolean).join("\n");

  return `${metadata}\n\n--- Imported description ---\n\n${issue.description || ""}`;
}

function toAdf(text) {
  return {
    type: "doc",
    version: 1,
    content: [
      {
        type: "codeBlock",
        attrs: { language: "markdown" },
        content: [{ type: "text", text: text.slice(0, 30000) }],
      },
    ],
  };
}

function labelsForIssue(issue) {
  return [...new Set([...(issue.labels || []), externalIdLabel(issue.externalId)])]
    .map(sanitizeLabel)
    .filter(Boolean)
    .slice(0, 50);
}

function externalIdLabel(externalId) {
  return `autoforge-id-${externalId}`;
}

function sanitizeLabel(value) {
  return value
    .toLowerCase()
    .replace(/[^a-z0-9_-]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .slice(0, 255);
}

function quoteJql(value) {
  return `"${String(value).replaceAll('"', '\\"')}"`;
}

main().catch((error) => {
  console.error(error instanceof Error ? error.message : String(error));
  process.exit(1);
});
