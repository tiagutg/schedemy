/// <reference types="vitest/config" />
import { fileURLToPath, URL } from "node:url"
import tailwindcss from "@tailwindcss/vite"
import react from "@vitejs/plugin-react"
import { defineConfig } from "vite"

// https://vite.dev/config/
export default defineConfig({
  // Caminho do site: "/" no desenvolvimento; "/schedemy/" no GitHub Pages (definido no workflow).
  base: process.env.BASE_PATH || "/",
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
  test: {
    // Testes unitários; os de ponta a ponta (e2e/) rodam com o Playwright.
    include: ["src/**/*.test.ts"],
  },
})
