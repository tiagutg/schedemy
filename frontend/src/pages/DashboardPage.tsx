import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import {
  IconArrowRight,
  IconCalendarCheck,
  IconCalendarX,
  IconCircleCheckFilled,
  IconClockHour4,
  IconLoader,
  IconPlus,
  IconUserPlus,
  type Icon,
} from "@tabler/icons-react";
import { agendamentosApi } from "@/api/agendamentos";
import { usuariosApi } from "@/api/usuarios";
import { useAuth } from "@/context/AuthContext";
import PageHeader from "@/components/common/PageHeader";
import Spinner from "@/components/common/Spinner";
import Pill from "@/components/common/Pill";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardAction,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { CORES_STATUS_AGENDAMENTO, rotularEnum } from "@/utils/domain";
import { formatarData, formatarHora } from "@/utils/format";
import type { Agendamento } from "@/types";

const STATUS_RESUMO = ["PENDENTE", "AGUARDANDO_RESPOSTA", "CONFIRMADO", "CONCLUIDO", "CANCELADO"];

// Ícone e legenda de cada cartão de resumo (apenas apresentação).
const VISUAL_STATUS: Record<string, { icone: Icon; legenda: string }> = {
  PENDENTE: { icone: IconLoader, legenda: "Aguardando análise" },
  AGUARDANDO_RESPOSTA: { icone: IconClockHour4, legenda: "Convites sem resposta" },
  CONFIRMADO: { icone: IconCalendarCheck, legenda: "Reuniões confirmadas" },
  CONCLUIDO: { icone: IconCircleCheckFilled, legenda: "Reuniões realizadas" },
  CANCELADO: { icone: IconCalendarX, legenda: "Reuniões canceladas" },
};

export default function DashboardPage() {
  const { perfilVinculado, pode } = useAuth();
  const [contagens, setContagens] = useState<Record<string, number> | null>(null);
  const [totalUsuarios, setTotalUsuarios] = useState<number | null>(null);
  const [proximos, setProximos] = useState<Agendamento[]>([]);
  const [carregando, setCarregando] = useState(true);

  useEffect(() => {
    let cancelado = false;
    async function carregar() {
      setCarregando(true);
      try {
        const [porStatus, usuariosPagina, agendamentosPagina] = await Promise.all([
          Promise.all(
            STATUS_RESUMO.map((status) =>
              agendamentosApi
                .listarTodos({ status, page: 0, size: 1 })
                .then((p) => [status, p.totalElementos] as const)
            )
          ),
          usuariosApi.listar({ page: 0, size: 1 }),
          agendamentosApi.listarTodos({ page: 0, size: 6, sort: "dataReuniao,asc" }),
        ]);
        if (cancelado) return;
        setContagens(Object.fromEntries(porStatus));
        setTotalUsuarios(usuariosPagina.totalElementos);
        setProximos(agendamentosPagina.conteudo);
      } catch {
        // painel é apenas informativo — erros de rede já aparecem em outras telas
      } finally {
        if (!cancelado) setCarregando(false);
      }
    }
    carregar();
    return () => {
      cancelado = true;
    };
  }, []);

  return (
    <>
      <PageHeader
        titulo={`Olá${perfilVinculado?.nome ? ", " + perfilVinculado.nome.split(" ")[0] : ""}!`}
        subtitulo="Visão geral dos agendamentos e cadastros do Schedemy."
      />

      {carregando ? (
        <div className="flex justify-center py-16 text-muted-foreground">
          <Spinner />
        </div>
      ) : (
        <>
          {/* 2 colunas no celular (o último cartão ocupa a linha), 3 em telas médias e 5 nas largas */}
          <div className="grid grid-cols-2 gap-3 *:data-[slot=card]:bg-gradient-to-t *:data-[slot=card]:from-primary/5 *:data-[slot=card]:to-card *:data-[slot=card]:shadow-xs sm:gap-4 @5xl/main:grid-cols-3 @7xl/main:grid-cols-5">
            {STATUS_RESUMO.map((status) => {
              const { icone: Icone, legenda } = VISUAL_STATUS[status];
              return (
                <Card key={status} className="@container/card last:col-span-2 @5xl/main:last:col-span-1">
                  {/* Sem o rodapé (cartão estreito), o espaçamento inferior fica no cabeçalho */}
                  <CardHeader className="grid-cols-1! pb-(--card-spacing) @[220px]/card:grid-cols-[1fr_auto]! @[220px]/card:pb-0">
                    <CardDescription>{rotularEnum(status)}</CardDescription>
                    <CardTitle className="text-2xl font-semibold tabular-nums @[250px]/card:text-3xl">
                      {contagens?.[status] ?? "—"}
                    </CardTitle>
                    <CardAction className="hidden @[220px]/card:block">
                      <Badge variant="outline">
                        <Icone />
                        Status
                      </Badge>
                    </CardAction>
                  </CardHeader>
                  <CardFooter className="hidden flex-col items-start gap-1.5 border-t-0 bg-transparent pt-0 text-sm @[220px]/card:flex">
                    <div className="line-clamp-1 flex gap-2 font-medium">{legenda}</div>
                    <div className="text-muted-foreground">Total de agendamentos neste status</div>
                  </CardFooter>
                </Card>
              );
            })}
          </div>

          <div className="grid grid-cols-1 gap-4 md:gap-6 @5xl/main:grid-cols-3">
            <Card className="@5xl/main:col-span-2">
              <CardHeader>
                <CardTitle>Próximos agendamentos</CardTitle>
                <CardDescription>As próximas reuniões pela data marcada</CardDescription>
                {pode("agendamentos.ver") && (
                  <CardAction>
                    <Button asChild variant="outline" size="sm">
                      <Link to="/agendamentos">
                        Ver todos
                        <IconArrowRight />
                      </Link>
                    </Button>
                  </CardAction>
                )}
              </CardHeader>
              <CardContent className="px-0">
                {proximos.length === 0 ? (
                  <p className="px-4 py-8 text-sm text-muted-foreground">Nenhum agendamento cadastrado ainda.</p>
                ) : (
                  <ul className="divide-y border-t">
                    {proximos.map((a) => (
                      <li key={a.id} className="flex items-center justify-between gap-3 px-4 py-3">
                        <div className="min-w-0">
                          <p className="truncate text-sm font-medium">{a.topico}</p>
                          <p className="text-xs text-muted-foreground">
                            {formatarData(a.dataReuniao)} às {formatarHora(a.horarioInicio)} · {a.nomeOrganizador}
                          </p>
                        </div>
                        <Pill className={CORES_STATUS_AGENDAMENTO[a.status]}>{rotularEnum(a.status)}</Pill>
                      </li>
                    ))}
                  </ul>
                )}
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Ações rápidas</CardTitle>
                <CardDescription>Atalhos para os cadastros mais usados</CardDescription>
              </CardHeader>
              <CardContent className="flex flex-col gap-4">
                {pode("usuarios.gerenciar") && (
                  <div className="flex items-center justify-between rounded-lg border px-3 py-2.5 text-sm">
                    <span className="text-muted-foreground">Usuários cadastrados</span>
                    <span className="text-lg font-semibold tabular-nums">{totalUsuarios ?? "—"}</span>
                  </div>
                )}
                <div className="flex flex-wrap gap-2">
                  {pode("agendamentos.criar") && (
                    <Button asChild className="flex-1">
                      <Link to="/agendamentos?novo=1">
                        <IconPlus />
                        Agendamento
                      </Link>
                    </Button>
                  )}
                  {pode("usuarios.gerenciar") && (
                    <Button asChild variant="outline" className="flex-1">
                      <Link to="/usuarios">
                        <IconUserPlus />
                        Usuário
                      </Link>
                    </Button>
                  )}
                </div>
              </CardContent>
            </Card>
          </div>
        </>
      )}
    </>
  );
}
