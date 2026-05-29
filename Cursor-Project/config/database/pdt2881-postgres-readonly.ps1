# Read-only PostgreSQL helper for PDT-2881 Playwright assertions (Dev).
# Requires: psql in PATH and env PDT2881_PG_HOST, PDT2881_PG_USER, PDT2881_PG_PASSWORD, PDT2881_PG_DATABASE
# Optional: PDT2881_PG_PORT (default 5432)

param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('contacts', 'countBySubject')]
    [string]$Mode,

    [int]$EmailCommunicationId = 0,

    [string]$EmailSubject = ''
)

function Assert-SelectOnly {
    param([string]$Sql)
    if ($Sql -notmatch '^\s*SELECT\s') {
        throw "Only SELECT statements are allowed."
    }
}

$pgHost = $env:PDT2881_PG_HOST
$pgUser = $env:PDT2881_PG_USER
$pgPassword = $env:PDT2881_PG_PASSWORD
$pgDatabase = $env:PDT2881_PG_DATABASE
$pgPort = if ($env:PDT2881_PG_PORT) { $env:PDT2881_PG_PORT } else { '5432' }

if (-not $pgHost -or -not $pgUser -or -not $pgPassword -or -not $pgDatabase) {
    Write-Error 'Missing PDT2881_PG_HOST / PDT2881_PG_USER / PDT2881_PG_PASSWORD / PDT2881_PG_DATABASE'
    exit 2
}

$psql = Get-Command psql -ErrorAction SilentlyContinue
if (-not $psql) {
    Write-Error 'psql CLI not found in PATH'
    exit 3
}

$env:PGPASSWORD = $pgPassword

if ($Mode -eq 'contacts') {
    if ($EmailCommunicationId -le 0) {
        Write-Error 'EmailCommunicationId must be a positive integer for contacts mode'
        exit 4
    }

    $sql = @"
SELECT ecc.id, ecc.email_address, ecc.status, ecc.task_id
FROM crm.email_communication_customer_contacts ecc
JOIN crm.email_communication_customers ec ON ec.id = ecc.email_communication_customer_id
WHERE ec.email_communication_id = $($EmailCommunicationId)
ORDER BY ecc.id
"@

    Assert-SelectOnly -Sql $sql

    $raw = & psql -h $pgHost -p $pgPort -U $pgUser -d $pgDatabase -t -A -F "`t" -c $sql 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Error $raw
        exit 5
    }

    $rows = @()
    foreach ($line in ($raw -split "`n")) {
        $trimmed = $line.Trim()
        if (-not $trimmed) { continue }
        $parts = $trimmed -split "`t"
        if ($parts.Count -lt 4) { continue }
        $rows += [ordered]@{
            id           = [long]$parts[0]
            emailAddress = $parts[1]
            status       = if ($parts[2] -eq '') { $null } else { $parts[2] }
            taskId       = if ($parts[3] -eq '') { $null } else { $parts[3] }
        }
    }

    $rows | ConvertTo-Json -Compress
    exit 0
}

if ($Mode -eq 'countBySubject') {
    if (-not $EmailSubject) {
        Write-Error 'EmailSubject is required for countBySubject mode'
        exit 6
    }

    $escapedSubject = $EmailSubject.Replace("'", "''")
    $sql = "SELECT COUNT(*) FROM crm.email_communications WHERE email_subject = '$escapedSubject';"
    Assert-SelectOnly -Sql $sql

    $raw = & psql -h $pgHost -p $pgPort -U $pgUser -d $pgDatabase -t -A -c $sql 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Error $raw
        exit 5
    }

    $count = [int]($raw.Trim())
    @{ count = $count } | ConvertTo-Json -Compress
    exit 0
}

Write-Error "Unknown mode: $Mode"
exit 1
