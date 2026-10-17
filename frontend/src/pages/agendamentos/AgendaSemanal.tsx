import { useEffect, useState } from "react";
import { IconChevronLeft, IconChevronRight } from "@tabler/icons-react";
import Pill from "@/components/common/Pill";
import Spinner from "@/components/common/Spinner";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { CORES_STATUS_AGENDAMENTO, rotularEnum } from "@/utils/domain";
import { formatarHora, hojeISO } from "@/utils/format";
import type { Agendamento } from "@/types";

const NOMES_DIAS = ["Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom"];

function paraISO(d: Date): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

/** Segunda-feira da semana que contém a data. */
function segundaDaSemana(base: Date): Date {
  const d = new Date(base.getFullYear(), base.getMonth(), base.getDate());
  d.setDate(d.getDate() - ((d.getDay() + 6) % 7));
  return d;
}

function somarDias(d: Date, dias: number): Date {
  const r = new Date(d);
  r.setDate(r.getDate() + dias);
  return r;
}

interface AgendaSemanalProps {
  /** Busca os agendamentos entre as duas datas (inclusive), já com os filtros da página. */
  buscarSemana: (dataInicio: string, dataFim: string) => Promise<Agendamento[]>;
  onAbrir: (agendamento: Agendamento) => void;
  /** Muda quando a lista precisa ser recarregada (filtros, criação, edição). */
  versao: string;
}

/** Os agendamentos de uma semana, de segunda a domingo, em colunas por dia. */
export default function AgendaSemanal({ buscarSemana, onAbrir, versao }: AgendaSemanalProps) {
  const [segunda, setSegunda] = useState(() => segundaDaSemana(new Date()));
  const dias = Array.from({ length: 7 }, (_, i) => somarDias(segunda, i));
  const inicioISO = paraISO(dias[0]);
  const fimISO = paraISO(dias[6]);
  const hoje = hojeISO();

  // Guarda para qual semana/versão a lista foi carregada; enquanto não bate, está carregando.
  const chave = `${inicioISO}|${fimISO}|${versao}`;
  const [resultado, setResultado] = useState<{ chave: string; agendamentos: Agendamento[] }>({
    chave: "",
    agendamentos: [],
  });
  const carregando = resultado.chave !== chave;
  const agendamentos = resultado.agendamentos;

  useEffect(() => {
    let cancelado = false;
    const chaveBusca = `${inicioISO}|${fimISO}|${versao}`;
    buscarSemana(inicioISO, fimISO)
      .then((lista) => !cancelado && setResultado({ chave: chaveBusca, agendamentos: lista }))
      .catch(() => !cancelado && setResultado({ chave: chaveBusca, agendamentos: [] }));
    return () => {
      cancelado = true;
    };
  }, [buscarSemana, inicioISO, fimISO, versao]);

  const rotuloSemana = `${dias[0].toLocaleDateString("pt-BR", { day: "2-digit", month: "short" })} – ${dias[6].toLocaleDateString("pt-BR", { day: "2-digit", month: "short", year: "numeric" })}`;

  return (
    <section aria-label="Agenda da semana" className="flex flex-col gap-3">
      <div className="flex flex-wrap items-center gap-2">
        <Button variant="outline" size="icon-sm" aria-label="Semana anterior" onClick={() => setSegunda(somarDias(segunda, -7))}>
          <IconChevronLeft />
        </Button>
        <Button variant="outline" size="icon-sm" aria-label="Próxima semana" onClick={() => setSegunda(somarDias(segunda, 7))}>
          <IconChevronRight />
        </Button>
        <Button variant="ghost" size="sm" onClick={() => setSegunda(segundaDaSemana(new Date()))}>
          Hoje
        </Button>
        <p className="text-sm font-medium text-brand-900">{rotuloSemana}</p>
        {carregando && <Spinner className="size-4 text-brand-700" />}
      </div>

      {/* Uma coluna por dia nas telas largas; no celular os dias ficam empilhados. */}
      <div className="grid grid-cols-1 gap-2 @3xl/main:grid-cols-7">
        {dias.map((dia, i) => {
          const iso = paraISO(dia);
          const doDia = agendamentos
            .filter((a) => a.dataReuniao === iso)
            .sort((a, b) => a.horarioInicio.localeCompare(b.horarioInicio));
          return (
            <section
              key={iso}
              className={cn(
                "flex min-h-24 flex-col gap-1.5 rounded-lg border p-2",
                iso === hoje ? "border-brand-300 bg-brand-50" : "bg-card",
                doDia.length === 0 && "@max-3xl/main:min-h-0"
              )}
            >
              <p className={cn("text-xs font-medium", iso === hoje ? "text-brand-700" : "text-muted-foreground")}>
                {NOMES_DIAS[i]} {dia.toLocaleDateString("pt-BR", { day: "2-digit", month: "2-digit" })}
                {iso === hoje && " · hoje"}
              </p>
              {doDia.length === 0 ? (
                <p className="text-xs text-muted-foreground">Sem reuniões</p>
              ) : (
                doDia.map((a) => (
                  <button
                    key={a.id}
                    type="button"
                    onClick={() => onAbrir(a)}
                    className={cn(
                      "flex flex-col gap-1 rounded-md border bg-card p-2 text-left text-xs shadow-xs transition-colors hover:border-brand-300 hover:bg-brand-50",
                      a.status === "CANCELADO" && "opacity-60"
                    )}
                  >
                    <span className="font-medium tabular-nums text-brand-700">
                      {formatarHora(a.horarioInicio)}–{formatarHora(a.horarioFim)}
                    </span>
                    <span className={cn("line-clamp-2 font-medium", a.status === "CANCELADO" && "line-through")}>
                      {a.topico}
                    </span>
                    <Pill className={cn("self-start", CORES_STATUS_AGENDAMENTO[a.status])}>{rotularEnum(a.status)}</Pill>
                  </button>
                ))
              )}
            </section>
          );
        })}
      </div>
    </section>
  );
}
