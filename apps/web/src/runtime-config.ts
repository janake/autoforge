export type AutoforgeRuntimeConfig = {
  apiBaseUrl: string;
  keycloak: {
    url: string;
    realm: string;
    clientId: string;
  };
};

const defaults: AutoforgeRuntimeConfig = {
  apiBaseUrl: "/api",
  keycloak: {
    url: "https://kc.prodet.org",
    realm: "autoforge",
    clientId: "autoforge-web",
  },
};

export function getRuntimeConfig(): AutoforgeRuntimeConfig {
  const runtime = window.__AUTOFORGE_CONFIG__;

  return {
    apiBaseUrl: runtime?.apiBaseUrl || defaults.apiBaseUrl,
    keycloak: {
      url: runtime?.keycloak?.url || defaults.keycloak.url,
      realm: runtime?.keycloak?.realm || defaults.keycloak.realm,
      clientId: runtime?.keycloak?.clientId || defaults.keycloak.clientId,
    },
  };
}
