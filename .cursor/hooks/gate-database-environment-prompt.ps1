# gate-database-environment-prompt.ps1
# Hook: beforeSubmitPrompt
# Rule DB.0a — block DB questions without env; persist user-named env in .cursor/.db-env-session.json

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot/lib/db-env-session.ps1"

$jsonInput = [Console]::In.ReadToEnd()

function Test-HasDatabaseEnvironment {
    param([string]$Text)
    return [bool](Get-CanonicalDbEnvironmentFromText -Text $Text)
}

function Test-NeedsLiveDatabase {
    param([string]$Text)
    if ([string]::IsNullOrWhiteSpace($Text)) { return $false }
    $t = $Text.ToLowerInvariant()

    if ($t -match '\b(select|insert|update|delete)\b') { return $true }
    if ($t -match '\b(database|postgres|postgresql|run (the )?query|sql)\b') { return $true }
    if ($t -match '\bproduction (data|db|database)\b') { return $true }

    $lookup = $t -match '\b(which|list|show|find|lookup|used in|linked to)\b'
    $entity = $t -match '\b(reminder|disconnection|request for disconnection|rfd|pod|contract|liability|receivable|invoice|payment|deposit)\b'
    $idHint = $t -match '\b\d{3,}\b'

    if ($lookup -and ($entity -or $idHint)) { return $true }
    if ($entity -and $idHint) { return $true }

    return $false
}

try {
    $input = $jsonInput | ConvertFrom-Json
    $prompt = [string]$input.prompt

    if (-not $prompt) {
        @{ continue = $true } | ConvertTo-Json -Compress
        exit 0
    }

    $canonicalEnv = Get-CanonicalDbEnvironmentFromText -Text $prompt
    $needsDb = Test-NeedsLiveDatabase -Text $prompt

    if ($needsDb -and -not $canonicalEnv) {
        Clear-DbEnvSession
        @{
            continue      = $false
            block         = $true
            user_message  = '[DB ENVIRONMENT REQUIRED - Rule DB.0a] This question needs live database data, but no environment was specified. Add one of: Dev, Dev2, Test, PreProd, Prod, or Experiments. Example: "On Test - which Request for Disconnection uses reminder 2163?"'
            agent_message = 'CRITICAL (Rule DB.0a): User prompt requires DB evidence but environment is missing. Do NOT call PostgreSQL MCP. Do NOT infer Dev/Test from PDT tickets or EnergoTS tests (e.g. PDT-2529 default reminder 2163 on Dev). Ask which environment or wait for user to resubmit with environment in the same message.'
        } | ConvertTo-Json -Compress
        exit 0
    }

    if ($canonicalEnv) {
        Set-DbEnvSession -CanonicalEnvironment $canonicalEnv -PromptSnippet $prompt
    }

    @{ continue = $true } | ConvertTo-Json -Compress
} catch {
    @{ continue = $true } | ConvertTo-Json -Compress
}
