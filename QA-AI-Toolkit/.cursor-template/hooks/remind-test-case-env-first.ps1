# remind-test-case-env-first.ps1 — beforeSubmitPrompt (non-blocking)
$jsonInput = [Console]::In.ReadToEnd()
try {
    $hookPayload = $jsonInput | ConvertFrom-Json
    $prompt = [string]$hookPayload.prompt
    if (-not $prompt) {
        @{ continue = $true } | ConvertTo-Json -Compress
        exit 0
    }
    $promptLower = $prompt.ToLowerInvariant()
    $tcPatterns = @('test case', 'test cases', 'test-case', 'test_case', 'generate tc')
    $hasTc = $false
    foreach ($p in $tcPatterns) {
        if ($promptLower.Contains($p)) { $hasTc = $true; break }
    }
    if (-not $hasTc) {
        @{ continue = $true } | ConvertTo-Json -Compress
        exit 0
    }
    $cfgPath = Join-Path (Get-Location) '.cursor\project-config.json'
    $envHint = 'an environment from .cursor/project-config.json'
    if (Test-Path $cfgPath) {
        try {
            $cfg = Get-Content -LiteralPath $cfgPath -Raw | ConvertFrom-Json
            $ids = @($cfg.environments | ForEach-Object { $_.id })
            if ($ids.Count -gt 0) { $envHint = ($ids -join '/') }
            $named = $false
            foreach ($id in $ids) {
                if ($promptLower -match [regex]::Escape($id.ToLowerInvariant())) { $named = $true; break }
            }
            if ($named -or $promptLower -match 'environment') {
                @{ continue = $true } | ConvertTo-Json -Compress
                exit 0
            }
        } catch {}
    }
    @{
        continue = $true
        agentMessage = "REMINDER (TC-ENV-ASK.0): Test case request without named environment. Ask which env ($envHint) before generating."
    } | ConvertTo-Json -Compress
} catch {
    @{ continue = $true } | ConvertTo-Json -Compress
}
