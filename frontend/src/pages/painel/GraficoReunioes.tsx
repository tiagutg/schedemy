import { useState } from "react";
import { Bar, BarChart, CartesianGrid, XAxis, YAxis } from "recharts";
import { Card, CardAction, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { ChartContainer, ChartLegend, ChartLegendContent, ChartTooltip, ChartTooltipContent, type ChartConfig } from "@/components/ui/chart";
import { NativeSelect, NativeSelectOption } from "@/components/ui/native-select";
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group";
import { intervaloDoPeriodo, PERIODOS, serieDiaria, type Periodo } from "@/utils/painel";
import type { Agendamento } from "@/types";

const config = {
  confirmadas: { label: "Confirmadas", color: "var(--primary)" },
  aguardando: { label: "Aguardando resposta", color: "var(--chart-2)" },
  canceladas: { label: "Canceladas", color: "var(--color-brick-700)" },
} satisfies ChartConfig;

function rotuloCurto(dataISO: string): string {
  const [, mes, dia] = dataISO.split("-");
  return `${dia}/${mes}`;
}

function rotuloLongo(dataISO: string): string {
  const [ano, mes, dia] = dataISO.split("-").map(Number);
  return new Date(ano, mes - 1, dia).toLocaleDateString("pt-BR", { weekday: "short", day: "2-digit", month: "short" });
}

/**
 * Reuniões por dia no período escolhido, separadas por situação. Barras empilhadas:
 * são contagens pequenas e discretas, que num gráfico de área viram picos isolados.
 */
export default function GraficoReunioes({ agendamentos, meusNumeros }: { agendamentos: Agendamento[]; meusNumeros: boolean }) {
  const [periodo, setPeriodo] = useState<Periodo>("prox30");
  const { inicio, fim } = intervaloDoPeriodo(periodo);
  const dados = serieDiaria(agendamentos, inicio, fim);
  const total = dados.reduce((soma, d) => soma + d.confirmadas + d.aguardando + d.canceladas, 0);
  const descricao = PERIODOS.find((p) => p.valor === periodo)?.descricao ?? "";

  return (
    <Card className="@container/card">
      <CardHeader>
        <CardTitle>{meusNumeros ? "Suas reuniões por dia" : "Reuniões por dia"}</CardTitle>
        <CardDescription>
          {descricao} · {total} {total === 1 ? "reunião" : "reuniões"}
        </CardDescription>
        <CardAction>
          <ToggleGroup
            type="single"
            value={periodo}
            onValueChange={(v) => v && setPeriodo(v as Periodo)}
            variant="outline"
            aria-label="Período do gráfico"
            className="hidden *:data-[slot=toggle-group-item]:px-4! @[720px]/card:flex"
          >
            {PERIODOS.map((p) => (
              <ToggleGroupItem key={p.valor} value={p.valor}>
                {p.rotulo}
              </ToggleGroupItem>
            ))}
          </ToggleGroup>
          <NativeSelect
            size="sm"
            aria-label="Período do gráfico"
            className="w-40 @[720px]/card:hidden"
            value={periodo}
            onChange={(e) => setPeriodo(e.target.value as Periodo)}
          >
            {PERIODOS.map((p) => (
              <NativeSelectOption key={p.valor} value={p.valor}>
                {p.rotulo}
              </NativeSelectOption>
            ))}
          </NativeSelect>
        </CardAction>
      </CardHeader>
      <CardContent className="px-2 pt-2 sm:px-6">
        <ChartContainer config={config} className="aspect-auto h-[260px] w-full">
          <BarChart data={dados} margin={{ left: 0, right: 8, top: 8 }}>
            <CartesianGrid vertical={false} />
            <XAxis dataKey="data" tickLine={false} axisLine={false} tickMargin={8} minTickGap={28} tickFormatter={rotuloCurto} />
            <YAxis allowDecimals={false} tickLine={false} axisLine={false} width={28} />
            <ChartTooltip
              cursor={{ fill: "var(--muted)" }}
              content={<ChartTooltipContent labelFormatter={(valor) => rotuloLongo(String(valor))} indicator="dot" />}
            />
            <Bar dataKey="confirmadas" stackId="reunioes" fill="var(--color-confirmadas)" maxBarSize={28} />
            <Bar dataKey="aguardando" stackId="reunioes" fill="var(--color-aguardando)" maxBarSize={28} />
            <Bar dataKey="canceladas" stackId="reunioes" fill="var(--color-canceladas)" radius={[4, 4, 0, 0]} maxBarSize={28} />
            <ChartLegend content={<ChartLegendContent />} />
          </BarChart>
        </ChartContainer>
      </CardContent>
    </Card>
  );
}
