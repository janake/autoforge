import { useEffect, useMemo, useState } from "react";
import { getRuntimeConfig } from "./runtime-config";
import { initializeKeycloak, loadAuthedJson, signIn, signOut } from "./auth/keycloak";
import type { BackendMeResponse } from "./types";

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

function isKeycloakCallback(): boolean {
  const searchParams = new URLSearchParams(window.location.search);
  const hashParams = new URLSearchParams(window.location.hash.replace(/^#/, ""));

  return [searchParams, hashParams].some(
    (params) => params.has("state") && (params.has("code") || params.has("error")),
  );
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

function PrivateWorkspace({
  profile,
  onSignOut,
}: {
  profile: BackendMeResponse;
  onSignOut: () => void;
}) {
  const config = useMemo(() => getRuntimeConfig(), []);

  const persona = profile.roles.includes("admin")
    ? "operator"
    : profile.roles.length > 0
      ? "builder"
      : "member";

  return (
    <>
      <section className="hero">
        <div className="hero-copy">
          <p className="eyebrow">Autoforge</p>
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

      <section className="status-grid" aria-label="Platform signals">
        {publicSignals.map((signal) => (
          <article className="card" key={signal.title}>
            <p className="card-kicker">ready</p>
            <h2>{signal.title}</h2>
            <p>{signal.body}</p>
          </article>
        ))}
      </section>

      <section className="workspace-grid" aria-label="User workspace">
        <article className="workspace-panel">
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

        <article className="workspace-panel">
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

        <article className="workspace-panel">
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
      </section>
    </>
  );
}

function App() {
  const [session, setSession] = useState<SessionState>({ status: "loading" });
  const apiRouteError =
    session.status === "error" && /Request failed with 404/.test(session.message);

  useEffect(() => {
    let cancelled = false;

    const bootstrap = async () => {
      try {
        const client = isKeycloakCallback()
          ? await initializeKeycloak()
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
        <>
          <PublicHero onSignIn={() => void signIn()} />
          <section className="status-grid" aria-label="Platform signals">
            {publicSignals.map((signal) => (
              <article className="card" key={signal.title}>
                <p className="card-kicker">loading</p>
                <h2>{signal.title}</h2>
                <p>{signal.body}</p>
              </article>
            ))}
          </section>
        </>
      )}

      {session.status === "public" && (
        <>
          <PublicHero onSignIn={() => void signIn()} />
          <section className="status-grid" aria-label="Platform signals">
            {publicSignals.map((signal) => (
              <article className="card" key={signal.title}>
                <p className="card-kicker">public</p>
                <h2>{signal.title}</h2>
                <p>{signal.body}</p>
              </article>
            ))}
          </section>
        </>
      )}

      {session.status === "ready" && (
        <PrivateWorkspace profile={session.profile} onSignOut={() => void signOut()} />
      )}

      {session.status === "error" && (
        <>
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
        </>
      )}
    </main>
  );
}

export default App;
