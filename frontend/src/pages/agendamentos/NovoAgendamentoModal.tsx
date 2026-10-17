import { useEffect, useState, type FormEvent } from "react";
import Modal from "@/components/common/Modal";
import FormActions from "@/components/common/FormActions";
import { SelectField, TextField, TextAreaField, FieldGrid } from "@/components/common/fields";
import Pill from "@/components/common/Pill";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { agendamentosApi } from "@/api/agendamentos";
import { duracoesApi } from "@/api/duracoes";
import type { ApiError } from "@/api/client";
import { useUsuariosOptions } from "@/hooks/useUsuariosOptions";
import { useAuth } from "@/context/AuthContext";
import { useToast } from "@/context/ToastContext";
import { FORMATOS_REUNIAO, CORES_TIPO_USUARIO, rotularEnum } from "@/utils/domain";
import { formatarData, formatarHora, hojeISO } from "@/utils/format";
import type { Agendamento, Duracao } from "@/types";
import AgendaDocentes from "./AgendaDocentes";
import AvisosAgendamento from "./AvisosAgendamento";

const FORM_VAZIO = {
  idOrganizador: "",
  idDuracao: "",
  dataReuniao: hojeISO(),
  horarioInicio: "10:00",
  formato: "PRESENCIAL",
  topico: "",
  resumo: "",
};

// RN 16: agendamentos com até 30 dias de antecedência.
const MAX_DIAS_ANTECEDENCIA = 30;

function dataMaxima(): string {
  const d = new Date();
  d.setDate(d.getDate() + MAX_DIAS_ANTECEDENCIA);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

export type ModoAgendamento = "criar" | "editar" | "remarcar";

interface NovoAgendamentoModalProps {
  aberto: boolean;
  onFechar: () => void;
  onSalvo?: (agendamento: Agendamento) => void;
  /**
   * criar: novo agendamento; editar: altera um agendamento PENDENTE (RN 14);
   * remarcar: cria um novo ligado ao original (RF 28) e cancela o original.
   */
  modo?: ModoAgendamento;
  base?: Agendamento | null;
}

// RN 03: recepção e administração não participam das reuniões, só agendam para outros.
const PODEM_PARTICIPAR = ["ALUNO", "PROFESSOR", "COORDENADOR"];
const DOCENTES = ["PROFESSOR", "COORDENADOR"];

const TITULOS: Record<ModoAgendamento, { titulo: string; botao: string; salvando: string }> = {
  criar: { titulo: "Novo agendamento", botao: "Criar agendamento", salvando: "Criando…" },
  editar: { titulo: "Editar agendamento", botao: "Salvar alterações", salvando: "Salvando…" },
  remarcar: { titulo: "Remarcar reunião", botao: "Remarcar", salvando: "Remarcando…" },
};

export default function NovoAgendamentoModal({ aberto, onFechar, onSalvo, modo = "criar", base }: NovoAgendamentoModalProps) {
  const toast = useToast();
  const { perfilVinculado: eu, pode } = useAuth();
  // Na criação, quem não agenda para outros é sempre o organizador; na edição e
  // na remarcação o organizador é o do agendamento original.
  const escolheOrganizador = modo === "criar" && pode("agendamentos.organizarPorOutros");
  const { usuarios: todos } = useUsuariosOptions();
  const usuarios = todos.filter((u) => PODEM_PARTICIPAR.includes(u.tipoUsuario));
  const [duracoes, setDuracoes] = useState<Duracao[]>([]);
  // O formulário nasce preenchido; a página recria o componente (key) a cada abertura.
  const [form, setForm] = useState(() =>
    base
      ? {
          idOrganizador: String(base.idOrganizador),
          idDuracao: "",
          dataReuniao: base.dataReuniao,
          horarioInicio: formatarHora(base.horarioInicio),
          formato: base.formato,
          topico: base.topico,
          resumo: base.resumo ?? "",
        }
      : { ...FORM_VAZIO, idOrganizador: escolheOrganizador ? "" : String(eu?.id ?? "") }
  );
  const [convidadosSelecionados, setConvidadosSelecionados] = useState<string[]>(() =>
    base ? base.participantes.filter((p) => p.papel === "CONVIDADO").map((p) => String(p.idUsuario)) : []
  );
  const [motivoRemarcacao, setMotivoRemarcacao] = useState("");
  const [erro, setErro] = useState("");
  const [salvando, setSalvando] = useState(false);

  useEffect(() => {
    if (!aberto) return;
    let cancelado = false;
    duracoesApi
      .listar(true)
      .then((lista) => !cancelado && setDuracoes(lista))
      .catch(() => !cancelado && setDuracoes([]));
    return () => {
      cancelado = true;
    };
  }, [aberto]);

  // A resposta do agendamento traz só os minutos; sem escolha explícita, casa com a lista de durações.
  const idDuracao =
    form.idDuracao || (base ? String(duracoes.find((d) => d.minutos === base.duracaoMinutos)?.id ?? "") : "");

  function alternarConvidado(id: string) {
    setConvidadosSelecionados((atual) =>
      atual.includes(id) ? atual.filter((c) => c !== id) : [...atual, id]
    );
  }

  const candidatosConvidado = form.idOrganizador
    ? usuarios.filter((u) => String(u.id) !== form.idOrganizador)
    : [];
  const docentesConvidados = usuarios
    .filter((u) => convidadosSelecionados.includes(String(u.id)) && DOCENTES.includes(u.tipoUsuario))
    .map((u) => ({ id: u.id, nome: u.nome }));
  const duracaoMinutos = duracoes.find((d) => String(d.id) === idDuracao)?.minutos ?? 0;
  // Os docentes convidados já têm a agenda no painel; os avisos cobrem os demais envolvidos.
  const organizadorUsuario = todos.find((u) => String(u.id) === form.idOrganizador) ?? null;
  const pessoasSemPainel = [
    ...(organizadorUsuario ? [organizadorUsuario] : []),
    ...usuarios.filter((u) => convidadosSelecionados.includes(String(u.id)) && !DOCENTES.includes(u.tipoUsuario)),
  ].map((u) => ({ id: u.id, nome: u.nome }));
  const textos = TITULOS[modo];

  async function salvar(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (!form.idOrganizador) return setErro("Selecione o organizador do agendamento.");
    if (!idDuracao) return setErro("Selecione a duração da reunião.");
    if (convidadosSelecionados.length === 0) return setErro("Selecione ao menos um convidado.");
    if (!form.topico.trim()) return setErro("Informe o tópico da reunião.");
    if (modo === "remarcar" && !motivoRemarcacao.trim()) return setErro("Informe o motivo da remarcação.");

    setSalvando(true);
    setErro("");
    const payload = {
      idOrganizador: Number(form.idOrganizador),
      idsConvidados: convidadosSelecionados.map(Number),
      idDuracao: Number(idDuracao),
      dataReuniao: form.dataReuniao,
      horarioInicio: `${form.horarioInicio}:00`,
      formato: form.formato,
      topico: form.topico,
      resumo: form.resumo || null,
      idAgendamentoOrigem: modo === "remarcar" ? base?.id : undefined,
    };
    try {
      let salvo: Agendamento;
      if (modo === "editar" && base) {
        salvo = await agendamentosApi.atualizar(base.id, payload);
      } else {
        salvo = await agendamentosApi.criar(payload);
      }
      // Na remarcação, a reunião original é cancelada com o motivo informado.
      if (modo === "remarcar" && base && eu) {
        try {
          await agendamentosApi.cancelar(base.id, {
            idUsuario: eu.id,
            motivo: `Remarcada para ${formatarData(form.dataReuniao)} às ${form.horarioInicio}: ${motivoRemarcacao.trim()}`,
            confirmar: true,
          });
        } catch (e2) {
          toast.erro(
            `A nova reunião foi criada, mas não foi possível cancelar a original: ${(e2 as ApiError).message}`
          );
        }
      }
      onSalvo?.(salvo);
    } catch (e2) {
      setErro((e2 as ApiError).message);
    } finally {
      setSalvando(false);
    }
  }

  const subtitulo =
    modo === "criar"
      ? "Ao salvar, o sistema confere conflitos de horário, prazos e a disponibilidade dos professores."
      : modo === "editar"
        ? "Só agendamentos pendentes podem ser editados, com pelo menos 24 horas de antecedência. Os convidados terão que responder de novo."
        : `A reunião de ${formatarData(base?.dataReuniao)} às ${formatarHora(base?.horarioInicio)} será cancelada e uma nova será criada.`;

  return (
    <Modal aberto={aberto} onFechar={onFechar} titulo={textos.titulo} subtitulo={subtitulo} largura="max-w-2xl">
      <form onSubmit={salvar} className="flex flex-col gap-4">
        <FieldGrid>
          {escolheOrganizador ? (
            <SelectField
              label="Organizador"
              required
              placeholder="Selecione…"
              options={usuarios.map((u) => ({ valor: u.id, rotulo: `${u.nome} (${rotularEnum(u.tipoUsuario)})` }))}
              value={form.idOrganizador}
              onChange={(e) => {
                setForm({ ...form, idOrganizador: e.target.value });
                setConvidadosSelecionados((atual) => atual.filter((c) => c !== e.target.value));
              }}
            />
          ) : (
            <TextField label="Organizador" value={base?.nomeOrganizador ?? eu?.nome ?? ""} disabled />
          )}
          <SelectField
            label="Duração"
            required
            placeholder="Selecione…"
            options={duracoes.map((d) => ({ valor: d.id, rotulo: `${d.minutos} minutos` }))}
            value={idDuracao}
            onChange={(e) => setForm({ ...form, idDuracao: e.target.value })}
          />
        </FieldGrid>

        <div className="flex flex-col gap-2">
          <Label>
            Convidados <span className="text-destructive">*</span>
          </Label>
          <div className="max-h-44 overflow-y-auto rounded-lg border p-1">
            {candidatosConvidado.length === 0 && (
              <p className="p-2 text-xs text-muted-foreground">Selecione o organizador primeiro.</p>
            )}
            {candidatosConvidado.map((u) => (
              <label key={u.id} className="flex cursor-pointer items-center gap-2 rounded-md px-2 py-1.5 hover:bg-muted">
                <Checkbox
                  checked={convidadosSelecionados.includes(String(u.id))}
                  onCheckedChange={() => alternarConvidado(String(u.id))}
                />
                <span className="flex-1 text-sm">{u.nome}</span>
                <Pill className={CORES_TIPO_USUARIO[u.tipoUsuario]}>{rotularEnum(u.tipoUsuario)}</Pill>
              </label>
            ))}
          </div>
          <p className="text-xs text-muted-foreground">
            É necessário ao menos 1 aluno e 1 professor/coordenador entre organizador e convidados.
          </p>
        </div>

        <FieldGrid colunas={3}>
          <div className="flex flex-col gap-2">
            <Label htmlFor="novo-data">Data</Label>
            <Input
              id="novo-data"
              type="date"
              min={hojeISO()}
              max={dataMaxima()}
              value={form.dataReuniao}
              onChange={(e) => setForm({ ...form, dataReuniao: e.target.value })}
              required
            />
          </div>
          <div className="flex flex-col gap-2">
            <Label htmlFor="novo-horario">Horário</Label>
            <Input
              id="novo-horario"
              type="time"
              step={300}
              value={form.horarioInicio}
              onChange={(e) => setForm({ ...form, horarioInicio: e.target.value })}
              required
            />
          </div>
          <SelectField
            label="Formato"
            required
            options={FORMATOS_REUNIAO.map((f) => ({ valor: f, rotulo: rotularEnum(f) }))}
            value={form.formato}
            onChange={(e) => setForm({ ...form, formato: e.target.value })}
          />
        </FieldGrid>

        {docentesConvidados.length > 0 ? (
          <AgendaDocentes
            docentes={docentesConvidados}
            dataISO={form.dataReuniao}
            duracaoMinutos={duracaoMinutos}
            horario={form.horarioInicio}
            ignorarAgendamentoId={modo === "editar" ? base?.id : null}
            onEscolherHorario={(horarioInicio) => setForm((atual) => ({ ...atual, horarioInicio }))}
          />
        ) : (
          <p className="text-xs text-muted-foreground">
            Convide um professor ou coordenador para ver os horários em que ele atende.
          </p>
        )}

        <AvisosAgendamento
          pessoas={pessoasSemPainel}
          organizador={
            organizadorUsuario && {
              id: organizadorUsuario.id,
              ehAluno: organizadorUsuario.tipoUsuario === "ALUNO",
              ehEu: organizadorUsuario.id === eu?.id,
            }
          }
          dataISO={form.dataReuniao}
          horario={form.horarioInicio}
          duracaoMinutos={duracaoMinutos}
          ignorarAgendamentoId={modo === "editar" ? base?.id : null}
          verificarLimite={modo !== "editar"}
        />

        <TextField
          label="Tópico"
          required
          placeholder="Ex.: Dúvidas sobre TCC"
          value={form.topico}
          onChange={(e) => setForm({ ...form, topico: e.target.value })}
        />
        <TextAreaField
          label="Resumo (opcional)"
          placeholder="Detalhes adicionais sobre a reunião"
          value={form.resumo}
          onChange={(e) => setForm({ ...form, resumo: e.target.value })}
        />
        {modo === "remarcar" && (
          <TextAreaField
            label="Motivo da remarcação"
            required
            placeholder="Ex.: Conflito com prova"
            value={motivoRemarcacao}
            onChange={(e) => setMotivoRemarcacao(e.target.value)}
          />
        )}

        {erro && (
          <p className="rounded-lg border border-destructive/30 bg-destructive/5 px-3 py-2 text-sm text-destructive">{erro}</p>
        )}

        <FormActions>
          <Button type="button" variant="outline" onClick={onFechar}>
            Cancelar
          </Button>
          <Button type="submit" disabled={salvando}>
            {salvando ? textos.salvando : textos.botao}
          </Button>
        </FormActions>
      </form>
    </Modal>
  );
}
