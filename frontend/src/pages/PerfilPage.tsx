import type { ReactNode } from "react";
import { IconInfoCircle } from "@tabler/icons-react";
import { useAuth } from "@/context/AuthContext";
import PageHeader from "@/components/common/PageHeader";
import Pill from "@/components/common/Pill";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { CORES_TIPO_USUARIO } from "@/utils/domain";
import { ROTULOS_PERFIL } from "@/utils/permissoes";

function Campo({ rotulo, children }: { rotulo: string; children: ReactNode }) {
  return (
    <div className="flex min-w-0 flex-col gap-0.5">
      <dt className="text-xs text-muted-foreground">{rotulo}</dt>
      <dd className="break-words text-sm font-medium">{children || "—"}</dd>
    </div>
  );
}

/** Dados do cadastro do usuário logado, só para consulta. */
export default function PerfilPage() {
  const { perfilVinculado: eu } = useAuth();
  if (!eu) return null;

  const partes = eu.nome.trim().split(/\s+/);
  const iniciais = (partes.length > 1 ? partes[0][0] + partes[partes.length - 1][0] : eu.nome.slice(0, 2)).toUpperCase();

  return (
    <>
      <PageHeader titulo="Meu perfil" subtitulo="Seus dados de cadastro no Schedemy." />

      <Card className="max-w-3xl">
        <CardHeader className="flex flex-row items-center gap-4">
          <Avatar className="size-14 rounded-xl">
            <AvatarFallback className="rounded-xl bg-brand-100 text-lg font-semibold text-brand-700">{iniciais}</AvatarFallback>
          </Avatar>
          <div className="flex min-w-0 flex-col gap-1">
            <CardTitle className="truncate text-lg">{eu.nome}</CardTitle>
            <Pill className={CORES_TIPO_USUARIO[eu.tipoUsuario]}>{ROTULOS_PERFIL[eu.tipoUsuario] ?? eu.tipoUsuario}</Pill>
          </div>
        </CardHeader>
        <CardContent>
          <dl className="grid grid-cols-1 gap-x-6 gap-y-4 sm:grid-cols-2">
            <Campo rotulo="E-mail">{eu.email}</Campo>
            <Campo rotulo="Telefone (WhatsApp)">{eu.telefoneWhatsapp}</Campo>
            {eu.tipoUsuario === "ALUNO" ? (
              <Campo rotulo="Matrícula / RA">{eu.matriculaRa}</Campo>
            ) : (
              <Campo rotulo="Nº de identificação">{eu.numIdentificacao}</Campo>
            )}
            <Campo rotulo="Departamento / curso">{eu.departamento}</Campo>
            <Campo rotulo="Situação">{eu.ativo ? "Ativo" : "Inativo"}</Campo>
          </dl>
        </CardContent>
      </Card>

      <p className="flex max-w-3xl items-start gap-2 text-sm text-muted-foreground">
        <IconInfoCircle className="mt-0.5 size-4 shrink-0" />
        Para alterar seus dados ou trocar a senha, procure a coordenação. A edição pelo próprio usuário ainda será
        liberada.
      </p>
    </>
  );
}
