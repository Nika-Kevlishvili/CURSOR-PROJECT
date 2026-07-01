# protect-app-code.ps1 — Tier A: block edits under configured app code path (read-only for AI)
$jsonInput = [Console]::In.ReadToEnd()
try {
    $hookInput = $jsonInput | ConvertFrom-Json
    $filePath = $hookInput.file_path
    $normalized = ($filePath -replace '\\', '/').ToLower()

    $guardSegment = 'project/app'
    $envFile = Join-Path (Get-Location) '.env'
    if (Test-Path -LiteralPath $envFile) {
        foreach ($line in Get-Content -LiteralPath $envFile) {
            if ($line -match '^\s*QA_APP_CODE_GLOB\s*=\s*(.+)$') {
                $guardSegment = ($matches[1].Trim() -replace '\\', '/').ToLower().TrimEnd('/')
                break
            }
        }
    }

    $pattern = [regex]::Escape($guardSegment).Replace('/', '[/\\]')
    if ($normalized -match "(^|/)$pattern(/|$)") {
        @{
            permission   = 'deny'
            user_message = "[HOOK BLOCKED] Edits under protected app code are forbidden. File: $filePath"
            agent_message = "BLOCK: App code path '$guardSegment' is read-only for AI (Rule 0.8 Tier A)."
        } | ConvertTo-Json -Compress
    } else {
        @{ permission = 'allow' } | ConvertTo-Json -Compress
    }
} catch {
    @{
        permission   = 'deny'
        user_message = '[HOOK ERROR] protect-app-code failed; blocked for safety.'
        agent_message = "BLOCK: $_"
    } | ConvertTo-Json -Compress
}
