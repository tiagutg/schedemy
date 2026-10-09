import { createContext, useCallback, useContext, useRef, type ReactNode } from "react";
import { toast as sonner } from "sonner";
import { Toaster } from "@/components/ui/sonner";

type TipoToast = "sucesso" | "erro" | "info";

interface ToastApi {
  sucesso: (msg: string) => number;
  erro: (msg: string) => number;
  info: (msg: string) => number;
}

const ToastContext = createContext<ToastApi | null>(null);

// Notificações exibidas com o Toaster (sonner) do shadcn/ui.
const EXIBIR: Record<TipoToast, typeof sonner.success> = {
  sucesso: sonner.success,
  erro: sonner.error,
  info: sonner.info,
};

export function ToastProvider({ children }: { children: ReactNode }) {
  const proximoId = useRef(0);

  const notificar = useCallback((mensagem: string, tipo: TipoToast = "info", duracaoMs = 4500) => {
    const id = ++proximoId.current;
    EXIBIR[tipo](mensagem, {
      id,
      duration: duracaoMs || Infinity,
      closeButton: true,
    });
    return id;
  }, []);

  const toast: ToastApi = {
    sucesso: (msg) => notificar(msg, "sucesso"),
    erro: (msg) => notificar(msg, "erro", 7000),
    info: (msg) => notificar(msg, "info"),
  };

  return (
    <ToastContext.Provider value={toast}>
      {children}
      <Toaster position="bottom-right" />
    </ToastContext.Provider>
  );
}

export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error("useToast deve ser usado dentro de um ToastProvider");
  return ctx;
}
