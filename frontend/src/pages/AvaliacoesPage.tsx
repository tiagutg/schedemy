import { useCallback, useEffect, useState, type FormEvent } from "react";
import { IconPlus } from "@tabler/icons-react";
import { agendamentosApi } from "@/api/agendamentos";
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
import { FieldError, SelectField, TextAreaField } from "@/components/common/fields";
import { Button } from "@/components/ui/button";
import { formatarData, formatarDataHora, formatarHora } from "@/utils/format";
import type { Agendamento, Avaliacao, Pagina, Params } from "@/types";

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

  // Detalhes das reuniões avaliadas, para mostrar tópico e data em vez do número.
  // Busca cada reunião da página (quem vê a avaliação participou dela); null = sem acesso.
  const [reunioes, setReunioes] = useState<Record<number, Agendamento | null>>({});
  const faltando = [...new Set(linhas.map((a) => a.idAgendamento))].filter((id) => !(id in reunioes)).join(",");
  useEffect(() => {
    if (!faltando) return;
    let cancelado = false;
    Promise.all(
      faltando.split(",").map((id) =>
        agendamentosApi
          .buscarPorId(id)
          .then((ag) => [Number(id), ag] as const)
          .catch(() => [Number(id), null] as const)
      )
    ).then((encontradas) => !cancelado && setReunioes((atual) => ({ ...atual, ...Object.fromEntries(encontradas) })));
    return () => {
      cancelado = true;
    };
  }, [faltando]);

  const [modalAberto, setModalAberto] = useState(false);
  const [form, setForm] = useState<{ idAgendamento: string; nota: number | string; comentario: string }>({
    idAgendamento: "",
    nota: 5,
    comentario: "",
  });
  const [erro, setErro] = useState("");
  const [salvando, setSalvando] = useState(false);
  // Reuniões concluídas do aluno que ele ainda não avaliou (null = carregando).
  const [paraAvaliar, setParaAvaliar] = useState<Agendamento[] | null>(null);

  async function abrirCriacao() {
    if (!alunoId) return toast.info("Selecione um aluno para avaliar uma reunião.");
    setForm({ idAgendamento: "", nota: 5, comentario: "" });
    setErro("");
    setParaAvaliar(null);
    setModalAberto(true);
    try {
      const [concluidas, avaliadas] = await Promise.all([
        agendamentosApi.listarPorUsuario(alunoId, { status: "CONCLUIDO", size: 100, sort: "dataReuniao,desc" }),
        avaliacoesApi.listarPorAluno(alunoId, { size: 200 }),
      ]);
      const jaAvaliadas = new Set(avaliadas.conteudo.map((av) => av.idAgendamento));
      const pendentes = concluidas.conteudo.filter((ag) => !jaAvaliadas.has(ag.id));
      setParaAvaliar(pendentes);
      if (pendentes.length === 1) setForm((atual) => ({ ...atual, idAgendamento: String(pendentes[0].id) }));
    } catch (e) {
      setParaAvaliar([]);
      setErro((e as ApiError).message);
    }
  }

  async function salvar(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (!form.idAgendamento) return setErro("Escolha a reunião que você quer avaliar.");
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
              { chave: "agendamento", titulo: "Reunião", render: (a) => {
                  const r = reunioes[a.idAgendamento];
                  return r ? (
                    <div>
                      <p className="font-medium">{r.topico}</p>
                      <p className="text-xs text-muted-foreground">
                        {formatarData(r.dataReuniao)} às {formatarHora(r.horarioInicio)} · {r.nomeOrganizador}
                      </p>
                    </div>
                  ) : (
                    `#${a.idAgendamento}`
                  );
                } },
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
          {paraAvaliar === null ? (
            <p className="py-2 text-sm text-muted-foreground">Carregando reuniões concluídas…</p>
          ) : paraAvaliar.length === 0 ? (
            <EmptyState
              titulo="Nada para avaliar"
              descricao="Não há reuniões concluídas aguardando avaliação. Elas aparecem aqui depois que a reunião acontece."
            />
          ) : (
            <SelectField
              label="Reunião"
              required
              placeholder="Selecione a reunião…"
              options={paraAvaliar.map((ag) => ({
                valor: ag.id,
                rotulo: `${formatarData(ag.dataReuniao)} ${formatarHora(ag.horarioInicio)} · ${ag.topico}`,
              }))}
              value={form.idAgendamento}
              onChange={(e) => setForm({ ...form, idAgendamento: e.target.value })}
            />
          )}
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
            maxLength={200}
          />
          {erro && <FieldError>{erro}</FieldError>}
          <FormActions>
            <Button type="button" variant="outline" onClick={() => setModalAberto(false)}>
              Cancelar
            </Button>
            <Button type="submit" disabled={salvando || !paraAvaliar?.length}>
              {salvando ? "Enviando…" : "Registrar avaliação"}
            </Button>
          </FormActions>
        </form>
      </Modal>
    </>
  );
}
