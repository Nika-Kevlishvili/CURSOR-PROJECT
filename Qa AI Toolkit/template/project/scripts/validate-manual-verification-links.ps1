<#
.SYNOPSIS
  Static checks for test run summary + portal links (no Playwright run required).

.DESCRIPTION
  - test-run-summary.fixtures.ts + manual-verification-links.fixtures.ts exist
  - cursor-test.fixtures.ts exposes TestRunSummary + afterEach API log
  - Playwright instructions document finalizeTestRunSummary pattern
  - energo-ts-test + playwright-test-validator SKILLs aligned

  Usage: powershell -ExecutionPolicy Bypass -File "Cursor-Project/scripts/validate-manual-verification-links.ps1"
#>
$ErrorActionPreference = 'Stop'
$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot '..\..')
$failures = @()
$warnings = @()

function Add-Failure([string]$Msg) { $script:failures += $Msg }
function Add-Warning([string]$Msg) { $script:warnings += $Msg }

$helperPath = Join-Path $RepoRoot 'Cursor-Project\EnergoTS\tests\cursor\shared\manual-verification-links.fixtures.ts'
$summaryPath = Join-Path $RepoRoot 'Cursor-Project\EnergoTS\tests\cursor\shared\test-run-summary.fixtures.ts'
$cursorFixturesPath = Join-Path $RepoRoot 'Cursor-Project\EnergoTS\tests\cursor\cursor-test.fixtures.ts'

if (-not (Test-Path -LiteralPath $summaryPath)) {
    Add-Failure 'Missing: Cursor-Project/EnergoTS/tests/cursor/shared/test-run-summary.fixtures.ts'
} else {
    $summary = Get-Content -LiteralPath $summaryPath -Raw
    foreach ($export in @('TestRunSummaryCollector', 'finalizeTestRunSummary')) {
        if ($summary -notmatch "export (function|class) $export") {
            Add-Failure "test-run-summary.fixtures missing export: $export"
        }
    }
    if ($summary -notmatch 'expectedResult' -or $summary -notmatch 'actualResult') {
        Add-Failure 'test-run-summary.fixtures must use expectedResult / actualResult labels'
    }
}

if (-not (Test-Path -LiteralPath $helperPath)) {
    Add-Failure 'Missing: Cursor-Project/EnergoTS/tests/cursor/shared/manual-verification-links.fixtures.ts'
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
    if ($helper -notmatch 'finalizeTestRunSummary') {
        Add-Failure 'manual-verification-links.fixtures must re-export finalizeTestRunSummary'
    }
}

if (-not (Test-Path -LiteralPath $cursorFixturesPath)) {
    Add-Failure 'Missing: Cursor-Project/EnergoTS/tests/cursor/cursor-test.fixtures.ts'
} else {
    $cursorFixtures = Get-Content -LiteralPath $cursorFixturesPath -Raw
    if ($cursorFixtures -notmatch "from '\.\./\.\./fixtures/baseFixture'") {
        Add-Failure 'cursor-test.fixtures must re-export from baseFixture'
    }
    if ($cursorFixtures -notmatch 'test\.afterEach') {
        Add-Failure 'cursor-test.fixtures must register afterEach for API responses'
    }
    if ($cursorFixtures -notmatch 'TestRunSummary') {
        Add-Failure 'cursor-test.fixtures must expose TestRunSummary fixture'
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
    if ($raw -notmatch 'finalizeTestRunSummary') {
        Add-Failure "$name must document finalizeTestRunSummary"
    }
    if ($raw -notmatch 'TestRunSummary') {
        Add-Failure "$name must document TestRunSummary fixture"
    }
    if ($name -eq 'test-writing-rules.instructions.md' -and $raw -notmatch 'cursor-test\.fixtures') {
        Add-Failure "$name must document cursor-test.fixtures for new cursor specs"
    }
    if ($raw -match 'test\.info\(\)\.attach[\s\S]{0,200}reportGenerator\.setLinksToResponses') {
        Add-Failure "$name still contains legacy reportGenerator attach pattern"
    }
}

$energoSkill = Join-Path $RepoRoot '.cursor\skills\energo-ts-test\SKILL.md'
if (-not (Test-Path -LiteralPath $energoSkill)) {
    Add-Failure 'Missing .cursor/skills/energo-ts-test/SKILL.md'
} elseif ((Get-Content -LiteralPath $energoSkill -Raw) -notmatch 'finalizeTestRunSummary') {
    Add-Failure 'energo-ts-test SKILL must mandate finalizeTestRunSummary'
} elseif ((Get-Content -LiteralPath $energoSkill -Raw) -notmatch 'TestRunSummary') {
    Add-Failure 'energo-ts-test SKILL must mandate TestRunSummary fixture'
} elseif ((Get-Content -LiteralPath $energoSkill -Raw) -notmatch 'cursor-test\.fixtures') {
    Add-Failure 'energo-ts-test SKILL must mandate cursor-test.fixtures for new cursor specs'
}

$validatorSkill = Join-Path $RepoRoot '.cursor\skills\playwright-test-validator\SKILL.md'
if (-not (Test-Path -LiteralPath $validatorSkill)) {
    Add-Failure 'Missing playwright-test-validator SKILL.md'
} elseif ((Get-Content -LiteralPath $validatorSkill -Raw) -notmatch 'finalizeTestRunSummary') {
    Add-Failure 'playwright-test-validator SKILL must check finalizeTestRunSummary'
} elseif ((Get-Content -LiteralPath $validatorSkill -Raw) -notmatch 'TestRunSummary') {
    Add-Failure 'playwright-test-validator SKILL must check TestRunSummary'
} elseif ((Get-Content -LiteralPath $validatorSkill -Raw) -notmatch 'recordCheck') {
    Add-Failure 'playwright-test-validator SKILL must check recordCheck'
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
