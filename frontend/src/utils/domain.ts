// Espelha os enums Java em br.edu.unisales.schedemy.domain.enums

export const TIPOS_USUARIO = ["ALUNO", "PROFESSOR", "COORDENADOR", "RECEPCIONISTA", "ADMIN"];

// RN 03: recepção e administração não participam das reuniões, só agendam para outros.
export const TIPOS_PARTICIPANTES = ["ALUNO", "PROFESSOR", "COORDENADOR"];

export const FORMATOS_REUNIAO = ["PRESENCIAL", "ONLINE", "HIBRIDO"];

export const STATUS_AGENDAMENTO = [
  "PENDENTE",
  "AGUARDANDO_RESPOSTA",
  "CONFIRMADO",
  "EM_ANDAMENTO",
  "CONCLUIDO",
  "CANCELADO",
];

export const DIAS_SEMANA = [
  { valor: 1, rotulo: "Domingo" },
  { valor: 2, rotulo: "Segunda-feira" },
  { valor: 3, rotulo: "Terça-feira" },
  { valor: 4, rotulo: "Quarta-feira" },
  { valor: 5, rotulo: "Quinta-feira" },
  { valor: 6, rotulo: "Sexta-feira" },
  { valor: 7, rotulo: "Sábado" },
];

export const STATUS_ENVIO = ["PENDENTE", "ENVIADO", "FALHA"];

export const TIPOS_OPERACAO_MOTIVO = [
  "CANCELAMENTO",
  "SOLICITACAO_EDICAO",
  "RECUSA_AGENDAMENTO",
  "RECUSA_REMARCACAO",
  "RECUSA_SOLICITACAO_EDICAO",
];

/** Mapeia o status do agendamento para a classe de cor usada nas pílulas (pill) da UI. */
export const CORES_STATUS_AGENDAMENTO: Record<string, string> = {
  PENDENTE: "bg-ink-100 text-ink-800",
  AGUARDANDO_RESPOSTA: "bg-brand-100 text-brand-700",
  CONFIRMADO: "bg-sage-100 text-sage-700",
  EM_ANDAMENTO: "bg-sage-100 text-sage-700",
  CONCLUIDO: "bg-marinho-700 text-white",
  CANCELADO: "bg-brick-100 text-brick-700",
};

export const CORES_STATUS_ENVIO: Record<string, string> = {
  PENDENTE: "bg-ink-100 text-ink-800",
  ENVIADO: "bg-sage-100 text-sage-700",
  FALHA: "bg-brick-100 text-brick-700",
};

export const CORES_TIPO_USUARIO: Record<string, string> = {
  ALUNO: "bg-ink-100 text-ink-800",
  PROFESSOR: "bg-sage-100 text-sage-700",
  COORDENADOR: "bg-brand-100 text-brand-700",
  RECEPCIONISTA: "bg-brick-100 text-brick-700",
  ADMIN: "bg-marinho-700 text-white",
};

export function rotularEnum(valor: string | null | undefined): string {
  if (!valor) return "";
  return valor
    .toLowerCase()
    .split("_")
    .map((parte) => parte.charAt(0).toUpperCase() + parte.slice(1))
    .join(" ");
}

export function rotularDiaSemana(valor: number | string): string {
  return DIAS_SEMANA.find((d) => d.valor === Number(valor))?.rotulo ?? `Dia ${valor}`;
}

/** RNF 19: ações da trilha de auditoria (espelha o enum AcaoAuditoria do back-end), com o rótulo da tela. */
export const ACOES_AUDITORIA: Record<string, string> = {
  AGENDAMENTO_CRIADO: "Reunião criada",
  AGENDAMENTO_EDITADO: "Reunião editada",
  AGENDAMENTO_REMARCADO: "Reunião remarcada",
  AGENDAMENTO_RESPONDIDO: "Convite respondido",
  AGENDAMENTO_CANCELADO: "Reunião cancelada",
  AGENDAMENTO_EXPIRADO: "Reunião cancelada sem confirmação",
  AGENDAMENTO_REMOVIDO: "Reunião removida",
  PEDIDO_ALTERACAO_CRIADO: "Alteração pedida",
  PEDIDO_ALTERACAO_ACEITO: "Alteração aceita",
  PEDIDO_ALTERACAO_RECUSADO: "Alteração recusada",
  PEDIDO_ALTERACAO_DESISTIDO: "Pedido de alteração retirado",
  PEDIDO_ALTERACAO_EXPIRADO: "Pedido de alteração expirado",
  DISPONIBILIDADE_CRIADA: "Horário de atendimento criado",
  DISPONIBILIDADE_EDITADA: "Horário de atendimento editado",
  DISPONIBILIDADE_REMOVIDA: "Horário de atendimento removido",
  BLOQUEIO_CRIADO: "Bloqueio criado",
  BLOQUEIO_EDITADO: "Bloqueio editado",
  BLOQUEIO_REMOVIDO: "Bloqueio removido",
  DURACAO_CRIADA: "Duração criada",
  DURACAO_EDITADA: "Duração editada",
  DURACAO_REMOVIDA: "Duração removida",
  USUARIO_CRIADO: "Usuário criado",
  USUARIO_EDITADO: "Usuário editado",
  USUARIO_ATIVADO: "Usuário ativado",
  USUARIO_DESATIVADO: "Usuário desativado",
  USUARIO_SENHA_ALTERADA: "Senha alterada",
  USUARIO_REMOVIDO: "Usuário removido",
  LOGIN: "Login",
  LOGIN_RECUSADO: "Login recusado",
  TERMO_ACEITO: "Termo LGPD aceito",
};

/** RNF 19: registros que a trilha referencia (log_auditoria.tabela_afetada). */
export const TABELAS_AUDITORIA: { valor: string; rotulo: string }[] = [
  { valor: "agendamento", rotulo: "Reunião" },
  { valor: "solicitacao_edicao", rotulo: "Pedido de alteração" },
  { valor: "disponibilidade", rotulo: "Horário de atendimento" },
  { valor: "bloqueio_periodo", rotulo: "Bloqueio" },
  { valor: "duracao_reuniao", rotulo: "Duração" },
  { valor: "usuario", rotulo: "Usuário" },
  { valor: "consentimento_termo", rotulo: "Termo LGPD" },
];
