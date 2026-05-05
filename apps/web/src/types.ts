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
