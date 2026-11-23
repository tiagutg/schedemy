import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from "react";
import { notificacoesApi } from "@/api/notificacoes";
import { useAuth } from "./AuthContext";

// De quanto em quanto tempo o contador de avisos não lidos é atualizado.
const INTERVALO_MS = 30_000;

interface AvisosContextValue {
  /** Avisos do usuário logado ainda não lidos. */
  naoLidos: number;
  atualizar: () => void;
  /**
   * Muda quando chega aviso novo ou quando a pessoa volta para a aba: as telas de reuniões
   * recarregam os dados ao vê-la mudar (quase toda mudança de reunião gera aviso, RF 23).
   */
  versaoDados: number;
}

const AvisosContext = createContext<AvisosContextValue>({ naoLidos: 0, atualizar: () => {}, versaoDados: 0 });

/** RF 23: contador de avisos do sistema (convites, respostas, cancelamentos, lembretes). */
export function AvisosProvider({ children }: { children: ReactNode }) {
  const { perfilVinculado: eu } = useAuth();
  const [naoLidos, setNaoLidos] = useState(0);
  const [versao, setVersao] = useState(0);
  const atualizar = useCallback(() => setVersao((v) => v + 1), []);
  const [versaoDados, setVersaoDados] = useState(0);
  // Aviso não lido mais recente já visto (null = ainda não consultado).
  const ultimoAviso = useRef<number | null>(null);

  useEffect(() => {
    if (!eu) return;
    let cancelado = false;
    const buscar = () =>
      notificacoesApi
        .listar({ usuarioId: eu.id, status: "PENDENTE", page: 0, size: 1, sort: "id,desc" })
        .then((p) => {
          if (cancelado) return;
          setNaoLidos(p.totalElementos);
          const maisRecente = p.conteudo[0]?.id ?? 0;
          if (ultimoAviso.current !== null && maisRecente > ultimoAviso.current) setVersaoDados((v) => v + 1);
          ultimoAviso.current = Math.max(ultimoAviso.current ?? 0, maisRecente);
        })
        .catch(() => {
          /* o contador é só um atalho: falhas aparecem na tela de notificações */
        });
    buscar();
    const timer = setInterval(() => {
      if (document.visibilityState === "visible") buscar();
    }, INTERVALO_MS);
    // De volta à aba: o que mudou enquanto ela estava escondida aparece sem recarregar a página.
    const aoVoltar = () => {
      if (document.visibilityState !== "visible") return;
      buscar();
      setVersaoDados((v) => v + 1);
    };
    document.addEventListener("visibilitychange", aoVoltar);
    return () => {
      cancelado = true;
      clearInterval(timer);
      document.removeEventListener("visibilitychange", aoVoltar);
    };
  }, [eu, versao]);

  const valor = useMemo(
    () => ({ naoLidos: eu ? naoLidos : 0, atualizar, versaoDados }),
    [eu, naoLidos, atualizar, versaoDados]
  );
  return <AvisosContext.Provider value={valor}>{children}</AvisosContext.Provider>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAvisos() {
  return useContext(AvisosContext);
}
