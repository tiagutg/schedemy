import { describe, expect, it } from "vitest";
import { lerSessao } from "./sessao";

const AGORA = Date.parse("2026-11-12T12:00:00Z");
const valida = { email: "a@unisales.edu.br", token: "x.y.z", expiraEm: "2026-11-12T20:00:00Z", usuarioId: 3 };

describe("lerSessao", () => {
  it("aceita a sessão com token dentro da validade", () => {
    expect(lerSessao(JSON.stringify(valida), AGORA)).toEqual(valida);
  });

  it("descarta o formato antigo, que guardava a senha", () => {
    const antiga = { username: "a@unisales.edu.br", password: "Senha123@", usuarioId: 3 };
    expect(lerSessao(JSON.stringify(antiga), AGORA)).toBeNull();
  });

  it("descarta token vencido", () => {
    expect(lerSessao(JSON.stringify({ ...valida, expiraEm: "2026-11-12T11:59:59Z" }), AGORA)).toBeNull();
  });

  it("descarta conteúdo vazio, corrompido ou com data inválida", () => {
    expect(lerSessao(null, AGORA)).toBeNull();
    expect(lerSessao("{", AGORA)).toBeNull();
    expect(lerSessao("null", AGORA)).toBeNull();
    expect(lerSessao(JSON.stringify({ ...valida, expiraEm: "amanhã" }), AGORA)).toBeNull();
  });

  it("usuarioId ausente vira null", () => {
    const { usuarioId: _, ...semId } = valida;
    expect(lerSessao(JSON.stringify(semId), AGORA)?.usuarioId).toBeNull();
  });
});
