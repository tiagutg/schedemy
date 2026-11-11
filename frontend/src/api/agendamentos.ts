import { api } from "./client";
import type { Agendamento, HorariosLivres, Pagina, Params } from "@/types";

type Id = number | string;

export const agendamentosApi = {
  listarPorUsuario: (usuarioId: Id, params?: Params) =>
    api.get<Pagina<Agendamento>>(`/api/v1/agendamentos/usuarios/${usuarioId}`, params),
  listarTodos: (params?: Params) => api.get<Pagina<Agendamento>>("/api/v1/agendamentos", params),
  buscarPorId: (id: Id) => api.get<Agendamento>(`/api/v1/agendamentos/${id}`),
  /** RF 14: só os horários livres para todos; não expõe reuniões nem bloqueios de ninguém. */
  horariosLivres: (
    params: { data: string; idDuracao: Id; idsUsuarios: Id[]; ignorarAgendamentoId?: Id | null },
    signal?: AbortSignal
  ) =>
    api.get<HorariosLivres>(
      "/api/v1/agendamentos/horarios-livres",
      { ...params, idsUsuarios: params.idsUsuarios.join(",") },
      signal
    ),
  criar: (dto: unknown) => api.post<Agendamento>("/api/v1/agendamentos", dto),
  atualizar: (id: Id, dto: unknown) => api.put<Agendamento>(`/api/v1/agendamentos/${id}`, dto),
  responder: (id: Id, dto: unknown) => api.patch<Agendamento>(`/api/v1/agendamentos/${id}/resposta`, dto),
  cancelar: (id: Id, dto: unknown) => api.patch<Agendamento>(`/api/v1/agendamentos/${id}/cancelamento`, dto),
  /** RF 28: cancela a original e cria a nova (com idAgendamentoOrigem) numa só transação. */
  remarcar: (id: Id, dto: unknown) => api.post<Agendamento>(`/api/v1/agendamentos/${id}/remarcacao`, dto),
  remover: (id: Id) => api.del(`/api/v1/agendamentos/${id}`),
};
