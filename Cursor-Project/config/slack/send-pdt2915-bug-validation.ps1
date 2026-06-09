$ErrorActionPreference = 'Stop'
$ws = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..')).Path
$envPath = Join-Path $ws 'Cursor-Project\EnergoTS\.env'

if (-not (Test-Path -LiteralPath $envPath)) {
    Write-Error "Not found: $envPath"
    exit 1
}

function Unquote-EnvValue([string]$raw) {
    $v = $raw.Trim()
    if ($v.StartsWith('"') -and $v.EndsWith('"') -and $v.Length -ge 2) { $v = $v.Substring(1, $v.Length - 2) }
    elseif ($v.StartsWith("'") -and $v.EndsWith("'") -and $v.Length -ge 2) { $v = $v.Substring(1, $v.Length - 2) }
    return $v.Trim()
}

$token = $null
Get-Content -LiteralPath $envPath | ForEach-Object {
    if ($_ -match '^\s*SLACK_BOT_TOKEN\s*=\s*(.+)$') {
        $token = Unquote-EnvValue $matches[1]
    }
    elseif (-not $token -and $_ -match '^\s*SLACK_API_TOKEN\s*=\s*(.+)$') {
        $token = Unquote-EnvValue $matches[1]
    }
}

if (-not $token) {
    Write-Error 'Set SLACK_BOT_TOKEN or SLACK_API_TOKEN in EnergoTS\.env'
    exit 1
}

$msg = @'
# Bug validation — PDT-2915

**Status:** COMPLETED
**Verdict:** VALID
**Environment:** prod
**Confidence:** 88%

---

### Reproduce steps

1. Open billing run INVOICE_CORRECTION preview (prod): https://apps.energo-pro.bg/phoenix-epres/billing-run/preview/pdf-documents?type=INVOICE_CORRECTION&id=3400
2. Review generated credit/debit note PDFs for PODs: 32Z1030030113939, EVN4048812, 32Z1030004126441, EVN4017929, EVN1069815, EVN1656841
3. Compare to correction scope — PODs reportedly have no correction data but still appear on invoices
4. Check billing-by-profile rows (prod): IDs 420447, 420844, 420424, 420485, 420828, 420848; field pod.billing_by_profile.invoiced
5. Reporter note: these PODs still have billing data by profile while the profile set is deleted in the system

---

### Expected behavior

**Confluence (decision basis):** Correction process should include only PODs from the old invoice; old billing data should not be unmarked after correction.

**From ticket + Prod DB:** PODs with DELETED billing_by_profile (invoiced=true) and zero volume delta between ACTIVE and DELETED profiles should not drive correction credit/debit note lines.

---

### Confluence evidence (decision basis)

**Phase 2 excluded:** yes

| Title | Page ID | URL |
|-------|---------|-----|
| Correction flow for product/service contracts | 256114692 | https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/256114692/Correction+flow+for+product+service+contracts |
| Phoenix documentation- Phase 1 | 164356 | https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/164356/Phoenix+documentation-+Phase+1 |

Classification: contextual match — correction should include only PODs from old invoice.

---

### Swagger validation

Spec: Cursor-Project/config/swagger/prod/swagger-spec.json (refreshed)
- INVOICE_CORRECTION billing type supported
- POD-level inclusion rules not in OpenAPI
Result: supports reporter for workflow; not applicable for POD inclusion rules

---

### Code validation

**Matches reported behavior (plausible faulty path):**
- volumeChange=true, priceChange=false → BillingRunCorrectionService.reverseInvoices() uses correction_pods with full_reversal_needed=true for SCALE/SETTLEMENT/DISCOUNT lines
- BillingByProfileService.delete() soft-deletes (status=DELETED); rows retained; delete blocked if invoiced=true
- Correction prep delegated to DB procedure run_standard_billing_main_data_preparation_correction (body not in Phoenix repo)

File: phoenix-core-lib/.../BillingRunCorrectionService.java (lines 56-58, 217-227)

---

### Database Investigation (Prod)

**Classification:** reveals_root_cause

**Billing run 3400:** INVOICE_CORRECTION, CANCELLED, volume_change=true, price_change=false, base invoice 121765 (old run 2912)

**6 BBP records (ticket IDs):** ALL status=DELETED, invoiced=true, 2972 billing_data rows each

**Zero volume delta:** ACTIVE (421xxx) vs DELETED (420xxx) profiles have identical SUM(billing_data_by_profile.value) per POD

**correction_billing_data_ids:** DELETED profiles (420424-420848) mapped as original=false correction side; 364 other PODs have ignore=true; ticket 6 PODs do NOT

**correction_pods:** All 6 ticket PODs: detail_type=VOLUME, is_recoginzed=true, full_reversal_needed=true

**Original invoice 121765:** All 6 PODs present (Confluence inclusion scope satisfied; issue is DELETED profile + zero delta processing)

---

### Verdict: VALID

| Dimension | Assessment |
|-----------|------------|
| Confluence | Contextual match |
| Code | Volume-only full_reversal path generates document lines |
| Prod DB | Root cause: DELETED BBP + identical volumes + ignore not set |
| Reporter | Confirmed deleted profiles; Expected Result empty in ticket |

---

### Next steps

1. Product: fill Expected Result in Jira
2. Fix: exclude DELETED billing_by_profile from correction side OR set ignore=true when volume delta = 0
3. Review stored procedure run_standard_billing_main_data_preparation_correction
4. Data cleanup for 6 DELETED BBP records (with product approval)

---

Agents involved: BugFinderAgent, PhoenixExpert, ProductionDataReaderAgent
'@

$targets = @(
    @{ Id = 'C0AUEEDVCEL'; Label = 'bug-validation' },
    @{ Id = 'C0AK96S1D7X'; Label = 'ai-report (fallback)' }
)

$posted = $false
$lastError = $null
foreach ($target in $targets) {
    $header = if ($target.Label -like '*fallback*') {
        "[Note: #bug-validation (C0AUEEDVCEL) not accessible to bot - posted to #ai-report instead]`n`n"
    } else { '' }

    $payload = @{
        channel = $target.Id
        text    = ($header + $msg)
        mrkdwn  = $false
    } | ConvertTo-Json -Depth 5 -Compress

    $bytes = [System.Text.Encoding]::UTF8.GetBytes($payload)
    $response = Invoke-RestMethod `
        -Uri 'https://slack.com/api/chat.postMessage' `
        -Headers @{ Authorization = "Bearer $token" } `
        -Method Post `
        -Body $bytes `
        -ContentType 'application/json; charset=utf-8'

    if ($response.ok) {
        Write-Host "OK: Posted PDT-2915 bug validation to $($target.Label) (ts=$($response.ts))"
        $posted = $true
        break
    }
    $lastError = $response.error
    Write-Host "WARN: $($target.Label) failed: $lastError"
}

if (-not $posted) {
    Write-Error "Slack delivery failed for all targets. Last error: $lastError"
    exit 1
}
