import { api } from "./client";
import type { Agendamento, Bloqueio, Pagina, Params } from "@/types";

type Id = number | string;

export const bloqueiosApi = {
  listar: (params?: Params) => api.get<Pagina<Bloqueio>>("/api/v1/bloqueios-periodo", params),
  buscarPorId: (id: Id) => api.get<Bloqueio>(`/api/v1/bloqueios-periodo/${id}`),
  criar: (dto: unknown) => api.post<Bloqueio>("/api/v1/bloqueios-periodo", dto),
  atualizar: (id: Id, dto: unknown) => api.put<Bloqueio>(`/api/v1/bloqueios-periodo/${id}`, dto),
  remover: (id: Id) => api.del(`/api/v1/bloqueios-periodo/${id}`),
  listarImpactados: (id: Id) => api.get<Agendamento[]>(`/api/v1/bloqueios-periodo/${id}/agendamentos-impactados`),
};
