# block-acli-rovo.ps1
# Hook: beforeShellExecution (matcher: acli | twg)
# Purpose: Keep Rovo credits at zero, cap Jira search size, and keep the bug-review
#          approval gate on official `acli jira workitem create` for Bug / Internal Bug.
# Fail-closed is set on the hook entry. This script still allows unrelated acli help.

$jsonInput = [Console]::In.ReadToEnd()

function Write-Allow {
    @{ continue = $true; permission = "allow" } | ConvertTo-Json -Compress
}

function Write-Deny {
    param([string]$UserMessage, [string]$AgentMessage)
    @{
        continue      = $false
        permission    = "deny"
        user_message  = $UserMessage
        agent_message = $AgentMessage
    } | ConvertTo-Json -Compress
}

function Get-ReviewFirstLine {
    param([string]$FilePath)
    if (-not $FilePath -or -not (Test-Path -LiteralPath $FilePath)) { return "" }
    try { return (Get-Content -LiteralPath $FilePath -TotalCount 1).Trim() } catch { return "" }
}

function Test-IsApprovedHeader {
    param([string]$FirstLine)
    return ($FirstLine -match "APPROVED" -and $FirstLine -notmatch "PENDING")
}

function Test-ReviewPathUnderReports {
    param([string]$FilePath, [string]$ReportsDir)
    if (-not $FilePath) { return $false }
    try {
        $full = [System.IO.Path]::GetFullPath($FilePath)
        $reportsFull = [System.IO.Path]::GetFullPath($ReportsDir)
        return $full.StartsWith($reportsFull, [StringComparison]::OrdinalIgnoreCase)
    } catch { return $false }
}

function Get-ApprovedReviewFiles {
    param([string]$ReportsDir)
    $approved = @()
    if (-not (Test-Path $ReportsDir)) { return $approved }
    Get-ChildItem -Path $ReportsDir -Recurse -Filter "BugReview_*.md" -File | ForEach-Object {
        $line = Get-ReviewFirstLine -FilePath $_.FullName
        if (Test-IsApprovedHeader -FirstLine $line) { $approved += $_ }
    }
    return $approved
}

function Resolve-ActiveReviewFile {
    param([string]$ReportsDir)
    $sidecarPath = Join-Path $ReportsDir ".active-bugreview"
    if (Test-Path -LiteralPath $sidecarPath) {
        try {
            $sidecarLine = (Get-Content -LiteralPath $sidecarPath -Raw).Trim()
            if ($sidecarLine -and (Test-ReviewPathUnderReports -FilePath $sidecarLine -ReportsDir $ReportsDir)) {
                return @{ File = (Get-Item -LiteralPath $sidecarLine); Source = "sidecar" }
            }
        } catch { }
    }
    $approvedFiles = @(Get-ApprovedReviewFiles -ReportsDir $ReportsDir)
    if ($approvedFiles.Count -eq 1) { return @{ File = $approvedFiles[0]; Source = "single" } }
    if ($approvedFiles.Count -gt 1) { return @{ File = $null; Source = "multiple_approved"; ApprovedCount = $approvedFiles.Count } }
    return @{ File = $null; Source = "none" }
}

try {
    if (-not $jsonInput) { Write-Allow; exit 0 }
    $payload = $jsonInput | ConvertFrom-Json
    $command = [string]$payload.command
    if (-not $command) { Write-Allow; exit 0 }
    $lower = $command.ToLower()

    if ($lower -match 'acli\s+rovodev' -or $lower -match 'acli\s+rovo\b' -or $lower -match '(^|[;&|]\s*)twg\b' -or $lower -match 'teamwork-graph') {
        Write-Deny "[HOOK BLOCKED] That command can spend Rovo credits." "BLOCK (ATLASSIAN.1): acli rovodev, acli rovo, and Teamwork Graph (twg) are forbidden. Use acli jira workitem view or search."
        exit 0
    }

    if ($lower -match 'acli\s+' -and $lower -match '--web(\s|$)') {
        Write-Deny "[HOOK BLOCKED] acli --web is not a data retrieval." "BLOCK (ATLASSIAN.2): do not pass --web to acli."
        exit 0
    }

    if ($lower -match 'acli\s+jira\s+workitem\s+(create-bulk|delete)\b' -or
        $lower -match 'acli\s+jira\s+(field|project)\s+(create|delete)\b' -or
        $lower -match 'acli\s+jira\s+workitem\s+(attachment|comment|link)\s+delete\b') {
        Write-Deny "[HOOK BLOCKED] That acli write is outside the allowed bug-reporter commands." "BLOCK (ATLASSIAN.3): allowed writes are acli jira workitem create and acli jira workitem edit only, after bug-review approval."
        exit 0
    }

    if ($lower -match 'acli\s+jira\s+workitem\s+search\b') {
        if ($lower -notmatch '--jql(\s|=)') {
            Write-Deny "[HOOK BLOCKED] acli jira workitem search requires --jql." "BLOCK (ATLASSIAN.2): add --jql. Do not run an unbounded search."
            exit 0
        }
        if ($lower -match '--paginate') {
            Write-Deny "[HOOK BLOCKED] acli jira workitem search --paginate is forbidden." "BLOCK (ATLASSIAN.2): use --limit 25 (maximum 50). Do not paginate search."
            exit 0
        }
        $countOnly = ($lower -match '--count') -and ($lower -notmatch '--json') -and ($lower -notmatch '--csv')
        if (-not $countOnly -and $lower -notmatch '--limit(\s|=)') {
            Write-Deny "[HOOK BLOCKED] acli jira workitem search requires --limit (maximum 50)." "BLOCK (ATLASSIAN.2): add --limit 25."
            exit 0
        }
        if ($lower -match '--limit(?:=|\s+)(\d+)') {
            $limit = [int]$Matches[1]
            if ($limit -gt 50) {
                Write-Deny "[HOOK BLOCKED] acli search --limit $limit exceeds 50." "BLOCK (ATLASSIAN.2): --limit must be 50 or less."
                exit 0
            }
        }
    }

    if ($lower -match 'acli\s+jira\s+workitem\s+comment\s+list\b' -and $lower -match '--paginate') {
        Write-Deny "[HOOK BLOCKED] comment list --paginate is forbidden." "BLOCK (ATLASSIAN.2): acli jira workitem comment list --limit 50 --json."
        exit 0
    }

    if ($lower -match 'acli\s+jira\s+workitem\s+create\b') {
        $isGuardedBug = $false
        if ($command -match '--type\s+[''"]?Internal Bug[''"]?') { $isGuardedBug = $true }
        if ($command -match '--type\s+[''"]?Bug[''"]?(\s|$)') { $isGuardedBug = $true }
        if ($command -match '--from-json\s+[''"]?([^\s''"]+)') {
            $jsonPath = $Matches[1]
            if (Test-Path -LiteralPath $jsonPath) {
                $body = Get-Content -LiteralPath $jsonPath -Raw -ErrorAction SilentlyContinue
                if ($body -match 'Internal Bug' -or $body -match '"Bug"') { $isGuardedBug = $true }
            }
        }
        if ($isGuardedBug) {
            $scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
            $workspaceRoot = Split-Path -Parent (Split-Path -Parent $scriptDir)
            $reportsDir = Join-Path $workspaceRoot "Cursor-Project/reports/Bug Reports"
            $resolved = Resolve-ActiveReviewFile -ReportsDir $reportsDir
            if ($resolved.Source -eq "multiple_approved") {
                Write-Deny "[HOOK BLOCKED] Multiple APPROVED BugReview files. Write .active-bugreview before acli jira workitem create." "BLOCK (PHOENIX-BUG.0): acli create denied until .active-bugreview points at the APPROVED review."
                exit 0
            }
            $reviewFile = $resolved.File
            if (-not $reviewFile) {
                Write-Deny "[HOOK BLOCKED] No APPROVED BugReview. Finish the bug reporter approval gate before creating the Jira work item." "BLOCK (PHOENIX-BUG.0): acli jira workitem create denied. No APPROVED BugReview_*.md."
                exit 0
            }
            $firstLine = Get-ReviewFirstLine -FilePath $reviewFile.FullName
            if (-not (Test-IsApprovedHeader -FirstLine $firstLine)) {
                Write-Deny "[HOOK BLOCKED] BugReview '$($reviewFile.Name)' is not APPROVED (first line: '$firstLine')." "BLOCK (PHOENIX-BUG.0): set the review header to APPROVED and write .active-bugreview before acli jira workitem create."
                exit 0
            }
        }
    }

    Write-Allow
} catch {
    @{
        continue      = $false
        permission    = "deny"
        user_message  = "[HOOK BLOCKED] acli safety hook failed. Command blocked so a Rovo or unapproved create cannot slip through."
        agent_message = "BLOCK: block-acli-rovo.ps1 failed closed. Error: $_"
    } | ConvertTo-Json -Compress
}
