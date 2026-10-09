# block-energots-branch-requests.ps1
# Hook: beforeSubmitPrompt
# Purpose: Block prompts that request branch switching in EnergoTS to branches other than 'cursor'
# Rule: ENERGOTS.0 - EnergoTS Branch Restriction

$jsonInput = [Console]::In.ReadToEnd()

try {
    $input = $jsonInput | ConvertFrom-Json
    $prompt = $input.prompt
    
    if (-not $prompt) {
        $response = @{ continue = $true }
        $response | ConvertTo-Json -Compress
        exit 0
    }
    
    $promptLower = $prompt.ToLower()
    
    # Keywords that indicate branch switching requests (forbidden)
    $branchSwitchKeywords = @(
        "checkout", "switch", "change branch", "switch branch",
        "go to branch", "move to branch", "use branch",
        "checkout branch", "switch to", "change to branch"
    )
    
    # Keywords that indicate sync/update operations (allowed)
    $syncKeywords = @(
        "update", "sync", "merge", "pull", "fetch", "rebase",
        "update from", "sync with", "merge from", "pull from",
        "fetch from", "rebase onto", "get changes from"
    )
    
    # EnergoTS-related keywords
    $energotsKeywords = @(
        "energots",
        "cursor-project/energots",
        "energots/",
        "energo"
    )
    
    # Branch names that are NOT allowed (anything except 'cursor')
    # We'll detect if a specific branch name is mentioned
    $hasBranchSwitchKeyword = $false
    $hasSyncKeyword = $false
    $hasEnergoTSKeyword = $false
    $hasForbiddenBranch = $false
    $mentionedBranch = $null
    
    # Check for branch switching keywords (forbidden)
    foreach ($keyword in $branchSwitchKeywords) {
        if ($promptLower -match "\b$keyword\b") {
            $hasBranchSwitchKeyword = $true
            break
        }
    }
    
    # Check for sync/update keywords (allowed)
    foreach ($keyword in $syncKeywords) {
        if ($promptLower -match "\b$keyword\b") {
            $hasSyncKeyword = $true
            break
        }
    }
    
    # If it's a sync operation, allow it (will check if it's about switching later)
    if ($hasSyncKeyword -and -not $hasBranchSwitchKeyword) {
        # This is likely a sync/update operation, not a branch switch
        # Allow it to proceed
        $response = @{ continue = $true }
        $response | ConvertTo-Json -Compress
        exit 0
    }
    
    # Check for EnergoTS keywords
    foreach ($keyword in $energotsKeywords) {
        if ($promptLower -match [regex]::Escape($keyword)) {
            $hasEnergoTSKeyword = $true
            break
        }
    }
    
    # Check if a specific branch name is mentioned (common branch names)
    $commonBranches = @(
        "main", "master", "dev", "develop", "development",
        "test", "testing", "staging", "prod", "production",
        "feature", "fix", "bugfix", "hotfix", "release"
    )
    
    foreach ($branch in $commonBranches) {
        if ($promptLower -match "\b$branch\b") {
            # Check if it's in context of EnergoTS and branch switching (not sync)
            if ($hasEnergoTSKeyword -and $hasBranchSwitchKeyword -and -not $hasSyncKeyword) {
                $hasForbiddenBranch = $true
                $mentionedBranch = $branch
                break
            }
        }
    }
    
    # Also check for explicit branch names in quotes or after "to" keyword
    if ($hasEnergoTSKeyword -and $hasBranchSwitchKeyword -and -not $hasSyncKeyword) {
        # Pattern: "checkout <branch>" or "switch to <branch>" (forbidden)
        if ($promptLower -match "(?:checkout|switch\s+to|change\s+to|go\s+to)\s+['""]?([a-z0-9_-]+)['""]?") {
            $mentionedBranch = $matches[1]
            if ($mentionedBranch -ne "cursor") {
                $hasForbiddenBranch = $true
            }
        }
    }
    
    # Block if prompt requests branch switch in EnergoTS AND mentions a branch other than 'cursor'
    # But allow if it's a sync/update operation (merge, pull, fetch, etc.)
    if ($hasEnergoTSKeyword -and $hasBranchSwitchKeyword -and $hasForbiddenBranch -and -not $hasSyncKeyword) {
        $response = @{
            continue = $false
            block = $true
            user_message = "[HOOK BLOCKED] EnergoTS project is locked to 'cursor' branch only. Cannot switch to '$mentionedBranch'. Request blocked. To update cursor from main, use: 'Update cursor branch from main' or 'Merge main into cursor'."
            agent_message = "CRITICAL: Prompt blocked. Rule ENERGOTS.0 states that EnergoTS project must remain on 'cursor' branch only. Attempted to switch to '$mentionedBranch'. Use sync operations (merge, pull, fetch) to update cursor from main instead."
        }
    } else {
        $response = @{ continue = $true }
    }
    
    $response | ConvertTo-Json -Compress
} catch {
    # On error, allow prompt but log warning
    $response = @{ continue = $true }
    $response | ConvertTo-Json -Compress
}
