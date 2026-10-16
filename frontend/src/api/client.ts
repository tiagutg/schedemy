import type { ErrorResponse, Params } from "@/types";

const BASE_URL: string = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";
const AUTH_STORAGE_KEY = "schedemy.auth";

export interface StoredAuth {
  username: string;
  password: string;
  usuarioId: number | null;
}

/** Classe de erro rica, carregando o corpo padronizado de erro da API (ErrorResponseDTO). */
export class ApiError extends Error {
  status: number;
  body: ErrorResponse | null;
  camposInvalidos: ErrorResponse["camposInvalidos"];

  constructor(status: number, body: ErrorResponse | null) {
    super(body?.mensagem || `Erro ${status} ao comunicar com a API.`);
    this.status = status;
    this.body = body;
    this.camposInvalidos = body?.camposInvalidos || null;
  }
}

export function getStoredAuth(): StoredAuth | null {
  try {
    const raw = localStorage.getItem(AUTH_STORAGE_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export function setStoredAuth(auth: StoredAuth | null) {
  if (auth) {
    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(auth));
  } else {
    localStorage.removeItem(AUTH_STORAGE_KEY);
  }
}

let aoSessaoInvalida: (() => void) | null = null;

/** Registra quem trata um 401 numa requisição autenticada (credenciais salvas que deixaram de valer). */
export function definirAoSessaoInvalida(tratador: (() => void) | null) {
  aoSessaoInvalida = tratador;
}

function buildAuthHeader() {
  const auth = getStoredAuth();
  if (!auth?.username) return null;
  const token = btoa(`${auth.username}:${auth.password}`);
  return `Basic ${token}`;
}

interface OpcoesRequisicao {
  method?: string;
  body?: unknown;
  params?: Params;
  signal?: AbortSignal;
}

/**
 * Wrapper central de requisições HTTP para a Schedemy API.
 * - injeta o header Authorization (Basic Auth) quando houver sessão salva;
 * - serializa/desserializa JSON automaticamente;
 * - normaliza erros no formato ErrorResponseDTO do backend em uma ApiError.
 */
export async function apiFetch<T = unknown>(
  path: string,
  { method = "GET", body, params, signal }: OpcoesRequisicao = {}
): Promise<T> {
  const url = new URL(path.startsWith("http") ? path : `${BASE_URL}${path}`);
  if (params) {
    Object.entries(params).forEach(([chave, valor]) => {
      if (valor !== undefined && valor !== null && valor !== "") {
        url.searchParams.set(chave, String(valor));
      }
    });
  }

  const headers: Record<string, string> = { Accept: "application/json" };
  const authHeader = buildAuthHeader();
  if (authHeader) headers.Authorization = authHeader;
  if (body !== undefined) headers["Content-Type"] = "application/json";

  let response: Response;
  try {
    response = await fetch(url.toString(), {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
      signal,
    });
  } catch {
    throw new ApiError(0, {
      mensagem:
        "Não foi possível conectar à API. Verifique se o back-end está rodando em " +
        BASE_URL +
        " e se o CORS está liberado.",
    });
  }

  if (response.status === 204) {
    return null as T;
  }

  const textoBruto = await response.text();
  const dados = textoBruto ? safeJsonParse(textoBruto) : null;

  if (!response.ok) {
    if (response.status === 401 && authHeader) aoSessaoInvalida?.();
    throw new ApiError(response.status, dados as ErrorResponse | null);
  }
  return dados as T;
}

function safeJsonParse(texto: string): unknown {
  try {
    return JSON.parse(texto);
  } catch {
    return { mensagem: texto };
  }
}

export const api = {
  get: <T = unknown>(path: string, params?: Params, signal?: AbortSignal) =>
    apiFetch<T>(path, { method: "GET", params, signal }),
  post: <T = unknown>(path: string, body?: unknown) => apiFetch<T>(path, { method: "POST", body }),
  put: <T = unknown>(path: string, body?: unknown) => apiFetch<T>(path, { method: "PUT", body }),
  patch: <T = unknown>(path: string, body?: unknown) => apiFetch<T>(path, { method: "PATCH", body }),
  del: <T = unknown>(path: string) => apiFetch<T>(path, { method: "DELETE" }),
};

export { BASE_URL };
