import { api } from "./client";
import type { Avaliacao, Pagina, Params } from "@/types";

type Id = number | string;

export const avaliacoesApi = {
  listarPorAluno: (alunoId: Id, params?: Params) =>
    api.get<Pagina<Avaliacao>>(`/api/v1/avaliacoes/alunos/${alunoId}`, params),
  buscarPorId: (id: Id) => api.get<Avaliacao>(`/api/v1/avaliacoes/${id}`),
  criar: (dto: unknown) => api.post<Avaliacao>("/api/v1/avaliacoes", dto),
  atualizar: (id: Id, dto: unknown) => api.put<Avaliacao>(`/api/v1/avaliacoes/${id}`, dto),
  remover: (id: Id) => api.del(`/api/v1/avaliacoes/${id}`),
};
