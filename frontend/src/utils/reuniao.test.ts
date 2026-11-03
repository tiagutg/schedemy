import { describe, expect, it } from "vitest";
import { descreverEnvolvidos, gerarIcs, nomeCurto, proximoDiaUtilISO, situacaoDaReuniao } from "./reuniao";
import type { Agendamento } from "@/types";

const ANTES = new Date(2026, 10, 1, 9, 0); // 01/11/2026 09:00, antes da reunião

function reuniao(parcial: Partial<Agendamento> = {}): Agendamento {
  return {
    id: 7,
    topico: "Orientação do TCC",
    dataReuniao: "2026-11-03",
    horarioInicio: "08:30:00",
    formato: "PRESENCIAL",
    status: "AGUARDANDO_RESPOSTA",
    idOrganizador: 1,
    nomeOrganizador: "Kevin Lins Carminati Andrade",
    duracaoMinutos: 30,
    participantes: [
      { idUsuario: 1, nomeUsuario: "Kevin Lins Carminati Andrade", papel: "ORGANIZADOR", statusResposta: "ACEITO" },
      { idUsuario: 6, nomeUsuario: "Rômulo Ferreira Douro", papel: "CONVIDADO", statusResposta: "PENDENTE" },
    ],
    ...parcial,
  };
}

describe("situacaoDaReuniao", () => {
  it("diz ao convidado que falta a resposta dele", () => {
    expect(situacaoDaReuniao(reuniao(), 6, ANTES)).toMatchObject({ rotulo: "Aguardando você", suaVez: true });
  });

  it("diz ao organizador que falta a resposta dos convidados", () => {
    expect(situacaoDaReuniao(reuniao(), 1, ANTES)).toMatchObject({ rotulo: "Aguardando convidados", suaVez: false });
    expect(situacaoDaReuniao(reuniao({ status: "PENDENTE" }), 1, ANTES).rotulo).toBe("Aguardando convidados");
  });

  it("reunião sem resposta cujo horário já passou não pede mais ação", () => {
    const depois = new Date(2026, 10, 3, 9, 0);
    expect(situacaoDaReuniao(reuniao(), 6, depois)).toMatchObject({ rotulo: "Não confirmada", suaVez: false });
  });

  it("status finais", () => {
    expect(situacaoDaReuniao(reuniao({ status: "CONFIRMADO" }), 6, ANTES).rotulo).toBe("Confirmada");
    expect(situacaoDaReuniao(reuniao({ status: "CONCLUIDO" }), 6, ANTES).rotulo).toBe("Concluída");
    expect(situacaoDaReuniao(reuniao({ status: "CANCELADO" }), 6, ANTES).rotulo).toBe("Cancelada");
  });
});

describe("descreverEnvolvidos", () => {
  it("mostra a quem participa com quem é a reunião", () => {
    expect(descreverEnvolvidos(reuniao(), 1)).toBe("com Rômulo Douro");
    expect(descreverEnvolvidos(reuniao(), 6)).toBe("com Kevin Andrade");
  });

  it("mostra à recepção o organizador e os convidados", () => {
    expect(descreverEnvolvidos(reuniao(), 12)).toBe("Kevin Andrade com Rômulo Douro");
  });
});

describe("nomeCurto", () => {
  it("usa o primeiro e o último nome", () => {
    expect(nomeCurto("Ana Beatriz do Ferrero Rocha")).toBe("Ana Rocha");
    expect(nomeCurto("Mariana")).toBe("Mariana");
  });
});

describe("proximoDiaUtilISO", () => {
  it("pula o fim de semana", () => {
    expect(proximoDiaUtilISO(new Date(2026, 9, 30))).toBe("2026-11-02"); // sexta → segunda
    expect(proximoDiaUtilISO(new Date(2026, 9, 27))).toBe("2026-10-28"); // terça → quarta
  });
});

describe("gerarIcs", () => {
  it("gera o evento com início, fim e link da sala", () => {
    const ics = gerarIcs(
      reuniao({ resumo: "Capítulo 2, revisão", salaVirtual: { linkTeams: "https://meet.google.com/abc" } }),
      ANTES
    );
    expect(ics).toContain("DTSTART:20261103T083000");
    expect(ics).toContain("DTEND:20261103T090000");
    expect(ics).toContain("SUMMARY:Orientação do TCC");
    expect(ics).toContain("Capítulo 2\\, revisão");
    expect(ics).toContain("URL:https://meet.google.com/abc");
    expect(ics.split("\r\n")[0]).toBe("BEGIN:VCALENDAR");
  });
});
