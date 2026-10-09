/**
 * PDT-3341 — Additional parameters in Government compensations
 *
 * Reference spec(s):
 * - tests/cursor/pdt-3087-government-compensation-defer-to-pdf.fixtures.ts
 *   (create/GET compensation, FOR_VOLUMES prechain, draft billing, start-generating /
 *    start-accounting, mass-import template/upload/poll, Endpoints.compensation =
 *    government-compensations, runMultiPodForVolumesPrechain)
 * - tests/cursor/pdt-3223-invoice-json-price-component-order.fixtures.ts
 *   (GET billing-run/generate-invoice-data?invoiceId=)
 * - tests/cursor/dev-volume-billing-two-compensations.fixtures.ts
 *   (two-POD chain via 3087 runMultiPodForVolumesPrechain)
 *
 * Swagger (dev): Cursor-Project/config/swagger/dev/swagger-spec.json
 * Backend TC: Cursor-Project/test_cases/Backend/PDT-3341_government_compensation_additional_parameters.md
 *
 * CheckResponse() only for 2xx. Negatives (400): expect(status).toBe(400) + body fragment.
 * TC-BE-22 asserts spec strings `ONLY-A, ` and `, ONLY-B` (Finding F3 — may fail on origin/dev).
 *
 * Invoice TCs (BE-10/11/19–23): reuse PDT-3087 FOR_VOLUMES billing-by-profile prechain
 * (priceSettlement + BBP) so generate-invoice-data is filled. Backend TC mentions
 * billing-by-scales; the additional-parameter assertions do not depend on scale vs profile.
 */

import { test, expect } from './cursor-test.fixtures';
import {
  finalizeTestRunSummary,
  buildProductContractTabLinks,
} from './shared/manual-verification-links.fixtures';
import {
  PDT_3341_KEY,
  SIMPLE_TIMEOUT_MS,
  IMPORT_TIMEOUT_MS,
  INVOICE_TIMEOUT_MS,
  PARAM64_A,
  PARAM64_B,
  PARAM65_C,
  PARAM65_D,
  PARAM65_E,
  ADDITIONAL_PARAM_KEYS,
  uniqueCompNumber,
  dateOnDayOfPeriod,
  sizeFragment,
  invoicedLockFragment,
  isEmptyOrNull,
  filledParams,
  assertAdditionalKeysPresent,
  assertAdditionalExact,
  assertInvoiceParamsExact,
  listingHasAdditionalParameterKeys,
  readHttpError,
  runPdt3341CustomerPodPrechain,
  buildPdt3341CompensationPayload,
  createPdt3341Compensation,
  putPdt3341Compensation,
  clonePayloadWithAdditional,
  getCustomerReceivable,
  getCompensationListingRow,
  fetchPdt3341InvoiceDocument,
  findTableCompensationRows,
  findSdCompensationRows,
  downloadGcTemplateBuffer,
  buildPdt3341MassImportBuffer,
  buildOldFormatGcMassImportBuffer,
  buildWrongHeaderGcMassImportBuffer,
  snapshotGcProcessIds,
  pollGcMassImportSettled,
  collectMassImportErrorBlob,
  assertInvalidFileFormatDualPath,
  resolveMassImportIdentifiers,
  runDevVolCompContractAndBbpPrechain,
  runMultiPodForVolumesPrechain,
  createForVolumesDraftBillingRun,
  startGeneratingAndWaitGenerated,
  startAccountingAndWaitCompleted,
  getCompensation,
  getInvoice,
  getCustomerLiability,
  findCompensationByNumber,
  pollPdfDocumentsPresent,
  pollGovernmentCompensationMassImportComplete,
  uploadGovernmentCompensationMassImport,
  toAmountNumber,
  buildDevBillingRunPreviewLink,
  buildDevCompensationPreviewLink,
  buildDevInvoicePreviewLink,
  DEV_PORTAL_BASE,
  type Pdt3341Fx,
  type AdditionalParameterMap,
} from './pdt-3341-government-compensation-additional-parameters.fixtures';

test.describe(
  '[PDT-3341]: Additional parameters in Government compensations',
  { tag: ['@billing', '@compensations', '@PDT-3341'] },
  () => {
    test('[PDT-3341]: TC-BE-1 – POST create with all twelve additional parameters filled; GET returns the same twelve values', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(SIMPLE_TIMEOUT_MS);
      const fx: Pdt3341Fx = { Request, GeneratePayload, Responses, Endpoints };
      const number = uniqueCompNumber('PDT3341-TCBE1-ALL');
      const additional = filledParams('ALL-P');

      const pre = await test.step('Precondition: billed customer, recipient, POD', async () =>
        runPdt3341CustomerPodPrechain(fx),
      );

      const created = await test.step('POST /government-compensations with all twelve params', async () => {
        const result = await createPdt3341Compensation(fx, {
          customerId: pre.customerId,
          podId: pre.podId,
          recipientId: pre.recipientCustomerId,
          documentPeriod: pre.documentPeriod,
          number,
          date: dateOnDayOfPeriod(pre.documentPeriod, 15),
          documentAmount: 20,
          volumes: 10,
          price: 2,
          reason: 'PDT-3341-TC-BE-1',
          additional,
        });
        TestRunSummary.registerPayload('compensation', result.payload);
        return result;
      });

      await test.step('GET by id — twelve values match create', async () => {
        const view = await getCompensation(Request, created.id);
        expect(view.compensationStatus).toBe('UNINVOICED');
        expect(view.number).toBe(number);
        expect(toAmountNumber(view.documentAmount)).toBe(20);
        expect(toAmountNumber(view.volumes)).toBe(10);
        assertAdditionalKeysPresent(view as Record<string, unknown>, 'TC-BE-1');
        assertAdditionalExact(view as Record<string, unknown>, additional, 'TC-BE-1');
        TestRunSummary.recordCheck({
          check: 'TC-BE-1 create+GET all twelve filled',
          expectedResult: 'HTTP 200; UNINVOICED; additionalParameter1=ALL-P1 … additionalParameter12=ALL-P12.',
          actualResult: `As expected — id=${created.id} status=${view.compensationStatus}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3341_KEY,
          relevantEntityKeys: ['customer', 'pod'],
          extraLinks: { compensation: [buildDevCompensationPreviewLink(created.id)] },
          snapshot: { tc: 'TC-BE-1', compensationId: created.id, number },
        });
      });
    });

    test('[PDT-3341]: TC-BE-2 – POST create omitting all additionalParameter fields; GET still returns twelve keys as empty or null', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(SIMPLE_TIMEOUT_MS);
      const fx: Pdt3341Fx = { Request, GeneratePayload, Responses, Endpoints };
      const number = uniqueCompNumber('PDT3341-TCBE2-OMIT');

      const pre = await test.step('Precondition: billed customer, recipient, POD', async () =>
        runPdt3341CustomerPodPrechain(fx),
      );

      const created = await test.step('POST required fields only (omit additionalParameter*)', async () => {
        const result = await createPdt3341Compensation(fx, {
          customerId: pre.customerId,
          podId: pre.podId,
          recipientId: pre.recipientCustomerId,
          documentPeriod: pre.documentPeriod,
          number,
          date: dateOnDayOfPeriod(pre.documentPeriod, 16),
          documentAmount: 33,
          volumes: 11,
          price: 3,
          reason: 'PDT-3341-TC-BE-2',
        });
        TestRunSummary.registerPayload('compensation', result.payload);
        for (const key of ADDITIONAL_PARAM_KEYS) {
          expect(
            Object.prototype.hasOwnProperty.call(result.payload, key),
            `create JSON must omit ${key}`,
          ).toBe(false);
        }
        return result;
      });

      await test.step('GET — twelve keys present as empty or null', async () => {
        const view = await getCompensation(Request, created.id);
        expect(view.compensationStatus).toBe('UNINVOICED');
        assertAdditionalKeysPresent(view as Record<string, unknown>, 'TC-BE-2');
        assertAdditionalExact(view as Record<string, unknown>, {}, 'TC-BE-2');
        TestRunSummary.recordCheck({
          check: 'TC-BE-2 omit keys; GET unused empty/null',
          expectedResult: 'POST 200; GET exposes additionalParameter1–12 as null or "".',
          actualResult: `As expected — id=${created.id}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3341_KEY,
          relevantEntityKeys: ['customer', 'pod'],
          extraLinks: { compensation: [buildDevCompensationPreviewLink(created.id)] },
          snapshot: { tc: 'TC-BE-2', compensationId: created.id, number },
        });
      });
    });

    test('[PDT-3341]: TC-BE-3 – POST create with mixed filled, empty-string, and null additional parameters; GET matches the mix', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(SIMPLE_TIMEOUT_MS);
      const fx: Pdt3341Fx = { Request, GeneratePayload, Responses, Endpoints };
      const number = uniqueCompNumber('PDT3341-TCBE3-MIX');
      const additional: AdditionalParameterMap = {
        additionalParameter1: 'MIX-FILLED-1',
        additionalParameter2: '',
        additionalParameter3: null,
        additionalParameter5: 'MIX-FILLED-5',
        additionalParameter6: '',
        additionalParameter7: 'MIX-FILLED-7',
      };

      const pre = await test.step('Precondition: billed customer, recipient, POD', async () =>
        runPdt3341CustomerPodPrechain(fx),
      );

      const created = await test.step('POST mixed occupancy payload', async () => {
        const result = await createPdt3341Compensation(fx, {
          customerId: pre.customerId,
          podId: pre.podId,
          recipientId: pre.recipientCustomerId,
          documentPeriod: pre.documentPeriod,
          number,
          date: dateOnDayOfPeriod(pre.documentPeriod, 17),
          documentAmount: 48,
          volumes: 12,
          price: 4,
          reason: 'PDT-3341-TC-BE-3',
          additional,
        });
        TestRunSummary.registerPayload('compensation', result.payload);
        return result;
      });

      await test.step('GET mixed values; unused empty/null', async () => {
        const view = await getCompensation(Request, created.id);
        assertAdditionalKeysPresent(view as Record<string, unknown>, 'TC-BE-3');
        expect(view.additionalParameter1).toBe('MIX-FILLED-1');
        expect(view.additionalParameter5).toBe('MIX-FILLED-5');
        expect(view.additionalParameter7).toBe('MIX-FILLED-7');
        for (const key of [
          'additionalParameter2',
          'additionalParameter3',
          'additionalParameter4',
          'additionalParameter6',
          'additionalParameter8',
          'additionalParameter9',
          'additionalParameter10',
          'additionalParameter11',
          'additionalParameter12',
        ] as const) {
          expect(isEmptyOrNull(view[key]), `TC-BE-3 unused ${key}`).toBe(true);
        }
        expect(toAmountNumber(view.documentAmount)).toBe(48);
        TestRunSummary.recordCheck({
          check: 'TC-BE-3 mixed occupancy',
          expectedResult: 'Filled 1/5/7 stored; 2/3/4/6/8–12 empty or null; amount 48.',
          actualResult: `As expected — id=${created.id}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3341_KEY,
          relevantEntityKeys: ['customer', 'pod'],
          extraLinks: { compensation: [buildDevCompensationPreviewLink(created.id)] },
          snapshot: { tc: 'TC-BE-3', compensationId: created.id, number },
        });
      });
    });

    test('[PDT-3341]: TC-BE-4 – POST create accepts Unicode and special characters in additional parameters; GET returns them unchanged', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(SIMPLE_TIMEOUT_MS);
      const fx: Pdt3341Fx = { Request, GeneratePayload, Responses, Endpoints };
      const number = uniqueCompNumber('PDT3341-TCBE4-UNI');
      const additional: AdditionalParameterMap = {
        additionalParameter1: 'Доп-парам_№1',
        additionalParameter2: '!@#$%^&*()[]{}',
        additionalParameter3: '€£¥ § ±',
        additionalParameter4: 'line1\tline2',
        additionalParameter5: 'ÄÖÜäöüß',
        additionalParameter6: '参数-6',
        additionalParameter7: 'P7-ok',
        additionalParameter8: 'P8-ok',
        additionalParameter9: 'P9-ok',
        additionalParameter10: 'P10-ok',
        additionalParameter11: 'P11-ok',
        additionalParameter12: 'P12-край',
      };

      const pre = await test.step('Precondition: billed customer, recipient, POD', async () =>
        runPdt3341CustomerPodPrechain(fx),
      );

      const created = await test.step('POST Unicode/special additional parameters', async () => {
        const result = await createPdt3341Compensation(fx, {
          customerId: pre.customerId,
          podId: pre.podId,
          recipientId: pre.recipientCustomerId,
          documentPeriod: pre.documentPeriod,
          number,
          date: dateOnDayOfPeriod(pre.documentPeriod, 18),
          documentAmount: 40,
          volumes: 8,
          price: 5,
          reason: 'PDT-3341-TC-BE-4',
          additional,
        });
        TestRunSummary.registerPayload('compensation', result.payload);
        return result;
      });

      await test.step('GET round-trip Unicode/special strings', async () => {
        const view = await getCompensation(Request, created.id);
        assertAdditionalExact(view as Record<string, unknown>, additional, 'TC-BE-4');
        TestRunSummary.recordCheck({
          check: 'TC-BE-4 Unicode/special round-trip',
          expectedResult: 'GET equals create strings (BG, symbols, tab, CJK, umlauts).',
          actualResult: `As expected — id=${created.id}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3341_KEY,
          relevantEntityKeys: ['customer', 'pod'],
          extraLinks: { compensation: [buildDevCompensationPreviewLink(created.id)] },
          snapshot: { tc: 'TC-BE-4', compensationId: created.id, number },
        });
      });
    });

    test('[PDT-3341]: TC-BE-5 – POST create accepts additionalParameter1 of exactly 64 characters', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(SIMPLE_TIMEOUT_MS);
      const fx: Pdt3341Fx = { Request, GeneratePayload, Responses, Endpoints };
      const number = uniqueCompNumber('PDT3341-TCBE5-L64');
      expect(PARAM64_A.length).toBe(64);

      const pre = await test.step('Precondition: billed customer, recipient, POD', async () =>
        runPdt3341CustomerPodPrechain(fx),
      );

      const created = await test.step('POST additionalParameter1 length 64', async () => {
        const result = await createPdt3341Compensation(fx, {
          customerId: pre.customerId,
          podId: pre.podId,
          recipientId: pre.recipientCustomerId,
          documentPeriod: pre.documentPeriod,
          number,
          date: dateOnDayOfPeriod(pre.documentPeriod, 19),
          documentAmount: 9,
          volumes: 9,
          price: 1,
          reason: 'PDT-3341-TC-BE-5',
          additional: { additionalParameter1: PARAM64_A },
        });
        TestRunSummary.registerPayload('compensation', result.payload);
        return result;
      });

      await test.step('GET additionalParameter1 length 64', async () => {
        const view = await getCompensation(Request, created.id);
        expect(String(view.additionalParameter1)).toBe(PARAM64_A);
        expect(String(view.additionalParameter1).length).toBe(64);
        TestRunSummary.recordCheck({
          check: 'TC-BE-5 create max 64',
          expectedResult: 'POST 200; GET additionalParameter1 is 64×A.',
          actualResult: `As expected — length=${String(view.additionalParameter1).length}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3341_KEY,
          relevantEntityKeys: ['customer', 'pod'],
          extraLinks: { compensation: [buildDevCompensationPreviewLink(created.id)] },
          snapshot: { tc: 'TC-BE-5', compensationId: created.id, number },
        });
      });
    });

    test('[PDT-3341]: TC-BE-6 – POST create rejects additionalParameter1 of 65 characters; no compensation created', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(SIMPLE_TIMEOUT_MS);
      const fx: Pdt3341Fx = { Request, GeneratePayload, Responses, Endpoints };
      const number = uniqueCompNumber('PDT3341-TCBE6-L65');
      expect(PARAM65_C.length).toBe(65);
      const fragment = sizeFragment(1);

      const pre = await test.step('Precondition: billed customer, recipient, POD', async () =>
        runPdt3341CustomerPodPrechain(fx),
      );

      await test.step('POST 65-char additionalParameter1 → 400', async () => {
        const payload = buildPdt3341CompensationPayload(GeneratePayload, {
          customerId: pre.customerId,
          podId: pre.podId,
          recipientId: pre.recipientCustomerId,
          documentPeriod: pre.documentPeriod,
          number,
          date: dateOnDayOfPeriod(pre.documentPeriod, 20),
          documentAmount: 7,
          volumes: 7,
          price: 1,
          reason: 'PDT-3341-TC-BE-6',
          additional: { additionalParameter1: PARAM65_C },
        });
        TestRunSummary.registerPayload('compensation', payload);
        const res = await Request.post(Endpoints.compensation, { data: payload });
        const err = await readHttpError(res);
        expect(err.status, 'TC-BE-6 must be HTTP 400 not 401/403').toBe(400);
        expect(err.text).toContain(fragment);
        TestRunSummary.recordCheck({
          check: 'TC-BE-6 create @Size 65',
          expectedResult: `HTTP 400 containing ${fragment}`,
          actualResult: `status=${err.status} body=${err.text.slice(0, 300)}`,
          passed: err.status === 400 && err.text.includes(fragment),
        });
      });

      await test.step('Listing has no row for rejected number', async () => {
        const listing = await getCompensationListingRow(Request, number);
        expect(listing.row, `no stored compensation ${number}`).toBeNull();
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3341_KEY,
          relevantEntityKeys: ['customer', 'pod'],
          snapshot: { tc: 'TC-BE-6', number },
        });
      });
    });

    test('[PDT-3341]: TC-BE-7 – Uninvoiced PUT accepts additionalParameter12 of exactly 64 characters; GET matches', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(SIMPLE_TIMEOUT_MS);
      const fx: Pdt3341Fx = { Request, GeneratePayload, Responses, Endpoints };
      const number = uniqueCompNumber('PDT3341-TCBE7-E64');
      expect(PARAM64_B.length).toBe(64);

      const pre = await test.step('Precondition: billed customer, recipient, POD', async () =>
        runPdt3341CustomerPodPrechain(fx),
      );

      const created = await test.step('Precondition: UNINVOICED compensation additionalParameter12=OLD12', async () => {
        const result = await createPdt3341Compensation(fx, {
          customerId: pre.customerId,
          podId: pre.podId,
          recipientId: pre.recipientCustomerId,
          documentPeriod: pre.documentPeriod,
          number,
          date: dateOnDayOfPeriod(pre.documentPeriod, 21),
          documentAmount: 12,
          volumes: 6,
          price: 2,
          reason: 'PDT-3341-TC-BE-7',
          additional: { additionalParameter12: 'OLD12' },
        });
        TestRunSummary.registerPayload('compensation', result.payload);
        return result;
      });

      await test.step('PUT additionalParameter12 length 64', async () => {
        const before = await getCompensation(Request, created.id);
        expect(before.compensationStatus).toBe('UNINVOICED');
        expect(before.additionalParameter12).toBe('OLD12');
        const edit = clonePayloadWithAdditional(created.payload, {
          additionalParameter12: PARAM64_B,
        });
        const put = await putPdt3341Compensation(fx, created.id, edit);
        expect(put.id).toBe(created.id);
        const after = await getCompensation(Request, created.id);
        expect(String(after.additionalParameter12)).toBe(PARAM64_B);
        expect(String(after.additionalParameter12).length).toBe(64);
        expect(after.compensationStatus).toBe('UNINVOICED');
        expect(toAmountNumber(after.documentAmount)).toBe(12);
        expect(toAmountNumber(after.volumes)).toBe(6);
        TestRunSummary.recordCheck({
          check: 'TC-BE-7 Uninvoiced PUT 64-char field 12',
          expectedResult: 'PUT 200; GET additionalParameter12=64×B; amount/volumes unchanged.',
          actualResult: `As expected — id=${created.id}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3341_KEY,
          relevantEntityKeys: ['customer', 'pod'],
          extraLinks: { compensation: [buildDevCompensationPreviewLink(created.id)] },
          snapshot: { tc: 'TC-BE-7', compensationId: created.id, number },
        });
      });
    });

    test('[PDT-3341]: TC-BE-8 – Uninvoiced PUT rejects additionalParameter7 of 65 characters; stored values unchanged', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(SIMPLE_TIMEOUT_MS);
      const fx: Pdt3341Fx = { Request, GeneratePayload, Responses, Endpoints };
      const number = uniqueCompNumber('PDT3341-TCBE8-E65');
      expect(PARAM65_D.length).toBe(65);
      const fragment = sizeFragment(7);

      const pre = await test.step('Precondition: billed customer, recipient, POD', async () =>
        runPdt3341CustomerPodPrechain(fx),
      );

      const created = await test.step('Precondition: UNINVOICED compensation KEEP-P1 / KEEP-P7', async () => {
        const result = await createPdt3341Compensation(fx, {
          customerId: pre.customerId,
          podId: pre.podId,
          recipientId: pre.recipientCustomerId,
          documentPeriod: pre.documentPeriod,
          number,
          date: dateOnDayOfPeriod(pre.documentPeriod, 22),
          documentAmount: 15,
          volumes: 5,
          price: 3,
          reason: 'PDT-3341-TC-BE-8',
          additional: { additionalParameter1: 'KEEP-P1', additionalParameter7: 'KEEP-P7' },
        });
        TestRunSummary.registerPayload('compensation', result.payload);
        return result;
      });

      await test.step('PUT 65-char additionalParameter7 → 400; GET unchanged', async () => {
        const before = await getCompensation(Request, created.id);
        expect(before.additionalParameter1).toBe('KEEP-P1');
        expect(before.additionalParameter7).toBe('KEEP-P7');
        const edit = clonePayloadWithAdditional(created.payload, {
          additionalParameter1: 'KEEP-P1',
          additionalParameter7: PARAM65_D,
        });
        const res = await Request.put(`${Endpoints.compensation}/${created.id}`, { data: edit });
        const err = await readHttpError(res);
        expect(err.status, 'TC-BE-8 PUT 400 not 401/403').toBe(400);
        expect(err.text).toContain(fragment);
        const after = await getCompensation(Request, created.id);
        expect(after.additionalParameter7).toBe('KEEP-P7');
        expect(after.additionalParameter1).toBe('KEEP-P1');
        expect(after.number).toBe(number);
        expect(after.compensationStatus).toBe('UNINVOICED');
        TestRunSummary.recordCheck({
          check: 'TC-BE-8 Uninvoiced PUT @Size 65 field 7',
          expectedResult: `HTTP 400 containing ${fragment}; GET still KEEP-P7 / KEEP-P1.`,
          actualResult: `status=${err.status}; p7=${after.additionalParameter7}`,
          passed: err.status === 400 && err.text.includes(fragment),
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3341_KEY,
          relevantEntityKeys: ['customer', 'pod'],
          extraLinks: { compensation: [buildDevCompensationPreviewLink(created.id)] },
          snapshot: { tc: 'TC-BE-8', compensationId: created.id, number },
        });
      });
    });

    test('[PDT-3341]: TC-BE-9 – Uninvoiced PUT updates all twelve additional parameters; GET matches; amounts and volumes unchanged', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(SIMPLE_TIMEOUT_MS);
      const fx: Pdt3341Fx = { Request, GeneratePayload, Responses, Endpoints };
      const number = uniqueCompNumber('PDT3341-TCBE9-PUT');
      const oldParams = filledParams('OLD-');
      const newParams = filledParams('NEW-');

      const pre = await test.step('Precondition: billed customer, recipient, POD', async () =>
        runPdt3341CustomerPodPrechain(fx),
      );

      const created = await test.step('Precondition: UNINVOICED compensation OLD-1…OLD-12', async () => {
        const result = await createPdt3341Compensation(fx, {
          customerId: pre.customerId,
          podId: pre.podId,
          recipientId: pre.recipientCustomerId,
          documentPeriod: pre.documentPeriod,
          number,
          date: dateOnDayOfPeriod(pre.documentPeriod, 23),
          documentAmount: 35,
          volumes: 14,
          price: 2.5,
          reason: 'PDT-3341-TC-BE-9',
          additional: oldParams,
        });
        TestRunSummary.registerPayload('compensation', result.payload);
        return result;
      });

      await test.step('PUT NEW-1…NEW-12; money fields unchanged', async () => {
        const before = await getCompensation(Request, created.id);
        assertAdditionalExact(before as Record<string, unknown>, oldParams, 'TC-BE-9 before');
        const edit = clonePayloadWithAdditional(created.payload, newParams);
        await putPdt3341Compensation(fx, created.id, edit);
        const after = await getCompensation(Request, created.id);
        assertAdditionalExact(after as Record<string, unknown>, newParams, 'TC-BE-9 after');
        expect(toAmountNumber(after.volumes)).toBe(14);
        expect(toAmountNumber(after.price)).toBe(2.5);
        expect(toAmountNumber(after.documentAmount)).toBe(35);
        expect(after.compensationStatus).toBe('UNINVOICED');
        TestRunSummary.recordCheck({
          check: 'TC-BE-9 PUT all twelve; amounts unchanged',
          expectedResult: 'GET NEW-1…NEW-12; volumes 14; price 2.5; amount 35; UNINVOICED.',
          actualResult: `As expected — id=${created.id}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3341_KEY,
          relevantEntityKeys: ['customer', 'pod'],
          extraLinks: { compensation: [buildDevCompensationPreviewLink(created.id)] },
          snapshot: { tc: 'TC-BE-9', compensationId: created.id, number },
        });
      });
    });

    test('[PDT-3341]: TC-BE-10 – After PDF and accounting, INVOICED GET still returns twelve additional parameters; receivable and liability amounts follow documentAmount', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(INVOICE_TIMEOUT_MS);
      const fx: Pdt3341Fx = { Request, GeneratePayload, Responses, Endpoints };
      const number = uniqueCompNumber('PDT3341-TCBE10-INV');
      const additional = filledParams('INV-P');

      const pre = await test.step('Precondition: FOR_VOLUMES contract + BBP', async () =>
        runDevVolCompContractAndBbpPrechain(fx),
      );
      const billing = await test.step('Precondition: FOR_VOLUMES draft billing', async () =>
        createForVolumesDraftBillingRun(fx),
      );
      const invoiceId = billing.draftInvoiceIds[0];

      const created = await test.step('Precondition: UNINVOICED compensation INV-P1…12', async () => {
        const result = await createPdt3341Compensation(fx, {
          customerId: pre.customerId,
          podId: pre.podId,
          recipientId: pre.recipientCustomerId,
          documentPeriod: pre.documentPeriod,
          number,
          date: dateOnDayOfPeriod(pre.documentPeriod, 10),
          documentAmount: 50,
          volumes: 20,
          price: 2.5,
          reason: 'PDT-3341-TC-BE-10',
          additional,
        });
        TestRunSummary.registerPayload('compensation', result.payload);
        return result;
      });

      await test.step('PDF + accounting → INVOICED GET still has twelve params', async () => {
        await startGeneratingAndWaitGenerated(Request, billing.billingRunId);
        await pollPdfDocumentsPresent(Request, billing.billingRunId);
        await startAccountingAndWaitCompleted(Request, billing.billingRunId);
        const view = await getCompensation(Request, created.id);
        expect(view.compensationStatus).toBe('INVOICED');
        assertAdditionalKeysPresent(view as Record<string, unknown>, 'TC-BE-10');
        assertAdditionalExact(view as Record<string, unknown>, additional, 'TC-BE-10');
        expect(toAmountNumber(view.documentAmount)).toBe(50);
        expect(toAmountNumber(view.volumes)).toBe(20);
        if (view.receivableForCustomer?.id) {
          const rec = await getCustomerReceivable(Request, Number(view.receivableForCustomer.id));
          expect(toAmountNumber(rec.initialAmount ?? rec.currentAmount)).toBeCloseTo(50, 2);
        }
        if (view.liabilityForRecipient?.id) {
          const liab = await getCustomerLiability(Request, Number(view.liabilityForRecipient.id));
          expect(toAmountNumber(liab.initialAmount ?? liab.currentAmount)).toBeCloseTo(50, 2);
        }
        const invoice = await getInvoice(Request, invoiceId);
        expect(Number(invoice.id), 'invoice GET id').toBe(invoiceId);
        TestRunSummary.recordCheck({
          check: 'TC-BE-10 INVOICED GET twelve params + documentAmount accounting',
          expectedResult: 'INVOICED; INV-P1…12 present; documentAmount 50; L/R follow amount 50.',
          actualResult: `As expected — status=${view.compensationStatus} R=${view.receivableForCustomer?.id} L=${view.liabilityForRecipient?.id}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3341_KEY,
          relevantEntityKeys: ['customer', 'productContract', 'billingRun', 'invoice'],
          extraLinks: {
            ...buildProductContractTabLinks(pre.contractId, DEV_PORTAL_BASE),
            billingRun: [buildDevBillingRunPreviewLink(billing.billingRunId)],
            invoice: [buildDevInvoicePreviewLink(invoiceId)],
            compensation: [buildDevCompensationPreviewLink(created.id)],
          },
          snapshot: { tc: 'TC-BE-10', compensationId: created.id, billingRunId: billing.billingRunId, invoiceId },
        });
      });
    });

    test('[PDT-3341]: TC-BE-11 – PUT of an INVOICED compensation is rejected with the edit-lock message; additional parameters unchanged', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(INVOICE_TIMEOUT_MS);
      const fx: Pdt3341Fx = { Request, GeneratePayload, Responses, Endpoints };
      const number = uniqueCompNumber('PDT3341-TCBE11-LOCK');
      const additional = filledParams('LOCK-P');

      const pre = await test.step('Precondition: FOR_VOLUMES contract + BBP', async () =>
        runDevVolCompContractAndBbpPrechain(fx),
      );
      const billing = await test.step('Precondition: FOR_VOLUMES draft billing', async () =>
        createForVolumesDraftBillingRun(fx),
      );

      const created = await test.step('Precondition: compensation then PDF+accounting → INVOICED', async () => {
        const result = await createPdt3341Compensation(fx, {
          customerId: pre.customerId,
          podId: pre.podId,
          recipientId: pre.recipientCustomerId,
          documentPeriod: pre.documentPeriod,
          number,
          date: dateOnDayOfPeriod(pre.documentPeriod, 11),
          documentAmount: 40,
          volumes: 16,
          price: 2.5,
          reason: 'PDT-3341-TC-BE-11',
          additional,
        });
        TestRunSummary.registerPayload('compensation', result.payload);
        await startGeneratingAndWaitGenerated(Request, billing.billingRunId);
        await startAccountingAndWaitCompleted(Request, billing.billingRunId);
        const invoiced = await getCompensation(Request, result.id);
        expect(invoiced.compensationStatus).toBe('INVOICED');
        return result;
      });

      await test.step('PUT INVOICED with HACK-P1 → 400 lock; GET unchanged', async () => {
        const before = await getCompensation(Request, created.id);
        expect(before.compensationStatus).toBe('INVOICED');
        expect(before.additionalParameter1).toBe('LOCK-P1');
        const edit = clonePayloadWithAdditional(created.payload, {
          ...additional,
          additionalParameter1: 'HACK-P1',
        });
        const res = await Request.put(`${Endpoints.compensation}/${created.id}`, { data: edit });
        const err = await readHttpError(res);
        const lock = invoicedLockFragment(created.id);
        expect(err.status, 'TC-BE-11 PUT 400 not 401/403').toBe(400);
        expect(err.text).toContain(lock);
        const after = await getCompensation(Request, created.id);
        expect(after.compensationStatus).toBe('INVOICED');
        assertAdditionalExact(after as Record<string, unknown>, additional, 'TC-BE-11 after illegal PUT');
        expect(after.additionalParameter1).not.toBe('HACK-P1');
        TestRunSummary.recordCheck({
          check: 'TC-BE-11 INVOICED PUT lock',
          expectedResult: `HTTP 400 containing ${lock}; params still LOCK-P1…12.`,
          actualResult: `status=${err.status}; p1=${after.additionalParameter1}`,
          passed: err.status === 400 && err.text.includes(lock),
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3341_KEY,
          relevantEntityKeys: ['customer', 'productContract', 'billingRun', 'invoice'],
          extraLinks: {
            billingRun: [buildDevBillingRunPreviewLink(billing.billingRunId)],
            compensation: [buildDevCompensationPreviewLink(created.id)],
          },
          snapshot: { tc: 'TC-BE-11', compensationId: created.id, billingRunId: billing.billingRunId },
        });
      });
    });

    test('[PDT-3341]: TC-BE-12 – Government compensation listing DTO does not expose additionalParameter1–12', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(SIMPLE_TIMEOUT_MS);
      const fx: Pdt3341Fx = { Request, GeneratePayload, Responses, Endpoints };
      const number = uniqueCompNumber('PDT3341-TCBE12-LST');
      const additional = filledParams('LIST-HIDE-');

      const pre = await test.step('Precondition: billed customer, recipient, POD', async () =>
        runPdt3341CustomerPodPrechain(fx),
      );

      const created = await test.step('Precondition: compensation with all twelve stored', async () => {
        const result = await createPdt3341Compensation(fx, {
          customerId: pre.customerId,
          podId: pre.podId,
          recipientId: pre.recipientCustomerId,
          documentPeriod: pre.documentPeriod,
          number,
          date: dateOnDayOfPeriod(pre.documentPeriod, 24),
          documentAmount: 16,
          volumes: 4,
          price: 4,
          reason: 'PDT-3341-TC-BE-12',
          additional,
        });
        TestRunSummary.registerPayload('compensation', result.payload);
        return result;
      });

      await test.step('Listing row has no additionalParameter keys; GET still has them', async () => {
        const listing = await getCompensationListingRow(Request, number);
        expect(listing.row, `listing row ${number}`).toBeTruthy();
        expect(Number(listing.row!.id)).toBe(created.id);
        const leaked = listingHasAdditionalParameterKeys(listing.row!);
        expect(leaked, 'listing must not expose additionalParameter*').toEqual([]);
        expect(listing.row!.number).toBe(number);
        expect(listing.row!.compensationStatus ?? listing.row!.status).toBeTruthy();
        const view = await getCompensation(Request, created.id);
        assertAdditionalExact(view as Record<string, unknown>, additional, 'TC-BE-12 view');
        TestRunSummary.recordCheck({
          check: 'TC-BE-12 listing omits additional parameters',
          expectedResult: 'Listing row present without additionalParameter1–12; GET still has LIST-HIDE-1…12.',
          actualResult: `As expected — listingKeys=${Object.keys(listing.row!).join(',')}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3341_KEY,
          relevantEntityKeys: ['customer', 'pod'],
          extraLinks: { compensation: [buildDevCompensationPreviewLink(created.id)] },
          snapshot: { tc: 'TC-BE-12', compensationId: created.id, number },
        });
      });
    });

    test('[PDT-3341]: TC-BE-14 – Mass import with all twelve additional-parameter cells filled persists values in mapper column order', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      FileUploadRequest,
      TestRunSummary,
    }) => {
      test.setTimeout(IMPORT_TIMEOUT_MS);
      const fx: Pdt3341Fx = { Request, GeneratePayload, Responses, Endpoints };
      const number = uniqueCompNumber('PDT3341-MI-FILL');
      const mapped = [
        'MAP-C11-P1',
        'MAP-C12-P2',
        'MAP-C13-P3',
        'MAP-C14-P4',
        'MAP-C15-P5',
        'MAP-C16-P6',
        'MAP-C17-P7',
        'MAP-C18-P8',
        'MAP-C19-P9',
        'MAP-C20-P10',
        'MAP-C21-P11',
        'MAP-C22-P12',
      ];

      const pre = await test.step('Precondition: billed customer, recipient, POD', async () =>
        runPdt3341CustomerPodPrechain(fx),
      );
      const ids = await test.step('Precondition: identifiers + currency name', async () =>
        resolveMassImportIdentifiers(Request, pre),
      );

      await test.step('Upload filled L–W cells; GET positional map', async () => {
        const beforeIds = await snapshotGcProcessIds(Request);
        const fileBuffer = await buildPdt3341MassImportBuffer(
          Request,
          {
            number,
            date: dateOnDayOfPeriod(pre.documentPeriod, 12),
            documentPeriod: pre.documentPeriod,
            volumes: 10,
            price: 1.5,
            reason: 'PDT-3341-TC-BE-14',
            documentAmount: 15,
            ...ids,
          },
          mapped,
        );
        TestRunSummary.registerPayload('massImportRow', { number, mapped, ...ids });
        const upload = await uploadGovernmentCompensationMassImport(
          FileUploadRequest,
          fileBuffer,
          'pdt-3341-mi-fill.xlsx',
        );
        expect(upload.status, 'mass import upload HTTP 202').toBe(202);
        await pollGovernmentCompensationMassImportComplete(Request, beforeIds);
        const found = await findCompensationByNumber(Request, number);
        expect(found, `imported ${number}`).toBeTruthy();
        const expected: AdditionalParameterMap = {};
        mapped.forEach((val, i) => {
          expected[`additionalParameter${i + 1}` as keyof AdditionalParameterMap] = val;
        });
        assertAdditionalExact(found as Record<string, unknown>, expected, 'TC-BE-14');
        TestRunSummary.recordCheck({
          check: 'TC-BE-14 mass import positional 11–22',
          expectedResult: 'additionalParameter1=MAP-C11-P1 … additionalParameter12=MAP-C22-P12 (no shift).',
          actualResult: `As expected — id=${found!.id}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3341_KEY,
          relevantEntityKeys: ['customer', 'pod'],
          snapshot: { tc: 'TC-BE-14', number },
        });
      });
    });

    test('[PDT-3341]: TC-BE-15 – Mass import with empty additional-parameter cells stores empty additional parameters', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      FileUploadRequest,
      TestRunSummary,
    }) => {
      test.setTimeout(IMPORT_TIMEOUT_MS);
      const fx: Pdt3341Fx = { Request, GeneratePayload, Responses, Endpoints };
      const number = uniqueCompNumber('PDT3341-MI-EMPTY');

      const pre = await test.step('Precondition: billed customer, recipient, POD', async () =>
        runPdt3341CustomerPodPrechain(fx),
      );
      const ids = await test.step('Precondition: identifiers + currency name', async () =>
        resolveMassImportIdentifiers(Request, pre),
      );

      await test.step('Upload blank L–W; GET unused empty/null', async () => {
        const beforeIds = await snapshotGcProcessIds(Request);
        const fileBuffer = await buildPdt3341MassImportBuffer(
          Request,
          {
            number,
            date: dateOnDayOfPeriod(pre.documentPeriod, 13),
            documentPeriod: pre.documentPeriod,
            volumes: 8,
            price: 2,
            reason: 'PDT-3341-TC-BE-15',
            documentAmount: 16,
            ...ids,
          },
          [],
        );
        TestRunSummary.registerPayload('massImportRow', { number, additional: 'blank 11–22', ...ids });
        const upload = await uploadGovernmentCompensationMassImport(
          FileUploadRequest,
          fileBuffer,
          'pdt-3341-mi-empty.xlsx',
        );
        expect(upload.status).toBe(202);
        await pollGovernmentCompensationMassImportComplete(Request, beforeIds);
        const found = await findCompensationByNumber(Request, number);
        expect(found, `imported ${number}`).toBeTruthy();
        expect(found!.number).toBe(number);
        expect(toAmountNumber(found!.documentAmount)).toBe(16);
        expect(found!.compensationStatus).toBe('UNINVOICED');
        assertAdditionalKeysPresent(found as Record<string, unknown>, 'TC-BE-15');
        assertAdditionalExact(found as Record<string, unknown>, {}, 'TC-BE-15');
        TestRunSummary.recordCheck({
          check: 'TC-BE-15 mass import empty cells',
          expectedResult: 'Compensation created; additionalParameter1–12 empty or null.',
          actualResult: `As expected — id=${found!.id}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3341_KEY,
          relevantEntityKeys: ['customer', 'pod'],
          snapshot: { tc: 'TC-BE-15', number },
        });
      });
    });

    test('[PDT-3341]: TC-BE-16 – Old Excel without the twelve additional-parameter columns is rejected as invalid file format', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      FileUploadRequest,
      TestRunSummary,
    }) => {
      test.setTimeout(IMPORT_TIMEOUT_MS);
      const fx: Pdt3341Fx = { Request, GeneratePayload, Responses, Endpoints };
      const number = uniqueCompNumber('PDT3341-MI-OLD');

      const pre = await test.step('Precondition: billed customer, recipient, POD', async () =>
        runPdt3341CustomerPodPrechain(fx),
      );
      const ids = await test.step('Precondition: identifiers + currency name', async () =>
        resolveMassImportIdentifiers(Request, pre),
      );

      await test.step('Upload old 11-column file → Invalid file format', async () => {
        await downloadGcTemplateBuffer(Request);
        const beforeIds = await snapshotGcProcessIds(Request);
        const fileBuffer = await buildOldFormatGcMassImportBuffer(Request, {
          number,
          date: dateOnDayOfPeriod(pre.documentPeriod, 14),
          documentPeriod: pre.documentPeriod,
          volumes: 3,
          price: 3,
          reason: 'PDT-3341-TC-BE-16',
          documentAmount: 9,
          ...ids,
        });
        TestRunSummary.registerPayload('massImportRow', { number, format: 'old-11-col', ...ids });
        const upload = await uploadGovernmentCompensationMassImport(
          FileUploadRequest,
          fileBuffer,
          'pdt-3341-mi-old.xlsx',
        );
        expect([400, 202, 200].includes(upload.status), 'not 401/403').toBe(true);
        expect(upload.status).not.toBe(401);
        expect(upload.status).not.toBe(403);
        const outcome = await assertInvalidFileFormatDualPath(Request, upload, beforeIds, number);
        TestRunSummary.recordCheck({
          check: 'TC-BE-16 old Excel Invalid file format',
          expectedResult: 'Upload 400 or process error containing Invalid file format; no compensation.',
          actualResult: `path=${outcome.path} status=${upload.status}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3341_KEY,
          relevantEntityKeys: ['customer', 'pod'],
          snapshot: { tc: 'TC-BE-16', number },
        });
      });
    });

    test('[PDT-3341]: TC-BE-17 – Excel whose additional-parameter header strings do not match the template is rejected as invalid file format', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      FileUploadRequest,
      TestRunSummary,
    }) => {
      test.setTimeout(IMPORT_TIMEOUT_MS);
      const fx: Pdt3341Fx = { Request, GeneratePayload, Responses, Endpoints };
      const number = uniqueCompNumber('PDT3341-MI-BADH');

      const pre = await test.step('Precondition: billed customer, recipient, POD', async () =>
        runPdt3341CustomerPodPrechain(fx),
      );
      const ids = await test.step('Precondition: identifiers + currency name', async () =>
        resolveMassImportIdentifiers(Request, pre),
      );

      await test.step('Upload Extra Param N headers → Invalid file format', async () => {
        const beforeIds = await snapshotGcProcessIds(Request);
        const fileBuffer = await buildWrongHeaderGcMassImportBuffer(
          Request,
          {
            number,
            date: dateOnDayOfPeriod(pre.documentPeriod, 25),
            documentPeriod: pre.documentPeriod,
            volumes: 2,
            price: 2,
            reason: 'PDT-3341-TC-BE-17',
            documentAmount: 4,
            ...ids,
          },
          'SHOULD-NOT-IMPORT',
        );
        TestRunSummary.registerPayload('massImportRow', { number, headers: 'Extra Param N', ...ids });
        const upload = await uploadGovernmentCompensationMassImport(
          FileUploadRequest,
          fileBuffer,
          'pdt-3341-mi-badh.xlsx',
        );
        expect(upload.status).not.toBe(401);
        expect(upload.status).not.toBe(403);
        const outcome = await assertInvalidFileFormatDualPath(Request, upload, beforeIds, number);
        TestRunSummary.recordCheck({
          check: 'TC-BE-17 wrong additional-parameter headers',
          expectedResult: 'Invalid file format; SHOULD-NOT-IMPORT not stored.',
          actualResult: `path=${outcome.path} status=${upload.status}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3341_KEY,
          relevantEntityKeys: ['customer', 'pod'],
          snapshot: { tc: 'TC-BE-17', number },
        });
      });
    });

    test('[PDT-3341]: TC-BE-18 – Mass-import row with additionalParameter3 longer than 64 characters fails that row; no compensation created', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      FileUploadRequest,
      TestRunSummary,
    }) => {
      test.setTimeout(IMPORT_TIMEOUT_MS);
      const fx: Pdt3341Fx = { Request, GeneratePayload, Responses, Endpoints };
      const number = uniqueCompNumber('PDT3341-MI-L65');
      expect(PARAM65_E.length).toBe(65);
      const fragment = sizeFragment(3);

      const pre = await test.step('Precondition: billed customer, recipient, POD', async () =>
        runPdt3341CustomerPodPrechain(fx),
      );
      const ids = await test.step('Precondition: identifiers + currency name', async () =>
        resolveMassImportIdentifiers(Request, pre),
      );

      await test.step('Upload 65-char additionalParameter3; row error; no compensation', async () => {
        const beforeIds = await snapshotGcProcessIds(Request);
        const additional: Array<string | null | undefined> = Array.from({ length: 12 }, () => undefined);
        additional[2] = PARAM65_E;
        const fileBuffer = await buildPdt3341MassImportBuffer(
          Request,
          {
            number,
            date: dateOnDayOfPeriod(pre.documentPeriod, 26),
            documentPeriod: pre.documentPeriod,
            volumes: 1,
            price: 1,
            reason: 'PDT-3341-TC-BE-18',
            documentAmount: 1,
            ...ids,
          },
          additional,
        );
        TestRunSummary.registerPayload('massImportRow', { number, additionalParameter3Length: 65, ...ids });
        const upload = await uploadGovernmentCompensationMassImport(
          FileUploadRequest,
          fileBuffer,
          'pdt-3341-mi-l65.xlsx',
        );
        expect([202, 200].includes(upload.status), `upload process created, got ${upload.status}`).toBe(
          true,
        );
        const outcome = await pollGcMassImportSettled(Request, beforeIds);
        expect(outcome, 'mass import process appeared').toBeTruthy();
        const blob = [
          upload.bodyText,
          outcome ? `${outcome.status} ${outcome.name}` : '',
          outcome ? await collectMassImportErrorBlob(Request, outcome.id) : '',
        ].join('\n');
        expect(blob).toContain(fragment);
        const found = await findCompensationByNumber(Request, number, { maxAttempts: 3, delayMs: 1000 });
        expect(found, `no compensation ${number}`).toBeNull();
        TestRunSummary.recordCheck({
          check: 'TC-BE-18 import @Size additionalParameter3',
          expectedResult: `Row error contains ${fragment}; listing empty.`,
          actualResult: `upload=${upload.status} found=${found?.id ?? 'none'}`,
          passed: blob.includes(fragment) && found == null,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3341_KEY,
          relevantEntityKeys: ['customer', 'pod'],
          snapshot: { tc: 'TC-BE-18', number },
        });
      });
    });

    test('[PDT-3341]: TC-BE-19 – Invoice document DD.TableCompensations AdditionalParameter1–12 match stored compensation values', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(INVOICE_TIMEOUT_MS);
      const fx: Pdt3341Fx = { Request, GeneratePayload, Responses, Endpoints };
      const number = uniqueCompNumber('PDT3341-TCBE19-DD');
      const additional = filledParams('DD-P');

      const pre = await test.step('Precondition: FOR_VOLUMES contract + BBP', async () =>
        runDevVolCompContractAndBbpPrechain(fx),
      );
      const billing = await test.step('Precondition: FOR_VOLUMES draft billing', async () =>
        createForVolumesDraftBillingRun(fx),
      );
      const invoiceId = billing.draftInvoiceIds[0];

      const created = await test.step('Precondition: compensation DD-P1…12', async () => {
        const result = await createPdt3341Compensation(fx, {
          customerId: pre.customerId,
          podId: pre.podId,
          recipientId: pre.recipientCustomerId,
          documentPeriod: pre.documentPeriod,
          number,
          date: dateOnDayOfPeriod(pre.documentPeriod, 9),
          documentAmount: 30,
          volumes: 10,
          price: 3,
          reason: 'PDT-3341-TC-BE-19',
          additional,
        });
        TestRunSummary.registerPayload('compensation', result.payload);
        return result;
      });

      await test.step('PDF then generate-invoice-data DD.TableCompensations', async () => {
        const stored = await getCompensation(Request, created.id);
        assertAdditionalExact(stored as Record<string, unknown>, additional, 'TC-BE-19 GET');
        await startGeneratingAndWaitGenerated(Request, billing.billingRunId);
        const pdfCount = await pollPdfDocumentsPresent(Request, billing.billingRunId);
        expect(pdfCount).toBeGreaterThan(0);
        const linked = await getCompensation(Request, created.id);
        expect(linked.index).toBe(0);
        expect(linked.invoice?.id).toBe(invoiceId);
        const doc = await fetchPdt3341InvoiceDocument(Request, invoiceId);
        const ddRows = findTableCompensationRows(doc, number);
        expect(ddRows.length, 'DD TableCompensations row').toBeGreaterThanOrEqual(1);
        assertInvoiceParamsExact(ddRows[0].row, additional, 'TC-BE-19 DD');
        TestRunSummary.recordCheck({
          check: 'TC-BE-19 DD.TableCompensations AdditionalParameter1–12',
          expectedResult: 'PascalCase AdditionalParameter1=DD-P1 … AdditionalParameter12=DD-P12.',
          actualResult: `As expected — invoiceId=${invoiceId} linked index=${linked.index}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3341_KEY,
          relevantEntityKeys: ['customer', 'productContract', 'billingRun', 'invoice'],
          extraLinks: {
            ...buildProductContractTabLinks(pre.contractId, DEV_PORTAL_BASE),
            billingRun: [buildDevBillingRunPreviewLink(billing.billingRunId)],
            invoice: [buildDevInvoicePreviewLink(invoiceId)],
            compensation: [buildDevCompensationPreviewLink(created.id)],
          },
          snapshot: { tc: 'TC-BE-19', compensationId: created.id, invoiceId, number },
        });
      });
    });

    test('[PDT-3341]: TC-BE-20 – SDCompensations groups by document number, date, period, and currency; distinct additional-parameter values are listed; no extra SD rows', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(INVOICE_TIMEOUT_MS);
      const fx: Pdt3341Fx = { Request, GeneratePayload, Responses, Endpoints };
      const number = uniqueCompNumber('PDT3341-SDGRP');

      const pre = await test.step('Precondition: two-POD FOR_VOLUMES chain', async () =>
        runMultiPodForVolumesPrechain(fx),
      );
      const billing = await test.step('Precondition: FOR_VOLUMES draft billing', async () =>
        createForVolumesDraftBillingRun(fx),
      );
      const invoiceId = billing.draftInvoiceIds[0];

      const { idA, idB } = await test.step('Precondition: two compensations same number, ALPHA vs BETA', async () => {
        const a = await createPdt3341Compensation(fx, {
          customerId: pre.customerId,
          podId: pre.podAId,
          recipientId: pre.recipientCustomerId,
          documentPeriod: pre.documentPeriod,
          number,
          date: dateOnDayOfPeriod(pre.documentPeriod, 8),
          documentAmount: 25,
          volumes: 10,
          price: 2.5,
          reason: 'PDT-3341-TC-BE-20-A',
          additional: { additionalParameter1: 'ALPHA' },
        });
        TestRunSummary.registerPayload('compensationA', a.payload);
        const payloadB = buildPdt3341CompensationPayload(GeneratePayload, {
          customerId: pre.customerId,
          podId: pre.podBId,
          recipientId: pre.recipientCustomerId,
          documentPeriod: pre.documentPeriod,
          number,
          date: dateOnDayOfPeriod(pre.documentPeriod, 8),
          documentAmount: 25,
          volumes: 10,
          price: 2.5,
          reason: 'PDT-3341-TC-BE-20-B',
          additional: { additionalParameter1: 'BETA' },
        });
        const resB = await Request.post(Endpoints.compensation, { data: payloadB });
        if (!resB.ok()) {
          const err = await readHttpError(resB);
          TestRunSummary.recordCheck({
            check: 'TC-BE-20 POST B same number',
            expectedResult: 'POST B HTTP 200 with the same document number (AC-10 grouping).',
            actualResult: `BLOCKED — duplicate number rejected status=${err.status} ${err.text.slice(0, 400)}`,
            passed: false,
          });
          expect(
            resB.ok(),
            `POST B same number must succeed (do not split SD groups): ${err.text}`,
          ).toBe(true);
        }
        await expect(resB).CheckResponse();
        const rawB = await resB.json();
        const idB = Number(rawB?.id ?? rawB);
        Responses.compensation.push({ id: idB, number, ...payloadB });
        TestRunSummary.registerPayload('compensationB', payloadB);
        return { idA: a.id, idB };
      });

      await test.step('PDF then one SD row listing ALPHA and BETA', async () => {
        await startGeneratingAndWaitGenerated(Request, billing.billingRunId);
        await pollPdfDocumentsPresent(Request, billing.billingRunId);
        const doc = await fetchPdt3341InvoiceDocument(Request, invoiceId);
        const sdRows = findSdCompensationRows(doc, number);
        expect(sdRows.length, 'exactly one SDCompensations row for grouping key').toBe(1);
        const sd = sdRows[0];
        const ap1 = String(sd.AdditionalParameter1 ?? '');
        expect(ap1.includes('ALPHA'), `AdditionalParameter1 lists ALPHA (got ${ap1})`).toBe(true);
        expect(ap1.includes('BETA'), `AdditionalParameter1 lists BETA (got ${ap1})`).toBe(true);
        expect(ap1).toMatch(/ALPHA,\s*BETA|BETA,\s*ALPHA/);
        expect(toAmountNumber(sd.Volumes)).toBe(20);
        expect(toAmountNumber(sd.Amount)).toBe(50);
        const ddRows = findTableCompensationRows(doc, number);
        expect(ddRows.length, 'two detailed TableCompensations rows').toBe(2);
        TestRunSummary.recordCheck({
          check: 'TC-BE-20 SD grouping AC-10',
          expectedResult: 'One SD row; AdditionalParameter1 lists ALPHA and BETA joined with ", "; volumes 20 amount 50.',
          actualResult: `sdCount=${sdRows.length} AdditionalParameter1=${ap1}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3341_KEY,
          relevantEntityKeys: ['customer', 'productContract', 'billingRun', 'invoice'],
          extraLinks: {
            billingRun: [buildDevBillingRunPreviewLink(billing.billingRunId)],
            invoice: [buildDevInvoicePreviewLink(invoiceId)],
            compensation: [idA, idB].map(buildDevCompensationPreviewLink),
          },
          snapshot: { tc: 'TC-BE-20', number, invoiceId, idA, idB },
        });
      });
    });

    test('[PDT-3341]: TC-BE-21 – Unused additional-parameter keys are present as empty string or null on DD.TableCompensations and SDCompensations', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(INVOICE_TIMEOUT_MS);
      const fx: Pdt3341Fx = { Request, GeneratePayload, Responses, Endpoints };
      const number = uniqueCompNumber('PDT3341-TCBE21-UNU');

      const pre = await test.step('Precondition: FOR_VOLUMES contract + BBP', async () =>
        runDevVolCompContractAndBbpPrechain(fx),
      );
      const billing = await test.step('Precondition: FOR_VOLUMES draft billing', async () =>
        createForVolumesDraftBillingRun(fx),
      );
      const invoiceId = billing.draftInvoiceIds[0];

      const created = await test.step('Precondition: compensation omitting all additionalParameter*', async () => {
        const result = await createPdt3341Compensation(fx, {
          customerId: pre.customerId,
          podId: pre.podId,
          recipientId: pre.recipientCustomerId,
          documentPeriod: pre.documentPeriod,
          number,
          date: dateOnDayOfPeriod(pre.documentPeriod, 7),
          documentAmount: 18,
          volumes: 6,
          price: 3,
          reason: 'PDT-3341-TC-BE-21',
        });
        TestRunSummary.registerPayload('compensation', result.payload);
        return result;
      });

      await test.step('generate-invoice-data unused keys on DD and SD', async () => {
        await startGeneratingAndWaitGenerated(Request, billing.billingRunId);
        await pollPdfDocumentsPresent(Request, billing.billingRunId);
        const linked = await getCompensation(Request, created.id);
        expect(linked.invoice?.id).toBe(invoiceId);
        const doc = await fetchPdt3341InvoiceDocument(Request, invoiceId);
        const ddRows = findTableCompensationRows(doc, number);
        const sdRows = findSdCompensationRows(doc, number);
        expect(ddRows.length).toBeGreaterThanOrEqual(1);
        expect(sdRows.length).toBeGreaterThanOrEqual(1);
        assertInvoiceParamsExact(ddRows[0].row, {}, 'TC-BE-21 DD');
        assertInvoiceParamsExact(sdRows[0], {}, 'TC-BE-21 SD');
        TestRunSummary.recordCheck({
          check: 'TC-BE-21 unused keys on DD and SD',
          expectedResult: 'AdditionalParameter1–12 present as "" or null on both objects.',
          actualResult: `As expected — invoiceId=${invoiceId}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3341_KEY,
          relevantEntityKeys: ['customer', 'productContract', 'billingRun', 'invoice'],
          extraLinks: {
            billingRun: [buildDevBillingRunPreviewLink(billing.billingRunId)],
            invoice: [buildDevInvoicePreviewLink(invoiceId)],
            compensation: [buildDevCompensationPreviewLink(created.id)],
          },
          snapshot: { tc: 'TC-BE-21', compensationId: created.id, invoiceId, number },
        });
      });
    });

    test('[PDT-3341]: TC-BE-24 – All twelve additional parameters filled appear on invoice DD and SD JSON after standard billing', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(INVOICE_TIMEOUT_MS);
      const fx: Pdt3341Fx = { Request, GeneratePayload, Responses, Endpoints };
      const number = uniqueCompNumber('PDT3341-FULL12');
      const additional = filledParams('INV-ALL');

      const pre = await test.step('Precondition: FOR_VOLUMES contract + BBP', async () =>
        runDevVolCompContractAndBbpPrechain(fx),
      );
      const billing = await test.step('Precondition: FOR_VOLUMES draft billing', async () =>
        createForVolumesDraftBillingRun(fx),
      );
      const invoiceId = billing.draftInvoiceIds[0];

      const created = await test.step('Precondition: compensation with all twelve additionalParameter* filled', async () => {
        const result = await createPdt3341Compensation(fx, {
          customerId: pre.customerId,
          podId: pre.podId,
          recipientId: pre.recipientCustomerId,
          documentPeriod: pre.documentPeriod,
          number,
          date: dateOnDayOfPeriod(pre.documentPeriod, 11),
          documentAmount: 36,
          volumes: 12,
          price: 3,
          reason: 'PDT-3341-TC-BE-24',
          additional,
        });
        TestRunSummary.registerPayload('compensation', result.payload);
        return result;
      });

      await test.step('PDF then generate-invoice-data — DD and SD AdditionalParameter1–12', async () => {
        const stored = await getCompensation(Request, created.id);
        assertAdditionalExact(stored as Record<string, unknown>, additional, 'TC-BE-24 GET');
        await startGeneratingAndWaitGenerated(Request, billing.billingRunId);
        const pdfCount = await pollPdfDocumentsPresent(Request, billing.billingRunId);
        expect(pdfCount).toBeGreaterThan(0);
        const linked = await getCompensation(Request, created.id);
        expect(linked.index).toBe(0);
        expect(linked.invoice?.id).toBe(invoiceId);
        const doc = await fetchPdt3341InvoiceDocument(Request, invoiceId);
        const ddRows = findTableCompensationRows(doc, number);
        expect(ddRows.length, 'DD TableCompensations row').toBeGreaterThanOrEqual(1);
        assertInvoiceParamsExact(ddRows[0].row, additional, 'TC-BE-24 DD');
        const sdRows = findSdCompensationRows(doc, number);
        expect(sdRows.length, 'exactly one SDCompensations row for single compensation').toBe(1);
        assertInvoiceParamsExact(sdRows[0], additional, 'TC-BE-24 SD');
        TestRunSummary.recordCheck({
          check: 'TC-BE-24 all twelve on DD and SD invoice JSON',
          expectedResult:
            'AdditionalParameter1=INV-ALL1 … AdditionalParameter12=INV-ALL12 on both DD.TableCompensations and SDCompensations.',
          actualResult: `As expected — invoiceId=${invoiceId} ddRows=${ddRows.length} sdRows=${sdRows.length}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3341_KEY,
          relevantEntityKeys: ['customer', 'productContract', 'billingRun', 'invoice'],
          extraLinks: {
            ...buildProductContractTabLinks(pre.contractId, DEV_PORTAL_BASE),
            billingRun: [buildDevBillingRunPreviewLink(billing.billingRunId)],
            invoice: [buildDevInvoicePreviewLink(invoiceId)],
            compensation: [buildDevCompensationPreviewLink(created.id)],
          },
          snapshot: { tc: 'TC-BE-24', compensationId: created.id, invoiceId, number },
        });
      });
    });

    test('[PDT-3341]: TC-BE-22 – Mixed filled and blank additional parameters in one SD group — spec lists all values including empty tokens', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(INVOICE_TIMEOUT_MS);
      const fx: Pdt3341Fx = { Request, GeneratePayload, Responses, Endpoints };
      const number = uniqueCompNumber('PDT3341-SDBLANK');
      const specAp1 = 'ONLY-A, ';
      const specAp2 = ', ONLY-B';

      const pre = await test.step('Precondition: two-POD FOR_VOLUMES chain', async () =>
        runMultiPodForVolumesPrechain(fx),
      );
      const billing = await test.step('Precondition: FOR_VOLUMES draft billing', async () =>
        createForVolumesDraftBillingRun(fx),
      );
      const invoiceId = billing.draftInvoiceIds[0];

      const { idA, idB } = await test.step('Precondition: compensation A then B (same number)', async () => {
        const a = await createPdt3341Compensation(fx, {
          customerId: pre.customerId,
          podId: pre.podAId,
          recipientId: pre.recipientCustomerId,
          documentPeriod: pre.documentPeriod,
          number,
          date: dateOnDayOfPeriod(pre.documentPeriod, 6),
          documentAmount: 22,
          volumes: 11,
          price: 2,
          reason: 'PDT-3341-TC-BE-22-A',
          additional: { additionalParameter1: 'ONLY-A' },
        });
        TestRunSummary.registerPayload('compensationA', a.payload);
        const payloadB = buildPdt3341CompensationPayload(GeneratePayload, {
          customerId: pre.customerId,
          podId: pre.podBId,
          recipientId: pre.recipientCustomerId,
          documentPeriod: pre.documentPeriod,
          number,
          date: dateOnDayOfPeriod(pre.documentPeriod, 6),
          documentAmount: 22,
          volumes: 11,
          price: 2,
          reason: 'PDT-3341-TC-BE-22-B',
          additional: { additionalParameter2: 'ONLY-B' },
        });
        const resB = await Request.post(Endpoints.compensation, { data: payloadB });
        if (!resB.ok()) {
          const err = await readHttpError(resB);
          TestRunSummary.recordCheck({
            check: 'TC-BE-22 POST B same number',
            expectedResult: 'POST B HTTP 200 with the same document number.',
            actualResult: `BLOCKED — status=${err.status} ${err.text.slice(0, 400)}`,
            passed: false,
          });
          expect(resB.ok(), `POST B same number must succeed: ${err.text}`).toBe(true);
        }
        await expect(resB).CheckResponse();
        const rawB = await resB.json();
        const idB = Number(rawB?.id ?? rawB);
        Responses.compensation.push({ id: idB, number, ...payloadB });
        TestRunSummary.registerPayload('compensationB', payloadB);
        return { idA: a.id, idB };
      });

      await test.step('generate-invoice-data SD exact empty-token join (spec / F3)', async () => {
        await startGeneratingAndWaitGenerated(Request, billing.billingRunId);
        await pollPdfDocumentsPresent(Request, billing.billingRunId);
        const doc = await fetchPdt3341InvoiceDocument(Request, invoiceId);
        const ddRows = findTableCompensationRows(doc, number);
        expect(ddRows.length).toBe(2);
        const ddP1 = ddRows.map((r) => String(r.row.AdditionalParameter1 ?? ''));
        const ddP2 = ddRows.map((r) => String(r.row.AdditionalParameter2 ?? ''));
        expect(ddP1, 'detailed AdditionalParameter1 includes ONLY-A (compensation A)').toContain('ONLY-A');
        expect(ddP2, 'detailed AdditionalParameter2 includes ONLY-B (compensation B)').toContain('ONLY-B');
        const sdRows = findSdCompensationRows(doc, number);
        expect(sdRows.length, 'exactly one SD row').toBe(1);
        const sd = sdRows[0];
        expect('AdditionalParameter1' in sd).toBe(true);
        expect('AdditionalParameter2' in sd).toBe(true);
        const actual1 = String(sd.AdditionalParameter1 ?? '');
        const actual2 = String(sd.AdditionalParameter2 ?? '');
        const droppedBlanks = actual1 === 'ONLY-A' && actual2 === 'ONLY-B';
        const passed = actual1 === specAp1 && actual2 === specAp2;
        TestRunSummary.recordCheck({
          check: 'TC-BE-22 SD mixed blanks including empty tokens (spec)',
          expectedResult: `AdditionalParameter1=${JSON.stringify(specAp1)}; AdditionalParameter2=${JSON.stringify(specAp2)}.`,
          actualResult: droppedBlanks
            ? `Not as expected — runtime joinParameter dropped blanks (Finding F3): AdditionalParameter1=${JSON.stringify(actual1)} AdditionalParameter2=${JSON.stringify(actual2)}`
            : `AdditionalParameter1=${JSON.stringify(actual1)} AdditionalParameter2=${JSON.stringify(actual2)}`,
          passed,
        });
        expect(actual1, 'spec AdditionalParameter1 = ONLY-A + ", " + empty (Finding F3 if ONLY-A only)').toBe(
          specAp1,
        );
        expect(actual2, 'spec AdditionalParameter2 = empty + ", " + ONLY-B (Finding F3 if ONLY-B only)').toBe(
          specAp2,
        );
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3341_KEY,
          relevantEntityKeys: ['customer', 'productContract', 'billingRun', 'invoice'],
          extraLinks: {
            billingRun: [buildDevBillingRunPreviewLink(billing.billingRunId)],
            invoice: [buildDevInvoicePreviewLink(invoiceId)],
            compensation: [idA, idB].map(buildDevCompensationPreviewLink),
          },
          snapshot: { tc: 'TC-BE-22', number, invoiceId, idA, idB },
        });
      });
    });

    test('[PDT-3341]: TC-BE-23 – Invoice FinalLiabilityAmount, compensation volumes, and currency are unchanged by additional parameters', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(INVOICE_TIMEOUT_MS);
      const fx: Pdt3341Fx = { Request, GeneratePayload, Responses, Endpoints };
      const number = uniqueCompNumber('PDT3341-TCBE23-AMT');
      const additional = filledParams('NO-MONEY-');
      additional.additionalParameter1 = 'NO-MONEY-IMPACT';
      additional.additionalParameter12 = 'NO-MONEY-12';

      const pre = await test.step('Precondition: FOR_VOLUMES contract + BBP', async () =>
        runDevVolCompContractAndBbpPrechain(fx),
      );
      const billing = await test.step('Precondition: FOR_VOLUMES draft billing', async () =>
        createForVolumesDraftBillingRun(fx),
      );
      const invoiceId = billing.draftInvoiceIds[0];
      const invoiceBefore = await getInvoice(Request, invoiceId);
      const invoiceTotalInclVat = toAmountNumber(invoiceBefore.totalAmountIncludingVat);

      const created = await test.step('Precondition: compensation with additional params amount 35', async () => {
        const result = await createPdt3341Compensation(fx, {
          customerId: pre.customerId,
          podId: pre.podId,
          recipientId: pre.recipientCustomerId,
          documentPeriod: pre.documentPeriod,
          number,
          date: dateOnDayOfPeriod(pre.documentPeriod, 5),
          documentAmount: 35,
          volumes: 7,
          price: 5,
          reason: 'PDT-3341-TC-BE-23',
          additional,
        });
        TestRunSummary.registerPayload('compensation', result.payload);
        return result;
      });

      await test.step('GET money fields + document JSON FinalLiability / SD amounts', async () => {
        const view = await getCompensation(Request, created.id);
        expect(toAmountNumber(view.volumes)).toBe(7);
        expect(toAmountNumber(view.documentAmount)).toBe(35);
        await startGeneratingAndWaitGenerated(Request, billing.billingRunId);
        await pollPdfDocumentsPresent(Request, billing.billingRunId);
        const after = await getCompensation(Request, created.id);
        expect(toAmountNumber(after.volumes)).toBe(7);
        expect(toAmountNumber(after.documentAmount)).toBe(35);
        const doc = await fetchPdt3341InvoiceDocument(Request, invoiceId);
        const sdRows = findSdCompensationRows(doc, number);
        expect(sdRows.length).toBeGreaterThanOrEqual(1);
        const sd = sdRows[0];
        expect(toAmountNumber(sd.Amount)).toBe(35);
        expect(toAmountNumber(sd.Volumes)).toBe(7);
        const sdCurrency = String(sd.Currency ?? doc.CurrencyAbr ?? doc.CurrencyPrintName ?? '');
        expect(sdCurrency, 'SD/document currency present').not.toBe('');
        const totalIncl = toAmountNumber(doc.TotalInclVat ?? invoiceTotalInclVat);
        const finalL = toAmountNumber(doc.FinalLiabilityAmount);
        expect(finalL, 'FinalLiabilityAmount numeric').toBeLessThanOrEqual(totalIncl);
        expect(
          totalIncl - finalL,
          'FinalLiabilityAmount reduced by at least compensation documentAmount 35 (same as without additional params)',
        ).toBeGreaterThanOrEqual(34.99);
        TestRunSummary.recordCheck({
          check: 'TC-BE-23 amounts/volumes/currency/final liability unchanged by additional params',
          expectedResult: 'GET volumes=7 amount=35; SD matches; FinalLiabilityAmount follows invoice−compensation formula.',
          actualResult: `totalIncl=${totalIncl} finalL=${finalL} sdAmount=${sd.Amount} invoiceDraftTotal=${invoiceTotalInclVat}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3341_KEY,
          relevantEntityKeys: ['customer', 'productContract', 'billingRun', 'invoice'],
          extraLinks: {
            billingRun: [buildDevBillingRunPreviewLink(billing.billingRunId)],
            invoice: [buildDevInvoicePreviewLink(invoiceId)],
            compensation: [buildDevCompensationPreviewLink(created.id)],
          },
          snapshot: {
            tc: 'TC-BE-23',
            compensationId: created.id,
            invoiceId,
            invoiceTotalInclVat,
          },
        });
      });
    });
  },
);
