import type { ReactNode } from "react";

/** Rodapé com os botões de ação de um formulário em diálogo. */
export default function FormActions({ children }: { children: ReactNode }) {
  return <div className="flex flex-col-reverse gap-2 border-t pt-4 sm:flex-row sm:justify-end">{children}</div>;
}
