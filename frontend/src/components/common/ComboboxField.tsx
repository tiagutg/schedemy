import { useId, useMemo, useRef, useState, type KeyboardEvent, type ReactNode } from "react";
import { IconChevronDown, IconX } from "@tabler/icons-react";
import { cn } from "@/lib/utils";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { normalizar } from "@/utils/format";
import { FieldError } from "./fields";

export interface OpcaoCombobox {
  valor: string | number;
  rotulo: string;
  /** Texto extra só para a busca (ex.: RA). */
  busca?: string;
  /** Aparece à direita na lista (ex.: o perfil). */
  detalhe?: ReactNode;
}

interface ComboboxFieldProps {
  label: ReactNode;
  options: OpcaoCombobox[];
  value: string;
  onChange: (valor: string) => void;
  placeholder?: string;
  required?: boolean;
  error?: string;
  className?: string;
  /** Mostra o "x" para voltar ao vazio (ex.: filtro "Todas as pessoas"). */
  limpavel?: boolean;
  /** Texto quando a busca não acha ninguém. */
  semResultado?: string;
}

const MAX_VISIVEIS = 50;
// Altura máxima da lista (max-h-64) mais a folga, para decidir se cabe abaixo do campo.
const ALTURA_LISTA = 256 + 8;

/**
 * Espaço livre abaixo e acima do campo, dentro da área visível: a tela e, num modal,
 * o contêiner com rolagem que corta o que passa dele.
 */
function espacoAoRedor(campo: HTMLElement): { abaixo: number; acima: number } {
  const r = campo.getBoundingClientRect();
  let topo = 0;
  let base = window.innerHeight;
  for (let el = campo.parentElement; el; el = el.parentElement) {
    if (/(auto|scroll|hidden)/.test(getComputedStyle(el).overflowY)) {
      const c = el.getBoundingClientRect();
      topo = Math.max(topo, c.top);
      base = Math.min(base, c.bottom);
      break;
    }
  }
  return { abaixo: base - r.bottom, acima: r.top - topo };
}

/**
 * Campo de busca e seleção: a pessoa digita e a lista vai sendo filtrada
 * (sem diferenciar maiúsculas e acentos). Setas, Enter e Esc funcionam como num select.
 */
export default function ComboboxField({
  label,
  options,
  value,
  onChange,
  placeholder,
  required,
  error,
  className,
  limpavel = false,
  semResultado = "Ninguém encontrado.",
}: ComboboxFieldProps) {
  const id = useId();
  const idLista = `${id}-lista`;
  const selecionada = options.find((o) => String(o.valor) === value) ?? null;
  const [aberto, setAberto] = useState(false);
  // null = não digitou nada desde que abriu: mostra todas as opções.
  const [texto, setTexto] = useState<string | null>(null);
  const [destaque, setDestaque] = useState(0);
  // Abre para cima quando não cabe embaixo (ex.: campo perto do fim do modal).
  const [paraCima, setParaCima] = useState(false);
  const [alturaMaxima, setAlturaMaxima] = useState<number | undefined>(undefined);
  const caixa = useRef<HTMLDivElement>(null);

  const filtradas = useMemo(() => {
    const termo = normalizar((texto ?? "").trim());
    const lista = termo
      ? options.filter((o) => normalizar(`${o.rotulo} ${o.busca ?? ""}`).includes(termo))
      : options;
    return lista.slice(0, MAX_VISIVEIS);
  }, [options, texto]);

  function abrir() {
    if (aberto) return;
    if (caixa.current) {
      const { abaixo, acima } = espacoAoRedor(caixa.current);
      const cima = abaixo < ALTURA_LISTA && acima > abaixo;
      setParaCima(cima);
      // Nunca menor que ~4 opções, para continuar usável mesmo com pouco espaço.
      setAlturaMaxima(Math.max(Math.min(ALTURA_LISTA, cima ? acima : abaixo) - 8, 140));
    }
    setAberto(true);
    setTexto(null);
    const atual = filtradas.findIndex((o) => String(o.valor) === value);
    setDestaque(Math.max(atual, 0));
  }
  function fechar() {
    setAberto(false);
    setTexto(null);
  }
  function escolher(opcao: OpcaoCombobox) {
    onChange(String(opcao.valor));
    fechar();
  }

  function aoTeclar(e: KeyboardEvent<HTMLInputElement>) {
    if (e.key === "ArrowDown" || e.key === "ArrowUp") {
      e.preventDefault();
      if (!aberto) return abrir();
      const passo = e.key === "ArrowDown" ? 1 : -1;
      setDestaque((d) => Math.min(Math.max(d + passo, 0), Math.max(filtradas.length - 1, 0)));
    } else if (e.key === "Enter") {
      if (!aberto) return;
      e.preventDefault();
      const opcao = filtradas[destaque];
      if (opcao) escolher(opcao);
    } else if (e.key === "Escape" && aberto) {
      e.preventDefault();
      fechar();
    }
  }

  const idDestaque = aberto && filtradas[destaque] ? `${id}-op-${destaque}` : undefined;

  return (
    <div className={cn("flex flex-col gap-2", className)}>
      <Label htmlFor={id}>
        {label} {required && <span className="text-destructive">*</span>}
      </Label>
      <div ref={caixa} className="relative">
        <Input
          id={id}
          role="combobox"
          autoComplete="off"
          aria-expanded={aberto}
          aria-controls={idLista}
          aria-autocomplete="list"
          aria-activedescendant={idDestaque}
          aria-invalid={!!error || undefined}
          placeholder={placeholder}
          className="pr-14"
          value={texto ?? selecionada?.rotulo ?? ""}
          onFocus={(e) => {
            abrir();
            e.currentTarget.select();
          }}
          onClick={abrir}
          onBlur={fechar}
          onKeyDown={aoTeclar}
          onChange={(e) => {
            setAberto(true);
            setTexto(e.target.value);
            setDestaque(0);
          }}
        />
        <div className="pointer-events-none absolute inset-y-0 right-2 flex items-center gap-1 text-muted-foreground">
          {limpavel && value && (
            <button
              type="button"
              aria-label="Limpar"
              className="pointer-events-auto rounded-sm p-0.5 hover:bg-muted hover:text-foreground"
              // mousedown: escolhe antes do blur do campo
              onMouseDown={(e) => {
                e.preventDefault();
                onChange("");
                fechar();
              }}
            >
              <IconX className="size-3.5" />
            </button>
          )}
          <IconChevronDown className="size-4" />
        </div>
        {aberto && (
          <ul
            id={idLista}
            role="listbox"
            className={cn(
              "absolute right-0 left-0 z-50 max-h-64 overflow-y-auto rounded-lg border bg-popover p-1 text-popover-foreground shadow-md",
              paraCima ? "bottom-full mb-1" : "top-full mt-1"
            )}
            style={{ maxHeight: alturaMaxima }}
          >
            {filtradas.length === 0 ? (
              <li className="px-2 py-1.5 text-sm text-muted-foreground">{semResultado}</li>
            ) : (
              filtradas.map((o, i) => (
                <li
                  key={o.valor}
                  id={`${id}-op-${i}`}
                  role="option"
                  aria-selected={String(o.valor) === value}
                  onMouseDown={(e) => {
                    e.preventDefault();
                    escolher(o);
                  }}
                  onMouseEnter={() => setDestaque(i)}
                  className={cn(
                    "flex cursor-pointer items-center justify-between gap-2 rounded-md px-2 py-1.5 text-sm",
                    i === destaque && "bg-muted",
                    String(o.valor) === value && "font-medium"
                  )}
                >
                  <span className="min-w-0 truncate">{o.rotulo}</span>
                  {o.detalhe}
                </li>
              ))
            )}
          </ul>
        )}
      </div>
      {error && <FieldError>{error}</FieldError>}
    </div>
  );
}
