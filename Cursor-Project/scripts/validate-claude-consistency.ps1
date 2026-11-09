<#
.SYNOPSIS
  Reconciliation / consistency checks for .claude orchestration (Phase 3+).

.DESCRIPTION
  Complements validate-claude-rules.ps1 with invariant and cross-file checks from
  CURSOR_OPERATING_MODEL.md and post-audit remediation.

  Usage: pwsh -NoProfile -File "Cursor-Project/scripts/validate-claude-consistency.ps1"
#>
$ErrorActionPreference = 'Stop'
$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot '..\..')
$RepoRoot = $RepoRoot.Path
$CursorRoot = Join-Path $RepoRoot '.claude'   # variable name kept from the Cursor-era script
$RulesRoot = Join-Path $CursorRoot 'rules'

$failures = @()
$warnings = @()

function Add-Failure([string]$Msg) { $script:failures += $Msg }
function Add-Warning([string]$Msg) { $script:warnings += $Msg }

# --- Always-loaded rules: a rule without `paths:` frontmatter loads in every session.
#     Seven core rules + atlassian_zero_cost (ATLASSIAN.0, always-on in the Cursor setup too).
$expectedAlwaysApply = @(
    'main\core_rules.md',
    'safety\safety_rules.md',
    'main\clarification_and_confidence.md',
    'main\evidence_only_project_answers.md',
    'main\senior_qa_product_quality.md',
    'workflows\workflow_rules.md',
    'agents\agent_rules.md',
    'integrations\atlassian_zero_cost.md'
)

$actualAlwaysApply = @()
Get-ChildItem -Path $RulesRoot -Recurse -Filter '*.md' -File | ForEach-Object {
    $raw = Get-Content -LiteralPath $_.FullName -Raw
    $fm = [regex]::Match($raw, '(?s)\A﻿?---\r?\n(.*?)\r?\n---')
    $hasPaths = $fm.Success -and ($fm.Groups[1].Value -match '(?m)^paths:\s*\r?\n\s*-')
    if (-not $hasPaths) {
        $actualAlwaysApply += ([System.IO.Path]::GetRelativePath($RulesRoot, $_.FullName) -replace '/', '\')
    }
}

$expectedNorm = $expectedAlwaysApply | Sort-Object
$actualNorm = $actualAlwaysApply | Sort-Object

foreach ($extra in ($actualNorm | Where-Object { $_ -notin $expectedNorm })) {
    Add-Failure "Unexpected always-loaded rule (no paths: frontmatter; target: $($expectedNorm.Count) core only): rules\$extra"
}
foreach ($missing in ($expectedNorm | Where-Object { $_ -notin $actualNorm })) {
    Add-Failure "Expected always-loaded rule is missing or scoped with paths: rules\$missing"
}

# --- NPR removed
$nprPath = Join-Path $RulesRoot 'workspace\no_auto_playwright_report_files.md'
if (Test-Path -LiteralPath $nprPath) {
    Add-Failure "Removed NPR rule still present: .claude/rules/workspace/no_auto_playwright_report_files.md"
}

# --- EnergoTS hooks registered in the Claude Code hook dispatcher
$settingsPath = Join-Path $CursorRoot 'settings.json'
$dispatcherPath = Join-Path $CursorRoot 'hooks\claude-hook.ps1'
if (-not (Test-Path -LiteralPath $settingsPath)) {
    Add-Failure 'Missing .claude/settings.json'
} elseif ((Get-Content -LiteralPath $settingsPath -Raw) -notmatch 'claude-hook\.ps1') {
    Add-Failure '.claude/settings.json does not run hooks/claude-hook.ps1'
}
if (-not (Test-Path -LiteralPath $dispatcherPath)) {
    Add-Failure 'Missing .claude/hooks/claude-hook.ps1'
} else {
    $hooksRaw = Get-Content -LiteralPath $dispatcherPath -Raw
    foreach ($script in @('block-energots-branch-requests.ps1', 'block-energots-branch-switch.ps1', 'protect-energots-writes.ps1')) {
        if ($hooksRaw -notmatch ("(?m)^\s*@\{\s*Script\s*=\s*'" + [regex]::Escape($script) + "'")) {
            Add-Failure "claude-hook.ps1 registry missing EnergoTS hook: $script"
        }
    }
}

# --- DRY precondition forbidden in .claude orchestration
$dryOrchestrationPattern = 'DRY preconditions|Reuse model - DRY|precondition DRY rules'
Get-ChildItem -Path $CursorRoot -Recurse -Include '*.md' -File | ForEach-Object {
    $t = Get-Content -LiteralPath $_.FullName -Raw
    if ([regex]::IsMatch($t, $dryOrchestrationPattern)) {
        Add-Failure "DRY precondition text in orchestration (use STANDALONE): $($_.FullName.Replace($RepoRoot, '').TrimStart('\', '/'))"
    }
}

# --- Rule QA.0: forbid legacy "code wins" authority in orchestration (.claude only)
$qaForbiddenPatterns = @(
    'code wins',
    'code still wins',
    'Code ALWAYS wins',
    'code > Confluence',
    'codebase > Confluence',
    'Prefer codebase over Confluence when they conflict'
)
Get-ChildItem -Path $CursorRoot -Recurse -Include '*.md' -File | ForEach-Object {
    $t = Get-Content -LiteralPath $_.FullName -Raw
    foreach ($pat in $qaForbiddenPatterns) {
        if ($t -like "*$pat*") {
            Add-Failure "Legacy code-wins authority in orchestration (use dual-track + Finding): $($_.FullName.Replace($RepoRoot, '').TrimStart('\', '/')) - pattern: $pat"
        }
    }
}

# --- Rule QA.0: required Senior QA artifacts
$qaRequiredFiles = @(
    '.claude\rules\main\senior_qa_product_quality.md',
    '.claude\skills\senior-qa-analysis\SKILL.md',
    '.claude\agents\senior-qa.md'
)
foreach ($rel in $qaRequiredFiles) {
    $full = Join-Path $RepoRoot $rel
    if (-not (Test-Path -LiteralPath $full)) {
        Add-Failure "Missing Senior QA artifact: $rel"
    }
}

$qaCrossRefs = @(
    @{ Path = '.claude\rules\main\core_rules.md'; Required = @('Rule 0.10', 'senior_qa_product_quality', 'Finding') }
    @{ Path = '.claude\skills\phoenix-commands\SKILL.md'; Required = @('senior-qa', 'senior-qa-analysis') }
    @{ Path = 'Cursor-Project\docs\AGENT_SUBAGENT_MAP.md'; Required = @('senior-qa.md') }
    @{ Path = '.claude\rules\main\clarification_and_confidence.md'; Required = @('Finding (Rule QA.2') }
)
foreach ($ref in $qaCrossRefs) {
    $full = Join-Path $RepoRoot $ref.Path
    if (-not (Test-Path -LiteralPath $full)) {
        Add-Failure "QA cross-ref target missing: $($ref.Path)"
        continue
    }
    $content = Get-Content -LiteralPath $full -Raw
    $hit = $false
    foreach ($req in $ref.Required) {
        if ($content -like "*$req*") { $hit = $true; break }
    }
    if (-not $hit) {
        Add-Failure "QA cross-ref missing in $($ref.Path) (expected one of: $($ref.Required -join ', '))"
    }
}

# --- clarification quick ref must not use legacy Conflicting evidence -10 alone
$confPath = Join-Path $RulesRoot 'main\clarification_and_confidence.md'
if (Test-Path -LiteralPath $confPath) {
    $conf = Get-Content -LiteralPath $confPath -Raw
    if ($conf -match 'Conflicting evidence \| - \| -10' -and $conf -notmatch 'Finding \(Rule QA\.2') {
        Add-Failure 'clarification_and_confidence.md still has legacy Conflicting evidence -10 without QA Finding factors'
    }
}

# --- Cross-file: Backend-only TC consistency
$crossFileChecks = @(
    @{
        Path = '.claude\agents\test-case-quality-validator.md'
        Forbidden = @('shared by both files', 'Read both TC files')
        Required = @('optional', 'Backend-only', 'file exists', 'Frontend file is **missing**')
    }
    @{
        Path = '.claude\agents\playwright-test-validator.md'
        Forbidden = @('Both `.md` files (`test_cases/Backend')
        Required = @('only when it exists', 'Backend-only', 'when no Frontend file')
    }
    @{
        Path = '.claude\agents\energo-ts-test.md'
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
            'Apply Test data steps 1-11',
            'Delta: confirm invoice status'
        )
        Required = @('TC-FRONTEND-ASK.0', 'STANDALONE', 'FORBIDDEN in new files')
    }
    @{
        Path = 'Cursor-Project\test_cases\README.md'
        Forbidden = @('Each topic produces two files with the same name')
        Required = @('Legacy topics', 'STANDALONE', 'TC-FRONTEND-ASK.0', 'Backend+Frontend was requested')
        Optional = $true   # test_cases/ is git-ignored: absent on CI and fresh clones
    }
)

foreach ($check in $crossFileChecks) {
    $full = Join-Path $RepoRoot $check.Path
    if (-not (Test-Path -LiteralPath $full)) {
        if ($check.Optional) { Add-Warning "Cross-file check skipped (git-ignored file not present): $($check.Path)" }
        else { Add-Failure "Cross-file check target missing: $($check.Path)" }
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
$wfPath = Join-Path $RulesRoot 'workflows\workflow_rules.md'
if (Test-Path -LiteralPath $wfPath) {
    $wf = Get-Content -LiteralPath $wfPath -Raw
    if ($wf -notmatch 'phoenix-bug-validation/SKILL') {
        Add-Failure 'workflow_rules.md missing phoenix-bug-validation SKILL reference for Rule 32'
    }
    if ($wf -notmatch 'Phase 2 exclusion|Diagrams') {
        Add-Warning 'workflow_rules.md may omit Rule 32 diagram/Phase-2 hints (recommended one-liner)'
    }
}

# --- evidence_only slim (Jira detail in jira-evidence SKILL)
$evidencePath = Join-Path $RulesRoot 'main\evidence_only_project_answers.md'
$jiraSkillPath = Join-Path $CursorRoot 'skills\jira-evidence\SKILL.md'
if (-not (Test-Path -LiteralPath $jiraSkillPath)) {
    Add-Failure 'Missing jira-evidence SKILL: .claude/skills/jira-evidence/SKILL.md'
}
if (Test-Path -LiteralPath $evidencePath) {
    $ev = Get-Content -LiteralPath $evidencePath -Raw
    $evLines = ($ev -split '\r?\n').Count
    if ($evLines -gt 110) {
        Add-Warning "evidence_only_project_answers.md has $evLines lines (target at most 110 after Jira slim)"
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

# --- EnergoTS hook: writes under EnergoTS are allowed (Rule 0.8)
$protectEnergo = Join-Path $CursorRoot 'hooks\protect-energots-writes.ps1'
if (Test-Path -LiteralPath $protectEnergo) {
    $eg = Get-Content -LiteralPath $protectEnergo -Raw
    if ($eg -match 'outside tests/' -and $eg -match 'permission\s*=\s*"deny"') {
        Add-Warning 'protect-energots-writes.ps1 still denies EnergoTS writes outside tests/ (Rule 0.8 allows them)'
    }
}

# --- Agent/skill coverage
$agentsDir = Join-Path $CursorRoot 'agents'
$skillsDir = Join-Path $CursorRoot 'skills'
$skillAliases = @{
    'bug-validator' = 'phoenix-bug-validation'
    'database-query' = 'phoenix-database'
    'jira-bug' = 'jira-bug-template'
    'senior-qa' = 'senior-qa-analysis'
}
$noSkillOk = @('phoenix-qa', 'report-generator', 'shell', 'test-runner', 'environment-access', 'postman-collection', 'jira-bug')

Get-ChildItem -Path $agentsDir -Filter '*.md' -File | ForEach-Object {
    $baseName = $_.BaseName
    $skillName = if ($skillAliases.ContainsKey($baseName)) { $skillAliases[$baseName] } else { $baseName }
    $skillPath = Join-Path $skillsDir "$skillName\SKILL.md"
    if (-not (Test-Path -LiteralPath $skillPath) -and $baseName -notin $noSkillOk) {
        Add-Warning "Agent without matching skill: .claude/agents/$($_.Name) (expected skills/$skillName/SKILL.md)"
    }
}

# --- CRITICAL to BLOCK/MUST tiering (Rule 0.9)
$legacyCritical = 0
Get-ChildItem -Path $RulesRoot -Recurse -Filter '*.md' -File | ForEach-Object {
    $raw = Get-Content -LiteralPath $_.FullName -Raw
    $hits = ([regex]::Matches($raw, 'Violation is a CRITICAL|CRITICAL SYSTEM ERROR')).Count
    if ($hits -gt 0) {
        $legacyCritical += $hits
        Add-Warning ('Legacy CRITICAL in {0}: {1} occurrence(s) - migrate to BLOCK/MUST/SHOULD per Rule 0.9' -f $_.FullName.Replace($RepoRoot, '').TrimStart('\', '/'), $hits)
    }
}
if ($legacyCritical -gt 5) {
    Add-Failure "Too many legacy CRITICAL occurrences in rules: $legacyCritical (target at most 5 for Rule 0.9 definitions only)"
}

# --- phoenix_branch_switching.md should be slim (detail in SKILL)
$phoenixSwitch = Join-Path $RulesRoot 'integrations\phoenix_branch_switching.md'
if (Test-Path -LiteralPath $phoenixSwitch) {
    $psLines = ((Get-Content -LiteralPath $phoenixSwitch -Raw) -split '\r?\n').Count
    if ($psLines -gt 55) {
        Add-Warning ('phoenix_branch_switching.md has {0} lines (target at most 55, detail in phoenix-branch-switching SKILL)' -f $psLines)
    }
    $ps = Get-Content -LiteralPath $phoenixSwitch -Raw
    if ($ps -notmatch 'phoenix-branch-switching/SKILL') {
        Add-Failure 'phoenix_branch_switching.md missing pointer to phoenix-branch-switching SKILL'
    }
}

# --- Confluence hook: fail-secure on error
$blockConfluence = Join-Path $CursorRoot 'hooks\block-confluence-write.ps1'
if (Test-Path -LiteralPath $blockConfluence) {
    $bc = Get-Content -LiteralPath $blockConfluence -Raw
    if ([regex]::IsMatch($bc, '(?s)catch\s*\{.*?permission\s*=\s*"allow"')) {
        Add-Failure 'block-confluence-write.ps1 catch block must deny (fail-secure), not allow'
    }
    if (-not [regex]::IsMatch($bc, '(?s)catch\s*\{.*?permission\s*=\s*"deny"')) {
        Add-Failure 'block-confluence-write.ps1 catch block must set permission deny (fail-secure)'
    }
}

# --- playwright_test_constraints.md (Rule 40)
$playwrightConstraints = Join-Path $RulesRoot 'workflows\playwright_test_constraints.md'
if (-not (Test-Path -LiteralPath $playwrightConstraints)) {
    Add-Failure 'Missing playwright_test_constraints.md (Rule 40 canonical home)'
} else {
    $pc = Get-Content -LiteralPath $playwrightConstraints -Raw
    if ($pc -notmatch 'Rule 40') {
        Add-Failure 'playwright_test_constraints.md missing Rule 40'
    }
    if ($pc -notmatch 'beforeAll') {
        Add-Failure 'playwright_test_constraints.md missing beforeAll ban'
    }
}

# --- handsoff_playwright_report.md removed (Rule 37 retired)
$handsoffReport = Join-Path $RulesRoot 'workflows\handsoff_playwright_report.md'
if (Test-Path -LiteralPath $handsoffReport) {
    Add-Failure 'handsoff_playwright_report.md should be removed (HandsOff Rule 37 retired)'
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
        Add-Warning ('Agent {0}.md has {1} lines (P1b target at most {2}, procedure belongs in SKILL)' -f $agentName, $lineCount, $thinAgentMaxLines)
    }
    if ($agentRaw -notmatch 'Procedure \(HOW\):') {
        Add-Warning "Agent $agentName.md missing 'Procedure (HOW):' pointer to SKILL"
    }
}

Write-Host "Repo root: $RepoRoot"
# --- Manual verification portal links (static, no Playwright run)
$mvScript = Join-Path $RepoRoot 'Cursor-Project\scripts\validate-manual-verification-links.ps1'
if (Test-Path -LiteralPath $mvScript) {
    & $mvScript
    if ($LASTEXITCODE -ne 0) {
        Add-Failure 'validate-manual-verification-links.ps1 failed (portal links static checks)'
    }
} else {
    Add-Warning 'Missing Cursor-Project/scripts/validate-manual-verification-links.ps1'
}

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

Write-Host ('validate-claude-consistency: OK ({0} always-loaded core, cross-file invariants pass).' -f $actualNorm.Count) -ForegroundColor Green
exit 0
