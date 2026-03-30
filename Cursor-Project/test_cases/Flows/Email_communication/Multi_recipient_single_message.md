# Email Communication – Multiple Recipients as One Message (PDT-2553)

**Jira:** PDT-2553 (Phoenix Delivery / customer feedback)  
**Type:** Task / Customer feedback  
**Summary:** When an email communication is sent to more than one contact, the system must deliver **one** outbound message with all intended recipients in the **To** field (single mass-communication request), not one separate send per recipient.

**Scope:** This document covers CRM email communication sending where one customer record has multiple contact email addresses. The expected behaviour is a single logical email visible to recipients (shared To visibility per product policy). The previous behaviour (as of current code in `EmailCommunicationSenderService`) loops contacts and calls `emailSenderService.sendEmail` once per address—see References. Tests here validate the **intended** post-change behaviour and baseline single-recipient behaviour.

---

## Test data (preconditions)

- **Environment:** Test (or Dev); `send.email.enabled=true` unless the scenario explicitly tests disabled sending.
- **Email mailbox:** A configured sender mailbox exists and is linked to the email communication.
- **Email communication:** An active `EmailCommunication` with subject, body, and optional attachments; linked `EmailCommunicationCustomer` with **two or more** distinct `EmailCommunicationCustomerContact` rows (valid email addresses).
- **Mass communication layer:** Access to logs or a test stub showing how many `sendEmail` invocations (or outbound requests) occur per send operation (e.g. mock, API trace).

---

## TC-1 (Positive): Two recipients — one outbound message with both addresses in To

**Objective:** Verify that for two contacts on the same communication, the integration sends **one** email request that includes both recipients in the To list (or equivalent single-message contract), not two sequential single-recipient calls.

**Preconditions:**
1. An email communication exists with one customer and exactly two active contacts with different email addresses.
2. Sending is enabled and the mass communication service accepts multi-recipient requests for this flow.

**Steps:**
1. Trigger send for this communication (e.g. via API that creates/sends email communication, or event path that invokes batch/single mass send—whichever applies after PDT-2553).
2. Capture mass communication client activity (logs, mock call count, or network trace).
3. Receive or inspect the composed message (test mailbox or stub response) and confirm both recipients appear on the **same** message To line (or product-defined equivalent).

**Expected result:** Exactly **one** outbound send operation is performed for the two contacts together. Both recipients appear on one message. No duplicate subject/body emails sent separately to each address for this communication.

**Actual result (if bug):** Two separate `sendEmail` (or equivalent) calls occur, or each recipient receives an isolated copy without the other on To—matching pre-fix behaviour.

**References:** PDT-2553; `EmailCommunicationSenderService` batch and single-mass loops (pre-change pattern).

---

## TC-2 (Positive): Single recipient — unchanged behaviour

**Objective:** Ensure that when only one contact exists, behaviour matches today: one send, success/error status and task mapping still work.

**Preconditions:**
1. Email communication with one customer and exactly one contact.

**Steps:**
1. Trigger send.
2. Verify one outbound operation and correct contact status (e.g. SUCCESS when API returns success).

**Expected result:** One send; contact status and customer aggregate status remain consistent with existing rules.

**References:** Single-recipient path in `sendBatch`, `sendSingleInternal`, `sendSingleMass`.

---

## TC-3 (Negative): Sending disabled — no outbound calls

**Objective:** When `send.email.enabled` is false, the system must not call the mass communication API for batch or mass single flows.

**Preconditions:**
1. Configuration sets sending disabled.
2. Valid multi-recipient communication ready to send.

**Steps:**
1. Trigger send.
2. Verify logs indicate skip and zero outbound email API calls.

**Expected result:** No `sendEmail` (or client) invocations; no erroneous success flags on contacts.

**References:** Early return in `sendBatch`, `sendSingleInternal`, `sendSingleMass` when disabled.

---

## TC-4 (Negative): Invalid or duplicate recipient data — validation before send

**Objective:** If the product validates email addresses before send, invalid duplicates or malformed addresses must be rejected or filtered without sending a partial multi-recipient message incorrectly.

**Preconditions:**
1. Communication with two contacts where one address is invalid (malformed) or policy forbids duplicate To—adjust to match Phoenix validation rules.

**Steps:**
1. Attempt send.
2. Observe API/UI errors and database contact statuses.

**Expected result:** Clear validation outcome; no silent send with wrong recipient set. (Align exact message with Phoenix validators.)

**References:** Email communication creation/update validation in `EmailCommunicationService`.

---

## TC-5 (Positive): Three or more contacts — still exactly one multi-recipient send per customer

**Objective:** Verify that the fix scales beyond two recipients: for one customer with many contact rows, the mass communication client is invoked **once** with all addresses together (or the documented equivalent), not once per contact.

**Preconditions:**
1. One `EmailCommunicationCustomer` with at least three active contacts with distinct valid email addresses.
2. Sending enabled; mass comm accepts multi-To for this flow.

**Steps:**
1. Trigger send (UI, API, or event path as used in regression).
2. Count outbound client calls for that customer (logs, mock, or trace).
3. Inspect the received message at test mailboxes: all intended addresses must appear on **one** logical message (To/Cc/Bcc per product; see `Status_polling_and_task_mapping.md` for privacy).

**Expected result:** One send operation per customer for the whole contact set (not three or more separate sends). Body and attachments match a single message.

**References:** `EmailCommunicationSenderService.sendBatch` / `sendSingleInternal` / `sendSingleMass` — contact loops today at ```109:149:Cursor-Project/Phoenix/phoenix-core-lib/src/main/java/bg/energo/phoenix/service/crm/emailCommunication/EmailCommunicationSenderService.java``` and equivalent blocks; post-PDT-2553 implementation should collapse inner per-contact `sendEmail` calls for the same customer into one batched request.

---

## TC-6 (Positive): Batch `sendBatch` with multiple customers — one multi-recipient message per customer, not one global email

**Objective:** When `EmailSendModel` contains several `EmailSendContactModel` entries (several customers in one batch job), each customer must get **its own** outbound message with **that customer’s** contacts only. Recipients from customer A must not appear on the same mass-comm request as customer B.

**Preconditions:**
1. Batch payload: at least two `EmailSendContactModel` rows, each with two contacts.
2. Sending enabled.

**Steps:**
1. Invoke the batch send path used in production (e.g. mass worker calling `sendBatch`).
2. For each customer, assert exactly **one** multi-recipient send and that the To list contains only that customer’s contact addresses.

**Expected result:** Two outbound operations total in this setup (one per customer), each with two recipients—not one email with four To addresses, and not four single-recipient sends per customer.

**References:** Outer loop over `emailSendContactModels` at ```96:166:Cursor-Project/Phoenix/phoenix-core-lib/src/main/java/bg/energo/phoenix/service/crm/emailCommunication/EmailCommunicationSenderService.java```.

---

## TC-7 (Positive): After-commit event path (`EmailCommunicationSendEvent`) — same single-message semantics

**Objective:** Sending triggered via `handleEmailCommunicationSendEvent` → `sendSingleInternal` must apply the same multi-recipient behaviour as other entry points (one message for all contacts of the customer).

**Preconditions:**
1. Flow that publishes `EmailCommunicationSendEvent` after transaction commit (e.g. create/update communication that triggers send).
2. Communication with two or more contacts.

**Steps:**
1. Complete the product flow that triggers the event.
2. Observe email client invocations and received messages.

**Expected result:** One multi-recipient send for the customer; parity with TC-1 and TC-5.

**References:** ```196:201:Cursor-Project/Phoenix/phoenix-core-lib/src/main/java/bg/energo/phoenix/service/crm/emailCommunication/EmailCommunicationSenderService.java``` (`handleEmailCommunicationSendEvent`); `sendSingleInternal` contact loop at ```222:248:Cursor-Project/Phoenix/phoenix-core-lib/src/main/java/bg/energo/phoenix/service/crm/emailCommunication/EmailCommunicationSenderService.java```.

---

## TC-8 (Positive): `sendSingleMass` path — multiple contacts as one message

**Objective:** The mass “single customer” API used by `sendSingleMass` must not regress: two or more contacts for the given `emailCommunicationCustomerId` result in **one** batched outbound call, matching `sendBatch` / event behaviour.

**Preconditions:**
1. `EmailCommunicationCustomer` with two or more contacts; valid mailbox and body (customer-level body where applicable).

**Steps:**
1. Call or trigger `sendSingleMass(emailCommunicationCustomerId)` per product integration.
2. Verify client call count and recipient headers.

**Expected result:** One multi-recipient send; customer and communication statuses updated per business rules.

**References:** Contact loop in `sendSingleMass` at ```298:346:Cursor-Project/Phoenix/phoenix-core-lib/src/main/java/bg/energo/phoenix/service/crm/emailCommunication/EmailCommunicationSenderService.java```.

---

## TC-9 (Positive): Attachments — one multi-recipient message carries the same attachment set

**Objective:** When the communication includes attachments, the **single** outbound message must include all attachments for every recipient; recipients must not receive different attachment sets for the same send.

**Preconditions:**
1. Email communication (or customer-level attachments for `sendSingleMass`) with at least one file attachment.
2. Two or more contacts.

**Steps:**
1. Trigger send.
2. Download or inspect messages at each recipient mailbox (or stub).

**Expected result:** One send operation; each recipient’s copy lists the same attachments (subject to provider behaviour). No per-contact attachment fetch that implies separate sends unless explicitly out of scope.

**References:** `fetchAttachmentsForEmail`, `fetchAttachmentsForEmailCustomer` in `EmailCommunicationSenderService`.

---

## TC-10 (Negative): Duplicate contact rows with the same email address

**Objective:** If two `EmailCommunicationCustomerContact` rows carry the **identical** address, the product must not cause duplicate deliveries or invalid mass-comm payloads (e.g. duplicate To rejected by provider). Expected behaviour should be documented: deduplicate To, reject at validation, or merge rows.

**Preconditions:**
1. Two contacts with the same `emailAddress` string (if the UI/API allows creating such data).

**Steps:**
1. Attempt send.
2. Observe validation errors, deduplicated To in the outbound request, or provider errors.

**Expected result:** Predictable outcome only—no silent double send to the same inbox, no unhandled provider 400 from duplicate recipients.

**References:** Contact iteration in all three send paths in `EmailCommunicationSenderService`.

---

## TC-11 (Negative): Single batched send — one undeliverable address may fail the entire message (all-or-nothing)

**Objective:** Jira discussion on [PDT-2553](https://oppa-support.atlassian.net/browse/PDT-2553) raises the risk that with **one** SMTP/API message to N recipients, a failure on one address might prevent delivery to **all**. Verify the **agreed** product behaviour with BA/engineering: either acceptable (documented), or mitigated (e.g. validation up front, provider capabilities, or fallback strategy).

**Preconditions:**
1. Mass comm stub or sandbox can simulate “message rejected” or hard bounce for one recipient in a multi-To send.
2. BA sign-off on expected user-visible outcome.

**Steps:**
1. Send to one good and one systematically bad address (per test harness).
2. Record contact-level statuses, customer status, user messaging, and whether anyone received the email.

**Expected result:** Behaviour matches signed-off design—either all fail together, partial success with documented mapping, or split-send fallback. No undefined mixed DB state (see also `Status_polling_and_task_mapping.md` TC-2).

**References:** Jira comment thread on PDT-2553 (risk: whole send fails if one address fails).

---

## References

- **Jira:** [PDT-2553](https://oppa-support.atlassian.net/browse/PDT-2553) – multi-recipient single message (Phoenix Delivery); comments note all-or-nothing delivery risk for one message to N addresses.
- **Code (current per-contact loop — pre/post change verification):** `Cursor-Project/Phoenix/phoenix-core-lib/src/main/java/bg/energo/phoenix/service/crm/emailCommunication/EmailCommunicationSenderService.java` — `sendBatch` inner loop ```109:149:Cursor-Project/Phoenix/phoenix-core-lib/src/main/java/bg/energo/phoenix/service/crm/emailCommunication/EmailCommunicationSenderService.java```, `sendSingleInternal` ```223:248:Cursor-Project/Phoenix/phoenix-core-lib/src/main/java/bg/energo/phoenix/service/crm/emailCommunication/EmailCommunicationSenderService.java```, `sendSingleMass` ```304:346:Cursor-Project/Phoenix/phoenix-core-lib/src/main/java/bg/energo/phoenix/service/crm/emailCommunication/EmailCommunicationSenderService.java```: each calls `emailSenderService.sendEmail` once per contact **today**; PDT-2553 targets one call (or equivalent) with multiple recipients per customer.
- **Interface:** `EmailSenderServiceInterface.sendEmail(String recipientEmailAddress, ...)` — single recipient parameter in current API; implementation change or overload expected for multi-recipient batching.
- **Confluence / search:** No dedicated Confluence page returned in Atlassian search for this exact topic; related mass mailing context: Jira REG-1079 / PDT-2365 (mass mailing send and statuses).
- **Cross-dependency:** `Cursor-Project/cross_dependencies/2026-03-30_PDT-2553-email-multi-recipient.json`.
