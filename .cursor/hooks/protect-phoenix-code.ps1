# protect-phoenix-code.ps1
# Hook: beforeFileEdit
# Purpose: Block ANY file edit under Cursor-Project/Phoenix/** (Rule 0.8 Tier A)
# Phase P1: all extensions — .md, .gradle, config, not just code

$jsonInput = [Console]::In.ReadToEnd()

try {
    $hookInput = $jsonInput | ConvertFrom-Json
    $filePath = $hookInput.file_path
    
    $normalizedPathLower = ($filePath -replace '\\', '/').ToLower()
    
    $isPhoenixPath = ($normalizedPathLower -match "(^|/)cursor-project/phoenix/")
    
    if ($isPhoenixPath) {
        $response = @{
            permission = "deny"
            user_message = "[HOOK BLOCKED] Any edit under Phoenix is forbidden (Rule 0.8 Tier A). File: $filePath"
            agent_message = "BLOCK: Phoenix is read-only for Cursor AI. File '$filePath' is under Cursor-Project/Phoenix/. No file type is exempt."
        }
    } else {
        $response = @{ permission = "allow" }
    }
    
    $response | ConvertTo-Json -Compress
} catch {
    $response = @{
        permission = "deny"
        user_message = "[HOOK ERROR] Error processing Phoenix protection hook. Blocked for safety. File: $filePath"
        agent_message = "BLOCK: protect-phoenix-code hook error: $_"
    }
    $response | ConvertTo-Json -Compress
}
