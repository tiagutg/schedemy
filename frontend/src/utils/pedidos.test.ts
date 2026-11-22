import { describe, expect, it } from "vitest";
import { desfechoDoPedido, prazoDeResposta } from "./pedidos";

describe("prazoDeResposta", () => {
  it("termina 24 h antes do horário atual da reunião", () => {
    expect(prazoDeResposta({ dataAtual: "2026-12-01", horarioInicioAtual: "08:30:00" })).toBe("30/11/2026 às 08:30");
  });

  it("vira o mês e o ano", () => {
    expect(prazoDeResposta({ dataAtual: "2027-01-01", horarioInicioAtual: "14:00:00" })).toBe("31/12/2026 às 14:00");
  });
});

describe("desfechoDoPedido", () => {
  const base = { idRespondidoPor: 6, nomeRespondidoPor: "Rômulo Ferreira Douro" };

  it("diz quem aceitou ou recusou", () => {
    expect(desfechoDoPedido({ ...base, status: "ACEITA" }, 1)).toBe("Aceito por Rômulo Ferreira Douro");
    expect(desfechoDoPedido({ ...base, status: "RECUSADA" }, 1)).toBe("Recusado por Rômulo Ferreira Douro");
  });

  it("usa 'você' para quem está logado", () => {
    expect(desfechoDoPedido({ ...base, status: "RECUSADA" }, 6)).toBe("Recusado por você");
  });

  it("pedido antigo, sem quem respondeu, fica só com o status", () => {
    expect(desfechoDoPedido({ status: "ACEITA", idRespondidoPor: null, nomeRespondidoPor: null }, 1)).toBe("Aceito");
  });

  it("expirado e cancelado não têm quem decidiu", () => {
    const sem = { idRespondidoPor: null, nomeRespondidoPor: null };
    expect(desfechoDoPedido({ ...sem, status: "EXPIRADA" })).toBe("Expirou sem resposta");
    expect(desfechoDoPedido({ ...sem, status: "CANCELADA" })).toMatch(/^Cancelado/);
  });
});
