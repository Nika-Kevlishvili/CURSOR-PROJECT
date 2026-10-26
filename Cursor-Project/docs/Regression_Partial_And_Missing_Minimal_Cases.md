# Minimal regression cases — partial and missing topics

These are scope cases, not executable test cases. Each one states the uncovered assertion only. Full preconditions, payloads, and environment setup are not included.

The ranking rule is `Cursor-Project/docs/regression-scope-method.md`. This file applies that rule to topics that are partly covered or not covered in EnergoTS.

Covered happy paths are omitted. Examples already in the REG pack and not repeated here: volume billing, electricity, over time, per piece, deposit offset, liability create, manual offset happy path, and the visual menu walk.

---

## How a procedure gets its rank

Ask these questions in order. The first yes fixes the rank. Do not raise a rank because a test is missing, and do not lower a rank because a happy-path spec already exists.

1. If this fails, does **posted money** change, or does **who is billed** change, or does **who has supply** change?
   - Yes → **HIGHEST**.
2. The case does not post the amount, the billable party, or the supply flag, but it is the **entrance** to that chain or a **neighboring** process that can change a later HIGHEST outcome?
   - Yes → **HIGH**.
3. The case is a **catalog**, a **read**, a **message**, or **customer master data**, and none of those create an amount?
   - Yes → **MEDIUM**.
4. What remains is a **screen**, a **group that does not block contract create**, or a **field format**?
   - **Out of scope**.

**Posted money** means an invoice amount, a liability, a receivable, an offset, a cancellation or reversal that restores those balances, a late-payment fine, a deposit liability, or a manual offset.

**Who is billed** means a signed contract and an active point of delivery. Supply activation, deactivation, and block-for-billing decide whether that point of delivery is invoiced.

**Who has supply** means the disconnection flag, the disconnection fee, and reconnection clearing that flag. The request-for-disconnection letter does not set the flag, so the letter is HIGH.

Every write on the HIGHEST chain needs an open accounting period. Closing a period while a billing run for it is still in progress is HIGHEST.

Same area, different rank:

| Procedure | Rank | Why |
|---|---|---|
| Standard billing produces a REAL invoice and a liability | HIGHEST | Posts the amount and the debt |
| Price condition used by that run | HIGHEST | Changes the invoice |
| Create a price component that no run uses yet | MEDIUM | Catalog |
| Standard cancellation or standard reversal | HIGHEST | Restores posted balances |
| Invoice correction, manual credit or debit note | HIGH | Alternate billing type. It can change money later. The standard unwind stays HIGHEST |
| Payment that offsets a liability | HIGHEST | Settlement |
| Collection-channel setup, Virtual POS configuration | HIGH | Setup. It does not offset |
| Late-payment fine, or a block that stops the fine, the offset, or the payment | HIGHEST | Creates or suppresses a liability |
| Create an interest rate | HIGH | Input to the fine |
| Disconnection execute, fee, reconnection | HIGHEST | Flag or fee |
| Reminder or request-for-disconnection letter | HIGH | Sends a document. Does not set the flag |
| Email created with an invoice, cancellation, reconnection invoice, order invoice, manual offset, reminder, or signed contract | HIGH | Customer copy of that document. The amount stays HIGHEST |
| Mass email that is not one of those documents | MEDIUM | Message only |
| Action or claimed penalty that posts a liability | HIGHEST | The result is a liability |
| Create a penalty definition | MEDIUM | Unused until an action uses it |
| Customer create or edit | MEDIUM | Master data. Does not post an amount |
| Connected customer groups | Out of scope | Does not gate contract create and does not post debt |

On Dev the penalty posts from the action. On Dev2 it posts from the claimed penalty. The rank stays HIGHEST on both because the result is a liability.

---

## HIGHEST — partial

The happy path exists. The assertion below does not.

| Id | Procedure | Assert this | Why HIGHEST | Already there |
|---|---|---|---|---|
| H-01 | Interim and advance | A missing interim must not be charged again on the later standard invoice. When an interim exists, the standard invoice deducts it. | Invoice amount | REG-718 deduction. Defects PDT-2750, PDT-2872, PDT-3113 |
| H-02 | Invoice cancellation with compensation | Cancelling a real invoice reverses the compensation liability and the institution receivable. A closed period rejects the cancel. | Restores posted balances | REG-1068 is manual credit-note cancel. Compensation cancel is PDT-3368. Closed period is PHN-3992 |
| H-03 | VAT base | The VAT base on the completed run equals the taxable amount on the REAL invoice. | Invoice tax | REG-963 is under `tests/cursor`, not the billing pack |
| H-04 | Billing run state | A second accounting start on a completed or in-progress run does not create a second invoice. A run left in `IN_PROGRESS_ACCOUNTING` resumes without a duplicate. | Duplicate posted debt | EXP-PARITY-04 covers the lock. Scheduler resume has no spec |
| H-05 | Manual invoice | A completed manual invoice is REAL and has one liability equal to the invoice total. | Posted debt | Cancellation is covered. The amount-to-liability assert for the manual type is thin. PHN-4209 is email only |
| H-06 | Government compensation on a billed invoice | The run creates a customer liability and an institution receivable, offsets them, and does not create a receivable of amount zero. | Posted debt and offset | GC-REG. Wiki page 1077608452 states the zero-amount rule |
| H-07 | Compensation after a later document | A credit note, debit note, or correction changes the compensation net. Old receivables do not take offset priority over the new document. Interest on the compensation liability stays empty when the rule says empty. | Rewrites posted debt | PDT-3470, 3483, 3486, 3487, 3490, 3500. Not one stable pack |
| H-08 | Automatic offset | The invoice liability is offset by the compensation receivable. The open balance after offset matches the net. | Offset | Manual offset is covered. Automatic compensation offset is PDT-3487 |
| H-09 | Late payment fine | The fine amount and due date match the open liability. Reverse removes the fine and does not leave it on a missing billing group. A block for interest stops the fine. | Fine liability | PDT-2459, PDT-3215, PDT-3219. The cron is H-33 |
| H-10 | Offline payment and reverse | Reverse restores the liability current amount and any fine link the payment had closed. | Restores posted cash | `paymentReverse.spec.ts`. Fine interaction is PDT-2459 |
| H-11 | Online collection | EasyPay and Virtual POS post a payment that offsets a liability inside an OPEN period. | Settlement | `onlinePayment`, PDT-3171, PDT-3205, PDT-3214. Dev2-only channels are H-32 |
| H-12 | Accounting period dates | An invoice, a payment, and a fine take the open period that contains their date. A date outside every open period is rejected. | Gate on every posted write | PDT-3215. The close-while-running case is H-28 |
| H-13 | Profile and scales quantities | The REAL invoice volume equals the profile or scale quantity, including rounding. | Invoice amount | Billing specs assume data. Rounding is EXP-PARITY-05. PHN-4048 is performance |
| H-14 | Price condition on a run | The invoice uses the price component that is valid on the billing date, including scale conditions. | Changes the invoice | EXP-PARITY-02, PDT-3315. Creating the component with no run is M-01 |
| H-15 | Penalty liability | The action (Dev) or the claimed penalty (Dev2) creates a liability. Reverse and offset of that liability restore the balance. | Liability | REG-921 creates the action only |
| H-16 | Disconnection and reconnection | Execute sets the supply flag and can post the fee. Reconnection clears the flag. The fee uses the newest contract billing group. | Supply flag and fee | REG-1048, ReconnectionAfterPayment, PDT-3379. The letter is G-07 |
| H-17 | Rescheduling | Executing a draft splits the liability. Reversal is rejected unless status is EXECUTED. | Rewrites debt | `happyPass.spec.ts`. Written case TC-BE-6 |
| H-18 | Blocks that stop money | A block for offset, payment, or interest prevents that write. Lifting the block allows it again. | Suppresses a liability or a settlement | Specs exist per block type. Assert the prevented write, not only the block record |
| H-19 | Supply activation | A scale row on a POD that was never manually activated does not become a REAL invoice or a liability. Deactivation and block-for-billing stop the next invoice. | Who is billed | Written case TC-BE-2. One POD lifecycle spec. PDT-2177 |
| H-20 | Overpayment | A payment larger than the liability sets current amount to 0 and creates a receivable for the remainder. | Receivable | Written case TC-BE-3. Offline payment specs do not assert the remainder |

## HIGHEST — missing

No meaningful spec was found.

| Id | Procedure | Assert this | Why HIGHEST |
|---|---|---|---|
| H-28 | Close period while a run is in progress | Close is rejected while a billing run for that period is not COMPLETED. After COMPLETED, close succeeds. | A run must not account into a closed month. Written case TC-BE-7 |
| H-29 | Contract status and termination jobs | `ProductContractStatusJob` and `ServiceContractStatusJob` move the contract to the status the dates require. The termination schedulers stop billing after the termination date. | Who is billed |
| H-30 | Paid proforma update | A proforma that has been paid is updated to paid and no longer looks open. | Posted payment state |
| H-31 | Overdue order-invoice removal | `OrderInvoicesRemoveScheduledService` removes the overdue proforma order invoice and does not leave its liability open. | Removes posted debt |
| H-32 | Dev2 collection channels | Bulgaria Post, Cash Terminal, and Systech each post a payment that offsets a liability on Dev2. | Settlement. EasyPay and Virtual POS stay H-11 on both environments |
| H-33 | Late-payment-fine job | `LatePaymentFineJobService` creates the fine for an overdue liability and does not create a second fine for the same debt on the next run. | Fine liability. Interactive fine checks are H-09 |
| H-34 | One-time billing creation job | `OneTimeCreationScheduler` creates the periodical one-time run once per due period. | Invoice amount |

## HIGH — partial

| Id | Procedure | Assert this | Why HIGH | Already there |
|---|---|---|---|---|
| G-01 | Invoice correction | A volume or price correction produces the credit or debit difference. Half reversal and full reversal do not both leave the original liability open. | Alternate billing type. Standard reversal stays HIGHEST | REG-694, REG-704, REG-705, PDT-3072, PDT-3500 |
| G-02 | Payment package and collection channel | An imported package locks when the file says lock. Partner export contains the open liabilities for that channel. | Neighbor of settlement. The offset itself is HIGHEST | `paymentPackage.spec.ts`, `collectionChannel.spec.ts`, PDT-3177, PDT-2187 |
| G-03 | Product contract version, resign, open end | After resign or a new version, the next bill uses the new POD interval and the new price. An open-ended contract stays billable until a termination date exists. | Entrance. The invoice assert is the HIGHEST billing case | PHN-362 series, PDT-3454. Base `productContract.spec.ts` is create only |
| G-04 | Product new version applied to contracts | Live contracts take the new product version. A contract mid-update is not left on the old price. | Entrance to the next invoice | PDT-2906, PDT-3330 |
| G-05 | Shared POD and pulling | Volume for a shared POD is billed on one contract for the handover period. It is not billed twice. | Entrance to who is billed | `pulling.spec.ts`, EXP-PARITY-01, PDT-2854 |
| G-06 | Mass import | A contract file, a payment file, and a government-compensation file each create the same objects a single create would create. A risk-list skip does not create the contract. | Channel into HIGHEST objects | `customerMass`, `priceParameterMP`, PHN-362, PDT-3035, PDT-2931. Liability, receivable, meter, and supply-activation imports have process classes and thin or no specs |
| G-07 | Request-for-disconnection letter | The letter is generated for an eligible POD. It does not set the supply flag and does not post the fee. | Neighbor of supply. The flag is H-16 | `requestForDisconnection.spec.ts`, reminder specs |
| G-08 | Periodic and day-31 billing | A periodicity record starts a run. Day 31 does not bill a month that has no day 31. | Entrance to the billing run | PHN-4377. The one-time creation cron is H-34 |
| G-09 | Invoice email on accounting | When the billing run is accounted and sending is EMAIL, one email object is created for the invoice communication contacts and the signed invoice is attached. PAPER creates no email. A zero-amount invoice or credit note creates no email. ACCORDING_TO_THE_CONTRACT sends only when the billing group mode is email. The alternative recipient is used when set. A deleted communication header does not send. | The customer copy of a posted invoice. The amount stays HIGHEST | Created in `BillingRunEmailSenderService` from `BillingRunStartAccountingInvokeService`. PDT-3063, PDT-3187, PHN-4209. No single case covers paper, zero amount, and according-to-contract together |
| G-09a | Reconnection invoice email | A reconnection invoice, and a reversal of a reconnection invoice, is emailed by `ReconnectionInvoiceEmailService`, not by the generic billing-run sender. | Same customer-document rule | PDT-3179. The reversal branch is in `isReconnectionReversal` |
| G-09b | Cancellation, order invoice, and manual offset emails | Cancellation creates an email when email contacts exist. An order invoice creates an email. Manual liability offsetting creates an email. | Each is the customer copy of that document | `InvoiceCancellationProcessor`, `OrderInvoiceService`, `ManualLiabilityOffsettingService`. No spec found that asserts the email object |
| G-09c | Reminder email | The reminder job creates an email when the contact type is email, and an SMS when the contact type is mobile. | Document to the customer. Does not post the debt | REG-1010. `ReminderJobService` itself is not asserted |
| G-09d | Signed contract email | `ContractEmailScheduledService` emails signed product-contract and service-contract documents. | Customer copy of the contract. Does not post an invoice | No spec found |
| G-09e | Email send pipeline | After the email object exists, `EmailCommunicationJob` generates the body, and the retry and status jobs move send status. A created object that never leaves draft is a failure. | Delivery of every HIGH email above | `EmailCommunicationJob`, `EmailCommunicationSendRetryJob`, `EmailCommunicationStatusUpdateJob`. No spec found |
| G-10 | Sales portal writes | A portal customer update, contract update, and payment read match the core record after the write. | Entrance from outside Phoenix | PHN-2130, PHN-2208, PHN-2136. Not a REG pack |
| G-11 | Express, service, goods, and service orders | Create succeeds. An order invoice, when the order generates one, is a separate document from a standard volume invoice. | Entrance. Order-invoice removal is H-31 | REG create specs. They stop at create |
| G-12 | Invoice document | The generated document total equals the REAL invoice total, including compensation rows. | The customer-facing copy of posted money. The ledger assert stays on the billing case | PHN-3948, PDT-3223 cover fields, not the total |
| G-13 | Invoice number job | `InvoiceNumberGenerationJob` assigns one number per REAL invoice. A second run does not assign a second number. | Document identity. The amount is HIGHEST | No spec. Import lock is PDT-3177 |
| G-14 | Payment-package midnight lock | `PaymentPackageBlockerScheduler` locks unlocked packages at end of day. A locked package rejects a new payment. | Neighbor of settlement | PDT-3177 is import lock, not the 23:59 job |
| G-15 | Liability export job | `ExportLiabilitiesJob` and `CollectionChannelJobService` export the same open liabilities the screen export would export. | Neighbor of collection | `collectionChannel.spec.ts`, PDT-2187. Not the hourly job |

## HIGH — missing

| Id | Procedure | Assert this | Why HIGH |
|---|---|---|---|
| G-20 | xEnergie sync | `XEnergieSynchronizationScheduler` sends the contract deal and the POD dates that match the signed contract version. A failed job does not mark the contract synced. | External book. It does not itself post the Phoenix liability |

## MEDIUM — partial

| Id | Procedure | Assert this | Why MEDIUM | Already there |
|---|---|---|---|---|
| M-01 | Price component catalog | Create and edit store the component. No invoice is asserted. | Catalog until a run uses it. The run is H-14 | Used as setup inside billing specs |
| M-02 | Customer master | Create private and legal customers. A new version is the one used by a later read. No amount is asserted. | Master data | REG-157, REG-158. Version defects PDT-2815 |
| M-03 | Mass email and SMS | The send uses the customer version valid on the send date. | Message. Does not post debt | PDT-2553, PDT-2815, PDT-2881 |
| M-04 | Customer assessment and coordinator objection | Assessment create stores the assessment. Objection create stores the objection. Withdrawal stores the withdrawal. No liability is asserted. | Operational record | REG-915, REG-895. Withdrawal has a service and no spec |
| M-05 | Nomenclature | A nomenclature row required by customer or product create can be read back. | Catalog | Used as setup. One language-param spec |
| M-06 | Indicators and unwanted customers | The indicator value matches the twelve-month POD history the wiki describes. Unwanted-customer create stores the flag. | Read or flag. Does not post debt | REG-28. PDT-3319 is one indicator. Wiki page 126648335 |

## MEDIUM — missing

| Id | Procedure | Assert this | Why MEDIUM |
|---|---|---|---|
| M-10 | Tasks and processes | A task reaches the next state when `TaskStateScheduler` runs. | Operational state. Does not post debt |
| M-11 | QES | `QesSigningService` marks the document signed after `QesScheduler` moves the file. The invoice total is not re-checked here. | Signing. The amount stays on the billing case |

## Do not add

| Topic | Why |
|---|---|
| Connected customer groups | Out of scope. They do not gate contract create and they do not post debt |
| Visual menu walk | Out of scope. `tests/regression` only screenshots screens |
| System messages and template converter screen | Out of scope unless the invoice document total breaks. That assert is G-12 |
| Portal language labels and non-financial filters | Out of scope. Display only. PHN-2808, PHN-4066, PHN-4067 already exist |

---

## What to automate first

HIGHEST missing, then the HIGHEST partial rows whose current spec does not assert the balance.

1. H-28, H-29, H-30, H-31, H-33, H-34
2. H-02, H-06, H-07, H-08, H-19, H-20
3. G-09 through G-09e, then G-20

API plus a database check of the liability, receivable, invoice status, or supply flag. One case per row above is enough. Registration of a generated file follows the same cut: `Cursor-Project/docs/regression-scope-method.md`, section **What is created on the REG board**.
