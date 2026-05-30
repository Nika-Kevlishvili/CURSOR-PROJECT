# warn-phoenix-code-edit.ps1
# Hook: afterFileEdit
# Purpose: Warn if any Phoenix file was edited (backup safety check)
# Rule: 0.8 - matches protect-phoenix-code.ps1 (all file types under Phoenix/**)

$jsonInput = [Console]::In.ReadToEnd()

try {
    $input = $jsonInput | ConvertFrom-Json
    $filePath = $input.file_path

    if (-not $filePath) {
        exit 0
    }

    $normalizedPathLower = ($filePath -replace '\\', '/').ToLower()
    $isPhoenixPath = ($normalizedPathLower -match "(^|/)cursor-project/phoenix/")

    if ($isPhoenixPath) {
        Write-Host "[WARNING] Phoenix file was edited: $filePath" -ForegroundColor Red
        Write-Host "Rule 0.8: Any modification under Cursor-Project/Phoenix/ is STRICTLY FORBIDDEN" -ForegroundColor Yellow
        Write-Host "Please review the changes and consider reverting if this was unintentional." -ForegroundColor Yellow
    }
} catch {
    # Silent fail for after hooks
}
