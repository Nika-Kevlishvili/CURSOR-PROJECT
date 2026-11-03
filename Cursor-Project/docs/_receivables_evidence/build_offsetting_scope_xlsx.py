# -*- coding: utf-8 -*-
"""Regression scope for automatic offsetting and direct offsetting only."""
from __future__ import annotations

from collections import defaultdict
from pathlib import Path

from openpyxl import Workbook
from openpyxl.styles import Alignment, Font, PatternFill
from openpyxl.utils import get_column_letter

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "docs" / "Offsetting_Automatic_And_Direct_Regression_Cases.xlsx"
OUT_MD = ROOT / "docs" / "Offsetting_Automatic_And_Direct_Catalog.md"

HEADERS = [
    "Theme", "Priority", "PriorityReason", "CaseId", "CaseTitle", "EvidenceType",
    "EvidenceCitation", "EvidenceQuote", "AutomationStatus", "AutomationName",
    "AutomationFile", "Notes",
]
SHEETS = ["00_Summary", "00_Unmapped_Gaps", "01_Automatic_Offsetting", "02_Direct_Offsetting"]
RANK = {"Highest": 0, "High": 1, "Medium": 2, "Low": 3}
HEADER_FILL = PatternFill("solid", fgColor="1F4E79")
HEADER_FONT = Font(color="FFFFFF", bold=True)
FILLS = {
    "Yes": PatternFill("solid", fgColor="C6EFCE"),
    "Partial": PatternFill("solid", fgColor="FFEB9C"),
    "No": PatternFill("solid", fgColor="FFC7CE"),
    "Highest": PatternFill("solid", fgColor="F4B183"),
    "High": PatternFill("solid", fgColor="FFE699"),
    "Medium": PatternFill("solid", fgColor="DDEBF7"),
    "Low": PatternFill("solid", fgColor="E7E6E6"),
}

AUTO = "01_Automatic_Offsetting"
DIRECT = "02_Direct_Offsetting"
TC_HEADERS = [
    "Process", "CaseId", "Type", "Priority", "Title", "Description",
    "Preconditions", "TestSteps", "ExpectedResult", "EvidenceCitation",
    "EvidenceQuote", "AutomationStatus", "AutomationFile", "ScopeRow",
]


def tc(process, case_id, kind, prio, title, description, pre, steps, expected, cite, quote, status, afile, scope):
    return [process, case_id, kind, prio, title, description, pre, steps, expected, cite, quote[:240], status, afile, scope]


def test_cases():
    auto_file = "EnergoTS/tests/receivableManagement/Payment/offlinePaymentCreateAndOffsetting.spec.ts"
    online_file = "EnergoTS/tests/receivableManagement/Payment/onlinePayment.spec.ts"
    block_file = "EnergoTS/tests/receivableManagement/massOperationForBlocking/BlockingOffsetting.spec.ts"
    cancel_file = "EnergoTS/tests/cursor/PDT-3578-invoice-cancellation-reverse-automatic-offsetting.spec.ts"
    auto_diagram = "Cursor-Project/config/Diagrams/Bundle 6/Updated 28012026 - Automatic offsetting.semantic.md"
    pay_diagram = "Cursor-Project/config/Diagrams/Bundle 6/automatic offsetting-Occurrence Date Payment.semantic.md"
    rows = [
        tc("Automatic", "TC-AO-1", "Positive", "Highest",
           "Offline payment covers the invoice liability and the remainder becomes a receivable",
           "After a real invoice liability exists, a larger offline payment offsets that liability and leaves a receivable for the difference. The receivable uses the payment billing group.",
           "1. POST /customer with a legal customer. Keep the customer id.\n"
           "2. POST /terms, POST /pod, POST /product, and POST /product-contract for that customer. Activate the POD with POST /contract-pods/manual.\n"
           "3. POST /billing-run for a manual invoice and wait until the invoice is real. GET /customer-liability/list for this customer and keep the one liability. Its invoice id is the invoice from this run.\n"
           "4. POST /collection-channel and POST /payment-package with lockStatus UNLOCKED.",
           "1. POST /payment with initialAmount 160.00 for this customer, package, and channel.\n"
           "2. GET /payment/{paymentId} and GET /customer-liability/{liabilityId}.\n"
           "3. From the payment offsettingResponseList take the row whose offsettingObjectType is RECEIVABLE. GET /customer-receivable/{that id}.",
           "Payment currentAmount is 0. The payment offsetting list contains both a LIABILITY row and a RECEIVABLE row. Liability currentAmount is 0. Receivable currentAmount equals payment initialAmount minus liability initialAmount, to two decimals. Receivable billingGroupResponse.id equals the payment contractBillingGroupId.",
           auto_file + ":123",
           "check amounts, check receivables billing group: payment currentAmount 0, liability currentAmount 0, receivable currentAmount close to payment initial minus liability initial.",
           "Yes", auto_file, "REG-1031"),
        tc("Automatic", "TC-AO-2", "Negative", "Highest",
           "A customer blocked for liability offsetting is not automatically offset",
           "After a mass block for liability offsetting that lists this customer, a new receivable of the same amount does not reduce the open liability.",
           "1. POST /customer with a legal customer. Keep the customer identifier.\n"
           "2. POST /mass-operation-for-blocking with isBlockForLiabilitiesOffsetting true, blockingForLiabilitiesOffsetting.fromDate yesterday, and the other blocking blocks null. listOfCustomers contains only this customer.\n"
           "3. Create terms, a price component, a POD, a product, and a product contract. Activate the POD. GET the contract and keep billingGroups[0].id.\n"
           "4. POST /customer-liability with initialAmount 100.00, that billingGroupId, and occurrenceDate yesterday.",
           "1. POST /customer-receivable with initialAmount 100.00, the same billingGroupId, and occurrenceDate today.\n"
           "2. GET /customer-receivable/{receivableId}.\n"
           "3. GET /customer-liability/{liabilityId}.",
           "Receivable currentAmount stays 100.00 and customerOffsettingResponseList is empty. Liability currentAmount stays 100.00 and customerOffsettingResponseList is empty.",
           block_file + ":9",
           "Create receivable to check automatic offsetting. Both current amounts stay 100 and both offsetting lists are empty.",
           "Yes", block_file, "REG-1014"),
        tc("Automatic", "TC-AO-3", "Positive", "Highest",
           "Invoice cancellation restores the receivable that automatic offsetting had consumed",
           "A receivable posted before the invoice is larger than the invoice total. Generating the invoice offsets that receivable to 0. Cancelling the invoice restores the receivable current amount to the value it had before the invoice.",
           "1. Start a volume debit-note draft and keep the draft invoice total T. T is greater than 0.\n"
           "2. POST /customer-receivable on the contract billing group with initialAmount T + 100.00. GET it and record currentAmount. It equals T + 100.00.\n"
           "3. POST /government-compensations with a negative document amount whose absolute value is T + 500.00.\n"
           "4. Generate and account the invoice until invoiceStatus is REAL. The main invoice liability currentAmount is 0. The compensation liability is still open and is lower than its initial amount. The receivable currentAmount is 0.",
           "1. POST /invoice-cancellation with the invoice number and a taxEventDate inside an open accounting period.\n"
           "2. Wait until the cancellation process finishes and GET /invoice shows invoiceStatus CANCELLED.\n"
           "3. GET /customer-receivable/{receivableId}.",
           "The receivable currentAmount equals the current amount recorded before the invoice was generated.",
           cancel_file + ":69",
           "After POST /invoice-cancellation the manual receivable currentAmount returns to the amount from before the invoice.",
           "Yes", cancel_file, "PDT-3578"),
        tc("Automatic", "TC-AO-4", "Positive", "Highest",
           "Saving a receivable deducts it against open liabilities until the receivable current amount is 0",
           "Automatic offsetting runs when a customer receivable is saved. The deduction stops when the receivable current amount reaches 0 or the liabilities are covered.",
           "1. POST /customer (PRIVATE, ACTIVE). Keep customerId.\n"
           "2. Take an open accounting-period id from GET /billing-run/accounting-period-available-list and the main currencyId from GET /currencies.\n"
           "3. POST /customer-liability for this customer with initialAmount 100.00, dueDate 2026-03-01, occurrenceDate 2026-03-01, and every block flag false. GET /customer-liability/{id} shows currentAmount 100.00.",
           "1. POST /customer-receivable for the same customer, period, and currency, initialAmount 40.00, dueDate 2026-03-20, occurrenceDate 2026-03-02, blockedForOffsetting false.\n"
           "2. GET /customer-receivable/{receivableId}.\n"
           "3. GET /customer-liability/{liabilityId}.",
           "The diagram rule applied to these inputs: receivable currentAmount is 0 and the offsetting list contains the liability. Liability initialAmount stays 100.00 and currentAmount is 60.00. The 40.00 and 100.00 figures are the test inputs. The written rule is the deduction sentence, not these two numbers.",
           "CustomerReceivableService.java:178-192; " + auto_diagram,
           "For each receivable make deduction for each liability until receivable current amount reaches 0 or until all liabilities are covered.",
           "No", "", "DIAG-AUTO-DEDUCT"),
        tc("Automatic", "TC-AO-5", "Positive", "Highest",
           "A receivable whose occurrence date is in the future is still offset",
           "Automatic offsetting still runs when the receivable occurrence date is in the future.",
           "1. POST /customer (PRIVATE, ACTIVE). Keep customerId.\n"
           "2. Take an open accounting-period id and the main currencyId.\n"
           "3. POST /customer-liability with initialAmount 100.00, dueDate 2026-03-01, occurrenceDate 2026-03-01, all block flags false. GET currentAmount is 100.00.",
           "1. POST /customer-receivable for the same customer with initialAmount 40.00, dueDate 2026-12-15, occurrenceDate 2026-12-01, blockedForOffsetting false.\n"
           "2. GET /customer-receivable/{receivableId}.\n"
           "3. GET /customer-liability/{liabilityId}.",
           "Offsetting still happens. Receivable currentAmount is 0. Liability currentAmount is 60.00. The offsetting list on the receivable contains that liability.",
           auto_diagram,
           "automatic offsetting can happen for liabilities /receivables for which occurrence date is in future",
           "No", "", "DIAG-AUTO-FUTURE"),
        tc("Automatic", "TC-AO-6", "Positive", "Highest",
           "Offsetting still runs when the occurrence date is later than the payment date",
           "The payment deduction still runs when the liability occurrence date is after the payment date.",
           "1. POST /customer (PRIVATE, ACTIVE). Keep customerId.\n"
           "2. Take an open accounting-period id, the main currencyId, and one collectionChannelId.\n"
           "3. POST /payment-package with lockStatus UNLOCKED, that channel, that period, and paymentDate 2026-10-27.\n"
           "4. POST /customer-liability with initialAmount 100.00 and occurrenceDate 2026-12-01, which is after the payment date. GET currentAmount is 100.00.",
           "1. POST /payment with paymentDate 2026-10-27, initialAmount 40.00, this customer, package, channel, period, and currency.\n"
           "2. GET /payment/{paymentId}.\n"
           "3. GET /customer-liability/{liabilityId}.",
           "The process still deducts. Applied to these inputs, payment currentAmount is 0 and liability currentAmount is 60.00. The 40.00 and 100.00 figures are the test inputs. The written rule is the occurrence-date sentence together with deduction until the payment current amount is 0.",
           pay_diagram,
           "The process must work even if Occurence Date > Payment Date",
           "No", "", "DIAG-AUTO-OCCURRENCE"),
        tc("Automatic", "TC-AO-7", "Positive", "Highest",
           "Separate invoice for each POD limits which liabilities are offset",
           "When the billing group is marked separate invoice for each POD, automatic offsetting takes only liabilities and receivables whose invoice exists with a separate POD, ordered by due date then id.",
           "1. POST /customer (PRIVATE, ACTIVE).\n"
           "2. Create two PODs and a product contract whose billing group has separateInvoiceForEachPod true.\n"
           "3. Produce one real invoice liability on the first POD and one real invoice liability on the second POD. Both current amounts are greater than 0. Record both due dates and ids.\n"
           "4. POST /customer-receivable for this customer with initialAmount large enough to cover only one of those liabilities, blockedForOffsetting false.",
           "1. GET /customer-receivable/{receivableId} and read customerOffsettingResponseList in the order the process applied them.",
           "Every offset liability is one whose invoice exists with a separate POD. The applied order is due date ascending, then id ascending. A liability whose invoice does not have a separate POD is not in the list.",
           auto_diagram,
           "Check billing group separate invoice for each POD is marked? Then take only liabilities/receivables for which invoice exists with separate POD. Order by due date asc and then by ID asc.",
           "No", "", "DIAG-AUTO-POD"),
        tc("Direct", "TC-DO-1", "Positive", "Highest",
           "An offline payment larger than the liability offsets it and generates a receivable for the rest",
           "Direct offsetting from an offline payment of 200.00 against one liability of 100.00 brings both current amounts to 0 and leaves a receivable of 100.00.",
           "1. POST /customer with a legal customer.\n"
           "2. POST /collection-channel and POST /payment-package.\n"
           "3. POST /customer-liability with initialAmount 100.00.",
           "1. POST /payment with initialAmount 200.00.\n"
           "2. GET /payment/{paymentId}.\n"
           "3. GET /customer-liability/{liabilityId}.\n"
           "4. In the payment offsettingResponseList find the row whose offsettingObjectType is RECEIVABLE.",
           "Payment currentAmount is 0. Liability currentAmount is 0. The payment offsetting list contains the liability id. The receivable row amount is -100.",
           auto_file + ":47",
           "Payment initialAmount 200 and liability initialAmount 100. Both current amounts are 0. Receivable amount is -100.",
           "Yes", auto_file, "REG-1005"),
        tc("Direct", "TC-DO-2", "Positive", "Highest",
           "An EasyPay online payment offsets combined liabilities",
           "When the EasyPay collection channel has combined checked, one online payment offsets the customer's open liabilities and the payment current amount becomes 0.",
           "1. POST /customer with a legal customer. Keep customerNumber.\n"
           "2. POST /customer-liability with initialAmount 100.00.\n"
           "3. POST /customer-liability with initialAmount 100.00 for the same customer.\n"
           "4. GET /collection-channel for EasyPay and type ONLINE. If combined is not checked, edit the channel so combined is checked.\n"
           "5. GET system configuration and keep easyPayMerchantId.",
           "1. GET the EasyPay init checksum, then GET epay/init-pay for this customer. STATUS is 00. Keep AMOUNT.\n"
           "2. GET the confirm checksum, then GET epay/confirm-pay. STATUS is 00.\n"
           "3. POST /payment/list for this customer and keep the payment id.\n"
           "4. GET /payment/{paymentId} until currentAmount is 0.",
           "A payment is created. Its currentAmount becomes 0.",
           online_file + ":8",
           "Online Payment - Create + Direct offsetting (Easy-pay integration), combine liabilities. Payment currentAmount is 0.",
           "Yes", online_file, "REG-1036"),
        tc("Direct", "TC-DO-3", "Positive", "Highest",
           "An EasyPay online payment on overdue liabilities offsets them and creates two late payment fines",
           "Two overdue liabilities, with combined checked on EasyPay, produce two late payment fines. The online payment current amount becomes 0.",
           "1. POST /customer with a legal customer.\n"
           "2. POST /interest-rate with a daily interest rate.\n"
           "3. POST /customer-liability twice, each from the overdue liability payload.\n"
           "4. GET the EasyPay ONLINE collection channel and set combined checked.\n"
           "5. GET system configuration and keep easyPayMerchantId.",
           "1. GET the EasyPay init checksum for this customer, then GET epay/init-pay. STATUS is 00. Keep AMOUNT.\n"
           "2. GET the confirm checksum, then GET epay/confirm-pay. STATUS is 00.\n"
           "3. POST /payment/list for this customer and keep the payment id.\n"
           "4. Wait until late payment fine generation returns two rows.\n"
           "5. GET /payment/{paymentId} until currentAmount is 0.",
           "Late payment fine content length is 2. Payment currentAmount is 0.",
           online_file + ":131",
           "When liabilities are overdue, create plus direct offsetting plus LPF creation. Two LPFs. Payment currentAmount is 0.",
           "Yes", online_file, "REG-1063"),
        tc("Direct", "TC-DO-4", "Negative", "Highest",
           "A payment does not offset a liability while the customer is blocked for liability offsetting",
           "The same block that stops automatic offsetting also stops the payment from reducing that liability. The payment generates a receivable instead.",
           "1. POST /customer with a legal customer.\n"
           "2. POST /mass-operation-for-blocking with isBlockForLiabilitiesOffsetting true, blockingForLiabilitiesOffsetting.fromDate yesterday, and the other blocking blocks null. listOfCustomers contains only this customer.\n"
           "3. Create the contract chain and POST /customer-liability with initialAmount 100.00. POST /customer-receivable with initialAmount 100.00. GET both. Each currentAmount is 100.00 and each offsetting list is empty.\n"
           "4. POST /collection-channel and POST /payment-package.",
           "1. POST /payment with initialAmount 100.00.\n"
           "2. GET /payment/{paymentId}.\n"
           "3. GET /customer-liability/{liabilityId}.",
           "The payment offsettingResponseList contains a RECEIVABLE row. Liability currentAmount stays 100.00 and customerOffsettingResponseList stays empty.",
           block_file + ":145",
           "create payment and check direct offsetting: a receivable is generated. Liability currentAmount stays 100 and its offsetting list stays empty.",
           "Yes", block_file, "REG-1014"),
        tc("Direct", "TC-DO-5", "Positive", "Medium",
           "A payment offset waits when offsetting is already running for those liabilities",
           "If an offsetting process is already running for the liabilities, the payment offset waits until that process ends and then deducts.",
           "1. POST /customer and one open liability with currentAmount greater than 0.\n"
           "2. POST /collection-channel and POST /payment-package.\n"
           "3. Start an offsetting process that already includes that liability and has not finished.",
           "1. POST /payment with initialAmount equal to the liability current amount while that process is still running.\n"
           "2. After the first process has ended, GET /payment/{paymentId} and GET /customer-liability/{liabilityId}.",
           "The payment offset does not fail because the first process was running. After the wait, payment currentAmount is 0 or the liabilities are covered.",
           pay_diagram,
           "Check for this liabilities is offsetting process already running? Then wait until process end.",
           "No", "", "DIAG-DIRECT-WAIT"),
        tc("Direct", "TC-DO-6", "Positive", "Highest",
           "A filled outgoing document on a separate-invoice billing group limits the payment offset to that group and POD",
           "When the payment billing group has separate invoice selected and the payment outgoing document is filled, the offset takes only liabilities for that billing group and that POD.",
           "1. POST /customer with two billing groups. One group has separateInvoiceForEachPod true and an outgoing document. The other group does not.\n"
           "2. Create one open invoice liability on each group. Both current amounts are greater than 0.\n"
           "3. POST /collection-channel and POST /payment-package.",
           "1. POST /payment on the separate-invoice billing group with the outgoing document filled and initialAmount equal to the first liability only.\n"
           "2. GET /payment/{paymentId} and both liabilities.",
           "The offsetting list contains only the liability for that billing group and that POD. The liability on the other billing group keeps its current amount.",
           pay_diagram,
           "Check if separate invoice is selected in billing group of payment and in payment outgoing document is filled. Then take only liabilities for the respective billing group and POD.",
           "No", "", "DIAG-DIRECT-POD"),
    ]
    return TC_HEADERS, rows


def write_tc_sheet(wb, name, items):
    ws = wb.create_sheet(name)
    ws.append(TC_HEADERS)
    for col in range(1, len(TC_HEADERS) + 1):
        cell = ws.cell(1, col)
        cell.fill = HEADER_FILL
        cell.font = HEADER_FONT
        cell.alignment = Alignment(wrap_text=True, vertical="center")
    for item in items:
        ws.append(item)
        kind = item[2]
        prio = item[3]
        status = item[11]
        ws.cell(ws.max_row, 3).fill = FILLS["No"] if kind == "Negative" else FILLS["Yes"]
        ws.cell(ws.max_row, 4).fill = FILLS[prio]
        ws.cell(ws.max_row, 12).fill = FILLS[status]
        for col in range(1, len(TC_HEADERS) + 1):
            ws.cell(ws.max_row, col).alignment = Alignment(wrap_text=True, vertical="top")
        ws.row_dimensions[ws.max_row].height = 90
    widths = [14, 12, 12, 12, 42, 42, 55, 55, 55, 42, 42, 16, 42, 18]
    for col, width in enumerate(widths, 1):
        ws.column_dimensions[get_column_letter(col)].width = width
    ws.freeze_panes = "A2"
    ws.auto_filter.ref = ws.dimensions
    ws.auto_filter.ref = "A1:%s%s" % (get_column_letter(len(TC_HEADERS)), ws.max_row)
    ws.sheet_view.showGridLines = True


def row(theme, prio, reason, case_id, title, etype, cite, quote, status, aname, afile, notes=""):
    return {
        "Theme": theme, "Priority": prio, "PriorityReason": reason, "CaseId": case_id,
        "CaseTitle": title, "EvidenceType": etype, "EvidenceCitation": cite,
        "EvidenceQuote": quote[:240], "AutomationStatus": status, "AutomationName": aname,
        "AutomationFile": afile, "Notes": notes, "_prio_rank": RANK[prio],
    }


def main() -> None:
    rows = [
        row(AUTO, "Highest", "Payment leaves a receivable and that receivable is offset automatically",
            "REG-1031", "Offline Payment create + automatic offsetting + receivable generation",
            "Jira", "REG-1031 | type=Test Case",
            "Offline Payment create + automatic offsetting + receivable generation",
            "Yes", "[REG-1031]: Payment create + automatic offsetting + receivable generation",
            "EnergoTS/tests/receivableManagement/Payment/offlinePaymentCreateAndOffsetting.spec.ts",
            "Jira status: Done; asserting test at line 123"),
        row(AUTO, "Highest", "Block for liabilities offsetting stops the automatic offset",
            "REG-1014", "Block for offsetting - list of customers",
            "Jira", "REG-1014 | type=Test Case",
            "Block for offsetting - list of customers",
            "Yes", "[REG-1014]: Block for offsetting - list of customers",
            "EnergoTS/tests/receivableManagement/massOperationForBlocking/BlockingOffsetting.spec.ts",
            "Jira status: Done; asserting test at line 9. The test then creates a receivable to check automatic offsetting."),
        row(AUTO, "Highest", "Cancelling the invoice reverses the automatic offsets",
            "PDT-3578", "[Backend] Invoice Cancellation - Reverse of Automating offseting",
            "Playwright",
            "EnergoTS/tests/cursor/PDT-3578-invoice-cancellation-reverse-automatic-offsetting.spec.ts:69",
            "[Backend] Invoice Cancellation - Reverse of Automating offseting",
            "Yes", "[PDT-3578]: [Backend] Invoice Cancellation - Reverse of Automating offseting",
            "EnergoTS/tests/cursor/PDT-3578-invoice-cancellation-reverse-automatic-offsetting.spec.ts",
            "Asserts the automatic offsets are reversed. The cancellation process itself stays in the billing catalog."),
        row(AUTO, "Highest", "Diagram process decision",
            "DIAG-AUTO-POD",
            "Separate invoice for each POD changes which liabilities and receivables are offset, and the order is due date then ID",
            "Diagram", "Updated 28012026 - Automatic offsetting.semantic.md",
            "Check billing group separate invoice for each POD is marked? Then order by due date asc and ID asc, or take only liabilities/receivables for which an invoice exists with a separate POD.",
            "No", "", "", "Kind=Decision"),
        row(AUTO, "Highest", "Diagram process outcome",
            "DIAG-AUTO-DEDUCT",
            "Each receivable is deducted against liabilities until the receivable current amount is 0 or the liabilities are covered",
            "Diagram", "Updated 28012026 - Automatic offsetting.semantic.md",
            "For each receivable make deduction for each liability until receivable current amount reaches 0 or until all liabilities are covered.",
            "No", "", "", "Kind=Step"),
        row(AUTO, "Highest", "Diagram process rule",
            "DIAG-AUTO-FUTURE",
            "Automatic offsetting still runs when the occurrence date is in the future",
            "Diagram", "Updated 28012026 - Automatic offsetting.semantic.md",
            "automatic offsetting can happen for liabilities /receivables for which occurrence date is in future",
            "No", "", "", "Kind=Step"),
        row(AUTO, "Highest", "Diagram process rule",
            "DIAG-AUTO-OCCURRENCE",
            "The offset still runs when the occurrence date is later than the payment date",
            "Diagram", "automatic offsetting-Occurrence Date Payment.semantic.md",
            "The process must work even if Occurence Date > Payment Date",
            "No", "", "", "Kind=Step"),
        row(AUTO, "Low", "Empty Confluence body — no executable process rule",
            "CONF-585698824",
            "[BodyEmpty of rules] Phase 2 - Automatic Offsetting Process (2)",
            "Confluence",
            "Phase 2 - Automatic Offsetting Process (2) | page 585698824 | https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/585698824",
            "Page body is diagram links only. No should/must sentence.",
            "No", "", "", "Use the diagram rows. Do not invent rules from the title."),
        row(AUTO, "Low", "Empty Confluence body — no executable process rule",
            "CONF-585699256",
            "[BodyEmpty of rules] Phase 2 - Automatic Liability Offsetting Process (2)",
            "Confluence",
            "Phase 2 - Automatic Liability Offsetting Process (2) | page 585699256 | https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/585699256",
            "Page body is one diagram URL. No should/must sentence.",
            "No", "", "", "Use the diagram rows. Do not invent rules from the title."),
        row(DIRECT, "Highest", "Payment is created and offsets the liability directly, then a receivable is generated for the remainder",
            "REG-1005", "Offline Payment create + direct offsetting + receivable generation",
            "Jira", "REG-1005 | type=Test Case",
            "Offline Payment create + direct offsetting + receivable generation",
            "Yes", "[REG-1005]: Create payment + direct offsetting + receivable generation",
            "EnergoTS/tests/receivableManagement/Payment/offlinePaymentCreateAndOffsetting.spec.ts",
            "Jira status: Done; asserting test at line 47"),
        row(DIRECT, "Highest", "Online payment offsets combined liabilities directly when the collection channel combined flag is checked",
            "REG-1036", "Online Payment - Create + Direct offsetting, when combined is checked in collection channel (EasyPay)",
            "Jira", "REG-1036 | type=Test Case",
            "Online Payment - Create + Direct offsetting, when combined is checked in collection channel(Easy-pay integration)",
            "Yes", "[REG-1036]: Online Payment - Create + Direct offsetting (Easy-pay integration), combine liabilities",
            "EnergoTS/tests/receivableManagement/Payment/onlinePayment.spec.ts",
            "Jira status: QA Failed; asserting test at line 8"),
        row(DIRECT, "Highest", "Online payment on overdue liabilities offsets them directly and creates the late payment fine",
            "REG-1063", "Online Payment - When liabilities are overdue (Create + direct offsetting + LPF creation)",
            "Jira", "REG-1063 | type=Test Case",
            "Online Payment - When liabilities are overdue (Create + direct offsetting + LPF creation)",
            "Yes", "[REG-1063]: Online Payment - When liabilities are overdue (Create + direct offsetting + LPF creation), when combined liability",
            "EnergoTS/tests/receivableManagement/Payment/onlinePayment.spec.ts",
            "Jira status: Done; asserting test at line 131"),
        row(DIRECT, "Highest", "Confluence process rule",
            "CONF-585698837-1",
            "Direct offsetting needs a liability id and one of receivable, payment, deposit, or rescheduling",
            "Confluence",
            "Phase 2 - Direct Offsetting Process (2) | page 585698837 | https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/585698837",
            "To generate the Direct Offsetting Process, you will need a liability ID (mandatory) and least one of the following: receivable, payment, deposit, rescheduling ID (mandatory).",
            "No", "", "", "Phase 1 page 198017044 has the same sentence."),
        row(DIRECT, "High", "Confluence says the rest of direct offsetting follows manual liability offsetting",
            "CONF-585698837-2",
            "After the ids are given, direct offsetting follows the manual liability offsetting process",
            "Confluence",
            "Phase 2 - Direct Offsetting Process (2) | page 585698837 | https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/585698837",
            "The rest of the process is the same as Manual liability offsetting.",
            "No", "", "", "This does not make an MLO test case a direct-offsetting case."),
        row(DIRECT, "Highest", "Diagram process outcome",
            "DIAG-DIRECT-PAY",
            "The payment is deducted against each liability until the payment current amount is 0 or the liabilities are covered",
            "Diagram", "automatic offsetting-Occurrence Date Payment.semantic.md",
            "make deduction from payment for each liability until payment current amount reaches 0 or until all liabilities are covered",
            "No", "", "", "Kind=Step. Payment is one of the sources named on the Direct Offsetting page."),
        row(DIRECT, "Medium", "Diagram wait path",
            "DIAG-DIRECT-WAIT",
            "If offsetting is already running for the liabilities, the payment offset waits until that process ends",
            "Diagram", "automatic offsetting-Occurrence Date Payment.semantic.md",
            "Check for this liabilities is offsetting process already running? Then wait until process end.",
            "No", "", "", "Kind=Decision"),
        row(DIRECT, "Highest", "Diagram process decision",
            "DIAG-DIRECT-POD",
            "A separate invoice on the payment billing group, with an outgoing document, limits the offset to that billing group and POD",
            "Diagram", "automatic offsetting-Occurrence Date Payment.semantic.md",
            "Check if separate invoice is selected in billing group of payment and in payment outgoing document is filled. Then take only liabilities for the respective billing group and POD.",
            "No", "", "", "Kind=Decision"),
    ]

    gaps = [dict(item, Theme="00_Unmapped_Gaps") for item in rows if item["AutomationStatus"] == "No"]

    wb = Workbook()
    wb.remove(wb.active)

    def write_sheet(name: str, items: list[dict]) -> None:
        ws = wb.create_sheet(name)
        ws.append(HEADERS)
        for col in range(1, len(HEADERS) + 1):
            cell = ws.cell(1, col)
            cell.fill = HEADER_FILL
            cell.font = HEADER_FONT
            cell.alignment = Alignment(wrap_text=True, vertical="center")
        ordered = sorted(items, key=lambda x: (x["_prio_rank"], 0 if x["AutomationStatus"] == "No" else 2, x["CaseId"]))
        for item in ordered:
            ws.append([item.get(h, "") for h in HEADERS])
            ws.cell(ws.max_row, HEADERS.index("AutomationStatus") + 1).fill = FILLS[item["AutomationStatus"]]
            ws.cell(ws.max_row, HEADERS.index("Priority") + 1).fill = FILLS[item["Priority"]]
        for col in range(1, len(HEADERS) + 1):
            ws.column_dimensions[get_column_letter(col)].width = min(46, max(14, len(HEADERS[col - 1]) + 2))
        ws.freeze_panes = "A2"
        ws.auto_filter.ref = ws.dimensions

    counts = defaultdict(lambda: defaultdict(int))
    prios = defaultdict(lambda: defaultdict(int))
    for item in rows:
        counts[item["Theme"]][item["AutomationStatus"]] += 1
        counts[item["Theme"]]["Total"] += 1
        prios["ALL"][item["Priority"]] += 1
        prios[item["Theme"]][item["Priority"]] += 1

    ws = wb.create_sheet("00_Summary", 0)
    lines = [
        "Automatic and Direct Offsetting — process-only regression scope",
        "Two processes only. Manual liability offsetting, deposit refund, payment reversal, and invoice correction stay out.",
        "Automatic, runtime: after a customer liability or a customer receivable is saved, AutomaticOffsettingService.offsetOfLiabilityAndReceivable runs. CustomerLiabilityService.java:146-168 and CustomerReceivableService.java:178-192.",
        "Direct, documented: liability ID plus one of receivable, payment, deposit, or rescheduling. Phase 2 page 585698837. The same sentence is on Phase 1 page 198017044.",
        "Direct, runtime: liabilityDirectOffsettingService.directOffsetting with operation context DLO. Example: CustomerLiabilityService.java:237-246 and CustomerReceivableService.java:532-538.",
        "Jira source: MCP search on oppa-support, project REG, summary contains offsetting. 18 hits. Kept the leaves whose title names automatic or direct offsetting, plus the block that stops liability offsetting.",
        "Removed from those 18: MLO (REG-831, REG-832, REG-1002), payment reverse (REG-1033, REG-1034), deposit edit (REG-727), deposit offset (REG-1350), invoice correction (REG-1053), cancellation/regeneration (REG-1391, REG-1392, REG-1398, REG-1399), claimed penalty (REG-1207).",
        "Confluence source: REST fallback (asterbit.atlassian.net/wiki). MCP timed out on an earlier pass; these pages were read by REST.",
        "Diagrams: Bundle 6 Updated 28012026 - Automatic offsetting.semantic.md and automatic offsetting-Occurrence Date Payment.semantic.md. Empty (continues...) branches are not rows.",
        "Priority: Highest is the offset, the block that stops it, or a diagram decision that changes what is offset. High is the documented link to the manual process. Low is an empty Confluence body.",
    ]
    for i, line in enumerate(lines, 1):
        ws.cell(i, 1, line)
    ws["A1"].font = Font(bold=True, size=14)
    header_row = 12
    ws.cell(header_row, 1, "Theme")
    for i, name in enumerate(["Total", "Yes", "Partial", "No", "Highest", "High", "Medium", "Low"], 2):
        ws.cell(header_row, i, name)
    for cell in ws[header_row]:
        if cell.value:
            cell.fill = HEADER_FILL
            cell.font = HEADER_FONT
    r = 13
    total = defaultdict(int)
    for theme in (AUTO, DIRECT):
        c = counts[theme]
        p = prios[theme]
        vals = [c["Total"], c["Yes"], c["Partial"], c["No"], p["Highest"], p["High"], p["Medium"], p["Low"]]
        ws.cell(r, 1, theme)
        for col, value in enumerate(vals, 2):
            ws.cell(r, col, value)
        total["Total"] += vals[0]
        total["Yes"] += vals[1]
        total["No"] += vals[3]
        r += 1
    ws.cell(r, 1, "ALL")
    ws.cell(r, 2, total["Total"])
    ws.cell(r, 3, total["Yes"])
    ws.cell(r, 4, 0)
    ws.cell(r, 5, total["No"])
    ws.cell(r, 6, prios["ALL"]["Highest"])
    ws.cell(r, 7, prios["ALL"]["High"])
    ws.cell(r, 8, prios["ALL"]["Medium"])
    ws.cell(r, 9, prios["ALL"]["Low"])
    for col in range(1, 10):
        ws.column_dimensions[get_column_letter(col)].width = 36

    write_sheet("00_Unmapped_Gaps", gaps)
    by_theme = defaultdict(list)
    for item in rows:
        by_theme[item["Theme"]].append(item)
    write_sheet(AUTO, by_theme[AUTO])
    write_sheet(DIRECT, by_theme[DIRECT])
    tc_headers, tc_rows = test_cases()
    write_tc_sheet(wb, "03_TC_Automatic", [item for item in tc_rows if item[0] == "Automatic"])
    write_tc_sheet(wb, "04_TC_Direct", [item for item in tc_rows if item[0] == "Direct"])
    ws.cell(r + 2, 1, "Test cases in 03_TC_Automatic and 04_TC_Direct: %s" % len(tc_rows))
    ws.cell(r + 3, 1, "A test case is one scenario. Amounts that a Playwright test asserts are copied from that test. Diagram cases use the diagram sentence as the expected result.")
    wb.save(OUT)
    OUT_MD.write_text(
        f"""# Automatic and Direct Offsetting — regression scope

**Workbook:** [`Offsetting_Automatic_And_Direct_Regression_Cases.xlsx`](Offsetting_Automatic_And_Direct_Regression_Cases.xlsx)

Rows: {total['Total']}. Automation Yes / No: {total['Yes']} / {total['No']}.

Automatic runs after a liability or a receivable is saved. Direct runs from a liability id plus a receivable, payment, deposit, or rescheduling id.

Sheets `03_TC_Automatic` and `04_TC_Direct` are the test cases for this scope. One scenario per row.

Jira source: MCP search. Confluence source: REST fallback (`asterbit.atlassian.net/wiki`).
""",
        encoding="utf-8",
    )
    print(f"rows={total['Total']} yes={total['Yes']} no={total['No']} file={OUT}")


if __name__ == "__main__":
    main()
