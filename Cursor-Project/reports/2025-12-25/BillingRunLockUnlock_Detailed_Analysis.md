# Lock-ების გათავისუფლების მექანიზმი - დეტალური ანალიზი

**თარიღი:** 2025-12-25  
**ანალიზი:** როგორ მუშაობს lock-ების გათავისუფლება, როდესაც ბილინგის რუნი კომპლეტდება  
**კოდის ლოკაცია:** `BillingRunStartAccountingService.java`

---

## 📋 Executive Summary

ბილინგის რუნის completion-ის დროს lock-ების გათავისუფლება **დამოკიდებულია stored procedure-ზე** `billing_run.make_billing_run_real(?)`. Java კოდში **არ არსებობს explicit unlock logic**, რაც ქმნის რისკს lock-ების დარჩენის შესახებ.

### ⚠️ პრობლემა
- BILLING202512220055-ს აქვს **1,291 lock** COMPLETED სტატუსის მიუხედავად
- Java კოდში არ არის fallback mechanism
- დამოკიდებულება მხოლოდ stored procedure-ზე

---

## 🔄 Billing Run Completion Flow

### 1. Java კოდის სტრუქტურა

**ფაილი:** `BillingRunStartAccountingService.java`  
**მეთოდი:** `execute(Long billingRunId, ...)`

#### Step-by-Step პროცესი:

```java
// 1. სტატუსის განახლება IN_PROGRESS_ACCOUNTING-ზე
billingRun.setProcessStage(BillingRunProcessStage.ACCOUNTING);
billingRun.setStatus(BillingStatus.IN_PROGRESS_ACCOUNTING);
billingRunRepository.save(billingRun);

// 2. ახალი Thread-ის გაშვება (ასინქრონული დამუშავება)
new Thread(() -> {
    try {
        // 3. Accounting პროცესის გაშვება
        billingRunStartAccountingInvokeService.invoke(billingRun);
        
        // 4. სტატუსის განახლება COMPLETED-ზე
        billingRun.setStatus(BillingStatus.COMPLETED);
        billingRunRepository.save(billingRun);
        
        // 5. Excel რეპორტის გენერაცია (optional)
        tryToGenerateExcelReportFile(billingRunId);
        
        // 6. Stored Procedure-ის გამოძახება
        Session session = entityManager.unwrap(Session.class);
        session.doWork((work) -> {
            Long runId = billingRun.getId();
            CallableStatement statement = work.prepareCall(
                "CALL billing_run.make_billing_run_real(?)"
            );
            statement.setLong(1, runId);
            statement.execute();  // ⚠️ აქ უნდა მოხდეს lock-ების გათავისუფლება
        });
        
    } catch (Exception e) {
        // 7. Error handling - სტატუსი იცვლება GENERATED-ზე
        billingRun.setStatus(BillingStatus.GENERATED);
        billingRunErrorService.publishBillingErrors(...);
    }
    
    // 8. Outdated დოკუმენტების წაშლა
    billingRunOutdatedDocumentService.deleteOutdatedDocuments(...);
    
    // 9. Notification-ის გაგზავნა
    notificationEventPublisher.publishNotification(...);
}).start();
```

### 2. კრიტიკული წერტილები

#### ⚠️ პრობლემური ადგილები:

1. **Lock Unlock-ის არარსებობა Java კოდში:**
   ```java
   // ❌ არ არსებობს:
   lockRepository.deleteByBillingId(billingRun.getId());
   ```

2. **დამოკიდებულება Stored Procedure-ზე:**
   - Lock-ების გათავისუფლება მხოლოდ `billing_run.make_billing_run_real(?)` პროცედურაზეა დამოკიდებული
   - თუ პროცედურა არ შეიცავს DELETE statement-ს, lock-ები დარჩება

3. **Exception Handling:**
   ```java
   catch (Exception e) {
       billingRun.setStatus(BillingStatus.GENERATED);
       // ❌ აქ არ ხდება lock-ების გათავისუფლება
   }
   ```

4. **Thread-ის გამოყენება:**
   - პროცესი მიმდინარეობს ახალ Thread-ში
   - თუ Thread crash-დება, lock-ები დარჩება

---

## 🔍 Lock-ების სტრუქტურა

### Lock Entity

**ცხრილი:** `lock.locks`  
**Schema:** `lock`

| სვეტი | ტიპი | აღწერა |
|-------|------|--------|
| `id` | String (PK) | Lock-ის უნიკალური იდენტიფიკატორი |
| `lock_key` | String | Lock-ის გასაღები (entity_type + entity_id) |
| `entity_type` | String | Entity-ის ტიპი (customers, contracts, etc.) |
| `entity_id` | String | Entity-ის ID |
| `billing_id` | Long | Billing run-ის ID (nullable) |
| `system_lock` | Boolean | System lock-ია თუ user lock-ია |

### Lock-ების ტიპები Billing Run-ისთვის

ბილინგის რუნის დროს lock-დება შემდეგი entity-ები:

1. **points-of-delivery** - მიწოდების წერტილები
2. **data-by-profiles** - პროფილის მონაცემები
3. **data-by-scales** - სკალის მონაცემები
4. **customers** - კლიენტები
5. **energy-product-contracts** - ენერგიის პროდუქტის კონტრაქტები
6. **price-components** - ფასის კომპონენტები
7. **groups-of-price-components** - ფასის კომპონენტების ჯგუფები
8. **vat-rates** - დღგ-ის განაკვეთები
9. **currencies** - ვალუტები
10. **energy-products** - ენერგიის პროდუქტები
11. **price-parameters** - ფასის პარამეტრები

---

## 🗄️ Stored Procedure - make_billing_run_real

### პროცედურის გამოძახება

```java
CallableStatement statement = work.prepareCall(
    "CALL billing_run.make_billing_run_real(?)"
);
statement.setLong(1, runId);
statement.execute();
```

### რა უნდა აკეთებდეს პროცედურამ:

1. **ბილინგის რუნის finalization:**
   - ინვოისების სტატუსის განახლება REAL-ზე
   - დოკუმენტების finalization
   - სხვა cleanup ოპერაციები

2. **Lock-ების გათავისუფლება:**
   ```sql
   DELETE FROM lock.locks 
   WHERE billing_id = :billingRunId;
   ```

### ⚠️ პრობლემა

**Stored procedure-ის კოდი არ არის ხელმისაწვდომი** კოდბეიზში. ის ალბათ:
- ბაზაშია განსაზღვრული
- Migration script-ებშია
- ან სხვა SQL ფაილებში

**დასკვნა:** ვერ ვადასტურებთ, რომ პროცედურა რეალურად ასრულებს lock-ების DELETE-ს.

---

## 📊 LockRepository ანალიზი

### არსებული მეთოდები

**ფაილი:** `LockRepository.java`

```java
@Repository
public interface LockRepository extends JpaRepository<Lock, String> {
    
    // ✅ არსებობს:
    List<Lock> findAllByLockKey(String lockKey);
    
    // ❌ არ არსებობს:
    // void deleteByBillingId(Long billingId);
    // void deleteAllByBillingId(Long billingId);
}
```

### რა აკლია:

1. **deleteByBillingId მეთოდი:**
   ```java
   @Modifying
   @Query("DELETE FROM Lock l WHERE l.billingId = :billingId")
   void deleteByBillingId(@Param("billingId") Long billingId);
   ```

2. **Custom query lock-ების წასაშლელად:**
   ```java
   @Modifying
   @Query("DELETE FROM Lock l WHERE l.billingId = :billingId AND l.systemLock = true")
   void deleteSystemLocksByBillingId(@Param("billingId") Long billingId);
   ```

---

## 🔄 რეალური სცენარი - BILLING202512220055

### რა მოხდა:

1. **ბილინგის რუნი შეიქმნა:**
   - ID: 1620
   - Billing Number: BILLING202512220055
   - Created: 2025-12-22 11:02:08

2. **Lock-ები შეიქმნა:**
   - 1,291 lock სხვადასხვა entity-ზე
   - System lock-ები (billing_id = 1620)

3. **ბილინგის რუნი COMPLETED გახდა:**
   - Status: COMPLETED
   - Process Stage: DRAFT
   - Modified: 2025-12-22 11:02:28

4. **Lock-ები დარჩა:**
   - ❌ 1,291 lock კვლავ არსებობს
   - ❌ Stored procedure-მა არ გაათავისუფლა lock-ები

### შესაძლო მიზეზები:

1. **Stored Procedure-ის პრობლემა:**
   - პროცედურა შეიძლება არ შეიცავდეს DELETE statement-ს
   - ან DELETE statement-მა ვერ შეასრულა (error, exception)

2. **Exception Handling:**
   - თუ პროცედურამ exception გადააგდო, lock-ები დარჩა
   - Java კოდში exception catch-დება, მაგრამ unlock-ი არ ხდება

3. **Transaction პრობლემა:**
   - თუ transaction rollback-და, lock-ების DELETE-იც rollback-და

4. **Timing Issue:**
   - Lock-ები შეიძლება შეიქმნა პროცედურის შემდეგ
   - ან პროცედურამ გააკეთა DELETE, მაგრამ შემდეგ lock-ები კვლავ შეიქმნა

---

## 💡 რეკომენდაციები

### 1. დაუყოვნებლივი გამოსწორება

#### A. LockRepository-ში მეთოდის დამატება:

```java
@Repository
public interface LockRepository extends JpaRepository<Lock, String> {
    
    List<Lock> findAllByLockKey(String lockKey);
    
    // ✅ დამატება:
    @Modifying
    @Query("DELETE FROM Lock l WHERE l.billingId = :billingId")
    void deleteByBillingId(@Param("billingId") Long billingId);
    
    @Modifying
    @Query("DELETE FROM Lock l WHERE l.billingId = :billingId AND l.systemLock = true")
    void deleteSystemLocksByBillingId(@Param("billingId") Long billingId);
}
```

#### B. BillingRunStartAccountingService-ში Unlock Logic-ის დამატება:

```java
// BillingRunStartAccountingService.java - execute() მეთოდში

new Thread(() -> {
    try {
        billingRunStartAccountingInvokeService.invoke(billingRun);
        
        billingRun.setStatus(BillingStatus.COMPLETED);
        billingRunRepository.save(billingRun);
        
        tryToGenerateExcelReportFile(billingRunId);
        
        // Stored Procedure-ის გამოძახება
        Session session = entityManager.unwrap(Session.class);
        session.doWork((work) -> {
            Long runId = billingRun.getId();
            CallableStatement statement = work.prepareCall(
                "CALL billing_run.make_billing_run_real(?)"
            );
            statement.setLong(1, runId);
            statement.execute();
        });
        
        // ✅ დამატება: Explicit unlock logic
        try {
            lockRepository.deleteByBillingId(billingRun.getId());
            log.debug("Successfully unlocked locks for billing run: {}", billingRunId);
        } catch (Exception unlockException) {
            log.error("Failed to unlock locks for billing run: {}", 
                     billingRunId, unlockException);
            // არ ვაგდებთ exception-ს, რადგან billing run უკვე COMPLETED-ია
        }
        
    } catch (Exception e) {
        billingRun.setStatus(BillingStatus.GENERATED);
        // ✅ დამატება: Unlock-ი error-ის შემთხვევაშიც
        try {
            lockRepository.deleteByBillingId(billingRun.getId());
            log.debug("Unlocked locks after error for billing run: {}", billingRunId);
        } catch (Exception unlockException) {
            log.error("Failed to unlock locks after error for billing run: {}", 
                     billingRunId, unlockException);
        }
        billingRunErrorService.publishBillingErrors(...);
    }
    
    // ... rest of the code
}).start();
```

### 2. Stored Procedure-ის შემოწმება

```sql
-- ბაზაში შემოწმება:
SELECT pg_get_functiondef(oid) 
FROM pg_proc 
WHERE proname = 'make_billing_run_real' 
  AND pronamespace = (SELECT oid FROM pg_namespace WHERE nspname = 'billing_run');

-- ან:
\df+ billing_run.make_billing_run_real
```

**რა უნდა შეიცავდეს:**
```sql
CREATE OR REPLACE PROCEDURE billing_run.make_billing_run_real(billing_run_id BIGINT)
LANGUAGE plpgsql
AS $$
BEGIN
    -- ... other logic ...
    
    -- Lock-ების გათავისუფლება
    DELETE FROM lock.locks 
    WHERE billing_id = billing_run_id;
    
    -- ... other logic ...
END;
$$;
```

### 3. Monitoring და Alerting

```java
// Completion-ის შემდეგ შემოწმება:
Long lockCount = lockRepository.countByBillingId(billingRun.getId());
if (lockCount > 0) {
    log.warn("Billing run {} completed but still has {} locks!", 
             billingRun.getId(), lockCount);
    // Alert-ის გაგზავნა
    alertService.sendAlert("BillingRunLockIssue", 
                          "Billing run " + billingRun.getId() + 
                          " completed with " + lockCount + " remaining locks");
}
```

### 4. Scheduled Cleanup Job

```java
@Scheduled(cron = "0 0 2 * * ?") // ყოველ დილას 2 საათზე
public void cleanupOrphanedLocks() {
    // ვპოულობთ COMPLETED billing run-ებს lock-ებით
    List<BillingRun> completedRunsWithLocks = billingRunRepository
        .findCompletedRunsWithLocks();
    
    for (BillingRun run : completedRunsWithLocks) {
        try {
            lockRepository.deleteByBillingId(run.getId());
            log.info("Cleaned up locks for billing run: {}", run.getId());
        } catch (Exception e) {
            log.error("Failed to cleanup locks for billing run: {}", 
                     run.getId(), e);
        }
    }
}
```

---

## 📈 სტატისტიკა

### Test Environment-ში:

- **COMPLETED billing runs with locks:** 6
- **Total locks:** 1,358
- **Critical case:** BILLING202512220055 - 1,291 locks

### Dev Environment-ში:

- **COMPLETED billing runs with locks:** 178
- **Total locks:** 201,000+
- **Critical cases:**
  - BILLING202511200006 - 156,651 locks
  - BILLING202512020002 - 44,431 locks

---

## 🎯 დასკვნა

### არსებული სიტუაცია:

1. ✅ **Java კოდი:** არ შეიცავს explicit unlock logic-ს
2. ⚠️ **Stored Procedure:** უცნობია, შეიცავს თუ არა DELETE statement-ს
3. ❌ **Fallback Mechanism:** არ არსებობს
4. ⚠️ **Error Handling:** Exception-ის შემთხვევაში lock-ები დარჩება

### რეკომენდებული გამოსწორება:

1. **LockRepository-ში** `deleteByBillingId` მეთოდის დამატება
2. **BillingRunStartAccountingService-ში** explicit unlock logic-ის დამატება
3. **Stored Procedure-ის** შემოწმება და გამოსწორება (თუ საჭიროა)
4. **Monitoring** და **Alerting** სისტემის დამატება
5. **Scheduled Cleanup Job** orphaned lock-ების წასაშლელად

---

**დოკუმენტაცია შექმნილია:** 2025-12-25  
**ბოლო განახლება:** 2025-12-25

