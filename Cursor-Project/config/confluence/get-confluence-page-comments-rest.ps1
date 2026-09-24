<#
.SYNOPSIS
    Fetches Confluence Cloud page footer + inline comments (read-only) via REST.

.DESCRIPTION
    GB-1746 helper: current Atlassian MCP in this workspace often has no
    getConfluencePageFooterComments / getConfluencePageInlineComments tools.
    Use this script after (or with) get-confluence-page-rest.ps1 when agents
    must include page comments in evidence.

    Auth: CONFLUENCE_EMAIL+CONFLUENCE_API_TOKEN or JIRA_EMAIL+JIRA_API_TOKEN.
    Wiki base: CONFLUENCE_WIKI_BASE, CONFLUENCE_URL, or JIRA_BASE_URL (+ /wiki).
    Never log secrets.

.PARAMETER PageId
    Numeric Confluence page ID (from URL .../pages/<id>/...).

.PARAMETER OutFile
    Optional path to write combined JSON. Default: stdout summary + JSON.

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File get-confluence-page-comments-rest.ps1 -PageId '779517953'
#>
param(
    [Parameter(Mandatory = $true)][string]$PageId,
    [string]$OutFile = ''
)

$ErrorActionPreference = 'Stop'

function Get-EnvValue {
    param([string]$VarName)

    $val = [System.Environment]::GetEnvironmentVariable($VarName)
    if ($val) { return $val }

    $envPaths = @(
        (Join-Path $PSScriptRoot '..\..\..\.env'),
        (Join-Path $PSScriptRoot '..\..\.env'),
        (Join-Path $PSScriptRoot '..\..\EnergoTS\.env'),
        (Join-Path $PSScriptRoot '..\..\Cursor Setup\env.example')
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
        Write-Error 'Missing credentials: set CONFLUENCE_EMAIL+CONFLUENCE_API_TOKEN or JIRA_EMAIL+JIRA_API_TOKEN.'
        exit 2
    }

    $pair = "${email}:${token}"
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($pair)
    $b64 = [Convert]::ToBase64String($bytes)
    return @{ Authorization = "Basic $b64"; Accept = 'application/json' }
}

function Invoke-CommentGet {
    param(
        [string]$Uri,
        [hashtable]$Headers
    )
    try {
        return Invoke-RestMethod -Uri $Uri -Headers $Headers -Method Get -ErrorAction Stop
    }
    catch {
        return $null
    }
}

$wikiBase = Get-WikiBase
if (-not $wikiBase) {
    Write-Error 'Missing wiki base: set CONFLUENCE_WIKI_BASE, or CONFLUENCE_URL, or JIRA_BASE_URL (atlassian.net) for derivation.'
    exit 2
}

$headers = Get-BasicAuthHeader

function Get-HasUsefulBody {
    param($Comment)
    if (-not $Comment) { return $false }
    $body = $Comment.body
    if (-not $body) { return $false }
    if ($body -is [string] -and $body.Trim().Length -gt 0) { return $true }
    if ($body.storage -and $body.storage.value) { return $true }
    if ($body.value) { return $true }
    return $false
}

function Expand-CommentBodies {
    param(
        $ListResponse,
        [string]$Kind, # footer | inline
        [hashtable]$Headers,
        [string]$WikiBase
    )
    if (-not $ListResponse -or -not $ListResponse.results) { return $ListResponse }
    $expanded = @()
    foreach ($c in @($ListResponse.results)) {
        if (Get-HasUsefulBody $c) {
            $expanded += $c
            continue
        }
        $id = $c.id
        if (-not $id) {
            $expanded += $c
            continue
        }
        $detailUri = if ($Kind -eq 'footer') {
            "$WikiBase/api/v2/footer-comments/$id" + '?body-format=storage'
        }
        else {
            "$WikiBase/api/v2/inline-comments/$id" + '?body-format=storage'
        }
        $detail = Invoke-CommentGet -Uri $detailUri -Headers $Headers
        if ($detail) { $expanded += $detail } else { $expanded += $c }
    }
    $ListResponse.results = $expanded
    return $ListResponse
}

# Prefer Confluence Cloud REST v2 page comment endpoints; fall back to v1 child/footer paths.
# Request body-format on list; hydrate per-comment when list returns empty body.
$footerV2 = Invoke-CommentGet -Uri "$wikiBase/api/v2/pages/$PageId/footer-comments?limit=50&body-format=storage" -Headers $headers
$inlineV2 = Invoke-CommentGet -Uri "$wikiBase/api/v2/pages/$PageId/inline-comments?limit=50&body-format=storage" -Headers $headers
$footerV2 = Expand-CommentBodies -ListResponse $footerV2 -Kind footer -Headers $headers -WikiBase $wikiBase
$inlineV2 = Expand-CommentBodies -ListResponse $inlineV2 -Kind inline -Headers $headers -WikiBase $wikiBase

$footerV1 = $null
$inlineV1 = $null
if (-not $footerV2) {
    $footerV1 = Invoke-CommentGet -Uri "$wikiBase/rest/api/content/$PageId/footer/comment?expand=body.storage,version,history" -Headers $headers
}
if (-not $inlineV2) {
    # Inline comments are not always exposed as a simple child collection; v1 marker resolution is best-effort.
    $inlineV1 = Invoke-CommentGet -Uri "$wikiBase/rest/api/content/$PageId/child/comment?expand=body.storage,version&limit=50" -Headers $headers
}

$result = [ordered]@{
    pageId           = $PageId
    wikiBase         = $wikiBase
    fetchedAt        = (Get-Date).ToString('o')
    footerCommentsV2 = $footerV2
    inlineCommentsV2 = $inlineV2
    footerCommentsV1 = $footerV1
    childCommentsV1  = $inlineV1
}

$footerCount = 0
$inlineCount = 0
if ($footerV2 -and $footerV2.results) { $footerCount = @($footerV2.results).Count }
elseif ($footerV1 -and $footerV1.results) { $footerCount = @($footerV1.results).Count }
if ($inlineV2 -and $inlineV2.results) { $inlineCount = @($inlineV2.results).Count }

$result['summary'] = [ordered]@{
    footerCommentCount = $footerCount
    inlineCommentCount = $inlineCount
    status             = if ($footerV2 -or $inlineV2 -or $footerV1 -or $inlineV1) { 'ok' } else { 'empty_or_unavailable' }
}

$json = ($result | ConvertTo-Json -Depth 30)

Write-Host "PageId: $PageId"
Write-Host "Footer comments: $footerCount"
Write-Host "Inline comments: $inlineCount"
Write-Host "Status: $($result.summary.status)"

if ($OutFile) {
    $json | Set-Content -LiteralPath $OutFile -Encoding UTF8
    Write-Host "Wrote $OutFile"
}
else {
    Write-Output $json
}
