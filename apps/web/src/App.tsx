import { useEffect, useMemo, useState, type FormEvent, type ReactNode } from "react";
import { getRuntimeConfig } from "./runtime-config";
import { initializeKeycloak, loadAuthedJson, postAuthedJson, signIn, signOut } from "./auth/keycloak";
import type { BackendMeResponse, CreateJobRequest, CreateJobResponse, JobResponse } from "./types";

type SessionState =
  | { status: "loading" }
  | { status: "public" }
  | { status: "ready"; profile: BackendMeResponse }
  | { status: "error"; message: string };

const publicSignals = [
  {
    title: "Public homepage",
    body: "The landing page stays open and does not force authentication.",
  },
  {
    title: "Signed-in workspace",
    body: "After sign in, the app loads the protected workspace and user profile.",
  },
  {
    title: "JWT-backed APIs",
    body: "The backend still validates bearer tokens for private endpoints.",
  },
];

const dashboardNav = [
  { label: "Overview", href: "#overview" },
  { label: "Jobs", href: "#jobs" },
  { label: "Status", href: "#job-status" },
  { label: "Jira", href: "#jira" },
  { label: "Git", href: "#git" },
  { label: "Runtime", href: "#runtime" },
];

function isKeycloakCallback(): boolean {
  const searchParams = new URLSearchParams(window.location.search);
  const hashParams = new URLSearchParams(window.location.hash.replace(/^#/, ""));

  return [searchParams, hashParams].some((params) => {
    return ["code", "error", "session_state", "iss"].some((key) => params.has(key));
  });
}

function PublicHero({ onSignIn }: { onSignIn: () => void }) {
  const config = useMemo(() => getRuntimeConfig(), []);

  return (
    <section className="hero">
      <div className="hero-copy">
        <p className="eyebrow">Autoforge</p>
        <h1>Public landing first, protected workspace after sign in.</h1>
        <p className="lead">
          The homepage is open to everyone. When you need the private workspace, sign in with
          Keycloak and continue into the authenticated area.
        </p>

        <div className="hero-actions">
          <button className="primary-button" type="button" onClick={onSignIn}>
            Sign in
          </button>
          <span className="hero-note">Identity provider: {config.keycloak.url}</span>
        </div>
      </div>
    </section>
  );
}

type JobSubmissionState =
  | { status: "idle" }
  | { status: "submitting" }
  | { status: "success"; jobId: string; jiraIssueKey: string }
  | { status: "error"; message: string };

type TrackedJobState =
  | { status: "idle" }
  | { status: "loading"; jobId: string }
  | { status: "ready"; job: JobResponse }
  | { status: "error"; jobId: string; message: string };

function readJobIdFromUrl(): string {
  return new URLSearchParams(window.location.search).get("jobId") ?? "";
}

function formatTimestamp(iso: string): string {
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(iso));
}

function syncJobIdInUrl(jobId: string): void {
  const url = new URL(window.location.href);

  if (jobId) {
    url.searchParams.set("jobId", jobId);
  } else {
    url.searchParams.delete("jobId");
  }

  window.history.replaceState({}, "", url);
}

function JobStatusPanel({ jobId }: { jobId: string }) {
  const [state, setState] = useState<TrackedJobState>({ status: "loading", jobId });

  useEffect(() => {
    let cancelled = false;
    let timer: number | undefined;

    const poll = async () => {
      try {
        const job = await loadAuthedJson<JobResponse>(`/v1/jobs/${jobId}`);

        if (cancelled) {
          return;
        }

        setState({ status: "ready", job });

        if (job.status !== "PR_OPENED" && job.status !== "FAILED") {
          timer = window.setTimeout(poll, 4000);
        }
      } catch (error) {
        if (cancelled) {
          return;
        }

        setState({
          status: "error",
          jobId,
          message: error instanceof Error ? error.message : "Unable to load job status.",
        });
        timer = window.setTimeout(poll, 6000);
      }
    };

    setState({ status: "loading", jobId });
    void poll();

    return () => {
      cancelled = true;

      if (timer !== undefined) {
        window.clearTimeout(timer);
      }
    };
  }, [jobId]);

  const resolvedStatus = state.status === "ready" ? state.job.status : "LOADING";
  const statusTone =
    resolvedStatus === "PR_OPENED"
      ? "done"
      : resolvedStatus === "FAILED"
        ? "failed"
        : resolvedStatus === "LOADING"
          ? "pending"
          : "running";

  return (
    <article className="workspace-panel job-status-panel" id="job-status">
      <div className="section-head">
        <h2>Job status</h2>
        <span className={`pill status-pill ${statusTone}`}>{statusTone}</span>
      </div>
      <p className="muted">Polling job {jobId} until the PR link becomes available.</p>

      {state.status === "error" && <p className="error-title">{state.message}</p>}

      {state.status === "ready" ? (
        <dl className="profile-list compact">
          <div>
            <dt>Job</dt>
            <dd>{state.job.jobId}</dd>
          </div>
          <div>
            <dt>Status</dt>
            <dd>{state.job.status}</dd>
          </div>
          <div>
            <dt>Repository</dt>
            <dd>{state.job.targetRepository}</dd>
          </div>
          <div>
            <dt>Base branch</dt>
            <dd>{state.job.baseBranch}</dd>
          </div>
          <div>
            <dt>Updated</dt>
            <dd>{formatTimestamp(state.job.updatedAt)}</dd>
          </div>
          <div>
            <dt>PR</dt>
            <dd>
              {state.job.prUrl ? (
                <a className="job-link" href={state.job.prUrl} target="_blank" rel="noreferrer">
                  Open pull request
                </a>
              ) : (
                "waiting for PR_OPENED"
              )}
            </dd>
          </div>
          {state.job.errorMessage && (
            <div>
              <dt>Failure</dt>
              <dd>{state.job.errorMessage}</dd>
            </div>
          )}
        </dl>
      ) : state.status === "loading" ? (
        <p className="muted">Loading job details...</p>
      ) : (
        <p className="muted">Retrying the job lookup in a few seconds.</p>
      )}
    </article>
  );
}

function PromptSubmissionPanel({ onJobCreated }: { onJobCreated: (jobId: string) => void }) {
  const [form, setForm] = useState<CreateJobRequest>({
    jiraIssueKey: "AUTO-225",
    prompt: "",
    targetRepository: "",
    baseBranch: "main",
  });
  const [submission, setSubmission] = useState<JobSubmissionState>({ status: "idle" });

  const updateField = <K extends keyof CreateJobRequest>(field: K, value: string) => {
    setForm((current) => ({ ...current, [field]: value }));
  };

  const onSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSubmission({ status: "submitting" });

    try {
      const response = await postAuthedJson<CreateJobResponse>("/v1/jobs", form);
      setSubmission({ status: "success", jobId: response.jobId, jiraIssueKey: response.jiraIssueKey });
      onJobCreated(response.jobId);
    } catch (error) {
      setSubmission({
        status: "error",
        message: error instanceof Error ? error.message : "Unable to submit prompt.",
      });
    }
  };

  return (
    <article className="workspace-panel" id="prompt">
      <div className="section-head">
        <h2>Submit prompt</h2>
        <span className="pill">Jira</span>
      </div>
      <p className="muted">
        Enter the Jira issue key, prompt, target repository, and base branch. The backend will
        create the queued job from that request.
      </p>

      <form className="prompt-form" onSubmit={onSubmit}>
        <label>
          <span>Jira issue key</span>
          <input
            name="jiraIssueKey"
            type="text"
            value={form.jiraIssueKey}
            onChange={(event) => updateField("jiraIssueKey", event.target.value)}
            placeholder="AUTO-225"
            autoComplete="off"
            required
          />
        </label>

        <label>
          <span>Prompt</span>
          <textarea
            name="prompt"
            value={form.prompt}
            onChange={(event) => updateField("prompt", event.target.value)}
            placeholder="Describe the task you want the agent to carry out"
            rows={6}
            required
          />
        </label>

        <label>
          <span>Target repository</span>
          <input
            name="targetRepository"
            type="text"
            value={form.targetRepository}
            onChange={(event) => updateField("targetRepository", event.target.value)}
            placeholder="owner/repo"
            autoComplete="off"
            required
          />
        </label>

        <label>
          <span>Base branch</span>
          <input
            name="baseBranch"
            type="text"
            value={form.baseBranch}
            onChange={(event) => updateField("baseBranch", event.target.value)}
            placeholder="main"
            autoComplete="off"
            required
          />
        </label>

        <div className="prompt-actions">
          <button className="primary-button" type="submit" disabled={submission.status === "submitting"}>
            {submission.status === "submitting" ? "Submitting..." : "Create job"}
          </button>
          <span className="muted">
            The request is authenticated with the current Keycloak session.
          </span>
        </div>
      </form>

      {submission.status === "success" && (
        <p className="success-title">
          Job {submission.jiraIssueKey} created as {submission.jobId}.
        </p>
      )}

      {submission.status === "error" && <p className="error-title">{submission.message}</p>}
    </article>
  );
}

function DashboardShell({
  mode,
  actionLabel,
  onAction,
  children,
}: {
  mode: string;
  actionLabel: string;
  onAction: () => void;
  children: ReactNode;
}) {
  return (
    <>
      <header className="dashboard-shell-header">
        <div>
          <p className="eyebrow">Autoforge</p>
          <h1>MVP control room</h1>
          <p className="shell-subtitle">Prompt-first Jira, job lifecycle, Git broker, and release flow.</p>
        </div>

        <nav className="dashboard-nav" aria-label="Primary navigation">
          {dashboardNav.map((item) => (
            <a key={item.href} href={item.href}>
              {item.label}
            </a>
          ))}
        </nav>

        <div className="dashboard-shell-actions">
          <span className="pill">{mode}</span>
          <button className="primary-button" type="button" onClick={onAction}>
            {actionLabel}
          </button>
        </div>
      </header>

      {children}
    </>
  );
}

function PrivateWorkspace({
  profile,
  onSignOut,
  jobId,
  onJobCreated,
}: {
  profile: BackendMeResponse;
  onSignOut: () => void;
  jobId: string;
  onJobCreated: (jobId: string) => void;
}) {
  const config = useMemo(() => getRuntimeConfig(), []);

  const persona = profile.roles.includes("admin")
    ? "operator"
    : profile.roles.length > 0
      ? "builder"
      : "member";

  return (
    <DashboardShell mode="authenticated" actionLabel="Sign out" onAction={onSignOut}>
      <section className="hero">
        <div className="hero-copy">
          <h1>Signed-in workspace for building the platform itself.</h1>
          <p className="lead">
            Keycloak backs the session, the backend validates tokens, and the UI adapts to the
            user profile returned from the private API.
          </p>
        </div>

        <div className="hero-panel">
          <div className="hero-panel-header">
            <span className="status-dot" />
            <span>Session</span>
          </div>
          <div className="hero-panel-body">
            <div>
              <p className="panel-label">Identity provider</p>
              <p className="panel-value">{config.keycloak.url}</p>
            </div>
            <div>
              <p className="panel-label">Client</p>
              <p className="panel-value">{config.keycloak.clientId}</p>
            </div>
            <div>
              <p className="panel-label">Workspace mode</p>
              <p className="panel-value">{persona}</p>
            </div>
          </div>
        </div>
      </section>

      <section className="status-grid" aria-label="Platform signals" id="overview">
        {publicSignals.map((signal) => (
          <article className="card" key={signal.title}>
            <p className="card-kicker">ready</p>
            <h2>{signal.title}</h2>
            <p>{signal.body}</p>
          </article>
        ))}
      </section>

      <section className="workspace-grid" aria-label="User workspace">
        <PromptSubmissionPanel
          onJobCreated={(createdJobId) => {
            onJobCreated(createdJobId);
            syncJobIdInUrl(createdJobId);
          }}
        />

        {jobId && <JobStatusPanel jobId={jobId} />}

        <article className="workspace-panel" id="jobs">
          <div className="section-head">
            <h2>Profile</h2>
            <button className="secondary-button" type="button" onClick={onSignOut}>
              Sign out
            </button>
          </div>

          <dl className="profile-list">
            <div>
              <dt>User</dt>
              <dd>{profile.username || profile.subject}</dd>
            </div>
            <div>
              <dt>Email</dt>
              <dd>{profile.email || "not provided"}</dd>
            </div>
            <div>
              <dt>Roles</dt>
              <dd>{profile.roles.length ? profile.roles.join(", ") : "none"}</dd>
            </div>
            <div>
              <dt>Issuer</dt>
              <dd>{profile.claims.issuer}</dd>
            </div>
          </dl>
        </article>

        <article className="workspace-panel" id="jira">
          <div className="section-head">
            <h2>Current API</h2>
            <span className="pill">private</span>
          </div>
          <p className="muted">
            The frontend calls the backend through the public gateway with the Keycloak bearer
            token attached.
          </p>
          <dl className="profile-list compact">
            <div>
              <dt>Subject</dt>
              <dd>{profile.subject}</dd>
            </div>
            <div>
              <dt>Audience</dt>
              <dd>{profile.claims.audience}</dd>
            </div>
            <div>
              <dt>Authorized party</dt>
              <dd>{profile.claims.authorizedParty}</dd>
            </div>
          </dl>
        </article>

        <article className="workspace-panel" id="runtime">
          <div className="section-head">
            <h2>Runtime</h2>
            <span className="pill">docker</span>
          </div>
          <p className="muted">
            Runtime config is loaded from <code>/config.js</code>. Backend issuer validation is
            driven by the private host environment.
          </p>
          <dl className="profile-list compact">
            <div>
              <dt>Keycloak URL</dt>
              <dd>{config.keycloak.url}</dd>
            </div>
            <div>
              <dt>API base</dt>
              <dd>{config.apiBaseUrl}</dd>
            </div>
            <div>
              <dt>Mode</dt>
              <dd>authenticated</dd>
            </div>
          </dl>
        </article>

        <article className="workspace-panel" id="git">
          <div className="section-head">
            <h2>Git broker</h2>
            <span className="pill">ready</span>
          </div>
          <p className="muted">
            Branch preparation, patch application, branch push, and PR publish are handled by the
            backend git broker pipeline.
          </p>
          <dl className="profile-list compact">
            <div>
              <dt>Branch flow</dt>
              <dd>feature branch, commit, push, PR</dd>
            </div>
            <div>
              <dt>Status flow</dt>
              <dd>QUEUED → RUNNING → PATCH_GENERATED → PR_OPENED</dd>
            </div>
          </dl>
        </article>
      </section>
    </DashboardShell>
  );
}

function App() {
  const [session, setSession] = useState<SessionState>({ status: "loading" });
  const [jobId, setJobId] = useState(() => readJobIdFromUrl());
  const apiRouteError =
    session.status === "error" && /Request failed with 404/.test(session.message);

  useEffect(() => {
    let cancelled = false;

    const bootstrap = async () => {
      try {
        const client = isKeycloakCallback()
          ? await initializeKeycloak("login-required")
          : await initializeKeycloak("check-sso");

        if (!client.authenticated) {
          if (!cancelled) {
            setSession({ status: "public" });
          }
          return;
        }

        const profile = await loadAuthedJson<BackendMeResponse>("/v1/me");

        if (!cancelled) {
          setSession({ status: "ready", profile });
          client.onAuthLogout = () => {
            window.location.reload();
          };
        }
      } catch (error) {
        if (!cancelled) {
          setSession({
            status: "error",
            message: error instanceof Error ? error.message : "Unable to initialize Keycloak.",
          });
        }
      }
    };

    void bootstrap();

    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <main className="app-shell">
      {session.status === "loading" && (
        <DashboardShell mode="loading" actionLabel="Sign in" onAction={() => void signIn()}>
          <PublicHero onSignIn={() => void signIn()} />
          <section className="status-grid" aria-label="Platform signals" id="overview">
            {publicSignals.map((signal) => (
              <article className="card" key={signal.title}>
                <p className="card-kicker">loading</p>
                <h2>{signal.title}</h2>
                <p>{signal.body}</p>
              </article>
            ))}
          </section>
        </DashboardShell>
      )}

      {session.status === "public" && (
        <DashboardShell mode="public" actionLabel="Sign in" onAction={() => void signIn()}>
          <PublicHero onSignIn={() => void signIn()} />
          <section className="status-grid" aria-label="Platform signals" id="overview">
            {publicSignals.map((signal) => (
              <article className="card" key={signal.title}>
                <p className="card-kicker">public</p>
                <h2>{signal.title}</h2>
                <p>{signal.body}</p>
              </article>
            ))}
          </section>
        </DashboardShell>
      )}

      {session.status === "ready" && (
        <PrivateWorkspace
          profile={session.profile}
          onSignOut={() => void signOut()}
          jobId={jobId}
          onJobCreated={setJobId}
        />
      )}

      {session.status === "error" && (
        <DashboardShell mode="error" actionLabel="Sign in" onAction={() => void signIn()}>
          <PublicHero onSignIn={() => void signIn()} />
          <section className="workspace-grid">
            <article className="workspace-panel">
              <div className="section-head">
                <h2>{apiRouteError ? "API route failed" : "Auth init failed"}</h2>
                <span className="pill">error</span>
              </div>
              <p className="error-title">
                {apiRouteError ? "Backend API route is not available." : "Unable to initialize Keycloak."}
              </p>
              <p className="muted">{session.message}</p>
            </article>
          </section>
        </DashboardShell>
      )}
    </main>
  );
}

export default App;
