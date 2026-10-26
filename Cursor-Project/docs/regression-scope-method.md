# Regression scope method

How to decide whether a Phoenix case belongs in regression, and at which rank.

This is an analysis rule. It is not a Confluence priority field. The rank and the Jira Regression Scope field are the same decision. Jira `customfield_11285` stores `Version 1`, `Version 1.1`, and `Version 1.2`. Those values are HIGHEST, HIGH, and MEDIUM. A case registered as HIGHEST runs in the first regression wave. The field has no LOW value. The rank comes from the failure impact below.

Companion pages:

- Georgian visual: `Cursor-Project/docs/regression-scope-pattern.html`
- Board mapping workbook: `Cursor-Project/reports/Chat reports/2026/september/29/REG_Board_Cases_vs_Independent_Scope.xlsx`
- Minimal cases for partial and missing topics: `Cursor-Project/docs/Regression_Partial_And_Missing_Minimal_Cases.md`

## The question

Ask, in order. The first yes fixes the rank.

1. If this case fails, does posted money change, or does who is billed or who has supply change?
   - Yes: **HIGHEST**.
2. The case does not post the amount itself, but it is the entrance to that chain or a neighboring process?
   - Yes: **HIGH**.
3. The case is catalog, a read, a message, or customer master data, and none of those create an amount?
   - Yes: **MEDIUM**.
4. What remains is a screen, a group that does not block contract create, or a field format?
   - **Out of scope**.

Posted money means an invoice amount, a liability, a receivable, an offset, a cancellation or reversal that restores those balances, a late-payment fine, a deposit liability, or a manual offset. Supply means the disconnection flag `disconnectionPowerSupply`, the disconnection fee, reconnection clearing that flag, and supply activation or deactivation that decides whether the point of delivery is billed.

## HIGHEST chain

Read left to right. A case that asserts the outcome of a box is HIGHEST.

1. **Who is billed.** A signed contract and an active point of delivery.
2. **What is billed.** The billing run and the REAL invoice, including volume, scale, profile, interim, overtime, per-piece, electricity, discount, compensation, and price conditions that change the invoice.
3. **Posted debt.** A liability, or a receivable from a credit note.
4. **Debt rewritten.** Payment and its reverse, rescheduling, late-payment fine, deposit liability, manual liability offset, and blocks that stop offset, payment, or interest.
5. **Supply.** Disconnection sets the flag and can post the fee. Reconnection clears the flag. Cancellation of a disconnection request must not leave a fee or a flag the execute path did not set.

Every write on this chain requires an open accounting period. Closing a period while a billing run is still in progress is HIGHEST.

External channels write the same settlement. EasyPay and Virtual POS exist on both Dev and Dev2. Bulgaria Post, Cash Terminal, and Systech exist on Dev2. The rank stays HIGHEST because the result is a payment against a liability.

The penalty liability is the same rank on both environments. On Dev the action posts it (`createLiabilityFromAction`). On Dev2 the claimed penalty posts it (`createLiabilityFromClaimedPenalty`). Reversal and offset of that liability stay HIGHEST.

## HIGH

HIGH is the entrance to the chain. The case can change a later HIGHEST outcome, and the title does not itself assert the invoice, the balance, or the supply flag.

- Contract create or edit, point-of-delivery create or import, re-signing, express contract, service contract.
- Goods or service order, including an invoice generated from the order. That path is separate from standard volume billing.
- Invoice correction and manual reversal or manual credit or debit note. Standard cancellation and standard reversal stay HIGHEST.
- Reminder and request-for-disconnection communication. They send a letter or a document. They do not set the supply flag.
- A customer email created because a process generated a document. The invoice amount stays HIGHEST. The email is a separate HIGH case. See **Customer email** below.
- Mass import as a channel, collection channel, payment package, and payment-channel configuration (Virtual POS, EasyPay).
- Interest rate as an input to the fine. The fine itself is HIGHEST.
- Mass-blocking setup, block lists for reminders, and block for supply termination. Blocks that stop offset, payment, or interest charging are HIGHEST.
- xEnergie deal number, documents and signing, unwanted-customer create (the gate input for a later contract), sales-portal customer update, coordinator objection when the title does not assert an overdue amount.
- Interim and advance payment definition. The interim billing run is HIGHEST.
- Catalog lock after a cancelled billing run.

A bare "create" on an HIGHEST process stays in that process. Record that the title only creates the object and does not assert the balance. Example: create payment is the settlement path, so the process is HIGHEST, and the assertion depth is object create only. Create collection channel stays HIGH.

## MEDIUM

- Price component, price parameter, terms, goods catalog, service definition, penalty definition, termination definition, groups of those objects, meter create. They do not change an invoice until a billing run, an order, an action, or a claimed penalty uses them.
- Customer create, edit, and new version.
- Mass email or SMS that is not the document produced by a billing, receivable, or contract process.
- Missed communication data on a customer, when the case does not assert that a generated invoice email was skipped.
- Sales-portal reads and other get-by-id or list calls.
- Customer assessment. It does not create a liability.
- KYC fields and reconnection screen display.
- A zero-amount invoice or credit note creates no customer email. That skip is asserted on the HIGH invoice-email case. The invoice amount of zero is still the HIGHEST billing case.

## Customer email

A process that generates a document for the customer also creates an email. That email does not change the invoice amount, the liability, or the supply flag, so it is not HIGHEST. It is HIGH, and it is mandatory on the same release as the process that creates it. The customer does not receive the document if this step fails.

Runtime today, from `phoenix-core-lib` and `phoenix-scheduler`:

| Process | Where the email is created |
|---|---|
| Billing run accounting | `BillingRunStartAccountingInvokeService.sendAccountingEmails` calls `BillingRunEmailSenderService.createEmailFromInvoiceAndSend` |
| Reversal of a reconnection invoice | Same accounting step, through `ReconnectionInvoiceEmailService` |
| Reconnection invoice | `ReconnectionInvoiceEmailService` |
| Invoice cancellation | `InvoiceCancellationProcessor` |
| Order invoice | `OrderInvoiceService` |
| Manual liability offsetting | `ManualLiabilityOffsettingService` |
| Reminder | `ReminderProcessService` when the contact type is email. `ReminderJobService` starts the job |
| Signed product or service contract | `ContractEmailScheduledService` |

`SendingAnInvoice.PAPER` does not create an invoice email. `EMAIL` uses the invoice communication contacts. `ACCORDING_TO_THE_CONTRACT` sends only when the billing group sending mode is email. A zero-amount invoice or credit note is skipped in `BillingRunEmailSenderService`. A deleted communication header must not send.

Creating the email object is not the same as delivery. `EmailCommunicationJob` generates the body. `EmailCommunicationSendRetryJob` and `EmailCommunicationStatusUpdateJob` follow send status. One regression case asserts the email object, the recipient, and the attachment. One case asserts the send job moves that object onward.

Mass mailing that is not one of the processes above stays MEDIUM.

## Out of scope

- Listing, sorting, paging, and filter screens.
- Connected customer groups. They do not gate contract create and they do not post debt.
- Manager phone or email length and format.
- A meta check that a test case can be created.

Do not put a case in regression because an API, a table, a page, or an automated test exists. Do not raise a rank because a test is missing.

## Same area, different rank

| Case | Rank | Reason |
| --- | --- | --- |
| Supply activation, deactivation, POD status change, block for billing | HIGHEST | Decides whether the point of delivery is invoiced |
| Create point of delivery | HIGH | The object can be billed later. Create does not assert an invoice |
| Invoice cancellation, standard reversal, payment reverse | HIGHEST | Restores a posted liability, receivable, or payment |
| Invoice correction, manual invoice reversal | HIGH | Alternate billing type |
| Create payment, online payment that offsets | HIGHEST | Settlement. A create-only title still belongs to this process |
| Collection channel, Virtual POS configuration | HIGH | Setup. It does not offset a liability |
| Late-payment fine, block for interest charging | HIGHEST | Creates or suppresses the fine liability |
| Create interest rate | HIGH | Input to the fine |
| Reminder, RFD letter | HIGH | Does not set the supply flag |
| Customer email for an invoice, cancellation, reconnection invoice, order invoice, manual offset, or signed contract | HIGH | Customer copy of the document. Does not change the amount or the flag |
| Mass email that is not one of those documents | MEDIUM | Message only |
| Disconnection execute, fee, reconnection | HIGHEST | Flag or fee liability |
| Re-signing validation | HIGH | Whether the next contract is allowed |
| Claimed penalty create, reverse, offset, and an action that posts a penalty | HIGHEST | The result is a liability |
| Create penalty definition | MEDIUM | Unused until an action or a claimed penalty |
| Price condition on a billing run (`COND`, `PCC`) | HIGHEST | Changes what is invoiced |
| Create price component | MEDIUM | Catalog until a run uses it |

## How to classify an existing case

1. Read the summary and, when the steps are available, the expected result.
2. Name the business outcome the case protects.
3. Apply the four questions.
4. If the summary and the steps protect different outcomes, rank the outcome the steps assert, and say so.
5. The 396 REG cases mapped on 29 September 2026 were classified from summaries only. Descriptions and steps were not read case by case. Counts from that pass: HIGHEST 228, HIGH 81, MEDIUM 69, out of scope 18.

## What is created on the REG board

Rank every scenario first. Then register one REG Test Case for each distinct outcome inside that rank. The registered case keeps the rank the four questions produced.

| Rank | Jira Regression Scope | Wave |
| --- | --- | --- |
| HIGHEST | `Version 1` | First regression wave |
| HIGH | `Version 1.1` | Entrance and neighboring processes |
| MEDIUM | `Version 1.2` | Catalog, reads, messages, customer master data |
| Not registered | Issue is not created | Stays in the source markdown |

HIGHEST and Version 1 are the same fact. A registered HIGHEST case runs in Version 1. Do not keep a second status that says HIGHEST and then leaves the case out of Version 1.

HIGH is not a later wave for extra money cases. Do not move a HIGHEST variant to Version 1.1 to shrink the first wave. Version 1.1 means the case does not itself assert posted money, who is billed, or the supply flag. Version 1.2 means catalog, a read, a message, or customer master data.

Two scenarios are the same outcome when a failure of either leaves the same wrong balance, the same wrong billed party, or the same wrong supply flag, and the second scenario only changes a count, a period, a second object of the same kind, or an extra error text on the same setup.

Register the representative scenario. Do not create REG issues for the others. Leave them in the source markdown. When a later change touches that variant, register it then, still at the rank the four questions give. A money outcome is still Version 1.

A check that is one more call after the representative setup, and that does not need a new customer, contract, or billing run, is written into the representative case. It is not a second REG issue.

Do not lower a rank because a representative already exists. Do not invent LOW. A scenario that is not the representative, and is not a different rank, is not created in Jira.

Out of scope stays out of scope. Ask once whether to create that case. **Yes** creates it with Regression Scope empty. **No** skips it.

Example. Invoice cancellation with government compensation registers three Version 1 cases: one positive compensation unwound to zero, one negative compensation that leaves a second customer liability, and one invoice that carries two compensations and a different unique-result count. A second period on that same two-compensation shape is not registered. A regenerate call refused on the cancelled invoice, and a second cancel of the same invoice number, are extra calls on the positive case. They are not separate REG issues.

## What this method does not do

It does not edit Jira by itself. Registration is `.cursor/skills/register-regression-test-cases/SKILL.md`. It does not say a case is well written. It does not treat the Regression Board as the source of what belongs in regression. It does not shrink a wave by relabeling a money case as HIGH or MEDIUM.
