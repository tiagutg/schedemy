import { useCallback, useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { IconCalendarWeek, IconEye, IconList, IconPlus } from "@tabler/icons-react";
import { agendamentosApi } from "@/api/agendamentos";
import type { ApiError } from "@/api/client";
import { usePaginatedList } from "@/hooks/usePaginatedList";
import { useUsuariosOptions } from "@/hooks/useUsuariosOptions";
import { useToast } from "@/context/ToastContext";
import { useAuth } from "@/context/AuthContext";
import PageHeader from "@/components/common/PageHeader";
import DataTable from "@/components/common/DataTable";
import Pagination from "@/components/common/Pagination";
import Pill from "@/components/common/Pill";
import EmptyState from "@/components/common/EmptyState";
import FiltrosCard from "@/components/common/FiltrosCard";
import { SelectField, TextField } from "@/components/common/fields";
import { Button } from "@/components/ui/button";
import { rotularEnum } from "@/utils/domain";
import { formatarData, formatarHora, hojeISO } from "@/utils/format";
import { FILTROS_STATUS, descreverEnvolvidos, situacaoDaReuniao } from "@/utils/reuniao";
import type { Agendamento, Params } from "@/types";
import NovoAgendamentoModal, { type ModoAgendamento } from "./NovoAgendamentoModal";
import AgendamentoDetailModal from "./AgendamentoDetailModal";
import AgendaSemanal from "./AgendaSemanal";

type Visao = "lista" | "semana";

// Período da lista: as próximas primeiro (a agenda do dia a dia), as anteriores da mais recente para trás.
const PERIODOS = [
  { valor: "proximas", rotulo: "Próximas" },
  { valor: "anteriores", rotulo: "Anteriores" },
  { valor: "todas", rotulo: "Todas" },
];
const ORDEM_CRESCENTE = ["dataReuniao,asc", "horarioInicio,asc"];
const ORDEM_DECRESCENTE = ["dataReuniao,desc", "horarioInicio,desc"];

function ontemISO(): string {
  const d = new Date();
  d.setDate(d.getDate() - 1);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}
const CHAVE_VISAO = "schedemy.agendamentos.visao";

function visaoSalva(): Visao {
  try {
    return localStorage.getItem(CHAVE_VISAO) === "semana" ? "semana" : "lista";
  } catch {
    return "lista";
  }
}

export default function AgendamentosPage() {
  const toast = useToast();
  const { pode, perfilVinculado: eu } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const { usuarios } = useUsuariosOptions();

  // Quem não acompanha a agenda geral vê só os agendamentos de que participa.
  const verTodos = pode("agendamentos.verTodos");
  const meuId = verTodos ? null : (eu?.id ?? null);

  const buscar = useCallback(
    (params: Params) => {
      const { usuarioId, periodo, ...resto } = params;
      const alvo = meuId ?? usuarioId;
      // O período só completa as datas que o usuário não escolheu.
      if (periodo === "proximas") {
        resto.dataInicio ||= hojeISO();
        resto.sort = ORDEM_CRESCENTE;
      } else if (periodo === "anteriores") {
        resto.dataFim ||= ontemISO();
        resto.sort = ORDEM_DECRESCENTE;
      } else if (periodo === "todas") {
        resto.sort = ORDEM_DECRESCENTE;
      }
      return alvo
        ? agendamentosApi.listarPorUsuario(alvo as string, resto)
        : agendamentosApi.listarTodos(resto);
    },
    [meuId]
  );
  const { pagina, linhas, carregando, filtros, aplicarFiltros, setPaginaAtual, recarregar } = usePaginatedList(buscar, {
    // Os cartões do painel abrem a lista já filtrada (?status=...).
    filtrosIniciais: {
      periodo: searchParams.get("status") ? "todas" : "proximas",
      status: searchParams.get("status") ?? undefined,
    },
  });

  // Busca por tópico ou nome, aplicada quando a pessoa para de digitar.
  const [textoBusca, setTextoBusca] = useState("");
  useEffect(() => {
    if ((filtros.busca ?? "") === textoBusca.trim()) return;
    const espera = setTimeout(() => aplicarFiltros({ ...filtros, busca: textoBusca.trim() }), 400);
    return () => clearTimeout(espera);
  }, [textoBusca, filtros, aplicarFiltros]);

  const [modalNovoAberto, setModalNovoAberto] = useState(
    searchParams.get("novo") === "1" && pode("agendamentos.criar")
  );
  // O link "?novo=1" (sidebar e painel) também abre o modal com a tela já montada.
  const abertoPorLink = searchParams.get("novo") === "1" && pode("agendamentos.criar");
  const [detalhe, setDetalhe] = useState<Agendamento | null>(null);
  const [visao, setVisao] = useState<Visao>(visaoSalva);
  // Recarrega a visão semanal depois de criar, editar ou responder.
  const [versaoSemana, setVersaoSemana] = useState(0);

  function trocarVisao(nova: Visao) {
    setVisao(nova);
    try {
      localStorage.setItem(CHAVE_VISAO, nova);
    } catch {
      /* preferência só desta sessão */
    }
  }

  const buscarSemana = useCallback(
    (dataInicio: string, dataFim: string) =>
      buscar({
        usuarioId: filtros.usuarioId,
        status: filtros.status,
        busca: filtros.busca,
        dataInicio,
        dataFim,
        page: 0,
        size: 200,
      }).then((p) => p.conteudo),
    [buscar, filtros.usuarioId, filtros.status, filtros.busca]
  );
  // Edição/remarcação abertas a partir do detalhe.
  const [alteracao, setAlteracao] = useState<{ modo: ModoAgendamento; base: Agendamento } | null>(null);

  // Cada abertura recria o formulário (key), para ele nascer limpo.
  const [aberturasNovo, setAberturasNovo] = useState(0);

  function abrirNovo() {
    setAberturasNovo((n) => n + 1);
    setModalNovoAberto(true);
  }
  function fecharNovo() {
    setModalNovoAberto(false);
    if (searchParams.get("novo")) {
      searchParams.delete("novo");
      setSearchParams(searchParams, { replace: true });
    }
  }
  function aoCriar() {
    toast.sucesso("Agendamento criado com sucesso!");
    fecharNovo();
    recarregar();
    setVersaoSemana((v) => v + 1);
  }
  function aoAlterar(salvo: Agendamento) {
    toast.sucesso(alteracao?.modo === "remarcar" ? "Reunião remarcada." : "Agendamento atualizado.");
    setAlteracao(null);
    setDetalhe(salvo);
    recarregar();
    setVersaoSemana((v) => v + 1);
  }

  async function abrirDetalhe(linha: Agendamento) {
    try {
      setDetalhe(await agendamentosApi.buscarPorId(linha.id));
    } catch (e) {
      toast.erro((e as ApiError).message);
    }
  }
  // Os avisos abrem a reunião direto (?abrir=<id>).
  const idParaAbrir = searchParams.get("abrir");
  useEffect(() => {
    if (!idParaAbrir) return;
    searchParams.delete("abrir");
    setSearchParams(searchParams, { replace: true });
    agendamentosApi
      .buscarPorId(idParaAbrir)
      .then(setDetalhe)
      .catch((e) => toast.erro((e as ApiError).message));
  }, [idParaAbrir, searchParams, setSearchParams, toast]);

  async function recarregarDetalhe() {
    if (!detalhe) return;
    try {
      setDetalhe(await agendamentosApi.buscarPorId(detalhe.id));
    } catch {
      /* silencioso: o modal principal já mostra erros via toast */
    }
    recarregar();
    setVersaoSemana((v) => v + 1);
  }

  return (
    <>
      <PageHeader
        titulo={verTodos ? "Agendamentos" : "Meus agendamentos"}
        subtitulo={
          verTodos
            ? "Solicitações e reuniões marcadas entre alunos, professores e coordenadores."
            : "Reuniões que você organizou ou para as quais foi convidado."
        }
        acao={
          pode("agendamentos.criar") ? (
            <Button onClick={abrirNovo}>
              <IconPlus />
              Novo agendamento
            </Button>
          ) : null
        }
      />

      <FiltrosCard>
        {verTodos && (
          <SelectField
            label="Usuário envolvido"
            className="w-full sm:w-64"
            placeholder="Todos os agendamentos"
            options={usuarios.map((u) => ({ valor: u.id, rotulo: u.nome }))}
            value={filtros.usuarioId || ""}
            onChange={(e) => aplicarFiltros({ ...filtros, usuarioId: e.target.value })}
          />
        )}
        <SelectField
          label="Status"
          className="w-full sm:w-52"
          placeholder="Todos os status"
          options={FILTROS_STATUS}
          value={filtros.status || ""}
          onChange={(e) => aplicarFiltros({ ...filtros, status: e.target.value })}
        />
        <TextField
          label="Buscar"
          type="search"
          className="w-full sm:w-56"
          placeholder="Tópico ou nome"
          value={textoBusca}
          onChange={(e) => setTextoBusca(e.target.value)}
        />
        {visao === "lista" && (
          <>
            <SelectField
              label="Período"
              className="w-full sm:w-36"
              options={PERIODOS}
              value={filtros.periodo || "todas"}
              onChange={(e) => aplicarFiltros({ ...filtros, periodo: e.target.value })}
            />
            <TextField
              label="De"
              type="date"
              className="min-w-0 flex-1 sm:w-auto sm:flex-none"
              value={filtros.dataInicio || ""}
              onChange={(e) => aplicarFiltros({ ...filtros, dataInicio: e.target.value })}
            />
            <TextField
              label="Até"
              type="date"
              className="min-w-0 flex-1 sm:w-auto sm:flex-none"
              value={filtros.dataFim || ""}
              onChange={(e) => aplicarFiltros({ ...filtros, dataFim: e.target.value })}
            />
          </>
        )}
        {/* No celular fica numa linha própria, para as datas não serem espremidas */}
        <div className="flex w-full flex-col gap-2 sm:ml-auto sm:w-auto">
          <span className="text-sm font-medium">Visualização</span>
          <div className="flex gap-1" role="group" aria-label="Visualização">
            <Button
              type="button"
              size="sm"
              variant={visao === "lista" ? "default" : "outline"}
              aria-pressed={visao === "lista"}
              onClick={() => trocarVisao("lista")}
            >
              <IconList />
              Lista
            </Button>
            <Button
              type="button"
              size="sm"
              variant={visao === "semana" ? "default" : "outline"}
              aria-pressed={visao === "semana"}
              onClick={() => trocarVisao("semana")}
            >
              <IconCalendarWeek />
              Semana
            </Button>
          </div>
        </div>
      </FiltrosCard>

      {visao === "semana" ? (
        <AgendaSemanal
          buscarSemana={buscarSemana}
          onAbrir={abrirDetalhe}
          versao={`${versaoSemana}|${filtros.usuarioId ?? ""}|${filtros.status ?? ""}|${filtros.busca ?? ""}`}
        />
      ) : (
        <>
          <DataTable
            carregando={carregando}
            chaveLinha={(a) => a.id}
            linhas={linhas}
            linhaVazia={
              <EmptyState
                titulo={filtros.periodo === "proximas" ? "Nenhuma reunião pela frente" : "Nenhuma reunião encontrada"}
                descricao={
                  filtros.periodo === "proximas"
                    ? "Para ver o histórico, troque o período para \"Anteriores\" ou \"Todas\"."
                    : "Ajuste os filtros para ver outras reuniões."
                }
              />
            }
            colunas={[
              { chave: "topico", titulo: "Reunião", render: (a) => (
                  <div className="min-w-0">
                    <p className="font-medium">{a.topico}</p>
                    <p className="text-xs text-muted-foreground">{descreverEnvolvidos(a, eu?.id)}</p>
                  </div>
                ) },
              { chave: "data", titulo: "Quando", render: (a) => (
                  <div>
                    <p className="tabular-nums">{`${formatarData(a.dataReuniao)} · ${formatarHora(a.horarioInicio)}`}</p>
                    <p className="text-xs text-muted-foreground">{rotularEnum(a.formato)}</p>
                  </div>
                ) },
              { chave: "status", titulo: "Situação", render: (a) => {
                  const situacao = situacaoDaReuniao(a, eu?.id);
                  return <Pill className={situacao.cor}>{situacao.rotulo}</Pill>;
                } },
            ]}
            aoClicarLinha={abrirDetalhe}
            ocultarAcoesNoCartao
            acoes={(a) => (
              <Button
                variant="ghost"
                size="sm"
                onClick={(e) => {
                  e.stopPropagation();
                  abrirDetalhe(a);
                }}
              >
                <IconEye />
                Ver detalhes
              </Button>
            )}
          />
          <Pagination pagina={pagina} onMudarPagina={setPaginaAtual} />
        </>
      )}

      <NovoAgendamentoModal
        key={`novo-${aberturasNovo}-${abertoPorLink}`}
        aberto={modalNovoAberto || abertoPorLink}
        onFechar={fecharNovo}
        onSalvo={aoCriar}
      />

      <NovoAgendamentoModal
        key={alteracao ? `${alteracao.modo}-${alteracao.base.id}` : "sem-alteracao"}
        aberto={!!alteracao}
        modo={alteracao?.modo}
        base={alteracao?.base}
        onFechar={() => setAlteracao(null)}
        onSalvo={aoAlterar}
      />

      <AgendamentoDetailModal
        key={detalhe?.id ?? "sem-detalhe"}
        agendamento={alteracao ? null : detalhe}
        onFechar={() => setDetalhe(null)}
        onAlterado={recarregarDetalhe}
        onAlterar={(modo, base) => setAlteracao({ modo, base })}
      />
    </>
  );
}
