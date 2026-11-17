import { Link } from "react-router-dom";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import MicrosoftIcon from "./MicrosoftIcon";

/**
 * Orientação para quem ainda não tem acesso. O RF 29 prevê o cadastro pela conta
 * Microsoft institucional; até o SSO existir, as contas são criadas pela administração
 * (não há autocadastro: sem confirmação por e-mail, alguém poderia se passar por outro aluno).
 */
export default function ComoObterAcesso({ className = "" }: { className?: string }) {
  return (
    <div className={cn("flex flex-col gap-6", className)}>
      <div className="flex flex-col items-center gap-1 text-center">
        <h1 className="text-2xl font-bold text-brand-900">Como obter acesso</h1>
        <p className="text-sm text-balance text-muted-foreground">
          As contas do Schedemy são criadas pela administração da instituição.
        </p>
      </div>

      <div className="flex flex-col gap-3 rounded-lg border bg-muted/40 p-4 text-sm">
        <p>
          Procure a <strong>coordenação do seu curso</strong> ou a <strong>recepção</strong> e informe seu nome
          completo, e-mail institucional e RA (para alunos). Você receberá os dados para entrar.
        </p>
        <p className="text-muted-foreground">
          Em breve será possível entrar e se cadastrar direto com a sua conta Microsoft institucional.
        </p>
      </div>

      <div className="flex flex-col gap-4">
        <Button type="button" variant="outline" size="lg" className="w-full" disabled>
          <MicrosoftIcon className="size-4" />
          Cadastro com conta Microsoft (em breve)
        </Button>

        <Button asChild size="lg" className="w-full">
          <Link to="/login">Voltar para o login</Link>
        </Button>
      </div>
    </div>
  );
}
