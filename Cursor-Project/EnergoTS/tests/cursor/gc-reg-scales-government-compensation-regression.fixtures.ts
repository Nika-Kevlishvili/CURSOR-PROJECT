/**
 * GC-REG-SCALES — Government Compensation process regression with billing data by scales.
 *
 * Reference spec(s):
 * - tests/cursor/GC-REG-government-compensation-process.spec.ts / gc-reg-government-compensation-regression.fixtures.ts
 * - tests/cursor/pdt-3223-invoice-json-tablepcscales-listpc-order.fixtures.ts (SLP POD + meter + BY_SCALES PC)
 * - tests/cursor/phn-4048-billing-data-by-scales-create-and-edit.fixtures.ts (POST /billing-by-scales HTTP 201)
 * - tests/cursor/PDT-3087-government-compensation-defer-to-pdf.fixtures.ts (FOR_VOLUMES billing run)
 *
 * Swagger: update-swagger-specs.ps1 failed this session (hosts unreachable); field names taken from cached config/swagger/dev/swagger-spec.json.
 * Backend TC: Cursor-Project/test_cases/Backend/Government_Compensation_Regression_Data_By_Scales.md
 *
 * Volume source: POST /billing-by-scales (not POST /billing-by-profile).
 * Billing run stays STANDARD_BILLING + FOR_VOLUMES + CONTRACT.
 */

import { test, expect } from '../../fixtures/baseFixture';
import type { baseFixture } from '../../fixtures/baseFixture';
import { envVariables } from '../../fixtures/envCashed';
import { randomGens } from '../../utils/randomGens';
import {
  DD_IBAN,
  DD_IBAN_INVOICE_CUSTOMER,
  DD_IBAN_INSTITUTION,
  DD_IBAN_INVOICE_ONLY,
  type GcDdPrechainResult,
  type GcRegFx,
  type DevVolCompPrechainResult,
} from './gc-reg-government-compensation-regression.fixtures';
import {
  PHN_4048_GRID_OPERATOR,
  createHeader,
  scaleCodeRow,
  postBillingByScales,
  type Phn4048Chain,
  type Phn4048Fx,
} from './phn-4048-billing-data-by-scales-create-and-edit.fixtures';

export const GC_REG_SCALES_KEY = 'GC-REG-SCALES';

export type GcRegScalesFx = GcRegFx & Pick<baseFixture, 'Nomenclatures'>;

export type { DevVolCompPrechainResult };

function asEntityId(raw: unknown): number {
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

function asPriceComponentId(stored: unknown): number {
  if (typeof stored === 'number' && Number.isFinite(stored) && stored > 0) return stored;
  if (stored !== null && typeof stored === 'object' && 'id' in stored) {
    const n = Number((stored as { id: unknown }).id);
    if (Number.isFinite(n) && n > 0) return n;
  }
  throw new Error('POST /price-components must return a numeric id or { id }.');
}

/** Previous calendar month (TC documentPeriod / BBS dateFrom–dateTo). */
export function previousCalendarMonthWindow(): { periodFrom: string; periodTo: string } {
  const now = new Date();
  const y = now.getUTCFullYear();
  const m = now.getUTCMonth();
  const prev = m === 0 ? { y: y - 1, m: 11 } : { y, m: m - 1 };
  const lastDay = new Date(Date.UTC(prev.y, prev.m + 1, 0)).getUTCDate();
  const mm = String(prev.m + 1).padStart(2, '0');
  return {
    periodFrom: `${prev.y}-${mm}-01`,
    periodTo: `${prev.y}-${mm}-${String(lastDay).padStart(2, '0')}`,
  };
}

function applyDirectDebitToLegalPayload(payload: Record<string, unknown>, iban: string): void {
  const banking = (payload.bankingDetails ?? {}) as Record<string, unknown>;
  banking.directDebit = true;
  banking.bankId = banking.bankId ?? envVariables.banks;
  banking.iban = iban;
  payload.bankingDetails = banking;
}

async function loadScaleCodeFields(
  Request: GcRegScalesFx['Request'],
  scaleId: number,
): Promise<{ scaleCode: string; scaleType: string }> {
  const maxAttempts = 4;
  let lastError: unknown;
  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    try {
      const res = await Request.get(`scales/${scaleId}`);
      await expect(res, `GET /scales/${scaleId}`).CheckResponse();
      const body = (await res.json()) as { scaleCode?: string | null; scaleType?: string | null };
      const scaleCode = String(body.scaleCode ?? '').trim();
      const scaleType = String(body.scaleType ?? '').trim();
      expect(scaleCode, 'scaleCode nomenclature').toBeTruthy();
      expect(scaleType, 'scaleType nomenclature').toBeTruthy();
      return { scaleCode, scaleType };
    } catch (error) {
      lastError = error;
      const message = String((error as Error)?.message ?? error ?? '');
      const retryable = /ECONNRESET|ETIMEDOUT|ECONNREFUSED|socket hang up/i.test(message);
      if (!retryable || attempt === maxAttempts) {
        throw error;
      }
      await new Promise((r) => setTimeout(r, 400 * attempt));
    }
  }
  throw lastError;
}

/**
 * FOR_VOLUMES contract + SLP POD + meter + POST /billing-by-scales (filled scaleCode row).
 * Does not POST /billing-by-profile.
 */
export async function runGcScalesContractAndBbsPrechain(
  ctx: GcRegScalesFx,
  options?: {
    withDirectDebit?: boolean;
    invoiceCustomerIban?: string;
    /** `null` = government institution created without Direct Debit. */
    institutionIban?: string | null;
  },
): Promise<DevVolCompPrechainResult & { invoiceCustomerIban?: string; institutionIban?: string | null }> {
  const { Request, GeneratePayload, Responses, Endpoints, Nomenclatures } = ctx;
  const withDd = options?.withDirectDebit === true;
  const invoiceCustomerIban = withDd ? (options?.invoiceCustomerIban ?? DD_IBAN) : undefined;
  const institutionIban = withDd
    ? (options && 'institutionIban' in options ? options.institutionIban : DD_IBAN)
    : undefined;
  const period = previousCalendarMonthWindow();
  const phnFx = ctx as unknown as Phn4048Fx;

  await test.step('Precondition: billed customer (LEGAL)', async () => {
    const payload = GeneratePayload.customers.customer_legal() as Record<string, unknown>;
    if (invoiceCustomerIban) applyDirectDebitToLegalPayload(payload, invoiceCustomerIban);
    const customer = await Request.post(Endpoints.customer, { data: payload });
    await expect(customer).CheckResponse();
    Responses.customer.push(await customer.json());
  });

  await test.step('Precondition: recipient customer (LEGAL, different)', async () => {
    const payload = GeneratePayload.customers.customer_legal() as Record<string, unknown>;
    if (institutionIban) applyDirectDebitToLegalPayload(payload, institutionIban);
    const recipient = await Request.post(Endpoints.customer, { data: payload });
    await expect(recipient).CheckResponse();
    Responses.customer.push(await recipient.json());
  });

  const gridOperatorId = Number(await Nomenclatures.grid_operator(PHN_4048_GRID_OPERATOR));
  expect(gridOperatorId, 'grid operator GIO').toBeGreaterThan(0);
  const measurementTypeId = Number(
    await Nomenclatures.measurement_type(PHN_4048_GRID_OPERATOR, gridOperatorId as unknown as string),
  );
  const scaleCodeId = Number(await Nomenclatures.scales_code('scaleCode'));
  expect(scaleCodeId, 'scales_code id').toBeGreaterThan(0);
  const { scaleCode, scaleType } = await loadScaleCodeFields(Request, scaleCodeId);

  await test.step('Precondition: terms', async () => {
    const term = await Request.post(Endpoints.terms, {
      data: GeneratePayload.productAndServices.term(),
    });
    await expect(term).CheckResponse();
    Responses.terms.push(await term.json());
  });

  await test.step('Precondition: BY_SCALES price component', async () => {
    const payload = GeneratePayload.productAndServices.scaleComponent();
    payload.name = `GC-SCALES-${randomGens.generateRandomString(true, false, 8)}`;
    payload.formulaRequest.expression = '100';
    payload.formulaRequest.issuedSeparateInvoice = 'INVOICE_ONE';
    payload.applicationModelRequest.applicationType = 'BY_SCALES';
    payload.applicationModelRequest.volumesByScaleRequest.scaleIds = [scaleCodeId];
    payload.doNotIncludeVatBase = false;
    const price = await Request.post(Endpoints.priceComponent, { data: payload });
    await expect(price).CheckResponse();
    Responses.priceComponent.push(asPriceComponentId(await price.json()));
  });

  await test.step('Precondition: product SUPPLY_ONLY', async () => {
    const product = await Request.post(Endpoints.product, {
      data: GeneratePayload.productAndServices.product(),
    });
    await expect(product).CheckResponse();
    Responses.product.push(await product.json());
  });

  let podIdentifier = '';
  let meterNumber = '';
  await test.step('Precondition: SLP POD + meter with meterScales', async () => {
    const podPayload = GeneratePayload.pointsOfDelivery.pod_slp();
    podPayload.gridOperatorId = gridOperatorId;
    podPayload.measurementTypeId = measurementTypeId;
    const podRes = await Request.post(Endpoints.pod, { data: podPayload });
    await expect(podRes).CheckResponse();
    const podCreated = (await podRes.json()) as { id?: number };
    const podId = asEntityId(podCreated);
    podIdentifier = String(podPayload.identifier ?? '').trim();
    expect(podIdentifier, 'pod_slp identifier').toBeTruthy();
    Responses.pod.push({
      ...podCreated,
      id: podId,
      identifier: podIdentifier,
      gridOperatorId,
      type: podPayload.type ?? 'CONSUMER',
      estimatedMonthlyAvgConsumption: Number(podPayload.estimatedMonthlyAvgConsumption ?? 0),
    } as (typeof Responses.pod)[number]);

    const meterPayload = GeneratePayload.pointsOfDelivery.meters() as unknown as {
      number?: string;
      gridOperatorId: number | null;
      podId: number | null;
      installmentDate?: string;
      removeDate?: string;
      meterScales: number[];
    };
    meterPayload.gridOperatorId = gridOperatorId;
    meterPayload.podId = podId;
    meterPayload.meterScales = [scaleCodeId];
    meterPayload.installmentDate = `${period.periodFrom.slice(0, 4)}-01-01`;
    meterPayload.removeDate = `${Number(period.periodFrom.slice(0, 4)) + 1}-12-31`;
    const meterRes = await Request.post(Endpoints.meters, { data: meterPayload });
    await expect(meterRes).CheckResponse();
    const meterBody = (await meterRes.json()) as { id?: number; number?: string };
    const meterId = asEntityId(meterBody.id ?? meterBody);
    meterNumber = String(meterPayload.number ?? meterBody.number ?? '').trim();
    expect(meterNumber, 'meter number').toBeTruthy();
    Responses.meters.push({ id: meterId, number: meterNumber });
  });

  let contractId = 0;
  await test.step('Precondition: product contract', async () => {
    const contractPayload = (await GeneratePayload.contractsAndOrders.product_contract()) as Record<
      string,
      unknown
    >;
    if (invoiceCustomerIban) {
      const additional = (contractPayload.additionalParameters ?? {}) as Record<string, unknown>;
      const banking = (additional.bankingDetails ?? {}) as Record<string, unknown>;
      banking.directDebit = true;
      banking.bankId = banking.bankId ?? envVariables.banks;
      banking.iban = invoiceCustomerIban;
      additional.bankingDetails = banking;
      contractPayload.additionalParameters = additional;
    }
    let monthlySum = 0;
    for (const pod of Responses.pod) {
      const row = pod as { type?: string; estimatedMonthlyAvgConsumption?: number };
      if ((row.type ?? 'CONSUMER') === 'CONSUMER') {
        monthlySum += Number(row.estimatedMonthlyAvgConsumption ?? 0);
      }
    }
    const additional = (contractPayload.additionalParameters ?? {}) as Record<string, unknown>;
    additional.estimatedTotalConsumptionUnderContractKwh = (monthlySum * 12) / 1000;
    contractPayload.additionalParameters = additional;
    const contract = await Request.post(Endpoints.productContract, { data: contractPayload });
    await expect(contract).CheckResponse();
    const body = await contract.json();
    Responses.productContract.push(body);
    contractId = asEntityId(body);
  });

  await test.step('Precondition: activate POD on contract', async () => {
    const podActivation = await Request.post('/contract-pods/manual', {
      data: await GeneratePayload.pointsOfDelivery.pod_activation(0, period.periodFrom),
    });
    await expect(podActivation).CheckResponse();
  });

  let bbsId = 0;
  await test.step('Precondition: POST /billing-by-scales filled scaleCode row', async () => {
    const chain: Phn4048Chain = {
      tag: 'GC-REG-SCALES',
      unique: randomGens.generateUniqueIdentifier(),
      gridOperatorId,
      scaleCodeId,
      tariffScaleId: 0,
      scaleCode,
      scaleType,
      tariffScale: '',
      tariffScaleType: '',
      podId: asEntityId(Responses.pod[0]),
      identifier: podIdentifier,
      meterId: asEntityId(Responses.meters[0]),
      meterNumber,
    };
    const invoiceNumber = `GCSC${randomGens.generateRandomString(true, false, 8)}`;
    const row = scaleCodeRow(chain, {
      periodFrom: period.periodFrom,
      periodTo: period.periodTo,
      meterNumber,
      scaleCode,
      scaleType,
      newMeterReading: 5,
      oldMeterReading: 0,
      difference: 5,
      multiplier: 8,
      totalVolumes: 40,
      index: 0,
    });
    const payload = createHeader(podIdentifier, invoiceNumber, [row], {
      dateFrom: period.periodFrom,
      dateTo: period.periodTo,
      invoiceDate: `${period.periodFrom}T00:00:00.000Z`,
      billingPowerInKw: 2,
      correction: false,
      override: false,
      saveRecordForIntermediatePeriod: true,
      saveRecordForMeterReadings: true,
    });
    bbsId = await postBillingByScales(phnFx, payload);
    expect(bbsId, 'POST /billing-by-scales id').toBeGreaterThan(0);
  });

  const customerId = asEntityId(Responses.customer[0]);
  const recipientCustomerId = asEntityId(Responses.customer[1]);
  expect(recipientCustomerId, 'recipient must differ from billed customer').not.toBe(customerId);

  return {
    customerId,
    recipientCustomerId,
    podId: asEntityId(Responses.pod[0]),
    contractId,
    bbpId: bbsId,
    periodFrom: period.periodFrom,
    periodTo: period.periodTo,
    documentPeriod: period.periodFrom,
    invoiceCustomerIban,
    institutionIban: institutionIban ?? null,
  };
}

export async function runGcScalesBillingPrechainWithDirectDebit(
  ctx: GcRegScalesFx,
): Promise<DevVolCompPrechainResult> {
  return runGcScalesContractAndBbsPrechain(ctx, { withDirectDebit: true });
}

export async function runGcScalesBillingPrechainDistinctInstitutionDd(
  ctx: GcRegScalesFx,
): Promise<GcDdPrechainResult> {
  return runGcScalesContractAndBbsPrechain(ctx, {
    withDirectDebit: true,
    invoiceCustomerIban: DD_IBAN_INVOICE_CUSTOMER,
    institutionIban: DD_IBAN_INSTITUTION,
  }) as Promise<GcDdPrechainResult>;
}

export async function runGcScalesBillingPrechainInvoiceDdInstitutionNone(
  ctx: GcRegScalesFx,
): Promise<GcDdPrechainResult> {
  return runGcScalesContractAndBbsPrechain(ctx, {
    withDirectDebit: true,
    invoiceCustomerIban: DD_IBAN_INVOICE_ONLY,
    institutionIban: null,
  }) as Promise<GcDdPrechainResult>;
}

export {
  ZERO_AMOUNT_ERROR,
  EDIT_LOCKED_ERROR,
  REGEN_NOT_REAL_ERROR,
  REGEN_NULL_INDEX_ERROR,
  DEV_PORTAL_BASE,
  runGcCreatePrechain,
  createGcExpectingCreated,
  completeForVolumesToAccounted,
  createForVolumesDraftBillingRun,
  postGcRaw,
  putGcRaw,
  patchRegenerateCompensationsOk,
  patchRegenerateCompensationsRaw,
  listCompensationsByNumber,
  getCompensation,
  getInvoice,
  getCustomerLiability,
  getCustomerReceivable,
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
  ZERO_AMOUNT_WIKI_EN,
  ZERO_AMOUNT_WIKI_BG,
  NON_NUMERIC_DOCUMENT_NUMBER,
  getCustomerById,
  isEmptyOutgoingExternal,
  calendarDatePart,
  pickInvoiceIncomeAccount,
  pickInvoiceCostCenter,
  pickLiabilityIban,
  pickReceivableBankAccount,
  buildDevBillingRunPreviewLink,
  buildDevCompensationPreviewLink,
  buildDevInvoicePreviewLink,
} from './gc-reg-government-compensation-regression.fixtures';
