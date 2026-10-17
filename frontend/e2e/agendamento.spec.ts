import { expect, test, type Page } from "@playwright/test";
import {
  CONTAS,
  removerAgendamentosComTopico,
  entrar,
  garantirDisponibilidade,
  paraISO,
  proximoDiaUtil,
} from "./apoio";

// Ciclo completo de uma reunião: criar, editar, aceitar o convite, remarcar.
test.describe.configure({ mode: "serial" });

const PREFIXO = "E2E ";
const topico = `${PREFIXO}Orientação ${Date.now()}`;
const dia = proximoDiaUtil(2);
const diaISO = paraISO(dia);
let horarioCriado = "";

test.beforeAll(async () => {
  await removerAgendamentosComTopico(PREFIXO);
  await garantirDisponibilidade(dia);
});

test.afterAll(async () => {
  // Remove o que o teste criou, para não sujar as listas nem contar no limite RN 13.
  await removerAgendamentosComTopico(PREFIXO);
});

const agendaDocentes = (page: Page) => page.getByRole("region", { name: "Agenda dos docentes" });
const horariosSugeridos = (page: Page) => agendaDocentes(page).getByRole("button", { name: /^\d{2}:\d{2}$/ });

async function abrirDetalhe(page: Page, texto: string, status?: string) {
  await page.goto("/agendamentos");
  let linha = page.getByRole("row").filter({ hasText: texto });
  if (status) linha = linha.filter({ hasText: status });
  await linha.getByRole("button", { name: "Ver detalhes" }).click();
  return page.getByRole("dialog");
}

test("aluno cria escolhendo um horário livre do professor", async ({ page }) => {
  await entrar(page, CONTAS.aluno);
  await page.goto("/agendamentos?novo=1");
  const modal = page.getByRole("dialog");

  await modal.locator("label", { hasText: CONTAS.professor.nome }).getByRole("checkbox").click();
  await modal.getByLabel("Duração").selectOption({ label: "30 minutos" });
  await modal.locator("#novo-data").fill(diaISO);
  await modal.locator("#novo-horario").fill("19:00");

  // Fora do horário de atendimento: o aviso aparece antes do envio.
  await expect(agendaDocentes(page)).toContainText("não está livre");
  await horariosSugeridos(page).first().click();
  await expect(agendaDocentes(page)).toContainText("está livre na agenda");
  horarioCriado = await modal.locator("#novo-horario").inputValue();

  await modal.getByLabel("Tópico").fill(topico);
  await modal.getByRole("button", { name: "Criar agendamento" }).click();
  await expect(page.getByText("Agendamento criado com sucesso!")).toBeVisible();
  await expect(page.getByRole("row").filter({ hasText: topico })).toBeVisible();
});

test("avisa antes do envio quando o próprio aluno já tem reunião no horário", async ({ page }) => {
  await entrar(page, CONTAS.aluno);
  await page.goto("/agendamentos?novo=1");
  const modal = page.getByRole("dialog");
  await modal.locator("label", { hasText: CONTAS.professor.nome }).getByRole("checkbox").click();
  await modal.getByLabel("Duração").selectOption({ label: "30 minutos" });
  await modal.locator("#novo-data").fill(diaISO);
  await modal.locator("#novo-horario").fill(horarioCriado);
  await expect(modal.getByText(`Você já tem uma reunião das ${horarioCriado}`)).toBeVisible();
});

test("outro aluno não enxerga o agendamento", async ({ page }) => {
  await entrar(page, CONTAS.outroAluno);
  await page.goto("/agendamentos");
  await expect(page.getByRole("heading", { name: "Meus agendamentos" })).toBeVisible();
  await expect(page.getByText(topico)).toHaveCount(0);
});

test("aluno edita o horário enquanto está pendente", async ({ page }) => {
  await entrar(page, CONTAS.aluno);
  const detalhe = await abrirDetalhe(page, topico);
  await detalhe.getByRole("button", { name: "Editar" }).click();

  const modal = page.getByRole("dialog", { name: "Editar agendamento" });
  await expect(modal.getByLabel("Duração")).toHaveValue(/.+/);
  const atual = await modal.locator("#novo-horario").inputValue();
  const outro = horariosSugeridos(page).filter({ hasNotText: atual }).first();
  const novoHorario = (await outro.textContent())!.trim();
  await outro.click();
  await modal.getByRole("button", { name: "Salvar alterações" }).click();

  await expect(page.getByText("Agendamento atualizado.")).toBeVisible();
  await expect(page.getByRole("dialog")).toContainText(novoHorario);
});

test("professor aceita o convite pelo painel inicial", async ({ page }) => {
  await entrar(page, CONTAS.professor);
  const convites = page.getByRole("region", { name: "Aguardando sua resposta" });
  const convite = convites.getByRole("listitem").filter({ hasText: topico });
  await expect(convite).toBeVisible();
  await convite.getByRole("button", { name: "Aceitar" }).click();
  await expect(page.getByText("Convite aceito.")).toBeVisible();
  await expect(page.getByRole("listitem").filter({ hasText: topico })).toHaveCount(0);
});

test("aluno remarca a reunião confirmada e o histórico registra o motivo", async ({ page }) => {
  await entrar(page, CONTAS.aluno);
  const detalhe = await abrirDetalhe(page, topico);
  await expect(detalhe).toContainText("Confirmado");
  await detalhe.getByRole("button", { name: "Remarcar" }).click();

  const modal = page.getByRole("dialog", { name: "Remarcar reunião" });
  const mais = agendaDocentes(page).getByRole("button", { name: /horários$/ });
  if (await mais.count()) await mais.click();
  await horariosSugeridos(page).last().click();
  await modal.getByLabel("Motivo da remarcação").fill("Choque com prova");
  await modal.getByRole("button", { name: "Remarcar" }).click();
  await expect(page.getByText("Reunião remarcada.")).toBeVisible();

  const original = await abrirDetalhe(page, topico, "Cancelado");
  await expect(original).toContainText("Histórico");
  await expect(original).toContainText("Remarcada para");
  await expect(original).toContainText("Choque com prova");
});

test("a visão semanal mostra a reunião no dia certo", async ({ page }) => {
  await entrar(page, CONTAS.aluno);
  await page.goto("/agendamentos");
  await page.getByRole("button", { name: "Semana" }).click();
  const semana = page.getByRole("region", { name: "Agenda da semana" });
  // Avança da semana atual até a semana da reunião.
  const segunda = (d: Date) => {
    const r = new Date(d.getFullYear(), d.getMonth(), d.getDate());
    r.setDate(r.getDate() - ((r.getDay() + 6) % 7));
    return r;
  };
  const semanas = Math.round((segunda(dia).getTime() - segunda(new Date()).getTime()) / (7 * 86_400_000));
  for (let i = 0; i < semanas; i++) await page.getByRole("button", { name: "Próxima semana" }).click();
  await expect(semana.getByText(topico).first()).toBeVisible();
  await page.getByRole("button", { name: "Lista" }).click();
});
