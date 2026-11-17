import { expect, test } from "@playwright/test";
import { api, CONTAS, entrar, garantirDisponibilidade, idDaConta, paraISO, proximoDiaUtil, removerAgendamentosComTopico } from "./apoio";

// RF 23: o sistema avisa os envolvidos. O aluno convida o professor; o professor
// vê o aviso, abre a reunião por ele e o aviso passa a constar como lido.
test.describe.configure({ mode: "serial" });

const PREFIXO = "E2E-AVISO ";
const topico = `${PREFIXO}${Date.now()}`;
const dia = proximoDiaUtil(12);
let idAgendamento = 0;

test.beforeAll(async () => {
  await garantirDisponibilidade(dia);
  const aluno = await api(CONTAS.aluno);
  const [idAluno, idProfessor] = await Promise.all([idDaConta(CONTAS.aluno), idDaConta(CONTAS.professor)]);
  const duracoes = await (await aluno.get("/api/v1/duracoes-reuniao", { params: { ativas: true } })).json();
  const resposta = await aluno.post("/api/v1/agendamentos", {
    data: {
      idOrganizador: idAluno,
      idsConvidados: [idProfessor],
      idDuracao: duracoes.find((d: { minutos: number }) => d.minutos === 30).id,
      dataReuniao: paraISO(dia),
      horarioInicio: "16:00:00",
      formato: "ONLINE",
      topico,
      linkReuniao: "https://meet.google.com/e2e-aviso",
    },
  });
  expect(resposta.ok(), await resposta.text()).toBeTruthy();
  const criado = await resposta.json();
  idAgendamento = criado.id;
  // O link informado pelo organizador vira a sala da reunião.
  expect(criado.salaVirtual.linkTeams).toBe("https://meet.google.com/e2e-aviso");
  await aluno.dispose();
});

test.afterAll(async () => {
  await removerAgendamentosComTopico(PREFIXO);
});

test("o convidado recebe o aviso e abre a reunião por ele", async ({ page }) => {
  await entrar(page, CONTAS.professor);
  const sino = page.getByRole("link", { name: /^Notificações: \d+ não lida/ });
  await expect(sino).toBeVisible();
  await sino.click();

  await expect(page.getByRole("heading", { name: "Notificações", level: 2 })).toBeVisible();
  const aviso = page.getByRole("button").filter({ hasText: topico });
  await expect(aviso).toContainText("convidou você");
  await expect(aviso).toContainText("não lida");
  await aviso.click();

  const detalhe = page.getByRole("dialog");
  await expect(detalhe).toContainText(topico);
  await expect(detalhe).toContainText("Aguardando você");
  await expect(detalhe.getByRole("link", { name: "Entrar na reunião" })).toHaveAttribute(
    "href",
    "https://meet.google.com/e2e-aviso"
  );
  await expect(detalhe.getByRole("button", { name: "Adicionar ao calendário" })).toBeVisible();

  await page.goto("/notificacoes");
  await expect(page.getByRole("button").filter({ hasText: topico })).not.toContainText("não lida");
});

test("o organizador é avisado quando o convidado aceita", async () => {
  const professor = await api(CONTAS.professor);
  const idProfessor = await idDaConta(CONTAS.professor);
  const resposta = await professor.patch(`/api/v1/agendamentos/${idAgendamento}/resposta`, {
    data: { idUsuario: idProfessor, aceitar: true },
  });
  expect(resposta.ok(), await resposta.text()).toBeTruthy();
  await professor.dispose();

  const aluno = await api(CONTAS.aluno);
  const idAluno = await idDaConta(CONTAS.aluno);
  const avisos = await (await aluno.get("/api/v1/notificacoes", { params: { usuarioId: idAluno, size: 50, sort: "criadoEm,desc" } })).json();
  const daReuniao = avisos.conteudo.filter((n: { idAgendamento: number }) => n.idAgendamento === idAgendamento);
  expect(daReuniao.map((n: { tipoEvento: string }) => n.tipoEvento)).toContain("CONFIRMACAO");
  await aluno.dispose();
});
