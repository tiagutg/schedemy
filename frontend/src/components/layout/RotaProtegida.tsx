import type { ReactNode } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "@/context/AuthContext";
import AcessoNegado from "./AcessoNegado";

/**
 * Protege uma rota. Sem sessão, manda para o login; com sessão mas sem a
 * permissão exigida, mostra a tela de acesso negado em vez de redirecionar,
 * para que o usuário entenda o que aconteceu.
 */
export default function RotaProtegida({ children, permissao }: { children: ReactNode; permissao?: string }) {
  const { estaAutenticado, pode } = useAuth();
  const local = useLocation();

  if (!estaAutenticado) {
    return <Navigate to="/login" replace state={{ de: local.pathname }} />;
  }

  if (permissao && !pode(permissao)) {
    return <AcessoNegado />;
  }

  return children;
}
