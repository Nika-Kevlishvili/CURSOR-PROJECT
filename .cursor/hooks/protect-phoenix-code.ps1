# protect-phoenix-code.ps1
# Hook: beforeFileEdit
# Purpose: Block only Phoenix code file edits
# Rule: 0.8 - Code Modification is STRICTLY FORBIDDEN

$jsonInput = [Console]::In.ReadToEnd()

try {
    $hookInput = $jsonInput | ConvertFrom-Json
    $filePath = $hookInput.file_path
    
    # Normalize path for matching (handle both absolute and relative paths)
    $normalizedPath = $filePath -replace '\\', '/'  # Normalize separators
    $normalizedPathLower = $normalizedPath.ToLower()
    
    # Only protect files inside Cursor-Project/Phoenix/**
    $isPhoenixPath = ($normalizedPathLower -match "(^|/)cursor-project/phoenix/")
    
    # Code file extensions
    $codeExtensions = @(
        ".java", ".ts", ".js", ".tsx", ".jsx", ".py", 
        ".html", ".css", ".xml", ".yaml", ".yml", 
        ".properties", ".sql", ".kt", ".scala", 
        ".groovy", ".scss", ".less"
    )
    
    $isCodeFile = $false
    
    # Check if file has code extension
    foreach ($ext in $codeExtensions) {
        if ($normalizedPathLower.EndsWith($ext)) {
            $isCodeFile = $true
            break
        }
    }
    
    # Block only Phoenix code file edits
    if ($isPhoenixPath -and $isCodeFile) {
        $response = @{
            permission = "deny"
            user_message = "[HOOK BLOCKED] Code modification in Phoenix project is FORBIDDEN (Rule 0.8). File: $filePath"
            agent_message = "CRITICAL: Code modification was blocked. Rule 0.8 states that code modification is STRICTLY FORBIDDEN. The file '$filePath' is inside Cursor-Project/Phoenix and has a code extension."
        }
    } else {
        $response = @{ permission = "allow" }
    }
    
    $response | ConvertTo-Json -Compress
} catch {
    # On error, deny by default for safety (fail-secure)
    $response = @{
        permission = "deny"
        user_message = "[HOOK ERROR] Error processing file protection hook. Blocked for safety. File: $filePath"
        agent_message = "CRITICAL: Hook error occurred. Blocked file edit for safety. Error: $_"
    }
    $response | ConvertTo-Json -Compress
}
