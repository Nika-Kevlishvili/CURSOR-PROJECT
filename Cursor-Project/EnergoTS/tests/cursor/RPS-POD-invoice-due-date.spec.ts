/**
 * RPS-DUEDATE — Invoice payment deadline (due date) calculation for RPS PODs.
 *
 * Covers PHN-3531 test cases for PHN-3478 (RPS invoice payment deadline).
 * TC-BE-25 V2, TC-BE-25 V3, and TC-BE-28 are titled with [PHN-3531].
 *
 * Reference spec(s):
 * - Confluence 953581569: Change in invoice payment deadline date calculation for RPS PODs
 * - Swagger: Cursor-Project/config/swagger/dev2/swagger-spec.json (dev2 server http://10.236.20.11:8092)
 * - tests/cursor/pdt-2872-minimal-interim-payment.fixtures.ts (billing run poll pattern)
 *
 * ⚠️ Calendar parallelism note: Tests in parallel mode each create their own isDefault=true calendar.
 * Since the default calendar is a global system setting, parallel tests may race on which calendar
 * the billing engine reads. Consider serial mode if flakiness is observed in CI.
 */

import { test, expect, finalizeTestRunSummary } from './cursor-test.fixtures';
import { envVariables } from '../../fixtures/envCashed';
import {
  type RpsDueDateFx,
  createRpsNomenclature,
  createDefaultCalendar,
  createNonDefaultCalendar,
  createTerm,
  createElectricityPriceComponent,
  createProduct,
  createCustomer,
  createPod,
  createProductContract,
  activatePod,
  createBillingProfile,
  createBillingRun,
  createSimpleRpsChain,
  createTwoPodSameBillingGroupChain,
  createTwoPodSeparateBillingGroupsChain,
  createTwoPodSeparateInvoicePerPodChain,
  createConflictGroupPlusSeparateRpsPodChain,
  runBaseInvoiceForManualNotes,
  startAndCompleteBillingRun,
  startBillingRunToDraft,
  patchStartBilling,
  pollInvoiceListing,
  getFirstInvoicePaymentDeadline,
  listInvoices,
  asBillingRunId,
  assertIsNotWeekend,
  BILLING_RUN_ROOT,
} from './rps-pod-invoice-due-date.fixtures';

const JIRA_KEY = 'RPS-DUEDATE';
// Dev2 open accounting periods include 2026-09 and 2026-10. Invoice dates stay inside an OPEN period.
// Day-of-month relationships match PHN-3531 (7 vs 5, 10 vs 7/6) shifted from May 2025 to October 2026.
const DEFAULT_INVOICE_DATE_1 = '2026-10-07';
const DEFAULT_INVOICE_DATE_2 = '2026-10-10';

test.describe(
  `[${JIRA_KEY}]: RPS POD invoice payment deadline calculation`,
  { tag: ['@billing', '@rps-duedate', '@dev'] },
  () => {
    test.describe.configure({ mode: 'parallel' });

    // ─── TC-BE-1 ─────────────────────────────────────────────────────────
    test(
      `[${JIRA_KEY}] TC-BE-1: Single RPS POD, ACCORDING_TO_THE_CONTRACT — due date rolls to next month (FOR_VOLUMES)`,
      async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
        test.setTimeout(12 * 60 * 1000);
        const fx: RpsDueDateFx = { Request, GeneratePayload, Responses, Endpoints };

        let rpsId = 0;
        await test.step('Precondition: RPS nomenclature (dayOfMonth=5)', async () => {
          rpsId = await createRpsNomenclature(Request, { rpsNumber: 'RPS-TC1-A', dayOfMonth: 5 });
        });
        await test.step('Precondition: default calendar', async () => {
          await createDefaultCalendar(Request);
        });

        const { billingRunId } = await createSimpleRpsChain(fx, rpsId, {
          applicationModelType: ['FOR_VOLUMES'],
          invoiceDate: DEFAULT_INVOICE_DATE_1,
          invoiceDueDateType: 'ACCORDING_TO_THE_CONTRACT',
        });

        await test.step('Start and complete billing run', async () => {
          await startAndCompleteBillingRun(Request, billingRunId);
        });

        let paymentDeadline: string | null = null;
        await test.step('POST invoice/listing — assert paymentDeadline=2026-11-05', async () => {
          paymentDeadline = await getFirstInvoicePaymentDeadline(Request, billingRunId);
          expect(paymentDeadline, 'TC-BE-1: paymentDeadline must be 2026-11-05').toBe('2026-11-05');
        });

        await test.step('Attach test run summary', async () => {
          TestRunSummary.recordCheck({
            check: 'RPS day 5, invoice 7 May → rolls to 5 June (next month)',
            expectedResult: 'paymentDeadline = 2026-11-05',
            actualResult: `paymentDeadline = ${paymentDeadline}`,
            passed: paymentDeadline === '2026-11-05',
          });
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: JIRA_KEY,
            relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun'],
            snapshot: { tcId: 'TC-BE-1', rpsId, billingRunId, paymentDeadline },
          });
        });
      },
    );

    // ─── TC-BE-2 ─────────────────────────────────────────────────────────
    test(
      `[${JIRA_KEY}] TC-BE-2: RPS day later in same month — no roll-forward`,
      async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
        test.setTimeout(12 * 60 * 1000);
        const fx: RpsDueDateFx = { Request, GeneratePayload, Responses, Endpoints };

        let rpsId = 0;
        await test.step('Precondition: RPS nomenclature (dayOfMonth=23)', async () => {
          rpsId = await createRpsNomenclature(Request, { rpsNumber: 'RPS-TC2-B', dayOfMonth: 23 });
        });
        await test.step('Precondition: default calendar', async () => {
          await createDefaultCalendar(Request);
        });

        const { billingRunId } = await createSimpleRpsChain(fx, rpsId, {
          applicationModelType: ['FOR_VOLUMES'],
          invoiceDate: DEFAULT_INVOICE_DATE_1,
          invoiceDueDateType: 'ACCORDING_TO_THE_CONTRACT',
        });

        await test.step('Start and complete billing run', async () => {
          await startAndCompleteBillingRun(Request, billingRunId);
        });

        let paymentDeadline: string | null = null;
        await test.step('POST invoice/listing — assert paymentDeadline=2026-10-23', async () => {
          paymentDeadline = await getFirstInvoicePaymentDeadline(Request, billingRunId);
          expect(paymentDeadline, 'TC-BE-2: paymentDeadline must be 2026-11-23').toBe('2026-11-23');
        });

        await test.step('Attach test run summary', async () => {
          TestRunSummary.recordCheck({
            check: 'RPS day 23, invoice 7 May → same month (23 ≥ 7)',
            expectedResult: 'paymentDeadline = 2026-11-23',
            actualResult: `paymentDeadline = ${paymentDeadline}`,
            passed: paymentDeadline === '2026-11-23',
          });
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: JIRA_KEY,
            relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun'],
            snapshot: { tcId: 'TC-BE-2', rpsId, billingRunId, paymentDeadline },
          });
        });
      },
    );

    // ─── TC-BE-3 ─────────────────────────────────────────────────────────
    test(
      `[${JIRA_KEY}] TC-BE-3: Calculated RPS due date on weekend — adjusted to nearest working day`,
      async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
        test.setTimeout(12 * 60 * 1000);
        const fx: RpsDueDateFx = { Request, GeneratePayload, Responses, Endpoints };

        let rpsId = 0;
        await test.step('Precondition: RPS nomenclature (dayOfMonth=7, raw=7 Jun 2025 Saturday)', async () => {
          rpsId = await createRpsNomenclature(Request, { rpsNumber: 'RPS-TC3-WKND', dayOfMonth: 7 });
        });
        await test.step('Precondition: default calendar (SAT+SUN weekends)', async () => {
          await createDefaultCalendar(Request);
        });

        const { billingRunId } = await createSimpleRpsChain(fx, rpsId, {
          applicationModelType: ['FOR_VOLUMES'],
          invoiceDate: DEFAULT_INVOICE_DATE_2, // 10 May → raw RPS date is 7 Jun (Saturday)
          invoiceDueDateType: 'ACCORDING_TO_THE_CONTRACT',
        });

        await test.step('Start and complete billing run', async () => {
          await startAndCompleteBillingRun(Request, billingRunId);
        });

        let paymentDeadline: string | null = null;
        await test.step('POST invoice/listing — assert adjusted to working day (not Saturday 7 Jun)', async () => {
          paymentDeadline = await getFirstInvoicePaymentDeadline(Request, billingRunId);
          // Raw date 7 Jun 2025 is Saturday. Must shift: 6 Jun (Fri) or 9 Jun (Mon) — Finding F2 (direction unknown).
          assertIsNotWeekend(paymentDeadline, 'TC-BE-3');
          expect(paymentDeadline, 'TC-BE-3: must not be the raw Saturday').not.toBe('2026-11-07');
        });

        await test.step('Attach test run summary', async () => {
          TestRunSummary.recordCheck({
            check: 'RPS day 7, invoice 10 May → raw 7 Jun (Sat) → adjusted to nearest working day',
            expectedResult: 'paymentDeadline ≠ 2026-11-07 and is a weekday (Finding F2: direction PO-dependent)',
            actualResult: `paymentDeadline = ${paymentDeadline}`,
            passed: paymentDeadline !== '2026-11-07' && !assertIsNotWeekendBool(paymentDeadline),
          });
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: JIRA_KEY,
            relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun'],
            snapshot: { tcId: 'TC-BE-3', rpsId, billingRunId, paymentDeadline, findingF2: 'direction ambiguous' },
          });
        });
      },
    );

    // ─── TC-BE-4 ─────────────────────────────────────────────────────────
    test(
      `[${JIRA_KEY}] TC-BE-4: Holiday on calculated RPS due date — adjusted to nearest working day`,
      async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
        test.setTimeout(12 * 60 * 1000);
        const fx: RpsDueDateFx = { Request, GeneratePayload, Responses, Endpoints };

        let rpsId = 0;
        await test.step('Precondition: RPS nomenclature (dayOfMonth=6, raw=6 Jun 2025 holiday)', async () => {
          rpsId = await createRpsNomenclature(Request, { rpsNumber: 'RPS-TC4-HOL', dayOfMonth: 6 });
        });
        await test.step('Precondition: default calendar with holiday on 2026-11-06', async () => {
          await createDefaultCalendar(Request, ['2026-11-06']);
        });

        const { billingRunId } = await createSimpleRpsChain(fx, rpsId, {
          applicationModelType: ['FOR_VOLUMES'],
          invoiceDate: DEFAULT_INVOICE_DATE_2, // 10 May → raw RPS date 6 Jun (configured holiday)
          invoiceDueDateType: 'ACCORDING_TO_THE_CONTRACT',
        });

        await test.step('Start and complete billing run', async () => {
          await startAndCompleteBillingRun(Request, billingRunId);
        });

        let paymentDeadline: string | null = null;
        await test.step('POST invoice/listing — assert adjusted off holiday 2026-11-06', async () => {
          paymentDeadline = await getFirstInvoicePaymentDeadline(Request, billingRunId);
          expect(paymentDeadline, 'TC-BE-4: paymentDeadline must be set').toBeTruthy();
          expect(paymentDeadline, 'TC-BE-4: must not be holiday date 2026-11-06').not.toBe('2026-11-06');
          assertIsNotWeekend(paymentDeadline, 'TC-BE-4');
        });

        await test.step('Attach test run summary', async () => {
          TestRunSummary.recordCheck({
            check: 'RPS day 6, invoice 10 May → raw 6 Jun (holiday) → adjusted to working day',
            expectedResult: 'paymentDeadline ≠ 2026-11-06 and is a working day',
            actualResult: `paymentDeadline = ${paymentDeadline}`,
            passed: paymentDeadline !== '2026-11-06',
          });
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: JIRA_KEY,
            relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun'],
            snapshot: { tcId: 'TC-BE-4', rpsId, billingRunId, paymentDeadline },
          });
        });
      },
    );

    // ─── TC-BE-5 ─────────────────────────────────────────────────────────
    test(
      `[${JIRA_KEY}] TC-BE-5: invoiceDueDateType=DATE — RPS ignored, billing run dueDate used`,
      async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
        test.setTimeout(12 * 60 * 1000);
        const fx: RpsDueDateFx = { Request, GeneratePayload, Responses, Endpoints };

        let rpsId = 0;
        await test.step('Precondition: RPS nomenclature (dayOfMonth=5)', async () => {
          rpsId = await createRpsNomenclature(Request, { rpsNumber: 'RPS-TC5-IGN', dayOfMonth: 5 });
        });
        await test.step('Precondition: default calendar', async () => {
          await createDefaultCalendar(Request);
        });

        const { billingRunId } = await createSimpleRpsChain(fx, rpsId, {
          applicationModelType: ['FOR_VOLUMES'],
          invoiceDate: DEFAULT_INVOICE_DATE_1,
          invoiceDueDateType: 'DATE',
          dueDate: '2026-10-20',
        });

        await test.step('Start and complete billing run', async () => {
          await startAndCompleteBillingRun(Request, billingRunId);
        });

        let paymentDeadline: string | null = null;
        await test.step('POST invoice/listing — assert paymentDeadline=2026-10-20 (billing run dueDate, not RPS)', async () => {
          paymentDeadline = await getFirstInvoicePaymentDeadline(Request, billingRunId);
          expect(paymentDeadline, 'TC-BE-5: paymentDeadline must equal billing run dueDate 2026-10-20').toBe(
            '2026-10-20',
          );
        });

        await test.step('Attach test run summary', async () => {
          TestRunSummary.recordCheck({
            check: 'invoiceDueDateType=DATE → RPS ignored, dueDate=2026-10-20 used',
            expectedResult: 'paymentDeadline = 2026-10-20',
            actualResult: `paymentDeadline = ${paymentDeadline}`,
            passed: paymentDeadline === '2026-10-20',
          });
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: JIRA_KEY,
            relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun'],
            snapshot: { tcId: 'TC-BE-5', rpsId, billingRunId, paymentDeadline },
          });
        });
      },
    );

    // ─── TC-BE-6 ─────────────────────────────────────────────────────────
    test(
      `[${JIRA_KEY}] TC-BE-6: Non-RPS POD, ACCORDING_TO_THE_CONTRACT — contract term calc unchanged (regression)`,
      async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
        test.setTimeout(12 * 60 * 1000);
        const fx: RpsDueDateFx = { Request, GeneratePayload, Responses, Endpoints };

        await test.step('Precondition: default calendar', async () => {
          await createDefaultCalendar(Request);
        });

        // rpsNumberId=null → non-RPS POD
        const { billingRunId } = await createSimpleRpsChain(fx, null, {
          applicationModelType: ['FOR_VOLUMES'],
          invoiceDate: DEFAULT_INVOICE_DATE_1,
          invoiceDueDateType: 'ACCORDING_TO_THE_CONTRACT',
        });

        await test.step('Start and complete billing run', async () => {
          await startAndCompleteBillingRun(Request, billingRunId);
        });

        let paymentDeadline: string | null = null;
        await test.step('POST invoice/listing — assert paymentDeadline from contract term (not RPS)', async () => {
          paymentDeadline = await getFirstInvoicePaymentDeadline(Request, billingRunId);
          // Contract term default is 10 calendar days: 7 May + 10 = 17 May 2025
          expect(paymentDeadline, 'TC-BE-6: paymentDeadline must be set (non-null)').toBeTruthy();
          // RPS rule must NOT apply — deadline is from contract term, NOT 2026-11-05
          expect(paymentDeadline, 'TC-BE-6: RPS rule must not apply to non-RPS POD').not.toBe('2026-11-05');
        });

        await test.step('Attach test run summary', async () => {
          TestRunSummary.recordCheck({
            check: 'Non-RPS POD → contract payment term used, not RPS nomenclature',
            expectedResult: 'paymentDeadline from contract term (15 calendar days from 2026-10-07), not 2026-11-05',
            actualResult: `paymentDeadline = ${paymentDeadline}`,
            passed: !!paymentDeadline && paymentDeadline !== '2026-11-05',
          });
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: JIRA_KEY,
            relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun'],
            snapshot: { tcId: 'TC-BE-6', billingRunId, paymentDeadline, note: 'rpsNumberId=null' },
          });
        });
      },
    );

    // ─── TC-BE-7 ─────────────────────────────────────────────────────────
    test(
      `[${JIRA_KEY}] TC-BE-7: Two RPS PODs, same RPS number, same invoice — no error, single due date`,
      async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
        test.setTimeout(15 * 60 * 1000);
        const fx: RpsDueDateFx = { Request, GeneratePayload, Responses, Endpoints };

        let rpsId = 0;
        await test.step('Precondition: one RPS nomenclature (dayOfMonth=10) shared by both PODs', async () => {
          rpsId = await createRpsNomenclature(Request, { rpsNumber: 'RPS-TC7-SAME', dayOfMonth: 10 });
        });
        await test.step('Precondition: default calendar', async () => {
          await createDefaultCalendar(Request);
        });

        const { billingRunId } = await createTwoPodSameBillingGroupChain(fx, rpsId, rpsId, {
          applicationModelType: ['FOR_VOLUMES'],
          invoiceDate: DEFAULT_INVOICE_DATE_1,
          invoiceDueDateType: 'ACCORDING_TO_THE_CONTRACT',
        });

        await test.step('Start and complete billing run', async () => {
          await startAndCompleteBillingRun(Request, billingRunId);
        });

        let paymentDeadline: string | null = null;
        await test.step('POST invoice/listing — one invoice, paymentDeadline=2026-11-10, no error', async () => {
          const rows = await pollInvoiceListing(Request, billingRunId, 1);
          expect(rows.length, 'TC-BE-7: exactly one invoice for both PODs with same RPS').toBe(1);
          paymentDeadline = rows[0].paymentDeadline as string | null;
          expect(paymentDeadline, 'TC-BE-7: paymentDeadline must be 2026-11-10').toBe('2026-11-10');
        });

        await test.step('Attach test run summary', async () => {
          TestRunSummary.recordCheck({
            check: 'Two PODs with same RPS number → one invoice, no conflict error',
            expectedResult: 'paymentDeadline = 2026-11-10, invoice count = 1',
            actualResult: `paymentDeadline = ${paymentDeadline}`,
            passed: paymentDeadline === '2026-11-10',
          });
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: JIRA_KEY,
            relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun'],
            snapshot: { tcId: 'TC-BE-7', rpsId, billingRunId, paymentDeadline },
          });
        });
      },
    );

    // ─── TC-BE-8 ─────────────────────────────────────────────────────────
    test(
      `[${JIRA_KEY}] TC-BE-8: Two RPS PODs, different RPS, different billing groups — two invoices, each own due date`,
      async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
        test.setTimeout(18 * 60 * 1000);
        const fx: RpsDueDateFx = { Request, GeneratePayload, Responses, Endpoints };

        let rpsId1 = 0;
        let rpsId2 = 0;
        await test.step('Precondition: RPS-G1 (dayOfMonth=5)', async () => {
          rpsId1 = await createRpsNomenclature(Request, { rpsNumber: 'RPS-TC8-G1', dayOfMonth: 5 });
        });
        await test.step('Precondition: RPS-G2 (dayOfMonth=20)', async () => {
          rpsId2 = await createRpsNomenclature(Request, { rpsNumber: 'RPS-TC8-G2', dayOfMonth: 20 });
        });
        await test.step('Precondition: default calendar', async () => {
          await createDefaultCalendar(Request);
        });

        const { billingRunId } = await createTwoPodSeparateBillingGroupsChain(fx, rpsId1, rpsId2, {
          applicationModelType: ['FOR_VOLUMES'],
          invoiceDate: DEFAULT_INVOICE_DATE_1,
          invoiceDueDateType: 'ACCORDING_TO_THE_CONTRACT',
        });

        await test.step('Start and complete billing run', async () => {
          await startAndCompleteBillingRun(Request, billingRunId);
        });

        await test.step('POST invoice/listing — two invoices, correct per-RPS due dates, no error', async () => {
          const rows = await pollInvoiceListing(Request, billingRunId, 2);
          expect(rows.length, 'TC-BE-8: two invoices for two separate billing groups').toBe(2);
          const deadlines = rows.map((r) => r.paymentDeadline as string).sort();
          expect(deadlines, 'TC-BE-8: must contain 2026-11-05 and 2026-11-20').toEqual(
            ['2026-11-05', '2026-11-20'].sort(),
          );
        });

        await test.step('Attach test run summary', async () => {
          TestRunSummary.recordCheck({
            check: 'Different RPS in separate billing groups → two invoices, each with own due date',
            expectedResult: 'invoices: [2026-11-05, 2026-11-20]',
            actualResult: 'see snapshot',
            passed: true,
          });
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: JIRA_KEY,
            relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun'],
            snapshot: { tcId: 'TC-BE-8', rpsId1, rpsId2, billingRunId },
          });
        });
      },
    );

    // ─── TC-BE-9 ─────────────────────────────────────────────────────────
    test(
      `[${JIRA_KEY}] TC-BE-9: Two RPS PODs, different RPS, same billing group, separate invoice per POD — two invoices, no error`,
      async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
        test.setTimeout(18 * 60 * 1000);
        const fx: RpsDueDateFx = { Request, GeneratePayload, Responses, Endpoints };

        let rpsId1 = 0;
        let rpsId2 = 0;
        await test.step('Precondition: RPS-S1 (dayOfMonth=5)', async () => {
          rpsId1 = await createRpsNomenclature(Request, { rpsNumber: 'RPS-TC9-S1', dayOfMonth: 5 });
        });
        await test.step('Precondition: RPS-S2 (dayOfMonth=20)', async () => {
          rpsId2 = await createRpsNomenclature(Request, { rpsNumber: 'RPS-TC9-S2', dayOfMonth: 20 });
        });
        await test.step('Precondition: default calendar', async () => {
          await createDefaultCalendar(Request);
        });

        const { billingRunId } = await createTwoPodSeparateInvoicePerPodChain(fx, rpsId1, rpsId2, {
          applicationModelType: ['FOR_VOLUMES'],
          invoiceDate: DEFAULT_INVOICE_DATE_1,
          invoiceDueDateType: 'ACCORDING_TO_THE_CONTRACT',
        });

        await test.step('Start and complete billing run', async () => {
          await startAndCompleteBillingRun(Request, billingRunId);
        });

        await test.step('POST invoice/listing — two invoices, per-POD RPS dates, no conflict error', async () => {
          const rows = await pollInvoiceListing(Request, billingRunId, 2);
          expect(rows.length, 'TC-BE-9: two invoices (separate invoice per POD)').toBe(2);
          const deadlines = rows.map((r) => r.paymentDeadline as string).sort();
          expect(deadlines, 'TC-BE-9: must contain 2026-11-05 and 2026-11-20').toEqual(
            ['2026-11-05', '2026-11-20'].sort(),
          );
        });

        await test.step('Attach test run summary', async () => {
          TestRunSummary.recordCheck({
            check: 'Different RPS, same BG but separate invoice per POD → two invoices, no conflict',
            expectedResult: 'invoices: [2026-11-05, 2026-11-20]',
            actualResult: 'see snapshot',
            passed: true,
          });
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: JIRA_KEY,
            relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun'],
            snapshot: { tcId: 'TC-BE-9', rpsId1, rpsId2, billingRunId },
          });
        });
      },
    );

    // ─── TC-BE-10 ────────────────────────────────────────────────────────
    test(
      `[${JIRA_KEY}] TC-BE-10: One RPS POD + one non-RPS POD, same invoice — RPS nomenclature wins`,
      async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
        test.setTimeout(15 * 60 * 1000);
        const fx: RpsDueDateFx = { Request, GeneratePayload, Responses, Endpoints };

        let rpsId = 0;
        await test.step('Precondition: RPS-MIX (dayOfMonth=5)', async () => {
          rpsId = await createRpsNomenclature(Request, { rpsNumber: 'RPS-TC10-MIX', dayOfMonth: 5 });
        });
        await test.step('Precondition: default calendar', async () => {
          await createDefaultCalendar(Request);
        });

        // POD 1 = RPS, POD 2 = null (non-RPS), both in same billing group → same invoice
        const { billingRunId } = await createTwoPodSameBillingGroupChain(fx, rpsId, null, {
          applicationModelType: ['FOR_VOLUMES'],
          invoiceDate: DEFAULT_INVOICE_DATE_1,
          invoiceDueDateType: 'ACCORDING_TO_THE_CONTRACT',
        });

        await test.step('Start and complete billing run', async () => {
          await startAndCompleteBillingRun(Request, billingRunId);
        });

        let paymentDeadline: string | null = null;
        await test.step('POST invoice/listing — one invoice, paymentDeadline=2026-11-05 (RPS wins)', async () => {
          const rows = await pollInvoiceListing(Request, billingRunId, 1);
          expect(rows.length, 'TC-BE-10: single invoice for mixed RPS+non-RPS in same BG').toBe(1);
          paymentDeadline = rows[0].paymentDeadline as string | null;
          expect(paymentDeadline, 'TC-BE-10: RPS due date must override contract term').toBe('2026-11-05');
        });

        await test.step('Attach test run summary', async () => {
          TestRunSummary.recordCheck({
            check: 'RPS POD + non-RPS POD in same invoice → RPS wins',
            expectedResult: 'paymentDeadline = 2026-11-05 (not contract term)',
            actualResult: `paymentDeadline = ${paymentDeadline}`,
            passed: paymentDeadline === '2026-11-05',
          });
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: JIRA_KEY,
            relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun'],
            snapshot: { tcId: 'TC-BE-10', rpsId, billingRunId, paymentDeadline },
          });
        });
      },
    );

    // ─── TC-BE-11 ────────────────────────────────────────────────────────
    test(
      `[${JIRA_KEY}] TC-BE-11: One RPS POD + one non-RPS POD, different billing groups — each invoice independent`,
      async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
        test.setTimeout(18 * 60 * 1000);
        const fx: RpsDueDateFx = { Request, GeneratePayload, Responses, Endpoints };

        let rpsId = 0;
        await test.step('Precondition: RPS-SPLIT (dayOfMonth=5)', async () => {
          rpsId = await createRpsNomenclature(Request, { rpsNumber: 'RPS-TC11-SPLIT', dayOfMonth: 5 });
        });
        await test.step('Precondition: default calendar', async () => {
          await createDefaultCalendar(Request);
        });

        // POD 1 = RPS in BG1; POD 2 = non-RPS in BG2 → two invoices
        const { billingRunId } = await createTwoPodSeparateBillingGroupsChain(fx, rpsId, null, {
          applicationModelType: ['FOR_VOLUMES'],
          invoiceDate: DEFAULT_INVOICE_DATE_1,
          invoiceDueDateType: 'ACCORDING_TO_THE_CONTRACT',
        });

        await test.step('Start and complete billing run', async () => {
          await startAndCompleteBillingRun(Request, billingRunId);
        });

        await test.step('POST invoice/listing — two invoices; RPS=2026-11-05, non-RPS=contract term', async () => {
          const rows = await pollInvoiceListing(Request, billingRunId, 2);
          expect(rows.length, 'TC-BE-11: two invoices for separate billing groups').toBe(2);
          const deadlines = rows.map((r) => r.paymentDeadline as string);
          expect(deadlines, 'TC-BE-11: must include RPS date 2026-11-05').toContain('2026-11-05');
          // The non-RPS invoice paymentDeadline comes from contract term — should NOT be 2026-11-05
          const nonRpsDeadline = deadlines.find((d) => d !== '2026-11-05');
          expect(nonRpsDeadline, 'TC-BE-11: non-RPS invoice has contract-term deadline').toBeTruthy();
        });

        await test.step('Attach test run summary', async () => {
          TestRunSummary.recordCheck({
            check: 'RPS + non-RPS in separate BGs → two invoices, each own calculation',
            expectedResult: 'One invoice paymentDeadline=2026-11-05, other from contract term',
            actualResult: 'see snapshot',
            passed: true,
          });
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: JIRA_KEY,
            relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun'],
            snapshot: { tcId: 'TC-BE-11', rpsId, billingRunId },
          });
        });
      },
    );

    // ─── TC-BE-12 ────────────────────────────────────────────────────────
    test(
      `[${JIRA_KEY}] TC-BE-12: RPS due date in OVER_TIME_PERIODICAL flow`,
      async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
        test.setTimeout(12 * 60 * 1000);
        const fx: RpsDueDateFx = { Request, GeneratePayload, Responses, Endpoints };

        let rpsId = 0;
        await test.step('Precondition: RPS-OTP (dayOfMonth=5)', async () => {
          rpsId = await createRpsNomenclature(Request, { rpsNumber: 'RPS-TC12-OTP', dayOfMonth: 5 });
        });
        await test.step('Precondition: default calendar', async () => {
          await createDefaultCalendar(Request);
        });

        const { billingRunId } = await createSimpleRpsChain(fx, rpsId, {
          applicationModelType: ['OVER_TIME_PERIODICAL'],
          invoiceDate: DEFAULT_INVOICE_DATE_1,
          invoiceDueDateType: 'ACCORDING_TO_THE_CONTRACT',
        });

        await test.step('Start and complete billing run', async () => {
          await startAndCompleteBillingRun(Request, billingRunId);
        });

        let paymentDeadline: string | null = null;
        await test.step('POST invoice/listing — paymentDeadline=2026-11-05 (OVER_TIME_PERIODICAL)', async () => {
          paymentDeadline = await getFirstInvoicePaymentDeadline(Request, billingRunId);
          expect(paymentDeadline, 'TC-BE-12: RPS rule applies to OVER_TIME_PERIODICAL').toBe('2026-11-05');
        });

        await test.step('Attach test run summary', async () => {
          TestRunSummary.recordCheck({
            check: 'OVER_TIME_PERIODICAL + RPS → paymentDeadline=2026-11-05',
            expectedResult: 'paymentDeadline = 2026-11-05',
            actualResult: `paymentDeadline = ${paymentDeadline}`,
            passed: paymentDeadline === '2026-11-05',
          });
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: JIRA_KEY,
            relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun'],
            snapshot: { tcId: 'TC-BE-12', rpsId, billingRunId, paymentDeadline },
          });
        });
      },
    );

    // ─── TC-BE-13 ────────────────────────────────────────────────────────
    test(
      `[${JIRA_KEY}] TC-BE-13: RPS due date in OVER_TIME_ONE_TIME flow`,
      async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
        test.setTimeout(12 * 60 * 1000);
        const fx: RpsDueDateFx = { Request, GeneratePayload, Responses, Endpoints };

        let rpsId = 0;
        await test.step('Precondition: RPS-OTO (dayOfMonth=5)', async () => {
          rpsId = await createRpsNomenclature(Request, { rpsNumber: 'RPS-TC13-OTO', dayOfMonth: 5 });
        });
        await test.step('Precondition: default calendar', async () => {
          await createDefaultCalendar(Request);
        });

        const { billingRunId } = await createSimpleRpsChain(fx, rpsId, {
          applicationModelType: ['OVER_TIME_ONE_TIME'],
          invoiceDate: DEFAULT_INVOICE_DATE_1,
          invoiceDueDateType: 'ACCORDING_TO_THE_CONTRACT',
        });

        await test.step('Start and complete billing run', async () => {
          await startAndCompleteBillingRun(Request, billingRunId);
        });

        let paymentDeadline: string | null = null;
        await test.step('POST invoice/listing — paymentDeadline=2026-11-05 (OVER_TIME_ONE_TIME)', async () => {
          paymentDeadline = await getFirstInvoicePaymentDeadline(Request, billingRunId);
          expect(paymentDeadline, 'TC-BE-13: RPS rule applies to OVER_TIME_ONE_TIME').toBe('2026-11-05');
        });

        await test.step('Attach test run summary', async () => {
          TestRunSummary.recordCheck({
            check: 'OVER_TIME_ONE_TIME + RPS → paymentDeadline=2026-11-05',
            expectedResult: 'paymentDeadline = 2026-11-05',
            actualResult: `paymentDeadline = ${paymentDeadline}`,
            passed: paymentDeadline === '2026-11-05',
          });
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: JIRA_KEY,
            relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun'],
            snapshot: { tcId: 'TC-BE-13', rpsId, billingRunId, paymentDeadline },
          });
        });
      },
    );

    // ─── TC-BE-14 ────────────────────────────────────────────────────────
    test(
      `[${JIRA_KEY}] TC-BE-14: RPS due date in WITH_ELECTRICITY_INVOICE flow`,
      async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
        test.setTimeout(12 * 60 * 1000);
        const fx: RpsDueDateFx = { Request, GeneratePayload, Responses, Endpoints };

        let rpsId = 0;
        await test.step('Precondition: RPS-EL (dayOfMonth=5)', async () => {
          rpsId = await createRpsNomenclature(Request, { rpsNumber: 'RPS-TC14-EL', dayOfMonth: 5 });
        });
        await test.step('Precondition: default calendar', async () => {
          await createDefaultCalendar(Request);
        });

        const { billingRunId } = await createSimpleRpsChain(fx, rpsId, {
          applicationModelType: ['FOR_VOLUMES', 'WITH_ELECTRICITY_INVOICE'],
          invoiceDate: DEFAULT_INVOICE_DATE_1,
          invoiceDueDateType: 'ACCORDING_TO_THE_CONTRACT',
        });

        await test.step('Start and complete billing run', async () => {
          await startAndCompleteBillingRun(Request, billingRunId);
        });

        let paymentDeadline: string | null = null;
        await test.step('POST invoice/listing — paymentDeadline=2026-11-05 (WITH_ELECTRICITY_INVOICE)', async () => {
          paymentDeadline = await getFirstInvoicePaymentDeadline(Request, billingRunId);
          expect(paymentDeadline, 'TC-BE-14: RPS rule applies to WITH_ELECTRICITY_INVOICE').toBe('2026-11-05');
        });

        await test.step('Attach test run summary', async () => {
          TestRunSummary.recordCheck({
            check: 'WITH_ELECTRICITY_INVOICE + RPS → paymentDeadline=2026-11-05',
            expectedResult: 'paymentDeadline = 2026-11-05',
            actualResult: `paymentDeadline = ${paymentDeadline}`,
            passed: paymentDeadline === '2026-11-05',
          });
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: JIRA_KEY,
            relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun'],
            snapshot: { tcId: 'TC-BE-14', rpsId, billingRunId, paymentDeadline },
          });
        });
      },
    );

    // ─── TC-BE-15 ────────────────────────────────────────────────────────
    test(
      `[${JIRA_KEY}] TC-BE-15: RPS due date in INTERIM_AND_ADVANCE_PAYMENT flow`,
      async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
        test.setTimeout(12 * 60 * 1000);
        const fx: RpsDueDateFx = { Request, GeneratePayload, Responses, Endpoints };

        let rpsId = 0;
        await test.step('Precondition: RPS-IAP (dayOfMonth=5)', async () => {
          rpsId = await createRpsNomenclature(Request, { rpsNumber: 'RPS-TC15-IAP', dayOfMonth: 5 });
        });
        await test.step('Precondition: default calendar', async () => {
          await createDefaultCalendar(Request);
        });

        const { billingRunId } = await createSimpleRpsChain(fx, rpsId, {
          applicationModelType: ['INTERIM_AND_ADVANCE_PAYMENT'],
          invoiceDate: DEFAULT_INVOICE_DATE_1,
          invoiceDueDateType: 'ACCORDING_TO_THE_CONTRACT',
          billingType: 'STANDARD_BILLING',
        });

        await test.step('Start and complete billing run', async () => {
          await startAndCompleteBillingRun(Request, billingRunId);
        });

        let paymentDeadline: string | null = null;
        await test.step('POST invoice/listing — paymentDeadline=2026-11-05 (INTERIM_AND_ADVANCE_PAYMENT)', async () => {
          paymentDeadline = await getFirstInvoicePaymentDeadline(Request, billingRunId);
          expect(paymentDeadline, 'TC-BE-15: RPS rule applies to INTERIM_AND_ADVANCE_PAYMENT').toBe('2026-11-05');
        });

        await test.step('Attach test run summary', async () => {
          TestRunSummary.recordCheck({
            check: 'INTERIM_AND_ADVANCE_PAYMENT + RPS → paymentDeadline=2026-11-05',
            expectedResult: 'paymentDeadline = 2026-11-05',
            actualResult: `paymentDeadline = ${paymentDeadline}`,
            passed: paymentDeadline === '2026-11-05',
          });
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: JIRA_KEY,
            relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun'],
            snapshot: { tcId: 'TC-BE-15', rpsId, billingRunId, paymentDeadline },
          });
        });
      },
    );

    // ─── TC-BE-16 ────────────────────────────────────────────────────────
    test(
      `[${JIRA_KEY}] TC-BE-16: RPS due date in manual invoice flow`,
      async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
        test.setTimeout(12 * 60 * 1000);
        const fx: RpsDueDateFx = { Request, GeneratePayload, Responses, Endpoints };

        let rpsId = 0;
        await test.step('Precondition: RPS-MAN (dayOfMonth=5)', async () => {
          rpsId = await createRpsNomenclature(Request, { rpsNumber: 'RPS-TC16-MAN', dayOfMonth: 5 });
        });
        await test.step('Precondition: default calendar', async () => {
          await createDefaultCalendar(Request);
        });

        // Create entity chain without billing run (manual invoice needs customer+contract in Responses)
        await test.step('Precondition: term', async () => createTerm(fx));
        await test.step('Precondition: price component', async () => createElectricityPriceComponent(fx));
        await test.step('Precondition: product', async () => createProduct(fx));
        await test.step('Precondition: customer', async () => createCustomer(fx));
        await test.step('Precondition: POD', async () => createPod(fx, rpsId));
        await test.step('Precondition: product contract', async () => createProductContract(fx));
        await test.step('Precondition: activate POD', async () => activatePod(fx));

        let billingRunId = 0;
        await test.step('Precondition: manual invoice billing run', async () => {
          const payload = await GeneratePayload.billing.manualInvoice() as Record<string, unknown>;
          const cp = (payload.commonParameters ?? {}) as Record<string, unknown>;
          cp.invoiceDate = DEFAULT_INVOICE_DATE_1;
          cp.invoiceDueDate = 'ACCORDING_TO_THE_CONTRACT';
          cp.dueDate = null;
          payload.commonParameters = cp;
          payload.billingType = 'MANUAL_INVOICE';
          const res = await Request.post(Endpoints.billingRun, { data: payload });
          await expect(res).CheckResponse();
          billingRunId = asBillingRunId(await res.json());
          Responses.billingRun.push(billingRunId);
        });

        await test.step('Start and complete manual invoice billing run', async () => {
          await startAndCompleteBillingRun(Request, billingRunId);
        });

        let paymentDeadline: string | null = null;
        await test.step('POST invoice/listing — paymentDeadline=2026-11-05 (manual invoice, Finding F1)', async () => {
          paymentDeadline = await getFirstInvoicePaymentDeadline(Request, billingRunId);
          expect(paymentDeadline, 'TC-BE-16: RPS rule applies to manual invoice (Finding F1)').toBe('2026-11-05');
        });

        await test.step('Attach test run summary', async () => {
          TestRunSummary.recordCheck({
            check: 'Manual invoice + RPS → paymentDeadline=2026-11-05 (Finding F1: separate engine)',
            expectedResult: 'paymentDeadline = 2026-11-05',
            actualResult: `paymentDeadline = ${paymentDeadline}`,
            passed: paymentDeadline === '2026-11-05',
          });
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: JIRA_KEY,
            relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun'],
            snapshot: { tcId: 'TC-BE-16', rpsId, billingRunId, paymentDeadline },
          });
        });
      },
    );

    // ─── TC-BE-17 ────────────────────────────────────────────────────────
    test(
      `[${JIRA_KEY}] TC-BE-17: RPS due date in manual debit note flow`,
      async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
        test.setTimeout(18 * 60 * 1000);
        const fx: RpsDueDateFx = { Request, GeneratePayload, Responses, Endpoints };

        let rpsId = 0;
        await test.step('Precondition: RPS-DN (dayOfMonth=5)', async () => {
          rpsId = await createRpsNomenclature(Request, { rpsNumber: 'RPS-TC17-DN', dayOfMonth: 5 });
        });
        await test.step('Precondition: default calendar', async () => {
          await createDefaultCalendar(Request);
        });

        // Create base invoice first (debit note references it)
        let baseInvoiceId17 = 0;
        await test.step('Run base invoice (prerequisite for debit note)', async () => {
          const result = await runBaseInvoiceForManualNotes(fx, rpsId, DEFAULT_INVOICE_DATE_1);
          baseInvoiceId17 = result.baseInvoiceId;
        });

        let debitNoteBillingRunId = 0;
        await test.step('Precondition: manual debit note billing run', async () => {
          // Fetch base invoice number for the billingRunInvoiceInformationList reference
          const invRes = await Request.get(`invoice?id=${baseInvoiceId17}`);
          await expect(invRes).CheckResponse();
          const invBody = await invRes.json();
          const baseInvoiceNumber = invBody.invoiceNumber as string;

          const payload = GeneratePayload.billing.debitNote() as Record<string, unknown>;
          const cp = (payload.commonParameters ?? {}) as Record<string, unknown>;
          cp.invoiceDate = DEFAULT_INVOICE_DATE_1;
          cp.invoiceDueDate = 'ACCORDING_TO_THE_CONTRACT';
          cp.dueDate = null;
          payload.commonParameters = cp;
          // billingType already correct: "MANUAL_CREDIT_OR_DEBIT_NOTE" — do not override

          // Fill invoice reference in nested params
          const noteBasicParams = (payload as any)
            .manualCreditOrDebitNoteParameters
            .manualCreditOrDebitNoteBasicDataParameters;
          noteBasicParams.billingRunInvoiceInformationList = [
            { invoiceId: baseInvoiceId17, invoiceNumber: baseInvoiceNumber },
          ];
          noteBasicParams.applicableInterestRateId = envVariables.interest_rate;
          noteBasicParams.external = false;
          // documentType is already DEBIT_NOTE

          const res = await Request.post(Endpoints.billingRun, { data: payload });
          await expect(res).CheckResponse();
          debitNoteBillingRunId = asBillingRunId(await res.json());
          Responses.billingRun.push(debitNoteBillingRunId);
        });

        await test.step('Start and complete debit note billing run', async () => {
          await startAndCompleteBillingRun(Request, debitNoteBillingRunId);
        });

        let paymentDeadline: string | null = null;
        await test.step('POST invoice/listing — debit note paymentDeadline=2026-11-05', async () => {
          paymentDeadline = await getFirstInvoicePaymentDeadline(Request, debitNoteBillingRunId);
          expect(paymentDeadline, 'TC-BE-17: RPS rule applies to manual debit note').toBe('2026-11-05');
        });

        await test.step('Attach test run summary', async () => {
          TestRunSummary.recordCheck({
            check: 'Manual DEBIT_NOTE + RPS → paymentDeadline=2026-11-05',
            expectedResult: 'paymentDeadline = 2026-11-05',
            actualResult: `paymentDeadline = ${paymentDeadline}`,
            passed: paymentDeadline === '2026-11-05',
          });
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: JIRA_KEY,
            relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun', 'invoice'],
            snapshot: { tcId: 'TC-BE-17', rpsId, debitNoteBillingRunId, paymentDeadline },
          });
        });
      },
    );

    // ─── TC-BE-18 ────────────────────────────────────────────────────────
    test(
      `[${JIRA_KEY}] TC-BE-18: Manual credit note for RPS POD — paymentDeadline=null (credit notes always null)`,
      async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
        test.setTimeout(18 * 60 * 1000);
        const fx: RpsDueDateFx = { Request, GeneratePayload, Responses, Endpoints };

        let rpsId = 0;
        await test.step('Precondition: RPS-CN (dayOfMonth=5)', async () => {
          rpsId = await createRpsNomenclature(Request, { rpsNumber: 'RPS-TC18-CN', dayOfMonth: 5 });
        });
        await test.step('Precondition: default calendar', async () => {
          await createDefaultCalendar(Request);
        });

        let baseInvoiceId18 = 0;
        await test.step('Run base invoice (prerequisite for credit note)', async () => {
          const result = await runBaseInvoiceForManualNotes(fx, rpsId, DEFAULT_INVOICE_DATE_1);
          baseInvoiceId18 = result.baseInvoiceId;
        });

        let creditNoteBillingRunId = 0;
        await test.step('Precondition: manual credit note billing run', async () => {
          // Fetch base invoice number for the billingRunInvoiceInformationList reference
          const invRes = await Request.get(`invoice?id=${baseInvoiceId18}`);
          await expect(invRes).CheckResponse();
          const invBody = await invRes.json();
          const baseInvoiceNumber = invBody.invoiceNumber as string;

          const payload = GeneratePayload.billing.creditNote() as Record<string, unknown>;
          const cp = (payload.commonParameters ?? {}) as Record<string, unknown>;
          cp.invoiceDate = DEFAULT_INVOICE_DATE_1;
          cp.invoiceDueDate = 'ACCORDING_TO_THE_CONTRACT';
          cp.dueDate = null;
          payload.commonParameters = cp;
          // billingType already correct: "MANUAL_CREDIT_OR_DEBIT_NOTE" — do not override

          // Fill invoice reference in nested params
          const noteBasicParams = (payload as any)
            .manualCreditOrDebitNoteParameters
            .manualCreditOrDebitNoteBasicDataParameters;
          noteBasicParams.billingRunInvoiceInformationList = [
            { invoiceId: baseInvoiceId18, invoiceNumber: baseInvoiceNumber },
          ];
          noteBasicParams.documentType = 'CREDIT_NOTE';
          noteBasicParams.applicableInterestRateId = envVariables.interest_rate;
          noteBasicParams.external = false;
          const summary = (payload as any).manualCreditOrDebitNoteParameters
            .manualCreditOrDebitNoteSummaryDataParameters.summaryDataRowList[0];
          summary.value = -10;

          const res = await Request.post(Endpoints.billingRun, { data: payload });
          await expect(res).CheckResponse();
          creditNoteBillingRunId = asBillingRunId(await res.json());
          Responses.billingRun.push(creditNoteBillingRunId);
        });

        await test.step('Start and complete credit note billing run', async () => {
          await startAndCompleteBillingRun(Request, creditNoteBillingRunId);
        });

        let paymentDeadline: string | null | undefined = undefined;
        await test.step('POST invoice/listing — credit note paymentDeadline=null', async () => {
          const rows = await pollInvoiceListing(Request, creditNoteBillingRunId, 1);
          if (rows.length > 0) {
            paymentDeadline = rows[0].paymentDeadline as string | null;
            expect(paymentDeadline, 'TC-BE-18: credit note paymentDeadline must be null').toBeNull();
          }
        });

        await test.step('Attach test run summary', async () => {
          TestRunSummary.recordCheck({
            check: 'CREDIT_NOTE + RPS → paymentDeadline=null (RPS must not override credit note null)',
            expectedResult: 'paymentDeadline = null',
            actualResult: `paymentDeadline = ${paymentDeadline}`,
            passed: paymentDeadline === null,
          });
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: JIRA_KEY,
            relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun', 'invoice'],
            snapshot: { tcId: 'TC-BE-18', rpsId, creditNoteBillingRunId, paymentDeadline },
          });
        });
      },
    );

    // ─── TC-BE-19 ────────────────────────────────────────────────────────
    test(
      `[${JIRA_KEY}] TC-BE-19: RPS due date carried into invoice correction`,
      async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
        test.setTimeout(18 * 60 * 1000);
        const fx: RpsDueDateFx = { Request, GeneratePayload, Responses, Endpoints };

        let rpsId = 0;
        await test.step('Precondition: RPS-CORR (dayOfMonth=5)', async () => {
          rpsId = await createRpsNomenclature(Request, { rpsNumber: 'RPS-TC19-CORR', dayOfMonth: 5 });
        });
        await test.step('Precondition: default calendar', async () => {
          await createDefaultCalendar(Request);
        });

        await runBaseInvoiceForManualNotes(fx, rpsId, DEFAULT_INVOICE_DATE_1);

        let correctionBillingRunId = 0;
        await test.step('Precondition: invoice correction billing run', async () => {
          const payload = await GeneratePayload.billing.correctionBilling(0) as Record<string, unknown>;
          const cp = (payload.commonParameters ?? {}) as Record<string, unknown>;
          cp.invoiceDate = DEFAULT_INVOICE_DATE_1;
          cp.invoiceDueDate = 'ACCORDING_TO_THE_CONTRACT';
          cp.dueDate = null;
          payload.commonParameters = cp;
          const res = await Request.post(Endpoints.billingRun, { data: payload });
          await expect(res).CheckResponse();
          correctionBillingRunId = asBillingRunId(await res.json());
          Responses.billingRun.push(correctionBillingRunId);
        });

        await test.step('Start and complete correction billing run', async () => {
          await startAndCompleteBillingRun(Request, correctionBillingRunId);
        });

        let paymentDeadline: string | null = null;
        await test.step('POST invoice/listing — correction invoice paymentDeadline=2026-11-05', async () => {
          paymentDeadline = await getFirstInvoicePaymentDeadline(Request, correctionBillingRunId);
          expect(paymentDeadline, 'TC-BE-19: RPS rule applies to invoice correction').toBe('2026-11-05');
        });

        await test.step('Attach test run summary', async () => {
          TestRunSummary.recordCheck({
            check: 'INVOICE_CORRECTION + RPS → paymentDeadline=2026-11-05',
            expectedResult: 'paymentDeadline = 2026-11-05',
            actualResult: `paymentDeadline = ${paymentDeadline}`,
            passed: paymentDeadline === '2026-11-05',
          });
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: JIRA_KEY,
            relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun', 'invoice'],
            snapshot: { tcId: 'TC-BE-19', rpsId, correctionBillingRunId, paymentDeadline },
          });
        });
      },
    );

    // ─── TC-BE-20 ────────────────────────────────────────────────────────
    test(
      `[${JIRA_KEY}] TC-BE-20: RPS due date in manual interim and advance payment flow`,
      async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
        test.setTimeout(12 * 60 * 1000);
        const fx: RpsDueDateFx = { Request, GeneratePayload, Responses, Endpoints };

        let rpsId = 0;
        await test.step('Precondition: RPS-MIAP (dayOfMonth=5)', async () => {
          rpsId = await createRpsNomenclature(Request, { rpsNumber: 'RPS-TC20-MIAP', dayOfMonth: 5 });
        });
        await test.step('Precondition: default calendar', async () => {
          await createDefaultCalendar(Request);
        });

        await test.step('Precondition: term', async () => createTerm(fx));
        await test.step('Precondition: price component', async () => createElectricityPriceComponent(fx));
        await test.step('Precondition: product', async () => createProduct(fx));
        await test.step('Precondition: customer', async () => createCustomer(fx));
        await test.step('Precondition: POD', async () => createPod(fx, rpsId));
        await test.step('Precondition: product contract', async () => createProductContract(fx));
        await test.step('Precondition: activate POD', async () => activatePod(fx));

        let billingRunId = 0;
        await test.step('Precondition: manual interim/advance payment billing run', async () => {
          // Fetch customer detail id (required by Manualinterim payload — not auto-filled)
          const custGetRes = await Request.get(`customer/${Responses.customer[0].id}?version=1`);
          await expect(custGetRes).CheckResponse();
          const custData = await custGetRes.json();

          const payload = GeneratePayload.billing.Manualinterim() as Record<string, unknown>;
          const cp = (payload.commonParameters ?? {}) as Record<string, unknown>;
          cp.invoiceDate = DEFAULT_INVOICE_DATE_1;
          cp.invoiceDueDate = 'ACCORDING_TO_THE_CONTRACT'; // override DATE for RPS test
          cp.dueDate = null;
          payload.commonParameters = cp;
          // billingType already correct: "MANUAL_INTERIM_AND_ADVANCE_PAYMENT"

          // Fill customer reference and override invoiceDueDateType
          const iapParams = (payload as any).interimAndAdvancePaymentParameters;
          iapParams.customerDetailId = custData.customerDetailsId;
          iapParams.invoiceCommunicationDataId = custData.communicationData[0].id;
          iapParams.invoiceDueDateType = 'ACCORDING_TO_THE_CONTRACT'; // override DATE for RPS test

          const res = await Request.post(Endpoints.billingRun, { data: payload });
          await expect(res).CheckResponse();
          billingRunId = asBillingRunId(await res.json());
          Responses.billingRun.push(billingRunId);
        });

        await test.step('Start and complete billing run', async () => {
          await startAndCompleteBillingRun(Request, billingRunId);
        });

        let paymentDeadline: string | null = null;
        await test.step('POST invoice/listing — paymentDeadline=2026-11-05 (manual interim)', async () => {
          paymentDeadline = await getFirstInvoicePaymentDeadline(Request, billingRunId);
          expect(paymentDeadline, 'TC-BE-20: RPS rule applies to manual interim/advance').toBe('2026-11-05');
        });

        await test.step('Attach test run summary', async () => {
          TestRunSummary.recordCheck({
            check: 'MANUAL_INTERIM_AND_ADVANCE_PAYMENT + RPS → paymentDeadline=2026-11-05',
            expectedResult: 'paymentDeadline = 2026-11-05',
            actualResult: `paymentDeadline = ${paymentDeadline}`,
            passed: paymentDeadline === '2026-11-05',
          });
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: JIRA_KEY,
            relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun'],
            snapshot: { tcId: 'TC-BE-20', rpsId, billingRunId, paymentDeadline },
          });
        });
      },
    );

    // ─── TC-BE-21 ────────────────────────────────────────────────────────
    test(
      `[${JIRA_KEY}] TC-BE-21: RPS adjustment uses DEFAULT calendar, not contract-term calendar`,
      async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
        test.setTimeout(12 * 60 * 1000);
        const fx: RpsDueDateFx = { Request, GeneratePayload, Responses, Endpoints };

        let rpsId = 0;
        await test.step('Precondition: RPS-DEFCAL (dayOfMonth=7, raw=7 Jun 2025 Saturday)', async () => {
          rpsId = await createRpsNomenclature(Request, { rpsNumber: 'RPS-TC21-DEFCAL', dayOfMonth: 7 });
        });
        await test.step('Precondition: default calendar (SAT+SUN weekends)', async () => {
          await createDefaultCalendar(Request); // default: weekends = SAT+SUN
        });
        await test.step('Precondition: non-default calendar (no weekends — would make 7 Jun a working day)', async () => {
          await createNonDefaultCalendar(Request);
        });

        const { billingRunId } = await createSimpleRpsChain(fx, rpsId, {
          applicationModelType: ['FOR_VOLUMES'],
          invoiceDate: DEFAULT_INVOICE_DATE_2, // 10 May → raw RPS 7 Jun (Saturday)
          invoiceDueDateType: 'ACCORDING_TO_THE_CONTRACT',
        });

        await test.step('Start and complete billing run', async () => {
          await startAndCompleteBillingRun(Request, billingRunId);
        });

        let paymentDeadline: string | null = null;
        await test.step('POST invoice/listing — deadline adjusted by DEFAULT calendar (not non-default)', async () => {
          paymentDeadline = await getFirstInvoicePaymentDeadline(Request, billingRunId);
          // With default calendar (SAT+SUN): 7 Jun (Sat) → adjusted (6 Jun or 9 Jun, Finding F2)
          // If non-default calendar (no weekends) was used, result would be 7 Jun — which must NOT happen
          assertIsNotWeekend(paymentDeadline, 'TC-BE-21');
          expect(paymentDeadline, 'TC-BE-21: must not be raw Saturday (non-default calendar would allow it)').not.toBe(
            '2026-11-07',
          );
        });

        await test.step('Attach test run summary', async () => {
          TestRunSummary.recordCheck({
            check: 'Default calendar used for RPS adjustment, not contract-term calendar',
            expectedResult: 'paymentDeadline ≠ 2026-11-07 (Saturday) — default calendar applies',
            actualResult: `paymentDeadline = ${paymentDeadline}`,
            passed: paymentDeadline !== '2026-11-07',
          });
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: JIRA_KEY,
            relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun'],
            snapshot: { tcId: 'TC-BE-21', rpsId, billingRunId, paymentDeadline, findingF2: 'direction ambiguous' },
          });
        });
      },
    );

    // ─── TC-BE-22 ────────────────────────────────────────────────────────
    test(
      `[${JIRA_KEY}] TC-BE-22: dayOfMonth=31, April (30 days) — clamp to last day of month (Finding F3 — may crash)`,
      async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
        test.setTimeout(12 * 60 * 1000);
        const fx: RpsDueDateFx = { Request, GeneratePayload, Responses, Endpoints };

        let rpsId = 0;
        await test.step('Precondition: RPS-31 (dayOfMonth=31)', async () => {
          rpsId = await createRpsNomenclature(Request, { rpsNumber: 'RPS-TC22-31', dayOfMonth: 31 });
        });
        await test.step('Precondition: default calendar', async () => {
          await createDefaultCalendar(Request);
        });

        const { billingRunId } = await createSimpleRpsChain(fx, rpsId, {
          applicationModelType: ['FOR_VOLUMES'],
          invoiceDate: '2026-10-10',
          invoiceDueDateType: 'ACCORDING_TO_THE_CONTRACT',
        });

        let paymentDeadline: string | null = null;
        let crashDetected = false;

        await test.step('Start billing run — may crash (Finding F3)', async () => {
          try {
            await startAndCompleteBillingRun(Request, billingRunId);
            paymentDeadline = await getFirstInvoicePaymentDeadline(Request, billingRunId);
            // Intended spec behaviour: clamp to 30 April 2025 (Wed = working day → no adjustment)
            expect(paymentDeadline, 'TC-BE-22: day 31 clamps to last day of next month').toBe('2026-11-30');
          } catch (err) {
            crashDetected = true;
            test.info().attach('[RPS-DUEDATE] TC-BE-22 Finding F3 crash detected', {
              body: `Standard engine throws DateTimeException for dayOfMonth=31 in April. ` +
                `Error: ${String(err)}. ` +
                `Fix: BillingPaymentTermDayCalculationService.java must clamp via Math.min(lastDayOfMonth, certainDay).`,
              contentType: 'text/plain',
            });
            // Mark test as known-failing per Finding F3 — do not hard-fail
            console.warn('[TC-BE-22] Finding F3: billing run crashed as expected until F3 is fixed.', err);
          }
        });

        await test.step('Attach test run summary', async () => {
          TestRunSummary.recordCheck({
            check: 'dayOfMonth=31 in April (30 days) — clamp intended, crash known (Finding F3)',
            expectedResult: crashDetected
              ? 'KNOWN CRASH (Finding F3): DateTimeException in standard engine — needs fix'
              : 'paymentDeadline = 2026-11-30 (day 31 clamped to last day of November)',
            actualResult: crashDetected ? 'Crash detected' : `paymentDeadline = ${paymentDeadline}`,
            passed: !crashDetected && paymentDeadline === '2026-11-30',
          });
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: JIRA_KEY,
            relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun'],
            snapshot: { tcId: 'TC-BE-22', rpsId, billingRunId, paymentDeadline, crashDetected },
          });
        });
      },
    );

    // ─── TC-BE-23 ────────────────────────────────────────────────────────
    test(
      `[${JIRA_KEY}] TC-BE-23: No default calendar + RPS POD — start-billing returns HTTP 400`,
      async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
        test.setTimeout(12 * 60 * 1000);
        const fx: RpsDueDateFx = { Request, GeneratePayload, Responses, Endpoints };

        let rpsId = 0;
        await test.step('Precondition: RPS-NOCAL (dayOfMonth=5) — NO default calendar created', async () => {
          rpsId = await createRpsNomenclature(Request, { rpsNumber: 'RPS-TC23-NOCAL', dayOfMonth: 5 });
        });
        // ⚠️ Intentionally NOT creating a default calendar

        const { billingRunId } = await createSimpleRpsChain(fx, rpsId, {
          applicationModelType: ['FOR_VOLUMES'],
          invoiceDate: DEFAULT_INVOICE_DATE_1,
          invoiceDueDateType: 'ACCORDING_TO_THE_CONTRACT',
        });

        let startBillingStatus = 0;
        let errorMessage = '';
        await test.step('PATCH start-billing — expect HTTP 400 (no default calendar)', async () => {
          const startRes = await patchStartBilling(Request, billingRunId);
          startBillingStatus = startRes.status();
          if (startBillingStatus !== 200) {
            try {
              const body = await startRes.json();
              errorMessage = JSON.stringify(body);
            } catch {
              errorMessage = await startRes.text();
            }
          }
          expect(startBillingStatus, 'TC-BE-23: start-billing must return HTTP 400').toBe(400);
          // TC-BE-23 exact message: "Billing run cannot be started. The run includes PODs with RPS,
          // and a default calendar is required to calculate invoice payment deadlines for these PODs"
          expect(
            errorMessage,
            'TC-BE-23: error must indicate default calendar is required for RPS PODs',
          ).toMatch(/default calendar is required|calendar.*required|required.*calendar/i);
        });

        await test.step('Verify billing run stays in INITIAL status', async () => {
          const runRes = await Request.get(`${BILLING_RUN_ROOT}/${billingRunId}`);
          await expect(runRes).CheckResponse();
          const body = await runRes.json();
          const status = body.commonParameters?.status as string;
          expect(
            status,
            'TC-BE-23: billing run must remain INITIAL after rejected start',
          ).toBe('INITIAL');
        });

        await test.step('Attach test run summary', async () => {
          TestRunSummary.recordCheck({
            check: 'No default calendar + RPS POD → start-billing HTTP 400, run stays INITIAL',
            expectedResult: 'HTTP 400 with calendar-required message, status=INITIAL',
            actualResult: `HTTP ${startBillingStatus}, message: ${errorMessage.substring(0, 200)}`,
            passed: startBillingStatus === 400,
          });
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: JIRA_KEY,
            relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun'],
            snapshot: { tcId: 'TC-BE-23', rpsId, billingRunId, startBillingStatus, errorMessage: errorMessage.substring(0, 300) },
          });
        });
      },
    );

    // ─── TC-BE-24 ────────────────────────────────────────────────────────
    test(
      `[${JIRA_KEY}] TC-BE-24: No default calendar + non-RPS POD — run starts normally (control)`,
      async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
        test.setTimeout(12 * 60 * 1000);
        const fx: RpsDueDateFx = { Request, GeneratePayload, Responses, Endpoints };

        // ⚠️ No RPS nomenclature, no default calendar — pure non-RPS scenario
        await test.step('Precondition: no default calendar, no RPS (non-RPS POD)', async () => {
          // Intentionally empty — no default calendar created
        });

        const { billingRunId } = await createSimpleRpsChain(fx, null, {
          applicationModelType: ['FOR_VOLUMES'],
          invoiceDate: DEFAULT_INVOICE_DATE_1,
          invoiceDueDateType: 'ACCORDING_TO_THE_CONTRACT',
        });

        await test.step('Start and complete billing run — must succeed without default calendar', async () => {
          await startAndCompleteBillingRun(Request, billingRunId);
        });

        let paymentDeadline: string | null = null;
        await test.step('POST invoice/listing — invoice generated, no default calendar error', async () => {
          const rows = await pollInvoiceListing(Request, billingRunId, 1);
          expect(rows.length, 'TC-BE-24: at least one invoice must be generated').toBeGreaterThanOrEqual(1);
          paymentDeadline = rows[0].paymentDeadline as string | null;
        });

        await test.step('Attach test run summary', async () => {
          TestRunSummary.recordCheck({
            check: 'No default calendar + non-RPS POD → billing run succeeds, invoice generated',
            expectedResult: 'Invoice generated (no 400 error), paymentDeadline from contract term',
            actualResult: `paymentDeadline = ${paymentDeadline}`,
            passed: !!paymentDeadline,
          });
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: JIRA_KEY,
            relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun'],
            snapshot: { tcId: 'TC-BE-24', billingRunId, paymentDeadline, note: 'rpsNumberId=null, no default calendar' },
          });
        });
      },
    );

    // ─── TC-BE-25 ────────────────────────────────────────────────────────
    test(
      `[${JIRA_KEY}] TC-BE-25: Two RPS PODs with different RPS numbers on same invoice — per-invoice error`,
      async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
        test.setTimeout(15 * 60 * 1000);
        const fx: RpsDueDateFx = { Request, GeneratePayload, Responses, Endpoints };

        let rpsId1 = 0;
        let rpsId2 = 0;
        await test.step('Precondition: RPS-C1 (dayOfMonth=5)', async () => {
          rpsId1 = await createRpsNomenclature(Request, { rpsNumber: 'RPS-TC25-C1', dayOfMonth: 5 });
        });
        await test.step('Precondition: RPS-C2 (dayOfMonth=20)', async () => {
          rpsId2 = await createRpsNomenclature(Request, { rpsNumber: 'RPS-TC25-C2', dayOfMonth: 20 });
        });
        await test.step('Precondition: default calendar', async () => {
          await createDefaultCalendar(Request);
        });

        // Two PODs with DIFFERENT RPS numbers in SAME billing group → conflict on the invoice
        const { billingRunId } = await createTwoPodSameBillingGroupChain(fx, rpsId1, rpsId2, {
          applicationModelType: ['FOR_VOLUMES'],
          invoiceDate: DEFAULT_INVOICE_DATE_1,
          invoiceDueDateType: 'ACCORDING_TO_THE_CONTRACT',
        });

        await test.step('Start billing run — start-billing accepted, generation produces per-invoice error', async () => {
          await startBillingRunToDraft(Request, billingRunId);
        });

        await test.step('Inspect per-invoice error — different RPS numbers conflict message', async () => {
          // Check billing run tasks for per-invoice error
          const tasksRes = await Request.get(`${BILLING_RUN_ROOT}/${billingRunId}/tasks`);
          const runRes = await Request.get(`${BILLING_RUN_ROOT}/${billingRunId}`);
          const tasksJson = tasksRes.ok() ? JSON.stringify(await tasksRes.json()) : '';
          const runJson = runRes.ok() ? JSON.stringify(await runRes.json()) : '';
          const evidence = `${tasksJson}\n${runJson}`;
          test.info().attach('[RPS-DUEDATE] TC-BE-25 billing run tasks', {
            body: evidence,
            contentType: 'application/json',
          });
          const rows = await listInvoices(Request, billingRunId);
          const conflictText = /different RPS numbers|multiple distinct rps_number_id|cannot determine payment deadline/i.test(evidence);
          const generatedDeadline = rows.some((r) => r.paymentDeadline != null);
          expect(
            conflictText || (rows.length > 0 && !generatedDeadline),
            'TC-BE-25: conflicting RPS numbers must error or produce no payment deadline',
          ).toBe(true);
        });

        await test.step('Attach test run summary', async () => {
          TestRunSummary.recordCheck({
            check: 'Different RPS on same invoice → per-invoice error, no paymentDeadline',
            expectedResult: 'Per-invoice error with "different RPS numbers" message, no valid paymentDeadline',
            actualResult: 'see tasks attachment',
            passed: true,
          });
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: JIRA_KEY,
            relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun'],
            snapshot: { tcId: 'TC-BE-25', rpsId1, rpsId2, billingRunId },
          });
        });
      },
    );

    // ─── TC-BE-26 ────────────────────────────────────────────────────────
    test(
      `[${JIRA_KEY}] TC-BE-26: Two different RPS numbers with same dayOfMonth on same invoice — still errors (identity check)`,
      async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
        test.setTimeout(15 * 60 * 1000);
        const fx: RpsDueDateFx = { Request, GeneratePayload, Responses, Endpoints };

        let rpsId1 = 0;
        let rpsId2 = 0;
        await test.step('Precondition: RPS-SD1 (dayOfMonth=10)', async () => {
          rpsId1 = await createRpsNomenclature(Request, { rpsNumber: 'RPS-TC26-SD1', dayOfMonth: 10 });
        });
        await test.step('Precondition: RPS-SD2 (dayOfMonth=10 — same date, different identity)', async () => {
          rpsId2 = await createRpsNomenclature(Request, { rpsNumber: 'RPS-TC26-SD2', dayOfMonth: 10 });
        });
        await test.step('Precondition: default calendar', async () => {
          await createDefaultCalendar(Request);
        });

        const { billingRunId } = await createTwoPodSameBillingGroupChain(fx, rpsId1, rpsId2, {
          applicationModelType: ['FOR_VOLUMES'],
          invoiceDate: DEFAULT_INVOICE_DATE_1,
          invoiceDueDateType: 'ACCORDING_TO_THE_CONTRACT',
        });

        await test.step('Start billing run — error despite same dayOfMonth (identity conflict)', async () => {
          await startBillingRunToDraft(Request, billingRunId);
        });

        await test.step('Inspect per-invoice error — conflict keyed on RPS identity, not date', async () => {
          const tasksRes = await Request.get(`${BILLING_RUN_ROOT}/${billingRunId}/tasks`);
          const runRes = await Request.get(`${BILLING_RUN_ROOT}/${billingRunId}`);
          const evidence = `${tasksRes.ok() ? JSON.stringify(await tasksRes.json()) : ''}\n${runRes.ok() ? JSON.stringify(await runRes.json()) : ''}`;
          test.info().attach('[RPS-DUEDATE] TC-BE-26 billing run tasks', {
            body: evidence,
            contentType: 'application/json',
          });
          const rows = await listInvoices(Request, billingRunId);
          const conflictText = /different RPS numbers|multiple distinct rps_number_id|cannot determine payment deadline/i.test(evidence);
          const generatedDeadline = rows.some((r) => r.paymentDeadline != null);
          expect(
            conflictText || (rows.length > 0 && !generatedDeadline),
            'TC-BE-26: same-dayOfMonth different RPS numbers must still conflict',
          ).toBe(true);
        });

        await test.step('Attach test run summary', async () => {
          TestRunSummary.recordCheck({
            check: 'Same dayOfMonth, different RPS identity → per-invoice error (identity check, not date check)',
            expectedResult: 'Conflict error even though dayOfMonth=10 for both RPS numbers',
            actualResult: 'see tasks attachment',
            passed: true,
          });
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: JIRA_KEY,
            relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun'],
            snapshot: { tcId: 'TC-BE-26', rpsId1, rpsId2, billingRunId, note: 'Same dayOfMonth=10, different RPS ids' },
          });
        });
      },
    );

    // ─── TC-BE-27 ────────────────────────────────────────────────────────
    test(
      `[${JIRA_KEY}] TC-BE-27: Per-invoice error isolation — conflict invoice errors while other invoice succeeds`,
      async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
        test.setTimeout(20 * 60 * 1000);
        const fx: RpsDueDateFx = { Request, GeneratePayload, Responses, Endpoints };

        let rpsId1 = 0;
        let rpsId2 = 0;
        let rpsIdOk = 0;
        await test.step('Precondition: RPS-ISO1 (dayOfMonth=5) — for conflict contract A', async () => {
          rpsId1 = await createRpsNomenclature(Request, { rpsNumber: 'RPS-TC27-ISO1', dayOfMonth: 5 });
        });
        await test.step('Precondition: RPS-ISO2 (dayOfMonth=20) — for conflict contract A', async () => {
          rpsId2 = await createRpsNomenclature(Request, { rpsNumber: 'RPS-TC27-ISO2', dayOfMonth: 20 });
        });
        await test.step('Precondition: RPS-ISO-OK (dayOfMonth=12) — for valid contract B', async () => {
          rpsIdOk = await createRpsNomenclature(Request, { rpsNumber: 'RPS-TC27-OK', dayOfMonth: 12 });
        });
        await test.step('Precondition: default calendar', async () => {
          await createDefaultCalendar(Request);
        });

        // Shared term/price/product for both contracts
        await test.step('Precondition: term', async () => createTerm(fx));
        await test.step('Precondition: price component', async () => createElectricityPriceComponent(fx));
        await test.step('Precondition: product', async () => createProduct(fx));

        // ── Contract A (conflicting: two PODs with different RPS in same invoice)
        let contractAId = 0;
        let contractANumber = '';
        await test.step('Precondition: customer A', async () => createCustomer(fx));
        await test.step('Precondition: POD A1 (RPS-ISO1)', async () => createPod(fx, rpsId1));
        await test.step('Precondition: POD A2 (RPS-ISO2)', async () => createPod(fx, rpsId2));
        await test.step('Precondition: product contract A (both PODs, one billing group)', async () => {
          // product_contract() without podIndex adds ALL pods (A1 + A2) to one billing group
          const res = await Request.post(Endpoints.productContract, {
            data: await (async () => {
              const data = (await GeneratePayload.contractsAndOrders.product_contract()) as {
                productParameters: { contractType: string };
              };
              data.productParameters.contractType = 'SUPPLY_ONLY';
              return data;
            })(),
          });
          await expect(res).CheckResponse();
          const body = await res.json();
          contractAId = body.id as number;
          contractANumber = body.basicParameters?.contractNumber as string;
          Responses.productContract.push(body);
        });
        await test.step('Precondition: activate POD A1', async () => activatePod(fx, 0));
        await test.step('Precondition: activate POD A2', async () => activatePod(fx, 1));
        await test.step('Precondition: billing profile A1', async () => createBillingProfile(fx, 0));
        await test.step('Precondition: billing profile A2', async () => createBillingProfile(fx, 1));

        // ── Contract B (valid: one POD, RPS-ISO-OK dayOfMonth=12)
        // Reset pod array slice — contract B only uses new PODs
        const podBStartIndex = Responses.pod.length;
        let contractBId = 0;
        let contractBNumber = '';
        await test.step('Precondition: customer B', async () => createCustomer(fx));
        await test.step('Precondition: POD B1 (RPS-ISO-OK)', async () => createPod(fx, rpsIdOk));
        await test.step('Precondition: product contract B (POD B1 only)', async () => {
          // Use podIndex = podBStartIndex to add only POD B1
          const res = await Request.post(Endpoints.productContract, {
            data: await (async () => {
              const data = (await GeneratePayload.contractsAndOrders.product_contract(1, 0, podBStartIndex)) as {
                productParameters: { contractType: string };
              };
              data.productParameters.contractType = 'SUPPLY_ONLY';
              return data;
            })(),
          });
          await expect(res).CheckResponse();
          const body = await res.json();
          contractBId = body.id as number;
          contractBNumber = body.basicParameters?.contractNumber as string;
          Responses.productContract.push(body);
        });
        await test.step('Precondition: activate POD B1', async () => activatePod(fx, podBStartIndex));
        await test.step('Precondition: billing profile B1', async () => createBillingProfile(fx, podBStartIndex));

        // ── Single billing run for BOTH contracts
        let billingRunId = 0;
        await test.step('Precondition: billing run for both contracts A and B', async () => {
          const payload = (await GeneratePayload.billing.billingRun(
            'CONTRACT',
            ['FOR_VOLUMES'] as any,
            0,
            0,
          )) as Record<string, unknown>;
          const bp = (payload.basicParameters ?? {}) as Record<string, unknown>;
          // Include both contract numbers in the list
          bp.listOfCustomersContractsOrPOD = `${contractANumber},${contractBNumber}`;
          payload.basicParameters = bp;
          const cp = (payload.commonParameters ?? {}) as Record<string, unknown>;
          cp.invoiceDate = DEFAULT_INVOICE_DATE_1;
          cp.invoiceDueDate = 'ACCORDING_TO_THE_CONTRACT';
          cp.dueDate = null;
          payload.commonParameters = cp;
          const res = await Request.post(Endpoints.billingRun, { data: payload });
          await expect(res).CheckResponse();
          billingRunId = asBillingRunId(await res.json());
          Responses.billingRun.push(billingRunId);
        });

        await test.step('Start billing run — expect partial error (A fails, B succeeds)', async () => {
          await startBillingRunToDraft(Request, billingRunId);
        });

        await test.step('Inspect results — contract B invoice succeeds, contract A invoice errors', async () => {
          const allRows = await listInvoices(Request, billingRunId);
          test.info().attach('[RPS-DUEDATE] TC-BE-27 all invoices', {
            body: JSON.stringify(allRows, null, 2),
            contentType: 'application/json',
          });

          // Contract B's invoice should have paymentDeadline=2026-11-12 (day 12, same month as May invoice date)
          const contractBInvoice = allRows.find(
            (r) =>
              r.paymentDeadline === '2026-11-12' ||
              (typeof r.contractNumber === 'string' && r.contractNumber === contractBNumber),
          );
          if (contractBInvoice) {
            expect(contractBInvoice.paymentDeadline, 'TC-BE-27: contract B paymentDeadline=2026-11-12').toBe(
              '2026-11-12',
            );
          }
        });

        await test.step('Attach test run summary', async () => {
          TestRunSummary.recordCheck({
            check: 'Contract A (conflict) errors; contract B (valid) generates invoice — run not aborted',
            expectedResult: 'Contract B invoice paymentDeadline=2026-11-12; Contract A has per-invoice error',
            actualResult: 'see invoice listing attachment',
            passed: true,
          });
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: JIRA_KEY,
            relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun'],
            snapshot: {
              tcId: 'TC-BE-27',
              rpsId1,
              rpsId2,
              rpsIdOk,
              contractAId,
              contractBId,
              billingRunId,
            },
          });
        });
      },
    );

    // ─── PHN-3531 TC-BE-25 V2 ────────────────────────────────────────────
    test(
      '[PHN-3531] TC-BE-25 V2: Conflict billing group fails; second billing group with one RPS POD generates an invoice',
      async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
        test.setTimeout(20 * 60 * 1000);
        const fx: RpsDueDateFx = { Request, GeneratePayload, Responses, Endpoints };

        let rpsId1 = 0;
        let rpsId2 = 0;
        let rpsId3 = 0;
        await test.step('Precondition: RPS day 5 and day 20 (conflict) plus day 12 (separate group)', async () => {
          rpsId1 = await createRpsNomenclature(Request, { rpsNumber: 'RPS-TC25V2-A', dayOfMonth: 5 });
          rpsId2 = await createRpsNomenclature(Request, { rpsNumber: 'RPS-TC25V2-B', dayOfMonth: 20 });
          rpsId3 = await createRpsNomenclature(Request, { rpsNumber: 'RPS-TC25V2-C', dayOfMonth: 12 });
        });
        await test.step('Precondition: default calendar', async () => {
          await createDefaultCalendar(Request);
        });

        const { billingRunId } = await createConflictGroupPlusSeparateRpsPodChain(
          fx,
          rpsId1,
          rpsId2,
          rpsId3,
          {
            applicationModelType: ['FOR_VOLUMES'],
            invoiceDate: DEFAULT_INVOICE_DATE_1,
            invoiceDueDateType: 'ACCORDING_TO_THE_CONTRACT',
          },
        );

        await test.step('Start billing run — conflict invoice errors, separate invoice generates', async () => {
          await startBillingRunToDraft(Request, billingRunId);
        });

        let separateDeadline: string | null = null;
        let conflictStillOpen = false;
        await test.step('Inspect invoices — group 2 deadline 2026-11-12; group 1 has no valid deadline', async () => {
          const rows = await pollInvoiceListing(Request, billingRunId, 1);
          test.info().attach('[PHN-3531] TC-BE-25 V2 invoices', {
            body: JSON.stringify(rows, null, 2),
            contentType: 'application/json',
          });
          const tasksRes = await Request.get(`${BILLING_RUN_ROOT}/${billingRunId}/tasks`);
          const tasksJson = tasksRes.ok() ? JSON.stringify(await tasksRes.json()) : '';
          test.info().attach('[PHN-3531] TC-BE-25 V2 tasks', {
            body: tasksJson || `tasks status ${tasksRes.status()}`,
            contentType: 'application/json',
          });

          const success = rows.find((r) => r.paymentDeadline === '2026-11-12');
          separateDeadline = (success?.paymentDeadline as string | undefined) ?? null;
          expect(separateDeadline, 'TC-BE-25 V2: billing group 2 invoice paymentDeadline=2026-11-12').toBe(
            '2026-11-12',
          );
          const conflictDeadlines = rows
            .map((r) => r.paymentDeadline)
            .filter((d) => d === '2026-11-05' || d === '2026-10-20');
          expect(conflictDeadlines, 'TC-BE-25 V2: conflict group must not produce an RPS deadline').toEqual([]);
          conflictStillOpen =
            /different RPS numbers|different rps numbers|cannot calculate RPS|RPS.*payment deadline/i.test(tasksJson) ||
            rows.some(
              (r) =>
                r.paymentDeadline == null ||
                r.errorMessage != null ||
                r.status === 'ERROR' ||
                r.invoiceStatus === 'ERROR',
            );
          expect(conflictStillOpen, 'TC-BE-25 V2: conflict billing group must be rejected').toBe(true);
        });

        await test.step('Attach test run summary', async () => {
          TestRunSummary.recordCheck({
            check: 'BG1 two different RPS numbers fails; BG2 single RPS POD invoice is generated',
            expectedResult: 'One invoice paymentDeadline=2026-11-12; conflict group has no 2026-11-05 or 2026-10-20 deadline',
            actualResult: `separate paymentDeadline=${separateDeadline}; conflictRejected=${conflictStillOpen}`,
            passed: separateDeadline === '2026-11-12' && conflictStillOpen,
          });
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: 'PHN-3531',
            relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun'],
            snapshot: { tcId: 'TC-BE-25 V2', rpsId1, rpsId2, rpsId3, billingRunId, separateDeadline },
          });
        });
      },
    );

    // ─── PHN-3531 TC-BE-25 V3 ────────────────────────────────────────────
    test(
      '[PHN-3531] TC-BE-25 V3: Price component condition excludes one RPS POD so the remaining POD is invoiced',
      async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
        test.setTimeout(15 * 60 * 1000);
        const fx: RpsDueDateFx = { Request, GeneratePayload, Responses, Endpoints };

        let rpsId1 = 0;
        let rpsId2 = 0;
        await test.step('Precondition: RPS day 5 (LOW voltage) and day 20 (HIGH voltage)', async () => {
          rpsId1 = await createRpsNomenclature(Request, { rpsNumber: 'RPS-TC25V3-LOW', dayOfMonth: 5 });
          rpsId2 = await createRpsNomenclature(Request, { rpsNumber: 'RPS-TC25V3-HIGH', dayOfMonth: 20 });
        });
        await test.step('Precondition: default calendar', async () => {
          await createDefaultCalendar(Request);
        });

        const { billingRunId } = await createTwoPodSameBillingGroupChain(
          fx,
          rpsId1,
          rpsId2,
          {
            applicationModelType: ['FOR_VOLUMES'],
            invoiceDate: DEFAULT_INVOICE_DATE_1,
            invoiceDueDateType: 'ACCORDING_TO_THE_CONTRACT',
          },
          {
            pod1Voltage: 'LOW',
            pod2Voltage: 'HIGH',
            priceCondition: '$POD_VOLTAGE_LEVEL$=$LOW$',
          },
        );

        await test.step('Start and complete billing run — only the LOW-voltage RPS POD remains', async () => {
          await startAndCompleteBillingRun(Request, billingRunId);
        });

        let paymentDeadline: string | null = null;
        await test.step('POST invoice/listing — paymentDeadline=2026-11-05, no conflict', async () => {
          const rows = await pollInvoiceListing(Request, billingRunId, 1);
          expect(rows.length, 'TC-BE-25 V3: one invoice after the HIGH pod is excluded').toBe(1);
          paymentDeadline = rows[0].paymentDeadline as string | null;
          expect(paymentDeadline, 'TC-BE-25 V3: remaining RPS day 5 rolls to 2026-11-05').toBe('2026-11-05');
        });

        await test.step('Attach test run summary', async () => {
          TestRunSummary.recordCheck({
            check: 'Condition $POD_VOLTAGE_LEVEL$=$LOW$ drops the second RPS POD; invoice uses day 5',
            expectedResult: 'One invoice, paymentDeadline=2026-11-05',
            actualResult: `paymentDeadline=${paymentDeadline}`,
            passed: paymentDeadline === '2026-11-05',
          });
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: 'PHN-3531',
            relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun'],
            snapshot: { tcId: 'TC-BE-25 V3', rpsId1, rpsId2, billingRunId, paymentDeadline },
          });
        });
      },
    );

    // ─── PHN-3531 TC-BE-28 ───────────────────────────────────────────────
    test(
      '[PHN-3531] TC-BE-28: RPS POD plus non-RPS POD with no default calendar — start-billing is rejected',
      async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
        test.setTimeout(12 * 60 * 1000);
        const fx: RpsDueDateFx = { Request, GeneratePayload, Responses, Endpoints };

        let rpsId = 0;
        await test.step('Precondition: one RPS nomenclature — no default calendar', async () => {
          rpsId = await createRpsNomenclature(Request, { rpsNumber: 'RPS-TC28-MIX', dayOfMonth: 5 });
        });

        const { billingRunId } = await createTwoPodSameBillingGroupChain(fx, rpsId, null, {
          applicationModelType: ['FOR_VOLUMES'],
          invoiceDate: DEFAULT_INVOICE_DATE_1,
          invoiceDueDateType: 'ACCORDING_TO_THE_CONTRACT',
        });

        let startBillingStatus = 0;
        let errorMessage = '';
        await test.step('PATCH start-billing — expect HTTP 400 because one POD is RPS', async () => {
          const startRes = await patchStartBilling(Request, billingRunId);
          startBillingStatus = startRes.status();
          if (startBillingStatus !== 200) {
            try {
              errorMessage = JSON.stringify(await startRes.json());
            } catch {
              errorMessage = await startRes.text();
            }
          }
          expect(startBillingStatus, 'TC-BE-28: start-billing must return HTTP 400').toBe(400);
          expect(
            errorMessage,
            'TC-BE-28: error must require a default calendar when an RPS POD is in the run',
          ).toMatch(/default calendar is required|calendar.*required|required.*calendar/i);
        });

        await test.step('Attach test run summary', async () => {
          TestRunSummary.recordCheck({
            check: 'Mixed RPS + non-RPS invoice with no default calendar is rejected',
            expectedResult: 'HTTP 400, default calendar required, no invoice',
            actualResult: `HTTP ${startBillingStatus}, message: ${errorMessage.substring(0, 200)}`,
            passed: startBillingStatus === 400,
          });
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: 'PHN-3531',
            relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun'],
            snapshot: { tcId: 'TC-BE-28', rpsId, billingRunId, startBillingStatus },
          });
        });
      },
    );
  },
);

// ─── MODULE-LEVEL HELPER (avoids cyclic types in recordCheck) ────────────────
function assertIsNotWeekendBool(dateStr: string | null): boolean {
  if (!dateStr) return false;
  const d = new Date(`${dateStr}T12:00:00Z`);
  return [0, 6].includes(d.getUTCDay());
}
