# protect-energots-writes.ps1
# Hook: beforeFileEdit
# Purpose: Block EnergoTS edits outside tests/; inside tests/ allow only Playwright test artifacts (Rule 0.8.1)

$jsonInput = [Console]::In.ReadToEnd()

try {
    $hookInput = $jsonInput | ConvertFrom-Json
    $filePath = $hookInput.file_path

    $normalizedPathLower = ($filePath -replace '\\', '/').ToLower()

    $isEnergoTSPath = ($normalizedPathLower -match "(^|/)cursor-project/energots/")
    $isTestsPath = ($normalizedPathLower -match "(^|/)cursor-project/energots/tests/")

    $allowedTestSuffixes = @('.spec.ts', '.fixtures.ts')
    $isAllowedTestArtifact = $false
    foreach ($suffix in $allowedTestSuffixes) {
        if ($normalizedPathLower.EndsWith($suffix)) {
            $isAllowedTestArtifact = $true
            break
        }
    }

    if ($isEnergoTSPath -and -not $isTestsPath) {
        $response = @{
            permission = "deny"
            user_message = "[HOOK BLOCKED] EnergoTS edits outside tests/ are forbidden (Rule 0.8 Tier B). File: $filePath"
            agent_message = "BLOCK: Only EnergoTSTestAgent may write under Cursor-Project/EnergoTS/tests/ (*.spec.ts, *.fixtures.ts). File '$filePath' is outside tests/."
        }
    } elseif ($isTestsPath -and -not $isAllowedTestArtifact) {
        $response = @{
            permission = "deny"
            user_message = "[HOOK BLOCKED] EnergoTS tests/ allows only *.spec.ts and *.fixtures.ts (Rule 0.8.1). File: $filePath"
            agent_message = "BLOCK: EnergoTS tests/ write denied — not a Playwright spec or fixtures file. Route to energo-ts-test workflow for .spec.ts / .fixtures.ts only."
        }
    } else {
        $response = @{ permission = "allow" }
    }

    $response | ConvertTo-Json -Compress
} catch {
    $response = @{
        permission = "deny"
        user_message = "[HOOK ERROR] EnergoTS protection hook failed. Blocked for safety."
        agent_message = "BLOCK: protect-energots-writes hook error: $_"
    }
    $response | ConvertTo-Json -Compress
}
