import { useCallback, useEffect, useState, type FormEvent } from "react";
import { useSearchParams } from "react-router-dom";
import { IconPencil, IconPlus, IconTrash } from "@tabler/icons-react";
import { bloqueiosApi } from "@/api/bloqueios";
import type { ApiError } from "@/api/client";
import { usePaginatedList } from "@/hooks/usePaginatedList";
import { useUsuariosOptions } from "@/hooks/useUsuariosOptions";
import { useReunioesAfetadas } from "@/hooks/useReunioesAfetadas";
import { useToast } from "@/context/ToastContext";
import { useAuth } from "@/context/AuthContext";
import PageHeader from "@/components/common/PageHeader";
import DataTable from "@/components/common/DataTable";
import Pagination from "@/components/common/Pagination";
import Modal from "@/components/common/Modal";
import ConfirmDialog from "@/components/common/ConfirmDialog";
import FiltrosCard from "@/components/common/FiltrosCard";
import FormActions from "@/components/common/FormActions";
import { TextField, SelectField, FieldGrid, CheckboxField, FieldError } from "@/components/common/fields";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { DIAS_SEMANA, rotularDiaSemana, rotularEnum } from "@/utils/domain";
import { formatarData, hojeISO } from "@/utils/format";
import type { Bloqueio, Params } from "@/types";
import ReunioesAfetadasModal from "./ReunioesAfetadasModal";

// Mesma faixa sugerida na criação de disponibilidade.
const HORARIO_PADRAO = { inicio: "08:00", fim: "12:00" };

const FORM_VAZIO = {
  idUsuario: "",
  dataInicio: hojeISO(),
  dataFim: hojeISO(),
  diaInteiro: false,
  horarioInicio: HORARIO_PADRAO.inicio,
  horarioFim: HORARIO_PADRAO.fim,
  eRecorrente: false,
  diaSemanaRecorrente: "2",
  // RF 05: só o recorrente pode ficar sem data de fim (vale até ser removido).
  semFim: false,
  motivo: "",
};

// "Toda segunda-feira", "Todo sábado".
function descreverRecorrencia(dia: number): string {
  return `${dia === 1 || dia === 7 ? "Todo" : "Toda"} ${rotularDiaSemana(dia).toLowerCase()}`;
}

function descreverPeriodo(b: Bloqueio): string {
  if (!b.dataFim) return `A partir de ${formatarData(b.dataInicio)} (sem data de fim)`;
  return b.dataInicio === b.dataFim
    ? formatarData(b.dataInicio)
    : `${formatarData(b.dataInicio)} – ${formatarData(b.dataFim)}`;
}

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

  // RF 06: criar ou editar um bloqueio pode cancelar reuniões; a API mostra quais antes de gravar.
  const afetadas = useReunioesAfetadas();

  function abrirCriacao() {
    setEditando(null);
    setForm({ ...FORM_VAZIO, idUsuario: meuId ? String(meuId) : "" });
    setErro("");
    setModalAberto(true);
  }

  // Atalho do painel: /bloqueios?novo=1 já abre o formulário.
  const [searchParams, setSearchParams] = useSearchParams();
  useEffect(() => {
    if (searchParams.get("novo") !== "1") return;
    setSearchParams({}, { replace: true });
    abrirCriacao();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchParams]);

  function abrirEdicao(b: Bloqueio) {
    setEditando(b);
    const diaInteiro = !b.horarioInicio && !b.horarioFim;
    setForm({
      idUsuario: String(b.idUsuario),
      dataInicio: b.dataInicio,
      dataFim: b.dataFim ?? "",
      diaInteiro,
      horarioInicio: b.horarioInicio ? b.horarioInicio.slice(0, 5) : diaInteiro ? HORARIO_PADRAO.inicio : "",
      horarioFim: b.horarioFim ? b.horarioFim.slice(0, 5) : diaInteiro ? HORARIO_PADRAO.fim : "",
      eRecorrente: b.eRecorrente,
      diaSemanaRecorrente: String(b.diaSemanaRecorrente || 2),
      semFim: b.eRecorrente && !b.dataFim,
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
    const semFim = form.eRecorrente && form.semFim;
    if (!semFim && !form.dataFim) {
      setErro("Informe a data de fim. Somente o bloqueio recorrente pode ficar sem data de fim.");
      return;
    }
    if (!semFim && form.dataFim < form.dataInicio) {
      setErro("A data de fim deve ser igual ou posterior à data de início.");
      return;
    }
    // Mesma regra do back: o fim precisa vir depois do início.
    if (!form.diaInteiro && form.horarioFim <= form.horarioInicio) {
      setErro("O horário de fim deve ser posterior ao de início.");
      return;
    }
    setSalvando(true);
    try {
      const payload = {
        idUsuario: Number(form.idUsuario),
        dataInicio: form.dataInicio,
        dataFim: semFim ? null : form.dataFim,
        horarioInicio: form.diaInteiro ? null : `${form.horarioInicio}:00`,
        horarioFim: form.diaInteiro ? null : `${form.horarioFim}:00`,
        eRecorrente: form.eRecorrente,
        diaSemanaRecorrente: form.eRecorrente ? Number(form.diaSemanaRecorrente) : null,
        motivo: form.motivo || null,
      };
      const id = editando?.id;
      await afetadas.executar(
        () => (id ? bloqueiosApi.previaDaEdicao(id, payload) : bloqueiosApi.previaDaCriacao(payload)),
        (cancelarAfetadas) =>
          id
            ? bloqueiosApi.atualizar(id, { ...payload, cancelarAfetadas })
            : bloqueiosApi.criar({ ...payload, cancelarAfetadas }),
        () => {
          toast.sucesso(id ? "Bloqueio atualizado." : "Bloqueio cadastrado.");
          setModalAberto(false);
          recarregar();
        }
      );
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
          // A coluna do docente só faz sentido para quem vê os bloqueios de vários.
          ...(gerenciaTodos
            ? [{ chave: "usuario", titulo: "Professor / coordenador", render: (b: Bloqueio) => b.nomeUsuario }]
            : []),
          { chave: "periodo", titulo: "Período", render: (b) => descreverPeriodo(b) },
          { chave: "horario", titulo: "Horário", render: (b) =>
              b.horarioInicio || b.horarioFim
                ? `${b.horarioInicio?.slice(0, 5) ?? "00:00"} – ${b.horarioFim?.slice(0, 5) ?? "fim do dia"}`
                : "Dia inteiro" },
          { chave: "recorrente", titulo: "Recorrente", render: (b) =>
              b.eRecorrente ? descreverRecorrencia(b.diaSemanaRecorrente ?? 0) : "Não" },
          { chave: "motivo", titulo: "Motivo", render: (b) => b.motivo || "—" },
        ]}
        acoes={(b) => (
          <div className="flex justify-end gap-1">
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
        aberto={modalAberto && !afetadas.pendente}
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
          <CheckboxField
            label="Bloqueio recorrente (repete semanalmente)"
            checked={form.eRecorrente}
            onCheckedChange={(eRecorrente) =>
              setForm({ ...form, eRecorrente, dataFim: form.dataFim || form.dataInicio })
            }
          />
          {form.eRecorrente && (
            <FieldGrid>
              <SelectField
                label="Dia da semana recorrente"
                required
                options={DIAS_SEMANA.map((d) => ({ valor: d.valor, rotulo: d.rotulo }))}
                value={form.diaSemanaRecorrente}
                onChange={(e) => setForm({ ...form, diaSemanaRecorrente: e.target.value })}
              />
              <CheckboxField
                className="sm:mt-6"
                label="Sem data de fim"
                checked={form.semFim}
                onCheckedChange={(semFim) =>
                  setForm({ ...form, semFim, dataFim: form.dataFim || form.dataInicio })
                }
              />
            </FieldGrid>
          )}

          <FieldGrid>
            <div className="flex flex-col gap-2">
              <Label htmlFor="bloq-data-inicio">{form.eRecorrente ? "Repete a partir de" : "Data de início"}</Label>
              <Input
                id="bloq-data-inicio"
                type="date"
                value={form.dataInicio}
                onChange={(e) => setForm({ ...form, dataInicio: e.target.value })}
                required
              />
            </div>
            {!(form.eRecorrente && form.semFim) && (
              <div className="flex flex-col gap-2">
                <Label htmlFor="bloq-data-fim">{form.eRecorrente ? "Repete até" : "Data de fim"}</Label>
                <Input
                  id="bloq-data-fim"
                  type="date"
                  min={form.dataInicio}
                  value={form.dataFim}
                  onChange={(e) => setForm({ ...form, dataFim: e.target.value })}
                  required
                />
              </div>
            )}
          </FieldGrid>
          {form.eRecorrente && form.semFim && (
            <p className="text-sm text-muted-foreground">O bloqueio vale toda semana até ser removido.</p>
          )}

          <CheckboxField
            label="Dia inteiro"
            checked={form.diaInteiro}
            onCheckedChange={(diaInteiro) =>
              setForm({
                ...form,
                diaInteiro,
                horarioInicio: form.horarioInicio || HORARIO_PADRAO.inicio,
                horarioFim: form.horarioFim || HORARIO_PADRAO.fim,
              })
            }
          />
          {!form.diaInteiro && (
            <FieldGrid>
              <div className="flex flex-col gap-2">
                <Label htmlFor="bloq-hora-inicio">Horário de início</Label>
                <Input
                  id="bloq-hora-inicio"
                  type="time"
                  value={form.horarioInicio}
                  onChange={(e) => setForm({ ...form, horarioInicio: e.target.value })}
                  required
                />
              </div>
              <div className="flex flex-col gap-2">
                <Label htmlFor="bloq-hora-fim">Horário de fim</Label>
                <Input
                  id="bloq-hora-fim"
                  type="time"
                  value={form.horarioFim}
                  onChange={(e) => setForm({ ...form, horarioFim: e.target.value })}
                  required
                />
              </div>
            </FieldGrid>
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

      <ReunioesAfetadasModal
        key={afetadas.pendente ? "afetadas" : "sem-afetadas"}
        reunioes={afetadas.pendente?.reunioes ?? null}
        aviso={afetadas.pendente?.aviso}
        confirmando={afetadas.confirmando}
        erro={afetadas.erro}
        onConfirmar={afetadas.confirmar}
        onVoltar={afetadas.desistir}
      />

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
