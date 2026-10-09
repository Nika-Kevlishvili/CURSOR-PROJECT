/**
 * PDT-3179 — E-mails for creation and reversal of reconnection invoices.
 *
 * Feature TCs TC-BE-1..13 + TC-BE-16 (Invoice Reversal billing run of reconnection).
 * Sequential in this file (fullyParallel false, not serial): one failure must not
 * skip the remaining tests. Tests PUT the shared default Taxes-for-grid-operator
 * and restore it — keep --workers=1 for this file.
 *
 * RFD is LIST_OF_CUSTOMERS + allSelected false + this test's POD (not Load PODs GET).
 * not ALL_CUSTOMERS and not allSelected true (select-all SQL + generateDocuments).
 * GET RFD has no taxCalculated (Swagger DPSRequestsResponse); assert FEE_CHARGED.
 *
 * Reference spec(s):
 * - tests/cursor/PDT-2861-rfd-pod-reconnection-fk.spec.ts
 * - tests/cursor/PDT-2880-pod-disconnected-banner.spec.ts
 * - tests/receivableManagement/massOperationForBlocking/blockingForReminderLetter.spec.ts
 * - tests/cursor/pdt-3171-easypay-proforma-due-date-validto.fixtures.ts
 * - tests/cursor/pdt-2971-rfd-shared-pod-multi-customer-merge.fixtures.ts
 * - tests/cursor/PDT-3219-lpf-reverse-document-no-billing-group.spec.ts
 */

import { test, expect } from './cursor-test.fixtures';
import type { TestRunSummaryCollector } from './shared/test-run-summary.fixtures';
import {
  finalizeTestRunSummary,
  buildProductContractTabLinks,
} from './shared/manual-verification-links.fixtures';
import { envVariables } from '../../fixtures/envCashed';
import {
  PDT_3179_KEY,
  PDT_3179_TITLE,
  asNumber,
  calculateTax,
  collectCustomerInvoices,
  countReconnectionInvoices,
  createExecutedDps,
  createExecutedRfd,
  customerIdentifier,
  deactivateCustomerEmailContacts,
  emailCommunicationId,
  emailHasAttachment,
  emailHasTopicInvoice,
  emailRecipientText,
  emailsCreatedAfter,
  entityId,
  errorHaystack,
  getDefaultGridTax,
  getEmail,
  getInvoice,
  getRfd,
  invoiceHasPdf,
  invoicesOfType,
  listEmails,
  pdt3179RelevantKeys,
  postDedicatedGridTax,
  postDpsRaw,
  postInactiveEmailTemplateClone,
  putExecutedDps,
  putGridTax,
  readHttpBody,
  restoreGridTax,
  runPdt3179ReceivableChain,
  completeReconnectionInvoiceReversalBillingRun,
  PDT_3179_TEST_TIMEOUT_MS,
  PDT_3179_REVERSAL_RUN_TIMEOUT_MS,
  type Pdt3179Fx,
  type Pdt3179TaxSnapshot,
  getDpsExpressFlag,
  reversalSourceId,
  emailTemplateIdOf,
  findLiabilityByInvoiceId,
} from './pdt-3179-reconnection-invoice-emails.fixtures';

const JIRA_TITLE = PDT_3179_TITLE;

function titleFor(tc: string, scenario: string): string {
  return `[${PDT_3179_KEY}]: ${JIRA_TITLE} | ${tc} – ${scenario}`;
}

async function attachSummary(
  TestRunSummary: TestRunSummaryCollector,
  Responses: Pdt3179Fx['Responses'],
  snapshot: Record<string, unknown>,
): Promise<void> {
  await test.step('Attach test run summary', async () => {
    TestRunSummary.registerPayload('customer', Responses.customer[0]);
    if (Responses.requestForDisconnection.length) {
      TestRunSummary.registerPayload('rfd', Responses.requestForDisconnection[0]);
    }
    if (Responses.disconnectionOfPowerSupply.length) {
      TestRunSummary.registerPayload('dps', Responses.disconnectionOfPowerSupply[0]);
    }
    let extra: Record<string, string[]> | undefined;
    try {
      extra = buildProductContractTabLinks(entityId(Responses.productContract[0]));
    } catch {
      extra = undefined;
    }
    finalizeTestRunSummary(TestRunSummary, Responses, {
      jiraKey: PDT_3179_KEY,
      relevantEntityKeys: pdt3179RelevantKeys(),
      extraLinks: extra && Object.keys(extra).length ? extra : undefined,
      snapshot,
    });
  });
}

test.describe(`[${PDT_3179_KEY}]: ${JIRA_TITLE}`, {
  tag: ['@dev', '@receivableManagement', '@pdt-3179'],
}, () => {
  test.describe.configure({ fullyParallel: false });

  test(titleFor('TC-BE-1', 'Calculate tax creates reconnection invoice and Email Communication'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3179_TEST_TIMEOUT_MS);
    const fx: Pdt3179Fx = { Request, GeneratePayload, Responses, Endpoints };
    let snapshot: Pdt3179TaxSnapshot | null = null;
    try {
      const email = 'pdt3179.tcbe1@example.com';
      await test.step('Precondition: supply chain + overdue manual liability with billing group + EXECUTED reminder', async () => {
        await runPdt3179ReceivableChain(fx, { emails: [email] });
      });
      snapshot = await test.step('Precondition: PUT default grid tax reconnection=10', async () => {
        const snap = await getDefaultGridTax(fx);
        await putGridTax(fx, snap.id, {
          ...snap.putPayload,
          taxForReconnection: 10,
          taxForExpressReconnection: 0,
        });
        return snap;
      });
      const requestId = await test.step('Precondition: EXECUTED RFD', async () =>
        createExecutedRfd(fx, { status: 'EXECUTED' }));
      TestRunSummary.registerPayload('customer', Responses.customer[0]);
      TestRunSummary.registerPayload('tax', { id: snapshot.id, taxForReconnection: 10 });

      const emailsBefore = await listEmails(fx, customerIdentifier(Responses));
      const calcRes = await test.step('Action: POST calculate-tax', async () => calculateTax(fx, requestId));
      await expect(calcRes).CheckResponse();

      const rfd = await getRfd(fx, requestId);
      const invoices = await collectCustomerInvoices(fx);
      const reconnection = invoicesOfType(invoices, 'RECONNECTION');
      const emailsAfter = await listEmails(fx, customerIdentifier(Responses));
      const newEmails = emailsCreatedAfter(emailsBefore, emailsAfter);
      const emailId = newEmails[0] ? emailCommunicationId(newEmails[0]) : null;
      const emailBody = emailId ? await getEmail(fx, emailId) : {};
      const recipients = emailRecipientText(emailBody);
      const recInvoice = reconnection[reconnection.length - 1];
      const passed =
        String(rfd.disconnectionRequestsStatus) === 'FEE_CHARGED' &&
        reconnection.length >= 1 &&
        String(recInvoice?.invoiceStatus) === 'REAL' &&
        newEmails.length === 1 &&
        recipients.includes(email) &&
        emailHasTopicInvoice(emailBody);

      TestRunSummary.recordCheck({
        check: 'TC-BE-1 calculate-tax reconnection invoice + Email topic Invoice',
        expectedResult:
          'HTTP 200; RFD FEE_CHARGED; REAL RECONNECTION invoice with PDF; one new Email Communication topic Invoice to pdt3179.tcbe1@example.com.',
        actualResult: passed
          ? `As expected — RFD ${String(rfd.disconnectionRequestsStatus)}, reconnection=${reconnection.length}, newEmails=${newEmails.length}, email=${emailId}.`
          : `Not as expected — RFD ${JSON.stringify({ status: rfd.disconnectionRequestsStatus })}, reconnection=${reconnection.length}, newEmails=${newEmails.length}, emailId=${emailId}.`,
        passed,
      });
      await attachSummary(TestRunSummary, Responses, {
        requestId,
        rfdStatus: rfd.disconnectionRequestsStatus,
        reconnectionCount: reconnection.length,
        emailId,
      });

      expect(String(rfd.disconnectionRequestsStatus)).toBe('FEE_CHARGED');
      expect(reconnection.length).toBeGreaterThanOrEqual(1);
      expect(String(recInvoice.invoiceStatus)).toBe('REAL');
      expect(String(recInvoice.invoiceDocumentType)).toBe('INVOICE');
      expect(invoiceHasPdf(recInvoice)).toBeTruthy();
      const recId = entityId(recInvoice);
      const recLiab = await findLiabilityByInvoiceId(fx, recId);
      if (recLiab) {
        const expectedAmt = asNumber(recInvoice.totalAmountIncludingVat ?? recInvoice.totalAmount);
        if (expectedAmt > 0) {
          expect(asNumber(recLiab.currentAmount)).toBe(expectedAmt);
        }
      }
      expect(emailId, 'Email Communication created').toBeTruthy();
      expect(newEmails.length).toBe(1);
      expect(emailHasTopicInvoice(emailBody)).toBeTruthy();
      expect(recipients).toContain(email);
      expect(emailHasAttachment(emailBody)).toBeTruthy();
      const tplId = emailTemplateIdOf(emailBody);
      if (tplId && snapshot) {
        expect(tplId).toBe(snapshot.putPayload.emailTemplateId);
      }
    } finally {
      if (snapshot) await restoreGridTax(fx, snapshot);
    }
  });

  test(titleFor('TC-BE-2', 'Two ACTIVE EMAILs aggregated with semicolon on one CRM row'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3179_TEST_TIMEOUT_MS);
    const fx: Pdt3179Fx = { Request, GeneratePayload, Responses, Endpoints };
    let snapshot: Pdt3179TaxSnapshot | null = null;
    try {
      const a = 'pdt3179.tcbe2a@example.com';
      const b = 'pdt3179.tcbe2b@example.com';
      await test.step('Precondition: supply chain with two EMAIL contacts', async () => {
        await runPdt3179ReceivableChain(fx, { emails: [a, b] });
      });
      snapshot = await getDefaultGridTax(fx);
      await putGridTax(fx, snapshot.id, { ...snapshot.putPayload, taxForReconnection: 10, taxForExpressReconnection: 0 });
      const requestId = await createExecutedRfd(fx);
      TestRunSummary.registerPayload('customer', Responses.customer[0]);

      const emailsBefore = await listEmails(fx, customerIdentifier(Responses));
      const calcRes = await calculateTax(fx, requestId);
      await expect(calcRes).CheckResponse();
      const emailsAfter = await listEmails(fx, customerIdentifier(Responses));
      const newEmails = emailsCreatedAfter(emailsBefore, emailsAfter);
      const emailBody = newEmails[0] ? await getEmail(fx, emailCommunicationId(newEmails[0])) : {};
      const recipients = emailRecipientText(emailBody);
      const reconnectionCount = await countReconnectionInvoices(fx);
      const passed =
        newEmails.length === 1 &&
        recipients.includes(a) &&
        recipients.includes(b) &&
        recipients.includes(';') &&
        reconnectionCount === 1;

      TestRunSummary.recordCheck({
        check: 'TC-BE-2 one Email Communication with both addresses joined by ;',
        expectedResult: `Exactly one new CRM row; recipients contain ${a} and ${b} separated by ;.`,
        actualResult: passed
          ? `As expected — newEmails=${newEmails.length}, recipients include both.`
          : `Not as expected — newEmails=${newEmails.length}, recipients=${recipients.slice(0, 400)}.`,
        passed,
      });
      await attachSummary(TestRunSummary, Responses, { requestId, newEmailCount: newEmails.length, reconnectionCount });
      expect(newEmails.length).toBe(1);
      expect(recipients).toContain(a);
      expect(recipients).toContain(b);
      expect(recipients).toContain(';');
      expect(emailHasAttachment(emailBody)).toBeTruthy();
      expect(reconnectionCount).toBe(1);
    } finally {
      if (snapshot) await restoreGridTax(fx, snapshot);
    }
  });

  test(titleFor('TC-BE-3', 'Express DPS creates extra reconnection invoice and email'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3179_TEST_TIMEOUT_MS);
    const fx: Pdt3179Fx = { Request, GeneratePayload, Responses, Endpoints };
    let snapshot: Pdt3179TaxSnapshot | null = null;
    try {
      const email = 'pdt3179.tcbe3@example.com';
      await runPdt3179ReceivableChain(fx, { emails: [email] });
      snapshot = await getDefaultGridTax(fx);
      await putGridTax(fx, snapshot.id, {
        ...snapshot.putPayload,
        taxForReconnection: 10,
        taxForExpressReconnection: 25,
      });
      // Same tax id as calculate-tax (default). A dedicated tax id is treated as a
      // tax change: reverse + new reconnection + express → 1→3 instead of +1.
      const requestId = await createExecutedRfd(fx);
      const calcRes = await calculateTax(fx, requestId);
      await expect(calcRes).CheckResponse();
      const invoicesBefore = invoicesOfType(await collectCustomerInvoices(fx), 'RECONNECTION');
      const emailsBefore = await listEmails(fx, customerIdentifier(Responses));
      TestRunSummary.registerPayload('tax', { defaultId: snapshot.id, taxForExpressReconnection: 25 });

      const dps = await test.step('Action: POST DPS expressReconnection=true', async () =>
        createExecutedDps(fx, { taxId: snapshot.id, express: true, requestId }));
      expect(dps.status).toBeGreaterThanOrEqual(200);
      expect(dps.status).toBeLessThan(300);

      const invoicesAfter = invoicesOfType(await collectCustomerInvoices(fx), 'RECONNECTION');
      const emailsAfter = await listEmails(fx, customerIdentifier(Responses));
      const expressInvoice = invoicesAfter.find((inv) => asNumber(inv.totalAmountExcludingVat) === 25);
      const passed =
        invoicesAfter.length === invoicesBefore.length + 1 &&
        Boolean(expressInvoice) &&
        emailsAfter.length >= emailsBefore.length + 1;

      TestRunSummary.recordCheck({
        check: 'TC-BE-3 express reconnection invoice 25.00 + email',
        expectedResult: 'New RECONNECTION invoice excluding VAT 25.00 and a new Email Communication; calculate-tax email remains.',
        actualResult: passed
          ? `As expected — reconnection ${invoicesBefore.length}→${invoicesAfter.length}, express exVAT=${expressInvoice?.totalAmountExcludingVat}.`
          : `Not as expected — reconnection ${invoicesBefore.length}→${invoicesAfter.length}.`,
        passed,
      });
      await attachSummary(TestRunSummary, Responses, { requestId, dpsId: dps.id, taxId: snapshot.id });
      expect(invoicesAfter.length).toBe(invoicesBefore.length + 1);
      expect(expressInvoice, 'express invoice exVAT 25.00').toBeTruthy();
      expect(emailsAfter.length).toBeGreaterThan(emailsBefore.length);
    } finally {
      if (snapshot) await restoreGridTax(fx, snapshot);
    }
  });

  test(titleFor('TC-BE-4', 'FEE_CHARGED DPS with different tax sends reverse and new-invoice emails'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3179_TEST_TIMEOUT_MS);
    const fx: Pdt3179Fx = { Request, GeneratePayload, Responses, Endpoints };
    let snapshot: Pdt3179TaxSnapshot | null = null;
    try {
      const email = 'pdt3179.tcbe4@example.com';
      await runPdt3179ReceivableChain(fx, { emails: [email] });
      snapshot = await getDefaultGridTax(fx);
      await putGridTax(fx, snapshot.id, { ...snapshot.putPayload, taxForReconnection: 10, taxForExpressReconnection: 0 });
      const taxIdB = await postDedicatedGridTax(fx, {
        costCenterControllingOrder: `PDT3179-BE4-${Date.now()}`,
        taxForReconnection: 20,
        taxForExpressReconnection: 0,
      });
      const requestId = await createExecutedRfd(fx);
      const calcRes = await calculateTax(fx, requestId);
      await expect(calcRes).CheckResponse();
      const invoicesAfterCharge = await collectCustomerInvoices(fx);
      const original = invoicesOfType(invoicesAfterCharge, 'RECONNECTION')[0];
      const originalInvoiceId = entityId(original);
      const emailCountAfterCharge = (await listEmails(fx, customerIdentifier(Responses))).length;
      TestRunSummary.registerPayload('tax', { taxIdA: snapshot.id, taxIdB });

      const dps = await test.step('Action: POST DPS with tax B (20.00)', async () =>
        createExecutedDps(fx, { taxId: taxIdB, express: false, requestId }));
      expect(dps.status).toBeGreaterThanOrEqual(200);
      expect(dps.status).toBeLessThan(300);

      const invoices = await collectCustomerInvoices(fx);
      const reversals = invoicesOfType(invoices, 'REVERSAL');
      const reconnections = invoicesOfType(invoices, 'RECONNECTION');
      const newReconnection = reconnections.find((inv) => asNumber(inv.totalAmountExcludingVat) === 20);
      const emailsAfter = await listEmails(fx, customerIdentifier(Responses));
      const passed =
        reversals.some((inv) => reversalSourceId(inv) === originalInvoiceId) &&
        Boolean(newReconnection) &&
        emailsAfter.length === emailCountAfterCharge + 2;

      TestRunSummary.recordCheck({
        check: 'TC-BE-4 reverse email + new reconnection email',
        expectedResult: 'Reversal of original; new RECONNECTION exVAT 20.00; Email Communication count +2.',
        actualResult: passed
          ? `As expected — reversals=${reversals.length}, emails ${emailCountAfterCharge}→${emailsAfter.length}.`
          : `Not as expected — reversals=${reversals.length}, emails ${emailCountAfterCharge}→${emailsAfter.length}.`,
        passed,
      });
      await attachSummary(TestRunSummary, Responses, { requestId, originalInvoiceId, taxIdB, emailCountAfterCharge });
      expect(dps.status).toBe(200);
      expect(reversals.some((inv) => reversalSourceId(inv) === originalInvoiceId)).toBeTruthy();
      expect(newReconnection, 'new reconnection 20.00').toBeTruthy();
      expect(emailsAfter.length).toBe(emailCountAfterCharge + 2);
    } finally {
      if (snapshot) await restoreGridTax(fx, snapshot);
    }
  });

  test(titleFor('TC-BE-5', 'FEE_CHARGED DPS with new tax 0 sends only reversal email'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3179_TEST_TIMEOUT_MS);
    const fx: Pdt3179Fx = { Request, GeneratePayload, Responses, Endpoints };
    let snapshot: Pdt3179TaxSnapshot | null = null;
    try {
      const email = 'pdt3179.tcbe5@example.com';
      await runPdt3179ReceivableChain(fx, { emails: [email] });
      snapshot = await getDefaultGridTax(fx);
      await putGridTax(fx, snapshot.id, { ...snapshot.putPayload, taxForReconnection: 10, taxForExpressReconnection: 0 });
      const taxIdZ = await postDedicatedGridTax(fx, {
        costCenterControllingOrder: `PDT3179-BE5-${Date.now()}`,
        taxForReconnection: 0,
        taxForExpressReconnection: 0,
      });
      const requestId = await createExecutedRfd(fx);
      const calcRes = await calculateTax(fx, requestId);
      await expect(calcRes).CheckResponse();
      const original = invoicesOfType(await collectCustomerInvoices(fx), 'RECONNECTION')[0];
      const originalInvoiceId = entityId(original);
      const emailsAfterCharge = await listEmails(fx, customerIdentifier(Responses));
      const emailCountAfterCharge = emailsAfterCharge.length;

      const dps = await createExecutedDps(fx, { taxId: taxIdZ, express: false, requestId });
      expect(dps.status).toBeGreaterThanOrEqual(200);
      expect(dps.status).toBeLessThan(300);

      const invoices = await collectCustomerInvoices(fx);
      const originalAfter = invoices.find((inv) => entityId(inv) === originalInvoiceId);
      const reversals = invoicesOfType(invoices, 'REVERSAL');
      const reconnections = invoicesOfType(invoices, 'RECONNECTION');
      const emailsAfter = await listEmails(fx, customerIdentifier(Responses));
      const newEmails = emailsCreatedAfter(emailsAfterCharge, emailsAfter);
      const newestBody = newEmails[0] ? await getEmail(fx, emailCommunicationId(newEmails[0])) : {};
      const passed =
        String(originalAfter?.invoiceType) === 'RECONNECTION' &&
        String(originalAfter?.invoiceStatus) === 'REAL' &&
        reversals.some((inv) => reversalSourceId(inv) === originalInvoiceId) &&
        reconnections.length === 1 &&
        newEmails.length === 1;

      TestRunSummary.recordCheck({
        check: 'TC-BE-5 tax 0 reverse only — no second RECONNECTION',
        expectedResult: 'Original RECONNECTION remains; one REVERSAL/CREDIT_NOTE; one new Email Communication; no second RECONNECTION.',
        actualResult: passed
          ? `As expected — reconnection=${reconnections.length}, newEmails=${newEmails.length}.`
          : `Not as expected — reconnection=${reconnections.length}, reversal=${reversals.length}, newEmails=${newEmails.length}.`,
        passed,
      });
      await attachSummary(TestRunSummary, Responses, { requestId, originalInvoiceId, taxIdZ });
      expect(String(originalAfter?.invoiceType)).toBe('RECONNECTION');
      expect(String(originalAfter?.invoiceStatus)).toBe('REAL');
      expect(reversals.some((inv) => reversalSourceId(inv) === originalInvoiceId)).toBeTruthy();
      expect(String(reversals[0]?.invoiceDocumentType)).toBe('CREDIT_NOTE');
      expect(reconnections.length).toBe(1);
      expect(emailsAfter.length).toBe(emailCountAfterCharge + 1);
      expect(newEmails.length).toBe(1);
      expect(emailRecipientText(newestBody)).toContain(email);
    } finally {
      if (snapshot) await restoreGridTax(fx, snapshot);
    }
  });

  test(titleFor('TC-BE-6', 'DRAFT RFD calculate-tax rejected with status is not executed'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3179_TEST_TIMEOUT_MS);
    const fx: Pdt3179Fx = { Request, GeneratePayload, Responses, Endpoints };
    let snapshot: Pdt3179TaxSnapshot | null = null;
    try {
      await runPdt3179ReceivableChain(fx, { emails: ['pdt3179.tcbe6@example.com'] });
      snapshot = await getDefaultGridTax(fx);
      await putGridTax(fx, snapshot.id, { ...snapshot.putPayload, taxForReconnection: 10, taxForExpressReconnection: 0 });
      const requestId = await createExecutedRfd(fx, { status: 'DRAFT' });
      const emailCountBefore = (await listEmails(fx, customerIdentifier(Responses))).length;
      const reconnectionBefore = await countReconnectionInvoices(fx);

      const calcRes = await calculateTax(fx, requestId);
      const err = await readHttpBody(calcRes);
      const hay = errorHaystack(err.status, err.text, err.json);
      const rfd = await getRfd(fx, requestId);
      const passed =
        err.status === 400 &&
        hay.toLowerCase().includes('status is not executed') &&
        String(rfd.disconnectionRequestsStatus) === 'DRAFT';

      TestRunSummary.recordCheck({
        check: 'TC-BE-6 calculate-tax on DRAFT RFD',
        expectedResult: 'HTTP 400 message contains "status is not executed!"; RFD stays DRAFT; no reconnection invoice/email.',
        actualResult: passed
          ? `As expected — ${err.status} ${err.text.slice(0, 200)}`
          : `Not as expected — ${err.status} ${err.text.slice(0, 300)}`,
        passed,
      });
      await attachSummary(TestRunSummary, Responses, { requestId, status: err.status, rfdStatus: rfd.disconnectionRequestsStatus });
      expect(err.status).toBe(400);
      expect(hay.toLowerCase()).toContain('status is not executed');
      expect(hay).toMatch(/OperationNotAllowedException/i);
      expect(String(rfd.disconnectionRequestsStatus)).toBe('DRAFT');
      expect(await countReconnectionInvoices(fx)).toBe(reconnectionBefore);
      expect((await listEmails(fx, customerIdentifier(Responses))).length).toBe(emailCountBefore);
    } finally {
      if (snapshot) await restoreGridTax(fx, snapshot);
    }
  });

  test(titleFor('TC-BE-7', 'Second calculate-tax rejected with Fee already charged'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3179_TEST_TIMEOUT_MS);
    const fx: Pdt3179Fx = { Request, GeneratePayload, Responses, Endpoints };
    let snapshot: Pdt3179TaxSnapshot | null = null;
    try {
      await runPdt3179ReceivableChain(fx, { emails: ['pdt3179.tcbe7@example.com'] });
      snapshot = await getDefaultGridTax(fx);
      await putGridTax(fx, snapshot.id, { ...snapshot.putPayload, taxForReconnection: 10, taxForExpressReconnection: 0 });
      const requestId = await createExecutedRfd(fx);
      const first = await calculateTax(fx, requestId);
      await expect(first).CheckResponse();
      const invoiceCountAfterFirst = (await collectCustomerInvoices(fx)).length;
      const emailCountAfterFirst = (await listEmails(fx, customerIdentifier(Responses))).length;

      const second = await calculateTax(fx, requestId);
      const err = await readHttpBody(second);
      const hay = errorHaystack(err.status, err.text, err.json);
      const rfd = await getRfd(fx, requestId);
      const passed =
        err.status === 400 &&
        hay.toLowerCase().includes('fee already charged') &&
        String(rfd.disconnectionRequestsStatus) === 'FEE_CHARGED';

      TestRunSummary.recordCheck({
        check: 'TC-BE-7 second calculate-tax rejected',
        expectedResult: 'HTTP 400 "Fee already charged!"; invoice and email counts unchanged.',
        actualResult: passed ? `As expected — ${err.status}` : `Not as expected — ${err.status} ${err.text.slice(0, 300)}`,
        passed,
      });
      await attachSummary(TestRunSummary, Responses, { requestId, status: err.status });
      expect(err.status).toBe(400);
      expect(hay.toLowerCase()).toContain('fee already charged');
      expect(hay).toMatch(/OperationNotAllowedException/i);
      expect(String(rfd.disconnectionRequestsStatus)).toBe('FEE_CHARGED');
      expect((await collectCustomerInvoices(fx)).length).toBe(invoiceCountAfterFirst);
      expect((await listEmails(fx, customerIdentifier(Responses))).length).toBe(emailCountAfterFirst);
    } finally {
      if (snapshot) await restoreGridTax(fx, snapshot);
    }
  });

  test(titleFor('TC-BE-8', 'Zero reconnection tax creates no invoice and no email'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3179_TEST_TIMEOUT_MS);
    const fx: Pdt3179Fx = { Request, GeneratePayload, Responses, Endpoints };
    let snapshot: Pdt3179TaxSnapshot | null = null;
    try {
      await runPdt3179ReceivableChain(fx, { emails: ['pdt3179.tcbe8@example.com'] });
      snapshot = await getDefaultGridTax(fx);
      await putGridTax(fx, snapshot.id, { ...snapshot.putPayload, taxForReconnection: 0, taxForExpressReconnection: 0 });
      const requestId = await createExecutedRfd(fx);
      const reconnectionBefore = await countReconnectionInvoices(fx);
      const emailCountBefore = (await listEmails(fx, customerIdentifier(Responses))).length;

      const calcRes = await calculateTax(fx, requestId);
      await expect(calcRes).CheckResponse();
      const rfd = await getRfd(fx, requestId);
      const reconnectionAfter = await countReconnectionInvoices(fx);
      const emailCountAfter = (await listEmails(fx, customerIdentifier(Responses))).length;
      const passed =
        String(rfd.disconnectionRequestsStatus) === 'FEE_CHARGED' &&
        reconnectionAfter === reconnectionBefore &&
        emailCountAfter === emailCountBefore;

      TestRunSummary.recordCheck({
        check: 'TC-BE-8 zero tax — FEE_CHARGED without reconnection invoice/email',
        expectedResult: 'HTTP 200; RFD FEE_CHARGED; no new RECONNECTION invoice or Email Communication.',
        actualResult: passed
          ? `As expected — status=${String(rfd.disconnectionRequestsStatus)}, reconnection ${reconnectionBefore}→${reconnectionAfter}.`
          : `Not as expected — status=${String(rfd.disconnectionRequestsStatus)}.`,
        passed,
      });
      await attachSummary(TestRunSummary, Responses, { requestId, rfdStatus: rfd.disconnectionRequestsStatus });
      expect(String(rfd.disconnectionRequestsStatus)).toBe('FEE_CHARGED');
      expect(reconnectionAfter).toBe(reconnectionBefore);
      expect(emailCountAfter).toBe(emailCountBefore);
    } finally {
      if (snapshot) await restoreGridTax(fx, snapshot);
    }
  });

  test(titleFor('TC-BE-9', 'No ACTIVE EMAIL keeps invoice and skips Email Communication'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3179_TEST_TIMEOUT_MS);
    const fx: Pdt3179Fx = { Request, GeneratePayload, Responses, Endpoints };
    let snapshot: Pdt3179TaxSnapshot | null = null;
    try {
      await runPdt3179ReceivableChain(fx, { phoneOnly: true });
      snapshot = await getDefaultGridTax(fx);
      await putGridTax(fx, snapshot.id, { ...snapshot.putPayload, taxForReconnection: 10, taxForExpressReconnection: 0 });
      await test.step('Precondition: deactivate EMAIL contacts (keep MOBILE_NUMBER)', async () => {
        await deactivateCustomerEmailContacts(fx);
      });
      const requestId = await createExecutedRfd(fx);
      const emailCountBefore = (await listEmails(fx, customerIdentifier(Responses))).length;

      const calcRes = await calculateTax(fx, requestId);
      await expect(calcRes).CheckResponse();
      const reconnection = invoicesOfType(await collectCustomerInvoices(fx), 'RECONNECTION');
      const emailCountAfter = (await listEmails(fx, customerIdentifier(Responses))).length;
      const passed = reconnection.length >= 1 && emailCountAfter === emailCountBefore;

      TestRunSummary.recordCheck({
        check: 'TC-BE-9 invoice kept, no Email Communication',
        expectedResult: 'RECONNECTION REAL invoice exists; Email Communication count unchanged (no "not sent" row).',
        actualResult: passed
          ? `As expected — reconnection=${reconnection.length}, emails ${emailCountBefore}→${emailCountAfter}.`
          : `Not as expected — reconnection=${reconnection.length}, emails ${emailCountBefore}→${emailCountAfter}.`,
        passed,
      });
      await attachSummary(TestRunSummary, Responses, { requestId, emailCountBefore, emailCountAfter });
      expect(reconnection.length).toBeGreaterThanOrEqual(1);
      expect(String(reconnection[0].invoiceStatus)).toBe('REAL');
      expect(emailCountAfter).toBe(emailCountBefore);
    } finally {
      if (snapshot) await restoreGridTax(fx, snapshot);
    }
  });

  test(titleFor('TC-BE-10', 'FEE_CHARGED DPS with same taxId does not reverse or re-email'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3179_TEST_TIMEOUT_MS);
    const fx: Pdt3179Fx = { Request, GeneratePayload, Responses, Endpoints };
    let snapshot: Pdt3179TaxSnapshot | null = null;
    try {
      await runPdt3179ReceivableChain(fx, { emails: ['pdt3179.tcbe10@example.com'] });
      snapshot = await getDefaultGridTax(fx);
      await putGridTax(fx, snapshot.id, { ...snapshot.putPayload, taxForReconnection: 10, taxForExpressReconnection: 0 });
      const requestId = await createExecutedRfd(fx);
      const calcRes = await calculateTax(fx, requestId);
      await expect(calcRes).CheckResponse();
      const original = invoicesOfType(await collectCustomerInvoices(fx), 'RECONNECTION')[0];
      const originalInvoiceId = entityId(original);
      const invoiceCountAfterCharge = (await collectCustomerInvoices(fx)).length;
      const emailCountAfterCharge = (await listEmails(fx, customerIdentifier(Responses))).length;

      const dps = await createExecutedDps(fx, { taxId: snapshot.id, express: false, requestId });
      expect(dps.status).toBeGreaterThanOrEqual(200);
      expect(dps.status).toBeLessThan(300);

      const originalAfter = await getInvoice(fx, originalInvoiceId);
      const invoices = await collectCustomerInvoices(fx);
      const emailsAfter = await listEmails(fx, customerIdentifier(Responses));
      const passed =
        String(originalAfter.invoiceType) === 'RECONNECTION' &&
        String(originalAfter.invoiceStatus) === 'REAL' &&
        invoices.length === invoiceCountAfterCharge &&
        emailsAfter.length === emailCountAfterCharge;

      TestRunSummary.recordCheck({
        check: 'TC-BE-10 same taxId — no reverse, no extra email',
        expectedResult: 'Original REAL reconnection unchanged; invoice and email counts unchanged.',
        actualResult: passed
          ? `As expected — original ${String(originalAfter.invoiceStatus)}, invoices ${invoiceCountAfterCharge}→${invoices.length}.`
          : `Not as expected — type=${String(originalAfter.invoiceType)} invoices ${invoiceCountAfterCharge}→${invoices.length}.`,
        passed,
      });
      await attachSummary(TestRunSummary, Responses, { requestId, originalInvoiceId, taxIdA: snapshot.id });
      expect(String(originalAfter.invoiceType)).toBe('RECONNECTION');
      expect(String(originalAfter.invoiceStatus)).toBe('REAL');
      expect(invoices.length).toBe(invoiceCountAfterCharge);
      expect(emailsAfter.length).toBe(emailCountAfterCharge);
    } finally {
      if (snapshot) await restoreGridTax(fx, snapshot);
    }
  });

  test(titleFor('TC-BE-11', 'INACTIVE email template keeps invoice and skips Email Communication'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary, FileUploadRequest,
  }) => {
    test.setTimeout(PDT_3179_TEST_TIMEOUT_MS);
    const fx: Pdt3179Fx = { Request, GeneratePayload, Responses, Endpoints, FileUploadRequest };
    let snapshot: Pdt3179TaxSnapshot | null = null;
    let restoreTemplate: (() => Promise<void>) | null = null;
    try {
      await runPdt3179ReceivableChain(fx, { emails: ['pdt3179.tcbe11@example.com'] });
      snapshot = await getDefaultGridTax(fx);
      const taxSnapshot = snapshot;
      const sourceTemplateId = taxSnapshot.putPayload.emailTemplateId || Number(envVariables.invoice_email_template);
      const cloned = await test.step('Precondition: clone tax email template as ACTIVE (source purpose)', async () =>
        postInactiveEmailTemplateClone(fx, sourceTemplateId));
      restoreTemplate = cloned.restore;
      await test.step('Precondition: PUT default grid tax with clone id while clone is ACTIVE', async () => {
        await putGridTax(fx, taxSnapshot.id, {
          ...taxSnapshot.putPayload,
          taxForReconnection: 10,
          taxForExpressReconnection: 0,
          emailTemplateId: cloned.templateId,
        });
      });
      await test.step('Precondition: PUT cloned email template INACTIVE', async () => {
        await cloned.deactivate();
      });
      const requestId = await createExecutedRfd(fx);
      const emailCountBefore = (await listEmails(fx, customerIdentifier(Responses))).length;
      TestRunSummary.registerPayload('tax', { emailTemplateId: cloned.templateId, templateStatus: 'INACTIVE' });

      const calcRes = await calculateTax(fx, requestId);
      await expect(calcRes).CheckResponse();
      const reconnection = invoicesOfType(await collectCustomerInvoices(fx), 'RECONNECTION');
      const emailCountAfter = (await listEmails(fx, customerIdentifier(Responses))).length;
      const passed = reconnection.length >= 1 && emailCountAfter === emailCountBefore;

      TestRunSummary.recordCheck({
        check: 'TC-BE-11 INACTIVE template — invoice kept, no email',
        expectedResult: 'RECONNECTION REAL invoice + liability remain; Email Communication count unchanged.',
        actualResult: passed
          ? `As expected — reconnection=${reconnection.length}, emails unchanged.`
          : `Not as expected — reconnection=${reconnection.length}, emails ${emailCountBefore}→${emailCountAfter}.`,
        passed,
      });
      await attachSummary(TestRunSummary, Responses, { requestId, templateId: cloned.templateId });
      expect(reconnection.length).toBeGreaterThanOrEqual(1);
      expect(String(reconnection[0].invoiceStatus)).toBe('REAL');
      expect(emailCountAfter).toBe(emailCountBefore);
    } finally {
      if (restoreTemplate) await restoreTemplate();
      if (snapshot) await restoreGridTax(fx, snapshot);
    }
  });

  test(titleFor('TC-BE-12', 'Cannot uncheck express reconnection on EXECUTED DPS'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3179_TEST_TIMEOUT_MS);
    const fx: Pdt3179Fx = { Request, GeneratePayload, Responses, Endpoints };
    let snapshot: Pdt3179TaxSnapshot | null = null;
    try {
      await runPdt3179ReceivableChain(fx, { emails: ['pdt3179.tcbe12@example.com'] });
      snapshot = await getDefaultGridTax(fx);
      await putGridTax(fx, snapshot.id, {
        ...snapshot.putPayload,
        taxForReconnection: 10,
        taxForExpressReconnection: 25,
      });
      const requestId = await createExecutedRfd(fx);
      const calcRes = await calculateTax(fx, requestId);
      await expect(calcRes).CheckResponse();
      const dps = await createExecutedDps(fx, { taxId: snapshot.id, express: true, requestId });
      expect(dps.id, 'DPS id').toBeTruthy();
      const emailCountAfterExpress = (await listEmails(fx, customerIdentifier(Responses))).length;
      const reconnectionAfterExpress = await countReconnectionInvoices(fx);

      const putErr = await test.step('Action: PUT DPS expressReconnection=false', async () =>
        putExecutedDps(fx, dps.id as number, { taxId: snapshot.id, express: false, requestId }));
      const hay = errorHaystack(putErr.status, putErr.text, putErr.json);
      const passed = putErr.status === 400 && hay.toLowerCase().includes('not possible to uncheck express reconnection');

      TestRunSummary.recordCheck({
        check: 'TC-BE-12 cannot uncheck express on EXECUTED DPS',
        expectedResult: 'HTTP 400 message contains "Not possible to uncheck express reconnection"; counts unchanged.',
        actualResult: passed ? `As expected — ${putErr.status}` : `Not as expected — ${putErr.status} ${putErr.text.slice(0, 300)}`,
        passed,
      });
      await attachSummary(TestRunSummary, Responses, { requestId, dpsId: dps.id, status: putErr.status });
      expect(putErr.status).toBe(400);
      expect(hay.toLowerCase()).toContain('not possible to uncheck express reconnection');
      expect(hay).toMatch(/ClientException/i);
      expect((await listEmails(fx, customerIdentifier(Responses))).length).toBe(emailCountAfterExpress);
      expect(await countReconnectionInvoices(fx)).toBe(reconnectionAfterExpress);
      expect(await getDpsExpressFlag(fx, dps.id as number)).toBe(true);
    } finally {
      if (snapshot) await restoreGridTax(fx, snapshot);
    }
  });

  test(titleFor('TC-BE-13', 'TC-F1 Zero Charge fee then DPS with positive tax creates first reconnection invoice'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3179_TEST_TIMEOUT_MS);
    const fx: Pdt3179Fx = { Request, GeneratePayload, Responses, Endpoints };
    let snapshot: Pdt3179TaxSnapshot | null = null;
    try {
      await runPdt3179ReceivableChain(fx, { emails: ['pdt3179.tcf1@example.com'] });
      snapshot = await getDefaultGridTax(fx);
      await putGridTax(fx, snapshot.id, { ...snapshot.putPayload, taxForReconnection: 0, taxForExpressReconnection: 0 });
      const taxIdPositive = await postDedicatedGridTax(fx, {
        costCenterControllingOrder: 'PDT3179-F1-CC',
        taxForReconnection: 10,
        taxForExpressReconnection: 0,
      });
      const requestId = await createExecutedRfd(fx);
      const calcRes = await calculateTax(fx, requestId);
      await expect(calcRes).CheckResponse();
      const rfdAfterCalc = await getRfd(fx, requestId);
      expect(String(rfdAfterCalc.disconnectionRequestsStatus)).toBe('FEE_CHARGED');
      expect(await countReconnectionInvoices(fx)).toBe(0);
      TestRunSummary.registerPayload('tax', { defaultZero: true, taxIdPositive });

      const emailsBeforeDps = await listEmails(fx, customerIdentifier(Responses));
      const dpsRaw = await test.step('Action: POST DPS with positive tax (may 500 if defect)', async () =>
        postDpsRaw(fx, { taxId: taxIdPositive, express: false, requestId }));
      const invoices = await collectCustomerInvoices(fx);
      const reconnection = invoicesOfType(invoices, 'RECONNECTION');
      const reversals = invoicesOfType(invoices, 'REVERSAL');
      const emailsAfterF1 = await listEmails(fx, customerIdentifier(Responses));
      const newEmails = emailsCreatedAfter(emailsBeforeDps, emailsAfterF1);
      const productCorrect =
        dpsRaw.status === 200 &&
        reconnection.length === 1 &&
        String(reconnection[0]?.invoiceStatus) === 'REAL' &&
        reversals.length === 0;

      TestRunSummary.recordCheck({
        check: 'TC-BE-13 / TC-F1 first reconnection after zero calculate-tax',
        expectedResult:
          'HTTP 200; exactly one REAL RECONNECTION (exVAT 10.00); no REVERSAL. FAIL if 500 / Invoice not found / null savedInvoiceId.',
        actualResult: productCorrect
          ? `As expected — DPS ${dpsRaw.status}, reconnection=${reconnection.length}.`
          : `Not as expected — DPS ${dpsRaw.status} ${dpsRaw.text.slice(0, 300)}; reconnection=${reconnection.length}, reversal=${reversals.length}.`,
        passed: productCorrect,
      });
      await attachSummary(TestRunSummary, Responses, {
        requestId,
        dpsStatus: dpsRaw.status,
        dpsBody: dpsRaw.text.slice(0, 500),
        reconnectionCount: reconnection.length,
      });
      expect(
        dpsRaw.status,
        `DPS must be HTTP 200 (product-correct). Got ${dpsRaw.status}: ${dpsRaw.text.slice(0, 400)}`,
      ).toBe(200);
      expect(reconnection.length).toBe(1);
      expect(String(reconnection[0].invoiceStatus)).toBe('REAL');
      expect(asNumber(reconnection[0].totalAmountExcludingVat)).toBe(10);
      expect(reversals.length).toBe(0);
      expect(newEmails.length, 'new Email Communication after DPS, not leftover CRM rows').toBeGreaterThan(0);
      expect(String((await getRfd(fx, requestId)).disconnectionRequestsStatus)).toBe('FEE_CHARGED');
    } finally {
      if (snapshot) await restoreGridTax(fx, snapshot);
    }
  });

  test(titleFor('TC-BE-16', 'Invoice Reversal billing run of reconnection invoice sends Email Communication'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3179_REVERSAL_RUN_TIMEOUT_MS);
    const fx: Pdt3179Fx = { Request, GeneratePayload, Responses, Endpoints };
    let snapshot: Pdt3179TaxSnapshot | null = null;
    try {
      const email = 'pdt3179.tcbe16@example.com';
      await runPdt3179ReceivableChain(fx, { emails: [email] });
      snapshot = await getDefaultGridTax(fx);
      await putGridTax(fx, snapshot.id, {
        ...snapshot.putPayload,
        taxForReconnection: 10,
        taxForExpressReconnection: 0,
      });
      const requestId = await createExecutedRfd(fx);
      const calcRes = await calculateTax(fx, requestId);
      await expect(calcRes).CheckResponse();

      const invoicesAfterCharge = await collectCustomerInvoices(fx);
      const reconnection = invoicesOfType(invoicesAfterCharge, 'RECONNECTION');
      expect(reconnection.length).toBeGreaterThanOrEqual(1);
      const original = reconnection[reconnection.length - 1];
      const originalInvoiceId = entityId(original);
      expect(String(original.invoiceStatus)).toBe('REAL');
      const emailsAfterCharge = await listEmails(fx, customerIdentifier(Responses));
      TestRunSummary.registerPayload('invoice', original);

      const billingRunId = await test.step('Action: INVOICE_REVERSAL billing run of reconnection invoice', async () =>
        completeReconnectionInvoiceReversalBillingRun(fx, originalInvoiceId));

      const invoicesAfter = await collectCustomerInvoices(fx);
      const reversals = invoicesOfType(invoicesAfter, 'REVERSAL');
      const matchingReversal = reversals.find((inv) => reversalSourceId(inv) === originalInvoiceId);
      const emailsAfter = await listEmails(fx, customerIdentifier(Responses));
      const newEmails = emailsCreatedAfter(emailsAfterCharge, emailsAfter);
      const emailId = newEmails[0] ? emailCommunicationId(newEmails[0]) : null;
      const emailBody = emailId ? await getEmail(fx, emailId) : {};
      const recipients = emailRecipientText(emailBody);
      const passed =
        Boolean(matchingReversal) &&
        String(matchingReversal?.invoiceDocumentType) === 'CREDIT_NOTE' &&
        newEmails.length >= 1 &&
        emailHasTopicInvoice(emailBody) &&
        recipients.includes(email);

      TestRunSummary.recordCheck({
        check: 'TC-BE-16 Invoice Reversal billing run of RECONNECTION sends Email Communication',
        expectedResult:
          'CREDIT_NOTE reversal of the reconnection invoice; at least one new Email Communication (topic Invoice) to pdt3179.tcbe16@example.com.',
        actualResult: passed
          ? `As expected — billingRun=${billingRunId}, reversal=${entityId(matchingReversal!)}, newEmails=${newEmails.length}, email=${emailId}.`
          : `Not as expected — billingRun=${billingRunId}, reversals=${reversals.length}, matching=${Boolean(matchingReversal)}, emails ${emailsAfterCharge.length}→${emailsAfter.length}, emailId=${emailId}.`,
        passed,
      });
      await attachSummary(TestRunSummary, Responses, {
        requestId,
        originalInvoiceId,
        billingRunId,
        reversalId: matchingReversal ? entityId(matchingReversal) : null,
        emailId,
        emailsBefore: emailsAfterCharge.length,
        emailsAfter: emailsAfter.length,
      });

      expect(matchingReversal, 'CREDIT_NOTE reversal linked to reconnection invoice').toBeTruthy();
      expect(String(matchingReversal!.invoiceDocumentType)).toBe('CREDIT_NOTE');
      expect(
        emailId,
        'Invoice Reversal of a reconnection invoice must create Email Communication (PDT-3179). Missing hook in processReconnectionInvoice.',
      ).toBeTruthy();
      expect(newEmails.length).toBeGreaterThanOrEqual(1);
      expect(emailHasTopicInvoice(emailBody)).toBeTruthy();
      expect(recipients).toContain(email);
    } finally {
      if (snapshot) await restoreGridTax(fx, snapshot);
    }
  });
});
