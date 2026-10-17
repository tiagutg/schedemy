import { expect, test } from "@playwright/test";
import { CONTAS, entrar } from "./apoio";

test("recusa e-mail ou senha inválidos", async ({ page }) => {
  await page.goto("/login");
  await page.getByLabel("E-mail institucional").fill(CONTAS.aluno.email);
  await page.locator("#senha").fill("senha-errada");
  await page.getByRole("button", { name: "Entrar", exact: true }).click();
  await expect(page.getByRole("alert")).toHaveText("E-mail ou senha inválidos.");
  await expect(page).toHaveURL(/\/login$/);
});

test("aluno vê só o próprio menu e os próprios agendamentos", async ({ page }) => {
  await entrar(page, CONTAS.aluno);
  const menu = page.locator("[data-sidebar=content]");
  await expect(menu.getByRole("link", { name: "Agendamentos" })).toBeVisible();
  await expect(menu.getByRole("link", { name: "Usuários" })).toHaveCount(0);
  await expect(menu.getByRole("link", { name: "Disponibilidades" })).toHaveCount(0);

  await page.goto("/agendamentos");
  await expect(page.getByRole("heading", { name: "Meus agendamentos" })).toBeVisible();
  await expect(page.getByLabel("Usuário envolvido")).toHaveCount(0);

  // O organizador é sempre o próprio aluno.
  await page.getByRole("button", { name: "Novo agendamento" }).first().click();
  await expect(page.getByLabel("Organizador")).toHaveValue(CONTAS.aluno.nome);
  await expect(page.getByLabel("Organizador")).toBeDisabled();
});

test("recepção vê a agenda geral e escolhe o organizador", async ({ page }) => {
  await entrar(page, CONTAS.recepcao);
  await page.goto("/agendamentos");
  await expect(page.getByRole("heading", { name: "Agendamentos", exact: true })).toBeVisible();
  await expect(page.getByLabel("Usuário envolvido")).toBeVisible();

  await page.getByRole("button", { name: "Novo agendamento" }).first().click();
  const organizador = page.getByLabel("Organizador");
  await expect(organizador).toBeEnabled();
  // Recepção e administração não aparecem como organizador (RN 03).
  await expect(organizador.locator("option", { hasText: "Recepcionista" })).toHaveCount(0);
});

test("professor gerencia só a própria disponibilidade", async ({ page }) => {
  await entrar(page, CONTAS.professor);
  await page.goto("/disponibilidades");
  await expect(page.getByRole("heading", { name: "Minhas disponibilidades" })).toBeVisible();
  await page.getByRole("button", { name: "Nova disponibilidade" }).click();
  await expect(page.getByLabel("Professor / coordenador")).toHaveValue(CONTAS.professor.nome);
  await expect(page.getByLabel("Professor / coordenador")).toBeDisabled();
});

test("sessão com senha que deixou de valer volta para o login", async ({ page }) => {
  await entrar(page, CONTAS.aluno);
  await page.evaluate(() => {
    const auth = JSON.parse(localStorage.getItem("schedemy.auth") as string);
    localStorage.setItem("schedemy.auth", JSON.stringify({ ...auth, password: "errada" }));
  });
  await page.goto("/agendamentos");
  await expect(page).toHaveURL(/\/login$/);
  await expect(page.getByText("Sua sessão expirou. Entre novamente.")).toBeVisible();
});
