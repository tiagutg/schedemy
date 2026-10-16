import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from "react";
import { definirAoSessaoInvalida, getStoredAuth, setStoredAuth, type ApiError, type StoredAuth } from "@/api/client";
import { usuariosApi } from "@/api/usuarios";
import { useToast } from "@/context/ToastContext";
import { derivarPerfil, perfilPode } from "@/utils/permissoes";
import type { Usuario } from "@/types";

interface Credenciais {
  /** E-mail do usuário cadastrado (é o login do Basic Auth). */
  username: string;
  password: string;
}

interface AuthContextValor {
  auth: StoredAuth | null;
  estaAutenticado: boolean;
  /** Cadastro do usuário logado. */
  perfilVinculado: Usuario | null;
  /** True enquanto o cadastro do usuário logado está sendo carregado. */
  buscandoPerfil: boolean;
  perfil: string | null;
  pode: (permissao: string) => boolean;
  carregando: boolean;
  erro: string | null;
  entrar: (credenciais: Credenciais) => Promise<boolean>;
  sair: () => void;
}

const AuthContext = createContext<AuthContextValor | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [auth, setAuth] = useState<StoredAuth | null>(() => getStoredAuth());
  const [perfilVinculado, setPerfilVinculado] = useState<Usuario | null>(null);
  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  // Enquanto o cadastro do usuário logado não chega, o perfil ainda é desconhecido.
  const [buscandoPerfil, setBuscandoPerfil] = useState(() => !!getStoredAuth());
  const toast = useToast();

  // Ao abrir com uma sessão salva, recarrega o cadastro do usuário logado,
  // pelo id guardado ou, em sessões antigas sem id, pelo e-mail do login.
  useEffect(() => {
    if (!auth || (auth.usuarioId && perfilVinculado?.id === auth.usuarioId)) {
      setBuscandoPerfil(false);
      return;
    }
    let cancelado = false;
    setBuscandoPerfil(true);
    const busca = auth.usuarioId
      ? usuariosApi.buscarPorId(auth.usuarioId)
      : usuariosApi.buscarPorEmail(auth.username);
    busca
      .then((usuario) => {
        if (cancelado) return;
        setPerfilVinculado(usuario);
        if (usuario && !auth.usuarioId) {
          const novaAuth = { ...auth, usuarioId: usuario.id };
          setStoredAuth(novaAuth);
          setAuth(novaAuth);
        }
      })
      .catch(() => !cancelado && setPerfilVinculado(null))
      .finally(() => !cancelado && setBuscandoPerfil(false));
    return () => {
      cancelado = true;
    };
  }, [auth, perfilVinculado?.id]);

  // Credenciais salvas que deixaram de valer (senha trocada, usuário inativado):
  // encerra a sessão em vez de deixar as telas falhando com 401.
  const authAtual = useRef(auth);
  useEffect(() => {
    authAtual.current = auth;
  }, [auth]);
  useEffect(() => {
    definirAoSessaoInvalida(() => {
      if (!authAtual.current) return; // durante o login, quem trata o erro é o formulário
      setStoredAuth(null);
      setAuth(null);
      setPerfilVinculado(null);
      toast.info("Sua sessão expirou. Entre novamente.");
    });
    return () => definirAoSessaoInvalida(null);
  }, [toast]);

  const entrar = useCallback(async ({ username, password }: Credenciais) => {
    setCarregando(true);
    setErro(null);
    try {
      // A busca do próprio cadastro também valida as credenciais (401 se inválidas).
      setStoredAuth({ username, password, usuarioId: null });
      const usuario = await usuariosApi.buscarPorEmail(username);
      const novaAuth = { username, password, usuarioId: usuario?.id ?? null };
      setStoredAuth(novaAuth);
      setPerfilVinculado(usuario);
      setAuth(novaAuth);
      return true;
    } catch (e) {
      const erroApi = e as ApiError;
      setStoredAuth(null);
      setAuth(null);
      setErro(
        erroApi.status === 401 || erroApi.status === 403
          ? "E-mail ou senha inválidos."
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

  const perfil = useMemo(() => derivarPerfil(perfilVinculado), [perfilVinculado]);
  const pode = useCallback((permissao: string) => perfilPode(perfil, permissao), [perfil]);

  const valor = useMemo(
    () => ({
      auth,
      estaAutenticado: !!auth,
      perfilVinculado,
      buscandoPerfil,
      perfil,
      pode,
      carregando,
      erro,
      entrar,
      sair,
    }),
    [auth, perfilVinculado, buscandoPerfil, perfil, pode, carregando, erro, entrar, sair]
  );

  return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth deve ser usado dentro de um AuthProvider");
  return ctx;
}
