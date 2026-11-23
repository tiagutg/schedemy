import { useCallback, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  IconBell,
  IconCalendarCheck,
  IconCalendarRepeat,
  IconCalendarX,
  IconCheck,
  IconChecks,
  IconClockHour4,
  IconMail,
  IconPencil,
  IconStar,
  type Icon,
} from "@tabler/icons-react";
import { notificacoesApi } from "@/api/notificacoes";
import type { ApiError } from "@/api/client";
import { usePaginatedList } from "@/hooks/usePaginatedList";
import { useAoAtualizarDados } from "@/hooks/useAoAtualizarDados";
import { useUsuariosOptions } from "@/hooks/useUsuariosOptions";
import { useToast } from "@/context/ToastContext";
import { useAuth } from "@/context/AuthContext";
import { useAvisos } from "@/context/AvisosContext";
import PageHeader from "@/components/common/PageHeader";
import Pagination from "@/components/common/Pagination";
import EmptyState from "@/components/common/EmptyState";
import Spinner from "@/components/common/Spinner";
import FiltrosCard from "@/components/common/FiltrosCard";
import { SelectField } from "@/components/common/fields";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { formatarDataHora } from "@/utils/format";
import type { Notificacao, Params } from "@/types";

// Ícone de cada tipo de aviso gerado pelo back-end (Notificador.java).
const ICONES: Record<string, Icon> = {
  CONVITE: IconMail,
  REMARCACAO: IconCalendarRepeat,
  AGENDADO_POR_TERCEIRO: IconMail,
  ALTERACAO: IconPencil,
  RESPOSTA: IconChecks,
  CONFIRMACAO: IconCalendarCheck,
  CANCELAMENTO: IconCalendarX,
  CANCELAMENTO_AGENDA: IconCalendarX,
  LEMBRETE: IconClockHour4,
  AVALIACAO_PENDENTE: IconStar,
  // RF 21/22: pedido de alteração e a decisão do docente.
  SOLICITACAO_EDICAO: IconPencil,
  SOLICITACAO_ACEITA: IconCalendarCheck,
  SOLICITACAO_RECUSADA: IconCalendarX,
  SOLICITACAO_ENCERRADA: IconCalendarX,
};

const FILTROS_LEITURA = [
  { valor: "PENDENTE", rotulo: "Não lidas" },
  { valor: "ENVIADO", rotulo: "Lidas" },
];

/**
 * RF 23: avisos do sistema sobre as reuniões de cada pessoa. A administração
 * também consulta os avisos de qualquer usuário.
 */
export default function NotificacoesPage() {
  const toast = useToast();
  const navigate = useNavigate();
  const { perfilVinculado: eu, pode } = useAuth();
  const { atualizar: atualizarContador } = useAvisos();
  const verDeTodos = pode("usuarios.gerenciar");
  const { usuarios } = useUsuariosOptions();
  const [marcandoTodas, setMarcandoTodas] = useState(false);

  const buscar = useCallback(
    (params: Params) => notificacoesApi.listar({ ...params, usuarioId: params.usuarioId || (verDeTodos ? "" : eu?.id) }),
    [eu, verDeTodos]
  );
  const { pagina, linhas, carregando, filtros, aplicarFiltros, setPaginaAtual, recarregar, atualizarEmSilencio } =
    usePaginatedList(buscar, {
      ordenarPor: "criadoEm,desc",
      filtrosIniciais: verDeTodos ? {} : { usuarioId: String(eu?.id ?? "") },
    });
  // Aviso novo aparece na lista sem recarregar a página.
  useAoAtualizarDados(atualizarEmSilencio);
  const minhas = !verDeTodos || filtros.usuarioId === String(eu?.id);

  const podeMarcar = (n: Notificacao) => n.statusEnvio === "PENDENTE" && n.idUsuario === eu?.id;

  async function abrir(n: Notificacao) {
    if (podeMarcar(n)) {
      try {
        await notificacoesApi.marcarComoLida(n.id);
        atualizarContador();
      } catch {
        /* abrir a reunião é o que importa; a marcação pode ser refeita */
      }
    }
    // RF 06/RF 28: reunião cancelada porque a agenda do docente mudou abre direto o "Agendar de novo".
    if (n.idAgendamento && n.tipoEvento === "CANCELAMENTO_AGENDA") navigate(`/agendamentos?reagendar=${n.idAgendamento}`);
    else if (n.idAgendamento) navigate(`/agendamentos?abrir=${n.idAgendamento}`);
    else recarregar();
  }

  // Marca uma só, sem abrir a reunião.
  async function marcarComoLida(n: Notificacao) {
    try {
      await notificacoesApi.marcarComoLida(n.id);
      atualizarContador();
      recarregar();
    } catch (e) {
      toast.erro((e as ApiError).message);
    }
  }

  async function marcarTodas() {
    setMarcandoTodas(true);
    try {
      const { marcadas } = await notificacoesApi.marcarTodasComoLidas();
      toast.sucesso(marcadas ? "Notificações marcadas como lidas." : "Não havia notificações por ler.");
      atualizarContador();
      recarregar();
    } catch (e) {
      toast.erro((e as ApiError).message);
    } finally {
      setMarcandoTodas(false);
    }
  }

  return (
    <>
      <PageHeader
        titulo="Notificações"
        subtitulo="Convites, respostas, cancelamentos e lembretes das suas reuniões."
        acao={
          minhas ? (
            <Button variant="outline" onClick={marcarTodas} disabled={marcandoTodas}>
              <IconChecks />
              Marcar todas como lidas
            </Button>
          ) : null
        }
      />

      <FiltrosCard>
        {verDeTodos && (
          <SelectField
            label="Usuário"
            className="w-full sm:w-64"
            placeholder="Todos"
            options={usuarios.map((u) => ({ valor: u.id, rotulo: u.nome }))}
            value={filtros.usuarioId || ""}
            onChange={(e) => aplicarFiltros({ ...filtros, usuarioId: e.target.value })}
          />
        )}
        <SelectField
          label="Mostrar"
          className="w-full sm:w-48"
          placeholder="Todas"
          options={FILTROS_LEITURA}
          value={filtros.status || ""}
          onChange={(e) => aplicarFiltros({ ...filtros, status: e.target.value })}
        />
      </FiltrosCard>

      {carregando ? (
        <div className="flex h-24 items-center justify-center rounded-lg border text-muted-foreground">
          <Spinner />
        </div>
      ) : linhas.length === 0 ? (
        <div className="rounded-lg border">
          <EmptyState
            titulo={filtros.status === "PENDENTE" ? "Tudo em dia" : "Nenhuma notificação"}
            descricao="Os avisos aparecem aqui quando alguém convida você, responde, altera ou cancela uma reunião."
          />
        </div>
      ) : (
        <ul className="divide-y overflow-hidden rounded-lg border">
          {linhas.map((n) => {
            const Icone = ICONES[n.tipoEvento] ?? IconBell;
            const naoLida = n.statusEnvio === "PENDENTE";
            return (
              <li
                key={n.id}
                className={cn(
                  "flex items-center transition-colors hover:bg-muted/60",
                  naoLida && "bg-brand-50 hover:bg-brand-100/70 dark:bg-brand-100/40"
                )}
              >
                <button
                  type="button"
                  onClick={() => abrir(n)}
                  className="flex min-w-0 flex-1 items-start gap-3 px-4 py-3 text-left focus-visible:bg-muted/50 focus-visible:outline-none"
                >
                  <span
                    className={cn(
                      "mt-0.5 flex size-8 shrink-0 items-center justify-center rounded-full",
                      naoLida ? "bg-brand-600 text-white" : "bg-muted text-muted-foreground"
                    )}
                  >
                    <Icone className="size-4" aria-hidden="true" />
                  </span>
                  <span className="min-w-0 flex-1">
                    <span className={cn("block text-sm", naoLida && "font-medium")}>{n.mensagem}</span>
                    <span className="mt-0.5 block text-xs text-muted-foreground">
                      {formatarDataHora(n.criadoEm)}
                      {!minhas && ` · para ${n.nomeUsuario}`}
                      {naoLida && <span className="sr-only"> · não lida</span>}
                    </span>
                  </span>
                </button>
                {naoLida && (
                  // À direita: o botão de marcar como lida (círculo) e, ao lado, a bolinha de não lida.
                  <div className="flex shrink-0 items-center gap-3 pr-4">
                    {podeMarcar(n) && (
                      <button
                        type="button"
                        title="Marcar como lida"
                        aria-label={`Marcar como lida: ${n.mensagem}`}
                        onClick={() => marcarComoLida(n)}
                        className="flex size-8 items-center justify-center rounded-full border border-brand-300 bg-background text-brand-700 transition-colors hover:border-brand-600 hover:bg-brand-600 hover:text-white focus-visible:ring-2 focus-visible:ring-ring focus-visible:outline-none"
                      >
                        <IconCheck className="size-4" aria-hidden="true" />
                      </button>
                    )}
                    <span aria-hidden="true" className="size-2.5 rounded-full bg-brand-600" />
                  </div>
                )}
              </li>
            );
          })}
        </ul>
      )}
      <Pagination pagina={pagina} onMudarPagina={setPaginaAtual} />
    </>
  );
}
