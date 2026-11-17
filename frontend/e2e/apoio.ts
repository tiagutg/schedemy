import { expect, request, type Page } from "@playwright/test";

export const API = "http://localhost:8080";

/** Contas do db/dump_schedemy.sql. */
export const CONTAS = {
  admin: { email: "admin@hotmail.com", senha: "admin123" },
  aluno: { email: "joao.aluno@unisales.edu.br", senha: "Senha123@", nome: "João Victor Wuthovsky Klein" },
  outroAluno: { email: "pedro.aluno@unisales.edu.br", senha: "Senha123@" },
  professor: { email: "lucas.professor@unisales.edu.br", senha: "Senha123@", nome: "Lucas Daniel Barboza" },
  recepcao: { email: "mariana.recepcao@unisales.edu.br", senha: "Senha123@" },
  /** Docente que os testes deixam sem horário de atendimento (ver semHorariosDeAtendimento). */
  semHorario: { email: "ebert.professor@unisales.edu.br", senha: "Senha123@", nome: "Éberte Valter da Silva Freitas" },
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

/** Token de acesso da conta (POST /api/v1/auth/login). */
export async function tokenDaConta(conta: Conta): Promise<string> {
  const anonimo = await request.newContext({ baseURL: API });
  const resposta = await anonimo.post("/api/v1/auth/login", { data: { email: conta.email, senha: conta.senha } });
  expect(resposta.ok(), await resposta.text()).toBeTruthy();
  const { token } = await resposta.json();
  await anonimo.dispose();
  return token;
}

/** Cliente da API autenticado como a conta informada (para preparar dados). */
export async function api(conta: Conta) {
  return request.newContext({
    baseURL: API,
    extraHTTPHeaders: { Authorization: `Bearer ${await tokenDaConta(conta)}` },
  });
}

/**
 * Id do usuário da conta, pelo próprio cadastro (`GET /usuarios/me`). A listagem completa
 * de usuários é só de admin/recepção e o resumo não traz e-mail (RNF 18).
 */
export async function idDaConta(conta: Conta): Promise<number> {
  const cliente = await api(conta);
  const resposta = await cliente.get("/api/v1/usuarios/me");
  expect(resposta.ok(), await resposta.text()).toBeTruthy();
  const { id } = await resposta.json();
  await cliente.dispose();
  return id;
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
  const idEu = await idDaConta(CONTAS.professor);
  const lista = await (
    await professor.get("/api/v1/disponibilidades", {
      params: { usuarioId: idEu, diaSemana: diaSemanaBanco(dia), ativo: true, size: 50 },
    })
  ).json();
  const cobre = lista.conteudo.some(
    (d: { horarioInicio: string; horarioFim: string }) => d.horarioInicio <= "08:00:00" && d.horarioFim >= "18:00:00"
  );
  if (!cobre) {
    const resposta = await professor.post("/api/v1/disponibilidades", {
      data: { idUsuario: idEu, diaSemana: diaSemanaBanco(dia), horarioInicio: "08:00:00", horarioFim: "18:00:00", ativo: true },
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
  const idAdmin = await idDaConta(CONTAS.admin);
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

interface DisponibilidadeApi {
  id: number;
  idUsuario: number;
  diaSemana: number;
  horarioInicio: string;
  horarioFim: string;
  ativo: boolean;
}

async function marcarHorarios(conta: Conta, lista: DisponibilidadeApi[], ativo: boolean) {
  const cliente = await api(conta);
  for (const d of lista) {
    const resposta = await cliente.put(`/api/v1/disponibilidades/${d.id}`, {
      data: { idUsuario: d.idUsuario, diaSemana: d.diaSemana, horarioInicio: d.horarioInicio, horarioFim: d.horarioFim, ativo },
    });
    expect(resposta.ok(), await resposta.text()).toBeTruthy();
  }
  await cliente.dispose();
}

/**
 * Simula um docente que ainda não cadastrou horários: desativa os horários dele durante o
 * teste e devolve a função que os reativa (chame no finally/afterAll).
 */
export async function semHorariosDeAtendimento(conta: Conta): Promise<() => Promise<void>> {
  const cliente = await api(conta);
  const idEu = await idDaConta(conta);
  const ativos: DisponibilidadeApi[] = (
    await (await cliente.get("/api/v1/disponibilidades", { params: { usuarioId: idEu, ativo: true, size: 100 } })).json()
  ).conteudo;
  await cliente.dispose();
  await marcarHorarios(conta, ativos, false);
  return () => marcarHorarios(conta, ativos, true);
}
