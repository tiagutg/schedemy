import { api } from "./client";
import type { CancelamentoDeAfetadas, Disponibilidade, Pagina, Params, ReuniaoAfetada } from "@/types";

type Id = number | string;

export const disponibilidadesApi = {
  listar: (params?: Params) => api.get<Pagina<Disponibilidade>>("/api/v1/disponibilidades", params),
  buscarPorId: (id: Id) => api.get<Disponibilidade>(`/api/v1/disponibilidades/${id}`),
  criar: (dto: unknown) => api.post<Disponibilidade>("/api/v1/disponibilidades", dto),
  /** Com reuniões afetadas, o corpo precisa de cancelarAfetadas (senão, 422 com a lista). */
  atualizar: (id: Id, dto: unknown) => api.put<Disponibilidade>(`/api/v1/disponibilidades/${id}`, dto),
  remover: (id: Id) => api.del(`/api/v1/disponibilidades/${id}`),
  /** RF 06: remove e cancela as reuniões afetadas confirmadas. */
  removerCancelando: (id: Id, dto: CancelamentoDeAfetadas) =>
    api.post<null>(`/api/v1/disponibilidades/${id}/remocao`, dto),
  /** RF 06/07: reuniões que a edição cancelaria (não grava nada). */
  previaDaEdicao: (id: Id, dto: unknown) => api.post<ReuniaoAfetada[]>(`/api/v1/disponibilidades/${id}/previa`, dto),
  /** RF 06/07: reuniões que a remoção cancelaria (não grava nada). */
  previaDaRemocao: (id: Id) => api.post<ReuniaoAfetada[]>(`/api/v1/disponibilidades/${id}/remocao/previa`),
};
