// Controle de acesso por perfil no front-end.
//
// Baseado na seção 5.3 ("Relação Entre Atores e Casos de Uso") do Documento de
// Definição de Requisitos v3.1.0 e nas regras RN 03, RN 06 e RNF 15.
//
// IMPORTANTE: isto é apenas a camada de interface — esconder um botão não
// protege a API. A autorização definitiva precisa ser feita no back-end
// (SecurityConfig / @PreAuthorize), que hoje apenas exige autenticação.

import type { StoredAuth } from "@/api/client";
import type { Usuario } from "@/types";

/** Perfis de acesso. Os quatro primeiros espelham o enum TipoUsuario do back-end. */
export const PERFIS = {
  ALUNO: "ALUNO",
  PROFESSOR: "PROFESSOR",
  COORDENADOR: "COORDENADOR",
  RECEPCIONISTA: "RECEPCIONISTA",
  ADMIN: "ADMIN",
} as const;

export type Perfil = (typeof PERFIS)[keyof typeof PERFIS];

/**
 * Contas fixas do SecurityConfig do back-end mapeadas para o perfil de acesso.
 * A conta "admin" acumula ADMIN/COORDENADOR/RECEPCIONISTA e é tratada como
 * superusuário administrativo.
 */
const PERFIL_POR_USUARIO: Record<string, Perfil> = {
  admin: PERFIS.ADMIN,
  coordenador: PERFIS.COORDENADOR,
  professor: PERFIS.PROFESSOR,
  recepcionista: PERFIS.RECEPCIONISTA,
  aluno: PERFIS.ALUNO,
};

/**
 * Permissões atribuídas a cada perfil.
 *
 * - agendamentos.*  → RF 13, 15, 16, 17, 18, 19, 20, 21, 28
 * - disponibilidades / bloqueios → RF 01-05, 12 e RN 06 (só professor/coordenador)
 * - duracoes → RF 01 / RN 17: durações são definidas pela instituição
 * - usuarios → gestão administrativa de cadastros
 * - avaliacoes → RF 24 (aluno avalia) e RNF 11 (coordenação acompanha)
 * - notificacoes → RF 23: o envio é do ator Sistema; a tela é um log operacional
 */
// A seção 5.3 do documento trata "Professor/Coordenador" como um único ator:
// os dois perfis compartilham exatamente as mesmas permissões.
const PERMISSOES_DOCENTE = [
  "agendamentos.ver",
  "agendamentos.criar",
  "agendamentos.cancelar",
  "agendamentos.responder",
  "disponibilidades.gerenciar",
  "bloqueios.gerenciar",
  "duracoes.gerenciar",
  "avaliacoes.ver",
  "notificacoes.ver",
];

const PERMISSOES_POR_PERFIL: Record<string, string[]> = {
  [PERFIS.ALUNO]: [
    "agendamentos.ver",
    "agendamentos.criar",
    "agendamentos.cancelar",
    "agendamentos.responder",
    "avaliacoes.ver",
    "avaliacoes.criar",
  ],
  [PERFIS.PROFESSOR]: PERMISSOES_DOCENTE,
  [PERFIS.COORDENADOR]: PERMISSOES_DOCENTE,
  // RN 03: a recepção não é convidada das reuniões, então não aceita/recusa convites.
  [PERFIS.RECEPCIONISTA]: ["agendamentos.ver", "agendamentos.criar", "agendamentos.cancelar"],
  [PERFIS.ADMIN]: ["*"],
};

/** Rótulo legível do perfil, usado no cabeçalho e no menu lateral. */
export const ROTULOS_PERFIL: Record<string, string> = {
  [PERFIS.ALUNO]: "Aluno",
  [PERFIS.PROFESSOR]: "Professor",
  [PERFIS.COORDENADOR]: "Coordenador",
  [PERFIS.RECEPCIONISTA]: "Recepcionista",
  [PERFIS.ADMIN]: "Administrador",
};

/**
 * Descobre o perfil da sessão.
 *
 * A conta autenticada (Basic Auth) é a fonte principal, porque é ela que define
 * as roles no back-end. O perfil vinculado (Usuario) serve de reserva para
 * contas que não estejam na lista fixa do SecurityConfig.
 */
export function derivarPerfil(auth: StoredAuth | null, perfilVinculado: Usuario | null): string | null {
  const porConta = PERFIL_POR_USUARIO[auth?.username?.trim().toLowerCase() as string];
  if (porConta) return porConta;
  if (perfilVinculado?.tipoUsuario) return perfilVinculado.tipoUsuario;
  return null;
}

/** Indica se o perfil possui a permissão informada. */
export function perfilPode(perfil: string | null, permissao: string): boolean {
  if (!perfil || !permissao) return false;
  const permissoes = PERMISSOES_POR_PERFIL[perfil] || [];
  return permissoes.includes("*") || permissoes.includes(permissao);
}
