import type { ComponentProps } from "react";
import { Link } from "react-router-dom";
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
} from "@/components/ui/sidebar";
import { useAuth } from "@/context/AuthContext";
import { ROTULOS_PERFIL } from "@/utils/permissoes";
import { NavMain } from "./NavMain";
import { NavUser } from "./NavUser";
import { ITENS_GESTAO, ITENS_NAV } from "./navegacao";

type AppSidebarProps = ComponentProps<typeof Sidebar> & {
  onSair: () => void;
};

export function AppSidebar({ onSair, ...props }: AppSidebarProps) {
  const { auth, perfilVinculado, perfil, pode } = useAuth();

  const visivel = (item: { permissao: string | null }) => !item.permissao || pode(item.permissao);

  return (
    <Sidebar collapsible="offcanvas" {...props}>
      <SidebarHeader>
        <SidebarMenu>
          <SidebarMenuItem>
            <SidebarMenuButton asChild className="data-[slot=sidebar-menu-button]:p-1.5!">
              <Link to="/">
                <img src="/schedemy-logo.png" alt="" aria-hidden="true" className="size-7 shrink-0 object-contain" />
                <span className="text-lg font-semibold tracking-tight">Schedemy</span>
              </Link>
            </SidebarMenuButton>
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarHeader>
      <SidebarContent>
        <NavMain itens={ITENS_NAV.filter(visivel)} criarAgendamento={pode("agendamentos.criar")} />
        <NavMain itens={ITENS_GESTAO.filter(visivel)} rotulo="Gestão" />
      </SidebarContent>
      <SidebarFooter>
        <NavUser
          nome={perfilVinculado?.nome || auth?.username || ""}
          descricao={perfil ? ROTULOS_PERFIL[perfil] || perfil : undefined}
          iniciais={auth?.username?.slice(0, 2).toUpperCase() ?? ""}
          onSair={onSair}
        />
      </SidebarFooter>
    </Sidebar>
  );
}
