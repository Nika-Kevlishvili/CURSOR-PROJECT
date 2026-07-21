# block-bugreview-unapproved-jira.ps1
# Hook: beforeMCPExecution
# Purpose: Block or challenge createJiraIssue for Internal Bug type unless the
#          corresponding BugReview file has been explicitly approved by the user.
#
# How it works:
#   1. Intercepts createJiraIssue calls where issueTypeName == "Internal Bug"
#   2. Finds the most recently modified BugReview_*.md in Cursor-Project/reports/Bug Reports/
#   3. Reads its first line to check approval state:
#        "# Bug Review — APPROVED"         -> allow (user confirmed via AskQuestion)
#        "# Bug Review — PENDING APPROVAL"  -> ask   (AskQuestion was not answered — native dialog)
#        no file or unrecognised state       -> deny  (no review process was started at all)
#
# States written by the agent:
#   PENDING APPROVAL  (review file created, AskQuestion not yet answered)
#   APPROVED          (agent writes this when user selects Agree, BEFORE createJiraIssue)
#   CREATED — PHN-XX  (agent writes this after successful ticket creation)

$jsonInput = [Console]::In.ReadToEnd()

try {
    $payload   = $jsonInput | ConvertFrom-Json
    $toolName  = $payload.tool_name
    $toolInput = $payload.tool_input

    # Only intercept createJiraIssue
    if ($toolName -ne "createJiraIssue") {
        @{ continue = $true; permission = "allow" } | ConvertTo-Json -Compress
        exit 0
    }

    # Parse tool arguments to check issue type
    $args = $null
    try {
        $args = if ($toolInput -is [string]) { $toolInput | ConvertFrom-Json } else { $toolInput }
    } catch { }

    $issueTypeName = ""
    if ($args) {
        $issueTypeName = if ($args.issueTypeName) { $args.issueTypeName } else { "" }
    }

    # Only guard Internal Bug type (PHN Phase 2 bug reporter)
    if ($issueTypeName -ne "Internal Bug") {
        @{ continue = $true; permission = "allow" } | ConvertTo-Json -Compress
        exit 0
    }

    # ── Find the most recently modified BugReview_*.md ──────────────────────────
    $scriptDir     = Split-Path -Parent $MyInvocation.MyCommand.Path
    $workspaceRoot = Split-Path -Parent (Split-Path -Parent $scriptDir)
    $reportsDir    = Join-Path $workspaceRoot "Cursor-Project\reports\Bug Reports"

    $reviewFile = $null
    if (Test-Path $reportsDir) {
        $reviewFile = Get-ChildItem -Path $reportsDir -Recurse -Filter "BugReview_*.md" -File |
                      Sort-Object LastWriteTime -Descending |
                      Select-Object -First 1
    }

    # ── Check approval state ─────────────────────────────────────────────────────
    if (-not $reviewFile) {
        # No review file exists at all — full block
        @{
            continue      = $true
            permission    = "deny"
            user_message  = "[HOOK BLOCKED] Cannot create Jira Internal Bug: no BugReview file found. Start the bug reporter workflow to create a review file and get approval first."
            agent_message = "BLOCK (PHOENIX-BUG.0): createJiraIssue denied. No BugReview_*.md file found under Cursor-Project/reports/Bug Reports/. The full approval workflow (review file → user Agree → APPROVED state) must complete before the ticket can be created."
        } | ConvertTo-Json -Compress
        exit 0
    }

    # Read the first line of the most recent review file
    $firstLine = ""
    try {
        $firstLine = (Get-Content -Path $reviewFile.FullName -TotalCount 1).Trim()
    } catch { }

    if ($firstLine -match "APPROVED" -and $firstLine -notmatch "PENDING") {
        # User has explicitly approved — allow through
        @{ continue = $true; permission = "allow" } | ConvertTo-Json -Compress
        exit 0
    }

    if ($firstLine -match "PENDING APPROVAL") {
        # Review file exists but user has not answered the AskQuestion yet
        # Use "ask" — surfaces a native Cursor permission dialog as a safety net
        @{
            continue      = $true
            permission    = "ask"
            user_message  = "[APPROVAL REQUIRED] The bug review file '$($reviewFile.Name)' is still in PENDING APPROVAL state. Did you confirm 'Agree' in the chat? The Jira Internal Bug ticket will only be created after you approve."
            agent_message = "BLOCK (PHOENIX-BUG.0): createJiraIssue intercepted — BugReview file is PENDING APPROVAL. You MUST update the review file first line to '# Bug Review — APPROVED' (Step 4: On Agree) before calling createJiraIssue. Do not proceed until the user explicitly approves."
        } | ConvertTo-Json -Compress
        exit 0
    }

    if ($firstLine -match "CREATED") {
        # File is from a previously created ticket — treat as no active approval for a new ticket
        @{
            continue      = $true
            permission    = "deny"
            user_message  = "[HOOK BLOCKED] The most recent BugReview file '$($reviewFile.Name)' is already marked CREATED. No active APPROVED review file found for a new ticket. Start a new bug report first."
            agent_message = "BLOCK (PHOENIX-BUG.0): createJiraIssue denied. Most recent BugReview file is already CREATED (a previous ticket). No APPROVED review file found for the current bug. Run the full bug reporter workflow again."
        } | ConvertTo-Json -Compress
        exit 0
    }

    # Unrecognised state — fail-secure: block
    @{
        continue      = $true
        permission    = "deny"
        user_message  = "[HOOK BLOCKED] BugReview file '$($reviewFile.Name)' has unrecognised approval state (first line: '$firstLine'). Cannot create Jira ticket. Ensure the review file starts with '# Bug Review — APPROVED'."
        agent_message = "BLOCK (PHOENIX-BUG.0): createJiraIssue denied. BugReview file first line is '$firstLine' — not APPROVED. Update the file to '# Bug Review — APPROVED' after user consent before calling createJiraIssue."
    } | ConvertTo-Json -Compress

} catch {
    # Fail-secure: if the hook errors, deny the operation
    @{
        continue      = $true
        permission    = "deny"
        user_message  = "[HOOK BLOCKED] Bug approval hook failed to run (error: $_). Jira Internal Bug creation blocked for safety. Please retry."
        agent_message = "BLOCK: block-bugreview-unapproved-jira hook threw an exception. Denying createJiraIssue as fail-secure. Fix the hook or retry."
    } | ConvertTo-Json -Compress
}
