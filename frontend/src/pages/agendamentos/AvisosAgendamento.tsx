import { useEffect, useState } from "react";
import { IconAlertTriangle } from "@tabler/icons-react";
import { agendamentosApi } from "@/api/agendamentos";
import { useAuth } from "@/context/AuthContext";
import { encontrarConflito, LIMITE_AGENDAMENTOS_ALUNO_24H, paraHora, paraMinutos, situacaoLimiteAluno } from "@/utils/agenda";
import { formatarDataHora } from "@/utils/format";
import type { Agendamento } from "@/types";

interface AvisosAgendamentoProps {
  /** Organizador e convidados que não aparecem no painel de agenda dos docentes. */
  pessoas: { id: number; nome: string }[];
  organizador: { id: number; ehAluno: boolean; ehEu: boolean } | null;
  dataISO: string;
  horario: string;
  duracaoMinutos: number;
  ignorarAgendamentoId?: number | null;
  /** Na edição o limite não se aplica: nenhum agendamento novo é criado. */
  verificarLimite: boolean;
}

/**
 * Antecipa duas regras que o back-end só acusa no envio: o choque de horário
 * com outra reunião de algum envolvido (RN 01) e o limite de agendamentos do
 * aluno em 24 horas (RN 13).
 */
export default function AvisosAgendamento({
  pessoas,
  organizador,
  dataISO,
  horario,
  duracaoMinutos,
  ignorarAgendamentoId,
  verificarLimite,
}: AvisosAgendamentoProps) {
  const { perfilVinculado: eu, perfil } = useAuth();
  // A API só mostra a agenda do próprio usuário (ou de todos, para a administração).
  const consultaveis = perfil === "ADMIN" ? pessoas : pessoas.filter((p) => p.id === eu?.id);
  const [porPessoa, setPorPessoa] = useState<Record<number, Agendamento[]>>({});
  // Agendamentos recentes do organizador e o momento da consulta (base da janela de 24 h).
  const [recentes, setRecentes] = useState<{ lista: Agendamento[]; agora: number }>({ lista: [], agora: 0 });
  const chavePessoas = consultaveis.map((p) => p.id).join(",");
  const organizadorConsultavel = !!organizador && consultaveis.some((p) => p.id === organizador.id);
  const idOrganizadorAluno = verificarLimite && organizador?.ehAluno && organizadorConsultavel ? organizador.id : null;

  useEffect(() => {
    if (!dataISO || !chavePessoas) return;
    let cancelado = false;
    const ids = chavePessoas.split(",").map(Number);
    Promise.all(
      ids.map((id) =>
        agendamentosApi
          .listarPorUsuario(id, { dataInicio: dataISO, dataFim: dataISO, size: 50 })
          .then((p) => [id, p.conteudo] as const)
          .catch(() => [id, []] as const)
      )
    ).then((resultado) => !cancelado && setPorPessoa(Object.fromEntries(resultado)));
    return () => {
      cancelado = true;
    };
  }, [chavePessoas, dataISO]);

  useEffect(() => {
    if (!idOrganizadorAluno) return;
    let cancelado = false;
    agendamentosApi
      .listarPorUsuario(idOrganizadorAluno, { size: 20, sort: "criadoEm,desc" })
      .then((p) => !cancelado && setRecentes({ lista: p.conteudo, agora: Date.now() }))
      .catch(() => !cancelado && setRecentes({ lista: [], agora: 0 }));
    return () => {
      cancelado = true;
    };
  }, [idOrganizadorAluno]);

  const avisos: string[] = [];

  if (horario && duracaoMinutos > 0) {
    for (const pessoa of consultaveis) {
      const choque = encontrarConflito(porPessoa[pessoa.id] ?? [], { dataISO, horario, duracaoMinutos, ignorarAgendamentoId });
      if (choque) {
        const quem = pessoa.id === organizador?.id && organizador.ehEu ? "Você já tem" : `${pessoa.nome} já tem`;
        const ate = choque.horarioFim ? paraHora(paraMinutos(choque.horarioFim)) : "";
        avisos.push(`${quem} uma reunião das ${paraHora(paraMinutos(choque.horarioInicio))} às ${ate} nesse dia.`);
      }
    }
  }

  const limite = idOrganizadorAluno ? situacaoLimiteAluno(recentes.lista, idOrganizadorAluno, recentes.agora) : null;
  if (limite?.atingido && limite.liberaEm) {
    const liberaEm = formatarDataHora(limite.liberaEm.toISOString());
    avisos.push(
      `${organizador?.ehEu ? "Você já criou" : "Este aluno já criou"} ${limite.recentes} agendamentos nas últimas 24 horas; o limite é ${LIMITE_AGENDAMENTOS_ALUNO_24H}. Um novo poderá ser criado a partir de ${liberaEm}.`
    );
  }

  if (avisos.length === 0) return null;

  return (
    <div className="flex flex-col gap-1.5 rounded-lg border border-amber-300 bg-amber-50 p-3 text-sm text-amber-800 dark:border-amber-500/40 dark:bg-amber-500/10 dark:text-amber-200">
      {avisos.map((aviso) => (
        <p key={aviso} className="flex items-start gap-1.5">
          <IconAlertTriangle className="mt-0.5 size-4 shrink-0" />
          {aviso}
        </p>
      ))}
    </div>
  );
}
