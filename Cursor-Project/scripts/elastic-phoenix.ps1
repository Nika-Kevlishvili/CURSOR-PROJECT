# Forwards to elastic-phoenix.py. Same arguments on Windows PowerShell and pwsh.
# Example:
#   pwsh -NoProfile -File Cursor-Project/scripts/elastic-phoenix.ps1 search-logs --environment test --level ERROR --days-back 1

$ErrorActionPreference = "Stop"
$python = $null
foreach ($name in @("python3", "python")) {
    $cmd = Get-Command $name -ErrorAction SilentlyContinue
    if ($cmd) { $python = $cmd.Source; break }
}
if (-not $python) {
    Write-Error "python3 is required to run elastic-phoenix.py"
    exit 1
}
$script = Join-Path $PSScriptRoot "elastic-phoenix.py"
& $python $script @args
exit $LASTEXITCODE
