# Dev Environment Price Parameter Name Analysis

**Date:** 2026-02-20  
**Time:** 18:20  
**Environment:** Dev (PostgreSQL Dev - 10.236.20.21)  
**Run ID:** 9487  

## Issue Summary

The user asked why `price_parameter_name` is NULL for pulled PODs in Dev environment, which causes the SLP generation to fail with `failed_slp_generation = true`. The analysis reveals the root cause and explains why this issue occurs.

## Root Cause Analysis

### 1. The Problem Statement

In the stored procedure `billing_run.generate_run_volume()`, there's this UPDATE statement:

```sql
UPDATE billing_run.bds_periods bds
SET failed_slp_generation = true
WHERE bds.run_id = p_run_id 
  AND bds.spl_candidate = TRUE 
  AND bds.price_parameter_name IS NULL 
  AND bds.is_active_record = 1;
```

This statement sets `failed_slp_generation = true` when:
- POD is an SLP candidate (`spl_candidate = TRUE`)
- But `price_parameter_name` is NULL
- Record is active

### 2. Why `price_parameter_name` is NULL

The `price_parameter_name` field is populated by the stored procedure `fill_by_volume_bds_slp_price_parameter_name_mapper()`. This procedure has specific requirements:

**Required Conditions for `price_parameter_name` Population:**
1. `slp_possible = TRUE` in `run_info_price_component_mapping`
2. `measurement_type = 'SLP'` in `pod_details`
3. `status = 'ACTIVE'` in `pod_measurement_types`
4. Valid `name` field in `pod_measurement_types`

### 3. Dev Environment Analysis (Run 9487)

**Total PODs in Run 9487:** 3

**POD Analysis:**

| POD ID | Identifier | slp_possible | measurement_type | price_parameter_name | Result |
|--------|------------|--------------|------------------|---------------------|---------|
| 300101 | 32XAUTOMATION22009022020 | `true` | SLP | AUTOMATION2121424114 | ✅ Works |
| 300102 | 32XAUTOMATION22009061647 | `null` | SLP | AUTOMATION2121424114 | ❌ NULL |
| 300104 | 32XAUTOMATION22009063757 | `null` | SLP | AUTOMATION2121424114 | ❌ NULL |

### 4. The Missing Link

**POD 300102 and 300104 have:**
- ✅ `measurement_type = 'SLP'` (correct)
- ✅ Valid `price_parameter_name` available in nomenclature (AUTOMATION2121424114)
- ✅ `status = 'ACTIVE'` in pod_measurement_types
- ❌ **`slp_possible = null`** (should be `true`)

**The stored procedure `fill_by_volume_bds_slp_price_parameter_name_mapper()` requires `slp_possible = TRUE`, but PODs 300102 and 300104 have `slp_possible = null`.**

### 5. Why `slp_possible` is NULL

The `slp_possible` field in `run_info_price_component_mapping` is populated by earlier procedures in the billing run process. When it's NULL, it means:

1. **Missing Price Component Mapping:** The price component mapping logic didn't identify these PODs as SLP-eligible
2. **Incomplete Mapping Process:** The procedure that populates `run_info_price_component_mapping` might not have run completely
3. **Configuration Issue:** The price component configuration for these PODs might be incomplete

### 6. Comparison with Production Issue

**In Production (POD 13420, Run 2448):**
- `slp_possible` was likely `true` initially
- But the parent-child relationship was incorrect (cross-POD assignment)
- This caused the SLP generation to fail at a later stage

**In Dev (PODs 300102/300104, Run 9487):**
- `slp_possible` is `null` from the start
- This prevents `price_parameter_name` from being populated
- Should trigger `failed_slp_generation = true` but doesn't (inconsistency)

## Fix Recommendations

### 1. Immediate Fix (Manual)

To test the fix in Dev environment, manually update the `slp_possible` field:

```sql
UPDATE billing_run.run_info_price_component_mapping ripcm
SET slp_possible = true
FROM billing_run.run_info ri
WHERE ripcm.run_info_id = ri.run_info_id 
  AND ripcm.run_id = ri.run_id
  AND ri.run_id = 9487
  AND ri.pod_id IN (300102, 300104)
  AND ripcm.is_active_record = 1;
```

Then re-run the price parameter name mapping:

```sql
CALL billing_run.fill_by_volume_bds_slp_price_parameter_name_mapper(9487, null);
```

### 2. Root Cause Fix

Investigate why the price component mapping procedure doesn't set `slp_possible = true` for these PODs:

1. **Check Price Component Configuration:** Verify that PODs 300102 and 300104 have proper price component configurations
2. **Review Mapping Logic:** Examine the procedure that populates `run_info_price_component_mapping`
3. **Validate Prerequisites:** Ensure all prerequisites for SLP eligibility are met

### 3. Code Logic Fix

The pulling logic should validate that `slp_possible` is properly set before proceeding with SLP generation. Add validation in the billing run procedures to ensure consistency.

## Testing the Bug Reproduction

To reproduce the original bug (like in Production), you can:

1. **Set up correct `slp_possible = true`** for PODs 300102 and 300104
2. **Manually create incorrect parent-child relationships** (cross-POD assignments)
3. **Run the SLP generation process**
4. **Observe `failed_slp_generation = true`** as expected

## Conclusion

The `price_parameter_name` is NULL in Dev environment because:

1. **`slp_possible = null`** in `run_info_price_component_mapping` for PODs 300102 and 300104
2. **The mapping procedure requires `slp_possible = TRUE`** to populate `price_parameter_name`
3. **This is a configuration/mapping issue** rather than a pulling logic bug

The bug you're investigating (cross-POD parent relationships) is a separate issue that occurs later in the process when the SLP generation logic fails due to incorrect parent-child assignments.

**Next Steps:**
1. Fix the `slp_possible` mapping issue in Dev
2. Test the cross-POD parent relationship bug reproduction
3. Implement the pulling logic fix identified in the Production analysis

---

**Analysis completed at:** 2026-02-20 18:20  
**Environment:** Dev PostgreSQL (10.236.20.21)  
**Analyst:** Cursor AI Assistant