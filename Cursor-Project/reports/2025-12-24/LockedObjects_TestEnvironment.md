# Locked Objects Analysis - Test Environment

**Date:** 2025-01-24  
**Environment:** Test (10.236.20.24:5432/phoenix)  
**Analysis:** All locked objects in Test environment

## Executive Summary

- **Total Locks:** 20
- **Active Locks:** 0
- **Expired Locks:** 20 (100%)
- **System Locks (from billing runs):** 0
- **User Locks:** 20
- **Status:** All locks are expired and should be cleaned up

## Lock Summary by Entity Type

| Entity Type | Lock Count | Unique Entities | Active | Expired | First Lock | Last Lock |
|-------------|------------|-----------------|--------|---------|------------|-----------|
| **terminations** | 6 | 6 | 0 | 6 | 2025-08-19 | 2025-08-20 |
| **price-components** | 3 | 3 | 0 | 3 | 2025-11-14 | 2025-11-26 |
| **groups-of-terminations** | 2 | 2 | 0 | 2 | 2025-08-19 | 2025-08-19 |
| **templates** | 2 | 2 | 0 | 2 | 2025-08-19 | 2025-12-01 |
| **interim-and-advance-payments** | 1 | 1 | 0 | 1 | 2025-08-28 | 2025-08-28 |
| **system-messages** | 1 | 1 | 0 | 1 | 2025-08-14 | 2025-08-14 |
| **company-details** | 1 | 1 | 0 | 1 | 2025-11-20 | 2025-11-20 |
| **terms** | 1 | 1 | 0 | 1 | 2025-08-22 | 2025-08-22 |
| **customers** | 1 | 1 | 0 | 1 | 2025-12-16 | 2025-12-16 |
| **groups-of-interim-and-advance-payment** | 1 | 1 | 0 | 1 | 2025-08-20 | 2025-08-20 |
| **groups-of-price-components** | 1 | 1 | 0 | 1 | 2025-08-29 | 2025-08-29 |

## Detailed Lock Information

### 1. Terminations (6 locks)

| Lock ID | Entity ID | Lock Owner | Created At | Expires At | Status |
|---------|-----------|------------|------------|------------|--------|
| 1744 | 1087 | Militsa Anastasova | 2025-08-20 07:28:13 | 2025-08-20 08:28:13 | EXPIRED |
| 1600 | 1010 | Ivaylo Papazov | 2025-08-19 11:08:24 | 2025-08-19 12:08:24 | EXPIRED |
| 1558 | 1038 | Ivaylo Papazov | 2025-08-19 09:56:10 | 2025-08-19 10:56:10 | EXPIRED |
| 1556 | 1028 | Ivaylo Papazov | 2025-08-19 09:53:48 | 2025-08-19 10:53:48 | EXPIRED |
| 1546 | 1035 | Ivaylo Papazov | 2025-08-19 09:46:56 | 2025-08-19 10:46:56 | EXPIRED |
| 1532 | 1027 | Ivaylo Papazov | 2025-08-19 09:14:51 | 2025-08-19 10:14:51 | EXPIRED |
| 1524 | 1025 | Ivaylo Papazov | 2025-08-19 08:55:24 | 2025-08-19 09:55:24 | EXPIRED |

### 2. Price Components (3 locks)

| Lock ID | Entity ID | Lock Owner | Created At | Expires At | Status |
|---------|-----------|------------|------------|------------|--------|
| 3804 | 1180 | Militsa Anastasova | 2025-11-26 05:07:46 | 2025-11-26 06:07:46 | EXPIRED |
| 3580 | 1213 | Militsa Anastasova | 2025-11-25 10:13:10 | 2025-11-25 11:13:10 | EXPIRED |
| 3292 | 1008 | Jivka Todorova | 2025-11-14 07:23:45 | 2025-11-14 08:23:45 | EXPIRED |

### 3. Groups of Terminations (2 locks)

| Lock ID | Entity ID | Lock Owner | Created At | Expires At | Status |
|---------|-----------|------------|------------|------------|--------|
| 1600 | 1010 | Ivaylo Papazov | 2025-08-19 11:08:24 | 2025-08-19 12:08:24 | EXPIRED |
| 1522 | 1005 | Ivaylo Papazov | 2025-08-19 08:50:44 | 2025-08-19 09:50:44 | EXPIRED |

### 4. Templates (2 locks)

| Lock ID | Entity ID | Lock Owner | Created At | Expires At | Status | Super Owner |
|---------|-----------|------------|------------|------------|--------|-------------|
| 4037 | 1007 | Phoenix Testa | 2025-12-01 12:24:53 | 2025-12-01 13:25:13 | EXPIRED | Yes |
| 1320 | 1008 | Daniela Mircheva | 2025-08-19 02:42:45 | 2025-08-21 06:06:46 | EXPIRED | No |

### 5. Other Entity Types (8 locks)

| Lock ID | Entity Type | Entity ID | Lock Owner | Created At | Expires At | Status |
|---------|-------------|-----------|------------|------------|------------|--------|
| 4214 | customers | 6031658 | Yusuf Yusuf | 2025-12-16 09:58:42 | 2025-12-16 10:58:42 | EXPIRED |
| 3341 | company-details | 1001 | George Gamjashvili | 2025-11-20 11:29:28 | 2025-12-04 10:57:39 | EXPIRED |
| 2466 | interim-and-advance-payments | 1021 | Ivaylo Papazov | 2025-08-28 09:50:55 | 2025-08-28 10:50:55 | EXPIRED |
| 2094 | terms | 1003 | Militsa Anastasova | 2025-08-22 04:41:20 | 2025-08-22 05:41:20 | EXPIRED |
| 1688 | groups-of-interim-and-advance-payment | 1001 | Ivaylo Papazov | 2025-08-20 05:17:37 | 2025-08-20 10:50:03 | EXPIRED |
| 2552 | groups-of-price-components | 1005 | Desislava Koleva | 2025-08-29 04:13:46 | 2025-08-29 05:13:46 | EXPIRED |
| 1011 | system-messages | 1016 | Rezo Peranidz | 2025-08-14 04:08:00 | 2025-08-14 05:08:00 | EXPIRED |

## Key Observations

1. **All Locks Are Expired:**
   - 100% of locks (20/20) have expired
   - Oldest lock: 2025-08-14 (system-messages:1016)
   - Newest lock: 2025-12-16 (customers:6031658)
   - All locks should be cleaned up

2. **No Active Locks:**
   - No currently active locks in the system
   - All locks have passed their expiration time

3. **No Billing Run Locks:**
   - All locks have `billing_id = NULL`
   - No system locks from billing runs
   - All are user-initiated locks

4. **Lock Owners:**
   - Most active users: Ivaylo Papazov (7 locks), Militsa Anastasova (4 locks)
   - Other users: Jivka Todorova, George Gamjashvili, Daniela Mircheva, Yusuf Yusuf, Rezo Peranidz, Desislava Koleva, Phoenix Testa

5. **Lock Duration:**
   - Most locks had 1-hour expiration (typical user session)
   - One template lock (1320) had extended expiration (~2 days)
   - One company-details lock (3341) had extended expiration (~14 days)

## Recommendations

### 1. Cleanup Expired Locks
All 20 locks are expired and should be removed:

```sql
-- Delete all expired locks
DELETE FROM lock.locks 
WHERE expires_at < CURRENT_TIMESTAMP;
```

### 2. Implement Automatic Cleanup
Consider implementing a scheduled job to automatically remove expired locks:

```sql
-- Example cleanup procedure
CREATE OR REPLACE FUNCTION lock.cleanup_expired_locks()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM lock.locks 
    WHERE expires_at < CURRENT_TIMESTAMP;
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;
```

### 3. Monitor Lock Health
- Set up alerts for high number of expired locks
- Monitor lock expiration patterns
- Track lock cleanup frequency

### 4. Lock Expiration Policy
- Review lock expiration times
- Consider shorter expiration for certain entity types
- Implement lock renewal mechanism for long-running operations

## SQL Queries Used

### Get All Locks
```sql
SELECT 
    l.id AS lock_id,
    l.lock_key,
    l.entity_type,
    l.entity_id,
    l.lock_owner,
    l.created_at,
    l.expires_at,
    l.system_lock,
    l.billing_id,
    CASE 
        WHEN l.expires_at < CURRENT_TIMESTAMP THEN 'EXPIRED'
        ELSE 'ACTIVE'
    END AS lock_status
FROM lock.locks l
ORDER BY l.created_at DESC;
```

### Get Lock Summary by Entity Type
```sql
SELECT 
    l.entity_type,
    COUNT(*) AS lock_count,
    COUNT(DISTINCT l.entity_id) AS unique_entities_locked,
    COUNT(CASE WHEN l.expires_at < CURRENT_TIMESTAMP THEN 1 END) AS expired_locks,
    COUNT(CASE WHEN l.expires_at >= CURRENT_TIMESTAMP THEN 1 END) AS active_locks
FROM lock.locks l
GROUP BY l.entity_type
ORDER BY lock_count DESC;
```

---

*Analysis performed on Test Environment*  
*Date: 2025-01-24*  
*Database: 10.236.20.24:5432/phoenix*

