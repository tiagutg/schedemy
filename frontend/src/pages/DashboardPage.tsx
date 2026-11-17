import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import {
  IconArrowRight,
  IconBan,
  IconChevronRight,
  IconHourglassHigh,
  IconPlus,
  IconStar,
  IconUserPlus,
} from "@tabler/icons-react";
import { agendamentosApi } from "@/api/agendamentos";
import { avaliacoesApi } from "@/api/avaliacoes";
import { usuariosApi } from "@/api/usuarios";
import { useAuth } from "@/context/AuthContext";
import Spinner from "@/components/common/Spinner";
import AvisoSemDisponibilidade from "./AvisoSemDisponibilidade";
import ConvitesPendentes from "./ConvitesPendentes";
import CartoesResumo from "./painel/CartoesResumo";
import GraficoReunioes from "./painel/GraficoReunioes";
import Pill from "@/components/common/Pill";
import { Button } from "@/components/ui/button";
import { Card, CardAction, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { formatarData, formatarHora } from "@/utils/format";
import { intervaloDoPainel, proximasReunioes, resumoDoMes } from "@/utils/painel";
import { descreverEnvolvidos, situacaoDaReuniao } from "@/utils/reuniao";
import type { Agendamento } from "@/types";

// Teto da busca do painel: 3 meses para trás e 30 dias à frente cabem com folga.
const LIMITE_REUNIOES = 2000;

interface DadosPainel {
  /** Reuniões do intervalo do painel (base dos cartões, do gráfico e das próximas). */
  reunioes: Agendamento[];
  paraAvaliar: number;
  totalUsuarios: number | null;
}

export default function DashboardPage() {
  const { perfilVinculado: eu, pode } = useAuth();
  const [dados, setDados] = useState<DadosPainel | null>(null);
  // Incrementada quando um convite é respondido, para atualizar os números.
  const [versao, setVersao] = useState(0);

  // Quem não acompanha a agenda geral vê os números dos próprios agendamentos.
  const meuId = pode("agendamentos.verTodos") ? null : (eu?.id ?? null);
  const ehAluno = pode("avaliacoes.criar") && !!eu;
  const ehDocente = pode("disponibilidades.gerenciar");

  useEffect(() => {
    let cancelado = false;
    const intervaloPainel = () => {
      const { inicio, fim } = intervaloDoPainel();
      return { dataInicio: inicio, dataFim: fim };
    };

    async function avaliacoesPendentes(): Promise<number> {
      if (!ehAluno || !eu) return 0;
      const [concluidas, avaliadas] = await Promise.all([
        agendamentosApi.listarPorUsuario(eu.id, { status: "CONCLUIDO", size: 100 }),
        avaliacoesApi.listarPorAluno(eu.id, { size: 200 }),
      ]);
      const jaAvaliadas = new Set(avaliadas.conteudo.map((a) => a.idAgendamento));
      return concluidas.conteudo.filter((a) => !jaAvaliadas.has(a.id)).length;
    }

    async function carregar() {
      try {
        const [pagina, paraAvaliar, usuarios] = await Promise.all([
          meuId
            ? agendamentosApi.listarPorUsuario(meuId, { ...intervaloPainel(), size: LIMITE_REUNIOES })
            : agendamentosApi.listarTodos({ ...intervaloPainel(), size: LIMITE_REUNIOES }),
          avaliacoesPendentes(),
          pode("usuarios.gerenciar") ? usuariosApi.listar({ page: 0, size: 1 }) : null,
        ]);
        if (cancelado) return;
        setDados({ reunioes: pagina.conteudo, paraAvaliar, totalUsuarios: usuarios?.totalElementos ?? null });
      } catch {
        // painel é apenas informativo — erros de rede já aparecem em outras telas
        if (!cancelado) setDados({ reunioes: [], paraAvaliar: 0, totalUsuarios: null });
      }
    }
    carregar();
    return () => {
      cancelado = true;
    };
  }, [meuId, versao, ehAluno, eu, pode]);

  const proximas = dados ? proximasReunioes(dados.reunioes, 5) : [];

  const subtitulo = meuId
    ? "Suas reuniões, convites e pendências em um só lugar."
    : "Acompanhe as reuniões marcadas entre alunos, professores e coordenadores.";

  return (
    <>
      {/* Faixa de boas-vindas com o azul da marca */}
      <div className="relative overflow-hidden rounded-xl bg-gradient-to-br from-marinho-900 via-marinho-700 to-marinho-500 px-5 py-6 text-white shadow-sm sm:px-8 sm:py-8">
        <img
          src="/schedemy-logo.png"
          alt=""
          aria-hidden="true"
          className="pointer-events-none absolute -right-4 -bottom-8 hidden w-40 rotate-6 opacity-25 sm:block"
        />
        <div className="relative flex flex-col gap-1">
          <h2 className="text-xl font-semibold tracking-tight break-words sm:text-2xl">
            {`Olá${eu?.nome ? ", " + eu.nome.split(" ")[0] : ""}!`}
          </h2>
          <p className="max-w-2xl text-sm text-white/85">{subtitulo}</p>
        </div>
      </div>

      <AvisoSemDisponibilidade />

      <ConvitesPendentes onRespondido={() => setVersao((v) => v + 1)} />

      {dados?.paraAvaliar ? (
        <Link
          to="/avaliacoes?nova=1"
          className="flex items-center gap-3 rounded-xl border border-amber-300 bg-amber-50 p-4 text-amber-900 transition-colors hover:bg-amber-100 dark:border-amber-500/40 dark:bg-amber-500/10 dark:text-amber-100 dark:hover:bg-amber-500/20"
        >
          <IconStar className="size-6 shrink-0 text-amber-600 dark:text-amber-300" />
          <span className="flex-1 text-sm">
            <span className="font-medium">
              {dados.paraAvaliar === 1
                ? "Você tem 1 reunião para avaliar."
                : `Você tem ${dados.paraAvaliar} reuniões para avaliar.`}
            </span>{" "}
            Conte como foi: leva menos de um minuto.
          </span>
          <IconChevronRight className="size-5 shrink-0" />
        </Link>
      ) : null}

      {!dados ? (
        <div className="flex justify-center py-16 text-muted-foreground">
          <Spinner />
        </div>
      ) : (
        <>
          <CartoesResumo
            resumo={resumoDoMes(dados.reunioes)}
            meusNumeros={!!meuId}
            aguardandoVoce={
              meuId ? dados.reunioes.filter((a) => situacaoDaReuniao(a, eu?.id).suaVez).length : null
            }
          />

          <GraficoReunioes agendamentos={dados.reunioes} meusNumeros={!!meuId} />

          <div className="grid grid-cols-1 gap-4 md:gap-6 @5xl/main:grid-cols-3">
            <Card className="@5xl/main:col-span-2">
              <CardHeader>
                <CardTitle>Próximas reuniões</CardTitle>
                <CardDescription>Confirmadas e aguardando resposta, a partir de hoje</CardDescription>
                {pode("agendamentos.ver") && (
                  <CardAction>
                    <Button asChild variant="outline" size="sm">
                      <Link to="/agendamentos">
                        Ver todas
                        <IconArrowRight />
                      </Link>
                    </Button>
                  </CardAction>
                )}
              </CardHeader>
              <CardContent className="px-0">
                {proximas.length === 0 ? (
                  <p className="px-4 py-8 text-sm text-muted-foreground">Nenhuma reunião marcada a partir de hoje.</p>
                ) : (
                  <ul className="divide-y border-t">
                    {proximas.map((a) => {
                      const situacao = situacaoDaReuniao(a, eu?.id);
                      return (
                        <li key={a.id}>
                          <Link
                            to={`/agendamentos?abrir=${a.id}`}
                            className="flex items-center justify-between gap-3 px-4 py-3 transition-colors hover:bg-muted/50"
                          >
                            <div className="min-w-0">
                              <p className="truncate text-sm font-medium">{a.topico}</p>
                              <p className="truncate text-xs text-muted-foreground">
                                {formatarData(a.dataReuniao)} às {formatarHora(a.horarioInicio)} · {descreverEnvolvidos(a, eu?.id)}
                              </p>
                            </div>
                            <Pill className={situacao.cor}>{situacao.rotulo}</Pill>
                          </Link>
                        </li>
                      );
                    })}
                  </ul>
                )}
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Atalhos</CardTitle>
                <CardDescription>O que você mais faz por aqui</CardDescription>
              </CardHeader>
              <CardContent className="flex flex-col gap-2">
                {pode("usuarios.gerenciar") && (
                  <div className="flex items-center justify-between rounded-lg border border-brand-200 bg-brand-50 px-3 py-2.5 text-sm dark:bg-brand-100">
                    <span className="text-brand-900">Usuários cadastrados</span>
                    <span className="text-lg font-semibold text-brand-700 tabular-nums">{dados.totalUsuarios ?? "—"}</span>
                  </div>
                )}
                {pode("agendamentos.criar") && (
                  <Button asChild className="justify-start">
                    <Link to="/agendamentos?novo=1">
                      <IconPlus />
                      Novo agendamento
                    </Link>
                  </Button>
                )}
                {ehAluno && (
                  <Button asChild variant="outline" className="justify-start">
                    <Link to="/avaliacoes?nova=1">
                      <IconStar />
                      Avaliar uma reunião
                    </Link>
                  </Button>
                )}
                {ehDocente && (
                  <>
                    <Button asChild variant="outline" className="justify-start">
                      <Link to="/disponibilidades">
                        <IconHourglassHigh />
                        Meus horários de atendimento
                      </Link>
                    </Button>
                    <Button asChild variant="outline" className="justify-start">
                      <Link to="/bloqueios?novo=1">
                        <IconBan />
                        Bloquear um período
                      </Link>
                    </Button>
                  </>
                )}
                {pode("usuarios.gerenciar") && (
                  <Button asChild variant="outline" className="justify-start">
                    <Link to="/usuarios">
                      <IconUserPlus />
                      Cadastrar usuário
                    </Link>
                  </Button>
                )}
              </CardContent>
            </Card>
          </div>
        </>
      )}
    </>
  );
}
