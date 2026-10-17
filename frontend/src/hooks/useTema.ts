import { useSyncExternalStore } from "react";

// Tema da interface: claro, escuro ou seguindo o sistema operacional.
// A escolha fica no navegador; o index.html aplica a classe antes do primeiro
// desenho da página para não piscar o tema errado.

export type Tema = "claro" | "escuro" | "sistema";

const CHAVE = "schedemy.tema";
const CONSULTA_ESCURO = "(prefers-color-scheme: dark)";
const ouvintes = new Set<() => void>();

function temaSalvo(): Tema {
  try {
    const valor = localStorage.getItem(CHAVE);
    return valor === "claro" || valor === "escuro" ? valor : "sistema";
  } catch {
    return "sistema";
  }
}

function escuroAtivo(tema: Tema): boolean {
  return tema === "escuro" || (tema === "sistema" && window.matchMedia(CONSULTA_ESCURO).matches);
}

function aplicar() {
  document.documentElement.classList.toggle("dark", escuroAtivo(temaSalvo()));
  ouvintes.forEach((ouvinte) => ouvinte());
}

export function definirTema(tema: Tema) {
  try {
    if (tema === "sistema") localStorage.removeItem(CHAVE);
    else localStorage.setItem(CHAVE, tema);
  } catch {
    /* sem armazenamento: vale só até recarregar */
  }
  aplicar();
}

function assinar(ouvinte: () => void) {
  ouvintes.add(ouvinte);
  const mql = window.matchMedia(CONSULTA_ESCURO);
  mql.addEventListener("change", aplicar);
  return () => {
    ouvintes.delete(ouvinte);
    mql.removeEventListener("change", aplicar);
  };
}

/** Tema escolhido e se a interface está escura agora. */
export function useTema() {
  const tema = useSyncExternalStore(assinar, temaSalvo, () => "sistema" as Tema);
  const escuro = useSyncExternalStore(
    assinar,
    () => document.documentElement.classList.contains("dark"),
    () => false
  );
  return { tema, escuro, definirTema };
}
