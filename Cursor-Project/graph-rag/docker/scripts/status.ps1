#!/usr/bin/env pwsh
# Show Graph RAG stack status and basic HTTP checks.

$ErrorActionPreference = 'Continue'
$ComposeDir = Split-Path -Parent $PSScriptRoot
Set-Location $ComposeDir

$EnvFile = Join-Path $ComposeDir '.env'
if (Test-Path $EnvFile) {
    docker compose --env-file $EnvFile ps
} else {
    docker compose ps
}

function Test-Url([string]$Name, [string]$Url) {
    try {
        $resp = Invoke-WebRequest -Uri $Url -Method Get -TimeoutSec 5 -UseBasicParsing
        Write-Host ("{0,-16} OK  {1} ({2})" -f $Name, $Url, [int]$resp.StatusCode)
    } catch {
        Write-Host ("{0,-16} FAIL {1} — {2}" -f $Name, $Url, $_.Exception.Message)
    }
}

Write-Host ""
Test-Url 'Neo4j' 'http://127.0.0.1:7474'
Test-Url 'Ollama' 'http://127.0.0.1:11434/api/tags'
try {
    $code = curl.exe -s -o NUL -w "%{http_code}" --max-time 3 http://127.0.0.1:8100/sse
    Write-Host ("{0,-16} {1}  {2}" -f 'GraphRAG SSE', $(if ($code -match '^(200|400|405|406)$') { 'OK' } else { 'FAIL' }), "http://127.0.0.1:8100/sse ($code)")
} catch {
    Write-Host ("{0,-16} FAIL {1}" -f 'GraphRAG SSE', $_.Exception.Message)
}
