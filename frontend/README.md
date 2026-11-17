# Schedemy — painel web

Front-end do **Schedemy**, sistema de agendamento de reuniões acadêmicas da UniSales. Conversa com a API
Spring Boot da pasta [`backend/`](../backend) deste mesmo repositório.

Instalação completa (banco, API e front), variáveis de ambiente, scripts, testes de ponta a ponta e contas de teste
estão no [README da raiz](../README.md). Aqui fica só o que é específico do front-end.

## Stack

- **React 19** + **TypeScript 7** + **Vite 8**
- **React Router 7** para a navegação
- **Tailwind CSS 4** + componentes **shadcn/ui** (Radix) em `src/components/ui/`
- **Recharts** nos gráficos do painel, **Sonner** nos avisos (toasts), ícones **Tabler**, fonte **Geist**
- **Vitest** (testes unitários) e **Playwright** + **axe** (ponta a ponta e acessibilidade)
- **oxlint** para o lint

## Pré-requisitos

- **Node.js 22.12** ou superior, com o npm (o Vitest 5 não roda em versões anteriores; o CI usa o Node 22)
- A API rodando em `http://localhost:8080` (veja o README da raiz)

## Como rodar

```bash
npm install
npm run dev        # http://localhost:5173
```

Para usar outra API, copie `.env.example` para `.env` e ajuste `VITE_API_BASE_URL`. A API só aceita chamadas
das origens configuradas no `CorsConfig` (por padrão `localhost:5173`, `3000` e `4200`; em outro endereço, defina
`CORS_ALLOWED_ORIGINS` na API).

Antes de dar uma tarefa como pronta, rode `npm run lint`, `npm test` e `npm run build` (é o que o CI confere).
Os testes de ponta a ponta (`npm run test:e2e`) exigem a API com o MySQL local e o `db/dump_schedemy.sql`;
**nunca rode contra o Supabase**, porque eles criam e apagam agendamentos.

## Login e permissões

- O login é por **e-mail e senha**: o `AuthContext` troca as credenciais por um token em `POST /api/v1/auth/login`,
  busca o próprio cadastro em `GET /api/v1/usuarios/me` e guarda **só o token** (com a validade) no `localStorage`
  (`schedemy.auth`). A senha não fica no navegador. Quando o token vence, a sessão se encerra sozinha; uma sessão
  no formato antigo (com senha) é apagada ao abrir o app (`src/utils/sessao.ts`).
- Não há cadastro aberto: `/cadastro` explica como obter acesso (a conta é criada pela coordenação/recepção).
- O menu e os botões variam por perfil (aluno, professor, coordenador, recepcionista, admin) conforme
  `src/utils/permissoes.ts`. Esse arquivo **só esconde a interface**: quem protege os dados é a API. Ao mudar uma
  regra de acesso, atualize também o `SecurityConfig` e o `SecurityConfigTest` do back-end.

## Telas

| Rota | Tela |
|---|---|
| `/` | Painel com indicadores, gráficos, convites pendentes e próximas reuniões |
| `/agendamentos` | Lista e visão semanal; criar, editar, responder convite, cancelar e remarcar |
| `/disponibilidades` | Grade semanal de horários do docente |
| `/bloqueios` | Bloqueios de agenda, com os agendamentos impactados |
| `/duracoes` | Durações de reunião permitidas |
| `/usuarios` | Cadastro de usuários (admin e recepção) |
| `/avaliacoes` | Avaliações de reuniões concluídas |
| `/notificacoes` | Avisos do sistema |
| `/perfil` | Dados da própria conta |

Em `src/App.tsx`, cada rota restrita passa por `RotaProtegida` com uma permissão de `src/utils/permissoes.ts`
(ex.: `usuarios.gerenciar`).

## Estrutura de pastas

```
src/
├── api/          # uma função por endpoint; client.ts é o apiFetch central (token Bearer + erros em ApiError)
├── components/
│   ├── auth/     # login e orientação de acesso
│   ├── common/   # tabela, modal, paginação, campos de formulário, filtros...
│   ├── layout/   # AppLayout, menu lateral, cabeçalho e RotaProtegida
│   └── ui/       # componentes shadcn/ui (evite editar à mão)
├── context/      # AuthContext (sessão), AvisosContext (notificações), ToastContext
├── hooks/        # usePaginatedList, useUsuariosOptions, useTema...
├── pages/        # uma tela por arquivo (agendamentos/ e painel/ em subpastas)
├── utils/        # formatação, rótulos do domínio, regras de agenda e permissões (+ testes *.test.ts)
├── types.ts      # tipos dos DTOs da API
├── App.tsx       # rotas
└── main.tsx      # ponto de entrada
e2e/              # testes Playwright (apoio.ts tem os utilitários de login e limpeza)
```

O alias `@/` aponta para `src/`.

## Integração com a API

- Todas as chamadas passam por `src/api/client.ts`, que injeta o cabeçalho `Authorization: Bearer <token>` e converte
  as respostas de erro (`ErrorResponseDTO`: `mensagem`, `camposInvalidos`) em `ApiError`. Um 401 com sessão salva
  encerra a sessão e volta para o login.
- As listas usam a paginação (`page`, `size`) e a ordenação (`sort=campo,asc|desc`) da API.
- Listas de pessoas para escolher convidados usam `GET /api/v1/usuarios/resumo`, que não traz e-mail nem telefone.
- A remarcação usa `POST /api/v1/agendamentos/{id}/remarcacao`, que cancela a original e cria a nova numa
  transação só.
