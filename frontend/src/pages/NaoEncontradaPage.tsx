import { Link, useLocation } from "react-router-dom";
import { IconArrowLeft, IconMapQuestion } from "@tabler/icons-react";
import { Button } from "@/components/ui/button";

/** Endereço que não corresponde a nenhuma tela do painel. */
export default function NaoEncontradaPage() {
  const { pathname } = useLocation();

  return (
    <div className="flex flex-1 flex-col items-center justify-center gap-3 py-16 text-center">
      <div className="mb-1 flex size-14 items-center justify-center rounded-xl bg-brand-100 text-brand-700">
        <IconMapQuestion className="size-7" />
      </div>
      <h2 className="text-xl font-semibold tracking-tight text-brand-900">Página não encontrada</h2>
      <p className="max-w-sm text-sm text-muted-foreground">
        O endereço <code className="rounded bg-muted px-1.5 py-0.5 text-xs break-all">{pathname}</code> não existe
        no Schedemy. Ele pode ter sido digitado errado ou a página mudou de lugar.
      </p>
      <Button asChild className="mt-2">
        <Link to="/">
          <IconArrowLeft />
          Voltar ao painel
        </Link>
      </Button>
    </div>
  );
}
