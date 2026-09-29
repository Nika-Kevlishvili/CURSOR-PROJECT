# protect-energots-writes.ps1
# Hook: beforeFileEdit
# Project-owner decision: writes under Cursor-Project/EnergoTS/ are allowed
# (page objects, fixtures, helpers, and frontend test generation).
# Phoenix remains protected by protect-phoenix-code.ps1.

$jsonInput = [Console]::In.ReadToEnd()

try {
    $hookInput = $jsonInput | ConvertFrom-Json
    $filePath = $hookInput.file_path
    $normalizedPathLower = ($filePath -replace '\\', '/').ToLower()
    $isEnergoTSPath = ($normalizedPathLower -match "(^|/)cursor-project/energots/")

    if ($isEnergoTSPath) {
        $response = @{
            permission = "allow"
            agent_message = "EnergoTS write allowed (Rule 0.8): $filePath"
        }
    } else {
        $response = @{ permission = "allow" }
    }

    $response | ConvertTo-Json -Compress
} catch {
    $response = @{
        permission = "deny"
        user_message = "[HOOK ERROR] EnergoTS write hook failed. Blocked for safety."
        agent_message = "BLOCK: protect-energots-writes hook error: $_"
    }
    $response | ConvertTo-Json -Compress
}
