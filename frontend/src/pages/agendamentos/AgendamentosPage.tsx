import { useCallback, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { IconEye, IconPlus } from "@tabler/icons-react";
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
  // Edição/remarcação abertas a partir do detalhe.
  const [alteracao, setAlteracao] = useState<{ modo: ModoAgendamento; base: Agendamento } | null>(null);

  function abrirNovo() {
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
  }
  function aoAlterar(salvo: Agendamento) {
    toast.sucesso(alteracao?.modo === "remarcar" ? "Reunião remarcada." : "Agendamento atualizado.");
    setAlteracao(null);
    setDetalhe(salvo);
    recarregar();
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
      </FiltrosCard>

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

      <NovoAgendamentoModal aberto={modalNovoAberto || abertoPorLink} onFechar={fecharNovo} onSalvo={aoCriar} />

      <NovoAgendamentoModal
        aberto={!!alteracao}
        modo={alteracao?.modo}
        base={alteracao?.base}
        onFechar={() => setAlteracao(null)}
        onSalvo={aoAlterar}
      />

      <AgendamentoDetailModal
        agendamento={alteracao ? null : detalhe}
        onFechar={() => setDetalhe(null)}
        onAlterado={recarregarDetalhe}
        onAlterar={(modo, base) => setAlteracao({ modo, base })}
      />
    </>
  );
}
