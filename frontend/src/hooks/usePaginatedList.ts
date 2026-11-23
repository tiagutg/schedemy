import { useCallback, useEffect, useState } from "react";
import { useToast } from "@/context/ToastContext";
import type { ApiError } from "@/api/client";
import type { Filtros, Pagina, Params } from "@/types";

interface Opcoes {
  tamanhoPagina?: number;
  filtrosIniciais?: Filtros;
  ordenarPor?: string;
}

/**
 * Encapsula o padrão comum de listagem paginada da Schedemy API:
 * mantém filtros, página atual, estado de carregamento e permite recarregar
 * após operações de criação/edição/remoção.
 *
 * `buscar` deve ser uma função (params) => Promise<PageResponseDTO>.
 */
export function usePaginatedList<T>(
  buscar: (params: Params) => Promise<Pagina<T>>,
  { tamanhoPagina = 10, filtrosIniciais = {}, ordenarPor }: Opcoes = {}
) {
  const toast = useToast();
  const [filtros, setFiltros] = useState<Filtros>(filtrosIniciais);
  const [paginaAtual, setPaginaAtual] = useState(0);
  const [pagina, setPagina] = useState<Pagina<T> | null>(null);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);

  const carregar = useCallback(
    async (silencioso: boolean) => {
      if (!silencioso) {
        setCarregando(true);
        setErro(null);
      }
      try {
        const params: Params = { ...filtros, page: paginaAtual, size: tamanhoPagina };
        if (ordenarPor) params.sort = ordenarPor;
        const resultado = await buscar(params);
        setPagina(resultado);
      } catch (e) {
        // Na atualização em segundo plano, uma falha não interrompe quem está usando a tela.
        if (silencioso) return;
        const mensagem = (e as ApiError).message;
        // Não deixa na tela linhas de outro filtro, que não batem com o que está selecionado.
        setPagina(null);
        setErro(mensagem);
        toast.erro(mensagem);
      } finally {
        if (!silencioso) setCarregando(false);
      }
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [buscar, filtros, paginaAtual, tamanhoPagina, ordenarPor]
  );
  const recarregar = useCallback(() => carregar(false), [carregar]);
  /** Atualiza os dados sem mostrar carregamento (ex.: chegou aviso novo). */
  const atualizarEmSilencio = useCallback(() => carregar(true), [carregar]);

  useEffect(() => {
    recarregar();
  }, [recarregar]);

  const aplicarFiltros = useCallback((novosFiltros: Filtros) => {
    setFiltros(novosFiltros);
    setPaginaAtual(0);
  }, []);

  return {
    pagina,
    linhas: pagina?.conteudo ?? [],
    carregando,
    erro,
    filtros,
    aplicarFiltros,
    paginaAtual,
    setPaginaAtual,
    recarregar,
    atualizarEmSilencio,
  };
}
