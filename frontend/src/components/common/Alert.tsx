import type { ReactNode } from "react";
import { IconX } from "@tabler/icons-react";
import { cn } from "@/lib/utils";

const ESTILOS = {
  erro: "border-destructive/30 text-destructive",
  aviso: "border-brand-600/30 text-brand-700",
  sucesso: "border-sage-700/30 text-sage-700",
  info: "text-foreground",
};

interface AlertProps {
  tipo?: keyof typeof ESTILOS;
  titulo?: ReactNode;
  children?: ReactNode;
  onFechar?: () => void;
}

export default function Alert({ tipo = "info", titulo, children, onFechar }: AlertProps) {
  return (
    <div className={cn("flex items-start gap-3 rounded-lg border bg-card px-4 py-3 text-sm", ESTILOS[tipo])}>
      <div className="flex-1">
        {titulo && <p className="font-medium tracking-tight">{titulo}</p>}
        {children && <div className="mt-0.5 opacity-90">{children}</div>}
      </div>
      {onFechar && (
        <button onClick={onFechar} className="shrink-0 opacity-60 hover:opacity-100" aria-label="Fechar">
          <IconX className="size-4" />
        </button>
      )}
    </div>
  );
}
