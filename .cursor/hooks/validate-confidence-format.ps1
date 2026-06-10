# validate-confidence-format.ps1
# Hook: afterAgentResponse
# Purpose: Non-blocking validation that workflow responses include proper Three-Zone confidence format.
# Rules: CONF.1 — does NOT block the response, only warns agent if format is missing.

$jsonInput = [Console]::In.ReadToEnd()

try {
    $hookPayload = $jsonInput | ConvertFrom-Json
    $response = $hookPayload.response

    if (-not $response) {
        @{ continue = $true } | ConvertTo-Json -Compress
        exit 0
    }

    $workflowIndicators = @(
        'Agents involved:',
        'bug validation', 'Bug Validation', 'BugFinder',
        'test case', 'Test Case', 'TC-BE-', 'TC-FE-',
        'HandsOff', 'handsoff',
        'PhoenixExpert',
        'DB environment:', 'ProductionDataReader',
        'CrossDependencyFinder',
        'EnergoTSTestAgent'
    )

    $isWorkflowResponse = $false
    foreach ($indicator in $workflowIndicators) {
        if ($response.Contains($indicator)) {
            $isWorkflowResponse = $true
            break
        }
    }

    if (-not $isWorkflowResponse) {
        @{ continue = $true } | ConvertTo-Json -Compress
        exit 0
    }

    $hasThreeZoneFormat = $response -match '\*\*Confidence:\s*\d+%\s*\((GO|CAUTION|STOP)\)\*\*'
    $hasEvidenceList = $response -match 'Evidence:\s*\['
    $hasOldFormat = ($response -match '\*\*Confidence:\s*\d+%\*\*') -and (-not $hasThreeZoneFormat)

    if ($hasThreeZoneFormat -and $hasEvidenceList) {
        @{ continue = $true } | ConvertTo-Json -Compress
        exit 0
    }

    $warnings = @()

    if ($hasOldFormat) {
        $warnings += "Uses OLD confidence format (missing zone). Required: **Confidence: XX% (GO|CAUTION|STOP)**"
    }
    elseif (-not $hasThreeZoneFormat) {
        $warnings += "Missing confidence score. Rule CONF.1 requires: **Confidence: XX% (ZONE)** for workflow responses."
    }

    if ($hasThreeZoneFormat -and (-not $hasEvidenceList)) {
        $warnings += "Missing Evidence factors list. Required: Evidence: [+factor1, -factor2, ...]"
    }

    if ($response -match '\*\*Confidence:\s*(\d+)%\s*\(STOP\)') {
        $hasVerdict = ($response -match '\b(VALID|NOT VALID|NEEDS CLARIFICATION|NEEDS APPROVAL)\b') -or
                      ($response -match '## (Backend |Frontend )?Test Cases')
        if ($hasVerdict) {
            $warnings += "STOP zone violation: verdict/deliverable found at < 55% confidence. Rule CONF.1.6 — MUST NOT deliver."
        }
    }

    if ($response -match '\*\*Confidence:\s*(\d+)%\s*\(CAUTION\)') {
        $hasAssumptions = $response -match 'Assumptions:\s*\['
        $hasVerifyList = $response -match 'Recommend user verify:'
        if (-not $hasAssumptions -and -not $hasVerifyList) {
            $warnings += "CAUTION zone missing Assumptions list and/or 'Recommend user verify:' items."
        }
    }

    if ($warnings.Count -gt 0) {
        $msg = "[CONF.1 HOOK] " + ($warnings -join " | ") + " See: .cursor/rules/scoring/confidence_scoring_matrix.mdc"
        @{
            continue = $true
            additional_context = $msg
        } | ConvertTo-Json -Compress
    }
    else {
        @{ continue = $true } | ConvertTo-Json -Compress
    }
}
catch {
    @{ continue = $true } | ConvertTo-Json -Compress
}
