import { api } from "./client";
import type { Bloqueio, Pagina, Params, ReuniaoAfetada } from "@/types";

type Id = number | string;

export const bloqueiosApi = {
  listar: (params?: Params) => api.get<Pagina<Bloqueio>>("/api/v1/bloqueios-periodo", params),
  buscarPorId: (id: Id) => api.get<Bloqueio>(`/api/v1/bloqueios-periodo/${id}`),
  /** Com reuniões afetadas, o corpo precisa de cancelarAfetadas (senão, 422 com a lista). */
  criar: (dto: unknown) => api.post<Bloqueio>("/api/v1/bloqueios-periodo", dto),
  atualizar: (id: Id, dto: unknown) => api.put<Bloqueio>(`/api/v1/bloqueios-periodo/${id}`, dto),
  remover: (id: Id) => api.del(`/api/v1/bloqueios-periodo/${id}`),
  /** RF 06/07: reuniões que o bloqueio novo cancelaria (não grava nada). */
  previaDaCriacao: (dto: unknown) => api.post<ReuniaoAfetada[]>("/api/v1/bloqueios-periodo/previa", dto),
  /** RF 06/07: reuniões que a edição cancelaria (não grava nada). */
  previaDaEdicao: (id: Id, dto: unknown) => api.post<ReuniaoAfetada[]>(`/api/v1/bloqueios-periodo/${id}/previa`, dto),
};
