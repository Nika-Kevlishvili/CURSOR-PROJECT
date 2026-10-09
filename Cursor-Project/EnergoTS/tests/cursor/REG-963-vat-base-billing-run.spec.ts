/**
 * REG-963 — For volumes - vat base case
 *
 * Automates TC-BE-1 … TC-BE-10 from
 * Cursor-Project/test_cases/Backend/VAT_base_billing_run.md
 *
 * Target env: Dev. TCs validate documented Confluence spec; TC-BE-1 / TC-BE-6 /
 * TC-BE-9 may fail against current runtime (Findings in the TC file).
 *
 * Reference spec(s):
 * - tests/billing/forVolumes/forVolumes.spec.ts
 * - tests/cursor/rps-pod-invoice-due-date.fixtures.ts
 * - tests/cursor/pdt-3223-invoice-json-tablepcscales-listpc-order.fixtures.ts
 * - tests/cursor/pdt-2915-invoice-correction-deleted-bbp.fixtures.ts
 * - tests/cursor/PHN-3951-cancelled-interim-invoice-price-component.spec.ts
 */

import { test, expect } from './cursor-test.fixtures';
import { finalizeTestRunSummary } from './shared/manual-verification-links.fixtures';
import {
  REG_963_KEY,
  REG_963_TITLE,
  MSG_VAT_BASE_REQUIRES_ALT_RECIPIENT,
  MSG_ALT_RECIPIENT_REQUIRES_VAT_BASE,
  amountsClose,
  billedValueForUnitPrice,
  createLegalCustomer,
  createPeriodicalVatBaseServiceChain,
  createVolumesVatBaseChain,
  createProfileData,
  customerIdentifier,
  excludedPcRows,
  excludedRowMatches,
  fetchInvoiceDocumentModel,
  getCustomerLiability,
  getInvoice,
  getInvoiceDetailedData,
  getInvoiceSummaryData,
  liabilitiesForInvoice,
  listCustomerLiabilities,
  listInvoicesByBillingRun,
  money,
  pickDraftByDocumentType,
  postPriceComponentExpecting400,
  runBillingThroughStatus,
  startAccountingToCompleted,
  tableExcludedPcRows,
  vatPercentFromInvoice,
  asBillingRunId,
  type Reg963Fx,
} from './reg-963-vat-base-billing-run.fixtures';

const BILLING_TIMEOUT_MS = 20 * 60 * 1000;
const NEGATIVE_TIMEOUT_MS = 5 * 60 * 1000;

function asFx(args: {
  Request: Reg963Fx['Request'];
  GeneratePayload: Reg963Fx['GeneratePayload'];
  Responses: Reg963Fx['Responses'];
  Endpoints: Reg963Fx['Endpoints'];
}): Reg963Fx {
  return args;
}

function expectedInclVat(exVat: number, vatPercent: number): number {
  return exVat * (1 + vatPercent / 100);
}

function registerChainPayloads(
  TestRunSummary: { registerPayload: (key: string, payload: unknown) => void },
  chain: {
    invoiceCustomer: unknown;
    billingRunId: number;
    altCustomers?: unknown[];
    altCustomer?: unknown;
  },
): void {
  TestRunSummary.registerPayload('customer', chain.invoiceCustomer);
  TestRunSummary.registerPayload('billingRun', { billingRunId: chain.billingRunId });
  if (chain.altCustomers?.length) {
    TestRunSummary.registerPayload('altCustomer', chain.altCustomers);
  } else if (chain.altCustomer) {
    TestRunSummary.registerPayload('altCustomer', chain.altCustomer);
  }
}

test.describe(`[${REG_963_KEY}]: ${REG_963_TITLE}`, { tag: ['@billing', '@reg-963'] }, () => {
  test(`[${REG_963_KEY}]: TC-BE-1 – Standard billing excludes VAT base from invoice totals and creates alt-recipient liability`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(BILLING_TIMEOUT_MS);
    const fx = asFx({ Request, GeneratePayload, Responses, Endpoints });

    const chain = await test.step('Precondition: volumes chain (regular 100 + VAT base 50)', async () =>
      createVolumesVatBaseChain(fx, {
        regularExpression: 100,
        vatBaseComponents: [{ expression: 50, altCustomerIndex: 1 }],
      }),
    );
    registerChainPayloads(TestRunSummary, chain);

    const { invoiceId } = await test.step('STANDARD_BILLING start-billing → COMPLETED', async () =>
      runBillingThroughStatus(fx, chain.billingRunId, 'COMPLETED'),
    );

    const invoice = await test.step('GET /invoice?id=', async () => getInvoice(fx, invoiceId));
    const detailed = await test.step('GET /invoice/detailed-data and /invoice/summary-data', async () => {
      const d = await getInvoiceDetailedData(fx, invoiceId);
      await getInvoiceSummaryData(fx, invoiceId);
      return d;
    });
    const expectedExVat = billedValueForUnitPrice(detailed, 100);
    const expectedAlt = billedValueForUnitPrice(detailed, 50);
    expect(expectedExVat, 'regular PC billed net (unitPrice 100 × kWh)').toBeGreaterThan(0);
    expect(expectedAlt, 'VAT base PC billed net (unitPrice 50 × kWh)').toBeGreaterThan(0);

    const vatPercent = vatPercentFromInvoice(invoice);
    const expectedIncl = expectedInclVat(expectedExVat, vatPercent);
    const exVat = money(invoice.totalAmountExcludingVat);
    const inclVat = money(invoice.totalAmountIncludingVat);

    const invoiceIdent = customerIdentifier(chain.invoiceCustomer);
    const altIdent = customerIdentifier(chain.altCustomers[0]);

    const altRows = await test.step('GET customer-liability/list for alternative recipient', async () =>
      listCustomerLiabilities(fx, altIdent),
    );
    const altForInvoice = await liabilitiesForInvoice(fx, altRows, invoiceId);
    expect(altForInvoice.length, 'exactly one alt liability for this invoice').toBe(1);
    const altLiability = altForInvoice[0];
    Responses.customerLiability.push(altLiability);

    const invoiceCustRows = await test.step('GET customer-liability/list for invoice customer', async () =>
      listCustomerLiabilities(fx, invoiceIdent),
    );
    const mainRows = await liabilitiesForInvoice(fx, invoiceCustRows, invoiceId);
    expect(mainRows.length, 'invoice-customer INVOICE liability').toBeGreaterThanOrEqual(1);
    const mainLiability = mainRows[0];

    const invoiceOk =
      String(invoice.invoiceStatus) === 'REAL' &&
      amountsClose(exVat, expectedExVat) &&
      amountsClose(inclVat, expectedIncl);
    const altOk =
      amountsClose(altLiability.initialAmount, expectedAlt) &&
      amountsClose(altLiability.currentAmount, expectedAlt) &&
      String(altLiability.creationType) === 'AUTOMATIC';
    const mainInitialOk = amountsClose(mainLiability.initialAmount, inclVat);
    const mainOffsetOnce = amountsClose(mainLiability.currentAmount, inclVat - expectedAlt);

    TestRunSummary.recordCheck({
      check: 'TC-BE-1 invoice totals exclude VAT base PC (regular line only)',
      expectedResult: `REAL invoice exVat=${expectedExVat} (unitPrice 100 × volume), inclVat=${expectedIncl.toFixed(2)}`,
      actualResult: `status=${invoice.invoiceStatus} type=${invoice.invoiceType} exVat=${exVat} inclVat=${inclVat}`,
      passed: invoiceOk,
    });
    TestRunSummary.recordCheck({
      check: 'TC-BE-1 alternative-recipient AUTOMATIC liability equals VAT base billed net',
      expectedResult: `One AUTOMATIC liability initialAmount=${expectedAlt} currentAmount=${expectedAlt} billingGroup null`,
      actualResult: `count=${altForInvoice.length} initial=${altLiability.initialAmount} current=${altLiability.currentAmount} creationType=${altLiability.creationType} billingGroup=${JSON.stringify(altLiability.billingGroupResponse)}`,
      passed: altOk && altLiability.billingGroupResponse == null,
    });
    TestRunSummary.recordCheck({
      check: 'TC-BE-1 main liability offset once by alt billed net (spec receivable offset)',
      expectedResult: `initialAmount=${inclVat} currentAmount=${inclVat - expectedAlt}`,
      actualResult: `initial=${mainLiability.initialAmount} current=${mainLiability.currentAmount}`,
      passed: mainInitialOk && mainOffsetOnce,
    });

    expect(String(invoice.invoiceStatus), 'invoiceStatus').toBe('REAL');
    expect(
      String(invoice.invoiceType) === 'STANDARD' || String(invoice.invoiceDocumentType) === 'INVOICE',
      `invoiceType=${invoice.invoiceType} invoiceDocumentType=${invoice.invoiceDocumentType}`,
    ).toBe(true);
    expect(amountsClose(exVat, expectedExVat), `ex-VAT ${exVat} must equal regular billed ${expectedExVat}`).toBe(
      true,
    );
    expect(amountsClose(inclVat, expectedIncl), `incl-VAT ${inclVat} must be ${expectedIncl}`).toBe(
      true,
    );
    expect(altForInvoice.length).toBe(1);
    expect(
      altLiability.billingGroupResponse == null,
      'alt liability billing group must be null',
    ).toBe(true);
    expect(amountsClose(altLiability.initialAmount, expectedAlt)).toBe(true);
    expect(amountsClose(altLiability.currentAmount, expectedAlt)).toBe(true);
    expect(amountsClose(mainLiability.initialAmount, inclVat)).toBe(true);
    expect(
      amountsClose(mainLiability.currentAmount, inclVat - expectedAlt),
      'main currentAmount equals inclVat minus one alt offset',
    ).toBe(true);

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: REG_963_KEY,
        relevantEntityKeys: ['customer', 'product', 'productContract', 'billingRun', 'invoice', 'customerLiability'],
        snapshot: { invoiceId, billingRunId: chain.billingRunId, exVat, inclVat },
      });
    });
  });

  test(`[${REG_963_KEY}]: TC-BE-2 – Two VAT base price components for the same alternative recipient create one grouped liability`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(BILLING_TIMEOUT_MS);
    const fx = asFx({ Request, GeneratePayload, Responses, Endpoints });

    const chain = await test.step('Precondition: regular 100 + VAT base 30 and 20 same alt', async () =>
      createVolumesVatBaseChain(fx, {
        regularExpression: 100,
        vatBaseComponents: [
          { expression: 30, altCustomerIndex: 1 },
          { expression: 20, altCustomerIndex: 1 },
        ],
      }),
    );
    registerChainPayloads(TestRunSummary, chain);

    const { invoiceId } = await test.step('STANDARD_BILLING → COMPLETED', async () =>
      runBillingThroughStatus(fx, chain.billingRunId, 'COMPLETED'),
    );
    const invoice = await getInvoice(fx, invoiceId);
    const detailed = await getInvoiceDetailedData(fx, invoiceId);
    const expectedExVat = billedValueForUnitPrice(detailed, 100);
    const expectedAlt =
      billedValueForUnitPrice(detailed, 30) + billedValueForUnitPrice(detailed, 20);
    expect(expectedExVat).toBeGreaterThan(0);
    expect(expectedAlt).toBeGreaterThan(0);
    const altRows = await liabilitiesForInvoice(
      fx,
      await listCustomerLiabilities(fx, customerIdentifier(chain.altCustomers[0])),
      invoiceId,
    );
    expect(altRows.length, 'grouped: exactly one alt liability').toBe(1);
    const detail = altRows[0];
    Responses.customerLiability.push(detail);

    const passed =
      amountsClose(invoice.totalAmountExcludingVat, expectedExVat) &&
      amountsClose(detail.initialAmount, expectedAlt) &&
      amountsClose(detail.currentAmount, expectedAlt) &&
      String(detail.creationType) === 'AUTOMATIC';
    TestRunSummary.recordCheck({
      check: 'TC-BE-2 grouping by alternative recipient (unitPrice 30+20 → one liability)',
      expectedResult: `exVat=${expectedExVat}; one AUTOMATIC alt liability ${expectedAlt}`,
      actualResult: `exVat=${invoice.totalAmountExcludingVat} altCount=${altRows.length} initial=${detail.initialAmount}`,
      passed,
    });
    expect(amountsClose(invoice.totalAmountExcludingVat, expectedExVat)).toBe(true);
    expect(altRows.length).toBe(1);
    expect(amountsClose(detail.initialAmount, expectedAlt)).toBe(true);

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: REG_963_KEY,
        relevantEntityKeys: ['customer', 'productContract', 'billingRun', 'invoice', 'customerLiability'],
        snapshot: { invoiceId },
      });
    });
  });

  test(`[${REG_963_KEY}]: TC-BE-3 – Two alternative recipients create two separate VAT base liabilities`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(BILLING_TIMEOUT_MS);
    const fx = asFx({ Request, GeneratePayload, Responses, Endpoints });

    const chain = await test.step('Precondition: regular 100 + VAT 30 (alt A) + VAT 20 (alt B)', async () =>
      createVolumesVatBaseChain(fx, {
        regularExpression: 100,
        vatBaseComponents: [
          { expression: 30, altCustomerIndex: 1 },
          { expression: 20, altCustomerIndex: 2 },
        ],
      }),
    );
    registerChainPayloads(TestRunSummary, chain);

    const { invoiceId } = await test.step('STANDARD_BILLING → COMPLETED', async () =>
      runBillingThroughStatus(fx, chain.billingRunId, 'COMPLETED'),
    );
    const invoice = await getInvoice(fx, invoiceId);
    const detailed = await getInvoiceDetailedData(fx, invoiceId);
    await getInvoiceSummaryData(fx, invoiceId);
    const expectedExVat = billedValueForUnitPrice(detailed, 100);
    const expectedA = billedValueForUnitPrice(detailed, 30);
    const expectedB = billedValueForUnitPrice(detailed, 20);
    expect(expectedExVat).toBeGreaterThan(0);
    expect(expectedA).toBeGreaterThan(0);
    expect(expectedB).toBeGreaterThan(0);

    const rowsA = await liabilitiesForInvoice(
      fx,
      await listCustomerLiabilities(fx, customerIdentifier(chain.altCustomers[0])),
      invoiceId,
    );
    const rowsB = await liabilitiesForInvoice(
      fx,
      await listCustomerLiabilities(fx, customerIdentifier(chain.altCustomers[1])),
      invoiceId,
    );
    expect(rowsA.length, 'recipient A one liability').toBe(1);
    expect(rowsB.length, 'recipient B one liability').toBe(1);
    const detA = rowsA[0];
    const detB = rowsB[0];

    const invoiceCustRows = await listCustomerLiabilities(
      fx,
      customerIdentifier(chain.invoiceCustomer),
    );
    const invoiceCustForThis = await liabilitiesForInvoice(fx, invoiceCustRows, invoiceId);
    const invoiceCustHasAltAmounts = invoiceCustForThis.some(
      (r) => amountsClose(r.initialAmount, expectedA) || amountsClose(r.initialAmount, expectedB),
    );

    TestRunSummary.recordCheck({
      check: 'TC-BE-3 two alt recipients → two liabilities; invoice regular billed net only',
      expectedResult: `exVat=${expectedExVat}; A=${expectedA}; B=${expectedB}; invoice customer has no A/B liability`,
      actualResult: `exVat=${invoice.totalAmountExcludingVat} A=${detA.initialAmount} B=${detB.initialAmount} invoiceCustHas30or20=${invoiceCustHasAltAmounts}`,
      passed:
        amountsClose(invoice.totalAmountExcludingVat, expectedExVat) &&
        amountsClose(detA.initialAmount, expectedA) &&
        amountsClose(detB.initialAmount, expectedB) &&
        !invoiceCustHasAltAmounts,
    });
    expect(String(invoice.invoiceStatus)).toBe('REAL');
    expect(amountsClose(invoice.totalAmountExcludingVat, expectedExVat)).toBe(true);
    expect(amountsClose(detA.initialAmount, expectedA) && amountsClose(detA.currentAmount, expectedA)).toBe(true);
    expect(String(detA.creationType), 'recipient A creationType').toBe('AUTOMATIC');
    expect(amountsClose(detB.initialAmount, expectedB) && amountsClose(detB.currentAmount, expectedB)).toBe(true);
    expect(String(detB.creationType), 'recipient B creationType').toBe('AUTOMATIC');
    expect(invoiceCustHasAltAmounts).toBe(false);

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: REG_963_KEY,
        relevantEntityKeys: ['customer', 'productContract', 'billingRun', 'invoice', 'customerLiability'],
        snapshot: { invoiceId },
      });
    });
  });

  test(`[${REG_963_KEY}]: TC-BE-4 – Zero-amount VAT base price component does not create an alternative-recipient liability`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(BILLING_TIMEOUT_MS);
    const fx = asFx({ Request, GeneratePayload, Responses, Endpoints });

    const chain = await test.step('Precondition: regular 100 + VAT base expression 0', async () =>
      createVolumesVatBaseChain(fx, {
        regularExpression: 100,
        vatBaseComponents: [{ expression: 0, altCustomerIndex: 1 }],
      }),
    );
    registerChainPayloads(TestRunSummary, chain);
    const altIdent = customerIdentifier(chain.altCustomers[0]);
    const baseline = await listCustomerLiabilities(fx, altIdent);
    const baselineCount = baseline.length;

    const { invoiceId } = await test.step('STANDARD_BILLING → COMPLETED (not rejected)', async () =>
      runBillingThroughStatus(fx, chain.billingRunId, 'COMPLETED'),
    );
    const invoice = await getInvoice(fx, invoiceId);
    const detailed = await getInvoiceDetailedData(fx, invoiceId);
    const expectedExVat = billedValueForUnitPrice(detailed, 100);
    expect(expectedExVat).toBeGreaterThan(0);
    const after = await listCustomerLiabilities(fx, altIdent);
    const newForInvoice = await liabilitiesForInvoice(fx, after, invoiceId);
    const listing = await listInvoicesByBillingRun(fx, chain.billingRunId);
    const listed = listing.some((row) => Number(row.id) === invoiceId);

    TestRunSummary.recordCheck({
      check: 'TC-BE-4 zero VAT base amount skips alt liability',
      expectedResult: `COMPLETED REAL invoice exVat=${expectedExVat}; no new alt row for this invoiceId`,
      actualResult: `status=${invoice.invoiceStatus} exVat=${invoice.totalAmountExcludingVat} newAltRows=${newForInvoice.length} baseline=${baselineCount} listed=${listed}`,
      passed:
        String(invoice.invoiceStatus) === 'REAL' &&
        amountsClose(invoice.totalAmountExcludingVat, expectedExVat) &&
        newForInvoice.length === 0 &&
        listed,
    });
    expect(String(invoice.invoiceStatus)).toBe('REAL');
    expect(amountsClose(invoice.totalAmountExcludingVat, expectedExVat)).toBe(true);
    expect(newForInvoice.length, 'no alt liability for zero-amount VAT base').toBe(0);
    expect(listed, 'invoice present on POST /invoice/listing').toBe(true);
    const invoiceCustRows = await listCustomerLiabilities(
      fx,
      customerIdentifier(chain.invoiceCustomer),
    );
    const mainForInvoice = await liabilitiesForInvoice(fx, invoiceCustRows, invoiceId);
    expect(mainForInvoice.length, 'invoice customer has a main liability for this invoice').toBeGreaterThanOrEqual(
      1,
    );

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: REG_963_KEY,
        relevantEntityKeys: ['customer', 'productContract', 'billingRun', 'invoice'],
        snapshot: { invoiceId, baselineCount },
      });
    });
  });

  test(`[${REG_963_KEY}]: TC-BE-5 – Price component with doNotIncludeVatBase true is rejected when customerDetailId is null`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(NEGATIVE_TIMEOUT_MS);
    const fx = asFx({ Request, GeneratePayload, Responses, Endpoints });

    await test.step('Precondition: alt customer exists but is omitted from PC payload', async () => {
      await createLegalCustomer(fx);
    });

    const payload = GeneratePayload.productAndServices.priceSettlement();
    payload.formulaRequest.expression = 50;
    payload.globalVatRate = true;
    payload.vatRateId = null;
    payload.doNotIncludeVatBase = true;
    payload.customerDetailId = null;
    TestRunSummary.registerPayload('priceComponent', payload);

    const { status, bodyText } = await test.step('POST /price-components with null customerDetailId', async () =>
      postPriceComponentExpecting400(fx, payload),
    );
    const passed = status === 400 && bodyText.includes(MSG_VAT_BASE_REQUIRES_ALT_RECIPIENT);
    TestRunSummary.recordCheck({
      check: 'TC-BE-5 HTTP 400 when VAT base checkbox is on without alternative recipient',
      expectedResult: `HTTP 400 containing "${MSG_VAT_BASE_REQUIRES_ALT_RECIPIENT}"`,
      actualResult: `HTTP ${status} body=${bodyText.slice(0, 400)}`,
      passed,
    });
    expect(status, 'POST /price-components must be 400').toBe(400);
    expect(bodyText).toContain(MSG_VAT_BASE_REQUIRES_ALT_RECIPIENT);

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: REG_963_KEY,
        relevantEntityKeys: ['customer'],
        snapshot: { status },
      });
    });
  });

  test(`[${REG_963_KEY}]: TC-BE-6 – Generated invoice document JSON lists excluded VAT base price components`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(BILLING_TIMEOUT_MS);
    const fx = asFx({ Request, GeneratePayload, Responses, Endpoints });

    const chain = await test.step('Precondition: regular 100 + named VATBASE50-NET', async () =>
      createVolumesVatBaseChain(fx, {
        regularExpression: 100,
        vatBaseComponents: [
          {
            expression: 50,
            altCustomerIndex: 1,
            name: 'VATBASE50-NET',
            displayName: 'VATBASE50-NET',
          },
        ],
      }),
    );
    registerChainPayloads(TestRunSummary, chain);

    const { invoiceId } = await test.step('start-billing → GENERATED (before accounting)', async () =>
      runBillingThroughStatus(fx, chain.billingRunId, 'GENERATED'),
    );
    const invoice = await getInvoice(fx, invoiceId);
    await startAccountingToCompleted(fx, chain.billingRunId);
    const doc = await test.step('GET /billing-run/generate-invoice-data?invoiceId=', async () =>
      fetchInvoiceDocumentModel(fx, invoiceId),
    );

    const sd = excludedPcRows(doc);
    const table = tableExcludedPcRows(doc);
    const sdOk = excludedRowMatches(sd, 50, 'VATBASE50-NET');
    const tableOk = excludedRowMatches(table, 50, 'VATBASE50-NET');
    const totalIncl = money(doc.TotalInclVat ?? doc.totalInclVat);
    const invoiceIncl = money(invoice.totalAmountIncludingVat);
    const totalsMatch = amountsClose(totalIncl, invoiceIncl);
    const compensations = invoice.compensations as unknown[] | undefined;
    const compensationsEmpty = !compensations || compensations.length === 0;
    const finalLiability = money(doc.FinalLiabilityAmount ?? doc.finalLiabilityAmount);
    const finalOk = !compensationsEmpty || amountsClose(finalLiability, totalIncl);

    TestRunSummary.recordCheck({
      check: 'TC-BE-6 SDExcludedPC and DD.TableExcludedPC contain VATBASE50-NET = 50',
      expectedResult: 'Both arrays length>=1 with Value=50 and PC containing VATBASE50-NET',
      actualResult: `SD=${JSON.stringify(sd).slice(0, 300)} Table=${JSON.stringify(table).slice(0, 300)}`,
      passed: sdOk && tableOk,
    });
    TestRunSummary.recordCheck({
      check: 'TC-BE-6 FinalLiabilityAmount equals TotalInclVat when compensations empty (spec)',
      expectedResult: 'FinalLiabilityAmount = TotalInclVat (do not subtract 50 again)',
      actualResult: `TotalInclVat=${totalIncl} invoiceIncl=${invoiceIncl} FinalLiabilityAmount=${finalLiability} compensationsEmpty=${compensationsEmpty}`,
      passed: totalsMatch && finalOk,
    });
    expect(sd.length, 'SDExcludedPC length').toBeGreaterThanOrEqual(1);
    expect(table.length, 'TableExcludedPC length').toBeGreaterThanOrEqual(1);
    expect(sdOk && tableOk).toBe(true);
    expect(totalsMatch).toBe(true);
    expect(finalOk, 'spec: FinalLiabilityAmount must not subtract 50 again').toBe(true);

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: REG_963_KEY,
        relevantEntityKeys: ['customer', 'productContract', 'billingRun', 'invoice'],
        snapshot: { invoiceId, totalIncl, finalLiability },
      });
    });
  });

  test(`[${REG_963_KEY}]: TC-BE-7 – Invoice correction credit note reverses the alternative-recipient VAT base liability`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(BILLING_TIMEOUT_MS);
    const fx = asFx({ Request, GeneratePayload, Responses, Endpoints });

    const chain = await test.step('Precondition: REAL standard invoice with alt liability 50', async () =>
      createVolumesVatBaseChain(fx, {
        regularExpression: 100,
        vatBaseComponents: [{ expression: 50, altCustomerIndex: 1 }],
      }),
    );
    registerChainPayloads(TestRunSummary, chain);
    const { invoiceId } = await runBillingThroughStatus(fx, chain.billingRunId, 'COMPLETED');
    const originalInvoice = await getInvoice(fx, invoiceId);
    expect(String(originalInvoice.invoiceStatus)).toBe('REAL');
    const detailed = await getInvoiceDetailedData(fx, invoiceId);
    const expectedAlt = billedValueForUnitPrice(detailed, 50);
    expect(expectedAlt).toBeGreaterThan(0);
    const altIdent = customerIdentifier(chain.altCustomers[0]);
    const altRows = await liabilitiesForInvoice(fx, await listCustomerLiabilities(fx, altIdent), invoiceId);
    expect(altRows.length).toBe(1);
    const altLiabilityId = Number(altRows[0].id);
    expect(amountsClose(altRows[0].currentAmount, expectedAlt)).toBe(true);

    await test.step('Precondition: corrected profile volumes 1000 → 0', async () => {
      await createProfileData(fx, {
        podIndex: 0,
        value: 0,
        periodFrom: String(chain.profilePayload.periodFrom),
        periodTo: String(chain.profilePayload.periodTo),
      });
    });

    const correctionRaw = await test.step('POST INVOICE_CORRECTION volumeChange=true', async () => {
      const payload = await GeneratePayload.billing.correctionBilling(0, false, true);
      payload.invoiceCorrectionParameters.priceChange = false;
      payload.invoiceCorrectionParameters.volumeChange = true;
      const res = await Request.post(Endpoints.billingRun, { data: payload });
      await expect(res).CheckResponse();
      const raw = await res.json();
      Responses.billingRun.push(raw);
      return raw;
    });
    const correctionBillingRunId = asBillingRunId(correctionRaw);

    const { invoiceId: cnInvoiceId } = await test.step(
      'Correction start-billing → COMPLETED; pick CREDIT_NOTE',
      async () =>
        runBillingThroughStatus(fx, correctionBillingRunId, 'COMPLETED', (drafts) =>
          pickDraftByDocumentType(drafts, 'CREDIT_NOTE'),
        ),
    );
    const cn = await getInvoice(fx, cnInvoiceId);
    const altAfter = await getCustomerLiability(fx, altLiabilityId);
    const offsets =
      (altAfter.customerLiabilityOffsettingReponseList as unknown[]) ??
      (altAfter.customerLiabilityOffsettingResponseList as unknown[]) ??
      [];

    const invoiceCustRows = await listCustomerLiabilities(
      fx,
      customerIdentifier(chain.invoiceCustomer),
    );
    const mainRows = await liabilitiesForInvoice(fx, invoiceCustRows, invoiceId);
    const main = mainRows[0];
    const mainReduced =
      main != null && money(main.currentAmount) < money(main.initialAmount);

    TestRunSummary.recordCheck({
      check: 'TC-BE-7 correction CREDIT_NOTE offsets alt liability to 0.00',
      expectedResult: 'CN REAL CREDIT_NOTE; alt currentAmount=0; offsetting list length>=1',
      actualResult: `cnStatus=${cn.invoiceStatus} cnType=${cn.invoiceDocumentType} altCurrent=${altAfter.currentAmount} offsets=${offsets.length} mainReduced=${mainReduced}`,
      passed:
        String(cn.invoiceStatus) === 'REAL' &&
        String(cn.invoiceDocumentType) === 'CREDIT_NOTE' &&
        amountsClose(altAfter.currentAmount, 0) &&
        offsets.length >= 1 &&
        mainReduced,
    });
    expect(String(cn.invoiceStatus)).toBe('REAL');
    expect(String(cn.invoiceDocumentType)).toBe('CREDIT_NOTE');
    expect(amountsClose(altAfter.currentAmount, 0)).toBe(true);
    expect(offsets.length).toBeGreaterThanOrEqual(1);
    expect(mainReduced).toBe(true);

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: REG_963_KEY,
        relevantEntityKeys: ['customer', 'productContract', 'billingRun', 'invoice', 'customerLiability'],
        snapshot: { invoiceId, cnInvoiceId, altLiabilityId },
      });
    });
  });

  test(`[${REG_963_KEY}]: TC-BE-8 – Invoice reversal billing run reverses the alternative-recipient VAT base liability`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(BILLING_TIMEOUT_MS);
    const fx = asFx({ Request, GeneratePayload, Responses, Endpoints });

    const chain = await test.step('Precondition: REAL standard invoice with alt liability 50', async () =>
      createVolumesVatBaseChain(fx, {
        regularExpression: 100,
        vatBaseComponents: [{ expression: 50, altCustomerIndex: 1 }],
      }),
    );
    registerChainPayloads(TestRunSummary, chain);
    const { invoiceId } = await runBillingThroughStatus(fx, chain.billingRunId, 'COMPLETED');
    const originalInvoice = await getInvoice(fx, invoiceId);
    expect(String(originalInvoice.invoiceStatus)).toBe('REAL');
    const detailed = await getInvoiceDetailedData(fx, invoiceId);
    const expectedAlt = billedValueForUnitPrice(detailed, 50);
    expect(expectedAlt).toBeGreaterThan(0);
    const altIdent = customerIdentifier(chain.altCustomers[0]);
    const altRows = await liabilitiesForInvoice(fx, await listCustomerLiabilities(fx, altIdent), invoiceId);
    expect(altRows.length, 'original alt liability for this invoice').toBe(1);
    const altLiabilityId = Number(altRows[0].id);
    expect(amountsClose(altRows[0].currentAmount, expectedAlt), 'original alt currentAmount before reversal').toBe(
      true,
    );

    const reversalRaw = await test.step('POST INVOICE_REVERSAL for original invoiceNumber', async () => {
      const payload = await GeneratePayload.billing.reversalBilling(0);
      payload.invoiceReversalParameters.fileId = null;
      const res = await Request.post(Endpoints.billingRun, { data: payload });
      await expect(res).CheckResponse();
      const raw = await res.json();
      Responses.billingRun.push(raw);
      return raw;
    });
    const reversalBillingRunId = asBillingRunId(reversalRaw);

    const { invoiceId: reversalInvoiceId } = await test.step(
      'Reversal start-billing → COMPLETED; pick REVERSAL',
      async () =>
        runBillingThroughStatus(fx, reversalBillingRunId, 'COMPLETED', (drafts) =>
          pickDraftByDocumentType(drafts, 'REVERSAL'),
        ),
    );
    const reversalInvoice = await getInvoice(fx, reversalInvoiceId);
    const originalAlt = await getCustomerLiability(fx, altLiabilityId);
    const altAfterList = await listCustomerLiabilities(fx, altIdent);
    const reversalLinked = await liabilitiesForInvoice(fx, altAfterList, reversalInvoiceId);
    expect(reversalLinked.length, 'alt liability tied to reversal invoice').toBeGreaterThanOrEqual(1);
    const reversalLiability = reversalLinked[0];
    const listing = await listInvoicesByBillingRun(fx, chain.billingRunId);
    const originalStillListed = listing.some((row) => Number(row.id) === invoiceId);

    TestRunSummary.recordCheck({
      check: 'TC-BE-8 reversal zeros original alt liability and creates 50.00 reversal-linked row',
      expectedResult: 'REVERSAL REAL; original currentAmount=0; new AUTOMATIC 50 on reversal invoice',
      actualResult: `type=${reversalInvoice.invoiceType} origCurrent=${originalAlt.currentAmount} newInitial=${reversalLiability.initialAmount} originalListed=${originalStillListed}`,
      passed:
        String(reversalInvoice.invoiceType) === 'REVERSAL' &&
        String(reversalInvoice.invoiceStatus) === 'REAL' &&
        amountsClose(originalAlt.currentAmount, 0) &&
        amountsClose(reversalLiability.initialAmount, expectedAlt) &&
        String(reversalLinked[0].creationType) === 'AUTOMATIC' &&
        originalStillListed,
    });
    expect(String(reversalInvoice.invoiceType)).toBe('REVERSAL');
    expect(String(reversalInvoice.invoiceStatus)).toBe('REAL');
    expect(amountsClose(originalAlt.currentAmount, 0)).toBe(true);
    expect(amountsClose(reversalLiability.initialAmount, expectedAlt)).toBe(true);
    expect(originalStillListed).toBe(true);

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: REG_963_KEY,
        relevantEntityKeys: ['customer', 'productContract', 'billingRun', 'invoice', 'customerLiability'],
        snapshot: { invoiceId, reversalInvoiceId, altLiabilityId },
      });
    });
  });

  test(`[${REG_963_KEY}]: TC-BE-9 – Service contract billing run does not apply VAT base split (wiki skip)`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(BILLING_TIMEOUT_MS);
    const fx = asFx({ Request, GeneratePayload, Responses, Endpoints });

    const chain = await test.step('Precondition: service contract + periodical VAT base PC 50', async () =>
      createPeriodicalVatBaseServiceChain(fx),
    );
    registerChainPayloads(TestRunSummary, chain);
    const altIdent = customerIdentifier(chain.altCustomer);
    const baseline = await listCustomerLiabilities(fx, altIdent);

    const { invoiceId } = await test.step('Service STANDARD_BILLING → COMPLETED (not HTTP 400)', async () =>
      runBillingThroughStatus(fx, chain.billingRunId, 'COMPLETED'),
    );
    const invoice = await getInvoice(fx, invoiceId);
    const after = await listCustomerLiabilities(fx, altIdent);
    const altForInvoice = await liabilitiesForInvoice(fx, after, invoiceId);
    const listing = await listInvoicesByBillingRun(fx, chain.billingRunId);
    const onServiceCustomer = listing.some((row) => Number(row.id) === invoiceId);
    const secondInvoiceToAlt = listing.some((row) => {
      const customer = row.customerResponse as { identifier?: string } | undefined;
      return customer?.identifier === altIdent && Number(row.id) !== invoiceId;
    });

    TestRunSummary.recordCheck({
      check: 'TC-BE-9 service contract skip: 50 included in invoice VAT base; no alt liability (spec)',
      expectedResult: 'REAL exVat=50.00; 0 alt liabilities; listing has this invoice on the service customer only',
      actualResult: `status=${invoice.invoiceStatus} exVat=${invoice.totalAmountExcludingVat} altRows=${altForInvoice.length} listed=${onServiceCustomer} secondToAlt=${secondInvoiceToAlt} baseline=${baseline.length}`,
      passed:
        String(invoice.invoiceStatus) === 'REAL' &&
        amountsClose(invoice.totalAmountExcludingVat, 50) &&
        altForInvoice.length === 0 &&
        onServiceCustomer &&
        !secondInvoiceToAlt,
    });
    expect(String(invoice.invoiceStatus)).toBe('REAL');
    expect(amountsClose(invoice.totalAmountExcludingVat, 50)).toBe(true);
    expect(altForInvoice.length, 'spec: no alt liability on service-contract VAT base').toBe(0);
    expect(secondInvoiceToAlt, 'no second invoice issued to the alternative recipient').toBe(false);

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: REG_963_KEY,
        relevantEntityKeys: ['customer', 'service', 'serviceContract', 'billingRun', 'invoice'],
        snapshot: { invoiceId, billingRunId: chain.billingRunId },
      });
    });
  });

  test(`[${REG_963_KEY}]: TC-BE-10 – Price component with customerDetailId set is rejected when doNotIncludeVatBase is false`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(NEGATIVE_TIMEOUT_MS);
    const fx = asFx({ Request, GeneratePayload, Responses, Endpoints });

    const customer = await test.step('Precondition: LEGAL customer for lastCustomerDetailId', async () =>
      createLegalCustomer(fx),
    );
    const payload = GeneratePayload.productAndServices.priceSettlement();
    payload.formulaRequest.expression = 50;
    payload.globalVatRate = true;
    payload.doNotIncludeVatBase = false;
    payload.customerDetailId = Number(customer.lastCustomerDetailId);
    TestRunSummary.registerPayload('priceComponent', payload);

    const { status, bodyText } = await test.step(
      'POST /price-components with alt recipient and checkbox off',
      async () => postPriceComponentExpecting400(fx, payload),
    );
    const passed = status === 400 && bodyText.includes(MSG_ALT_RECIPIENT_REQUIRES_VAT_BASE);
    TestRunSummary.recordCheck({
      check: 'TC-BE-10 HTTP 400 when alternative recipient is set without VAT base checkbox',
      expectedResult: `HTTP 400 containing "${MSG_ALT_RECIPIENT_REQUIRES_VAT_BASE}"`,
      actualResult: `HTTP ${status} body=${bodyText.slice(0, 400)}`,
      passed,
    });
    expect(status).toBe(400);
    expect(bodyText).toContain(MSG_ALT_RECIPIENT_REQUIRES_VAT_BASE);

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: REG_963_KEY,
        relevantEntityKeys: ['customer'],
        snapshot: { status },
      });
    });
  });
});
