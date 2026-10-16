import { useCallback, useState, type FormEvent } from "react";
import { IconPlus } from "@tabler/icons-react";
import { avaliacoesApi } from "@/api/avaliacoes";
import type { ApiError } from "@/api/client";
import { usePaginatedList } from "@/hooks/usePaginatedList";
import { useUsuariosOptions } from "@/hooks/useUsuariosOptions";
import { useToast } from "@/context/ToastContext";
import { useAuth } from "@/context/AuthContext";
import PageHeader from "@/components/common/PageHeader";
import DataTable from "@/components/common/DataTable";
import Pagination from "@/components/common/Pagination";
import Modal from "@/components/common/Modal";
import EmptyState from "@/components/common/EmptyState";
import FiltrosCard from "@/components/common/FiltrosCard";
import FormActions from "@/components/common/FormActions";
import { FieldError, SelectField, TextField, TextAreaField } from "@/components/common/fields";
import { Button } from "@/components/ui/button";
import { formatarDataHora } from "@/utils/format";
import type { Avaliacao, Pagina, Params } from "@/types";

export default function AvaliacoesPage() {
  const toast = useToast();
  const { pode, perfilVinculado: eu } = useAuth();
  const { usuarios } = useUsuariosOptions("ALUNO");
  // O aluno vê e registra as próprias avaliações; docência e administração escolhem o aluno.
  const verTodas = pode("avaliacoes.verTodas");
  const [alunoEscolhido, setAlunoEscolhido] = useState("");
  const alunoId = verTodas ? alunoEscolhido : String(eu?.id ?? "");

  const buscar = useCallback(
    (params: Params) =>
      alunoId
        ? avaliacoesApi.listarPorAluno(alunoId, params)
        : Promise.resolve({ conteudo: [], totalPaginas: 0 } as unknown as Pagina<Avaliacao>),
    [alunoId]
  );
  const { pagina, linhas, carregando, setPaginaAtual, recarregar } = usePaginatedList(buscar, { ordenarPor: "criadoEm,desc" });

  const [modalAberto, setModalAberto] = useState(false);
  const [form, setForm] = useState<{ idAgendamento: string; nota: number | string; comentario: string }>({
    idAgendamento: "",
    nota: 5,
    comentario: "",
  });
  const [erro, setErro] = useState("");
  const [salvando, setSalvando] = useState(false);

  function abrirCriacao() {
    if (!alunoId) return toast.info("Selecione um aluno para avaliar uma reunião.");
    setForm({ idAgendamento: "", nota: 5, comentario: "" });
    setErro("");
    setModalAberto(true);
  }

  async function salvar(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (!form.idAgendamento) return setErro("Informe o ID do agendamento concluído.");
    setSalvando(true);
    try {
      await avaliacoesApi.criar({
        idAgendamento: Number(form.idAgendamento),
        idAluno: Number(alunoId),
        nota: Number(form.nota),
        comentario: form.comentario || null,
      });
      toast.sucesso("Avaliação registrada. Obrigado pelo retorno!");
      setModalAberto(false);
      recarregar();
    } catch (e2) {
      setErro((e2 as ApiError).message);
      toast.erro((e2 as ApiError).message);
    } finally {
      setSalvando(false);
    }
  }

  return (
    <>
      <PageHeader
        titulo={verTodas ? "Avaliações" : "Minhas avaliações"}
        subtitulo={
          verTodas
            ? "Avaliações registradas pelos alunos após reuniões concluídas."
            : "Avaliações que você registrou após reuniões concluídas."
        }
        acao={
          // RF 24: apenas o aluno avalia a reunião; a coordenação só acompanha.
          pode("avaliacoes.criar") ? (
            <Button onClick={abrirCriacao}>
              <IconPlus />
              Nova avaliação
            </Button>
          ) : null
        }
      />

      {verTodas && (
        <FiltrosCard>
          <SelectField
            label="Aluno"
            className="w-full sm:w-72"
            placeholder="Selecione um aluno…"
            options={usuarios.map((u) => ({ valor: u.id, rotulo: u.nome }))}
            value={alunoEscolhido}
            onChange={(e) => {
              setAlunoEscolhido(e.target.value);
              setPaginaAtual(0);
            }}
          />
        </FiltrosCard>
      )}

      {!alunoId ? (
        <EmptyState titulo="Selecione um aluno" descricao="Escolha um aluno acima para ver as avaliações registradas por ele." />
      ) : (
        <>
          <DataTable
            carregando={carregando}
            chaveLinha={(a) => a.id}
            linhas={linhas}
            linhaVazia={
              <EmptyState
                titulo="Nenhuma avaliação ainda"
                descricao={verTodas ? "Este aluno ainda não avaliou reuniões." : "Avalie uma reunião depois que ela for concluída."}
              />
            }
            colunas={[
              { chave: "agendamento", titulo: "Agendamento", render: (a) => `#${a.idAgendamento}` },
              { chave: "nota", titulo: "Nota", render: (a) => "⭐".repeat(a.nota) || "0" },
              { chave: "comentario", titulo: "Comentário", render: (a) => a.comentario || "—" },
              { chave: "data", titulo: "Registrada em", render: (a) => formatarDataHora(a.criadoEm) },
            ]}
          />
          <Pagination pagina={pagina} onMudarPagina={setPaginaAtual} />
        </>
      )}

      <Modal aberto={modalAberto} onFechar={() => setModalAberto(false)} titulo="Nova avaliação" largura="max-w-md">
        <form onSubmit={salvar} className="flex flex-col gap-4">
          <TextField
            label="ID do agendamento (concluído)"
            type="number"
            required
            value={form.idAgendamento}
            onChange={(e) => setForm({ ...form, idAgendamento: e.target.value })}
          />
          <SelectField
            label="Nota"
            options={[0, 1, 2, 3, 4, 5].map((n) => ({ valor: n, rotulo: `${n} estrela${n === 1 ? "" : "s"}` }))}
            value={form.nota}
            onChange={(e) => setForm({ ...form, nota: e.target.value })}
          />
          <TextAreaField
            label="Comentário (opcional)"
            value={form.comentario}
            onChange={(e) => setForm({ ...form, comentario: e.target.value })}
            placeholder="Conte como foi o atendimento"
          />
          {erro && <FieldError>{erro}</FieldError>}
          <FormActions>
            <Button type="button" variant="outline" onClick={() => setModalAberto(false)}>
              Cancelar
            </Button>
            <Button type="submit" disabled={salvando}>
              {salvando ? "Enviando…" : "Registrar avaliação"}
            </Button>
          </FormActions>
        </form>
      </Modal>
    </>
  );
}
