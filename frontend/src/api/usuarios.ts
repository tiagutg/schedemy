import { api } from "./client";
import type { Pagina, Params, Usuario } from "@/types";

type Id = number | string;

export const usuariosApi = {
  listar: (params?: Params) => api.get<Pagina<Usuario>>("/api/v1/usuarios", params),
  buscarPorId: (id: Id) => api.get<Usuario>(`/api/v1/usuarios/${id}`),
  criar: (dto: unknown) => api.post<Usuario>("/api/v1/usuarios", dto),
  atualizar: (id: Id, dto: unknown) => api.put<Usuario>(`/api/v1/usuarios/${id}`, dto),
  remover: (id: Id) => api.del(`/api/v1/usuarios/${id}`),

  /**
   * Localiza o cadastro de um e-mail percorrendo a listagem.
   * Paliativo até o back-end expor o usuário logado (ex.: GET /api/v1/usuarios/me).
   */
  async buscarPorEmail(email: string): Promise<Usuario | null> {
    const alvo = email.trim().toLowerCase();
    for (let page = 0; ; page++) {
      const pagina = await usuariosApi.listar({ page, size: 200 });
      const usuario = pagina.conteudo.find((u) => u.email.toLowerCase() === alvo);
      if (usuario) return usuario;
      if (pagina.ultima || pagina.conteudo.length === 0) return null;
    }
  },
};
