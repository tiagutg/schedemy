import type { ReactNode } from "react";
import { Button } from "@/components/ui/button";
import Modal from "./Modal";

interface ConfirmDialogProps {
  aberto: boolean;
  titulo?: string;
  descricao?: ReactNode;
  confirmarRotulo?: string;
  tom?: "primary" | "danger";
  carregando?: boolean;
  onConfirmar: () => void;
  onCancelar: () => void;
}

export default function ConfirmDialog({
  aberto,
  titulo = "Confirmar ação",
  descricao,
  confirmarRotulo = "Confirmar",
  tom = "primary",
  carregando,
  onConfirmar,
  onCancelar,
}: ConfirmDialogProps) {
  return (
    <Modal aberto={aberto} titulo={titulo} onFechar={onCancelar} largura="max-w-sm">
      <p className="text-sm text-muted-foreground">{descricao}</p>
      <div className="flex justify-end gap-2">
        <Button variant="outline" onClick={onCancelar}>
          Cancelar
        </Button>
        <Button
          className={tom === "danger" ? "bg-destructive text-white hover:bg-destructive/90" : undefined}
          onClick={onConfirmar}
          disabled={carregando}
        >
          {confirmarRotulo}
        </Button>
      </div>
    </Modal>
  );
}
