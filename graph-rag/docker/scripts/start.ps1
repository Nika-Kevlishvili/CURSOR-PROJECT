#!/usr/bin/env pwsh
# Start portable Graph RAG Docker stack (Neo4j + Ollama + MCP SSE).
# Run from anywhere; resolves compose dir relative to this script.

$ErrorActionPreference = 'Stop'
$ComposeDir = Split-Path -Parent $PSScriptRoot
Set-Location $ComposeDir

$EnvFile = Join-Path $ComposeDir '.env'
$EnvExample = Join-Path $ComposeDir '.env.example'
if (-not (Test-Path $EnvFile)) {
    if (-not (Test-Path $EnvExample)) {
        throw ".env.example not found in $ComposeDir"
    }
    Copy-Item $EnvExample $EnvFile
    Write-Host "Created .env from .env.example — edit NEO4J_PASSWORD and GRAPH_RAG_WORKSPACE_HOST, then re-run."
    exit 2
}

# Load OLLAMA_MODEL from .env (simple KEY=VAL parser)
$OllamaModel = 'qwen2.5:3b'
Get-Content $EnvFile | ForEach-Object {
    if ($_ -match '^\s*OLLAMA_MODEL\s*=\s*(.+)\s*$') {
        $OllamaModel = $Matches[1].Trim().Trim('"').Trim("'")
    }
}

Write-Host "Building / starting Graph RAG stack in $ComposeDir ..."
docker compose --env-file $EnvFile up -d --build
if ($LASTEXITCODE -ne 0) { throw "docker compose up failed (exit $LASTEXITCODE)" }

Write-Host "Waiting for Ollama..."
$deadline = (Get-Date).AddMinutes(5)
do {
    docker compose exec -T ollama ollama list 2>$null | Out-Null
    if ($LASTEXITCODE -eq 0) { break }
    Start-Sleep -Seconds 3
} while ((Get-Date) -lt $deadline)

Write-Host "Pulling model: $OllamaModel (first run may take a while)..."
docker compose exec -T ollama ollama pull $OllamaModel
if ($LASTEXITCODE -ne 0) { throw "ollama pull failed (exit $LASTEXITCODE)" }

Write-Host ""
Write-Host "Stack started."
Write-Host "  Neo4j Browser : http://localhost:7474"
Write-Host "  Ollama API    : http://localhost:11434"
Write-Host "  GraphRAG MCP  : http://localhost:8100/sse"
Write-Host ""
Write-Host "Cursor mcp.json (this machine):"
Write-Host '  "GraphRAG": { "type": "http", "url": "http://127.0.0.1:8100/sse" }'
Write-Host "Other PCs: use http://<SERVER_LAN_IP>:8100/sse"
