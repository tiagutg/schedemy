import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { IconAlertTriangle, IconArrowLeft } from "@tabler/icons-react";
import { lgpdApi } from "@/api/lgpd";
import type { ApiError } from "@/api/client";
import { useAuth } from "@/context/AuthContext";
import DocumentoLgpd from "@/components/common/DocumentoLgpd";
import Spinner from "@/components/common/Spinner";
import { formatarData, formatarDataHora } from "@/utils/format";
import type { Consentimento, Termo } from "@/types";

/** RNF 18: termo de uso e política de privacidade em vigor, abertos a qualquer pessoa (mesmo sem login). */
export default function PrivacidadePage() {
  const { estaAutenticado } = useAuth();
  const [termo, setTermo] = useState<Termo | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [meuAceite, setMeuAceite] = useState<Consentimento | null>(null);

  useEffect(() => {
    lgpdApi
      .termo()
      .then(setTermo)
      .catch((e) => setErro((e as ApiError).message));
  }, []);
  useEffect(() => {
    if (!estaAutenticado) return;
    lgpdApi
      .situacao()
      .then(setMeuAceite)
      .catch(() => setMeuAceite(null));
  }, [estaAutenticado]);

  return (
    <main className="min-h-svh bg-muted/40 px-4 py-6 sm:py-10">
      <div className="mx-auto flex w-full max-w-3xl flex-col gap-6 rounded-xl border bg-card p-5 shadow-sm sm:p-8">
        <Link to={estaAutenticado ? "/" : "/login"} className="flex items-center gap-1 text-sm text-primary hover:underline">
          <IconArrowLeft className="size-4" />
          {estaAutenticado ? "Voltar ao Schedemy" : "Voltar ao login"}
        </Link>
        <header className="flex flex-col gap-1">
          <h1 className="text-2xl font-semibold tracking-tight">Termo de uso e privacidade</h1>
          {termo && (
            <p className="text-sm text-muted-foreground">
              Versão de {formatarData(termo.publicadoEm)}
              {meuAceite?.versaoAceita === termo.versao && meuAceite.aceitoEm
                ? ` · você aceitou em ${formatarDataHora(meuAceite.aceitoEm)}`
                : ""}
            </p>
          )}
        </header>
        {!termo && !erro && (
          <div className="flex justify-center py-10">
            <Spinner />
          </div>
        )}
        {erro && <p className="text-sm text-destructive">{erro}</p>}
        {termo && (
          <>
            {termo.rascunho && (
              <p className="flex items-start gap-2 rounded-lg border border-amber-300 bg-amber-50 p-3 text-sm text-amber-900 dark:border-amber-500/40 dark:bg-amber-500/10 dark:text-amber-100">
                <IconAlertTriangle className="mt-0.5 size-4 shrink-0" />
                {termo.avisoRascunho || "Texto-base em revisão."}
              </p>
            )}
            <DocumentoLgpd documento={termo.termo} />
            <DocumentoLgpd documento={termo.politica} />
          </>
        )}
      </div>
    </main>
  );
}
