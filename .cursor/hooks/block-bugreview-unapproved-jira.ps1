# block-bugreview-unapproved-jira.ps1
# Hook: beforeMCPExecution
# Purpose: Block or challenge createJiraIssue for Internal Bug and External Bug unless the
#          corresponding BugReview file has been explicitly approved by the user.
#
# How it works:
#   1. Intercepts createJiraIssue (or CallDynamicTool wrapping createJiraIssue) where
#      issueTypeName is "Internal Bug" or "Bug"
#   2. Resolves the active BugReview file (sidecar -> single APPROVED scan -> latest mtime)
#   3. Reads first line for approval state:
#        "# Bug Review - APPROVED"           -> allow
#        "# Bug Review - PENDING APPROVAL"   -> ask
#        "# Bug Review - CANCELLED"          -> deny
#        "# Bug Review - CREATED - KEY"      -> deny (stale for new ticket)
#
# Sidecar: Cursor-Project/reports/Bug Reports/.active-bugreview (one line = absolute path)
# Written by agent on Step 4 Agree; cleared on CREATED / CANCELLED
#
# Fail-open: empty stdin, parse failures, unexpected payload shapes, and all non-create
# MCP (Jira reads, Confluence reads, PostgreSQL/Oracle/MySQL, Elasticsearch, Slack) MUST allow.
# Fail-secure: only a parsed guarded createJiraIssue (Internal Bug / Bug) is denied when
# the approval gate fails.

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

    $sidecarPath = Join-Path $ReportsDir ".active-bugreview"
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

function Write-HookAllow {
    @{ continue = $true; permission = "allow" } | ConvertTo-Json -Compress
}

function Write-HookDeny {
    param([string]$UserMessage, [string]$AgentMessage)
    @{
        continue      = $true
        permission    = "deny"
        user_message  = $UserMessage
        agent_message = $AgentMessage
    } | ConvertTo-Json -Compress
}

function Get-ToolField {
    param($Obj, [string]$Camel, [string]$Snake)
    if ($null -eq $Obj) { return $null }
    $v = $Obj.$Camel
    if ($null -eq $v) { $v = $Obj.$Snake }
    return $v
}

# $true only after successful JSON parse of a guarded create.
$isGuardedCreate = $false
$toolName = $null

try {
    if ([string]::IsNullOrWhiteSpace($jsonInput)) {
        Write-HookAllow
        exit 0
    }

    $jsonForParse = $jsonInput.Trim()
    if ($jsonForParse.StartsWith([char]0xFEFF)) {
        $jsonForParse = $jsonForParse.Substring(1)
    }

    $payload = $null
    try {
        $payload = $jsonForParse | ConvertFrom-Json -ErrorAction Stop
    } catch {
        # Parse failure — fail open (never deny/ask unparsed MCP, including reads)
        Write-HookAllow
        exit 0
    }

    if ($null -eq $payload) {
        Write-HookAllow
        exit 0
    }

    $toolName = Get-ToolField -Obj $payload -Camel "toolName" -Snake "tool_name"
    $toolInput = Get-ToolField -Obj $payload -Camel "toolInput" -Snake "tool_input"
    if ($null -eq $toolInput) { $toolInput = $payload.arguments }

    $createInput = $null
    if ($toolName -eq "createJiraIssue") {
        $isGuardedCreate = $true
        $createInput = $toolInput
    } elseif ($toolName -eq "CallDynamicTool") {
        $innerName = $null
        if ($toolInput) {
            $innerName = Get-ToolField -Obj $toolInput -Camel "toolName" -Snake "tool_name"
            if ($innerName -ne "createJiraIssue") {
                $innerArgs = Get-ToolField -Obj $toolInput -Camel "arguments" -Snake "args"
                if ($innerArgs) {
                    $fromArgs = Get-ToolField -Obj $innerArgs -Camel "toolName" -Snake "tool_name"
                    if ($fromArgs -eq "createJiraIssue") { $innerName = $fromArgs }
                }
            }
        }
        if ($innerName -eq "createJiraIssue") {
            $isGuardedCreate = $true
            $createInput = Get-ToolField -Obj $toolInput -Camel "arguments" -Snake "args"
            if ($null -eq $createInput) { $createInput = $toolInput }
        }
    }

    if (-not $isGuardedCreate) {
        Write-HookAllow
        exit 0
    }

    $args = $null
    try {
        $args = if ($createInput -is [string]) { $createInput | ConvertFrom-Json -ErrorAction Stop } else { $createInput }
    } catch { }

    $issueTypeName = ""
    if ($args) {
        $issueTypeName = if ($args.issueTypeName) { $args.issueTypeName } else { "" }
    }

    $guardedTypes = @("Internal Bug", "Bug")
    if ($issueTypeName -notin $guardedTypes) {
        Write-HookAllow
        exit 0
    }

    $scriptDir     = Split-Path -Parent $MyInvocation.MyCommand.Path
    $workspaceRoot = Split-Path -Parent (Split-Path -Parent $scriptDir)
    $reportsDir    = Join-Path $workspaceRoot "Cursor-Project\reports\Bug Reports"

    $resolved = Resolve-ActiveReviewFile -ReportsDir $reportsDir

    if ($resolved.Source -eq "multiple_approved") {
        Write-HookDeny `
            -UserMessage "[HOOK BLOCKED] Multiple APPROVED BugReview files found ($($resolved.ApprovedCount)). Write .active-bugreview sidecar on Step 4 Agree with the correct review file path before createJiraIssue." `
            -AgentMessage "BLOCK (PHOENIX-BUG.0): createJiraIssue denied. Multiple BugReview files have APPROVED headers and no valid .active-bugreview sidecar. On Step 4 Agree, write Cursor-Project/reports/Bug Reports/.active-bugreview with the absolute path to the current review file before calling createJiraIssue."
        exit 0
    }

    $reviewFile = $resolved.File

    if (-not $reviewFile) {
        Write-HookDeny `
            -UserMessage "[HOOK BLOCKED] Cannot create Jira bug (Internal Bug or External Bug): no BugReview file found. Start the bug reporter workflow to create a review file and get approval first." `
            -AgentMessage "BLOCK (PHOENIX-BUG.0): createJiraIssue denied. No BugReview_*.md file found under Cursor-Project/reports/Bug Reports/. The full approval workflow (review file -> user Agree -> APPROVED + .active-bugreview sidecar) must complete before the ticket can be created."
        exit 0
    }

    $firstLine = Get-ReviewFirstLine -FilePath $reviewFile.FullName

    if (Test-IsApprovedHeader -FirstLine $firstLine) {
        Write-HookAllow
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

    if ($firstLine -match "CANCELLED") {
        Write-HookDeny `
            -UserMessage "[HOOK BLOCKED] BugReview '$($reviewFile.Name)' is CANCELLED. Start a new bug report to file on Jira." `
            -AgentMessage "BLOCK (PHOENIX-BUG.0): createJiraIssue denied. BugReview header is CANCELLED."
        exit 0
    }

    if ($firstLine -match "CREATED") {
        Write-HookDeny `
            -UserMessage "[HOOK BLOCKED] BugReview '$($reviewFile.Name)' is already CREATED. No active APPROVED review for a new ticket. Start a new bug report first." `
            -AgentMessage "BLOCK (PHOENIX-BUG.0): createJiraIssue denied. BugReview is CREATED (previous ticket). Run the full bug reporter workflow again."
        exit 0
    }

    Write-HookDeny `
        -UserMessage "[HOOK BLOCKED] BugReview '$($reviewFile.Name)' has unrecognised state (first line: '$firstLine'). Valid create header: APPROVED after Step 4 Agree." `
        -AgentMessage "BLOCK (PHOENIX-BUG.0): createJiraIssue denied. BugReview first line is '$firstLine'. Expected APPROVED header plus .active-bugreview sidecar after user Agree."

} catch {
    # Deny only if we already identified a guarded create before the exception.
    if ($isGuardedCreate) {
        Write-HookDeny `
            -UserMessage "[HOOK BLOCKED] Bug approval hook failed (error: $_). Jira bug creation blocked for safety. Please retry." `
            -AgentMessage "BLOCK: block-bugreview-unapproved-jira hook threw an exception. Denying createJiraIssue as fail-secure."
    } else {
        Write-HookAllow
    }
}
