import { useEffect, useMemo, useState } from "react";
import { IconAlertTriangle, IconCalendarSearch, IconCircleCheck } from "@tabler/icons-react";
import { agendamentosApi } from "@/api/agendamentos";
import { bloqueiosApi } from "@/api/bloqueios";
import type { ApiError } from "@/api/client";
import { disponibilidadesApi } from "@/api/disponibilidades";
import Spinner from "@/components/common/Spinner";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/context/AuthContext";
import { cn } from "@/lib/utils";
import {
  calcularAgendaDoDia,
  diaSemanaDe,
  diasDeAtendimentoEmComum,
  paraHora,
  quemImpedeHorarioEmComum,
  resumirSemana,
  rotuloDia,
  temHorarioEmComumNaSemana,
} from "@/utils/agenda";
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
  /** Escolhe outro dia (e já o primeiro horário livre nele). */
  onEscolherData?: (dataISO: string, horario: string) => void;
  /** Tira um convidado da reunião (oferecido para docente sem horário de atendimento). */
  onRemoverConvidado?: (id: number) => void;
  /** O organizador não pode ser tirado da própria reunião. */
  idOrganizador?: number | null;
}

const MAX_SUGESTOES = 16;
// Dias consultados no back-end para achar os próximos dias livres para todos.
const DIAS_CONSULTADOS = 10;
const DIAS_EXIBIDOS = 6;

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
  onEscolherData,
  onRemoverConvidado,
  idOrganizador,
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

  // Grade semanal de cada docente (pública): mostra em que dias atende e indica os dias candidatos.
  const [grades, setGrades] = useState<{ chave: string; dados: Record<number, Disponibilidade[]> }>({
    chave: "",
    dados: {},
  });
  useEffect(() => {
    if (!chaveDocentes) return;
    let cancelado = false;
    const chave = chaveDocentes;
    Promise.all(
      chaveDocentes.split(",").map(async (id) => {
        const p = await disponibilidadesApi.listar({ usuarioId: id, ativo: true, size: 50 });
        return [Number(id), p.conteudo] as const;
      })
    )
      .then((lista) => !cancelado && setGrades({ chave, dados: Object.fromEntries(lista) }))
      .catch(() => !cancelado && setGrades({ chave, dados: {} }));
    return () => {
      cancelado = true;
    };
  }, [chaveDocentes]);
  const gradesProntas = grades.chave === chaveDocentes;

  // Próximos dias em que todos atendem, confirmados no back-end (descontando reuniões e bloqueios).
  const candidatos = useMemo(
    () =>
      gradesProntas
        ? diasDeAtendimentoEmComum(
            chaveDocentes.split(",").map((id) => grades.dados[Number(id)] ?? []),
            new Date(),
            30,
            Math.max(duracaoMinutos, 1)
          ).slice(
            0,
            DIAS_CONSULTADOS
          )
        : [],
    [gradesProntas, grades, chaveDocentes, duracaoMinutos]
  );
  const chaveDias = `${idsEnvolvidos.join(",")}|${idDuracao}|${ignorarAgendamentoId ?? ""}|${candidatos.join(",")}`;
  const [diasLivres, setDiasLivres] = useState<{ chave: string; dias: { data: string; horarios: string[] }[] }>({
    chave: "",
    dias: [],
  });
  useEffect(() => {
    if (!idDuracao || idsEnvolvidos.length === 0 || !gradesProntas) return;
    const controle = new AbortController();
    const chave = chaveDias;
    Promise.all(
      candidatos.map((data) =>
        agendamentosApi
          .horariosLivres({ data, idDuracao, idsUsuarios: idsEnvolvidos, ignorarAgendamentoId }, controle.signal)
          .then((r) => ({ data, horarios: r.horarios.map((h) => h.slice(0, 5)) }))
          .catch(() => ({ data, horarios: [] as string[] }))
      )
    ).then((dias) => {
      if (!controle.signal.aborted) setDiasLivres({ chave, dias: dias.filter((d) => d.horarios.length > 0) });
    });
    return () => controle.abort();
    // chaveDias resume idsEnvolvidos, idDuracao, ignorarAgendamentoId e os candidatos.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [chaveDias, gradesProntas]);
  const carregandoDias = !!idDuracao && (!gradesProntas || diasLivres.chave !== chaveDias);
  const semGrade = gradesProntas ? docentes.filter((d) => (grades.dados[d.id] ?? []).length === 0) : [];
  // Nenhum dia serve para todos: explica o motivo e quem trava, em vez de mandar trocar a data.
  const nenhumDiaLivre =
    !!onEscolherData && !!idDuracao && !carregandoDias && semGrade.length === 0 && diasLivres.dias.length === 0;
  const gradesDosDocentes = docentes.map((d) => grades.dados[d.id] ?? []);
  const semComumNaSemana =
    nenhumDiaLivre && docentes.length > 1 && !temHorarioEmComumNaSemana(gradesDosDocentes, duracaoMinutos);
  const quemImpede = semComumNaSemana
    ? quemImpedeHorarioEmComum(gradesDosDocentes, duracaoMinutos).map((i) => docentes[i])
    : [];
  const ocultarDia = semGrade.length > 0 || nenhumDiaLivre;

  if (docentes.length === 0) return null;

  const carregandoAgendas = agendas.chave !== chaveAgendas;
  const sugestoes = livres.chave === chaveLivres ? livres.horarios : [];
  const erroLivres = livres.chave === chaveLivres ? livres.erro : "";
  // Com docente sem grade, o aviso de cima já explica: não repete "horário não está livre".
  const podeAvaliar = !carregandoLivres && !erroLivres && !!horario && duracaoMinutos > 0 && !ocultarDia;
  const horarioLivre = sugestoes.includes(horario);

  return (
    <section aria-label="Agenda dos docentes" className="flex flex-col gap-3 rounded-lg border border-brand-200 bg-brand-50 p-3.5">
      {onEscolherData && idDuracao && (
        <div className="flex flex-col gap-2 border-b border-brand-200 pb-3" aria-label="Próximos dias livres" role="group">
          <p className="flex items-center gap-1.5 text-sm font-medium text-brand-900">
            <IconCalendarSearch className="size-4" />
            Próximos dias com horário livre para todos
            {carregandoDias && <Spinner className="size-3.5 text-brand-700" />}
          </p>
          {semGrade.length > 0 ? (
            <ul className="flex flex-col gap-1.5">
              {semGrade.map((d) => (
                <li
                  key={d.id}
                  className="flex flex-wrap items-center justify-between gap-2 rounded-md border border-amber-300 bg-amber-50 px-2.5 py-1.5 text-xs text-amber-900 dark:border-amber-500/40 dark:bg-amber-500/10 dark:text-amber-100"
                >
                  <span>
                    <span className="font-medium">{d.nome}</span> ainda não cadastrou horários de atendimento, então
                    não dá para agendar com essa pessoa.
                  </span>
                  {onRemoverConvidado && d.id !== idOrganizador && (
                    <Button type="button" variant="outline" size="sm" className="h-7" onClick={() => onRemoverConvidado(d.id)}>
                      Tirar da reunião
                    </Button>
                  )}
                </li>
              ))}
            </ul>
          ) : carregandoDias ? (
            <p className="text-xs text-muted-foreground">Procurando dias livres…</p>
          ) : diasLivres.dias.length === 0 ? (
            <div className="flex flex-col gap-1.5 text-xs text-amber-700 dark:text-amber-300">
              <p>
                {semComumNaSemana
                  ? `Esses ${docentes.length} docentes não têm horário de atendimento em comum na semana, então nenhum dia serve para todos.`
                  : "Os horários em comum já estão ocupados ou bloqueados nos próximos 30 dias. Tente outra duração ou outros convidados."}
              </p>
              {quemImpede.length > 0 ? (
                <ul className="flex flex-col gap-1.5">
                  {quemImpede.map((d) => (
                    <li
                      key={d.id}
                      className="flex flex-wrap items-center justify-between gap-2 rounded-md border border-amber-300 bg-amber-50 px-2.5 py-1.5 text-amber-900 dark:border-amber-500/40 dark:bg-amber-500/10 dark:text-amber-100"
                    >
                      <span>
                        Sem <span className="font-medium">{d.nome}</span>, os demais têm horário em comum.
                      </span>
                      {onRemoverConvidado && d.id !== idOrganizador && (
                        <Button type="button" variant="outline" size="sm" className="h-7" onClick={() => onRemoverConvidado(d.id)}>
                          Tirar da reunião
                        </Button>
                      )}
                    </li>
                  ))}
                </ul>
              ) : (
                semComumNaSemana && <p>Tente convidar menos docentes de uma vez: veja abaixo quando cada um atende.</p>
              )}
            </div>
          ) : (
            <div className="grid grid-cols-2 gap-1.5 sm:grid-cols-3">
              {diasLivres.dias.slice(0, DIAS_EXIBIDOS).map((d) => (
                <button
                  key={d.data}
                  type="button"
                  aria-pressed={d.data === dataISO}
                  onClick={() => onEscolherData(d.data, d.horarios[0])}
                  className={cn(
                    "flex min-w-0 flex-col items-start rounded-md border px-2.5 py-1.5 text-left transition-colors",
                    d.data === dataISO
                      ? "border-primary bg-primary text-primary-foreground"
                      : "border-brand-200 bg-card text-brand-900 hover:bg-brand-100"
                  )}
                >
                  <span className="text-xs font-medium">{rotuloDia(d.data)}</span>
                  <span className={cn("text-[11px]", d.data === dataISO ? "opacity-90" : "text-muted-foreground")}>
                    {d.horarios.length} {d.horarios.length === 1 ? "horário" : "horários"}, das {d.horarios[0]}
                  </span>
                </button>
              ))}
            </div>
          )}
        </div>
      )}

      {!ocultarDia && (
        <div className="flex flex-wrap items-center justify-between gap-2">
          <p className="text-sm font-medium text-brand-900">Horários livres em {formatarData(dataISO)}</p>
          {(carregandoLivres || carregandoAgendas) && <Spinner className="size-4 text-brand-700" />}
        </div>
      )}

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
      ) : ocultarDia ? null : erroLivres ? (
        <p className="text-xs text-destructive">{erroLivres}</p>
      ) : (
        !carregandoLivres &&
        (sugestoes.length === 0 ? (
          <p className="text-xs text-amber-700 dark:text-amber-300">
            Nenhum horário livre para todos os envolvidos neste dia.{" "}
            {onEscolherData && diasLivres.dias.length > 0 ? "Escolha um dos dias acima." : "Tente outra data."}
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
          const grade = gradesProntas ? (grades.dados[docente.id] ?? []) : null;
          const semana = grade && grade.length > 0 ? resumirSemana(grade) : "";
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
                <p className="text-xs text-amber-700 dark:text-amber-300">
                  {grade && grade.length === 0
                    ? "Ainda não cadastrou horários de atendimento."
                    : "Não atende neste dia da semana."}
                </p>
              ) : (
                <p className="text-xs text-muted-foreground">
                  Neste dia: {agenda.janelas.map((j) => `${paraHora(j.inicio)}–${paraHora(j.fim)}`).join(", ")}
                  {agenda.ocupados.length > 0 &&
                    ` · Ocupado: ${agenda.ocupados.map((o) => `${paraHora(o.inicio)}–${paraHora(o.fim)}${o.rotulo?.startsWith("Bloqueio") ? " (bloqueio)" : ""}`).join(", ")}`}
                </p>
              )}
              {semana && <p className="text-xs text-muted-foreground">Na semana: {semana}</p>}
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
