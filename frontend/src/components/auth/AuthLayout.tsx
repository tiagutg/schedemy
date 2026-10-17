import type { ReactNode } from "react";
import { Link } from "react-router-dom";

/**
 * Moldura das telas de acesso (login e cadastro): formulário à esquerda e
 * foto do campus com a marca à direita.
 */
export default function AuthLayout({ children }: { children: ReactNode }) {
  return (
    <div className="grid min-h-svh lg:grid-cols-2">
      {/* Coluna do formulário */}
      {/* pt com safe-area: afasta o conteúdo do recorte da câmera nos celulares */}
      <div className="flex flex-col gap-4 p-6 pt-[max(2.5rem,env(safe-area-inset-top))] md:p-10">
        {/* Marca no canto: só no desktop (no celular ela fica acima do formulário) */}
        <div className="hidden justify-start gap-2 lg:flex">
          <Link to="/login" className="flex items-center gap-2 font-semibold text-brand-700">
            <img
              src="/schedemy-logo.png"
              alt=""
              aria-hidden="true"
              className="size-6 rounded-md object-contain"
            />
            Schedemy
          </Link>
        </div>

        <div className="flex flex-1 items-center justify-center">
          <div className="flex w-full max-w-xs flex-col gap-8">
            {/* Marca centralizada no celular/tablet, onde a foto lateral não aparece */}
            <Link to="/login" className="flex flex-col items-center gap-2 lg:hidden">
              <img
                src="/schedemy-logo.png"
                alt=""
                aria-hidden="true"
                className="size-16 object-contain drop-shadow-sm"
              />
              <span className="text-lg font-semibold tracking-tight text-brand-700">Schedemy</span>
            </Link>
            {children}
          </div>
        </div>

        <p className="hidden text-center text-xs text-muted-foreground md:text-left [@media(min-height:700px)]:block">
          Sistema de agendamentos acadêmicos
        </p>
      </div>

      {/* Coluna da imagem */}
      <div className="relative hidden bg-muted lg:block">
        <img
          src="/campus.jpg"
          alt="Campus da instituição ao entardecer"
          className="absolute inset-0 h-full w-full object-cover"
        />
        {/* Véu azul da marca para dar contraste ao conteúdo sobreposto */}
        <div className="absolute inset-0 bg-marinho-950/30" />
        <div className="absolute inset-0 bg-gradient-to-t from-marinho-950/90 via-marinho-950/45 to-marinho-900/25" />

        <div className="absolute inset-0 flex flex-col items-center justify-center p-12 text-center text-white">
          <img
            src="/schedemy-logo.png"
            alt="Logo do Schedemy"
            className="w-28 drop-shadow-[0_12px_28px_rgba(13,30,49,0.55)] xl:w-32"
          />
          <h2 className="mt-6 text-5xl font-semibold tracking-tight drop-shadow-md">Schedemy</h2>
          <p className="mt-3 max-w-sm text-base leading-relaxed text-balance text-white/90 drop-shadow">
            Agendamentos acadêmicos organizados em um só lugar, alunos, professores,
            coordenação e recepção.
          </p>
        </div>
      </div>
    </div>
  );
}
