import { Link, NavLink, useLocation } from "react-router-dom";
import { IconCirclePlusFilled } from "@tabler/icons-react";
import {
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  useSidebar,
} from "@/components/ui/sidebar";
import type { ItemNav } from "./navegacao";

interface NavMainProps {
  itens: ItemNav[];
  rotulo?: string;
  /** Exibe o botão de criação rápida de agendamento no topo do grupo. */
  criarAgendamento?: boolean;
}

export function NavMain({ itens, rotulo, criarAgendamento = false }: NavMainProps) {
  const { setOpenMobile } = useSidebar();
  const { pathname } = useLocation();

  if (itens.length === 0 && !criarAgendamento) return null;

  return (
    <SidebarGroup>
      {rotulo && <SidebarGroupLabel>{rotulo}</SidebarGroupLabel>}
      <SidebarGroupContent className="flex flex-col gap-2">
        {criarAgendamento && (
          <SidebarMenu>
            <SidebarMenuItem>
              <SidebarMenuButton
                asChild
                tooltip="Novo agendamento"
                className="min-w-8 bg-primary text-primary-foreground duration-200 ease-linear hover:bg-primary/90 hover:text-primary-foreground active:bg-primary/90 active:text-primary-foreground"
              >
                <Link to="/agendamentos?novo=1" onClick={() => setOpenMobile(false)}>
                  <IconCirclePlusFilled />
                  <span>Novo agendamento</span>
                </Link>
              </SidebarMenuButton>
            </SidebarMenuItem>
          </SidebarMenu>
        )}
        <SidebarMenu>
          {itens.map((item) => {
            const ativo = item.fim ? pathname === item.to : pathname.startsWith(item.to);
            return (
              <SidebarMenuItem key={item.to}>
                <SidebarMenuButton asChild tooltip={item.rotulo} isActive={ativo}>
                  <NavLink to={item.to} end={item.fim} onClick={() => setOpenMobile(false)}>
                    <item.icone />
                    <span>{item.rotulo}</span>
                  </NavLink>
                </SidebarMenuButton>
              </SidebarMenuItem>
            );
          })}
        </SidebarMenu>
      </SidebarGroupContent>
    </SidebarGroup>
  );
}
