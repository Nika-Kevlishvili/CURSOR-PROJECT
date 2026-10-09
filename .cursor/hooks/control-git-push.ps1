# control-git-push.ps1
# Hook: beforeShellExecution
# Purpose: Ask permission for git push/commit operations

$jsonInput = [Console]::In.ReadToEnd()

try {
    $input = $jsonInput | ConvertFrom-Json
    $command = $input.command
    $commandLower = $command.ToLower()
    
    $requiresPermission = $false
    
    if ($commandLower -match "git\s+commit") { $requiresPermission = $true }
    if ($commandLower -match "git\s+push") { $requiresPermission = $true }
    if ($commandLower -match "git\s+merge") { $requiresPermission = $true }
    if ($commandLower -match "git\s+rebase") { $requiresPermission = $true }
    if ($commandLower -match "gh\s+pr\s+(create|merge)") { $requiresPermission = $true }
    
    if ($requiresPermission) {
        $response = @{
            continue = $true
            permission = "ask"
            user_message = "[PERMISSION REQUIRED] Git write operation detected: $command"
            agent_message = "Git write operation requires explicit user approval. Please wait for user approval."
        }
    } else {
        $response = @{ continue = $true; permission = "allow" }
    }
    
    $response | ConvertTo-Json -Compress
} catch {
    @{ continue = $true; permission = "ask"; user_message = "[PERMISSION REQUIRED] Error parsing git command." } | ConvertTo-Json -Compress
}
