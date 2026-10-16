import { useState, type FormEvent } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "@/context/AuthContext";
import { useToast } from "@/context/ToastContext";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Separator } from "@/components/ui/separator";
import CampoSenha from "./CampoSenha";
import MicrosoftIcon from "./MicrosoftIcon";

export default function LoginForm({ className = "" }: { className?: string }) {
  const { entrar, carregando, erro } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const local = useLocation();

  const [identificador, setIdentificador] = useState("");
  const [senha, setSenha] = useState("");

  async function aoSubmeter(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const ok = await entrar({ username: identificador.trim(), password: senha });
    if (ok) navigate(local.state?.de || "/", { replace: true });
  }

  return (
    <form onSubmit={aoSubmeter} className={cn("flex flex-col gap-6", className)}>
      <div className="flex flex-col items-center gap-1 text-center">
        <h1 className="text-2xl font-bold text-brand-900">Entre na sua conta</h1>
        <p className="text-sm text-balance text-muted-foreground">
          Informe seu e-mail institucional para acessar o Schedemy
        </p>
      </div>

      <div className="flex flex-col gap-6">
        <div className="flex flex-col gap-2">
          <Label htmlFor="email">E-mail institucional</Label>
          <Input
            id="email"
            type="email"
            autoComplete="username"
            value={identificador}
            onChange={(e) => setIdentificador(e.target.value)}
            placeholder="nome.sobrenome@instituicao.edu.br"
            autoFocus
            required
          />
        </div>

        <div className="flex flex-col gap-2">
          <div className="flex items-center">
            <Label htmlFor="senha">Senha</Label>
            <button
              type="button"
              onClick={() => toast.info("Procure a coordenação para redefinir sua senha institucional.")}
              className="ml-auto text-sm text-brand-600 underline-offset-4 hover:underline"
            >
              Esqueceu sua senha?
            </button>
          </div>
          <CampoSenha id="senha" value={senha} onChange={(e) => setSenha(e.target.value)} />
        </div>

        {erro && (
          <p role="alert" className="rounded-lg border border-destructive/30 bg-destructive/5 px-3 py-2 text-sm text-destructive">
            {erro}
          </p>
        )}

        <Button type="submit" size="lg" className="w-full" disabled={carregando}>
          {carregando ? "Entrando…" : "Entrar"}
        </Button>

        <div className="flex items-center gap-3">
          <Separator className="flex-1" />
          <span className="text-sm text-muted-foreground">Ou continue com</span>
          <Separator className="flex-1" />
        </div>

        <Button
          type="button"
          variant="outline"
          size="lg"
          className="w-full"
          onClick={() =>
            toast.info("O acesso com a conta Microsoft institucional ainda será habilitado. Use seu e-mail e senha.")
          }
        >
          <MicrosoftIcon className="size-4" />
          Entrar com conta Microsoft
        </Button>

        <p className="text-center text-sm text-muted-foreground">
          Não tem uma conta?{" "}
          <Link to="/cadastro" className="font-medium text-brand-600 underline underline-offset-4">
            Cadastre-se
          </Link>
        </p>
      </div>
    </form>
  );
}
