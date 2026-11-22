import type { DocumentoLgpd as Documento } from "@/types";

/** RNF 18: termo ou política, com as seções numeradas que vêm da API. */
export default function DocumentoLgpd({ documento, nivel = 2 }: { documento: Documento; nivel?: 2 | 3 }) {
  const Titulo = nivel === 2 ? "h2" : "h3";
  const Secao = nivel === 2 ? "h3" : "h4";
  return (
    <article className="flex flex-col gap-4">
      <Titulo className="text-lg font-semibold tracking-tight">{documento.titulo}</Titulo>
      {documento.secoes.map((s) => (
        <section key={s.titulo} className="flex flex-col gap-1.5">
          <Secao className="text-sm font-semibold">{s.titulo}</Secao>
          {s.paragrafos.map((p, i) => (
            <p key={i} className="text-sm leading-relaxed text-muted-foreground">
              {p}
            </p>
          ))}
        </section>
      ))}
    </article>
  );
}
