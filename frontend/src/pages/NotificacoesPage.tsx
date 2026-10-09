import { useCallback } from "react";
import { IconSend } from "@tabler/icons-react";
import { notificacoesApi } from "@/api/notificacoes";
import type { ApiError } from "@/api/client";
import { usePaginatedList } from "@/hooks/usePaginatedList";
import { useUsuariosOptions } from "@/hooks/useUsuariosOptions";
import { useToast } from "@/context/ToastContext";
import PageHeader from "@/components/common/PageHeader";
import DataTable from "@/components/common/DataTable";
import Pagination from "@/components/common/Pagination";
import Pill from "@/components/common/Pill";
import FiltrosCard from "@/components/common/FiltrosCard";
import { SelectField } from "@/components/common/fields";
import { Button } from "@/components/ui/button";
import { STATUS_ENVIO, CORES_STATUS_ENVIO, rotularEnum } from "@/utils/domain";
import { formatarDataHora } from "@/utils/format";
import type { Notificacao, Params } from "@/types";

export default function NotificacoesPage() {
  const toast = useToast();
  const { usuarios } = useUsuariosOptions();
  const buscar = useCallback((params: Params) => notificacoesApi.listar(params), []);
  const { pagina, linhas, carregando, filtros, aplicarFiltros, setPaginaAtual, recarregar } = usePaginatedList(buscar, {
    ordenarPor: "criadoEm,desc",
  });

  async function marcarComoEnviada(n: Notificacao) {
    try {
      await notificacoesApi.marcarComoEnviada(n.id);
      toast.sucesso("Notificação marcada como enviada.");
      recarregar();
    } catch (e) {
      toast.erro((e as ApiError).message);
    }
  }

  return (
    <>
      <PageHeader
        titulo="Notificações"
        subtitulo="Fila e histórico de notificações enviadas aos envolvidos nas reuniões."
      />

      <FiltrosCard>
        <SelectField
          label="Usuário"
          className="w-full sm:w-64"
          placeholder="Todos"
          options={usuarios.map((u) => ({ valor: u.id, rotulo: u.nome }))}
          value={filtros.usuarioId || ""}
          onChange={(e) => aplicarFiltros({ ...filtros, usuarioId: e.target.value })}
        />
        <SelectField
          label="Status de envio"
          className="w-full sm:w-48"
          placeholder="Todos"
          options={STATUS_ENVIO.map((s) => ({ valor: s, rotulo: rotularEnum(s) }))}
          value={filtros.status || ""}
          onChange={(e) => aplicarFiltros({ ...filtros, status: e.target.value })}
        />
      </FiltrosCard>

      <DataTable
        carregando={carregando}
        chaveLinha={(n) => n.id}
        linhas={linhas}
        colunas={[
          { chave: "mensagem", titulo: "Mensagem", render: (n) => (
              <div>
                <p className="font-medium">{n.mensagem}</p>
                <p className="text-xs text-muted-foreground">
                  {rotularEnum(n.tipoEvento)} · {rotularEnum(n.canalEnvio)} · agendamento #{n.idAgendamento}
                </p>
              </div>
            ) },
          { chave: "status", titulo: "Status", render: (n) => (
              <Pill className={CORES_STATUS_ENVIO[n.statusEnvio]}>{rotularEnum(n.statusEnvio)}</Pill>
            ) },
          { chave: "criadoEm", titulo: "Criada em", render: (n) => formatarDataHora(n.criadoEm) },
        ]}
        acoes={(n) =>
          n.statusEnvio === "PENDENTE" && (
            <Button variant="ghost" size="sm" onClick={() => marcarComoEnviada(n)}>
              <IconSend />
              Marcar como enviada
            </Button>
          )
        }
      />
      <Pagination pagina={pagina} onMudarPagina={setPaginaAtual} />
    </>
  );
}
