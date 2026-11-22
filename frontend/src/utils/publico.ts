/**
 * Endereço de um arquivo da pasta public/ respeitando o caminho base do site:
 * "/" no desenvolvimento e "/schedemy/" no GitHub Pages (BASE_PATH no vite.config.ts).
 */
export function arquivoPublico(nome: string): string {
  return `${import.meta.env.BASE_URL}${nome}`;
}
