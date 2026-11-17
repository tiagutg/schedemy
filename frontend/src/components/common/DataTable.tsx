import type { Key, KeyboardEvent, ReactNode } from "react";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Card, CardContent, CardFooter } from "@/components/ui/card";
import Spinner from "./Spinner";
import EmptyState from "./EmptyState";

export interface Coluna<T> {
  chave: string;
  titulo: ReactNode;
  render?: (linha: T) => ReactNode;
}

interface DataTableProps<T> {
  colunas: Coluna<T>[];
  linhas: T[];
  carregando?: boolean;
  chaveLinha: (linha: T) => Key;
  linhaVazia?: ReactNode;
  acoes?: (linha: T) => ReactNode;
  /** Torna a linha (e o cartão no celular) clicável. */
  aoClicarLinha?: (linha: T) => void;
  /** No celular, sem rodapé de ações: o cartão inteiro já abre a linha (requer aoClicarLinha). */
  ocultarAcoesNoCartao?: boolean;
}

function valorCelula<T>(coluna: Coluna<T>, linha: T): ReactNode {
  return coluna.render ? coluna.render(linha) : (linha as Record<string, ReactNode>)[coluna.chave];
}

/**
 * Listagem responsiva: tabela quando a área de conteúdo é larga e cartões
 * empilhados quando é estreita (celular, ou tablet com a sidebar aberta).
 * A troca usa a container query da área principal (@container/main).
 */
export default function DataTable<T>({
  colunas,
  linhas,
  carregando,
  chaveLinha,
  linhaVazia,
  acoes,
  aoClicarLinha,
  ocultarAcoesNoCartao = false,
}: DataTableProps<T>) {
  const totalColunas = colunas.length + (acoes ? 1 : 0);
  const vazio = linhaVazia || <EmptyState titulo="Nenhum registro encontrado" />;
  const [principal, ...demais] = colunas;

  return (
    <>
      {/* Tabela: áreas de conteúdo a partir de 48rem */}
      <div className="hidden overflow-hidden rounded-lg border @3xl/main:block">
        <Table>
          <TableHeader className="sticky top-0 z-10 bg-brand-100 [&_th]:font-semibold [&_th]:text-brand-900">
            <TableRow>
              {colunas.map((coluna) => (
                <TableHead key={coluna.chave}>{coluna.titulo}</TableHead>
              ))}
              {acoes && <TableHead className="text-right">Ações</TableHead>}
            </TableRow>
          </TableHeader>
          <TableBody>
            {carregando ? (
              <TableRow>
                <TableCell colSpan={totalColunas} className="h-24 text-center text-muted-foreground">
                  <Spinner className="mx-auto" />
                </TableCell>
              </TableRow>
            ) : linhas.length === 0 ? (
              <TableRow>
                <TableCell colSpan={totalColunas}>{vazio}</TableCell>
              </TableRow>
            ) : (
              linhas.map((linha) => (
                <TableRow
                  key={chaveLinha(linha)}
                  className={aoClicarLinha ? "cursor-pointer" : undefined}
                  // O teclado usa o botão de ação da linha; o clique na linha é um atalho do mouse.
                  onClick={aoClicarLinha ? () => aoClicarLinha(linha) : undefined}
                >
                  {colunas.map((coluna) => (
                    <TableCell key={coluna.chave}>{valorCelula(coluna, linha)}</TableCell>
                  ))}
                  {acoes && <TableCell className="text-right">{acoes(linha)}</TableCell>}
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      {/* Cartões: áreas de conteúdo estreitas */}
      <div className="flex flex-col gap-3 @3xl/main:hidden">
        {carregando ? (
          <div className="flex h-24 items-center justify-center rounded-lg border text-muted-foreground">
            <Spinner />
          </div>
        ) : linhas.length === 0 ? (
          <div className="rounded-lg border">{vazio}</div>
        ) : (
          linhas.map((linha) => {
            const acoesLinha = ocultarAcoesNoCartao && aoClicarLinha ? null : acoes?.(linha);
            const clicavel = aoClicarLinha
              ? {
                  role: "button",
                  tabIndex: 0,
                  onClick: () => aoClicarLinha(linha),
                  onKeyDown: (e: KeyboardEvent) => {
                    if (e.key === "Enter" || e.key === " ") {
                      e.preventDefault();
                      aoClicarLinha(linha);
                    }
                  },
                }
              : {};
            return (
              <Card
                key={chaveLinha(linha)}
                size="sm"
                className={aoClicarLinha ? "cursor-pointer shadow-xs transition-colors hover:bg-muted/50 focus-visible:ring-2 focus-visible:ring-ring focus-visible:outline-none" : "shadow-xs"}
                {...clicavel}
              >
                <CardContent className="flex flex-col gap-3">
                  {principal && <div className="text-sm font-medium break-words">{valorCelula(principal, linha)}</div>}
                  {demais.length > 0 && (
                    <dl className="grid grid-cols-2 gap-x-4 gap-y-2.5 text-sm">
                      {demais.map((coluna) => (
                        <div key={coluna.chave} className="flex min-w-0 flex-col gap-0.5">
                          <dt className="text-xs text-muted-foreground">{coluna.titulo}</dt>
                          <dd className="break-words">{valorCelula(coluna, linha)}</dd>
                        </div>
                      ))}
                    </dl>
                  )}
                </CardContent>
                {acoesLinha ? (
                  <CardFooter className="flex-wrap justify-end gap-1 [&>div]:flex-wrap">{acoesLinha}</CardFooter>
                ) : null}
              </Card>
            );
          })
        )}
      </div>
    </>
  );
}
