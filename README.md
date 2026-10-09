# 🚀 Schedemy

Guia de configuração, compilação e execução do projeto **Schedemy** em ambiente local.

---

## 🛠️ Pré-requisitos

Certifique-se de ter as seguintes ferramentas instaladas e configuradas em sua máquina:

- **Java JDK**
- **Apache Maven**
- **MySQL Server** (rodando na porta `3306`)

---

## ⚙️ Passo a Passo para Instalação e Execução

### 1. Configuração do Banco de Dados
1. Certifique-se de que o serviço do **MySQL** está iniciado na porta `3306`.
2. Execute o script de criação do banco de dados localizado no caminho:
   ```text
   ./schedemy-main/schedemy/db/db_schedemy.sql
   ```

> ⚠️ **Nota:** A configuração padrão do projeto espera um usuário do MySQL **sem senha**.

---

### 2. Compilação do Projeto
Navegue até o diretório do projeto e execute o comando do Maven para instalar as dependências e compilar a aplicação:

```bash
cd ./schedemy-main/schedemy
mvn clean install
```

---

### 3. Execução da Aplicação
No mesmo diretório (`./schedemy-main/schedemy`), execute o seguinte comando para iniciar o servidor Spring Boot:

```bash
mvn spring-boot:run
```

---

## 🌐 Acesso e Testes da API

Acesse a interface interativa do Swagger no seu navegador para visualizar e testar os endpoints disponíveis:

- **URL do Swagger UI:** [http://localhost:8080/swagger-ui/index.html#/](http://localhost:8080/swagger-ui/index.html#/)

### 🔑 Credenciais de Acesso (Login)

| Parâmetro | Valor |
| :--- | :--- |
| **Usuário** | `admin` |
| **Senha** | `admin123` |
