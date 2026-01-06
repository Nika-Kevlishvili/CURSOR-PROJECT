# Creation Date Analysis - COMPLETED Billing Runs with Remaining Locks

**Date:** 2025-12-25  
**Analysis:** When were billing runs with remaining locks created?  
**Environments Analyzed:** Test, Dev, Dev2, PreProd

## Executive Summary

### Timeline Overview

| Environment | Oldest Billing | Newest Billing | Date Range | Total Affected |
|------------|---------------|----------------|------------|----------------|
| **Test** | 2025-12-22 | 2025-12-24 | **3 days** | 6 billing runs |
| **Dev** | 2025-11-06 | 2025-12-24 | **49 days** | 178 billing runs |
| **Dev2** | - | - | - | 0 (clean) |
| **PreProd** | - | - | - | 0 (clean) |

### Key Findings

1. **Test Environment:** Issues started on **December 22, 2025** and continue through **December 24, 2025**
2. **Dev Environment:** Issues date back to **November 6, 2025** and continue through **December 24, 2025**
3. **Pattern:** The problem is **ongoing** and affects billing runs created recently

---

## Test Environment Analysis

### Summary
- **Total affected:** 6 billing runs
- **Date range:** December 22-24, 2025 (3 days)
- **Total locks:** 1,358
- **Average locks per run:** 226.33

### Daily Breakdown

| Date | Count | Total Locks | Avg Locks | Billing Numbers |
|------|-------|-------------|-----------|-----------------|
| **2025-12-24** | 1 | 11 | 11.00 | BILLING202512240001 |
| **2025-12-23** | 4 | 56 | 14.00 | BILLING202512230016, BILLING202512230015, BILLING202512230013, BILLING202512230012 |
| **2025-12-22** | 1 | 1,291 | 1,291.00 | BILLING202512220055 ⚠️ |

### Critical Case
- **BILLING202512220055** (December 22, 2025)
  - Created: 2025-12-22 13:02:08
  - Lock Count: **1,291 locks**
  - This is the most critical case in Test environment

---

## Dev Environment Analysis

### Summary
- **Total affected:** 178 billing runs
- **Date range:** November 6 - December 24, 2025 (49 days)
- **Total locks:** 203,225
- **Average locks per run:** 1,141.71

### Monthly Distribution

| Month | Count | Total Locks | Avg Locks |
|-------|-------|-------------|-----------|
| **December 2025** | 156 | 46,469 | 297.88 |
| **November 2025** | 22 | 156,756 | 7,125.27 |

**Note:** November has fewer billing runs but significantly more locks per run, indicating more severe cases.

### Daily Breakdown (Last 30 Days)

| Date | Count | Total Locks | Avg Locks | Notes |
|------|-------|-------------|-----------|-------|
| 2025-12-24 | 2 | 16 | 8.00 | Recent |
| 2025-12-23 | 30 | 289 | 9.63 | **Peak day** |
| 2025-12-22 | 12 | 90 | 7.50 | |
| 2025-12-19 | 8 | 68 | 8.50 | |
| 2025-12-18 | 4 | 20 | 5.00 | |
| 2025-12-17 | 8 | 71 | 8.88 | |
| 2025-12-16 | 8 | 54 | 6.75 | |
| 2025-12-15 | 8 | 88 | 11.00 | |
| 2025-12-14 | 16 | 118 | 7.38 | |
| 2025-12-13 | 15 | 105 | 7.00 | |
| 2025-12-12 | 1 | 11 | 11.00 | |
| 2025-12-11 | 9 | 87 | 9.67 | |
| 2025-12-10 | 13 | 83 | 6.38 | |
| 2025-12-09 | 9 | 89 | 9.89 | |
| 2025-12-08 | 3 | 33 | 11.00 | |
| 2025-12-05 | 6 | 740 | 123.33 | ⚠️ High locks |
| 2025-12-03 | 3 | 76 | 25.33 | |
| **2025-12-02** | 1 | **44,431** | **44,431.00** | ⚠️ **CRITICAL** |
| 2025-11-28 | 1 | 5 | 5.00 | |
| 2025-11-26 | 1 | 5 | 5.00 | |
| 2025-11-24 | 2 | 10 | 5.00 | |
| **2025-11-20** | 2 | **156,656** | **78,328.00** | ⚠️ **CRITICAL** |
| 2025-11-19 | 3 | 15 | 5.00 | |
| 2025-11-17 | 2 | 10 | 5.00 | |
| 2025-11-12 | 5 | 25 | 5.00 | |
| 2025-11-11 | 4 | 20 | 5.00 | |
| 2025-11-06 | 2 | 10 | 5.00 | **Oldest** |

### Critical Cases by Date

1. **November 20, 2025** ⚠️ **MOST CRITICAL**
   - BILLING202511200006: **156,651 locks**
   - Created: 2025-11-20 10:24:09
   - This is the single worst case across all environments

2. **December 2, 2025** ⚠️ **CRITICAL**
   - BILLING202512020002: **44,431 locks**
   - Created: 2025-12-02 10:25:22

3. **December 5, 2025** ⚠️ **HIGH**
   - Multiple billing runs with high lock counts:
     - BILLING202512050004: 639 locks
     - BILLING202512050005: 31 locks
     - BILLING202512050009: 22 locks
     - BILLING202512050008: 22 locks
     - BILLING202512050003: 21 locks

### Top 10 Billing Runs by Lock Count (Dev)

| Rank | Billing Number | Date | Lock Count |
|------|----------------|------|------------|
| 1 | BILLING202511200006 | 2025-11-20 | **156,651** ⚠️ |
| 2 | BILLING202512020002 | 2025-12-02 | **44,431** ⚠️ |
| 3 | BILLING202512050004 | 2025-12-05 | **639** |
| 4 | BILLING202512030008 | 2025-12-03 | 45 |
| 5 | BILLING202512050005 | 2025-12-05 | 31 |
| 6 | BILLING202512030013 | 2025-12-03 | 26 |
| 7 | BILLING202512090034 | 2025-12-09 | 24 |
| 8 | BILLING202512050009 | 2025-12-05 | 22 |
| 9 | BILLING202512050008 | 2025-12-05 | 22 |
| 10 | BILLING202512050003 | 2025-12-05 | 21 |

---

## Pattern Analysis

### Timeline Pattern

1. **November 2025:**
   - Issues started appearing on **November 6, 2025**
   - Most severe cases occurred on **November 20, 2025** (156,651 locks)
   - Average: 5-10 locks per run (except critical cases)

2. **December 2025:**
   - **December 2, 2025:** Second most critical case (44,431 locks)
   - **December 5, 2025:** Multiple high-lock cases (639, 31, 22, 22, 21 locks)
   - **December 23, 2025:** Peak day with 30 affected billing runs
   - **December 24, 2025:** Issues continue (2 billing runs)

### Frequency Pattern

- **Daily frequency:** 1-30 billing runs per day affected
- **Peak day:** December 23, 2025 (30 billing runs)
- **Consistent pattern:** Issues occur almost daily since November 6, 2025

### Lock Count Pattern

- **Most common:** 5 locks (currencies, vat-rates) - ~78 cases
- **Second most common:** 11 locks (full entity set) - ~65 cases
- **Critical outliers:** 639-156,651 locks (systemic failures)

---

## Conclusions

### Key Observations

1. **Problem is Ongoing:**
   - Issues started in **November 2025** and continue through **December 2025**
   - Most recent affected billing run: **December 24, 2025**
   - Problem is **active and current**

2. **Severity Escalation:**
   - November 20, 2025: First critical case (156,651 locks)
   - December 2, 2025: Second critical case (44,431 locks)
   - December 5, 2025: Multiple high-lock cases
   - Recent days: Lower lock counts but higher frequency

3. **Environment Comparison:**
   - **Test:** Issues started December 22, 2025 (recent)
   - **Dev:** Issues started November 6, 2025 (older)
   - **Dev2:** No issues (working correctly)
   - **PreProd:** No billing runs found

4. **Peak Activity:**
   - **December 23, 2025:** Highest number of affected billing runs (30 in Dev)
   - **November 20, 2025:** Highest lock count (156,651 locks)

### Recommendations

1. **Immediate Investigation:**
   - Focus on billing runs created on **November 20, 2025** and **December 2, 2025**
   - These dates show systemic failures

2. **Code Changes Timeline:**
   - Check what code changes were deployed around **November 6, 2025**
   - This is when issues first appeared

3. **Recent Activity:**
   - Monitor billing runs created after **December 24, 2025**
   - Problem is ongoing and needs immediate attention

4. **Pattern Recognition:**
   - Investigate why **December 23, 2025** had 30 affected billing runs
   - Check for system load, deployment, or configuration changes

---

**Report Generated:** 2025-12-25  
**Script:** `agents/analyze_billing_lock_dates.py`  
**Analysis Period:** November 6 - December 24, 2025




