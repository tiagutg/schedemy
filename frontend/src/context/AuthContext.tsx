import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from "react";
import {
  definirAoSessaoInvalida,
  definirAoTermoPendente,
  getStoredAuth,
  setStoredAuth,
  type ApiError,
  type StoredAuth,
} from "@/api/client";
import { authApi } from "@/api/auth";
import { lgpdApi } from "@/api/lgpd";
import { usuariosApi } from "@/api/usuarios";
import { useToast } from "@/context/ToastContext";
import { derivarPerfil, perfilPode } from "@/utils/permissoes";
import type { Usuario } from "@/types";

interface Credenciais {
  /** E-mail do usuário cadastrado. */
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
  /** RNF 18: falta aceitar a versão em vigor do termo (null enquanto não se sabe). */
  termoPendente: boolean | null;
  /** RNF 18: aceita a versão lida; a API recusa se o termo mudou nesse meio-tempo. */
  aceitarTermo: (versao: string) => Promise<void>;
}

const AuthContext = createContext<AuthContextValor | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [auth, setAuth] = useState<StoredAuth | null>(() => getStoredAuth());
  const [perfilVinculado, setPerfilVinculado] = useState<Usuario | null>(null);
  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  // Sessão para a qual a busca do cadastro já terminou (com ou sem sucesso).
  const [perfilResolvidoPara, setPerfilResolvidoPara] = useState<string | null>(null);
  // RNF 18: situação do aceite do termo para a sessão atual.
  const [termoPendente, setTermoPendente] = useState<boolean | null>(null);
  const toast = useToast();

  const chaveSessao = auth ? `${auth.token}|${auth.usuarioId ?? ""}` : null;
  const perfilEmDia = !!auth?.usuarioId && perfilVinculado?.id === auth.usuarioId;
  // Enquanto o cadastro do usuário logado não chega, o perfil ainda é desconhecido.
  const buscandoPerfil = !!auth && !perfilEmDia && perfilResolvidoPara !== chaveSessao;

  // Ao abrir com uma sessão salva, recarrega o cadastro do usuário logado (GET /usuarios/me).
  useEffect(() => {
    if (!auth || (auth.usuarioId && perfilVinculado?.id === auth.usuarioId)) return;
    let cancelado = false;
    const chave = `${auth.token}|${auth.usuarioId ?? ""}`;
    usuariosApi
      .eu()
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
      .finally(() => !cancelado && setPerfilResolvidoPara(chave));
    return () => {
      cancelado = true;
    };
  }, [auth, perfilVinculado?.id]);

  // RNF 18: com a sessão aberta, confere se o termo em vigor já foi aceito.
  useEffect(() => {
    if (!auth) return;
    let cancelado = false;
    lgpdApi
      .situacao()
      .then((s) => !cancelado && setTermoPendente(s.pendente))
      // Sem a resposta, deixa a API decidir: um 403 TERMO_PENDENTE reabre o termo.
      .catch(() => !cancelado && setTermoPendente(false));
    return () => {
      cancelado = true;
    };
  }, [auth]);
  useEffect(() => {
    definirAoTermoPendente(() => setTermoPendente(true));
    return () => definirAoTermoPendente(null);
  }, []);
  const aceitarTermo = useCallback(async (versao: string) => {
    const s = await lgpdApi.aceitar(versao);
    setTermoPendente(s.pendente);
  }, []);

  // Token que deixou de valer (venceu, senha trocada, usuário inativado):
  // encerra a sessão em vez de deixar as telas falhando com 401.
  const authAtual = useRef(auth);
  useEffect(() => {
    authAtual.current = auth;
  }, [auth]);
  const encerrarSessaoExpirada = useCallback(() => {
    if (!authAtual.current) return; // durante o login, quem trata o erro é o formulário
    setStoredAuth(null);
    setAuth(null);
    setPerfilVinculado(null);
    setTermoPendente(null);
    toast.info("Sua sessão expirou. Entre novamente.");
  }, [toast]);
  useEffect(() => {
    definirAoSessaoInvalida(encerrarSessaoExpirada);
    return () => definirAoSessaoInvalida(null);
  }, [encerrarSessaoExpirada]);

  // Com a tela aberta, encerra no instante em que o token vence (sem esperar a próxima chamada falhar).
  useEffect(() => {
    if (!auth) return;
    const restante = Date.parse(auth.expiraEm) - Date.now();
    const temporizador = setTimeout(encerrarSessaoExpirada, Math.max(0, Math.min(restante, 2 ** 31 - 1)));
    return () => clearTimeout(temporizador);
  }, [auth, encerrarSessaoExpirada]);

  const entrar = useCallback(async ({ username, password }: Credenciais) => {
    setCarregando(true);
    setErro(null);
    try {
      // A senha só vai no login; daqui em diante, o navegador guarda apenas o token.
      const { token, expiraEm } = await authApi.entrar(username, password);
      setStoredAuth({ email: username, token, expiraEm, usuarioId: null });
      const usuario = await usuariosApi.eu();
      const novaAuth = { email: username, token, expiraEm, usuarioId: usuario?.id ?? null };
      setStoredAuth(novaAuth);
      setPerfilVinculado(usuario);
      setTermoPendente(null); // a situação do termo é conferida para a sessão nova
      setAuth(novaAuth);
      return true;
    } catch (e) {
      const erroApi = e as ApiError;
      setStoredAuth(null);
      setAuth(null);
      setErro(
        erroApi.status === 401 || erroApi.status === 403 || erroApi.status === 400
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
    setTermoPendente(null);
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
      termoPendente: auth ? termoPendente : null,
      aceitarTermo,
    }),
    [auth, perfilVinculado, buscandoPerfil, perfil, pode, carregando, erro, entrar, sair, termoPendente, aceitarTermo]
  );

  return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth deve ser usado dentro de um AuthProvider");
  return ctx;
}
