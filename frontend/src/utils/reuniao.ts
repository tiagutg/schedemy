// Como cada reunião é apresentada para quem está vendo: com quem é, de quem é a vez
// de responder e o arquivo de calendário (.ics).

import { paraDataLocal } from "./format";
import type { Agendamento } from "@/types";

export interface Situacao {
  rotulo: string;
  cor: string;
  /** O usuário logado precisa agir (responder ao convite). */
  suaVez: boolean;
}

const AGUARDANDO = ["PENDENTE", "AGUARDANDO_RESPOSTA"];

/** Filtro de status da lista: "Aguardando resposta" junta PENDENTE e AGUARDANDO_RESPOSTA. */
export const FILTROS_STATUS = [
  { valor: AGUARDANDO.join(","), rotulo: "Aguardando resposta" },
  { valor: "CONFIRMADO", rotulo: "Confirmadas" },
  { valor: "CONCLUIDO", rotulo: "Concluídas" },
  { valor: "CANCELADO", rotulo: "Canceladas" },
];

function inicioDaReuniao(a: Agendamento): Date {
  const data = paraDataLocal(a.dataReuniao) ?? new Date(0);
  const [h, m] = a.horarioInicio.split(":").map(Number);
  data.setHours(h, m || 0, 0, 0);
  return data;
}

/**
 * Status em linguagem de quem usa: em vez de "Pendente" e "Aguardando Resposta",
 * diz se falta a resposta do usuário logado ou dos outros convidados.
 */
export function situacaoDaReuniao(a: Agendamento, euId: number | null | undefined, agora = new Date()): Situacao {
  switch (a.status) {
    case "CONFIRMADO":
      return { rotulo: "Confirmada", cor: "bg-sage-100 text-sage-700", suaVez: false };
    case "CONCLUIDO":
      return { rotulo: "Concluída", cor: "bg-marinho-700 text-white", suaVez: false };
    case "CANCELADO":
      return { rotulo: "Cancelada", cor: "bg-brick-100 text-brick-700", suaVez: false };
  }
  if (AGUARDANDO.includes(a.status)) {
    if (inicioDaReuniao(a) <= agora) {
      return { rotulo: "Não confirmada", cor: "bg-ink-100 text-ink-800", suaVez: false };
    }
    const minhaResposta = a.participantes.find((p) => p.idUsuario === euId)?.statusResposta;
    if (minhaResposta === "PENDENTE") {
      return { rotulo: "Aguardando você", cor: "bg-amber-100 text-amber-800 dark:bg-amber-500/15 dark:text-amber-200", suaVez: true };
    }
    return { rotulo: "Aguardando convidados", cor: "bg-brand-100 text-brand-700", suaVez: false };
  }
  return { rotulo: a.status, cor: "bg-ink-100 text-ink-800", suaVez: false };
}

/** "Rômulo Ferreira Douro" → "Rômulo Douro". */
export function nomeCurto(nome: string): string {
  const partes = nome.trim().split(/\s+/);
  return partes.length > 1 ? `${partes[0]} ${partes[partes.length - 1]}` : nome;
}

/**
 * Com quem é a reunião, do ponto de vista do usuário logado: os outros participantes.
 * Quem não participa (recepção) vê "organizador com convidados".
 */
export function descreverEnvolvidos(a: Agendamento, euId: number | null | undefined): string {
  const participo = a.participantes.some((p) => p.idUsuario === euId);
  if (participo) {
    const outros = a.participantes.filter((p) => p.idUsuario !== euId).map((p) => nomeCurto(p.nomeUsuario));
    return outros.length ? `com ${outros.join(", ")}` : "";
  }
  const convidados = a.participantes.filter((p) => p.papel === "CONVIDADO").map((p) => nomeCurto(p.nomeUsuario));
  return convidados.length
    ? `${nomeCurto(a.nomeOrganizador)} com ${convidados.join(", ")}`
    : nomeCurto(a.nomeOrganizador);
}

/** Próximo dia útil a partir de amanhã (yyyy-MM-dd): data padrão de um novo agendamento. */
export function proximoDiaUtilISO(hoje = new Date()): string {
  const d = new Date(hoje.getFullYear(), hoje.getMonth(), hoje.getDate() + 1);
  while (d.getDay() === 0 || d.getDay() === 6) d.setDate(d.getDate() + 1);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

function carimboIcs(data: Date): string {
  const p = (n: number) => String(n).padStart(2, "0");
  return `${data.getFullYear()}${p(data.getMonth() + 1)}${p(data.getDate())}T${p(data.getHours())}${p(data.getMinutes())}00`;
}

function escaparIcs(texto: string): string {
  return texto.replace(/\\/g, "\\\\").replace(/;/g, "\\;").replace(/,/g, "\\,").replace(/\r?\n/g, "\\n");
}

/** Evento de calendário (RFC 5545) aceito por Google Agenda, Outlook e Apple Calendário. */
export function gerarIcs(a: Agendamento, agora = new Date()): string {
  const inicio = inicioDaReuniao(a);
  const fim = new Date(inicio.getTime() + (a.duracaoMinutos ?? 30) * 60_000);
  const descricao = [
    a.resumo,
    `Participantes: ${a.participantes.map((p) => p.nomeUsuario).join(", ")}`,
    a.salaVirtual?.linkTeams && `Sala online: ${a.salaVirtual.linkTeams}`,
  ]
    .filter(Boolean)
    .join("\n");
  const linhas = [
    "BEGIN:VCALENDAR",
    "VERSION:2.0",
    "PRODID:-//Schedemy//Agendamentos//PT-BR",
    "CALSCALE:GREGORIAN",
    "BEGIN:VEVENT",
    `UID:agendamento-${a.id}@schedemy`,
    `DTSTAMP:${carimboIcs(agora)}`,
    // Horário local (sem fuso): o calendário usa o fuso de quem importa.
    `DTSTART:${carimboIcs(inicio)}`,
    `DTEND:${carimboIcs(fim)}`,
    `SUMMARY:${escaparIcs(a.topico)}`,
    `DESCRIPTION:${escaparIcs(descricao)}`,
    a.salaVirtual?.linkTeams ? `URL:${a.salaVirtual.linkTeams}` : "",
    "END:VEVENT",
    "END:VCALENDAR",
  ].filter(Boolean);
  return linhas.join("\r\n") + "\r\n";
}

export function baixarIcs(a: Agendamento) {
  const blob = new Blob([gerarIcs(a)], { type: "text/calendar;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = `reuniao-${a.dataReuniao}-${a.id}.ics`;
  link.click();
  URL.revokeObjectURL(url);
}
