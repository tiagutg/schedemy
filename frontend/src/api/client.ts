import type { ErrorResponse, Params } from "@/types";
import { lerSessao, type StoredAuth } from "@/utils/sessao";

const BASE_URL: string = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";
const AUTH_STORAGE_KEY = "schedemy.auth";

export type { StoredAuth };

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
    const sessao = lerSessao(raw);
    // Apaga o que não serve mais — inclusive a senha do formato antigo (Basic Auth).
    if (raw && !sessao) localStorage.removeItem(AUTH_STORAGE_KEY);
    return sessao;
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
let aoTermoPendente: (() => void) | null = null;

/** Erro da API para quem ainda não aceitou a versão em vigor do termo (RNF 18). */
export const ERRO_TERMO_PENDENTE = "TERMO_PENDENTE";

/** Registra quem trata um 401 numa requisição autenticada (token vencido ou que deixou de valer). */
export function definirAoSessaoInvalida(tratador: (() => void) | null) {
  aoSessaoInvalida = tratador;
}

/** RNF 18: registra quem trata o 403 de termo pendente (ex.: versão nova publicada com a sessão aberta). */
export function definirAoTermoPendente(tratador: (() => void) | null) {
  aoTermoPendente = tratador;
}

function buildAuthHeader() {
  const auth = getStoredAuth();
  return auth ? `Bearer ${auth.token}` : null;
}

interface OpcoesRequisicao {
  method?: string;
  body?: unknown;
  params?: Params;
  signal?: AbortSignal;
  /** Não envia o token (login). */
  semAutenticacao?: boolean;
}

/**
 * Wrapper central de requisições HTTP para a Schedemy API.
 * - injeta o header Authorization (Bearer, token do login) quando houver sessão salva;
 * - serializa/desserializa JSON automaticamente;
 * - normaliza erros no formato ErrorResponseDTO do backend em uma ApiError.
 */
export async function apiFetch<T = unknown>(
  path: string,
  { method = "GET", body, params, signal, semAutenticacao = false }: OpcoesRequisicao = {}
): Promise<T> {
  const url = new URL(path.startsWith("http") ? path : `${BASE_URL}${path}`);
  if (params) {
    Object.entries(params).forEach(([chave, valor]) => {
      // Listas viram parâmetros repetidos (ex.: sort=data,asc&sort=hora,asc).
      for (const item of Array.isArray(valor) ? valor : [valor]) {
        if (item !== undefined && item !== null && item !== "") {
          url.searchParams.append(chave, String(item));
        }
      }
    });
  }

  const headers: Record<string, string> = { Accept: "application/json" };
  const authHeader = semAutenticacao ? null : buildAuthHeader();
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
  } catch (erroDeRede) {
    // O detalhe técnico fica no console para quem desenvolve; a tela mostra só o que a pessoa pode fazer.
    if (!signal?.aborted) {
      console.error(`Falha de rede ao chamar ${url.toString()} (API em ${BASE_URL}; confira se ela está no ar e o CORS).`, erroDeRede);
    }
    throw new ApiError(0, {
      mensagem:
        "Não foi possível conectar ao Schedemy. Verifique sua internet e tente de novo. " +
        "Se o sistema estava sem uso, ele pode levar até 1 minuto para voltar.",
    });
  }

  if (response.status === 204) {
    return null as T;
  }

  const textoBruto = await response.text();
  const dados = textoBruto ? safeJsonParse(textoBruto) : null;

  if (!response.ok) {
    if (response.status === 401 && authHeader) aoSessaoInvalida?.();
    if (response.status === 403 && (dados as ErrorResponse | null)?.erro === ERRO_TERMO_PENDENTE) aoTermoPendente?.();
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
