/// <reference types="vite/client" />

interface Window {
  __AUTOFORGE_CONFIG__?: {
    apiBaseUrl?: string;
    keycloak?: {
      url?: string;
      realm?: string;
      clientId?: string;
    };
  };
}
