import type { ReactNode } from "react";

interface PageHeaderProps {
  titulo: ReactNode;
  subtitulo?: ReactNode;
  acao?: ReactNode;
}

export default function PageHeader({ titulo, subtitulo, acao }: PageHeaderProps) {
  return (
    <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
      <div className="flex min-w-0 flex-col gap-1">
        <h2 className="text-xl font-semibold tracking-tight break-words text-brand-900 sm:text-2xl">{titulo}</h2>
        {subtitulo && <p className="max-w-2xl text-sm text-muted-foreground">{subtitulo}</p>}
      </div>
      {/* No celular a ação ocupa a largura toda, abaixo do título */}
      {acao && <div className="shrink-0 *:w-full sm:*:w-auto">{acao}</div>}
    </div>
  );
}
