import { useState, type ChangeEventHandler } from "react";
import { IconEye, IconEyeOff } from "@tabler/icons-react";
import { Input } from "@/components/ui/input";

interface CampoSenhaProps {
  id: string;
  value: string;
  onChange: ChangeEventHandler<HTMLInputElement>;
  autoComplete?: string;
  invalido?: boolean;
}

/** Campo de senha com botão de mostrar/ocultar, usado no login e no cadastro. */
export default function CampoSenha({ id, value, onChange, autoComplete = "current-password", invalido = false }: CampoSenhaProps) {
  const [visivel, setVisivel] = useState(false);

  return (
    <div className="relative">
      <Input
        id={id}
        className="pr-10"
        type={visivel ? "text" : "password"}
        autoComplete={autoComplete}
        value={value}
        onChange={onChange}
        aria-invalid={invalido || undefined}
      />
      <button
        type="button"
        onClick={() => setVisivel((v) => !v)}
        className="absolute inset-y-0 right-0 flex w-9 items-center justify-center text-muted-foreground transition-colors hover:text-foreground"
        aria-label={visivel ? "Ocultar senha" : "Mostrar senha"}
      >
        {visivel ? <IconEyeOff className="size-4" /> : <IconEye className="size-4" />}
      </button>
    </div>
  );
}
