<#
  Sobe a API do Schedemy usando o banco PostgreSQL no Supabase (perfil "supabase").

  Uso (no PowerShell, dentro da pasta backend):
    .\subir-api-supabase.ps1
    .\subir-api-supabase.ps1 -Arquivo C:\caminho\para\supabase.env

  Sem -Arquivo, usa o supabase.env da raiz do projeto (ignorado pelo Git: nunca vai para o GitHub).
  O modelo do arquivo está em supabase.env.example, também na raiz do projeto.
  Para voltar ao MySQL local, suba a API normalmente: .\mvnw.cmd spring-boot:run
#>
param(
    [string]$Arquivo
)

$ErrorActionPreference = 'Stop'

$raiz = Split-Path $PSScriptRoot -Parent
if (-not $Arquivo) { $Arquivo = Join-Path $raiz 'supabase.env' }

if (-not (Test-Path $Arquivo)) {
    Write-Host "Arquivo de conexão não encontrado: $Arquivo" -ForegroundColor Red
    Write-Host 'Copie o supabase.env.example (raiz do projeto) para supabase.env, na mesma pasta, e preencha os dados.'
    exit 1
}
Write-Host "Usando a conexão de $Arquivo"

# Carrega as variáveis DB_* só neste processo, sem exibir a senha.
$obrigatorias = 'DB_HOST', 'DB_PORT', 'DB_NAME', 'DB_USER', 'DB_PASSWORD'
foreach ($linha in Get-Content $Arquivo) {
    if ($linha -match '^\s*([A-Z_]+)\s*=\s*(.*)$') {
        Set-Item -Path "env:$($Matches[1])" -Value $Matches[2].Trim()
    }
}
# Vazio ou ainda com o texto de exemplo entre < > conta como não preenchido.
$faltando = $obrigatorias | Where-Object {
    $valor = (Get-Item "env:$_" -ErrorAction SilentlyContinue).Value
    -not $valor -or $valor -match '<.*>'
}
if ($faltando) {
    Write-Host "Preencha no $Arquivo : $($faltando -join ', ')" -ForegroundColor Red
    exit 1
}

if (-not $env:JAVA_HOME) {
    Write-Host 'Defina a variável JAVA_HOME apontando para um JDK 21 antes de rodar.' -ForegroundColor Red
    exit 1
}

$env:SPRING_PROFILES_ACTIVE = 'supabase'
Write-Host "Subindo a API com o banco do Supabase ($env:DB_HOST)..." -ForegroundColor Cyan

# "clean" recompila tudo com o JDK do JAVA_HOME (evita classes compiladas pela IDE em outra versão).
Set-Location $PSScriptRoot
& .\mvnw.cmd clean spring-boot:run
