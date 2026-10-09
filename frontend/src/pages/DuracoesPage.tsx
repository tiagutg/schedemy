import { useEffect, useState, type FormEvent } from "react";
import { IconPencil, IconPlus, IconTrash } from "@tabler/icons-react";
import { duracoesApi } from "@/api/duracoes";
import type { ApiError } from "@/api/client";
import { useToast } from "@/context/ToastContext";
import PageHeader from "@/components/common/PageHeader";
import DataTable from "@/components/common/DataTable";
import Modal from "@/components/common/Modal";
import ConfirmDialog from "@/components/common/ConfirmDialog";
import Pill from "@/components/common/Pill";
import FormActions from "@/components/common/FormActions";
import { TextField, CheckboxField } from "@/components/common/fields";
import { Button } from "@/components/ui/button";
import type { Duracao } from "@/types";

export default function DuracoesPage() {
  const toast = useToast();
  const [lista, setLista] = useState<Duracao[]>([]);
  const [carregando, setCarregando] = useState(true);

  const [modalAberto, setModalAberto] = useState(false);
  const [editando, setEditando] = useState<Duracao | null>(null);
  const [minutos, setMinutos] = useState<number | string>(30);
  const [ativo, setAtivo] = useState(true);
  const [erro, setErro] = useState("");
  const [salvando, setSalvando] = useState(false);
  const [paraRemover, setParaRemover] = useState<Duracao | null>(null);
  const [removendo, setRemovendo] = useState(false);

  async function carregar() {
    setCarregando(true);
    try {
      setLista(await duracoesApi.listar(false));
    } catch (e) {
      toast.erro((e as ApiError).message);
    } finally {
      setCarregando(false);
    }
  }

  useEffect(() => {
    carregar();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function abrirCriacao() {
    setEditando(null);
    setMinutos(30);
    setAtivo(true);
    setErro("");
    setModalAberto(true);
  }

  function abrirEdicao(d: Duracao) {
    setEditando(d);
    setMinutos(d.minutos);
    setAtivo(d.ativo);
    setErro("");
    setModalAberto(true);
  }

  async function salvar(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (!minutos || Number(minutos) < 1) {
      setErro("Informe uma duração válida em minutos.");
      return;
    }
    setSalvando(true);
    try {
      const payload = { minutos: Number(minutos), ativo };
      if (editando) {
        await duracoesApi.atualizar(editando.id, payload);
        toast.sucesso("Duração atualizada.");
      } else {
        await duracoesApi.criar(payload);
        toast.sucesso("Duração cadastrada.");
      }
      setModalAberto(false);
      carregar();
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
      await duracoesApi.remover(paraRemover.id);
      toast.sucesso("Duração removida.");
      setParaRemover(null);
      carregar();
    } catch (e) {
      toast.erro((e as ApiError).message);
    } finally {
      setRemovendo(false);
    }
  }

  return (
    <>
      <PageHeader
        titulo="Durações de reunião"
        subtitulo="Opções de duração disponíveis ao criar um agendamento (10 a 60 minutos)."
        acao={
          <Button onClick={abrirCriacao}>
            <IconPlus />
            Nova duração
          </Button>
        }
      />

      <DataTable
        carregando={carregando}
        chaveLinha={(d) => d.id}
        linhas={lista}
        colunas={[
          { chave: "minutos", titulo: "Duração", render: (d) => <span className="font-medium">{d.minutos} minutos</span> },
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

      <Modal
        aberto={modalAberto}
        onFechar={() => setModalAberto(false)}
        titulo={editando ? "Editar duração" : "Nova duração"}
        largura="max-w-sm"
      >
        <form onSubmit={salvar} className="flex flex-col gap-4">
          <TextField
            label="Minutos"
            type="number"
            min={1}
            max={480}
            required
            value={minutos}
            onChange={(e) => setMinutos(e.target.value)}
            error={erro}
          />
          <CheckboxField label="Duração ativa" checked={ativo} onCheckedChange={setAtivo} />
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
        titulo="Remover duração"
        descricao={`Remover a duração de ${paraRemover?.minutos} minutos? Agendamentos já criados não serão afetados.`}
        confirmarRotulo="Remover"
        tom="danger"
        carregando={removendo}
        onConfirmar={confirmarRemocao}
        onCancelar={() => setParaRemover(null)}
      />
    </>
  );
}
