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
    
    # Normalize path for matching
    $normalizedPath = $filePath -replace '\\', '/'
    $normalizedPathLower = $normalizedPath.ToLower()
    
    # Protected paths
    $protectedPaths = @("cursor-project", "phoenix")
    
    # Code file extensions
    $codeExtensions = @(".java", ".ts", ".js", ".tsx", ".jsx", ".py", ".html", ".css", ".xml", ".yaml", ".yml", ".properties", ".sql", ".kt", ".scala", ".groovy")
    
    $isProtectedPath = $false
    $isCodeFile = $false
    
    # Check if path contains protected directory
    foreach ($path in $protectedPaths) {
        if ($normalizedPathLower -match [regex]::Escape($path)) {
            $isProtectedPath = $true
            break
        }
    }
    
    # Check if file has code extension
    foreach ($ext in $codeExtensions) {
        if ($normalizedPathLower.EndsWith($ext)) {
            $isCodeFile = $true
            break
        }
    }
    
    # Warn if Phoenix code was edited (this should not happen if beforeFileEdit worked)
    if ($isProtectedPath -and $isCodeFile) {
        Write-Host "[WARNING] Phoenix code file was edited: $filePath" -ForegroundColor Red
        Write-Host "Rule 0.8: Code modification in Phoenix project is STRICTLY FORBIDDEN" -ForegroundColor Yellow
        Write-Host "Please review the changes and consider reverting if this was unintentional." -ForegroundColor Yellow
    }
} catch {
    # Silent fail for after hooks
}
