import { useId, type ComponentProps, type ReactNode } from "react";
import { cn } from "@/lib/utils";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Checkbox } from "@/components/ui/checkbox";
import { NativeSelect, NativeSelectOption } from "@/components/ui/native-select";

/** Rótulo do campo com o asterisco de obrigatório. */
function Rotulo({ htmlFor, label, required }: { htmlFor: string; label: ReactNode; required?: boolean }) {
  return (
    <Label htmlFor={htmlFor}>
      {label} {required && <span className="text-destructive">*</span>}
    </Label>
  );
}

export function FieldError({ children }: { children: ReactNode }) {
  return <p className="text-sm text-destructive">{children}</p>;
}

// `required` só exibe o asterisco: a validação fica a cargo de cada tela.
type TextFieldProps = Omit<ComponentProps<"input">, "required"> & {
  label: ReactNode;
  error?: string;
  required?: boolean;
};

export function TextField({ label, error, required, className = "", id, ...props }: TextFieldProps) {
  const idGerado = useId();
  const idCampo = id ?? idGerado;
  return (
    <div className={cn("flex flex-col gap-2", className)}>
      <Rotulo htmlFor={idCampo} label={label} required={required} />
      <Input id={idCampo} aria-invalid={!!error || undefined} {...props} />
      {error && <FieldError>{error}</FieldError>}
    </div>
  );
}

type TextAreaFieldProps = Omit<ComponentProps<"textarea">, "required"> & {
  label: ReactNode;
  error?: string;
  required?: boolean;
};

export function TextAreaField({ label, error, required, className = "", id, ...props }: TextAreaFieldProps) {
  const idGerado = useId();
  const idCampo = id ?? idGerado;
  return (
    <div className={cn("flex flex-col gap-2", className)}>
      <Rotulo htmlFor={idCampo} label={label} required={required} />
      <Textarea id={idCampo} className="min-h-[84px] resize-y" aria-invalid={!!error || undefined} {...props} />
      {error && <FieldError>{error}</FieldError>}
    </div>
  );
}

export type Opcao = string | { valor: string | number; rotulo: string };

type SelectFieldProps = Omit<ComponentProps<"select">, "required" | "size"> & {
  label: ReactNode;
  error?: string;
  required?: boolean;
  options: Opcao[];
  placeholder?: string;
};

export function SelectField({ label, error, required, options, placeholder, className = "", id, ...props }: SelectFieldProps) {
  const idGerado = useId();
  const idCampo = id ?? idGerado;
  return (
    <div className={cn("flex flex-col gap-2", className)}>
      <Rotulo htmlFor={idCampo} label={label} required={required} />
      <NativeSelect id={idCampo} className="w-full" aria-invalid={!!error || undefined} {...props}>
        {placeholder && <NativeSelectOption value="">{placeholder}</NativeSelectOption>}
        {options.map((opcao) =>
          typeof opcao === "object" ? (
            <NativeSelectOption key={opcao.valor} value={opcao.valor}>
              {opcao.rotulo}
            </NativeSelectOption>
          ) : (
            <NativeSelectOption key={opcao} value={opcao}>
              {opcao}
            </NativeSelectOption>
          )
        )}
      </NativeSelect>
      {error && <FieldError>{error}</FieldError>}
    </div>
  );
}

interface CheckboxFieldProps {
  label: ReactNode;
  checked: boolean;
  onCheckedChange: (checked: boolean) => void;
  className?: string;
}

export function CheckboxField({ label, checked, onCheckedChange, className = "" }: CheckboxFieldProps) {
  const id = useId();
  return (
    <div className={cn("flex items-center gap-2", className)}>
      <Checkbox id={id} checked={checked} onCheckedChange={(valor) => onCheckedChange(valor === true)} />
      <Label htmlFor={id} className="font-normal">
        {label}
      </Label>
    </div>
  );
}

export function FieldGrid({ children, colunas = 2 }: { children: ReactNode; colunas?: 1 | 2 | 3 }) {
  const classesColunas = colunas === 3 ? "sm:grid-cols-3" : colunas === 1 ? "sm:grid-cols-1" : "sm:grid-cols-2";
  return <div className={`grid grid-cols-1 gap-4 ${classesColunas}`}>{children}</div>;
}
