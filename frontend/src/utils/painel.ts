// Números do painel calculados a partir das reuniões de um período: resumo do mês
// com a variação em relação ao mês anterior e a série diária do gráfico.

import type { Agendamento } from "@/types";

export type Periodo = "prox30" | "30d" | "90d";

export const PERIODOS: { valor: Periodo; rotulo: string; descricao: string }[] = [
  { valor: "prox30", rotulo: "Próximos 30 dias", descricao: "Reuniões marcadas para os próximos 30 dias" },
  { valor: "30d", rotulo: "Últimos 30 dias", descricao: "Reuniões dos últimos 30 dias" },
  { valor: "90d", rotulo: "Últimos 3 meses", descricao: "Reuniões dos últimos 3 meses" },
];

const AGUARDANDO = ["PENDENTE", "AGUARDANDO_RESPOSTA"];
// Para os números do mês, confirmada e concluída contam como reunião que vai/foi acontecer.
const VALIDAS = ["CONFIRMADO", "CONCLUIDO"];

export function paraISO(d: Date): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

function somarDias(d: Date, dias: number): Date {
  return new Date(d.getFullYear(), d.getMonth(), d.getDate() + dias);
}

/** Intervalo de datas (inclusivo) de cada período do gráfico. */
export function intervaloDoPeriodo(periodo: Periodo, hoje = new Date()): { inicio: string; fim: string } {
  if (periodo === "prox30") return { inicio: paraISO(hoje), fim: paraISO(somarDias(hoje, 30)) };
  const dias = periodo === "30d" ? 30 : 90;
  return { inicio: paraISO(somarDias(hoje, -dias)), fim: paraISO(hoje) };
}

/** Tudo o que o painel precisa: do início do mês anterior (ou 3 meses atrás) até 30 dias à frente. */
export function intervaloDoPainel(hoje = new Date()): { inicio: string; fim: string } {
  const inicioMesAnterior = new Date(hoje.getFullYear(), hoje.getMonth() - 1, 1);
  const tresMeses = somarDias(hoje, -90);
  return {
    inicio: paraISO(inicioMesAnterior < tresMeses ? inicioMesAnterior : tresMeses),
    fim: paraISO(somarDias(hoje, 30)),
  };
}

export interface PontoDiario {
  data: string;
  confirmadas: number;
  aguardando: number;
  canceladas: number;
}

/** Uma entrada por dia do intervalo (dias sem reunião entram com zero, para o gráfico não pular datas). */
export function serieDiaria(agendamentos: Agendamento[], inicio: string, fim: string): PontoDiario[] {
  const porDia = new Map<string, PontoDiario>();
  const [a, m, d] = inicio.split("-").map(Number);
  for (let dia = new Date(a, m - 1, d); paraISO(dia) <= fim; dia = somarDias(dia, 1)) {
    const data = paraISO(dia);
    porDia.set(data, { data, confirmadas: 0, aguardando: 0, canceladas: 0 });
  }
  for (const ag of agendamentos) {
    const ponto = porDia.get(ag.dataReuniao);
    if (!ponto) continue;
    if (VALIDAS.includes(ag.status)) ponto.confirmadas++;
    else if (AGUARDANDO.includes(ag.status)) ponto.aguardando++;
    else if (ag.status === "CANCELADO") ponto.canceladas++;
  }
  return [...porDia.values()];
}

interface NumerosDoMes {
  /** Reuniões não canceladas com data no mês. */
  total: number;
  concluidas: number;
  canceladas: number;
  /** Canceladas sobre todas as marcadas no mês (0 a 100), ou null sem reuniões. */
  taxaCancelamento: number | null;
}

function numerosDoMes(agendamentos: Agendamento[], ano: number, mes: number): NumerosDoMes {
  const prefixo = `${ano}-${String(mes + 1).padStart(2, "0")}-`;
  const doMes = agendamentos.filter((a) => a.dataReuniao.startsWith(prefixo));
  const canceladas = doMes.filter((a) => a.status === "CANCELADO").length;
  return {
    total: doMes.length - canceladas,
    concluidas: doMes.filter((a) => a.status === "CONCLUIDO").length,
    canceladas,
    taxaCancelamento: doMes.length ? Math.round((canceladas / doMes.length) * 100) : null,
  };
}

export interface ResumoDoMes {
  atual: NumerosDoMes;
  anterior: NumerosDoMes;
  /** Aguardando resposta, de hoje em diante. */
  aguardando: number;
}

export function resumoDoMes(agendamentos: Agendamento[], hoje = new Date()): ResumoDoMes {
  const hojeISO = paraISO(hoje);
  return {
    atual: numerosDoMes(agendamentos, hoje.getFullYear(), hoje.getMonth()),
    anterior: numerosDoMes(
      agendamentos,
      hoje.getMonth() === 0 ? hoje.getFullYear() - 1 : hoje.getFullYear(),
      (hoje.getMonth() + 11) % 12
    ),
    aguardando: agendamentos.filter((a) => AGUARDANDO.includes(a.status) && a.dataReuniao >= hojeISO).length,
  };
}

/** Variação percentual de `anterior` para `atual`; null quando não há base de comparação. */
export function variacaoPercentual(atual: number, anterior: number): number | null {
  if (anterior === 0) return null;
  return Math.round(((atual - anterior) / anterior) * 100);
}

/** Próximas reuniões ativas, em ordem de data e hora. */
export function proximasReunioes(agendamentos: Agendamento[], quantidade: number, hoje = new Date()): Agendamento[] {
  const hojeISO = paraISO(hoje);
  return agendamentos
    .filter((a) => a.dataReuniao >= hojeISO && [...AGUARDANDO, "CONFIRMADO"].includes(a.status))
    .sort((x, y) => (x.dataReuniao + x.horarioInicio).localeCompare(y.dataReuniao + y.horarioInicio))
    .slice(0, quantidade);
}

const MESES = ["janeiro", "fevereiro", "março", "abril", "maio", "junho", "julho", "agosto", "setembro", "outubro", "novembro", "dezembro"];

export function nomeDoMes(mes: number): string {
  return MESES[((mes % 12) + 12) % 12];
}
