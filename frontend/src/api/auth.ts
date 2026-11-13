import { apiFetch } from "./client";

export interface TokenDeAcesso {
  token: string;
  tipo: "Bearer";
  /** ISO 8601, UTC. */
  expiraEm: string;
}

export const authApi = {
  /** Troca e-mail e senha pelo token de acesso (401 se não conferirem). A senha não fica guardada. */
  entrar: (email: string, senha: string) =>
    apiFetch<TokenDeAcesso>("/api/v1/auth/login", {
      method: "POST",
      body: { email, senha },
      semAutenticacao: true,
    }),
};
