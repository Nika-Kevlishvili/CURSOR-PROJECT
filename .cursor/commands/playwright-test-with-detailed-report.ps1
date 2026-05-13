# playwright-test-with-detailed-report.ps1
# Runs Playwright from EnergoTS, then generates Cursor-Project/EnergoTS/playwright-report-detailed.md next to playwright-report.json (Rule DPR.0: user may prefer manual steps — see playwright_detailed_reporting.mdc).
#
# Usage:
#   powershell -ExecutionPolicy Bypass -File ".cursor/commands/playwright-test-with-detailed-report.ps1"
#   powershell -ExecutionPolicy Bypass -File ".cursor/commands/playwright-test-with-detailed-report.ps1" "--grep" "REG-123"

param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$PlaywrightArgs
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$workspaceRoot = Split-Path -Parent (Split-Path -Parent $scriptDir)
$cursorProjectPath = Join-Path $workspaceRoot "Cursor-Project"
$energotsPath = Join-Path $cursorProjectPath "EnergoTS"
$detailedGenerator = Join-Path $cursorProjectPath "config\playwright\generate-detailed-report.mjs"

if (-not (Test-Path $energotsPath)) {
    Write-Error "EnergoTS not found at: $energotsPath"
    exit 1
}
if (-not (Test-Path $detailedGenerator)) {
    Write-Error "Detailed report script not found at: $detailedGenerator"
    exit 1
}

Push-Location $energotsPath
$testExit = 0
try {
    if ($null -eq $PlaywrightArgs -or $PlaywrightArgs.Count -eq 0) {
        npx playwright test
    } else {
        npx playwright test @PlaywrightArgs
    }
    $testExit = $LASTEXITCODE
} finally {
    $jsonPath = Join-Path $energotsPath "playwright-report.json"
    if (Test-Path $jsonPath) {
        Write-Host "[playwright-test-with-detailed-report] Generating detailed markdown..." -ForegroundColor Cyan
        node $detailedGenerator
        if ($LASTEXITCODE -ne 0) { Write-Warning "Detailed report generator exited with code $LASTEXITCODE" }
    } else {
        Write-Warning "[playwright-test-with-detailed-report] No playwright-report.json — skip detailed report."
    }
    Pop-Location
}

exit $testExit
