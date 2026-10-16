export function formatarData(dataISO: string | null | undefined): string {
  if (!dataISO) return "—";
  const [ano, mes, dia] = dataISO.split("-");
  if (!ano || !mes || !dia) return dataISO;
  return `${dia}/${mes}/${ano}`;
}

export function formatarHora(horaISO: string | null | undefined): string {
  if (!horaISO) return "—";
  return horaISO.slice(0, 5);
}

export function formatarDataHora(dataHoraISO: string | null | undefined): string {
  if (!dataHoraISO) return "—";
  const data = new Date(dataHoraISO);
  if (Number.isNaN(data.getTime())) return dataHoraISO;
  return data.toLocaleString("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

/** Converte uma data ISO (yyyy-MM-dd) em um objeto Date local, evitando bugs de fuso horário. */
export function paraDataLocal(dataISO: string | null | undefined): Date | null {
  if (!dataISO) return null;
  const [ano, mes, dia] = dataISO.split("-").map(Number);
  return new Date(ano, mes - 1, dia);
}

export function hojeISO(): string {
  const hoje = new Date();
  const mes = String(hoje.getMonth() + 1).padStart(2, "0");
  const dia = String(hoje.getDate()).padStart(2, "0");
  return `${hoje.getFullYear()}-${mes}-${dia}`;
}
