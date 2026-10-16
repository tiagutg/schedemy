import { Suspense, type CSSProperties } from "react";
import { Outlet, useLocation, useNavigate } from "react-router-dom";
import { SidebarInset, SidebarProvider } from "@/components/ui/sidebar";
import CarregandoPagina from "@/components/common/CarregandoPagina";
import { useAuth } from "@/context/AuthContext";
import { AppSidebar } from "./AppSidebar";
import { SiteHeader } from "./SiteHeader";
import { tituloDaRota } from "./navegacao";

export default function AppLayout() {
  const { sair } = useAuth();
  const navigate = useNavigate();
  const { pathname } = useLocation();

  function aoSair() {
    sair();
    navigate("/login", { replace: true });
  }

  return (
    <SidebarProvider
      style={
        {
          "--sidebar-width": "calc(var(--spacing) * 72)",
          "--header-height": "calc(var(--spacing) * 12)",
        } as CSSProperties
      }
    >
      <AppSidebar variant="inset" onSair={aoSair} />
      {/* min-w-0 permite que tabelas largas rolem em vez de empurrar o layout */}
      <SidebarInset className="min-w-0">
        <SiteHeader titulo={tituloDaRota(pathname)} />
        <div className="flex flex-1 flex-col">
          <div className="@container/main flex flex-1 flex-col gap-2">
            {/* Em monitores muito largos o conteúdo é limitado para manter a leitura confortável */}
            <div className="mx-auto flex w-full max-w-screen-2xl flex-col gap-4 px-4 py-4 md:gap-6 md:py-6 lg:px-6">
              {/* Sidebar e cabeçalho ficam visíveis enquanto a página é carregada */}
              <Suspense fallback={<CarregandoPagina />}>
                <Outlet />
              </Suspense>
            </div>
          </div>
        </div>
      </SidebarInset>
    </SidebarProvider>
  );
}
