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

  const recarregar = useCallback(async () => {
    setCarregando(true);
    setErro(null);
    try {
      const params: Params = { ...filtros, page: paginaAtual, size: tamanhoPagina };
      if (ordenarPor) params.sort = ordenarPor;
      const resultado = await buscar(params);
      setPagina(resultado);
    } catch (e) {
      const mensagem = (e as ApiError).message;
      setErro(mensagem);
      toast.erro(mensagem);
    } finally {
      setCarregando(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [buscar, filtros, paginaAtual, tamanhoPagina, ordenarPor]);

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
  };
}
