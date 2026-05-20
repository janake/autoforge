import { useEffect, useMemo, useState, type FormEvent, type ReactNode } from "react";
import { getRuntimeConfig } from "./runtime-config";
import { deleteAuthedJson, initializeKeycloak, loadAuthedJson, postAuthedFormData, postAuthedJson, putAuthedJson, signIn, signOut } from "./auth/keycloak";
import type {
  BackendMeResponse,
  CreateJobResponse,
  LearningContentGenerationResponse,
  LearningIngestionResponse,
  LearningImageAssetResponse,
  LearningSourceVersionReference,
  LearningQuestionAttemptAnswerPayload,
  LearningQuestionAttemptResponse,
  LearningQuestionPayload,
  LearningQuestionDisputeResponse,
  LearningQuestionProgressResponse,
  LearningQuestionSetPayload,
  LearningQuestionSetSettingsRequest,
  LearningMaterialAssignmentRequest,
  LearningMaterialResponse,
  LearningMaterialSourceResponse,
  JobResponse,
  PromptDraftResponse,
  PromptIntent,
} from "./types";

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
  { label: "Learning", href: "#learning" },
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
  | { status: "success"; jobId: string }
  | { status: "error"; message: string };

type TrackedJobState =
  | { status: "idle" }
  | { status: "loading"; jobId: string }
  | { status: "ready"; job: JobResponse }
  | { status: "error"; jobId: string; message: string };

function readJobIdFromUrl(): string {
  return new URLSearchParams(window.location.search).get("jobId") ?? "";
}

function readPathname(): string {
  return window.location.pathname;
}

function parseLearningMaterialPath(pathname: string): string | null {
  const segments = pathname.split("/").filter(Boolean);
  if (segments.length === 3 && segments[0] === "learning" && segments[1] === "materials") {
    return decodeURIComponent(segments[2]);
  }
  return null;
}

function syncPathnameInUrl(pathname: string): void {
  const url = new URL(window.location.href);
  url.pathname = pathname;
  window.history.pushState({}, "", url);
}

function formatTimestamp(iso: string): string {
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(iso));
}

function formatLocalDateTime(iso: string | null): string {
  if (!iso) {
    return "";
  }

  const date = new Date(iso);
  const offsetMs = date.getTimezoneOffset() * 60000;
  return new Date(date.getTime() - offsetMs).toISOString().slice(0, 16);
}

function parseLocalDateTime(value: string): string | null {
  const trimmed = value.trim();
  if (!trimmed) {
    return null;
  }

  return new Date(trimmed).toISOString();
}

type LearningWorkspaceItem = LearningMaterialResponse & {
  generations: LearningContentGenerationResponse[];
};

type TeacherProgressRow = {
  progress: LearningQuestionProgressResponse;
  generation: LearningContentGenerationResponse | null;
  latestAttempt: LearningQuestionAttemptResponse | null;
  openDisputeCount: number;
};

type LearningWorkspaceState =
  | { status: "loading" }
  | { status: "ready"; materials: LearningWorkspaceItem[] }
  | { status: "error"; message: string };

function latestByType(
  generations: LearningContentGenerationResponse[],
  generationType: LearningContentGenerationResponse["generationType"]
): LearningContentGenerationResponse | null {
  return generations.find((generation) => generation.generationType === generationType) ?? null;
}

function questionSetStatusTone(status: LearningContentGenerationResponse["questionSetStatus"]): string {
  switch (status) {
    case "PUBLISHED":
      return "done";
    case "ARCHIVED":
      return "failed";
    default:
      return "pending";
  }
}

function parseAssignmentList(value: string): string[] {
  return value
    .split(/[\n,]/)
    .map((entry) => entry.trim())
    .filter((entry) => entry.length > 0)
    .filter((entry, index, entries) => entries.indexOf(entry) === index);
}

function canCreateLearningContent(roles: string[]): boolean {
  return roles
    .map((role) => role.trim().toLowerCase().replace(/-/g, "_"))
    .some((role) => role === "admin" || role === "teacher" || role === "learning_teacher");
}

function LearningWorkspacePanel({ roles, onOpenMaterial }: { roles: string[]; onOpenMaterial: (materialId: string) => void }) {
  const [state, setState] = useState<LearningWorkspaceState>({ status: "loading" });
  const [refreshToken, setRefreshToken] = useState(0);
  const [uploadStatus, setUploadStatus] = useState<string | null>(null);
  const [uploadError, setUploadError] = useState<string | null>(null);
  const canUploadMaterials = canCreateLearningContent(roles);

  useEffect(() => {
    let cancelled = false;

    const loadWorkspace = async () => {
      try {
        const materials = await loadAuthedJson<LearningMaterialResponse[]>("/v1/learning/materials");
        const items = await Promise.all(
          materials.map(async (material) => ({
            ...material,
            generations: await loadAuthedJson<LearningContentGenerationResponse[]>(`/v1/learning/materials/${material.id}/generations`),
          }))
        );

        if (!cancelled) {
          setState({ status: "ready", materials: items });
        }
      } catch (error) {
        if (!cancelled) {
          setState({
            status: "error",
            message: error instanceof Error ? error.message : "Unable to load learning workspace.",
          });
        }
      }
    };

    void loadWorkspace();

    return () => {
      cancelled = true;
    };
  }, [refreshToken]);

  const uploadMaterial = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setUploadError(null);
    setUploadStatus("Uploading material...");

    const form = event.currentTarget;
    const formData = new FormData(form);

    try {
      await postAuthedFormData<LearningMaterialResponse>("/v1/learning/materials", formData);
      form.reset();
      setRefreshToken((value) => value + 1);
      setUploadStatus("Material uploaded.");
    } catch (error) {
      setUploadStatus(null);
      setUploadError(error instanceof Error ? error.message : "Unable to upload learning material.");
    }
  };

  return (
    <article className="workspace-panel learning-panel" id="learning">
      <div className="section-head">
        <h2>Learning</h2>
        <span className="pill">private</span>
      </div>
      <p className="muted">
        Your own learning materials, generated questions, and summaries live here. The list is scoped to the signed-in user only.
      </p>

      {canUploadMaterials ? (
        <form className="prompt-form learning-upload-form" onSubmit={uploadMaterial}>
          <div className="learning-upload-grid">
            <label>
              <span>Title</span>
              <input name="title" placeholder="Algebra basics" />
            </label>
            <label>
              <span>Description</span>
              <input name="description" placeholder="Short note about the material" />
            </label>
            <label>
              <span>File</span>
              <input name="file" type="file" accept=".pdf,.txt,.md,.markdown" required />
            </label>
          </div>
          <div className="prompt-actions">
            <button className="primary-button" type="submit">Upload material</button>
            <span className="muted">PDF, TXT, Markdown supported.</span>
          </div>
          {uploadStatus && <p className="success-title">{uploadStatus}</p>}
          {uploadError && <p className="error-title">{uploadError}</p>}
        </form>
      ) : (
        <div className="learning-empty-state">
          <h3>Student learning mode</h3>
          <p>Only teacher/admin roles can create learning materials. Assigned materials remain available below.</p>
        </div>
      )}

      {state.status === "loading" && <p className="muted">Loading your learning workspace...</p>}
      {state.status === "error" && <p className="error-title">{state.message}</p>}

      {state.status === "ready" && state.materials.length === 0 && (
        <div className="learning-empty-state">
          <h3>No learning materials yet</h3>
          <p>Upload a file to start building your personal learning workspace.</p>
        </div>
      )}

      {state.status === "ready" && state.materials.length > 0 && (
        <div className="learning-material-grid">
          {state.materials.map((material) => {
            const latestQuestions = latestByType(material.generations, "QUESTION_SET");
            const latestSummary = latestByType(material.generations, "SUMMARY");

            return (
              <article className="learning-material-card" key={material.id}>
                <div className="section-head">
                  <h3>{material.title}</h3>
                  <span className="pill">{material.canManageAssignments ? "owned" : "shared"}</span>
                </div>
                <p className="muted">{material.description || material.originalFilename || "No description"}</p>
                <div className="learning-card-actions">
                  <button className="secondary-button" type="button" onClick={() => onOpenMaterial(material.id)}>
                    Open detail
                  </button>
                </div>

                <div className="learning-generation-list">
                  <section>
                    <h4>Questions</h4>
                    <p>{latestQuestions ? latestQuestions.content : "No questions generated yet."}</p>
                  </section>
                  <section>
                    <h4>Summary</h4>
                    <p>{latestSummary ? latestSummary.content : "No summary generated yet."}</p>
                  </section>
                </div>
              </article>
            );
          })}
        </div>
      )}
    </article>
  );
}

function parseQuestionSet(structuredContent: string | null): LearningQuestionSetPayload | null {
  if (!structuredContent) {
    return null;
  }

  try {
    return JSON.parse(structuredContent) as LearningQuestionSetPayload;
  } catch {
    return null;
  }
}

function resolveQuestionCorrectOptionIndexes(question: LearningQuestionPayload): number[] {
  if (question.correctOptionIndexes && question.correctOptionIndexes.length > 0) {
    return question.correctOptionIndexes;
  }

  return question.correctOptionIndex == null ? [] : [question.correctOptionIndex];
}

function resolveQuestionAnswerType(question: LearningQuestionPayload): LearningQuestionPayload["answerType"] {
  if (question.answerType) {
    return question.answerType;
  }

  return resolveQuestionCorrectOptionIndexes(question).length > 1 ? "MULTI_CORRECT" : "SINGLE_CORRECT";
}

function parseAttemptAnswers(rawAnswers: string): LearningQuestionAttemptAnswerPayload[] | null {
  try {
    const parsed = JSON.parse(rawAnswers) as LearningQuestionAttemptAnswerPayload[];
    return Array.isArray(parsed) ? parsed : null;
  } catch {
    return null;
  }
}

function disputeStatusTone(status: LearningQuestionDisputeResponse["status"]): string {
  switch (status) {
    case "ACCEPTED":
      return "done";
    case "REJECTED":
      return "failed";
    default:
      return "pending";
  }
}

function progressStatusTone(status: LearningQuestionProgressResponse["status"]): string {
  switch (status) {
    case "REVIEWED":
    case "COMPLETED":
      return "done";
    default:
      return "pending";
  }
}

function questionSetAttemptCount(attempts: LearningQuestionAttemptResponse[], generationId: string): number {
  return attempts.filter((attempt) => attempt.generationId === generationId).length;
}

function questionSetBlockedReason(deadlineAt: string | null, maxAttempts: number | null, attemptCount: number): string | null {
  if (deadlineAt && new Date(deadlineAt).getTime() < Date.now()) {
    return "Deadline passed.";
  }

  if (maxAttempts !== null && attemptCount >= maxAttempts) {
    return "Maximum attempts reached.";
  }

  return null;
}

function normalizeGroupFilter(value: string): string {
  return value.trim().replace(/^\//, "");
}

function LearningMaterialDetailPanel({ materialId, onBack }: { materialId: string; onBack: () => void }) {
  const [refreshToken, setRefreshToken] = useState(0);
  const [sourceStatus, setSourceStatus] = useState<string | null>(null);
  const [sourceError, setSourceError] = useState<string | null>(null);
  const [generationStatus, setGenerationStatus] = useState<string | null>(null);
  const [generationError, setGenerationError] = useState<string | null>(null);
  const [assignmentStatus, setAssignmentStatus] = useState<string | null>(null);
  const [assignmentError, setAssignmentError] = useState<string | null>(null);
  const [studentSubjectsText, setStudentSubjectsText] = useState("");
  const [groupNamesText, setGroupNamesText] = useState("");
  const [disputeStatus, setDisputeStatus] = useState<string | null>(null);
  const [disputeError, setDisputeError] = useState<string | null>(null);
  const [progressGroupFilter, setProgressGroupFilter] = useState("all");
  const [state, setState] = useState<
    | { status: "loading" }
    | {
        status: "ready";
        material: LearningMaterialResponse;
        ingestion: LearningIngestionResponse;
        generations: LearningContentGenerationResponse[];
        attempts: LearningQuestionAttemptResponse[];
        disputes: LearningQuestionDisputeResponse[];
      }
    | { status: "error"; message: string }
  >({ status: "loading" });

  useEffect(() => {
    if (state.status !== "ready") {
      return;
    }

    setStudentSubjectsText(state.material.studentSubjects.join(", "));
    setGroupNamesText(state.material.groupNames.join(", "));
  }, [
    refreshToken,
    state.status,
    state.status === "ready" ? state.material.studentSubjects.join("\u0000") : null,
    state.status === "ready" ? state.material.groupNames.join("\u0000") : null,
  ]);

  useEffect(() => {
    let cancelled = false;

    const loadDetail = async () => {
      try {
        const [material, ingestion, generations, attempts, disputes] = await Promise.all([
          loadAuthedJson<LearningMaterialResponse>(`/v1/learning/materials/${materialId}`),
          loadAuthedJson<LearningIngestionResponse>(`/v1/learning/materials/${materialId}/ingestion`),
          loadAuthedJson<LearningContentGenerationResponse[]>(`/v1/learning/materials/${materialId}/generations`),
          loadAuthedJson<LearningQuestionAttemptResponse[]>(`/v1/learning/materials/${materialId}/question-attempts`),
          loadAuthedJson<LearningQuestionDisputeResponse[]>(`/v1/learning/materials/${materialId}/question-disputes`),
        ]);

        if (!cancelled) {
          setState({ status: "ready", material, ingestion, generations, attempts, disputes });
        }
      } catch (error) {
        if (!cancelled) {
          setState({
            status: "error",
            message: error instanceof Error ? error.message : "Unable to load learning material detail.",
          });
        }
      }
    };

    void loadDetail();

    return () => {
      cancelled = true;
    };
  }, [materialId, refreshToken]);

  const progressGroupOptions = useMemo(() => {
    if (state.status !== "ready") {
      return ["all"];
    }

    const groups = new Set<string>();
    state.material.groupNames.map(normalizeGroupFilter).filter(Boolean).forEach((group) => groups.add(group));
    state.material.progressEntries.forEach((progress) => {
      progress.studentGroups.map(normalizeGroupFilter).filter(Boolean).forEach((group) => groups.add(group));
    });
    return ["all", ...Array.from(groups).sort((left, right) => left.localeCompare(right))];
  }, [state]);

  const teacherProgressRows = useMemo<TeacherProgressRow[]>(() => {
    if (state.status !== "ready") {
      return [];
    }

    return state.material.progressEntries
      .filter((progress) => progressGroupFilter === "all" || progress.studentGroups.some((group) => normalizeGroupFilter(group) === progressGroupFilter))
      .map((progress) => {
        const generation = state.generations.find((item) => item.id === progress.generationId) ?? null;
        const matchingAttempts = state.attempts.filter((attempt) => attempt.generationId === progress.generationId && attempt.studentSubject === progress.studentSubject);
        const openDisputeCount = state.disputes.filter(
          (dispute) => matchingAttempts.some((attempt) => attempt.id === dispute.attemptId) && dispute.status === "OPEN"
        ).length;

        return {
          progress,
          generation,
          latestAttempt: matchingAttempts[0] ?? null,
          openDisputeCount,
        };
      })
      .sort((left, right) => new Date(right.progress.updatedAt).getTime() - new Date(left.progress.updatedAt).getTime());
  }, [progressGroupFilter, state]);

  const uploadSource = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSourceError(null);
    setSourceStatus("Uploading source...");

    const form = event.currentTarget;
    const formData = new FormData(form);

    try {
      await postAuthedFormData<LearningMaterialResponse>(`/v1/learning/materials/${materialId}/sources`, formData);
      form.reset();
      setRefreshToken((value) => value + 1);
      setSourceStatus("Source uploaded.");
    } catch (error) {
      setSourceStatus(null);
      setSourceError(error instanceof Error ? error.message : "Unable to upload learning source.");
    }
  };

  const deleteSource = async (sourceId: string) => {
    setSourceError(null);
    setSourceStatus("Deleting source...");

    try {
      await deleteAuthedJson<LearningMaterialResponse>(`/v1/learning/materials/${materialId}/sources/${sourceId}`);
      setRefreshToken((value) => value + 1);
      setSourceStatus("Source deleted.");
    } catch (error) {
      setSourceStatus(null);
      setSourceError(error instanceof Error ? error.message : "Unable to delete learning source.");
    }
  };

  const replaceAssignments = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setAssignmentError(null);
    setAssignmentStatus("Saving assignments...");

    try {
      await putAuthedJson<LearningMaterialResponse>(`/v1/learning/materials/${materialId}/assignments`, {
        studentSubjects: parseAssignmentList(studentSubjectsText),
        groupNames: parseAssignmentList(groupNamesText),
      } satisfies LearningMaterialAssignmentRequest);
      setRefreshToken((value) => value + 1);
      setAssignmentStatus("Assignments saved.");
    } catch (error) {
      setAssignmentStatus(null);
      setAssignmentError(error instanceof Error ? error.message : "Unable to save assignments.");
    }
  };

  const updateQuestionSetStatus = async (generationId: string, action: "publish" | "archive") => {
    setGenerationError(null);
    setGenerationStatus(action === "publish" ? "Publishing question set..." : "Archiving question set...");

    try {
      await postAuthedJson<LearningContentGenerationResponse>(`/v1/learning/materials/${materialId}/question-sets/${generationId}/${action}`, {});
      setRefreshToken((value) => value + 1);
      setGenerationStatus(action === "publish" ? "Question set published." : "Question set archived.");
    } catch (error) {
      setGenerationStatus(null);
      setGenerationError(error instanceof Error ? error.message : "Unable to update question set status.");
    }
  };

  const updateQuestionSetSettings = async (event: FormEvent<HTMLFormElement>, generationId: string) => {
    event.preventDefault();
    setGenerationError(null);
    setGenerationStatus("Saving question set settings...");

    const formData = new FormData(event.currentTarget);
    const deadlineAt = parseLocalDateTime(String(formData.get("deadlineAt") ?? ""));
    const maxAttemptsText = String(formData.get("maxAttempts") ?? "").trim();
    const maxAttempts = maxAttemptsText ? Number(maxAttemptsText) : null;

    try {
      await putAuthedJson<LearningContentGenerationResponse>(`/v1/learning/materials/${materialId}/question-sets/${generationId}/settings`, {
        deadlineAt,
        maxAttempts,
      } satisfies LearningQuestionSetSettingsRequest);
      setRefreshToken((value) => value + 1);
      setGenerationStatus("Question set settings saved.");
    } catch (error) {
      setGenerationStatus(null);
      setGenerationError(error instanceof Error ? error.message : "Unable to save question set settings.");
    }
  };

  const submitDispute = async (event: FormEvent<HTMLFormElement>, attemptId: string) => {
    event.preventDefault();
    setDisputeError(null);
    setDisputeStatus("Submitting dispute...");

    const formData = new FormData(event.currentTarget);
    const questionIndex = Number(formData.get("questionIndex"));
    const selectedOptionIndex = Number(formData.get("selectedOptionIndex"));
    const reason = String(formData.get("reason") ?? "").trim();

    try {
      await postAuthedJson(`/v1/learning/materials/${materialId}/question-attempts/${attemptId}/disputes`, {
        questionIndex,
        selectedOptionIndex,
        reason,
      });
      event.currentTarget.reset();
      setRefreshToken((value) => value + 1);
      setDisputeStatus("Dispute submitted.");
    } catch (error) {
      setDisputeStatus(null);
      setDisputeError(error instanceof Error ? error.message : "Unable to submit dispute.");
    }
  };

  return (
    <article className="workspace-panel learning-detail-panel" id="learning-detail">
      <div className="section-head learning-detail-head">
        <div>
          <p className="eyebrow">Learning detail</p>
          <h2>{state.status === "ready" ? state.material.title : "Loading material..."}</h2>
        </div>
        <button className="secondary-button" type="button" onClick={onBack}>
          Back to learning
        </button>
      </div>

      {state.status === "loading" && <p className="muted">Loading material detail...</p>}

      {state.status === "error" && <p className="error-title">{state.message}</p>}

      {state.status === "ready" && (
        <div className="learning-detail-layout">
          <section className="learning-detail-hero">
            <div className="section-head">
              <h3>{state.material.title}</h3>
              <span className={`pill ${state.material.canManageAssignments ? "done" : "pending"}`}>
                {state.material.canManageAssignments ? "owner view" : "student view"}
              </span>
            </div>
            <p className="muted">{state.material.description || state.material.originalFilename || "No description"}</p>
            <dl className="profile-list compact learning-detail-meta">
              <div>
                <dt>Ingestion</dt>
                <dd>{state.ingestion.status}</dd>
              </div>
              <div>
                <dt>Generations</dt>
                <dd>{state.generations.length}</dd>
              </div>
              <div>
                <dt>Owner subject</dt>
                <dd>{state.material.ownerSubject}</dd>
              </div>
              <div>
                <dt>File</dt>
                <dd>{state.material.originalFilename || "not uploaded"}</dd>
              </div>
            </dl>
          </section>

          <section className="learning-detail-grid">
            {state.material.canManageAssignments ? (
              <article className="card learning-detail-card">
                <p className="card-kicker">owner</p>
                <h3>Assignments and admin view</h3>
                <form className="prompt-form" onSubmit={replaceAssignments}>
                  <div className="learning-upload-grid">
                    <label>
                      <span>Student subjects</span>
                      <textarea
                        name="studentSubjects"
                        rows={4}
                        placeholder="student-1, student-2"
                        value={studentSubjectsText}
                        onChange={(event) => setStudentSubjectsText(event.target.value)}
                      />
                    </label>
                    <label>
                      <span>Group names</span>
                      <textarea
                        name="groupNames"
                        rows={4}
                        placeholder="group-a, group-b"
                        value={groupNamesText}
                        onChange={(event) => setGroupNamesText(event.target.value)}
                      />
                    </label>
                  </div>
                  <div className="prompt-actions">
                    <button className="primary-button" type="submit">Save assignments</button>
                    <span className="muted">Comma or newline separated values.</span>
                  </div>
                </form>
                {assignmentStatus && <p className="success-title">{assignmentStatus}</p>}
                {assignmentError && <p className="error-title">{assignmentError}</p>}
                <dl className="profile-list compact">
                  <div>
                    <dt>Students</dt>
                    <dd>{state.material.studentSubjects.length ? state.material.studentSubjects.join(", ") : "none"}</dd>
                  </div>
                  <div>
                    <dt>Groups</dt>
                    <dd>{state.material.groupNames.length ? state.material.groupNames.join(", ") : "none"}</dd>
                  </div>
                  <div>
                    <dt>Ingestion retries</dt>
                    <dd>{state.ingestion.retryCount}</dd>
                  </div>
                  <div>
                    <dt>Last error</dt>
                    <dd>{state.ingestion.lastError || "none"}</dd>
                  </div>
                </dl>
              </article>
            ) : (
              <article className="card learning-detail-card">
                <p className="card-kicker">student</p>
                <h3>Your learning view</h3>
                <p className="muted">This page shows the material, your generated content, and your own learning status only.</p>
                <dl className="profile-list compact">
                  <div>
                    <dt>Current ingestion status</dt>
                    <dd>{state.ingestion.status}</dd>
                  </div>
                  <div>
                    <dt>Retry count</dt>
                    <dd>{state.ingestion.retryCount}</dd>
                  </div>
                </dl>
              </article>
            )}

            {state.material.canManageAssignments ? (
              <article className="card learning-detail-card">
                <p className="card-kicker">dashboard</p>
                <div className="section-head">
                  <h3>Teacher progress dashboard</h3>
                  <span className="pill">{teacherProgressRows.length}</span>
                </div>
                <div className="prompt-actions">
                  <label>
                    <span>Group filter</span>
                    <select value={progressGroupFilter} onChange={(event) => setProgressGroupFilter(event.target.value)}>
                      {progressGroupOptions.map((group) => (
                        <option key={group} value={group}>
                          {group === "all" ? "All groups" : group}
                        </option>
                      ))}
                    </select>
                  </label>
                  <span className="muted">Filtered by {progressGroupFilter === "all" ? "all assigned groups" : progressGroupFilter}.</span>
                </div>
                {teacherProgressRows.length === 0 ? (
                  <p className="muted">No matching progress rows yet.</p>
                ) : (
                  <div className="learning-detail-generation-list">
                    {teacherProgressRows.map(({ progress, generation, latestAttempt, openDisputeCount }) => (
                      <section className="learning-detail-generation-card" key={progress.id}>
                        <div className="section-head">
                          <h4>{progress.studentSubject}</h4>
                          <div className="learning-generation-badges">
                            <span className={`pill status-pill ${progressStatusTone(progress.status)}`}>{progress.status}</span>
                            <span className="pill status-pill">{generation ? generation.generationType : "Generation"}</span>
                          </div>
                        </div>
                        <dl className="profile-list compact">
                          <div>
                            <dt>Groups</dt>
                            <dd>{progress.studentGroups.length ? progress.studentGroups.join(", ") : "none"}</dd>
                          </div>
                          <div>
                            <dt>Score</dt>
                            <dd>{progress.score === null ? "n/a" : `${progress.score}/${progress.totalQuestions ?? 0}`}</dd>
                          </div>
                          <div>
                            <dt>Attempts</dt>
                            <dd>{progress.attemptCount}</dd>
                          </div>
                          <div>
                            <dt>Submitted</dt>
                            <dd>{progress.submittedAt ? formatTimestamp(progress.submittedAt) : latestAttempt ? formatTimestamp(latestAttempt.submittedAt) : "not submitted"}</dd>
                          </div>
                          <div>
                            <dt>Open disputes</dt>
                            <dd>{openDisputeCount}</dd>
                          </div>
                          <div>
                            <dt>Reviewed</dt>
                            <dd>{progress.reviewedAt ? formatTimestamp(progress.reviewedAt) : "not reviewed"}</dd>
                          </div>
                        </dl>
                      </section>
                    ))}
                  </div>
                )}
              </article>
            ) : state.material.progressEntries.length > 0 && (
              <article className="card learning-detail-card">
                <p className="card-kicker">progress</p>
                <div className="section-head">
                  <h3>Learning progress</h3>
                  <span className="pill">{state.material.progressEntries.length}</span>
                </div>
                <div className="learning-detail-generation-list">
                  {state.material.progressEntries.map((progress) => (
                    <section className="learning-detail-generation-card" key={progress.id}>
                      <div className="section-head">
                        <h4>{progress.generationId.slice(0, 8)}</h4>
                        <span className={`pill status-pill ${progressStatusTone(progress.status)}`}>{progress.status}</span>
                      </div>
                      <dl className="profile-list compact">
                        <div>
                          <dt>Score</dt>
                          <dd>{progress.score === null ? "n/a" : `${progress.score}/${progress.totalQuestions ?? 0}`}</dd>
                        </div>
                        <div>
                          <dt>Attempts</dt>
                          <dd>{progress.attemptCount}</dd>
                        </div>
                        <div>
                          <dt>Started</dt>
                          <dd>{progress.startedAt ? formatTimestamp(progress.startedAt) : "not started"}</dd>
                        </div>
                        <div>
                          <dt>Submitted</dt>
                          <dd>{progress.submittedAt ? formatTimestamp(progress.submittedAt) : "not submitted"}</dd>
                        </div>
                        <div>
                          <dt>Reviewed</dt>
                          <dd>{progress.reviewedAt ? formatTimestamp(progress.reviewedAt) : "not reviewed"}</dd>
                        </div>
                      </dl>
                    </section>
                  ))}
                </div>
              </article>
            )}

            {!state.material.canManageAssignments && (
              <article className="card learning-detail-card">
                <p className="card-kicker">practice</p>
                <div className="section-head">
                  <h3>Question attempts and disputes</h3>
                  <span className="pill">{state.attempts.length}</span>
                </div>
                <p className="muted">Review prior attempts and file a dispute for a specific question and option index.</p>
                {disputeStatus && <p className="success-title">{disputeStatus}</p>}
                {disputeError && <p className="error-title">{disputeError}</p>}
                {state.attempts.length === 0 ? (
                  <p className="muted">No attempts have been submitted yet.</p>
                ) : (
                  <div className="learning-detail-generation-list">
                    {state.attempts.map((attempt) => {
                      const generation = state.generations.find((item) => item.id === attempt.generationId);
                      const parsedQuestionSet = generation ? parseQuestionSet(generation.structuredContent) : null;
                      const parsedAnswers = parseAttemptAnswers(attempt.answers);
                      const attemptDisputes = state.disputes.filter((dispute) => dispute.attemptId === attempt.id);

                      return (
                        <section className="learning-detail-generation-card" key={attempt.id}>
                          <div className="section-head">
                            <h4>{generation ? generation.generationType : "Question attempt"}</h4>
                            <div className="learning-generation-badges">
                              <span className="pill status-pill done">
                                {attempt.score}/{attempt.totalQuestions}
                              </span>
                              <span className="pill status-pill">{formatTimestamp(attempt.submittedAt)}</span>
                            </div>
                          </div>
                          <p className="muted">Generation: {attempt.generationId}</p>
                          {parsedQuestionSet && parsedAnswers && (
                            <div className="learning-question-set">
                              {parsedAnswers.map((answer) => {
                                const question = parsedQuestionSet.questions[answer.questionIndex];
                                return (
                                  <article className="learning-question-card" key={`${attempt.id}-${answer.questionIndex}`}>
                                    <div className="section-head">
                                      <p className="card-kicker">Question {answer.questionIndex + 1}</p>
                                      <span className="pill status-pill">Selected {answer.selectedOptionIndexes.join(", ") || "none"}</span>
                                    </div>
                                    <h5>{question ? question.prompt : `Question ${answer.questionIndex + 1}`}</h5>
                                    <p className="muted">
                                      {question
                                        ? answer.selectedOptionIndexes.map((optionIndex) => question.options[optionIndex]?.text ?? `#${optionIndex}`).join(", ") || "No option selected"
                                        : JSON.stringify(answer)}
                                    </p>
                                  </article>
                                );
                              })}
                            </div>
                          )}
                          {!parsedQuestionSet && <pre className="learning-detail-content">{attempt.answers}</pre>}

                          <form className="prompt-form learning-upload-form" onSubmit={(event) => void submitDispute(event, attempt.id)}>
                            <div className="learning-upload-grid">
                              <label>
                                <span>Question index</span>
                                <input name="questionIndex" type="number" min={0} max={Math.max(attempt.totalQuestions - 1, 0)} placeholder="0" required />
                              </label>
                              <label>
                                <span>Selected option index</span>
                                <input name="selectedOptionIndex" type="number" min={0} placeholder="0" required />
                              </label>
                            </div>
                            <label>
                              <span>Reason</span>
                              <textarea name="reason" rows={3} placeholder="Explain why this answer should be reviewed" required />
                            </label>
                            <div className="prompt-actions">
                              <button className="primary-button" type="submit">Submit dispute</button>
                              <span className="muted">Indexes are zero-based, matching the backend.</span>
                            </div>
                          </form>

                          {attemptDisputes.length > 0 && (
                            <div className="learning-generation-list">
                              {attemptDisputes.map((dispute) => (
                                <article className="learning-detail-generation-card" key={dispute.id}>
                                  <div className="section-head">
                                    <h5>Dispute #{dispute.id.slice(0, 8)}</h5>
                                    <span className={`pill status-pill ${disputeStatusTone(dispute.status)}`}>{dispute.status}</span>
                                  </div>
                                  <p className="muted">Question {dispute.questionIndex + 1}, option {dispute.selectedOptionIndex}</p>
                                  <p>{dispute.reason}</p>
                                </article>
                              ))}
                            </div>
                          )}
                        </section>
                      );
                    })}
                  </div>
                )}
              </article>
            )}

            <article className="card learning-detail-card">
              <p className="card-kicker">sources</p>
              <div className="section-head">
                <h3>Learning sources</h3>
                <span className="pill">{state.material.sources.length}</span>
              </div>
              {state.material.canManageAssignments && (
                <form className="prompt-form learning-upload-form" onSubmit={uploadSource}>
                  <div className="learning-upload-grid">
                    <label>
                      <span>Source name</span>
                      <input name="sourceName" placeholder="Lecture notes" />
                    </label>
                    <label>
                      <span>File</span>
                      <input name="file" type="file" accept=".pdf,.txt,.md,.markdown" required />
                    </label>
                  </div>
                  <div className="prompt-actions">
                    <button className="primary-button" type="submit">Add source</button>
                    <span className="muted">Sources are included in ingestion and generation.</span>
                  </div>
                </form>
              )}
              {sourceStatus && <p className="success-title">{sourceStatus}</p>}
              {sourceError && <p className="error-title">{sourceError}</p>}
              {state.material.sources.length === 0 ? (
                <p className="muted">No active sources yet.</p>
              ) : (
                <div className="learning-source-list">
                  {state.material.sources.map((source: LearningMaterialSourceResponse) => (
                    <article className="learning-source-card" key={source.id}>
                      <div className="section-head">
                        <div>
                          <h4>{source.sourceName}</h4>
                          <p className="muted">{source.originalFilename || source.contentType || "Uploaded source"}</p>
                        </div>
                        <span className={`pill ${source.sourceType === "PRIMARY_UPLOAD" ? "done" : "pending"}`}>{source.sourceType}</span>
                      </div>
                      <dl className="profile-list compact">
                        <div>
                          <dt>File size</dt>
                          <dd>{source.fileSize ? `${Math.ceil(source.fileSize / 1024)} KB` : "unknown"}</dd>
                        </div>
                        <div>
                          <dt>Created</dt>
                          <dd>{formatTimestamp(source.createdAt)}</dd>
                        </div>
                      </dl>
                      {state.material.canManageAssignments && (
                        <button className="secondary-button" type="button" onClick={() => void deleteSource(source.id)}>
                          Delete source
                        </button>
                      )}
                    </article>
                  ))}
                </div>
              )}
            </article>

            <article className="card learning-detail-card">
              <p className="card-kicker">content</p>
              <h3>Latest generated content</h3>
              {generationStatus && <p className="success-title">{generationStatus}</p>}
              {generationError && <p className="error-title">{generationError}</p>}
              {state.generations.length === 0 ? (
                <p className="muted">No generated questions or summaries yet.</p>
              ) : (
                <div className="learning-detail-generation-list">
                  {state.generations.map((generation) => {
                    const structured = parseQuestionSet(generation.structuredContent);

                    return (
                      <section className="learning-detail-generation-card" key={`${generation.id}-${generation.deadlineAt ?? ""}-${generation.maxAttempts ?? ""}`}>
                        <div className="section-head">
                          <h4>{generation.generationType === "QUESTION_SET" ? "Questions" : "Summary"}</h4>
                          <div className="learning-generation-badges">
                            {generation.generationType === "QUESTION_SET" && generation.questionSetStatus && (
                              <span className={`pill status-pill ${questionSetStatusTone(generation.questionSetStatus)}`}>
                                {generation.questionSetStatus}
                              </span>
                            )}
                            <span className={`pill status-pill ${generation.generationStatus === "COMPLETED" ? "done" : "failed"}`}>
                              {generation.generationStatus}
                            </span>
                          </div>
                        </div>
                        {generation.generationType === "QUESTION_SET" && !state.material.canManageAssignments && (
                          <dl className="profile-list compact">
                            <div>
                              <dt>Deadline</dt>
                              <dd>{generation.deadlineAt ? formatTimestamp(generation.deadlineAt) : "none"}</dd>
                            </div>
                            <div>
                              <dt>Max attempts</dt>
                              <dd>{generation.maxAttempts ?? "unlimited"}</dd>
                            </div>
                            <div>
                              <dt>Used attempts</dt>
                              <dd>{questionSetAttemptCount(state.attempts, generation.id)}</dd>
                            </div>
                            <div>
                              <dt>Remaining</dt>
                              <dd>
                                {generation.maxAttempts === null
                                  ? "unlimited"
                                  : Math.max(generation.maxAttempts - questionSetAttemptCount(state.attempts, generation.id), 0)}
                              </dd>
                            </div>
                            <div>
                              <dt>Blocked reason</dt>
                              <dd>{questionSetBlockedReason(generation.deadlineAt, generation.maxAttempts, questionSetAttemptCount(state.attempts, generation.id)) || "available"}</dd>
                            </div>
                          </dl>
                        )}
                        <p className="muted">
                          {generation.fallbackUsed ? generation.fallbackReason || "Fallback used" : "Generated from the learning source material."}
                        </p>
                        {generation.sourceVersions.length > 0 && (
                          <dl className="profile-list compact">
                            <div>
                              <dt>Source versions</dt>
                              <dd>{generation.sourceVersions.map((source: LearningSourceVersionReference) => source.sourceName).join(", ")}</dd>
                            </div>
                            <div>
                              <dt>Source hashes</dt>
                              <dd>{generation.sourceVersions.map((source: LearningSourceVersionReference) => source.contentHash.slice(0, 12)).join(", ")}</dd>
                            </div>
                          </dl>
                        )}
                        {generation.generationType === "QUESTION_SET" && state.material.canManageAssignments && (
                          <form className="prompt-form learning-upload-form" onSubmit={(event) => void updateQuestionSetSettings(event, generation.id)}>
                            <div className="learning-upload-grid">
                              <label>
                                <span>Deadline</span>
                                <input name="deadlineAt" type="datetime-local" defaultValue={formatLocalDateTime(generation.deadlineAt)} />
                              </label>
                              <label>
                                <span>Max attempts</span>
                                <input
                                  name="maxAttempts"
                                  type="number"
                                  min={1}
                                  placeholder="unlimited"
                                  defaultValue={generation.maxAttempts ?? ""}
                                />
                              </label>
                            </div>
                            <div className="prompt-actions">
                              <button className="primary-button" type="submit">Save question set settings</button>
                              <span className="muted">Leave fields blank for unlimited attempts.</span>
                            </div>
                          </form>
                        )}
                        <pre className="learning-detail-content">{generation.content}</pre>
                        {state.material.canManageAssignments && generation.generationType === "QUESTION_SET" && (
                          <div className="learning-card-actions">
                            {generation.questionSetStatus !== "PUBLISHED" && (
                              <button className="secondary-button" type="button" onClick={() => void updateQuestionSetStatus(generation.id, "publish")}>
                                Publish
                              </button>
                            )}
                            {generation.questionSetStatus !== "ARCHIVED" && (
                              <button className="secondary-button" type="button" onClick={() => void updateQuestionSetStatus(generation.id, "archive")}>
                                Archive
                              </button>
                            )}
                          </div>
                        )}
                        {structured && generation.generationType === "QUESTION_SET" && (
                          <div className="learning-question-set">
                            {structured.questions.map((question: LearningQuestionPayload, index: number) => (
                              <article className="learning-question-card" key={`${generation.id}-${index}`}>
                                <div className="section-head">
                                  <p className="card-kicker">Question {index + 1}</p>
                                  <span className="pill status-pill">
                                    {resolveQuestionAnswerType(question) === "MULTI_CORRECT"
                                      ? `Válassz ${resolveQuestionCorrectOptionIndexes(question).length} választ`
                                      : "Válassz 1 választ"}
                                  </span>
                                </div>
                                <h5>{question.prompt}</h5>
                                <ul className="learning-option-list">
                                  {question.options.map((option, optionIndex) => (
                                    <li key={option.key} className={resolveQuestionCorrectOptionIndexes(question).includes(optionIndex) ? "correct" : ""}>
                                      <strong>{option.key}.</strong> {option.text}
                                    </li>
                                  ))}
                                </ul>
                                <p className="muted">{question.explanation}</p>
                              </article>
                            ))}
                          </div>
                        )}
                        {!!generation.errorMessage && <p className="error-title">{generation.errorMessage}</p>}
                      </section>
                    );
                  })}
                </div>
              )}
            </article>

            <article className="card learning-detail-card learning-detail-image-card">
              <p className="card-kicker">images</p>
              <h3>Image assets</h3>
              {state.material.imageAssets.length === 0 ? (
                <p className="muted">No image assets are attached to this material yet.</p>
              ) : (
                <div className="learning-image-grid">
                  {state.material.imageAssets.map((asset: LearningImageAssetResponse) => (
                    <figure className="learning-image-card" key={asset.id}>
                      <img src={asset.assetUrl} alt={asset.altText || "Learning asset"} />
                      <figcaption>
                        <strong>{asset.mimeType}</strong>
                        <span>{Math.ceil(asset.sizeBytes / 1024)} KB</span>
                      </figcaption>
                    </figure>
                  ))}
                </div>
              )}
            </article>
          </section>
        </div>
      )}
    </article>
  );
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

type PromptFlowState =
  | { status: "idle" }
  | { status: "busy"; action: string }
  | { status: "error"; message: string }
  | { status: "ready"; draft: PromptDraftResponse; implementationJobId: string | null };

const intentOptions: PromptIntent[] = ["TASK", "BUG", "FEATURE", "EPIC"];

function promptFlowStage(draft?: PromptDraftResponse | null, implementationJobId?: string | null): string {
  if (!draft) {
    return "draft conversation";
  }

  if (implementationJobId) {
    return "implementation running";
  }

  if (draft.status === "TICKET_CREATED") {
    return "ticket created";
  }

  if (draft.status === "APPROVED" || draft.status === "READY_FOR_APPROVAL") {
    return "ready for ticket";
  }

  return "draft conversation";
}

function PromptDraftPanel({ onJobCreated }: { onJobCreated: (jobId: string) => void }) {
  const [prompt, setPrompt] = useState("");
  const [followUp, setFollowUp] = useState("");
  const [selectedIntent, setSelectedIntent] = useState<PromptIntent | "">("");
  const [implementationJobId, setImplementationJobId] = useState<string | null>(null);
  const [submission, setSubmission] = useState<JobSubmissionState>({ status: "idle" });
  const [state, setState] = useState<PromptFlowState>({ status: "idle" });

  const draft = state.status === "ready" ? state.draft : null;
  const latestAssistantMessage = draft?.messages.slice().reverse().find((message) => message.role === "ASSISTANT") ?? null;

  useEffect(() => {
    if (!draft) {
      return;
    }

    if (draft.selectedIntent) {
      setSelectedIntent(draft.selectedIntent);
      return;
    }

    if (draft.intent && draft.intent !== "QUESTION") {
      setSelectedIntent(draft.intent);
      return;
    }

    setSelectedIntent("TASK");
  }, [draft?.draftId, draft?.selectedIntent, draft?.intent]);

  const setReady = (nextDraft: PromptDraftResponse) => {
    setState({
      status: "ready",
      draft: nextDraft,
      implementationJobId: implementationJobId,
    });
  };

  const createDraft = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setState({ status: "busy", action: "Creating draft..." });

    try {
      const response = await postAuthedJson<PromptDraftResponse>("/v1/prompt-drafts", { prompt });
      setPrompt("");
      setImplementationJobId(null);
      setState({ status: "ready", draft: response, implementationJobId: null });
    } catch (error) {
      setState({
        status: "error",
        message: error instanceof Error ? error.message : "Unable to create prompt draft.",
      });
    }
  };

  const addMessage = async () => {
    if (!draft || !followUp.trim()) {
      return;
    }

    setState({ status: "busy", action: "Updating draft..." });

    try {
      const response = await postAuthedJson<PromptDraftResponse>(`/v1/prompt-drafts/${draft.draftId}/messages`, {
        content: followUp,
      });
      setFollowUp("");
      setReady(response);
    } catch (error) {
      setState({
        status: "error",
        message: error instanceof Error ? error.message : "Unable to update the prompt draft.",
      });
    }
  };

  const approveDraft = async () => {
    if (!draft || !selectedIntent) {
      return;
    }

    setState({ status: "busy", action: "Approving draft..." });

    try {
      const response = await postAuthedJson<PromptDraftResponse>(`/v1/prompt-drafts/${draft.draftId}/approve`, {
        selectedIntent,
      });
      setReady(response);
    } catch (error) {
      setState({
        status: "error",
        message: error instanceof Error ? error.message : "Unable to approve the prompt draft.",
      });
    }
  };

  const createTicket = async () => {
    if (!draft) {
      return;
    }

    setState({ status: "busy", action: "Creating Jira ticket..." });

    try {
      const response = await postAuthedJson<PromptDraftResponse>(`/v1/prompt-drafts/${draft.draftId}/jira-ticket`, {});
      setReady(response);
    } catch (error) {
      setState({
        status: "error",
        message: error instanceof Error ? error.message : "Unable to create the Jira ticket.",
      });
    }
  };

  const startImplementation = async () => {
    if (!draft) {
      return;
    }

    setState({ status: "busy", action: "Starting implementation..." });

    try {
      const response = await postAuthedJson<JobResponse>(`/v1/prompt-drafts/${draft.draftId}/job`, {});
      setImplementationJobId(response.jobId);
      onJobCreated(response.jobId);
      setState({ status: "ready", draft, implementationJobId: response.jobId });
    } catch (error) {
      setState({
        status: "error",
        message: error instanceof Error ? error.message : "Unable to start implementation.",
      });
    }
  };

  const copyLatestAssistantMessage = () => {
    if (!latestAssistantMessage) {
      return;
    }

    setFollowUp(latestAssistantMessage.content);
  };

  const createJobFromReply = async () => {
    if (!followUp.trim()) {
      return;
    }

    setSubmission({ status: "submitting" });

    try {
      const response = await postAuthedJson<CreateJobResponse>("/v1/jobs", { prompt: followUp });
      setSubmission({ status: "success", jobId: response.jobId });
      onJobCreated(response.jobId);
    } catch (error) {
      setSubmission({
        status: "error",
        message: error instanceof Error ? error.message : "Unable to create job.",
      });
    }
  };

  const stage = promptFlowStage(draft, implementationJobId);

  return (
    <article className="workspace-panel" id="prompt-flow">
      <div className="section-head">
        <h2>Prompt flow</h2>
        <span className="pill">{stage}</span>
      </div>
      <p className="muted">
        Draft, clarify, approve, create the Jira ticket, and then launch implementation from the approved ticket.
      </p>

      {state.status === "error" && <p className="error-title">{state.message}</p>}
      {state.status === "busy" && <p className="muted">{state.action}</p>}

      {!draft ? (
        <form className="prompt-form" onSubmit={createDraft}>
          <label>
            <span>Prompt draft</span>
            <textarea
              name="prompt-draft"
              value={prompt}
              onChange={(event) => setPrompt(event.target.value)}
              placeholder="Describe the change in enough detail for the system to ask follow-up questions"
              rows={6}
              required
            />
          </label>

          <div className="prompt-actions">
            <button className="primary-button" type="submit" disabled={state.status === "busy"}>
              {state.status === "busy" ? "Creating..." : "Start draft"}
            </button>
            <span className="muted">The backend will classify intent and ask for more detail when needed.</span>
          </div>
        </form>
      ) : (
        <div className="prompt-flow-body">
          <dl className="profile-list compact">
            <div>
              <dt>Conversation</dt>
              <dd>{draft.status === "CLARIFYING" ? "draft conversation" : "complete"}</dd>
            </div>
            <div>
              <dt>Ready for ticket</dt>
              <dd>{draft.status === "READY_FOR_APPROVAL" || draft.status === "APPROVED" || draft.status === "TICKET_CREATED" ? "yes" : "no"}</dd>
            </div>
            <div>
              <dt>Ticket</dt>
              <dd>{draft.jiraIssueKey ? `${draft.jiraIssueKey} (${draft.jiraIssueUrl ?? ""})` : "not created yet"}</dd>
            </div>
            <div>
              <dt>Implementation</dt>
              <dd>{implementationJobId ? `running as ${implementationJobId}` : "waiting"}</dd>
            </div>
          </dl>

          <section className="prompt-flow-card">
            <h3>Conversation</h3>
            <div className="prompt-message-list">
              {draft.messages.map((message, index) => (
                <div key={`${message.role}-${message.createdAt}-${index}`} className={`prompt-message ${message.role.toLowerCase()}`}>
                  <span>{message.role}</span>
                  <p>{message.content}</p>
                </div>
              ))}
            </div>

            {draft.status === "CLARIFYING" && (
              <div className="prompt-form prompt-reply-form">
                <label>
                  <span>Reply</span>
                  <textarea
                    value={followUp}
                    onChange={(event) => setFollowUp(event.target.value)}
                    placeholder="Add more context, acceptance criteria, or repository details"
                    rows={4}
                  />
                </label>

                <div className="prompt-actions">
                  <button className="secondary-button" type="button" onClick={() => void addMessage()} disabled={state.status === "busy" || !followUp.trim()}>
                    Add reply
                  </button>
                  <button className="secondary-button" type="button" onClick={copyLatestAssistantMessage} disabled={!latestAssistantMessage || state.status === "busy"}>
                    Copy last assistant
                  </button>
                  <button className="primary-button" type="button" onClick={() => void createJobFromReply()} disabled={submission.status === "submitting" || !followUp.trim()}>
                    {submission.status === "submitting" ? "Creating..." : "Create job"}
                  </button>
                </div>

                {submission.status === "success" && <p className="success-title">Job created as {submission.jobId}.</p>}
                {submission.status === "error" && <p className="error-title">{submission.message}</p>}
              </div>
            )}
          </section>

          {draft.pendingQuestions.length > 0 && (
            <section className="prompt-flow-card">
              <h3>Questions</h3>
              <ul className="prompt-question-list">
                {draft.pendingQuestions.map((question) => (
                  <li key={question}>{question}</li>
                ))}
              </ul>
            </section>
          )}

          {(draft.status === "READY_FOR_APPROVAL" || draft.status === "APPROVED" || draft.status === "TICKET_CREATED") && (
            <section className="prompt-flow-card">
              <h3>Decision</h3>
              <div className="intent-chooser">
                {intentOptions.map((intent) => (
                  <button
                    key={intent}
                    type="button"
                    className={selectedIntent === intent ? "primary-button" : "secondary-button"}
                    onClick={() => setSelectedIntent(intent)}
                  >
                    {intent}
                  </button>
                ))}
              </div>

              <div className="prompt-actions">
                <button className="secondary-button" type="button" onClick={() => void approveDraft()} disabled={state.status === "busy" || !selectedIntent || draft.status !== "READY_FOR_APPROVAL"}>
                  Approve draft
                </button>
                <button className="secondary-button" type="button" onClick={() => void createTicket()} disabled={state.status === "busy" || draft.status !== "APPROVED" && draft.status !== "TICKET_CREATED"}>
                  Create Jira ticket
                </button>
                <button className="primary-button" type="button" onClick={() => void startImplementation()} disabled={state.status === "busy" || draft.status !== "TICKET_CREATED"}>
                  Start implementation
                </button>
              </div>
            </section>
          )}
        </div>
      )}
    </article>
  );
}

function DashboardShell({
  mode,
  actionLabel,
  onAction,
  navItems = dashboardNav,
  children,
}: {
  mode: string;
  actionLabel: string;
  onAction: () => void;
  navItems?: typeof dashboardNav;
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
          {navItems.map((item) => (
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
  pathname,
  onNavigate,
}: {
  profile: BackendMeResponse;
  onSignOut: () => void;
  jobId: string;
  onJobCreated: (jobId: string) => void;
  pathname: string;
  onNavigate: (pathname: string) => void;
}) {
  const config = useMemo(() => getRuntimeConfig(), []);
  const isDeveloper = profile.groups.includes("developer");
  const navItems = isDeveloper
    ? [dashboardNav[0], { label: "AI", href: "#ai" }, ...dashboardNav.slice(1)]
    : dashboardNav;

  const persona = profile.roles.includes("admin")
    ? "operator"
    : profile.roles.length > 0
      ? "builder"
      : "member";
  const learningMaterialId = parseLearningMaterialPath(pathname);

  return (
    <DashboardShell mode="authenticated" actionLabel="Sign out" onAction={onSignOut} navItems={navItems}>
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
        {learningMaterialId ? (
          <LearningMaterialDetailPanel materialId={learningMaterialId} onBack={() => onNavigate("/")} />
        ) : (
          <LearningWorkspacePanel roles={profile.roles} onOpenMaterial={(materialId) => onNavigate(`/learning/materials/${materialId}`)} />
        )}

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
              <dt>Groups</dt>
              <dd>{profile.groups.length ? profile.groups.join(", ") : "none"}</dd>
            </div>
            <div>
              <dt>Issuer</dt>
              <dd>{profile.claims.issuer}</dd>
            </div>
          </dl>
        </article>

        {isDeveloper ? (
          <article className="workspace-panel" id="ai">
            <div className="section-head">
              <h2>AI menu</h2>
              <span className="pill">developer</span>
            </div>
            <p className="muted">
              This menu groups multiple AI tools behind a single developer-only entrypoint.
            </p>
            <div className="status-grid">
              <article className="card">
                <p className="card-kicker">current</p>
                <h3>Prompt flow</h3>
                <p>Draft, clarify, approve, ticket, and launch implementation.</p>
              </article>
              <article className="card">
                <p className="card-kicker">current</p>
                <h3>Job status</h3>
                <p>Follow AI-generated work from queue to PR publication.</p>
              </article>
              <article className="card">
                <p className="card-kicker">future</p>
                <h3>More tools</h3>
                <p>The AI menu can grow with more backend-assisted capabilities.</p>
              </article>
            </div>
          </article>
        ) : (
          <article className="workspace-panel" id="ai">
            <div className="section-head">
              <h2>AI menu</h2>
              <span className="pill">restricted</span>
            </div>
            <p className="error-title">AI tools are available only to Keycloak developer-group members.</p>
            <p className="muted">Ask an admin to add your account to the `developer` group to unlock this area.</p>
          </article>
        )}

        {isDeveloper && (
          <>
            <PromptDraftPanel
              onJobCreated={(createdJobId) => {
                onJobCreated(createdJobId);
                syncJobIdInUrl(createdJobId);
              }}
            />

            {jobId && <JobStatusPanel jobId={jobId} />}
          </>
        )}

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
  const [pathname, setPathname] = useState(() => readPathname());
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

  useEffect(() => {
    const syncPath = () => setPathname(readPathname());
    window.addEventListener("popstate", syncPath);
    return () => window.removeEventListener("popstate", syncPath);
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
          pathname={pathname}
          onNavigate={(nextPathname) => {
            syncPathnameInUrl(nextPathname);
            setPathname(nextPathname);
          }}
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
