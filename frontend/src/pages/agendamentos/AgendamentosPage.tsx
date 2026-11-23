import { useCallback, useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { IconCalendarWeek, IconEye, IconList, IconPlus } from "@tabler/icons-react";
import { agendamentosApi } from "@/api/agendamentos";
import type { ApiError } from "@/api/client";
import { usePaginatedList } from "@/hooks/usePaginatedList";
import { useAoAtualizarDados } from "@/hooks/useAoAtualizarDados";
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
import ComboboxField from "@/components/common/ComboboxField";
import { Button } from "@/components/ui/button";
import { CORES_TIPO_USUARIO, TIPOS_PARTICIPANTES, rotularEnum } from "@/utils/domain";
import { formatarHora, hojeISO } from "@/utils/format";
import { FILTROS_STATUS, STATUS_ATIVOS, descreverEnvolvidos, rotuloGrupoDoDia, situacaoDaReuniao } from "@/utils/reuniao";
import type { Agendamento, Params } from "@/types";
import NovoAgendamentoModal, { type ModoAgendamento } from "./NovoAgendamentoModal";
import AgendamentoDetailModal from "./AgendamentoDetailModal";
import AgendaSemanal from "./AgendaSemanal";

type Visao = "lista" | "semana";

// Período da lista: as próximas (só as que ainda vão acontecer) da mais perto para a mais longe;
// as anteriores e "todas", da mais recente para trás.
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
  // Recepção e administração não participam de reuniões (RN 03): não há o que filtrar por elas.
  const participantes = usuarios.filter((u) => TIPOS_PARTICIPANTES.includes(u.tipoUsuario));

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
        // Concluídas e canceladas não são "próximas"; um status escolhido no filtro tem prioridade.
        resto.status ||= STATUS_ATIVOS;
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
  const { pagina, linhas, carregando, filtros, aplicarFiltros, setPaginaAtual, recarregar, atualizarEmSilencio } =
    usePaginatedList(buscar, {
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
    const mensagens: Record<ModoAgendamento, string> = {
      criar: "Agendamento criado com sucesso!",
      replicar: "Agendamento criado com sucesso!",
      editar: "Agendamento atualizado.",
      remarcar: "Reunião remarcada.",
      solicitar: "Pedido de alteração enviado ao docente.",
    };
    toast.sucesso(mensagens[alteracao?.modo ?? "editar"]);
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

  // RF 06/RF 28: o aviso de reunião cancelada por mudança na agenda abre direto o "Agendar de novo"
  // (?reagendar=<id>). Quem não pode agendar de novo a partir dela vê só o detalhe.
  const idParaReagendar = searchParams.get("reagendar");
  useEffect(() => {
    if (!idParaReagendar) return;
    searchParams.delete("reagendar");
    setSearchParams(searchParams, { replace: true });
    agendamentosApi
      .buscarPorId(idParaReagendar)
      .then((base) => {
        // Mesma regra do botão "Agendar de novo" no detalhe.
        const participo = base.participantes.some((p) => p.idUsuario === eu?.id);
        const podeReplicar =
          pode("agendamentos.criar") &&
          (participo || base.idOrganizador === eu?.id || pode("agendamentos.organizarPorOutros"));
        if (podeReplicar) setAlteracao({ modo: "replicar", base });
        else setDetalhe(base);
      })
      .catch((e) => toast.erro((e as ApiError).message));
  }, [idParaReagendar, searchParams, setSearchParams, toast, eu?.id, pode]);

  // Mudanças feitas por outras pessoas (aceites, recusas, cancelamentos…) aparecem sem recarregar a página.
  useAoAtualizarDados(() => {
    atualizarEmSilencio();
    setVersaoSemana((v) => v + 1);
    const aberto = detalhe?.id;
    if (aberto == null) return;
    agendamentosApi
      .buscarPorId(aberto)
      .then((novo) => setDetalhe((atual) => (atual?.id === novo.id ? novo : atual)))
      .catch(() => {
        /* silencioso: segue mostrando o que já estava na tela */
      });
  });

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
          <ComboboxField
            label="Participante"
            className="w-full sm:w-64"
            placeholder="Todas as pessoas"
            limpavel
            options={participantes.map((u) => ({
              valor: u.id,
              rotulo: u.nome,
              busca: u.matriculaRa ?? "",
              detalhe: <Pill className={CORES_TIPO_USUARIO[u.tipoUsuario]}>{rotularEnum(u.tipoUsuario)}</Pill>,
            }))}
            value={filtros.usuarioId || ""}
            onChange={(valor) => aplicarFiltros({ ...filtros, usuarioId: valor })}
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
            grupo={(a) => rotuloGrupoDoDia(a.dataReuniao)}
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
                    {/* A data está no cabeçalho do grupo do dia. */}
                    <p className="tabular-nums">
                      {formatarHora(a.horarioInicio)}
                      {a.horarioFim ? `–${formatarHora(a.horarioFim)}` : ""}
                    </p>
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
