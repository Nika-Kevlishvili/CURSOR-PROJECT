param(
    [int]$Port = 4173
)

$ErrorActionPreference = 'Stop'
$siteRoot = Split-Path -Parent $PSScriptRoot
$url = "http://127.0.0.1:$Port/"

$python = Get-Command python -ErrorAction SilentlyContinue
if (-not $python) { $python = Get-Command py -ErrorAction SilentlyContinue }
if (-not $python) {
    Write-Error 'Python is required to serve the site locally.'
}

Start-Process $url | Out-Null
Write-Host "QA AI plan site: $url"
Write-Host "Stop with Ctrl+C"
& $python.Source -m http.server $Port --directory $siteRoot
