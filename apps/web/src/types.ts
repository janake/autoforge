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

export type LearningMaterialResponse = {
  id: string;
  title: string;
  description: string | null;
  originalFilename: string | null;
  contentType: string | null;
  fileSize: number | null;
  ownerSubject: string;
  studentSubjects: string[];
  groupNames: string[];
  canManageAssignments: boolean;
  createdAt: string;
  updatedAt: string;
};

export type LearningContentGenerationType = "QUESTION_SET" | "SUMMARY";

export type LearningContentSourceReference = {
  chunkIndex: number;
  excerpt: string;
};

export type LearningContentGenerationResponse = {
  id: string;
  materialId: string;
  generationType: LearningContentGenerationType;
  content: string;
  sources: LearningContentSourceReference[];
  fallbackUsed: boolean;
  fallbackReason: string | null;
  createdAt: string;
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
