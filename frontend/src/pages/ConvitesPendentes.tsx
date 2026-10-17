import { useCallback, useEffect, useState } from "react";
import { IconCheck, IconMailQuestion, IconX } from "@tabler/icons-react";
import { agendamentosApi } from "@/api/agendamentos";
import type { ApiError } from "@/api/client";
import { useAuth } from "@/context/AuthContext";
import { useToast } from "@/context/ToastContext";
import { TextAreaField } from "@/components/common/fields";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { formatarData, formatarHora, hojeISO } from "@/utils/format";
import { rotularEnum } from "@/utils/domain";
import type { Agendamento } from "@/types";

/**
 * Convites do usuário logado que ainda esperam resposta (RF 19), com
 * aceitar/recusar ali mesmo. Não aparece quando não há nada pendente.
 */
export default function ConvitesPendentes({ onRespondido }: { onRespondido?: () => void }) {
  const toast = useToast();
  const { perfilVinculado: eu, pode } = useAuth();
  const [convites, setConvites] = useState<Agendamento[]>([]);
  const [recusando, setRecusando] = useState<number | null>(null);
  const [motivo, setMotivo] = useState("");
  const [enviando, setEnviando] = useState(false);
  const podeResponder = pode("agendamentos.responder") && !!eu;

  const carregar = useCallback(async () => {
    if (!eu) return [];
    const pagina = await agendamentosApi.listarPorUsuario(eu.id, {
      dataInicio: hojeISO(),
      size: 50,
      sort: "dataReuniao,asc",
    });
    return pagina.conteudo.filter(
      (a) =>
        a.status !== "CANCELADO" &&
        a.participantes.some(
          (p) => p.idUsuario === eu.id && p.papel === "CONVIDADO" && p.statusResposta === "PENDENTE"
        )
    );
  }, [eu]);

  useEffect(() => {
    if (!podeResponder) return;
    let cancelado = false;
    carregar()
      .then((lista) => !cancelado && setConvites(lista))
      .catch(() => !cancelado && setConvites([]));
    return () => {
      cancelado = true;
    };
  }, [carregar, podeResponder]);

  async function responder(a: Agendamento, aceitar: boolean) {
    if (!eu) return;
    if (!aceitar && !motivo.trim()) return toast.erro("Informe o motivo da recusa.");
    setEnviando(true);
    try {
      await agendamentosApi.responder(a.id, { idUsuario: eu.id, aceitar, motivo: aceitar ? null : motivo.trim() });
      toast.sucesso(aceitar ? "Convite aceito." : "Convite recusado.");
      setRecusando(null);
      setMotivo("");
      setConvites(await carregar());
      onRespondido?.();
    } catch (e) {
      toast.erro((e as ApiError).message);
    } finally {
      setEnviando(false);
    }
  }

  if (!podeResponder || convites.length === 0) return null;

  return (
    <Card role="region" aria-label="Aguardando sua resposta" className="border-brand-300 bg-brand-50">
      <CardHeader>
        <CardTitle className="flex items-center gap-2 text-brand-900">
          <IconMailQuestion className="size-5 text-brand-600" />
          Aguardando sua resposta
          <Badge className="bg-brand-600 text-white">{convites.length}</Badge>
        </CardTitle>
        <CardDescription>Reuniões para as quais você foi convidado e ainda não respondeu.</CardDescription>
      </CardHeader>
      <CardContent className="px-0">
        <ul className="divide-y divide-brand-200 border-t border-brand-200">
          {convites.map((a) => (
            <li key={a.id} className="flex flex-col gap-3 px-4 py-3">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div className="min-w-0">
                  <p className="truncate text-sm font-medium">{a.topico}</p>
                  <p className="text-xs text-muted-foreground">
                    {formatarData(a.dataReuniao)} às {formatarHora(a.horarioInicio)} · {rotularEnum(a.formato)} ·
                    organizado por {a.nomeOrganizador}
                  </p>
                </div>
                {recusando !== a.id && (
                  <div className="flex gap-2">
                    <Button size="sm" disabled={enviando} onClick={() => responder(a, true)}>
                      <IconCheck />
                      Aceitar
                    </Button>
                    <Button
                      size="sm"
                      variant="outline"
                      disabled={enviando}
                      onClick={() => {
                        setRecusando(a.id);
                        setMotivo("");
                      }}
                    >
                      <IconX />
                      Recusar
                    </Button>
                  </div>
                )}
              </div>
              {recusando === a.id && (
                <div className="flex flex-col gap-2 rounded-lg border border-destructive/30 bg-card p-3">
                  <TextAreaField
                    label="Motivo da recusa"
                    required
                    value={motivo}
                    onChange={(e) => setMotivo(e.target.value)}
                    placeholder="Ex.: Conflito de horário"
                  />
                  <div className="flex justify-end gap-2">
                    <Button size="sm" variant="outline" onClick={() => setRecusando(null)}>
                      Voltar
                    </Button>
                    <Button
                      size="sm"
                      className="bg-destructive text-white hover:bg-destructive/90"
                      disabled={enviando || !motivo.trim()}
                      onClick={() => responder(a, false)}
                    >
                      Confirmar recusa
                    </Button>
                  </div>
                </div>
              )}
            </li>
          ))}
        </ul>
      </CardContent>
    </Card>
  );
}
