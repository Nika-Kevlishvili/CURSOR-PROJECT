<#
.SYNOPSIS
  Reconciliation / consistency checks for .cursor orchestration (Phase 3+).

.DESCRIPTION
  Complements validate-cursor-rules.ps1 with invariant and cross-file checks from
  CURSOR_OPERATING_MODEL.md and post-audit remediation.

  Usage: powershell -ExecutionPolicy Bypass -File "Cursor-Project/scripts/validate-cursor-consistency.ps1"
#>
$ErrorActionPreference = 'Stop'
$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot '..\..')
$CursorRoot = Join-Path $RepoRoot '.cursor'
$RulesRoot = Join-Path $CursorRoot 'rules'

$failures = @()
$warnings = @()

function Add-Failure([string]$Msg) { $script:failures += $Msg }
function Add-Warning([string]$Msg) { $script:warnings += $Msg }

# --- Six core alwaysApply rules (Phase 3 target)
$expectedAlwaysApply = @(
    'main\core_rules.mdc',
    'safety\safety_rules.mdc',
    'main\clarification_and_confidence.mdc',
    'main\evidence_only_project_answers.mdc',
    'workflows\workflow_rules.mdc',
    'agents\agent_rules.mdc'
)

$actualAlwaysApply = @()
Get-ChildItem -Path $RulesRoot -Recurse -Filter '*.mdc' -File | ForEach-Object {
    $raw = Get-Content -LiteralPath $_.FullName -Raw
    if ($raw -match '(?m)^alwaysApply:\s*true\s*$') {
        $rel = $_.FullName.Replace($RulesRoot + '\', '').Replace($RulesRoot + '/', '')
        $actualAlwaysApply += $rel -replace '/', '\'
    }
}

$expectedNorm = $expectedAlwaysApply | Sort-Object
$actualNorm = $actualAlwaysApply | Sort-Object

foreach ($extra in ($actualNorm | Where-Object { $_ -notin $expectedNorm })) {
    Add-Failure "Unexpected alwaysApply:true rule (target: 6 core only): rules\$extra"
}
foreach ($missing in ($expectedNorm | Where-Object { $_ -notin $actualNorm })) {
    Add-Failure "Missing expected alwaysApply:true rule: rules\$missing"
}

# --- Scoped rules must have globs
Get-ChildItem -Path $RulesRoot -Recurse -Filter '*.mdc' | ForEach-Object {
    $raw = Get-Content -LiteralPath $_.FullName -Raw
    if ($raw -match 'alwaysApply:\s*false') {
        if ($raw -notmatch 'globs:\s*\r?\n\s*-') {
            Add-Warning "alwaysApply:false without globs: $($_.FullName.Replace($RepoRoot, '').TrimStart('\'))"
        }
    }
}

# --- NPR removed
$nprPath = Join-Path $RulesRoot 'workspace\no_auto_playwright_report_files.mdc'
if (Test-Path -LiteralPath $nprPath) {
    Add-Failure "Removed NPR rule still present: .cursor/rules/workspace/no_auto_playwright_report_files.mdc"
}

# --- EnergoTS hooks in hooks.json
$hooksJsonPath = Join-Path $CursorRoot 'hooks.json'
if (-not (Test-Path -LiteralPath $hooksJsonPath)) {
    Add-Failure 'Missing .cursor/hooks.json'
} else {
    $hooksRaw = Get-Content -LiteralPath $hooksJsonPath -Raw
    foreach ($script in @('block-energots-branch-requests.ps1', 'block-energots-branch-switch.ps1', 'protect-energots-writes.ps1')) {
        if ($hooksRaw -notmatch [regex]::Escape($script)) {
            Add-Failure "hooks.json missing EnergoTS hook: $script"
        }
    }
}

# --- DRY precondition forbidden in .cursor orchestration
$dryOrchestrationPattern = 'DRY preconditions|Reuse model — DRY|precondition DRY rules'
Get-ChildItem -Path $CursorRoot -Recurse -Include '*.md', '*.mdc' -File | ForEach-Object {
    $t = Get-Content -LiteralPath $_.FullName -Raw
    if ([regex]::IsMatch($t, $dryOrchestrationPattern)) {
        Add-Failure "DRY precondition text in orchestration (use STANDALONE): $($_.FullName.Replace($RepoRoot, '').TrimStart('\'))"
    }
}

# --- Cross-file: Backend-only HandsOff consistency
$crossFileChecks = @(
    @{
        Path = '.cursor\rules\workflows\handsoff_playwright_report.mdc'
        Forbidden = @(
            'verify that both `.md` files exist',
            'verify both `.md` files exist',
            'save as **two separate files**'
        )
        Required = @('TC-FRONTEND', 'Frontend scope', 'Backend-only', 'when Frontend scope', 'when that file exists')
    }
    @{
        Path = '.cursor\agents\hands-off.md'
        Forbidden = @('two separate files', 'Verify both `.md` files')
        Required = @('TC-FRONTEND', 'Frontend scope', 'Backend file **always**', 'Backend file always')
    }
    @{
        Path = '.cursor\agents\test-case-quality-validator.md'
        Forbidden = @('shared by both files', 'Read both TC files')
        Required = @('optional', 'Backend-only', 'file exists', 'Frontend file is **missing**')
    }
    @{
        Path = '.cursor\agents\playwright-test-validator.md'
        Forbidden = @('Both `.md` files (`test_cases/Backend')
        Required = @('only when it exists', 'Backend-only', 'when no Frontend file')
    }
    @{
        Path = '.cursor\agents\energo-ts-test.md'
        Forbidden = @('Read** both .md files')
        Required = @('when that file exists', 'when present', 'Frontend when')
    }
    @{
        Path = 'Cursor-Project\docs\test_case_quality_rubric.md'
        Forbidden = @('| **4. Delta clarity**', 'precondition DRY rules', 'Every negative TC must have a Delta')
        Required = @('Scenario differentiation', 'TC-STANDALONE', 'Legacy read-only')
    }
    @{
        Path = 'Cursor-Project\config\template\Test_case_template.md'
        Forbidden = @(
            'Each topic produces **two separate files**',
            'Apply Test data steps 1–11',
            'Delta: confirm invoice status'
        )
        Required = @('TC-FRONTEND-ASK.0', 'STANDALONE', 'FORBIDDEN in new files')
    }
    @{
        Path = 'Cursor-Project\test_cases\README.md'
        Forbidden = @('Each topic produces two files with the same name')
        Required = @('Legacy topics', 'STANDALONE', 'TC-FRONTEND-ASK.0')
    }
)

foreach ($check in $crossFileChecks) {
    $full = Join-Path $RepoRoot $check.Path
    if (-not (Test-Path -LiteralPath $full)) {
        Add-Failure "Cross-file check target missing: $($check.Path)"
        continue
    }
    $content = Get-Content -LiteralPath $full -Raw
    foreach ($bad in $check.Forbidden) {
        if ($content -like "*$bad*") {
            Add-Failure "Forbidden pattern in $($check.Path): $bad"
        }
    }
    $reqHit = $false
    foreach ($req in $check.Required) {
        if ($content -like "*$req*") { $reqHit = $true; break }
    }
    if (-not $reqHit) {
        Add-Failure "Missing required Backend-only/STANDALONE marker in $($check.Path) (expected one of: $($check.Required -join ', '))"
    }
}

# --- TC quality skill: 10-axis
$tcQualitySkill = Join-Path $CursorRoot 'skills\test-case-quality-validator\SKILL.md'
if (Test-Path -LiteralPath $tcQualitySkill) {
    $tq = Get-Content -LiteralPath $tcQualitySkill -Raw
    if ($tq -match '6 axes|8/12|6-axis') {
        Add-Failure 'test-case-quality-validator SKILL still references 6-axis / 8/12 rubric'
    }
    if ($tq -notmatch '10-axis|10 axes|80/100') {
        Add-Warning 'test-case-quality-validator SKILL may not mention 10-axis / 80 threshold'
    }
}

# --- workflow_rules slim must reference Rule 32 SKILL extras
$wfPath = Join-Path $RulesRoot 'workflows\workflow_rules.mdc'
if (Test-Path -LiteralPath $wfPath) {
    $wf = Get-Content -LiteralPath $wfPath -Raw
    if ($wf -notmatch 'phoenix-bug-validation/SKILL') {
        Add-Failure 'workflow_rules.mdc missing phoenix-bug-validation SKILL reference for Rule 32'
    }
    if ($wf -notmatch 'Phase 2 exclusion|Diagrams') {
        Add-Warning 'workflow_rules.mdc may omit Rule 32 diagram/Phase-2 hints (recommended one-liner)'
    }
}

# --- Agent/skill coverage
$agentsDir = Join-Path $CursorRoot 'agents'
$skillsDir = Join-Path $CursorRoot 'skills'
$skillAliases = @{
    'bug-validator' = 'phoenix-bug-validation'
    'database-query' = 'phoenix-database'
    'jira-bug' = 'jira-bug-template'
}
$noSkillOk = @('hands-off', 'phoenix-qa', 'report-generator', 'shell', 'test-runner', 'environment-access', 'postman-collection')

Get-ChildItem -Path $agentsDir -Filter '*.md' -File | Where-Object { $_.Name -ne 'README.md' } | ForEach-Object {
    $baseName = $_.BaseName
    $skillName = if ($skillAliases.ContainsKey($baseName)) { $skillAliases[$baseName] } else { $baseName }
    $skillPath = Join-Path $skillsDir "$skillName\SKILL.md"
    if (-not (Test-Path -LiteralPath $skillPath) -and $baseName -notin $noSkillOk) {
        Add-Warning "Agent without matching skill: .cursor/agents/$($_.Name) (expected skills/$skillName/SKILL.md)"
    }
}

Write-Host "Repo root: $RepoRoot"
Write-Host ""

if ($warnings.Count -gt 0) {
    Write-Host 'WARNINGS:' -ForegroundColor Yellow
    $warnings | ForEach-Object { Write-Host "  $_" -ForegroundColor Yellow }
    Write-Host ''
}

if ($failures.Count -gt 0) {
    Write-Host 'FAILURES:' -ForegroundColor Red
    $failures | ForEach-Object { Write-Host "  $_" -ForegroundColor Red }
    exit 1
}

Write-Host "validate-cursor-consistency: OK ($($actualNorm.Count) alwaysApply core; cross-file invariants pass)." -ForegroundColor Green
exit 0
