import { Link } from "react-router-dom";
import {
  IconCalendarCheck,
  IconCalendarX,
  IconCircleCheckFilled,
  IconClockHour4,
  IconMinus,
  IconTrendingDown,
  IconTrendingUp,
  type Icon,
} from "@tabler/icons-react";
import { Badge } from "@/components/ui/badge";
import { Card, CardAction, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { cn } from "@/lib/utils";
import { nomeDoMes, variacaoPercentual, type ResumoDoMes } from "@/utils/painel";

/** Selo de tendência: verde quando a mudança é boa, vermelho quando é ruim. */
function Tendencia({ valor, sufixo = "%", melhorQuandoSobe = true }: { valor: number | null; sufixo?: string; melhorQuandoSobe?: boolean }) {
  if (valor === null) return null;
  const Icone = valor > 0 ? IconTrendingUp : valor < 0 ? IconTrendingDown : IconMinus;
  const bom = valor === 0 ? null : valor > 0 === melhorQuandoSobe;
  return (
    <Badge
      variant="outline"
      className={cn(
        "tabular-nums",
        bom === true && "border-sage-700/30 bg-sage-100 text-sage-700",
        bom === false && "border-brick-700/30 bg-brick-100 text-brick-700"
      )}
    >
      <Icone />
      {valor > 0 ? "+" : ""}
      {valor}
      {sufixo}
    </Badge>
  );
}

interface CartaoProps {
  titulo: string;
  valor: string | number;
  icone: Icon;
  selo?: React.ReactNode;
  destaque: string;
  detalhe: string;
  para: string;
}

function Cartao({ titulo, valor, icone: Icone, selo, destaque, detalhe, para }: CartaoProps) {
  return (
    <Link
      to={para}
      className="group rounded-xl focus-visible:ring-2 focus-visible:ring-ring focus-visible:outline-none"
    >
      <Card className="@container/card h-full border-t-2 border-t-brand-600 bg-gradient-to-t from-primary/5 to-card shadow-xs transition-colors group-hover:border-brand-300">
        <CardHeader className="pb-(--card-spacing) @[220px]/card:pb-0">
          <CardDescription>{titulo}</CardDescription>
          <CardTitle className="text-2xl font-semibold text-brand-700 tabular-nums @[250px]/card:text-3xl">{valor}</CardTitle>
          {/* Em cartões estreitos (celular) ficam só o título e o número. */}
          {selo && <CardAction className="hidden @[250px]/card:block">{selo}</CardAction>}
        </CardHeader>
        <CardFooter className="hidden flex-col items-start gap-1 border-t-0 bg-transparent text-sm @[220px]/card:flex">
          <div className="flex items-center gap-2 font-medium">
            {destaque}
            <Icone className="size-4 shrink-0 text-brand-600" />
          </div>
          <div className="text-muted-foreground">{detalhe}</div>
        </CardFooter>
      </Card>
    </Link>
  );
}

function diferenca(atual: number, anterior: number, mesAnterior: string, singular: string, plural: string): string {
  const d = atual - anterior;
  if (d === 0) return `Mesmo número de ${mesAnterior}`;
  const n = Math.abs(d);
  return `${n} ${n === 1 ? singular : plural} ${d > 0 ? "a mais" : "a menos"} que em ${mesAnterior}`;
}

/** Os 4 números do mês no topo do painel, comparados ao mês anterior. */
export default function CartoesResumo({
  resumo,
  aguardandoVoce,
  meusNumeros,
  hoje = new Date(),
}: {
  resumo: ResumoDoMes;
  /** Convites que dependem do usuário logado (null para quem não participa das reuniões). */
  aguardandoVoce: number | null;
  meusNumeros: boolean;
  hoje?: Date;
}) {
  const { atual, anterior } = resumo;
  const mes = nomeDoMes(hoje.getMonth());
  const mesAnterior = nomeDoMes(hoje.getMonth() - 1);
  const marcadasNoMes = atual.total + atual.canceladas;
  const variacaoTaxa =
    atual.taxaCancelamento !== null && anterior.taxaCancelamento !== null
      ? atual.taxaCancelamento - anterior.taxaCancelamento
      : null;

  return (
    <div className="grid grid-cols-2 gap-3 sm:gap-4 @5xl/main:grid-cols-4">
      <Cartao
        titulo={meusNumeros ? `Suas reuniões em ${mes}` : `Reuniões em ${mes}`}
        valor={atual.total}
        icone={IconCalendarCheck}
        selo={<Tendencia valor={variacaoPercentual(atual.total, anterior.total)} />}
        destaque={diferenca(atual.total, anterior.total, mesAnterior, "reunião", "reuniões")}
        detalhe="Confirmadas, concluídas e aguardando resposta"
        para="/agendamentos"
      />
      <Cartao
        titulo="Aguardando resposta"
        valor={resumo.aguardando}
        icone={IconClockHour4}
        selo={
          aguardandoVoce ? (
            <Badge variant="outline" className="border-amber-300 bg-amber-50 text-amber-800 dark:border-amber-500/40 dark:bg-amber-500/15 dark:text-amber-200">
              {aguardandoVoce} com você
            </Badge>
          ) : null
        }
        destaque={
          aguardandoVoce === null
            ? "Convites ainda sem resposta"
            : aguardandoVoce > 0
              ? `${aguardandoVoce} ${aguardandoVoce === 1 ? "depende" : "dependem"} da sua resposta`
              : "Nenhuma depende de você"
        }
        detalhe="Reuniões de hoje em diante sem confirmação"
        para="/agendamentos?status=PENDENTE,AGUARDANDO_RESPOSTA"
      />
      <Cartao
        titulo={`Concluídas em ${mes}`}
        valor={atual.concluidas}
        icone={IconCircleCheckFilled}
        selo={<Tendencia valor={variacaoPercentual(atual.concluidas, anterior.concluidas)} />}
        destaque={diferenca(atual.concluidas, anterior.concluidas, mesAnterior, "reunião", "reuniões")}
        detalhe="Reuniões que já aconteceram"
        para="/agendamentos?status=CONCLUIDO"
      />
      <Cartao
        titulo="Taxa de cancelamento"
        valor={atual.taxaCancelamento === null ? "—" : `${atual.taxaCancelamento}%`}
        icone={IconCalendarX}
        selo={<Tendencia valor={variacaoTaxa} sufixo=" p.p." melhorQuandoSobe={false} />}
        destaque={
          marcadasNoMes === 0
            ? `Nenhuma reunião em ${mes} ainda`
            : `${atual.canceladas} de ${marcadasNoMes} ${marcadasNoMes === 1 ? "reunião cancelada" : "reuniões canceladas"}`
        }
        detalhe={`Em ${mes}, comparada a ${mesAnterior}`}
        para="/agendamentos?status=CANCELADO"
      />
    </div>
  );
}
