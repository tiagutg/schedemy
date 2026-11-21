import { expect, test, type Page } from "@playwright/test";
import {
  CONTAS,
  removerAgendamentosComTopico,
  entrar,
  garantirDisponibilidade,
  paraISO,
  proximoDiaUtil,
  semHorariosDeAtendimento,
} from "./apoio";

// Ciclo completo de uma reunião: criar, editar, aceitar o convite, pedir alteração (RF 21/22), cancelar.
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
  // RF 14: as sugestões já descontam reuniões e bloqueios de todos, sem expor a agenda do professor.
  await expect(agendaDocentes(page)).toContainText("está livre para todos os envolvidos");
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
  await expect(convites.getByRole("listitem").filter({ hasText: topico })).toHaveCount(0);
});

test("aluno pede a alteração da reunião confirmada e o professor aceita (RF 21/22)", async ({ page }) => {
  await entrar(page, CONTAS.aluno);
  const detalhe = await abrirDetalhe(page, topico);
  await expect(detalhe).toContainText("Confirmada");
  // RF 21: confirmada com 24 h ou mais de antecedência, o aluno pede em vez de remarcar.
  await expect(detalhe.getByRole("button", { name: "Remarcar" })).toHaveCount(0);
  await detalhe.getByRole("button", { name: "Pedir alteração" }).click();

  const modal = page.getByRole("dialog", { name: "Pedir alteração" });
  const atual = await modal.locator("#novo-horario").inputValue();
  const mais = agendaDocentes(page).getByRole("button", { name: /horários$/ });
  if (await mais.count()) await mais.click();
  const outro = horariosSugeridos(page).filter({ hasNotText: atual }).last();
  const novoHorario = (await outro.textContent())!.trim();
  await outro.click();
  await modal.getByLabel("Motivo do pedido").fill("Choque com prova");
  await modal.getByRole("button", { name: "Enviar pedido" }).click();
  await expect(page.getByText("Pedido de alteração enviado ao docente.")).toBeVisible();

  // Até a resposta, a reunião continua no horário original.
  const pedido = page.getByRole("dialog").getByRole("region", { name: "Pedido de alteração" });
  await expect(pedido).toContainText("Choque com prova");
  await expect(pedido).toContainText(novoHorario);
  await expect(page.getByRole("dialog")).toContainText("Alteração pedida");

  // RF 22 / RN 12: o professor da reunião decide.
  await entrar(page, CONTAS.professor);
  const doProfessor = await abrirDetalhe(page, topico);
  await doProfessor.getByRole("button", { name: "Aceitar alteração" }).click();
  await expect(page.getByText("Alteração aceita: a reunião foi atualizada.")).toBeVisible();
  await expect(page.getByRole("dialog")).toContainText(novoHorario);
  await expect(page.getByRole("dialog")).toContainText("Confirmada");
  await expect(page.getByRole("dialog").getByRole("region", { name: "Pedido de alteração" })).toHaveCount(0);
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

test("sugere os próximos dias livres e preenche data e horário ao escolher", async ({ page }) => {
  const restaurar = await semHorariosDeAtendimento(CONTAS.semHorario);
  try {
    await entrar(page, CONTAS.aluno);
    await page.goto("/agendamentos?novo=1");
    const modal = page.getByRole("dialog");
    await modal.locator("label", { hasText: CONTAS.professor.nome }).getByRole("checkbox").click();

    // Professor sem horário de atendimento: o painel oferece tirá-lo da reunião.
    await modal.locator("label", { hasText: CONTAS.semHorario.nome }).getByRole("checkbox").click();
    const proximos = page.getByRole("group", { name: "Próximos dias livres" });
    await expect(proximos).toContainText("ainda não cadastrou horários de atendimento");
    await expect(agendaDocentes(page)).not.toContainText("Horários livres em");
    await proximos.getByRole("button", { name: "Tirar da reunião" }).click();
    await expect(modal.locator("label", { hasText: CONTAS.semHorario.nome }).getByRole("checkbox")).not.toBeChecked();

    const dias = proximos.getByRole("button");
    await expect(dias.first()).toBeVisible({ timeout: 20_000 });
    const ultimo = dias.last();
    // "Qui, 12/11" + "N horários, das 08:00"
    const [rotulo, detalhe] = (await ultimo.innerText()).split("\n");
    await ultimo.click();
    await expect(ultimo).toHaveAttribute("aria-pressed", "true");

    const [dia, mes] = rotulo.split(", ")[1].split("/");
    await expect(modal.getByLabel("Data")).toHaveValue(new RegExp(`-${mes}-${dia}$`));
    await expect(modal.getByLabel("Horário")).toHaveValue(detalhe.split(", das ")[1].trim());
    await expect(agendaDocentes(page)).toContainText("está livre para todos os envolvidos");
  } finally {
    await restaurar();
  }
});

test("aluno cancela a reunião e o histórico registra o motivo", async ({ page }) => {
  await entrar(page, CONTAS.aluno);
  const detalhe = await abrirDetalhe(page, topico);
  await detalhe.getByRole("button", { name: "Cancelar agendamento" }).click();
  const modal = page.getByRole("dialog", { name: "Cancelar agendamento" });
  await modal.getByLabel("Motivo do cancelamento").fill("Choque com prova");
  await modal.getByRole("button", { name: "Confirmar cancelamento" }).click();
  await expect(page.getByText("Agendamento cancelado.")).toBeVisible();

  const original = await abrirDetalhe(page, topico, "Cancelada");
  await expect(original).toContainText("Histórico");
  await expect(original).toContainText("Choque com prova");
});

test("aluno agenda de novo a partir da reunião cancelada, sem alterar a original (RF 28)", async ({ page }) => {
  await entrar(page, CONTAS.aluno);
  const original = await abrirDetalhe(page, topico, "Cancelada");
  await original.getByRole("button", { name: "Agendar de novo" }).click();

  const modal = page.getByRole("dialog", { name: "Agendar de novo" });
  // Tópico e participantes vêm da reunião anterior.
  await expect(modal.getByLabel("Tópico")).toHaveValue(topico);
  await expect(modal.locator("label", { hasText: CONTAS.professor.nome }).getByRole("checkbox")).toBeChecked();

  await modal.locator("#novo-data").fill(diaISO);
  const mais = agendaDocentes(page).getByRole("button", { name: /horários$/ });
  if (await mais.count()) await mais.click();
  await horariosSugeridos(page).first().click();
  await expect(agendaDocentes(page)).toContainText("está livre para todos os envolvidos");
  await modal.getByRole("button", { name: "Criar agendamento" }).click();
  await expect(page.getByText("Agendamento criado com sucesso!")).toBeVisible();

  // A nova nasce pendente e a original continua cancelada.
  await page.goto("/agendamentos");
  const linhas = page.getByRole("row").filter({ hasText: topico });
  await expect(linhas.filter({ hasText: "Cancelada" })).toHaveCount(1);
  await expect(linhas.filter({ hasText: /Pendente|Aguardando/ }).first()).toBeVisible();
});
