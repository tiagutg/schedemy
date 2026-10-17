import { useEffect, useState } from "react";
import { IconCalendarRepeat, IconCheck, IconExternalLink, IconPencil, IconX } from "@tabler/icons-react";
import Modal from "@/components/common/Modal";
import ConfirmDialog from "@/components/common/ConfirmDialog";
import FormActions from "@/components/common/FormActions";
import Pill from "@/components/common/Pill";
import { TextAreaField } from "@/components/common/fields";
import { Button } from "@/components/ui/button";
import { agendamentosApi } from "@/api/agendamentos";
import { registrosMotivoApi } from "@/api/registrosMotivo";
import type { ApiError } from "@/api/client";
import { useToast } from "@/context/ToastContext";
import { useAuth } from "@/context/AuthContext";
import { CORES_STATUS_AGENDAMENTO, rotularEnum } from "@/utils/domain";
import { formatarData, formatarDataHora, formatarHora } from "@/utils/format";
import type { Agendamento, Participante, RegistroMotivo } from "@/types";

interface AgendamentoDetailModalProps {
  agendamento: Agendamento | null;
  onFechar: () => void;
  onAlterado: () => void;
  /** Abre o formulário para editar ou remarcar este agendamento. */
  onAlterar?: (modo: "editar" | "remarcar", agendamento: Agendamento) => void;
}

export default function AgendamentoDetailModal({ agendamento, onFechar, onAlterado, onAlterar }: AgendamentoDetailModalProps) {
  const toast = useToast();
  const { pode, perfilVinculado: eu } = useAuth();
  const [participanteRecusando, setParticipanteRecusando] = useState<Participante | null>(null);
  const [motivoRecusa, setMotivoRecusa] = useState("");
  const [respondendo, setRespondendo] = useState(false);

  const [modalCancelar, setModalCancelar] = useState(false);
  const [motivoCancelamento, setMotivoCancelamento] = useState("");
  const [cancelando, setCancelando] = useState(false);

  const [modalRemover, setModalRemover] = useState(false);
  const [removendo, setRemovendo] = useState(false);

  // Histórico de motivos (cancelamentos, recusas e edições) registrado pela API (RF 11).
  const [historico, setHistorico] = useState<RegistroMotivo[]>([]);
  // Momento de referência para os prazos de edição/remarcação (a página recria o modal por agendamento).
  const [agora] = useState(() => Date.now());
  useEffect(() => {
    if (!agendamento) return;
    let cancelado = false;
    registrosMotivoApi
      .listarPorAgendamento(agendamento.id, { size: 50, sort: "criadoEm,desc" })
      .then((p) => !cancelado && setHistorico(p.conteudo))
      .catch(() => !cancelado && setHistorico([]));
    return () => {
      cancelado = true;
    };
  }, [agendamento]);

  if (!agendamento) return null;
  const a = agendamento;
  // Além do estado do agendamento, cada ação depende da permissão do perfil.
  const podeCancelar =
    a.status !== "CANCELADO" && a.status !== "CONCLUIDO" && pode("agendamentos.cancelar");
  // Editar e remarcar são do organizador; recepção e administração podem fazer por ele.
  const podeGerir = a.idOrganizador === eu?.id || pode("agendamentos.organizarPorOutros");
  const horasAteReuniao = (new Date(`${a.dataReuniao}T${a.horarioInicio}`).getTime() - agora) / 3_600_000;
  // RN 14: edição direta só de pendentes, com 24 h de antecedência. Fora disso, remarca-se
  // (nova reunião ligada à original, que é cancelada — exige 1 h de antecedência, RN 05).
  const podeEditar = podeGerir && a.status === "PENDENTE" && horasAteReuniao >= 24;
  const podeRemarcar =
    podeGerir &&
    !podeEditar &&
    ["PENDENTE", "AGUARDANDO_RESPOSTA", "CONFIRMADO"].includes(a.status) &&
    horasAteReuniao >= 1;
  // A remoção definitiva não consta nos casos de uso: fica restrita à administração.
  const podeRemover =
    (a.status === "PENDENTE" || a.status === "CANCELADO") && pode("agendamentos.remover");
  // RF 19: aceitar/recusar convite é do convidado (aluno, professor ou coordenador),
  // e cada um responde só o próprio convite; a administração pode responder por outros.
  const podeResponderPor = (p: Participante) =>
    pode("agendamentos.responderPorOutros") || (pode("agendamentos.responder") && p.idUsuario === eu?.id);

  async function responder(participante: Participante, aceitar: boolean, motivo?: string) {
    setRespondendo(true);
    try {
      await agendamentosApi.responder(a.id, { idUsuario: participante.idUsuario, aceitar, motivo: motivo || null });
      const quem = participante.idUsuario === eu?.id ? "Você" : participante.nomeUsuario;
      toast.sucesso(aceitar ? `${quem} aceitou o convite.` : `${quem} recusou o convite.`);
      setParticipanteRecusando(null);
      setMotivoRecusa("");
      onAlterado();
    } catch (e) {
      toast.erro((e as ApiError).message);
    } finally {
      setRespondendo(false);
    }
  }

  async function confirmarCancelamento() {
    if (!eu) return toast.erro("Não foi possível identificar o usuário logado.");
    if (!motivoCancelamento.trim()) return toast.erro("Informe o motivo do cancelamento.");
    setCancelando(true);
    try {
      // O cancelamento é registrado em nome de quem está logado (RF 11).
      await agendamentosApi.cancelar(a.id, {
        idUsuario: eu.id,
        motivo: motivoCancelamento,
        confirmar: true,
      });
      toast.sucesso("Agendamento cancelado.");
      setModalCancelar(false);
      onAlterado();
      onFechar();
    } catch (e) {
      toast.erro((e as ApiError).message);
    } finally {
      setCancelando(false);
    }
  }

  async function confirmarRemocao() {
    setRemovendo(true);
    try {
      await agendamentosApi.remover(a.id);
      toast.sucesso("Agendamento removido.");
      onAlterado();
      onFechar();
    } catch (e) {
      toast.erro((e as ApiError).message);
    } finally {
      setRemovendo(false);
    }
  }

  return (
    <>
      <Modal aberto={!!agendamento} onFechar={onFechar} titulo={a.topico} subtitulo={`Agendamento #${a.id}`} largura="max-w-2xl">
        <div className="flex flex-col gap-5">
          <div className="flex flex-wrap items-center gap-2">
            <Pill className={CORES_STATUS_AGENDAMENTO[a.status]}>{rotularEnum(a.status)}</Pill>
            <Pill className="bg-ink-100 text-ink-800">{rotularEnum(a.formato)}</Pill>
            <Pill className="bg-ink-100 text-ink-800">{a.duracaoMinutos} min</Pill>
          </div>

          <div className="grid grid-cols-2 gap-4 rounded-lg border bg-muted/50 p-4 text-sm sm:grid-cols-4">
            <div>
              <p className="text-xs text-muted-foreground">Data</p>
              <p className="font-medium">{formatarData(a.dataReuniao)}</p>
            </div>
            <div>
              <p className="text-xs text-muted-foreground">Horário</p>
              <p className="font-medium">
                {formatarHora(a.horarioInicio)} – {formatarHora(a.horarioFim)}
              </p>
            </div>
            <div className="col-span-2">
              <p className="text-xs text-muted-foreground">Organizador</p>
              <p className="font-medium">{a.nomeOrganizador}</p>
            </div>
          </div>

          {a.idAgendamentoOrigem && (
            <p className="flex items-center gap-1.5 text-sm text-muted-foreground">
              <IconCalendarRepeat className="size-4 shrink-0" />
              Remarcação do agendamento #{a.idAgendamentoOrigem}
            </p>
          )}

          {a.resumo && (
            <div className="flex flex-col gap-1.5">
              <p className="text-sm font-medium">Resumo</p>
              <p className="text-sm text-muted-foreground">{a.resumo}</p>
            </div>
          )}

          {a.salaVirtual && (
            <div className="rounded-lg border p-3.5">
              <p className="text-xs text-muted-foreground">Sala virtual</p>
              <a
                href={a.salaVirtual.linkTeams}
                target="_blank"
                rel="noreferrer"
                className="mt-1 flex items-center gap-1.5 truncate text-sm font-medium underline-offset-4 hover:underline"
              >
                <IconExternalLink className="size-4 shrink-0" />
                <span className="truncate">{a.salaVirtual.linkTeams}</span>
              </a>
            </div>
          )}

          <div className="flex flex-col gap-2">
            <p className="text-sm font-medium">Participantes</p>
            <ul className="divide-y rounded-lg border">
              {a.participantes.map((p) => (
                <li key={p.idUsuario} className="flex flex-wrap items-center justify-between gap-x-3 gap-y-2 px-3.5 py-2.5">
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium">
                      {p.nomeUsuario}
                      {p.idUsuario === eu?.id && <span className="font-normal text-muted-foreground"> (você)</span>}
                    </p>
                    <p className="text-xs text-muted-foreground">{rotularEnum(p.papel)}</p>
                  </div>
                  <div className="flex flex-wrap items-center gap-2">
                    {p.statusResposta === "PENDENTE" && a.status !== "CANCELADO" && podeResponderPor(p) ? (
                      <>
                        <Button
                          variant="outline"
                          size="sm"
                          className="text-sage-700"
                          disabled={respondendo}
                          onClick={() => responder(p, true)}
                        >
                          <IconCheck />
                          Aceitar
                        </Button>
                        <Button
                          variant="destructive"
                          size="sm"
                          disabled={respondendo}
                          onClick={() => setParticipanteRecusando(p)}
                        >
                          <IconX />
                          Recusar
                        </Button>
                      </>
                    ) : (
                      <Pill
                        className={
                          p.statusResposta === "ACEITO"
                            ? "bg-sage-100 text-sage-700"
                            : p.statusResposta === "RECUSADO"
                            ? "bg-brick-100 text-brick-700"
                            : "bg-ink-100 text-ink-800"
                        }
                      >
                        {rotularEnum(p.statusResposta)}
                      </Pill>
                    )}
                  </div>
                </li>
              ))}
            </ul>
          </div>

          {participanteRecusando && (
            <div className="flex flex-col gap-3 rounded-lg border border-destructive/30 bg-destructive/5 p-4">
              <p className="text-sm font-medium text-destructive">
                {participanteRecusando.idUsuario === eu?.id
                  ? "Motivo da sua recusa"
                  : `Motivo da recusa de ${participanteRecusando.nomeUsuario}`}
              </p>
              <TextAreaField
                label="Justificativa"
                required
                value={motivoRecusa}
                onChange={(e) => setMotivoRecusa(e.target.value)}
                placeholder="Ex.: Conflito de horário"
              />
              <div className="flex flex-wrap justify-end gap-2">
                <Button variant="outline" size="sm" onClick={() => setParticipanteRecusando(null)}>
                  Cancelar
                </Button>
                <Button
                  size="sm"
                  className="bg-destructive text-white hover:bg-destructive/90"
                  disabled={!motivoRecusa.trim() || respondendo}
                  onClick={() => responder(participanteRecusando, false, motivoRecusa)}
                >
                  Confirmar recusa
                </Button>
              </div>
            </div>
          )}

          {historico.length > 0 && (
            <div className="flex flex-col gap-2">
              <p className="text-sm font-medium">Histórico</p>
              <ul className="divide-y rounded-lg border">
                {historico.map((r) => (
                  <li key={r.id} className="flex flex-col gap-0.5 px-3.5 py-2.5 text-sm">
                    <div className="flex flex-wrap items-center justify-between gap-x-3 gap-y-1">
                      <span className="font-medium">
                        {rotularEnum(r.tipoOperacao)} · {r.idUsuario === eu?.id ? "você" : r.nomeUsuario}
                      </span>
                      <span className="text-xs text-muted-foreground">{formatarDataHora(r.criadoEm)}</span>
                    </div>
                    <p className="text-muted-foreground">{r.justificativa}</p>
                  </li>
                ))}
              </ul>
            </div>
          )}

          <FormActions>
            {podeRemover && (
              <Button variant="destructive" onClick={() => setModalRemover(true)}>
                Remover definitivamente
              </Button>
            )}
            {podeCancelar && (
              <Button className="bg-destructive text-white hover:bg-destructive/90" onClick={() => setModalCancelar(true)}>
                Cancelar agendamento
              </Button>
            )}
            {podeEditar && onAlterar && (
              <Button variant="outline" onClick={() => onAlterar("editar", a)}>
                <IconPencil />
                Editar
              </Button>
            )}
            {podeRemarcar && onAlterar && (
              <Button variant="outline" onClick={() => onAlterar("remarcar", a)}>
                <IconCalendarRepeat />
                Remarcar
              </Button>
            )}
            <Button variant="outline" onClick={onFechar}>
              Fechar
            </Button>
          </FormActions>
        </div>
      </Modal>

      <Modal aberto={modalCancelar} onFechar={() => setModalCancelar(false)} titulo="Cancelar agendamento" largura="max-w-md">
        <div className="flex flex-col gap-4">
          <TextAreaField
            label="Motivo do cancelamento"
            required
            value={motivoCancelamento}
            onChange={(e) => setMotivoCancelamento(e.target.value)}
            placeholder="Ex.: Imprevisto pessoal"
          />
          <p className="text-sm text-muted-foreground">
            O cancelamento fica registrado em seu nome e exige no mínimo 1 hora de antecedência em relação ao
            horário da reunião.
          </p>
          <FormActions>
            <Button variant="outline" onClick={() => setModalCancelar(false)}>
              Voltar
            </Button>
            <Button
              className="bg-destructive text-white hover:bg-destructive/90"
              disabled={cancelando}
              onClick={confirmarCancelamento}
            >
              {cancelando ? "Cancelando…" : "Confirmar cancelamento"}
            </Button>
          </FormActions>
        </div>
      </Modal>

      <ConfirmDialog
        aberto={modalRemover}
        titulo="Remover agendamento"
        descricao="Essa ação remove definitivamente o agendamento (permitido apenas para PENDENTE ou CANCELADO)."
        confirmarRotulo="Remover"
        tom="danger"
        carregando={removendo}
        onConfirmar={confirmarRemocao}
        onCancelar={() => setModalRemover(false)}
      />
    </>
  );
}
