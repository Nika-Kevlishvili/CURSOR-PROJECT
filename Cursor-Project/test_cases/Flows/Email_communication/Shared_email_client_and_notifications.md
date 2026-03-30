# Email Communication – Shared Email Client and Other Flows (PDT-2553)

**Jira:** PDT-2553 (Phoenix Delivery – Customer Feedback)  
**Type:** Customer Feedback / regression  
**Summary:** Changes to `EmailSenderServiceInterface` or batching behaviour for CRM email communication can affect **other** callers that use the same email client (for example notifications or contract-related emails). This document captures regression checks for those shared integration points.

**Scope:** PDT-2553 focuses on CRM email communication and multiple recipients. The same email abstraction is used elsewhere. After the change, those flows must still send mail correctly, use valid recipient parameters, and must not break compilation or runtime wiring (including mock implementations in tests).

---

## Test data (preconditions)

- **Environment:** Test or Dev; known builds before and after the PDT-2553 change for comparison if needed.
- **Access:** Ability to trigger or observe at least one **notification** email path and one **contract / termination / objection** path that uses the shared email client (as applicable in your product).
- **Monitoring:** Logs, mocks, or test assertions that show `EmailSenderServiceInterface` (or implementing bean) invocations.

---

## TC-1 (Positive): Notification email sends successfully with expected single-recipient contract

**Objective:** Verify that a typical notification email (for example a system or user-triggered notification that uses `EmailSenderServiceInterface`) still succeeds when the implementation for PDT-2553 introduces multi-recipient support for CRM only. The shared caller should continue to pass one recipient and receive a normal response.

**Preconditions:**
1. A notification scenario exists that sends one email to one address through the same email client stack used by CRM.
2. Sending is enabled in the test environment.

**Steps:**
1. Trigger the notification (for example via the UI, API, or scheduled job, depending on the product).
2. Confirm one outbound send (or equivalent) with the expected recipient and no error in application logs.
3. If tests exist that mock the email client, run them and confirm they still pass.

**Expected result:** Notification is delivered or mocked successfully; no regression from CRM multi-recipient refactoring (wrong method overload, null handling, or bean wiring).

**References:** Cross-dependency `shared`: NotificationService; `EmailSenderServiceInterface`.

---

## TC-2 (Negative): Invalid or empty recipient on a non-CRM path returns a clear failure and does not corrupt state

**Objective:** If a non-CRM caller passes an invalid or empty recipient, the system must reject or handle the error safely (validation or domain error), without partially updating unrelated CRM email communication rows.

**Preconditions:**
1. A test harness or API can invoke the shared email path with an invalid recipient (as allowed by security rules in Test).

**Steps:**
1. Invoke the operation with an empty, malformed, or disallowed recipient address (according to product rules).
2. Observe HTTP or UI error handling and database state for unrelated entities.

**Expected result:** Clear failure; no silent success; CRM email communication records for other tenants or IDs are unchanged.

**References:** Validation on email send paths; shared client error handling.

---

## TC-3 (Positive): Contract termination or objection email flow still completes after interface alignment

**Objective:** Flows such as termination or objection that today send email with a **single** recipient per call must keep working if the interface adds optional multi-recipient parameters or changes batching internally. The flow should complete and produce the expected document or status.

**Preconditions:**
1. A reproducible termination or objection email step exists in Test (or documented API).
2. PDT-2553 implementation is merged on the branch under test.

**Steps:**
1. Execute the flow end-to-end (create prerequisites, trigger send).
2. Verify email dispatch count and recipient match the business rules for that flow (typically one message to one address).
3. Verify downstream status (for example communication recorded, task completed) matches pre-change behaviour.

**Expected result:** Same functional outcome as before PDT-2553 for these flows; no duplicate sends unless required by design.

**References:** Cross-dependency `shared`: “Termination / objection flows… single recipient pattern”.

---

## References

- **Jira:** [PDT-2553](https://oppa-support.atlassian.net/browse/PDT-2553) – multiple recipients as one message (CRM scope); regression for shared client.
- **Cross-dependency:** `Cursor-Project/cross_dependencies/2026-03-30_PDT-2553-email-multi-recipient.json` (`shared` section).
- **Code (read-only):** `EmailSenderServiceInterface`, `EmailSenderService`, CRM `EmailCommunicationSenderService`.
