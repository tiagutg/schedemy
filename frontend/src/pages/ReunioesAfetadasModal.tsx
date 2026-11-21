import { useState, type FormEvent } from "react";
import { IconAlertTriangle } from "@tabler/icons-react";
import Modal from "@/components/common/Modal";
import FormActions from "@/components/common/FormActions";
import { FieldError, TextAreaField } from "@/components/common/fields";
import { Button } from "@/components/ui/button";
import { formatarData, formatarHora } from "@/utils/format";
import type { ReuniaoAfetada } from "@/types";

interface ReunioesAfetadasModalProps {
  reunioes: ReuniaoAfetada[] | null;
  /** Explica por que a lista apareceu de novo. */
  aviso?: string;
  confirmando: boolean;
  erro?: string;
  onConfirmar: (motivo: string) => void;
  onVoltar: () => void;
}

// Mesmo limite do motivo de cancelamento na API.
const MAX_MOTIVO = 500;

/**
 * RF 06/RF 07/RF 10: lista as reuniões que a mudança na agenda cancela (aluno, tópico, data e horário) e pede o
 * motivo (RF 11) antes de gravar. Os envolvidos recebem o aviso com o link para agendar de novo.
 */
export default function ReunioesAfetadasModal({
  reunioes,
  aviso,
  confirmando,
  erro,
  onConfirmar,
  onVoltar,
}: ReunioesAfetadasModalProps) {
  const [motivo, setMotivo] = useState("");
  const [faltaMotivo, setFaltaMotivo] = useState(false);
  const total = reunioes?.length ?? 0;
  const algumaEmMenosDeUmaHora = reunioes?.some((r) => r.menosDeUmaHora) ?? false;

  function enviar(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (!motivo.trim()) {
      setFaltaMotivo(true);
      return;
    }
    onConfirmar(motivo);
  }

  return (
    <Modal
      aberto={!!reunioes}
      onFechar={confirmando ? undefined : onVoltar}
      largura="max-w-lg"
      titulo={total === 1 ? "1 reunião será cancelada" : `${total} reuniões serão canceladas`}
      subtitulo="Com essa mudança, estas reuniões ficam fora do seu horário de atendimento. Ao confirmar, elas são canceladas e os envolvidos recebem um aviso (no sistema e por e-mail) para agendar de novo."
    >
      <form onSubmit={enviar} className="flex flex-col gap-4">
        {aviso && (
          <p className="rounded-lg border border-amber-300 bg-amber-50 px-3 py-2 text-sm text-amber-900 dark:border-amber-500/40 dark:bg-amber-500/10 dark:text-amber-100">
            {aviso}
          </p>
        )}

        <ul className="max-h-72 divide-y overflow-y-auto rounded-lg border" aria-label="Reuniões afetadas">
          {reunioes?.map((r) => (
            <li key={r.id} className="flex flex-col gap-0.5 px-3 py-2.5">
              <p className="text-sm font-medium">{r.topico}</p>
              <p className="text-xs text-muted-foreground">
                {formatarData(r.dataReuniao)}, {formatarHora(r.horarioInicio)}
                {r.horarioFim ? `–${formatarHora(r.horarioFim)}` : ""}
                {r.alunos.length > 0 && ` · ${r.alunos.join(", ")}`}
              </p>
              {r.outrosDocentes.length > 0 && (
                <p className="text-xs text-muted-foreground">
                  Também com {r.outrosDocentes.join(", ")}: a reunião é cancelada para todos.
                </p>
              )}
              {r.menosDeUmaHora && (
                <p className="flex items-center gap-1 text-xs font-medium text-amber-700 dark:text-amber-300">
                  <IconAlertTriangle className="size-3.5" />
                  Começa em menos de 1 hora.
                </p>
              )}
            </li>
          ))}
        </ul>

        {algumaEmMenosDeUmaHora && (
          <p className="text-xs text-muted-foreground">
            Reuniões que começam em menos de 1 hora também são canceladas: avise os envolvidos se puder.
          </p>
        )}

        <TextAreaField
          label="Motivo do cancelamento"
          required
          placeholder="Ex.: Reunião do colegiado no mesmo horário"
          maxLength={MAX_MOTIVO}
          value={motivo}
          error={faltaMotivo && !motivo.trim() ? "Informe o motivo: ele vai no aviso aos envolvidos." : undefined}
          onChange={(e) => setMotivo(e.target.value)}
        />

        {erro && <FieldError>{erro}</FieldError>}

        <FormActions>
          <Button type="button" variant="outline" onClick={onVoltar} disabled={confirmando}>
            Voltar
          </Button>
          <Button type="submit" variant="destructive" disabled={confirmando}>
            {confirmando ? "Salvando…" : total === 1 ? "Cancelar a reunião e salvar" : `Cancelar ${total} reuniões e salvar`}
          </Button>
        </FormActions>
      </form>
    </Modal>
  );
}
