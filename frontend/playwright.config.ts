import { defineConfig } from "@playwright/test";

/**
 * Testes de ponta a ponta do painel.
 *
 * Pré-requisitos: MySQL com o db/dump_schedemy.sql importado e a API rodando
 * em http://localhost:8080. O servidor do Vite é iniciado aqui se ainda não
 * estiver no ar. Usa o Chrome instalado na máquina (sem download de navegador).
 */
export default defineConfig({
  testDir: "./e2e",
  // Os cenários compartilham o mesmo banco: rodam em sequência.
  workers: 1,
  fullyParallel: false,
  timeout: 60_000,
  expect: { timeout: 10_000 },
  reporter: [["list"]],
  use: {
    baseURL: "http://localhost:5173",
    channel: "chrome",
    headless: true,
    locale: "pt-BR",
    trace: "retain-on-failure",
  },
  webServer: {
    command: "npm run dev",
    url: "http://localhost:5173",
    reuseExistingServer: true,
    timeout: 60_000,
  },
});
