export type BackendMeResponse = {
  subject: string;
  username: string;
  email: string | null;
  roles: string[];
  groups: string[];
  claims: {
    issuer: string;
    audience: string;
    authorizedParty: string;
  };
};

export type CreateJobRequest = {
  prompt: string;
};

export type CreateJobResponse = {
  jobId: string;
  status: string;
};

export type PromptIntent = "QUESTION" | "TASK" | "BUG" | "FEATURE" | "EPIC";

export type PromptDraftMessageResponse = {
  role: "USER" | "ASSISTANT";
  content: string;
  createdAt: string;
};

export type PromptDraftResponse = {
  draftId: string;
  prompt: string;
  status: "DRAFT" | "CLARIFYING" | "READY_FOR_APPROVAL" | "APPROVED" | "TICKET_CREATED";
  intent: PromptIntent | null;
  intentConfidence: number | null;
  intentReason: string | null;
  selectedIntent: PromptIntent | null;
  readyForApproval: boolean;
  approvedBy: string | null;
  approvedAt: string | null;
  jiraIssueKey: string | null;
  jiraIssueUrl: string | null;
  pendingQuestions: string[];
  messages: PromptDraftMessageResponse[];
  createdAt: string;
  updatedAt: string;
};

export type JobResponse = {
  jobId: string;
  prompt: string;
  targetRepository: string;
  baseBranch: string;
  status: string;
  prUrl: string;
  errorMessage: string | null;
  createdAt: string;
  updatedAt: string;
};
