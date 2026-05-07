#!/usr/bin/env node

import fs from "node:fs";
import path from "node:path";

const rootDir = process.cwd();
const defaultOutput = path.join(rootDir, "build", "jira-import-plan.json");

function parseArgs(argv) {
  const args = {
    output: defaultOutput,
    pretty: true,
  };

  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];
    if (arg === "--output" || arg === "-o") {
      args.output = path.resolve(rootDir, argv[index + 1] || "");
      index += 1;
      continue;
    }
    if (arg === "--compact") {
      args.pretty = false;
      continue;
    }
    if (arg === "--help" || arg === "-h") {
      printHelp();
      process.exit(0);
    }
    throw new Error(`Unknown argument: ${arg}`);
  }

  return args;
}

function printHelp() {
  console.log(`Usage: node ops/jira/generate-import-plan.mjs [--output <file>] [--compact]

Generates a Jira migration dry-run JSON from docs/tasks, docs/epics, and docs/releases.md.
No Jira credentials are required and no Jira API calls are made.`);
}

function readText(relativePath) {
  return fs.readFileSync(path.join(rootDir, relativePath), "utf8");
}

function exists(relativePath) {
  return fs.existsSync(path.join(rootDir, relativePath));
}

function listMarkdownFiles(relativeDir) {
  const absoluteDir = path.join(rootDir, relativeDir);
  if (!fs.existsSync(absoluteDir)) {
    return [];
  }

  return fs.readdirSync(absoluteDir)
    .filter((entry) => entry.endsWith(".md"))
    .sort((left, right) => left.localeCompare(right, "en", { numeric: true }))
    .map((entry) => path.join(relativeDir, entry).replaceAll(path.sep, "/"));
}

function sectionMap(markdown) {
  const sections = new Map();
  const matches = [...markdown.matchAll(/^##\s+(.+)$/gm)];

  for (let index = 0; index < matches.length; index += 1) {
    const match = matches[index];
    const next = matches[index + 1];
    const title = normalizeHeading(match[1]);
    const start = match.index + match[0].length;
    const end = next ? next.index : markdown.length;
    sections.set(title, markdown.slice(start, end).trim());
  }

  return sections;
}

function normalizeHeading(value) {
  return value
    .trim()
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-|-$/g, "");
}

function firstNonEmptyLine(value) {
  return (value || "")
    .split(/\r?\n/)
    .map((line) => line.trim())
    .find(Boolean) || "";
}

function bulletItems(value) {
  return (value || "")
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter((line) => line.startsWith("- "))
    .map((line) => line.slice(2).trim());
}

function parseTitle(markdown, fallbackId) {
  const heading = markdown.match(/^#\s+(.+)$/m)?.[1]?.trim();
  if (!heading) {
    return fallbackId;
  }

  const bracketed = heading.match(/^\[?([A-Z]+-\d+)\]?\s*(.*)$/);
  if (bracketed) {
    return bracketed[2].replace(/^[-: ]+/, "").trim() || bracketed[1];
  }

  const colon = heading.match(/^(EPIC-\d+)\s*:\s*(.+)$/i);
  if (colon) {
    return colon[2].trim();
  }

  return heading;
}

function parseExternalIdFromFile(filePath) {
  return path.basename(filePath, ".md");
}

function parseReleases() {
  if (!exists("docs/releases.md")) {
    return { versions: [], taskVersions: new Map(), warnings: ["docs/releases.md is missing"] };
  }

  const markdown = readText("docs/releases.md");
  const matches = [...markdown.matchAll(/^##\s+(.+)$/gm)];
  const versions = [];
  const taskVersions = new Map();
  const warnings = [];

  for (let index = 0; index < matches.length; index += 1) {
    const match = matches[index];
    const next = matches[index + 1];
    const version = match[1].trim();
    const body = markdown.slice(match.index + match[0].length, next ? next.index : markdown.length);
    const tasks = [...body.matchAll(/-\s+`([A-Z]+-\d+)`\s*:\s*(.+)$/gm)].map((taskMatch) => ({
      id: taskMatch[1],
      summary: taskMatch[2].trim(),
    }));

    versions.push({ version, tasks });
    for (const task of tasks) {
      if (taskVersions.has(task.id)) {
        warnings.push(`Duplicate release mapping for ${task.id}`);
      }
      taskVersions.set(task.id, version);
    }
  }

  return { versions, taskVersions, warnings };
}

function inferTaskIssueType(externalId, title, sourceFile) {
  if (externalId.startsWith("BUG-")) {
    return "Bug";
  }

  const text = `${title} ${sourceFile}`.toLowerCase();
  if (/research|spike|felmer|felder|vizsgalat|audit/.test(text)) {
    return "Spike";
  }
  if (/rules|docs|documentation|document|manifest|version|process|tooling|migration|protocol/.test(text)) {
    return "Task";
  }
  return "Story";
}

function mapStatus({ status, branch, pr }) {
  const normalized = (status || "").toLowerCase();

  if (/cancel/.test(normalized)) {
    return { status: "Cancelled", reason: "Task status is cancelled" };
  }
  if (/block|blocked|fuggo|függ/.test(normalized)) {
    return { status: "Blocked", reason: "Task status indicates blocked" };
  }
  if (/done|complete|completed|released/.test(normalized)) {
    return { status: "Done", reason: "Task status indicates completed" };
  }
  if (pr) {
    return { status: "In Review", reason: "Task has a PR reference" };
  }
  if (branch) {
    return { status: "In Progress", reason: "Task has a branch reference" };
  }
  if (/progress|in_progress/.test(normalized)) {
    return { status: "In Progress", reason: "Task status is in progress" };
  }
  return { status: "Backlog", reason: "No stronger status signal found" };
}

function parseTask(filePath, releaseMap) {
  const markdown = readText(filePath);
  const sections = sectionMap(markdown);
  const externalId = parseExternalIdFromFile(filePath);
  const title = parseTitle(markdown, externalId);
  const statusText = firstNonEmptyLine(sections.get("statusz"));
  const version = firstNonEmptyLine(sections.get("verzio"));
  const branch = firstNonEmptyLine(sections.get("branch"));
  const prRaw = sections.get("pr") || "";
  const prMatch = prRaw.match(/PR\s*#?(\d+)/i);
  const pr = prMatch ? `PR #${prMatch[1]}` : "";
  const acceptanceCriteria = bulletItems(sections.get("acceptance-criteria"));
  const documents = bulletItems(sections.get("dokumentumok-es-fajlok"));
  const stepLog = bulletItems(sections.get("lepesnaplo"));
  const result = sections.get("eredmeny") || "";
  const mapped = mapStatus({ status: statusText, branch, pr });
  const releaseVersion = releaseMap.get(externalId) || "";
  const warnings = [];

  if (!version) {
    warnings.push("Missing Verzió section value");
  }
  if (!releaseVersion) {
    warnings.push("Missing docs/releases.md mapping");
  }
  if (version && releaseVersion && version !== releaseVersion) {
    warnings.push(`Task version ${version} differs from release mapping ${releaseVersion}`);
  }
  if (acceptanceCriteria.length === 0) {
    warnings.push("Missing acceptance criteria bullets");
  }

  return {
    source: "task-file",
    sourceFile: filePath,
    externalId,
    issueType: inferTaskIssueType(externalId, title, filePath),
    summary: title,
    status: mapped.status,
    statusReason: mapped.reason,
    fixVersion: releaseVersion || version || null,
    labels: taskLabels(externalId, filePath, title),
    fields: {
      originalStatus: statusText || null,
      branch: branch || null,
      pr: pr || null,
      acceptanceCriteria,
      documents,
      stepLog,
      result: result || null,
    },
    description: markdown.trim(),
    warnings,
  };
}

function taskLabels(externalId, sourceFile, title) {
  const labels = new Set(["autoforge", "migration"]);
  labels.add(externalId.startsWith("BUG-") ? "bug" : "auto");

  const text = `${sourceFile} ${title}`.toLowerCase();
  for (const label of ["docs", "infra", "security", "deploy", "auth", "frontend", "backend", "gateway", "ai"]) {
    if (text.includes(label)) {
      labels.add(label);
    }
  }
  return [...labels].sort();
}

function parseEpic(filePath) {
  const markdown = readText(filePath);
  const externalId = parseExternalIdFromFile(filePath).match(/^(EPIC-\d+)/i)?.[1]?.toUpperCase() || parseExternalIdFromFile(filePath);
  const title = parseTitle(markdown, externalId);

  return {
    source: "epic-file",
    sourceFile: filePath,
    externalId,
    issueType: "Epic",
    summary: title,
    status: "Backlog",
    statusReason: "Epic document imported as backlog unless already created in Jira",
    fixVersion: null,
    labels: ["autoforge", "epic", "migration"],
    fields: {},
    description: markdown.trim(),
    warnings: [],
  };
}

function parseStoryBreakdown(filePath) {
  const markdown = readText(filePath);
  const matches = [...markdown.matchAll(/^##\s+(EPIC\d+-STORY-\d+)\s*:\s*(.+)$/gm)];
  const issues = [];

  for (let index = 0; index < matches.length; index += 1) {
    const match = matches[index];
    const next = matches[index + 1];
    const externalId = match[1];
    const summary = match[2].trim();
    const body = markdown.slice(match.index + match[0].length, next ? next.index : markdown.length).trim();
    const parentExternalId = externalId.match(/^(EPIC\d+)-/)?.[1]?.replace("EPIC", "EPIC-") || null;
    const issueType = body.match(/^Issue type:\s*`?([^`\n]+)`?/m)?.[1]?.trim() || "Story";
    const priority = body.match(/^Priority:\s*`?([^`\n]+)`?/m)?.[1]?.trim() || null;
    const goal = body.match(/^Goal:\s*(.+)$/m)?.[1]?.trim() || "";
    const dependencies = body.match(/^Dependencies:\s*(.+)$/m)?.[1]?.trim() || "";
    const output = body.match(/^Output:\s*(.+)$/m)?.[1]?.trim() || "";

    issues.push({
      source: "epic-story-breakdown",
      sourceFile: filePath,
      externalId,
      parentExternalId,
      issueType,
      summary,
      status: "Backlog",
      statusReason: "Story breakdown entries start in Backlog",
      fixVersion: null,
      labels: ["autoforge", "epic-story", "migration"],
      fields: {
        priority,
        goal,
        acceptanceCriteria: acceptanceCriteriaFromBlock(body),
        dependencies: dependencies || null,
        output: output || null,
      },
      description: body,
      warnings: [],
    });
  }

  return issues;
}

function acceptanceCriteriaFromBlock(body) {
  const start = body.indexOf("Acceptance criteria:");
  if (start === -1) {
    return [];
  }

  const after = body.slice(start + "Acceptance criteria:".length);
  const stop = after.search(/^(Dependencies|Output):/m);
  const criteriaBlock = stop === -1 ? after : after.slice(0, stop);
  return bulletItems(criteriaBlock);
}

function buildPlan() {
  const releaseData = parseReleases();
  const taskFiles = listMarkdownFiles("docs/tasks")
    .filter((filePath) => /\/(AUTO|BUG)-\d+\.md$/.test(filePath));
  const epicFiles = listMarkdownFiles("docs/epics")
    .filter((filePath) => /\/EPIC-\d+-.+\.md$/.test(filePath) && !filePath.includes("-stories.md"));
  const storyBreakdownFiles = listMarkdownFiles("docs/epics")
    .filter((filePath) => filePath.includes("-stories.md"));

  const issues = [
    ...epicFiles.map(parseEpic),
    ...storyBreakdownFiles.flatMap(parseStoryBreakdown),
    ...taskFiles.map((filePath) => parseTask(filePath, releaseData.taskVersions)),
  ];

  const duplicateIds = duplicates(issues.map((issue) => issue.externalId));
  const warnings = [
    ...releaseData.warnings,
    ...duplicateIds.map((id) => `Duplicate issue external ID in import plan: ${id}`),
  ];

  return {
    metadata: {
      generatedAt: new Date().toISOString(),
      generator: "ops/jira/generate-import-plan.mjs",
      mode: "dry-run",
      jiraApiCalls: false,
      issueCount: issues.length,
      taskFileCount: taskFiles.length,
      epicFileCount: epicFiles.length,
      storyBreakdownFileCount: storyBreakdownFiles.length,
    },
    warnings,
    issues,
  };
}

function duplicates(values) {
  const seen = new Set();
  const dupes = new Set();
  for (const value of values) {
    if (seen.has(value)) {
      dupes.add(value);
    }
    seen.add(value);
  }
  return [...dupes].sort();
}

function writeJson(filePath, value, pretty) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
  const json = JSON.stringify(value, null, pretty ? 2 : 0);
  fs.writeFileSync(filePath, `${json}\n`, "utf8");
}

try {
  const args = parseArgs(process.argv.slice(2));
  const plan = buildPlan();
  writeJson(args.output, plan, args.pretty);
  console.log(`Jira dry-run import plan written to ${path.relative(rootDir, args.output)}`);
  console.log(`Issues: ${plan.metadata.issueCount}`);
  console.log(`Warnings: ${plan.warnings.length + plan.issues.reduce((count, issue) => count + issue.warnings.length, 0)}`);
} catch (error) {
  console.error(error instanceof Error ? error.message : String(error));
  process.exit(1);
}
