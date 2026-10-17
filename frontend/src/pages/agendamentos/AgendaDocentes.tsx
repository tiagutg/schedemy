import { useEffect, useState } from "react";
import { IconAlertTriangle, IconCircleCheck } from "@tabler/icons-react";
import { agendamentosApi } from "@/api/agendamentos";
import { bloqueiosApi } from "@/api/bloqueios";
import { disponibilidadesApi } from "@/api/disponibilidades";
import Spinner from "@/components/common/Spinner";
import { cn } from "@/lib/utils";
import { calcularAgendaDoDia, diaSemanaDe, horarioCabe, paraHora } from "@/utils/agenda";
import { formatarData } from "@/utils/format";
import type { Agendamento, Bloqueio, Disponibilidade } from "@/types";

interface DadosDocente {
  disponibilidades: Disponibilidade[];
  /** null quando o perfil logado não pode consultar bloqueios. */
  bloqueios: Bloqueio[] | null;
  agendamentos: Agendamento[];
}

interface AgendaDocentesProps {
  docentes: { id: number; nome: string }[];
  dataISO: string;
  duracaoMinutos: number;
  horario: string;
  /** Na edição, a própria reunião não conta como ocupação. */
  ignorarAgendamentoId?: number | null;
  onEscolherHorario: (horario: string) => void;
}

const MAX_SUGESTOES = 16;

/**
 * Mostra, para cada docente convidado, quando ele atende no dia escolhido, o que
 * já está ocupado e os horários livres para a duração escolhida. Evita que a
 * pessoa só descubra no envio que o horário está fora da disponibilidade.
 */
export default function AgendaDocentes({
  docentes,
  dataISO,
  duracaoMinutos,
  horario,
  ignorarAgendamentoId,
  onEscolherHorario,
}: AgendaDocentesProps) {
  const [expandidos, setExpandidos] = useState<Record<number, boolean>>({});
  const chaveDocentes = docentes.map((d) => d.id).join(",");
  // Guarda para quais docentes/data os dados foram carregados; enquanto não bate, está carregando.
  const chave = `${chaveDocentes}|${dataISO}`;
  const [resultado, setResultado] = useState<{ chave: string; dados: Record<number, DadosDocente> }>({
    chave: "",
    dados: {},
  });
  const carregando = resultado.chave !== chave;
  const dados = resultado.dados;

  useEffect(() => {
    if (!dataISO || !chaveDocentes) return;
    let cancelado = false;
    const ids = chaveDocentes.split(",").map(Number);
    const chaveBusca = `${chaveDocentes}|${dataISO}`;
    Promise.all(
      ids.map(async (id) => {
        const [disponibilidades, bloqueios, agendamentos] = await Promise.all([
          disponibilidadesApi
            .listar({ usuarioId: id, diaSemana: diaSemanaDe(dataISO), ativo: true, size: 50 })
            .then((p) => p.conteudo),
          bloqueiosApi
            .listar({ usuarioId: id, size: 200 })
            .then((p) => p.conteudo)
            .catch(() => null),
          agendamentosApi
            .listarPorUsuario(id, { dataInicio: dataISO, dataFim: dataISO, size: 50 })
            .then((p) => p.conteudo),
        ]);
        return [id, { disponibilidades, bloqueios, agendamentos }] as const;
      })
    )
      .then((porDocente) => !cancelado && setResultado({ chave: chaveBusca, dados: Object.fromEntries(porDocente) }))
      .catch(() => !cancelado && setResultado({ chave: chaveBusca, dados: {} }));
    return () => {
      cancelado = true;
    };
  }, [chaveDocentes, dataISO]);

  if (docentes.length === 0) return null;

  const agendas = docentes.map((d) => {
    const info = dados[d.id];
    const agenda = info
      ? calcularAgendaDoDia({
          dataISO,
          duracaoMinutos,
          disponibilidades: info.disponibilidades,
          bloqueios: info.bloqueios ?? [],
          agendamentos: info.agendamentos,
          ignorarAgendamentoId,
          passoMinutos: 30,
        })
      : null;
    const encaixe = agenda && horario && duracaoMinutos ? horarioCabe(agenda, horario, duracaoMinutos) : null;
    return { docente: d, info, agenda, encaixe };
  });

  // O back-end aceita se ao menos um docente convidado atende no horário (RN 06).
  const algumAtende = agendas.some((a) => a.encaixe?.dentroDaJanela && !a.encaixe.conflito);
  const podeAvaliar = !carregando && !!horario && duracaoMinutos > 0 && agendas.every((a) => a.agenda);
  const semPermissaoBloqueios = agendas.some((a) => a.info && a.info.bloqueios === null);

  return (
    <section aria-label="Agenda dos docentes" className="flex flex-col gap-3 rounded-lg border border-brand-200 bg-brand-50 p-3.5">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <p className="text-sm font-medium text-brand-900">Agenda dos docentes em {formatarData(dataISO)}</p>
        {carregando && <Spinner className="size-4 text-brand-700" />}
      </div>

      {podeAvaliar && (
        <p
          className={cn(
            "flex items-start gap-1.5 text-sm",
            algumAtende ? "text-sage-700" : "text-amber-700 dark:text-amber-300"
          )}
        >
          {algumAtende ? (
            <IconCircleCheck className="mt-0.5 size-4 shrink-0" />
          ) : (
            <IconAlertTriangle className="mt-0.5 size-4 shrink-0" />
          )}
          {algumAtende
            ? `O horário ${horario} está livre na agenda de pelo menos um docente.`
            : `O horário ${horario} (${duracaoMinutos} min) não está livre para nenhum docente convidado. Escolha um horário sugerido abaixo.`}
        </p>
      )}

      {!duracaoMinutos && <p className="text-xs text-muted-foreground">Escolha a duração para ver os horários livres.</p>}

      <ul className="flex flex-col gap-3">
        {agendas.map(({ docente, agenda }) => (
          <li key={docente.id} className="flex flex-col gap-1.5 rounded-md bg-card p-2.5 text-sm shadow-xs">
            <p className="font-medium">{docente.nome}</p>
            {!agenda ? (
              <p className="text-xs text-muted-foreground">Carregando…</p>
            ) : agenda.janelas.length === 0 ? (
              <p className="text-xs text-amber-700 dark:text-amber-300">Não atende neste dia da semana.</p>
            ) : (
              <>
                <p className="text-xs text-muted-foreground">
                  Atende: {agenda.janelas.map((j) => `${paraHora(j.inicio)}–${paraHora(j.fim)}`).join(", ")}
                  {agenda.ocupados.length > 0 &&
                    ` · Ocupado: ${agenda.ocupados.map((o) => `${paraHora(o.inicio)}–${paraHora(o.fim)}${o.rotulo?.startsWith("Bloqueio") ? " (bloqueio)" : ""}`).join(", ")}`}
                </p>
                {duracaoMinutos > 0 &&
                  (agenda.horariosLivres.length === 0 ? (
                    <p className="text-xs text-amber-700 dark:text-amber-300">Sem horários livres para {duracaoMinutos} min neste dia.</p>
                  ) : (
                    <div className="flex flex-wrap gap-1">
                      {agenda.horariosLivres
                        .slice(0, expandidos[docente.id] ? undefined : MAX_SUGESTOES)
                        .map((h) => (
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
                      {agenda.horariosLivres.length > MAX_SUGESTOES && (
                        <button
                          type="button"
                          onClick={() =>
                            setExpandidos((atual) => ({ ...atual, [docente.id]: !atual[docente.id] }))
                          }
                          className="self-center px-1 text-xs font-medium text-brand-600 hover:underline"
                        >
                          {expandidos[docente.id] ? "mostrar menos" : `+${agenda.horariosLivres.length - MAX_SUGESTOES} horários`}
                        </button>
                      )}
                    </div>
                  ))}
              </>
            )}
          </li>
        ))}
      </ul>

      {semPermissaoBloqueios && (
        <p className="text-xs text-muted-foreground">
          Bloqueios de período dos docentes não aparecem para o seu perfil; o horário ainda pode estar bloqueado.
        </p>
      )}
    </section>
  );
}
