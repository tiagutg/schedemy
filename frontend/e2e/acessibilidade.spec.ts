import AxeBuilder from "@axe-core/playwright";
import { expect, test, type Page } from "@playwright/test";
import { CONTAS, entrar } from "./apoio";

// Verificação automática de acessibilidade (WCAG 2.1 A/AA) nas telas principais,
// nos temas claro e escuro.

async function semViolacoes(page: Page, tela: string) {
  const { violations } = await new AxeBuilder({ page })
    .withTags(["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"])
    .analyze();
  const resumo = violations.map(
    (v) => `${v.id} (${v.impact}): ${v.help}\n    ${v.nodes.slice(0, 3).map((n) => n.target.join(" ")).join("\n    ")}`
  );
  expect.soft(resumo, `Problemas de acessibilidade em ${tela}`).toEqual([]);
}

async function usarTema(page: Page, tema: "claro" | "escuro") {
  await page.evaluate((t) => localStorage.setItem("schedemy.tema", t), tema);
}

for (const tema of ["claro", "escuro"] as const) {
  test.describe(`tema ${tema}`, () => {
    test("login", async ({ page }) => {
      await page.goto("/login");
      await usarTema(page, tema);
      await page.reload();
      await semViolacoes(page, "login");
    });

    test("telas do aluno", async ({ page }) => {
      await entrar(page, CONTAS.aluno);
      await usarTema(page, tema);
      await page.reload();
      await expect(page.getByRole("heading", { level: 2 }).first()).toBeVisible();
      await semViolacoes(page, "painel");

      await page.goto("/agendamentos");
      await expect(page.getByRole("heading", { name: "Meus agendamentos" })).toBeVisible();
      await semViolacoes(page, "agendamentos (lista)");

      await page.getByRole("button", { name: "Semana" }).click();
      await expect(page.getByRole("region", { name: "Agenda da semana" })).toBeVisible();
      await semViolacoes(page, "agendamentos (semana)");
      await page.getByRole("button", { name: "Lista" }).click();

      await page.getByRole("button", { name: "Novo agendamento" }).first().click();
      const modal = page.getByRole("dialog");
      await modal.locator("label", { hasText: CONTAS.professor.nome }).getByRole("checkbox").click();
      await modal.getByLabel("Duração").selectOption({ label: "30 minutos" });
      await expect(page.getByRole("region", { name: "Agenda dos docentes" })).toBeVisible();
      await semViolacoes(page, "novo agendamento");
      await page.keyboard.press("Escape");

      await page.goto("/avaliacoes");
      await expect(page.getByRole("heading", { name: "Minhas avaliações" })).toBeVisible();
      await semViolacoes(page, "avaliações");

      await page.goto("/perfil");
      await expect(page.getByRole("heading", { name: "Meu perfil", level: 2 })).toBeVisible();
      await semViolacoes(page, "meu perfil");
    });

    test("telas do professor e da administração", async ({ page }) => {
      await entrar(page, CONTAS.professor);
      await usarTema(page, tema);
      await page.goto("/bloqueios");
      await expect(page.getByRole("heading", { name: "Meus bloqueios de período" })).toBeVisible();
      await semViolacoes(page, "bloqueios");

      await entrar(page, CONTAS.admin);
      await usarTema(page, tema);
      await page.goto("/usuarios");
      await expect(page.getByRole("heading", { name: "Usuários", exact: true })).toBeVisible();
      await semViolacoes(page, "usuários");
    });
  });
}
