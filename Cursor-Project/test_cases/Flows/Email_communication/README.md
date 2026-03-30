# Email_communication – Flow-based test cases

Tests for CRM email communication: creation, send triggers, mass communication integration, recipient handling, and status updates.

| File | Content |
|------|---------|
| **Multi_recipient_single_message.md** | PDT-2553: one outbound message with multiple recipients in To (single API/SMTP semantics), versus N separate sends; 3+ contacts; batch with multiple customers (one message per customer); event, `sendSingleMass`, and attachment parity; duplicate-address edge case; all-or-nothing failure risk from Jira; single-recipient baseline; send-disabled; validation. |
| **Status_polling_and_task_mapping.md** | Per-contact taskId, customer/contact status, mass batch paths, partial outcomes, privacy (To vs Bcc), and mock client alignment after interface changes. |
| **Shared_email_client_and_notifications.md** | Regression: shared `EmailSenderServiceInterface` callers (notifications, termination/objection-style flows) after PDT-2553 multi-recipient CRM changes; invalid recipient handling. |
