# Billing Run-ების ავტომატურად COMPLETED სტატუსში გადასვლის ანალიზი

**თარიღი:** 2025-12-25  
**გარემო:** Test (10.236.20.24:5432/phoenix)  
**პრობლემა:** Billing run-ები გადავიდა COMPLETED სტატუსში ავტომატურად, მილიწამების დაშორებით  
**ფოკუსი:** CANCELLED-დან COMPLETED-ზე გადასული billing run-ები

## 🔍 პრობლემის აღწერა

Test გარემოზე billing run-ები ავტომატურად გადავიდა COMPLETED სტატუსში, ხშირად მილიწამების დაშორებით. ეს მიუთითებს, რომ რამდენიმე billing run ერთდროულად დამუშავდა და სტატუსი შეიცვალა.

**⚠️ მნიშვნელოვანი შენიშვნა:** ბაზაში არ არსებობს billing run-ების სტატუსის ისტორიის ცხრილი (`billing_status_change_hist` ან მსგავსი). ამიტომ, **შეუძლებელია ზუსტად დადგინდეს, რომელ billing run-ებს ჰქონდათ CANCELLED წინა სტატუსად**. 

თუმცა, შეიძლება გავაანალიზოთ შემდეგი ინდიკატორები:
- დიდი დროის ინტერვალი create_date-სა და modify_date-ს შორის (შეიძლება მიუთითებდეს, რომ იყო CANCELLED და შემდეგ resume-და)
- Process stage-ის ანალიზი (თუ process stage არის DRAFT, DRAFT_DOCUMENT ან ACCOUNTING, შეიძლება resume-და)

## 📊 ნაპოვნი ჩანაწერები

ბაზაში ნაპოვნია რამდენიმე billing run, რომლებიც COMPLETED სტატუსში გადავიდნენ. ქვემოთ მოცემულია ანალიზი, რომელიც ფოკუსირებულია CANCELLED-დან COMPLETED-ზე გადასვლის შესაძლო შემთხვევებზე:

### კრიტიკული შემთხვევები:

1. **Billing Run ID 1647** - გადავიდა COMPLETED-ში **14.78 მილიწამში** შექმნის შემდეგ
   - Billing Number: BILLING202512230014
   - Create Date: 2025-12-23 13:55:15.351
   - Modify Date: 2025-12-23 13:55:15.366
   - Type: STANDARD_BILLING
   - Process Stage: DRAFT

2. **2025-12-22, 11:54:xx** - **19 billing run** ერთდროულად გადავიდა COMPLETED-ში:
   - IDs: 1601-1619
   - ყველა STANDARD_BILLING ტიპისაა
   - ყველა DRAFT process stage-ზეა
   - Modify dates: 11:53:13 - 11:54:39 (დაახლოებით 1 წუთის განმავლობაში)

## 🔎 მიზეზის იდენტიფიცირება

### 1. ავტომატური Accounting პროცესი

**ფაილი:** `BillingRunStartGenerationService.java` (ხაზები 108-114)

```java
try {
    List<RunStage> runStages = ListUtils.emptyIfNull(billingRun.getRunStages());
    if (CollectionUtils.isNotEmpty(runStages)) {
        if (runStages.contains(RunStage.AUTOMATICALLY_ACCOUNTING)) {
            getNextJobInChain().execute(billingRunId, false, false);  // ⚠️ ავტომატურად იწყებს accounting-ს
        }
    }
}
```

**მიზეზი:** როდესაც billing run-ს აქვს `AUTOMATICALLY_ACCOUNTING` run stage, generation-ის დასრულების შემდეგ იგი **ავტომატურად** იწყებს accounting პროცესს, რაც შემდეგ ავტომატურად აყენებს COMPLETED სტატუსში.

### 2. Accounting-ის დასრულება და COMPLETED სტატუსში გადასვლა

**ფაილი:** `BillingRunStartAccountingService.java` (ხაზები 108-114)

```java
new Thread(() -> {
    try {
        billingRunStartAccountingInvokeService.invoke(billingRun);
        
        log.debug("Billing run status changed to 'COMPLETED'");
        billingRun.setStatus(BillingStatus.COMPLETED);  // ⚠️ ავტომატურად იცვლება COMPLETED-ზე
        billingRunRepository.save(billingRun);
        // ...
    }
}).start();
```

**მიზეზი:** Accounting პროცესის წარმატებით დასრულების შემდეგ, billing run-ის სტატუსი **ავტომატურად** იცვლება COMPLETED-ზე.

### 3. Scheduler-ის როლის გავლენა

**ფაილი:** `BillingRunStandardPreparationStateScheduler.java` (ხაზები 34-40)

```java
@Scheduled(fixedDelay = 15, timeUnit = TimeUnit.SECONDS)
public void stateListener() {
    log.debug(LoggerUtils.prettyLogging("Billing run state listener iteration started %s".formatted(LocalDate.now())));
    ExecutorService processExecutor = Executors.newSingleThreadExecutor();
    processExecutor.submit(stateHandler::finishStandardBillingProcessing);
    processExecutor.submit(stateHandler::startStandardBillingProcessing);
    processExecutor.shutdown();
}
```

**მიზეზი:** Scheduler ყოველ 15 წამში ერთხელ ამოწმებს და ამუშავებს billing run-ებს. თუ რამდენიმე billing run ერთდროულად არის მზად დამუშავებისთვის, ისინი ერთდროულად იწყებენ პროცესს.

### 4. Generation-ის დასრულება და ავტომატური Accounting-ის გაშვება

**ფაილი:** `BillingRunStandardInvoiceGenerationService.java` (ხაზები 140-153)

```java
if (!applicationModelContainsInterimAdvancePayment) {
    billingRun.setStatus(BillingStatus.DRAFT);
    billingRunRepository.save(billingRun);
    
    billingRunRepository.finalizeDataPreparation(billingRun.getId());
    
    invoiceService.generateExcel(billingRunId, InvoiceStatus.DRAFT);
    
    if (ListUtils.emptyIfNull(billingRun.getRunStages()).contains(RunStage.GENERATE_AND_SIGN)) {
        billingRunStartGenerationService.execute(billingRunId, false, false);  // ⚠️ იწყებს generation-ს
    }
}
```

**მიზეზი:** Generation-ის დასრულების შემდეგ, თუ billing run-ს აქვს `GENERATE_AND_SIGN` run stage, იგი იწყებს generation პროცესს, რომელიც შემდეგ ავტომატურად იწყებს accounting-ს (თუ აქვს `AUTOMATICALLY_ACCOUNTING`).

## 🔍 CANCELLED-დან COMPLETED-ზე გადასვლის ანალიზი

### Resume() მეთოდის ანალიზი

**ფაილი:** `BillingRunService.java` (ხაზები 2549-2584)

```java
@Transactional
public void resume(Long billingRunId, boolean mustCheckPermission) {
    BillingRun billingRun = billingRunRepository.findById(billingRunId)
        .orElseThrow(...);
    
    BillingRunProcessStage processStage = billingRun.getProcessStage();
    if (Objects.isNull(processStage)) {
        throw new IllegalArgumentsProvidedException("Cannot resume process, unknown process stage");
    }

    switch (processStage) {
        case DRAFT -> {
            billingRun.setStatus(BillingStatus.IN_PROGRESS_DRAFT);
            // ...
        }
        case DRAFT_DOCUMENT -> {
            billingRun.setStatus(BillingStatus.IN_PROGRESS_GENERATION);
            // ...
        }
        case ACCOUNTING -> {
            billingRun.setStatus(BillingStatus.IN_PROGRESS_ACCOUNTING);
            // ...
        }
    }
}
```

**⚠️ კრიტიკული პრობლემა:** `resume()` მეთოდი **არ ამოწმებს** billing run-ის მიმდინარე სტატუსს (CANCELLED, PAUSED, და ა.შ.). ის მხოლოდ `processStage`-ს ამოწმებს.

**ეს ნიშნავს:**
- თუ billing run-ს აქვს **CANCELLED** სტატუსი, მაგრამ `processStage` არის DRAFT, DRAFT_DOCUMENT ან ACCOUNTING
- `resume()` მეთოდი **შეიძლება გაეშვას** და billing run-ი გადავა IN_PROGRESS სტატუსში
- შემდეგ, თუ billing run-ს აქვს `AUTOMATICALLY_ACCOUNTING` run stage, იგი ავტომატურად გადავა COMPLETED-ზე

### Cancel() მეთოდის ანალიზი

**ფაილი:** `BillingRunService.java` (ხაზები 2475-2513)

```java
public void cancel(Long billingRunId, boolean mustCheckPermission) {
    // ...
    List<BillingStatus> availableStatusesForTermination = List.of(
        BillingStatus.INITIAL,
        BillingStatus.IN_PROGRESS_DRAFT,
        BillingStatus.DRAFT,
        BillingStatus.IN_PROGRESS_GENERATION,
        BillingStatus.GENERATED,
        BillingStatus.PAUSED
    );
    
    if (!availableStatusesForTermination.contains(billingRun.getStatus())) {
        throw new IllegalArgumentsProvidedException("Termination available only for followed statuses: [...]");
    }
    
    // ...
    billingRun.setStatus(BillingStatus.CANCELLED);
    billingRunRepository.save(billingRun);
}
```

**დასკვნა:** Cancel() მეთოდი აყენებს CANCELLED სტატუსში, მაგრამ **არ იცვლის** `processStage`-ს. ეს ნიშნავს, რომ `processStage` შეიძლება დარჩეს DRAFT, DRAFT_DOCUMENT ან ACCOUNTING.

### შესაძლო სცენარი: CANCELLED → COMPLETED

1. Billing run იქმნება და `processStage` არის DRAFT
2. Billing run გადადის GENERATED სტატუსში
3. Billing run-ს აქვს `AUTOMATICALLY_ACCOUNTING` run stage
4. Billing run იწყებს accounting პროცესს
5. **რაიმე მიზეზით** billing run-ი გადადის CANCELLED სტატუსში (მაგ. manual cancel)
6. `processStage` კვლავ დარჩება ACCOUNTING (ან DRAFT_DOCUMENT)
7. `resume()` მეთოდი გაეშვება (მანუალურად ან scheduler-ის მიერ)
8. `resume()` ამოწმებს მხოლოდ `processStage`-ს, არა სტატუსს
9. Billing run გადადის IN_PROGRESS_ACCOUNTING სტატუსში
10. Accounting პროცესი დასრულდება
11. Billing run **ავტომატურად** გადადის COMPLETED სტატუსში

## 🎯 რატომ ხდება ეს?

### სცენარი 1: AUTOMATICALLY_ACCOUNTING Run Stage-ით

1. Billing run იქმნება `AUTOMATICALLY_ACCOUNTING` run stage-ით
2. Generation პროცესი დასრულდება და სტატუსი იცვლება GENERATED-ზე
3. **ავტომატურად** იწყება accounting პროცესი (BillingRunStartGenerationService, ხაზი 112)
4. Accounting პროცესი დასრულდება
5. **ავტომატურად** სტატუსი იცვლება COMPLETED-ზე (BillingRunStartAccountingService, ხაზი 113)

### სცენარი 2: Resume() + BillingRunStartAccountingScheduler (AUTOMATICALLY_ACCOUNTING-ის გარეშე)

**⚠️ ეს არის მთავარი მიზეზი, რატომ billing run-ები გადავიდა COMPLETED-ში AUTOMATICALLY_ACCOUNTING-ის გარეშე!**

1. Billing run-ს აქვს **GENERATED** სტატუსი და **ACCOUNTING** `processStage` (მაგ. წინა accounting პროცესი დაიწყო, მაგრამ შეწყდა)
2. `resume()` მეთოდი გაეშვება (მანუალურად ან scheduler-ის მიერ)
3. `resume()` ამოწმებს მხოლოდ `processStage`-ს, არა სტატუსს
4. თუ `processStage` არის ACCOUNTING, `resume()` აყენებს სტატუსს **IN_PROGRESS_ACCOUNTING**-ზე
5. **BillingRunStartAccountingScheduler** (PostConstruct-ში) ამოწმებს IN_PROGRESS_ACCOUNTING სტატუსის billing run-ებს
6. Scheduler **ავტომატურად** იწყებს accounting პროცესს `accountingService.execute(billingRunId, true, false)` - სადაც `isResumeProcess = true`
7. Accounting პროცესი დასრულდება
8. Billing run **ავტომატურად** გადადის COMPLETED სტატუსში

**კოდი:**
- `BillingRunService.java:2579-2581` - resume() აყენებს IN_PROGRESS_ACCOUNTING-ზე
- `BillingRunStartAccountingScheduler.java:35` - ამოწმებს IN_PROGRESS_ACCOUNTING სტატუსის billing run-ებს
- `BillingRunStartAccountingScheduler.java:54` - ავტომატურად იწყებს accounting-ს `isResumeProcess = true`-ით
- `BillingRunStartAccountingService.java:88-92` - თუ `isResumeProcess = true`, არ ამოწმებს GENERATED სტატუსს

### სცენარი 3: Scheduler-ის გავლენა (AUTOMATICALLY_ACCOUNTING-ით)

1. რამდენიმე billing run ერთდროულად არის DRAFT სტატუსში
2. Scheduler (ყოველ 15 წამში) ამოწმებს და ამუშავებს მათ
3. თუ ყველას აქვს `AUTOMATICALLY_ACCOUNTING`, ისინი ერთდროულად იწყებენ accounting-ს
4. Accounting-ის დასრულების შემდეგ, ყველა ერთდროულად გადავიდა COMPLETED-ში

## 🔍 Billing Run ID 1620 - კონკრეტული ანალიზი

### რატომ გადავიდა COMPLETED-ში AUTOMATICALLY_ACCOUNTING-ის გარეშე?

**Billing Run ID 1620:**
- Billing Number: BILLING202512220055
- Type: STANDARD_BILLING
- Process Stage: DRAFT (COMPLETED-ში გადასვლის დროს)
- Create Date: 2025-12-22 11:02:08.136
- Modify Date: 2025-12-22 11:02:28.387
- Seconds Between: 20.25 წამი

**შესაძლო სცენარი:**

1. Billing run შეიქმნა და გადავიდა GENERATED სტატუსში
2. Accounting პროცესი დაიწყო (მანუალურად ან სხვა გზით) და `processStage` გახდა ACCOUNTING
3. **რაიმე მიზეზით** accounting პროცესი შეწყდა ან billing run-ი გადავიდა DRAFT სტატუსში, მაგრამ `processStage` დარჩა ACCOUNTING (ან გადავიდა DRAFT-ზე)
4. `resume()` მეთოდი გაეშვება (მანუალურად ან scheduler-ის მიერ)
5. `resume()` ამოწმებს `processStage`-ს:
   - თუ `processStage` არის ACCOUNTING → აყენებს IN_PROGRESS_ACCOUNTING-ზე
   - თუ `processStage` არის DRAFT → აყენებს IN_PROGRESS_DRAFT-ზე
6. **BillingRunStartAccountingScheduler** (PostConstruct-ში) ამოწმებს IN_PROGRESS_ACCOUNTING სტატუსის billing run-ებს
7. Scheduler **ავტომატურად** იწყებს accounting პროცესს
8. Accounting პროცესი დასრულდება (20.25 წამში)
9. Billing run **ავტომატურად** გადადის COMPLETED სტატუსში

**⚠️ მნიშვნელოვანი:** ეს ხდება **AUTOMATICALLY_ACCOUNTING run stage-ის გარეშე**, რადგან:
- `BillingRunStartAccountingScheduler` ამოწმებს IN_PROGRESS_ACCOUNTING სტატუსის billing run-ებს
- Scheduler იწყებს accounting-ს `isResumeProcess = true`-ით
- `BillingRunStartAccountingService.execute()` არ ამოწმებს GENERATED სტატუსს, თუ `isResumeProcess = true`

## 📋 დეტალური ანალიზი

### Billing Run-ები, რომლებიც სწრაფად გადავიდნენ COMPLETED-ში:

| ID | Billing Number | Seconds Between | Type | Process Stage |
|----|----------------|-----------------|------|---------------|
| 1647 | BILLING202512230014 | 0.014 | STANDARD_BILLING | DRAFT |
| 1620 | BILLING202512220055 | 20.25 | STANDARD_BILLING | DRAFT |
| 1633 | BILLING202512220068 | 25.98 | MANUAL_CREDIT_OR_DEBIT_NOTE | ACCOUNTING |
| 1632 | BILLING202512220067 | 11.52 | MANUAL_CREDIT_OR_DEBIT_NOTE | ACCOUNTING |
| 1631 | BILLING202512220066 | 13.27 | MANUAL_INTERIM_AND_ADVANCE_PAYMENT | ACCOUNTING |

### ერთდროულად დასრულებული Billing Run-ები (2025-12-22, 11:54:xx):

**19 billing run** გადავიდა COMPLETED-ში დაახლოებით 1 წუთის განმავლობაში:
- IDs: 1601-1619
- ყველა STANDARD_BILLING ტიპისაა
- ყველა DRAFT process stage-ზეა
- Create dates: 10:33:36 - 10:36:08
- Modify dates: 11:53:13 - 11:54:39

ეს მიუთითებს, რომ:
1. ყველა ეს billing run ერთდროულად შეიქმნა (დაახლოებით 2.5 წუთის განმავლობაში)
2. ყველა ერთდროულად დამუშავდა
3. ყველა ერთდროულად გადავიდა COMPLETED-ში (დაახლოებით 1 წუთის განმავლობაში)

## 🔧 რეკომენდაციები CANCELLED-დან COMPLETED-ზე გადასვლის თავიდან ასაცილებლად

### 1. Resume() მეთოდის გაუმჯობესება

**პრობლემა:** `resume()` მეთოდი არ ამოწმებს billing run-ის მიმდინარე სტატუსს.

**გადაწყვეტილება:**
```java
@Transactional
public void resume(Long billingRunId, boolean mustCheckPermission) {
    BillingRun billingRun = billingRunRepository.findById(billingRunId)
        .orElseThrow(...);
    
    // ⚠️ დამატება: შეამოწმეთ, რომ billing run არ არის CANCELLED
    if (billingRun.getStatus() == BillingStatus.CANCELLED) {
        throw new IllegalArgumentsProvidedException(
            "Cannot resume billing run with CANCELLED status. Billing run must be cancelled first or recreated."
        );
    }
    
    // ⚠️ დამატება: შეამოწმეთ, რომ billing run არ არის DELETED
    if (billingRun.getStatus() == BillingStatus.DELETED) {
        throw new IllegalArgumentsProvidedException(
            "Cannot resume billing run with DELETED status."
        );
    }
    
    BillingRunProcessStage processStage = billingRun.getProcessStage();
    // ... existing code ...
}
```

### 2. Cancel() მეთოდის გაუმჯობესება

**პრობლემა:** `cancel()` მეთოდი არ იცვლის `processStage`-ს, რაც შეიძლება გამოიწვიოს პრობლემები resume-ის დროს.

**გადაწყვეტილება:**
```java
public void cancel(Long billingRunId, boolean mustCheckPermission) {
    // ... existing code ...
    
    billingRun.setStatus(BillingStatus.CANCELLED);
    // ⚠️ დამატება: processStage-ის გაუქმება
    billingRun.setProcessStage(null);
    billingRunRepository.save(billingRun);
}
```

### 3. Scheduler-ის გაუმჯობესება

**პრობლემა:** Scheduler-ები არ ამოწმებენ CANCELLED სტატუსს, როდესაც ამუშავებენ billing run-ებს.

**გადაწყვეტილება:**
- `BillingRunStandardPreparationStateHandler`-ში დაამატეთ შემოწმება, რომ billing run არ არის CANCELLED
- `BillingRunStartAccountingScheduler`-ში დაამატეთ შემოწმება, რომ billing run არ არის CANCELLED

## 🔧 რეკომენდაციები

### 1. Run Stage-ების შემოწმება

შეამოწმეთ, რომელ billing run-ებს აქვთ `AUTOMATICALLY_ACCOUNTING` run stage:

```sql
SELECT 
    id,
    billing_number,
    status,
    run_stage,
    modify_date
FROM billing.billings
WHERE run_stage IS NOT NULL
AND modify_date >= CURRENT_DATE - INTERVAL '7 days'
ORDER BY modify_date DESC;
```

### 2. ლოგების შემოწმება

შეამოწმეთ აპლიკაციის ლოგები შემდეგი თარიღებისთვის:
- 2025-12-22 11:53:xx - 11:54:xx (19 billing run ერთდროულად)
- 2025-12-23 13:55:15 (ID 1647 - 14.78ms)

მოძებნეთ:
- `"Billing run status changed to 'COMPLETED'"`
- `"AUTOMATICALLY_ACCOUNTING"`
- `"Starting billing run {} accounting"`

### 3. კოდის ანალიზი

**პრობლემური ადგილები:**

1. **BillingRunStartGenerationService.java:112** - ავტომატურად იწყებს accounting-ს
2. **BillingRunStartAccountingService.java:113** - ავტომატურად იცვლის COMPLETED-ზე
3. **BillingRunStandardPreparationStateScheduler.java:34** - scheduler ყოველ 15 წამში

### 4. შესაძლო გადაწყვეტილებები

1. **Run Stage-ების კონტროლი:**
   - შეამოწმეთ, რომელ billing run-ებს აქვთ `AUTOMATICALLY_ACCOUNTING`
   - განიხილეთ, არის თუ არა ეს სწორი ქცევა

2. **Scheduler-ის ოპტიმიზაცია:**
   - განიხილეთ scheduler-ის სიხშირის შემცირება
   - ან billing run-ების batch processing-ის დამატება

3. **ლოგირების გაუმჯობესება:**
   - დაამატეთ ლოგები, რომელიც აჩვენებს, როდის და რატომ იცვლება სტატუსი
   - დაამატეთ ლოგები run stage-ების შემოწმებისთვის

4. **სტატუსის ისტორიის ცხრილი:**
   - განიხილეთ `billing_status_change_hist` ცხრილის დამატება (მსგავსი `account_period_status_change_hist`-ისა)
   - ეს დაგვეხმარება სტატუსის ცვლილებების ისტორიის თვალყურდევნებაში
   - ეს განსაკუთრებით მნიშვნელოვანია CANCELLED-დან COMPLETED-ზე გადასვლის დასადგენად

5. **CANCELLED სტატუსის შემოწმება:**
   - ყველა scheduler-ში დაამატეთ შემოწმება, რომ billing run არ არის CANCELLED
   - `resume()` მეთოდში დაამატეთ CANCELLED სტატუსის შემოწმება
   - `cancel()` მეთოდში განიხილეთ `processStage`-ის გაუქმება

## 📌 დასკვნა

Billing run-ები ავტომატურად გადავიდა COMPLETED სტატუსში შემდეგი მიზეზების გამო:

### ძირითადი მიზეზები:

1. **AUTOMATICALLY_ACCOUNTING Run Stage:** როდესაც billing run-ს აქვს ეს run stage, იგი ავტომატურად იწყებს accounting პროცესს generation-ის დასრულების შემდეგ.

2. **ავტომატური სტატუსის ცვლილება:** Accounting პროცესის წარმატებით დასრულების შემდეგ, სტატუსი ავტომატურად იცვლება COMPLETED-ზე.

3. **Scheduler-ის გავლენა:** Scheduler ყოველ 15 წამში ამოწმებს და ამუშავებს billing run-ებს, რაც შეიძლება გამოიწვიოს რამდენიმე billing run-ის ერთდროულად დამუშავება.

4. **Thread-based Processing:** Accounting პროცესი მიმდინარეობს ახალ Thread-ში, რაც შეიძლება გამოიწვიოს რამდენიმე billing run-ის ერთდროულად დასრულება.

5. **Resume() + BillingRunStartAccountingScheduler (AUTOMATICALLY_ACCOUNTING-ის გარეშე):** 
   - `resume()` მეთოდი, თუ `processStage` არის ACCOUNTING, აყენებს სტატუსს IN_PROGRESS_ACCOUNTING-ზე
   - `BillingRunStartAccountingScheduler` (PostConstruct-ში) ამოწმებს IN_PROGRESS_ACCOUNTING სტატუსის billing run-ებს
   - Scheduler **ავტომატურად** იწყებს accounting პროცესს `isResumeProcess = true`-ით
   - Accounting პროცესი დასრულდება და billing run **ავტომატურად** გადადის COMPLETED-ზე
   - **ეს ხდება AUTOMATICALLY_ACCOUNTING run stage-ის გარეშე!**

### CANCELLED-დან COMPLETED-ზე გადასვლის მიზეზი:

6. **Resume() მეთოდის ნაკლი:** `resume()` მეთოდი არ ამოწმებს billing run-ის მიმდინარე სტატუსს (CANCELLED, DELETED). ის მხოლოდ `processStage`-ს ამოწმებს, რაც შეიძლება გამოიწვიოს CANCELLED billing run-ის resume-და და შემდეგ COMPLETED-ზე გადასვლა.

**დასკვნა:** Billing run-ები შეიძლება გადავიდნენ COMPLETED სტატუსში **ორი გზით:**
1. **AUTOMATICALLY_ACCOUNTING run stage-ით** - generation-ის დასრულების შემდეგ ავტომატურად იწყებს accounting-ს
2. **Resume() + BillingRunStartAccountingScheduler-ით** - თუ `processStage` არის ACCOUNTING, resume() აყენებს IN_PROGRESS_ACCOUNTING-ზე, და scheduler ავტომატურად იწყებს accounting-ს

**CANCELLED-დან COMPLETED-ზე გადასვლა არ არის მოსალოდნელი ქცევა** და საჭიროებს გასწორებას `resume()` მეთოდში.

---

**ანგარიში გენერირებულია:** 2025-12-25  
**ბაზა:** phoenix (Test Environment - 10.236.20.24:5432)  
**ანალიზი:** Billing run-ების ავტომატური COMPLETED სტატუსში გადასვლის მიზეზების იდენტიფიცირება

