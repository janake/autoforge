import Keycloak from "keycloak-js";
import { getRuntimeConfig } from "../runtime-config";

let keycloak: Keycloak | null = null;

function createKeycloak(): Keycloak {
  if (!keycloak) {
    const runtimeConfig = getRuntimeConfig();
    keycloak = new Keycloak({
      url: runtimeConfig.keycloak.url,
      realm: runtimeConfig.keycloak.realm,
      clientId: runtimeConfig.keycloak.clientId,
    });
  }

  return keycloak;
}

export async function initializeKeycloak(onLoad?: "check-sso" | "login-required"): Promise<Keycloak> {
  const client = createKeycloak();

  if (!client.didInitialize) {
    await client.init({
      ...(onLoad ? { onLoad } : {}),
      pkceMethod: "S256",
      checkLoginIframe: false,
      enableLogging: false,
    });

    client.onTokenExpired = () => {
      client.updateToken(30).catch(async () => {
        const loginUrl = await client.createLoginUrl({ redirectUri: window.location.origin });
        window.location.assign(loginUrl);
      });
    };
  }

  return client;
}

export function getKeycloak(): Keycloak {
  return createKeycloak();
}

export async function signIn(): Promise<void> {
  const client = await initializeKeycloak();
  await client.login({ redirectUri: window.location.origin });
}

export async function signOut(): Promise<void> {
  const client = await initializeKeycloak();
  await client.logout({ redirectUri: window.location.origin });
}

export async function loadAuthedJson<T>(path: string): Promise<T> {
  const response = await authedFetch(path);
  return (await response.json()) as T;
}

export async function postAuthedJson<T>(path: string, body: unknown): Promise<T> {
  const response = await authedFetch(path, {
    method: "POST",
    body: JSON.stringify(body),
    headers: {
      "Content-Type": "application/json",
    },
  });

  return (await response.json()) as T;
}

async function authedFetch(path: string, init: RequestInit = {}): Promise<Response> {
  const client = createKeycloak();
  const runtimeConfig = getRuntimeConfig();
  const url = `${runtimeConfig.apiBaseUrl.replace(/\/$/, "")}/${path.replace(/^\//, "")}`;

  if (client.isTokenExpired(30)) {
    await client.updateToken(30);
  }

  const response = await fetch(url, {
    ...init,
    headers: {
      Authorization: `Bearer ${client.token ?? ""}`,
      "Content-Type": "application/json",
      ...(init.headers ?? {}),
    },
  });

  if (!response.ok) {
    throw new Error(`Request failed with ${response.status}`);
  }

  return response;
}
