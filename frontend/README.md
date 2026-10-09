# Schedemy Web

Front-end (React + Vite) do **Schedemy — Sistema de Agendamento de Reuniões no AVA**, consumindo a
API REST desenvolvida em `schedemy-api` (Spring Boot).

## Stack

- **React 19** + **Vite** (JavaScript)
- **React Router** para navegação
- **Tailwind CSS** para estilização
- Fontes: **Fraunces** (títulos) + **IBM Plex Sans** (texto) + **IBM Plex Mono** (código)
- Autenticação **HTTP Basic** (igual ao back-end), sem dependências de estado externas (Redux etc.)

## Pré-requisitos

- Node.js 18+ e npm
- O back-end **schedemy-api** rodando (por padrão em `http://localhost:8080`) — veja o guia de
  instalação do back-end para colocá-lo no ar antes de usar este front-end.

## Como rodar

```bash
npm install
npm run dev
```

A aplicação sobe em `http://localhost:5173` (padrão do Vite).

Por padrão, o front-end aponta para `http://localhost:8080` como URL da API. Para apontar para
outro endereço, copie `.env.example` para `.env` e ajuste:

```bash
VITE_API_BASE_URL=http://localhost:8080
```

> O back-end já vem com CORS liberado para `localhost:5173`, `3000` e `4200` (veja
> `CorsConfig.java` no projeto `schedemy-api`).

## Login

A tela de login usa as mesmas credenciais HTTP Basic configuradas no `SecurityConfig` do
back-end. Botões de atalho já preenchem usuário/senha de cada papel:

| Usuário | Senha | Papel |
|---|---|---|
| `admin` | `admin123` | ADMIN / COORDENADOR / RECEPCIONISTA |
| `coordenador` | `coordenador123` | COORDENADOR |
| `professor` | `professor123` | PROFESSOR |
| `recepcionista` | `recepcionista123` | RECEPCIONISTA |
| `aluno` | `aluno123` | ALUNO |

## Funcionalidades

- **Painel** — indicadores de agendamentos por status e próximos compromissos.
- **Agendamentos** — criação (com seleção de organizador, convidados, duração, formato),
  listagem com filtros (usuário, status, período), detalhes com aceite/recusa por participante,
  cancelamento (com motivo obrigatório) e remoção.
- **Disponibilidades** — grade semanal de horários de professores/coordenadores.
- **Bloqueios de período** — cadastro de indisponibilidades, com visualização dos agendamentos
  impactados.
- **Durações de reunião** — CRUD das durações padronizadas (10 a 60 minutos).
- **Usuários** — CRUD de alunos, professores, coordenadores e recepcionistas.
- **Avaliações** — consulta e registro de avaliações de reuniões concluídas.
- **Notificações** — fila/histórico de notificações, com opção de marcar como enviada.

## Build de produção

```bash
npm run build
npm run preview   # serve o build em http://localhost:4173 para conferência local
```

Os arquivos finais são gerados em `dist/`.

## Estrutura de pastas

```
src/
├── api/            # funções que conversam com a Schedemy API (fetch)
├── components/
│   ├── layout/      # AppLayout (sidebar/topbar) e RotaProtegida
│   └── ui/           # Table, Modal, Pagination, campos de formulário, etc.
├── context/         # AuthContext (sessão) e ToastContext (notificações)
├── hooks/           # usePaginatedList, useUsuariosOptions
├── pages/           # uma pasta/arquivo por tela
├── utils/           # formatação de datas e rótulos de enums do domínio
├── App.jsx          # rotas
└── main.jsx         # bootstrap do React
```

## Notas de integração com o back-end

- Todas as chamadas usam o wrapper `src/api/client.js`, que injeta o header `Authorization:
  Basic ...` automaticamente a partir da sessão salva em `localStorage`, e normaliza erros no
  mesmo formato do `ErrorResponseDTO` do back-end (com `mensagem` e `camposInvalidos`, quando
  aplicável).
- As páginas respeitam a paginação (`PageResponseDTO`) e a ordenação (`?sort=campo,asc|desc`)
  já implementadas na API.
- Este front-end cobre a primeira camada de uso do sistema. Itens como aprovação de solicitação
  de edição já confirmada, sugestão de remarcação e favoritar convidados ainda não têm CRUD
  próprio na API (ver README do `schedemy-api`) e, por isso, não têm tela aqui.
