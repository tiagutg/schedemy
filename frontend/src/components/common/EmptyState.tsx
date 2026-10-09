import type { ReactNode } from "react";
import { IconCalendar } from "@tabler/icons-react";

interface EmptyStateProps {
  titulo?: string;
  descricao?: ReactNode;
  acao?: ReactNode;
}

export default function EmptyState({ titulo = "Nada por aqui ainda", descricao, acao }: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center gap-2 py-12 text-center">
      <div className="mb-1 flex size-10 items-center justify-center rounded-lg bg-muted text-foreground">
        <IconCalendar className="size-5" />
      </div>
      <p className="text-base font-medium tracking-tight">{titulo}</p>
      {descricao && <p className="max-w-sm text-sm text-muted-foreground">{descricao}</p>}
      {acao}
    </div>
  );
}
