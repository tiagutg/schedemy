import { api } from "./client";
import type { Pagina, Params, RegistroMotivo } from "@/types";

type Id = number | string;

export const registrosMotivoApi = {
  listarPorAgendamento: (agendamentoId: Id, params?: Params) =>
    api.get<Pagina<RegistroMotivo>>(`/api/v1/registros-motivo/agendamentos/${agendamentoId}`, params),
  buscarPorId: (id: Id) => api.get<RegistroMotivo>(`/api/v1/registros-motivo/${id}`),
  criar: (dto: unknown) => api.post<RegistroMotivo>("/api/v1/registros-motivo", dto),
};
