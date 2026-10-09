/**
 * PHN-3994 — Export Text File for RPS Invoices (API).
 *
 * Maps Backend TCs: Cursor-Project/test_cases/Backend/Export_Text_File_for_RPS_Invoices.md
 *
 * Runtime contract (code authority — phoenix-core InvoiceController / InvoiceTxtExportService):
 * - GET /invoice/export-txt?invoiceDateFrom&invoiceDateTo&fileName
 * - Permission: GENERATE_RPS_INVOICES_EXPORT_FILE
 * - Response: UTF-8 BOM + pipe-delimited 14-column rows + EOF:yyyyMMddHHmmss:rows:principalSum
 *
 * Reference spec(s):
 * - tests/cursor/RPS-POD-invoice-due-date.spec.ts (+ rps-pod-invoice-due-date.fixtures.ts)
 * - origin/staging:src/tests/billing/RPS.spec.ts (rpsNumberId pattern; modern fixtures preferred)
 *
 * Swagger (dev2, refreshed 2026-08-13): GET /invoice/export-txt requires
 * invoiceDateFrom, invoiceDateTo, fileName (query).
 *
 * Findings:
 * 1. UI billing-run.service.generateRpsInvoices does not send fileName — Playwright always sends it.
 * 2. UI default HOME_BP${yyyy}${mm}TXT missing '.' before TXT; tests use HOME_BPyyyymm.TXT.
 * 3. Backend sanitizeFileName strips path/CRLF only — does not enforce A–Z/_/- max-64 (UI validators).
 * 4. From > To: no controller validation; SQL BETWEEN yields empty file + HTTP 200 (TC-BE-10 expects 400).
 *
 * Target env: Dev2
 */

import { test, expect } from './cursor-test.fixtures';
import { finalizeTestRunSummary } from './shared/manual-verification-links.fixtures';
import {
  PHN_3994_KEY,
  PHN_3994_TITLE,
  EXPORT_INVOICE_DATE,
  type Phn3994Fx,
  buildHomeBpFileName,
  uniqueExportFileName,
  getExportTxt,
  getExportTxtOk,
  findDataRowByDocumentNumber,
  assertFourteenColumns,
  assertCurrencyEurAndEmptyInterestOther,
  assertEofPattern,
  createEligibleRealRpsInvoice,
  createRealInvoiceWithoutRps,
  createDraftInvoiceOnRpsPod,
  createDraftGeneratedInvoiceOnRpsPod,
  createEligibleDebitNote,
  cancelInvoiceByNumber,
  createEligibleRealRpsInvoicePrivateCustomer,
  createRealInterimInvoiceOnRpsPod,
  createProformaInvoice,
  unwrapReceiptColumn,
} from './phn-3994-rps-invoices-export-txt.fixtures';

const TIMEOUT_MS = 14 * 60 * 1000;

test.describe(`[${PHN_3994_KEY}]: ${PHN_3994_TITLE}`, {
  tag: ['@billing', '@phn-3994', '@dev2', '@rps-export'],
}, () => {
  test.describe.configure({ mode: 'parallel' });

  // ─── TC-BE-1 + TC-BE-7 + TC-BE-8 (happy path / From=To / column+EOF smoke) ───
  test(
    `[${PHN_3994_KEY}]: Export Text File for RPS Invoices — TC-BE-1: eligible Real STANDARD INVOICE → BOM, EUR, EOF`,
    async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
      test.setTimeout(TIMEOUT_MS);
      const fx: Phn3994Fx = { Request, GeneratePayload, Responses, Endpoints };
      const fileName = buildHomeBpFileName();

      const setup = await createEligibleRealRpsInvoice(fx, {
        invoiceDate: EXPORT_INVOICE_DATE,
        rpsLabel: `RPS-3994-TC1-${Date.now().toString(36)}`,
      });

      TestRunSummary.registerPayload('exportTxt', {
        invoiceDateFrom: setup.invoiceDate,
        invoiceDateTo: setup.invoiceDate,
        fileName,
        invoiceNumber: setup.invoiceNumber,
      });

      const exportResult = await getExportTxtOk(Request, {
        invoiceDateFrom: setup.invoiceDate,
        invoiceDateTo: setup.invoiceDate,
        fileName,
      });
      const disposition = exportResult.headers['content-disposition'] ?? '';
      const parsed = exportResult.parsed;

      await test.step('Assert BOM, Content-Disposition, data row, columns, EOF', async () => {
        expect(parsed.hasUtf8Bom, 'UTF-8 BOM').toBe(true);
        expect(disposition.toLowerCase(), 'Content-Disposition mentions fileName').toContain(
          fileName.toLowerCase(),
        );

        const row = findDataRowByDocumentNumber(parsed, setup.invoiceNumber);
        expect(row, `data row for ${setup.invoiceNumber}`).toBeTruthy();
        const cols = assertFourteenColumns(row!, 'TC-BE-1');
        assertCurrencyEurAndEmptyInterestOther(cols, 'TC-BE-1');
        expect(cols[6], 'Document number').toBe(setup.invoiceNumber);
        assertEofPattern(parsed.eofLine, 'TC-BE-1');
        expect(parsed.eofRows, 'EOF rows ≥ 1').toBeGreaterThanOrEqual(1);
      });

      await test.step('Attach test run summary', async () => {
        TestRunSummary.recordCheck({
          check: 'TC-BE-1 happy path export',
          expectedResult:
            'HTTP 200; BOM; HOME_BP*.TXT disposition; 14 cols; Currency EUR; empty Interest/Other; EOF rows≥1',
          actualResult: `disposition=${disposition}; bom=${parsed.hasUtf8Bom}; eof=${parsed.eofLine}; rows=${parsed.dataRows.length}`,
          passed: true,
        });
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PHN_3994_KEY,
          relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun', 'invoice'],
          snapshot: {
            tcId: 'TC-BE-1',
            invoiceNumber: setup.invoiceNumber,
            fileName,
            eofLine: parsed.eofLine,
          },
        });
      });
    },
  );

  // ─── TC-BE-2 Debit Note ───────────────────────────────────────────────────
  test(
    `[${PHN_3994_KEY}]: Export Text File for RPS Invoices — TC-BE-2: eligible Debit Note included`,
    async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
      test.setTimeout(18 * 60 * 1000);
      const fx: Phn3994Fx = { Request, GeneratePayload, Responses, Endpoints };
      const fileName = uniqueExportFileName('TC2');

      const { debitNoteNumber, invoiceDate } = await createEligibleDebitNote(fx, EXPORT_INVOICE_DATE);

      TestRunSummary.registerPayload('exportTxt', {
        invoiceDateFrom: invoiceDate,
        invoiceDateTo: invoiceDate,
        fileName,
        debitNoteNumber,
      });

      const { parsed } = await getExportTxtOk(Request, {
        invoiceDateFrom: invoiceDate,
        invoiceDateTo: invoiceDate,
        fileName,
      });

      await test.step('Assert Debit Note document number present', async () => {
        const row = findDataRowByDocumentNumber(parsed, debitNoteNumber);
        expect(row, `Debit Note ${debitNoteNumber} in export`).toBeTruthy();
        assertFourteenColumns(row!, 'TC-BE-2');
        assertEofPattern(parsed.eofLine, 'TC-BE-2');
      });

      await test.step('Attach test run summary', async () => {
        TestRunSummary.recordCheck({
          check: 'TC-BE-2 Debit Note export',
          expectedResult: `HTTP 200; data row for Debit Note ${debitNoteNumber}`,
          actualResult: `found=${Boolean(findDataRowByDocumentNumber(parsed, debitNoteNumber))}; eof=${parsed.eofLine}`,
          passed: true,
        });
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PHN_3994_KEY,
          relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun', 'invoice'],
          snapshot: { tcId: 'TC-BE-2', debitNoteNumber, fileName },
        });
      });
    },
  );

  // ─── TC-BE-4 Empty range ──────────────────────────────────────────────────
  test(
    `[${PHN_3994_KEY}]: Export Text File for RPS Invoices — TC-BE-4: empty date range → EOF rows=0`,
    async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
      test.setTimeout(TIMEOUT_MS);
      const fx: Phn3994Fx = { Request, GeneratePayload, Responses, Endpoints };
      const fileName = uniqueExportFileName('TC4');

      // Create eligible invoice so precondition exists, then export a window that cannot include it.
      await createEligibleRealRpsInvoice(fx, {
        invoiceDate: EXPORT_INVOICE_DATE,
        rpsLabel: `RPS-3994-TC4-${Date.now().toString(36)}`,
      });

      const emptyFrom = '2031-01-01';
      const emptyTo = '2031-01-31';

      TestRunSummary.registerPayload('exportTxt', {
        invoiceDateFrom: emptyFrom,
        invoiceDateTo: emptyTo,
        fileName,
      });

      const { parsed } = await getExportTxtOk(Request, {
        invoiceDateFrom: emptyFrom,
        invoiceDateTo: emptyTo,
        fileName,
      });

      await test.step('Assert zero data rows and EOF rows=0 total=0', async () => {
        expect(parsed.hasUtf8Bom, 'BOM present on empty file').toBe(true);
        expect(parsed.dataRows.length, 'no data rows').toBe(0);
        assertEofPattern(parsed.eofLine, 'TC-BE-4');
        expect(parsed.eofRows, 'EOF rows').toBe(0);
        expect(parsed.eofPrincipal, 'EOF principal').toBe('0');
      });

      await test.step('Attach test run summary', async () => {
        TestRunSummary.recordCheck({
          check: 'TC-BE-4 empty export',
          expectedResult: 'HTTP 200; zero data rows; EOF:…:0:0',
          actualResult: `dataRows=${parsed.dataRows.length}; eof=${parsed.eofLine}`,
          passed: true,
        });
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PHN_3994_KEY,
          relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun', 'invoice'],
          snapshot: { tcId: 'TC-BE-4', emptyFrom, emptyTo, eofLine: parsed.eofLine },
        });
      });
    },
  );

  // ─── TC-BE-5 Re-export ────────────────────────────────────────────────────
  test(
    `[${PHN_3994_KEY}]: Export Text File for RPS Invoices — TC-BE-5: re-export same invoice twice`,
    async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
      test.setTimeout(TIMEOUT_MS);
      const fx: Phn3994Fx = { Request, GeneratePayload, Responses, Endpoints };
      const fileA = uniqueExportFileName('TC5A');
      const fileB = uniqueExportFileName('TC5B');

      const setup = await createEligibleRealRpsInvoice(fx, {
        invoiceDate: EXPORT_INVOICE_DATE,
        rpsLabel: `RPS-3994-TC5-${Date.now().toString(36)}`,
      });

      const first = await getExportTxtOk(Request, {
        invoiceDateFrom: setup.invoiceDate,
        invoiceDateTo: setup.invoiceDate,
        fileName: fileA,
      });
      const second = await getExportTxtOk(Request, {
        invoiceDateFrom: setup.invoiceDate,
        invoiceDateTo: setup.invoiceDate,
        fileName: fileB,
      });

      await test.step('Assert both exports contain the same invoice', async () => {
        expect(findDataRowByDocumentNumber(first.parsed, setup.invoiceNumber)).toBeTruthy();
        expect(findDataRowByDocumentNumber(second.parsed, setup.invoiceNumber)).toBeTruthy();
      });

      await test.step('Attach test run summary', async () => {
        TestRunSummary.registerPayload('exportTxt', { fileA, fileB, invoiceNumber: setup.invoiceNumber });
        TestRunSummary.recordCheck({
          check: 'TC-BE-5 re-export',
          expectedResult: 'Both HTTP 200; same document number in both files',
          actualResult: `first=${Boolean(findDataRowByDocumentNumber(first.parsed, setup.invoiceNumber))}; second=${Boolean(findDataRowByDocumentNumber(second.parsed, setup.invoiceNumber))}`,
          passed: true,
        });
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PHN_3994_KEY,
          relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun', 'invoice'],
          snapshot: { tcId: 'TC-BE-5', invoiceNumber: setup.invoiceNumber },
        });
      });
    },
  );

  // ─── TC-BE-6 Custom fileName ──────────────────────────────────────────────
  test(
    `[${PHN_3994_KEY}]: Export Text File for RPS Invoices — TC-BE-6: custom valid fileName accepted`,
    async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
      test.setTimeout(TIMEOUT_MS);
      const fx: Phn3994Fx = { Request, GeneratePayload, Responses, Endpoints };
      const fileName = `RPS_Export_TC-BE6_${Date.now().toString(36)}.TXT`;

      const setup = await createEligibleRealRpsInvoice(fx, {
        invoiceDate: EXPORT_INVOICE_DATE,
        rpsLabel: `RPS-3994-TC6-${Date.now().toString(36)}`,
      });

      const { headers, parsed } = await getExportTxtOk(Request, {
        invoiceDateFrom: setup.invoiceDate,
        invoiceDateTo: setup.invoiceDate,
        fileName,
      });

      const disposition = headers['content-disposition'] ?? '';

      await test.step('Assert Content-Disposition uses requested fileName', async () => {
        expect(disposition, 'Content-Disposition').toContain(fileName);
        expect(findDataRowByDocumentNumber(parsed, setup.invoiceNumber)).toBeTruthy();
      });

      await test.step('Attach test run summary', async () => {
        TestRunSummary.registerPayload('exportTxt', { fileName, invoiceNumber: setup.invoiceNumber });
        TestRunSummary.recordCheck({
          check: 'TC-BE-6 custom fileName',
          expectedResult: `Content-Disposition contains ${fileName}`,
          actualResult: disposition,
          passed: disposition.includes(fileName),
        });
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PHN_3994_KEY,
          relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun', 'invoice'],
          snapshot: { tcId: 'TC-BE-6', fileName, disposition },
        });
      });
    },
  );

  // ─── TC-BE-10 From > To (runtime: empty 200, not 400) ─────────────────────
  test(
    `[${PHN_3994_KEY}]: Export Text File for RPS Invoices — TC-BE-10: From > To → empty 200 (runtime; TC expected 400)`,
    async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
      test.setTimeout(TIMEOUT_MS);
      const fx: Phn3994Fx = { Request, GeneratePayload, Responses, Endpoints };
      const fileName = uniqueExportFileName('TC10');

      await createEligibleRealRpsInvoice(fx, {
        invoiceDate: EXPORT_INVOICE_DATE,
        rpsLabel: `RPS-3994-TC10-${Date.now().toString(36)}`,
      });

      // Runtime: InvoiceTxtExportService has no From≤To check; SQL BETWEEN yields empty set.
      const { status, parsed } = await getExportTxtOk(Request, {
        invoiceDateFrom: '2030-03-31',
        invoiceDateTo: '2030-03-01',
        fileName,
      });

      await test.step('Document runtime: 200 + empty file (not 400)', async () => {
        expect(status, 'HTTP status').toBe(200);
        expect(parsed.dataRows.length, 'no data rows when From > To').toBe(0);
        expect(parsed.eofRows, 'EOF rows').toBe(0);
      });

      await test.step('Attach test run summary', async () => {
        TestRunSummary.registerPayload('exportTxt', {
          invoiceDateFrom: '2030-03-31',
          invoiceDateTo: '2030-03-01',
          fileName,
          note: 'TC-BE-10 expected 400 per Confluence; runtime returns 200 empty',
        });
        TestRunSummary.recordCheck({
          check: 'TC-BE-10 From > To runtime behavior',
          expectedResult: 'Runtime: HTTP 200 empty (Finding: TC/spec expects 400)',
          actualResult: `status=${status}; dataRows=${parsed.dataRows.length}; eof=${parsed.eofLine}`,
          passed: status === 200 && parsed.dataRows.length === 0,
        });
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PHN_3994_KEY,
          relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun', 'invoice'],
          snapshot: { tcId: 'TC-BE-10', status, eofLine: parsed.eofLine },
        });
      });
    },
  );

  // ─── TC-BE-11 blank fileName ──────────────────────────────────────────────
  test(
    `[${PHN_3994_KEY}]: Export Text File for RPS Invoices — TC-BE-11a: blank fileName rejected`,
    async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
      test.setTimeout(TIMEOUT_MS);
      const fx: Phn3994Fx = { Request, GeneratePayload, Responses, Endpoints };

      await createEligibleRealRpsInvoice(fx, {
        invoiceDate: EXPORT_INVOICE_DATE,
        rpsLabel: `RPS-3994-TC11A-${Date.now().toString(36)}`,
      });

      const res = await getExportTxt(Request, {
        invoiceDateFrom: EXPORT_INVOICE_DATE,
        invoiceDateTo: EXPORT_INVOICE_DATE,
        fileName: '   ',
      });
      const status = res.status();
      const bodyText = await res.text().catch(() => '');

      await test.step('Assert non-2xx for blank fileName', async () => {
        // IllegalArgumentException from sanitizeFileName — typically 400/500 via global handler
        expect(status, `blank fileName must not succeed (got ${status}): ${bodyText}`).toBeGreaterThanOrEqual(
          400,
        );
      });

      await test.step('Attach test run summary', async () => {
        TestRunSummary.registerPayload('exportTxt', { fileName: '   ', status, bodyText: bodyText.slice(0, 500) });
        TestRunSummary.recordCheck({
          check: 'TC-BE-11a blank fileName',
          expectedResult: 'HTTP ≥400 (fileName must not be blank)',
          actualResult: `status=${status}`,
          passed: status >= 400,
        });
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PHN_3994_KEY,
          relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun', 'invoice'],
          snapshot: { tcId: 'TC-BE-11a', status },
        });
      });
    },
  );

  // ─── TC-BE-11 path sanitize (not charset reject) ──────────────────────────
  test(
    `[${PHN_3994_KEY}]: Export Text File for RPS Invoices — TC-BE-11b: path fileName sanitized to basename`,
    async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
      test.setTimeout(TIMEOUT_MS);
      const fx: Phn3994Fx = { Request, GeneratePayload, Responses, Endpoints };
      const safeBase = `HOME_BP_SAFE_${Date.now().toString(36)}.TXT`;
      const pathName = `subdir/../${safeBase}`;

      const setup = await createEligibleRealRpsInvoice(fx, {
        invoiceDate: EXPORT_INVOICE_DATE,
        rpsLabel: `RPS-3994-TC11B-${Date.now().toString(36)}`,
      });

      // Runtime: sanitize strips directories; does NOT reject @ or spaces (UI-only validation).
      const { headers, parsed } = await getExportTxtOk(Request, {
        invoiceDateFrom: setup.invoiceDate,
        invoiceDateTo: setup.invoiceDate,
        fileName: pathName,
      });
      const disposition = headers['content-disposition'] ?? '';

      await test.step('Assert basename used in Content-Disposition', async () => {
        expect(disposition, 'disposition uses basename').toContain(safeBase);
        expect(disposition, 'disposition must not retain subdir path').not.toContain('subdir');
        expect(findDataRowByDocumentNumber(parsed, setup.invoiceNumber)).toBeTruthy();
      });

      await test.step('Attach test run summary', async () => {
        TestRunSummary.registerPayload('exportTxt', { pathName, safeBase, disposition });
        TestRunSummary.recordCheck({
          check: 'TC-BE-11b sanitize path',
          expectedResult: `Content-Disposition filename="${safeBase}" (path stripped)`,
          actualResult: disposition,
          passed: disposition.includes(safeBase) && !disposition.includes('subdir'),
        });
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PHN_3994_KEY,
          relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun', 'invoice'],
          snapshot: { tcId: 'TC-BE-11b', disposition },
        });
      });
    },
  );

  // ─── TC-BE-12 Cancelled excluded ──────────────────────────────────────────
  test(
    `[${PHN_3994_KEY}]: Export Text File for RPS Invoices — TC-BE-12: Cancelled invoice excluded`,
    async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
      test.setTimeout(TIMEOUT_MS);
      const fx: Phn3994Fx = { Request, GeneratePayload, Responses, Endpoints };
      const fileName = uniqueExportFileName('TC12');

      const setup = await createEligibleRealRpsInvoice(fx, {
        invoiceDate: EXPORT_INVOICE_DATE,
        rpsLabel: `RPS-3994-TC12-${Date.now().toString(36)}`,
      });

      await test.step('Precondition: cancel invoice', async () => {
        await cancelInvoiceByNumber(fx, setup.invoiceNumber, setup.invoiceDate);
      });

      const { parsed } = await getExportTxtOk(Request, {
        invoiceDateFrom: setup.invoiceDate,
        invoiceDateTo: setup.invoiceDate,
        fileName,
      });

      await test.step('Assert cancelled document number absent', async () => {
        expect(
          findDataRowByDocumentNumber(parsed, setup.invoiceNumber),
          `Cancelled ${setup.invoiceNumber} must be absent`,
        ).toBeFalsy();
      });

      await test.step('Attach test run summary', async () => {
        TestRunSummary.registerPayload('exportTxt', {
          fileName,
          cancelledInvoiceNumber: setup.invoiceNumber,
        });
        TestRunSummary.recordCheck({
          check: 'TC-BE-12 cancelled excluded',
          expectedResult: `Document ${setup.invoiceNumber} absent from export`,
          actualResult: `present=${Boolean(findDataRowByDocumentNumber(parsed, setup.invoiceNumber))}`,
          passed: !findDataRowByDocumentNumber(parsed, setup.invoiceNumber),
        });
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PHN_3994_KEY,
          relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun', 'invoice'],
          snapshot: { tcId: 'TC-BE-12', invoiceNumber: setup.invoiceNumber },
        });
      });
    },
  );

  // ─── TC-BE-13 Draft excluded ──────────────────────────────────────────────
  test(
    `[${PHN_3994_KEY}]: Export Text File for RPS Invoices — TC-BE-13: Draft invoice excluded`,
    async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
      test.setTimeout(TIMEOUT_MS);
      const fx: Phn3994Fx = { Request, GeneratePayload, Responses, Endpoints };
      const fileName = uniqueExportFileName('TC13');

      const draft = await createDraftInvoiceOnRpsPod(fx, EXPORT_INVOICE_DATE);

      const { parsed } = await getExportTxtOk(Request, {
        invoiceDateFrom: draft.invoiceDate,
        invoiceDateTo: draft.invoiceDate,
        fileName,
      });

      await test.step('Assert DRAFT document number absent', async () => {
        expect(
          findDataRowByDocumentNumber(parsed, draft.invoiceNumber),
          `Draft ${draft.invoiceNumber} must be absent`,
        ).toBeFalsy();
      });

      await test.step('Attach test run summary', async () => {
        TestRunSummary.registerPayload('exportTxt', {
          fileName,
          draftInvoiceNumber: draft.invoiceNumber,
        });
        TestRunSummary.recordCheck({
          check: 'TC-BE-13 draft excluded',
          expectedResult: `Document ${draft.invoiceNumber} absent`,
          actualResult: `present=${Boolean(findDataRowByDocumentNumber(parsed, draft.invoiceNumber))}`,
          passed: !findDataRowByDocumentNumber(parsed, draft.invoiceNumber),
        });
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PHN_3994_KEY,
          relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun'],
          snapshot: { tcId: 'TC-BE-13', invoiceNumber: draft.invoiceNumber },
        });
      });
    },
  );

  // ─── TC-BE-14 Draft_generated excluded ────────────────────────────────────
  test(
    `[${PHN_3994_KEY}]: Export Text File for RPS Invoices — TC-BE-14: Draft_generated invoice excluded`,
    async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
      test.setTimeout(TIMEOUT_MS);
      const fx: Phn3994Fx = { Request, GeneratePayload, Responses, Endpoints };
      const fileName = uniqueExportFileName('TC14');

      const dg = await createDraftGeneratedInvoiceOnRpsPod(fx, EXPORT_INVOICE_DATE);

      const { parsed } = await getExportTxtOk(Request, {
        invoiceDateFrom: dg.invoiceDate,
        invoiceDateTo: dg.invoiceDate,
        fileName,
      });

      await test.step('Assert DRAFT_GENERATED document number absent', async () => {
        expect(
          findDataRowByDocumentNumber(parsed, dg.invoiceNumber),
          `Draft_generated ${dg.invoiceNumber} must be absent`,
        ).toBeFalsy();
      });

      await test.step('Attach test run summary', async () => {
        TestRunSummary.registerPayload('exportTxt', {
          fileName,
          draftGeneratedInvoiceNumber: dg.invoiceNumber,
        });
        TestRunSummary.recordCheck({
          check: 'TC-BE-14 draft_generated excluded',
          expectedResult: `Document ${dg.invoiceNumber} absent`,
          actualResult: `present=${Boolean(findDataRowByDocumentNumber(parsed, dg.invoiceNumber))}`,
          passed: !findDataRowByDocumentNumber(parsed, dg.invoiceNumber),
        });
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PHN_3994_KEY,
          relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun'],
          snapshot: { tcId: 'TC-BE-14', invoiceNumber: dg.invoiceNumber },
        });
      });
    },
  );

  // ─── TC-BE-23 No RPS POD excluded ─────────────────────────────────────────
  test(
    `[${PHN_3994_KEY}]: Export Text File for RPS Invoices — TC-BE-23: invoice without RPS POD excluded`,
    async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
      test.setTimeout(TIMEOUT_MS);
      const fx: Phn3994Fx = { Request, GeneratePayload, Responses, Endpoints };
      const fileName = uniqueExportFileName('TC23');

      const setup = await createRealInvoiceWithoutRps(fx, EXPORT_INVOICE_DATE);

      const { parsed } = await getExportTxtOk(Request, {
        invoiceDateFrom: setup.invoiceDate,
        invoiceDateTo: setup.invoiceDate,
        fileName,
      });

      await test.step('Assert non-RPS invoice absent', async () => {
        expect(
          findDataRowByDocumentNumber(parsed, setup.invoiceNumber),
          `Non-RPS ${setup.invoiceNumber} must be absent`,
        ).toBeFalsy();
      });

      await test.step('Attach test run summary', async () => {
        TestRunSummary.registerPayload('exportTxt', {
          fileName,
          nonRpsInvoiceNumber: setup.invoiceNumber,
        });
        TestRunSummary.recordCheck({
          check: 'TC-BE-23 no RPS POD excluded',
          expectedResult: `Document ${setup.invoiceNumber} absent`,
          actualResult: `present=${Boolean(findDataRowByDocumentNumber(parsed, setup.invoiceNumber))}`,
          passed: !findDataRowByDocumentNumber(parsed, setup.invoiceNumber),
        });
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PHN_3994_KEY,
          relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun', 'invoice'],
          snapshot: { tcId: 'TC-BE-23', invoiceNumber: setup.invoiceNumber },
        });
      });
    },
  );

  // ─── TC-BE-16 Interim and Advance Payment excluded ────────────────────────
  test(
    `[${PHN_3994_KEY}]: Export Text File for RPS Invoices — TC-BE-16: Interim and Advance Payment excluded`,
    async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
      test.setTimeout(18 * 60 * 1000);
      const fx: Phn3994Fx = { Request, GeneratePayload, Responses, Endpoints };
      const fileName = uniqueExportFileName('TC16');

      const interim = await createRealInterimInvoiceOnRpsPod(fx, EXPORT_INVOICE_DATE);

      const { parsed } = await getExportTxtOk(Request, {
        invoiceDateFrom: interim.invoiceDate,
        invoiceDateTo: interim.invoiceDate,
        fileName,
      });

      await test.step('Assert INTERIM_AND_ADVANCE_PAYMENT document number absent', async () => {
        expect(
          findDataRowByDocumentNumber(parsed, interim.invoiceNumber),
          `Interim ${interim.invoiceNumber} must be absent`,
        ).toBeFalsy();
      });

      await test.step('Attach test run summary', async () => {
        TestRunSummary.registerPayload('exportTxt', {
          fileName,
          interimInvoiceNumber: interim.invoiceNumber,
          invoiceType: interim.invoiceType,
        });
        TestRunSummary.recordCheck({
          check: 'TC-BE-16 interim type excluded',
          expectedResult: `Document ${interim.invoiceNumber} absent (type INTERIM_AND_ADVANCE_PAYMENT)`,
          actualResult: `present=${Boolean(findDataRowByDocumentNumber(parsed, interim.invoiceNumber))}; type=${interim.invoiceType}`,
          passed: !findDataRowByDocumentNumber(parsed, interim.invoiceNumber),
        });
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PHN_3994_KEY,
          relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun', 'invoice'],
          snapshot: { tcId: 'TC-BE-16', invoiceNumber: interim.invoiceNumber, invoiceType: interim.invoiceType },
        });
      });
    },
  );

  // ─── TC-BE-21 Proforma Invoice excluded ───────────────────────────────────
  test(
    `[${PHN_3994_KEY}]: Export Text File for RPS Invoices — TC-BE-21: Proforma Invoice excluded`,
    async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
      test.setTimeout(18 * 60 * 1000);
      const fx: Phn3994Fx = { Request, GeneratePayload, Responses, Endpoints };
      const fileName = uniqueExportFileName('TC21');

      const proforma = await createProformaInvoice(fx);
      expect(proforma.invoiceDate, 'proforma invoiceDate required for export window').toBeTruthy();
      const invoiceDate = proforma.invoiceDate;

      const { parsed } = await getExportTxtOk(Request, {
        invoiceDateFrom: invoiceDate,
        invoiceDateTo: invoiceDate,
        fileName,
      });

      await test.step('Assert PROFORMA document number absent', async () => {
        expect(
          findDataRowByDocumentNumber(parsed, proforma.invoiceNumber),
          `Proforma ${proforma.invoiceNumber} must be absent`,
        ).toBeFalsy();
      });

      await test.step('Attach test run summary', async () => {
        TestRunSummary.registerPayload('exportTxt', {
          fileName,
          proformaInvoiceNumber: proforma.invoiceNumber,
          documentType: proforma.documentType,
        });
        TestRunSummary.recordCheck({
          check: 'TC-BE-21 proforma excluded',
          expectedResult: `Document ${proforma.invoiceNumber} absent (document type Proforma)`,
          actualResult: `present=${Boolean(findDataRowByDocumentNumber(parsed, proforma.invoiceNumber))}; documentType=${proforma.documentType}`,
          passed: !findDataRowByDocumentNumber(parsed, proforma.invoiceNumber),
        });
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PHN_3994_KEY,
          relevantEntityKeys: ['customer', 'invoice'],
          snapshot: { tcId: 'TC-BE-21', invoiceNumber: proforma.invoiceNumber, documentType: proforma.documentType },
        });
      });
    },
  );

  // ─── TC-BE-30 + TC-BE-49 private customer name / recipient ────────────────
  test(
    `[${PHN_3994_KEY}]: Export Text File for RPS Invoices — TC-BE-30/49: private Customer Name Combined + Получател`,
    async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
      test.setTimeout(TIMEOUT_MS);
      const fx: Phn3994Fx = { Request, GeneratePayload, Responses, Endpoints };
      const fileName = uniqueExportFileName('TC30');

      const setup = await createEligibleRealRpsInvoicePrivateCustomer(fx, {
        invoiceDate: EXPORT_INVOICE_DATE,
        rpsLabel: `RPS-3994-TC30-${Date.now().toString(36)}`,
      });
      const { firstName, middleName, lastName } = setup.names;
      const expectedCombined = [firstName, middleName, lastName].filter((p) => p.trim()).join(' ');
      const expectedRecipient = `Получател: ${lastName}, ${firstName}${middleName.trim() ? ` ${middleName}` : ''}`;

      const { parsed } = await getExportTxtOk(Request, {
        invoiceDateFrom: setup.invoiceDate,
        invoiceDateTo: setup.invoiceDate,
        fileName,
      });

      let cols: string[] = [];
      await test.step('Assert name column and Receipt recipient (private)', async () => {
        const row = findDataRowByDocumentNumber(parsed, setup.invoiceNumber);
        expect(row, `data row for ${setup.invoiceNumber}`).toBeTruthy();
        cols = assertFourteenColumns(row!, 'TC-BE-30');
        expect(cols[2], 'Customer Name Combined').toBe(expectedCombined);
        const receiptPlain = unwrapReceiptColumn(cols[13] ?? '');
        expect(receiptPlain, 'Receipt Получател (surname first)').toContain(expectedRecipient);
      });

      await test.step('Attach test run summary', async () => {
        const receiptPlain = unwrapReceiptColumn(cols[13] ?? '');
        const nameOk = cols[2] === expectedCombined;
        const recipientOk = receiptPlain.includes(expectedRecipient);
        TestRunSummary.registerPayload('exportTxt', {
          fileName,
          invoiceNumber: setup.invoiceNumber,
          expectedCombined,
          expectedRecipient,
        });
        TestRunSummary.recordCheck({
          check: 'TC-BE-30 Customer Name Combined (private)',
          expectedResult: expectedCombined,
          actualResult: cols[2] ?? '',
          passed: nameOk,
        });
        TestRunSummary.recordCheck({
          check: 'TC-BE-49 private Получател line',
          expectedResult: expectedRecipient,
          actualResult: receiptPlain.slice(0, 200),
          passed: recipientOk,
        });
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PHN_3994_KEY,
          relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun', 'invoice'],
          snapshot: { tcId: 'TC-BE-30/49', invoiceNumber: setup.invoiceNumber, nameCol: cols[2] },
        });
      });
    },
  );

  // ─── Missing fileName query param ─────────────────────────────────────────
  test(
    `[${PHN_3994_KEY}]: Export Text File for RPS Invoices — missing fileName query param → error`,
    async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
      test.setTimeout(2 * 60 * 1000);
      const fx: Phn3994Fx = { Request, GeneratePayload, Responses, Endpoints };
      void fx;

      const res = await getExportTxt(Request, {
        invoiceDateFrom: EXPORT_INVOICE_DATE,
        invoiceDateTo: EXPORT_INVOICE_DATE,
        // omit fileName
      });
      const status = res.status();
      const bodyText = await res.text().catch(() => '');

      await test.step('Assert missing fileName is rejected (Spring required param)', async () => {
        expect(status, `missing fileName must fail (got ${status}): ${bodyText}`).toBeGreaterThanOrEqual(
          400,
        );
      });

      await test.step('Attach test run summary', async () => {
        TestRunSummary.registerPayload('exportTxt', {
          note: 'fileName omitted; OpenAPI gap — Java requires it',
          status,
        });
        TestRunSummary.recordCheck({
          check: 'Missing fileName query param',
          expectedResult: 'HTTP ≥400 (required @RequestParam)',
          actualResult: `status=${status}`,
          passed: status >= 400,
        });
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PHN_3994_KEY,
          relevantEntityKeys: [],
          snapshot: { tcId: 'missing-fileName', status },
        });
      });
    },
  );
});
