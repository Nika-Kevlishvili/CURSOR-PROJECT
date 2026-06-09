<#
.SYNOPSIS
  Run PDT-2915 read-only DB evidence queries (Prod or any env with psql).

.PARAMETER BillingRunId
  Invoice correction billing run id (default 3400 for PDT-2915 Prod).

.ENV (optional — do not commit secrets)
  PDT2915_PG_HOST, PDT2915_PG_PORT (default 5000), PDT2915_PG_USER, PDT2915_PG_PASSWORD, PDT2915_PG_DATABASE (default phoenix)

.EXAMPLE
  $env:PDT2915_PG_HOST='10.236.20.78'; $env:PDT2915_PG_USER='readonly_user'; ...
  powershell -ExecutionPolicy Bypass -File Cursor-Project/config/bug-validation/run-pdt2915-db-evidence.ps1 -BillingRunId 3400
#>
param(
    [int]$BillingRunId = 3400
)

$ErrorActionPreference = 'Stop'

function Assert-SelectOnly([string]$Sql) {
    if ($Sql -notmatch '(?is)^\s*SELECT\s') { throw 'Only SELECT allowed.' }
}

$pgHost = $env:PDT2915_PG_HOST
$pgUser = $env:PDT2915_PG_USER
$pgPassword = $env:PDT2915_PG_PASSWORD
$pgDatabase = if ($env:PDT2915_PG_DATABASE) { $env:PDT2915_PG_DATABASE } else { 'phoenix' }
$pgPort = if ($env:PDT2915_PG_PORT) { $env:PDT2915_PG_PORT } else { '5000' }

if (-not $pgHost -or -not $pgUser -or -not $pgPassword) {
    Write-Host 'SKIP: Set PDT2915_PG_HOST, PDT2915_PG_USER, PDT2915_PG_PASSWORD (and optional PORT/DATABASE) for psql execution.'
    Write-Host "Or run queries manually from: config/bug-validation/queries/PDT-2915-invoice-correction-deleted-bbp.sql (billing_run_id=$BillingRunId)"
    exit 2
}

$psql = Get-Command psql -ErrorAction SilentlyContinue
if (-not $psql) {
    Write-Error 'psql not in PATH'
    exit 3
}

$env:PGPASSWORD = $pgPassword
$bid = $BillingRunId

function Invoke-Select([string]$Name, [string]$Sql) {
    Assert-SelectOnly $Sql
    Write-Host "`n=== $Name ===" -ForegroundColor Cyan
    & psql -h $pgHost -p $pgPort -U $pgUser -d $pgDatabase -c $Sql
    if ($LASTEXITCODE -ne 0) { throw "Query failed: $Name" }
}

Invoke-Select '1_billing_run' @"
SELECT id, billing_number, type, status, billing_run_process_stage, price_change, volume_change
FROM billing.billings WHERE id = $bid;
"@

Invoke-Select '2_correction_base' @"
SELECT id, run_id, old_run_id, invoice_id, status, reverse_status
FROM billing_run.correction_invoices_base WHERE run_id = $bid;
"@

Invoke-Select '3_correction_pods' @"
SELECT cp.pod_id, p.identifier, cp.detail_type, cp.is_recoginzed, cp.full_reversal_needed
FROM billing_run.correction_pods cp
LEFT JOIN pod.pod p ON p.id = cp.pod_id
WHERE cp.run_id = $bid ORDER BY p.identifier;
"@

Invoke-Select '4_deleted_bbp_correction_side' @"
SELECT cbdi.id, cbdi.pod_id, p.identifier, cbdi.profile_id, bbp.status, bbp.invoiced, cbdi.original, cbdi.ignore
FROM billing_run.correction_billing_data_ids cbdi
JOIN pod.pod p ON p.id = cbdi.pod_id
LEFT JOIN pod.billing_by_profile bbp ON bbp.id = cbdi.profile_id
WHERE cbdi.run_id = $bid AND cbdi.original = false AND bbp.status = 'DELETED'
ORDER BY p.identifier;
"@

Invoke-Select '5_zero_volume_delta' @"
WITH pairs AS (
  SELECT p.identifier, bbp.id AS bbp_id, bbp.status, ROUND(SUM(bdbp.value)::numeric, 3) AS total_value
  FROM pod.billing_by_profile bbp
  JOIN pod.pod p ON p.id = bbp.pod_id
  JOIN pod.billing_data_by_profile bdbp ON bdbp.billing_by_profile_id = bbp.id
  WHERE bbp.pod_id IN (SELECT DISTINCT pod_id FROM billing_run.correction_pods WHERE run_id = $bid)
  GROUP BY p.identifier, bbp.id, bbp.status
)
SELECT d.identifier, d.total_value AS deleted_total, a.total_value AS active_total, (d.total_value = a.total_value) AS zero_delta
FROM pairs d
JOIN pairs a ON a.identifier = d.identifier AND a.status = 'ACTIVE' AND d.status = 'DELETED'
ORDER BY d.identifier;
"@

Write-Host "`nOK: PDT-2915 DB evidence queries completed for billing_run_id=$bid" -ForegroundColor Green
