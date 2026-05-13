export type BackendMeResponse = {
  subject: string;
  username: string;
  email: string | null;
  roles: string[];
  claims: {
    issuer: string;
    audience: string;
    authorizedParty: string;
  };
};

export type CreateJobRequest = {
  jiraIssueKey: string;
  prompt: string;
  targetRepository: string;
  baseBranch: string;
};

export type CreateJobResponse = {
  jobId: string;
  jiraIssueKey: string;
  status: string;
};

export type JobResponse = {
  jobId: string;
  jiraIssueKey: string;
  prompt: string;
  targetRepository: string;
  baseBranch: string;
  status: string;
  prUrl: string | null;
  errorMessage: string | null;
  createdAt: string;
  updatedAt: string;
};
