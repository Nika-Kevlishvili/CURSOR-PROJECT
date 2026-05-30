# warn-phoenix-code-edit.ps1
# Hook: afterFileEdit
# Purpose: Warn if Phoenix code files were edited (backup safety check)
# Rule: 0.8 - Code Modification is STRICTLY FORBIDDEN

$jsonInput = [Console]::In.ReadToEnd()

try {
    $input = $jsonInput | ConvertFrom-Json
    $filePath = $input.file_path

    if (-not $filePath) {
        exit 0
    }

    $normalizedPathLower = ($filePath -replace '\\', '/').ToLower()

    # Match protect-phoenix-code.ps1: only Cursor-Project/Phoenix/**
    $isPhoenixPath = ($normalizedPathLower -match "(^|/)cursor-project/phoenix/")

    $codeExtensions = @(".java", ".ts", ".js", ".tsx", ".jsx", ".py", ".html", ".css", ".xml", ".yaml", ".yml", ".properties", ".sql", ".kt", ".scala", ".groovy")

    $isCodeFile = $false
    foreach ($ext in $codeExtensions) {
        if ($normalizedPathLower.EndsWith($ext)) {
            $isCodeFile = $true
            break
        }
    }

    if ($isPhoenixPath -and $isCodeFile) {
        Write-Host "[WARNING] Phoenix code file was edited: $filePath" -ForegroundColor Red
        Write-Host "Rule 0.8: Code modification in Phoenix project is STRICTLY FORBIDDEN" -ForegroundColor Yellow
        Write-Host "Please review the changes and consider reverting if this was unintentional." -ForegroundColor Yellow
    }
} catch {
    # Silent fail for after hooks
}
