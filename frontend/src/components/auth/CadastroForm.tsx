import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { usuariosApi } from "@/api/usuarios";
import type { ApiError } from "@/api/client";
import { useToast } from "@/context/ToastContext";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Separator } from "@/components/ui/separator";
import CampoSenha from "./CampoSenha";
import MicrosoftIcon from "./MicrosoftIcon";

const SENHA_MIN = 8;

interface Campos {
  nome: string;
  email: string;
  senha: string;
  confirmacao: string;
}

type Erros = Partial<Record<keyof Campos, string>>;

/** Valida os campos no cliente e devolve um mapa { campo: mensagem }. */
function validar({ nome, email, senha, confirmacao }: Campos): Erros {
  const erros: Erros = {};

  if (!nome.trim()) erros.nome = "Informe seu nome completo.";
  else if (nome.trim().length < 3) erros.nome = "O nome deve ter ao menos 3 caracteres.";
  else if (!nome.trim().includes(" ")) erros.nome = "Informe o nome e o sobrenome.";

  if (!email.trim()) erros.email = "Informe seu e-mail institucional.";
  else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) erros.email = "Informe um e-mail válido.";

  if (!senha) erros.senha = "Crie uma senha.";
  else if (senha.length < SENHA_MIN) erros.senha = `A senha deve ter no mínimo ${SENHA_MIN} caracteres.`;

  if (!confirmacao) erros.confirmacao = "Confirme a senha.";
  else if (confirmacao !== senha) erros.confirmacao = "As senhas não coincidem.";

  return erros;
}

export default function CadastroForm({ className = "" }: { className?: string }) {
  const toast = useToast();
  const navigate = useNavigate();

  const [campos, setCampos] = useState<Campos>({ nome: "", email: "", senha: "", confirmacao: "" });
  const [erros, setErros] = useState<Erros>({});
  const [erroGeral, setErroGeral] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  function alterar(campo: keyof Campos, valor: string) {
    setCampos((atual) => ({ ...atual, [campo]: valor }));
    setErros((atual) => (atual[campo] ? { ...atual, [campo]: undefined } : atual));
  }

  async function aoSubmeter(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setErroGeral(null);

    const encontrados = validar(campos);
    setErros(encontrados);
    if (Object.keys(encontrados).length > 0) return;

    setEnviando(true);
    try {
      await usuariosApi.criar({
        nome: campos.nome.trim(),
        email: campos.email.trim(),
        senha: campos.senha,
        tipoUsuario: "ALUNO",
        ativo: true,
      });
      toast.sucesso("Conta criada! Faça login para continuar.");
      navigate("/login", { replace: true });
    } catch (e) {
      const erro = e as ApiError;
      // Se o back-end voltar a exigir autenticação no cadastro, orientamos o
      // usuário a procurar a coordenação.
      if (erro.status === 401 || erro.status === 403) {
        setErroGeral(
          "O cadastro por esta tela ainda não está liberado. Procure a coordenação do curso para criar seu acesso."
        );
      } else if (erro.camposInvalidos) {
        setErros(erro.camposInvalidos as unknown as Erros);
        setErroGeral(erro.message);
      } else {
        setErroGeral(erro.message || "Não foi possível criar a conta.");
      }
    } finally {
      setEnviando(false);
    }
  }

  return (
    <form onSubmit={aoSubmeter} noValidate className={cn("flex flex-col gap-6", className)}>
      <div className="flex flex-col items-center gap-1 text-center">
        <h1 className="text-2xl font-bold text-brand-900">Crie sua conta</h1>
        <p className="hidden text-sm text-balance text-muted-foreground [@media(min-height:700px)]:block">
          Preencha os dados abaixo para criar sua conta no Schedemy
        </p>
      </div>

      <div className="flex flex-col gap-4">
        <div className="flex flex-col gap-2">
          <Label htmlFor="nome">Nome completo</Label>
          <Input
            id="nome"
            type="text"
            autoComplete="name"
            value={campos.nome}
            onChange={(e) => alterar("nome", e.target.value)}
            placeholder="João da Silva"
            aria-invalid={!!erros.nome || undefined}
            autoFocus
          />
          {erros.nome && <p className="text-sm text-destructive">{erros.nome}</p>}
        </div>

        <div className="flex flex-col gap-2">
          <Label htmlFor="email">E-mail institucional</Label>
          <Input
            id="email"
            type="email"
            autoComplete="email"
            value={campos.email}
            onChange={(e) => alterar("email", e.target.value)}
            placeholder="nome.sobrenome@instituicao.edu.br"
            aria-invalid={!!erros.email || undefined}
          />
          {erros.email ? (
            <p className="text-sm text-destructive">{erros.email}</p>
          ) : (
            <p className="hidden text-sm text-muted-foreground [@media(min-height:700px)]:block">
              Usaremos este endereço para enviar as confirmações de agendamento.
            </p>
          )}
        </div>

        <div className="flex flex-col gap-2">
          <Label htmlFor="senha">Senha</Label>
          <CampoSenha
            id="senha"
            autoComplete="new-password"
            value={campos.senha}
            onChange={(e) => alterar("senha", e.target.value)}
            invalido={!!erros.senha}
          />
          {erros.senha ? (
            <p className="text-sm text-destructive">{erros.senha}</p>
          ) : (
            <p className="hidden text-sm text-muted-foreground [@media(min-height:700px)]:block">
              Deve ter no mínimo {SENHA_MIN} caracteres.
            </p>
          )}
        </div>

        <div className="flex flex-col gap-2">
          <Label htmlFor="confirmacao">Confirmar senha</Label>
          <CampoSenha
            id="confirmacao"
            autoComplete="new-password"
            value={campos.confirmacao}
            onChange={(e) => alterar("confirmacao", e.target.value)}
            invalido={!!erros.confirmacao}
          />
          {erros.confirmacao && <p className="text-sm text-destructive">{erros.confirmacao}</p>}
        </div>

        {erroGeral && (
          <p role="alert" className="rounded-lg border border-destructive/30 bg-destructive/5 px-3 py-2 text-sm text-destructive">
            {erroGeral}
          </p>
        )}

        <Button type="submit" size="lg" className="w-full" disabled={enviando}>
          {enviando ? "Criando conta…" : "Criar conta"}
        </Button>
      </div>

      <div className="flex items-center gap-3">
        <Separator className="flex-1" />
        <span className="text-sm text-muted-foreground">Ou continue com</span>
        <Separator className="flex-1" />
      </div>

      <div className="flex flex-col gap-4">
        <Button
          type="button"
          variant="outline"
          size="lg"
          className="w-full"
          onClick={() => toast.info("O cadastro com a conta Microsoft institucional ainda será habilitado.")}
        >
          <MicrosoftIcon className="size-4" />
          Cadastrar com conta Microsoft
        </Button>

        <p className="text-center text-sm text-muted-foreground">
          Já tem uma conta?{" "}
          <Link to="/login" className="font-medium text-brand-600 underline underline-offset-4">
            Entrar
          </Link>
        </p>
      </div>
    </form>
  );
}
