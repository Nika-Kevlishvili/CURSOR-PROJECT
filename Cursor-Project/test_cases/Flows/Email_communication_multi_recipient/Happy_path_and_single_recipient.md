## Test cases for Happy_path_and_single_recipient – Email communication multi-recipient (PDT-2553)

**Jira:** PDT-2553 (Phoenix Delivery)  
**Type:** Bug  
**Summary:** Validate correct email sending behaviour and tracking when sending to a single or multiple recipients using CRM email communication flows.

**Scope:** CRM email communication flow for sending messages to customer contacts from Phoenix. This file focuses on the baseline behaviour for a single recipient and core positive paths for multiple recipients (two and many recipients) where the system must send exactly one email with multiple recipients instead of separate emails per contact. It also covers basic resend behaviour for successfully sent communications.

---

## Test data (preconditions)

Shared setup for this file (environment + entities):

- **Environment:** Test (Phoenix Test environment with working email integration and mass communication client configured).
- **Customer(s):**
  - At least one **Customer A** with status Active.
  - At least one **Customer B** with status Active (for multi-customer scenarios).
- **Contacts:**
  - **Customer A** has:
    - **Contact A1** with a valid, unique email address `contactA1@example.test`.
    - **Contact A2** with a valid, unique email address `contactA2@example.test`.
  - **Customer B** has:
    - **Contact B1** with a valid, unique email address `contactB1@example.test`.
- **Email communication template and configuration:**
  - A valid email template (subject and body) exists or free-form composition is allowed.
  - Outbound email sender configuration is active (mailbox, SMTP/mass_comm client, etc.).
  - Activities/tasks and reporting for email communication are enabled per standard configuration.
- **Technical logging/monitoring:**
  - Access to the external email client logs/mailbox (e.g. mass_comm or SMTP sink) that can show:
    - Number of outbound messages created.
    - The `To` / `Cc` / `Bcc` headers for each message.
    - Message identifiers for correlation.
- **Permissions:**
  - Test user has permission to:
    - Create email communication from the CRM UI or via `POST /email-communication`.
    - Select multiple customer contacts as recipients.
    - Trigger resend via `GET /email-communication/{id}/resend` (if applicable).

---

## TC-1 (Positive): Send email to a single recipient – baseline behaviour

**Description:** Verify that when the user sends an email to exactly one recipient, the system sends one email addressed only to that recipient and sets the correct communication and contact statuses.

**Preconditions:**
1. Test data above is present.
2. User is authenticated and can access the email communication creation screen or API.
3. No previous email communication exists for **Contact A1** with the same subject and content within the current test run (to simplify verification).

**Test steps:**
1. Create a new email communication (UI or `POST /email-communication`) for **Customer A** and select **Contact A1** as the only recipient.
2. Set a unique subject such as `PDT-2553_single_recipient_{timestamp}` and a simple body text, and choose the active mailbox/sender.
3. Submit the email communication for sending.
4. Wait for the asynchronous send processing to complete (poll status via UI or API until it reaches a final state such as SENT or FAILED).
5. In the Phoenix UI or via the email communication API, verify the stored email communication record:
   - Single recipient linked (Contact A1).
   - Email communication status is **SENT** (or equivalent success state).
   - EmailCommunicationCustomerContact status for Contact A1 is **SENT/SUCCESS**.
6. In the external email client logs/mailbox, search by subject `PDT-2553_single_recipient_{timestamp}` and retrieve the outbound message(s).

**Expected test case results:**  
- Exactly **one** outbound email message exists in the external client for the given subject.  
- The single message has the `To` (or main recipient) header containing only `contactA1@example.test`.  
- No additional email messages are present for the same subject.  
- Phoenix email communication record shows one EmailCommunicationCustomerContact entry for Contact A1 with a success status.  
- The related activity/task for the email communication is created and linked to **Customer A** and Contact A1 as per CRM design.

**Actual result (if bug):** Historically the bug is related to multiple recipients; single recipient is usually correct. Any deviation (e.g. multiple messages for one recipient) must be recorded here.

**References:**  
- Endpoint: `POST /email-communication`.  
- Endpoint: `GET /email-communication/{id}`.  
- Endpoint: `GET /email-communication/{id}/resend` (for resend, used in later cases).  
- Jira: PDT-2553.

---

## TC-2 (Positive): Send email to two recipients – one email with multiple recipients

**Description:** Verify that when the user selects two contacts of the same customer as recipients, the system sends exactly one email whose recipient list contains both contacts (e.g. To or To+Cc), instead of sending two separate emails.

**Preconditions:**
1. Test data above is present.
2. User is authenticated and can create email communication for **Customer A**.
3. Both **Contact A1** and **Contact A2** have valid email addresses, are active, and not blocked from email communication.
4. There is no prior email communication with subject `PDT-2553_two_recipients_{timestamp}` in this environment.

**Test steps:**
1. Start composing a new email communication for **Customer A** and select both **Contact A1** and **Contact A2** as recipients.
2. Set a unique subject `PDT-2553_two_recipients_{timestamp}` and body text that clearly identifies this test case.
3. Submit the email communication to be sent.
4. Wait until the EmailCommunicationSenderService (and any async jobs) process the communication and the Phoenix email communication status reaches its final state.
5. Inspect the email communication record in Phoenix:
   - One email communication entity associated with **Customer A**.
   - Two EmailCommunicationCustomerContact entries (A1 and A2) with status **SENT/SUCCESS**.
6. Query or open the external email client logs/mailbox and search for messages with subject `PDT-2553_two_recipients_{timestamp}`.
7. Count how many messages exist and inspect the email headers for the unique message(s).

**Expected test case results:**  
- Exactly **one** outbound email message is created for this communication.  
- The message has a recipient list including **both** `contactA1@example.test` and `contactA2@example.test`, either in `To`, or split between `To` and `Cc`/`Bcc` per system design.  
- There are **no** duplicate or extra messages with this subject.  
- Phoenix’s email communication record shows one EmailCommunicationCustomer entity with two related EmailCommunicationCustomerContact entries, each with success status.  
- Activities/tasks and reporting entries show one communication action referencing both contacts (or two linked to a single underlying email, depending on design) without duplication of the actual email message.

**Actual result (if bug):** Historically, the bug causes **two separate emails** to be sent (one per contact). If this still happens, logs should show two messages with identical subject and body but different single recipients.

**References:**  
- EmailCommunicationSenderService.sendBatch / sendSingleMass (code-level context).  
- EmailSenderServiceInterface integration.  
- Jira: PDT-2553.

---

## TC-3 (Positive): Send email to many recipients across multiple customers – single email with combined recipients

**Description:** Verify that when the user selects contacts from multiple customers in a single ad-hoc email communication, the system sends one email message with all selected recipients rather than separate messages per customer/contact.

**Preconditions:**
1. Test data above is present.
2. User is authenticated and allowed to send email to multiple customers at once (UI or API supports this operation).
3. Valid contacts:
   - **Customer A** – Contacts A1 and A2.
   - **Customer B** – Contact B1.
4. System configuration allows sending one email across multiple customers (as per CRM design for this flow).
5. No prior communication exists with subject `PDT-2553_multi_customer_{timestamp}`.

**Test steps:**
1. Start a new email communication creation, selecting **Customer A** (Contacts A1, A2) and **Customer B** (Contact B1) as recipients in a single operation.
2. Set the subject to `PDT-2553_multi_customer_{timestamp}` and body text identifying all recipient contacts.
3. Choose an appropriate sender mailbox and trigger sending of the communication.
4. Wait for the processing to finish and ensure the main email communication status is in a final state.
5. Inspect the Phoenix email communication and related entities:
   - A single communication object for this operation.
   - EmailCommunicationCustomer entries for **Customer A** and **Customer B**.
   - EmailCommunicationCustomerContact entries for A1, A2, and B1 all marked with success status.
6. Search the external email client logs/mailbox for messages with subject `PDT-2553_multi_customer_{timestamp}`.
7. Count the number of messages and inspect their recipient headers.

**Expected test case results:**  
- Exactly **one** outbound email message exists for this communication subject.  
- That message contains **all three** addresses: `contactA1@example.test`, `contactA2@example.test`, and `contactB1@example.test`, in To/Cc/Bcc headers according to configuration.  
- Phoenix stores all three contacts and both customers as participants in a single communication instance.  
- There are no duplicate messages (e.g. one per customer or one per contact).  
- Any activities/tasks created refer back to this single communication and its single underlying email message.

**Actual result (if bug):** If the current implementation still sends one email per contact, logs will show three separate messages instead of one, and that deviation must be documented here.

**References:**  
- Mass email communication design documentation (Confluence, if available).  
- Jira: PDT-2553.

---

## TC-4 (Positive): Resend previously sent email communication – single resend message with same recipient set

**Description:** Verify that when the user resends a previously sent email communication that had multiple recipients, the system still sends a single email with the same recipient set and does not multiply messages on resend.

**Preconditions:**
1. At least one email communication from **TC-2** or **TC-3** is present with status SENT/SUCCESS and stored EmailCommunicationCustomerContact entries.
2. The resend operation is supported via UI or `GET /email-communication/{id}/resend`.
3. User has permission to perform the resend operation.

**Test steps:**
1. Identify the existing email communication created in **TC-2** or **TC-3** (using its id and subject).
2. Trigger resend using the supported mechanism:
   - UI: Use the "Resend" action/button for that communication.
   - API: Call `GET /email-communication/{id}/resend` with the communication id.
3. Wait for resend processing to complete and for the new send operation to reach its final state.
4. Inspect Phoenix records:
   - Ensure resend operation is logged (e.g. as another send attempt linked to the same communication, or as a new communication referencing the original).
   - Verify that all original recipients are still associated with the resend (no recipients lost).
5. Search the external email client logs/mailbox for new messages that correspond to the resend action (e.g. by timestamp and subject).
6. Count how many new messages have been created for the resend and inspect their recipient headers.

**Expected test case results:**  
- The resend operation produces exactly **one** new outbound email message.  
- That message has the same set of recipients as the original send (e.g. both A1 and A2, or all A1, A2, B1 depending on which original communication was resent).  
- Phoenix tracking (statuses, activities/tasks) reflects a single resend attempt for the communication, not multiple duplicate attempts per contact.  
- No unexpected additional messages are generated beyond the original and this one resend.

**Actual result (if bug):** If the implementation loops per contact when resending, logs may show multiple resend emails; that must be captured and quantified here.

**References:**  
- Endpoint: `GET /email-communication/{id}/resend`.  
- Jira: PDT-2553.

---

## TC-5 (Negative): Attempt to send email with no recipients selected

**Description:** Verify that the system prevents sending an email communication when no recipients are selected, validating input at UI/API level and not producing any outbound email.

**Preconditions:**
1. Test data above is present.
2. User is authenticated and can open the email communication creation screen or has access to `POST /email-communication`.

**Test steps:**
1. Start a new email communication creation (UI or API) without selecting any customer contacts as recipients.
2. Fill in a valid subject and body (e.g. `PDT-2553_no_recipient_{timestamp}`) but leave the recipients list empty.
3. Attempt to submit/send the email communication.
4. Observe the response:
   - UI: error message, field validation, or blocked button behaviour.
   - API: HTTP error response with appropriate status code and validation message.
5. Verify in Phoenix that no email communication record was created with a SENT or PENDING status, and that no EmailCommunicationCustomerContact records were inserted.
6. Check external email logs/mailbox for any message with the test subject.

**Expected test case results:**  
- The system clearly rejects the attempt to send an email with no recipients (e.g. HTTP 400 validation error, or UI validation message).  
- No email communication with a sending status is persisted; at most, a draft may exist but is not processed by the sender service.  
- There is **no** outbound email message created in the external email client for this test subject.  
- The user receives an understandable error explaining that at least one recipient must be selected.

**Actual result (if bug):** If the system allows sending without recipients and silently discards or misbehaves, that must be noted here.

**References:**  
- Input validation rules for email communication creation.  
- Jira: PDT-2553.

---

## References

- **Jira:** PDT-2553 – When send email to more than one recipient, send one email with multiple recipients not many separate emails.  
- **Related flows:** Existing `Email_communication` flow test cases (multi-recipient single message, status polling, shared email client integration).  
- **APIs:** `POST /email-communication`, `GET /email-communication/{id}`, `GET /email-communication/{id}/resend`.

