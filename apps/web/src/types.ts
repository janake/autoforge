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
  progressEntries: LearningQuestionProgressResponse[];
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

export type LearningQuestionAttemptAnswerPayload = {
  questionIndex: number;
  selectedOptionIndexes: number[];
};

export type LearningQuestionAttemptResponse = {
  id: string;
  materialId: string;
  generationId: string;
  studentSubject: string;
  score: number;
  totalQuestions: number;
  answers: string;
  submittedAt: string;
};

export type LearningQuestionDisputeStatus = "OPEN" | "ACCEPTED" | "REJECTED";

export type LearningQuestionDisputeResponse = {
  id: string;
  materialId: string;
  attemptId: string;
  studentSubject: string;
  questionIndex: number;
  selectedOptionIndex: number;
  reason: string;
  status: LearningQuestionDisputeStatus;
  reviewerSubject: string | null;
  reviewReason: string | null;
  overrideScore: number | null;
  createdAt: string;
  reviewedAt: string | null;
};

export type LearningQuestionProgressStatus = "ASSIGNED" | "STARTED" | "SUBMITTED" | "REVIEWED" | "COMPLETED";

export type LearningQuestionProgressResponse = {
  id: string;
  materialId: string;
  generationId: string;
  studentSubject: string;
  status: LearningQuestionProgressStatus;
  attemptId: string | null;
  attemptCount: number;
  score: number | null;
  totalQuestions: number | null;
  createdAt: string;
  updatedAt: string;
  startedAt: string | null;
  submittedAt: string | null;
  reviewedAt: string | null;
  completedAt: string | null;
};

export type LearningMaterialAssignmentRequest = {
  studentSubjects: string[];
  groupNames: string[];
};

export type LearningContentGenerationType = "QUESTION_SET" | "SUMMARY";

export type LearningQuestionSetStatus = "DRAFT" | "PUBLISHED" | "ARCHIVED";

export type LearningContentSourceReference = {
  chunkIndex: number;
  excerpt: string;
};

export type LearningSourceVersionReference = {
  id: string;
  sourceName: string;
  originalFilename: string | null;
  contentHash: string;
  contentETag: string;
  createdAt: string;
};

export type LearningQuestionOptionPayload = {
  key: string;
  text: string;
};

export type LearningQuestionAnswerType = "SINGLE_CORRECT" | "MULTI_CORRECT";

export type LearningQuestionPayload = {
  prompt: string;
  options: LearningQuestionOptionPayload[];
  correctOptionIndex?: number | null;
  answerType?: LearningQuestionAnswerType | null;
  correctOptionIndexes?: number[] | null;
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
  sourceVersions: LearningSourceVersionReference[];
  questionSetStatus: LearningQuestionSetStatus | null;
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
