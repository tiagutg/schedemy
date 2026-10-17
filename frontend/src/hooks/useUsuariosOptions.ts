import { useEffect, useState } from "react";
import { usuariosApi } from "@/api/usuarios";
import type { Usuario } from "@/types";

/** Carrega uma lista de usuários (opcionalmente filtrada por tipo) para popular seletores. */
export function useUsuariosOptions(tipo?: string) {
  // Guarda para qual filtro a lista foi carregada; enquanto não bate, está carregando.
  const [resultado, setResultado] = useState<{ tipo?: string; usuarios: Usuario[] } | null>(null);

  useEffect(() => {
    let cancelado = false;
    usuariosApi
      .listar({ tipo, ativo: true, size: 200, sort: "nome,asc" })
      .then((pagina) => !cancelado && setResultado({ tipo, usuarios: pagina.conteudo }))
      .catch(() => !cancelado && setResultado({ tipo, usuarios: [] }));
    return () => {
      cancelado = true;
    };
  }, [tipo]);

  const emDia = resultado !== null && resultado.tipo === tipo;
  return { usuarios: emDia ? resultado.usuarios : [], carregando: !emDia };
}
