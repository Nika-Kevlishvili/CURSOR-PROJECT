# Email Communication – Status Polling, TaskId, and Regression (PDT-2553)

**Jira:** PDT-2553 (Phoenix Delivery – Customer Feedback; status as per Jira)  
**Type:** Customer Feedback  
**Summary:** Changing from N sends to one multi-recipient send affects **per-contact taskId**, status polling (`fetchTaskStatus` / `fetchContactStatuses`), and customer-level aggregation. This document covers those risks called out in cross-dependency analysis.

**Scope:** After a single message replaces per-contact sends, the mass communication platform may return **one** task for the whole message instead of one task per contact. Tests ensure UI, jobs, and database fields (`EmailCommunicationCustomerContact.taskId`, `EmailCommunicationContactStatus`) stay consistent with the new contract.

---

## Test data (preconditions)

- **Environment:** Test; sending enabled; ability to simulate mass comm responses (success, failure, pending).
- **Email communication:** Multiple contacts under one customer; optional attachments.
- **Status job:** If `EmailCommunicationStatusUpdateJobService` (or equivalent) polls task IDs per contact, identify how it behaves when task IDs are shared or absent per contact.

---

## TC-1 (Positive): Multi-recipient success — all contacts resolve to consistent success state

**Objective:** When one outbound message succeeds, every intended contact row reflects success (or a documented aggregated rule), and the customer header status is `SENT_SUCCESSFULLY` where applicable.

**Preconditions:**
1. Two or more contacts; mass comm returns success once for the batched message.

**Steps:**
1. Send and wait for processing (and polling job if used).
2. Inspect each `EmailCommunicationCustomerContact` status and `taskId` (if still populated).
3. Inspect `EmailCommunicationCustomer` status.

**Expected result:** No contact left in a stale IN_PROGRESS state incorrectly. Customer status matches “at least one success” rule or updated rule for single-task semantics.

**References:** `sendBatch` customer status logic (`isAnySend`); `sendSingleInternal` taskId assignment per contact (inner contact loop and `sendEmail` response handling in `EmailCommunicationSenderService`).

---

## TC-2 (Negative): Partial delivery / bounce — behaviour defined for single message

**Objective:** With one message to multiple recipients, partial failure (one address bounces) must match product rules: either fail whole message or document per-recipient outcome if the provider supports it.

**Preconditions:**
1. Mass comm stub can return partial failure or aggregate failure for multi-To sends.

**Steps:**
1. Trigger send with configured partial failure.
2. Observe contact statuses, error codes, and retry behaviour.

**Expected result:** Documented behaviour only—no inconsistent DB state (e.g. one SUCCESS and one ERROR without an explained rule). Align with Confluence/BA once defined.

**References:** Cross-dependency `what_could_break`: “Partial failure semantics”.

---

## TC-3 (Positive): Status polling — one task ID mapped to multiple contacts

**Objective:** If the implementation stores one `taskId` on all contacts or on a parent row, the polling job must still update statuses correctly without duplicate processing or missed updates.

**Preconditions:**
1. Feature implementation for PDT-2553 completed; task ID strategy documented.

**Steps:**
1. After send, run or await status update job.
2. Verify final statuses for all contacts and idempotency (no duplicate notifications).

**Expected result:** All contacts reach terminal states as designed; no infinite polling.

**References:** `EmailSenderServiceInterface.fetchTaskStatus`, `fetchContactStatuses`; status update services (see cross_dependencies JSON).

---

## TC-4 (Negative): Privacy — recipients must not see each other unless product requires To

**Objective:** If policy requires hidden recipients, the implementation must use Bcc (or equivalent), not To, for all addresses.

**Preconditions:**
1. Product requirement documented (customer feedback may imply visible To—confirm).

**Steps:**
1. Send to multiple internal test mailboxes.
2. Inspect received headers (To vs Bcc).

**Expected result:** Matches legal/product requirement; no accidental exposure.

**References:** Cross-dependency `what_could_break`: “Privacy / visibility”.

---

## TC-5 (Positive): MockEmailSenderService implements any new interface

**Objective:** If `EmailSenderServiceInterface` gains multi-recipient methods or changes signature, test profiles using `MockEmailSenderService` must compile and behave in tests.

**Preconditions:**
1. Local or CI test suite that wires mock email sender.

**Steps:**
1. Run unit/integration tests touching CRM email send paths after interface change.

**Expected result:** Green build; mock records multi-recipient calls if asserted.

**References:** `MockEmailSenderService.java`; cross-dependency `what_could_break`.

---

## References

- **Jira:** PDT-2553 (regression scope).
- **Cross-dependency artefact:** `Cursor-Project/cross_dependencies/2026-03-30_PDT-2553-email-multi-recipient.json`.
- **Code:** `EmailCommunicationSenderService.java`; `EmailSenderServiceInterface.java`.
