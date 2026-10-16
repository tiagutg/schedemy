import { useCallback, useState, type FormEvent } from "react";
import { IconAlertTriangle, IconPencil, IconPlus, IconTrash } from "@tabler/icons-react";
import { bloqueiosApi } from "@/api/bloqueios";
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
import EmptyState from "@/components/common/EmptyState";
import FiltrosCard from "@/components/common/FiltrosCard";
import FormActions from "@/components/common/FormActions";
import { TextField, SelectField, FieldGrid, CheckboxField, FieldError } from "@/components/common/fields";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { DIAS_SEMANA, CORES_STATUS_AGENDAMENTO, rotularEnum } from "@/utils/domain";
import { formatarData, hojeISO } from "@/utils/format";
import type { Agendamento, Bloqueio, Params } from "@/types";

const FORM_VAZIO = {
  idUsuario: "",
  dataInicio: hojeISO(),
  dataFim: hojeISO(),
  horarioInicio: "",
  horarioFim: "",
  eRecorrente: false,
  diaSemanaRecorrente: "2",
  motivo: "",
};

export default function BloqueiosPage() {
  const toast = useToast();
  const { pode, perfilVinculado: eu } = useAuth();
  // O docente gerencia os próprios bloqueios; a administração, os de qualquer docente.
  const gerenciaTodos = pode("agenda.gerenciarTodos");
  const meuId = gerenciaTodos ? null : (eu?.id ?? null);
  const { usuarios: professores } = useUsuariosOptions();
  const docentes = professores.filter((u) => u.tipoUsuario === "PROFESSOR" || u.tipoUsuario === "COORDENADOR");

  const buscar = useCallback(
    (params: Params) => bloqueiosApi.listar(meuId ? { ...params, usuarioId: meuId } : params),
    [meuId]
  );
  const { pagina, linhas, carregando, filtros, aplicarFiltros, setPaginaAtual, recarregar } = usePaginatedList(buscar);

  const [modalAberto, setModalAberto] = useState(false);
  const [editando, setEditando] = useState<Bloqueio | null>(null);
  const [form, setForm] = useState(FORM_VAZIO);
  const [erro, setErro] = useState("");
  const [salvando, setSalvando] = useState(false);
  const [paraRemover, setParaRemover] = useState<Bloqueio | null>(null);
  const [removendo, setRemovendo] = useState(false);

  const [modalImpactados, setModalImpactados] = useState<Bloqueio | null>(null);
  const [impactados, setImpactados] = useState<Agendamento[]>([]);
  const [carregandoImpactados, setCarregandoImpactados] = useState(false);

  function abrirCriacao() {
    setEditando(null);
    setForm({ ...FORM_VAZIO, idUsuario: meuId ? String(meuId) : "" });
    setErro("");
    setModalAberto(true);
  }

  function abrirEdicao(b: Bloqueio) {
    setEditando(b);
    setForm({
      idUsuario: String(b.idUsuario),
      dataInicio: b.dataInicio,
      dataFim: b.dataFim,
      horarioInicio: b.horarioInicio ? b.horarioInicio.slice(0, 5) : "",
      horarioFim: b.horarioFim ? b.horarioFim.slice(0, 5) : "",
      eRecorrente: b.eRecorrente,
      diaSemanaRecorrente: String(b.diaSemanaRecorrente || 2),
      motivo: b.motivo || "",
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
    setSalvando(true);
    try {
      const payload = {
        idUsuario: Number(form.idUsuario),
        dataInicio: form.dataInicio,
        dataFim: form.dataFim,
        horarioInicio: form.horarioInicio ? `${form.horarioInicio}:00` : null,
        horarioFim: form.horarioFim ? `${form.horarioFim}:00` : null,
        eRecorrente: form.eRecorrente,
        diaSemanaRecorrente: form.eRecorrente ? Number(form.diaSemanaRecorrente) : null,
        motivo: form.motivo || null,
      };
      if (editando) {
        await bloqueiosApi.atualizar(editando.id, payload);
        toast.sucesso("Bloqueio atualizado.");
      } else {
        await bloqueiosApi.criar(payload);
        toast.sucesso("Bloqueio cadastrado.");
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
      await bloqueiosApi.remover(paraRemover.id);
      toast.sucesso("Bloqueio removido.");
      setParaRemover(null);
      recarregar();
    } catch (e) {
      toast.erro((e as ApiError).message);
    } finally {
      setRemovendo(false);
    }
  }

  async function abrirImpactados(bloqueio: Bloqueio) {
    setModalImpactados(bloqueio);
    setCarregandoImpactados(true);
    try {
      setImpactados(await bloqueiosApi.listarImpactados(bloqueio.id));
    } catch (e) {
      toast.erro((e as ApiError).message);
    } finally {
      setCarregandoImpactados(false);
    }
  }

  return (
    <>
      <PageHeader
        titulo={gerenciaTodos ? "Bloqueios de período" : "Meus bloqueios de período"}
        subtitulo={
          gerenciaTodos
            ? "Períodos de indisponibilidade na agenda de professores e coordenadores."
            : "Períodos em que você não está disponível para reuniões."
        }
        acao={
          <Button onClick={abrirCriacao}>
            <IconPlus />
            Novo bloqueio
          </Button>
        }
      />

      {gerenciaTodos && (
        <FiltrosCard>
          <SelectField
            label="Professor / coordenador"
            className="w-full sm:w-64"
            placeholder="Todos"
            options={docentes.map((u) => ({ valor: u.id, rotulo: u.nome }))}
            value={filtros.usuarioId || ""}
            onChange={(e) => aplicarFiltros({ ...filtros, usuarioId: e.target.value })}
          />
        </FiltrosCard>
      )}

      <DataTable
        carregando={carregando}
        chaveLinha={(b) => b.id}
        linhas={linhas}
        colunas={[
          { chave: "usuario", titulo: "Professor / coordenador", render: (b) => b.nomeUsuario },
          { chave: "periodo", titulo: "Período", render: (b) =>
              b.dataInicio === b.dataFim ? formatarData(b.dataInicio) : `${formatarData(b.dataInicio)} – ${formatarData(b.dataFim)}` },
          { chave: "horario", titulo: "Horário", render: (b) =>
              b.horarioInicio ? `${b.horarioInicio.slice(0, 5)} – ${b.horarioFim?.slice(0, 5)}` : "Dia inteiro" },
          { chave: "recorrente", titulo: "Recorrente", render: (b) => (b.eRecorrente ? "Sim" : "Não") },
          { chave: "motivo", titulo: "Motivo", render: (b) => b.motivo || "—" },
        ]}
        acoes={(b) => (
          <div className="flex justify-end gap-1">
            <Button variant="ghost" size="sm" onClick={() => abrirImpactados(b)}>
              <IconAlertTriangle />
              Impactados
            </Button>
            <Button variant="ghost" size="sm" onClick={() => abrirEdicao(b)}>
              <IconPencil />
              Editar
            </Button>
            <Button variant="destructive" size="sm" onClick={() => setParaRemover(b)}>
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
        titulo={editando ? "Editar bloqueio" : "Novo bloqueio"}
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
          <FieldGrid>
            <div className="flex flex-col gap-2">
              <Label htmlFor="bloq-data-inicio">Data de início</Label>
              <Input
                id="bloq-data-inicio"
                type="date"
                value={form.dataInicio}
                onChange={(e) => setForm({ ...form, dataInicio: e.target.value })}
                required
              />
            </div>
            <div className="flex flex-col gap-2">
              <Label htmlFor="bloq-data-fim">Data de fim</Label>
              <Input
                id="bloq-data-fim"
                type="date"
                value={form.dataFim}
                onChange={(e) => setForm({ ...form, dataFim: e.target.value })}
                required
              />
            </div>
          </FieldGrid>
          <FieldGrid>
            <div className="flex flex-col gap-2">
              <Label htmlFor="bloq-hora-inicio">Horário de início (opcional)</Label>
              <Input
                id="bloq-hora-inicio"
                type="time"
                value={form.horarioInicio}
                onChange={(e) => setForm({ ...form, horarioInicio: e.target.value })}
              />
            </div>
            <div className="flex flex-col gap-2">
              <Label htmlFor="bloq-hora-fim">Horário de fim (opcional)</Label>
              <Input
                id="bloq-hora-fim"
                type="time"
                value={form.horarioFim}
                onChange={(e) => setForm({ ...form, horarioFim: e.target.value })}
              />
            </div>
          </FieldGrid>
          <p className="text-sm text-muted-foreground">Deixe os horários em branco para bloquear o dia inteiro.</p>

          <CheckboxField
            label="Bloqueio recorrente (repete semanalmente)"
            checked={form.eRecorrente}
            onCheckedChange={(eRecorrente) => setForm({ ...form, eRecorrente })}
          />
          {form.eRecorrente && (
            <SelectField
              label="Dia da semana recorrente"
              required
              options={DIAS_SEMANA.map((d) => ({ valor: d.valor, rotulo: d.rotulo }))}
              value={form.diaSemanaRecorrente}
              onChange={(e) => setForm({ ...form, diaSemanaRecorrente: e.target.value })}
            />
          )}

          <TextField
            label="Motivo"
            placeholder="Ex.: Congresso acadêmico"
            value={form.motivo}
            onChange={(e) => setForm({ ...form, motivo: e.target.value })}
          />

          {erro && <FieldError>{erro}</FieldError>}

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

      <Modal
        aberto={!!modalImpactados}
        onFechar={() => setModalImpactados(null)}
        titulo="Agendamentos impactados"
        subtitulo={modalImpactados ? `Bloqueio de ${modalImpactados.nomeUsuario}` : ""}
      >
        {carregandoImpactados ? (
          <p className="py-6 text-center text-sm text-muted-foreground">Carregando…</p>
        ) : impactados.length === 0 ? (
          <EmptyState titulo="Nenhum agendamento impactado" descricao="Este bloqueio não colide com reuniões já marcadas." />
        ) : (
          <ul className="divide-y rounded-lg border">
            {impactados.map((a) => (
              <li key={a.id} className="flex items-center justify-between gap-3 px-3 py-2.5">
                <div>
                  <p className="text-sm font-medium">{a.topico}</p>
                  <p className="text-xs text-muted-foreground">
                    {formatarData(a.dataReuniao)} às {a.horarioInicio?.slice(0, 5)}
                  </p>
                </div>
                <Pill className={CORES_STATUS_AGENDAMENTO[a.status]}>{rotularEnum(a.status)}</Pill>
              </li>
            ))}
          </ul>
        )}
      </Modal>

      <ConfirmDialog
        aberto={!!paraRemover}
        titulo="Remover bloqueio"
        descricao="Tem certeza que deseja remover este bloqueio de período?"
        confirmarRotulo="Remover"
        tom="danger"
        carregando={removendo}
        onConfirmar={confirmarRemocao}
        onCancelar={() => setParaRemover(null)}
      />
    </>
  );
}
