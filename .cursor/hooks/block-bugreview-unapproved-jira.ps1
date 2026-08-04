# block-bugreview-unapproved-jira.ps1
# Hook: beforeMCPExecution
# Purpose: Block or challenge createJiraIssue for Internal Bug and External Bug unless the
#          corresponding BugReview file has been explicitly approved by the user.
#
# How it works:
#   1. Intercepts createJiraIssue where issueTypeName is "Internal Bug" or "Bug"
#   2. Resolves the active BugReview file (sidecar -> single APPROVED scan -> latest mtime)
#   3. Reads first line for approval state:
#        "# Bug Review - APPROVED"           -> allow
#        "# Bug Review - PENDING APPROVAL"   -> ask
#        "# Bug Review - VALIDATION STOPPED" -> deny
#        "# Bug Review - CANCELLED"          -> deny
#        "# Bug Review - CREATED - KEY"      -> deny (stale for new ticket)
#
# Sidecar: Cursor-Project/reports/Bug Reports/.active-bugreview (one line = absolute path)
# Written by agent on Step 4 Agree; cleared on CREATED / VALIDATION STOPPED / CANCELLED

$jsonInput = [Console]::In.ReadToEnd()

function Get-ReviewFirstLine {
    param([string]$FilePath)
    if (-not $FilePath -or -not (Test-Path -LiteralPath $FilePath)) { return "" }
    try {
        return (Get-Content -LiteralPath $FilePath -TotalCount 1).Trim()
    } catch {
        return ""
    }
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
    } catch {
        return $false
    }
}

function Get-ApprovedReviewFiles {
    param([string]$ReportsDir)
    $approved = @()
    if (-not (Test-Path $ReportsDir)) { return $approved }
    Get-ChildItem -Path $ReportsDir -Recurse -Filter "BugReview_*.md" -File | ForEach-Object {
        $line = Get-ReviewFirstLine -FilePath $_.FullName
        if (Test-IsApprovedHeader -FirstLine $line) {
            $approved += $_
        }
    }
    return $approved
}

function Resolve-ActiveReviewFile {
    param([string]$ReportsDir)

    $sidecarPath = Join-Path $reportsDir ".active-bugreview"
    if (Test-Path -LiteralPath $sidecarPath) {
        try {
            $sidecarLine = (Get-Content -LiteralPath $sidecarPath -Raw).Trim()
            if ($sidecarLine -and (Test-ReviewPathUnderReports -FilePath $sidecarLine -ReportsDir $ReportsDir)) {
                $sidecarFile = Get-Item -LiteralPath $sidecarLine
                return @{ File = $sidecarFile; Source = "sidecar" }
            }
        } catch { }
    }

    $approvedFiles = Get-ApprovedReviewFiles -ReportsDir $ReportsDir
    if ($approvedFiles.Count -eq 1) {
        return @{ File = $approvedFiles[0]; Source = "approved_scan" }
    }
    if ($approvedFiles.Count -gt 1) {
        return @{ File = $null; Source = "multiple_approved"; ApprovedCount = $approvedFiles.Count }
    }

    if (Test-Path $ReportsDir) {
        $latest = Get-ChildItem -Path $ReportsDir -Recurse -Filter "BugReview_*.md" -File |
                  Sort-Object LastWriteTime -Descending |
                  Select-Object -First 1
        if ($latest) {
            return @{ File = $latest; Source = "latest_mtime" }
        }
    }

    return @{ File = $null; Source = "none" }
}

try {
    $payload   = $jsonInput | ConvertFrom-Json
    $toolName  = $payload.tool_name
    $toolInput = $payload.tool_input

    if ($toolName -ne "createJiraIssue") {
        @{ continue = $true; permission = "allow" } | ConvertTo-Json -Compress
        exit 0
    }

    $args = $null
    try {
        $args = if ($toolInput -is [string]) { $toolInput | ConvertFrom-Json } else { $toolInput }
    } catch { }

    $issueTypeName = ""
    if ($args) {
        $issueTypeName = if ($args.issueTypeName) { $args.issueTypeName } else { "" }
    }

    $guardedTypes = @("Internal Bug", "Bug")
    if ($issueTypeName -notin $guardedTypes) {
        @{ continue = $true; permission = "allow" } | ConvertTo-Json -Compress
        exit 0
    }

    $scriptDir     = Split-Path -Parent $MyInvocation.MyCommand.Path
    $workspaceRoot = Split-Path -Parent (Split-Path -Parent $scriptDir)
    $reportsDir    = Join-Path $workspaceRoot "Cursor-Project\reports\Bug Reports"

    $resolved = Resolve-ActiveReviewFile -ReportsDir $reportsDir

    if ($resolved.Source -eq "multiple_approved") {
        @{
            continue      = $true
            permission    = "deny"
            user_message  = "[HOOK BLOCKED] Multiple APPROVED BugReview files found ($($resolved.ApprovedCount)). Write .active-bugreview sidecar on Step 4 Agree with the correct review file path before createJiraIssue."
            agent_message = "BLOCK (PHOENIX-BUG.0): createJiraIssue denied. Multiple BugReview files have APPROVED headers and no valid .active-bugreview sidecar. On Step 4 Agree, write Cursor-Project/reports/Bug Reports/.active-bugreview with the absolute path to the current review file before calling createJiraIssue."
        } | ConvertTo-Json -Compress
        exit 0
    }

    $reviewFile = $resolved.File

    if (-not $reviewFile) {
        @{
            continue      = $true
            permission    = "deny"
            user_message  = "[HOOK BLOCKED] Cannot create Jira bug (Internal Bug or External Bug): no BugReview file found. Start the bug reporter workflow to create a review file and get approval first."
            agent_message = "BLOCK (PHOENIX-BUG.0): createJiraIssue denied. No BugReview_*.md file found under Cursor-Project/reports/Bug Reports/. The full approval workflow (review file -> Step 3.5 -> user Agree -> APPROVED + .active-bugreview sidecar) must complete before the ticket can be created."
        } | ConvertTo-Json -Compress
        exit 0
    }

    $firstLine = Get-ReviewFirstLine -FilePath $reviewFile.FullName

    if (Test-IsApprovedHeader -FirstLine $firstLine) {
        @{ continue = $true; permission = "allow" } | ConvertTo-Json -Compress
        exit 0
    }

    if ($firstLine -match "PENDING APPROVAL") {
        @{
            continue      = $true
            permission    = "ask"
            user_message  = "[APPROVAL REQUIRED] The bug review file '$($reviewFile.Name)' is still in PENDING APPROVAL state. Did you confirm 'Agree' in the chat? The Jira bug ticket will only be created after you approve."
            agent_message = "BLOCK (PHOENIX-BUG.0): createJiraIssue intercepted - BugReview file is PENDING APPROVAL. You MUST update the review file first line to APPROVED header and write .active-bugreview sidecar (Step 4: On Agree) before calling createJiraIssue."
        } | ConvertTo-Json -Compress
        exit 0
    }

    if ($firstLine -match "VALIDATION STOPPED") {
        @{
            continue      = $true
            permission    = "deny"
            user_message  = "[HOOK BLOCKED] BugReview '$($reviewFile.Name)' is VALIDATION STOPPED. Pre-create validation failed - start a new bug report or fix evidence before filing on Jira."
            agent_message = "BLOCK (PHOENIX-BUG.0): createJiraIssue denied. BugReview header is VALIDATION STOPPED. Do not create Jira ticket; validation stop is mandatory."
        } | ConvertTo-Json -Compress
        exit 0
    }

    if ($firstLine -match "CANCELLED") {
        @{
            continue      = $true
            permission    = "deny"
            user_message  = "[HOOK BLOCKED] BugReview '$($reviewFile.Name)' is CANCELLED. Start a new bug report to file on Jira."
            agent_message = "BLOCK (PHOENIX-BUG.0): createJiraIssue denied. BugReview header is CANCELLED."
        } | ConvertTo-Json -Compress
        exit 0
    }

    if ($firstLine -match "CREATED") {
        @{
            continue      = $true
            permission    = "deny"
            user_message  = "[HOOK BLOCKED] BugReview '$($reviewFile.Name)' is already CREATED. No active APPROVED review for a new ticket. Start a new bug report first."
            agent_message = "BLOCK (PHOENIX-BUG.0): createJiraIssue denied. BugReview is CREATED (previous ticket). Run the full bug reporter workflow again."
        } | ConvertTo-Json -Compress
        exit 0
    }

    @{
        continue      = $true
        permission    = "deny"
        user_message  = "[HOOK BLOCKED] BugReview '$($reviewFile.Name)' has unrecognised state (first line: '$firstLine'). Valid create header: APPROVED after Step 4 Agree."
        agent_message = "BLOCK (PHOENIX-BUG.0): createJiraIssue denied. BugReview first line is '$firstLine'. Expected APPROVED header plus .active-bugreview sidecar after user Agree."
    } | ConvertTo-Json -Compress

} catch {
    @{
        continue      = $true
        permission    = "deny"
        user_message  = "[HOOK BLOCKED] Bug approval hook failed (error: $_). Jira bug creation blocked for safety. Please retry."
        agent_message = "BLOCK: block-bugreview-unapproved-jira hook threw an exception. Denying createJiraIssue as fail-secure."
    } | ConvertTo-Json -Compress
}
