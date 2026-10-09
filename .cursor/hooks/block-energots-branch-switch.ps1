# block-energots-branch-switch.ps1
# Hook: beforeShellExecution
# Purpose: Block git checkout/switch operations in EnergoTS directory to branches other than 'cursor'
# Rule: ENERGOTS.0 - EnergoTS Branch Restriction

$jsonInput = [Console]::In.ReadToEnd()

try {
    $input = $jsonInput | ConvertFrom-Json
    $command = $input.command
    $workingDirectory = $input.working_directory
    
    if (-not $command) {
        $response = @{ continue = $true }
        $response | ConvertTo-Json -Compress
        exit 0
    }
    
    $commandLower = $command.ToLower().Trim()
    
    # Check if command is a git branch switching operation
    $isBranchOperation = $false
    $targetBranch = $null
    
    # EXCEPTION: Allow fetch/merge/pull/rebase operations from main branch
    # These operations update cursor branch from main without switching branches
    # NOTE: While hooks allow these operations, the rule (ENERGOTS.0) requires
    # explicit user request - Cursor AI MUST NOT perform these automatically
    $isMainSyncOperation = $false
    if ($commandLower -match "git\s+(fetch|merge|pull|rebase)\s+(origin/)?main") {
        $isMainSyncOperation = $true
    }
    if ($commandLower -match "git\s+fetch\s+origin\s+main") {
        $isMainSyncOperation = $true
    }
    if ($commandLower -match "git\s+merge\s+origin/main") {
        $isMainSyncOperation = $true
    }
    if ($commandLower -match "git\s+pull\s+origin\s+main") {
        $isMainSyncOperation = $true
    }
    if ($commandLower -match "git\s+rebase\s+origin/main") {
        $isMainSyncOperation = $true
    }
    
    # If it's a main sync operation, allow it (will check EnergoTS directory later)
    if ($isMainSyncOperation) {
        # We'll check EnergoTS directory and allow if in EnergoTS
        # This allows updating cursor branch from main
    }
    
    # Patterns for git checkout/switch operations
    if ($commandLower -match "git\s+(checkout|switch)\s+(-b\s+)?([^\s]+)") {
        $isBranchOperation = $true
        $targetBranch = $matches[3]
        
        # Skip if it's a file checkout (has file path indicators)
        if ($targetBranch -match "\.(ts|js|tsx|jsx|java|py|md|json|yaml|yml|xml|css|html|sql)$") {
            $isBranchOperation = $false
            $targetBranch = $null
        }
        
        # Skip if it's a tag checkout (starts with tags/ or refs/)
        if ($targetBranch -match "^(tags|refs)/") {
            $isBranchOperation = $false
            $targetBranch = $null
        }
    }
    
    # If not a branch operation, allow it
    if (-not $isBranchOperation -or -not $targetBranch) {
        $response = @{ continue = $true }
        $response | ConvertTo-Json -Compress
        exit 0
    }
    
    # Normalize paths for matching
    $normalizedWorkingDir = ""
    if ($workingDirectory) {
        $normalizedWorkingDir = $workingDirectory -replace '\\', '/'  # Normalize separators
        $normalizedWorkingDir = $normalizedWorkingDir.ToLower()
    }
    
    # Also check command for EnergoTS path
    $commandNormalized = $command -replace '\\', '/'
    $commandNormalizedLower = $commandNormalized.ToLower()
    
    # Check if operation targets EnergoTS directory
    $isEnergoTSOperation = $false
    
    # Check working directory
    if ($normalizedWorkingDir -match "energots") {
        $isEnergoTSOperation = $true
    }
    
    # Check command path (cd commands, etc.)
    if ($commandNormalizedLower -match "energots") {
        $isEnergoTSOperation = $true
    }
    
    # Check if command contains path to EnergoTS
    if ($commandNormalizedLower -match "cursor-project[\\/]energots") {
        $isEnergoTSOperation = $true
    }
    
    # If not targeting EnergoTS, allow it
    if (-not $isEnergoTSOperation) {
        $response = @{ continue = $true }
        $response | ConvertTo-Json -Compress
        exit 0
    }
    
    # EXCEPTION: Allow main sync operations (fetch/merge/pull/rebase from main)
    # These operations update cursor branch from main without switching branches
    if ($isMainSyncOperation -and $isEnergoTSOperation) {
        $response = @{ continue = $true }
        $response | ConvertTo-Json -Compress
        exit 0
    }
    
    # If not a branch operation, allow it (e.g., fetch, pull without branch switch)
    if (-not $isBranchOperation) {
        $response = @{ continue = $true }
        $response | ConvertTo-Json -Compress
        exit 0
    }
    
    # Normalize target branch name (remove quotes, trim)
    $targetBranchNormalized = $targetBranch.Trim('"', "'", '`').Trim()
    
    # Check if target branch is 'cursor' (allowed)
    if ($targetBranchNormalized -eq "cursor") {
        $response = @{ continue = $true }
        $response | ConvertTo-Json -Compress
        exit 0
    }
    
    # Block if trying to switch to a different branch in EnergoTS
    $response = @{
        continue = $false
        block = $true
        permission = "deny"
        user_message = "[HOOK BLOCKED] EnergoTS project is locked to 'cursor' branch only. Cannot switch to '$targetBranchNormalized'. Operation blocked. To update cursor from main, use: git fetch origin main && git merge origin/main"
        agent_message = "CRITICAL: Branch switch blocked. Rule ENERGOTS.0 states that EnergoTS project must remain on 'cursor' branch only. Attempted to switch to '$targetBranchNormalized' in EnergoTS directory. Use 'git fetch origin main && git merge origin/main' to update cursor branch from main instead."
    }
    
    $response | ConvertTo-Json -Compress
} catch {
    # On error, deny by default for safety (fail-secure)
    $response = @{
        continue = $false
        block = $true
        permission = "deny"
        user_message = "[HOOK ERROR] Error processing EnergoTS branch lock hook. Blocked for safety."
        agent_message = "CRITICAL: Hook error occurred. Blocked branch operation for safety. Error: $_"
    }
    $response | ConvertTo-Json -Compress
}
