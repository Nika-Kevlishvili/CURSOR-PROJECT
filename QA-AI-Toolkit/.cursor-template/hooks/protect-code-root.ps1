# protect-code-root.ps1 — afterFileEdit
# Warn/deny edits under codeRoot / protectedPaths from project-config.json
$jsonInput = [Console]::In.ReadToEnd()
try {
    $input = $jsonInput | ConvertFrom-Json
    $filePath = [string]($input.file_path)
    if (-not $filePath) { $filePath = [string]$input.path }
    $cfgPath = Join-Path (Get-Location) '.cursor\project-config.json'
    $protected = @()
    if (Test-Path $cfgPath) {
        $cfg = Get-Content -LiteralPath $cfgPath -Raw | ConvertFrom-Json
        if ($cfg.codeRoot) { $protected += [string]$cfg.codeRoot }
        if ($cfg.protectedPaths) { $protected += @($cfg.protectedPaths | ForEach-Object { [string]$_ }) }
    }
    $norm = $filePath -replace '/', '\'
    $hit = $false
    foreach ($p in $protected) {
        if ([string]::IsNullOrWhiteSpace($p)) { continue }
        $prefix = ($p -replace '/', '\').TrimEnd('\')
        if ($norm -match [regex]::Escape($prefix)) { $hit = $true; break }
    }
    # Allow edits under .cursor, docs, reports, test_cases, config even if nested oddly
    if ($norm -match '\\.cursor\\' -or $norm -match '\\docs\\' -or $norm -match '\\reports\\' -or $norm -match '\\test_cases\\' -or $norm -match '\\config\\') {
        $hit = $false
    }
    if ($hit) {
        @{
            continue = $true
            permission = 'deny'
            user_message = "[HOOK BLOCKED] Edits under protected code root are forbidden by default (Rule 0.8). File: $filePath"
            agent_message = 'BLOCK: Do not modify application source under codeRoot/protectedPaths. Analyze and recommend only.'
        } | ConvertTo-Json -Compress
    } else {
        @{ continue = $true; permission = 'allow' } | ConvertTo-Json -Compress
    }
} catch {
    @{ continue = $true; permission = 'allow' } | ConvertTo-Json -Compress
}
