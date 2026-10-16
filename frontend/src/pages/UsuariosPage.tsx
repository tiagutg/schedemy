import { useCallback, useState, type FormEvent } from "react";
import { IconPencil, IconPlus, IconTrash } from "@tabler/icons-react";
import { usuariosApi } from "@/api/usuarios";
import type { ApiError } from "@/api/client";
import { usePaginatedList } from "@/hooks/usePaginatedList";
import { useToast } from "@/context/ToastContext";
import PageHeader from "@/components/common/PageHeader";
import DataTable from "@/components/common/DataTable";
import Pagination from "@/components/common/Pagination";
import Modal from "@/components/common/Modal";
import ConfirmDialog from "@/components/common/ConfirmDialog";
import Pill from "@/components/common/Pill";
import FiltrosCard from "@/components/common/FiltrosCard";
import FormActions from "@/components/common/FormActions";
import { TextField, SelectField, FieldGrid, CheckboxField } from "@/components/common/fields";
import { Button } from "@/components/ui/button";
import { TIPOS_USUARIO, CORES_TIPO_USUARIO } from "@/utils/domain";
import type { Params, TipoUsuario, Usuario } from "@/types";

interface FormUsuario {
  nome: string;
  email: string;
  telefoneWhatsapp: string;
  tipoUsuario: TipoUsuario | string;
  matriculaRa: string;
  numIdentificacao: string;
  departamento: string;
  ativo: boolean;
}

const FORM_VAZIO: FormUsuario = {
  nome: "",
  email: "",
  telefoneWhatsapp: "",
  tipoUsuario: "ALUNO",
  matriculaRa: "",
  numIdentificacao: "",
  departamento: "",
  ativo: true,
};

export default function UsuariosPage() {
  const toast = useToast();
  const buscar = useCallback((params: Params) => usuariosApi.listar(params), []);
  const { pagina, linhas, carregando, filtros, aplicarFiltros, setPaginaAtual, recarregar } = usePaginatedList(
    buscar,
    { ordenarPor: "nome,asc" }
  );

  const [modalAberto, setModalAberto] = useState(false);
  const [editando, setEditando] = useState<Usuario | null>(null);
  const [form, setForm] = useState<FormUsuario>(FORM_VAZIO);
  const [erros, setErros] = useState<Record<string, string>>({});
  const [salvando, setSalvando] = useState(false);
  const [paraRemover, setParaRemover] = useState<Usuario | null>(null);
  const [removendo, setRemovendo] = useState(false);

  function abrirCriacao() {
    setEditando(null);
    setForm(FORM_VAZIO);
    setErros({});
    setModalAberto(true);
  }

  function abrirEdicao(usuario: Usuario) {
    setEditando(usuario);
    setForm({
      nome: usuario.nome,
      email: usuario.email,
      telefoneWhatsapp: usuario.telefoneWhatsapp || "",
      tipoUsuario: usuario.tipoUsuario,
      matriculaRa: usuario.matriculaRa || "",
      numIdentificacao: usuario.numIdentificacao || "",
      departamento: usuario.departamento || "",
      ativo: usuario.ativo,
    });
    setErros({});
    setModalAberto(true);
  }

  function validar() {
    const novosErros: Record<string, string> = {};
    if (!form.nome || form.nome.trim().length < 3) novosErros.nome = "Informe ao menos 3 caracteres.";
    if (!form.email || !/^\S+@\S+\.\S+$/.test(form.email)) novosErros.email = "E-mail inválido.";
    setErros(novosErros);
    return Object.keys(novosErros).length === 0;
  }

  async function salvar(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (!validar()) return;
    setSalvando(true);
    try {
      const payload = { ...form, telefoneWhatsapp: form.telefoneWhatsapp || null };
      if (editando) {
        await usuariosApi.atualizar(editando.id, payload);
        toast.sucesso("Usuário atualizado com sucesso.");
      } else {
        await usuariosApi.criar(payload);
        toast.sucesso("Usuário cadastrado com sucesso.");
      }
      setModalAberto(false);
      recarregar();
    } catch (e2) {
      const erro = e2 as ApiError;
      if (erro.camposInvalidos) {
        const mapa: Record<string, string> = {};
        erro.camposInvalidos.forEach((c) => (mapa[c.campo] = c.mensagem));
        setErros(mapa);
      }
      toast.erro(erro.message);
    } finally {
      setSalvando(false);
    }
  }

  async function confirmarRemocao() {
    if (!paraRemover) return;
    setRemovendo(true);
    try {
      await usuariosApi.remover(paraRemover.id);
      toast.sucesso("Usuário removido.");
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
        titulo="Usuários"
        subtitulo="Alunos, professores, coordenadores e recepcionistas cadastrados no Schedemy."
        acao={
          <Button onClick={abrirCriacao}>
            <IconPlus />
            Novo usuário
          </Button>
        }
      />

      <FiltrosCard>
        <TextField
          label="Buscar por nome"
          placeholder="Ex.: Fernanda"
          className="w-full sm:w-56"
          value={filtros.nome || ""}
          onChange={(e) => aplicarFiltros({ ...filtros, nome: e.target.value })}
        />
        <SelectField
          label="Tipo"
          className="w-full sm:w-48"
          placeholder="Todos os tipos"
          options={TIPOS_USUARIO}
          value={filtros.tipo || ""}
          onChange={(e) => aplicarFiltros({ ...filtros, tipo: e.target.value })}
        />
        <SelectField
          label="Status"
          className="w-full sm:w-40"
          placeholder="Todos"
          options={[
            { valor: "true", rotulo: "Ativos" },
            { valor: "false", rotulo: "Inativos" },
          ]}
          value={filtros.ativo ?? ""}
          onChange={(e) => aplicarFiltros({ ...filtros, ativo: e.target.value })}
        />
      </FiltrosCard>

      <DataTable
        carregando={carregando}
        chaveLinha={(u) => u.id}
        linhas={linhas}
        colunas={[
          { chave: "nome", titulo: "Nome", render: (u) => (
              <div>
                <p className="font-medium">{u.nome}</p>
                <p className="text-xs text-muted-foreground">{u.email}</p>
              </div>
            ) },
          { chave: "tipoUsuario", titulo: "Tipo", render: (u) => (
              <Pill className={CORES_TIPO_USUARIO[u.tipoUsuario]}>{u.tipoUsuario}</Pill>
            ) },
          { chave: "identificacao", titulo: "Matrícula/ID", render: (u) => u.matriculaRa || u.numIdentificacao || "—" },
          { chave: "departamento", titulo: "Departamento", render: (u) => u.departamento || "—" },
          { chave: "ativo", titulo: "Status", render: (u) => (
              <Pill className={u.ativo ? "bg-sage-100 text-sage-700" : "bg-brick-100 text-brick-700"}>
                {u.ativo ? "Ativo" : "Inativo"}
              </Pill>
            ) },
        ]}
        acoes={(u) => (
          <div className="flex justify-end gap-1">
            <Button variant="ghost" size="sm" onClick={() => abrirEdicao(u)}>
              <IconPencil />
              Editar
            </Button>
            <Button variant="destructive" size="sm" onClick={() => setParaRemover(u)}>
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
        titulo={editando ? "Editar usuário" : "Novo usuário"}
        subtitulo={editando ? `#${editando.id} · ${editando.email}` : "Preencha os dados do novo usuário."}
      >
        <form onSubmit={salvar} className="flex flex-col gap-4">
          <FieldGrid>
            <TextField
              label="Nome completo"
              required
              value={form.nome}
              onChange={(e) => setForm({ ...form, nome: e.target.value })}
              error={erros.nome}
            />
            <TextField
              label="E-mail"
              type="email"
              required
              value={form.email}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
              error={erros.email}
            />
          </FieldGrid>
          <FieldGrid>
            <SelectField
              label="Tipo de usuário"
              required
              options={TIPOS_USUARIO}
              value={form.tipoUsuario}
              onChange={(e) => setForm({ ...form, tipoUsuario: e.target.value })}
            />
            <TextField
              label="Telefone (WhatsApp)"
              placeholder="27988887777"
              value={form.telefoneWhatsapp}
              onChange={(e) => setForm({ ...form, telefoneWhatsapp: e.target.value })}
            />
          </FieldGrid>
          <FieldGrid colunas={1}>
            <TextField
              label="Matrícula / RA"
              value={form.matriculaRa}
              onChange={(e) => setForm({ ...form, matriculaRa: e.target.value })}
            />
          </FieldGrid>

          <CheckboxField
            label="Usuário ativo"
            className="pt-1"
            checked={form.ativo}
            onCheckedChange={(ativo) => setForm({ ...form, ativo })}
          />

          <FormActions>
            <Button type="button" variant="outline" onClick={() => setModalAberto(false)}>
              Cancelar
            </Button>
            <Button type="submit" disabled={salvando}>
              {salvando ? "Salvando…" : editando ? "Salvar alterações" : "Cadastrar"}
            </Button>
          </FormActions>
        </form>
      </Modal>

      <ConfirmDialog
        aberto={!!paraRemover}
        titulo="Remover usuário"
        descricao={`Tem certeza que deseja remover "${paraRemover?.nome}"? Essa ação não pode ser desfeita.`}
        confirmarRotulo="Remover"
        tom="danger"
        carregando={removendo}
        onConfirmar={confirmarRemocao}
        onCancelar={() => setParaRemover(null)}
      />
    </>
  );
}
