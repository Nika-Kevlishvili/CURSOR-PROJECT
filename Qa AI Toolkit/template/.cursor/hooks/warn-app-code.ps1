# warn-app-code-edit.ps1 — warn if protected app code path was edited
$jsonInput = [Console]::In.ReadToEnd()
try {
    $input = $jsonInput | ConvertFrom-Json
    $filePath = $input.file_path
    if (-not $filePath) { exit 0 }
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
        Write-Host "[WARNING] Protected app code edited: $filePath" -ForegroundColor Red
    }
} catch { }
