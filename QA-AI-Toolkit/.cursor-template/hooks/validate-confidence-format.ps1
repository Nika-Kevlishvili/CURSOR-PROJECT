# validate-confidence-format.ps1 — afterAgentResponse (non-blocking)
$jsonInput = [Console]::In.ReadToEnd()
try {
    $hookPayload = $jsonInput | ConvertFrom-Json
    $response = [string]$hookPayload.response
    if (-not $response) {
        @{ continue = $true } | ConvertTo-Json -Compress
        exit 0
    }
    $indicators = @(
        'Agents involved:',
        'bug validation', 'Bug Validation',
        'test case', 'Test Case', 'TC-BE-', 'TC-FE-',
        'ProductExpert', 'CrossDependencyFinder'
    )
    $isWorkflow = $false
    foreach ($i in $indicators) {
        if ($response.Contains($i)) { $isWorkflow = $true; break }
    }
    if (-not $isWorkflow) {
        @{ continue = $true } | ConvertTo-Json -Compress
        exit 0
    }
    $hasZone = $response -match '\*\*Confidence:\s*\d+%\s*\((GO|CAUTION|STOP)\)\*\*'
    $hasEvidence = $response -match 'Evidence:\s*\['
    $warnings = @()
    if (-not $hasZone) {
        $warnings += 'Missing **Confidence: XX% (GO|CAUTION|STOP)** (CONF.1).'
    }
    if ($hasZone -and -not $hasEvidence) {
        $warnings += 'Missing Evidence: [...] list.'
    }
    if ($warnings.Count -gt 0) {
        @{
            continue = $true
            agentMessage = '[CONF.1 HOOK] ' + ($warnings -join ' | ')
        } | ConvertTo-Json -Compress
    } else {
        @{ continue = $true } | ConvertTo-Json -Compress
    }
} catch {
    @{ continue = $true } | ConvertTo-Json -Compress
}
