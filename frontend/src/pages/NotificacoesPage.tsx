import { useCallback, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  IconBell,
  IconCalendarCheck,
  IconCalendarRepeat,
  IconCalendarX,
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
  LEMBRETE: IconClockHour4,
  AVALIACAO_PENDENTE: IconStar,
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
  const { pagina, linhas, carregando, filtros, aplicarFiltros, setPaginaAtual, recarregar } = usePaginatedList(buscar, {
    ordenarPor: "criadoEm,desc",
    filtrosIniciais: verDeTodos ? {} : { usuarioId: String(eu?.id ?? "") },
  });
  const minhas = !verDeTodos || filtros.usuarioId === String(eu?.id);

  async function abrir(n: Notificacao) {
    if (n.statusEnvio === "PENDENTE" && n.idUsuario === eu?.id) {
      try {
        await notificacoesApi.marcarComoLida(n.id);
        atualizarContador();
      } catch {
        /* abrir a reunião é o que importa; a marcação pode ser refeita */
      }
    }
    if (n.idAgendamento) navigate(`/agendamentos?abrir=${n.idAgendamento}`);
    else recarregar();
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
              <li key={n.id}>
                <button
                  type="button"
                  onClick={() => abrir(n)}
                  className={cn(
                    "flex w-full items-start gap-3 px-4 py-3 text-left transition-colors hover:bg-muted/50 focus-visible:bg-muted/50 focus-visible:outline-none",
                    naoLida && "bg-brand-50 dark:bg-brand-100/40"
                  )}
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
                  {naoLida && <span aria-hidden="true" className="mt-2 size-2 shrink-0 rounded-full bg-brand-600" />}
                </button>
              </li>
            );
          })}
        </ul>
      )}
      <Pagination pagina={pagina} onMudarPagina={setPaginaAtual} />
    </>
  );
}
