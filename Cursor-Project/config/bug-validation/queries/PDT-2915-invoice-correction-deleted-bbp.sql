-- PDT-2915 pattern checks (READ-ONLY). Replace :billing_run_id (e.g. 3400).
-- Run on the same environment as the bug (prod for PDT-2915).

-- 1) Billing run type and correction flags
SELECT id, billing_number, type, status, billing_run_process_stage, price_change, volume_change
FROM billing.billings
WHERE id = :billing_run_id;

-- 2) Base correction invoice link
SELECT id, run_id, old_run_id, invoice_id, status, reverse_status
FROM billing_run.correction_invoices_base
WHERE run_id = :billing_run_id;

-- 3) PODs flagged for full reversal (volume-only correction path)
SELECT cp.pod_id, p.identifier, cp.detail_type, cp.is_recoginzed, cp.full_reversal_needed
FROM billing_run.correction_pods cp
LEFT JOIN pod.pod p ON p.id = cp.pod_id
WHERE cp.run_id = :billing_run_id
ORDER BY p.identifier;

-- 4) DELETED billing_by_profile used as correction side (original=false)
SELECT cbdi.id, cbdi.pod_id, p.identifier, cbdi.profile_id, bbp.status, bbp.invoiced, cbdi.original, cbdi.ignore
FROM billing_run.correction_billing_data_ids cbdi
JOIN pod.pod p ON p.id = cbdi.pod_id
LEFT JOIN pod.billing_by_profile bbp ON bbp.id = cbdi.profile_id
WHERE cbdi.run_id = :billing_run_id
  AND cbdi.original = false
  AND bbp.status = 'DELETED'
ORDER BY p.identifier;

-- 5) Zero volume delta: ACTIVE vs DELETED profile totals per POD (bug signature)
WITH pairs AS (
  SELECT
    p.identifier,
    bbp.id AS bbp_id,
    bbp.status,
    ROUND(SUM(bdbp.value)::numeric, 3) AS total_value
  FROM pod.billing_by_profile bbp
  JOIN pod.pod p ON p.id = bbp.pod_id
  JOIN pod.billing_data_by_profile bdbp ON bdbp.billing_by_profile_id = bbp.id
  WHERE bbp.pod_id IN (
    SELECT DISTINCT pod_id FROM billing_run.correction_pods WHERE run_id = :billing_run_id
  )
  GROUP BY p.identifier, bbp.id, bbp.status
)
SELECT
  d.identifier,
  d.total_value AS deleted_total,
  a.total_value AS active_total,
  (d.total_value = a.total_value) AS zero_delta
FROM pairs d
JOIN pairs a ON a.identifier = d.identifier AND a.status = 'ACTIVE' AND d.status = 'DELETED'
ORDER BY d.identifier;
