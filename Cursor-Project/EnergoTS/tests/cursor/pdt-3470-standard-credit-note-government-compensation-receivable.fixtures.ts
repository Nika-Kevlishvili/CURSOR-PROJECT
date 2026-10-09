/**
 * PDT-3470 — STANDARD Credit Note from IAP-over-invoice must create the government
 * compensation customer receivable (and paired government liability).
 *
 * Scope: creation of compensation L/R after start-accounting. IAP leftover
 * auto-offset / currentAmount → 0 is NOT asserted (Finding F1).
 *
 * Volume data uses billing-by-profile (same env-agnostic path as EXP-PARITY-03 /
 * PDT-3087). Backend TC text mentions billing-by-scales; the observable outcome
 * is a FOR_VOLUMES STANDARD + CREDIT_NOTE whose incl-VAT total is less than IAP.
 *
 * Contract type is COMBINED (working FOR_VOLUMES path). TC text says SUPPLY_ONLY.
 *
 * Swagger (`Cursor-Project/config/swagger/test/swagger-spec.json`, Rule 41 this session):
 * - POST /government-compensations CompensationRequest
 * - GET /customer-receivable CustomerReceivableListingRequest.customerReceivableSearchBy
 * - GET /customer-liability/list CustomerLiabilityListingRequest.searchFields
 *
 * Reference spec(s):
 * - tests/cursor/EXP-PARITY-03-interim-deduction-and-issue-date.spec.ts
 * - tests/cursor/exp-parity-03-interim-deduction-and-issue-date.fixtures.ts
 * - tests/cursor/pdt-3087-government-compensation-defer-to-pdf.fixtures.ts
 * - tests/cursor/dev-volume-billing-two-compensations.fixtures.ts
 * - tests/cursor/shared/exp-parity-contract.fixtures.ts
 */

import { expect, test } from './cursor-test.fixtures';
import type { baseFixture } from './cursor-test.fixtures';
import { randomGens } from '../../utils/randomGens';
import {
  profileDateRangeFromAnchor,
  termPayloadWithMinimalPaymentDays,
} from './PDT-2529-rfd-data-model-happy-path.fixtures';
import {
  type Pdt2376BillingAnchor,
} from './pdt-2376-volume-with-electricity.fixtures';
import {
  applyExpParityCombinedProduct,
  postExpParitySignedProductContract,
} from './shared/exp-parity-contract.fixtures';
import {
  applyExpParityLocalAddress,
  postExpParityLegalCustomer,
  type ExpParityLocalAddressIds,
} from './shared/exp-parity-customer.fixtures';
import {
  buildExactAmountSamePeriodIap,
  createStandardBillingRun,
  getInvoiceRecord,
  startBillingAndObserveDraft,
  type ExpParity03Fx,
} from './exp-parity-03-interim-deduction-and-issue-date.fixtures';
import {
  COMP_ROOT,
  BILLING_RUN_ROOT,
  buildCompensationPayload,
  buildDevCompensationPreviewLink,
  buildDevInvoicePreviewLink,
  getCompensation,
  getInvoice,
  isNullishEntityRef,
  monthStartFromPeriod,
  startAccountingAndWaitCompleted,
  startGeneratingAndWaitGenerated,
  type InvoiceView,
} from './pdt-3087-government-compensation-defer-to-pdf.fixtures';
import { applyPdt2915FixedProfileVolume } from './pdt-2915-invoice-correction-deleted-bbp.fixtures';

export { getCompensation, isNullishEntityRef };

export const PDT_3470_KEY = 'PDT-3470';
export const PDT_3470_TITLE =
  'Government compensation - when the generated document is Credit note - receivable for customer and liability for government are not generated';

export const PDT_3470_TEST_TIMEOUT_MS = 55 * 60 * 1000;

export const PDT_3470_IAP_EXACT_AMOUNT = 200;
export const PDT_3470_VOLUME_PC_EXPRESSION = '10';

export const PDT_3470_ZERO_AMOUNT_MESSAGE = 'documentAmount-[documentAmount] must not be zero;';

const POD_MANUAL_ACTIVATION_ROOT = 'contract-pods/manual';
const BILLING_DATA_CRON_SETTLE_MS = 3 * 60 * 1000;

export type Pdt3470Fx = Pick<
  baseFixture,
  'Request' | 'GeneratePayload' | 'Responses' | 'Endpoints'
>;

export type Pdt3470ReceivableView = {
  id: number;
  creationType?: string;
  status?: string;
  initialAmount?: number | string;
  currentAmount?: number | string;
  customerResponse?: { id?: number } | null;
  invoiceResponse?: { id?: number } | null;
  [key: string]: unknown;
};

export type Pdt3470LiabilityView = {
  id: number;
  creationType?: string;
  status?: string;
  initialAmount?: number | string;
  currentAmount?: number | string;
  customerResponse?: { id?: number } | null;
  invoiceResponse?: { id?: number } | null;
  [key: string]: unknown;
};

export type Pdt3470IapReadyChain = {
  anchor: Pdt2376BillingAnchor;
  billedCustomerId: number;
  billedCustomerIdentifier: string;
  recipientId: number;
  recipientIdentifier: string;
  billedPodId: number;
  unrelatedPodId: number | null;
  contractId: number;
  iapBillingRunId: number;
  iapInvoiceId: number;
  documentPeriod: string;
};

export type Pdt3470VolumeCreditNote = {
  volumeBillingRunId: number;
  creditNoteInvoiceId: number;
  cnTotalInclVat: number;
};

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function isTransientNetworkError(err: unknown): boolean {
  const msg = err instanceof Error ? `${err.message}\n${err.stack ?? ''}` : String(err);
  return /ETIMEDOUT|ECONNRESET|ECONNREFUSED|socket hang up|Timeout|timed out|UND_ERR/i.test(msg);
}

async function withTransientRetry<T>(fn: () => Promise<T>, attempts = 5): Promise<T> {
  let last: unknown;
  for (let i = 1; i <= attempts; i++) {
    try {
      return await fn();
    } catch (err) {
      last = err;
      if (!isTransientNetworkError(err) || i === attempts) {
        throw err;
      }
      await sleep(1500 * i);
    }
  }
  throw last;
}

/**
 * Latest OPEN accounting period, not the PDT-2376 preferred 2025-12-31 window.
 * Dev2 keeps many periods OPEN. POD activation uses the billed month start so
 * the interim run and the contract overlap.
 */
async function resolvePdt3470OpenAnchor(
  Request: Pdt3470Fx['Request'],
): Promise<Pdt2376BillingAnchor> {
  const listRes = await withTransientRetry(() =>
    Request.get('billing-run/accounting-period-available-list?page=0&size=50'),
  );
  await expect(listRes).CheckResponse();
  const page = (await listRes.json()) as {
    content?: Array<{ id?: number; status?: string; endDate?: string; name?: string }>;
  };
  const open = (page.content ?? []).filter(
    (row) => String(row.status).toUpperCase() === 'OPEN' && row.id != null && row.endDate,
  );
  open.sort((a, b) => String(b.endDate).localeCompare(String(a.endDate)));
  const latest = open[0];
  if (!latest?.id || !latest.endDate) {
    throw new Error('PDT-3470: no OPEN accounting period available for the billing window');
  }
  const invoicePeriodTo = String(latest.endDate).slice(0, 10);
  const profileStart = `${invoicePeriodTo.slice(0, 7)}-01`;
  return {
    invoicePeriodTo,
    taxEventDate: invoicePeriodTo,
    invoiceDate: invoicePeriodTo,
    accountingPeriodId: Number(latest.id),
    profileStart,
    profileEnd: invoicePeriodTo,
    commentary: `Latest OPEN period ${latest.name ?? latest.id} (${profileStart}..${invoicePeriodTo}) so POD activation and the interim run share one month.`,
  };
}

export function asEntityId(raw: unknown): number {
  if (typeof raw === 'number' && Number.isFinite(raw) && raw > 0) {
    return raw;
  }
  if (raw && typeof raw === 'object' && 'id' in raw) {
    const n = Number((raw as { id: unknown }).id);
    if (Number.isFinite(n) && n > 0) {
      return n;
    }
  }
  const n = Number(raw);
  if (Number.isFinite(n) && n > 0) {
    return n;
  }
  throw new Error(`Expected numeric entity id, got: ${JSON.stringify(raw)}`);
}

export function nestedId(raw: unknown): number | null {
  if (typeof raw === 'number' && Number.isFinite(raw) && raw > 0) {
    return raw;
  }
  if (raw && typeof raw === 'object' && 'id' in raw) {
    const n = Number((raw as { id?: unknown }).id);
    if (Number.isFinite(n) && n > 0) {
      return n;
    }
  }
  return null;
}

export function money(raw: unknown): number {
  const n = Number(raw);
  if (!Number.isFinite(n)) {
    throw new Error(`Expected numeric amount, got: ${JSON.stringify(raw)}`);
  }
  return Number(n.toFixed(2));
}

export function uniqueCompensationNumber(tcTag: string): string {
  const suffix = randomGens.generateRandomString(true, true, 8);
  return `PDT3470-${tcTag}-${suffix}`.slice(0, 50);
}

function podSettlementPayloadUnique(
  GeneratePayload: Pdt3470Fx['GeneratePayload'],
  suffix: string,
  addressIds: ExpParityLocalAddressIds,
): Record<string, unknown> {
  const payload = GeneratePayload.pointsOfDelivery.pod_settlement() as Record<string, unknown>;
  const core = randomGens.generateUniqueIdentifier();
  payload.identifier = (`32X${core}${suffix}`).slice(0, 33);
  applyExpParityLocalAddress(payload, addressIds);
  return payload;
}

function buildPeriodicalIapExact(
  GeneratePayload: Pdt3470Fx['GeneratePayload'],
  amount: number,
): Record<string, unknown> {
  const payload = buildExactAmountSamePeriodIap(
    GeneratePayload as ExpParity03Fx['GeneratePayload'],
    amount,
  );
  payload.issuingForTheMonthToCurrent = 'ZERO';
  payload.noInterestInOverdueDebt = false;
  payload.paymentType = 'OBLIGATORY';
  payload.matchesWithTermOfStandardInvoice = false;
  return payload;
}

export function pdt3470RelevantKeys(): string[] {
  return [
    'customer',
    'pod',
    'product',
    'productContract',
    'billingRun',
    'invoice',
    'compensation',
    'customerReceivable',
    'customerLiability',
  ];
}

export function pdt3470PortalExtraLinks(opts: {
  compensationId?: number;
  creditNoteInvoiceId?: number;
}): Record<string, string[]> {
  const extra: Record<string, string[]> = {};
  if (opts.compensationId && opts.compensationId > 0) {
    extra.compensationPreview = [buildDevCompensationPreviewLink(opts.compensationId)];
  }
  if (opts.creditNoteInvoiceId && opts.creditNoteInvoiceId > 0) {
    extra.creditNotePreview = [buildDevInvoicePreviewLink(opts.creditNoteInvoiceId)];
  }
  return extra;
}

/**
 * Billed legal customer + government recipient + IAP 200 PERIODICAL + COMBINED
 * contract + billed POD profile + IAP billing run to REAL.
 * Optional second POD is created but not activated (TC-BE-3).
 */
export async function buildPdt3470IapRealChain(
  fx: Pdt3470Fx,
  opts: { includeUnrelatedPod?: boolean; iapExactAmount?: number; volumeKwh?: number; anchor?: Pdt2376BillingAnchor } = {},
): Promise<Pdt3470IapReadyChain> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const includeUnrelatedPod = opts.includeUnrelatedPod === true;
  const iapExactAmount = opts.iapExactAmount ?? PDT_3470_IAP_EXACT_AMOUNT;
  // Price expression is 10. Default profile volume billed ~317 excl. VAT, so after
  // deducting IAP 200 the net stayed positive (DEBIT_NOTE). 1 kWh keeps the
  // period invoice below the interim so the document is a CREDIT_NOTE.
  const volumeKwh = opts.volumeKwh ?? 1;

  const anchor = opts.anchor ?? (await resolvePdt3470OpenAnchor(Request));
  const documentPeriod = monthStartFromPeriod(anchor.profileStart);

  const billed = await test.step('Precondition: billed legal customer', async () =>
    postExpParityLegalCustomer(fx));

  const recipient = await test.step(
    'Precondition: government recipient customer (≠ billed)',
    async () => postExpParityLegalCustomer(fx, billed.addressIds),
  );
  expect(recipient.id, 'recipientId must differ from billed customerId').not.toBe(billed.id);

  await test.step('Precondition: terms', async () => {
    const res = await Request.post(Endpoints.terms, {
      data: termPayloadWithMinimalPaymentDays(GeneratePayload),
    });
    await expect(res).CheckResponse();
    Responses.terms.push(await res.json());
  });

  await test.step(
    `Precondition: volume price component expression ${PDT_3470_VOLUME_PC_EXPRESSION}`,
    async () => {
      const payload = GeneratePayload.productAndServices.priceSettlement();
      payload.formulaRequest.expression = PDT_3470_VOLUME_PC_EXPRESSION;
      const res = await Request.post(Endpoints.priceComponent, { data: payload });
      await expect(res).CheckResponse();
      Responses.priceComponent.push(await res.json());
    },
  );

  await test.step(
    `Precondition: IAP EXACT_AMOUNT ${iapExactAmount} PERIODICAL ZERO`,
    async () => {
      const res = await Request.post(Endpoints.interim, {
        data: buildPeriodicalIapExact(GeneratePayload, iapExactAmount),
      });
      await expect(res).CheckResponse();
      Responses.interim.push(await res.json());
    },
  );

  await test.step('Precondition: billed POD', async () => {
    const res = await Request.post(Endpoints.pod, {
      data: podSettlementPayloadUnique(GeneratePayload, 'B', billed.addressIds),
    });
    await expect(res).CheckResponse();
    Responses.pod.push(await res.json());
  });
  const billedPodId = asEntityId(Responses.pod[Responses.pod.length - 1]);

  let unrelatedPodId: number | null = null;
  if (includeUnrelatedPod) {
    await test.step('Precondition: unrelated POD (not on contract, not billed)', async () => {
      const res = await Request.post(Endpoints.pod, {
        data: podSettlementPayloadUnique(GeneratePayload, 'U', billed.addressIds),
      });
      await expect(res).CheckResponse();
      Responses.pod.push(await res.json());
    });
    unrelatedPodId = asEntityId(Responses.pod[Responses.pod.length - 1]);
  }

  await test.step('Precondition: product (COMBINED + IAP) + SIGNED contract', async () => {
    const productPayload = applyExpParityCombinedProduct(
      GeneratePayload.productAndServices.product(),
    ) as { paymentGuarantees?: string[] };
    productPayload.paymentGuarantees = ['NO'];
    const product = await Request.post(Endpoints.product, { data: productPayload });
    await expect(product).CheckResponse();
    Responses.product.push(await product.json());

    const contractPayload = await GeneratePayload.contractsAndOrders.product_contract();
    const contractBody = await postExpParitySignedProductContract(Request, contractPayload);
    Responses.productContract.push(contractBody);
  });
  const contractId = asEntityId(Responses.productContract[Responses.productContract.length - 1]);

  await test.step('Precondition: activate billed POD only', async () => {
    // Interim billing includes a POD only when the invoice date is between
    // activation and deactivation. Activate on the billed month start.
    const res = await Request.post(POD_MANUAL_ACTIVATION_ROOT, {
      data: await GeneratePayload.pointsOfDelivery.pod_activation(0, anchor.profileStart),
    });
    await expect(res).CheckResponse();
  });

  await test.step('Precondition: billing-by-profile for billed POD', async () => {
    if (typeof volumeKwh === 'number') {
      const startDate = `${anchor.profileStart.slice(0, 7)}-01`;
      const year = Number(startDate.slice(0, 4));
      const month = Number(startDate.slice(5, 7));
      const lastDay = new Date(Date.UTC(year, month, 0)).getUTCDate();
      const endDate = `${startDate.slice(0, 7)}-${String(lastDay).padStart(2, '0')}`;
      const payload = await GeneratePayload.energyData.profile1Month(0, [{ startDate, endDate }]);
      payload.timeZone = 'CET';
      applyPdt2915FixedProfileVolume(payload as Record<string, unknown>, volumeKwh);
      const profiles = await Request.post(Endpoints.profilesByMonth, { data: payload });
      await expect(profiles).CheckResponse();
      const profileData = await profiles.json();
      Responses.dataByProfiles.push({
        id: profileData,
        periodFrom: payload.periodFrom,
        periodTo: payload.periodTo,
        periodType: payload.periodType,
      });
      return;
    }
    const dateRanges = profileDateRangeFromAnchor(anchor);
    const payload = await GeneratePayload.energyData.profile1Month(0, dateRanges);
    payload.timeZone = 'CET';
    const profiles = await Request.post(Endpoints.profilesByMonth, { data: payload });
    await expect(profiles).CheckResponse();
    const profileData = await profiles.json();
    Responses.dataByProfiles.push({
      id: profileData,
      periodFrom: payload.periodFrom,
      periodTo: payload.periodTo,
      periodType: payload.periodType,
    });
  });

  await test.step(
    `Precondition: settle ${Math.round(BILLING_DATA_CRON_SETTLE_MS / 1000)}s for volumes billing-data cron`,
    async () => {
      await sleep(BILLING_DATA_CRON_SETTLE_MS);
    },
  );

  let iapBillingRunId = 0;
  await test.step('Precondition: INTERIM_AND_ADVANCE_PAYMENT billing run (no maxEndDate)', async () => {
    iapBillingRunId = await createStandardBillingRun(
      fx as ExpParity03Fx,
      ['INTERIM_AND_ADVANCE_PAYMENT'],
      anchor,
    );
    Responses.billingRun[Responses.billingRun.length - 1] = {
      id: iapBillingRunId,
      invoiceNumbers: 1,
    };
  });

  await test.step('Precondition: complete IAP billing to REAL', async () => {
    const startRes = await withTransientRetry(() =>
      Request.patch(`${BILLING_RUN_ROOT}/start-billing?billingRunId=${iapBillingRunId}`),
    );
    await expect(startRes).CheckResponse();

    const deadline = Date.now() + 20 * 60 * 1000;
    let draftIds: number[] = [];
    let lastStatus = '';
    while (Date.now() < deadline) {
      const statusRes = await withTransientRetry(() =>
        Request.get(`${BILLING_RUN_ROOT}/${iapBillingRunId}`),
      );
      await expect(statusRes).CheckResponse();
      lastStatus = String(
        ((await statusRes.json()) as { commonParameters?: { status?: string } }).commonParameters
          ?.status ?? '',
      );
      const draftRes = await withTransientRetry(() =>
        Request.get(`${BILLING_RUN_ROOT}/draft-invoices?id=${iapBillingRunId}&page=0&size=50`),
      );
      await expect(draftRes).CheckResponse();
      const page = (await draftRes.json()) as { content?: Array<{ id?: number }> };
      draftIds = (page.content ?? [])
        .map((row) => Number(row.id))
        .filter((id) => Number.isFinite(id) && id > 0);
      if (draftIds.length > 0) {
        break;
      }
      if (['DELETED', 'CANCELLED', 'IN_PROGRESS_TERMINATION'].includes(lastStatus)) {
        throw new Error(`IAP billing run ${iapBillingRunId} terminated with status ${lastStatus}`);
      }
      await sleep(15_000);
    }
    expect(
      draftIds.length,
      `IAP billing run ${iapBillingRunId} produced a draft (lastStatus=${lastStatus})`,
    ).toBeGreaterThan(0);
    for (const id of draftIds) {
      if (!Responses.invoice.includes(id as never)) {
        Responses.invoice.push(id);
      }
    }
    await startGeneratingAndWaitGenerated(Request, iapBillingRunId);
    await startAccountingAndWaitCompleted(Request, iapBillingRunId);
  });

  const iapInvoiceId = asEntityId(Responses.invoice[Responses.invoice.length - 1]);
  const iapBody = await getInvoiceRecord(Request, iapInvoiceId);
  expect(iapBody.invoiceStatus, 'IAP invoice must be REAL before volumes').toBe('REAL');
  expect(iapBody.invoiceType).toBe('INTERIM_AND_ADVANCE_PAYMENT');

  return {
    anchor,
    billedCustomerId: billed.id,
    billedCustomerIdentifier: billed.identifier,
    recipientId: recipient.id,
    recipientIdentifier: recipient.identifier,
    billedPodId,
    unrelatedPodId,
    contractId,
    iapBillingRunId,
    iapInvoiceId,
    documentPeriod,
  };
}

export async function createPdt3470Compensation(
  fx: Pdt3470Fx,
  opts: {
    customerId: number;
    podId: number;
    recipientId: number;
    documentPeriod: string;
    documentAmount: number;
    volumes: number;
    price: number;
    number: string;
    reason: string;
  },
): Promise<{ id: number; number: string }> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const payload = buildCompensationPayload(GeneratePayload, {
    customerId: opts.customerId,
    podId: opts.podId,
    recipientId: opts.recipientId,
    documentPeriod: opts.documentPeriod,
    documentAmount: opts.documentAmount,
    volumes: opts.volumes,
    price: opts.price,
    reason: opts.reason,
  });
  payload.number = opts.number;
  payload.date = opts.documentPeriod.slice(0, 10);
  const res = await Request.post(Endpoints.compensation, { data: payload });
  await expect(res).CheckResponse();
  const id = asEntityId(await res.json());
  Responses.compensation.push({ id, number: payload.number, ...payload });

  const view = await getCompensation(Request, id);
  expect(view.compensationStatus, 'new compensation is UNINVOICED').toBe('UNINVOICED');
  expect(isNullishEntityRef(view.invoice), 'new compensation has no invoice').toBe(true);
  expect(view.index == null, 'new compensation index is null').toBe(true);
  expect(isNullishEntityRef(view.receivableForCustomer), 'new compensation has no receivable').toBe(
    true,
  );
  expect(isNullishEntityRef(view.liabilityForRecipient), 'new compensation has no liability').toBe(
    true,
  );
  return { id, number: payload.number };
}

/**
 * FOR_VOLUMES run to DRAFT; fail if the document is not CREDIT_NOTE (IAP not greater
 * than the period invoice).
 */
export async function startPdt3470VolumeCreditNoteDraft(
  fx: Pdt3470Fx,
  chain: Pdt3470IapReadyChain,
  compensationAmount: number,
): Promise<Pdt3470VolumeCreditNote> {
  const { Request, Responses } = fx;

  const volumeBillingRunId = await test.step(
    'Action: FOR_VOLUMES billing run (maxEndDate present)',
    async () => {
      const id = await createStandardBillingRun(fx as ExpParity03Fx, ['FOR_VOLUMES'], chain.anchor);
      Responses.billingRun[Responses.billingRun.length - 1] = {
        id,
        invoiceNumbers: 1,
      };
      return id;
    },
  );

  const observation = await startBillingAndObserveDraft(Request, volumeBillingRunId, ['FOR_VOLUMES']);
  expect(observation.draftInvoiceIds.length, 'volume run produced a draft invoice').toBeGreaterThan(
    0,
  );
  const creditNoteInvoiceId = observation.draftInvoiceIds[0];
  if (!Responses.invoice.includes(creditNoteInvoiceId as never)) {
    Responses.invoice.push(creditNoteInvoiceId);
  }

  const invoice = await getInvoice(Request, creditNoteInvoiceId);
  expect(invoice.invoiceType, 'volume invoiceType is STANDARD').toBe('STANDARD');
  expect(
    invoice.invoiceDocumentType,
    'IAP must exceed the period invoice so the document is CREDIT_NOTE — reduce volumes or increase IAP',
  ).toBe('CREDIT_NOTE');
  expect(nestedId(invoice.customer), 'Credit Note customer is the billed customer').toBe(
    chain.billedCustomerId,
  );

  const cnTotalInclVat = money(invoice.totalAmountIncludingVat);
  expect(
    cnTotalInclVat,
    'Credit Note incl-VAT total must differ from compensation documentAmount',
  ).not.toBe(money(compensationAmount));

  return { volumeBillingRunId, creditNoteInvoiceId, cnTotalInclVat };
}

export async function generatePdt3470VolumeRun(
  fx: Pdt3470Fx,
  volumeBillingRunId: number,
): Promise<void> {
  await startGeneratingAndWaitGenerated(fx.Request, volumeBillingRunId);
}

export async function accountPdt3470VolumeRun(
  fx: Pdt3470Fx,
  volumeBillingRunId: number,
  creditNoteInvoiceId: number,
): Promise<InvoiceView> {
  await startAccountingAndWaitCompleted(fx.Request, volumeBillingRunId);
  const invoice = await getInvoice(fx.Request, creditNoteInvoiceId);
  expect(invoice.invoiceStatus, 'Credit Note is REAL after accounting').toBe('REAL');
  expect(invoice.invoiceType).toBe('STANDARD');
  expect(invoice.invoiceDocumentType).toBe('CREDIT_NOTE');
  return invoice;
}

export async function getReceivable(
  Request: Pdt3470Fx['Request'],
  receivableId: number,
): Promise<Pdt3470ReceivableView> {
  const res = await Request.get(`customer-receivable/${receivableId}`);
  await expect(res).CheckResponse();
  const body = (await res.json()) as Pdt3470ReceivableView;
  return { ...body, id: Number(body.id ?? receivableId) };
}

export async function getLiability(
  Request: Pdt3470Fx['Request'],
  liabilityId: number,
): Promise<Pdt3470LiabilityView> {
  const res = await Request.get(`customer-liability/${liabilityId}`);
  await expect(res).CheckResponse();
  const body = (await res.json()) as Pdt3470LiabilityView;
  return { ...body, id: Number(body.id ?? liabilityId) };
}

export async function listReceivablesForCustomer(
  Request: Pdt3470Fx['Request'],
  customerIdentifier: string,
): Promise<Array<{ id: number }>> {
  const res = await Request.get('customer-receivable', {
    params: {
      page: 0,
      size: 50,
      prompt: customerIdentifier,
      customerReceivableSearchBy: 'CUSTOMER',
    },
  });
  await expect(res).CheckResponse();
  const body = (await res.json()) as { content?: Array<{ id?: number }> };
  return (body.content ?? [])
    .map((row) => Number(row.id))
    .filter((id) => Number.isFinite(id) && id > 0)
    .map((id) => ({ id }));
}

export async function listLiabilitiesForCustomer(
  Request: Pdt3470Fx['Request'],
  customerIdentifier: string,
): Promise<Array<{ id: number }>> {
  const res = await Request.get('customer-liability/list', {
    params: {
      page: 0,
      size: 50,
      columns: 'ID',
      direction: 'DESC',
      prompt: customerIdentifier,
      searchFields: 'CUSTOMER',
    },
  });
  await expect(res).CheckResponse();
  const body = (await res.json()) as { content?: Array<{ id?: number }> };
  return (body.content ?? [])
    .map((row) => Number(row.id))
    .filter((id) => Number.isFinite(id) && id > 0)
    .map((id) => ({ id }));
}

export async function listCompensationsByNumber(
  Request: Pdt3470Fx['Request'],
  number: string,
): Promise<Array<{ id: number; number?: string }>> {
  const res = await Request.get(`${COMP_ROOT}/listing`, {
    params: {
      page: 0,
      size: 50,
      searchBy: 'NUMBER',
      prompt: number,
      sortBy: 'CREATE_DATE',
      direction: 'DESC',
    },
  });
  await expect(res).CheckResponse();
  const body = (await res.json()) as { content?: Array<{ id?: number; number?: string }> };
  const needle = number.trim().toLowerCase();
  return (body.content ?? []).filter(
    (row) => String(row.number ?? '').trim().toLowerCase() === needle && Number(row.id) > 0,
  ) as Array<{ id: number; number?: string }>;
}

export async function findCreditNoteReceivable(
  Request: Pdt3470Fx['Request'],
  opts: {
    customerIdentifier: string;
    creditNoteInvoiceId: number;
    compensationReceivableId: number;
    cnTotalInclVat: number;
  },
): Promise<Pdt3470ReceivableView> {
  const listed = await listReceivablesForCustomer(Request, opts.customerIdentifier);
  const absCn = money(Math.abs(opts.cnTotalInclVat));
  for (const row of listed) {
    if (row.id === opts.compensationReceivableId) {
      continue;
    }
    const detail = await getReceivable(Request, row.id);
    if (nestedId(detail.invoiceResponse) !== opts.creditNoteInvoiceId) {
      continue;
    }
    if (detail.creationType !== 'AUTOMATIC') {
      continue;
    }
    if (money(detail.initialAmount) === absCn) {
      return detail;
    }
  }
  throw new Error(
    `No Credit Note AUTOMATIC receivable (initialAmount=${absCn}) found on invoice ${opts.creditNoteInvoiceId} besides compensation receivable ${opts.compensationReceivableId}`,
  );
}

export async function receivablesOnCreditNote(
  Request: Pdt3470Fx['Request'],
  customerIdentifier: string,
  creditNoteInvoiceId: number,
): Promise<Pdt3470ReceivableView[]> {
  const listed = await listReceivablesForCustomer(Request, customerIdentifier);
  const out: Pdt3470ReceivableView[] = [];
  for (const row of listed) {
    const detail = await getReceivable(Request, row.id);
    if (nestedId(detail.invoiceResponse) === creditNoteInvoiceId) {
      out.push(detail);
    }
  }
  return out;
}

export async function postZeroAmountCompensationExpect400(
  fx: Pdt3470Fx,
  payload: Record<string, unknown>,
): Promise<{ status: number; haystack: string }> {
  const res = await fx.Request.post(fx.Endpoints.compensation, { data: payload });
  const status = res.status();
  const raw = await res.json().catch(async () => res.text());
  const haystack = typeof raw === 'string' ? raw : JSON.stringify(raw);
  return { status, haystack };
}
