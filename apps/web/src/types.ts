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
  storageObjectKey: string | null;
  storageObjectUri: string | null;
  contentHash: string | null;
  contentETag: string | null;
  ownerSubject: string;
  studentSubjects: string[];
  groupNames: string[];
  canManageAssignments: boolean;
  sources: LearningMaterialSourceResponse[];
  imageAssets: LearningImageAssetResponse[];
  createdAt: string;
  updatedAt: string;
};

export type LearningMaterialSourceResponse = {
  id: string;
  materialId: string;
  sourceType: string;
  sourceName: string;
  originalFilename: string | null;
  contentType: string | null;
  fileSize: number | null;
  storageObjectKey: string;
  storageObjectUri: string;
  contentHash: string;
  contentETag: string;
  deletedAt: string | null;
  createdAt: string;
  updatedAt: string;
};

export type LearningImageAssetResponse = {
  id: string;
  materialId: string;
  mimeType: string;
  sizeBytes: number;
  contentHash: string;
  altText: string | null;
  assetUrl: string;
  createdAt: string;
};

export type LearningContentGenerationType = "QUESTION_SET" | "SUMMARY";

export type LearningContentSourceReference = {
  chunkIndex: number;
  excerpt: string;
};

export type LearningQuestionOptionPayload = {
  key: string;
  text: string;
};

export type LearningQuestionPayload = {
  prompt: string;
  options: LearningQuestionOptionPayload[];
  correctOptionIndex: number;
  explanation: string;
  sources: LearningContentSourceReference[];
  imageAssetReference: string | null;
};

export type LearningQuestionSetPayload = {
  materialId: string;
  materialTitle: string;
  retrievalContext: string;
  questions: LearningQuestionPayload[];
};

export type LearningContentGenerationResponse = {
  id: string;
  materialId: string;
  generationType: LearningContentGenerationType;
  content: string;
  sources: LearningContentSourceReference[];
  fallbackUsed: boolean;
  fallbackReason: string | null;
  generationStatus: "COMPLETED" | "FAILED";
  structuredContent: string | null;
  errorMessage: string | null;
  createdAt: string;
};

export type LearningIngestionResponse = {
  jobId: string;
  materialId: string;
  status: "QUEUED" | "PROCESSING" | "COMPLETED" | "FAILED";
  retryCount: number;
  chunkCount: number;
  embeddingCount: number;
  lastError: string | null;
  createdAt: string;
  updatedAt: string;
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
