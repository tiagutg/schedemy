import type { ReactNode } from "react";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";

export default function Pill({ children, className = "" }: { children: ReactNode; className?: string }) {
  return (
    <Badge variant="outline" className={cn("border-transparent px-1.5", className)}>
      {children}
    </Badge>
  );
}
