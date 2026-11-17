// Cálculo da agenda de um docente num dia: janelas de atendimento, horários
// ocupados e sugestões de início para uma reunião de duração conhecida.
// Serve só para exibir a agenda do próprio docente: os horários livres para
// todos os envolvidos (RF 14) vêm do back-end (GET /agendamentos/horarios-livres),
// que usa a mesma regra da validação ao salvar.

import { hojeISO, paraDataLocal } from "./format";
import type { Agendamento, Bloqueio, Disponibilidade } from "@/types";

export interface Intervalo {
  inicio: number; // minutos desde 00:00
  fim: number;
  rotulo?: string;
}

/** "08:30" ou "08:30:00" → 510. */
export function paraMinutos(hora: string): number {
  const [h, m] = hora.split(":").map(Number);
  return h * 60 + (m || 0);
}

/** 510 → "08:30". */
export function paraHora(minutos: number): string {
  return `${String(Math.floor(minutos / 60)).padStart(2, "0")}:${String(minutos % 60).padStart(2, "0")}`;
}

/** Dia da semana no padrão do banco: 1 = domingo … 7 = sábado. */
export function diaSemanaDe(dataISO: string): number {
  return (paraDataLocal(dataISO)?.getDay() ?? 0) + 1;
}

function bloqueioAfeta(b: Bloqueio, dataISO: string): boolean {
  if (dataISO < b.dataInicio || dataISO > b.dataFim) return false;
  return !b.eRecorrente || b.diaSemanaRecorrente === diaSemanaDe(dataISO);
}

function sobrepoe(a: Intervalo, b: Intervalo): boolean {
  return a.inicio < b.fim && b.inicio < a.fim;
}

export interface AgendaDoDia {
  janelas: Intervalo[];
  ocupados: Intervalo[];
  horariosLivres: string[];
}

export function calcularAgendaDoDia({
  dataISO,
  duracaoMinutos,
  disponibilidades,
  bloqueios,
  agendamentos,
  ignorarAgendamentoId,
  passoMinutos = 15,
}: {
  dataISO: string;
  duracaoMinutos: number;
  disponibilidades: Disponibilidade[];
  bloqueios: Bloqueio[];
  agendamentos: Agendamento[];
  ignorarAgendamentoId?: number | null;
  passoMinutos?: number;
}): AgendaDoDia {
  const dia = diaSemanaDe(dataISO);
  const janelas = disponibilidades
    .filter((d) => d.ativo && d.diaSemana === dia)
    .map((d) => ({ inicio: paraMinutos(d.horarioInicio), fim: paraMinutos(d.horarioFim) }))
    .sort((a, b) => a.inicio - b.inicio);

  const ocupados: Intervalo[] = [
    ...agendamentos
      .filter((a) => a.dataReuniao === dataISO && a.status !== "CANCELADO" && a.id !== ignorarAgendamentoId)
      .map((a) => ({
        inicio: paraMinutos(a.horarioInicio),
        fim: a.horarioFim ? paraMinutos(a.horarioFim) : paraMinutos(a.horarioInicio) + (a.duracaoMinutos ?? 0),
        rotulo: "Reunião",
      })),
    ...bloqueios
      .filter((b) => bloqueioAfeta(b, dataISO))
      .map((b) => ({
        inicio: b.horarioInicio ? paraMinutos(b.horarioInicio) : 0,
        fim: b.horarioFim ? paraMinutos(b.horarioFim) : 24 * 60,
        rotulo: b.motivo ? `Bloqueio: ${b.motivo}` : "Bloqueio",
      })),
  ].sort((a, b) => a.inicio - b.inicio);

  // Hoje, só sugere horários a partir de agora.
  const agora = new Date();
  const minimo = dataISO === hojeISO() ? agora.getHours() * 60 + agora.getMinutes() : 0;

  const horariosLivres: string[] = [];
  if (duracaoMinutos > 0) {
    for (const janela of janelas) {
      const primeiro = Math.ceil(Math.max(janela.inicio, minimo) / passoMinutos) * passoMinutos;
      for (let t = primeiro; t + duracaoMinutos <= janela.fim; t += passoMinutos) {
        const slot = { inicio: t, fim: t + duracaoMinutos };
        if (!ocupados.some((o) => sobrepoe(slot, o))) horariosLivres.push(paraHora(t));
      }
    }
  }

  return { janelas, ocupados, horariosLivres: [...new Set(horariosLivres)] };
}

/** O horário escolhido cabe numa janela de atendimento e não colide com nada? */
export function horarioCabe(agenda: AgendaDoDia, horario: string, duracaoMinutos: number) {
  const slot = { inicio: paraMinutos(horario), fim: paraMinutos(horario) + duracaoMinutos };
  const dentroDaJanela = agenda.janelas.some((j) => slot.inicio >= j.inicio && slot.fim <= j.fim);
  const conflito = agenda.ocupados.find((o) => sobrepoe(slot, o));
  return { dentroDaJanela, conflito };
}

function fimDe(a: Agendamento): number {
  return a.horarioFim ? paraMinutos(a.horarioFim) : paraMinutos(a.horarioInicio) + (a.duracaoMinutos ?? 0);
}

/**
 * Reunião ativa que choca com o horário proposto (RN 01), ou undefined.
 * Na edição, a própria reunião é ignorada.
 */
export function encontrarConflito(
  agendamentos: Agendamento[],
  {
    dataISO,
    horario,
    duracaoMinutos,
    ignorarAgendamentoId,
  }: { dataISO: string; horario: string; duracaoMinutos: number; ignorarAgendamentoId?: number | null }
): Agendamento | undefined {
  const inicio = paraMinutos(horario);
  const fim = inicio + duracaoMinutos;
  return agendamentos.find(
    (a) =>
      a.dataReuniao === dataISO &&
      a.status !== "CANCELADO" &&
      a.id !== ignorarAgendamentoId &&
      paraMinutos(a.horarioInicio) < fim &&
      inicio < fimDe(a)
  );
}

// RN 13: no máximo 2 agendamentos criados por aluno a cada 24 horas.
export const LIMITE_AGENDAMENTOS_ALUNO_24H = 2;
const DIA_MS = 24 * 3_600_000;

/**
 * Quantos agendamentos o aluno organizou nas últimas 24 horas (sem os
 * cancelados, como o back-end conta) e, se o limite foi atingido, a partir
 * de quando poderá criar outro.
 */
export function situacaoLimiteAluno(
  agendamentos: Agendamento[],
  idAluno: number,
  agoraMs: number,
  limite = LIMITE_AGENDAMENTOS_ALUNO_24H
): { recentes: number; atingido: boolean; liberaEm: Date | null } {
  const criados = agendamentos
    .filter((a) => a.idOrganizador === idAluno && a.status !== "CANCELADO" && !!a.criadoEm)
    .map((a) => new Date(a.criadoEm as string).getTime())
    .filter((t) => t >= agoraMs - DIA_MS)
    .sort((a, b) => a - b);
  if (criados.length < limite) return { recentes: criados.length, atingido: false, liberaEm: null };
  // Libera quando sobrarem menos que `limite` criações dentro da janela de 24 h.
  return { recentes: criados.length, atingido: true, liberaEm: new Date(criados[criados.length - limite] + DIA_MS) };
}

const DIAS_CURTOS = ["Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb"];

/** Rótulo curto de uma data: "Ter, 03/11". */
export function rotuloDia(dataISO: string): string {
  const [, mes, dia] = dataISO.split("-");
  return `${DIAS_CURTOS[diaSemanaDe(dataISO) - 1]}, ${dia}/${mes}`;
}

/** Horário de atendimento da semana em uma linha: "Seg 08:00–12:00 · Qua 08:00–12:00". */
export function resumirSemana(disponibilidades: Disponibilidade[]): string {
  return disponibilidades
    .filter((d) => d.ativo)
    .sort((a, b) => a.diaSemana - b.diaSemana || paraMinutos(a.horarioInicio) - paraMinutos(b.horarioInicio))
    .map((d) => `${DIAS_CURTOS[d.diaSemana - 1]} ${d.horarioInicio.slice(0, 5)}–${d.horarioFim.slice(0, 5)}`)
    .join(" · ");
}

/**
 * Datas (yyyy-MM-dd) de hoje até `maxDias` em que todos os docentes atendem ao mesmo tempo
 * por pelo menos `duracaoMinutos`, pela grade semanal. São só candidatas: reuniões e
 * bloqueios quem confere é o back-end.
 */
export function diasDeAtendimentoEmComum(
  grades: Disponibilidade[][],
  hoje = new Date(),
  maxDias = 30,
  duracaoMinutos = 1
): string[] {
  if (grades.length === 0) return [];
  const servem = new Set<number>();
  for (let dia = 1; dia <= 7; dia++) {
    if (comunsNoDia(grades, dia).some((i) => i.fim - i.inicio >= duracaoMinutos)) servem.add(dia);
  }
  const dias: string[] = [];
  for (let i = 0; i <= maxDias; i++) {
    const d = new Date(hoje.getFullYear(), hoje.getMonth(), hoje.getDate() + i);
    if (servem.has(d.getDay() + 1)) {
      dias.push(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`);
    }
  }
  return dias;
}

/** Interseção de dois conjuntos de intervalos (em minutos). */
function cruzar(a: Intervalo[], b: Intervalo[]): Intervalo[] {
  const comuns: Intervalo[] = [];
  for (const x of a) {
    for (const y of b) {
      const inicio = Math.max(x.inicio, y.inicio);
      const fim = Math.min(x.fim, y.fim);
      if (fim > inicio) comuns.push({ inicio, fim });
    }
  }
  return comuns;
}

/** Pela grade semanal, existe algum horário em que todos atendem por pelo menos `duracaoMinutos`? */
export function temHorarioEmComumNaSemana(grades: Disponibilidade[][], duracaoMinutos: number): boolean {
  if (grades.length === 0) return false;
  for (let dia = 1; dia <= 7; dia++) {
    if (comunsNoDia(grades, dia).some((i) => i.fim - i.inicio >= duracaoMinutos)) return true;
  }
  return false;
}

/** Intervalos de um dia da semana em que todas as grades atendem. */
function comunsNoDia(grades: Disponibilidade[][], dia: number): Intervalo[] {
  return grades
    .map((grade) =>
      grade
        .filter((d) => d.ativo && d.diaSemana === dia)
        .map((d) => ({ inicio: paraMinutos(d.horarioInicio), fim: paraMinutos(d.horarioFim) }))
    )
    .reduce((acc, atual) => cruzar(acc, atual));
}

/** Posições das grades que, se saírem, deixam os demais com horário em comum na semana. */
export function quemImpedeHorarioEmComum(grades: Disponibilidade[][], duracaoMinutos: number): number[] {
  if (grades.length < 2 || temHorarioEmComumNaSemana(grades, duracaoMinutos)) return [];
  return grades
    .map((_, i) => i)
    .filter((i) => temHorarioEmComumNaSemana(grades.filter((_, j) => j !== i), duracaoMinutos));
}
