// RF 21/22: textos e prazos dos pedidos de alteração de reunião confirmada.

import type { SolicitacaoEdicao } from "@/types";

/** RN 14: o docente responde até 24 h antes do horário original (depois disso a rotina expira o pedido). */
export const HORAS_PARA_RESPONDER = 24;

const dois = (n: number) => String(n).padStart(2, "0");

/** Fim do prazo de resposta ("dd/mm/aaaa às HH:mm"), contado do horário atual da reunião. */
export function prazoDeResposta(p: Pick<SolicitacaoEdicao, "dataAtual" | "horarioInicioAtual">): string {
  const [ano, mes, dia] = p.dataAtual.split("-").map(Number);
  const [hora, minuto] = p.horarioInicioAtual.split(":").map(Number);
  const prazo = new Date(ano, mes - 1, dia, hora, minuto);
  prazo.setHours(prazo.getHours() - HORAS_PARA_RESPONDER);
  return `${dois(prazo.getDate())}/${dois(prazo.getMonth() + 1)}/${prazo.getFullYear()} às ${dois(prazo.getHours())}:${dois(prazo.getMinutes())}`;
}

/** Como o pedido terminou, com quem decidiu quando a API informa (pedidos antigos não têm esse dado). */
export function desfechoDoPedido(
  p: Pick<SolicitacaoEdicao, "status" | "idRespondidoPor" | "nomeRespondidoPor">,
  euId?: number | null
): string {
  const quem = p.idRespondidoPor != null && p.idRespondidoPor === euId ? "você" : p.nomeRespondidoPor;
  switch (p.status) {
    case "PENDENTE":
      return "Aguardando o docente";
    case "ACEITA":
      return quem ? `Aceito por ${quem}` : "Aceito";
    case "RECUSADA":
      return quem ? `Recusado por ${quem}` : "Recusado";
    case "CANCELADA":
      return "Cancelado (desistência ou reunião cancelada)";
    case "EXPIRADA":
      return "Expirou sem resposta";
  }
}

export const CORES_STATUS_PEDIDO: Record<SolicitacaoEdicao["status"], string> = {
  PENDENTE: "bg-brand-100 text-brand-700",
  ACEITA: "bg-sage-100 text-sage-700",
  RECUSADA: "bg-brick-100 text-brick-700",
  CANCELADA: "bg-ink-100 text-ink-800",
  EXPIRADA: "bg-ink-100 text-ink-800",
};
