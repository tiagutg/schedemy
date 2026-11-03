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

// O dia a dia de todos os perfis (grupo principal da sidebar).
export const ITENS_NAV: ItemNav[] = [
  { to: "/", rotulo: "Painel", fim: true, icone: IconDashboard, permissao: null },
  { to: "/agendamentos", rotulo: "Agendamentos", icone: IconCalendarEvent, permissao: "agendamentos.ver" },
  { to: "/notificacoes", rotulo: "Notificações", icone: IconBell, permissao: "notificacoes.ver" },
  { to: "/avaliacoes", rotulo: "Avaliações", icone: IconStar, permissao: "avaliacoes.ver" },
];

// Agenda do professor/coordenador: quando atende e quando não está disponível.
export const ITENS_AGENDA: ItemNav[] = [
  { to: "/disponibilidades", rotulo: "Horários de atendimento", icone: IconClockHour4, permissao: "disponibilidades.gerenciar" },
  { to: "/bloqueios", rotulo: "Bloqueios de período", icone: IconBan, permissao: "bloqueios.gerenciar" },
];

// Parâmetros e cadastros da instituição.
export const ITENS_CONFIGURACAO: ItemNav[] = [
  { to: "/usuarios", rotulo: "Usuários", icone: IconUsers, permissao: "usuarios.gerenciar" },
  { to: "/duracoes", rotulo: "Durações de reunião", icone: IconHourglass, permissao: "duracoes.gerenciar" },
];

/** Título exibido no cabeçalho para a rota atual. */
export function tituloDaRota(pathname: string): string {
  const item = [...ITENS_NAV, ...ITENS_AGENDA, ...ITENS_CONFIGURACAO].find((i) => (i.fim ? pathname === i.to : pathname.startsWith(i.to)));
  if (pathname.startsWith("/perfil")) return "Meu perfil";
  return item?.rotulo ?? "Página não encontrada";
}
