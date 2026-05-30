# protect-energots-writes.ps1
# Hook: beforeFileEdit
# Purpose: Block EnergoTS code edits outside tests/ (Rule 0.8 Tier B)
# Only Cursor-Project/EnergoTS/tests/** may be written by energo-ts-test agent workflow

$jsonInput = [Console]::In.ReadToEnd()

try {
    $hookInput = $jsonInput | ConvertFrom-Json
    $filePath = $hookInput.file_path

    $normalizedPath = ($filePath -replace '\\', '/').ToLower()

    $isEnergoTSPath = ($normalizedPath -match "(^|/)cursor-project/energots/")
    $isTestsPath = ($normalizedPath -match "(^|/)cursor-project/energots/tests/")

    $codeExtensions = @(
        ".java", ".ts", ".js", ".tsx", ".jsx", ".py",
        ".html", ".css", ".xml", ".yaml", ".yml",
        ".properties", ".sql", ".kt", ".scala",
        ".groovy", ".scss", ".less", ".json", ".mjs"
    )

    $isCodeFile = $false
    foreach ($ext in $codeExtensions) {
        if ($normalizedPath.EndsWith($ext)) {
            $isCodeFile = $true
            break
        }
    }

    if ($isEnergoTSPath -and -not $isTestsPath -and $isCodeFile) {
        $response = @{
            permission = "deny"
            user_message = "[HOOK BLOCKED] EnergoTS edits outside tests/ are forbidden (Rule 0.8 Tier B). File: $filePath"
            agent_message = "CRITICAL: Only EnergoTSTestAgent may write under Cursor-Project/EnergoTS/tests/. The file '$filePath' is outside tests/."
        }
    } else {
        $response = @{ permission = "allow" }
    }

    $response | ConvertTo-Json -Compress
} catch {
    $response = @{
        permission = "deny"
        user_message = "[HOOK ERROR] EnergoTS protection hook failed. Blocked for safety."
        agent_message = "CRITICAL: protect-energots-writes hook error: $_"
    }
    $response | ConvertTo-Json -Compress
}
