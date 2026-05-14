<#
.SYNOPSIS
    Fetches a single Confluence Cloud wiki page via REST (read-only).

.DESCRIPTION
    Uses Confluence Cloud REST API with Basic Auth to fetch a page by its numeric
    page ID. Supports both v1 (rest/api/content) and v2 (api/v2/pages) endpoints.
    Credentials and wiki base URL are resolved from the toolkit .env file.

    Wiki base URL resolution order:
      1. CONFLUENCE_WIKI_BASE (explicit)
      2. CONFLUENCE_URL (normalized to .../wiki)
      3. JIRA_BASE_URL + /wiki (same Atlassian Cloud site)

.PARAMETER PageId
    Numeric Confluence page ID (from URL .../pages/<id>/...).

.PARAMETER Api
    v1 = rest/api/content with full expand (default).
    v2 = api/v2/pages with body-format=storage.

.PARAMETER OutFile
    Optional path to write JSON. If omitted, prints to stdout.

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File get-page.ps1 -PageId '779517953'

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File get-page.ps1 -PageId '779517953' -Api v2 -OutFile page.json
#>
param(
    [Parameter(Mandatory = $true)][string]$PageId,
    [ValidateSet('v1', 'v2')][string]$Api = 'v1',
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

function Get-BasicAuthHeader {
    $email = Get-EnvValue 'CONFLUENCE_EMAIL'
    if (-not $email) { $email = Get-EnvValue 'JIRA_EMAIL' }

    $token = Get-EnvValue 'CONFLUENCE_API_TOKEN'
    if (-not $token) { $token = Get-EnvValue 'JIRA_API_TOKEN' }

    if (-not $email -or -not $token) {
        Write-Error 'Missing credentials: set CONFLUENCE_EMAIL+CONFLUENCE_API_TOKEN or JIRA_EMAIL+JIRA_API_TOKEN in .env'
        Write-Host 'Hint: copy .env.example to .env and fill in your credentials.'
        exit 2
    }

    $pair  = "${email}:${token}"
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($pair)
    $b64   = [Convert]::ToBase64String($bytes)
    return @{ Authorization = "Basic $b64"; Accept = 'application/json' }
}

$wikiBase = Get-WikiBase
if (-not $wikiBase) {
    Write-Error 'Missing wiki base URL: set CONFLUENCE_WIKI_BASE, CONFLUENCE_URL, or JIRA_BASE_URL in .env'
    Write-Host 'Hint: copy .env.example to .env and fill in your credentials.'
    exit 2
}

$headers = Get-BasicAuthHeader
if (-not $headers) { exit 2 }

Write-Host "Fetching Confluence page $PageId from $wikiBase ..."

try {
    if ($Api -eq 'v1') {
        $expand = 'body.storage,body.view,version,space,ancestors,metadata.labels,children.page'
        $enc = [System.Uri]::EscapeDataString($expand)
        $uri = "$wikiBase/rest/api/content/$PageId" + "?expand=$enc"
    }
    else {
        $uri = "$wikiBase/api/v2/pages/$PageId" + '?body-format=storage'
    }

    $response = Invoke-RestMethod -Uri $uri -Headers $headers -Method Get -ErrorAction Stop
    $json = $response | ConvertTo-Json -Depth 30

    if ($OutFile) {
        $json | Set-Content -LiteralPath $OutFile -Encoding UTF8
        Write-Host "Wrote $OutFile"
    }
    else {
        Write-Output $json
    }
}
catch {
    Write-Error "Confluence REST request failed: $($_.Exception.Message)"
    if ($_.ErrorDetails.Message) {
        Write-Error $_.ErrorDetails.Message
    }
    exit 1
}
