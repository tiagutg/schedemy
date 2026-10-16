import { useCallback, useState, type FormEvent } from "react";
import { IconPencil, IconPlus, IconTrash } from "@tabler/icons-react";
import { disponibilidadesApi } from "@/api/disponibilidades";
import type { ApiError } from "@/api/client";
import { usePaginatedList } from "@/hooks/usePaginatedList";
import { useUsuariosOptions } from "@/hooks/useUsuariosOptions";
import { useToast } from "@/context/ToastContext";
import { useAuth } from "@/context/AuthContext";
import PageHeader from "@/components/common/PageHeader";
import DataTable from "@/components/common/DataTable";
import Pagination from "@/components/common/Pagination";
import Modal from "@/components/common/Modal";
import ConfirmDialog from "@/components/common/ConfirmDialog";
import Pill from "@/components/common/Pill";
import FiltrosCard from "@/components/common/FiltrosCard";
import FormActions from "@/components/common/FormActions";
import { SelectField, TextField, FieldGrid, CheckboxField, FieldError } from "@/components/common/fields";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { DIAS_SEMANA, rotularDiaSemana, rotularEnum } from "@/utils/domain";
import type { Disponibilidade, Params } from "@/types";

const FORM_VAZIO = { idUsuario: "", diaSemana: "2", horarioInicio: "08:00", horarioFim: "12:00", ativo: true };

export default function DisponibilidadesPage() {
  const toast = useToast();
  const { pode, perfilVinculado: eu } = useAuth();
  // O docente gerencia a própria grade; a administração, a de qualquer docente.
  const gerenciaTodos = pode("agenda.gerenciarTodos");
  const meuId = gerenciaTodos ? null : (eu?.id ?? null);
  const { usuarios: professores } = useUsuariosOptions();
  const buscar = useCallback(
    (params: Params) => disponibilidadesApi.listar(meuId ? { ...params, usuarioId: meuId } : params),
    [meuId]
  );
  const { pagina, linhas, carregando, filtros, aplicarFiltros, setPaginaAtual, recarregar } = usePaginatedList(buscar);

  const docentes = professores.filter((u) => u.tipoUsuario === "PROFESSOR" || u.tipoUsuario === "COORDENADOR");

  const [modalAberto, setModalAberto] = useState(false);
  const [editando, setEditando] = useState<Disponibilidade | null>(null);
  const [form, setForm] = useState(FORM_VAZIO);
  const [erro, setErro] = useState("");
  const [salvando, setSalvando] = useState(false);
  const [paraRemover, setParaRemover] = useState<Disponibilidade | null>(null);
  const [removendo, setRemovendo] = useState(false);

  function abrirCriacao() {
    setEditando(null);
    setForm({ ...FORM_VAZIO, idUsuario: meuId ? String(meuId) : "" });
    setErro("");
    setModalAberto(true);
  }

  function abrirEdicao(d: Disponibilidade) {
    setEditando(d);
    setForm({
      idUsuario: String(d.idUsuario),
      diaSemana: String(d.diaSemana),
      horarioInicio: d.horarioInicio.slice(0, 5),
      horarioFim: d.horarioFim.slice(0, 5),
      ativo: d.ativo,
    });
    setErro("");
    setModalAberto(true);
  }

  async function salvar(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (!form.idUsuario) {
      setErro("Selecione um professor ou coordenador.");
      return;
    }
    if (form.horarioFim <= form.horarioInicio) {
      setErro("O horário de fim deve ser posterior ao horário de início.");
      return;
    }
    setSalvando(true);
    try {
      const payload = {
        idUsuario: Number(form.idUsuario),
        diaSemana: Number(form.diaSemana),
        horarioInicio: `${form.horarioInicio}:00`,
        horarioFim: `${form.horarioFim}:00`,
        ativo: form.ativo,
      };
      if (editando) {
        await disponibilidadesApi.atualizar(editando.id, payload);
        toast.sucesso("Disponibilidade atualizada.");
      } else {
        await disponibilidadesApi.criar(payload);
        toast.sucesso("Disponibilidade cadastrada.");
      }
      setModalAberto(false);
      recarregar();
    } catch (e2) {
      setErro((e2 as ApiError).message);
      toast.erro((e2 as ApiError).message);
    } finally {
      setSalvando(false);
    }
  }

  async function confirmarRemocao() {
    if (!paraRemover) return;
    setRemovendo(true);
    try {
      await disponibilidadesApi.remover(paraRemover.id);
      toast.sucesso("Disponibilidade removida.");
      setParaRemover(null);
      recarregar();
    } catch (e) {
      toast.erro((e as ApiError).message);
    } finally {
      setRemovendo(false);
    }
  }

  return (
    <>
      <PageHeader
        titulo={gerenciaTodos ? "Disponibilidades" : "Minhas disponibilidades"}
        subtitulo={
          gerenciaTodos
            ? "Grade semanal de horários de atendimento de professores e coordenadores."
            : "Sua grade semanal de horários de atendimento."
        }
        acao={
          <Button onClick={abrirCriacao}>
            <IconPlus />
            Nova disponibilidade
          </Button>
        }
      />

      <FiltrosCard>
        {gerenciaTodos && (
          <SelectField
            label="Professor / coordenador"
            className="w-full sm:w-64"
            placeholder="Todos"
            options={docentes.map((u) => ({ valor: u.id, rotulo: u.nome }))}
            value={filtros.usuarioId || ""}
            onChange={(e) => aplicarFiltros({ ...filtros, usuarioId: e.target.value })}
          />
        )}
        <SelectField
          label="Dia da semana"
          className="w-full sm:w-48"
          placeholder="Todos"
          options={DIAS_SEMANA.map((d) => ({ valor: d.valor, rotulo: d.rotulo }))}
          value={filtros.diaSemana || ""}
          onChange={(e) => aplicarFiltros({ ...filtros, diaSemana: e.target.value })}
        />
      </FiltrosCard>

      <DataTable
        carregando={carregando}
        chaveLinha={(d) => d.id}
        linhas={linhas}
        colunas={[
          { chave: "usuario", titulo: "Professor / coordenador", render: (d) => d.nomeUsuario },
          { chave: "dia", titulo: "Dia da semana", render: (d) => rotularDiaSemana(d.diaSemana) },
          { chave: "horario", titulo: "Horário", render: (d) => `${d.horarioInicio.slice(0, 5)} – ${d.horarioFim.slice(0, 5)}` },
          { chave: "ativo", titulo: "Status", render: (d) => (
              <Pill className={d.ativo ? "bg-sage-100 text-sage-700" : "bg-brick-100 text-brick-700"}>
                {d.ativo ? "Ativa" : "Inativa"}
              </Pill>
            ) },
        ]}
        acoes={(d) => (
          <div className="flex justify-end gap-1">
            <Button variant="ghost" size="sm" onClick={() => abrirEdicao(d)}>
              <IconPencil />
              Editar
            </Button>
            <Button variant="destructive" size="sm" onClick={() => setParaRemover(d)}>
              <IconTrash />
              Remover
            </Button>
          </div>
        )}
      />
      <Pagination pagina={pagina} onMudarPagina={setPaginaAtual} />

      <Modal
        aberto={modalAberto}
        onFechar={() => setModalAberto(false)}
        titulo={editando ? "Editar disponibilidade" : "Nova disponibilidade"}
      >
        <form onSubmit={salvar} className="flex flex-col gap-4">
          {gerenciaTodos ? (
            <SelectField
              label="Professor / coordenador"
              required
              placeholder="Selecione…"
              options={docentes.map((u) => ({ valor: u.id, rotulo: `${u.nome} (${rotularEnum(u.tipoUsuario)})` }))}
              value={form.idUsuario}
              onChange={(e) => setForm({ ...form, idUsuario: e.target.value })}
            />
          ) : (
            <TextField label="Professor / coordenador" value={eu?.nome ?? ""} disabled />
          )}
          <SelectField
            label="Dia da semana"
            required
            options={DIAS_SEMANA.map((d) => ({ valor: d.valor, rotulo: d.rotulo }))}
            value={form.diaSemana}
            onChange={(e) => setForm({ ...form, diaSemana: e.target.value })}
          />
          <FieldGrid>
            <div className="flex flex-col gap-2">
              <Label htmlFor="disp-inicio">Horário de início</Label>
              <Input
                id="disp-inicio"
                type="time"
                value={form.horarioInicio}
                onChange={(e) => setForm({ ...form, horarioInicio: e.target.value })}
                required
              />
            </div>
            <div className="flex flex-col gap-2">
              <Label htmlFor="disp-fim">Horário de fim</Label>
              <Input
                id="disp-fim"
                type="time"
                value={form.horarioFim}
                onChange={(e) => setForm({ ...form, horarioFim: e.target.value })}
                required
              />
            </div>
          </FieldGrid>
          {erro && <FieldError>{erro}</FieldError>}
          <CheckboxField
            label="Disponibilidade ativa"
            checked={form.ativo}
            onCheckedChange={(ativo) => setForm({ ...form, ativo })}
          />
          <FormActions>
            <Button type="button" variant="outline" onClick={() => setModalAberto(false)}>
              Cancelar
            </Button>
            <Button type="submit" disabled={salvando}>
              {salvando ? "Salvando…" : "Salvar"}
            </Button>
          </FormActions>
        </form>
      </Modal>

      <ConfirmDialog
        aberto={!!paraRemover}
        titulo="Remover disponibilidade"
        descricao="Tem certeza que deseja remover esta disponibilidade?"
        confirmarRotulo="Remover"
        tom="danger"
        carregando={removendo}
        onConfirmar={confirmarRemocao}
        onCancelar={() => setParaRemover(null)}
      />
    </>
  );
}
