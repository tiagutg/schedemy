import { IconLoader2 } from "@tabler/icons-react";
import { cn } from "@/lib/utils";

export default function Spinner({ className = "" }: { className?: string }) {
  return <IconLoader2 className={cn("size-4 animate-spin", className)} role="status" aria-label="Carregando" />;
}
