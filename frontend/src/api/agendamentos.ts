import { api } from "./client";
import type { Agendamento, HorariosLivres, Pagina, Params, SolicitacaoEdicao } from "@/types";

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
  /** Remarcação: cancela a original e cria a nova (com idAgendamentoOrigem) numa só transação. */
  remarcar: (id: Id, dto: unknown) => api.post<Agendamento>(`/api/v1/agendamentos/${id}/remarcacao`, dto),
  remover: (id: Id) => api.del(`/api/v1/agendamentos/${id}`),
  /** RF 21: pede a alteração de reunião confirmada (aluno participante ou recepção). */
  pedirAlteracao: (id: Id, dto: unknown) =>
    api.post<SolicitacaoEdicao>(`/api/v1/agendamentos/${id}/solicitacoes-edicao`, dto),
  /** Pedidos de alteração da reunião, do mais recente para o mais antigo. */
  listarPedidosDeAlteracao: (id: Id) =>
    api.get<SolicitacaoEdicao[]>(`/api/v1/agendamentos/${id}/solicitacoes-edicao`),
  /** RF 22: o docente aceita ou recusa (motivo obrigatório ao recusar). */
  responderPedidoDeAlteracao: (idPedido: Id, dto: { aceitar: boolean; motivo?: string | null }) =>
    api.patch<SolicitacaoEdicao>(`/api/v1/solicitacoes-edicao/${idPedido}/resposta`, dto),
  /** Quem pediu (ou a recepção) desiste. */
  desistirDoPedidoDeAlteracao: (idPedido: Id) =>
    api.patch<SolicitacaoEdicao>(`/api/v1/solicitacoes-edicao/${idPedido}/cancelamento`),
};
