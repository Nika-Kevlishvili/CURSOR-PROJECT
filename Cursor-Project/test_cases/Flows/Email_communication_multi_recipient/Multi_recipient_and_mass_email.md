## Test cases for Multi_recipient_and_mass_email – Email communication multi-recipient (PDT-2553)

**Jira:** PDT-2553 (Phoenix Delivery)  
**Type:** Bug  
**Summary:** Validate behaviour when sending emails to multiple recipients using mass email and batch processing, ensuring that a single email with combined recipients is sent instead of separate emails per contact, and that statuses and reporting remain consistent.

**Scope:** CRM mass email communication flow using EmailCommunicationSenderService and MassEmailCommunicationProcessingService for larger recipient sets. This file covers multiple-recipient scenarios in mass/batch processing, including per-contact status handling, batch segmentation, and logging, while enforcing the requirement that only one outbound email is created per communication (with multiple recipients) per batch.

---

## Test data (preconditions)

Shared setup for this file (environment + entities):

- **Environment:** Test (Phoenix Test environment with configured and reachable mass email integration).
- **Customer(s) and contacts:**
  - **Customer C1** with 10 active contacts `c1_contact1@example.test` … `c1_contact10@example.test`.
  - **Customer C2** with 5 active contacts `c2_contact1@example.test` … `c2_contact5@example.test`.
  - **Customer C3** with 1 active contact `c3_contact1@example.test`.
- **Mass email configuration:**
  - Mass email functionality is enabled in Phoenix, and the MassEmailCommunicationProcessingService job is configured and executable (either automatically or manually triggered).
  - Email sender configuration supports adding many recipients in To/Cc/Bcc up to the allowed maximum per message.
- **Technical monitoring:**
  - Access to logs for:
    - MassEmailCommunicationProcessingService.
    - EmailCommunicationSenderService.sendBatch / sendSingleMass.
    - External mass email client / SMTP server.
- **Permissions:**
  - Test user can:
    - Define mass email campaigns or create mass email communication entities.
    - Trigger the mass email processing job (manually or by scheduling it for immediate run).

---

## TC-1 (Positive): Mass email to moderate-sized recipient list – one email per batch with combined recipients

**Description:** Verify that when a mass email is created with a moderate number of recipients (e.g. 10 contacts), the batch processing sends a single email with all recipients in the batch, instead of sending one email per contact.

**Preconditions:**
1. Test data above is present.
2. Mass email job scheduling or manual triggering is configured and accessible.
3. Mass email configuration (SMTP/mass_comm) allows at least 10 recipients per email without violating provider limits.

**Test steps:**
1. Create a new mass email communication entity (via UI or API) targeting **Customer C1** and selecting all 10 contacts as recipients.
2. Set a unique subject such as `PDT-2553_mass_10_recipients_{timestamp}` and appropriate body text.
3. Ensure the communication is marked for mass/batch processing (e.g. by setting a mass communication flag or using a mass email creation endpoint).
4. Trigger the MassEmailCommunicationProcessingService job (immediately, if possible).
5. Monitor logs or job status until the mass email communication processing completes.
6. In Phoenix:
   - Inspect the mass email communication entity and verify that it references all 10 contacts.
   - Check EmailCommunicationCustomerContact entries for each of the 10 contacts and their statuses (ideally SENT/SUCCESS).
7. In the external mass email client logs:
   - Search for messages with subject `PDT-2553_mass_10_recipients_{timestamp}`.
   - Count how many messages exist and inspect their recipient headers.

**Expected test case results:**  
- The mass email job processes the communication without error.  
- Exactly **one** outbound email message is created for this communication batch.  
- That message contains all 10 contact email addresses in the recipient headers (To/Cc/Bcc according to configuration).  
- All 10 EmailCommunicationCustomerContact entries are marked as SENT/SUCCESS; if some addresses are invalid, their handling is covered in negative cases from another file, not here.  
- There are no duplicate messages (no separate email per contact).  
- Any batch or job-level logs clearly indicate that the 10 recipients were processed together in one send operation.

**Actual result (if bug):** The previous defect may result in 10 individual emails (one per contact). If this behaviour persists, log details must capture the count and recipients.

**References:**  
- MassEmailCommunicationProcessingService implementation.  
- EmailCommunicationSenderService.sendBatch / sendSingleMass.  
- Jira: PDT-2553.

---

## TC-2 (Positive): Mass email across multiple customers – single email per batch covering all recipients

**Description:** Verify that when a mass email campaign includes recipients across multiple customers, the system groups them into a single email per batch, not separate emails per customer/contact.

**Preconditions:**
1. Test data above is present.
2. System supports mass email to recipients from multiple customers under the same campaign.
3. Provider limits (per email recipient cap) can handle the total number of recipients in this test (e.g. 16 contacts: 10 + 5 + 1).

**Test steps:**
1. Create a mass email communication (campaign) that:
   - Targets **Customer C1** (10 contacts).
   - Targets **Customer C2** (5 contacts).
   - Targets **Customer C3** (1 contact).
2. Set a unique subject `PDT-2553_mass_multi_customer_{timestamp}` and email body describing this scenario.
3. Mark the communication as mass/batch eligible and schedule or trigger the mass email job.
4. Allow the MassEmailCommunicationProcessingService to process the communication and observe logs for completion.
5. In Phoenix:
   - Confirm that the communication lists all 16 contacts as intended recipients across the three customers.
   - Verify that each EmailCommunicationCustomerContact entry has a proper status after processing.
6. In the external mass email logs:
   - Search for messages with this subject.
   - Count created messages and inspect their recipient headers.

**Expected test case results:**  
- Mass email processing completes successfully.  
- Exactly **one** outbound email message per processing batch is created (for this simple scenario, one batch is expected, resulting in one message).  
- The message contains all 16 email addresses in its recipient headers, representing all selected contacts from all customers.  
- Phoenix shows accurate per-contact statuses without extra or missing records.  
- There is no evidence of per-customer or per-contact duplication (no multiple emails with the same subject for the same campaign).

**Actual result (if bug):** If the system sends separate emails per customer or per contact, this will manifest as multiple emails with the same subject and partial recipient lists.

**References:**  
- mass email campaign design and cross-customer support.  
- Jira: PDT-2553.

---

## TC-3 (Positive): Mass email with batch splitting at provider limit – one email per batch, no per-contact duplication

**Description:** Verify behaviour when the total recipient count exceeds the provider's maximum recipients per email, ensuring that the system splits the recipients into multiple batches, each sending one email per batch (with multiple recipients), rather than one email per contact.

**Preconditions:**
1. Test data above is present.
2. Provider recipient limit per email is known (e.g. 50 emails per message). Assume a known limit `N`.
3. Sufficient test contacts are created across customers to exceed that limit (e.g. `N + 10` contacts total).
4. System configuration supports batch splitting by recipient count.

**Test steps:**
1. Create or reuse customers and contacts so that the total number of unique recipient email addresses is `N + 10` (for example).
2. Create a mass email campaign including all these contacts for a single send operation.
3. Use subject `PDT-2553_mass_batch_split_{timestamp}` and relevant body text.
4. Trigger the MassEmailCommunicationProcessingService job and wait for completion.
5. In Phoenix:
   - Confirm that the mass communication includes all intended recipients.
   - Review logs or job-level metadata showing that recipients were split into multiple batches.
6. In the external mass email logs:
   - Search for emails with the test subject.
   - Count the number of messages sent.
   - For each message, inspect the number of recipients.

**Expected test case results:**  
- The recipients are divided into the minimal number of batches required by the provider limit (e.g. 2 messages for `N + 10` where the first has N recipients and the second has 10).  
- Each batch results in **one** email message with multiple recipients, not multiple emails per contact.  
- The total number of outbound messages equals the number of batches, not the number of recipients.  
- Phoenix's per-contact statuses correctly reflect success per contact with batch assignment only an internal detail; there is no double-sending to any contact.  
- Logs do not show per-contact send loops or repeated sends to the same address.

**Actual result (if bug):** An incorrect implementation might still loop per contact within each batch; that would produce `N + 10` emails instead of 2–3, which must be recorded.

**References:**  
- Provider documentation for per-email recipient limits.  
- MassEmailCommunicationProcessingService batch splitting logic.  
- Jira: PDT-2553.

---

## TC-4 (Negative): Mass email job retry does not multiply sent emails for successful recipients

**Description:** Verify that when a mass email job is retried (e.g. due to transient failures), the system does not resend emails to recipients who were already successfully processed, and does not create duplicate messages for them.

**Preconditions:**
1. At least one mass email campaign from **TC-1**, **TC-2**, or **TC-3** has been executed, with some recipients marked as SENT and optionally some in FAILED state.
2. The system supports re-running the mass email job for the same campaign or has a dedicated retry mechanism.
3. Access to job logs and external email logs is available for correlation.

**Test steps:**
1. Identify a mass email campaign where:
   - Some recipients have success status.
   - Optionally, some recipients have failed or pending status (e.g. force one mailbox to temporarily fail during the first run).
2. Re-trigger the mass email job or use the retry mechanism for this campaign.
3. Wait for job completion.
4. Inspect Phoenix:
   - Check updated statuses for previously failed or pending recipients.
   - Verify that previously successful recipients have not been marked as resent or duplicated.
5. In external email logs:
   - Filter messages by the campaign subject and time window covering both initial run and retry.
   - Compare recipients of the initial messages to those of the retry-related messages.

**Expected test case results:**  
- Retry processing only attempts to send emails to recipients that did not already have a definitive success outcome.  
- No duplicate emails are sent to recipients that were already marked as SENT/SUCCESS from the initial run.  
- The total count of outbound messages after retry aligns with the number of previously failed recipients (and any required batch splitting), not all recipients again.  
- Phoenix status tracking clearly shows which recipients were retried and the final outcome for each.

**Actual result (if bug):** If the system re-sends emails to all recipients on retry, logs will show excess messages and previously successful contacts receiving duplicates.

**References:**  
- MassEmailCommunicationProcessingService retry or idempotency logic.  
- Jira: PDT-2553.

---

## TC-5 (Negative): Mass email campaign with no valid recipients – no outbound email created

**Description:** Verify that when a mass email campaign has zero valid recipients at processing time (e.g. all contacts filtered out or flagged as invalid), the system does not create any outbound email messages.

**Preconditions:**
1. A set of customers/contacts exists where each contact is marked as not eligible for email (e.g. unsubscribed, invalid, blocked for bulk email).
2. System filters such contacts out during mass email preparation.

**Test steps:**
1. Configure or select a recipient segment that is known to resolve to zero valid email recipients after applying eligibility filters.
2. Create a mass email campaign using this segment with subject `PDT-2553_mass_zero_valid_{timestamp}`.
3. Trigger the mass email processing job.
4. Wait for job completion and review job and error logs.
5. Inspect Phoenix:
   - Ensure that the mass email campaign shows zero final recipients (or that all are flagged as ineligible).
   - Verify that no EmailCommunicationCustomerContact entries reach a SENT state.
6. Check the external email logs/mailbox for any message with the test subject.

**Expected test case results:**  
- The system detects that there are zero valid recipients for the campaign.  
- No outbound email message is created for this campaign.  
- Phoenix data reflects either:
  - A clear explanation/status on the campaign that no eligible recipients were found, or
  - All intended contacts are marked with an appropriate failure/ineligible status but no sending attempt is registered.  
- External email logs show no messages with the test subject.

**Actual result (if bug):** If the system nonetheless sends emails (e.g. to invalid/unsubscribed contacts) or attempts to send to an empty list, that behaviour must be captured.

**References:**  
- Recipient eligibility and unsubscribe logic for mass email.  
- Jira: PDT-2553.

---

## References

- **Jira:** PDT-2553 – When send email to more than one recipient, send one email with multiple recipients not many separate emails.  
- **Services:** MassEmailCommunicationProcessingService, EmailCommunicationSenderService.sendBatch / sendSingleMass.  
- **Integration:** External mass email / SMTP provider configuration and logs.

