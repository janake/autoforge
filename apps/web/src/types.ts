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
  prompt: string;
};

export type CreateJobResponse = {
  jobId: string;
  status: string;
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
