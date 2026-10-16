// Espelha os enums Java em br.edu.unisales.schedemy.domain.enums

export const TIPOS_USUARIO = ["ALUNO", "PROFESSOR", "COORDENADOR", "RECEPCIONISTA"];

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
];

/** Mapeia o status do agendamento para a classe de cor usada nas pílulas (pill) da UI. */
export const CORES_STATUS_AGENDAMENTO: Record<string, string> = {
  PENDENTE: "bg-ink-100 text-ink-800",
  AGUARDANDO_RESPOSTA: "bg-brand-100 text-brand-700",
  CONFIRMADO: "bg-sage-100 text-sage-700",
  EM_ANDAMENTO: "bg-sage-100 text-sage-700",
  CONCLUIDO: "bg-brand-700 text-white",
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
