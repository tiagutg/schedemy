/** Sessão guardada no navegador: só o token de acesso, nunca a senha (item 2.3). */
export interface StoredAuth {
  /** E-mail usado no login (só para exibir enquanto o cadastro não carrega). */
  email: string;
  /** JWT devolvido por POST /api/v1/auth/login, enviado como "Authorization: Bearer". */
  token: string;
  /** Instante de expiração do token (ISO 8601, UTC). */
  expiraEm: string;
  usuarioId: number | null;
}

/**
 * Lê a sessão salva. Devolve null — e quem chama apaga o registro — quando:
 * - o formato é o antigo (Basic Auth, com a senha em texto) ou está corrompido;
 * - o token já venceu.
 */
export function lerSessao(bruto: string | null, agora: number = Date.now()): StoredAuth | null {
  if (!bruto) return null;
  let dados: unknown;
  try {
    dados = JSON.parse(bruto);
  } catch {
    return null;
  }
  if (!dados || typeof dados !== "object") return null;
  const { email, token, expiraEm, usuarioId } = dados as Record<string, unknown>;
  if (typeof token !== "string" || !token || typeof expiraEm !== "string") return null;
  const expira = Date.parse(expiraEm);
  if (Number.isNaN(expira) || expira <= agora) return null;
  return {
    email: typeof email === "string" ? email : "",
    token,
    expiraEm,
    usuarioId: typeof usuarioId === "number" ? usuarioId : null,
  };
}
