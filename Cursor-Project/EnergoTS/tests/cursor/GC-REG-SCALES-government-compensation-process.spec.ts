/**
 * GC-REG-SCALES — Government Compensation process regression (data by scales volume source).
 *
 * Reference spec(s):
 * - tests/cursor/GC-REG-government-compensation-process.spec.ts / gc-reg-government-compensation-regression.fixtures.ts
 * - tests/cursor/pdt-3223-invoice-json-tablepcscales-listpc-order.fixtures.ts
 * - tests/cursor/phn-4048-billing-data-by-scales-create-and-edit.fixtures.ts
 * - tests/cursor/PDT-3087-government-compensation-defer-to-pdf.spec.ts / .fixtures.ts
 *
 * Swagger refresh: executed this session via update-swagger-specs.ps1 (dev/test/dev2/experiment/prod).
 * Backend TC: Cursor-Project/test_cases/Backend/Government_Compensation_Regression_Data_By_Scales.md
 *
 * Product scope: same GC APIs as GC-REG; invoice volumes from POST /billing-by-scales
 * (PC BY_SCALES). Billing run remains FOR_VOLUMES CONTRACT.
 */

import { test, expect } from './cursor-test.fixtures';
import {
  finalizeTestRunSummary,
  buildProductContractTabLinks,
} from './shared/manual-verification-links.fixtures';
import { randomGens } from '../../utils/randomGens';
import {
  GC_REG_SCALES_KEY,
  ZERO_AMOUNT_ERROR,
  EDIT_LOCKED_ERROR,
  REGEN_NOT_REAL_ERROR,
  REGEN_NULL_INDEX_ERROR,
  ZERO_AMOUNT_WIKI_EN,
  ZERO_AMOUNT_WIKI_BG,
  NON_NUMERIC_DOCUMENT_NUMBER,
  DEV_PORTAL_BASE,
  runGcCreatePrechain,
  runGcScalesContractAndBbsPrechain,
  runGcScalesBillingPrechainWithDirectDebit,
  runGcScalesBillingPrechainDistinctInstitutionDd,
  runGcScalesBillingPrechainInvoiceDdInstitutionNone,
  createGcExpectingCreated,
  postGcRaw,
  putGcRaw,
  patchRegenerateCompensationsOk,
  patchRegenerateCompensationsRaw,
  listCompensationsByNumber,
  getCompensation,
  getInvoice,
  getCustomerLiability,
  getCustomerReceivable,
  completeForVolumesToAccounted,
  createForVolumesDraftBillingRun,
  extractPartyCustomerId,
  resolveInvoiceMainCustomerLiability,
  findCompensationByNumber,
  resolveEntityIdentifier,
  resolveCurrencyName,
  buildGovernmentCompensationMassImportBuffer,
  uploadGovernmentCompensationMassImport,
  pollGovernmentCompensationMassImportComplete,
  toAmountNumber,
  resolveInvoiceCustomerLiabilityId,
  assertGcUninvoicedEmptyFinancials,
  assertPositiveApplyAttachments,
  assertNegativeApplyAttachments,
  billingGroupIsEmpty,
  isDirectDebitOn,
  shortId,
  buildCompensationPayload,
  todayIso,
  isEmptyOutgoingExternal,
  calendarDatePart,
  pickInvoiceIncomeAccount,
  pickInvoiceCostCenter,
  pickLiabilityIban,
  pickReceivableBankAccount,
  buildDevBillingRunPreviewLink,
  buildDevCompensationPreviewLink,
  buildDevInvoicePreviewLink,
} from './gc-reg-scales-government-compensation-regression.fixtures';

const CREATE_TIMEOUT_MS = 8 * 60 * 1000;
const BILLING_TIMEOUT_MS = 45 * 60 * 1000;

test.describe(
  '[GC-REG-SCALES]: Government Compensation process regression (data by scales)',
  { tag: ['@billing', '@compensations', '@GC-REG-SCALES'] },
  () => {
    // Sequential billing: run this file with --workers=1. Do not use mode:'serial'
    // (Playwright skips remaining independent TCs after one failure).
    test('[GC-REG-SCALES]: TC-BE-1 – Create government compensation with mandatory fields → UNINVOICED', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(CREATE_TIMEOUT_MS);
      const fx = { Request, GeneratePayload, Responses, Endpoints };

      const pre = await test.step('Precondition: customer + recipient + POD', async () =>
        runGcCreatePrechain(fx),
      );

      const created = await test.step('POST /government-compensations amount 100.00', async () =>
        createGcExpectingCreated(fx, {
          customerId: pre.customerId,
          podId: pre.podId,
          recipientId: pre.recipientId,
          documentPeriod: pre.documentPeriod,
          documentAmount: 100,
          volumes: 10,
          price: 10,
          reason: 'GC-REG-SCALES-TC-BE-1',
        }),
      );
      TestRunSummary.registerPayload('compensation', created.payload);

      await test.step('GET view + listing UNINVOICED', async () => {
        const view = await getCompensation(Request, created.id);
        expect(shortId(view, 'customer'), 'customer.id').toBe(pre.customerId);
        expect(shortId(view, 'pod'), 'pod.id').toBe(pre.podId);
        expect(shortId(view, 'recipient'), 'recipient.id').toBe(pre.recipientId);
        expect(toAmountNumber(view.documentAmount), 'documentAmount').toBe(100);
        assertGcUninvoicedEmptyFinancials(view, 'TC-BE-1');

        const listing = await listCompensationsByNumber(Request, created.payload.number);
        expect(listing.status, 'listing HTTP 206').toBe(206);
        const row = listing.content.find(
          (r) => String(r.number ?? '').trim() === created.payload.number,
        );
        expect(row, 'listing contains created number').toBeTruthy();
        expect(row?.compensationStatus, 'listing status').toBe('UNINVOICED');

        TestRunSummary.recordCheck({
          check: 'TC-BE-1 create UNINVOICED',
          expectedResult: 'HTTP 201; GET UNINVOICED; listing 206 includes row; no L/R/invoice.',
          actualResult: `As expected — gcId=${created.id} status=${view.compensationStatus} listing=${listing.status}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: GC_REG_SCALES_KEY,
          relevantEntityKeys: ['customer', 'pod'],
          extraLinks: {
            compensation: [buildDevCompensationPreviewLink(created.id)],
          },
          snapshot: { tc: 'TC-BE-1', gcId: created.id, number: created.payload.number },
        });
      });
    });

    test('[GC-REG-SCALES]: TC-BE-2 – Create with documentAmount = 0 is rejected and no row is stored', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(CREATE_TIMEOUT_MS);
      const fx = { Request, GeneratePayload, Responses, Endpoints };

      const pre = await test.step('Precondition: customer + recipient + POD', async () =>
        runGcCreatePrechain(fx),
      );

      const uniqueNumber = `GC-ZERO-${randomGens.generateRandomString(true, false, 8)}`;
      const payload = buildCompensationPayload(GeneratePayload, {
        customerId: pre.customerId,
        podId: pre.podId,
        recipientId: pre.recipientId,
        documentPeriod: pre.documentPeriod,
        documentAmount: 1,
        volumes: 1,
        price: 1,
        reason: 'zero-amount-reject',
      });
      payload.number = uniqueNumber;
      payload.date = todayIso();
      payload.documentAmount = 0;
      TestRunSummary.registerPayload('compensation', payload);

      await test.step('POST amount 0 → 400; listing has no row', async () => {
        const posted = await postGcRaw(fx, payload);
        expect(posted.status, 'zero amount HTTP 400').toBe(400);
        expect(posted.bodyText, 'zero amount error fragment').toContain(ZERO_AMOUNT_ERROR);

        const listing = await listCompensationsByNumber(Request, uniqueNumber);
        expect(listing.status).toBe(206);
        const hit = listing.content.find(
          (r) => String(r.number ?? '').trim() === uniqueNumber,
        );
        expect(hit, 'no ACTIVE zero-amount GC stored').toBeFalsy();

        TestRunSummary.recordCheck({
          check: 'TC-BE-2 zero amount rejected',
          expectedResult: `HTTP 400 containing ${ZERO_AMOUNT_ERROR}; listing has no row.`,
          actualResult: `As expected — status=${posted.status} listingRows=${listing.content.length}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: GC_REG_SCALES_KEY,
          relevantEntityKeys: ['customer', 'pod'],
          snapshot: { tc: 'TC-BE-2', number: uniqueNumber },
        });
      });
    });

    test('[GC-REG-SCALES]: TC-BE-3 – Create with negative documentAmount → UNINVOICED', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(CREATE_TIMEOUT_MS);
      const fx = { Request, GeneratePayload, Responses, Endpoints };

      const pre = await test.step('Precondition: customer + recipient + POD', async () =>
        runGcCreatePrechain(fx),
      );

      const created = await test.step('POST documentAmount -50.00', async () =>
        createGcExpectingCreated(fx, {
          customerId: pre.customerId,
          podId: pre.podId,
          recipientId: pre.recipientId,
          documentPeriod: pre.documentPeriod,
          documentAmount: -50,
          volumes: 10,
          price: 5,
          reason: 'GC-REG-SCALES-TC-BE-3',
        }),
      );
      TestRunSummary.registerPayload('compensation', created.payload);

      await test.step('GET view UNINVOICED negative amount', async () => {
        const view = await getCompensation(Request, created.id);
        expect(toAmountNumber(view.documentAmount), 'documentAmount -50').toBe(-50);
        assertGcUninvoicedEmptyFinancials(view, 'TC-BE-3');

        TestRunSummary.recordCheck({
          check: 'TC-BE-3 negative amount create',
          expectedResult: 'HTTP 201; UNINVOICED; amount -50; no L/R/invoice.',
          actualResult: `As expected — gcId=${created.id} amount=${view.documentAmount} status=${view.compensationStatus}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: GC_REG_SCALES_KEY,
          relevantEntityKeys: ['customer', 'pod'],
          extraLinks: { compensation: [buildDevCompensationPreviewLink(created.id)] },
          snapshot: { tc: 'TC-BE-3', gcId: created.id },
        });
      });
    });

    test('[GC-REG-SCALES]: TC-BE-4 – Edit is rejected when compensation status is INVOICED', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      Nomenclatures,
      TestRunSummary,
    }) => {
      test.setTimeout(BILLING_TIMEOUT_MS);
      const fx = { Request, GeneratePayload, Responses, Endpoints, Nomenclatures };

      const pre = await test.step('Precondition: FOR_VOLUMES contract + data by scales', async () =>
        runGcScalesContractAndBbsPrechain(fx),
      );

      const created = await test.step('Precondition: UNINVOICED GC amount 100', async () =>
        createGcExpectingCreated(fx, {
          customerId: pre.customerId,
          podId: pre.podId,
          recipientId: pre.recipientCustomerId,
          documentPeriod: pre.documentPeriod,
          documentAmount: 100,
          volumes: 10,
          price: 10,
          reason: 'GC-REG-SCALES-TC-BE-4',
        }),
      );
      TestRunSummary.registerPayload('compensation', created.payload);

      const billed = await test.step('Precondition: Standard Billing through accounting', async () =>
        completeForVolumesToAccounted(fx),
      );

      await test.step('PUT edit INVOICED GC → 400; GET unchanged', async () => {
        const before = await getCompensation(Request, created.id);
        expect(before.compensationStatus, 'must be INVOICED before edit').toBe('INVOICED');
        const originalReason = String(before.reason ?? created.payload.reason);
        const originalAmount = toAmountNumber(before.documentAmount);

        const editPayload = {
          ...created.payload,
          reason: 'GC-REG-SCALES-TC-BE-4-EDIT',
          documentAmount: 120,
        };
        const put = await putGcRaw(fx, created.id, editPayload);
        expect(put.status, 'INVOICED edit HTTP 400').toBe(400);
        expect(put.bodyText, 'locked / cannot be updated').toContain(EDIT_LOCKED_ERROR);

        const after = await getCompensation(Request, created.id);
        expect(after.compensationStatus).toBe('INVOICED');
        expect(String(after.reason ?? '')).toBe(originalReason);
        expect(toAmountNumber(after.documentAmount)).toBe(originalAmount);

        TestRunSummary.recordCheck({
          check: 'TC-BE-4 INVOICED edit rejected',
          expectedResult: 'PUT 400 cannot be updated; GET keeps original amount/reason INVOICED.',
          actualResult: `As expected — put=${put.status} amount=${after.documentAmount} reason=${after.reason}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: GC_REG_SCALES_KEY,
          relevantEntityKeys: ['customer', 'productContract', 'billingRun', 'invoice'],
          extraLinks: {
            ...buildProductContractTabLinks(pre.contractId, DEV_PORTAL_BASE),
            billingRun: [buildDevBillingRunPreviewLink(billed.billingRunId)],
            compensation: [buildDevCompensationPreviewLink(created.id)],
            invoice: [buildDevInvoicePreviewLink(billed.invoiceId)],
          },
          snapshot: { tc: 'TC-BE-4', gcId: created.id, invoiceId: billed.invoiceId },
        });
      });
    });

    test('[GC-REG-SCALES]: TC-BE-5 – E2E amount > 0 — Start Accounting → INVOICED, index 0, L recipient, R customer, offset', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      Nomenclatures,
      TestRunSummary,
    }) => {
      test.setTimeout(BILLING_TIMEOUT_MS);
      const fx = { Request, GeneratePayload, Responses, Endpoints, Nomenclatures };

      const pre = await test.step('Precondition: FOR_VOLUMES contract + data by scales', async () =>
        runGcScalesContractAndBbsPrechain(fx),
      );

      const created = await test.step('Precondition: GC amount 100.00', async () =>
        createGcExpectingCreated(fx, {
          customerId: pre.customerId,
          podId: pre.podId,
          recipientId: pre.recipientCustomerId,
          documentPeriod: pre.documentPeriod,
          documentAmount: 100,
          volumes: 10,
          price: 10,
          reason: 'GC-REG-SCALES-TC-BE-5',
        }),
      );
      TestRunSummary.registerPayload('compensation', created.payload);

      const billed = await test.step('Standard Billing start-billing → PDF → accounting', async () =>
        completeForVolumesToAccounted(fx),
      );

      await test.step('Assert INVOICED index 0, parties, offset', async () => {
        const invoice = await getInvoice(Request, billed.invoiceId);
        expect(invoice.invoiceStatus, 'invoice REAL').toBe('REAL');

        const comp = await getCompensation(Request, created.id);
        assertPositiveApplyAttachments(comp, billed.invoiceId, 'TC-BE-5');

        const recipientLiabilityId = shortId(comp, 'liabilityForRecipient');
        const customerReceivableId = shortId(comp, 'receivableForCustomer');
        expect(recipientLiabilityId).toBeTruthy();
        expect(customerReceivableId).toBeTruthy();

        const recipientLiability = await getCustomerLiability(Request, recipientLiabilityId!);
        expect(toAmountNumber(recipientLiability.initialAmount), 'recipient L initial 100').toBe(100);
        expect(String(recipientLiability.creationType ?? '').toUpperCase()).toBe('AUTOMATIC');
        expect(
          extractPartyCustomerId(recipientLiability as Record<string, unknown>),
          'liability customer = recipient',
        ).toBe(pre.recipientCustomerId);
        expect(Number(recipientLiability.invoiceResponse?.id), 'liability invoice').toBe(billed.invoiceId);

        const customerReceivable = await getCustomerReceivable(Request, customerReceivableId!);
        expect(
          extractPartyCustomerId(customerReceivable),
          'receivable customer = invoice/GC customer',
        ).toBe(pre.customerId);
        expect(toAmountNumber(customerReceivable.initialAmount), 'receivable initial abs 100').toBe(100);

        const invoiceLiability = await resolveInvoiceMainCustomerLiability(
          Request,
          billed.invoiceId,
          pre.customerId,
          [recipientLiabilityId!],
        );
        const initial = toAmountNumber(invoiceLiability.initialAmount);
        const current = toAmountNumber(invoiceLiability.currentAmount);
        expect(
          current,
          `invoice customer INVOICE liability offset by GC 100 (id=${invoiceLiability.id} initial=${initial})`,
        ).toBeCloseTo(initial - 100, 2);

        TestRunSummary.recordCheck({
          check: 'TC-BE-5 positive apply',
          expectedResult: 'INVOICED index 0; L recipient + R customer amount 100; invoice REAL; offset.',
          actualResult: `As expected — status=${comp.compensationStatus} index=${comp.index} L=${recipientLiabilityId} R=${customerReceivableId} current=${current}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: GC_REG_SCALES_KEY,
          relevantEntityKeys: ['customer', 'productContract', 'billingRun', 'invoice'],
          extraLinks: {
            ...buildProductContractTabLinks(pre.contractId, DEV_PORTAL_BASE),
            billingRun: [buildDevBillingRunPreviewLink(billed.billingRunId)],
            compensation: [buildDevCompensationPreviewLink(created.id)],
            invoice: [buildDevInvoicePreviewLink(billed.invoiceId)],
          },
          snapshot: { tc: 'TC-BE-5', gcId: created.id, invoiceId: billed.invoiceId },
        });
      });
    });

    test('[GC-REG-SCALES]: TC-BE-6 – E2E amount < 0 — liability for customer, receivable for recipient', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      Nomenclatures,
      TestRunSummary,
    }) => {
      test.setTimeout(BILLING_TIMEOUT_MS);
      const fx = { Request, GeneratePayload, Responses, Endpoints, Nomenclatures };

      const pre = await test.step('Precondition: FOR_VOLUMES contract + data by scales', async () =>
        runGcScalesContractAndBbsPrechain(fx),
      );

      const created = await test.step('Precondition: GC amount -50.00', async () =>
        createGcExpectingCreated(fx, {
          customerId: pre.customerId,
          podId: pre.podId,
          recipientId: pre.recipientCustomerId,
          documentPeriod: pre.documentPeriod,
          documentAmount: -50,
          volumes: 10,
          price: 5,
          reason: 'GC-REG-SCALES-TC-BE-6',
        }),
      );
      TestRunSummary.registerPayload('compensation', created.payload);

      const billed = await test.step('Standard Billing through accounting', async () =>
        completeForVolumesToAccounted(fx),
      );

      await test.step('Assert swapped parties, abs 50', async () => {
        const invoice = await getInvoice(Request, billed.invoiceId);
        expect(invoice.invoiceStatus).toBe('REAL');
        const comp = await getCompensation(Request, created.id);
        assertNegativeApplyAttachments(comp, billed.invoiceId, 'TC-BE-6');

        const customerLiabilityId = shortId(comp, 'liabilityForCustomer');
        const recipientReceivableId = shortId(comp, 'receivableForRecipient');
        const customerLiability = await getCustomerLiability(Request, customerLiabilityId!);
        const recipientReceivable = await getCustomerReceivable(Request, recipientReceivableId!);

        expect(
          extractPartyCustomerId(customerLiability as Record<string, unknown>),
          'liability customer = GC customer',
        ).toBe(pre.customerId);
        expect(
          extractPartyCustomerId(recipientReceivable),
          'receivable customer = recipient',
        ).toBe(pre.recipientCustomerId);
        expect(toAmountNumber(customerLiability.initialAmount)).toBe(50);
        expect(toAmountNumber(recipientReceivable.initialAmount)).toBe(50);

        TestRunSummary.recordCheck({
          check: 'TC-BE-6 negative apply party swap',
          expectedResult: 'INVOICED index 0; L customer + R recipient; amounts 50 abs.',
          actualResult: `As expected — L=${customerLiabilityId} R=${recipientReceivableId} status=${comp.compensationStatus}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: GC_REG_SCALES_KEY,
          relevantEntityKeys: ['customer', 'productContract', 'billingRun', 'invoice'],
          extraLinks: {
            billingRun: [buildDevBillingRunPreviewLink(billed.billingRunId)],
            compensation: [buildDevCompensationPreviewLink(created.id)],
            invoice: [buildDevInvoicePreviewLink(billed.invoiceId)],
          },
          snapshot: { tc: 'TC-BE-6', gcId: created.id, invoiceId: billed.invoiceId },
        });
      });
    });

    test('[GC-REG-SCALES]: TC-BE-7 – Government liability billing group is empty', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      Nomenclatures,
      TestRunSummary,
    }) => {
      test.setTimeout(BILLING_TIMEOUT_MS);
      const fx = { Request, GeneratePayload, Responses, Endpoints, Nomenclatures };

      const pre = await test.step('Precondition: FOR_VOLUMES contract + data by scales', async () =>
        runGcScalesContractAndBbsPrechain(fx),
      );

      const created = await test.step('Precondition: GC amount 100.00', async () =>
        createGcExpectingCreated(fx, {
          customerId: pre.customerId,
          podId: pre.podId,
          recipientId: pre.recipientCustomerId,
          documentPeriod: pre.documentPeriod,
          documentAmount: 100,
          volumes: 10,
          price: 10,
          reason: 'GC-REG-SCALES-TC-BE-7',
        }),
      );
      TestRunSummary.registerPayload('compensation', created.payload);

      const billed = await test.step('Standard Billing through accounting', async () =>
        completeForVolumesToAccounted(fx),
      );

      await test.step('Assert recipient liability billing group empty', async () => {
        const comp = await getCompensation(Request, created.id);
        assertPositiveApplyAttachments(comp, billed.invoiceId, 'TC-BE-7');
        const recipientLiability = await getCustomerLiability(
          Request,
          shortId(comp, 'liabilityForRecipient')!,
        );
        expect(billingGroupIsEmpty(recipientLiability), 'government liability billing group empty').toBe(
          true,
        );

        const invoiceLiabilityId = await resolveInvoiceCustomerLiabilityId(Request, billed.invoiceId);
        const invoiceLiability = await getCustomerLiability(Request, invoiceLiabilityId);

        TestRunSummary.recordCheck({
          check: 'TC-BE-7 government liability billing group empty',
          expectedResult: 'Recipient liability billingGroup / contractBillingGroupId null/empty.',
          actualResult: `As expected — govBG empty; invoiceLiabilityId=${invoiceLiability.id}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: GC_REG_SCALES_KEY,
          relevantEntityKeys: ['customer', 'productContract', 'billingRun', 'invoice'],
          extraLinks: {
            billingRun: [buildDevBillingRunPreviewLink(billed.billingRunId)],
            compensation: [buildDevCompensationPreviewLink(created.id)],
            invoice: [buildDevInvoicePreviewLink(billed.invoiceId)],
          },
          snapshot: { tc: 'TC-BE-7', gcId: created.id, invoiceId: billed.invoiceId },
        });
      });
    });

    test('[GC-REG-SCALES]: TC-BE-8 – Direct debit runtime — liability DD null/false; receivable DD from invoice', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      Nomenclatures,
      TestRunSummary,
    }) => {
      test.setTimeout(BILLING_TIMEOUT_MS);
      const fx = { Request, GeneratePayload, Responses, Endpoints, Nomenclatures };

      const pre = await test.step('Precondition: FOR_VOLUMES chain with Direct Debit + data by scales', async () =>
        runGcScalesBillingPrechainWithDirectDebit(fx),
      );

      const created = await test.step('Precondition: GC amount 100.00', async () =>
        createGcExpectingCreated(fx, {
          customerId: pre.customerId,
          podId: pre.podId,
          recipientId: pre.recipientCustomerId,
          documentPeriod: pre.documentPeriod,
          documentAmount: 100,
          volumes: 10,
          price: 10,
          reason: 'GC-REG-SCALES-TC-BE-8',
        }),
      );
      TestRunSummary.registerPayload('compensation', created.payload);

      const billed = await test.step('Standard Billing through accounting', async () =>
        completeForVolumesToAccounted(fx),
      );

      await test.step('Assert runtime DD: liability false/null; receivable from invoice', async () => {
        const invoice = await getInvoice(Request, billed.invoiceId);
        expect(invoice.invoiceStatus).toBe('REAL');
        expect(isDirectDebitOn((invoice as { directDebit?: unknown }).directDebit), 'invoice DD true').toBe(
          true,
        );

        const comp = await getCompensation(Request, created.id);
        assertPositiveApplyAttachments(comp, billed.invoiceId, 'TC-BE-8');
        const liability = await getCustomerLiability(Request, shortId(comp, 'liabilityForRecipient')!);
        const receivable = await getCustomerReceivable(Request, shortId(comp, 'receivableForCustomer')!);

        expect(
          isDirectDebitOn((liability as { directDebit?: unknown }).directDebit),
          'recipient liability DD must be null/false',
        ).toBe(false);
        expect(
          isDirectDebitOn(receivable.directDebit),
          'customer receivable DD matches invoice (true)',
        ).toBe(true);

        TestRunSummary.recordCheck({
          check: 'TC-BE-8 Direct Debit runtime',
          expectedResult: 'Liability DD null/false; receivable DD true from invoice (not institution).',
          actualResult: `As expected — invoiceDD=${(invoice as { directDebit?: unknown }).directDebit} Ldd=${(liability as { directDebit?: unknown }).directDebit} Rdd=${receivable.directDebit}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: GC_REG_SCALES_KEY,
          relevantEntityKeys: ['customer', 'productContract', 'billingRun', 'invoice'],
          extraLinks: {
            billingRun: [buildDevBillingRunPreviewLink(billed.billingRunId)],
            compensation: [buildDevCompensationPreviewLink(created.id)],
            invoice: [buildDevInvoicePreviewLink(billed.invoiceId)],
          },
          snapshot: { tc: 'TC-BE-8', gcId: created.id, invoiceId: billed.invoiceId },
        });
      });
    });

    test('[GC-REG-SCALES]: TC-BE-9 – Compensation for unrelated POD is not applied to the invoice', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      Nomenclatures,
      TestRunSummary,
    }) => {
      test.setTimeout(BILLING_TIMEOUT_MS);
      const fx = { Request, GeneratePayload, Responses, Endpoints, Nomenclatures };

      const pre = await test.step('Precondition: billed POD contract + data by scales', async () =>
        runGcScalesContractAndBbsPrechain(fx),
      );

      let podBId = 0;
      await test.step('Precondition: unrelated POD B (not activated on contract)', async () => {
        const pod = await Request.post(Endpoints.pod, {
          data: GeneratePayload.pointsOfDelivery.pod_settlement(),
        });
        await expect(pod).CheckResponse();
        const body = await pod.json();
        Responses.pod.push(body);
        podBId = Number((body as { id: number }).id);
        expect(podBId).not.toBe(pre.podId);
      });

      const created = await test.step('Create GC on POD B', async () =>
        createGcExpectingCreated(fx, {
          customerId: pre.customerId,
          podId: podBId,
          recipientId: pre.recipientCustomerId,
          documentPeriod: pre.documentPeriod,
          documentAmount: 100,
          volumes: 10,
          price: 10,
          reason: 'GC-REG-SCALES-TC-BE-9',
        }),
      );
      TestRunSummary.registerPayload('compensation', created.payload);

      const billed = await test.step('Bill contract POD A through accounting', async () =>
        completeForVolumesToAccounted(fx),
      );

      await test.step('Assert GC stays UNINVOICED; invoice still REAL', async () => {
        const invoice = await getInvoice(Request, billed.invoiceId);
        expect(invoice.invoiceStatus).toBe('REAL');
        const comp = await getCompensation(Request, created.id);
        assertGcUninvoicedEmptyFinancials(comp, 'TC-BE-9 unrelated POD');

        TestRunSummary.recordCheck({
          check: 'TC-BE-9 unrelated POD not applied',
          expectedResult: 'Invoice REAL; GC UNINVOICED with empty L/R/invoice.',
          actualResult: `As expected — invoice=${invoice.invoiceStatus} gc=${comp.compensationStatus}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: GC_REG_SCALES_KEY,
          relevantEntityKeys: ['customer', 'pod', 'billingRun', 'invoice'],
          extraLinks: {
            billingRun: [buildDevBillingRunPreviewLink(billed.billingRunId)],
            compensation: [buildDevCompensationPreviewLink(created.id)],
            invoice: [buildDevInvoicePreviewLink(billed.invoiceId)],
          },
          snapshot: {
            tc: 'TC-BE-9',
            billedPodId: pre.podId,
            unrelatedPodId: podBId,
            gcId: created.id,
          },
        });
      });
    });

    test('[GC-REG-SCALES]: TC-BE-10 – Mass import GOVERNMENT_COMPENSATION creates UNINVOICED row', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      FileUploadRequest,
      TestRunSummary,
    }) => {
      test.setTimeout(CREATE_TIMEOUT_MS);
      const fx = { Request, GeneratePayload, Responses, Endpoints };

      const pre = await test.step('Precondition: customer + recipient + POD', async () =>
        runGcCreatePrechain(fx),
      );

      const importNumber = `GC-REG-MI-${randomGens.generateRandomString(true, false, 8)}`;
      let compensationId = 0;

      await test.step('Upload GOVERNMENT_COMPENSATION mass import', async () => {
        const podIdentifier = await resolveEntityIdentifier(Request, 'pod', pre.podId);
        const currencyName = await resolveCurrencyName(Request);
        const beforeProcesses = await Request.get('process', {
          params: { page: 0, size: 50, sortBy: 'ID', sortDirection: 'DESC' },
        });
        const beforeIds = new Set<number>();
        if (beforeProcesses.ok()) {
          const body = (await beforeProcesses.json()) as { content?: Array<{ id?: number }> };
          for (const row of body.content ?? []) {
            const id = Number(row.id);
            if (Number.isFinite(id) && id > 0) beforeIds.add(id);
          }
        }

        const fileBuffer = await buildGovernmentCompensationMassImportBuffer(Request, {
          number: importNumber,
          date: todayIso(),
          documentPeriod: pre.documentPeriod,
          volumes: 10,
          price: 10,
          reason: 'GC-REG-SCALES-TC-BE-10',
          documentAmount: 100,
          currencyName,
          customerIdentifier: pre.customerIdentifier,
          podIdentifier,
          recipientIdentifier: pre.recipientIdentifier,
        });
        TestRunSummary.registerPayload('massImportRow', {
          number: importNumber,
          customerIdentifier: pre.customerIdentifier,
          podIdentifier,
          recipientIdentifier: pre.recipientIdentifier,
        });

        const upload = await uploadGovernmentCompensationMassImport(
          FileUploadRequest,
          fileBuffer,
          'gc-reg-government-compensation-mass-import.xlsx',
        );
        expect(upload.status, 'mass import upload HTTP 202').toBe(202);
        await pollGovernmentCompensationMassImportComplete(Request, beforeIds);

        const listing = await listCompensationsByNumber(Request, importNumber);
        expect(listing.status).toBe(206);
        const found = await findCompensationByNumber(Request, importNumber);
        expect(found, `imported compensation ${importNumber}`).toBeTruthy();
        compensationId = found!.id;
        expect(toAmountNumber(found!.documentAmount)).toBe(100);
        assertGcUninvoicedEmptyFinancials(found!, 'TC-BE-10 mass import');

        TestRunSummary.recordCheck({
          check: 'TC-BE-10 mass import UNINVOICED',
          expectedResult: 'Upload 202; process COMPLETED; GC UNINVOICED amount 100.',
          actualResult: `As expected — upload=${upload.status} gcId=${compensationId}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: GC_REG_SCALES_KEY,
          relevantEntityKeys: ['customer', 'pod'],
          extraLinks: compensationId
            ? { compensation: [buildDevCompensationPreviewLink(compensationId)] }
            : undefined,
          snapshot: { tc: 'TC-BE-10', importNumber, compensationId },
        });
      });
    });

    test('[GC-REG-SCALES]: TC-BE-11 – Full regeneracia — reverse L/R, index increment, re-apply new Uninvoiced GC', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      Nomenclatures,
      TestRunSummary,
    }) => {
      test.setTimeout(BILLING_TIMEOUT_MS);
      const fx = { Request, GeneratePayload, Responses, Endpoints, Nomenclatures };

      const pre = await test.step('Precondition: FOR_VOLUMES contract + data by scales', async () =>
        runGcScalesContractAndBbsPrechain(fx),
      );

      const gc1 = await test.step('Precondition: first GC amount 100.00', async () =>
        createGcExpectingCreated(fx, {
          customerId: pre.customerId,
          podId: pre.podId,
          recipientId: pre.recipientCustomerId,
          documentPeriod: pre.documentPeriod,
          documentAmount: 100,
          volumes: 10,
          price: 10,
          reason: 'GC-REG-SCALES-TC-BE-11-A',
        }),
      );
      TestRunSummary.registerPayload('compensation', gc1.payload);

      const billed = await test.step('First apply through accounting', async () =>
        completeForVolumesToAccounted(fx),
      );

      const invoiceBefore = await getInvoice(Request, billed.invoiceId);
      expect(invoiceBefore.invoiceStatus).toBe('REAL');
      const first = await getCompensation(Request, gc1.id);
      assertPositiveApplyAttachments(first, billed.invoiceId, 'TC-BE-11 first apply');
      // InvoiceResponse (Swagger/code) has no compensationIndex — use GC.index as baseline.
      const baseline = Number(first.index);
      expect(Number.isFinite(baseline), `GC index after first apply (got ${String(first.index)})`).toBe(
        true,
      );

      const gc2 = await test.step('Precondition: second UNINVOICED GC amount 20.00', async () =>
        createGcExpectingCreated(fx, {
          customerId: pre.customerId,
          podId: pre.podId,
          recipientId: pre.recipientCustomerId,
          documentPeriod: pre.documentPeriod,
          documentAmount: 20,
          volumes: 10,
          price: 2,
          reason: 'GC-REG-SCALES-TC-BE-11-B',
        }),
      );
      const gc2Before = await getCompensation(Request, gc2.id);
      expect(gc2Before.compensationStatus).toBe('UNINVOICED');

      await test.step('PATCH regenerate-compensations → reverse + re-apply', async () => {
        await patchRegenerateCompensationsOk(Request, billed.invoiceId);
        const invoiceAfter = await getInvoice(Request, billed.invoiceId);
        expect(invoiceAfter.invoiceStatus).toBe('REAL');

        const after1 = await getCompensation(Request, gc1.id);
        expect(shortId(after1, 'liabilityForCustomer'), 'reverse L customer on gc1').toBeTruthy();
        expect(shortId(after1, 'receivableForRecipient'), 'reverse R recipient on gc1').toBeTruthy();

        const after2 = await getCompensation(Request, gc2.id);
        expect(after2.compensationStatus, 'gc2 INVOICED after regeneracia').toBe('INVOICED');
        const indexAfter = Number(after2.index);
        expect(indexAfter, 'gc2 index incremented vs first apply').toBe(baseline + 1);
        expect(after2.invoice?.id).toBe(billed.invoiceId);
        expect(shortId(after2, 'liabilityForRecipient'), 're-apply L recipient').toBeTruthy();
        expect(shortId(after2, 'receivableForCustomer'), 're-apply R customer').toBeTruthy();

        const invoiceIdx = invoiceAfter.compensationIndex;
        if (invoiceIdx !== undefined && invoiceIdx !== null) {
          expect(Number(invoiceIdx), 'invoice.compensationIndex when present').toBe(indexAfter);
        }

        TestRunSummary.recordCheck({
          check: 'TC-BE-11 full regeneracia',
          expectedResult: 'PATCH 202; index+1; gc1 reverse L/R; gc2 INVOICED with new apply parties.',
          actualResult: `As expected — index ${baseline}→${indexAfter} gc2=${after2.compensationStatus}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: GC_REG_SCALES_KEY,
          relevantEntityKeys: ['customer', 'productContract', 'billingRun', 'invoice'],
          extraLinks: {
            billingRun: [buildDevBillingRunPreviewLink(billed.billingRunId)],
            compensation: [
              buildDevCompensationPreviewLink(gc1.id),
              buildDevCompensationPreviewLink(gc2.id),
            ],
            invoice: [buildDevInvoicePreviewLink(billed.invoiceId)],
          },
          snapshot: { tc: 'TC-BE-11', gcId1: gc1.id, gcId2: gc2.id, invoiceId: billed.invoiceId },
        });
      });
    });

    test('[GC-REG-SCALES]: TC-BE-12 – Regeneracia rejected when invoice is not REAL', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      Nomenclatures,
      TestRunSummary,
    }) => {
      test.setTimeout(BILLING_TIMEOUT_MS);
      const fx = { Request, GeneratePayload, Responses, Endpoints, Nomenclatures };

      const pre = await test.step('Precondition: FOR_VOLUMES contract + data by scales', async () =>
        runGcScalesContractAndBbsPrechain(fx),
      );

      const created = await test.step('Precondition: UNINVOICED GC', async () =>
        createGcExpectingCreated(fx, {
          customerId: pre.customerId,
          podId: pre.podId,
          recipientId: pre.recipientCustomerId,
          documentPeriod: pre.documentPeriod,
          documentAmount: 100,
          volumes: 10,
          price: 10,
          reason: 'GC-REG-SCALES-TC-BE-12',
        }),
      );
      TestRunSummary.registerPayload('compensation', created.payload);

      const billing = await test.step('Precondition: draft only (invoice not REAL)', async () =>
        createForVolumesDraftBillingRun(fx),
      );
      const invoiceId = billing.draftInvoiceIds[0];

      await test.step('PATCH regeneracia on non-REAL → 400', async () => {
        const invoice = await getInvoice(Request, invoiceId);
        expect(invoice.invoiceStatus, 'must not be REAL').not.toBe('REAL');

        const patched = await patchRegenerateCompensationsRaw(Request, invoiceId);
        expect(patched.status, 'non-REAL regeneracia HTTP 400').toBe(400);
        expect(patched.bodyText).toContain(REGEN_NOT_REAL_ERROR);

        const gc = await getCompensation(Request, created.id);
        assertGcUninvoicedEmptyFinancials(gc, 'TC-BE-12 after rejected regeneracia');
        const invoiceAfter = await getInvoice(Request, invoiceId);
        expect(invoiceAfter.invoiceStatus).not.toBe('REAL');

        TestRunSummary.recordCheck({
          check: 'TC-BE-12 regeneracia rejected non-REAL',
          expectedResult: `HTTP 400 containing ${REGEN_NOT_REAL_ERROR}; GC stays UNINVOICED.`,
          actualResult: `As expected — status=${patched.status} gc=${gc.compensationStatus}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: GC_REG_SCALES_KEY,
          relevantEntityKeys: ['customer', 'billingRun', 'invoice'],
          extraLinks: {
            billingRun: [buildDevBillingRunPreviewLink(billing.billingRunId)],
            compensation: [buildDevCompensationPreviewLink(created.id)],
            invoice: [buildDevInvoicePreviewLink(invoiceId)],
          },
          snapshot: { tc: 'TC-BE-12', gcId: created.id, invoiceId },
        });
      });
    });

    test('[GC-REG-SCALES]: TC-BE-13 – Regeneracia rejected when compensationIndex is null', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      Nomenclatures,
      TestRunSummary,
    }) => {
      test.setTimeout(BILLING_TIMEOUT_MS);
      const fx = { Request, GeneratePayload, Responses, Endpoints, Nomenclatures };

      const pre = await test.step('Precondition: FOR_VOLUMES contract + data by scales (no GC)', async () =>
        runGcScalesContractAndBbsPrechain(fx),
      );

      const billed = await test.step('Accounting without any government compensation', async () =>
        completeForVolumesToAccounted(fx),
      );

      await test.step('PATCH regeneracia when compensationIndex is null → 400', async () => {
        const invoice = await getInvoice(Request, billed.invoiceId);
        TestRunSummary.registerPayload('invoice', invoice);
        expect(invoice.invoiceStatus).toBe('REAL');
        expect(
          invoice.compensationIndex === null || invoice.compensationIndex === undefined,
          `TC-BE-13 requires null compensationIndex, got ${String(invoice.compensationIndex)} — re-setup if env set an index without GCs`,
        ).toBe(true);

        const patched = await patchRegenerateCompensationsRaw(Request, billed.invoiceId);
        expect(patched.status, 'null index regeneracia HTTP 400').toBe(400);
        expect(patched.bodyText).toContain(REGEN_NULL_INDEX_ERROR);

        const after = await getInvoice(Request, billed.invoiceId);
        expect(after.invoiceStatus).toBe('REAL');
        expect(after.compensationIndex === null || after.compensationIndex === undefined).toBe(true);

        TestRunSummary.recordCheck({
          check: 'TC-BE-13 regeneracia rejected null index',
          expectedResult: `HTTP 400 containing ${REGEN_NULL_INDEX_ERROR}; index stays null.`,
          actualResult: `As expected — status=${patched.status} index=${String(after.compensationIndex)}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: GC_REG_SCALES_KEY,
          relevantEntityKeys: ['customer', 'productContract', 'billingRun', 'invoice'],
          extraLinks: {
            ...buildProductContractTabLinks(pre.contractId, DEV_PORTAL_BASE),
            billingRun: [buildDevBillingRunPreviewLink(billed.billingRunId)],
            invoice: [buildDevInvoicePreviewLink(billed.invoiceId)],
          },
          snapshot: { tc: 'TC-BE-13', invoiceId: billed.invoiceId, contractId: pre.contractId },
        });
      });
    });

    test(
      '[GC-REG-SCALES]: TC-BE-14 – Auto L/R field table on scales invoice — outgoing Empty, income account and cost center from original invoice',
      { tag: ['@gc-spec'] },
      async ({ Request, GeneratePayload, Responses, Endpoints, Nomenclatures, TestRunSummary }) => {
        test.setTimeout(BILLING_TIMEOUT_MS);
        const fx = { Request, GeneratePayload, Responses, Endpoints, Nomenclatures };

        const pre = await test.step('Precondition: FOR_VOLUMES contract + data by scales', async () =>
          runGcScalesContractAndBbsPrechain(fx),
        );

        const created = await test.step('Precondition: GC amount 100.00 prefix GC-OUT-EMPTY-', async () =>
          createGcExpectingCreated(fx, {
            customerId: pre.customerId,
            podId: pre.podId,
            recipientId: pre.recipientCustomerId,
            documentPeriod: pre.documentPeriod,
            documentAmount: 100,
            volumes: 10,
            price: 10,
            reason: 'spec-field-table-outgoing-income-cc',
            number: `GC-OUT-EMPTY-${randomGens.generateRandomString(true, false, 8)}`,
          }),
        );
        TestRunSummary.registerPayload('compensation', created.payload);

        const billed = await test.step('Standard Billing through accounting', async () =>
          completeForVolumesToAccounted(fx),
        );

        await test.step('Assert wiki field table: outgoing empty; income/cost center from invoice', async () => {
          const invoice = await getInvoice(Request, billed.invoiceId);
          expect(invoice.invoiceStatus).toBe('REAL');
          const invoiceNumber = String(invoice.invoiceNumber ?? '').trim();
          const invoiceIncome = pickInvoiceIncomeAccount(invoice);
          const invoiceCostCenter = pickInvoiceCostCenter(invoice);

          const comp = await getCompensation(Request, created.id);
          assertPositiveApplyAttachments(comp, billed.invoiceId, 'TC-BE-14');
          const liability = await getCustomerLiability(Request, shortId(comp, 'liabilityForRecipient')!);
          const receivable = await getCustomerReceivable(Request, shortId(comp, 'receivableForCustomer')!);

          const lOutgoing = (liability as Record<string, unknown>).outgoingDocumentFromExternalSystem;
          const rOutgoing = receivable.outgoingDocumentFromAnExternalSystem;
          expect(isEmptyOutgoingExternal(lOutgoing), 'liability outgoingDocumentFromExternalSystem Empty').toBe(
            true,
          );
          expect(String(lOutgoing ?? '').trim(), 'liability outgoing must not be invoice number').not.toBe(
            invoiceNumber,
          );
          expect(
            isEmptyOutgoingExternal(rOutgoing),
            'receivable outgoingDocumentFromAnExternalSystem empty',
          ).toBe(true);
          expect(String(rOutgoing ?? '').trim(), 'receivable outgoing must not be invoice number').not.toBe(
            invoiceNumber,
          );
          expect(String((liability as Record<string, unknown>).numberOfIncomeAccount ?? '').trim()).toBe(
            invoiceIncome,
          );
          expect(String(receivable.numberOfIncomeAccount ?? '').trim()).toBe(invoiceIncome);
          expect(String((liability as Record<string, unknown>).costCenterControllingOrder ?? '').trim()).toBe(
            invoiceCostCenter,
          );
          expect(String(receivable.costCenterControllingOrder ?? '').trim()).toBe(invoiceCostCenter);

          TestRunSummary.recordCheck({
            check: 'TC-BE-14 spec field table outgoing/income/cost center',
            expectedResult:
              'Outgoing external Empty (not invoice number); numberOfIncomeAccount and costCenterControllingOrder equal GET invoice.',
            actualResult: `Lout=${String(lOutgoing)} Rout=${String(rOutgoing)} invoiceNo=${invoiceNumber}`,
            passed: true,
          });
        });

        await test.step('Attach test run summary', async () => {
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: GC_REG_SCALES_KEY,
            relevantEntityKeys: ['customer', 'productContract', 'billingRun', 'invoice'],
            extraLinks: {
              billingRun: [buildDevBillingRunPreviewLink(billed.billingRunId)],
              compensation: [buildDevCompensationPreviewLink(created.id)],
              invoice: [buildDevInvoicePreviewLink(billed.invoiceId)],
            },
            snapshot: { tc: 'TC-BE-14', gcId: created.id, invoiceId: billed.invoiceId },
          });
        });
      },
    );

    test(
      '[GC-REG-SCALES]: TC-BE-15 – Compensation receivable occurrence date equals creation date (scales invoice)',
      { tag: ['@gc-spec'] },
      async ({ Request, GeneratePayload, Responses, Endpoints, Nomenclatures, TestRunSummary }) => {
        test.setTimeout(BILLING_TIMEOUT_MS);
        const fx = { Request, GeneratePayload, Responses, Endpoints, Nomenclatures };

        const pre = await test.step('Precondition: FOR_VOLUMES contract + data by scales', async () =>
          runGcScalesContractAndBbsPrechain(fx),
        );

        const created = await test.step('Precondition: GC amount 100.00 prefix GC-OCC-DATE-', async () =>
          createGcExpectingCreated(fx, {
            customerId: pre.customerId,
            podId: pre.podId,
            recipientId: pre.recipientCustomerId,
            documentPeriod: pre.documentPeriod,
            documentAmount: 100,
            volumes: 10,
            price: 10,
            reason: 'GC-REG-SCALES-TC-BE-15',
            number: `GC-OCC-DATE-${randomGens.generateRandomString(true, false, 8)}`,
          }),
        );
        TestRunSummary.registerPayload('compensation', created.payload);

        const billed = await test.step('Standard Billing through accounting', async () =>
          completeForVolumesToAccounted(fx),
        );

        await test.step('Assert occurrenceDate date part equals creationDate date part', async () => {
          const invoice = await getInvoice(Request, billed.invoiceId);
          expect(invoice.invoiceStatus).toBe('REAL');
          const invoiceDate = calendarDatePart(invoice.invoiceDate ?? invoice.date);
          const comp = await getCompensation(Request, created.id);
          assertPositiveApplyAttachments(comp, billed.invoiceId, 'TC-BE-15');
          const receivable = await getCustomerReceivable(Request, shortId(comp, 'receivableForCustomer')!);
          const occurrence = calendarDatePart(receivable.occurrenceDate);
          const creation = calendarDatePart(receivable.creationDate);
          expect(occurrence, 'wiki 1077608452 occurrence = creation date').toBe(creation);

          TestRunSummary.recordCheck({
            check: 'TC-BE-15 receivable occurrenceDate = creationDate',
            expectedResult: 'occurrenceDate calendar date equals creationDate calendar date (not invoice date).',
            actualResult: `occurrence=${occurrence} creation=${creation} invoiceDate=${invoiceDate}`,
            passed: true,
          });
        });

        await test.step('Attach test run summary', async () => {
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: GC_REG_SCALES_KEY,
            relevantEntityKeys: ['customer', 'productContract', 'billingRun', 'invoice'],
            extraLinks: {
              billingRun: [buildDevBillingRunPreviewLink(billed.billingRunId)],
              compensation: [buildDevCompensationPreviewLink(created.id)],
              invoice: [buildDevInvoicePreviewLink(billed.invoiceId)],
            },
            snapshot: { tc: 'TC-BE-15', gcId: created.id, invoiceId: billed.invoiceId },
          });
        });
      },
    );

    test(
      '[GC-REG-SCALES]: TC-BE-16 – Compensation receivable current amount equals zero after apply offset (scales invoice)',
      { tag: ['@gc-spec'] },
      async ({ Request, GeneratePayload, Responses, Endpoints, Nomenclatures, TestRunSummary }) => {
        test.setTimeout(BILLING_TIMEOUT_MS);
        const fx = { Request, GeneratePayload, Responses, Endpoints, Nomenclatures };

        const pre = await test.step('Precondition: FOR_VOLUMES contract + data by scales', async () =>
          runGcScalesContractAndBbsPrechain(fx),
        );

        const created = await test.step('Precondition: GC amount 100.00 prefix GC-R-CUR-0-', async () =>
          createGcExpectingCreated(fx, {
            customerId: pre.customerId,
            podId: pre.podId,
            recipientId: pre.recipientCustomerId,
            documentPeriod: pre.documentPeriod,
            documentAmount: 100,
            volumes: 10,
            price: 10,
            reason: 'GC-REG-SCALES-TC-BE-16',
            number: `GC-R-CUR-0-${randomGens.generateRandomString(true, false, 8)}`,
          }),
        );
        TestRunSummary.registerPayload('compensation', created.payload);

        const billed = await test.step('Standard Billing through accounting', async () =>
          completeForVolumesToAccounted(fx),
        );

        await test.step('Assert receivable currentAmount is 0', async () => {
          const invoice = await getInvoice(Request, billed.invoiceId);
          expect(invoice.invoiceStatus).toBe('REAL');
          const comp = await getCompensation(Request, created.id);
          assertPositiveApplyAttachments(comp, billed.invoiceId, 'TC-BE-16');
          const receivable = await getCustomerReceivable(Request, shortId(comp, 'receivableForCustomer')!);
          expect(toAmountNumber(receivable.currentAmount), 'wiki 1077608452 current amount = 0').toBe(0);

          TestRunSummary.recordCheck({
            check: 'TC-BE-16 receivable currentAmount = 0',
            expectedResult: 'GET /customer-receivable currentAmount is 0 after offset.',
            actualResult: `current=${String(receivable.currentAmount)} initial=${String(receivable.initialAmount)}`,
            passed: true,
          });
        });

        await test.step('Attach test run summary', async () => {
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: GC_REG_SCALES_KEY,
            relevantEntityKeys: ['customer', 'productContract', 'billingRun', 'invoice'],
            extraLinks: {
              billingRun: [buildDevBillingRunPreviewLink(billed.billingRunId)],
              compensation: [buildDevCompensationPreviewLink(created.id)],
              invoice: [buildDevInvoicePreviewLink(billed.invoiceId)],
            },
            snapshot: { tc: 'TC-BE-16', gcId: created.id, invoiceId: billed.invoiceId },
          });
        });
      },
    );

    test(
      '[GC-REG-SCALES]: TC-BE-17 – Direct debit on auto L and R from government institution (distinct IBANs, scales invoice)',
      { tag: ['@gc-spec'] },
      async ({ Request, GeneratePayload, Responses, Endpoints, Nomenclatures, TestRunSummary }) => {
        test.setTimeout(BILLING_TIMEOUT_MS);
        const fx = { Request, GeneratePayload, Responses, Endpoints, Nomenclatures };

        const pre = await test.step('Precondition: scales chain with distinct institution IBAN', async () =>
          runGcScalesBillingPrechainDistinctInstitutionDd(fx),
        );

        const created = await test.step('Precondition: GC amount 100.00 prefix GC-DD-INST-', async () =>
          createGcExpectingCreated(fx, {
            customerId: pre.customerId,
            podId: pre.podId,
            recipientId: pre.recipientCustomerId,
            documentPeriod: pre.documentPeriod,
            documentAmount: 100,
            volumes: 10,
            price: 10,
            reason: 'GC-REG-SCALES-TC-BE-17',
            number: `GC-DD-INST-${randomGens.generateRandomString(true, false, 8)}`,
          }),
        );
        TestRunSummary.registerPayload('compensation', created.payload);

        const billed = await test.step('Standard Billing through accounting', async () =>
          completeForVolumesToAccounted(fx),
        );

        await test.step('Assert L and R Direct Debit from government institution', async () => {
          const invoice = await getInvoice(Request, billed.invoiceId);
          expect(invoice.invoiceStatus).toBe('REAL');
          const institutionIban = String(pre.institutionIban ?? '');
          const invoiceCustomerIban = String(pre.invoiceCustomerIban ?? '');

          const comp = await getCompensation(Request, created.id);
          assertPositiveApplyAttachments(comp, billed.invoiceId, 'TC-BE-17');
          const liability = await getCustomerLiability(Request, shortId(comp, 'liabilityForRecipient')!);
          const receivable = await getCustomerReceivable(Request, shortId(comp, 'receivableForCustomer')!);

          expect(isDirectDebitOn((liability as { directDebit?: unknown }).directDebit), 'liability DD true').toBe(
            true,
          );
          expect(pickLiabilityIban(liability as Record<string, unknown>)).toBe(institutionIban);
          expect(pickLiabilityIban(liability as Record<string, unknown>)).not.toBe(invoiceCustomerIban);
          expect(isDirectDebitOn(receivable.directDebit), 'receivable DD true').toBe(true);
          expect(pickReceivableBankAccount(receivable)).toBe(institutionIban);
          expect(pickReceivableBankAccount(receivable)).not.toBe(invoiceCustomerIban);

          TestRunSummary.recordCheck({
            check: 'TC-BE-17 DD from government institution',
            expectedResult: 'Liability and receivable DD true; IBAN = institution, not invoice customer.',
            actualResult: `Liban=${pickLiabilityIban(liability as Record<string, unknown>)} inst=${institutionIban}`,
            passed: true,
          });
        });

        await test.step('Attach test run summary', async () => {
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: GC_REG_SCALES_KEY,
            relevantEntityKeys: ['customer', 'productContract', 'billingRun', 'invoice'],
            extraLinks: {
              billingRun: [buildDevBillingRunPreviewLink(billed.billingRunId)],
              compensation: [buildDevCompensationPreviewLink(created.id)],
              invoice: [buildDevInvoicePreviewLink(billed.invoiceId)],
            },
            snapshot: { tc: 'TC-BE-17', gcId: created.id, invoiceId: billed.invoiceId },
          });
        });
      },
    );

    test(
      '[GC-REG-SCALES]: TC-BE-18 – Direct debit unselected when government institution has no DD (scales invoice)',
      { tag: ['@gc-spec'] },
      async ({ Request, GeneratePayload, Responses, Endpoints, Nomenclatures, TestRunSummary }) => {
        test.setTimeout(BILLING_TIMEOUT_MS);
        const fx = { Request, GeneratePayload, Responses, Endpoints, Nomenclatures };

        const pre = await test.step('Precondition: invoice customer DD on; institution DD off', async () =>
          runGcScalesBillingPrechainInvoiceDdInstitutionNone(fx),
        );

        const created = await test.step('Precondition: GC amount 100.00 prefix GC-DD-NONE-', async () =>
          createGcExpectingCreated(fx, {
            customerId: pre.customerId,
            podId: pre.podId,
            recipientId: pre.recipientCustomerId,
            documentPeriod: pre.documentPeriod,
            documentAmount: 100,
            volumes: 10,
            price: 10,
            reason: 'GC-REG-SCALES-TC-BE-18',
            number: `GC-DD-NONE-${randomGens.generateRandomString(true, false, 8)}`,
          }),
        );
        TestRunSummary.registerPayload('compensation', created.payload);

        const billed = await test.step('Standard Billing through accounting', async () =>
          completeForVolumesToAccounted(fx),
        );

        await test.step('Assert L and R Direct Debit unselected', async () => {
          const invoice = await getInvoice(Request, billed.invoiceId);
          expect(invoice.invoiceStatus).toBe('REAL');
          expect(isDirectDebitOn((invoice as { directDebit?: unknown }).directDebit), 'invoice DD true').toBe(
            true,
          );

          const comp = await getCompensation(Request, created.id);
          assertPositiveApplyAttachments(comp, billed.invoiceId, 'TC-BE-18');
          const liability = await getCustomerLiability(Request, shortId(comp, 'liabilityForRecipient')!);
          const receivable = await getCustomerReceivable(Request, shortId(comp, 'receivableForCustomer')!);

          expect(
            isDirectDebitOn((liability as { directDebit?: unknown }).directDebit),
            'liability DD unselected',
          ).toBe(false);
          expect(isDirectDebitOn(receivable.directDebit), 'receivable DD unselected (not from invoice)').toBe(
            false,
          );
          expect(pickReceivableBankAccount(receivable)).not.toBe(String(pre.invoiceCustomerIban ?? ''));

          TestRunSummary.recordCheck({
            check: 'TC-BE-18 DD unselected when institution has no DD',
            expectedResult: 'L and R directDebit false/null; receivable must not copy invoice IBAN.',
            actualResult: `invoiceDD=${String((invoice as { directDebit?: unknown }).directDebit)} Rdd=${String(receivable.directDebit)}`,
            passed: true,
          });
        });

        await test.step('Attach test run summary', async () => {
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: GC_REG_SCALES_KEY,
            relevantEntityKeys: ['customer', 'productContract', 'billingRun', 'invoice'],
            extraLinks: {
              billingRun: [buildDevBillingRunPreviewLink(billed.billingRunId)],
              compensation: [buildDevCompensationPreviewLink(created.id)],
              invoice: [buildDevInvoicePreviewLink(billed.invoiceId)],
            },
            snapshot: { tc: 'TC-BE-18', gcId: created.id, invoiceId: billed.invoiceId },
          });
        });
      },
    );

    test(
      '[GC-REG-SCALES]: TC-BE-19 – Create is rejected when Document Number is not numeric',
      { tag: ['@gc-spec'] },
      async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
        test.setTimeout(CREATE_TIMEOUT_MS);
        const fx = { Request, GeneratePayload, Responses, Endpoints };

        const pre = await test.step('Precondition: customer + recipient + POD', async () =>
          runGcCreatePrechain(fx),
        );

        const uniqueNumber = `${NON_NUMERIC_DOCUMENT_NUMBER}-${randomGens.generateRandomString(true, false, 6)}`;
        const payload = buildCompensationPayload(GeneratePayload, {
          customerId: pre.customerId,
          podId: pre.podId,
          recipientId: pre.recipientId,
          documentPeriod: pre.documentPeriod,
          documentAmount: 100,
          volumes: 10,
          price: 10,
          reason: 'non-numeric-number-reject',
        });
        payload.number = uniqueNumber;
        payload.date = todayIso();
        TestRunSummary.registerPayload('compensation', payload);

        await test.step('POST alphanumeric number → 400; listing has no row', async () => {
          const posted = await postGcRaw(fx, payload);
          expect(posted.status, 'non-numeric Document Number HTTP 400').toBe(400);
          expect(
            /numeric|number/i.test(posted.bodyText),
            'error indicates Document Number is not numeric',
          ).toBe(true);

          const listing = await listCompensationsByNumber(Request, uniqueNumber);
          expect(listing.status).toBe(206);
          const hit = listing.content.find((r) => String(r.number ?? '').trim() === uniqueNumber);
          expect(hit, 'no ACTIVE GC stored for alphanumeric number').toBeFalsy();

          TestRunSummary.recordCheck({
            check: 'TC-BE-19 alphanumeric Document Number rejected',
            expectedResult: 'HTTP 400 indicating number is not numeric; listing has no row.',
            actualResult: `status=${posted.status} body=${posted.bodyText.slice(0, 180)}`,
            passed: true,
          });
        });

        await test.step('Attach test run summary', async () => {
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: GC_REG_SCALES_KEY,
            relevantEntityKeys: ['customer', 'pod'],
            snapshot: { tc: 'TC-BE-19', number: uniqueNumber },
          });
        });
      },
    );

    test(
      '[GC-REG-SCALES]: TC-BE-20 – Zero documentAmount is rejected with the Confluence EN and BG messages',
      { tag: ['@gc-spec'] },
      async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
        test.setTimeout(CREATE_TIMEOUT_MS);
        const fx = { Request, GeneratePayload, Responses, Endpoints };

        const pre = await test.step('Precondition: customer + recipient + POD', async () =>
          runGcCreatePrechain(fx),
        );

        const uniqueNumber = `GC-ZERO-WIKI-${randomGens.generateRandomString(true, false, 8)}`;
        const payload = buildCompensationPayload(GeneratePayload, {
          customerId: pre.customerId,
          podId: pre.podId,
          recipientId: pre.recipientId,
          documentPeriod: pre.documentPeriod,
          documentAmount: 1,
          volumes: 1,
          price: 1,
          reason: 'zero-amount-wiki-message',
        });
        payload.number = uniqueNumber;
        payload.date = todayIso();
        payload.documentAmount = 0;
        TestRunSummary.registerPayload('compensation', payload);

        await test.step('POST amount 0 → 400 with wiki EN/BG text', async () => {
          const posted = await postGcRaw(fx, payload);
          expect(posted.status, 'zero amount HTTP 400').toBe(400);
          const hasWiki =
            posted.bodyText.includes(ZERO_AMOUNT_WIKI_EN) || posted.bodyText.includes(ZERO_AMOUNT_WIKI_BG);
          expect(hasWiki, `wiki EN or BG zero-amount message required; body=${posted.bodyText}`).toBe(true);

          const listing = await listCompensationsByNumber(Request, uniqueNumber);
          expect(listing.status).toBe(206);
          const hit = listing.content.find((r) => String(r.number ?? '').trim() === uniqueNumber);
          expect(hit, 'no ACTIVE zero-amount GC stored').toBeFalsy();

          TestRunSummary.recordCheck({
            check: 'TC-BE-20 zero amount wiki EN/BG messages',
            expectedResult: `HTTP 400 containing "${ZERO_AMOUNT_WIKI_EN}" and/or BG wiki sentence.`,
            actualResult: `status=${posted.status} hasWiki=${hasWiki}`,
            passed: true,
          });
        });

        await test.step('Attach test run summary', async () => {
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: GC_REG_SCALES_KEY,
            relevantEntityKeys: ['customer', 'pod'],
            snapshot: { tc: 'TC-BE-20', number: uniqueNumber },
          });
        });
      },
    );
  },
);
