import { expect, request, type Page } from "@playwright/test";

export const API = "http://localhost:8080";

/** Contas do db/dump_schedemy.sql. */
export const CONTAS = {
  admin: { email: "admin@hotmail.com", senha: "admin123" },
  aluno: { email: "joao.aluno@unisales.edu.br", senha: "Senha123@", nome: "João Victor Wuthovsky Klein" },
  outroAluno: { email: "pedro.aluno@unisales.edu.br", senha: "Senha123@" },
  professor: { email: "lucas.professor@unisales.edu.br", senha: "Senha123@", nome: "Lucas Daniel Barboza" },
  recepcao: { email: "mariana.recepcao@unisales.edu.br", senha: "Senha123@" },
} as const;

type Conta = { email: string; senha: string };

export async function entrar(page: Page, conta: Conta) {
  await page.goto("/login");
  await page.evaluate(() => localStorage.clear());
  await page.goto("/login");
  await page.getByLabel("E-mail institucional").fill(conta.email);
  await page.locator("#senha").fill(conta.senha);
  await page.getByRole("button", { name: "Entrar", exact: true }).click();
  await expect(page).toHaveURL("/");
}

/** Cliente da API autenticado como a conta informada (para preparar dados). */
export async function api(conta: Conta) {
  return request.newContext({
    baseURL: API,
    extraHTTPHeaders: {
      Authorization: "Basic " + Buffer.from(`${conta.email}:${conta.senha}`).toString("base64"),
    },
  });
}

export function paraISO(d: Date): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

/** Próximo dia útil a pelo menos `dias` dias de hoje (edição exige 24 h de antecedência). */
export function proximoDiaUtil(dias = 2): Date {
  const d = new Date();
  d.setDate(d.getDate() + dias);
  while (d.getDay() === 0 || d.getDay() === 6) d.setDate(d.getDate() + 1);
  return d;
}

/** Dia da semana no padrão do banco: 1 = domingo … 7 = sábado. */
export function diaSemanaBanco(d: Date): number {
  return d.getDay() + 1;
}

/** Garante que o professor de teste atenda das 08:00 às 18:00 no dia informado. */
export async function garantirDisponibilidade(dia: Date) {
  const professor = await api(CONTAS.professor);
  const usuarios = await (await professor.get("/api/v1/usuarios", { params: { size: 200 } })).json();
  const eu = usuarios.conteudo.find((u: { email: string }) => u.email === CONTAS.professor.email);
  const lista = await (
    await professor.get("/api/v1/disponibilidades", {
      params: { usuarioId: eu.id, diaSemana: diaSemanaBanco(dia), ativo: true, size: 50 },
    })
  ).json();
  const cobre = lista.conteudo.some(
    (d: { horarioInicio: string; horarioFim: string }) => d.horarioInicio <= "08:00:00" && d.horarioFim >= "18:00:00"
  );
  if (!cobre) {
    const resposta = await professor.post("/api/v1/disponibilidades", {
      data: { idUsuario: eu.id, diaSemana: diaSemanaBanco(dia), horarioInicio: "08:00:00", horarioFim: "18:00:00", ativo: true },
    });
    expect(resposta.ok(), await resposta.text()).toBeTruthy();
  }
  await professor.dispose();
}

/**
 * Remove pela API os agendamentos de teste: cancela os ativos e apaga os
 * cancelados (a administração pode remover agendamentos cancelados), para não
 * sujar as listas nem contar no limite de 2 agendamentos por aluno (RN 13).
 */
export async function removerAgendamentosComTopico(prefixo: string) {
  const admin = await api(CONTAS.admin);
  const lista = await (await admin.get("/api/v1/agendamentos", { params: { size: 500 } })).json();
  const usuarios = await (await admin.get("/api/v1/usuarios", { params: { size: 200 } })).json();
  const idAdmin = usuarios.conteudo.find((u: { email: string }) => u.email === CONTAS.admin.email).id;
  // Mais recentes primeiro: uma remarcação aponta para a reunião original.
  const ordenados = [...lista.conteudo].sort((x: { id: number }, y: { id: number }) => y.id - x.id);
  for (const a of ordenados) {
    if (!a.topico.startsWith(prefixo) || a.status === "CONCLUIDO") continue;
    if (a.status !== "CANCELADO") {
      await admin.patch(`/api/v1/agendamentos/${a.id}/cancelamento`, {
        data: { idUsuario: idAdmin, motivo: "Limpeza dos testes automatizados", confirmar: true },
      });
    }
    const resposta = await admin.delete(`/api/v1/agendamentos/${a.id}`);
    expect(resposta.ok(), `remover #${a.id}: ${await resposta.text()}`).toBeTruthy();
  }
  await admin.dispose();
}
