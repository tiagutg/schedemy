import { lazy, Suspense } from "react";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import { AuthProvider } from "@/context/AuthContext";
import { ToastProvider } from "@/context/ToastContext";
import { TooltipProvider } from "@/components/ui/tooltip";
import RotaProtegida from "@/components/layout/RotaProtegida";
import AppLayout from "@/components/layout/AppLayout";
import CarregandoPagina from "@/components/common/CarregandoPagina";

// Cada página vira um chunk próprio, baixado só quando a rota é acessada.
const LoginPage = lazy(() => import("@/pages/LoginPage"));
const CadastroPage = lazy(() => import("@/pages/CadastroPage"));
const DashboardPage = lazy(() => import("@/pages/DashboardPage"));
const UsuariosPage = lazy(() => import("@/pages/UsuariosPage"));
const DuracoesPage = lazy(() => import("@/pages/DuracoesPage"));
const DisponibilidadesPage = lazy(() => import("@/pages/DisponibilidadesPage"));
const BloqueiosPage = lazy(() => import("@/pages/BloqueiosPage"));
const AgendamentosPage = lazy(() => import("@/pages/agendamentos/AgendamentosPage"));
const AvaliacoesPage = lazy(() => import("@/pages/AvaliacoesPage"));
const NotificacoesPage = lazy(() => import("@/pages/NotificacoesPage"));
const PerfilPage = lazy(() => import("@/pages/PerfilPage"));
const AuditoriaPage = lazy(() => import("@/pages/AuditoriaPage"));
const PrivacidadePage = lazy(() => import("@/pages/PrivacidadePage"));
const NaoEncontradaPage = lazy(() => import("@/pages/NaoEncontradaPage"));

export default function App() {
  return (
    <TooltipProvider>
      <ToastProvider>
        <AuthProvider>
          {/* No GitHub Pages o site fica em /schedemy/ (BASE_URL); no desenvolvimento, na raiz. */}
          <BrowserRouter basename={import.meta.env.BASE_URL}>
            <Suspense fallback={<CarregandoPagina telaCheia />}>
            <Routes>
              <Route path="/login" element={<LoginPage />} />
              <Route path="/cadastro" element={<CadastroPage />} />
              {/* RNF 18: termo e política abertos a todos, antes e depois do login */}
              <Route path="/privacidade" element={<PrivacidadePage />} />

              <Route
                path="/"
                element={
                  <RotaProtegida>
                    <AppLayout />
                  </RotaProtegida>
                }
              >
                <Route index element={<DashboardPage />} />
                <Route
                  path="usuarios"
                  element={
                    <RotaProtegida permissao="usuarios.gerenciar">
                      <UsuariosPage />
                    </RotaProtegida>
                  }
                />
                <Route
                  path="duracoes"
                  element={
                    <RotaProtegida permissao="duracoes.gerenciar">
                      <DuracoesPage />
                    </RotaProtegida>
                  }
                />
                <Route
                  path="disponibilidades"
                  element={
                    <RotaProtegida permissao="disponibilidades.gerenciar">
                      <DisponibilidadesPage />
                    </RotaProtegida>
                  }
                />
                <Route
                  path="bloqueios"
                  element={
                    <RotaProtegida permissao="bloqueios.gerenciar">
                      <BloqueiosPage />
                    </RotaProtegida>
                  }
                />
                <Route
                  path="agendamentos"
                  element={
                    <RotaProtegida permissao="agendamentos.ver">
                      <AgendamentosPage />
                    </RotaProtegida>
                  }
                />
                <Route
                  path="avaliacoes"
                  element={
                    <RotaProtegida permissao="avaliacoes.ver">
                      <AvaliacoesPage />
                    </RotaProtegida>
                  }
                />
                <Route
                  path="notificacoes"
                  element={
                    <RotaProtegida permissao="notificacoes.ver">
                      <NotificacoesPage />
                    </RotaProtegida>
                  }
                />
                <Route
                  path="auditoria"
                  element={
                    <RotaProtegida permissao="auditoria.ver">
                      <AuditoriaPage />
                    </RotaProtegida>
                  }
                />
                <Route path="perfil" element={<PerfilPage />} />
                {/* Qualquer outro endereço: página de não encontrada dentro do painel */}
                <Route path="*" element={<NaoEncontradaPage />} />
              </Route>
            </Routes>
            </Suspense>
          </BrowserRouter>
        </AuthProvider>
      </ToastProvider>
    </TooltipProvider>
  );
}
