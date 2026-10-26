import { expect, test, type APIRequestContext } from "@playwright/test";
import { api, CONTAS, garantirDisponibilidade, paraISO, proximoDiaUtil } from "./apoio";

// Regras de agenda validadas direto na API: bloqueios (RF 04, RN 15), horários
// livres para todos (RF 14) e a recepção agendando por terceiros (RF 13, 20, 21).
// Usa outro dia e outro aluno que o agendamento.spec.ts, para não interferir nele.
test.describe.configure({ mode: "serial" });

const PREFIXO = "E2E-AGENDA ";
const dia = proximoDiaUtil(9);
const diaISO = paraISO(dia);

let idProfessor = 0;
let idOutroAluno = 0;
let idDuracao30 = 0;
let idBloqueio: number | null = null;

async function idPorEmail(cliente: APIRequestContext, email: string): Promise<number> {
  const usuarios = await (await cliente.get("/api/v1/usuarios", { params: { size: 200 } })).json();
  return usuarios.conteudo.find((u: { email: string }) => u.email === email).id;
}

function payload(horarioInicio: string, topico: string) {
  return {
    idOrganizador: idOutroAluno,
    idsConvidados: [idProfessor],
    idDuracao: idDuracao30,
    dataReuniao: diaISO,
    horarioInicio,
    formato: "PRESENCIAL",
    topico: `${PREFIXO}${topico}`,
  };
}

test.beforeAll(async () => {
  await garantirDisponibilidade(dia);
  const professor = await api(CONTAS.professor);
  idProfessor = await idPorEmail(professor, CONTAS.professor.email);
  idOutroAluno = await idPorEmail(professor, CONTAS.outroAluno.email);
  const duracoes = await (await professor.get("/api/v1/duracoes-reuniao", { params: { ativas: true } })).json();
  idDuracao30 = duracoes.find((d: { minutos: number }) => d.minutos === 30).id;

  // Bloqueio das 08:00 às 09:00 no dia do teste, com um motivo que não pode vazar.
  const resposta = await professor.post("/api/v1/bloqueios-periodo", {
    data: {
      idUsuario: idProfessor,
      dataInicio: diaISO,
      dataFim: diaISO,
      horarioInicio: "08:00:00",
      horarioFim: "09:00:00",
      eRecorrente: false,
      motivo: "Consulta medica",
    },
  });
  expect(resposta.ok(), await resposta.text()).toBeTruthy();
  idBloqueio = (await resposta.json()).id;
  await professor.dispose();
});

test.afterAll(async () => {
  const admin = await api(CONTAS.admin);
  const idAdmin = await idPorEmail(admin, CONTAS.admin.email);
  const lista = await (await admin.get("/api/v1/agendamentos", { params: { size: 500 } })).json();
  for (const a of lista.conteudo) {
    if (!a.topico.startsWith(PREFIXO)) continue;
    if (a.status !== "CANCELADO") {
      await admin.patch(`/api/v1/agendamentos/${a.id}/cancelamento`, {
        data: { idUsuario: idAdmin, motivo: "Limpeza dos testes automatizados", confirmar: true },
      });
    }
    await admin.delete(`/api/v1/agendamentos/${a.id}`);
  }
  if (idBloqueio) await admin.delete(`/api/v1/bloqueios-periodo/${idBloqueio}`);
  await admin.dispose();
});

test("horários livres descontam o bloqueio do professor sem expor o motivo", async () => {
  const aluno = await api(CONTAS.outroAluno);
  const resposta = await aluno.get("/api/v1/agendamentos/horarios-livres", {
    params: { data: diaISO, idDuracao: idDuracao30, idsUsuarios: `${idOutroAluno},${idProfessor}` },
  });
  expect(resposta.ok(), await resposta.text()).toBeTruthy();
  const corpo = await resposta.json();
  expect(corpo.horarios).not.toContain("08:00:00");
  expect(corpo.horarios).not.toContain("08:45:00");
  expect(corpo.horarios).toContain("09:00:00");
  expect(JSON.stringify(corpo)).not.toContain("Consulta");
  await aluno.dispose();
});

test("a API recusa agendamento em período bloqueado", async () => {
  const aluno = await api(CONTAS.outroAluno);
  const resposta = await aluno.post("/api/v1/agendamentos", { data: payload("08:30:00", "Bloqueado") });
  expect(resposta.status()).toBe(422);
  const corpo = await resposta.json();
  expect(corpo.mensagem).toContain("nao esta disponivel neste horario");
  expect(corpo.mensagem).not.toContain("Consulta");
  await aluno.dispose();
});

test("recepção agenda por um aluno, vê na lista geral e cancela", async () => {
  const recepcao = await api(CONTAS.recepcao);
  const idRecepcao = await idPorEmail(recepcao, CONTAS.recepcao.email);

  const criado = await recepcao.post("/api/v1/agendamentos", { data: payload("10:00:00", "Recepcao") });
  expect(criado.status(), await criado.text()).toBe(201);
  const agendamento = await criado.json();
  expect(agendamento.idOrganizador).toBe(idOutroAluno);
  // RN 03: a recepção não entra como participante.
  expect(agendamento.participantes.map((p: { idUsuario: number }) => p.idUsuario)).not.toContain(idRecepcao);

  const lista = await recepcao.get("/api/v1/agendamentos", { params: { size: 500 } });
  expect(lista.ok()).toBeTruthy();
  expect((await lista.json()).conteudo.map((a: { id: number }) => a.id)).toContain(agendamento.id);

  // RN 03: a recepção não aceita nem recusa convites.
  const resposta = await recepcao.patch(`/api/v1/agendamentos/${agendamento.id}/resposta`, {
    data: { idUsuario: idRecepcao, aceitar: true },
  });
  expect(resposta.status()).toBe(403);

  const cancelado = await recepcao.patch(`/api/v1/agendamentos/${agendamento.id}/cancelamento`, {
    data: { idUsuario: idRecepcao, motivo: "Aluno pediu pelo balcao", confirmar: true },
  });
  expect(cancelado.ok(), await cancelado.text()).toBeTruthy();
  expect((await cancelado.json()).status).toBe("CANCELADO");

  const historico = await recepcao.get(`/api/v1/registros-motivo/agendamentos/${agendamento.id}`);
  expect(historico.ok()).toBeTruthy();
  expect(JSON.stringify(await historico.json())).toContain("Aluno pediu pelo balcao");
  await recepcao.dispose();
});
