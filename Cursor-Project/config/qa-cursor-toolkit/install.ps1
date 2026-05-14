<#
.SYNOPSIS
    Installs the QA Cursor Toolkit into a target project.

.DESCRIPTION
    Copies agents, skills, commands, rules, templates, hooks, and .gitignore
    into the target project's .cursor/ and config/ directories.
    Creates .env from .env.example if not present.

.PARAMETER TargetProject
    Path to the target project root (where .cursor/ will be created/updated).

.PARAMETER SkipEnv
    Skip .env setup (if you want to configure credentials later).

.EXAMPLE
    .\install.ps1 -TargetProject "C:\Users\me\MyProject"
#>

param(
    [Parameter(Mandatory=$true)]
    [string]$TargetProject,

    [switch]$SkipEnv
)

$ErrorActionPreference = "Stop"
$toolkitRoot = Split-Path -Parent $MyInvocation.MyCommand.Path

function Copy-ToolkitFolder {
    param([string]$Source, [string]$Dest)
    if (Test-Path $Source) {
        New-Item -ItemType Directory -Path $Dest -Force | Out-Null
        Copy-Item -Path "$Source\*" -Destination $Dest -Recurse -Force
        Write-Host "  [OK] $Dest" -ForegroundColor Green
    } else {
        Write-Host "  [SKIP] Source not found: $Source" -ForegroundColor Yellow
    }
}

Write-Host "`n=== QA Cursor Toolkit Installer ===" -ForegroundColor Cyan
Write-Host "Toolkit: $toolkitRoot"
Write-Host "Target:  $TargetProject`n"

if (-not (Test-Path $TargetProject)) {
    Write-Host "[ERROR] Target project path does not exist: $TargetProject" -ForegroundColor Red
    exit 1
}

$cursorDir = Join-Path $TargetProject ".cursor"

Write-Host "1. Installing Cursor rules..." -ForegroundColor White
Copy-ToolkitFolder "$toolkitRoot\rules" "$cursorDir\rules"

Write-Host "2. Installing agents..." -ForegroundColor White
$agentsDir = Join-Path $cursorDir "agents"
New-Item -ItemType Directory -Path $agentsDir -Force | Out-Null
Get-ChildItem "$toolkitRoot\agents\*.md" | ForEach-Object {
    Copy-Item $_.FullName -Destination $agentsDir -Force
    Write-Host "  [OK] agents\$($_.Name)" -ForegroundColor Green
}

Write-Host "3. Installing skills..." -ForegroundColor White
Copy-ToolkitFolder "$toolkitRoot\skills" "$cursorDir\skills"

Write-Host "4. Installing commands..." -ForegroundColor White
$commandsDir = Join-Path $cursorDir "commands"
New-Item -ItemType Directory -Path $commandsDir -Force | Out-Null
Get-ChildItem "$toolkitRoot\commands\*.md" | ForEach-Object {
    Copy-Item $_.FullName -Destination $commandsDir -Force
    Write-Host "  [OK] commands\$($_.Name)" -ForegroundColor Green
}

Write-Host "5. Installing hooks..." -ForegroundColor White
$hooksSource = Join-Path $toolkitRoot "hooks"
if (Test-Path $hooksSource) {
    Copy-ToolkitFolder $hooksSource "$cursorDir\hooks"
    $hooksJson = Join-Path $toolkitRoot "hooks.json"
    if (Test-Path $hooksJson) {
        Copy-Item $hooksJson -Destination (Join-Path $cursorDir "hooks.json") -Force
        Write-Host "  [OK] hooks.json" -ForegroundColor Green
    }
}

Write-Host "6. Installing templates..." -ForegroundColor White
$templatesDir = Join-Path $TargetProject "config\templates"
Copy-ToolkitFolder "$toolkitRoot\templates" $templatesDir

Write-Host "7. Installing scripts..." -ForegroundColor White
$scriptsDir = Join-Path $TargetProject "config\scripts"
Copy-ToolkitFolder "$toolkitRoot\scripts" $scriptsDir

Write-Host "8. Setting up test_cases structure..." -ForegroundColor White
$tcDir = Join-Path $TargetProject "test_cases"
@("Backend", "Frontend") | ForEach-Object {
    $sub = Join-Path $tcDir $_
    if (-not (Test-Path $sub)) {
        New-Item -ItemType Directory -Path $sub -Force | Out-Null
        Write-Host "  [CREATED] test_cases\$_\" -ForegroundColor Green
    } else {
        Write-Host "  [EXISTS] test_cases\$_\" -ForegroundColor Gray
    }
}

if (-not $SkipEnv) {
    Write-Host "9. Setting up .env..." -ForegroundColor White
    $envTarget = Join-Path $TargetProject ".env"
    if (-not (Test-Path $envTarget)) {
        Copy-Item (Join-Path $toolkitRoot ".env.example") $envTarget
        Write-Host "  [CREATED] .env (from .env.example — fill in your credentials!)" -ForegroundColor Yellow
    } else {
        Write-Host "  [EXISTS] .env already exists — skipping" -ForegroundColor Gray
    }
}

$gitignoreTarget = Join-Path $TargetProject ".gitignore"
if (Test-Path $gitignoreTarget) {
    $content = Get-Content $gitignoreTarget -Raw
    if ($content -notmatch '\.env') {
        Add-Content $gitignoreTarget "`n# QA Toolkit`n.env`noutput/`n"
        Write-Host "10. Updated .gitignore with .env exclusion" -ForegroundColor Green
    } else {
        Write-Host "10. .gitignore already excludes .env" -ForegroundColor Gray
    }
} else {
    Copy-Item (Join-Path $toolkitRoot ".gitignore") $gitignoreTarget
    Write-Host "10. Created .gitignore" -ForegroundColor Green
}

Write-Host "`n=== Installation Complete ===" -ForegroundColor Cyan
Write-Host @"

Next steps:
  1. Edit $envTarget and fill in your credentials
  2. Edit .cursor\rules\main\evidence_only_answers.mdc
     -> Fill in your Jira custom field IDs (run expand=names to discover them)
  3. Edit .cursor\rules\integrations\database_workflow.mdc
     -> Add your project's SQL patterns and MCP server names
  4. Edit config\templates\Test_case_template.md
     -> Update data layers with your domain entities

"@ -ForegroundColor White
