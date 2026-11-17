import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { notificacoesApi } from "@/api/notificacoes";
import { useAuth } from "./AuthContext";

// De quanto em quanto tempo o contador de avisos não lidos é atualizado.
const INTERVALO_MS = 60_000;

interface AvisosContextValue {
  /** Avisos do usuário logado ainda não lidos. */
  naoLidos: number;
  atualizar: () => void;
}

const AvisosContext = createContext<AvisosContextValue>({ naoLidos: 0, atualizar: () => {} });

/** RF 23: contador de avisos do sistema (convites, respostas, cancelamentos, lembretes). */
export function AvisosProvider({ children }: { children: ReactNode }) {
  const { perfilVinculado: eu } = useAuth();
  const [naoLidos, setNaoLidos] = useState(0);
  const [versao, setVersao] = useState(0);
  const atualizar = useCallback(() => setVersao((v) => v + 1), []);

  useEffect(() => {
    if (!eu) return;
    let cancelado = false;
    const buscar = () =>
      notificacoesApi
        .listar({ usuarioId: eu.id, status: "PENDENTE", page: 0, size: 1 })
        .then((p) => !cancelado && setNaoLidos(p.totalElementos))
        .catch(() => {
          /* o contador é só um atalho: falhas aparecem na tela de notificações */
        });
    buscar();
    const timer = setInterval(() => {
      if (document.visibilityState === "visible") buscar();
    }, INTERVALO_MS);
    return () => {
      cancelado = true;
      clearInterval(timer);
    };
  }, [eu, versao]);

  const valor = useMemo(() => ({ naoLidos: eu ? naoLidos : 0, atualizar }), [eu, naoLidos, atualizar]);
  return <AvisosContext.Provider value={valor}>{children}</AvisosContext.Provider>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAvisos() {
  return useContext(AvisosContext);
}
