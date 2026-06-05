<#
.SYNOPSIS
  Static checks for manual verification portal links (no Playwright run required).

.DESCRIPTION
  - Shared helper file exists under EnergoTS/tests/cursor/shared/
  - Playwright instructions use attachManualVerificationLinks (no legacy JSON-only attach)
  - energo-ts-test SKILL mandates the attach step

  Usage: powershell -ExecutionPolicy Bypass -File "Cursor-Project/scripts/validate-manual-verification-links.ps1"
#>
$ErrorActionPreference = 'Stop'
$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot '..\..')
$failures = @()
$warnings = @()

function Add-Failure([string]$Msg) { $script:failures += $Msg }
function Add-Warning([string]$Msg) { $script:warnings += $Msg }

$helperPath = Join-Path $RepoRoot 'Cursor-Project\EnergoTS\tests\cursor\shared\manual-verification-links.fixtures.ts'
if (-not (Test-Path -LiteralPath $helperPath)) {
    Add-Failure "Missing shared helper: Cursor-Project/EnergoTS/tests/cursor/shared/manual-verification-links.fixtures.ts"
} else {
    $helper = Get-Content -LiteralPath $helperPath -Raw
    foreach ($export in @(
        'attachManualVerificationLinks',
        'buildProcessPreviewLink',
        'buildProductContractTabLinks',
        'formatPlainPortalLinks',
        'buildResponsesSnapshot'
    )) {
        if ($helper -notmatch "export function $export") {
            Add-Failure "Helper missing export: $export"
        }
    }
}

$instructionsRoot = Join-Path $RepoRoot 'Cursor-Project\config\playwright_generation\playwright instructions'
$instructionFiles = @(
    'test-writing-rules.instructions.md',
    'precondition-data-creation.instructions.md',
    'SKILL.md'
)

foreach ($name in $instructionFiles) {
    $path = Join-Path $instructionsRoot $name
    if (-not (Test-Path -LiteralPath $path)) {
        Add-Failure "Missing instruction file: $name"
        continue
    }
    $raw = Get-Content -LiteralPath $path -Raw
    if ($raw -notmatch 'attachManualVerificationLinks') {
        Add-Failure "$name must document attachManualVerificationLinks"
    }
    if ($raw -match 'test\.info\(\)\.attach[\s\S]{0,200}reportGenerator\.setLinksToResponses') {
        Add-Failure "$name still contains legacy reportGenerator attach pattern"
    }
}

$energoSkill = Join-Path $RepoRoot '.cursor\skills\energo-ts-test\SKILL.md'
if (-not (Test-Path -LiteralPath $energoSkill)) {
    Add-Failure 'Missing .cursor/skills/energo-ts-test/SKILL.md'
} elseif ((Get-Content -LiteralPath $energoSkill -Raw) -notmatch 'attachManualVerificationLinks') {
    Add-Failure 'energo-ts-test SKILL must mandate attachManualVerificationLinks'
}

# --- Inline smoke (pure logic, no Playwright / no API)
if (Test-Path -LiteralPath $helperPath) {
    $helper = Get-Content -LiteralPath $helperPath -Raw
    $env:FRONTEND_BASE_URL = 'http://smoke.test:8080/app'
    if ($helper -notmatch 'processes/preview\?id=') {
        Add-Failure 'buildProcessPreviewLink must emit processes/preview URL pattern'
    }
    if ($helper -notmatch 'energy-product-contracts/preview/basic-parameters') {
        Add-Failure 'buildProductContractTabLinks must emit contract preview URL pattern'
    }
    if ($helper -notmatch 'customerLabels|Labels') {
        Add-Failure 'buildResponsesSnapshot must enrich entity labels for manual verification'
    }
    Remove-Item Env:FRONTEND_BASE_URL -ErrorAction SilentlyContinue
}

$validatorSkill = Join-Path $RepoRoot '.cursor\skills\playwright-test-validator\SKILL.md'
if (-not (Test-Path -LiteralPath $validatorSkill)) {
    Add-Warning 'Missing playwright-test-validator SKILL.md'
} elseif ((Get-Content -LiteralPath $validatorSkill -Raw) -notmatch 'attachManualVerificationLinks') {
    Add-Warning 'playwright-test-validator SKILL should check attachManualVerificationLinks'
}

Write-Host "Repo root: $RepoRoot"
if ($warnings.Count) {
    Write-Host ''
    Write-Host 'WARNINGS:'
    $warnings | ForEach-Object { Write-Host "  $_" }
}
if ($failures.Count) {
    Write-Host ''
    Write-Host 'FAILURES:'
    $failures | ForEach-Object { Write-Host "  $_" }
    exit 1
}

Write-Host ''
Write-Host 'validate-manual-verification-links: OK'
exit 0
