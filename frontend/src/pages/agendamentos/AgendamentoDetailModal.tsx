import { cn } from "@/lib/utils";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  IconCalendarPlus,
  IconCalendarRepeat,
  IconCopy,
  IconCheck,
  IconExternalLink,
  IconMessageQuestion,
  IconPencil,
  IconShieldSearch,
  IconStar,
  IconVideo,
  IconX,
} from "@tabler/icons-react";
import Modal from "@/components/common/Modal";
import ConfirmDialog from "@/components/common/ConfirmDialog";
import FormActions from "@/components/common/FormActions";
import Pill from "@/components/common/Pill";
import { TextAreaField } from "@/components/common/fields";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { Button } from "@/components/ui/button";
import { agendamentosApi } from "@/api/agendamentos";
import { registrosMotivoApi } from "@/api/registrosMotivo";
import type { ApiError } from "@/api/client";
import { useToast } from "@/context/ToastContext";
import { useAuth } from "@/context/AuthContext";
import { rotularEnum } from "@/utils/domain";
import { baixarIcs, situacaoDaReuniao } from "@/utils/reuniao";
import { formatarData, formatarDataHora, formatarHora } from "@/utils/format";
import { CORES_STATUS_PEDIDO, desfechoDoPedido } from "@/utils/pedidos";
import type { Agendamento, Participante, RegistroMotivo, SolicitacaoEdicao } from "@/types";

interface AgendamentoDetailModalProps {
  agendamento: Agendamento | null;
  onFechar: () => void;
  onAlterado: () => void;
  /** Abre o formulário para editar, remarcar ou agendar de novo (RF 28) a partir deste agendamento. */
  onAlterar?: (modo: "editar" | "remarcar" | "replicar" | "solicitar", agendamento: Agendamento) => void;
}

export default function AgendamentoDetailModal({ agendamento, onFechar, onAlterado, onAlterar }: AgendamentoDetailModalProps) {
  const toast = useToast();
  const navigate = useNavigate();
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
  // RF 21/22: pedidos de alteração da reunião (só um pode estar pendente).
  const [pedidos, setPedidos] = useState<SolicitacaoEdicao[]>([]);
  const [recusandoPedido, setRecusandoPedido] = useState(false);
  const [motivoRecusaPedido, setMotivoRecusaPedido] = useState("");
  const [decidindo, setDecidindo] = useState(false);
  // Momento de referência para os prazos de edição/remarcação (a página recria o modal por agendamento).
  const [agora] = useState(() => Date.now());
  useEffect(() => {
    if (!agendamento) return;
    let cancelado = false;
    registrosMotivoApi
      .listarPorAgendamento(agendamento.id, { size: 50, sort: "criadoEm,desc" })
      .then((p) => !cancelado && setHistorico(p.conteudo))
      .catch(() => !cancelado && setHistorico([]));
    agendamentosApi
      .listarPedidosDeAlteracao(agendamento.id)
      .then((lista) => !cancelado && setPedidos(lista))
      .catch(() => !cancelado && setPedidos([]));
    return () => {
      cancelado = true;
    };
  }, [agendamento]);

  if (!agendamento) return null;
  const a = agendamento;
  // Além do estado do agendamento, cada ação depende da permissão do perfil.
  const podeCancelar =
    a.status !== "CANCELADO" && a.status !== "CONCLUIDO" && a.status !== "EM_ANDAMENTO" && pode("agendamentos.cancelar");
  // Editar e remarcar são do organizador; recepção e administração podem fazer por ele.
  const podeGerir = a.idOrganizador === eu?.id || pode("agendamentos.organizarPorOutros");
  const horasAteReuniao = (new Date(`${a.dataReuniao}T${a.horarioInicio}`).getTime() - agora) / 3_600_000;
  // RN 14: edição direta só de pendentes, com 24 h de antecedência. Fora disso, remarca-se
  // (nova reunião ligada à original, que é cancelada — exige 1 h de antecedência, RN 05).
  const podeEditar = podeGerir && a.status === "PENDENTE" && horasAteReuniao >= 24;
  const participoAntes = a.participantes.some((p) => p.idUsuario === eu?.id);
  const pedidoPendente = pedidos.find((p) => p.status === "PENDENTE") ?? null;
  const pedidosEncerrados = pedidos.filter((p) => p.status !== "PENDENTE");
  // RF 21: reunião confirmada, até 24 h antes (RN 14): o aluno participante (ou a recepção) pede a alteração.
  const podePedirAlteracao =
    pode("agendamentos.pedirAlteracao") &&
    a.status === "CONFIRMADO" &&
    horasAteReuniao >= 24 &&
    ((eu?.tipoUsuario === "ALUNO" && participoAntes) || pode("agendamentos.organizarPorOutros"));
  // RF 22 / RN 12: o primeiro docente da reunião que responder decide (a administração também pode).
  const podeDecidirPedido =
    !!pedidoPendente &&
    pode("agendamentos.decidirAlteracao") &&
    (participoAntes || eu?.tipoUsuario === "ADMIN") &&
    horasAteReuniao >= 24;
  const podeDesistirDoPedido =
    !!pedidoPendente &&
    pode("agendamentos.pedirAlteracao") &&
    (pedidoPendente.idSolicitante === eu?.id || pode("agendamentos.organizarPorOutros"));
  const podeRemarcar =
    podeGerir &&
    !podeEditar &&
    !podePedirAlteracao &&
    ["PENDENTE", "AGUARDANDO_RESPOSTA", "CONFIRMADO"].includes(a.status) &&
    horasAteReuniao >= 1;
  // A remoção definitiva não consta nos casos de uso: fica restrita à administração.
  const podeRemover =
    (a.status === "PENDENTE" || a.status === "CANCELADO") && pode("agendamentos.remover");
  // RF 19: aceitar/recusar convite é do convidado (aluno, professor ou coordenador),
  // e cada um responde só o próprio convite (a API não aceita responder por outra pessoa).
  // A API só aceita resposta antes do início e enquanto a reunião não foi cancelada, iniciada ou concluída.
  const podeResponderPor = (p: Participante) =>
    pode("agendamentos.responder") &&
    p.idUsuario === eu?.id &&
    ["PENDENTE", "AGUARDANDO_RESPOSTA", "CONFIRMADO"].includes(a.status) &&
    horasAteReuniao > 0;
  const situacao = situacaoDaReuniao(a, eu?.id);
  const participo = participoAntes;
  // RF 28: quem participou (ou a recepção/administração) agenda de novo com os mesmos dados, em qualquer status.
  const podeReplicar = pode("agendamentos.criar") && (participo || podeGerir);
  // RF 24: o aluno participante avalia a reunião concluída.
  const podeAvaliar = a.status === "CONCLUIDO" && participo && pode("avaliacoes.criar");
  const podeExportar = ["PENDENTE", "AGUARDANDO_RESPOSTA", "CONFIRMADO"].includes(a.status) && horasAteReuniao > 0;
  const linkSala = a.salaVirtual?.linkTeams;

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

  async function decidirPedido(aceitar: boolean) {
    if (!pedidoPendente) return;
    if (!aceitar && !motivoRecusaPedido.trim()) return toast.erro("Informe o motivo da recusa.");
    setDecidindo(true);
    try {
      await agendamentosApi.responderPedidoDeAlteracao(pedidoPendente.id, {
        aceitar,
        motivo: aceitar ? null : motivoRecusaPedido.trim(),
      });
      toast.sucesso(aceitar ? "Alteração aceita: a reunião foi atualizada." : "Pedido de alteração recusado.");
      setRecusandoPedido(false);
      setMotivoRecusaPedido("");
      onAlterado();
    } catch (e) {
      toast.erro((e as ApiError).message);
    } finally {
      setDecidindo(false);
    }
  }

  async function desistirDoPedido() {
    if (!pedidoPendente) return;
    setDecidindo(true);
    try {
      await agendamentosApi.desistirDoPedidoDeAlteracao(pedidoPendente.id);
      toast.sucesso("Pedido de alteração retirado.");
      onAlterado();
    } catch (e) {
      toast.erro((e as ApiError).message);
    } finally {
      setDecidindo(false);
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
            <Pill className={situacao.cor}>{situacao.rotulo}</Pill>
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

          {/* RF 16: o resumo aparece sempre, para ficar claro quando o organizador não preencheu. */}
          <div className="flex flex-col gap-1.5">
            <p className="text-sm font-medium">Resumo</p>
            <p className={cn("text-sm text-muted-foreground", !a.resumo && "italic")}>
              {a.resumo || "Nenhum resumo informado."}
            </p>
          </div>

          {linkSala && (
            <div className="flex flex-wrap items-center gap-3 rounded-lg border p-3.5">
              <div className="min-w-0 flex-1">
                <p className="text-xs text-muted-foreground">Sala online</p>
                <a
                  href={linkSala}
                  target="_blank"
                  rel="noreferrer"
                  className="mt-1 flex items-center gap-1.5 truncate text-sm underline-offset-4 hover:underline"
                >
                  <IconExternalLink className="size-4 shrink-0" />
                  <span className="truncate">{linkSala}</span>
                </a>
              </div>
              {situacao.rotulo !== "Cancelada" && a.status !== "CONCLUIDO" && (
                <Button asChild size="sm">
                  <a href={linkSala} target="_blank" rel="noreferrer">
                    <IconVideo />
                    Entrar na reunião
                  </a>
                </Button>
              )}
            </div>
          )}
          {!linkSala && a.formato !== "PRESENCIAL" && (
            <p className="rounded-lg border border-dashed p-3.5 text-sm text-muted-foreground">
              Reunião {a.formato === "HIBRIDO" ? "híbrida" : "on-line"} ainda sem link da sala. Combine o endereço com o
              organizador.
            </p>
          )}

          {(podeExportar || podeAvaliar) && (
            <div className="flex flex-wrap gap-2">
              {podeExportar && (
                <Button variant="outline" size="sm" onClick={() => baixarIcs(a)}>
                  <IconCalendarPlus />
                  Adicionar ao calendário
                </Button>
              )}
              {podeAvaliar && (
                <Button size="sm" onClick={() => navigate(`/avaliacoes?avaliar=${a.id}`)}>
                  <IconStar />
                  Avaliar reunião
                </Button>
              )}
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
                maxLength={500}
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

          {pedidoPendente && (
            <PedidoDeAlteracao
              pedido={pedidoPendente}
              agendamento={a}
              euId={eu?.id}
              podeDecidir={podeDecidirPedido}
              podeDesistir={podeDesistirDoPedido}
              ocupado={decidindo}
              recusando={recusandoPedido}
              motivoRecusa={motivoRecusaPedido}
              onMotivoRecusa={setMotivoRecusaPedido}
              onAceitar={() => decidirPedido(true)}
              onRecusar={() => (recusandoPedido ? decidirPedido(false) : setRecusandoPedido(true))}
              onVoltar={() => setRecusandoPedido(false)}
              onDesistir={desistirDoPedido}
            />
          )}

          {pedidosEncerrados.length > 0 && (
            <div className="flex flex-col gap-2">
              <p className="text-sm font-medium">Pedidos de alteração anteriores</p>
              <ul className="divide-y rounded-lg border">
                {pedidosEncerrados.map((p) => (
                  <li key={p.id} className="flex flex-col gap-0.5 px-3.5 py-2.5 text-sm">
                    <div className="flex flex-wrap items-center justify-between gap-x-3 gap-y-1">
                      <Pill className={CORES_STATUS_PEDIDO[p.status]}>{desfechoDoPedido(p, eu?.id)}</Pill>
                      <span className="text-xs text-muted-foreground">{formatarDataHora(p.respondidoEm ?? p.criadoEm)}</span>
                    </div>
                    <p className="text-muted-foreground">
                      {p.idSolicitante === eu?.id ? "Você pediu" : `${p.nomeSolicitante} pediu`}{" "}
                      {formatarData(p.novaData)}, {formatarHora(p.novoHorarioInicio)}–{formatarHora(p.novoHorarioFim)}: {p.motivo}
                    </p>
                    {p.motivoRecusa && <p className="text-muted-foreground">Motivo da recusa: {p.motivoRecusa}</p>}
                  </li>
                ))}
              </ul>
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
            {podePedirAlteracao && onAlterar && (
              <Button variant="outline" onClick={() => onAlterar("solicitar", a)}>
                <IconMessageQuestion />
                Pedir alteração
              </Button>
            )}
            {podeRemarcar && onAlterar && (
              <Tooltip>
                <TooltipTrigger asChild>
                  <Button variant="outline" onClick={() => onAlterar("remarcar", a)}>
                    <IconCalendarRepeat />
                    Remarcar
                  </Button>
                </TooltipTrigger>
                {/* RN 14: explica por que não aparece "Editar". */}
                <TooltipContent>
                  {a.status === "PENDENTE"
                    ? "Faltam menos de 24 h: não dá mais para editar, só remarcar."
                    : "Reunião já confirmada: não dá para editar, só remarcar."}
                </TooltipContent>
              </Tooltip>
            )}
            {podeReplicar && onAlterar && (
              <Button variant="outline" onClick={() => onAlterar("replicar", a)}>
                <IconCopy />
                Agendar de novo
              </Button>
            )}
            {/* RNF 19: a administração vê a trilha desta reunião */}
            {pode("auditoria.ver") && (
              <Button variant="ghost" onClick={() => navigate(`/auditoria?tabela=agendamento&idRegistro=${a.id}`)}>
                <IconShieldSearch />
                Ver na auditoria
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
            maxLength={500}
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

interface PedidoDeAlteracaoProps {
  pedido: SolicitacaoEdicao;
  agendamento: Agendamento;
  euId?: number;
  podeDecidir: boolean;
  podeDesistir: boolean;
  ocupado: boolean;
  recusando: boolean;
  motivoRecusa: string;
  onMotivoRecusa: (motivo: string) => void;
  onAceitar: () => void;
  onRecusar: () => void;
  onVoltar: () => void;
  onDesistir: () => void;
}

/** RF 21/22: o que o pedido muda na reunião, com as ações de quem decide e de quem pediu. */
function PedidoDeAlteracao({
  pedido: p,
  agendamento: a,
  euId,
  podeDecidir,
  podeDesistir,
  ocupado,
  recusando,
  motivoRecusa,
  onMotivoRecusa,
  onAceitar,
  onRecusar,
  onVoltar,
  onDesistir,
}: PedidoDeAlteracaoProps) {
  const mudancas: { rotulo: string; de: string; para: string }[] = [];
  const quandoAtual = `${formatarData(a.dataReuniao)}, ${formatarHora(a.horarioInicio)}–${formatarHora(a.horarioFim)}`;
  const quandoNovo = `${formatarData(p.novaData)}, ${formatarHora(p.novoHorarioInicio)}–${formatarHora(p.novoHorarioFim)}`;
  if (quandoAtual !== quandoNovo) mudancas.push({ rotulo: "Quando", de: quandoAtual, para: quandoNovo });
  if (a.formato !== p.novoFormato) mudancas.push({ rotulo: "Formato", de: rotularEnum(a.formato), para: rotularEnum(p.novoFormato) });
  if (a.topico !== p.novoTopico) mudancas.push({ rotulo: "Tópico", de: a.topico, para: p.novoTopico });
  if ((a.resumo ?? "") !== (p.novoResumo ?? "")) mudancas.push({ rotulo: "Resumo", de: a.resumo || "—", para: p.novoResumo || "—" });
  const convidadosAtuais = a.participantes.filter((x) => x.papel === "CONVIDADO");
  const entram = p.novosConvidados.filter((c) => !convidadosAtuais.some((x) => x.idUsuario === c.id)).map((c) => c.nome);
  const saem = convidadosAtuais.filter((x) => !p.novosConvidados.some((c) => c.id === x.idUsuario)).map((x) => x.nomeUsuario);

  return (
    <section
      aria-label="Pedido de alteração"
      className="flex flex-col gap-3 rounded-lg border border-brand-200 bg-brand-50 p-4 text-sm"
    >
      <div>
        <p className="font-medium text-brand-900">Pedido de alteração aguardando o docente</p>
        <p className="text-xs text-muted-foreground">
          {p.idSolicitante === euId ? "Você pediu" : `${p.nomeSolicitante} pediu`} em {formatarDataHora(p.criadoEm)}.
          Até a resposta, a reunião continua como está acima.
        </p>
      </div>
      <dl className="flex flex-col gap-1.5">
        {mudancas.map((m) => (
          <div key={m.rotulo} className="flex flex-wrap gap-x-2">
            <dt className="font-medium">{m.rotulo}:</dt>
            <dd>
              <span className="text-muted-foreground line-through">{m.de}</span> → {m.para}
            </dd>
          </div>
        ))}
        {entram.length > 0 && (
          <div className="flex flex-wrap gap-x-2">
            <dt className="font-medium">Entram:</dt>
            <dd>{entram.join(", ")}</dd>
          </div>
        )}
        {saem.length > 0 && (
          <div className="flex flex-wrap gap-x-2">
            <dt className="font-medium">Saem:</dt>
            <dd>{saem.join(", ")}</dd>
          </div>
        )}
        <div className="flex flex-wrap gap-x-2">
          <dt className="font-medium">Motivo:</dt>
          <dd className="text-muted-foreground">{p.motivo}</dd>
        </div>
      </dl>

      {podeDecidir && recusando && (
        <TextAreaField
          label="Motivo da recusa"
          required
          maxLength={500}
          placeholder="Ex.: Tenho banca nesse horário"
          value={motivoRecusa}
          onChange={(e) => onMotivoRecusa(e.target.value)}
        />
      )}
      {(podeDecidir || podeDesistir) && (
        <div className="flex flex-wrap justify-end gap-2">
          {podeDesistir && !recusando && (
            <Button variant="outline" size="sm" disabled={ocupado} onClick={onDesistir}>
              Desistir do pedido
            </Button>
          )}
          {podeDecidir && recusando && (
            <Button variant="outline" size="sm" disabled={ocupado} onClick={onVoltar}>
              Voltar
            </Button>
          )}
          {podeDecidir && (
            <Button variant="destructive" size="sm" disabled={ocupado || (recusando && !motivoRecusa.trim())} onClick={onRecusar}>
              <IconX />
              {recusando ? "Confirmar recusa" : "Recusar"}
            </Button>
          )}
          {podeDecidir && !recusando && (
            <Button size="sm" disabled={ocupado} onClick={onAceitar}>
              <IconCheck />
              Aceitar alteração
            </Button>
          )}
        </div>
      )}
    </section>
  );
}
