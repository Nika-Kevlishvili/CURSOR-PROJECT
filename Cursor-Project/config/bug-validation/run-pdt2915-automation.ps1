<#
.SYNOPSIS
  PDT-2915 automation entry point (Rule 32 evidence + TO-BE Playwright pointer).

.PARAMETER SkipDb
  Skip psql DB evidence (use when only MCP or manual SQL).

.PARAMETER SkipSlack
  Skip Slack post.

.PARAMETER BillingRunId
  Prod correction run id for DB checks (default 3400).

.PARAMETER ReportMarkdownPath
  Optional bug validation report .md for Slack.
#>
param(
    [int]$BillingRunId = 3400,
    [switch]$SkipDb,
    [switch]$SkipSlack,
    [string]$ReportMarkdownPath = ''
)

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path

Write-Host 'PDT-2915 automation' -ForegroundColor Green
Write-Host "  SQL checklist: config/bug-validation/queries/PDT-2915-invoice-correction-deleted-bbp.sql"
Write-Host "  Pattern:       config/bug-validation/patterns/pdt-2915-invoice-correction-deleted-bbp.json"
Write-Host '  Playwright TO-BE: EnergoTS/tests/cursor/PDT-2915-invoice-correction-deleted-bbp.spec.ts'
Write-Host ''

if (-not $SkipDb) {
    $dbScript = Join-Path $PSScriptRoot 'run-pdt2915-db-evidence.ps1'
    & $dbScript -BillingRunId $BillingRunId
    if ($LASTEXITCODE -eq 2) {
        Write-Host 'DB: skipped (set PDT2915_PG_* env) or use PostgreSQLProd MCP + SQL file' -ForegroundColor Yellow
    }
}

if (-not $SkipSlack -and $ReportMarkdownPath) {
    $slackScript = Join-Path $root 'config\slack\send-bug-validation-to-slack.ps1'
    & $slackScript -ReportMarkdownPath $ReportMarkdownPath -IssueKey 'PDT-2915'
} elseif (-not $SkipSlack) {
    Write-Host 'Slack: pass -ReportMarkdownPath or use Slack MCP slack_send_message -> C0AUEEDVCEL' -ForegroundColor Yellow
}

Write-Host ''
Write-Host 'Playwright (Dev, after fix should PASS):' -ForegroundColor Cyan
Write-Host '  cd Cursor-Project/EnergoTS'
Write-Host '  npx playwright test tests/cursor/PDT-2915-invoice-correction-deleted-bbp.spec.ts --grep PDT-2915 --project=main'
