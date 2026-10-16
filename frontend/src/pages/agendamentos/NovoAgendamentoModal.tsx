import { useEffect, useState, type FormEvent } from "react";
import Modal from "@/components/common/Modal";
import FormActions from "@/components/common/FormActions";
import { SelectField, TextField, TextAreaField, FieldGrid } from "@/components/common/fields";
import Pill from "@/components/common/Pill";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { duracoesApi } from "@/api/duracoes";
import type { ApiError } from "@/api/client";
import { useUsuariosOptions } from "@/hooks/useUsuariosOptions";
import { useAuth } from "@/context/AuthContext";
import { FORMATOS_REUNIAO, CORES_TIPO_USUARIO, rotularEnum } from "@/utils/domain";
import { hojeISO } from "@/utils/format";
import type { Duracao } from "@/types";

const FORM_VAZIO = {
  idOrganizador: "",
  idDuracao: "",
  dataReuniao: hojeISO(),
  horarioInicio: "10:00",
  formato: "PRESENCIAL",
  topico: "",
  resumo: "",
};

interface NovoAgendamentoModalProps {
  aberto: boolean;
  onFechar: () => void;
  onCriado?: () => void;
  criarAgendamento: (payload: unknown) => Promise<unknown>;
}

// RN 03: recepção e administração não participam das reuniões, só agendam para outros.
const PODEM_PARTICIPAR = ["ALUNO", "PROFESSOR", "COORDENADOR"];

export default function NovoAgendamentoModal({ aberto, onFechar, onCriado, criarAgendamento }: NovoAgendamentoModalProps) {
  const { perfilVinculado: eu, pode } = useAuth();
  // Quem não agenda para outros é sempre o organizador do próprio agendamento.
  const escolheOrganizador = pode("agendamentos.organizarPorOutros");
  const { usuarios: todos } = useUsuariosOptions();
  const usuarios = todos.filter((u) => PODEM_PARTICIPAR.includes(u.tipoUsuario));
  const [duracoes, setDuracoes] = useState<Duracao[]>([]);
  const [form, setForm] = useState(FORM_VAZIO);
  const [convidadosSelecionados, setConvidadosSelecionados] = useState<string[]>([]);
  const [erro, setErro] = useState("");
  const [salvando, setSalvando] = useState(false);

  useEffect(() => {
    if (aberto) {
      duracoesApi.listar(true).then(setDuracoes).catch(() => setDuracoes([]));
      setForm({ ...FORM_VAZIO, idOrganizador: escolheOrganizador ? "" : String(eu?.id ?? "") });
      setConvidadosSelecionados([]);
      setErro("");
    }
  }, [aberto, escolheOrganizador, eu?.id]);

  function alternarConvidado(id: string) {
    setConvidadosSelecionados((atual) =>
      atual.includes(id) ? atual.filter((c) => c !== id) : [...atual, id]
    );
  }

  const candidatosConvidado = form.idOrganizador
    ? usuarios.filter((u) => String(u.id) !== form.idOrganizador)
    : [];

  async function salvar(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (!form.idOrganizador) return setErro("Selecione o organizador do agendamento.");
    if (!form.idDuracao) return setErro("Selecione a duração da reunião.");
    if (convidadosSelecionados.length === 0) return setErro("Selecione ao menos um convidado.");
    if (!form.topico.trim()) return setErro("Informe o tópico da reunião.");

    setSalvando(true);
    setErro("");
    try {
      const payload = {
        idOrganizador: Number(form.idOrganizador),
        idsConvidados: convidadosSelecionados.map(Number),
        idDuracao: Number(form.idDuracao),
        dataReuniao: form.dataReuniao,
        horarioInicio: `${form.horarioInicio}:00`,
        formato: form.formato,
        topico: form.topico,
        resumo: form.resumo || null,
      };
      await criarAgendamento(payload);
      onCriado?.();
    } catch (e2) {
      setErro((e2 as ApiError).message);
    } finally {
      setSalvando(false);
    }
  }

  return (
    <Modal aberto={aberto} onFechar={onFechar} titulo="Novo agendamento" subtitulo="RN 01 a RN 18 são validadas pelo back-end." largura="max-w-2xl">
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
            <TextField label="Organizador" value={eu?.nome ?? ""} disabled />
          )}
          <SelectField
            label="Duração"
            required
            placeholder="Selecione…"
            options={duracoes.map((d) => ({ valor: d.id, rotulo: `${d.minutos} minutos` }))}
            value={form.idDuracao}
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
              value={form.horarioInicio}
              onChange={(e) => setForm({ ...form, horarioInicio: e.target.value })}
              required
            />
          </div>
          <SelectField
            label="Formato"
            required
            options={FORMATOS_REUNIAO}
            value={form.formato}
            onChange={(e) => setForm({ ...form, formato: e.target.value })}
          />
        </FieldGrid>

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

        {erro && (
          <p className="rounded-lg border border-destructive/30 bg-destructive/5 px-3 py-2 text-sm text-destructive">{erro}</p>
        )}

        <FormActions>
          <Button type="button" variant="outline" onClick={onFechar}>
            Cancelar
          </Button>
          <Button type="submit" disabled={salvando}>
            {salvando ? "Criando…" : "Criar agendamento"}
          </Button>
        </FormActions>
      </form>
    </Modal>
  );
}
