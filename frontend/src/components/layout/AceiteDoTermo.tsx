import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { IconAlertTriangle, IconShieldCheck } from "@tabler/icons-react";
import { lgpdApi } from "@/api/lgpd";
import type { ApiError } from "@/api/client";
import { useAuth } from "@/context/AuthContext";
import { useToast } from "@/context/ToastContext";
import DocumentoLgpd from "@/components/common/DocumentoLgpd";
import Spinner from "@/components/common/Spinner";
import { CheckboxField } from "@/components/common/fields";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { formatarData } from "@/utils/format";
import type { Termo } from "@/types";

/**
 * RNF 18: aparece no lugar do sistema enquanto o usuário não aceitar a versão em vigor do termo de uso e da
 * política de privacidade (no primeiro acesso e a cada versão nova). Sem aceitar, dá para sair.
 */
export default function AceiteDoTermo() {
  const { aceitarTermo, sair, perfilVinculado } = useAuth();
  const toast = useToast();
  const [termo, setTermo] = useState<Termo | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [concordo, setConcordo] = useState(false);
  const [enviando, setEnviando] = useState(false);

  function carregar() {
    setErro(null);
    lgpdApi
      .termo()
      .then(setTermo)
      .catch((e) => setErro((e as ApiError).message));
  }
  useEffect(carregar, []);

  async function aceitar() {
    if (!termo) return;
    setEnviando(true);
    try {
      await aceitarTermo(termo.versao);
      toast.sucesso("Termo aceito. Bem-vindo ao Schedemy!");
    } catch (e) {
      // O termo pode ter mudado enquanto a pessoa lia: mostra a versão nova.
      toast.erro((e as ApiError).message);
      setConcordo(false);
      carregar();
    } finally {
      setEnviando(false);
    }
  }

  return (
    <main className="flex min-h-svh items-start justify-center bg-muted/40 px-4 py-6 sm:py-10">
      <Card role="region" aria-label="Termo de uso e privacidade" className="w-full max-w-3xl">
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-xl">
            <IconShieldCheck className="size-6 text-primary" />
            Antes de continuar
          </CardTitle>
          <CardDescription>
            {perfilVinculado?.nome ? `${perfilVinculado.nome.split(" ")[0]}, para` : "Para"} usar o Schedemy, leia e
            aceite o termo de uso e a política de privacidade (LGPD).
            {termo && ` Versão de ${formatarData(termo.publicadoEm)}.`}
          </CardDescription>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          {!termo && !erro && (
            <div className="flex justify-center py-10">
              <Spinner />
            </div>
          )}
          {erro && (
            <div className="flex flex-col items-start gap-2 text-sm text-destructive">
              {erro}
              <Button variant="outline" size="sm" onClick={carregar}>
                Tentar de novo
              </Button>
            </div>
          )}
          {termo && (
            <>
              {termo.rascunho && (
                <p className="flex items-start gap-2 rounded-lg border border-amber-300 bg-amber-50 p-3 text-sm text-amber-900 dark:border-amber-500/40 dark:bg-amber-500/10 dark:text-amber-100">
                  <IconAlertTriangle className="mt-0.5 size-4 shrink-0" />
                  {termo.avisoRascunho || "Texto-base em revisão."}
                </p>
              )}
              <div
                tabIndex={0}
                aria-label="Texto do termo e da política"
                className="flex max-h-[55svh] flex-col gap-8 overflow-y-auto rounded-lg border bg-card p-4 sm:p-6"
              >
                <DocumentoLgpd documento={termo.termo} nivel={3} />
                <DocumentoLgpd documento={termo.politica} nivel={3} />
              </div>
              <CheckboxField
                label="Li e aceito o termo de uso e a política de privacidade"
                checked={concordo}
                onCheckedChange={(v) => setConcordo(v === true)}
              />
            </>
          )}
        </CardContent>
        <CardFooter className="flex flex-wrap justify-between gap-2">
          <Link to="/privacidade" target="_blank" className="text-sm text-primary underline-offset-4 hover:underline">
            Abrir em outra aba
          </Link>
          <div className="flex gap-2">
            <Button variant="outline" onClick={sair}>
              Sair
            </Button>
            <Button disabled={!termo || !concordo || enviando} onClick={aceitar}>
              Aceitar e continuar
            </Button>
          </div>
        </CardFooter>
      </Card>
    </main>
  );
}
