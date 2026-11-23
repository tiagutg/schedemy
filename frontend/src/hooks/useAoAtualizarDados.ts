import { useEffect, useRef } from "react";
import { useAvisos } from "@/context/AvisosContext";

/**
 * Roda `acao` quando os dados podem ter mudado fora desta tela: chegou aviso novo
 * (aceite, recusa, cancelamento, confirmação…) ou a pessoa voltou para a aba.
 * Não roda na montagem — a tela já carrega os dados ao abrir.
 */
export function useAoAtualizarDados(acao: () => void) {
  const { versaoDados } = useAvisos();
  const acaoAtual = useRef(acao);
  const versaoVista = useRef(versaoDados);

  useEffect(() => {
    acaoAtual.current = acao;
  });

  useEffect(() => {
    if (versaoDados === versaoVista.current) return;
    versaoVista.current = versaoDados;
    acaoAtual.current();
  }, [versaoDados]);
}
