import { api } from "./client";
import type { Pagina, Params, Usuario, UsuarioResumo } from "@/types";

type Id = number | string;

export const usuariosApi = {
  /** Cadastro completo (com e-mail e telefone): somente admin e recepção. */
  listar: (params?: Params) => api.get<Pagina<Usuario>>("/api/v1/usuarios", params),
  /** Resumo para escolher convidados e filtros, liberado a qualquer perfil (RNF 18). */
  listarResumo: (params?: Params) => api.get<Pagina<UsuarioResumo>>("/api/v1/usuarios/resumo", params),
  buscarPorId: (id: Id) => api.get<Usuario>(`/api/v1/usuarios/${id}`),
  /** Cadastro do usuário logado (também valida a credencial: 401 se inválida). */
  eu: () => api.get<Usuario>("/api/v1/usuarios/me"),
  criar: (dto: unknown) => api.post<Usuario>("/api/v1/usuarios", dto),
  atualizar: (id: Id, dto: unknown) => api.put<Usuario>(`/api/v1/usuarios/${id}`, dto),
  remover: (id: Id) => api.del(`/api/v1/usuarios/${id}`),
};
