import { useCallback, useState } from "react";
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
import FiltrosCard from "@/components/common/FiltrosCard";
import { SelectField, TextField } from "@/components/common/fields";
import { Button } from "@/components/ui/button";
import { STATUS_AGENDAMENTO, CORES_STATUS_AGENDAMENTO, rotularEnum } from "@/utils/domain";
import { formatarData, formatarHora } from "@/utils/format";
import type { Agendamento, Params } from "@/types";
import NovoAgendamentoModal, { type ModoAgendamento } from "./NovoAgendamentoModal";
import AgendamentoDetailModal from "./AgendamentoDetailModal";
import AgendaSemanal from "./AgendaSemanal";

type Visao = "lista" | "semana";
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
      const { usuarioId, ...resto } = params;
      const alvo = meuId ?? usuarioId;
      return alvo
        ? agendamentosApi.listarPorUsuario(alvo as string, resto)
        : agendamentosApi.listarTodos(resto);
    },
    [meuId]
  );
  const { pagina, linhas, carregando, filtros, aplicarFiltros, setPaginaAtual, recarregar } = usePaginatedList(buscar, {
    ordenarPor: "dataReuniao,desc",
  });

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
      buscar({ usuarioId: filtros.usuarioId, status: filtros.status, dataInicio, dataFim, page: 0, size: 200 }).then(
        (p) => p.conteudo
      ),
    [buscar, filtros.usuarioId, filtros.status]
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
          options={STATUS_AGENDAMENTO.map((s) => ({ valor: s, rotulo: rotularEnum(s) }))}
          value={filtros.status || ""}
          onChange={(e) => aplicarFiltros({ ...filtros, status: e.target.value })}
        />
        {visao === "lista" && (
          <>
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
          versao={`${versaoSemana}|${filtros.usuarioId ?? ""}|${filtros.status ?? ""}`}
        />
      ) : (
        <>
          <DataTable
            carregando={carregando}
            chaveLinha={(a) => a.id}
            linhas={linhas}
            colunas={[
              { chave: "topico", titulo: "Tópico", render: (a) => (
                  <div>
                    <p className="font-medium">{a.topico}</p>
                    <p className="text-xs text-muted-foreground">{a.nomeOrganizador}</p>
                  </div>
                ) },
              { chave: "data", titulo: "Data / hora", render: (a) => `${formatarData(a.dataReuniao)} · ${formatarHora(a.horarioInicio)}` },
              { chave: "formato", titulo: "Formato", render: (a) => rotularEnum(a.formato) },
              { chave: "status", titulo: "Status", render: (a) => (
                  <Pill className={CORES_STATUS_AGENDAMENTO[a.status]}>{rotularEnum(a.status)}</Pill>
                ) },
            ]}
            acoes={(a) => (
              <Button variant="ghost" size="sm" onClick={() => abrirDetalhe(a)}>
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
