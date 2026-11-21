import { api } from "./client";
import type { LogAuditoria, Pagina, Params } from "@/types";

/** RNF 19: trilha de auditoria (só administração). */
export const auditoriaApi = {
  listar: (params?: Params) => api.get<Pagina<LogAuditoria>>("/api/v1/auditoria", params),
};
