import { useEffect, useState } from "react";
import { IconAlertTriangle } from "@tabler/icons-react";
import { agendamentosApi } from "@/api/agendamentos";
import { paraHora, paraMinutos } from "@/utils/agenda";
import { formatarDataHora } from "@/utils/format";
import type { Agendamento } from "@/types";

// RN 13: no máximo 2 agendamentos criados por aluno a cada 24 horas.
const LIMITE_AGENDAMENTOS_ALUNO_24H = 2;
const DIA_MS = 24 * 3_600_000;

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
  const [porPessoa, setPorPessoa] = useState<Record<number, Agendamento[]>>({});
  const [criadosRecentes, setCriadosRecentes] = useState<Agendamento[]>([]);
  const chavePessoas = pessoas.map((p) => p.id).join(",");
  const idOrganizadorAluno = verificarLimite && organizador?.ehAluno ? organizador.id : null;

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
      .then((p) => {
        if (cancelado) return;
        const desde = Date.now() - DIA_MS;
        setCriadosRecentes(
          p.conteudo.filter(
            (a) =>
              a.idOrganizador === idOrganizadorAluno &&
              a.status !== "CANCELADO" &&
              !!a.criadoEm &&
              new Date(a.criadoEm).getTime() >= desde
          )
        );
      })
      .catch(() => !cancelado && setCriadosRecentes([]));
    return () => {
      cancelado = true;
    };
  }, [idOrganizadorAluno]);

  const avisos: string[] = [];

  if (horario && duracaoMinutos > 0) {
    const inicio = paraMinutos(horario);
    const fim = inicio + duracaoMinutos;
    for (const pessoa of pessoas) {
      const choque = (porPessoa[pessoa.id] ?? []).find(
        (a) =>
          a.dataReuniao === dataISO &&
          a.status !== "CANCELADO" &&
          a.id !== ignorarAgendamentoId &&
          paraMinutos(a.horarioInicio) < fim &&
          inicio < (a.horarioFim ? paraMinutos(a.horarioFim) : paraMinutos(a.horarioInicio) + (a.duracaoMinutos ?? 0))
      );
      if (choque) {
        const quem = pessoa.id === organizador?.id && organizador.ehEu ? "Você já tem" : `${pessoa.nome} já tem`;
        const ate = choque.horarioFim ? paraHora(paraMinutos(choque.horarioFim)) : "";
        avisos.push(`${quem} uma reunião das ${paraHora(paraMinutos(choque.horarioInicio))} às ${ate} nesse dia.`);
      }
    }
  }

  if (idOrganizadorAluno && criadosRecentes.length >= LIMITE_AGENDAMENTOS_ALUNO_24H) {
    const maisAntigo = criadosRecentes
      .map((a) => new Date(a.criadoEm as string).getTime())
      .sort((a, b) => a - b)[criadosRecentes.length - LIMITE_AGENDAMENTOS_ALUNO_24H];
    const liberaEm = formatarDataHora(new Date(maisAntigo + DIA_MS).toISOString());
    avisos.push(
      `${organizador?.ehEu ? "Você já criou" : "Este aluno já criou"} ${criadosRecentes.length} agendamentos nas últimas 24 horas; o limite é ${LIMITE_AGENDAMENTOS_ALUNO_24H} (RN 13). Um novo poderá ser criado a partir de ${liberaEm}.`
    );
  }

  if (avisos.length === 0) return null;

  return (
    <div className="flex flex-col gap-1.5 rounded-lg border border-amber-300 bg-amber-50 p-3 text-sm text-amber-800">
      {avisos.map((aviso) => (
        <p key={aviso} className="flex items-start gap-1.5">
          <IconAlertTriangle className="mt-0.5 size-4 shrink-0" />
          {aviso}
        </p>
      ))}
    </div>
  );
}
