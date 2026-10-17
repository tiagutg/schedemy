# 🚀 Schedemy

Sistema de agendamentos acadêmicos que organiza reuniões entre alunos, professores, coordenação e recepção.

O projeto tem duas partes:

- **Back-end:** API REST em Spring Boot, com banco MySQL.
- **Front-end:** painel web em React, com TypeScript e shadcn/ui.

---

## 🧱 Tecnologias

| Camada | Tecnologias |
| :--- | :--- |
| **Back-end** | Java 21, Spring Boot 4, Spring Data JPA, Spring Security (Basic Auth), Springdoc OpenAPI (Swagger) |
| **Banco de dados** | MySQL 8 |
| **Front-end** | React 19, TypeScript, Vite, Tailwind CSS 4, shadcn/ui, React Router |

---

## 📁 Estrutura do repositório

```text
.
├── backend/          # API REST (Spring Boot + Maven Wrapper)
├── frontend/         # Painel web (React + Vite)
├── db/
│   ├── db_schedemy.sql     # Script de criação do banco (vazio)
│   └── dump_schedemy.sql   # Banco completo com usuários de teste
└── Documentacao/     # Coleção de requisições da API
```

---

## 🛠️ Pré-requisitos

Instale e configure estas ferramentas na sua máquina:

- **Java JDK 21**
- **MySQL Server 8**, rodando na porta `3306`
- **Node.js 20** ou superior (com o npm)

> O Maven não precisa ser instalado: o back-end usa o **Maven Wrapper** (`mvnw`).

---

## ⚙️ Como executar

### 1. Banco de dados

1. Inicie o serviço do **MySQL** na porta `3306`.
2. Importe o dump, que cria o banco `schedemy_db` já com os usuários de teste (veja [Contas de acesso](#-contas-de-acesso)):

   ```bash
   mysql -u root < db/dump_schedemy.sql
   ```

   No MySQL Workbench: **Server → Data Import → Import from Self-Contained File**, selecione `db/dump_schedemy.sql` e clique em **Start Import**.

> ⚠️ **Atenção:** o dump apaga e recria todas as tabelas do `schedemy_db`. Faça backup antes se tiver dados que queira manter.
>
> Para criar o banco vazio, sem usuários, use `db/db_schedemy.sql`. Nesse caso, cadastre o primeiro usuário pela tela "Cadastre-se" ou pelo Swagger.

> ⚠️ **Nota:** por padrão, a API usa o usuário `root` **sem senha**. Para usar outras credenciais, veja a seção [Variáveis de ambiente](#-variáveis-de-ambiente).

### 2. Back-end (API)

```bash
cd backend
./mvnw spring-boot:run
```

No Windows, use `mvnw.cmd spring-boot:run`.

A API fica disponível em **http://localhost:8080**.

### 3. Front-end (painel web)

Em outro terminal:

```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

No Windows, troque `cp` por `copy`.

O painel fica disponível em **http://localhost:5173**.

---

## 🔧 Variáveis de ambiente

### Back-end

Todas são opcionais. Sem elas, a API usa os valores padrão.

| Variável | Padrão | Descrição |
| :--- | :--- | :--- |
| `SERVER_PORT` | `8080` | Porta da API |
| `DB_HOST` | `localhost` | Host do MySQL |
| `DB_PORT` | `3306` | Porta do MySQL |
| `DB_NAME` | `schedemy_db` | Nome do banco |
| `DB_USER` | `root` | Usuário do MySQL |
| `DB_PASSWORD` | *(vazio)* | Senha do MySQL |
| `SHOW_SQL` | `false` | Exibe no log o SQL gerado pelo Hibernate |

### Front-end (`frontend/.env`)

| Variável | Padrão | Descrição |
| :--- | :--- | :--- |
| `VITE_API_BASE_URL` | `http://localhost:8080` | Endereço da API |

---

## 📜 Scripts do front-end

| Comando | Descrição |
| :--- | :--- |
| `npm run dev` | Inicia o servidor de desenvolvimento |
| `npm run build` | Checa os tipos e gera a versão de produção em `dist/` |
| `npm run preview` | Serve localmente a versão de produção |
| `npm run lint` | Analisa o código com o oxlint |
| `npm test` | Roda os testes unitários (Vitest) |
| `npm run test:e2e` | Roda os testes de ponta a ponta (Playwright) |

### Testes de ponta a ponta

Os testes em `frontend/e2e/` usam o painel como um usuário faria: login por perfil, criar, editar, aceitar e remarcar uma reunião, visão semanal e sessão expirada.

Antes de rodar:

1. Importe o `db/dump_schedemy.sql` (os testes usam as contas de teste).
2. Deixe a API rodando em `http://localhost:8080`.
3. Tenha o Google Chrome instalado (os testes usam o navegador da máquina).

O servidor do front-end é iniciado pelos próprios testes, se ainda não estiver no ar. Os agendamentos criados durante os testes são removidos no final.

---

## 🌐 Documentação da API

A interface interativa do Swagger lista e permite testar todos os endpoints:

- **Swagger UI:** [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

A coleção de requisições também está em [`Documentacao/`](Documentacao/).

---

## 🔑 Contas de acesso

O login é feito com o **e-mail e a senha** de um usuário cadastrado no banco. As senhas ficam criptografadas (BCrypt), e o perfil de acesso vem do tipo do usuário.

Estas contas vêm no `db/dump_schedemy.sql`:

| Perfil | E-mail | Senha |
| :--- | :--- | :--- |
| Administrador (acesso total) | `admin@hotmail.com` | `admin123` |
| Coordenador | `marcelo.coord@unisales.edu.br`, `fernanda.coord@unisales.edu.br` | `Senha123@` |
| Professor | `romulo.professor@unisales.edu.br`, `ebert.professor@unisales.edu.br`, `mykael.professor@unisales.edu.br`, `lucas.professor@unisales.edu.br` | `Senha123@` |
| Recepcionista | `ana.recepcao@unisales.edu.br`, `mariana.recepcao@unisales.edu.br` | `Senha123@` |
| Aluno | `kevin.aluno@unisales.edu.br`, `pedro.aluno@unisales.edu.br`, `tiago.aluno@unisales.edu.br`, `diogo.aluno@unisales.edu.br`, `joao.aluno@unisales.edu.br` | `Senha123@` |

> ⚠️ São contas de teste, com senhas públicas. Não use este dump em produção.
