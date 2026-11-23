import type { ReactNode } from "react";
import { Card, CardContent } from "@/components/ui/card";

/**
 * Cartão que agrupa os filtros de uma listagem. Sem o overflow-hidden do Card,
 * para a lista de um campo de busca e seleção (ComboboxField) abrir por cima da tabela.
 */
export default function FiltrosCard({ children }: { children: ReactNode }) {
  return (
    <Card size="sm" className="overflow-visible shadow-xs">
      <CardContent className="flex flex-wrap items-end gap-3">{children}</CardContent>
    </Card>
  );
}
