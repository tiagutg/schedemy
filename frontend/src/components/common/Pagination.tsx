import { IconChevronLeft, IconChevronRight } from "@tabler/icons-react";
import { Button } from "@/components/ui/button";
import type { Pagina } from "@/types";

interface PaginationProps {
  pagina: Pagina<unknown> | null;
  onMudarPagina: (pagina: number) => void;
}

export default function Pagination({ pagina, onMudarPagina }: PaginationProps) {
  if (!pagina || pagina.totalPaginas <= 1) return null;

  const { paginaAtual, totalPaginas, totalElementos, primeira, ultima } = pagina;

  return (
    <div className="flex items-center justify-between px-4">
      <div className="hidden flex-1 text-sm text-muted-foreground lg:flex">
        {totalElementos} registro{totalElementos === 1 ? "" : "s"}
      </div>
      <div className="flex w-full items-center gap-8 lg:w-fit">
        <div className="flex w-fit items-center justify-center text-sm font-medium">
          Página {paginaAtual + 1} de {totalPaginas}
        </div>
        <div className="ml-auto flex items-center gap-2 lg:ml-0">
          <Button
            variant="outline"
            size="icon"
            className="size-8"
            disabled={primeira}
            onClick={() => onMudarPagina(paginaAtual - 1)}
          >
            <span className="sr-only">Página anterior</span>
            <IconChevronLeft />
          </Button>
          <Button
            variant="outline"
            size="icon"
            className="size-8"
            disabled={ultima}
            onClick={() => onMudarPagina(paginaAtual + 1)}
          >
            <span className="sr-only">Próxima página</span>
            <IconChevronRight />
          </Button>
        </div>
      </div>
    </div>
  );
}
