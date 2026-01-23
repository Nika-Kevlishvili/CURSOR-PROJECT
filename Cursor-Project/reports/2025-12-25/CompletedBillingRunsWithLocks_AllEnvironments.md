# Completed Billing Runs with Remaining Locks - All Environments Analysis

**Date:** 2025-12-25  
**Analysis:** Find all billing runs with status COMPLETED that still have locked objects  
**Environments Analyzed:** Test, Dev, Dev2, PreProd

## Executive Summary

⚠️ **WARNING:** Found **184 COMPLETED billing runs** with remaining locks across all environments.

### Critical Findings

| Environment | Total Completed | With Locks | Total Locks | Status |
|-------------|----------------|-----------|-------------|--------|
| **Test** | 580 | **6** | 1,358 | ⚠️ Issues Found |
| **Dev** | 1,206 | **178** | 203,225 | ⚠️ **CRITICAL** |
| **Dev2** | 853 | 0 | 0 | ✅ Clean |
| **PreProd** | 0 | 0 | 0 | ✅ Clean |

## Detailed Findings

### Test Environment (10.236.20.24:5432/phoenix)

**Total COMPLETED billing runs:** 580  
**COMPLETED billing runs with locks:** 6  
**Total locks for COMPLETED runs:** 1,358

#### Affected Billing Runs:

1. **BILLING202512240001** (ID: 1650)
   - Lock Count: 11
   - Locked Entity Types: currencies, customers, data-by-profiles, energy-product-contracts, energy-products, points-of-delivery, price-components, vat-rates
   - Created: 2025-12-24 09:32:57
   - Modified: 2025-12-24 09:38:14

2. **BILLING202512230016** (ID: 1649)
   - Lock Count: 11
   - Locked Entity Types: currencies, customers, data-by-scales, energy-product-contracts, energy-products, points-of-delivery, price-components, vat-rates
   - Created: 2025-12-23 15:58:47
   - Modified: 2025-12-23 16:01:10

3. **BILLING202512230015** (ID: 1648)
   - Lock Count: 11
   - Locked Entity Types: currencies, customers, data-by-scales, energy-product-contracts, energy-products, points-of-delivery, price-components, vat-rates
   - Created: 2025-12-23 15:58:19
   - Modified: 2025-12-23 16:01:25

4. **BILLING202512230013** (ID: 1646)
   - Lock Count: 11
   - Locked Entity Types: currencies, customers, data-by-scales, energy-product-contracts, energy-products, points-of-delivery, price-components, vat-rates
   - Created: 2025-12-23 15:50:32
   - Modified: 2025-12-23 15:53:25

5. **BILLING202512230012** (ID: 1645)
   - Lock Count: 23
   - Locked Entity Types: currencies, customers, data-by-profiles, energy-product-contracts, energy-products, groups-of-price-components, points-of-delivery, price-components, price-parameters, vat-rates
   - Created: 2025-12-23 12:57:13
   - Modified: 2025-12-23 12:58:27

6. **BILLING202512220055** (ID: 1620) ⚠️ **CRITICAL**
   - Lock Count: **1,291**
   - Locked Entity Types: currencies, customers, data-by-profiles, data-by-scales, energy-product-contracts, energy-products, groups-of-price-components, points-of-delivery, price-components, price-parameters, vat-rates
   - Created: 2025-12-22 13:02:08
   - Modified: 2025-12-22 13:02:28

### Dev Environment (10.236.20.21:5432/phoenix)

**Total COMPLETED billing runs:** 1,206  
**COMPLETED billing runs with locks:** **178** ⚠️  
**Total locks for COMPLETED runs:** **203,225** ⚠️ **CRITICAL**

#### Most Critical Cases:

1. **BILLING202512020002** (ID: 1270) ⚠️ **CRITICAL**
   - Lock Count: **44,431**
   - Created: 2025-12-02 10:25:22
   - Modified: 2025-12-02 11:14:13

2. **BILLING202511200006** (ID: 1127) ⚠️ **CRITICAL**
   - Lock Count: **156,651**
   - Created: 2025-11-20 10:24:09
   - Modified: 2025-11-20 10:24:13

3. **BILLING202512050004** (ID: 1341) ⚠️ **CRITICAL**
   - Lock Count: **639**
   - Created: 2025-12-05 08:38:50
   - Modified: 2025-12-05 08:47:03

#### Pattern Analysis:

- **Most common lock count:** 5 locks (currencies, vat-rates) - 78 billing runs
- **Second most common:** 11 locks (full entity set) - 65 billing runs
- **Third most common:** 12 locks (with data-by-scales) - 8 billing runs

#### Recent Affected Runs (Last 3 Days):

- BILLING202512240002 (ID: 2169) - 11 locks
- BILLING202512240001 (ID: 2168) - 5 locks
- BILLING202512230117 (ID: 2148) - 5 locks
- BILLING202512230115 (ID: 2146) - 5 locks
- BILLING202512230101 (ID: 2132) - 11 locks

### Dev2 Environment (10.236.20.22:5432/phoenix)

**Total COMPLETED billing runs:** 853  
**COMPLETED billing runs with locks:** 0  
**Total locks for COMPLETED runs:** 0

✅ **No issues found** - All COMPLETED billing runs have been properly unlocked.

### PreProd Environment (10.236.20.76:5432/phoenix)

**Total COMPLETED billing runs:** 0  
**COMPLETED billing runs with locks:** 0  
**Total locks for COMPLETED runs:** 0

✅ **No billing runs found** - Environment appears to be clean or unused.

## Root Cause Analysis

### Issue Pattern

1. **Inconsistent Unlock Behavior:**
   - Dev2 environment shows 0 locks for all COMPLETED runs (working correctly)
   - Test and Dev environments show locks remaining after completion
   - This suggests the unlock logic is inconsistent or failing in some cases

2. **Lock Types Affected:**
   - Most common: `currencies` and `vat-rates` (5 locks) - appears in 78+ cases
   - Full entity set: 11-12 locks including customers, contracts, products, PODs, etc.
   - Critical cases: 639-156,651 locks (systemic failure)

3. **Timeline Analysis:**
   - Issues date back to at least November 2025
   - Recent issues (December 2025) continue to occur
   - No clear pattern in dates or times

### Possible Causes

1. **Stored Procedure Failure:**
   - `billing_run.make_billing_run_real(?)` may not always execute unlock logic
   - Exception handling might swallow unlock errors
   - Transaction rollback might prevent unlock

2. **Race Conditions:**
   - Locks created after stored procedure execution
   - Status updated before unlock completes
   - Concurrent processes interfering

3. **Missing Explicit Unlock:**
   - Java code relies on stored procedure for unlock
   - No explicit `lockRepository.deleteByBillingId()` call in completion flow
   - Stored procedure might have bugs or edge cases

## Recommendations

### Immediate Actions

1. **Investigate Critical Cases:**
   - BILLING202511200006 (Dev) - 156,651 locks
   - BILLING202512020002 (Dev) - 44,431 locks
   - BILLING202512220055 (Test) - 1,291 locks

2. **Manual Cleanup (if safe):**
   ```sql
   -- Review and potentially cleanup locks for specific billing runs
   DELETE FROM lock.locks 
   WHERE billing_id IN (1270, 1127, 1620);
   ```

3. **Add Monitoring:**
   - Alert when COMPLETED billing runs have locks
   - Daily automated check for orphaned locks
   - Report generation for lock cleanup

### Long-term Solutions

1. **Add Explicit Unlock in Java Code:**
   ```java
   // In BillingRunStartAccountingService.java after completion
   lockRepository.deleteByBillingId(billingRun.getId());
   ```

2. **Fix Stored Procedure:**
   - Review `billing_run.make_billing_run_real` stored procedure
   - Ensure unlock logic is always executed
   - Add error handling and logging

3. **Add Integration Tests:**
   - Test that locks are released after completion
   - Test edge cases and error scenarios
   - Verify unlock happens in all completion paths

4. **Implement Lock Cleanup Job:**
   - Scheduled job to find and cleanup orphaned locks
   - Run daily/weekly to prevent accumulation
   - Log cleanup actions for audit

## SQL Query Used

```sql
SELECT 
    b.id AS billing_run_id,
    b.billing_number,
    b.status::text AS status,
    b.create_date,
    b.modify_date,
    COUNT(l.id) AS lock_count,
    STRING_AGG(DISTINCT l.entity_type, ', ' ORDER BY l.entity_type) AS locked_entity_types
FROM billing.billings b
INNER JOIN lock.locks l ON l.billing_id = b.id
WHERE b.status::text = 'COMPLETED'
GROUP BY b.id, b.billing_number, b.status, b.create_date, b.modify_date
ORDER BY b.id DESC
```

## Creation Date Analysis

### Timeline Summary

- **Test Environment:**
  - Oldest affected: **December 22, 2025**
  - Newest affected: **December 24, 2025**
  - Date range: **3 days**

- **Dev Environment:**
  - Oldest affected: **November 6, 2025**
  - Newest affected: **December 24, 2025**
  - Date range: **49 days**

### Critical Dates

1. **November 20, 2025:** Most critical case (156,651 locks) - BILLING202511200006
2. **December 2, 2025:** Second critical case (44,431 locks) - BILLING202512020002
3. **December 5, 2025:** Multiple high-lock cases (639, 31, 22, 22, 21 locks)
4. **December 23, 2025:** Peak day with 30 affected billing runs

**Note:** For detailed date analysis, see `BillingLockCreationDates_Analysis.md`

## Next Steps

1. ✅ **Analysis Complete** - All environments checked
2. ✅ **Date Analysis Complete** - Timeline identified
3. ⏳ **Investigation Required** - Root cause analysis needed (focus on Nov 6, Nov 20, Dec 2, Dec 5)
4. ⏳ **Fix Implementation** - Add explicit unlock logic
5. ⏳ **Testing** - Verify fix works in all scenarios
6. ⏳ **Monitoring** - Set up alerts and automated checks
7. ⏳ **Cleanup** - Remove orphaned locks (after investigation)

---

**Report Generated:** 2025-12-25  
**Script:** `examples/check_completed_billing_locks.py`  
**Date Analysis Script:** `examples/analyze_billing_lock_dates.py`  
**Total Issues Found:** 184 COMPLETED billing runs with remaining locks  
**Date Range:** November 6, 2025 - December 24, 2025

