// Controle de acesso por perfil no front-end.
//
// Baseado no Documento de Definição de Requisitos v3.1.0 (atores citados nos
// RFs) e nas regras RN 03, RN 06 e RNF 15.
//
// IMPORTANTE: isto é apenas a camada de interface — esconder um botão não
// protege a API. A autorização definitiva é feita no back-end (SecurityConfig).

import type { Usuario } from "@/types";

/** Perfis de acesso. Espelham o enum TipoUsuario do back-end. */
export const PERFIS = {
  ALUNO: "ALUNO",
  PROFESSOR: "PROFESSOR",
  COORDENADOR: "COORDENADOR",
  RECEPCIONISTA: "RECEPCIONISTA",
  ADMIN: "ADMIN",
} as const;

export type Perfil = (typeof PERFIS)[keyof typeof PERFIS];

/**
 * Permissões atribuídas a cada perfil.
 *
 * - agendamentos.*  → RF 13, 15, 16, 17, 18, 19, 20, 21, 28
 * - agendamentos.pedirAlteracao → RF 21: aluno (ou a recepção por ele) pede alteração de reunião confirmada
 * - agendamentos.decidirAlteracao → RF 22 / RN 12: só professor ou coordenador aceita ou recusa
 *   (a lista de pedidos, GET /solicitacoes-edicao, é de todos os perfis: o escopo pelo logado fica na API)
 * - disponibilidades / bloqueios → RF 01-05, 12 e RN 06 (só professor/coordenador)
 * - duracoes → RF 01 / RN 17: durações são definidas pela instituição, só a administração gerencia (admin tem "*")
 * - usuarios → gestão administrativa de cadastros
 * - avaliacoes → RF 24 (aluno avalia) e RNF 11 (coordenação acompanha)
 * - notificacoes → RF 23: o sistema gera os avisos; cada perfil lê os seus
 * - auditoria.ver → RNF 19: trilha de auditoria, só administração (o admin tem "*")
 * - o termo LGPD (RNF 18) vale para todos os perfis: sem aceitar, nada abre (RotaProtegida e a API)
 *
 * Por padrão cada pessoa age em nome próprio (organiza, cancela, responde e
 * avalia como o usuário logado). As permissões abaixo liberam agir por outros:
 * - agendamentos.verTodos / organizarPorOutros → recepção agenda para terceiros (RN 03)
 * - agenda.gerenciarTodos → disponibilidades e bloqueios de qualquer docente
 * - avaliacoes.verTodas → docência acompanha as avaliações dos alunos (RNF 11)
 */
// Os RFs sempre citam "professor ou coordenador" juntos: os dois perfis
// compartilham exatamente as mesmas permissões.
const PERMISSOES_DOCENTE = [
  "agendamentos.ver",
  "agendamentos.criar",
  "agendamentos.cancelar",
  "agendamentos.responder",
  "agendamentos.decidirAlteracao",
  "disponibilidades.gerenciar",
  "bloqueios.gerenciar",
  "avaliacoes.ver",
  "avaliacoes.verTodas",
  "notificacoes.ver",
];

const PERMISSOES_POR_PERFIL: Record<string, string[]> = {
  [PERFIS.ALUNO]: [
    "agendamentos.ver",
    "agendamentos.criar",
    "agendamentos.cancelar",
    "agendamentos.responder",
    "agendamentos.pedirAlteracao",
    "avaliacoes.ver",
    "avaliacoes.criar",
    "notificacoes.ver",
  ],
  [PERFIS.PROFESSOR]: PERMISSOES_DOCENTE,
  [PERFIS.COORDENADOR]: PERMISSOES_DOCENTE,
  // RN 03: a recepção não é convidada das reuniões, então não aceita/recusa convites.
  [PERFIS.RECEPCIONISTA]: [
    "agendamentos.ver",
    "agendamentos.verTodos",
    "agendamentos.criar",
    "agendamentos.organizarPorOutros",
    "agendamentos.cancelar",
    "agendamentos.pedirAlteracao",
    "notificacoes.ver",
  ],
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
 * O login é feito com o e-mail de um usuário cadastrado, e o back-end usa o
 * tipoUsuario desse cadastro como role; o perfil segue a mesma regra.
 */
export function derivarPerfil(perfilVinculado: Usuario | null): string | null {
  return perfilVinculado?.tipoUsuario ?? null;
}

/** Indica se o perfil possui a permissão informada. */
export function perfilPode(perfil: string | null, permissao: string): boolean {
  if (!perfil || !permissao) return false;
  const permissoes = PERMISSOES_POR_PERFIL[perfil] || [];
  return permissoes.includes("*") || permissoes.includes(permissao);
}
