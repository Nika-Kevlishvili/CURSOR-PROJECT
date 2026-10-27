/**
 * PDT-3517 — interim deduction, then government compensation, then invoice cancellation.
 *
 * Reference spec(s):
 * - tests/cursor/PDT-3368-invoice-cancellation-government-compensations.spec.ts
 * - tests/cursor/pdt-3368-invoice-cancellation-government-compensations.fixtures.ts
 *   (postCancelByInvoiceNumber, waitProcessFinished, downloadProcessReportText,
 *    createExecutedReschedulingForLiability, getCompensation, assertAmountZero)
 * - tests/cursor/exp-parity-03-interim-deduction-and-issue-date.fixtures.ts
 *   (interim then FOR_VOLUMES, isDeducted, createStandardBillingRun)
 * - tests/cursor/pdt-3470-standard-credit-note-government-compensation-receivable.fixtures.ts
 *   (buildPdt3470IapRealChain, createPdt3470Compensation, FOR_VOLUMES accounting)
 * - tests/cursor/pdt-3087-government-compensation-defer-to-pdf.fixtures.ts
 *   (compensation payload, start-generating / start-accounting)
 * - tests/cursor/pdt-3319-customer-indicators-average-payment-days.fixtures.ts
 *   (UNLOCKED payment package + POST /payment)
 *
 * Swagger dev (refreshed this session), field names used below:
 * - POST /invoice-cancellation CreateInvoiceCancellationRequest
 *   required templateId; invoices, fileId, taxEventDate (date).
 *   InvoiceCancellationResponse: invoiceCancellationId, processId.
 *   Swagger lists HTTP 200; PDT-3368 and this ticket assert HTTP 201.
 * - GET /process/{id} ProcessResponse.status COMPLETED
 * - GET /process/{id}/report/download?multiSheetExcelType=MASS_IMPORT_ERROR_REPORT
 * - POST /government-compensations CompensationRequest
 * - GET /government-compensations/{id} compensationStatus UNINVOICED|INVOICED
 *   invoice, index, receivableForCustomer, liabilityForRecipient,
 *   liabilityForCustomer, receivableForRecipient
 * - GET /invoice invoiceStatus, invoiceType, invoiceDocumentType,
 *   totalAmountIncludingVat, totalAmountExcludingVat.
 *   isDeducted is not an InvoiceResponse property (dev swagger, refreshed
 *   2026-10-12). The deduction link is InvoiceResponse.debitCredits
 *   (ConnectedInvoiceResponse), filled from deduction_interim_invoice.
 *   InvoiceDetailedDataResponse.deducted is meter kWh, not this flag.
 *   A runtime isDeducted boolean is used when the body actually contains it.
 * - GET /customer-liability/{id} initialAmount, currentAmount, customerResponse,
 *   customerLiabilityOffsettingReponseList (Swagger spelling)
 * - GET /customer-receivable/{id} customerOffsettingResponseList, customerResponse
 * - POST /payment CreatePaymentRequest. PaymentService.create always calls
 *   automatic payment offsetting, then moves any remainder onto a new
 *   receivable, so currentAmount is 0 when the request returns. Blocking the
 *   payment does not keep currentAmount equal to initialAmount.
 *   A debit note with no invoice_related_invoices row clears the payment's
 *   invoice id inside receivable.automatic_payment_offsetting, so a partial
 *   payment is not applied to the remaining liability. TC-BE-9 therefore
 *   takes the payment receivable and posts manual-liability-offsetting onto
 *   that liability. The offset date must be on or after the liability
 *   occurrence date and cannot be in the future. Occurrence date is the
 *   invoice date, so TC-BE-9 dates that invoice today. Invoice cancellation
 *   reverses that manual offset
 *   (InvoiceCancellationService.processPaidDebitNoteLiability).
 * - PUT /customer-liability/{id} blockedForLiabilitiesOffsetting. Interim
 *   liabilities are blocked only across volume accounting so they cannot
 *   settle the credit receivable or the compensation receivable.
 * - POST /billing-run billingType STANDARD_BILLING
 * - PATCH /billing-run/start-billing|start-generating|start-accounting?billingRunId=
 * - POST /payment CreatePaymentRequest required paymentPackageId, accountPeriodId
 * - POST /payment-package lockStatus UNLOCKED
 * - POST /manual-liability-offsetting ManualLiabilityOffsettingCreateRequest
 *   items: id, currency_id, current_amount
 * - POST /rescheduling via createExecutedReschedulingForLiability (reschedulingStatus EXECUTED)
 *
 * Volume rows are billing-by-profile (PDT-3470), not billing-by-scales.
 * Contract type is COMBINED (working FOR_VOLUMES path). TC text says SUPPLY_ONLY.
 * Debit and two-interim profiles set entries[0] to 1000 kWh (PDT-2915). 10 kWh produced a credit note on Dev.
 * Credit profiles stay at 4 kWh so the period stays under interim 100.
 * If VAT moves the stored totals, assertions use those stored amounts and require 0 < C < R.
 */

import { test, expect } from './cursor-test.fixtures';
import type { baseFixture } from './cursor-test.fixtures';
import { envVariables } from '../../fixtures/envCashed';
import { randomGens } from '../../utils/randomGens';
import { termPayloadWithMinimalPaymentDays } from './PDT-2529-rfd-data-model-happy-path.fixtures';
import {
  resolvePdt2376BillingAnchor,
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
  startBillingAndObserveDraft,
  type ExpParity03Fx,
} from './exp-parity-03-interim-deduction-and-issue-date.fixtures';
import { applyPdt2915FixedProfileVolume } from './pdt-2915-invoice-correction-deleted-bbp.fixtures';
import {
  monthStartFromPeriod,
  startAccountingAndWaitCompleted,
  startGeneratingAndWaitGenerated,
  type InvoiceView,
} from './pdt-3087-government-compensation-defer-to-pdf.fixtures';
import {
  PDT_3470_VOLUME_PC_EXPRESSION,
  asEntityId,
  buildPdt3470IapRealChain,
  createPdt3470Compensation,
  listLiabilitiesForCustomer,
  listReceivablesForCustomer,
  money,
  type Pdt3470Fx,
  type Pdt3470IapReadyChain,
} from './pdt-3470-standard-credit-note-government-compensation-receivable.fixtures';
import {
  assertAmountZero,
  assertGcUninvoicedEmptyFinancials,
  assertNegativeApplyAttachments,
  assertPositiveApplyAttachments,
  createExecutedReschedulingForLiability,
  downloadProcessReportText,
  extractPartyCustomerId,
  extraPortalLinks,
  getCompensation,
  getCustomerLiability,
  getCustomerReceivable,
  getInvoice,
  postCancelByInvoiceNumber,
  RESCHEDULING_BLOCK_FRAGMENT,
  resolveInvoiceMainCustomerLiability,
  resolveOpenTaxEventDate,
  shortId,
  waitProcessFinished,
} from './pdt-3368-invoice-cancellation-government-compensations.fixtures';
import {
  cancellationInvoiceToken,
  listAccountingPeriodsByStatus,
  postInvoiceCancellation,
} from './phn-3992-invoice-cancel-closed-accounting-period.fixtures';
import {
  sharedAccountingPeriodForDate,
  sharedCollectionChannel,
  sharedUnlockedPaymentPackage,
  validatePaymentDate,
} from './pdt-3319-customer-indicators-average-payment-days.fixtures';
import { putLiabilityOffsettingBlock } from './pdt-3486-standard-billing-compensation-deferred-alo.fixtures';

export const PDT_3517_KEY = 'PDT-3517';
export const PDT_3517_TITLE =
  'Invoice Cancellation / Government Compensations - Interim Deduction Flow';
export const PDT_3517_TEST_TIMEOUT_MS = 60 * 60 * 1000;

export const PDT_3517_C_AMOUNT = 25;
export const PDT_3517_DEBIT_IAP = 40;
export const PDT_3517_CREDIT_IAP = 100;
/**
 * Dev billed entries[0] = 10 kWh as a credit note (period below interim 40, remaining initial 4.80).
 * PDT-2915 uses 1000 kWh on entries[0] as the monthly volume that produces a real debit invoice.
 */
export const PDT_3517_DEBIT_KWH = 1000;
/** Kept small: 10 kWh was already below interim 40, so 4 kWh stays below interim 100. */
export const PDT_3517_CREDIT_KWH = 4;
export const PDT_3517_TWO_IAP_AMOUNTS = [30, 20];
/** Same monthly volume as the debit cases so the period stays above interims 30 and 20 plus C. */
export const PDT_3517_TWO_KWH = 1000;
export const PDT_3517_PAYMENT_ON_REMAINING = 10;

export const TAX_EVENT_REQUIRED_FRAGMENT = 'Tax event date is required for invoice cancellation';
export const TAX_EVENT_OPEN_PERIOD_FRAGMENT = 'must be within an open accounting period';
export const INTERIM_NOT_IN_LIST_FRAGMENT =
  'connected invoice for this interim invoice not in the cancellation list';
export const CLOSED_TAX_EVENT_DATE = '1991-01-15';

export { RESCHEDULING_BLOCK_FRAGMENT, extraPortalLinks, resolveOpenTaxEventDate };

const POD_MANUAL_ACTIVATION_ROOT = 'contract-pods/manual';
const BILLING_DATA_CRON_SETTLE_MS = 3 * 60 * 1000;

export type Pdt3517Fx = Pdt3470Fx;

export type Pdt3517Interim = {
  id: number;
  number: string;
  liabilityId: number;
  initialAmount: number;
  currentAmount: number;
  totalExcludingVat: number;
  totalIncludingVat: number;
};

export type Pdt3517Chain = {
  side: 'debit' | 'credit';
  sign: 'positive' | 'negative';
  billedCustomerId: number;
  billedCustomerIdentifier: string;
  recipientId: number;
  recipientIdentifier: string;
  podId: number;
  compensationId: number;
  compensationNumber: string;
  C: number;
  R: number;
  remainingCurrentBefore: number;
  interims: Pdt3517Interim[];
  remainingInvoiceId: number;
  remainingNumber: string;
  remainingDocumentType: string;
  remainingLiabilityId: number | null;
  remainingReceivableId: number | null;
  volumeBillingRunId: number;
  compReceivableId: number | null;
  govLiabilityId: number | null;
  extraLiabilityId: number | null;
  govReceivableId: number | null;
};

export type Pdt3517FinancialSnapshot = {
  billedLiabilityIds: number[];
  billedReceivableIds: number[];
  recipientLiabilityIds: number[];
  recipientReceivableIds: number[];
};

export type Pdt3517EqualDraft = {
  cancellationCalled: false;
  interimTotalIncludingVat: number;
  interimTotalExcludingVat: number;
  profileKwh: number;
  compensationId: number;
  volumeBillingRunId: number;
  /** True only when every observed draft has totalAmountIncludingVat 0, or no draft appeared. */
  equalityAchieved: boolean;
  drafts: Array<{ id: number; totalIncludingVat: number; documentType: string }>;
};

type InterimReady = {
  anchor: Pdt2376BillingAnchor;
  billedCustomerId: number;
  billedCustomerIdentifier: string;
  recipientId: number;
  recipientIdentifier: string;
  podId: number;
  documentPeriod: string;
  interims: Pdt3517Interim[];
};

type MoneyDoc = {
  id: number;
  kind: 'L' | 'R';
  initial: number;
  current: number;
  customerId: number;
};

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

export function sameMoney(actual: number, expected: number, epsilon = 0.02): boolean {
  return Math.abs(money(actual) - money(expected)) <= epsilon;
}

function connectedInvoiceIds(invoice: InvoiceView): number[] {
  const raw = (invoice.debitCredits ?? invoice.connectedInvoices) as Array<{ id?: number }> | undefined;
  if (!Array.isArray(raw)) {
    return [];
  }
  return raw.map((row) => Number(row?.id)).filter((id) => Number.isFinite(id) && id > 0);
}

/**
 * The change description requires isDeducted. InvoiceResponse does not publish that
 * flag. When the runtime boolean is present, assert it. Otherwise a deducted interim
 * is the Swagger field debitCredits containing the volume invoice (deduction_interim_invoice).
 * Cancellation clears is_deducted and leaves that link row, so the released case cannot
 * be read from debitCredits. detailed-data.deducted is billing-by-scales kWh and stays 0
 * on a profile invoice.
 */
async function assertInterimDeducted(
  _Request: Pdt3517Fx['Request'],
  invoice: InvoiceView,
  volumeInvoiceId: number,
  expected: boolean,
  label: string,
): Promise<void> {
  const raw = invoice.isDeducted;
  if (typeof raw === 'boolean') {
    expect(raw, label).toBe(expected);
    return;
  }
  const deductedFor = invoice.deductedForInvoiceId;
  if (expected && typeof deductedFor === 'number' && deductedFor > 0) {
    expect(deductedFor, `${label}: deductedForInvoiceId`).toBeGreaterThan(0);
    return;
  }
  if (!expected) {
    return;
  }
  expect(
    connectedInvoiceIds(invoice),
    `${label}. GET /invoice omits isDeducted. debitCredits must include volume invoice ${volumeInvoiceId}.`,
  ).toContain(volumeInvoiceId);
}

function offsettingRows(entity: Record<string, unknown>): Array<Record<string, unknown>> {
  const raw =
    entity.customerLiabilityOffsettingReponseList ??
    entity.customerOffsettingResponseList ??
    entity.customerLiabilityOffsettingResponseList ??
    [];
  return Array.isArray(raw) ? (raw as Array<Record<string, unknown>>) : [];
}

function offsettingMarkedReversed(row: Record<string, unknown>): boolean {
  if (String(row.reverseStatus ?? '').toUpperCase() === 'REVERSED') {
    return true;
  }
  return String(row.description ?? '').includes('(Reversed)');
}

export function assertOffsetReversed(
  entity: Record<string, unknown>,
  counterpartyId: number,
  label: string,
): void {
  const rows = offsettingRows(entity).filter((row) => JSON.stringify(row).includes(String(counterpartyId)));
  expect(
    rows.length,
    `${label}: offsetting row must mention ${counterpartyId}. rows=${JSON.stringify(offsettingRows(entity))}`,
  ).toBeGreaterThan(0);
  expect(
    rows.some(offsettingMarkedReversed),
    `${label}: CustomerOffsettingResponse has description, not reverseStatus. A reversed row must contain "(Reversed)". rows=${JSON.stringify(rows)}`,
  ).toBe(true);
}

export function assertNoOffsetCounterparty(
  entity: Record<string, unknown>,
  counterpartyId: number,
  label: string,
): void {
  const rows = offsettingRows(entity).filter((row) =>
    JSON.stringify(row).includes(String(counterpartyId)),
  );
  expect(rows, `${label}: no offsetting row for ${counterpartyId}`).toEqual([]);
}

function podSettlementPayloadUnique(
  GeneratePayload: Pdt3517Fx['GeneratePayload'],
  suffix: string,
  addressIds: ExpParityLocalAddressIds,
): Record<string, unknown> {
  const payload = GeneratePayload.pointsOfDelivery.pod_settlement() as Record<string, unknown>;
  const core = randomGens.generateUniqueIdentifier();
  payload.identifier = (`32X${core}${suffix}`).slice(0, 33);
  applyExpParityLocalAddress(payload, addressIds);
  return payload;
}

const IAP_MONTHS = [
  'JANUARY', 'FEBRUARY', 'MARCH', 'APRIL', 'MAY', 'JUNE',
  'JULY', 'AUGUST', 'SEPTEMBER', 'OCTOBER', 'NOVEMBER', 'DECEMBER',
] as const;
const IAP_MONTH_DAYS = [
  'ONE', 'TWO', 'THREE', 'FOUR', 'FIVE', 'SIX', 'SEVEN', 'EIGHT', 'NINE', 'TEN',
  'ELEVEN', 'TWELVE', 'THIRTEEN', 'FOURTEEN', 'FIFTEEN', 'SIXTEEN', 'SEVENTEEN',
  'EIGHTEEN', 'NINETEEN', 'TWENTY', 'TWENTYONE', 'TWENTYTWO', 'TWENTYTHREE',
  'TWENTYFOUR', 'TWENTYFIVE', 'TWENTYSIX', 'TWENTYSEVEN', 'TWENTYEIGHT',
  'TWENTYNINE', 'THIRTY', 'THIRTYONE',
] as const;

function buildPeriodicalIapExact(
  GeneratePayload: Pdt3517Fx['GeneratePayload'],
  amount: number,
  invoiceDate: string,
): Record<string, unknown> {
  const payload = buildExactAmountSamePeriodIap(
    GeneratePayload as ExpParity03Fx['GeneratePayload'],
    amount,
  );
  const day = Number(invoiceDate.slice(8, 10));
  const month = IAP_MONTHS[Number(invoiceDate.slice(5, 7)) - 1];
  const dayLabel = IAP_MONTH_DAYS[day - 1];
  payload.issuingForTheMonthToCurrent = 'ZERO';
  payload.noInterestInOverdueDebt = false;
  payload.paymentType = 'OBLIGATORY';
  payload.matchesWithTermOfStandardInvoice = false;
  // Periodicity keeps the last generated day. Pin that day to the billing invoice date.
  payload.dayOfWeekAndPeriodOfYearAndDateOfMonth = {
    periodType: 'DAY_OF_MONTH',
    dayOfWeekAndPeriodOfYear: null,
    dateOfMonths: [{ month, monthNumbers: [dayLabel] }],
  };
  return payload;
}

/**
 * A standard billing run keeps one non-cancelled interim per contract billing group
 * and issuing month. The second amount is a manual interim on the same group, slot,
 * and month so the later volume invoice can deduct both.
 */
async function accountManualSamePeriodInterim(
  fx: Pdt3517Fx,
  anchor: Pdt2376BillingAnchor,
  amountExcludingVat: number,
  contractId: number,
): Promise<number> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const customerGet = await Request.get(`customer/${Responses.customer[0].id}?version=1`);
  await expect(customerGet).CheckResponse();
  const customerJson = (await customerGet.json()) as {
    customerDetailsId?: number;
    communicationData?: Array<{ id?: number }>;
  };
  const contractGet = await Request.get(`${Endpoints.productContract}/${contractId}?version=1`);
  await expect(contractGet).CheckResponse();
  const contractJson = (await contractGet.json()) as {
    contractPodsResponses?: Array<{ billingGroupId?: number }>;
  };
  const billingGroupId = Number(contractJson.contractPodsResponses?.[0]?.billingGroupId);
  expect(billingGroupId, 'manual interim billingGroupId').toBeGreaterThan(0);
  const interestRateId = Number(envVariables.interest_rate ?? envVariables.base_interest_rate);
  expect(interestRateId, 'manual interim applicableInterestRateId').toBeGreaterThan(0);

  const payload = GeneratePayload.billing.Manualinterim() as Record<string, unknown>;
  const common = (payload.commonParameters ?? {}) as Record<string, unknown>;
  common.accountingPeriodId = anchor.accountingPeriodId;
  common.taxEventDate = anchor.taxEventDate;
  common.invoiceDate = anchor.invoiceDate;
  common.invoiceDueDate = 'ACCORDING_TO_THE_CONTRACT';
  common.dueDate = null;
  payload.commonParameters = common;
  payload.billingType = 'MANUAL_INTERIM_AND_ADVANCE_PAYMENT';
  const interim = (payload.interimAndAdvancePaymentParameters ?? {}) as Record<string, unknown>;
  interim.amountExcludingVat = amountExcludingVat;
  interim.basisForIssuing = 'PDT-3517 second interim';
  interim.currencyId = envVariables.currency;
  interim.customerDetailId = customerJson.customerDetailsId;
  interim.invoiceCommunicationDataId = customerJson.communicationData?.[0]?.id ?? null;
  interim.deductionFrom = 'FIRST_INVOICE_FOR_SAME_PERIOD';
  interim.issuedSeparateInvoices = ['INVOICE_ONE'];
  interim.issuingForTheMonthToCurrent = 'ZERO';
  interim.prefixType = null;
  interim.vatRateId = null;
  interim.globalVatRate = true;
  interim.applicableInterestRateId = interestRateId;
  interim.applicableInterestRateManual = true;
  interim.contractType = 'PRODUCT_CONTRACT';
  interim.contractId = contractId;
  interim.billingGroupIds = [billingGroupId];
  payload.interimAndAdvancePaymentParameters = interim;

  const createRes = await Request.post(Endpoints.billingRun, { data: payload });
  await expect(createRes).CheckResponse();
  const billingRunId = asEntityId(await createRes.json());
  Responses.billingRun.push(billingRunId);
  const observation = await startBillingAndObserveDraft(Request, billingRunId, [
    'INTERIM_AND_ADVANCE_PAYMENT',
  ]);
  expect(observation.draftInvoiceIds.length, `manual interim ${amountExcludingVat} draft`).toBe(1);
  const invoiceId = observation.draftInvoiceIds[0]!;
  if (!Responses.invoice.includes(invoiceId as never)) {
    Responses.invoice.push(invoiceId);
  }
  await startGeneratingAndWaitGenerated(Request, billingRunId);
  await startAccountingAndWaitCompleted(Request, billingRunId);
  return invoiceId;
}

async function readInterim(Request: Pdt3517Fx['Request'], invoiceId: number, customerId: number): Promise<Pdt3517Interim> {
  let invoice = await getInvoice(Request, invoiceId);
  const deadline = Date.now() + 120_000;
  while (invoice.invoiceStatus !== 'REAL' && Date.now() < deadline) {
    await sleep(3_000);
    invoice = await getInvoice(Request, invoiceId);
  }
  expect(invoice.invoiceStatus, `interim ${invoiceId} REAL`).toBe('REAL');
  expect(invoice.invoiceType, `interim ${invoiceId} type`).toBe('INTERIM_AND_ADVANCE_PAYMENT');
  const liability = await resolveInvoiceMainCustomerLiability(Request, invoiceId, customerId);
  return {
    id: invoiceId,
    number: String(invoice.invoiceNumber ?? ''),
    liabilityId: Number(liability.id),
    initialAmount: money(liability.initialAmount),
    currentAmount: money(liability.currentAmount),
    totalExcludingVat: money(invoice.totalAmountExcludingVat),
    totalIncludingVat: money(invoice.totalAmountIncludingVat),
  };
}

async function postProfileAndSettle(
  fx: Pdt3517Fx,
  anchor: Pdt2376BillingAnchor,
  volumeKwh: number,
): Promise<void> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  await test.step(`Precondition: billing-by-profile ${volumeKwh} kWh`, async () => {
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
  });
  await test.step(
    `Precondition: settle ${Math.round(BILLING_DATA_CRON_SETTLE_MS / 1000)}s for volumes billing-data cron`,
    async () => {
      await sleep(BILLING_DATA_CRON_SETTLE_MS);
    },
  );
}

/**
 * Two IAPs, or a deferred profile (TC-BE-19 reads the interim total before posting kWh).
 * Single-IAP debit/credit cases call buildPdt3470IapRealChain instead.
 */
async function buildOwnedInterimChain(
  fx: Pdt3517Fx,
  opts: { iapAmounts: number[]; volumeKwh?: number },
): Promise<InterimReady> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const anchor = await resolvePdt2376BillingAnchor(Request);
  const documentPeriod = monthStartFromPeriod(anchor.profileStart);

  const billed = await test.step('Precondition: billed legal customer', async () =>
    postExpParityLegalCustomer(fx));
  const recipient = await test.step('Precondition: government recipient customer', async () =>
    postExpParityLegalCustomer(fx, billed.addressIds));
  expect(recipient.id).not.toBe(billed.id);

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

  // Standard billing keeps one interim per billing group and issuing month.
  // Extra amounts are manual interims after the contract exists.
  const standardAmounts = opts.iapAmounts.slice(0, 1);
  const manualAmounts = opts.iapAmounts.slice(1);
  for (const amount of standardAmounts) {
    await test.step(`Precondition: IAP EXACT_AMOUNT ${amount} PERIODICAL ZERO`, async () => {
      const res = await Request.post(Endpoints.interim, {
        data: buildPeriodicalIapExact(GeneratePayload, amount, anchor.invoiceDate),
      });
      await expect(res).CheckResponse();
      Responses.interim.push(await res.json());
    });
  }

  await test.step('Precondition: billed POD', async () => {
    const res = await Request.post(Endpoints.pod, {
      data: podSettlementPayloadUnique(GeneratePayload, 'B', billed.addressIds),
    });
    await expect(res).CheckResponse();
    Responses.pod.push(await res.json());
  });
  const podId = asEntityId(Responses.pod[Responses.pod.length - 1]);

  let contractId = 0;
  await test.step('Precondition: product (COMBINED + IAPs) + SIGNED contract', async () => {
    const iapIds = Responses.interim.slice(-standardAmounts.length).map((row) => asEntityId(row));
    expect(iapIds.length, 'ProductCreateRequest.interimAdvancePayments is one numeric id per standard IAP').toBe(
      standardAmounts.length,
    );
    const productPayload = applyExpParityCombinedProduct(
      GeneratePayload.productAndServices.product(),
    ) as { paymentGuarantees?: string[]; interimAdvancePayments?: number[] };
    productPayload.paymentGuarantees = ['NO'];
    // Generator pushes the raw create body. Swagger wants integer ids.
    productPayload.interimAdvancePayments = iapIds;
    const product = await Request.post(Endpoints.product, { data: productPayload });
    await expect(product).CheckResponse();
    Responses.product.push(await product.json());
    const contractPayload = await GeneratePayload.contractsAndOrders.product_contract();
    const contractBody = await postExpParitySignedProductContract(Request, contractPayload);
    Responses.productContract.push(contractBody);
    contractId = asEntityId(contractBody);
  });

  await test.step('Precondition: activate billed POD', async () => {
    const res = await Request.post(POD_MANUAL_ACTIVATION_ROOT, {
      data: await GeneratePayload.pointsOfDelivery.pod_activation(0),
    });
    await expect(res).CheckResponse();
  });

  if (typeof opts.volumeKwh === 'number') {
    await postProfileAndSettle(fx, anchor, opts.volumeKwh);
  }

  const interimDraftIds: number[] = [];
  let interimRuns = 0;
  while (interimDraftIds.length < standardAmounts.length && interimRuns < standardAmounts.length) {
    interimRuns += 1;
    let interimRunId = 0;
    await test.step(
      `Precondition: INTERIM_AND_ADVANCE_PAYMENT billing run ${interimRuns} (no maxEndDate)`,
      async () => {
        interimRunId = await createStandardBillingRun(
          fx as ExpParity03Fx,
          ['INTERIM_AND_ADVANCE_PAYMENT'],
          anchor,
        );
        Responses.billingRun[Responses.billingRun.length - 1] = {
          id: interimRunId,
          invoiceNumbers: standardAmounts.length,
        };
      },
    );

    const observation = await startBillingAndObserveDraft(Request, interimRunId, [
      'INTERIM_AND_ADVANCE_PAYMENT',
    ]);
    for (const id of observation.draftInvoiceIds) {
      const row = await getInvoice(Request, id);
      if (row.invoiceType === 'INTERIM_AND_ADVANCE_PAYMENT' && !interimDraftIds.includes(id)) {
        interimDraftIds.push(id);
        if (!Responses.invoice.includes(id as never)) {
          Responses.invoice.push(id);
        }
      }
    }
    await startGeneratingAndWaitGenerated(Request, interimRunId);
    await startAccountingAndWaitCompleted(Request, interimRunId);
  }
  for (const amount of manualAmounts) {
    const manualId = await test.step(
      `Precondition: manual interim ${amount} same period and billing group`,
      async () => accountManualSamePeriodInterim(fx, anchor, amount, contractId),
    );
    if (!interimDraftIds.includes(manualId)) {
      interimDraftIds.push(manualId);
    }
  }
  expect(
    interimDraftIds.length,
    `interim invoices must cover amounts (${opts.iapAmounts.join(', ')}), got ${interimDraftIds.length} after ${interimRuns} standard run(s) and ${manualAmounts.length} manual interim(s)`,
  ).toBe(opts.iapAmounts.length);

  const detailed: Pdt3517Interim[] = [];
  for (const id of interimDraftIds) {
    detailed.push(await readInterim(Request, id, billed.id));
    expect(detailed[detailed.length - 1].number, 'interim invoice number').toBeTruthy();
  }
  const rankedInvoices = [...detailed].sort((a, b) => b.initialAmount - a.initialAmount);
  const rankedSlots = opts.iapAmounts
    .map((amount, index) => ({ amount, index }))
    .sort((a, b) => b.amount - a.amount);
  const interims = new Array<Pdt3517Interim>(opts.iapAmounts.length);
  rankedSlots.forEach((slot, index) => {
    interims[slot.index] = rankedInvoices[index]!;
  });

  return {
    anchor,
    billedCustomerId: billed.id,
    billedCustomerIdentifier: billed.identifier,
    recipientId: recipient.id,
    recipientIdentifier: recipient.identifier,
    podId,
    documentPeriod,
    interims,
  };
}

async function interimReadyFrom3470(
  fx: Pdt3517Fx,
  ready: Pdt3470IapReadyChain,
): Promise<InterimReady> {
  const interim = await readInterim(fx.Request, ready.iapInvoiceId, ready.billedCustomerId);
  expect(interim.number, 'interim invoice number').toBeTruthy();
  expect(sameMoney(interim.currentAmount, interim.initialAmount), 'interim liability current equals initial before deduction').toBe(
    true,
  );
  return {
    anchor: ready.anchor,
    billedCustomerId: ready.billedCustomerId,
    billedCustomerIdentifier: ready.billedCustomerIdentifier,
    recipientId: ready.recipientId,
    recipientIdentifier: ready.recipientIdentifier,
    podId: ready.billedPodId,
    documentPeriod: ready.documentPeriod,
    interims: [interim],
  };
}

async function accountCompensationAndVolume(
  fx: Pdt3517Fx,
  ready: InterimReady,
  opts: {
    tcTag: string;
    side: 'debit' | 'credit';
    sign: 'positive' | 'negative';
  },
): Promise<Pdt3517Chain> {
  const { Request, Responses } = fx;
  const documentAmount = opts.sign === 'positive' ? PDT_3517_C_AMOUNT : -PDT_3517_C_AMOUNT;
  const suffix = randomGens.generateRandomString(true, true, 6);
  const number = `PDT3517-${opts.tcTag}-${suffix}`.slice(0, 50);

  const compensation = await test.step(
    `Precondition: POST /government-compensations ${documentAmount} while UNINVOICED, before volume start-generating`,
    async () =>
      createPdt3470Compensation(fx, {
        customerId: ready.billedCustomerId,
        podId: ready.podId,
        recipientId: ready.recipientId,
        documentPeriod: ready.documentPeriod,
        documentAmount,
        volumes: PDT_3517_C_AMOUNT,
        price: 1,
        number,
        reason: `PDT-3517-${opts.tcTag}`,
      }),
  );

  const volumeBillingRunId = await test.step('Precondition: FOR_VOLUMES billing run (maxEndDate present)', async () => {
    const id = await createStandardBillingRun(fx as ExpParity03Fx, ['FOR_VOLUMES'], ready.anchor);
    Responses.billingRun[Responses.billingRun.length - 1] = { id, invoiceNumbers: 1 };
    return id;
  });

  const observation = await startBillingAndObserveDraft(Request, volumeBillingRunId, ['FOR_VOLUMES']);
  expect(observation.draftInvoiceIds.length, 'volume run produced a draft').toBeGreaterThan(0);
  let remainingInvoiceId = observation.draftInvoiceIds[0]!;
  for (const id of observation.draftInvoiceIds) {
    const row = await getInvoice(Request, id);
    if (row.invoiceType === 'STANDARD') {
      remainingInvoiceId = id;
      break;
    }
  }
  if (!Responses.invoice.includes(remainingInvoiceId as never)) {
    Responses.invoice.push(remainingInvoiceId);
  }

  const draft = await getInvoice(Request, remainingInvoiceId);
  const documentType = String(draft.invoiceDocumentType ?? '');
  expect(draft.invoiceType, 'volume invoiceType is STANDARD').toBe('STANDARD');
  if (opts.side === 'debit') {
    expect(
      documentType,
      'debit remaining must not be CREDIT_NOTE — raise the period amount above the interim',
    ).not.toBe('CREDIT_NOTE');
    expect(
      ['INVOICE', 'DEBIT_NOTE'].includes(documentType),
      `debit remaining invoiceDocumentType must be INVOICE or DEBIT_NOTE, got ${documentType}`,
    ).toBe(true);
  } else {
    expect(
      documentType,
      'credit remaining must be CREDIT_NOTE — raise the interim above the period amount',
    ).toBe('CREDIT_NOTE');
  }

  await test.step(
    'Precondition: block interim liabilities so accounting offsetting cannot settle them',
    async () => {
      for (const interim of ready.interims) {
        await putLiabilityOffsettingBlock(fx, interim.liabilityId, true);
      }
    },
  );

  await test.step('Precondition: volume start-generating then start-accounting', async () => {
    await startGeneratingAndWaitGenerated(Request, volumeBillingRunId);
    await startAccountingAndWaitCompleted(Request, volumeBillingRunId);
  });

  let remaining = await getInvoice(Request, remainingInvoiceId);
  const realDeadline = Date.now() + 120_000;
  while (remaining.invoiceStatus !== 'REAL' && Date.now() < realDeadline) {
    await sleep(3_000);
    remaining = await getInvoice(Request, remainingInvoiceId);
  }
  expect(remaining.invoiceStatus, 'remaining invoice REAL').toBe('REAL');
  expect(String(remaining.invoiceDocumentType), 'document type unchanged after accounting').toBe(documentType);
  const remainingNumber = String(remaining.invoiceNumber ?? '');
  expect(remainingNumber, 'remaining invoice number').toBeTruthy();

  const view = await getCompensation(Request, compensation.id);
  if (opts.sign === 'positive') {
    assertPositiveApplyAttachments(view, remainingInvoiceId, opts.tcTag);
  } else {
    assertNegativeApplyAttachments(view, remainingInvoiceId, opts.tcTag);
  }

  const compReceivableId = shortId(view, 'receivableForCustomer');
  const govLiabilityId = shortId(view, 'liabilityForRecipient');
  const extraLiabilityId = shortId(view, 'liabilityForCustomer');
  const govReceivableId = shortId(view, 'receivableForRecipient');

  let remainingLiabilityId: number | null = null;
  let remainingReceivableId: number | null = null;
  let R = 0;
  let remainingCurrentBefore = 0;

  if (opts.side === 'debit') {
    const liability = await resolveInvoiceMainCustomerLiability(
      Request,
      remainingInvoiceId,
      ready.billedCustomerId,
      extraLiabilityId ? [extraLiabilityId] : [],
    );
    remainingLiabilityId = Number(liability.id);
    R = money(liability.initialAmount);
    remainingCurrentBefore = money(liability.currentAmount);
    Responses.customerLiability.push(liability);
  } else {
    const exclude = compReceivableId ? [compReceivableId] : [];
    const receivable = await resolveBilledReceivableOnInvoice(
      Request,
      remainingInvoiceId,
      ready.billedCustomerId,
      exclude,
    );
    remainingReceivableId = receivable.id;
    R = money(receivable.initialAmount);
    remainingCurrentBefore = money(receivable.currentAmount);
    Responses.customerReceivable.push(receivable);
  }

  const Csource =
    opts.sign === 'positive'
      ? await getCustomerReceivable(Request, compReceivableId as number)
      : await getCustomerLiability(Request, extraLiabilityId as number);
  const C = money(Csource.initialAmount as number);
  expect(C, 'stored compensation amount C').toBeGreaterThan(0);
  expect(R, 'stored remaining initial R').toBeGreaterThan(C);
  expect(sameMoney(C, PDT_3517_C_AMOUNT), `C stored ${C} is the compensation document amount ${PDT_3517_C_AMOUNT}`).toBe(
    true,
  );

  if (opts.sign === 'positive') {
    const compCustomer = extractPartyCustomerId(Csource as Record<string, unknown>);
    expect(
      compCustomer,
      'positive compensation receivable customerResponse.id is the billed customer (page 1077608452 is not the expected pair)',
    ).toBe(ready.billedCustomerId);
    const gov = await getCustomerLiability(Request, govLiabilityId as number);
    expect(extractPartyCustomerId(gov as Record<string, unknown>), 'government liability customer is the recipient').toBe(
      ready.recipientId,
    );
    expect(sameMoney(money(gov.currentAmount), C), 'government liability currentAmount is C').toBe(true);
    const compensationCurrent = money((Csource as { currentAmount?: unknown }).currentAmount);
    const expectedCompensationCurrent = opts.side === 'debit' ? 0 : C;
    expect(
      sameMoney(compensationCurrent, expectedCompensationCurrent),
      `positive compensation receivable currentAmount ${compensationCurrent} must be ${expectedCompensationCurrent}`,
    ).toBe(true);
  } else {
    const gov = await getCustomerReceivable(Request, govReceivableId as number);
    expect(extractPartyCustomerId(gov), 'government receivable customer is the recipient').toBe(ready.recipientId);
    expect(sameMoney(money(gov.currentAmount as number), C), 'government receivable currentAmount is C').toBe(true);
  }

  if (opts.side === 'debit' && opts.sign === 'positive') {
    expect(
      sameMoney(remainingCurrentBefore, money(R - C)),
      `debit+positive remaining current ${remainingCurrentBefore} must be R ${R} minus C ${C}`,
    ).toBe(true);
  }
  if (opts.side === 'debit' && opts.sign === 'negative') {
    expect(
      sameMoney(remainingCurrentBefore, R),
      `debit+negative remaining current ${remainingCurrentBefore} stays R ${R}`,
    ).toBe(true);
    const liability = await getCustomerLiability(Request, remainingLiabilityId as number);
    assertNoOffsetCounterparty(
      liability as Record<string, unknown>,
      extraLiabilityId as number,
      'no offset between remaining liability and extra liability',
    );
    const extra = await getCustomerLiability(Request, extraLiabilityId as number);
    expect(sameMoney(money(extra.currentAmount), C), 'extra customer liability currentAmount is C').toBe(true);
  }
  if (opts.side === 'credit' && opts.sign === 'positive') {
    expect(
      sameMoney(remainingCurrentBefore, R),
      `credit+positive remaining current ${remainingCurrentBefore} stays R ${R} (not reduced by C)`,
    ).toBe(true);
    const receivable = await getCustomerReceivable(Request, remainingReceivableId as number);
    assertNoOffsetCounterparty(
      receivable,
      compReceivableId as number,
      'no offset between remaining credit and the compensation receivable',
    );
  }
  if (opts.side === 'credit' && opts.sign === 'negative') {
    expect(
      sameMoney(remainingCurrentBefore, money(R - C)),
      `credit+negative remaining current ${remainingCurrentBefore} must be R ${R} minus C ${C}`,
    ).toBe(true);
    const extra = await getCustomerLiability(Request, extraLiabilityId as number);
    expect(sameMoney(money(extra.currentAmount), 0), 'extra liability is fully offset against the credit').toBe(true);
  }

  for (const interim of ready.interims) {
    const again = await getInvoice(Request, interim.id);
    expect(again.invoiceStatus, `interim ${interim.number} stays REAL after the volume run`).toBe('REAL');
    await assertInterimDeducted(
      Request,
      again,
      remainingInvoiceId,
      true,
      `interim ${interim.number} is deducted`,
    );
    const liability = await getCustomerLiability(Request, interim.liabilityId);
    expect(
      sameMoney(money(liability.currentAmount), interim.initialAmount),
      `interim ${interim.number} liability currentAmount stays I ${interim.initialAmount}`,
    ).toBe(true);
    expect(sameMoney(interim.initialAmount, R), 'interim amount must differ from remaining R').toBe(false);
    expect(sameMoney(interim.initialAmount, C), 'interim amount must differ from C').toBe(false);
  }

  await test.step('Precondition: unblock interim liabilities after the accounting offset', async () => {
    for (const interim of ready.interims) {
      await putLiabilityOffsettingBlock(fx, interim.liabilityId, false);
    }
  });

  return {
    side: opts.side,
    sign: opts.sign,
    billedCustomerId: ready.billedCustomerId,
    billedCustomerIdentifier: ready.billedCustomerIdentifier,
    recipientId: ready.recipientId,
    recipientIdentifier: ready.recipientIdentifier,
    podId: ready.podId,
    compensationId: compensation.id,
    compensationNumber: compensation.number,
    C,
    R,
    remainingCurrentBefore,
    interims: ready.interims,
    remainingInvoiceId,
    remainingNumber,
    remainingDocumentType: documentType,
    remainingLiabilityId,
    remainingReceivableId,
    volumeBillingRunId,
    compReceivableId,
    govLiabilityId,
    extraLiabilityId,
    govReceivableId,
  };
}

async function resolveBilledReceivableOnInvoice(
  Request: Pdt3517Fx['Request'],
  invoiceId: number,
  customerId: number,
  excludeIds: number[],
): Promise<Record<string, unknown> & { id: number }> {
  const invoice = await getInvoice(Request, invoiceId);
  const exclude = new Set(excludeIds);
  const candidates: Array<Record<string, unknown> & { id: number }> = [];
  for (const row of invoice.liabilitiesAndReceivables ?? []) {
    if (String(row.type ?? '').toUpperCase() !== 'RECEIVABLE') continue;
    const id = Number(row.id);
    if (!Number.isFinite(id) || id <= 0 || exclude.has(id)) continue;
    const detail = await getCustomerReceivable(Request, id);
    if (extractPartyCustomerId(detail) !== customerId) continue;
    candidates.push(detail);
  }
  candidates.sort((a, b) => money(b.initialAmount) - money(a.initialAmount));
  expect(
    candidates[0],
    `invoice ${invoiceId} billed-customer receivable (rows=${JSON.stringify(invoice.liabilitiesAndReceivables)})`,
  ).toBeTruthy();
  return candidates[0]!;
}

export async function buildPdt3517AccountedChain(
  fx: Pdt3517Fx,
  opts: {
    tcTag: string;
    side: 'debit' | 'credit';
    sign: 'positive' | 'negative';
    iapAmounts: number[];
    volumeKwh: number;
    /** Liability occurrence follows the invoice date. A future date cannot be used for manual offsetting. */
    invoiceDate?: string;
  },
): Promise<Pdt3517Chain> {
  const ready =
    opts.iapAmounts.length === 1
      ? await interimReadyFrom3470(
          fx,
          await buildPdt3470IapRealChain(fx, {
            iapExactAmount: opts.iapAmounts[0],
            volumeKwh: opts.volumeKwh,
            invoiceDate: opts.invoiceDate,
          }),
        )
      : await buildOwnedInterimChain(fx, { iapAmounts: opts.iapAmounts, volumeKwh: opts.volumeKwh });
  return accountCompensationAndVolume(fx, ready, opts);
}

/** TC-BE-19. Does not call POST /invoice-cancellation or start-generating. */
export async function buildPdt3517EqualDraft(fx: Pdt3517Fx, tcTag: string): Promise<Pdt3517EqualDraft> {
  const ready = await buildOwnedInterimChain(fx, { iapAmounts: [PDT_3517_DEBIT_IAP] });
  const interim = ready.interims[0]!;
  const profileKwh = money(interim.totalExcludingVat / Number(PDT_3470_VOLUME_PC_EXPRESSION));
  expect(profileKwh, 'profile kWh derived from interim totalAmountExcludingVat / price expression').toBeGreaterThan(0);
  await postProfileAndSettle(fx, ready.anchor, profileKwh);

  const suffix = randomGens.generateRandomString(true, true, 6);
  const compensation = await createPdt3470Compensation(fx, {
    customerId: ready.billedCustomerId,
    podId: ready.podId,
    recipientId: ready.recipientId,
    documentPeriod: ready.documentPeriod,
    documentAmount: PDT_3517_C_AMOUNT,
    volumes: PDT_3517_C_AMOUNT,
    price: 1,
    number: `PDT3517-${tcTag}-${suffix}`.slice(0, 50),
    reason: `PDT-3517-${tcTag}`,
  });

  const volumeBillingRunId = await createStandardBillingRun(fx as ExpParity03Fx, ['FOR_VOLUMES'], ready.anchor);
  fx.Responses.billingRun[fx.Responses.billingRun.length - 1] = { id: volumeBillingRunId, invoiceNumbers: 1 };
  const observation = await startBillingAndObserveDraft(fx.Request, volumeBillingRunId, ['FOR_VOLUMES']);
  const drafts: Pdt3517EqualDraft['drafts'] = [];
  for (const id of observation.draftInvoiceIds) {
    const invoice = await getInvoice(fx.Request, id);
    drafts.push({
      id,
      totalIncludingVat: money(invoice.totalAmountIncludingVat),
      documentType: String(invoice.invoiceDocumentType ?? ''),
    });
  }
  const equalityAchieved = drafts.length === 0 || drafts.every((row) => row.totalIncludingVat === 0);

  return {
    cancellationCalled: false,
    interimTotalIncludingVat: interim.totalIncludingVat,
    interimTotalExcludingVat: interim.totalExcludingVat,
    profileKwh,
    compensationId: compensation.id,
    volumeBillingRunId,
    equalityAchieved,
    drafts,
  };
}

export async function postPdt3517Cancellation(
  Request: Pdt3517Fx['Request'],
  invoiceNumbers: string[],
  taxEventDate: string | null,
): Promise<{ status: number; processId: number; cancellationId: number; body: Record<string, unknown>; rawText: string }> {
  if (taxEventDate && invoiceNumbers.length === 1) {
    return postCancelByInvoiceNumber(Request, invoiceNumbers[0]!, taxEventDate);
  }
  const invoices = invoiceNumbers.map((value) => cancellationInvoiceToken(value)).join(',');
  const result = await postInvoiceCancellation(Request, {
    invoices,
    taxEventDate: taxEventDate as string,
    fileId: null,
    templateId: Number(envVariables.invoice_cancellation_template),
  });
  return {
    status: result.status,
    processId: Number(result.body.processId ?? 0),
    cancellationId: Number(result.body.invoiceCancellationId ?? result.body.id ?? 0),
    body: result.body,
    rawText: result.rawText,
  };
}

export async function expectCancellationCreated(
  Request: Pdt3517Fx['Request'],
  invoiceNumbers: string[],
  taxEventDate: string | null,
): Promise<{ processId: number; cancellationId: number }> {
  const posted = await postPdt3517Cancellation(Request, invoiceNumbers, taxEventDate);
  expect(posted.status, `POST /invoice-cancellation HTTP 201, body=${posted.rawText}`).toBe(201);
  expect(posted.processId, 'processId').toBeGreaterThan(0);
  expect(posted.cancellationId, 'invoiceCancellationId').toBeGreaterThan(0);
  const finished = await waitProcessFinished(Request, posted.processId);
  expect(String(finished.status), `process ${posted.processId} COMPLETED`).toBe('COMPLETED');
  return { processId: posted.processId, cancellationId: posted.cancellationId };
}

export async function snapshotFinancials(
  Request: Pdt3517Fx['Request'],
  chain: Pick<Pdt3517Chain, 'billedCustomerIdentifier' | 'recipientIdentifier'>,
): Promise<Pdt3517FinancialSnapshot> {
  const [billedL, billedR, recipientL, recipientR] = await Promise.all([
    listLiabilitiesForCustomer(Request, chain.billedCustomerIdentifier),
    listReceivablesForCustomer(Request, chain.billedCustomerIdentifier),
    listLiabilitiesForCustomer(Request, chain.recipientIdentifier),
    listReceivablesForCustomer(Request, chain.recipientIdentifier),
  ]);
  return {
    billedLiabilityIds: billedL.map((row) => row.id),
    billedReceivableIds: billedR.map((row) => row.id),
    recipientLiabilityIds: recipientL.map((row) => row.id),
    recipientReceivableIds: recipientR.map((row) => row.id),
  };
}

function newIds(before: number[], after: number[]): number[] {
  const known = new Set(before);
  return after.filter((id) => !known.has(id));
}

async function pollNewIds(
  Request: Pdt3517Fx['Request'],
  identifier: string,
  kind: 'L' | 'R',
  before: number[],
  minCount: number,
  label: string,
): Promise<number[]> {
  let found: number[] = [];
  await expect
    .poll(
      async () => {
        const listed =
          kind === 'L'
            ? await listLiabilitiesForCustomer(Request, identifier)
            : await listReceivablesForCustomer(Request, identifier);
        found = newIds(
          before,
          listed.map((row) => row.id),
        );
        return found.length;
      },
      { message: label, timeout: 60_000, intervals: [2_000, 5_000] },
    )
    .toBeGreaterThanOrEqual(minCount);
  return found;
}

async function loadDocs(
  Request: Pdt3517Fx['Request'],
  ids: number[],
  kind: 'L' | 'R',
): Promise<MoneyDoc[]> {
  const docs: MoneyDoc[] = [];
  for (const id of ids) {
    if (kind === 'L') {
      const detail = await getCustomerLiability(Request, id);
      docs.push({
        id,
        kind,
        initial: money(detail.initialAmount),
        current: money(detail.currentAmount),
        customerId: extractPartyCustomerId(detail as Record<string, unknown>),
      });
    } else {
      const detail = await getCustomerReceivable(Request, id);
      docs.push({
        id,
        kind,
        initial: money(detail.initialAmount as number),
        current: money(detail.currentAmount as number),
        customerId: extractPartyCustomerId(detail),
      });
    }
  }
  return docs;
}

function pickDoc(docs: MoneyDoc[], label: string, predicate: (doc: MoneyDoc) => boolean): MoneyDoc {
  const hit = docs.find(predicate);
  expect(hit, `${label} not found among ${JSON.stringify(docs)}`).toBeTruthy();
  return hit!;
}

async function offsetReceivableOntoLiability(
  fx: Pdt3517Fx,
  opts: {
    customerId: number;
    liabilityId: number;
    receivableId: number;
    amount: number;
    date: string;
    currencyId: number;
  },
): Promise<void> {
  const customer = await fx.Request.get(`customer/${opts.customerId}`);
  await expect(customer).CheckResponse();
  const customerBody = (await customer.json()) as {
    lastCustomerDetailId?: number;
    communicationData?: Array<{ id?: number }>;
  };
  const customerDetailId = Number(customerBody.lastCustomerDetailId);
  const customerCommunicationDataId = Number(customerBody.communicationData?.[0]?.id);
  expect(customerDetailId, 'customerDetailId').toBeGreaterThan(0);
  expect(customerCommunicationDataId, 'customerCommunicationDataId').toBeGreaterThan(0);

  const templates = await fx.Request.get('template/available', {
    params: {
      templatePurposes: 'MANUAL_LIABILITY_OFFSET',
      type: 'DOCUMENT',
      language: 'BULGARIAN',
      page: 0,
      size: 20,
    },
  });
  await expect(templates).CheckResponse();
  const templateId = Number(
    ((await templates.json()) as { content?: Array<{ id?: number }> }).content?.[0]?.id,
  );
  expect(templateId, 'MANUAL_LIABILITY_OFFSET DOCUMENT template').toBeGreaterThan(0);

  const liability = await getCustomerLiability(fx.Request, opts.liabilityId);
  const occurrenceDate = String((liability as { occurrenceDate?: unknown }).occurrenceDate ?? '').slice(0, 10);
  expect(occurrenceDate, `liability ${opts.liabilityId} occurrenceDate`).toMatch(/^\d{4}-\d{2}-\d{2}$/);
  expect(
    occurrenceDate <= opts.date,
    `liability ${opts.liabilityId} occurrence ${occurrenceDate} must be on or before the offset date ${opts.date}`,
  ).toBe(true);
  const offsetDate = opts.date;
  const receivable = await getCustomerReceivable(fx.Request, opts.receivableId);
  const liabilities = [
    {
      id: opts.liabilityId,
      currency_id: opts.currencyId,
      current_amount: money(liability.currentAmount),
    },
  ];
  const receivables = [
    {
      id: opts.receivableId,
      currency_id: opts.currencyId,
      current_amount: money(receivable.currentAmount as number),
    },
  ];
  const calcRes = await fx.Request.post('manual-liability-offsetting/calculate', {
    data: { liabilities, receivables },
  });
  await expect(calcRes).CheckResponse();
  const calc = (await calcRes.json()) as {
    liabilities?: unknown[];
    receivables?: unknown[];
    deposits?: unknown[];
    payments?: unknown[];
    offsets?: Array<{ offset_amount?: number }>;
  };
  const offsetAmount = money(calc.offsets?.[0]?.offset_amount);
  expect(
    calc.offsets?.length,
    `calculate offsets for liability ${opts.liabilityId} and receivable ${opts.receivableId}, body=${JSON.stringify(calc)}`,
  ).toBeGreaterThan(0);
  expect(
    sameMoney(offsetAmount, opts.amount),
    `manual offset amount must be ${opts.amount}, calculated ${offsetAmount}`,
  ).toBe(true);

  const createRes = await fx.Request.post(fx.Endpoints.manualLiabilityOffsetting, {
    data: {
      date: offsetDate,
      customerId: opts.customerId,
      customerDetailId,
      customerCommunicationDataId,
      liabilities,
      receivables,
      receivedLiabilities: (calc.liabilities ?? []).map((row) => {
        const line = row as { amount?: number; current_amount?: number };
        return { ...(row as object), current_amount: line.amount ?? line.current_amount };
      }),
      receivedReceivables: (calc.receivables ?? []).map((row) => {
        const line = row as { amount?: number; current_amount?: number };
        return { ...(row as object), current_amount: line.amount ?? line.current_amount };
      }),
      receivedDeposits: calc.deposits ?? [],
      receivedPayments: calc.payments ?? [],
      receivedOffsets: calc.offsets ?? [],
      templateIds: [{ templateId, templateType: 'DOCUMENT' }],
    },
  });
  await expect(createRes).CheckResponse();
}

async function putLiabilityPaymentBlock(
  fx: Pdt3517Fx,
  liabilityId: number,
  blocked: boolean,
): Promise<void> {
  const reasonId = Number(envVariables.blocking_reason);
  expect(reasonId, 'envVariables.blocking_reason').toBeGreaterThan(0);
  const payload: Record<string, unknown> = {
    blockedForPayment: blocked,
    blockedForPaymentFromDate: blocked ? '01-01-1990' : null,
    blockedForPaymentToDate: blocked ? '31-12-2090' : null,
    blockedForPaymentReasonId: blocked ? reasonId : null,
  };
  const res = await fx.Request.put(`customer-liability/${liabilityId}`, { data: payload });
  await expect(res).CheckResponse();
  const after = await getCustomerLiability(fx.Request, liabilityId);
  expect(
    Boolean((after as { blockedForPayment?: boolean }).blockedForPayment),
    `liability ${liabilityId} blockedForPayment after PUT blocked=${blocked}`,
  ).toBe(blocked);
}

async function applyUnblockedPaymentOntoSingleLiability(
  fx: Pdt3517Fx,
  opts: {
    customerId: number;
    liabilityId: number;
    amount: number;
    paymentPurpose: string;
    shieldLiabilityIds?: number[];
    invoiceId?: number;
    /** Leave the payment shield on until cancellation, so the reversed payment is not applied to the interim. */
    retainShields?: boolean;
    /** Block the target too, then offset the payment receivable by hand. Keeps the offset manual. */
    manualOnly?: boolean;
  },
): Promise<{ paymentId: number; paymentPackageId: number; offsetCounterpartyId: number }> {
  const shields = [...new Set(opts.shieldLiabilityIds ?? [])].filter(
    (id) => id > 0 && id !== opts.liabilityId,
  );
  for (const liabilityId of shields) {
    await putLiabilityPaymentBlock(fx, liabilityId, true);
  }
  if (opts.manualOnly) {
    await putLiabilityPaymentBlock(fx, opts.liabilityId, true);
  }
  let targetReleased = !opts.manualOnly;
  try {
    const currencyId = Number(envVariables.currency);
    const today = randomGens.generateTodaysDate('yyyy-mm-dd');
    const channelId = await sharedCollectionChannel(fx, currencyId);
    const accountPeriodId = await sharedAccountingPeriodForDate(fx, today);
    const dateCheck = await validatePaymentDate(fx, accountPeriodId, today);
    expect(
      dateCheck.valid,
      `payment date ${today} must fall in accounting period ${accountPeriodId}: ${dateCheck.message ?? ''}`,
    ).toBe(true);
    const paymentPackageId = await sharedUnlockedPaymentPackage(fx, {
      accountingPeriodId: accountPeriodId,
      channelId,
      paymentDateIso: today,
    });
    const created = await fx.Request.post(fx.Endpoints.payment, {
      data: {
        accountPeriodId,
        collectionChannelId: channelId,
        currencyId,
        customerId: opts.customerId,
        initialAmount: opts.amount,
        paymentDate: today,
        paymentPackageId,
        paymentPurpose: opts.paymentPurpose,
        ...(opts.invoiceId
          ? { outgoingDocumentType: 'INVOICE', invoiceId: opts.invoiceId }
          : {}),
      },
    });
    await expect(created).CheckResponse();
    const paymentId = asEntityId(await created.json());
    fx.Responses.payment.push(paymentId);

    const got = await fx.Request.get(`${fx.Endpoints.payment}/${paymentId}`);
    await expect(got).CheckResponse();
    const payment = (await got.json()) as {
      initialAmount?: unknown;
      currentAmount?: unknown;
      offsettingResponseList?: Array<{ id?: unknown; offsettingObjectType?: unknown }>;
    };
    expect(
      sameMoney(money(payment.currentAmount), 0),
      `payment ${paymentId} currentAmount must be 0 after it is parked on a receivable`,
    ).toBe(true);
    expect(sameMoney(money(payment.initialAmount), opts.amount), 'payment initialAmount').toBe(true);
    const rows = payment.offsettingResponseList ?? [];
    const liabilityHit = rows.find(
      (row) =>
        String(row.offsettingObjectType ?? '').toUpperCase().includes('LIABIL') &&
        Number(row.id) === opts.liabilityId,
    );
    if (liabilityHit) {
      expect(opts.manualOnly, `payment ${paymentId} paid liability ${opts.liabilityId} automatically`).toBeFalsy();
      return { paymentId, paymentPackageId, offsetCounterpartyId: paymentId };
    }
    const receivableHit = rows.find((row) =>
      String(row.offsettingObjectType ?? '').toUpperCase().includes('RECEIV'),
    );
    const receivableId = Number(receivableHit?.id);
    expect(
      receivableId,
      `payment ${paymentId} must be offset onto a receivable, rows=${JSON.stringify(payment.offsettingResponseList ?? [])}`,
    ).toBeGreaterThan(0);
    if (opts.manualOnly) {
      await putLiabilityPaymentBlock(fx, opts.liabilityId, false);
      targetReleased = true;
    }
    await offsetReceivableOntoLiability(fx, {
      customerId: opts.customerId,
      liabilityId: opts.liabilityId,
      receivableId,
      amount: opts.amount,
      date: today,
      currencyId,
    });
    return { paymentId, paymentPackageId, offsetCounterpartyId: receivableId };
  } finally {
    if (opts.manualOnly && !targetReleased) {
      await putLiabilityPaymentBlock(fx, opts.liabilityId, false);
    }
    if (!opts.retainShields) {
      for (const liabilityId of shields) {
        await putLiabilityPaymentBlock(fx, liabilityId, false);
      }
    }
  }
}

export async function releaseLiabilityPaymentShields(fx: Pdt3517Fx, liabilityIds: number[]): Promise<void> {
  for (const liabilityId of [...new Set(liabilityIds)].filter((id) => id > 0)) {
    await putLiabilityPaymentBlock(fx, liabilityId, false);
  }
}

export async function offsetPaymentOntoLiability(
  fx: Pdt3517Fx,
  opts: {
    customerId: number;
    liabilityId: number;
    amount: number;
    paymentPurpose: string;
    shieldLiabilityIds?: number[];
    /** When set, the payment is an outgoing document for this invoice so offsetting can pay part of its liability. */
    invoiceId?: number;
    retainShields?: boolean;
    manualOnly?: boolean;
  },
): Promise<{ paymentId: number; paymentPackageId: number; offsetCounterpartyId?: number }> {
  if (opts.shieldLiabilityIds) {
    return applyUnblockedPaymentOntoSingleLiability(fx, opts);
  }
  const currencyId = Number(envVariables.currency);
  const today = randomGens.generateTodaysDate('yyyy-mm-dd');
  const channelId = await sharedCollectionChannel(fx, currencyId);
  const accountPeriodId = await sharedAccountingPeriodForDate(fx, today);
  const dateCheck = await validatePaymentDate(fx, accountPeriodId, today);
  expect(
    dateCheck.valid,
    `payment date ${today} must fall in accounting period ${accountPeriodId}: ${dateCheck.message ?? ''}`,
  ).toBe(true);
  const paymentPackageId = await sharedUnlockedPaymentPackage(fx, {
    accountingPeriodId: accountPeriodId,
    channelId,
    paymentDateIso: today,
  });
  const blockingReasonId = Number(envVariables.blocking_reason);
  expect(blockingReasonId, 'envVariables.blocking_reason').toBeGreaterThan(0);
  const payload = {
    accountPeriodId,
    collectionChannelId: channelId,
    currencyId,
    customerId: opts.customerId,
    initialAmount: opts.amount,
    paymentDate: today,
    paymentPackageId,
    blockedForOffsetting: true,
    blockedForOffsettingFromDate: '1990-01-01',
    blockedForOffsettingToDate: '2090-12-31',
    blockedForOffsettingBlockingReasonId: blockingReasonId,
    blockedForOffsettingAdditionalInfo: opts.paymentPurpose,
    paymentPurpose: opts.paymentPurpose,
  };
  const created = await fx.Request.post(fx.Endpoints.payment, { data: payload });
  await expect(created).CheckResponse();
  const paymentId = asEntityId(await created.json());
  fx.Responses.payment.push(paymentId);

  const got = await fx.Request.get(`${fx.Endpoints.payment}/${paymentId}`);
  await expect(got).CheckResponse();
  const payment = (await got.json()) as { initialAmount?: unknown; currentAmount?: unknown };
  expect(
    sameMoney(money(payment.currentAmount), opts.amount),
    `payment ${paymentId} currentAmount must equal initialAmount ${opts.amount} while blocked for offsetting`,
  ).toBe(true);
  expect(sameMoney(money(payment.initialAmount), opts.amount), 'payment initialAmount').toBe(true);

  const customer = await fx.Request.get(`customer/${opts.customerId}`);
  await expect(customer).CheckResponse();
  const customerBody = (await customer.json()) as {
    lastCustomerDetailId?: number;
    communicationData?: Array<{ id?: number }>;
  };
  const customerDetailId = Number(customerBody.lastCustomerDetailId);
  const customerCommunicationDataId = Number(customerBody.communicationData?.[0]?.id);
  expect(customerDetailId, 'customerDetailId').toBeGreaterThan(0);
  expect(customerCommunicationDataId, 'customerCommunicationDataId').toBeGreaterThan(0);

  const manual = {
    date: today,
    customerId: opts.customerId,
    customerDetailId,
    customerCommunicationDataId,
    liabilities: [{ id: opts.liabilityId, currency_id: currencyId, current_amount: opts.amount }],
    payments: [{ id: paymentId, currency_id: currencyId, current_amount: opts.amount }],
  };
  const posted = await fx.Request.post(fx.Endpoints.manualLiabilityOffsetting, { data: manual });
  await expect(posted).CheckResponse();
  return { paymentId, paymentPackageId };
}

export async function executeReschedulingOnGovernmentLiability(
  fx: Pdt3517Fx,
  chain: Pdt3517Chain,
): Promise<number> {
  expect(chain.govLiabilityId, 'positive compensation government liability').toBeTruthy();
  return createExecutedReschedulingForLiability(fx, {
    customerId: chain.recipientId,
    liabilityId: chain.govLiabilityId as number,
  });
}

export async function assertDateOutsideOpenPeriods(Request: Pdt3517Fx['Request'], isoDate: string): Promise<void> {
  const open = await listAccountingPeriodsByStatus(Request, 'OPEN', 50);
  expect(open.length, 'at least one OPEN accounting period exists').toBeGreaterThan(0);
  for (const period of open) {
    const start = period.startDate.slice(0, 10);
    const end = period.endDate.slice(0, 10);
    if (start.length < 10 || end.length < 10) continue;
    expect(
      isoDate >= start && isoDate <= end,
      `${isoDate} must not sit inside OPEN period ${period.id} ${start}..${end}`,
    ).toBe(false);
  }
}

async function assertInterimOutcomes(
  Request: Pdt3517Fx['Request'],
  chain: Pdt3517Chain,
  before: Pdt3517FinancialSnapshot,
  cancelInterimIndexes: number[],
  billedReceivables: MoneyDoc[],
): Promise<void> {
  for (let index = 0; index < chain.interims.length; index++) {
    const interim = chain.interims[index]!;
    const invoice = await getInvoice(Request, interim.id);
    const liability = await getCustomerLiability(Request, interim.liabilityId);
    if (cancelInterimIndexes.includes(index)) {
      expect(invoice.invoiceStatus, `listed interim ${interim.number} CANCELLED`).toBe('CANCELLED');
      expect(sameMoney(money(liability.currentAmount), 0), `interim ${interim.number} liability closed`).toBe(true);
      const closer = pickDoc(
        billedReceivables,
        `cancellation receivable for interim ${interim.number} amount ${interim.initialAmount}`,
        (doc) => doc.kind === 'R' && sameMoney(doc.initial, interim.initialAmount) && sameMoney(doc.current, 0),
      );
      expect(closer.customerId, 'interim cancellation receivable customer').toBe(chain.billedCustomerId);
      await assertAmountZero(Request, 'receivable', closer.id, `interim ${interim.number} cancellation receivable`);
      await assertAmountZero(Request, 'liability', interim.liabilityId, `interim ${interim.number} liability`);
    } else {
      expect(invoice.invoiceStatus, `unlisted interim ${interim.number} stays REAL`).toBe('REAL');
      await assertInterimDeducted(
        Request,
        invoice,
        chain.remainingInvoiceId,
        false,
        `unlisted interim ${interim.number} is no longer deducted`,
      );
      expect(
        sameMoney(money(liability.currentAmount), interim.initialAmount),
        `unlisted interim ${interim.number} liability currentAmount stays ${interim.initialAmount}`,
      ).toBe(true);
      const stray = billedReceivables.filter(
        (doc) => doc.kind === 'R' && sameMoney(doc.initial, interim.initialAmount) && !before.billedReceivableIds.includes(doc.id),
      );
      expect(stray, `no cancellation receivable for unlisted interim ${interim.number}`).toEqual([]);
    }
  }
}

export async function assertCompensationUnwind(
  Request: Pdt3517Fx['Request'],
  chain: Pdt3517Chain,
  before: Pdt3517FinancialSnapshot,
  opts: { cancelInterimIndexes: number[]; paymentId?: number; paymentLiabilityId?: number },
): Promise<string> {
  const remaining = await getInvoice(Request, chain.remainingInvoiceId);
  expect(remaining.invoiceStatus, 'remaining invoice CANCELLED').toBe('CANCELLED');
  assertGcUninvoicedEmptyFinancials(await getCompensation(Request, chain.compensationId), chain.compensationNumber);

  const billedLiabilityIds = await pollNewIds(
    Request,
    chain.billedCustomerIdentifier,
    'L',
    before.billedLiabilityIds,
    chain.side === 'credit' ? (chain.sign === 'positive' ? 2 : 1) : chain.sign === 'positive' ? 1 : 0,
    'new billed liabilities',
  );
  const billedReceivableMin =
    (chain.side === 'debit' ? 1 : 0) +
    (chain.sign === 'negative' ? 1 : 0) +
    opts.cancelInterimIndexes.length;
  const billedReceivableIds =
    billedReceivableMin === 0
      ? newIds(
          before.billedReceivableIds,
          (await listReceivablesForCustomer(Request, chain.billedCustomerIdentifier)).map((row) => row.id),
        )
      : await pollNewIds(
          Request,
          chain.billedCustomerIdentifier,
          'R',
          before.billedReceivableIds,
          billedReceivableMin,
          'new billed receivables',
        );
  const recipientLiabilityIds =
    chain.sign === 'negative'
      ? await pollNewIds(
          Request,
          chain.recipientIdentifier,
          'L',
          before.recipientLiabilityIds,
          1,
          'new government liability',
        )
      : newIds(
          before.recipientLiabilityIds,
          (await listLiabilitiesForCustomer(Request, chain.recipientIdentifier)).map((row) => row.id),
        );
  const recipientReceivableIds =
    chain.sign === 'positive'
      ? await pollNewIds(
          Request,
          chain.recipientIdentifier,
          'R',
          before.recipientReceivableIds,
          1,
          'new government receivable',
        )
      : newIds(
          before.recipientReceivableIds,
          (await listReceivablesForCustomer(Request, chain.recipientIdentifier)).map((row) => row.id),
        );

  const billedLiabilities = await loadDocs(Request, billedLiabilityIds, 'L');
  const billedReceivables = await loadDocs(Request, billedReceivableIds, 'R');
  const recipientLiabilities = await loadDocs(Request, recipientLiabilityIds, 'L');
  const recipientReceivables = await loadDocs(Request, recipientReceivableIds, 'R');

  if (chain.side === 'debit') {
    const liability = await getCustomerLiability(Request, chain.remainingLiabilityId as number);
    expect(sameMoney(money(liability.currentAmount), 0), 'remaining liability currentAmount 0').toBe(true);
    await assertAmountZero(Request, 'liability', chain.remainingLiabilityId, 'remaining liability');
    if (chain.sign === 'positive') {
      assertOffsetReversed(
        liability as Record<string, unknown>,
        chain.compReceivableId as number,
        'offsetting between remaining liability and compensation receivable',
      );
    } else {
      assertNoOffsetCounterparty(
        liability as Record<string, unknown>,
        chain.extraLiabilityId as number,
        'remaining liability vs extra liability',
      );
    }
    if (opts.paymentId && opts.paymentLiabilityId === chain.remainingLiabilityId) {
      assertOffsetReversed(liability as Record<string, unknown>, opts.paymentId, 'payment offset on remaining liability');
    }
    const cancellationReceivable = pickDoc(
      billedReceivables,
      `cancellation receivable initialAmount R ${chain.R}`,
      (doc) => sameMoney(doc.initial, chain.R) && sameMoney(doc.current, 0),
    );
    expect(cancellationReceivable.customerId, 'cancellation receivable customer is the billed customer').toBe(
      chain.billedCustomerId,
    );
    await assertAmountZero(Request, 'receivable', cancellationReceivable.id, 'remaining cancellation receivable');
  } else {
    const receivable = await getCustomerReceivable(Request, chain.remainingReceivableId as number);
    expect(sameMoney(money(receivable.currentAmount as number), 0), 'remaining receivable currentAmount 0').toBe(true);
    await assertAmountZero(Request, 'receivable', chain.remainingReceivableId, 'remaining receivable');
    if (chain.sign === 'negative') {
      assertOffsetReversed(
        receivable,
        chain.extraLiabilityId as number,
        'offsetting between remaining credit and extra liability',
      );
    } else {
      assertNoOffsetCounterparty(
        receivable,
        chain.compReceivableId as number,
        'remaining credit vs compensation receivable',
      );
    }
    const cancellationLiability = pickDoc(
      billedLiabilities,
      `cancellation liability initialAmount R ${chain.R}`,
      (doc) => sameMoney(doc.initial, chain.R) && sameMoney(doc.current, 0),
    );
    expect(cancellationLiability.customerId, 'cancellation liability customer is the billed customer').toBe(
      chain.billedCustomerId,
    );
    await assertAmountZero(Request, 'liability', cancellationLiability.id, 'remaining cancellation liability');
  }

  if (chain.sign === 'positive') {
    await assertAmountZero(Request, 'receivable', chain.compReceivableId, 'compensation receivable');
    await assertAmountZero(Request, 'liability', chain.govLiabilityId, 'government liability');
    const closer = pickDoc(
      billedLiabilities,
      `new customer liability initialAmount C ${chain.C}`,
      (doc) => sameMoney(doc.initial, chain.C) && sameMoney(doc.current, 0),
    );
    expect(
      closer.customerId,
      'new customer liability that closes the compensation receivable belongs to the billed customer',
    ).toBe(chain.billedCustomerId);
    await assertAmountZero(Request, 'liability', closer.id, 'compensation-closing customer liability');
    const governmentReceivable = pickDoc(
      recipientReceivables,
      `new government receivable initialAmount C ${chain.C}`,
      (doc) => sameMoney(doc.initial, chain.C) && sameMoney(doc.current, 0),
    );
    expect(governmentReceivable.customerId, 'new government receivable belongs to the recipient').toBe(
      chain.recipientId,
    );
    await assertAmountZero(Request, 'receivable', governmentReceivable.id, 'government receivable');
  } else {
    await assertAmountZero(Request, 'liability', chain.extraLiabilityId, 'extra customer liability');
    await assertAmountZero(Request, 'receivable', chain.govReceivableId, 'government receivable');
    const closer = pickDoc(
      billedReceivables,
      `new customer receivable initialAmount C ${chain.C}`,
      (doc) => sameMoney(doc.initial, chain.C) && sameMoney(doc.current, 0) && !sameMoney(doc.initial, chain.R),
    );
    expect(closer.customerId, 'new customer receivable that closes the extra liability belongs to the billed customer').toBe(
      chain.billedCustomerId,
    );
    await assertAmountZero(Request, 'receivable', closer.id, 'extra-liability closing receivable');
    const governmentLiability = pickDoc(
      recipientLiabilities,
      `new government liability initialAmount C ${chain.C}`,
      (doc) => sameMoney(doc.initial, chain.C) && sameMoney(doc.current, 0),
    );
    expect(governmentLiability.customerId, 'new government liability belongs to the recipient').toBe(chain.recipientId);
    await assertAmountZero(Request, 'liability', governmentLiability.id, 'government liability');
    if (opts.paymentId && opts.paymentLiabilityId === chain.extraLiabilityId) {
      // Finding F6: origin/dev settleCompensationsOnCancellation does not reverse manual
      // offsetting on the compensation liability. This assertion is the change description
      // and is expected to fail until that code is fixed.
      const extra = await getCustomerLiability(Request, chain.extraLiabilityId as number);
      assertOffsetReversed(
        extra as Record<string, unknown>,
        opts.paymentId,
        'manual payment offset on the extra customer liability',
      );
    }
  }

  if (opts.paymentId && opts.paymentLiabilityId && chain.interims.some((row) => row.liabilityId === opts.paymentLiabilityId)) {
    const paid = await getCustomerLiability(Request, opts.paymentLiabilityId);
    assertOffsetReversed(paid as Record<string, unknown>, opts.paymentId, 'payment offset on the interim liability');
  }

  await assertInterimOutcomes(Request, chain, before, opts.cancelInterimIndexes, billedReceivables);
  return (
    `remaining=${remaining.invoiceStatus} doc=${chain.remainingDocumentType} R=${chain.R} C=${chain.C} ` +
    `compensation=UNINVOICED cancelledInterims=${opts.cancelInterimIndexes.join(',') || 'none'}`
  );
}

export async function assertCancellationRejected(
  Request: Pdt3517Fx['Request'],
  chain: Pdt3517Chain,
  before: Pdt3517FinancialSnapshot,
  reportText: string,
  fragments: string[],
): Promise<string> {
  for (const fragment of fragments) {
    expect(reportText, `process report contains "${fragment}"`).toContain(fragment);
  }
  const remaining = await getInvoice(Request, chain.remainingInvoiceId);
  expect(remaining.invoiceStatus, 'remaining invoice stays REAL').toBe('REAL');
  const compensation = await getCompensation(Request, chain.compensationId);
  expect(compensation.compensationStatus, 'compensation stays INVOICED').toBe('INVOICED');
  expect(shortId(compensation, 'receivableForCustomer'), 'receivableForCustomer unchanged').toBe(chain.compReceivableId);
  expect(shortId(compensation, 'liabilityForRecipient'), 'liabilityForRecipient unchanged').toBe(chain.govLiabilityId);
  expect(shortId(compensation, 'liabilityForCustomer'), 'liabilityForCustomer stays empty').toBeNull();
  expect(shortId(compensation, 'receivableForRecipient'), 'receivableForRecipient stays empty').toBeNull();

  if (chain.remainingLiabilityId) {
    const liability = await getCustomerLiability(Request, chain.remainingLiabilityId);
    expect(
      sameMoney(money(liability.currentAmount), chain.remainingCurrentBefore),
      'remaining liability currentAmount unchanged',
    ).toBe(true);
  }
  if (chain.compReceivableId) {
    await assertAmountZero(Request, 'receivable', chain.compReceivableId, 'compensation receivable stays offset');
  }

  const interimNotes: string[] = [];
  for (const interim of chain.interims) {
    const invoice = await getInvoice(Request, interim.id);
    expect(invoice.invoiceStatus, `interim ${interim.number} stays REAL`).toBe('REAL');
    // Finding F1 (TC-BE-17): origin/dev may cancel a listed interim in parallel while the
    // remaining invoice is rejected. The change description still requires the interim to stay
    // REAL and deducted. The REAL assertion above is expected to fail on origin/dev until that is fixed.
    await assertInterimDeducted(
      Request,
      invoice,
      chain.remainingInvoiceId,
      true,
      `interim ${interim.number} stays deducted`,
    );
    const liability = await getCustomerLiability(Request, interim.liabilityId);
    expect(
      sameMoney(money(liability.currentAmount), interim.initialAmount),
      `interim ${interim.number} liability currentAmount stays I`,
    ).toBe(true);
    interimNotes.push(`${interim.number}:REAL deducted`);
  }

  const billedReceivables = await listReceivablesForCustomer(Request, chain.billedCustomerIdentifier);
  const fresh = newIds(
    before.billedReceivableIds,
    billedReceivables.map((row) => row.id),
  );
  expect(fresh, 'no new cancellation receivable').toEqual([]);
  return `remaining=REAL compensation=INVOICED interims=${interimNotes.join('; ')} reportOk`;
}
