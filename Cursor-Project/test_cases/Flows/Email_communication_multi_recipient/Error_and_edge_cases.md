## Test cases for Error_and_edge_cases – Email communication multi-recipient (PDT-2553)

**Jira:** PDT-2553 (Phoenix Delivery)  
**Type:** Bug  
**Summary:** Validate negative and edge-case behaviour for multi-recipient email communication, including invalid and mixed recipients, unreachable recipients, missing mailbox configuration, per-contact status handling, and regression for logs, activities, and reporting.

**Scope:** CRM email communication and mass mailing error-handling and edge conditions. This file covers how the system behaves when some recipients are invalid or unreachable, when configuration issues occur (e.g. missing mailbox), when a single customer has multiple contacts, and when emails are resent or statuses are updated. The emphasis is on ensuring that errors do not cause per-contact message multiplication and that the system correctly handles partial failures while preserving the single-message-per-batch requirement.

---

## Test data (preconditions)

Shared setup for this file (environment + entities):

- **Environment:** Test (Phoenix Test environment).
- **Customers and contacts:**
  - **Customer D1** with:
    - **Contact D1_valid** – valid email `d1_valid@example.test`.
    - **Contact D1_invalid** – invalid email (e.g. malformed format) `d1_invalid_at_example`.
    - **Contact D1_bounce** – syntactically valid but configured/tested to always bounce or be unreachable `d1_bounce@example.invalid`.
  - **Customer D2** with:
    - **Contact D2_valid** – valid email `d2_valid@example.test`.
- **Mailbox configuration variants:**
  - A correctly configured **Mailbox_Active** for normal sending.
  - An intentionally misconfigured **Mailbox_Invalid** (e.g. disabled SMTP or wrong credentials) used only for negative tests.
- **Monitoring:**
  - Access to external email/mass communication logs, including bounce/error reports where applicable.
  - Access to Phoenix logs for EmailCommunicationSenderService and EmailSenderServiceInterface.
- **Permissions:**
  - Test user can select specific mailboxes, define recipients, and resend or inspect communication status via UI/API.

---

## TC-1 (Negative): Mixed valid and invalid recipient emails – one email sent, clear per-contact status

**Description:** Verify that when sending to a mix of syntactically valid and invalid email addresses in a single communication, the system does not attempt to send multiple emails, handles invalid addresses correctly (validation or per-contact failure), and sends only one email message to the valid recipients (if validation is not strict), with clear per-contact statuses.

**Preconditions:**
1. Test data above is present.
2. Email validation rules for email communication are known (either reject malformed addresses upfront or allow them to be attempted and fail).

**Test steps:**
1. Create a new email communication for **Customer D1** selecting:
   - **Contact D1_valid**.
   - **Contact D1_invalid**.
2. Use **Mailbox_Active** as the sender and set subject `PDT-2553_mixed_valid_invalid_{timestamp}`.
3. Attempt to send the email communication.
4. Observe behaviour:
   - If the system performs strict validation, capture any validation error for D1_invalid.
   - If the system allows the send, proceed to asynchronous processing completion.
5. Inspect Phoenix:
   - Check EmailCommunicationCustomerContact entries and their statuses for D1_valid and D1_invalid.
   - Confirm any validation or failure messages are recorded for D1_invalid.
6. Check external email logs for messages with the test subject and inspect recipients.

**Expected test case results:**  
- If strict validation is applied:
  - The system rejects the communication creation or sending step, citing invalid email format for D1_invalid.
  - No outbound email is created; both contacts are not sent to.  
- If per-contact error handling is applied:
  - Exactly **one** email message is sent to **D1_valid** only, not two messages.
  - EmailCommunicationCustomerContact for D1_valid is SENT/SUCCESS; for D1_invalid is FAILED/INVALID with a clear error reason.  
- In both models, there are no duplicated messages per contact, and no more than one outbound message for the entire communication.  
- Logs and activities reflect per-contact status without conflicting or duplicated email sends.

**Actual result (if bug):** If the system sends separate emails or attempts multiple sends per contact due to mixed validity, that behaviour must be recorded.

**References:**  
- Email validation rules for email communication.  
- Jira: PDT-2553.

---

## TC-2 (Negative): Unreachable recipient (bounce) – no resend multiplication, correct per-contact failure

**Description:** Verify that when a recipient’s address is syntactically valid but the mailbox is unreachable and causes bounces, the system records a failure for that contact without continuously resending or multiplying messages and without impacting other recipients.

**Preconditions:**
1. Test data above is present.
2. External email provider or test configuration can simulate bounces for `d1_bounce@example.invalid`.

**Test steps:**
1. Create an email communication for **Customer D1** selecting:
   - **Contact D1_valid**.
   - **Contact D1_bounce**.
2. Use **Mailbox_Active** as sender and subject `PDT-2553_unreachable_recipient_{timestamp}`.
3. Send the email communication and wait for processing completion and bounce handling (if asynchronous).
4. Inspect Phoenix:
   - Check EmailCommunicationCustomerContact statuses for D1_valid and D1_bounce.
   - Look for any retry indicators or error messages associated with D1_bounce.
5. Inspect external email logs:
   - Confirm that at most **one** send attempt was made per recipient for this communication (unless configured retries exist with clear limits).
   - Verify that D1_bounce generates a bounce or error from the provider.
6. If configured retries exist, verify they are limited and do not create an unbounded number of emails or duplicate communication entries.

**Expected test case results:**  
- For D1_valid:
  - Exactly one successful send, with success status and no excessive retries.  
- For D1_bounce:
  - One or a limited number of send attempts according to retry policy, followed by a FAILED/BOUNCED status in Phoenix.  
- Total number of outbound messages remains limited and aligned with retry policy, not multiplied per failure.  
- The system does not consider the communication as fully failed if at least one recipient succeeded; per-contact statuses reflect mixed outcome.  
- Logs clearly distinguish between successful and bounced recipients without duplicate underlying email messages beyond defined retries.

**Actual result (if bug):** If bounces cause uncontrolled retry loops or repeated messages, this must be logged with counts and timestamps.

**References:**  
- EmailSenderServiceInterface error and retry handling.  
- Provider bounce-handling flow.  
- Jira: PDT-2553.

---

## TC-3 (Negative): Missing or misconfigured mailbox – communication not sent and no recipient-specific duplication

**Description:** Verify that when the selected mailbox or outbound email configuration is missing or misconfigured, the system blocks sending the communication with a clear error and does not produce any outbound emails or per-contact duplicated attempts.

**Preconditions:**
1. **Mailbox_Invalid** exists and is deliberately misconfigured or disabled for sending.
2. User can select **Mailbox_Invalid** in the email communication creation form (or via API).

**Test steps:**
1. Create a new email communication for **Customer D2** selecting **Contact D2_valid** as the only recipient.
2. Set subject `PDT-2553_invalid_mailbox_{timestamp}` and choose **Mailbox_Invalid** as the sender.
3. Attempt to send the email communication.
4. Observe:
   - Immediate validation or configuration error in UI/API, or
   - Failure during asynchronous processing with clear logs.
5. Inspect Phoenix:
   - Verify the status of the email communication entity (e.g. FAILED_CONFIGURATION or similar).
   - Check that there are no EmailCommunicationCustomerContact entries with success status for this communication.
6. Check external email logs for any messages with this subject.

**Expected test case results:**  
- The system prevents sending via a misconfigured or disabled mailbox and returns a clear error to the user.  
- No outbound email messages exist in the external email logs for this subject.  
- Phoenix records the failure at the communication level (and optionally per-contact) but without any partial successes.  
- There is no repeated per-contact sending attempt because the configuration is invalid globally for the communication.

**Actual result (if bug):** If the system attempts sending anyway and creates messages or inconsistent statuses, that must be documented.

**References:**  
- Mailbox configuration validation logic.  
- Jira: PDT-2553.

---

## TC-4 (Positive): Single customer with multiple contacts – per-contact statuses with one email and clear activity mapping

**Description:** Verify that when a single customer has multiple contacts included in one email communication, the system still sends one email with multiple recipients, maintains per-contact statuses, and maps activities/tasks correctly without duplication of emails.

**Preconditions:**
1. Test data above is present.
2. **Customer D1** has multiple contacts (valid and possibly invalid/bounce ones) available for selection.

**Test steps:**
1. Create an email communication for **Customer D1** selecting:
   - **Contact D1_valid**.
   - Optionally, one or more other valid contacts under the same customer (if configured).
2. Use **Mailbox_Active** and subject `PDT-2553_single_customer_multi_contacts_{timestamp}`.
3. Send the email communication and wait for processing completion.
4. Inspect Phoenix:
   - Confirm that exactly one communication exists for this operation with multiple EmailCommunicationCustomerContact entries for customer D1.
   - Verify per-contact statuses are correct (success for valid addresses, failures only where appropriate).
   - Review related activities/tasks and ensure that:
     - They are properly linked to Customer D1.
     - They indicate multiple contacts without duplicating the underlying email entry.
5. Check external email logs:
   - Confirm exactly one outbound email message was sent for this operation.
   - Verify that the message’s recipient headers include all selected contacts’ email addresses.

**Expected test case results:**  
- A single email is sent for the communication, even though multiple contacts under the same customer are selected.  
- Phoenix keeps clear per-contact statuses while referring to a single underlying communication and email message.  
- Activities/tasks are not duplicated per contact in a way that implies multiple separate emails; instead they are correctly associated with one communication touching multiple contacts.  
- There is no per-contact send loop or error.

**Actual result (if bug):** If the system still sends one email per contact under the same customer, this must be noted with evidence from logs.

**References:**  
- EmailCommunicationCustomerContact and EmailCommunicationCustomer relations.  
- Activities/tasks creation rules for email communication.  
- Jira: PDT-2553.

---

## TC-5 (Negative): Resend after partial failure – no additional duplication for previously successful recipients

**Description:** Verify that when an email communication with multiple recipients has mixed outcomes (some success, some failure) and the user resends the communication, only the failed recipients are attempted again, and the system does not duplicate successful emails or create multiple communications.

**Preconditions:**
1. At least one email communication exists (from **TC-1** or **TC-2**) where:
   - Some recipients have success status (e.g. D1_valid).
   - Some recipients have failure status (e.g. D1_bounce or D1_invalid).
2. A resend operation is supported (UI or `GET /email-communication/{id}/resend`).

**Test steps:**
1. Identify such a communication with mixed outcomes.
2. Trigger the resend operation for this communication.
3. Wait for resend processing to complete.
4. Inspect Phoenix:
   - Verify that the communication and per-contact statuses now show:
     - Previously successful recipients remain successful without new send attempts (or at most are clearly marked as not retried).
     - Previously failed recipients show new attempts and possibly updated statuses.  
   - Ensure that no duplicate email communication entity is created unless explicitly expected by design (and if so, this is documented).
5. Inspect external email logs:
   - Count how many new messages appear as a result of the resend.
   - Verify that the set of recipient email addresses for the new messages corresponds only to previously failed recipients (subject to batching rules).

**Expected test case results:**  
- Resend attempts target only failed recipients; successful recipients are not re-sent to.  
- The number of new outbound email messages is aligned with the number of previously failed recipients and batching rules, not all recipients.  
- Phoenix’s tracking clearly differentiates initial and resend attempts without duplicating communication entities or per-contact statuses for successful recipients.  
- No regression to per-contact sending behaviour occurs during resend.

**Actual result (if bug):** If successful recipients are resent or if multiple emails are generated for the same contact across resend attempts beyond policy, this deviation must be documented.

**References:**  
- Email resend logic and per-contact retry policies.  
- Jira: PDT-2553.

---

## References

- **Jira:** PDT-2553 – When send email to more than one recipient, send one email with multiple recipients not many separate emails.  
- **Services:** EmailCommunicationSenderService, EmailSenderServiceInterface, MassEmailCommunicationProcessingService.  
- **Integration points:** External email client logs, activities/tasks, and reporting dashboards that rely on correct per-contact and per-communication status.

