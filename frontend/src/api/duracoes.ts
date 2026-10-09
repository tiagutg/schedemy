import { api } from "./client";
import type { Duracao } from "@/types";

type Id = number | string;

export const duracoesApi = {
  listar: (apenasAtivas = false) => api.get<Duracao[]>("/api/v1/duracoes-reuniao", { ativas: apenasAtivas }),
  buscarPorId: (id: Id) => api.get<Duracao>(`/api/v1/duracoes-reuniao/${id}`),
  criar: (dto: unknown) => api.post<Duracao>("/api/v1/duracoes-reuniao", dto),
  atualizar: (id: Id, dto: unknown) => api.put<Duracao>(`/api/v1/duracoes-reuniao/${id}`, dto),
  remover: (id: Id) => api.del(`/api/v1/duracoes-reuniao/${id}`),
};
