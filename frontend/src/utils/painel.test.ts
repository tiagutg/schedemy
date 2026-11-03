import { describe, expect, it } from "vitest";
import {
  intervaloDoPainel,
  intervaloDoPeriodo,
  proximasReunioes,
  resumoDoMes,
  serieDiaria,
  variacaoPercentual,
} from "./painel";
import type { Agendamento } from "@/types";

const HOJE = new Date(2026, 9, 30); // 30/10/2026

function reuniao(id: number, dataReuniao: string, status: string, horarioInicio = "08:00:00"): Agendamento {
  return {
    id,
    topico: `Reunião ${id}`,
    dataReuniao,
    horarioInicio,
    formato: "PRESENCIAL",
    status,
    idOrganizador: 1,
    nomeOrganizador: "Kevin",
    participantes: [],
  };
}

describe("intervalos", () => {
  it("cada período do gráfico", () => {
    expect(intervaloDoPeriodo("prox30", HOJE)).toEqual({ inicio: "2026-10-30", fim: "2026-11-29" });
    expect(intervaloDoPeriodo("30d", HOJE)).toEqual({ inicio: "2026-09-30", fim: "2026-10-30" });
    expect(intervaloDoPeriodo("90d", HOJE)).toEqual({ inicio: "2026-08-01", fim: "2026-10-30" });
  });

  it("o painel busca do mais antigo necessário até 30 dias à frente", () => {
    expect(intervaloDoPainel(HOJE)).toEqual({ inicio: "2026-08-01", fim: "2026-11-29" });
    // Em janeiro, o mês anterior é dezembro do ano anterior.
    expect(intervaloDoPainel(new Date(2027, 0, 10)).inicio).toBe("2026-10-12");
  });
});

describe("serieDiaria", () => {
  it("preenche os dias sem reunião e separa por situação", () => {
    const serie = serieDiaria(
      [
        reuniao(1, "2026-11-02", "CONFIRMADO"),
        reuniao(2, "2026-11-02", "CONCLUIDO"),
        reuniao(3, "2026-11-02", "PENDENTE"),
        reuniao(4, "2026-11-03", "CANCELADO"),
        reuniao(5, "2026-12-01", "CONFIRMADO"), // fora do intervalo
      ],
      "2026-11-01",
      "2026-11-03"
    );
    expect(serie).toEqual([
      { data: "2026-11-01", confirmadas: 0, aguardando: 0, canceladas: 0 },
      { data: "2026-11-02", confirmadas: 2, aguardando: 1, canceladas: 0 },
      { data: "2026-11-03", confirmadas: 0, aguardando: 0, canceladas: 1 },
    ]);
  });
});

describe("resumoDoMes", () => {
  it("compara o mês atual com o anterior", () => {
    const resumo = resumoDoMes(
      [
        reuniao(1, "2026-10-05", "CONCLUIDO"),
        reuniao(2, "2026-10-20", "CANCELADO"),
        reuniao(3, "2026-10-31", "PENDENTE"),
        reuniao(4, "2026-11-04", "AGUARDANDO_RESPOSTA"),
        reuniao(5, "2026-09-10", "CONCLUIDO"),
        reuniao(6, "2026-10-29", "PENDENTE"), // passou sem resposta: não conta como aguardando
      ],
      HOJE
    );
    expect(resumo.atual).toEqual({ total: 3, concluidas: 1, canceladas: 1, taxaCancelamento: 25 });
    expect(resumo.anterior).toEqual({ total: 1, concluidas: 1, canceladas: 0, taxaCancelamento: 0 });
    expect(resumo.aguardando).toBe(2);
  });
});

describe("variacaoPercentual", () => {
  it("calcula a variação e evita dividir por zero", () => {
    expect(variacaoPercentual(12, 10)).toBe(20);
    expect(variacaoPercentual(5, 10)).toBe(-50);
    expect(variacaoPercentual(3, 0)).toBeNull();
  });
});

describe("proximasReunioes", () => {
  it("só as ativas de hoje em diante, por data e hora", () => {
    const lista = proximasReunioes(
      [
        reuniao(1, "2026-11-02", "CONFIRMADO", "14:00:00"),
        reuniao(2, "2026-11-02", "PENDENTE", "08:00:00"),
        reuniao(3, "2026-10-29", "CONFIRMADO"),
        reuniao(4, "2026-11-01", "CANCELADO"),
      ],
      5,
      HOJE
    );
    expect(lista.map((a) => a.id)).toEqual([2, 1]);
  });
});
