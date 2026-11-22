import { cn } from "@/lib/utils";
import { paraHora, paraMinutos } from "@/utils/agenda";
import type { Disponibilidade } from "@/types";

// Segunda a sábado; domingo só aparece se houver atendimento nele.
const DIAS = [
  { valor: 2, curto: "Seg" },
  { valor: 3, curto: "Ter" },
  { valor: 4, curto: "Qua" },
  { valor: 5, curto: "Qui" },
  { valor: 6, curto: "Sex" },
  { valor: 7, curto: "Sáb" },
];
const PX_POR_HORA = 28;
// A partir de ~2 h o bloco tem altura para início, duração e fim em linhas separadas.
const ALTURA_PARA_TRES_LINHAS = 56;

/** 840 → "14 h"; 90 → "1 h 30"; 45 → "45 min". */
function descreverDuracao(minutos: number): string {
  const h = Math.floor(minutos / 60);
  const m = minutos % 60;
  if (!h) return `${m} min`;
  return m ? `${h} h ${String(m).padStart(2, "0")}` : `${h} h`;
}

/** Os horários de atendimento da semana como blocos numa grade (seg–sáb). */
export default function GradeSemanal({
  disponibilidades,
  onEditar,
}: {
  disponibilidades: Disponibilidade[];
  onEditar?: (d: Disponibilidade) => void;
}) {
  const dias = disponibilidades.some((d) => d.diaSemana === 1) ? [{ valor: 1, curto: "Dom" }, ...DIAS] : DIAS;
  // Faixa exibida: das 8h às 18h, ampliada para caber todos os blocos.
  const inicio = Math.min(8 * 60, ...disponibilidades.map((d) => paraMinutos(d.horarioInicio)));
  const fim = Math.max(18 * 60, ...disponibilidades.map((d) => paraMinutos(d.horarioFim)));
  const primeiraHora = Math.floor(inicio / 60);
  const ultimaHora = Math.ceil(fim / 60);
  const altura = (ultimaHora - primeiraHora) * PX_POR_HORA;
  const horas = Array.from({ length: ultimaHora - primeiraHora + 1 }, (_, i) => primeiraHora + i);
  const topo = (minutos: number) => ((minutos - primeiraHora * 60) / 60) * PX_POR_HORA;

  return (
    <div className="rounded-lg border p-3 sm:p-4" aria-label="Grade semanal de atendimento" role="figure">
      <div className="grid gap-1 sm:gap-2" style={{ gridTemplateColumns: `2.25rem repeat(${dias.length}, minmax(0, 1fr))` }}>
        <div />
        {dias.map((d) => (
          <div key={d.valor} className="pb-1 text-center text-xs font-medium text-muted-foreground">
            {d.curto}
          </div>
        ))}

        <div className="relative" style={{ height: altura }}>
          {horas.map((h) => (
            <span
              key={h}
              className="absolute right-1 -translate-y-1/2 text-[10px] text-muted-foreground tabular-nums"
              style={{ top: (h - primeiraHora) * PX_POR_HORA }}
            >
              {String(h).padStart(2, "0")}h
            </span>
          ))}
        </div>
        {dias.map((dia) => (
          <div key={dia.valor} className="relative rounded-md bg-muted/40" style={{ height: altura }}>
            {horas.slice(1, -1).map((h) => (
              <div
                key={h}
                className="absolute inset-x-0 border-t border-dashed border-border/60"
                style={{ top: (h - primeiraHora) * PX_POR_HORA }}
              />
            ))}
            {disponibilidades
              .filter((d) => d.diaSemana === dia.valor)
              .map((d) => {
                const ini = paraMinutos(d.horarioInicio);
                const fimBloco = paraMinutos(d.horarioFim);
                const rotulo = `${paraHora(ini)}–${paraHora(fimBloco)}`;
                const altura = Math.max(topo(fimBloco) - topo(ini), 18);
                // Bloco alto: início no topo, duração no meio e fim embaixo, como uma linha do tempo.
                // Bloco curto: o intervalo numa linha só.
                const alto = altura >= ALTURA_PARA_TRES_LINHAS;
                return (
                  <button
                    key={d.id}
                    type="button"
                    disabled={!onEditar}
                    onClick={() => onEditar?.(d)}
                    title={`${rotulo}${d.ativo ? "" : " (inativa)"}`}
                    aria-label={`${dia.curto} ${rotulo}${d.ativo ? "" : ", inativa"}`}
                    className={cn(
                      "absolute inset-x-1 flex flex-col overflow-hidden rounded-md border-l-4 py-1 pr-1.5 pl-2 text-left text-[10px] leading-tight font-medium tabular-nums transition-colors sm:text-xs",
                      alto ? "justify-between" : "justify-center",
                      d.ativo
                        ? "border-primary bg-primary/10 text-primary hover:bg-primary/20"
                        : "border border-l-4 border-dashed border-muted-foreground/50 bg-background text-muted-foreground",
                      "focus-visible:ring-2 focus-visible:ring-ring focus-visible:outline-none"
                    )}
                    style={{ top: topo(ini), height: altura }}
                  >
                    {alto ? (
                      <>
                        <span>{paraHora(ini)}</span>
                        <span className="self-center text-center font-normal opacity-80">
                          {descreverDuracao(fimBloco - ini)}
                          {!d.ativo && <span className="block">Inativo</span>}
                        </span>
                        <span>{paraHora(fimBloco)}</span>
                      </>
                    ) : (
                      <span className="max-w-full truncate">{rotulo}</span>
                    )}
                  </button>
                );
              })}
          </div>
        ))}
      </div>
      <p className="mt-3 text-xs text-muted-foreground">Clique num bloco para editar. Blocos tracejados estão inativos.</p>
    </div>
  );
}
