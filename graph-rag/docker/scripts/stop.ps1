#!/usr/bin/env pwsh
# Stop Graph RAG Docker stack (keeps named volumes / data).

$ErrorActionPreference = 'Stop'
$ComposeDir = Split-Path -Parent $PSScriptRoot
Set-Location $ComposeDir

$EnvFile = Join-Path $ComposeDir '.env'
$args = @('compose')
if (Test-Path $EnvFile) { $args += @('--env-file', $EnvFile) }
$args += @('down')

Write-Host "Stopping Graph RAG stack..."
docker @args
if ($LASTEXITCODE -ne 0) { throw "docker compose down failed (exit $LASTEXITCODE)" }
Write-Host "Stopped. Named volumes retained (neo4j_data, ollama_data)."
