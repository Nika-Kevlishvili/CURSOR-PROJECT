# Shared DB environment session helpers (Rule DB.0a)
# Session is written when the USER names an environment in a submitted prompt.
# PostgreSQL MCP is allowed only while session matches the MCP server (TTL 2 hours).

$script:DbEnvSessionRelativePath = '.cursor/.db-env-session.json'
$script:DbEnvSessionTtlMinutes = 120

function Get-DbEnvSessionPath {
    $root = $env:CURSOR_PROJECT_DIR
    if ([string]::IsNullOrWhiteSpace($root)) {
        $root = (Get-Location).Path
    }
    return (Join-Path $root $script:DbEnvSessionRelativePath)
}

function Get-CanonicalDbEnvironmentFromText {
    param([string]$Text)
    if ([string]::IsNullOrWhiteSpace($Text)) { return $null }
    $t = $Text.ToLowerInvariant()
    if ($t -match '\b(your choice|use default|same as last time|default env)\b') { return $null }
    if ($t -match '\b(postgresqldev2|user-postgresqldev2)\b') { return 'dev2' }
    if ($t -match '\bdev2\b') { return 'dev2' }
    if ($t -match '\b(postgresqltest|user-postgresqltest)\b') { return 'test' }
    if ($t -match '\btest\b') { return 'test' }
    if ($t -match '\b(postgresqlpreprod|user-postgresqlpreprod|preprod|pre-prod)\b') { return 'preprod' }
    if ($t -match '\b(postgresqlprod|user-postgresqlprod)\b') { return 'prod' }
    if ($t -match '\b(prod|production)\b') { return 'prod' }
    if ($t -match '\b(postgresqlexperiments|user-postgresqlexperiments|experiments)\b') { return 'experiments' }
    if ($t -match '\b(postgresqldev|user-postgresqldev)\b') { return 'dev' }
    if ($t -match '\bdev\b') { return 'dev' }
    return $null
}

function Get-CanonicalDbEnvironmentFromMcpHaystack {
    param([string]$Haystack)
    if ([string]::IsNullOrWhiteSpace($Haystack)) { return $null }
    $h = $Haystack.ToLowerInvariant()
    if ($h -match 'postgresqldev2|user-postgresqldev2') { return 'dev2' }
    if ($h -match 'postgresqldev[^2]|user-postgresqldev[^2]') { return 'dev' }
    if ($h -match 'postgresqltest|user-postgresqltest') { return 'test' }
    if ($h -match 'postgresqlpreprod|user-postgresqlpreprod') { return 'preprod' }
    if ($h -match 'postgresqlprod|user-postgresqlprod') { return 'prod' }
    if ($h -match 'postgresqlexperiments|user-postgresqlexperiments') { return 'experiments' }
    return $null
}

function Get-DbEnvironmentDisplayLabel {
    param([string]$Canonical)
    switch ($Canonical) {
        'dev2' { return 'Dev2' }
        'dev' { return 'Dev' }
        'test' { return 'Test' }
        'preprod' { return 'PreProd' }
        'prod' { return 'Prod' }
        'experiments' { return 'Experiments' }
        default { return $Canonical }
    }
}

function Clear-DbEnvSession {
    $path = Get-DbEnvSessionPath
    if (Test-Path -LiteralPath $path) {
        Remove-Item -LiteralPath $path -Force -ErrorAction SilentlyContinue
    }
}

function Set-DbEnvSession {
    param(
        [Parameter(Mandatory = $true)][string]$CanonicalEnvironment,
        [string]$PromptSnippet = ''
    )
    $path = Get-DbEnvSessionPath
    $dir = Split-Path -Parent $path
    if (-not (Test-Path -LiteralPath $dir)) {
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
    }
    $payload = @{
        environment     = $CanonicalEnvironment
        label           = (Get-DbEnvironmentDisplayLabel -Canonical $CanonicalEnvironment)
        confirmedAtUtc  = (Get-Date).ToUniversalTime().ToString('o')
        promptSnippet   = if ($PromptSnippet.Length -gt 160) { $PromptSnippet.Substring(0, 160) } else { $PromptSnippet }
    }
    $payload | ConvertTo-Json -Compress | Set-Content -LiteralPath $path -Encoding UTF8
}

function Get-DbEnvSession {
    $path = Get-DbEnvSessionPath
    if (-not (Test-Path -LiteralPath $path)) { return $null }
    try {
        $raw = Get-Content -LiteralPath $path -Raw -Encoding UTF8
        $session = $raw | ConvertFrom-Json
        if (-not $session.environment) { return $null }
        if ($session.confirmedAtUtc) {
            $confirmed = [datetime]::Parse($session.confirmedAtUtc, $null, [Globalization.DateTimeStyles]::RoundtripKind)
            $age = ((Get-Date).ToUniversalTime() - $confirmed.ToUniversalTime()).TotalMinutes
            if ($age -gt $script:DbEnvSessionTtlMinutes) {
                Clear-DbEnvSession
                return $null
            }
        }
        return $session
    } catch {
        Clear-DbEnvSession
        return $null
    }
}
