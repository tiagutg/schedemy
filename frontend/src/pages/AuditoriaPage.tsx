import { useCallback, useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { IconExternalLink } from "@tabler/icons-react";
import { auditoriaApi } from "@/api/auditoria";
import { usuariosApi } from "@/api/usuarios";
import { usePaginatedList } from "@/hooks/usePaginatedList";
import PageHeader from "@/components/common/PageHeader";
import DataTable from "@/components/common/DataTable";
import Pagination from "@/components/common/Pagination";
import FiltrosCard from "@/components/common/FiltrosCard";
import Pill from "@/components/common/Pill";
import { SelectField, TextField } from "@/components/common/fields";
import { Button } from "@/components/ui/button";
import { ACOES_AUDITORIA, TABELAS_AUDITORIA } from "@/utils/domain";
import { formatarDataHora } from "@/utils/format";
import type { Filtros, LogAuditoria, Params, UsuarioResumo } from "@/types";

const SISTEMA = "sistema";

/** Filtros que a tela aceita no endereço (ex.: o detalhe da reunião abre ?tabela=agendamento&idRegistro=42). */
const CHAVES_FILTRO = ["autor", "acao", "tabela", "idRegistro", "dataInicio", "dataFim"] as const;

function rotuloTabela(tabela: string) {
  return TABELAS_AUDITORIA.find((t) => t.valor === tabela)?.rotulo ?? tabela;
}

function corDaAcao(acao: string) {
  if (/CANCELADO|REMOVID|RECUSAD|RECUSADO|EXPIRAD|DESATIVADO/.test(acao)) return "bg-brick-100 text-brick-700";
  if (/CRIAD|ACEITO|ATIVADO/.test(acao)) return "bg-sage-100 text-sage-700";
  if (acao.startsWith("LOGIN") || acao === "TERMO_ACEITO") return "bg-ink-100 text-ink-800";
  return "bg-brand-100 text-brand-700";
}

/** RNF 19: trilha de auditoria (quem fez o quê, quando e de onde). Só administração. */
export default function AuditoriaPage() {
  const [searchParams] = useSearchParams();
  const [filtrosIniciais] = useState<Filtros>(() =>
    Object.fromEntries(CHAVES_FILTRO.map((c) => [c, searchParams.get(c) ?? undefined]).filter(([, v]) => v))
  );
  const [usuarios, setUsuarios] = useState<UsuarioResumo[]>([]);

  useEffect(() => {
    usuariosApi
      .listarResumo({ size: 500, sort: "nome,asc" })
      .then((p) => setUsuarios(p.conteudo))
      .catch(() => setUsuarios([]));
  }, []);

  const buscar = useCallback((params: Params) => {
    const { autor, ...resto } = params as Params & { autor?: string };
    return auditoriaApi.listar({
      ...resto,
      idUsuario: autor && autor !== SISTEMA ? autor : undefined,
      somenteSistema: autor === SISTEMA ? true : undefined,
    });
  }, []);
  const { pagina, linhas, carregando, filtros, aplicarFiltros, setPaginaAtual } = usePaginatedList(buscar, {
    tamanhoPagina: 25,
    filtrosIniciais,
  });
  const mudar = (chave: string, valor: string) => aplicarFiltros({ ...filtros, [chave]: valor || undefined });

  return (
    <>
      <PageHeader
        titulo="Auditoria"
        subtitulo="Quem fez o quê, quando e de onde: reuniões, pedidos de alteração, agenda dos docentes, cadastros, acessos e aceite do termo."
      />

      <FiltrosCard>
        <SelectField
          label="Autor"
          className="w-full sm:w-56"
          placeholder="Todos"
          options={[
            { valor: SISTEMA, rotulo: "Sistema (automático)" },
            ...usuarios.map((u) => ({ valor: String(u.id), rotulo: u.nome })),
          ]}
          value={filtros.autor || ""}
          onChange={(e) => mudar("autor", e.target.value)}
        />
        <SelectField
          label="Ação"
          className="w-full sm:w-56"
          placeholder="Todas"
          options={Object.entries(ACOES_AUDITORIA).map(([valor, rotulo]) => ({ valor, rotulo }))}
          value={filtros.acao || ""}
          onChange={(e) => mudar("acao", e.target.value)}
        />
        <SelectField
          label="Registro"
          className="w-full sm:w-48"
          placeholder="Todos"
          options={TABELAS_AUDITORIA}
          value={filtros.tabela || ""}
          onChange={(e) => mudar("tabela", e.target.value)}
        />
        <TextField
          label="Nº do registro"
          type="number"
          min={1}
          className="w-full sm:w-32"
          value={filtros.idRegistro || ""}
          onChange={(e) => mudar("idRegistro", e.target.value)}
        />
        <TextField
          label="De"
          type="date"
          className="w-full sm:w-40"
          value={filtros.dataInicio || ""}
          onChange={(e) => mudar("dataInicio", e.target.value)}
        />
        <TextField
          label="Até"
          type="date"
          className="w-full sm:w-40"
          value={filtros.dataFim || ""}
          onChange={(e) => mudar("dataFim", e.target.value)}
        />
        {Object.values(filtros).some(Boolean) && (
          <Button variant="ghost" className="self-end" onClick={() => aplicarFiltros({})}>
            Limpar filtros
          </Button>
        )}
      </FiltrosCard>

      <DataTable<LogAuditoria>
        carregando={carregando}
        chaveLinha={(l) => l.id}
        linhas={linhas}
        linhaVazia="Nenhuma ação registrada com esses filtros."
        colunas={[
          { chave: "criadoEm", titulo: "Quando", render: (l) => <span className="whitespace-nowrap">{formatarDataHora(l.criadoEm)}</span> },
          {
            chave: "autor",
            titulo: "Autor",
            render: (l) =>
              l.idUsuario ? (
                <div>
                  <p className="font-medium">{l.nomeUsuario}</p>
                  <p className="text-xs text-muted-foreground">{l.tipoUsuario}</p>
                </div>
              ) : (
                <span className="text-muted-foreground italic">Sistema</span>
              ),
          },
          {
            chave: "acao",
            titulo: "Ação",
            render: (l) => <Pill className={corDaAcao(l.acao)}>{ACOES_AUDITORIA[l.acao] ?? l.acao}</Pill>,
          },
          {
            chave: "registro",
            titulo: "Registro",
            render: (l) => (
              <span className="whitespace-nowrap">
                {rotuloTabela(l.tabelaAfetada)}
                {l.idRegistro != null && ` nº ${l.idRegistro}`}
                {l.tabelaAfetada === "agendamento" && l.idRegistro != null && l.acao !== "AGENDAMENTO_REMOVIDO" && (
                  <Link
                    to={`/agendamentos?abrir=${l.idRegistro}`}
                    className="ml-1 inline-flex align-middle text-primary"
                    aria-label={`Abrir a reunião ${l.idRegistro}`}
                  >
                    <IconExternalLink className="size-3.5" />
                  </Link>
                )}
              </span>
            ),
          },
          { chave: "detalhes", titulo: "Detalhes", render: (l) => <span className="text-sm">{l.detalhes || "—"}</span> },
          { chave: "ip", titulo: "IP", render: (l) => <span className="font-mono text-xs">{l.ipOrigem || "—"}</span> },
        ]}
      />
      <Pagination pagina={pagina} onMudarPagina={setPaginaAtual} />
    </>
  );
}
