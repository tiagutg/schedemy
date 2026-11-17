#!/usr/bin/env bash
# Sobe a API do Schedemy usando o banco PostgreSQL no Supabase (perfil "supabase").
# Versão Linux/macOS do subir-api-supabase.ps1.
#
# Uso (dentro da pasta backend):
#   ./subir-api-supabase.sh
#   ./subir-api-supabase.sh /caminho/para/supabase.env
#
# Sem argumento, usa o supabase.env da raiz do projeto (ignorado pelo Git).
# Para voltar ao MySQL local, suba a API normalmente: ./mvnw spring-boot:run
set -euo pipefail

pasta_script="$(cd "$(dirname "$0")" && pwd)"
arquivo="${1:-$(dirname "$pasta_script")/supabase.env}"

if [[ ! -f "$arquivo" ]]; then
    echo "Arquivo de conexao nao encontrado: $arquivo" >&2
    echo "Copie o supabase.env.example (raiz do projeto) para supabase.env e preencha os dados." >&2
    exit 1
fi
echo "Usando a conexao de $arquivo"

# Carrega as variáveis DB_* só neste processo, sem exibir a senha.
while IFS= read -r linha || [[ -n "$linha" ]]; do
    linha="${linha%$'\r'}"
    if [[ "$linha" =~ ^[[:space:]]*([A-Z_]+)[[:space:]]*=[[:space:]]*(.*)$ ]]; then
        valor="${BASH_REMATCH[2]}"
        valor="${valor%"${valor##*[![:space:]]}"}"
        export "${BASH_REMATCH[1]}=$valor"
    fi
done < "$arquivo"

# Vazio ou ainda com o texto de exemplo entre < > conta como não preenchido.
faltando=()
for var in DB_HOST DB_PORT DB_NAME DB_USER DB_PASSWORD; do
    valor="${!var:-}"
    if [[ -z "$valor" || "$valor" =~ \<.*\> ]]; then
        faltando+=("$var")
    fi
done
if (( ${#faltando[@]} )); then
    echo "Preencha no $arquivo: ${faltando[*]}" >&2
    exit 1
fi

# Usa o JDK 21 se o JAVA_HOME não estiver definido.
if [[ -z "${JAVA_HOME:-}" ]]; then
    for jdk in /usr/lib/jvm/java-21-openjdk* /usr/lib/jvm/openjdk-21; do
        if [[ -x "$jdk/bin/java" ]]; then export JAVA_HOME="$jdk"; break; fi
    done
fi
if [[ -z "${JAVA_HOME:-}" ]]; then
    echo "Defina a variavel JAVA_HOME apontando para um JDK 21 antes de rodar." >&2
    exit 1
fi
echo "JAVA_HOME=$JAVA_HOME"

export SPRING_PROFILES_ACTIVE=supabase
echo "Subindo a API com o banco do Supabase ($DB_HOST)..."

# "clean" recompila tudo com o JDK do JAVA_HOME (evita classes compiladas pela IDE em outra versão).
cd "$pasta_script"
exec ./mvnw clean spring-boot:run
