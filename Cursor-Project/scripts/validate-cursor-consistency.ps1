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
        Path = '.cursor\commands\hands-off.md'
        Forbidden = @(
            'may still proceed to Step 5',
            'After the limit, proceed to Step 5'
        )
        Required = @('Step 3.5', 'test-case-quality-validator', 'BLOCK WORKFLOW', 'TC quality')
    }
    @{
        Path = '.cursor\agents\hands-off.md'
        Forbidden = @('may still proceed', 'proceed to run tests and **include validation')
        Required = @('Step 3.5', 'test-case-quality-validator', 'BLOCK', 'TC quality')
    }
    @{
        Path = '.cursor\rules\workflows\handsoff_playwright_report.mdc'
        Forbidden = @(
            'proceed to run tests and **include validation',
            'Step 1.5):** test-case-quality'
        )
        Required = @('Step 3.5', 'BLOCK WORKFLOW', 'test-case-quality-validator')
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

# --- evidence_only slim (Jira detail in jira-evidence SKILL)
$evidencePath = Join-Path $RulesRoot 'main\evidence_only_project_answers.mdc'
$jiraSkillPath = Join-Path $CursorRoot 'skills\jira-evidence\SKILL.md'
if (-not (Test-Path -LiteralPath $jiraSkillPath)) {
    Add-Failure 'Missing jira-evidence SKILL: .cursor/skills/jira-evidence/SKILL.md'
}
if (Test-Path -LiteralPath $evidencePath) {
    $ev = Get-Content -LiteralPath $evidencePath -Raw
    $evLines = ($ev -split '\r?\n').Count
    if ($evLines -gt 110) {
        Add-Warning "evidence_only_project_answers.mdc has $evLines lines (target <=110 after Jira slim)"
    }
    if ($ev -match 'customfield_10103') {
        Add-Failure 'evidence_only still contains Jira custom field table — move to jira-evidence SKILL'
    }
    if ($ev -notmatch 'jira-evidence/SKILL') {
        Add-Failure 'evidence_only missing pointer to jira-evidence SKILL'
    }
}

# --- Phoenix hook: block all files under Phoenix (not extension-only)
$protectPhoenix = Join-Path $CursorRoot 'hooks\protect-phoenix-code.ps1'
if (Test-Path -LiteralPath $protectPhoenix) {
    $ph = Get-Content -LiteralPath $protectPhoenix -Raw
    if ($ph -match '\$isCodeFile' -and $ph -match '\$isPhoenixPath -and \$isCodeFile') {
        Add-Warning 'protect-phoenix-code.ps1 still uses extension-only gate (target: all files under Phoenix/**)'
    }
}

# --- EnergoTS hook: tests/ allowlist
$protectEnergo = Join-Path $CursorRoot 'hooks\protect-energots-writes.ps1'
if (Test-Path -LiteralPath $protectEnergo) {
    $eg = Get-Content -LiteralPath $protectEnergo -Raw
    if ($eg -notmatch '\.spec\.ts' -or $eg -notmatch '\.fixtures\.ts') {
        Add-Warning 'protect-energots-writes.ps1 may not restrict tests/ to spec/fixtures only'
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
$noSkillOk = @('hands-off', 'phoenix-qa', 'report-generator', 'shell', 'test-runner', 'environment-access', 'postman-collection', 'jira-bug')

Get-ChildItem -Path $agentsDir -Filter '*.md' -File | Where-Object { $_.Name -ne 'README.md' } | ForEach-Object {
    $baseName = $_.BaseName
    $skillName = if ($skillAliases.ContainsKey($baseName)) { $skillAliases[$baseName] } else { $baseName }
    $skillPath = Join-Path $skillsDir "$skillName\SKILL.md"
    if (-not (Test-Path -LiteralPath $skillPath) -and $baseName -notin $noSkillOk) {
        Add-Warning "Agent without matching skill: .cursor/agents/$($_.Name) (expected skills/$skillName/SKILL.md)"
    }
}

# --- CRITICAL → BLOCK/MUST tiering (Rule 0.9)
$legacyCritical = 0
Get-ChildItem -Path $RulesRoot -Recurse -Filter '*.mdc' -File | ForEach-Object {
    $raw = Get-Content -LiteralPath $_.FullName -Raw
    $hits = ([regex]::Matches($raw, 'Violation is a CRITICAL|CRITICAL SYSTEM ERROR')).Count
    if ($hits -gt 0) {
        $legacyCritical += $hits
        Add-Warning "Legacy CRITICAL in $($_.FullName.Replace($RepoRoot, '').TrimStart('\')): $hits occurrence(s) — migrate to BLOCK/MUST/SHOULD per Rule 0.9"
    }
}
if ($legacyCritical -gt 5) {
    Add-Failure "Too many legacy CRITICAL occurrences in .mdc rules: $legacyCritical (target <=5 for Rule 0.9 definitions only)"
}

# --- phoenix_branch_switching.mdc should be slim (detail in SKILL)
$phoenixSwitch = Join-Path $RulesRoot 'integrations\phoenix_branch_switching.mdc'
if (Test-Path -LiteralPath $phoenixSwitch) {
    $psLines = ((Get-Content -LiteralPath $phoenixSwitch -Raw) -split '\r?\n').Count
    if ($psLines -gt 55) {
        Add-Warning "phoenix_branch_switching.mdc has $psLines lines (target <=55; detail in phoenix-branch-switching SKILL)"
    }
    $ps = Get-Content -LiteralPath $phoenixSwitch -Raw
    if ($ps -notmatch 'phoenix-branch-switching/SKILL') {
        Add-Failure 'phoenix_branch_switching.mdc missing pointer to phoenix-branch-switching SKILL'
    }
}

# --- Confluence hook: fail-secure on error
$blockConfluence = Join-Path $CursorRoot 'hooks\block-confluence-write.ps1'
if (Test-Path -LiteralPath $blockConfluence) {
    $bc = Get-Content -LiteralPath $blockConfluence -Raw
    if ($bc -match 'catch\s*\{[^}]*permission\s*=\s*"allow"') {
        Add-Failure 'block-confluence-write.ps1 catch block must deny (fail-secure), not allow'
    }
}

# --- handsoff_playwright_report.mdc should be slim (detail in SKILL)
$handsoffReport = Join-Path $RulesRoot 'workflows\handsoff_playwright_report.mdc'
if (Test-Path -LiteralPath $handsoffReport) {
    $hrLines = ((Get-Content -LiteralPath $handsoffReport -Raw) -split '\r?\n').Count
    if ($hrLines -gt 55) {
        Add-Warning "handsoff_playwright_report.mdc has $hrLines lines (target <=55; detail in hands-off-playwright-report SKILL)"
    }
    $hr = Get-Content -LiteralPath $handsoffReport -Raw
    if ($hr -notmatch 'hands-off-playwright-report/SKILL') {
        Add-Failure 'handsoff_playwright_report.mdc missing pointer to hands-off-playwright-report SKILL'
    }
}

# --- Fat agents must be thin I/O contracts (P1b)
$thinAgentMaxLines = 90
$thinAgents = @('bug-validator', 'test-case-generator', 'cross-dependency-finder', 'energo-ts-test', 'environment-resolver', 'playwright-test-validator', 'test-case-quality-validator', 'production-data-reader', 'energo-ts-run')
foreach ($agentName in $thinAgents) {
    $agentPath = Join-Path $agentsDir "$agentName.md"
    if (-not (Test-Path -LiteralPath $agentPath)) { continue }
    $agentRaw = Get-Content -LiteralPath $agentPath -Raw
    $lineCount = ($agentRaw -split '\r?\n').Count
    if ($lineCount -gt $thinAgentMaxLines) {
        Add-Warning "Agent $agentName.md has $lineCount lines (P1b target <= $thinAgentMaxLines — procedure belongs in SKILL)"
    }
    if ($agentRaw -notmatch 'Procedure \(HOW\):') {
        Add-Warning "Agent $agentName.md missing 'Procedure (HOW):' pointer to SKILL"
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
