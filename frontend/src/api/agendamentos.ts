import { api } from "./client";
import type { Agendamento, Pagina, Params } from "@/types";

type Id = number | string;

export const agendamentosApi = {
  listarPorUsuario: (usuarioId: Id, params?: Params) =>
    api.get<Pagina<Agendamento>>(`/api/v1/agendamentos/usuarios/${usuarioId}`, params),
  listarTodos: (params?: Params) => api.get<Pagina<Agendamento>>("/api/v1/agendamentos", params),
  buscarPorId: (id: Id) => api.get<Agendamento>(`/api/v1/agendamentos/${id}`),
  criar: (dto: unknown) => api.post<Agendamento>("/api/v1/agendamentos", dto),
  atualizar: (id: Id, dto: unknown) => api.put<Agendamento>(`/api/v1/agendamentos/${id}`, dto),
  responder: (id: Id, dto: unknown) => api.patch<Agendamento>(`/api/v1/agendamentos/${id}/resposta`, dto),
  cancelar: (id: Id, dto: unknown) => api.patch<Agendamento>(`/api/v1/agendamentos/${id}/cancelamento`, dto),
  remover: (id: Id) => api.del(`/api/v1/agendamentos/${id}`),
};
