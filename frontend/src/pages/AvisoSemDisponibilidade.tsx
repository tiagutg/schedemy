import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { IconCalendarExclamation } from "@tabler/icons-react";
import { disponibilidadesApi } from "@/api/disponibilidades";
import { useAuth } from "@/context/AuthContext";
import { Button } from "@/components/ui/button";

/**
 * Lembra o professor/coordenador de cadastrar os horários de atendimento:
 * sem disponibilidade ativa, nenhum aluno consegue agendar com ele (RN 06).
 */
export default function AvisoSemDisponibilidade() {
  const { perfil, perfilVinculado: eu } = useAuth();
  const ehDocente = perfil === "PROFESSOR" || perfil === "COORDENADOR";
  const [semHorarios, setSemHorarios] = useState(false);

  useEffect(() => {
    if (!ehDocente || !eu) return;
    let cancelado = false;
    disponibilidadesApi
      .listar({ usuarioId: eu.id, ativo: true, size: 1 })
      .then((p) => !cancelado && setSemHorarios(p.totalElementos === 0))
      .catch(() => !cancelado && setSemHorarios(false));
    return () => {
      cancelado = true;
    };
  }, [ehDocente, eu]);

  if (!ehDocente || !semHorarios) return null;

  return (
    <div
      role="status"
      className="flex flex-col gap-3 rounded-xl border border-amber-300 bg-amber-50 p-4 text-amber-900 sm:flex-row sm:items-center dark:border-amber-500/40 dark:bg-amber-500/10 dark:text-amber-100"
    >
      <IconCalendarExclamation className="size-6 shrink-0 text-amber-600 dark:text-amber-300" />
      <div className="flex-1">
        <p className="font-medium">Você ainda não cadastrou seus horários de atendimento</p>
        <p className="text-sm opacity-90">
          Sem eles, nenhum aluno consegue marcar reunião com você.
        </p>
      </div>
      <Button asChild size="sm">
        <Link to="/disponibilidades">Cadastrar horários</Link>
      </Button>
    </div>
  );
}
