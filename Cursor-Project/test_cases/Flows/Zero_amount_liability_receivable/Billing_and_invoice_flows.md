# Billing Run and Invoice Flows – No Zero-Amount Liability or Receivable Persisted (PDT-2474)

**Jira:** PDT-2474 (Phoenix)  
**Type:** Task / Customer Feedback  
**Summary:** Flows that generate liabilities and receivables (billing run, invoice cancellation, reversals, credit notes, etc.) must not persist CustomerLiability or CustomerReceivable with initial amount zero. When the computed amount is zero, the system must skip creating the record or handle it so that no zero-amount entity is stored.

**Scope:** This document covers billing run start accounting, invoice cancellation, invoice reversal, billing run credit notes and corrections, and related flows. The expected behaviour is that no liability or receivable with amount zero is ever persisted. Integration points: Billing run start accounting, Invoice cancellation, Invoice reversal, Billing run credit notes/corrections/VAT base/compensations; JPA @PrePersist (ZeroAmountValidationListener) or equivalent must prevent zero from being saved.

---

## Test data (preconditions)

- **Environment:** Test (or as specified in the ticket).
- **Customer and contracts:** A customer exists with product and/or service contracts and PODs (Points of Delivery) so that billing and invoicing flows can be executed.
- **Billing data:** Metering or billing input data exists such that a scenario can be constructed where the computed liability or receivable would be zero (e.g. zero consumption, full credit, or correction that nets to zero).
- **Invoice and payment data (for cancellation/reversal):** Where testing invoice cancellation or reversal, an invoice and optionally payment data exist so that the cancellation or reversal flow can be run; a scenario where the resulting liability/receivable would be zero can be set up or identified.

---

## TC-1 (Positive): Billing run does not persist liability or receivable when computed amount is zero

**Objective:** Verify that when a billing run (or start-accounting step) computes a liability or receivable with total amount zero (e.g. zero consumption, or line items that net to zero), the system does not create a CustomerLiability or CustomerReceivable record with initialAmount zero. Either the record is not created at all, or the flow skips persistence for that case.

**Preconditions:**
1. A billing run can be executed (e.g. billing run start accounting) for a customer/contract/POD combination.
2. The billing input or configuration is such that the computed liability (or receivable, if applicable) for at least one case is zero (e.g. zero volume, zero price, or adjustment that brings the total to zero).

**Steps:**
1. Set up or select a billing scenario where the computed liability (or receivable) amount is zero.
2. Execute the billing run (or the start-accounting step that creates liabilities/receivables).
3. Query or inspect the database (or API) for CustomerLiability and CustomerReceivable records created by this run.
4. Check that no record exists with initialAmount equal to zero for this run.

**Expected result:** No CustomerLiability or CustomerReceivable with initialAmount zero is persisted. The billing run either skips creating a record when the amount is zero, or validation (e.g. ZeroAmountValidationListener) prevents persistence. The user or downstream processes do not see a liability or receivable with amount zero.

**References:** PDT-2474; Billing run start accounting; integration point: Billing run; what could break: billing/invoice zero total must be skipped.

---

## TC-2 (Positive): Invoice cancellation does not persist zero-amount liability or receivable

**Objective:** Verify that when an invoice cancellation is performed and the cancellation would result in a liability or receivable with amount zero (e.g. cancelling an invoice that has zero total, or a cancellation that nets to zero), the system does not create a CustomerLiability or CustomerReceivable with initialAmount zero.

**Preconditions:**
1. An invoice exists that can be cancelled (e.g. not already cancelled, and in a state that allows cancellation).
2. The cancellation flow is such that it creates or updates liabilities/receivables; a scenario is chosen or constructed where the resulting amount would be zero (e.g. invoice with zero total, or cancellation adjustment that nets to zero).

**Steps:**
1. Perform the invoice cancellation (via API or the applicable flow) for the chosen invoice.
2. After cancellation completes, check the created or updated CustomerLiability and CustomerReceivable records linked to this cancellation.
3. Verify that no record has initialAmount equal to zero.

**Expected result:** Invoice cancellation completes without persisting any CustomerLiability or CustomerReceivable with initialAmount zero. If the cancellation would produce a zero amount, that record is not created or is skipped. No zero-amount liability or receivable appears in the system for this cancellation.

**References:** PDT-2474; Invoice cancellation flow; integration point: Invoice cancellation; what could break: reversal/cancellation zero must be skipped.

---

## TC-3 (Positive): Invoice reversal does not persist zero-amount liability or receivable

**Objective:** Verify that when an invoice reversal is performed and the reversal would result in a liability or receivable with amount zero, the system does not create a CustomerLiability or CustomerReceivable with initialAmount zero.

**Preconditions:**
1. An invoice exists that can be reversed (e.g. in a state that allows reversal).
2. The reversal flow creates or updates liabilities/receivables; the scenario yields a computed amount of zero (e.g. reversal of a zero-total invoice or adjustment that nets to zero).

**Steps:**
1. Perform the invoice reversal (via API or the applicable flow).
2. After reversal completes, check the created or updated CustomerLiability and CustomerReceivable records linked to this reversal.
3. Verify that no record has initialAmount equal to zero.

**Expected result:** Invoice reversal completes without persisting any CustomerLiability or CustomerReceivable with initialAmount zero. The flow either does not create a record when the amount is zero or validation prevents zero from being persisted.

**References:** PDT-2474; Invoice reversal; integration point: Invoice reversal; what could break: reversal zero must be skipped.

---

## TC-4 (Negative): Persistence layer rejects zero amount (e.g. JPA @PrePersist)

**Objective:** Verify that if any flow attempts to persist a CustomerLiability or CustomerReceivable with initialAmount zero (e.g. due to a bug or missing check upstream), the persistence layer (e.g. ZeroAmountValidationListener or equivalent @PrePersist logic) throws or rejects the persist so that no zero-amount record is ever stored. This is a defensive negative case ensuring the system fails safely.

**Preconditions:**
1. The system has a global validation (e.g. JPA @PrePersist on the entity or a listener) that checks initialAmount before persist.
2. A way to simulate or trigger an attempt to persist with zero amount exists (e.g. unit test, or a known path that would otherwise write zero).

**Steps:**
1. Trigger or simulate an attempt to persist a CustomerLiability or CustomerReceivable with initialAmount equal to zero (e.g. via the persistence layer or an integration test that bypasses API validation).
2. Observe the outcome: the persist must fail (exception or rollback) and no record with initialAmount zero must appear in the database.

**Expected result:** The persistence attempt fails (e.g. exception thrown or transaction rolled back). No CustomerLiability or CustomerReceivable with initialAmount zero is stored. This ensures that even if an upstream flow erroneously tries to persist zero, the system does not allow it.

**References:** PDT-2474; ZeroAmountValidationListener; JPA @PrePersist; integration point: persistence layer; what could break: deposit/LPF/rescheduling/payment/disconnection must not persist zero.

---

## TC-5 (Positive): Billing run credit note or correction does not persist zero-amount receivable

**Objective:** Verify that when a billing run generates credit notes, invoice corrections, VAT base adjustments, or compensations, and the computed receivable amount is zero, the system does not persist a CustomerReceivable with initialAmount zero.

**Preconditions:**
1. A billing run (or sub-flow) that creates receivables for credit notes, corrections, VAT base adjustments, or compensations can be executed.
2. The input or scenario is such that the computed receivable amount for at least one case is zero.

**Steps:**
1. Execute the flow that creates receivables for credit notes, corrections, VAT base adjustments, or compensations in a scenario where the amount is zero.
2. Check the created CustomerReceivable records.
3. Verify that no record has initialAmount equal to zero.

**Expected result:** No CustomerReceivable with initialAmount zero is created. The flow skips creation or validation prevents zero from being persisted. Receivables with valid non-zero amounts are still created when applicable.

**References:** PDT-2474; Billing run: Credit notes, invoice corrections, VAT base adjustments, Compensations; integration point: Billing run credit notes/corrections/VAT base/compensations.

---

## TC-6 (Positive): VAT base liability or receivable with zero amount is skipped before save

**Objective:** Verify that when the billing run or related flow builds liabilities or receivables per VAT base and the computed amount for a VAT base is zero, the system skips creating that liability or receivable (CustomerLiabilityMapperService and CustomerReceivableService skip zero-amount VAT base entries before saveAll). No record with initialAmount zero is persisted.

**Preconditions:**
1. A billing run (or start-accounting) can be executed that generates liabilities or receivables by VAT base.
2. The input data or scenario yields at least one VAT base with computed amount zero.

**Steps:**
1. Execute the flow that creates liabilities or receivables by VAT base (e.g. BillingRunStartAccountingInvokeService generateLiabilities / generateReceivables) in a scenario where one or more VAT bases have amount zero.
2. Inspect the created CustomerLiability and CustomerReceivable records.
3. Verify that no record has initialAmount zero and that zero-amount VAT base entries were skipped (not persisted).

**Expected result:** Zero-amount VAT base liability or receivable is not persisted. The mapper/service skips such entries before calling saveAll. Only non-zero amounts are stored. Technical reference: CustomerLiabilityMapperService skip zero-amount VAT base liability; CustomerReceivableService skip zero-amount VAT base receivable.

**References:** PDT-2474; CustomerLiabilityMapperService; CustomerReceivableMapperService; CustomerReceivableService; integration point: Billing run start accounting; technical_details from cross_dependency_data.

---

## TC-7 (Positive): Goods order invoice does not persist zero-amount liability or receivable

**Objective:** Verify that when a goods order invoice is processed (GoodsOrderProcessService) and the computed liability or receivable amount is zero, the system does not create a CustomerLiability or CustomerReceivable with initialAmount zero. Either the record is not created or validation prevents persistence.

**Preconditions:**
1. A goods order can be processed that creates liabilities and/or receivables via CustomerLiabilityService and CustomerReceivableService.
2. A scenario exists or can be set up where the computed amount for a liability or receivable is zero (e.g. zero quantity, zero price, or adjustment that nets to zero).

**Steps:**
1. Process a goods order invoice in a scenario where the resulting liability or receivable amount is zero.
2. Check the created CustomerLiability and CustomerReceivable records for this invoice.
3. Verify that no record has initialAmount equal to zero.

**Expected result:** No CustomerLiability or CustomerReceivable with initialAmount zero is created for the goods order invoice. The flow either skips creation for zero amount or the persistence layer rejects zero.

**References:** PDT-2474; Goods order invoice; GoodsOrderProcessService; entry_points from cross_dependency_data.

---

## TC-8 (Positive): Service order invoice does not persist zero-amount liability or receivable

**Objective:** Verify that when a service order invoice is processed (ServiceOrderProcessService) and the computed liability or receivable amount is zero, the system does not create a CustomerLiability or CustomerReceivable with initialAmount zero.

**Preconditions:**
1. A service order can be processed that creates liabilities and/or receivables.
2. A scenario exists or can be set up where the computed amount is zero.

**Steps:**
1. Process a service order invoice in a scenario where the resulting liability or receivable amount is zero.
2. Check the created CustomerLiability and CustomerReceivable records.
3. Verify that no record has initialAmount equal to zero.

**Expected result:** No CustomerLiability or CustomerReceivable with initialAmount zero is created for the service order invoice. The flow skips or validation prevents zero.

**References:** PDT-2474; Service order invoice; ServiceOrderProcessService; entry_points from cross_dependency_data.

---

## TC-9 (Negative): Billing run with invoice totalling zero does not create liability or receivable with amount zero

**Objective:** Verify that when a billing run produces an invoice (or line) with total amount zero and the flow would otherwise create a liability or receivable for it, the system does not persist a CustomerLiability or CustomerReceivable with initialAmount zero. Reporting or downstream processes must not assume one record per invoice when the amount is zero.

**Preconditions:**
1. A billing run can be executed that creates liabilities/receivables per invoice or per line.
2. The billing input is such that at least one invoice (or line) has total amount zero.

**Steps:**
1. Execute the billing run for the zero-total scenario.
2. Query CustomerLiability and CustomerReceivable created for this run.
3. Verify that no record has initialAmount zero; if the run would have created one record per invoice, the zero-total case must be skipped.

**Expected result:** No zero-amount liability or receivable is persisted. The system skips creating a record when the amount is zero. What could break: reporting may assume one record per invoice—test confirms zero-total does not create a record.

**References:** PDT-2474; BillingRunStartAccountingInvokeService; what_could_break: billing run/invoice flows with zero total.

---

## References

- **Jira:** PDT-2474 – Liabilities and receivables shouldn't be generated with amount zero.
- **Liability flows:** Billing run, Invoice (Goods order, Service order, Billing run, request for disconnection, disconnection of power supply), Invoice cancellation, Reversals (invoice, payment, manual liability offsetting), Action, Deposit, Late payment fine, Rescheduling, Manual liability, Payment.
- **Receivable flows:** Billing run (Credit notes, invoice corrections, VAT base adjustments, Compensations), Invoice cancellation, Payments, Reversals (Payment, Late payment fine, Manual liability offsetting, Invoice, Rescheduling), Manual receivable, Deposit.
- **Integration points:** Billing run start accounting, Invoice cancellation, Invoice reversal, Billing run credit notes/corrections/VAT base/compensations; JPA @PrePersist (ZeroAmountValidationListener).
- **What could break:** Flows computing zero amount must throw at persist or skip; billing/invoice zero total skipped; reversal/cancellation zero must be skipped.
