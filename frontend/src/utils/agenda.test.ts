import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  calcularAgendaDoDia,
  diaSemanaDe,
  encontrarConflito,
  horarioCabe,
  paraHora,
  paraMinutos,
  situacaoLimiteAluno,
} from "./agenda";
import type { Agendamento, Bloqueio, Disponibilidade } from "@/types";

// 2026-10-19 é uma segunda-feira (dia 2 no padrão do banco: 1 = domingo).
const SEGUNDA = "2026-10-19";

function disponibilidade(inicio: string, fim: string, diaSemana = 2, ativo = true): Disponibilidade {
  return { id: 1, idUsuario: 10, nomeUsuario: "Prof", diaSemana, horarioInicio: `${inicio}:00`, horarioFim: `${fim}:00`, ativo };
}

function reuniao(parcial: Partial<Agendamento> & { horarioInicio: string }): Agendamento {
  return {
    id: 100,
    topico: "Reunião",
    dataReuniao: SEGUNDA,
    formato: "PRESENCIAL",
    status: "CONFIRMADO",
    idOrganizador: 1,
    nomeOrganizador: "Aluno",
    participantes: [],
    ...parcial,
  };
}

function bloqueio(parcial: Partial<Bloqueio> = {}): Bloqueio {
  return {
    id: 1,
    idUsuario: 10,
    nomeUsuario: "Prof",
    dataInicio: SEGUNDA,
    dataFim: SEGUNDA,
    eRecorrente: false,
    ...parcial,
  };
}

describe("conversões de hora", () => {
  it("converte entre texto e minutos", () => {
    expect(paraMinutos("08:30")).toBe(510);
    expect(paraMinutos("08:30:00")).toBe(510);
    expect(paraHora(510)).toBe("08:30");
    expect(paraHora(0)).toBe("00:00");
  });

  it("usa o padrão do banco para o dia da semana (1 = domingo)", () => {
    expect(diaSemanaDe("2026-10-18")).toBe(1); // domingo
    expect(diaSemanaDe(SEGUNDA)).toBe(2);
    expect(diaSemanaDe("2026-10-24")).toBe(7); // sábado
  });
});

describe("calcularAgendaDoDia", () => {
  beforeEach(() => {
    // "Hoje" fixo antes da data das reuniões, para não cortar horários passados.
    vi.useFakeTimers();
    vi.setSystemTime(new Date(2026, 9, 16, 10, 0));
  });
  afterEach(() => vi.useRealTimers());

  const base = { dataISO: SEGUNDA, bloqueios: [], agendamentos: [] };

  it("sugere horários dentro da disponibilidade do dia, no passo pedido", () => {
    const agenda = calcularAgendaDoDia({
      ...base,
      duracaoMinutos: 30,
      passoMinutos: 30,
      disponibilidades: [disponibilidade("08:00", "10:00")],
    });
    expect(agenda.janelas).toEqual([{ inicio: 480, fim: 600 }]);
    // A última sugestão precisa terminar até o fim da janela.
    expect(agenda.horariosLivres).toEqual(["08:00", "08:30", "09:00", "09:30"]);
  });

  it("ignora disponibilidades de outro dia ou inativas", () => {
    const agenda = calcularAgendaDoDia({
      ...base,
      duracaoMinutos: 30,
      disponibilidades: [disponibilidade("08:00", "12:00", 3), disponibilidade("08:00", "12:00", 2, false)],
    });
    expect(agenda.janelas).toEqual([]);
    expect(agenda.horariosLivres).toEqual([]);
  });

  it("não sugere horários que choquem com reuniões ativas", () => {
    const agenda = calcularAgendaDoDia({
      ...base,
      duracaoMinutos: 30,
      passoMinutos: 30,
      disponibilidades: [disponibilidade("08:00", "10:00")],
      agendamentos: [
        reuniao({ id: 1, horarioInicio: "08:30:00", horarioFim: "09:00:00" }),
        // Cancelada não ocupa.
        reuniao({ id: 2, horarioInicio: "09:00:00", horarioFim: "09:30:00", status: "CANCELADO" }),
      ],
    });
    expect(agenda.ocupados).toHaveLength(1);
    expect(agenda.horariosLivres).toEqual(["08:00", "09:00", "09:30"]);
  });

  it("na edição, a própria reunião não conta como ocupação", () => {
    const agenda = calcularAgendaDoDia({
      ...base,
      duracaoMinutos: 30,
      passoMinutos: 30,
      disponibilidades: [disponibilidade("08:00", "09:00")],
      agendamentos: [reuniao({ id: 7, horarioInicio: "08:00:00", horarioFim: "08:30:00" })],
      ignorarAgendamentoId: 7,
    });
    expect(agenda.horariosLivres).toEqual(["08:00", "08:30"]);
  });

  it("bloqueio sem horário ocupa o dia inteiro", () => {
    const agenda = calcularAgendaDoDia({
      ...base,
      duracaoMinutos: 30,
      disponibilidades: [disponibilidade("08:00", "12:00")],
      bloqueios: [bloqueio()],
    });
    expect(agenda.horariosLivres).toEqual([]);
  });

  it("bloqueio recorrente só vale no dia da semana indicado", () => {
    const disponibilidades = [disponibilidade("08:00", "09:00")];
    const recorrenteNaTerca = bloqueio({ dataFim: "2026-12-31", eRecorrente: true, diaSemanaRecorrente: 3 });
    const recorrenteNaSegunda = { ...recorrenteNaTerca, diaSemanaRecorrente: 2 };
    expect(
      calcularAgendaDoDia({ ...base, duracaoMinutos: 60, disponibilidades, bloqueios: [recorrenteNaTerca] }).horariosLivres
    ).toEqual(["08:00"]);
    expect(
      calcularAgendaDoDia({ ...base, duracaoMinutos: 60, disponibilidades, bloqueios: [recorrenteNaSegunda] }).horariosLivres
    ).toEqual([]);
  });

  it("no dia de hoje, só sugere horários a partir de agora", () => {
    vi.setSystemTime(new Date(2026, 9, 19, 9, 10)); // segunda, 09:10
    const agenda = calcularAgendaDoDia({
      ...base,
      duracaoMinutos: 30,
      passoMinutos: 30,
      disponibilidades: [disponibilidade("08:00", "11:00")],
    });
    expect(agenda.horariosLivres).toEqual(["09:30", "10:00", "10:30"]);
  });
});

describe("horarioCabe", () => {
  const agenda = { janelas: [{ inicio: 480, fim: 600 }], ocupados: [{ inicio: 540, fim: 570 }], horariosLivres: [] };

  it("aceita um horário dentro da janela e sem conflito", () => {
    expect(horarioCabe(agenda, "08:00", 30)).toEqual({ dentroDaJanela: true, conflito: undefined });
  });

  it("acusa horário que passa do fim do atendimento", () => {
    expect(horarioCabe(agenda, "09:45", 30).dentroDaJanela).toBe(false);
  });

  it("acusa conflito com horário ocupado", () => {
    expect(horarioCabe(agenda, "08:45", 30).conflito).toEqual({ inicio: 540, fim: 570 });
  });
});

describe("encontrarConflito", () => {
  const existentes = [reuniao({ id: 1, horarioInicio: "09:00:00", horarioFim: "09:30:00" })];
  const proposta = { dataISO: SEGUNDA, duracaoMinutos: 30 };

  it("encontra reunião que se sobrepõe", () => {
    expect(encontrarConflito(existentes, { ...proposta, horario: "09:15" })?.id).toBe(1);
  });

  it("horários encostados não são conflito", () => {
    expect(encontrarConflito(existentes, { ...proposta, horario: "08:30" })).toBeUndefined();
    expect(encontrarConflito(existentes, { ...proposta, horario: "09:30" })).toBeUndefined();
  });

  it("ignora outro dia, reunião cancelada e a própria reunião na edição", () => {
    expect(encontrarConflito(existentes, { ...proposta, dataISO: "2026-10-20", horario: "09:00" })).toBeUndefined();
    expect(
      encontrarConflito([{ ...existentes[0], status: "CANCELADO" }], { ...proposta, horario: "09:00" })
    ).toBeUndefined();
    expect(encontrarConflito(existentes, { ...proposta, horario: "09:00", ignorarAgendamentoId: 1 })).toBeUndefined();
  });

  it("usa a duração quando a reunião não traz horário de fim", () => {
    const semFim = [reuniao({ id: 2, horarioInicio: "10:00:00", horarioFim: undefined, duracaoMinutos: 60 })];
    expect(encontrarConflito(semFim, { ...proposta, horario: "10:45" })?.id).toBe(2);
  });
});

describe("situacaoLimiteAluno", () => {
  const agora = new Date(2026, 9, 16, 12, 0).getTime();
  const horasAtras = (h: number) => new Date(agora - h * 3_600_000).toISOString();
  const criado = (id: number, h: number, parcial: Partial<Agendamento> = {}) =>
    reuniao({ id, horarioInicio: "08:00:00", idOrganizador: 1, criadoEm: horasAtras(h), ...parcial });

  it("abaixo do limite, libera", () => {
    expect(situacaoLimiteAluno([criado(1, 2)], 1, agora)).toEqual({ recentes: 1, atingido: false, liberaEm: null });
  });

  it("no limite, informa quando o mais antigo sai da janela de 24 h", () => {
    const situacao = situacaoLimiteAluno([criado(1, 2), criado(2, 5)], 1, agora);
    expect(situacao.atingido).toBe(true);
    expect(situacao.recentes).toBe(2);
    expect(situacao.liberaEm?.getTime()).toBe(agora + 19 * 3_600_000); // criado há 5 h → libera em 19 h
  });

  it("não conta cancelados, criados há mais de 24 h nem reuniões de outro organizador", () => {
    const lista = [
      criado(1, 2),
      criado(2, 3, { status: "CANCELADO" }),
      criado(3, 30),
      criado(4, 1, { idOrganizador: 99 }),
    ];
    expect(situacaoLimiteAluno(lista, 1, agora)).toEqual({ recentes: 1, atingido: false, liberaEm: null });
  });

  it("acima do limite, libera quando sobram menos que o limite na janela", () => {
    const situacao = situacaoLimiteAluno([criado(1, 1), criado(2, 4), criado(3, 10)], 1, agora);
    // É preciso que os dois mais antigos (10 h e 4 h) saiam; o de 4 h sai em 20 h.
    expect(situacao.liberaEm?.getTime()).toBe(agora + 20 * 3_600_000);
  });
});
