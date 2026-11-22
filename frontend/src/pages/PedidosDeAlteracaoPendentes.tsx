import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { IconChevronRight, IconMessageQuestion } from "@tabler/icons-react";
import { agendamentosApi } from "@/api/agendamentos";
import { useAuth } from "@/context/AuthContext";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { formatarData, formatarHora } from "@/utils/format";
import { prazoDeResposta } from "@/utils/pedidos";
import type { SolicitacaoEdicao } from "@/types";

const LIMITE = 20;

/**
 * RF 21/22: pedidos de alteração de reunião confirmada que ainda esperam o docente. A API já aplica o
 * escopo: o docente vê os das reuniões de que participa (e decide), o aluno os das suas reuniões, a
 * recepção e a administração todos. Cada item abre o detalhe da reunião, onde fica a decisão.
 * Não aparece quando não há nada pendente.
 */
export default function PedidosDeAlteracaoPendentes() {
  const { perfilVinculado: eu, pode } = useAuth();
  const [pedidos, setPedidos] = useState<SolicitacaoEdicao[]>([]);
  const [total, setTotal] = useState(0);
  const gestor = pode("agendamentos.organizarPorOutros");
  const decide = pode("agendamentos.decidirAlteracao") && !gestor;

  useEffect(() => {
    if (!eu || !pode("agendamentos.ver")) return;
    let cancelado = false;
    agendamentosApi
      .listarPedidosDeAlteracaoNoEscopo({ status: "PENDENTE", size: LIMITE })
      .then((p) => {
        if (cancelado) return;
        setPedidos(p.conteudo);
        setTotal(p.totalElementos);
      })
      // painel é apenas informativo: sem a lista, o bloco some
      .catch(() => !cancelado && setPedidos([]));
    return () => {
      cancelado = true;
    };
  }, [eu, pode]);

  if (pedidos.length === 0) return null;

  const titulo = decide ? "Pedidos de alteração para você decidir" : "Pedidos de alteração aguardando o docente";
  const descricao = decide
    ? "Alunos ou a recepção pediram para mudar estas reuniões confirmadas. Até a resposta, elas continuam no horário de hoje."
    : gestor
      ? "Reuniões confirmadas com pedido de alteração ainda sem resposta do docente."
      : "Pedidos de alteração das suas reuniões que ainda esperam a resposta do docente.";

  return (
    <Card role="region" aria-label={titulo} className="border-brand-300 bg-brand-50">
      <CardHeader>
        <CardTitle className="flex items-center gap-2 text-brand-900">
          <IconMessageQuestion className="size-5 text-brand-600" />
          {titulo}
          <Badge className="bg-primary text-primary-foreground">{total}</Badge>
        </CardTitle>
        <CardDescription>{descricao}</CardDescription>
      </CardHeader>
      <CardContent className="px-0">
        <ul className="divide-y divide-brand-200 border-t border-brand-200">
          {pedidos.map((p) => {
            const hoje = `${formatarData(p.dataAtual)} às ${formatarHora(p.horarioInicioAtual)}`;
            const pedido = `${formatarData(p.novaData)} às ${formatarHora(p.novoHorarioInicio)}`;
            return (
              <li key={p.id}>
                <Link
                  to={`/agendamentos?abrir=${p.idAgendamento}`}
                  className="flex items-center justify-between gap-3 px-4 py-3 transition-colors hover:bg-brand-100/60"
                >
                  <div className="min-w-0">
                    <p className="truncate text-sm font-medium">{p.topicoAtual}</p>
                    <p className="text-xs text-muted-foreground">
                      {hoje === pedido ? hoje : `${hoje} → ${pedido}`} · pedido por{" "}
                      {p.idSolicitante === eu?.id ? "você" : p.nomeSolicitante}
                    </p>
                    <p className="text-xs text-muted-foreground">
                      {decide ? "Responda até " : "O docente responde até "}
                      {prazoDeResposta(p)}
                    </p>
                  </div>
                  <span className="flex shrink-0 items-center gap-1 text-xs font-medium text-brand-700">
                    {decide ? "Decidir" : "Ver pedido"}
                    <IconChevronRight className="size-4" />
                  </span>
                </Link>
              </li>
            );
          })}
        </ul>
        {total > pedidos.length && (
          <p className="border-t border-brand-200 px-4 pt-3 text-xs text-muted-foreground">
            Mostrando os {pedidos.length} mais próximos de {total}.
          </p>
        )}
      </CardContent>
    </Card>
  );
}
