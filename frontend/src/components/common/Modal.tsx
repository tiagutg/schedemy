import type { ReactNode } from "react";
import { cn } from "@/lib/utils";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";

const LARGURAS = {
  "max-w-sm": "sm:max-w-sm",
  "max-w-md": "sm:max-w-md",
  "max-w-lg": "sm:max-w-lg",
  "max-w-2xl": "sm:max-w-2xl",
} as const;

interface ModalProps {
  aberto: boolean;
  titulo: ReactNode;
  subtitulo?: ReactNode;
  onFechar?: () => void;
  largura?: keyof typeof LARGURAS;
  children: ReactNode;
}

export default function Modal({ aberto, titulo, subtitulo, onFechar, largura = "max-w-lg", children }: ModalProps) {
  return (
    <Dialog open={aberto} onOpenChange={(aberto) => !aberto && onFechar?.()}>
      <DialogContent
        className={cn("max-h-[calc(100dvh-2rem)] overflow-y-auto", LARGURAS[largura])}
        // Como antes, o modal só fecha pelo botão, pelo Esc ou pelas ações do conteúdo.
        onInteractOutside={(e) => e.preventDefault()}
      >
        <DialogHeader>
          <DialogTitle>{titulo}</DialogTitle>
          {subtitulo ? <DialogDescription>{subtitulo}</DialogDescription> : null}
        </DialogHeader>
        {children}
      </DialogContent>
    </Dialog>
  );
}
