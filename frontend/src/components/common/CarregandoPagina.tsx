import { cn } from "@/lib/utils";
import Spinner from "./Spinner";

/** Indicador exibido enquanto o código de uma página é baixado. */
export default function CarregandoPagina({ telaCheia = false }: { telaCheia?: boolean }) {
  return (
    <div
      className={cn(
        "flex items-center justify-center text-muted-foreground",
        telaCheia ? "min-h-svh" : "py-16"
      )}
    >
      <Spinner className="size-5" />
    </div>
  );
}
