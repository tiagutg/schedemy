import { api } from "./client";
import type { Pagina, Params, Usuario } from "@/types";

type Id = number | string;

export const usuariosApi = {
  listar: (params?: Params) => api.get<Pagina<Usuario>>("/api/v1/usuarios", params),
  buscarPorId: (id: Id) => api.get<Usuario>(`/api/v1/usuarios/${id}`),
  criar: (dto: unknown) => api.post<Usuario>("/api/v1/usuarios", dto),
  atualizar: (id: Id, dto: unknown) => api.put<Usuario>(`/api/v1/usuarios/${id}`, dto),
  remover: (id: Id) => api.del(`/api/v1/usuarios/${id}`),
};
