<#
.SYNOPSIS
    Searches Confluence Cloud via CQL (Confluence Query Language) using REST API.

.DESCRIPTION
    Executes a CQL query against the Confluence Cloud search endpoint and returns
    matching pages with space and version info. Credentials and wiki base URL are
    resolved from the toolkit .env file.

.PARAMETER Cql
    CQL query string (e.g. 'type=page AND space=MYSPACE AND title~"search term"').

.PARAMETER Limit
    Max results to return (default: 15).

.PARAMETER OutFile
    Optional path to write JSON. If omitted, prints to stdout.

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File cql-search.ps1 -Cql 'type=page AND title~"API documentation"'

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File cql-search.ps1 -Cql 'space=DEV AND label="release-notes"' -Limit 25
#>
param(
    [Parameter(Mandatory = $true)][string]$Cql,
    [int]$Limit = 15,
    [string]$OutFile = ''
)

if (-not $PSScriptRoot) {
    $PSScriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
}

$toolkitRoot = Split-Path -Parent $PSScriptRoot

function Get-EnvValue {
    param([string]$VarName)

    $val = [System.Environment]::GetEnvironmentVariable($VarName)
    if ($val) { return $val }

    $envPaths = @(
        (Join-Path $toolkitRoot '.env')
    )

    foreach ($envPath in $envPaths) {
        if (Test-Path -LiteralPath $envPath) {
            $lines = Get-Content -LiteralPath $envPath -ErrorAction SilentlyContinue
            foreach ($line in $lines) {
                $trimmed = $line.Trim()
                if (-not $trimmed -or $trimmed.StartsWith('#')) { continue }
                if ($trimmed -match "^\s*$VarName\s*=\s*['""]?(.+?)['""]?\s*$") {
                    return $Matches[1]
                }
            }
        }
    }
    return $null
}

function Get-WikiBase {
    $explicit = Get-EnvValue 'CONFLUENCE_WIKI_BASE'
    if ($explicit) {
        return $explicit.TrimEnd('/')
    }

    $url = Get-EnvValue 'CONFLUENCE_URL'
    if ($url) {
        $u = $url.Trim().TrimEnd('/')
        if ($u -match '^(https://[^/]+/wiki)') {
            return $Matches[1]
        }
        if ($u -match '^(https://[^/]+)$') {
            return "$($Matches[1])/wiki"
        }
    }

    $jiraBase = Get-EnvValue 'JIRA_BASE_URL'
    if ($jiraBase) {
        $jb = $jiraBase.Trim().TrimEnd('/')
        if ($jb -match '^(https://[^/]+\.atlassian\.net)$') {
            return "$($Matches[1])/wiki"
        }
    }
    return $null
}

$email = Get-EnvValue 'CONFLUENCE_EMAIL'
if (-not $email) { $email = Get-EnvValue 'JIRA_EMAIL' }

$token = Get-EnvValue 'CONFLUENCE_API_TOKEN'
if (-not $token) { $token = Get-EnvValue 'JIRA_API_TOKEN' }

if (-not $email -or -not $token) {
    Write-Error 'Missing credentials: set CONFLUENCE_EMAIL+CONFLUENCE_API_TOKEN or JIRA_EMAIL+JIRA_API_TOKEN in .env'
    Write-Host 'Hint: copy .env.example to .env and fill in your credentials.'
    exit 1
}

$wikiBase = Get-WikiBase
if (-not $wikiBase) {
    Write-Error 'Missing wiki base URL: set CONFLUENCE_WIKI_BASE, CONFLUENCE_URL, or JIRA_BASE_URL in .env'
    exit 1
}

$pair  = "${email}:${token}"
$bytes = [System.Text.Encoding]::UTF8.GetBytes($pair)
$b64   = [Convert]::ToBase64String($bytes)
$headers = @{
    'Authorization' = "Basic $b64"
    'Accept'        = 'application/json'
}

$enc = [Uri]::EscapeDataString($Cql)
$searchUrl = "${wikiBase}/rest/api/content/search?cql=$enc&limit=$Limit&expand=space,version"

Write-Host "Searching Confluence: $Cql (limit=$Limit) ..."

try {
    $raw = Invoke-WebRequest -Uri $searchUrl -Headers $headers -UseBasicParsing
    $content = $raw.Content

    if ($OutFile) {
        $content | Set-Content -LiteralPath $OutFile -Encoding UTF8
        Write-Host "Wrote $OutFile"
    }
    else {
        Write-Output $content
    }
}
catch {
    Write-Error "Confluence search failed: $($_.Exception.Message)"
    if ($_.ErrorDetails.Message) {
        Write-Error $_.ErrorDetails.Message
    }
    exit 1
}
