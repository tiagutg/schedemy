// Cálculo da agenda de um docente num dia: janelas de atendimento, horários
// ocupados e sugestões de início para uma reunião de duração conhecida.
// Espelha as regras que o back-end valida ao criar/editar um agendamento:
// o horário precisa caber numa disponibilidade ativa do dia (RN 06) e não
// pode sobrepor outra reunião (RN 01). Bloqueios entram como ocupação.

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
