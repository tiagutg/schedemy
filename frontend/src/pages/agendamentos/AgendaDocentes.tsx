import { useEffect, useState } from "react";
import { IconAlertTriangle, IconCircleCheck } from "@tabler/icons-react";
import { agendamentosApi } from "@/api/agendamentos";
import { bloqueiosApi } from "@/api/bloqueios";
import type { ApiError } from "@/api/client";
import { disponibilidadesApi } from "@/api/disponibilidades";
import Spinner from "@/components/common/Spinner";
import { useAuth } from "@/context/AuthContext";
import { cn } from "@/lib/utils";
import { calcularAgendaDoDia, diaSemanaDe, paraHora } from "@/utils/agenda";
import { formatarData } from "@/utils/format";
import type { Agendamento, Bloqueio, Disponibilidade } from "@/types";

interface DadosDocente {
  disponibilidades: Disponibilidade[];
  /** Só da agenda do próprio docente logado: a dos outros não é exposta (RNF 15). */
  bloqueios: Bloqueio[];
  agendamentos: Agendamento[];
}

interface AgendaDocentesProps {
  /** Professores e coordenadores envolvidos (organizador e convidados). */
  docentes: { id: number; nome: string }[];
  /** Todos os envolvidos: o horário precisa estar livre para cada um deles (RF 14). */
  idsEnvolvidos: number[];
  idDuracao: string;
  dataISO: string;
  duracaoMinutos: number;
  horario: string;
  /** Na edição, a própria reunião não conta como ocupação. */
  ignorarAgendamentoId?: number | null;
  onEscolherHorario: (horario: string) => void;
}

const MAX_SUGESTOES = 16;

/**
 * Mostra os horários em que todos os envolvidos estão livres (RF 14), calculados
 * pelo back-end com a mesma regra que valida o agendamento ao salvar. Para cada
 * docente aparece o horário de atendimento (público); reuniões e bloqueios só
 * aparecem na agenda do próprio docente logado.
 */
export default function AgendaDocentes({
  docentes,
  idsEnvolvidos,
  idDuracao,
  dataISO,
  duracaoMinutos,
  horario,
  ignorarAgendamentoId,
  onEscolherHorario,
}: AgendaDocentesProps) {
  const { perfilVinculado: eu } = useAuth();
  const [expandido, setExpandido] = useState(false);
  const meuId = eu?.id ?? null;

  // Horários livres para todos: recarrega quando muda quem participa, a data ou a duração.
  const chaveLivres = `${idsEnvolvidos.join(",")}|${dataISO}|${idDuracao}|${ignorarAgendamentoId ?? ""}`;
  const [livres, setLivres] = useState<{ chave: string; horarios: string[]; erro: string }>({
    chave: "",
    horarios: [],
    erro: "",
  });
  const carregandoLivres = !!idDuracao && livres.chave !== chaveLivres;

  useEffect(() => {
    if (!dataISO || !idDuracao || idsEnvolvidos.length === 0) return;
    const controle = new AbortController();
    const chave = chaveLivres;
    agendamentosApi
      .horariosLivres(
        { data: dataISO, idDuracao, idsUsuarios: idsEnvolvidos, ignorarAgendamentoId },
        controle.signal
      )
      .then((r) => setLivres({ chave, horarios: r.horarios.map((h) => h.slice(0, 5)), erro: "" }))
      .catch((e) => {
        if (!controle.signal.aborted) setLivres({ chave, horarios: [], erro: (e as ApiError).message });
      });
    return () => controle.abort();
    // chaveLivres resume idsEnvolvidos, dataISO, idDuracao e ignorarAgendamentoId.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [chaveLivres]);

  // Horário de atendimento de cada docente e, para o próprio docente, a agenda do dia.
  const chaveDocentes = docentes.map((d) => d.id).join(",");
  const chaveAgendas = `${chaveDocentes}|${dataISO}`;
  const [agendas, setAgendas] = useState<{ chave: string; dados: Record<number, DadosDocente> }>({
    chave: "",
    dados: {},
  });

  useEffect(() => {
    if (!dataISO || !chaveDocentes) return;
    let cancelado = false;
    const ids = chaveDocentes.split(",").map(Number);
    const chave = `${chaveDocentes}|${dataISO}`;
    Promise.all(
      ids.map(async (id) => {
        const ehEu = id === meuId;
        const [disponibilidades, bloqueios, agendamentos] = await Promise.all([
          disponibilidadesApi
            .listar({ usuarioId: id, diaSemana: diaSemanaDe(dataISO), ativo: true, size: 50 })
            .then((p) => p.conteudo),
          ehEu
            ? bloqueiosApi
                .listar({ usuarioId: id, size: 200 })
                .then((p) => p.conteudo)
                .catch(() => [])
            : [],
          ehEu
            ? agendamentosApi
                .listarPorUsuario(id, { dataInicio: dataISO, dataFim: dataISO, size: 50 })
                .then((p) => p.conteudo)
                .catch(() => [])
            : [],
        ]);
        return [id, { disponibilidades, bloqueios, agendamentos }] as const;
      })
    )
      .then((porDocente) => !cancelado && setAgendas({ chave, dados: Object.fromEntries(porDocente) }))
      .catch(() => !cancelado && setAgendas({ chave, dados: {} }));
    return () => {
      cancelado = true;
    };
  }, [chaveDocentes, dataISO, meuId]);

  if (docentes.length === 0) return null;

  const carregandoAgendas = agendas.chave !== chaveAgendas;
  const sugestoes = livres.chave === chaveLivres ? livres.horarios : [];
  const erroLivres = livres.chave === chaveLivres ? livres.erro : "";
  const podeAvaliar = !carregandoLivres && !erroLivres && !!horario && duracaoMinutos > 0;
  const horarioLivre = sugestoes.includes(horario);

  return (
    <section aria-label="Agenda dos docentes" className="flex flex-col gap-3 rounded-lg border border-brand-200 bg-brand-50 p-3.5">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <p className="text-sm font-medium text-brand-900">Horários livres em {formatarData(dataISO)}</p>
        {(carregandoLivres || carregandoAgendas) && <Spinner className="size-4 text-brand-700" />}
      </div>

      {podeAvaliar && (
        <p
          className={cn(
            "flex items-start gap-1.5 text-sm",
            horarioLivre ? "text-sage-700" : "text-amber-700 dark:text-amber-300"
          )}
        >
          {horarioLivre ? (
            <IconCircleCheck className="mt-0.5 size-4 shrink-0" />
          ) : (
            <IconAlertTriangle className="mt-0.5 size-4 shrink-0" />
          )}
          {horarioLivre
            ? `O horário ${horario} está livre para todos os envolvidos.`
            : `O horário ${horario} (${duracaoMinutos} min) não está livre para todos os envolvidos. Escolha um horário sugerido abaixo.`}
        </p>
      )}

      {!idDuracao ? (
        <p className="text-xs text-muted-foreground">Escolha a duração para ver os horários livres.</p>
      ) : erroLivres ? (
        <p className="text-xs text-destructive">{erroLivres}</p>
      ) : (
        !carregandoLivres &&
        (sugestoes.length === 0 ? (
          <p className="text-xs text-amber-700 dark:text-amber-300">
            Nenhum horário livre para todos os envolvidos neste dia. Tente outra data.
          </p>
        ) : (
          <div className="flex flex-wrap gap-1">
            {sugestoes.slice(0, expandido ? undefined : MAX_SUGESTOES).map((h) => (
              <button
                key={h}
                type="button"
                onClick={() => onEscolherHorario(h)}
                className={cn(
                  "rounded-md border px-2 py-0.5 text-xs tabular-nums transition-colors",
                  h === horario
                    ? "border-primary bg-primary text-primary-foreground"
                    : "border-brand-200 bg-card text-brand-700 hover:bg-brand-100"
                )}
              >
                {h}
              </button>
            ))}
            {sugestoes.length > MAX_SUGESTOES && (
              <button
                type="button"
                onClick={() => setExpandido((atual) => !atual)}
                className="self-center px-1 text-xs font-medium text-brand-600 hover:underline"
              >
                {expandido ? "mostrar menos" : `+${sugestoes.length - MAX_SUGESTOES} horários`}
              </button>
            )}
          </div>
        ))
      )}

      <ul className="flex flex-col gap-2">
        {docentes.map((docente) => {
          const info = carregandoAgendas ? undefined : agendas.dados[docente.id];
          const agenda = info
            ? calcularAgendaDoDia({
                dataISO,
                duracaoMinutos,
                disponibilidades: info.disponibilidades,
                bloqueios: info.bloqueios,
                agendamentos: info.agendamentos,
                ignorarAgendamentoId,
              })
            : null;
          return (
            <li key={docente.id} className="flex flex-col gap-1 rounded-md bg-card p-2.5 text-sm shadow-xs">
              <p className="font-medium">
                {docente.nome}
                {docente.id === meuId && <span className="font-normal text-muted-foreground"> (você)</span>}
              </p>
              {!agenda ? (
                <p className="text-xs text-muted-foreground">Carregando…</p>
              ) : agenda.janelas.length === 0 ? (
                <p className="text-xs text-amber-700 dark:text-amber-300">Não atende neste dia da semana.</p>
              ) : (
                <p className="text-xs text-muted-foreground">
                  Atende: {agenda.janelas.map((j) => `${paraHora(j.inicio)}–${paraHora(j.fim)}`).join(", ")}
                  {agenda.ocupados.length > 0 &&
                    ` · Ocupado: ${agenda.ocupados.map((o) => `${paraHora(o.inicio)}–${paraHora(o.fim)}${o.rotulo?.startsWith("Bloqueio") ? " (bloqueio)" : ""}`).join(", ")}`}
                </p>
              )}
            </li>
          );
        })}
      </ul>

      <p className="text-xs text-muted-foreground">
        Os horários sugeridos já descontam reuniões marcadas e períodos bloqueados de todos os envolvidos, sem
        mostrar a agenda de ninguém.
      </p>
    </section>
  );
}
