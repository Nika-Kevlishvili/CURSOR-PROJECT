# Forwards to psql-phoenix.py. Same arguments on Windows PowerShell and pwsh.
# Example:
#   pwsh -NoProfile -File Cursor-Project/scripts/psql-phoenix.ps1 health --environment test

$ErrorActionPreference = "Stop"
$python = $null
foreach ($name in @("python3", "python")) {
    $cmd = Get-Command $name -ErrorAction SilentlyContinue
    if ($cmd) { $python = $cmd.Source; break }
}
if (-not $python) {
    Write-Error "python3 is required to run psql-phoenix.py"
    exit 1
}
$script = Join-Path $PSScriptRoot "psql-phoenix.py"
& $python $script @args
exit $LASTEXITCODE
