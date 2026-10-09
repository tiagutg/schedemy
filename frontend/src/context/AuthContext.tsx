import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { getStoredAuth, setStoredAuth, type ApiError, type StoredAuth } from "@/api/client";
import { usuariosApi } from "@/api/usuarios";
import { derivarPerfil, perfilPode } from "@/utils/permissoes";
import type { Usuario } from "@/types";

interface Credenciais {
  username: string;
  password: string;
  usuarioId?: number | null;
}

interface AuthContextValor {
  auth: StoredAuth | null;
  estaAutenticado: boolean;
  perfilVinculado: Usuario | null;
  perfil: string | null;
  pode: (permissao: string) => boolean;
  carregando: boolean;
  erro: string | null;
  entrar: (credenciais: Credenciais) => Promise<boolean>;
  sair: () => void;
  vincularPerfil: (usuario: Usuario | null) => void;
}

const AuthContext = createContext<AuthContextValor | null>(null);

/** Papéis pré-cadastrados no SecurityConfig do back-end, usados para o atalho de login. */
export const CONTAS_PADRAO = [
  { usuario: "admin", senha: "admin123", papel: "ADMIN / COORDENADOR / RECEPCIONISTA" },
  { usuario: "coordenador", senha: "coordenador123", papel: "COORDENADOR" },
  { usuario: "professor", senha: "professor123", papel: "PROFESSOR" },
  { usuario: "recepcionista", senha: "recepcionista123", papel: "RECEPCIONISTA" },
  { usuario: "aluno", senha: "aluno123", papel: "ALUNO" },
];

export function AuthProvider({ children }: { children: ReactNode }) {
  const [auth, setAuth] = useState<StoredAuth | null>(() => getStoredAuth());
  const [perfilVinculado, setPerfilVinculado] = useState<Usuario | null>(null);
  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);

  // Ao carregar uma sessão salva, tenta recarregar o perfil (Usuario) vinculado, se houver.
  useEffect(() => {
    if (auth?.usuarioId) {
      usuariosApi
        .buscarPorId(auth.usuarioId)
        .then(setPerfilVinculado)
        .catch(() => setPerfilVinculado(null));
    }
  }, [auth?.usuarioId]);

  const entrar = useCallback(async ({ username, password, usuarioId }: Credenciais) => {
    setCarregando(true);
    setErro(null);
    try {
      // Valida as credenciais chamando um endpoint protegido simples.
      setStoredAuth({ username, password, usuarioId: usuarioId || null });
      await usuariosApi.listar({ page: 0, size: 1 });
      const novaAuth = { username, password, usuarioId: usuarioId || null };
      setAuth(novaAuth);
      if (usuarioId) {
        try {
          setPerfilVinculado(await usuariosApi.buscarPorId(usuarioId));
        } catch {
          setPerfilVinculado(null);
        }
      }
      return true;
    } catch (e) {
      const erroApi = e as ApiError;
      setStoredAuth(null);
      setAuth(null);
      setErro(
        erroApi.status === 401 || erroApi.status === 403
          ? "Usuário ou senha inválidos."
          : erroApi.message || "Não foi possível entrar."
      );
      return false;
    } finally {
      setCarregando(false);
    }
  }, []);

  const sair = useCallback(() => {
    setStoredAuth(null);
    setAuth(null);
    setPerfilVinculado(null);
  }, []);

  const vincularPerfil = useCallback(
    (usuario: Usuario | null) => {
      setPerfilVinculado(usuario);
      const novaAuth = { ...auth, usuarioId: usuario?.id ?? null } as StoredAuth;
      setAuth(novaAuth);
      setStoredAuth(novaAuth);
    },
    [auth]
  );

  const perfil = useMemo(() => derivarPerfil(auth, perfilVinculado), [auth, perfilVinculado]);
  const pode = useCallback((permissao: string) => perfilPode(perfil, permissao), [perfil]);

  const valor = useMemo(
    () => ({
      auth,
      estaAutenticado: !!auth,
      perfilVinculado,
      perfil,
      pode,
      carregando,
      erro,
      entrar,
      sair,
      vincularPerfil,
    }),
    [auth, perfilVinculado, perfil, pode, carregando, erro, entrar, sair, vincularPerfil]
  );

  return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth deve ser usado dentro de um AuthProvider");
  return ctx;
}
