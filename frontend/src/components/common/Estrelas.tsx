import { IconStar, IconStarFilled } from "@tabler/icons-react";
import { cn } from "@/lib/utils";

const MAXIMO = 5;

/** Nota de 0 a 5 com as estrelas vazias à mostra, para a escala ficar clara. */
export function Estrelas({ nota, className }: { nota: number; className?: string }) {
  return (
    <span className={cn("inline-flex items-center gap-0.5", className)} role="img" aria-label={`Nota ${nota} de ${MAXIMO}`}>
      {Array.from({ length: MAXIMO }, (_, i) =>
        i < nota ? (
          <IconStarFilled key={i} className="size-4 text-amber-500" aria-hidden="true" />
        ) : (
          <IconStar key={i} className="size-4 text-muted-foreground/50" aria-hidden="true" />
        )
      )}
    </span>
  );
}

/** Escolha da nota clicando nas estrelas (grupo de rádio, acessível pelo teclado). */
export function EscolhaDeEstrelas({ nota, onChange }: { nota: number; onChange: (nota: number) => void }) {
  const legendas = ["Muito ruim", "Ruim", "Regular", "Boa", "Excelente"];
  return (
    <fieldset className="flex flex-col gap-2">
      <legend className="mb-2 text-sm font-medium">Nota</legend>
      <div className="flex items-center gap-3">
        <div className="flex gap-1" role="radiogroup" aria-label="Nota da reunião">
          {Array.from({ length: MAXIMO }, (_, i) => {
            const valor = i + 1;
            const marcada = valor <= nota;
            return (
              <button
                key={valor}
                type="button"
                role="radio"
                aria-checked={nota === valor}
                aria-label={`${valor} de ${MAXIMO}: ${legendas[i]}`}
                onClick={() => onChange(valor)}
                className="rounded-md p-0.5 transition-transform hover:scale-110 focus-visible:ring-2 focus-visible:ring-ring focus-visible:outline-none"
              >
                {marcada ? (
                  <IconStarFilled className="size-7 text-amber-500" aria-hidden="true" />
                ) : (
                  <IconStar className="size-7 text-muted-foreground/60" aria-hidden="true" />
                )}
              </button>
            );
          })}
        </div>
        <span className="text-sm text-muted-foreground">{nota > 0 ? legendas[nota - 1] : "Sem nota"}</span>
      </div>
    </fieldset>
  );
}
