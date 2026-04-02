# Zero-amount liability and receivable prevention – All generation flows (PDT-2474)

**Jira:** PDT-2474 (Phoenix Delivery)  
**Type:** Customer Feedback  
**Summary:** Liabilities and receivables should not be generated with amount zero. Every flow that generates liabilities or receivables must prevent creation of entities with initialAmount == 0.  
**Scope:** All liability and receivable creation flows — manual creation, billing run, deposit, late payment fine, rescheduling, payment, action, goods/service orders, invoice cancellation, invoice reversal, payment reversal, MLO reversal, LPF reversal, rescheduling reversal, credit note, compensation, VAT base adjustment, disconnection, JPA listener, negative-amount validation, and invoice correction. Core implementation: ZeroAmountValidationListener (@PrePersist on CustomerLiability and CustomerReceivable) throws IllegalArgumentException when initialAmount == 0. Request-level validation: @DecimalMin(value="0", inclusive=false) on CustomerLiabilityRequest.initialAmount, @Positive on CustomerReceivableRequest.initialAmount.

---

## Test data (preconditions)

The following shared setup applies to test cases that require a full billing chain. Individual test cases reference these steps and add scenario-specific variations.

### Shared billing chain (referenced as "Billing Chain Steps 1–9")

1. **Create customer** via `POST /customer` with parameters: `{ type: "PRIVATE", firstName: "Test", lastName: "User", identificationNumber: "EGN1234567890", status: "ACTIVE" }`. Note the returned `customerId`.
2. **Create POD** via `POST /pod` with parameters: `{ type: "ELECTRICITY", identifier: "BG-POD-TEST-001", status: "ACTIVE", activationDate: "2024-01-01" }`. Note the returned `podId`.
3. **Create product** via `POST /product` with parameters: `{ name: "Standard Electricity", term: "INDEFINITE", dataDeliveryType: "BY_PROFILE", status: "ACTIVE" }`. Note the returned `productId`.
4. **Create terms** via `POST /terms` with parameters: `{ productId: <productId from step 3>, name: "Standard Terms", validFrom: "2024-01-01" }`. Note the returned `termsId`.
5. **Create price component** via `POST /price-component` with parameters: `{ productId: <productId from step 3>, type: "ENERGY", rate: 0.15, currency: "BGN", unit: "kWh", validFrom: "2024-01-01" }`. Note the returned `priceComponentId`.
6. **Create product contract** via `POST /product-contract` with parameters: `{ customerId: <customerId from step 1>, podId: <podId from step 2>, productId: <productId from step 3>, status: "ACTIVE", entryIntoForceDate: "2025-01-01" }`. Note the returned `contractId`.
7. **Create energy data / billing profile** via the energy data endpoint (e.g. `POST /energy-data`) with parameters: `{ contractId: <contractId from step 6>, podId: <podId from step 2>, consumptionKwh: 1000, periodFrom: "2025-01-01", periodTo: "2025-01-31" }`.
8. **Create billing run** via `POST /billing-run` with parameters: `{ type: "STANDARD", periodFrom: "2025-01-01", periodTo: "2025-01-31", contractId: <contractId from step 6> }`. Note the returned `billingRunId`.
9. **Execute billing run** via `POST /billing-run/{billingRunId}/execute`. Wait for completion. The billing run generates an invoice. Note the returned `invoiceId` and `invoiceTotal`.

### Shared customer-only setup (referenced as "Customer Setup Step 1")

1. **Create customer** via `POST /customer` with parameters: `{ type: "PRIVATE", firstName: "Test", lastName: "User", identificationNumber: "EGN1234567890", status: "ACTIVE" }`. Note the returned `customerId`.

---

## Backend Test Cases

### TC-BE-1 (Positive): Manual liability creation with valid non-zero amount

**Description:** Verify that a liability can be created manually via API when the initialAmount is a valid non-zero positive value.

**Preconditions:**
1. Create customer via `POST /customer` with parameters: `{ type: "PRIVATE", firstName: "Manual", lastName: "LiabilityTest", identificationNumber: "EGN0000000001", status: "ACTIVE" }`. Note the returned `customerId`.

**Test steps:**
1. Send `POST /customer-liability` with payload: `{ customerId: <customerId from precondition step 1>, initialAmount: 150.00, currency: "BGN", description: "Manual test liability" }`.
2. Assert HTTP response status is `201 Created`.
3. Assert response body contains `id` (non-null), `initialAmount: 150.00`, `currency: "BGN"`, and `status: "ACTIVE"`.
4. Send `GET /customer-liability/{liabilityId}` using the returned `id`.
5. Assert HTTP response status is `200 OK` and returned `initialAmount` equals `150.00`.

**Expected test case results:**
- Liability is created successfully with `initialAmount = 150.00`.
- Response status is `201 Created`.
- Persisted liability has correct amount, currency, and active status.

---

### TC-BE-2 (Negative): Manual liability creation with zero amount rejected by @DecimalMin

**Description:** Verify that attempting to create a liability with initialAmount = 0 is rejected at the request validation level (@DecimalMin(value="0", inclusive=false)).

**Preconditions:**
1. Create customer via `POST /customer` with parameters: `{ type: "PRIVATE", firstName: "ZeroLiab", lastName: "Test", identificationNumber: "EGN0000000002", status: "ACTIVE" }`. Note the returned `customerId`.

**Test steps:**
1. Send `POST /customer-liability` with payload: `{ customerId: <customerId from precondition step 1>, initialAmount: 0, currency: "BGN", description: "Zero amount liability" }`.
2. Assert HTTP response status is `400 Bad Request`.
3. Assert response body contains a validation error referencing `initialAmount` and the constraint `@DecimalMin`.
4. Send `GET /customer-liability?customerId=<customerId>` to list liabilities for the customer.
5. Assert no liability with `initialAmount = 0` exists for the customer.

**Expected test case results:**
- Request is rejected with `400 Bad Request`.
- Validation error message references `initialAmount` must be greater than 0.
- No zero-amount liability is persisted.

---

### TC-BE-3 (Positive): Manual receivable creation with valid non-zero amount

**Description:** Verify that a receivable can be created manually via API when the initialAmount is a valid non-zero positive value.

**Preconditions:**
1. Create customer via `POST /customer` with parameters: `{ type: "PRIVATE", firstName: "Manual", lastName: "ReceivableTest", identificationNumber: "EGN0000000003", status: "ACTIVE" }`. Note the returned `customerId`.

**Test steps:**
1. Send `POST /customer-receivable` with payload: `{ customerId: <customerId from precondition step 1>, initialAmount: 250.00, currency: "BGN", description: "Manual test receivable" }`.
2. Assert HTTP response status is `201 Created`.
3. Assert response body contains `id` (non-null), `initialAmount: 250.00`, `currency: "BGN"`, and `status: "ACTIVE"`.
4. Send `GET /customer-receivable/{receivableId}` using the returned `id`.
5. Assert HTTP response status is `200 OK` and returned `initialAmount` equals `250.00`.

**Expected test case results:**
- Receivable is created successfully with `initialAmount = 250.00`.
- Response status is `201 Created`.
- Persisted receivable has correct amount, currency, and active status.

---

### TC-BE-4 (Negative): Manual receivable creation with zero amount rejected by @Positive

**Description:** Verify that attempting to create a receivable with initialAmount = 0 is rejected at the request validation level (@Positive).

**Preconditions:**
1. Create customer via `POST /customer` with parameters: `{ type: "PRIVATE", firstName: "ZeroRecv", lastName: "Test", identificationNumber: "EGN0000000004", status: "ACTIVE" }`. Note the returned `customerId`.

**Test steps:**
1. Send `POST /customer-receivable` with payload: `{ customerId: <customerId from precondition step 1>, initialAmount: 0, currency: "BGN", description: "Zero amount receivable" }`.
2. Assert HTTP response status is `400 Bad Request`.
3. Assert response body contains a validation error referencing `initialAmount` and the constraint `@Positive`.
4. Send `GET /customer-receivable?customerId=<customerId>` to list receivables for the customer.
5. Assert no receivable with `initialAmount = 0` exists for the customer.

**Expected test case results:**
- Request is rejected with `400 Bad Request`.
- Validation error message references `initialAmount` must be positive.
- No zero-amount receivable is persisted.

---

### TC-BE-5 (Positive): Liability from billing run invoice with non-zero total

**Description:** Verify that a billing run producing an invoice with a non-zero total correctly generates a liability with the invoice total as initialAmount.

**Preconditions:**
1. Create customer via `POST /customer` with parameters: `{ type: "PRIVATE", firstName: "BillingPos", lastName: "LiabTest", identificationNumber: "EGN0000000005", status: "ACTIVE" }`. Note the returned `customerId`.
2. Create POD via `POST /pod` with parameters: `{ type: "ELECTRICITY", identifier: "BG-POD-BILL-001", status: "ACTIVE", activationDate: "2024-01-01" }`. Note the returned `podId`.
3. Create product via `POST /product` with parameters: `{ name: "Standard Electricity", term: "INDEFINITE", dataDeliveryType: "BY_PROFILE", status: "ACTIVE" }`. Note the returned `productId`.
4. Create terms via `POST /terms` with parameters: `{ productId: <productId from step 3>, name: "Standard Terms", validFrom: "2024-01-01" }`.
5. Create price component via `POST /price-component` with parameters: `{ productId: <productId from step 3>, type: "ENERGY", rate: 0.15, currency: "BGN", unit: "kWh", validFrom: "2024-01-01" }`.
6. Create product contract via `POST /product-contract` with parameters: `{ customerId: <customerId from step 1>, podId: <podId from step 2>, productId: <productId from step 3>, status: "ACTIVE", entryIntoForceDate: "2025-01-01" }`. Note the returned `contractId`.
7. Create energy data via energy data endpoint with parameters: `{ contractId: <contractId from step 6>, podId: <podId from step 2>, consumptionKwh: 1000, periodFrom: "2025-01-01", periodTo: "2025-01-31" }`.
8. Create billing run via `POST /billing-run` with parameters: `{ type: "STANDARD", periodFrom: "2025-01-01", periodTo: "2025-01-31", contractId: <contractId from step 6> }`. Note the returned `billingRunId`.
9. Execute billing run via `POST /billing-run/{billingRunId}/execute`. Wait for status `COMPLETED`. Note the generated `invoiceId` and `invoiceTotal` (expected: 150.00 BGN = 1000 kWh × 0.15 BGN/kWh).

**Test steps:**
1. Send `POST /customer-liability/test/invoice/{invoiceId}` using the `invoiceId` from precondition step 9.
2. Assert HTTP response status is `200 OK` or `201 Created`.
3. Assert response body contains a liability with `initialAmount` equal to the invoice total (150.00 BGN).
4. Send `GET /customer-liability?customerId=<customerId>` and verify a liability exists with `initialAmount = 150.00`.

**Expected test case results:**
- Liability is generated from billing run invoice with `initialAmount = 150.00`.
- The liability is persisted and retrievable.

---

### TC-BE-6 (Negative): Billing run invoice with zero total does not generate zero-amount liability

**Description:** Verify that when a billing run produces an invoice with zero total (e.g., consumption is 0 kWh or price components net to zero), no zero-amount liability is created.

**Preconditions:**
1. Create customer via `POST /customer` with parameters: `{ type: "PRIVATE", firstName: "BillingNeg", lastName: "LiabTest", identificationNumber: "EGN0000000006", status: "ACTIVE" }`. Note the returned `customerId`.
2. Create POD via `POST /pod` with parameters: `{ type: "ELECTRICITY", identifier: "BG-POD-BILL-002", status: "ACTIVE", activationDate: "2024-01-01" }`. Note the returned `podId`.
3. Create product via `POST /product` with parameters: `{ name: "Zero Consumption Product", term: "INDEFINITE", dataDeliveryType: "BY_PROFILE", status: "ACTIVE" }`. Note the returned `productId`.
4. Create terms via `POST /terms` with parameters: `{ productId: <productId from step 3>, name: "Standard Terms", validFrom: "2024-01-01" }`.
5. Create price component via `POST /price-component` with parameters: `{ productId: <productId from step 3>, type: "ENERGY", rate: 0.15, currency: "BGN", unit: "kWh", validFrom: "2024-01-01" }`.
6. Create product contract via `POST /product-contract` with parameters: `{ customerId: <customerId from step 1>, podId: <podId from step 2>, productId: <productId from step 3>, status: "ACTIVE", entryIntoForceDate: "2025-01-01" }`. Note the returned `contractId`.
7. Create energy data via energy data endpoint with parameters: `{ contractId: <contractId from step 6>, podId: <podId from step 2>, consumptionKwh: 0, periodFrom: "2025-01-01", periodTo: "2025-01-31" }` (zero consumption).
8. Create billing run via `POST /billing-run` with parameters: `{ type: "STANDARD", periodFrom: "2025-01-01", periodTo: "2025-01-31", contractId: <contractId from step 6> }`. Note the returned `billingRunId`.
9. Execute billing run via `POST /billing-run/{billingRunId}/execute`. Wait for status `COMPLETED`. Invoice total should be 0.00 BGN.

**Test steps:**
1. Verify billing run completed. Check generated invoice total equals 0.00 via `GET /invoice?billingRunId=<billingRunId>`.
2. Send `GET /customer-liability?customerId=<customerId>` to list all liabilities for the customer.
3. Assert that no liability with `initialAmount = 0` exists.
4. Assert that the total number of liabilities for this customer from this billing run is 0.

**Expected test case results:**
- No zero-amount liability is generated from the billing run.
- BillingRunStartAccountingInvokeService skips liability creation when invoice total is 0.

---

### TC-BE-7 (Positive): Receivable from billing run invoice with non-zero total

**Description:** Verify that a billing run producing an invoice with a non-zero total correctly generates a receivable when the flow produces a receivable (e.g., credit-type invoice).

**Preconditions:**
1. Create customer via `POST /customer` with parameters: `{ type: "PRIVATE", firstName: "BillingPos", lastName: "RecvTest", identificationNumber: "EGN0000000007", status: "ACTIVE" }`. Note the returned `customerId`.
2. Create POD via `POST /pod` with parameters: `{ type: "ELECTRICITY", identifier: "BG-POD-BILL-003", status: "ACTIVE", activationDate: "2024-01-01" }`. Note the returned `podId`.
3. Create product via `POST /product` with parameters: `{ name: "Credit Product", term: "INDEFINITE", dataDeliveryType: "BY_PROFILE", status: "ACTIVE" }`. Note the returned `productId`.
4. Create terms via `POST /terms` with parameters: `{ productId: <productId from step 3>, name: "Standard Terms", validFrom: "2024-01-01" }`.
5. Create price component via `POST /price-component` with parameters: `{ productId: <productId from step 3>, type: "ENERGY", rate: -0.10, currency: "BGN", unit: "kWh", validFrom: "2024-01-01" }` (negative rate for credit scenario).
6. Create product contract via `POST /product-contract` with parameters: `{ customerId: <customerId from step 1>, podId: <podId from step 2>, productId: <productId from step 3>, status: "ACTIVE", entryIntoForceDate: "2025-01-01" }`. Note the returned `contractId`.
7. Create energy data via energy data endpoint with parameters: `{ contractId: <contractId from step 6>, podId: <podId from step 2>, consumptionKwh: 500, periodFrom: "2025-01-01", periodTo: "2025-01-31" }`.
8. Create billing run via `POST /billing-run` with parameters: `{ type: "STANDARD", periodFrom: "2025-01-01", periodTo: "2025-01-31", contractId: <contractId from step 6> }`. Note the returned `billingRunId`.
9. Execute billing run via `POST /billing-run/{billingRunId}/execute`. Wait for status `COMPLETED`. Invoice produces receivable with non-zero amount (50.00 BGN = 500 kWh × 0.10 BGN/kWh credit).

**Test steps:**
1. Send `GET /customer-receivable?customerId=<customerId>` to list receivables for the customer.
2. Assert that a receivable exists with `initialAmount = 50.00` and `currency = "BGN"`.
3. Assert HTTP response status is `200 OK`.
4. Assert the receivable is linked to the billing run invoice.

**Expected test case results:**
- Receivable is generated from billing run with `initialAmount = 50.00`.
- The receivable is persisted and retrievable.

---

### TC-BE-8 (Negative): Billing run invoice with zero total does not generate zero-amount receivable

**Description:** Verify that when a billing run produces a credit invoice with zero total, no zero-amount receivable is created.

**Preconditions:**
1. Create customer via `POST /customer` with parameters: `{ type: "PRIVATE", firstName: "BillingNeg", lastName: "RecvTest", identificationNumber: "EGN0000000008", status: "ACTIVE" }`. Note the returned `customerId`.
2. Create POD via `POST /pod` with parameters: `{ type: "ELECTRICITY", identifier: "BG-POD-BILL-004", status: "ACTIVE", activationDate: "2024-01-01" }`. Note the returned `podId`.
3. Create product via `POST /product` with parameters: `{ name: "Zero Credit Product", term: "INDEFINITE", dataDeliveryType: "BY_PROFILE", status: "ACTIVE" }`. Note the returned `productId`.
4. Create terms via `POST /terms` with parameters: `{ productId: <productId from step 3>, name: "Standard Terms", validFrom: "2024-01-01" }`.
5. Create price component via `POST /price-component` with parameters: `{ productId: <productId from step 3>, type: "ENERGY", rate: -0.10, currency: "BGN", unit: "kWh", validFrom: "2024-01-01" }`.
6. Create product contract via `POST /product-contract` with parameters: `{ customerId: <customerId from step 1>, podId: <podId from step 2>, productId: <productId from step 3>, status: "ACTIVE", entryIntoForceDate: "2025-01-01" }`. Note the returned `contractId`.
7. Create energy data via energy data endpoint with parameters: `{ contractId: <contractId from step 6>, podId: <podId from step 2>, consumptionKwh: 0, periodFrom: "2025-01-01", periodTo: "2025-01-31" }` (zero consumption → zero credit).
8. Create billing run via `POST /billing-run` with parameters: `{ type: "STANDARD", periodFrom: "2025-01-01", periodTo: "2025-01-31", contractId: <contractId from step 6> }`. Note the returned `billingRunId`.
9. Execute billing run via `POST /billing-run/{billingRunId}/execute`. Wait for status `COMPLETED`. Invoice total should be 0.00.

**Test steps:**
1. Verify billing run completed. Check generated invoice total equals 0.00 via `GET /invoice?billingRunId=<billingRunId>`.
2. Send `GET /customer-receivable?customerId=<customerId>` to list all receivables for the customer.
3. Assert that no receivable with `initialAmount = 0` exists.
4. Assert that the total number of receivables for this customer from this billing run is 0.

**Expected test case results:**
- No zero-amount receivable is generated from the billing run.
- BillingRunStartAccountingInvokeService skips receivable creation when invoice total is 0.

---

### TC-BE-9 (Positive): Liability from deposit with non-zero amount

**Description:** Verify that creating a deposit with a non-zero amount correctly generates a corresponding liability.

**Preconditions:**
1. Create customer via `POST /customer` with parameters: `{ type: "PRIVATE", firstName: "DepositPos", lastName: "LiabTest", identificationNumber: "EGN0000000009", status: "ACTIVE" }`. Note the returned `customerId`.
2. Create deposit via `POST /deposit` with parameters: `{ customerId: <customerId from step 1>, amount: 200.00, currency: "BGN", description: "Test deposit" }`. Note the returned `depositId`.

**Test steps:**
1. Send `POST /customer-liability/test/deposit/{depositId}` using the `depositId` from precondition step 2.
2. Assert HTTP response status is `200 OK` or `201 Created`.
3. Assert response body contains a liability with `initialAmount = 200.00` and `currency = "BGN"`.
4. Send `GET /customer-liability?customerId=<customerId>` and verify a liability exists with `initialAmount = 200.00`.

**Expected test case results:**
- Liability is generated from deposit with `initialAmount = 200.00`.
- The liability is persisted and linked to the deposit.

---

### TC-BE-10 (Negative): Deposit with zero amount does not generate zero-amount liability

**Description:** Verify that creating or processing a deposit with zero amount does not generate a zero-amount liability. DepositService should either reject the request or skip liability creation.

**Preconditions:**
1. Create customer via `POST /customer` with parameters: `{ type: "PRIVATE", firstName: "DepositNeg", lastName: "LiabTest", identificationNumber: "EGN0000000010", status: "ACTIVE" }`. Note the returned `customerId`.

**Test steps:**
1. Send `POST /deposit` with payload: `{ customerId: <customerId from precondition step 1>, amount: 0, currency: "BGN", description: "Zero deposit" }`.
2. Assert HTTP response status is `400 Bad Request` (validation rejects zero amount) OR if deposit creation is accepted, proceed to step 3.
3. If deposit was created with zero amount, send `GET /customer-liability?customerId=<customerId>`.
4. Assert that no liability with `initialAmount = 0` exists for the customer.

**Expected test case results:**
- Either the deposit request with zero amount is rejected at validation, or no zero-amount liability is generated.
- No `CustomerLiability` entity with `initialAmount = 0` is persisted.

---

### TC-BE-11 (Positive): Receivable from deposit with non-zero amount

**Description:** Verify that processing a deposit refund or deposit-related flow with a non-zero amount correctly generates a corresponding receivable.

**Preconditions:**
1. Create customer via `POST /customer` with parameters: `{ type: "PRIVATE", firstName: "DepositPos", lastName: "RecvTest", identificationNumber: "EGN0000000011", status: "ACTIVE" }`. Note the returned `customerId`.
2. Create deposit via `POST /deposit` with parameters: `{ customerId: <customerId from step 1>, amount: 300.00, currency: "BGN", description: "Test deposit for receivable" }`. Note the returned `depositId`.
3. Process deposit to generate a receivable (e.g. deposit refund flow or deposit release that creates a receivable with amount 300.00).

**Test steps:**
1. Send `GET /customer-receivable?customerId=<customerId>` to list receivables for the customer.
2. Assert that a receivable exists with `initialAmount = 300.00` and `currency = "BGN"`.
3. Assert the receivable is linked to the deposit from precondition step 2.

**Expected test case results:**
- Receivable is generated from deposit flow with `initialAmount = 300.00`.
- The receivable is persisted and retrievable.

---

### TC-BE-12 (Negative): Deposit with zero amount does not generate zero-amount receivable

**Description:** Verify that processing a deposit with zero amount does not generate a zero-amount receivable. DepositService should skip receivable creation when the calculated amount is zero.

**Preconditions:**
1. Create customer via `POST /customer` with parameters: `{ type: "PRIVATE", firstName: "DepositNeg", lastName: "RecvTest", identificationNumber: "EGN0000000012", status: "ACTIVE" }`. Note the returned `customerId`.

**Test steps:**
1. Send `POST /deposit` with payload: `{ customerId: <customerId from precondition step 1>, amount: 0, currency: "BGN", description: "Zero deposit for receivable" }`.
2. Assert HTTP response status is `400 Bad Request` OR if accepted, proceed to step 3.
3. Send `GET /customer-receivable?customerId=<customerId>`.
4. Assert that no receivable with `initialAmount = 0` exists for the customer.

**Expected test case results:**
- Either the deposit request with zero amount is rejected, or no zero-amount receivable is generated.
- No `CustomerReceivable` entity with `initialAmount = 0` is persisted.

---

### TC-BE-13 (Positive): Late payment fine liability with non-zero fine amount

**Description:** Verify that a late payment fine (LPF) for an overdue liability generates a LPF liability with a non-zero fine amount.

**Preconditions:**
1. Create customer via `POST /customer` with parameters: `{ type: "PRIVATE", firstName: "LPFPos", lastName: "LiabTest", identificationNumber: "EGN0000000013", status: "ACTIVE" }`. Note the returned `customerId`.
2. Create POD via `POST /pod` with parameters: `{ type: "ELECTRICITY", identifier: "BG-POD-LPF-001", status: "ACTIVE", activationDate: "2024-01-01" }`. Note the returned `podId`.
3. Create product via `POST /product` with parameters: `{ name: "LPF Test Product", term: "INDEFINITE", dataDeliveryType: "BY_PROFILE", status: "ACTIVE" }`. Note the returned `productId`.
4. Create terms via `POST /terms` with parameters: `{ productId: <productId from step 3>, name: "Standard Terms", validFrom: "2024-01-01" }`.
5. Create price component via `POST /price-component` with parameters: `{ productId: <productId from step 3>, type: "ENERGY", rate: 0.20, currency: "BGN", unit: "kWh", validFrom: "2024-01-01" }`.
6. Create product contract via `POST /product-contract` with parameters: `{ customerId: <customerId from step 1>, podId: <podId from step 2>, productId: <productId from step 3>, status: "ACTIVE", entryIntoForceDate: "2025-01-01" }`. Note the returned `contractId`.
7. Create energy data via energy data endpoint with parameters: `{ contractId: <contractId from step 6>, podId: <podId from step 2>, consumptionKwh: 500, periodFrom: "2025-01-01", periodTo: "2025-01-31" }`.
8. Create billing run via `POST /billing-run` with parameters: `{ type: "STANDARD", periodFrom: "2025-01-01", periodTo: "2025-01-31", contractId: <contractId from step 6> }`. Note the returned `billingRunId`.
9. Execute billing run via `POST /billing-run/{billingRunId}/execute`. Wait for completion. Invoice total = 100.00 BGN. Liability is created with `initialAmount = 100.00`.
10. Liability due date passes without payment — the liability enters overdue state.
11. LPF configuration is active with fine rate > 0 (e.g. 0.05% per day).
12. Trigger late payment fine calculation via `POST /customer-liability/test/latePaymentFine/{liabilityId}`. Note the returned LPF `liabilityId`.

**Test steps:**
1. Assert HTTP response status is `200 OK` or `201 Created` from precondition step 12.
2. Send `GET /customer-liability/{lpfLiabilityId}` using the LPF liability ID.
3. Assert the LPF liability has `initialAmount > 0` (calculated fine based on overdue days × rate × principal).
4. Assert the LPF liability is linked to the original overdue liability.

**Expected test case results:**
- LPF liability is generated with a non-zero `initialAmount`.
- The fine is correctly calculated based on overdue amount and configured rate.

---

### TC-BE-14 (Negative): Late payment fine with zero calculated fine does not generate zero-amount liability

**Description:** Verify that when the LPF calculation results in zero (e.g., fine rate is 0, or overdue amount rounds to zero fine), no zero-amount liability is created.

**Preconditions:**
1–9. Same as TC-BE-13 preconditions (billing chain producing a liability).
10. Liability due date passes without payment — the liability enters overdue state.
11. LPF configuration: fine rate is set such that the calculated fine rounds to 0 (e.g., very small principal with minimal overdue days, or fine rate = 0).
12. Trigger late payment fine calculation via `POST /customer-liability/test/latePaymentFine/{liabilityId}`.

**Test steps:**
1. Assert the response from LPF calculation indicates either no LPF was created or the request was handled without error.
2. Send `GET /customer-liability?customerId=<customerId>` to list all liabilities.
3. Assert that no LPF liability with `initialAmount = 0` exists.
4. Assert only the original billing liability exists (or any prior non-zero LPFs).

**Expected test case results:**
- No zero-amount LPF liability is generated.
- LatePaymentFineService skips creation when calculated fine amount is zero.

---

### TC-BE-15 (Positive): Rescheduling produces installment liabilities with non-zero amounts

**Description:** Verify that creating a rescheduling plan for an existing liability generates installment liabilities each with a non-zero initialAmount.

**Preconditions:**
1. Create customer via `POST /customer` with parameters: `{ type: "PRIVATE", firstName: "ReschedPos", lastName: "Test", identificationNumber: "EGN0000000015", status: "ACTIVE" }`. Note the returned `customerId`.
2. Create POD via `POST /pod` with parameters: `{ type: "ELECTRICITY", identifier: "BG-POD-RESCH-001", status: "ACTIVE", activationDate: "2024-01-01" }`. Note the returned `podId`.
3. Create product via `POST /product` with parameters: `{ name: "Rescheduling Test Product", term: "INDEFINITE", dataDeliveryType: "BY_PROFILE", status: "ACTIVE" }`. Note the returned `productId`.
4. Create terms via `POST /terms` with parameters: `{ productId: <productId from step 3>, name: "Standard Terms", validFrom: "2024-01-01" }`.
5. Create price component via `POST /price-component` with parameters: `{ productId: <productId from step 3>, type: "ENERGY", rate: 0.20, currency: "BGN", unit: "kWh", validFrom: "2024-01-01" }`.
6. Create product contract via `POST /product-contract` with parameters: `{ customerId: <customerId from step 1>, podId: <podId from step 2>, productId: <productId from step 3>, status: "ACTIVE", entryIntoForceDate: "2025-01-01" }`. Note the returned `contractId`.
7. Create energy data via energy data endpoint with parameters: `{ contractId: <contractId from step 6>, podId: <podId from step 2>, consumptionKwh: 3000, periodFrom: "2025-01-01", periodTo: "2025-01-31" }`.
8. Create billing run via `POST /billing-run` with parameters: `{ type: "STANDARD", periodFrom: "2025-01-01", periodTo: "2025-01-31", contractId: <contractId from step 6> }`. Note the returned `billingRunId`.
9. Execute billing run via `POST /billing-run/{billingRunId}/execute`. Wait for completion. Invoice total = 600.00 BGN. Liability created with `initialAmount = 600.00`.
10. Note the `liabilityId` of the billing liability.
11. Create rescheduling plan via `POST /rescheduling` with parameters: `{ liabilityId: <liabilityId from step 10>, installmentCount: 3, installmentAmount: 200.00 }`. Note the returned `reschedulingId`.

**Test steps:**
1. Assert HTTP response status is `200 OK` or `201 Created` from precondition step 11.
2. Send `GET /customer-liability?customerId=<customerId>` to list all liabilities.
3. Assert that exactly 3 installment liabilities exist, each with `initialAmount = 200.00`.
4. Assert no installment liability has `initialAmount = 0`.
5. Assert the original liability is marked as rescheduled.

**Expected test case results:**
- Three installment liabilities are created, each with `initialAmount = 200.00`.
- No zero-amount installment liability is generated.

---

### TC-BE-16 (Negative): Rescheduling producing zero-amount installment does not generate zero-amount liability

**Description:** Verify that if a rescheduling plan would produce an installment with zero amount (e.g., rounding edge case or misconfigured installment count), no zero-amount liability is created.

**Preconditions:**
1–9. Same as TC-BE-15 preconditions (billing chain producing a 600.00 liability).
10. Note the `liabilityId` of the billing liability.
11. Attempt rescheduling via `POST /rescheduling` with parameters that would produce a zero-amount installment (e.g., `{ liabilityId: <liabilityId from step 10>, installmentCount: 601 }` — rounding 600/601 could produce zero for some installments).

**Test steps:**
1. If the request is accepted, send `GET /customer-liability?customerId=<customerId>` to list all liabilities.
2. Assert that no liability with `initialAmount = 0` exists.
3. If the request is rejected with a validation error, assert HTTP response status is `400 Bad Request`.

**Expected test case results:**
- No zero-amount installment liability is generated.
- ReschedulingService either rejects the configuration or skips zero-amount installments.

---

### TC-BE-17 (Positive): Liability from payment with non-zero amount

**Description:** Verify that processing a payment with a non-zero amount that generates a related liability (e.g., payment processing fee or unallocated payment liability) creates the liability with the correct non-zero amount.

**Preconditions:**
1. Create customer via `POST /customer` with parameters: `{ type: "PRIVATE", firstName: "PaymentPos", lastName: "LiabTest", identificationNumber: "EGN0000000017", status: "ACTIVE" }`. Note the returned `customerId`.
2. Create POD via `POST /pod` with parameters: `{ type: "ELECTRICITY", identifier: "BG-POD-PAY-001", status: "ACTIVE", activationDate: "2024-01-01" }`. Note the returned `podId`.
3. Create product via `POST /product` with parameters: `{ name: "Payment Test Product", term: "INDEFINITE", dataDeliveryType: "BY_PROFILE", status: "ACTIVE" }`. Note the returned `productId`.
4. Create terms via `POST /terms` with parameters: `{ productId: <productId from step 3>, name: "Standard Terms", validFrom: "2024-01-01" }`.
5. Create price component via `POST /price-component` with parameters: `{ productId: <productId from step 3>, type: "ENERGY", rate: 0.10, currency: "BGN", unit: "kWh", validFrom: "2024-01-01" }`.
6. Create product contract via `POST /product-contract` with parameters: `{ customerId: <customerId from step 1>, podId: <podId from step 2>, productId: <productId from step 3>, status: "ACTIVE", entryIntoForceDate: "2025-01-01" }`. Note the returned `contractId`.
7. Create energy data via energy data endpoint with parameters: `{ contractId: <contractId from step 6>, podId: <podId from step 2>, consumptionKwh: 1000, periodFrom: "2025-01-01", periodTo: "2025-01-31" }`.
8. Create billing run via `POST /billing-run` with parameters: `{ type: "STANDARD", periodFrom: "2025-01-01", periodTo: "2025-01-31", contractId: <contractId from step 6> }`. Note the returned `billingRunId`.
9. Execute billing run via `POST /billing-run/{billingRunId}/execute`. Wait for completion. Invoice total = 100.00 BGN. Liability created with `initialAmount = 100.00`. Note the `liabilityId`.
10. Create payment via `POST /payment` with parameters: `{ customerId: <customerId from step 1>, amount: 100.00, currency: "BGN" }`. Note the returned `paymentId`.

**Test steps:**
1. Assert HTTP response status is `200 OK` or `201 Created` from precondition step 10.
2. Send `GET /customer-liability?customerId=<customerId>` to list all liabilities.
3. Verify that any new liabilities generated by the payment flow have `initialAmount > 0`.
4. Assert no liability with `initialAmount = 0` exists for the customer (except the original billing liability which is being offset).

**Expected test case results:**
- Any liability generated by the payment processing flow has a non-zero `initialAmount`.
- PaymentService does not create zero-amount liabilities.

---

### TC-BE-18 (Negative): Payment with zero amount does not generate zero-amount liability

**Description:** Verify that attempting to create a payment with zero amount is rejected or does not produce any zero-amount liability.

**Preconditions:**
1. Create customer via `POST /customer` with parameters: `{ type: "PRIVATE", firstName: "PaymentNeg", lastName: "LiabTest", identificationNumber: "EGN0000000018", status: "ACTIVE" }`. Note the returned `customerId`.
2–9. Billing chain steps (same as TC-BE-17 preconditions steps 2–9). Liability created with `initialAmount = 100.00`.

**Test steps:**
1. Send `POST /payment` with payload: `{ customerId: <customerId>, amount: 0, currency: "BGN" }`.
2. Assert HTTP response status is `400 Bad Request` (validation rejects zero amount).
3. If request was accepted, send `GET /customer-liability?customerId=<customerId>`.
4. Assert no liability with `initialAmount = 0` exists for the customer.

**Expected test case results:**
- Payment with zero amount is rejected at validation.
- No zero-amount liability is generated.

---

### TC-BE-19 (Positive): Receivable from overpayment with non-zero amount

**Description:** Verify that when a customer overpays (payment exceeds total liabilities), a receivable is generated for the overpayment difference with a non-zero initialAmount.

**Preconditions:**
1. Create customer via `POST /customer` with parameters: `{ type: "PRIVATE", firstName: "OverpayPos", lastName: "RecvTest", identificationNumber: "EGN0000000019", status: "ACTIVE" }`. Note the returned `customerId`.
2–9. Billing chain steps (same as TC-BE-5 preconditions). Invoice total = 100.00 BGN. Liability created with `initialAmount = 100.00`. Note the `liabilityId`.
10. Create payment via `POST /payment` with parameters: `{ customerId: <customerId from step 1>, amount: 150.00, currency: "BGN" }` (overpayment of 50.00 beyond the 100.00 liability). Note the returned `paymentId`.

**Test steps:**
1. Assert HTTP response status is `200 OK` or `201 Created` from precondition step 10.
2. Send `GET /customer-receivable?customerId=<customerId>` to list receivables.
3. Assert a receivable exists with `initialAmount = 50.00` (overpayment difference).
4. Assert the receivable is linked to the payment.

**Expected test case results:**
- Receivable is generated for the overpayment with `initialAmount = 50.00`.
- The receivable is persisted and correctly calculated.

---

### TC-BE-20 (Negative): Payment exactly matching liability does not generate zero-amount receivable

**Description:** Verify that when a payment exactly matches the outstanding liability amount, no zero-amount receivable is generated for the zero difference.

**Preconditions:**
1. Create customer via `POST /customer` with parameters: `{ type: "PRIVATE", firstName: "ExactPay", lastName: "RecvTest", identificationNumber: "EGN0000000020", status: "ACTIVE" }`. Note the returned `customerId`.
2–9. Billing chain steps (same as TC-BE-5 preconditions). Invoice total = 100.00 BGN. Liability created with `initialAmount = 100.00`. Note the `liabilityId`.
10. Create payment via `POST /payment` with parameters: `{ customerId: <customerId from step 1>, amount: 100.00, currency: "BGN" }` (exact match). Note the returned `paymentId`.

**Test steps:**
1. Assert HTTP response status is `200 OK` or `201 Created` from precondition step 10.
2. Send `GET /customer-receivable?customerId=<customerId>` to list receivables.
3. Assert no receivable with `initialAmount = 0` exists for the customer.
4. Assert the total number of receivables generated by this payment is 0 (exact match produces no overpayment).

**Expected test case results:**
- No receivable is generated when payment exactly matches the liability.
- PaymentService correctly identifies zero difference and skips receivable creation.

---

### TC-BE-21 (Positive): Liability from action with non-zero amount

**Description:** Verify that processing an action (deprecated endpoint) with a non-zero charge amount generates a liability with the correct initialAmount.

**Preconditions:**
1. Create customer via `POST /customer` with parameters: `{ type: "PRIVATE", firstName: "ActionPos", lastName: "LiabTest", identificationNumber: "EGN0000000021", status: "ACTIVE" }`. Note the returned `customerId`.
2. Create action via `POST /action` with parameters: `{ customerId: <customerId from step 1>, chargeAmount: 75.00, currency: "BGN", type: "SERVICE_CHARGE" }`. Note the returned `actionId`.

**Test steps:**
1. Send `POST /customer-liability/test/action/{actionId}` using the `actionId` from precondition step 2.
2. Assert HTTP response status is `200 OK` or `201 Created`.
3. Assert response body contains a liability with `initialAmount = 75.00`.
4. Send `GET /customer-liability?customerId=<customerId>` and verify the liability exists.

**Expected test case results:**
- Liability is generated from the action with `initialAmount = 75.00`.
- The liability is persisted and linked to the action.

---

### TC-BE-22 (Negative): Action with zero amount does not generate zero-amount liability

**Description:** Verify that processing an action with zero charge amount does not generate a zero-amount liability.

**Preconditions:**
1. Create customer via `POST /customer` with parameters: `{ type: "PRIVATE", firstName: "ActionNeg", lastName: "LiabTest", identificationNumber: "EGN0000000022", status: "ACTIVE" }`. Note the returned `customerId`.
2. Create action via `POST /action` with parameters: `{ customerId: <customerId from step 1>, chargeAmount: 0, currency: "BGN", type: "SERVICE_CHARGE" }`. Note the returned `actionId` if created.

**Test steps:**
1. If action was created with zero amount, send `POST /customer-liability/test/action/{actionId}`.
2. Assert either the request is rejected or no liability is returned.
3. Send `GET /customer-liability?customerId=<customerId>`.
4. Assert no liability with `initialAmount = 0` exists for the customer.

**Expected test case results:**
- No zero-amount liability is generated from the action.
- The system either rejects the zero-amount action or skips liability creation.

---

### TC-BE-23 (Positive): Liability from goods order invoice with non-zero total

**Description:** Verify that processing a goods order with a non-zero total generates a liability with the correct initialAmount.

**Preconditions:**
1. Create customer via `POST /customer` with parameters: `{ type: "PRIVATE", firstName: "GoodsPos", lastName: "LiabTest", identificationNumber: "EGN0000000023", status: "ACTIVE" }`. Note the returned `customerId`.
2. Create goods order via `POST /goods-order` with parameters: `{ customerId: <customerId from step 1>, items: [{ name: "Electric Meter", quantity: 1, unitPrice: 120.00, currency: "BGN" }] }`. Note the returned `goodsOrderId`.
3. Process goods order to generate invoice via `POST /goods-order/{goodsOrderId}/process`. Wait for completion. Note the `invoiceId` and `invoiceTotal` (120.00 BGN).

**Test steps:**
1. Send `GET /customer-liability?customerId=<customerId>` to list liabilities.
2. Assert a liability exists with `initialAmount = 120.00` linked to the goods order invoice.
3. Assert the liability `currency` is `"BGN"`.

**Expected test case results:**
- Liability is generated from goods order invoice with `initialAmount = 120.00`.
- GoodsOrderProcessService correctly creates the liability.

---

### TC-BE-24 (Negative): Goods order with zero total does not generate zero-amount liability

**Description:** Verify that a goods order with a zero total (e.g., free goods or promotional items) does not generate a zero-amount liability.

**Preconditions:**
1. Create customer via `POST /customer` with parameters: `{ type: "PRIVATE", firstName: "GoodsNeg", lastName: "LiabTest", identificationNumber: "EGN0000000024", status: "ACTIVE" }`. Note the returned `customerId`.
2. Create goods order via `POST /goods-order` with parameters: `{ customerId: <customerId from step 1>, items: [{ name: "Free Promo Item", quantity: 1, unitPrice: 0, currency: "BGN" }] }`. Note the returned `goodsOrderId`.
3. Process goods order via `POST /goods-order/{goodsOrderId}/process`. Wait for completion.

**Test steps:**
1. Send `GET /customer-liability?customerId=<customerId>` to list liabilities.
2. Assert no liability with `initialAmount = 0` exists for the customer.
3. Assert no liability was created for this goods order.

**Expected test case results:**
- No zero-amount liability is generated from the zero-total goods order.
- GoodsOrderProcessService skips liability creation when invoice total is 0.

---

### TC-BE-25 (Positive): Liability from service order invoice with non-zero total

**Description:** Verify that processing a service order with a non-zero total generates a liability with the correct initialAmount.

**Preconditions:**
1. Create customer via `POST /customer` with parameters: `{ type: "PRIVATE", firstName: "ServicePos", lastName: "LiabTest", identificationNumber: "EGN0000000025", status: "ACTIVE" }`. Note the returned `customerId`.
2. Create service order via `POST /service-order` with parameters: `{ customerId: <customerId from step 1>, services: [{ name: "Installation Service", quantity: 1, unitPrice: 85.00, currency: "BGN" }] }`. Note the returned `serviceOrderId`.
3. Process service order to generate invoice via `POST /service-order/{serviceOrderId}/process`. Wait for completion. Note the `invoiceId` and `invoiceTotal` (85.00 BGN).

**Test steps:**
1. Send `GET /customer-liability?customerId=<customerId>` to list liabilities.
2. Assert a liability exists with `initialAmount = 85.00` linked to the service order invoice.
3. Assert the liability `currency` is `"BGN"`.

**Expected test case results:**
- Liability is generated from service order invoice with `initialAmount = 85.00`.
- ServiceOrderProcessService correctly creates the liability.

---

### TC-BE-26 (Negative): Service order with zero total does not generate zero-amount liability

**Description:** Verify that a service order with a zero total does not generate a zero-amount liability.

**Preconditions:**
1. Create customer via `POST /customer` with parameters: `{ type: "PRIVATE", firstName: "ServiceNeg", lastName: "LiabTest", identificationNumber: "EGN0000000026", status: "ACTIVE" }`. Note the returned `customerId`.
2. Create service order via `POST /service-order` with parameters: `{ customerId: <customerId from step 1>, services: [{ name: "Free Consultation", quantity: 1, unitPrice: 0, currency: "BGN" }] }`. Note the returned `serviceOrderId`.
3. Process service order via `POST /service-order/{serviceOrderId}/process`. Wait for completion.

**Test steps:**
1. Send `GET /customer-liability?customerId=<customerId>` to list liabilities.
2. Assert no liability with `initialAmount = 0` exists for the customer.
3. Assert no liability was created for this service order.

**Expected test case results:**
- No zero-amount liability is generated from the zero-total service order.
- ServiceOrderProcessService skips liability creation when invoice total is 0.

---

### TC-BE-27 (Positive): Liability from invoice cancellation with non-zero amount

**Description:** Verify that cancelling an invoice with a non-zero total generates a reversal liability with a non-zero initialAmount.

**Preconditions:**
1. Create customer via `POST /customer` with parameters: `{ type: "PRIVATE", firstName: "InvCancelPos", lastName: "LiabTest", identificationNumber: "EGN0000000027", status: "ACTIVE" }`. Note the returned `customerId`.
2. Create POD via `POST /pod` with parameters: `{ type: "ELECTRICITY", identifier: "BG-POD-INVCAN-001", status: "ACTIVE", activationDate: "2024-01-01" }`. Note the returned `podId`.
3. Create product via `POST /product` with parameters: `{ name: "Invoice Cancel Product", term: "INDEFINITE", dataDeliveryType: "BY_PROFILE", status: "ACTIVE" }`. Note the returned `productId`.
4. Create terms via `POST /terms` with parameters: `{ productId: <productId from step 3>, name: "Standard Terms", validFrom: "2024-01-01" }`.
5. Create price component via `POST /price-component` with parameters: `{ productId: <productId from step 3>, type: "ENERGY", rate: 0.15, currency: "BGN", unit: "kWh", validFrom: "2024-01-01" }`.
6. Create product contract via `POST /product-contract` with parameters: `{ customerId: <customerId from step 1>, podId: <podId from step 2>, productId: <productId from step 3>, status: "ACTIVE", entryIntoForceDate: "2025-01-01" }`. Note the returned `contractId`.
7. Create energy data via energy data endpoint with parameters: `{ contractId: <contractId from step 6>, podId: <podId from step 2>, consumptionKwh: 1000, periodFrom: "2025-01-01", periodTo: "2025-01-31" }`.
8. Create billing run via `POST /billing-run` with parameters: `{ type: "STANDARD", periodFrom: "2025-01-01", periodTo: "2025-01-31", contractId: <contractId from step 6> }`. Note the returned `billingRunId`.
9. Execute billing run via `POST /billing-run/{billingRunId}/execute`. Wait for completion. Invoice total = 150.00 BGN. Note the `invoiceId`. Liability created with `initialAmount = 150.00`.
10. Note the `liabilityId` of the original liability.
11. Cancel invoice via `POST /invoice-cancellation` with parameters: `{ invoiceId: <invoiceId from step 9> }`. Note the returned `cancellationId`.

**Test steps:**
1. Assert HTTP response status is `200 OK` or `201 Created` from precondition step 11.
2. Send `GET /customer-liability?customerId=<customerId>` to list all liabilities.
3. Assert a reversal/cancellation liability exists with `initialAmount` > 0 (matching the cancelled invoice amount of 150.00).
4. Assert no liability with `initialAmount = 0` exists.

**Expected test case results:**
- Cancellation liability is generated with non-zero `initialAmount`.
- InvoiceCancellationService creates the reversal liability correctly.

---

### TC-BE-28 (Negative): Invoice cancellation with zero net does not generate zero-amount liability

**Description:** Verify that if an invoice cancellation results in a zero net liability amount (e.g., invoice was already fully offset), no zero-amount liability is created.

**Preconditions:**
1–9. Same as TC-BE-27 preconditions (billing chain with invoice total = 150.00).
10. Fully offset the original liability (e.g., via payment of 150.00 or other offsetting mechanism) so that the cancellation produces a zero net effect on liabilities.
11. Cancel invoice via `POST /invoice-cancellation` with parameters: `{ invoiceId: <invoiceId from step 9> }`.

**Test steps:**
1. Send `GET /customer-liability?customerId=<customerId>` to list all liabilities.
2. Assert no liability with `initialAmount = 0` exists for the customer.
3. Verify that the cancellation flow did not create a zero-amount liability.

**Expected test case results:**
- No zero-amount liability is generated from the invoice cancellation.
- InvoiceCancellationService skips zero-amount liability creation.

---

### TC-BE-29 (Positive): Receivable from invoice cancellation with non-zero amount

**Description:** Verify that cancelling an invoice that was already paid generates a receivable (refund) with a non-zero initialAmount.

**Preconditions:**
1–9. Same as TC-BE-27 preconditions (billing chain with invoice total = 150.00).
10. Liability created with `initialAmount = 150.00`. Note the `liabilityId`.
11. Create payment via `POST /payment` with parameters: `{ customerId: <customerId from step 1>, amount: 150.00, currency: "BGN" }` to fully pay the liability.
12. Cancel invoice via `POST /invoice-cancellation` with parameters: `{ invoiceId: <invoiceId from step 9> }`.

**Test steps:**
1. Assert HTTP response status is `200 OK` or `201 Created` from precondition step 12.
2. Send `GET /customer-receivable?customerId=<customerId>` to list receivables.
3. Assert a receivable exists with `initialAmount = 150.00` (refund for the cancelled paid invoice).
4. Assert no receivable with `initialAmount = 0` exists.

**Expected test case results:**
- Receivable is generated for the cancellation refund with `initialAmount = 150.00`.
- InvoiceCancellationService creates the refund receivable correctly.

---

### TC-BE-30 (Negative): Invoice cancellation with zero receivable does not generate zero-amount receivable

**Description:** Verify that when an invoice cancellation would produce a zero-amount receivable (e.g., no payment was made on the invoice), no zero-amount receivable is created.

**Preconditions:**
1–9. Same as TC-BE-27 preconditions (billing chain with invoice total = 150.00).
10. Liability created but NO payment made (unpaid invoice).
11. Cancel invoice via `POST /invoice-cancellation` with parameters: `{ invoiceId: <invoiceId from step 9> }`.

**Test steps:**
1. Send `GET /customer-receivable?customerId=<customerId>` to list receivables.
2. Assert no receivable with `initialAmount = 0` exists for the customer.
3. Assert the cancellation did not create any zero-amount receivable.

**Expected test case results:**
- No zero-amount receivable is generated.
- InvoiceCancellationService skips receivable creation when the refund amount would be zero.

---

### TC-BE-31 (Positive): Liability from invoice reversal with non-zero amount

**Description:** Verify that reversing an invoice with a non-zero total generates a reversal liability with a non-zero initialAmount.

**Preconditions:**
1. Create customer via `POST /customer` with parameters: `{ type: "PRIVATE", firstName: "InvRevPos", lastName: "LiabTest", identificationNumber: "EGN0000000031", status: "ACTIVE" }`. Note the returned `customerId`.
2. Create POD via `POST /pod` with parameters: `{ type: "ELECTRICITY", identifier: "BG-POD-INVREV-001", status: "ACTIVE", activationDate: "2024-01-01" }`. Note the returned `podId`.
3. Create product via `POST /product` with parameters: `{ name: "Invoice Reversal Product", term: "INDEFINITE", dataDeliveryType: "BY_PROFILE", status: "ACTIVE" }`. Note the returned `productId`.
4. Create terms via `POST /terms` with parameters: `{ productId: <productId from step 3>, name: "Standard Terms", validFrom: "2024-01-01" }`.
5. Create price component via `POST /price-component` with parameters: `{ productId: <productId from step 3>, type: "ENERGY", rate: 0.20, currency: "BGN", unit: "kWh", validFrom: "2024-01-01" }`.
6. Create product contract via `POST /product-contract` with parameters: `{ customerId: <customerId from step 1>, podId: <podId from step 2>, productId: <productId from step 3>, status: "ACTIVE", entryIntoForceDate: "2025-01-01" }`. Note the returned `contractId`.
7. Create energy data via energy data endpoint with parameters: `{ contractId: <contractId from step 6>, podId: <podId from step 2>, consumptionKwh: 500, periodFrom: "2025-01-01", periodTo: "2025-01-31" }`.
8. Create billing run via `POST /billing-run` with parameters: `{ type: "STANDARD", periodFrom: "2025-01-01", periodTo: "2025-01-31", contractId: <contractId from step 6> }`. Note the returned `billingRunId`.
9. Execute billing run via `POST /billing-run/{billingRunId}/execute`. Wait for completion. Invoice total = 100.00 BGN. Note the `invoiceId`. Liability created with `initialAmount = 100.00`.
10. Note the `liabilityId` of the original liability.
11. Reverse invoice via `POST /invoice-reversal` with parameters: `{ invoiceId: <invoiceId from step 9> }`. Note the returned `reversalId`.

**Test steps:**
1. Assert HTTP response status is `200 OK` or `201 Created` from precondition step 11.
2. Send `GET /customer-liability?customerId=<customerId>` to list all liabilities.
3. Assert a reversal liability exists with `initialAmount` > 0 (matching the reversed invoice amount of 100.00).
4. Assert the original liability status is updated (e.g., `REVERSED`).
5. Assert no liability with `initialAmount = 0` exists.

**Expected test case results:**
- Reversal liability is generated with non-zero `initialAmount`.
- InvoiceReversalProcessService creates the reversal liability correctly.

---

### TC-BE-32 (Negative): Invoice reversal with zero calculated amount does not generate zero-amount liability (signum branching)

**Description:** Verify that when invoice reversal calculation produces zero (signum == 0 branch), no zero-amount liability is created. The signum branching in InvoiceReversalProcessService must skip creation when the result is zero.

**Preconditions:**
1–9. Same as TC-BE-31 preconditions (billing chain with original invoice).
10. Configure a scenario where the reversal produces offsetting amounts that net to zero (e.g., original invoice was already corrected to produce a zero difference).
11. Reverse invoice via `POST /invoice-reversal` with parameters: `{ invoiceId: <invoiceId from step 9> }`.

**Test steps:**
1. Send `GET /customer-liability?customerId=<customerId>` to list all liabilities.
2. Assert no liability with `initialAmount = 0` exists for the customer.
3. Verify that the signum == 0 branch in InvoiceReversalProcessService correctly skips liability creation.

**Expected test case results:**
- No zero-amount liability is generated from the invoice reversal.
- InvoiceReversalProcessService handles the signum == 0 case by skipping creation.

---

### TC-BE-33 (Positive): Receivable from invoice reversal with non-zero amount

**Description:** Verify that reversing an invoice that was paid generates a receivable with a non-zero initialAmount.

**Preconditions:**
1–9. Same as TC-BE-31 preconditions (billing chain producing invoice total = 100.00).
10. Liability created with `initialAmount = 100.00`.
11. Create payment via `POST /payment` with parameters: `{ customerId: <customerId from step 1>, amount: 100.00, currency: "BGN" }` to fully pay the liability.
12. Reverse invoice via `POST /invoice-reversal` with parameters: `{ invoiceId: <invoiceId from step 9> }`.

**Test steps:**
1. Assert HTTP response status is `200 OK` or `201 Created` from precondition step 12.
2. Send `GET /customer-receivable?customerId=<customerId>` to list receivables.
3. Assert a receivable exists with `initialAmount` > 0 linked to the invoice reversal.
4. Assert no receivable with `initialAmount = 0` exists.

**Expected test case results:**
- Receivable is generated from the invoice reversal with a non-zero `initialAmount`.
- InvoiceReversalProcessService creates the refund receivable correctly.

---

### TC-BE-34 (Negative): Invoice reversal with zero receivable amount does not generate zero-amount receivable (signum == 0 branch)

**Description:** Verify that when invoice reversal receivable calculation produces zero (signum == 0), no zero-amount receivable is created.

**Preconditions:**
1–9. Same as TC-BE-31 preconditions (billing chain with original invoice).
10. Liability created but NO payment made (unpaid invoice).
11. Reverse invoice via `POST /invoice-reversal` with parameters: `{ invoiceId: <invoiceId from step 9> }`.

**Test steps:**
1. Send `GET /customer-receivable?customerId=<customerId>` to list receivables.
2. Assert no receivable with `initialAmount = 0` exists for the customer.
3. Verify the signum == 0 branch in InvoiceReversalProcessService correctly skips receivable creation.

**Expected test case results:**
- No zero-amount receivable is generated from the invoice reversal.
- InvoiceReversalProcessService handles the signum == 0 case by skipping receivable creation.

---

### TC-BE-35 (Positive): Liability from payment reversal with non-zero amount

**Description:** Verify that reversing a payment generates a liability (to restore the original debt) with a non-zero initialAmount.

**Preconditions:**
1. Create customer via `POST /customer` with parameters: `{ type: "PRIVATE", firstName: "PayRevPos", lastName: "LiabTest", identificationNumber: "EGN0000000035", status: "ACTIVE" }`. Note the returned `customerId`.
2. Create POD via `POST /pod` with parameters: `{ type: "ELECTRICITY", identifier: "BG-POD-PAYREV-001", status: "ACTIVE", activationDate: "2024-01-01" }`. Note the returned `podId`.
3. Create product via `POST /product` with parameters: `{ name: "Payment Reversal Product", term: "INDEFINITE", dataDeliveryType: "BY_PROFILE", status: "ACTIVE" }`. Note the returned `productId`.
4. Create terms via `POST /terms` with parameters: `{ productId: <productId from step 3>, name: "Standard Terms", validFrom: "2024-01-01" }`.
5. Create price component via `POST /price-component` with parameters: `{ productId: <productId from step 3>, type: "ENERGY", rate: 0.10, currency: "BGN", unit: "kWh", validFrom: "2024-01-01" }`.
6. Create product contract via `POST /product-contract` with parameters: `{ customerId: <customerId from step 1>, podId: <podId from step 2>, productId: <productId from step 3>, status: "ACTIVE", entryIntoForceDate: "2025-01-01" }`. Note the returned `contractId`.
7. Create energy data via energy data endpoint with parameters: `{ contractId: <contractId from step 6>, podId: <podId from step 2>, consumptionKwh: 1000, periodFrom: "2025-01-01", periodTo: "2025-01-31" }`.
8. Create billing run via `POST /billing-run` with parameters: `{ type: "STANDARD", periodFrom: "2025-01-01", periodTo: "2025-01-31", contractId: <contractId from step 6> }`. Note the returned `billingRunId`.
9. Execute billing run via `POST /billing-run/{billingRunId}/execute`. Wait for completion. Invoice total = 100.00 BGN. Liability created with `initialAmount = 100.00`. Note the `liabilityId`.
10. Create payment via `POST /payment` with parameters: `{ customerId: <customerId from step 1>, amount: 100.00, currency: "BGN" }`. Note the returned `paymentId`. Payment offsets the liability.
11. Reverse payment via `POST /payment-reversal` with parameters: `{ paymentId: <paymentId from step 10> }`. Note the returned `reversalId`.

**Test steps:**
1. Assert HTTP response status is `200 OK` or `201 Created` from precondition step 11.
2. Send `GET /customer-liability?customerId=<customerId>` to list all liabilities.
3. Assert a new liability exists (from payment reversal) with `initialAmount = 100.00` (restoring the debt).
4. Assert no liability with `initialAmount = 0` exists.

**Expected test case results:**
- Liability is generated from payment reversal with `initialAmount = 100.00`.
- The reversed payment's offset is undone, restoring the customer's debt.

---

### TC-BE-36 (Negative): Payment reversal with zero amount does not generate zero-amount liability

**Description:** Verify that reversing a payment that would result in a zero-amount liability does not create one.

**Preconditions:**
1–9. Same as TC-BE-35 preconditions (billing chain with liability).
10. Create payment via `POST /payment` with parameters configured such that reversal would produce a zero-net liability (e.g., the payment was already reversed or fully compensated).
11. Attempt payment reversal via `POST /payment-reversal`.

**Test steps:**
1. Send `GET /customer-liability?customerId=<customerId>` to list all liabilities.
2. Assert no liability with `initialAmount = 0` exists for the customer.

**Expected test case results:**
- No zero-amount liability is generated from the payment reversal.
- PaymentService handles the zero-amount case by skipping liability creation.

---

### TC-BE-37 (Positive): Receivable from payment reversal with non-zero amount

**Description:** Verify that reversing a payment that included an overpayment generates a receivable reversal with a non-zero initialAmount.

**Preconditions:**
1–9. Same as TC-BE-35 preconditions (billing chain with liability = 100.00).
10. Create payment via `POST /payment` with parameters: `{ customerId: <customerId from step 1>, amount: 150.00, currency: "BGN" }` (overpayment: 50.00 receivable was created). Note the returned `paymentId`.
11. Reverse payment via `POST /payment-reversal` with parameters: `{ paymentId: <paymentId from step 10> }`.

**Test steps:**
1. Assert HTTP response status is `200 OK` or `201 Created` from precondition step 11.
2. Send `GET /customer-receivable?customerId=<customerId>` to list receivables.
3. Assert a reversal receivable exists with `initialAmount > 0` (reversing the overpayment receivable).
4. Assert no receivable with `initialAmount = 0` exists.

**Expected test case results:**
- Receivable is generated from payment reversal with a non-zero `initialAmount`.
- PaymentService correctly handles the receivable reversal.

---

### TC-BE-38 (Negative): Payment reversal with zero receivable does not generate zero-amount receivable

**Description:** Verify that reversing a payment where no overpayment existed does not generate a zero-amount receivable.

**Preconditions:**
1–9. Same as TC-BE-35 preconditions (billing chain with liability = 100.00).
10. Create payment via `POST /payment` with parameters: `{ customerId: <customerId from step 1>, amount: 100.00, currency: "BGN" }` (exact match, no overpayment receivable). Note the returned `paymentId`.
11. Reverse payment via `POST /payment-reversal` with parameters: `{ paymentId: <paymentId from step 10> }`.

**Test steps:**
1. Send `GET /customer-receivable?customerId=<customerId>` to list receivables.
2. Assert no receivable with `initialAmount = 0` exists for the customer.
3. Assert no receivable was generated for this payment reversal (no overpayment to reverse).

**Expected test case results:**
- No zero-amount receivable is generated from the payment reversal.
- PaymentService correctly identifies zero receivable difference and skips creation.

---

### TC-BE-39 (Positive): Liability from MLO reversal with non-zero amount

**Description:** Verify that reversing a manual liability offsetting (MLO) restores a liability with a non-zero initialAmount.

**Preconditions:**
1. Create customer via `POST /customer` with parameters: `{ type: "PRIVATE", firstName: "MLORevPos", lastName: "LiabTest", identificationNumber: "EGN0000000039", status: "ACTIVE" }`. Note the returned `customerId`.
2. Create liability via `POST /customer-liability` with parameters: `{ customerId: <customerId from step 1>, initialAmount: 500.00, currency: "BGN", description: "MLO test liability" }`. Note the returned `liabilityId`.
3. Create receivable via `POST /customer-receivable` with parameters: `{ customerId: <customerId from step 1>, initialAmount: 300.00, currency: "BGN", description: "MLO test receivable" }`. Note the returned `receivableId`.
4. Create MLO via `POST /manual-liability-offsetting` with parameters: `{ liabilityId: <liabilityId from step 2>, receivableId: <receivableId from step 3>, amount: 300.00 }`. Note the returned `mloId`.
5. Reverse MLO via `POST /manual-liability-offsetting/reverse` with parameters: `{ mloId: <mloId from step 4> }`.

**Test steps:**
1. Assert HTTP response status is `200 OK` or `201 Created` from precondition step 5.
2. Send `GET /customer-liability?customerId=<customerId>` to list all liabilities.
3. Assert a restored/reversal liability exists with `initialAmount = 300.00` (restoring the offset amount).
4. Assert no liability with `initialAmount = 0` exists.

**Expected test case results:**
- Liability is restored from MLO reversal with `initialAmount = 300.00`.
- ManualLiabilityOffsettingService correctly creates the reversal liability.

---

### TC-BE-40 (Negative): MLO reversal with zero amount does not generate zero-amount liability

**Description:** Verify that reversing an MLO that would produce a zero-amount liability does not create one.

**Preconditions:**
1. Create customer via `POST /customer` with parameters: `{ type: "PRIVATE", firstName: "MLORevNeg", lastName: "LiabTest", identificationNumber: "EGN0000000040", status: "ACTIVE" }`. Note the returned `customerId`.
2. Create liability via `POST /customer-liability` with parameters: `{ customerId: <customerId from step 1>, initialAmount: 100.00, currency: "BGN" }`. Note the returned `liabilityId`.
3. Create receivable via `POST /customer-receivable` with parameters: `{ customerId: <customerId from step 1>, initialAmount: 100.00, currency: "BGN" }`. Note the returned `receivableId`.
4. Create MLO via `POST /manual-liability-offsetting` with parameters configured such that the reversal would produce a zero-amount net liability.
5. Reverse MLO via `POST /manual-liability-offsetting/reverse`.

**Test steps:**
1. Send `GET /customer-liability?customerId=<customerId>` to list all liabilities.
2. Assert no liability with `initialAmount = 0` exists for the customer.

**Expected test case results:**
- No zero-amount liability is generated from the MLO reversal.
- ManualLiabilityOffsettingService skips zero-amount liability creation.

---

### TC-BE-41 (Positive): Receivable from MLO reversal with non-zero amount

**Description:** Verify that reversing an MLO restores a receivable with a non-zero initialAmount.

**Preconditions:**
1. Create customer via `POST /customer` with parameters: `{ type: "PRIVATE", firstName: "MLORevPos", lastName: "RecvTest", identificationNumber: "EGN0000000041", status: "ACTIVE" }`. Note the returned `customerId`.
2. Create liability via `POST /customer-liability` with parameters: `{ customerId: <customerId from step 1>, initialAmount: 400.00, currency: "BGN" }`. Note the returned `liabilityId`.
3. Create receivable via `POST /customer-receivable` with parameters: `{ customerId: <customerId from step 1>, initialAmount: 250.00, currency: "BGN" }`. Note the returned `receivableId`.
4. Create MLO via `POST /manual-liability-offsetting` with parameters: `{ liabilityId: <liabilityId from step 2>, receivableId: <receivableId from step 3>, amount: 250.00 }`. Note the returned `mloId`.
5. Reverse MLO via `POST /manual-liability-offsetting/reverse` with parameters: `{ mloId: <mloId from step 4> }`.

**Test steps:**
1. Assert HTTP response status is `200 OK` or `201 Created` from precondition step 5.
2. Send `GET /customer-receivable?customerId=<customerId>` to list receivables.
3. Assert a restored/reversal receivable exists with `initialAmount = 250.00`.
4. Assert no receivable with `initialAmount = 0` exists.

**Expected test case results:**
- Receivable is restored from MLO reversal with `initialAmount = 250.00`.
- ManualLiabilityOffsettingService correctly creates the reversal receivable.

---

### TC-BE-42 (Negative): MLO reversal with zero receivable does not generate zero-amount receivable

**Description:** Verify that reversing an MLO that would produce a zero-amount receivable does not create one.

**Preconditions:**
1. Create customer via `POST /customer` with parameters: `{ type: "PRIVATE", firstName: "MLORevNeg", lastName: "RecvTest", identificationNumber: "EGN0000000042", status: "ACTIVE" }`. Note the returned `customerId`.
2. Create liability via `POST /customer-liability` with parameters: `{ customerId: <customerId from step 1>, initialAmount: 200.00, currency: "BGN" }`. Note the returned `liabilityId`.
3. Create receivable via `POST /customer-receivable` with parameters: `{ customerId: <customerId from step 1>, initialAmount: 200.00, currency: "BGN" }`. Note the returned `receivableId`.
4. Create MLO via `POST /manual-liability-offsetting` with parameters configured such that reversal produces zero receivable amount.
5. Reverse MLO via `POST /manual-liability-offsetting/reverse`.

**Test steps:**
1. Send `GET /customer-receivable?customerId=<customerId>` to list receivables.
2. Assert no receivable with `initialAmount = 0` exists for the customer.

**Expected test case results:**
- No zero-amount receivable is generated from the MLO reversal.
- ManualLiabilityOffsettingService skips zero-amount receivable creation.

---

### TC-BE-43 (Positive): Receivable from LPF reversal with non-zero amount

**Description:** Verify that reversing a late payment fine generates a receivable (refund of fine) with a non-zero initialAmount.

**Preconditions:**
1. Create customer via `POST /customer` with parameters: `{ type: "PRIVATE", firstName: "LPFRevPos", lastName: "RecvTest", identificationNumber: "EGN0000000043", status: "ACTIVE" }`. Note the returned `customerId`.
2. Create POD via `POST /pod` with parameters: `{ type: "ELECTRICITY", identifier: "BG-POD-LPFREV-001", status: "ACTIVE", activationDate: "2024-01-01" }`. Note the returned `podId`.
3. Create product via `POST /product` with parameters: `{ name: "LPF Reversal Product", term: "INDEFINITE", dataDeliveryType: "BY_PROFILE", status: "ACTIVE" }`. Note the returned `productId`.
4. Create terms via `POST /terms` with parameters: `{ productId: <productId from step 3>, name: "Standard Terms", validFrom: "2024-01-01" }`.
5. Create price component via `POST /price-component` with parameters: `{ productId: <productId from step 3>, type: "ENERGY", rate: 0.15, currency: "BGN", unit: "kWh", validFrom: "2024-01-01" }`.
6. Create product contract via `POST /product-contract` with parameters: `{ customerId: <customerId from step 1>, podId: <podId from step 2>, productId: <productId from step 3>, status: "ACTIVE", entryIntoForceDate: "2025-01-01" }`. Note the returned `contractId`.
7. Create energy data via energy data endpoint with parameters: `{ contractId: <contractId from step 6>, podId: <podId from step 2>, consumptionKwh: 1000, periodFrom: "2025-01-01", periodTo: "2025-01-31" }`.
8. Create billing run via `POST /billing-run` with parameters: `{ type: "STANDARD", periodFrom: "2025-01-01", periodTo: "2025-01-31", contractId: <contractId from step 6> }`. Note the returned `billingRunId`.
9. Execute billing run via `POST /billing-run/{billingRunId}/execute`. Wait for completion. Liability created with `initialAmount = 150.00`. Note the `liabilityId`.
10. Liability becomes overdue (due date passes without payment).
11. LPF is calculated and an LPF liability is created (e.g., fine amount = 5.00 BGN). Note the `lpfLiabilityId`.
12. LPF liability is paid via `POST /payment` with parameters: `{ customerId: <customerId>, amount: 5.00, currency: "BGN" }`.
13. Reverse LPF via the LPF reversal endpoint (e.g., `POST /late-payment-fine/reverse` with `{ lpfLiabilityId: <lpfLiabilityId from step 11> }`).

**Test steps:**
1. Assert HTTP response status is `200 OK` or `201 Created` from precondition step 13.
2. Send `GET /customer-receivable?customerId=<customerId>` to list receivables.
3. Assert a receivable exists with `initialAmount = 5.00` (refund of the reversed LPF).
4. Assert no receivable with `initialAmount = 0` exists.

**Expected test case results:**
- Receivable is generated from LPF reversal with `initialAmount = 5.00`.
- LatePaymentFineService correctly creates the refund receivable.

---

### TC-BE-44 (Negative): LPF reversal with zero amount does not generate zero-amount receivable

**Description:** Verify that reversing an LPF that would produce a zero-amount receivable does not create one.

**Preconditions:**
1–11. Same as TC-BE-43 preconditions (billing chain + LPF calculation).
12. LPF liability is NOT paid (unpaid LPF → reversal produces zero refund receivable).
13. Reverse LPF via the LPF reversal endpoint.

**Test steps:**
1. Send `GET /customer-receivable?customerId=<customerId>` to list receivables.
2. Assert no receivable with `initialAmount = 0` exists for the customer.

**Expected test case results:**
- No zero-amount receivable is generated from the LPF reversal.
- LatePaymentFineService skips zero-amount receivable creation.

---

### TC-BE-45 (Positive): Receivable from rescheduling reversal with non-zero amount

**Description:** Verify that reversing a rescheduling plan generates receivables for payments already made on installments, each with a non-zero initialAmount.

**Preconditions:**
1. Create customer via `POST /customer` with parameters: `{ type: "PRIVATE", firstName: "ReschRevPos", lastName: "RecvTest", identificationNumber: "EGN0000000045", status: "ACTIVE" }`. Note the returned `customerId`.
2. Create POD via `POST /pod` with parameters: `{ type: "ELECTRICITY", identifier: "BG-POD-RESCHREV-001", status: "ACTIVE", activationDate: "2024-01-01" }`. Note the returned `podId`.
3. Create product via `POST /product` with parameters: `{ name: "Rescheduling Reversal Product", term: "INDEFINITE", dataDeliveryType: "BY_PROFILE", status: "ACTIVE" }`. Note the returned `productId`.
4. Create terms via `POST /terms` with parameters: `{ productId: <productId from step 3>, name: "Standard Terms", validFrom: "2024-01-01" }`.
5. Create price component via `POST /price-component` with parameters: `{ productId: <productId from step 3>, type: "ENERGY", rate: 0.20, currency: "BGN", unit: "kWh", validFrom: "2024-01-01" }`.
6. Create product contract via `POST /product-contract` with parameters: `{ customerId: <customerId from step 1>, podId: <podId from step 2>, productId: <productId from step 3>, status: "ACTIVE", entryIntoForceDate: "2025-01-01" }`. Note the returned `contractId`.
7. Create energy data via energy data endpoint with parameters: `{ contractId: <contractId from step 6>, podId: <podId from step 2>, consumptionKwh: 3000, periodFrom: "2025-01-01", periodTo: "2025-01-31" }`.
8. Create billing run via `POST /billing-run` with parameters: `{ type: "STANDARD", periodFrom: "2025-01-01", periodTo: "2025-01-31", contractId: <contractId from step 6> }`. Note the returned `billingRunId`.
9. Execute billing run via `POST /billing-run/{billingRunId}/execute`. Liability created with `initialAmount = 600.00`. Note the `liabilityId`.
10. Create rescheduling plan via `POST /rescheduling` with parameters: `{ liabilityId: <liabilityId from step 9>, installmentCount: 3, installmentAmount: 200.00 }`. Note the returned `reschedulingId`.
11. Pay one installment via `POST /payment` with parameters: `{ customerId: <customerId>, amount: 200.00, currency: "BGN" }`.
12. Reverse rescheduling via `POST /rescheduling/reverse` with parameters: `{ reschedulingId: <reschedulingId from step 10> }`.

**Test steps:**
1. Assert HTTP response status is `200 OK` or `201 Created` from precondition step 12.
2. Send `GET /customer-receivable?customerId=<customerId>` to list receivables.
3. Assert a receivable exists with `initialAmount = 200.00` (refund for the paid installment).
4. Assert no receivable with `initialAmount = 0` exists.

**Expected test case results:**
- Receivable is generated from rescheduling reversal with `initialAmount = 200.00`.
- ReschedulingService correctly creates refund receivables for paid installments.

---

### TC-BE-46 (Negative): Rescheduling reversal with zero amount does not generate zero-amount receivable

**Description:** Verify that reversing a rescheduling where no installments were paid does not generate a zero-amount receivable.

**Preconditions:**
1–10. Same as TC-BE-45 preconditions (billing chain + rescheduling plan with 3 installments).
11. No installments are paid.
12. Reverse rescheduling via `POST /rescheduling/reverse` with parameters: `{ reschedulingId: <reschedulingId from step 10> }`.

**Test steps:**
1. Send `GET /customer-receivable?customerId=<customerId>` to list receivables.
2. Assert no receivable with `initialAmount = 0` exists for the customer.
3. Assert no receivable was generated (no paid installments to refund).

**Expected test case results:**
- No zero-amount receivable is generated from the rescheduling reversal.
- ReschedulingService skips receivable creation when refund amount is zero.

---

### TC-BE-47 (Positive): Receivable from credit note via billing run with non-zero amount

**Description:** Verify that a billing run producing a credit note generates a receivable with a non-zero initialAmount.

**Preconditions:**
1. Create customer via `POST /customer` with parameters: `{ type: "PRIVATE", firstName: "CreditNotePos", lastName: "RecvTest", identificationNumber: "EGN0000000047", status: "ACTIVE" }`. Note the returned `customerId`.
2. Create POD via `POST /pod` with parameters: `{ type: "ELECTRICITY", identifier: "BG-POD-CREDIT-001", status: "ACTIVE", activationDate: "2024-01-01" }`. Note the returned `podId`.
3. Create product via `POST /product` with parameters: `{ name: "Credit Note Product", term: "INDEFINITE", dataDeliveryType: "BY_PROFILE", status: "ACTIVE" }`. Note the returned `productId`.
4. Create terms via `POST /terms` with parameters: `{ productId: <productId from step 3>, name: "Standard Terms", validFrom: "2024-01-01" }`.
5. Create price component via `POST /price-component` with parameters: `{ productId: <productId from step 3>, type: "ENERGY", rate: 0.15, currency: "BGN", unit: "kWh", validFrom: "2024-01-01" }`.
6. Create product contract via `POST /product-contract` with parameters: `{ customerId: <customerId from step 1>, podId: <podId from step 2>, productId: <productId from step 3>, status: "ACTIVE", entryIntoForceDate: "2025-01-01" }`. Note the returned `contractId`.
7. Create energy data via energy data endpoint with parameters: `{ contractId: <contractId from step 6>, podId: <podId from step 2>, consumptionKwh: 1000, periodFrom: "2025-01-01", periodTo: "2025-01-31" }` (original).
8. Create corrective billing run via `POST /billing-run` with parameters: `{ type: "CORRECTION", periodFrom: "2025-01-01", periodTo: "2025-01-31", contractId: <contractId from step 6> }` (configured to produce a credit note — e.g., reduced consumption from correction). Note the returned `billingRunId`.
9. Execute corrective billing run via `POST /billing-run/{billingRunId}/execute`. Wait for completion. Credit note with amount > 0 is generated (e.g., 30.00 BGN credit).

**Test steps:**
1. Send `GET /customer-receivable?customerId=<customerId>` to list receivables.
2. Assert a receivable exists with `initialAmount = 30.00` (credit note amount).
3. Assert no receivable with `initialAmount = 0` exists.

**Expected test case results:**
- Receivable is generated from credit note with `initialAmount = 30.00`.
- BillingRunStartAccountingInvokeService correctly creates the credit note receivable.

---

### TC-BE-48 (Negative): Credit note with zero amount does not generate zero-amount receivable

**Description:** Verify that when a corrective billing run produces a credit note with zero amount (no correction difference), no zero-amount receivable is created.

**Preconditions:**
1–7. Same as TC-BE-47 preconditions (billing chain).
8. Create corrective billing run via `POST /billing-run` with parameters: `{ type: "CORRECTION", periodFrom: "2025-01-01", periodTo: "2025-01-31", contractId: <contractId from step 6> }` (configured to produce zero correction difference — same consumption data). Note the returned `billingRunId`.
9. Execute corrective billing run via `POST /billing-run/{billingRunId}/execute`. Wait for completion. Credit note amount = 0.00 (no difference).

**Test steps:**
1. Send `GET /customer-receivable?customerId=<customerId>` to list receivables.
2. Assert no receivable with `initialAmount = 0` exists for the customer.

**Expected test case results:**
- No zero-amount receivable is generated from the zero-amount credit note.
- BillingRunStartAccountingInvokeService skips creation when credit note amount is 0.

---

### TC-BE-49 (Positive): Receivable from compensation with non-zero amount

**Description:** Verify that the compensation flow generates a receivable with a non-zero initialAmount when there is a genuine compensation amount.

**Preconditions:**
1. Create customer via `POST /customer` with parameters: `{ type: "PRIVATE", firstName: "CompPos", lastName: "RecvTest", identificationNumber: "EGN0000000049", status: "ACTIVE" }`. Note the returned `customerId`.
2. Create POD via `POST /pod` with parameters: `{ type: "ELECTRICITY", identifier: "BG-POD-COMP-001", status: "ACTIVE", activationDate: "2024-01-01" }`. Note the returned `podId`.
3. Create product via `POST /product` with parameters: `{ name: "Compensation Product", term: "INDEFINITE", dataDeliveryType: "BY_PROFILE", status: "ACTIVE" }`. Note the returned `productId`.
4. Create terms via `POST /terms` with parameters: `{ productId: <productId from step 3>, name: "Standard Terms", validFrom: "2024-01-01" }`.
5. Create price component via `POST /price-component` with parameters: `{ productId: <productId from step 3>, type: "ENERGY", rate: 0.15, currency: "BGN", unit: "kWh", validFrom: "2024-01-01" }`.
6. Create product contract via `POST /product-contract` with parameters: `{ customerId: <customerId from step 1>, podId: <podId from step 2>, productId: <productId from step 3>, status: "ACTIVE", entryIntoForceDate: "2025-01-01" }`. Note the returned `contractId`.
7. Create energy data via energy data endpoint with parameters: `{ contractId: <contractId from step 6>, podId: <podId from step 2>, consumptionKwh: 800, periodFrom: "2025-01-01", periodTo: "2025-01-31" }`.
8. Create existing receivable (overpayment) via `POST /customer-receivable` with parameters: `{ customerId: <customerId from step 1>, initialAmount: 50.00, currency: "BGN" }` to provide a balance for compensation.
9. Create billing run via `POST /billing-run` with parameters: `{ type: "STANDARD", periodFrom: "2025-01-01", periodTo: "2025-01-31", contractId: <contractId from step 6> }` with compensation logic enabled. Note the returned `billingRunId`.
10. Execute billing run via `POST /billing-run/{billingRunId}/execute`. Wait for completion. Compensation produces a non-zero receivable.

**Test steps:**
1. Send `GET /customer-receivable?customerId=<customerId>` to list receivables.
2. Assert a compensation receivable exists with `initialAmount > 0`.
3. Assert no receivable with `initialAmount = 0` exists for the customer.

**Expected test case results:**
- Compensation receivable is generated with a non-zero `initialAmount`.
- CompensationReceivableService correctly creates the receivable.

---

### TC-BE-50 (Negative): Compensation with zero amount does not generate zero-amount receivable

**Description:** Verify that when the compensation calculation produces zero (e.g., liabilities and receivables exactly balance), no zero-amount receivable is created.

**Preconditions:**
1–7. Same as TC-BE-49 preconditions (billing chain).
8. Create existing receivable via `POST /customer-receivable` with parameters configured so that compensation exactly offsets existing liabilities (zero net compensation).
9. Create billing run via `POST /billing-run` with compensation logic. Note the returned `billingRunId`.
10. Execute billing run. Compensation calculation results in zero.

**Test steps:**
1. Send `GET /customer-receivable?customerId=<customerId>` to list receivables.
2. Assert no receivable with `initialAmount = 0` was created during this billing run's compensation phase.

**Expected test case results:**
- No zero-amount receivable is generated from the compensation.
- CompensationReceivableService skips creation when compensation amount is zero.

---

### TC-BE-51 (Positive): Liability from VAT base adjustment with non-zero amount

**Description:** Verify that a billing run with VAT base adjustment generates a liability with a non-zero initialAmount.

**Preconditions:**
1. Create customer via `POST /customer` with parameters: `{ type: "PRIVATE", firstName: "VATPos", lastName: "LiabTest", identificationNumber: "EGN0000000051", status: "ACTIVE" }`. Note the returned `customerId`.
2. Create POD via `POST /pod` with parameters: `{ type: "ELECTRICITY", identifier: "BG-POD-VAT-001", status: "ACTIVE", activationDate: "2024-01-01" }`. Note the returned `podId`.
3. Create product via `POST /product` with parameters: `{ name: "VAT Adjustment Product", term: "INDEFINITE", dataDeliveryType: "BY_PROFILE", status: "ACTIVE", vatApplicable: true }`. Note the returned `productId`.
4. Create terms via `POST /terms` with parameters: `{ productId: <productId from step 3>, name: "Standard Terms", validFrom: "2024-01-01" }`.
5. Create price component via `POST /price-component` with parameters: `{ productId: <productId from step 3>, type: "ENERGY", rate: 0.20, currency: "BGN", unit: "kWh", validFrom: "2024-01-01", vatRate: 20 }`.
6. Create product contract via `POST /product-contract` with parameters: `{ customerId: <customerId from step 1>, podId: <podId from step 2>, productId: <productId from step 3>, status: "ACTIVE", entryIntoForceDate: "2025-01-01" }`. Note the returned `contractId`.
7. Create energy data via energy data endpoint with parameters: `{ contractId: <contractId from step 6>, podId: <podId from step 2>, consumptionKwh: 1000, periodFrom: "2025-01-01", periodTo: "2025-01-31" }`.
8. Create billing run via `POST /billing-run` with parameters: `{ type: "STANDARD", periodFrom: "2025-01-01", periodTo: "2025-01-31", contractId: <contractId from step 6> }` with VAT base adjustment applicable. Note the returned `billingRunId`.
9. Execute billing run via `POST /billing-run/{billingRunId}/execute`. Wait for completion.
10. VAT base adjustment produces a non-zero liability.

**Test steps:**
1. Send `GET /customer-liability?customerId=<customerId>` to list liabilities.
2. Assert a VAT-related liability exists with `initialAmount > 0`.
3. Assert no liability with `initialAmount = 0` exists.

**Expected test case results:**
- Liability from VAT base adjustment is generated with a non-zero `initialAmount`.
- The mapper correctly calculates the VAT adjustment amount.

---

### TC-BE-52 (Negative): VAT base with zero summarized amount skips liability creation (mapper lines ~422-424)

**Description:** Verify that when the VAT base adjustment mapper calculates a zero summarized amount (lines ~422-424), liability creation is skipped and no zero-amount liability is persisted.

**Preconditions:**
1–7. Same as TC-BE-51 preconditions (billing chain with VAT product).
8. Configure energy data so that the VAT base adjustment calculation results in zero (e.g., all VAT components exactly cancel out or consumption is zero).
9. Create billing run via `POST /billing-run` with parameters. Note the returned `billingRunId`.
10. Execute billing run. VAT base summarized amount = 0.

**Test steps:**
1. Send `GET /customer-liability?customerId=<customerId>` to list liabilities.
2. Assert no liability with `initialAmount = 0` exists for the customer.
3. Verify the VAT base adjustment was skipped (no VAT-type liability created).

**Expected test case results:**
- No zero-amount liability is generated from VAT base adjustment.
- The mapper at lines ~422-424 correctly skips creation when summarized amount is 0.

---

### TC-BE-53 (Positive): Liability from disconnection with non-zero amount

**Description:** Verify that processing a disconnection request with associated costs generates a liability with a non-zero initialAmount.

**Preconditions:**
1. Create customer via `POST /customer` with parameters: `{ type: "PRIVATE", firstName: "DisconnPos", lastName: "LiabTest", identificationNumber: "EGN0000000053", status: "ACTIVE" }`. Note the returned `customerId`.
2. Create POD via `POST /pod` with parameters: `{ type: "ELECTRICITY", identifier: "BG-POD-DISC-001", status: "ACTIVE", activationDate: "2024-01-01" }`. Note the returned `podId`.
3. Create product via `POST /product` with parameters: `{ name: "Disconnection Product", term: "INDEFINITE", dataDeliveryType: "BY_PROFILE", status: "ACTIVE" }`. Note the returned `productId`.
4. Create product contract via `POST /product-contract` with parameters: `{ customerId: <customerId from step 1>, podId: <podId from step 2>, productId: <productId from step 3>, status: "ACTIVE", entryIntoForceDate: "2025-01-01" }`. Note the returned `contractId`.
5. Submit disconnection request via `POST /disconnection` with parameters: `{ contractPodId: <contractPodId from the contract>, reason: "NON_PAYMENT", disconnectionFee: 50.00, currency: "BGN" }`. Note the returned `disconnectionId`.

**Test steps:**
1. Send `GET /customer-liability?customerId=<customerId>` to list liabilities.
2. Assert a liability exists with `initialAmount = 50.00` linked to the disconnection.
3. Assert the liability `currency` is `"BGN"`.

**Expected test case results:**
- Liability is generated from disconnection with `initialAmount = 50.00`.
- DisconnectionOfPowerSupplyService correctly creates the disconnection liability.

---

### TC-BE-54 (Negative): Disconnection with zero invoice total does not generate zero-amount liability

**Description:** Verify that when a disconnection is processed with no associated fee (zero invoice total), no zero-amount liability is created.

**Preconditions:**
1–4. Same as TC-BE-53 preconditions (customer + POD + product + contract).
5. Submit disconnection request via `POST /disconnection` with parameters: `{ contractPodId: <contractPodId>, reason: "CUSTOMER_REQUEST", disconnectionFee: 0, currency: "BGN" }` (no fee for customer-requested disconnection).

**Test steps:**
1. Send `GET /customer-liability?customerId=<customerId>` to list liabilities.
2. Assert no liability with `initialAmount = 0` exists for the customer.
3. Assert no disconnection-related liability was created.

**Expected test case results:**
- No zero-amount liability is generated from the disconnection.
- DisconnectionOfPowerSupplyService skips liability creation when fee is zero.

---

### TC-BE-55 (Negative): ZeroAmountValidationListener blocks zero-amount CustomerLiability at @PrePersist

**Description:** Verify that the ZeroAmountValidationListener (@PrePersist JPA listener on CustomerLiability) throws an IllegalArgumentException when an attempt is made to persist a CustomerLiability entity with initialAmount == 0, regardless of the originating flow.

**Preconditions:**
1. Create customer via `POST /customer` with parameters: `{ type: "PRIVATE", firstName: "ListenerNeg", lastName: "LiabTest", identificationNumber: "EGN0000000055", status: "ACTIVE" }`. Note the returned `customerId`.

**Test steps:**
1. Attempt to create a liability through any flow that bypasses request-level validation but results in `initialAmount = 0` being set on the entity before persist (e.g., a service-level flow that constructs the entity directly).
2. Assert that the persistence operation fails with an `IllegalArgumentException` thrown by `ZeroAmountValidationListener`.
3. Assert the error message indicates that zero-amount liabilities are not allowed.
4. Send `GET /customer-liability?customerId=<customerId>`.
5. Assert no liability with `initialAmount = 0` exists — the listener prevented persistence.

**Expected test case results:**
- `ZeroAmountValidationListener` intercepts the `@PrePersist` event.
- `IllegalArgumentException` is thrown with a message about zero amount.
- The entity is NOT persisted to the database.

---

### TC-BE-56 (Negative): ZeroAmountValidationListener blocks zero-amount CustomerReceivable at @PrePersist

**Description:** Verify that the ZeroAmountValidationListener (@PrePersist JPA listener on CustomerReceivable) throws an IllegalArgumentException when an attempt is made to persist a CustomerReceivable entity with initialAmount == 0, regardless of the originating flow.

**Preconditions:**
1. Create customer via `POST /customer` with parameters: `{ type: "PRIVATE", firstName: "ListenerNeg", lastName: "RecvTest", identificationNumber: "EGN0000000056", status: "ACTIVE" }`. Note the returned `customerId`.

**Test steps:**
1. Attempt to create a receivable through any flow that bypasses request-level validation but results in `initialAmount = 0` being set on the entity before persist (e.g., a service-level flow that constructs the entity directly).
2. Assert that the persistence operation fails with an `IllegalArgumentException` thrown by `ZeroAmountValidationListener`.
3. Assert the error message indicates that zero-amount receivables are not allowed.
4. Send `GET /customer-receivable?customerId=<customerId>`.
5. Assert no receivable with `initialAmount = 0` exists — the listener prevented persistence.

**Expected test case results:**
- `ZeroAmountValidationListener` intercepts the `@PrePersist` event.
- `IllegalArgumentException` is thrown with a message about zero amount.
- The entity is NOT persisted to the database.

---

### TC-BE-57 (Negative): Manual liability creation with negative amount rejected

**Description:** Verify that attempting to create a liability with a negative initialAmount is rejected at the request validation level (@DecimalMin(value="0", inclusive=false) rejects values <= 0).

**Preconditions:**
1. Create customer via `POST /customer` with parameters: `{ type: "PRIVATE", firstName: "NegLiab", lastName: "Test", identificationNumber: "EGN0000000057", status: "ACTIVE" }`. Note the returned `customerId`.

**Test steps:**
1. Send `POST /customer-liability` with payload: `{ customerId: <customerId from precondition step 1>, initialAmount: -50.00, currency: "BGN", description: "Negative amount liability" }`.
2. Assert HTTP response status is `400 Bad Request`.
3. Assert response body contains a validation error referencing `initialAmount` and the constraint `@DecimalMin`.
4. Send `GET /customer-liability?customerId=<customerId>`.
5. Assert no liability with `initialAmount = -50.00` exists for the customer.

**Expected test case results:**
- Request is rejected with `400 Bad Request`.
- Validation error message indicates `initialAmount` must be greater than 0.
- No negative-amount liability is persisted.

---

### TC-BE-58 (Negative): Manual receivable creation with negative amount rejected

**Description:** Verify that attempting to create a receivable with a negative initialAmount is rejected at the request validation level (@Positive rejects values <= 0).

**Preconditions:**
1. Create customer via `POST /customer` with parameters: `{ type: "PRIVATE", firstName: "NegRecv", lastName: "Test", identificationNumber: "EGN0000000058", status: "ACTIVE" }`. Note the returned `customerId`.

**Test steps:**
1. Send `POST /customer-receivable` with payload: `{ customerId: <customerId from precondition step 1>, initialAmount: -50.00, currency: "BGN", description: "Negative amount receivable" }`.
2. Assert HTTP response status is `400 Bad Request`.
3. Assert response body contains a validation error referencing `initialAmount` and the constraint `@Positive`.
4. Send `GET /customer-receivable?customerId=<customerId>`.
5. Assert no receivable with `initialAmount = -50.00` exists for the customer.

**Expected test case results:**
- Request is rejected with `400 Bad Request`.
- Validation error message indicates `initialAmount` must be positive.
- No negative-amount receivable is persisted.

---

### TC-BE-59 (Positive): Receivable from invoice correction with non-zero amount

**Description:** Verify that a correction billing run that produces a difference from the original invoice generates a receivable with a non-zero initialAmount.

**Preconditions:**
1. Create customer via `POST /customer` with parameters: `{ type: "PRIVATE", firstName: "InvCorrPos", lastName: "RecvTest", identificationNumber: "EGN0000000059", status: "ACTIVE" }`. Note the returned `customerId`.
2. Create POD via `POST /pod` with parameters: `{ type: "ELECTRICITY", identifier: "BG-POD-CORR-001", status: "ACTIVE", activationDate: "2024-01-01" }`. Note the returned `podId`.
3. Create product via `POST /product` with parameters: `{ name: "Invoice Correction Product", term: "INDEFINITE", dataDeliveryType: "BY_PROFILE", status: "ACTIVE" }`. Note the returned `productId`.
4. Create terms via `POST /terms` with parameters: `{ productId: <productId from step 3>, name: "Standard Terms", validFrom: "2024-01-01" }`.
5. Create price component via `POST /price-component` with parameters: `{ productId: <productId from step 3>, type: "ENERGY", rate: 0.15, currency: "BGN", unit: "kWh", validFrom: "2024-01-01" }`.
6. Create product contract via `POST /product-contract` with parameters: `{ customerId: <customerId from step 1>, podId: <podId from step 2>, productId: <productId from step 3>, status: "ACTIVE", entryIntoForceDate: "2025-01-01" }`. Note the returned `contractId`.
7. Create energy data via energy data endpoint with parameters: `{ contractId: <contractId from step 6>, podId: <podId from step 2>, consumptionKwh: 1000, periodFrom: "2025-01-01", periodTo: "2025-01-31" }`.
8. Create original billing run via `POST /billing-run` with parameters: `{ type: "STANDARD", periodFrom: "2025-01-01", periodTo: "2025-01-31", contractId: <contractId from step 6> }`. Note the returned `billingRunId`.
9. Execute original billing run via `POST /billing-run/{billingRunId}/execute`. Wait for completion. Original invoice total = 150.00 BGN. Note the `originalInvoiceId`.
10. Create correction billing run via `POST /billing-run` with parameters: `{ type: "CORRECTION", periodFrom: "2025-01-01", periodTo: "2025-01-31", contractId: <contractId from step 6>, originalInvoiceId: <originalInvoiceId from step 9> }` (with corrected energy data showing lower consumption, e.g., 800 kWh → corrected total = 120.00 BGN, difference = 30.00 BGN). Note the returned `correctionBillingRunId`.
11. Execute correction billing run via `POST /billing-run/{correctionBillingRunId}/execute`. Wait for completion. Correction produces receivable for the 30.00 BGN overpayment.

**Test steps:**
1. Send `GET /customer-receivable?customerId=<customerId>` to list receivables.
2. Assert a receivable exists with `initialAmount = 30.00` (correction difference: 150.00 - 120.00).
3. Assert no receivable with `initialAmount = 0` exists.
4. Assert the receivable is linked to the correction invoice.

**Expected test case results:**
- Receivable is generated from invoice correction with `initialAmount = 30.00`.
- The correction billing run correctly calculates the difference and creates the receivable.

---

### TC-BE-60 (Negative): Invoice correction with zero correction amount does not generate zero-amount receivable

**Description:** Verify that when a correction billing run produces zero difference from the original invoice (same consumption, same prices), no zero-amount receivable is created.

**Preconditions:**
1–9. Same as TC-BE-59 preconditions (billing chain with original invoice total = 150.00).
10. Create correction billing run via `POST /billing-run` with parameters: `{ type: "CORRECTION", periodFrom: "2025-01-01", periodTo: "2025-01-31", contractId: <contractId from step 6>, originalInvoiceId: <originalInvoiceId from step 9> }` (with IDENTICAL energy data — same consumption of 1000 kWh → corrected total = 150.00 BGN, difference = 0). Note the returned `correctionBillingRunId`.
11. Execute correction billing run via `POST /billing-run/{correctionBillingRunId}/execute`. Wait for completion. Correction difference = 0.00.

**Test steps:**
1. Send `GET /customer-receivable?customerId=<customerId>` to list receivables.
2. Assert no receivable with `initialAmount = 0` exists for the customer.
3. Assert no receivable was generated from this correction billing run.

**Expected test case results:**
- No zero-amount receivable is generated from the zero-difference correction.
- The correction billing run correctly identifies zero difference and skips receivable creation.

---

## References

- **Jira:** PDT-2474 — "Liabilities and receivables shouldn't be generated with amount zero"
- **ZeroAmountValidationListener:** `@PrePersist` JPA listener on `CustomerLiability` and `CustomerReceivable`; throws `IllegalArgumentException` when `initialAmount == 0`
- **CustomerLiabilityRequest:** `@DecimalMin(value="0", inclusive=false)` on `initialAmount`
- **CustomerReceivableRequest:** `@Positive` on `initialAmount`
- **CustomerLiabilityService:** Central service for all liability creation flows
- **CustomerReceivableService:** Central service for all receivable creation flows
- **BillingRunStartAccountingInvokeService:** Generates liabilities/receivables from billing run invoices
- **DepositService:** Generates liabilities/receivables from deposit operations
- **PaymentService:** Generates liabilities/receivables from payment operations
- **LatePaymentFineService:** Generates LPF liabilities
- **ReschedulingService:** Generates installment liabilities from rescheduling
- **ManualLiabilityOffsettingService:** MLO and MLO reversal flows
- **InvoiceCancellationService:** Generates liabilities/receivables from invoice cancellation
- **InvoiceReversalProcessService:** Generates liabilities/receivables from invoice reversal (signum branching)
- **GoodsOrderProcessService:** Generates liabilities from goods order invoices
- **ServiceOrderProcessService:** Generates liabilities from service order invoices
- **CompensationReceivableService:** Generates receivables from compensation flows
- **DisconnectionOfPowerSupplyService:** Generates liabilities from disconnection
- **Entity dependency order:** Customer → Product → Terms → Price Component → Product Contract → Energy Data → Billing Run
