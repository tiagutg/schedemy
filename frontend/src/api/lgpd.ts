import { api, apiFetch } from "./client";
import type { Consentimento, Termo } from "@/types";

/** RNF 18: termo e política (público) e o aceite do usuário logado. */
export const lgpdApi = {
  termo: () => apiFetch<Termo>("/api/v1/termos", { semAutenticacao: true }),
  situacao: () => api.get<Consentimento>("/api/v1/consentimentos/meu"),
  aceitar: (versao: string) => api.post<Consentimento>("/api/v1/consentimentos", { versao }),
};
