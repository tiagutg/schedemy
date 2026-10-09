import { api } from "./client";
import type { Disponibilidade, Pagina, Params } from "@/types";

type Id = number | string;

export const disponibilidadesApi = {
  listar: (params?: Params) => api.get<Pagina<Disponibilidade>>("/api/v1/disponibilidades", params),
  buscarPorId: (id: Id) => api.get<Disponibilidade>(`/api/v1/disponibilidades/${id}`),
  criar: (dto: unknown) => api.post<Disponibilidade>("/api/v1/disponibilidades", dto),
  atualizar: (id: Id, dto: unknown) => api.put<Disponibilidade>(`/api/v1/disponibilidades/${id}`, dto),
  remover: (id: Id) => api.del(`/api/v1/disponibilidades/${id}`),
};
