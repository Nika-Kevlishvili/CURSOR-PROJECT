# remind-test-case-env-first.ps1
# Hook: beforeSubmitPrompt
# Purpose: Non-blocking reminder when user requests test case generation without naming environment.
# Rules: TC-ENV-ASK.0, CONF.0, Rule 35 — does NOT block the prompt.

$jsonInput = [Console]::In.ReadToEnd()

try {
    $hookPayload = $jsonInput | ConvertFrom-Json
    $prompt = $hookPayload.prompt

    if (-not $prompt) {
        @{ continue = $true } | ConvertTo-Json -Compress
        exit 0
    }

    $promptLower = $prompt.ToLower()

    # Test-case generation intent (EN + common Georgian transliteration)
    $tcPatterns = @(
        'test case', 'test cases', 'test-case', 'test_case',
        'generate tc', 'ტესტ ქეის', 'testcase'
    )
    $hasTcIntent = $false
    foreach ($p in $tcPatterns) {
        if ($promptLower.Contains($p)) {
            $hasTcIntent = $true
            break
        }
    }

    if (-not $hasTcIntent) {
        @{ continue = $true } | ConvertTo-Json -Compress
        exit 0
    }

    # Explicit environment in prompt (avoid matching "test" inside "test case")
    $scrubbed = $promptLower -replace 'test\s*cases?', ' '
    $scrubbed = $scrubbed -replace 'test-case', ' '

    $hasExplicitEnv = $false
    if ($scrubbed -match '\b(dev2|dev|preprod|prod|experiments)\b') {
        $hasExplicitEnv = $true
    }
    elseif ($scrubbed -match '\btest\b') {
        $hasExplicitEnv = $true
    }
    elseif ($promptLower -match 'environment\s*[:=]\s*(dev2|dev|test|preprod|prod|experiments)') {
        $hasExplicitEnv = $true
    }
    elseif ($promptLower -match '\b(on|in|for|using)\s+(dev2|dev|test|preprod|prod|experiments)\b') {
        $hasExplicitEnv = $true
    }

    if ($hasExplicitEnv) {
        @{ continue = $true } | ConvertTo-Json -Compress
        exit 0
    }

    $msg = 'REMINDER (Rule TC-ENV-ASK.0 / CONF.0 / Rule 35): Test case request without named environment. Order: (1) Ask Dev/Dev2/Test/PreProd/Prod/Experiments via AskQuestion or environment-resolver — no inference from Jira status/fix version; (2) TC-FRONTEND-ASK.0 if needed; (3) switch-phoenix-branches then cross-dep. Jira read-only OK before (1); no Phoenix grep/branch switch before env confirmed in chat.'

    @{
        continue      = $true
        agent_message = $msg
    } | ConvertTo-Json -Compress
}
catch {
    @{ continue = $true } | ConvertTo-Json -Compress
}
