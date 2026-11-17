import { Link } from "react-router-dom";
import { IconBell } from "@tabler/icons-react";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import { SidebarTrigger } from "@/components/ui/sidebar";
import { useAvisos } from "@/context/AvisosContext";

export function SiteHeader({ titulo }: { titulo: string }) {
  const { naoLidos } = useAvisos();
  const rotuloAvisos = naoLidos > 0 ? `Notificações: ${naoLidos} não lida${naoLidos === 1 ? "" : "s"}` : "Notificações";

  return (
    // Fixo no topo: o botão do menu fica sempre ao alcance ao rolar a página.
    <header className="sticky top-0 z-20 flex h-(--header-height) shrink-0 items-center gap-2 border-b bg-background/95 backdrop-blur transition-[width,height] ease-linear group-has-data-[collapsible=icon]/sidebar-wrapper:h-(--header-height) supports-[backdrop-filter]:bg-background/80 md:rounded-t-xl">
      <div className="flex w-full min-w-0 items-center gap-1 px-4 lg:gap-2 lg:px-6">
        <SidebarTrigger className="-ml-1" />
        <Separator orientation="vertical" className="mx-2 data-[orientation=vertical]:h-4" />
        <h1 className="truncate text-base font-semibold text-brand-700">{titulo}</h1>
        <Button asChild variant="ghost" size="icon" className="relative ml-auto shrink-0">
          <Link to="/notificacoes" aria-label={rotuloAvisos} title={rotuloAvisos}>
            <IconBell />
            {naoLidos > 0 && (
              <span
                aria-hidden="true"
                className="absolute -top-0.5 -right-0.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-destructive px-1 text-[10px] leading-none font-semibold text-white"
              >
                {naoLidos > 9 ? "9+" : naoLidos}
              </span>
            )}
          </Link>
        </Button>
      </div>
    </header>
  );
}
