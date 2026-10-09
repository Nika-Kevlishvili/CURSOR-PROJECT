/**
 * Helpers for REG-1297 — standard invoice deduction by manual interim billing run (Dev).
 * Linked to REG-562.
 *
 * Differs from REG-718 (product IAP nomenclature + INTERIM_AND_ADVANCE_PAYMENT run):
 * this path uses MANUAL_INTERIM_AND_ADVANCE_PAYMENT with amountExcludingVat on the run.
 *
 * Reference:
 * - src/tests/billing/forVolumes/deduction.spec.ts (summary INTERIM row asserts)
 * - src/tests/billing/dataPreparation/forVolumesDataPrep.ts (REG_718 chain)
 * - src/backend/jsons/payloads/billing/manualoInterim.ts
 * - Phoenix BillingRunStandardInvoiceGenerationProcessor (INTERIM_DEDUCTION)
 *
 * Swagger (dev, refreshed this session): BillingRunCreateRequest.billingType includes
 * MANUAL_INTERIM_AND_ADVANCE_PAYMENT; InterimAndAdvancePaymentParameters required
 * amountExcludingVat, deductionFrom, issuingForTheMonthToCurrent, issuedSeparateInvoices,
 * customerDetailId, currencyId, basisForIssuing. Enums: FIRST_INVOICE_FOR_SAME_PERIOD,
 * ZERO / MINUS_ONE, INVOICE_ONE.
 *
 * Doc gap (Senior QA): Confluence Create/Calculate manual interim describe issuing +
 * "deduction from" but not that a later STANDARD invoice gets INTERIM_DEDUCTION rows.
 * Test asserts runtime (code) behavior.
 */
import { test, expect, type baseFixture } from '../../../backend/fixtures/baseFixture';
import { envVariables } from '../../../backend/fixtures/envCashed';

export const JIRA_KEY = 'REG-1297';
export const JIRA_TITLE = 'Manual interim billing run - standard invoice deduction (from same period)';
export const BILLING_TIMEOUT_MS = 20 * 60 * 1000;
/** Manual interim amount excl. VAT — must clear 5 EUR incl. VAT gate (wiki page 585728606). */
export const MANUAL_INTERIM_AMOUNT_EXCL_VAT = 100;

export type ManualInterimDeductionFx = Pick<
  baseFixture,
  'Request' | 'GeneratePayload' | 'Responses' | 'Endpoints' | 'FileUploadRequest'
>;

type InvoicePreview = {
  invoiceType?: string;
  invoiceStatus?: string;
  invoiceDocumentType?: string;
  totalAmountExcludingVat?: string | number;
  issuingForTheMonth?: string | null;
  basisForIssuing?: string;
};

type InvoiceSummaryRow = {
  priceComponent?: string;
  value?: string | number | null;
};

function amount(value: string | number | null | undefined): number {
  return Number(value);
}

function isInterimSummaryRow(row: InvoiceSummaryRow): boolean {
  return String(row.priceComponent ?? '').startsWith('INTERIM');
}

function asEntityId(raw: unknown): number {
  if (typeof raw === 'number' && Number.isFinite(raw)) return raw;
  if (typeof raw === 'object' && raw !== null && 'id' in raw) {
    const id = (raw as { id: unknown }).id;
    if (typeof id === 'number' && Number.isFinite(id)) return id;
    if (typeof id === 'string' && id.trim() !== '') return Number(id);
  }
  if (typeof raw === 'string' && raw.trim() !== '') return Number(raw);
  throw new Error(`Cannot resolve entity id from: ${JSON.stringify(raw)}`);
}

/**
 * Customer → settlement PC (expression=1) → term → POD → product (no IAP) →
 * product contract → activate → BBP current month (CET).
 * Current-month BBP pairs with issuingForTheMonthToCurrent=ZERO on the manual interim.
 */
export async function runManualInterimDeductionPrechain(
  fx: ManualInterimDeductionFx,
): Promise<{
  customerId: number;
  contractId: number;
  billingGroupId: number;
  customerDetailId: number;
  communicationDataId: number;
  bbpPeriodFrom: string;
  bbpPeriodTo: string;
}> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;

  await test.step('Precondition: legal customer', async () => {
    const customer = await Request.post(Endpoints.customer, {
      data: GeneratePayload.customers.customer_legal(),
    });
    await expect(customer).CheckResponse();
    Responses.customer.push(await customer.json());
  });

  await test.step('Precondition: settlement price component (expression=1)', async () => {
    const payload = GeneratePayload.productAndServices.priceSettlement();
    payload.formulaRequest.expression = 1;
    const price = await Request.post(Endpoints.priceComponent, { data: payload });
    await expect(price).CheckResponse();
    Responses.priceComponent.push(await price.json());
  });

  await test.step('Precondition: terms', async () => {
    const term = await Request.post(Endpoints.terms, {
      data: GeneratePayload.productAndServices.term(),
    });
    await expect(term).CheckResponse();
    Responses.terms.push(await term.json());
  });

  await test.step('Precondition: settlement POD', async () => {
    const pod = await Request.post(Endpoints.pod, {
      data: GeneratePayload.pointsOfDelivery.pod_settlement(),
    });
    await expect(pod).CheckResponse();
    Responses.pod.push(await pod.json());
  });

  await test.step('Precondition: product (no product IAP)', async () => {
    const product = await Request.post(Endpoints.product, {
      data: GeneratePayload.productAndServices.product(),
    });
    await expect(product).CheckResponse();
    Responses.product.push(await product.json());
  });

  await test.step('Precondition: product contract', async () => {
    const contract = await Request.post(Endpoints.productContract, {
      data: await GeneratePayload.contractsAndOrders.product_contract(),
    });
    await expect(contract).CheckResponse();
    Responses.productContract.push(await contract.json());
  });

  await test.step('Precondition: activate POD', async () => {
    const activation = await Request.post('/contract-pods/manual', {
      data: await GeneratePayload.pointsOfDelivery.pod_activation(),
    });
    await expect(activation).CheckResponse();
  });

  let bbpPeriodFrom = '';
  let bbpPeriodTo = '';
  await test.step('Precondition: billing-by-profile (current month, CET)', async () => {
    const payload = await GeneratePayload.energyData.profile1Month();
    payload.timeZone = 'CET';
    const profiles = await Request.post('billing-by-profile', { data: payload });
    await expect(profiles).CheckResponse();
    const profileData = await profiles.json();
    bbpPeriodFrom = String(payload.periodFrom);
    bbpPeriodTo = String(payload.periodTo);
    Responses.dataByProfiles.push({
      id: profileData,
      periodFrom: payload.periodFrom,
      periodTo: payload.periodTo,
      periodType: payload.periodType,
    });
  });

  const customerGet = await Request.get(
    `customer/${Responses.customer[0].id}?version=1`,
  );
  await expect(customerGet).CheckResponse();
  const customerJson = await customerGet.json();
  const customerDetailId = Number(customerJson.customerDetailsId);
  const communicationDataId = Number(customerJson.communicationData?.[0]?.id);
  expect(customerDetailId, 'customerDetailsId required').toBeGreaterThan(0);
  expect(communicationDataId, 'invoice communication data id required').toBeGreaterThan(0);

  const contractId = asEntityId(Responses.productContract[0]);
  const contractGet = await Request.get(`product-contract/${contractId}?version=1`);
  await expect(contractGet).CheckResponse();
  const contractJson = await contractGet.json();
  const billingGroupId = Number(
    contractJson.contractPodsResponses?.[0]?.billingGroupId ??
      contractJson.billingGroups?.[0]?.id,
  );
  expect(billingGroupId, 'product contract billing group id required').toBeGreaterThan(0);

  return {
    customerId: asEntityId(Responses.customer[0]),
    contractId,
    billingGroupId,
    customerDetailId,
    communicationDataId,
    bbpPeriodFrom,
    bbpPeriodTo,
  };
}

/** Build MANUAL_INTERIM_AND_ADVANCE_PAYMENT create payload for the given contract. */
export async function buildManualInterimBillingPayload(
  fx: ManualInterimDeductionFx,
  args: {
    customerDetailId: number;
    communicationDataId: number;
    contractId: number;
    billingGroupId: number;
    amountExcludingVat?: number;
    issuingForTheMonthToCurrent?: 'ZERO' | 'MINUS_ONE';
  },
): Promise<Record<string, unknown>> {
  const payload = fx.GeneratePayload.billing.Manualinterim() as Record<string, unknown>;
  const iap = {
    ...(payload.interimAndAdvancePaymentParameters as Record<string, unknown>),
  };

  iap.amountExcludingVat = args.amountExcludingVat ?? MANUAL_INTERIM_AMOUNT_EXCL_VAT;
  iap.deductionFrom = 'FIRST_INVOICE_FOR_SAME_PERIOD';
  // BBP uses current month (profile1Month monthOffset=0) → ZERO matches period month.
  iap.issuingForTheMonthToCurrent = args.issuingForTheMonthToCurrent ?? 'ZERO';
  iap.issuedSeparateInvoices = ['INVOICE_ONE'];
  iap.customerDetailId = args.customerDetailId;
  iap.invoiceCommunicationDataId = args.communicationDataId;
  iap.contractType = 'PRODUCT_CONTRACT';
  iap.contractId = args.contractId;
  iap.billingGroupIds = [args.billingGroupId];
  iap.currencyId = envVariables.currency;
  iap.prefixType = null;
  iap.applicableInterestRateId = envVariables.interest_rate;
  iap.applicableInterestRateManual = true;
  iap.numberOfIncomeAccountManual = true;
  iap.costCenterControllingOrderManual = true;
  iap.vatRateManual = true;
  iap.globalVatRate = true;
  iap.vatRateId = null;
  iap.basisForIssuing = 'Manual interim for standard deduction test';

  payload.interimAndAdvancePaymentParameters = iap;
  payload.billingType = 'MANUAL_INTERIM_AND_ADVANCE_PAYMENT';
  return payload;
}

export async function createAndCompleteManualInterim(
  fx: ManualInterimDeductionFx,
  args: {
    customerDetailId: number;
    communicationDataId: number;
    contractId: number;
    billingGroupId: number;
  },
): Promise<{ billingRunId: number; interimInvoiceId: number; interimExclVat: number }> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;

  const payload = await buildManualInterimBillingPayload(fx, args);
  const createRes = await Request.post(Endpoints.billingRun, { data: payload });
  await expect(createRes).CheckResponse();
  const billingRaw = await createRes.json();
  const billingRunId = asEntityId(billingRaw);
  Responses.billingRun.push(
    typeof billingRaw === 'object' && billingRaw !== null ? billingRaw : { id: billingRunId },
  );

  await GeneratePayload.billing.waitForInvoiceGeneration(true, true, 1, 0);
  expect(Responses.invoice.length, 'manual interim must produce ≥1 invoice').toBeGreaterThanOrEqual(
    1,
  );

  let interimInvoiceId = 0;
  let interimExclVat = NaN;
  for (const id of Responses.invoice) {
    const invRes = await Request.get(`${Endpoints.invoice}?id=${id}`);
    await expect(invRes).CheckResponse();
    const inv = (await invRes.json()) as InvoicePreview;
    if (inv.invoiceType === 'INTERIM_AND_ADVANCE_PAYMENT') {
      interimInvoiceId = asEntityId(id);
      interimExclVat = amount(inv.totalAmountExcludingVat);
      expect(inv.invoiceStatus, 'manual interim must be REAL before standard deduction').toBe(
        'REAL',
      );
      expect(interimExclVat, 'manual interim excl. VAT').toBe(MANUAL_INTERIM_AMOUNT_EXCL_VAT);
      break;
    }
  }
  expect(interimInvoiceId, 'no REAL INTERIM_AND_ADVANCE_PAYMENT invoice found').toBeGreaterThan(0);

  return { billingRunId, interimInvoiceId, interimExclVat };
}

export async function createAndCompleteForVolumesOnSameContract(
  fx: ManualInterimDeductionFx,
): Promise<{ billingRunId: number; standardInvoiceId: number }> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;

  const volumesPayload = await GeneratePayload.billing.billingRun('CONTRACT', ['FOR_VOLUMES']);
  const createRes = await Request.post(Endpoints.billingRun, { data: volumesPayload });
  await expect(createRes).CheckResponse();
  const billingRaw = await createRes.json();
  const billingRunId = asEntityId(billingRaw);
  Responses.billingRun.push(
    typeof billingRaw === 'object' && billingRaw !== null ? billingRaw : { id: billingRunId },
  );

  const volumesBillingIndex = Responses.billingRun.length - 1;
  await GeneratePayload.billing.waitForInvoiceGeneration(true, true, 1, volumesBillingIndex);

  let standardInvoiceId = 0;
  for (const id of Responses.invoice) {
    const invRes = await Request.get(`${Endpoints.invoice}?id=${id}`);
    await expect(invRes).CheckResponse();
    const inv = (await invRes.json()) as InvoicePreview;
    if (inv.invoiceType === 'STANDARD') {
      standardInvoiceId = asEntityId(id);
      break;
    }
  }
  expect(standardInvoiceId, 'no STANDARD volumes invoice found').toBeGreaterThan(0);
  return { billingRunId, standardInvoiceId };
}

/**
 * Assert STANDARD volumes invoice deducts the manual interim amount
 * (same summary-row pattern as deduction.spec.ts / REG-718).
 */
export async function assertStandardInvoiceDeductsManualInterim(
  Request: baseFixture['Request'],
  Endpoints: baseFixture['Endpoints'],
  args: {
    interimInvoiceId: number;
    standardInvoiceId: number;
    interimExclVat: number;
    bbpPeriodFrom: string;
    bbpPeriodTo: string;
  },
): Promise<void> {
  const interimRes = await Request.get(`${Endpoints.invoice}?id=${args.interimInvoiceId}`);
  await expect(interimRes).CheckResponse();
  const interimInv = (await interimRes.json()) as InvoicePreview;

  const standardRes = await Request.get(`${Endpoints.invoice}?id=${args.standardInvoiceId}`);
  await expect(standardRes).CheckResponse();
  const standardInv = (await standardRes.json()) as InvoicePreview;

  expect(interimInv.invoiceType).toBe('INTERIM_AND_ADVANCE_PAYMENT');
  expect(interimInv.invoiceStatus).toBe('REAL');
  expect(amount(interimInv.totalAmountExcludingVat)).toBe(args.interimExclVat);

  expect(standardInv.invoiceType).toBe('STANDARD');
  expect(standardInv.invoiceStatus).toBe('REAL');

  const summaryRes = await Request.get(
    `${Endpoints.invoice}/summary-data?id=${args.standardInvoiceId}&page=0&size=25`,
  );
  await expect(summaryRes).CheckResponse();
  const rows: InvoiceSummaryRow[] = (await summaryRes.json()).content ?? [];
  expect(rows.length, 'STANDARD invoice/summary-data returned no lines').toBeGreaterThanOrEqual(2);

  const deductionRow = rows.find(isInterimSummaryRow);
  if (!deductionRow) {
    throw new Error(
      `No INTERIM deduction row on STANDARD invoice ${args.standardInvoiceId}. ` +
        `Interim issuingForTheMonth=${interimInv.issuingForTheMonth}; ` +
        `BBP=${args.bbpPeriodFrom}..${args.bbpPeriodTo}; ` +
        `summary priceComponents=[${rows.map(r => r.priceComponent).join(', ')}]. ` +
        `Likely issuing-month / contract / slot mismatch (see getAllNotDeductedInterimSamePeriod).`,
    );
  }
  expect(amount(deductionRow.value)).toBe(-args.interimExclVat);

  const energyExclVat = rows
    .filter(row => !isInterimSummaryRow(row))
    .reduce((sum, row) => sum + amount(row.value), 0);
  expect(energyExclVat, 'volumes energy summary must be > interim amount').toBeGreaterThan(
    args.interimExclVat,
  );

  const netExclVat = energyExclVat - args.interimExclVat;
  expect(amount(standardInv.totalAmountExcludingVat)).toBeCloseTo(Math.abs(netExclVat), 2);
  // Processor sets CREDIT_NOTE if net < 0 after deduction, else DEBIT_NOTE.
  expect(standardInv.invoiceDocumentType).toBe(netExclVat < 0 ? 'CREDIT_NOTE' : 'DEBIT_NOTE');
  // Note: runtime may set interim.isDeducted after standard accounting; field is not in Dev
  // InvoiceResponse OpenAPI — primary proof is the INTERIM summary row above.
}
