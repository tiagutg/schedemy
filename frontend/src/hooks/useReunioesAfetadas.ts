import { useState } from "react";
import { ApiError } from "@/api/client";
import type { CancelamentoDeAfetadas, ReuniaoAfetada } from "@/types";

/** Grava a mudança; recebe a confirmação (ids vistos + motivo) quando há reuniões afetadas. */
type Salvar = (cancelarAfetadas: CancelamentoDeAfetadas | null) => Promise<unknown>;

interface Pendente {
  reunioes: ReuniaoAfetada[];
  salvar: Salvar;
  aoConcluir: () => void;
  /** Explica por que a lista apareceu de novo (mudou desde a prévia). */
  aviso: string;
}

/** 422 da API com a lista de reuniões afetadas (mudou desde a prévia, ou salvou sem prévia). */
export function reunioesDoErro(e: unknown): ReuniaoAfetada[] | null {
  if (!(e instanceof ApiError) || e.status !== 422) return null;
  const lista = e.body?.reunioesAfetadas;
  return lista && lista.length > 0 ? lista : null;
}

/**
 * RF 06/RF 07/RF 10: mudança na agenda do docente em dois passos. Pergunta à API quais reuniões seriam
 * canceladas (prévia, nada é gravado); sem nenhuma, grava direto. Com alguma, guarda a mudança até o docente
 * ver a lista e informar o motivo (RF 11); só então grava, e a API cancela as reuniões na mesma transação.
 */
export function useReunioesAfetadas() {
  const [pendente, setPendente] = useState<Pendente | null>(null);
  const [confirmando, setConfirmando] = useState(false);
  const [erro, setErro] = useState("");

  /** Erros da prévia ou do salvamento direto sobem para a tela que chamou. */
  async function executar(previa: () => Promise<ReuniaoAfetada[]>, salvar: Salvar, aoConcluir: () => void) {
    const reunioes = await previa();
    if (reunioes.length > 0) {
      setErro("");
      setPendente({ reunioes, salvar, aoConcluir, aviso: "" });
      return;
    }
    try {
      await salvar(null);
      aoConcluir();
    } catch (e) {
      // Alguém agendou na janela entre a prévia e o salvamento: mostra a lista em vez do erro.
      const novas = reunioesDoErro(e);
      if (!novas) throw e;
      setErro("");
      setPendente({ reunioes: novas, salvar, aoConcluir, aviso: "Uma reunião foi marcada enquanto você editava." });
    }
  }

  async function confirmar(motivo: string) {
    if (!pendente) return;
    setConfirmando(true);
    setErro("");
    try {
      await pendente.salvar({ idsAgendamentos: pendente.reunioes.map((r) => r.id), motivo: motivo.trim() });
      setPendente(null);
      pendente.aoConcluir();
    } catch (e) {
      const novas = reunioesDoErro(e);
      if (novas) {
        setPendente({ ...pendente, reunioes: novas, aviso: "A lista mudou desde que você abriu esta janela. Confira de novo." });
      } else {
        setErro((e as ApiError).message);
      }
    } finally {
      setConfirmando(false);
    }
  }

  return {
    pendente,
    confirmando,
    erro,
    executar,
    confirmar,
    desistir: () => setPendente(null),
  };
}
