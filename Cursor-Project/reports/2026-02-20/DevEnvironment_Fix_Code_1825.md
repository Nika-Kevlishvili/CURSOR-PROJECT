# Dev Environment Fix Code - SLP Possible Issue

**Date:** 2026-02-20  
**Time:** 18:25  
**Environment:** Dev (PostgreSQL Dev - 10.236.20.21)  
**Run ID:** 9487  

## Root Cause Analysis

The `slp_possible` field is NULL because of a **date mismatch** between contract periods and billing data scale periods:

| POD ID | Contract Period | BDS Period | Overlap | Issue |
|--------|----------------|------------|---------|-------|
| 300101 | 2024-12-31 to 2025-12-30 | 2025-12-31 to 2026-01-30 | ✅ YES | Works |
| 300102 | 2024-12-31 to 2025-12-30 | 2025-12-31 to 2026-01-30 | ❌ NO | No mapping created |
| 300104 | 2024-12-31 to 2025-12-30 | (no BDS period) | ❌ NO | No BDS data |

## The Process Flow Issue

1. **`fill_by_volume_bds_mapping`** creates `run_info_bd_mapping` only when contract period overlaps with BDS period
2. **`fill_by_volume_pc_mapping`** requires `run_info_bd_mapping` to exist to set `slp_possible = true`
3. **No overlap → No bd_mapping → No slp_possible → NULL price_parameter_name**

## Fix Options

### Option 1: Fix Contract Periods (Recommended)

The contract periods appear to be incorrect. They should overlap with the billing data periods.

**Investigation Query:**
```sql
-- Check if contract periods are correctly calculated
SELECT 
    r.pod_id,
    r.contract_activation_date,
    r.contract_termination_date,
    r.cp_calculated_period_from,
    r.cp_calculated_period_to,
    bds.date_from as bds_date_from,
    bds.date_to as bds_date_to
FROM billing_run.run_info r 
LEFT JOIN billing_run.bds_periods bds ON bds.pod_id = r.pod_id AND bds.run_id = r.run_id
WHERE r.run_id = 9487 AND r.is_active_record = 1
ORDER BY r.pod_id;
```

**Fix Code:**
```sql
-- Update contract periods to overlap with BDS periods
UPDATE billing_run.run_info 
SET cp_calculated_period_from = '2025-12-31',
    cp_calculated_period_to = '2026-01-30'
WHERE run_id = 9487 
  AND pod_id IN (300102, 300104)
  AND is_active_record = 1;

-- Re-run the mapping procedures
CALL billing_run.fill_by_volume_bds_mapping(9487, null);
CALL billing_run.fill_by_volume_pc_mapping(9487, null, null);
CALL billing_run.fill_by_volume_bds_slp_price_parameter_name_mapper(9487, null);
```

### Option 2: Fix BDS Period Creation

If the BDS periods are incorrect, fix the billing data scale generation:

**Investigation Query:**
```sql
-- Check billing_by_scale data for these PODs
SELECT 
    bbs.pod_id,
    bbs.date_from,
    bbs.date_to,
    bbs.status,
    bbs.correction,
    bbs.override
FROM pod.billing_by_scale bbs
WHERE bbs.pod_id IN (300102, 300104)
  AND bbs.status = 'ACTIVE'
  AND NOT bbs.correction
  AND NOT bbs.override
ORDER BY bbs.pod_id, bbs.date_from;
```

**Fix Code (if BDS data exists but wasn't processed):**
```sql
-- Re-run BDS periods generation
CALL billing_run.fill_by_volume_bds(9487);

-- Then re-run dependent procedures
CALL billing_run.fill_by_volume_bds_mapping(9487, null);
CALL billing_run.fill_by_volume_pc_mapping(9487, null, null);
CALL billing_run.fill_by_volume_bds_slp_price_parameter_name_mapper(9487, null);
```

### Option 3: Modify Mapping Logic (Code Change)

If the date mismatch is expected behavior, modify the `fill_by_volume_pc_mapping` procedure to handle cases without bd_mapping.

**Location:** Stored procedure `billing_run.fill_by_volume_pc_mapping`

**Current Logic:**
```sql
FROM billing_run.run_info r 
JOIN billing_run.run_info_bd_mapping m ON r.run_info_id = m.run_info_id 
JOIN billing_run.pc_app_model_mapping t ON t.product_detail_id = r.product_detail_id 
```

**Modified Logic:**
```sql
FROM billing_run.run_info r 
LEFT JOIN billing_run.run_info_bd_mapping m ON r.run_info_id = m.run_info_id 
JOIN billing_run.pc_app_model_mapping t ON t.product_detail_id = r.product_detail_id 
WHERE (m.run_info_id IS NOT NULL OR r.pod_version_measurement_type = 'SLP')
```

## Recommended Fix Sequence

### Step 1: Investigate Root Cause
```sql
-- Check contract vs BDS period alignment
SELECT 
    r.pod_id,
    r.contract_activation_date,
    r.contract_termination_date, 
    r.cp_calculated_period_from,
    r.cp_calculated_period_to,
    bds.date_from as bds_date_from,
    bds.date_to as bds_date_to,
    CASE 
        WHEN bds.date_from <= r.cp_calculated_period_to 
         AND bds.date_to >= r.cp_calculated_period_from 
        THEN 'SHOULD_MAP' 
        ELSE 'NO_MAPPING' 
    END as mapping_status
FROM billing_run.run_info r 
LEFT JOIN billing_run.bds_periods bds ON bds.pod_id = r.pod_id AND bds.run_id = r.run_id
WHERE r.run_id = 9487 AND r.is_active_record = 1
ORDER BY r.pod_id;
```

### Step 2: Apply Appropriate Fix
Based on investigation results, choose Option 1, 2, or 3 above.

### Step 3: Validate Fix
```sql
-- Verify slp_possible is now set correctly
SELECT 
    r.pod_id,
    ripcm.slp_possible,
    bp.price_parameter_name
FROM billing_run.run_info r
LEFT JOIN billing_run.run_info_price_component_mapping ripcm 
    ON ripcm.run_info_id = r.run_info_id AND ripcm.run_id = r.run_id AND ripcm.is_active_record = 1
LEFT JOIN billing_run.bds_periods bp 
    ON bp.pod_id = r.pod_id AND bp.run_id = r.run_id AND bp.is_active_record = 1
WHERE r.run_id = 9487 AND r.is_active_record = 1
ORDER BY r.pod_id;
```

## Code Location for Permanent Fix

**File:** PostgreSQL stored procedures in `billing_run` schema

**Procedures to modify:**
1. **`fill_by_volume_bds_mapping`** - Fix date overlap logic
2. **`fill_by_volume_pc_mapping`** - Handle missing bd_mapping gracefully  
3. **Contract period calculation** - Ensure proper period alignment

**Key Logic to Review:**
```sql
-- In fill_by_volume_bds_mapping
WHERE bds.date_from <= cp_calculated_period_to
  AND bds.date_to >= cp_calculated_period_from

-- In fill_by_volume_pc_mapping  
JOIN billing_run.run_info_bd_mapping m ON r.run_info_id = m.run_info_id
```

## Prevention

To prevent this issue in the future:

1. **Add validation** in billing run procedures to check for date alignment
2. **Add logging** when PODs are excluded due to date mismatches
3. **Review contract period calculation logic** to ensure proper alignment with billing data
4. **Add error handling** for PODs without bd_mapping but with SLP measurement type

---

**Analysis completed:** 2026-02-20 18:25  
**Environment:** Dev PostgreSQL (10.236.20.21)  
**Next Steps:** Apply recommended fix and validate results