import type { ReactNode } from "react";
import { Card, CardContent } from "@/components/ui/card";

/** Cartão que agrupa os filtros de uma listagem. */
export default function FiltrosCard({ children }: { children: ReactNode }) {
  return (
    <Card size="sm" className="shadow-xs">
      <CardContent className="flex flex-wrap items-end gap-3">{children}</CardContent>
    </Card>
  );
}
