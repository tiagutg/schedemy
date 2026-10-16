import { api } from "./client";
import type { Notificacao, Pagina, Params } from "@/types";

type Id = number | string;

export const notificacoesApi = {
  listar: (params?: Params) => api.get<Pagina<Notificacao>>("/api/v1/notificacoes", params),
  buscarPorId: (id: Id) => api.get<Notificacao>(`/api/v1/notificacoes/${id}`),
  criar: (dto: unknown) => api.post<Notificacao>("/api/v1/notificacoes", dto),
  marcarComoEnviada: (id: Id) => api.patch<Notificacao>(`/api/v1/notificacoes/${id}/enviar`),
  remover: (id: Id) => api.del(`/api/v1/notificacoes/${id}`),
};
