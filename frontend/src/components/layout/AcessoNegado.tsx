import { Link } from "react-router-dom";
import { IconLock } from "@tabler/icons-react";
import { useAuth } from "@/context/AuthContext";
import { ROTULOS_PERFIL } from "@/utils/permissoes";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";

/** Exibida quando o usuário autenticado acessa uma área fora do seu perfil. */
export default function AcessoNegado() {
  const { perfil } = useAuth();

  return (
    <div className="flex min-h-[60vh] items-center justify-center px-4">
      <Card className="w-full max-w-md text-center">
        <CardHeader className="justify-items-center">
          <div className="mb-2 flex size-10 items-center justify-center rounded-lg bg-destructive/10 text-destructive">
            <IconLock className="size-5" />
          </div>
          <CardTitle className="text-lg">Acesso restrito</CardTitle>
          <CardDescription>
            Esta área não faz parte das funções do perfil
            {perfil ? ` ${ROTULOS_PERFIL[perfil] || perfil}` : ""}. Se você precisa
            dela, fale com a coordenação do curso.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <Button asChild>
            <Link to="/">Voltar ao painel</Link>
          </Button>
        </CardContent>
      </Card>
    </div>
  );
}
