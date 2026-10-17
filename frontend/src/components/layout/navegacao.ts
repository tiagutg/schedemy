import {
  IconBan,
  IconCalendarEvent,
  IconClockHour4,
  IconDashboard,
  IconBell,
  IconHourglass,
  IconStar,
  IconUsers,
  type Icon,
} from "@tabler/icons-react";

export interface ItemNav {
  to: string;
  rotulo: string;
  icone: Icon;
  /** `null` significa visível para qualquer usuário autenticado. */
  permissao: string | null;
  fim?: boolean;
}

// Itens da agenda acadêmica (grupo principal da sidebar).
export const ITENS_NAV: ItemNav[] = [
  { to: "/", rotulo: "Painel", fim: true, icone: IconDashboard, permissao: null },
  { to: "/agendamentos", rotulo: "Agendamentos", icone: IconCalendarEvent, permissao: "agendamentos.ver" },
  { to: "/disponibilidades", rotulo: "Disponibilidades", icone: IconClockHour4, permissao: "disponibilidades.gerenciar" },
  { to: "/bloqueios", rotulo: "Bloqueios de período", icone: IconBan, permissao: "bloqueios.gerenciar" },
  { to: "/duracoes", rotulo: "Durações de reunião", icone: IconHourglass, permissao: "duracoes.gerenciar" },
];

// Itens administrativos (grupo secundário da sidebar).
export const ITENS_GESTAO: ItemNav[] = [
  { to: "/usuarios", rotulo: "Usuários", icone: IconUsers, permissao: "usuarios.gerenciar" },
  { to: "/avaliacoes", rotulo: "Avaliações", icone: IconStar, permissao: "avaliacoes.ver" },
  { to: "/notificacoes", rotulo: "Notificações", icone: IconBell, permissao: "notificacoes.ver" },
];

/** Título exibido no cabeçalho para a rota atual. */
export function tituloDaRota(pathname: string): string {
  const item = [...ITENS_NAV, ...ITENS_GESTAO].find((i) => (i.fim ? pathname === i.to : pathname.startsWith(i.to)));
  if (pathname.startsWith("/perfil")) return "Meu perfil";
  return item?.rotulo ?? "Página não encontrada";
}
