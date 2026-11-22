import { expect, test } from "@playwright/test";
import { API, CONTAS, api, entrar } from "./apoio";

// RNF 18 (termo LGPD) e RNF 19 (auditoria), com uma conta nova a cada execução: o dump não guarda aceites, mas
// as contas de teste já aceitaram nos outros arquivos, e quem já entrou não pode ser excluído (só desativado).
test.describe.configure({ mode: "serial" });

const sufixo = Date.now();
const conta = { email: `e2e.termo.${sufixo}@exemplo.com`, senha: "Senha123@" };
let idConta = 0;

test.beforeAll(async () => {
  const admin = await api(CONTAS.admin);
  const resposta = await admin.post("/api/v1/usuarios", {
    data: { nome: `E2E Termo ${sufixo}`, email: conta.email, senha: conta.senha, tipoUsuario: "ALUNO", ativo: true },
  });
  expect(resposta.ok(), await resposta.text()).toBeTruthy();
  idConta = (await resposta.json()).id;
  await admin.dispose();
});

test.afterAll(async () => {
  // Com login na trilha, a conta não pode ser excluída: fica desativada.
  const admin = await api(CONTAS.admin);
  await admin.put(`/api/v1/usuarios/${idConta}`, {
    data: { nome: `E2E Termo ${sufixo}`, email: conta.email, tipoUsuario: "ALUNO", ativo: false },
  });
  await admin.dispose();
});

test("o termo e a política ficam abertos antes do login", async ({ page }) => {
  await page.goto("/login");
  await page.getByRole("link", { name: "Termo de uso e privacidade" }).click();
  await expect(page).toHaveURL(/\/privacidade$/);
  await expect(page.getByRole("heading", { name: "Termo de uso e consentimento" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Política de privacidade" })).toBeVisible();
});

test("no primeiro acesso, nada abre até aceitar o termo; a API também recusa", async ({ page, request }) => {
  await page.goto("/login");
  await page.getByLabel("E-mail institucional").fill(conta.email);
  await page.locator("#senha").fill(conta.senha);
  await page.getByRole("button", { name: "Entrar", exact: true }).click();

  const termo = page.getByRole("region", { name: "Termo de uso e privacidade" });
  await expect(termo).toBeVisible();
  await expect(page.locator("[data-sidebar=content]")).toHaveCount(0);

  // A API recusa as demais rotas enquanto o termo estiver pendente.
  const token = await page.evaluate(() => JSON.parse(localStorage.getItem("schedemy.auth") as string).token);
  const bloqueada = await request.get(`${API}/api/v1/agendamentos/usuarios/${idConta}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  expect(bloqueada.status()).toBe(403);
  expect((await bloqueada.json()).erro).toBe("TERMO_PENDENTE");

  const aceitar = termo.getByRole("button", { name: "Aceitar e continuar" });
  await expect(aceitar).toBeDisabled();
  await termo.getByLabel("Li e aceito o termo de uso e a política de privacidade").click();
  await aceitar.click();
  await expect(page.getByText("Termo aceito. Bem-vindo ao Schedemy!")).toBeVisible();
  await expect(page.locator("[data-sidebar=content]").getByRole("link", { name: "Agendamentos" })).toBeVisible();

  // Aceito, não aparece de novo no próximo acesso.
  await page.reload();
  await expect(page.locator("[data-sidebar=content]")).toBeVisible();
  await expect(termo).toHaveCount(0);
});

test("o admin vê na auditoria o login e o aceite da conta nova; os demais perfis não", async ({ page }) => {
  await entrar(page, CONTAS.admin);
  await page.locator("[data-sidebar=content]").getByRole("link", { name: "Auditoria" }).click();
  await page.getByLabel("Autor").selectOption({ label: `E2E Termo ${sufixo}` });
  const linhas = page.getByRole("row");
  await expect(linhas.filter({ hasText: "Termo LGPD aceito" })).toHaveCount(1);
  await expect(linhas.filter({ hasText: "Login" }).first()).toBeVisible();

  // A criação da conta pelo admin também ficou registrada.
  await page.getByRole("button", { name: "Limpar filtros" }).click();
  await page.getByLabel("Ação").selectOption({ label: "Usuário criado" });
  await expect(linhas.filter({ hasText: `E2E Termo ${sufixo}` })).toHaveCount(1);

  await entrar(page, CONTAS.recepcao);
  await expect(page.locator("[data-sidebar=content]").getByRole("link", { name: "Auditoria" })).toHaveCount(0);
  await page.goto("/auditoria");
  await expect(page.getByText("Acesso restrito")).toBeVisible();
  await expect(page.getByRole("row")).toHaveCount(0);
});
