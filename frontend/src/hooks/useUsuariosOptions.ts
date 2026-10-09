import { useEffect, useState } from "react";
import { usuariosApi } from "@/api/usuarios";
import type { Usuario } from "@/types";

/** Carrega uma lista de usuários (opcionalmente filtrada por tipo) para popular seletores. */
export function useUsuariosOptions(tipo?: string) {
  const [usuarios, setUsuarios] = useState<Usuario[]>([]);
  const [carregando, setCarregando] = useState(true);

  useEffect(() => {
    let cancelado = false;
    setCarregando(true);
    usuariosApi
      .listar({ tipo, ativo: true, size: 200, sort: "nome,asc" })
      .then((pagina) => {
        if (!cancelado) setUsuarios(pagina.conteudo);
      })
      .catch(() => !cancelado && setUsuarios([]))
      .finally(() => !cancelado && setCarregando(false));
    return () => {
      cancelado = true;
    };
  }, [tipo]);

  return { usuarios, carregando };
}
