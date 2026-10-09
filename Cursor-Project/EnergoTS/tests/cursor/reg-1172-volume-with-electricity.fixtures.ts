/**
 * REG-1172 — multi-POD volume + WITH_ELECTRICITY_INVOICE; deactivated POD before meter reading "to" excluded.
 *
 * Business rule (day-level): electricity PC when `deactivationDate >= meterReadingTo` or deactivation is null.
 * Dates are derived from OPEN accounting period anchor (not fixed calendar literals).
 *
 * Dev2:
 * - `BASE_URL=https://devapps.energo-pro.bg/backend/phoenix-dev2`
 * - Run `npx playwright test --project=setup` once per env switch (refreshes `fixtures/envVariables.json`)
 * - `prepareReg1172LegalCustomerPayload` patches MAIL+coordinates payload for dev2 portal rules
 *
 * Reference spec(s):
 * - tests/billing/electricity/withElectricity(Product).spec.ts — [REG-991] electricity happy pass (primary)
 * - tests/cursor/pdt-2376-volume-with-electricity.fixtures.ts — billing anchor, POD matrix activation, detailed-data match
 * - tests/cursor/PHN-2130-sales-portal-contract-update.spec.ts (dev2 CONTRACT/BILLING comm entries)
 */

import { expect } from '../../fixtures/baseFixture';
import type { baseFixture } from '../../fixtures/baseFixture';
import { envVariables } from '../../fixtures/envCashed';
import { configuredBaseURL } from '../../fixtures/utils/baseUrl';
import {
  applyPdt2376PodActivationMatrix,
  buildPdt2376ElectricityMatchContext,
  fetchAggregatedInvoiceDetailedData,
  getMatchedElectricityRowsForPodCandidates,
  hydratePdt2376ElectricityMatchContextIfBareId,
  listInvoiceIdsForBillingRun,
  normalizeBillingRunId,
  resolveAttachmentPeriodBounds,
  resolvePdt2376BillingAnchor,
  resolvePdt2376ElectricityPriceComponentId,
  summarizeMatchedRowsBrief,
  type Pdt2376BillingAnchor,
  type Pdt2376ElectricityMatchContext,
  type Pdt2376PodElectricityDetailedAttachmentEntry,
  type Pdt2376PodMatrixRow,
} from './pdt-2376-volume-with-electricity.fixtures';

type FixtureRequest = baseFixture['Request'];
type ProductContractPayload = Awaited<
  ReturnType<baseFixture['GeneratePayload']['contractsAndOrders']['product_contract']>
>;

export const REG_1172_DEV2_BASE_URL = 'https://devapps.energo-pro.bg/backend/phoenix-dev2';

export function isReg1172Dev2Target(): boolean {
  const raw = (configuredBaseURL || process.env.BASE_URL || '').toLowerCase();
  return raw.includes('phoenix-dev2') || raw.includes('/dev2');
}

type CommEntry = Record<string, unknown>;

function cloneAddressWithoutCoordinates(template: CommEntry): Record<string, unknown> {
  const address = JSON.parse(JSON.stringify(template.address ?? {})) as Record<string, unknown>;
  delete address.latitude;
  delete address.longitude;
  return address;
}

function buildReg1172CommEntry(
  template: CommEntry,
  purposeId: number,
  label: string,
  email: string,
): CommEntry {
  return {
    status: 'ACTIVE',
    contactTypeName: label,
    contactPurposes: [{ contactPurposeId: purposeId, status: 'ACTIVE' }],
    address: cloneAddressWithoutCoordinates(template),
    communicationContacts: [
      {
        sendSms: false,
        platformId: envVariables.platform,
        status: 'ACTIVE',
        contactType: 'EMAIL',
        contactValue: email,
      },
    ],
    contactPersons: [],
  };
}

/** Dev2 rejects MAIL + lat/long on create; split CONTRACT/BILLING comm rows like PHN-2130. */
export function prepareReg1172LegalCustomerPayload(
  rawPayload: Record<string, unknown>,
): Record<string, unknown> {
  if (!isReg1172Dev2Target()) {
    return rawPayload;
  }
  const template = Array.isArray(rawPayload.communicationData)
    ? (rawPayload.communicationData[0] as CommEntry)
    : ({} as CommEntry);
  rawPayload.communicationData = [
    buildReg1172CommEntry(template, envVariables.contact_purpose, 'CONTRACT COMM', 'contract-reg1172@test.com'),
    buildReg1172CommEntry(template, envVariables.billing_purpose, 'BILLING COMM', 'billing-reg1172@test.com'),
  ];
  return rawPayload;
}

export async function resolveReg1172CommunicationIdsForContract(
  Request: FixtureRequest,
  customerDetailsId: number,
): Promise<{ billingId: number; contractId: number }> {
  const billingRes = await Request.get('customer/communication-data/list', {
    params: { customerDetailsId, communicationDataType: 'BILLING' },
  });
  await expect(billingRes).CheckResponse();
  const billingRows = (await billingRes.json()) as Array<{ id?: number }>;

  const contractRes = await Request.get('customer/communication-data/list', {
    params: { customerDetailsId, communicationDataType: 'CONTRACT' },
  });
  await expect(contractRes).CheckResponse();
  const contractRows = (await contractRes.json()) as Array<{ id?: number }>;

  const billingId = billingRows[0]?.id;
  const contractId = contractRows[0]?.id;
  if (!billingId || !contractId) {
    throw new Error(
      `[REG-1172] Missing BILLING/CONTRACT communication ids (billing=${billingId}, contract=${contractId})`,
    );
  }
  return { billingId, contractId };
}

/** Dev2 split comm rows need distinct billing vs contract ids on product-contract POST. */
export async function buildReg1172ProductContractPayload(
  GeneratePayload: baseFixture['GeneratePayload'],
  Request: FixtureRequest,
  Responses: baseFixture['Responses'],
): Promise<ProductContractPayload> {
  const payload = await GeneratePayload.contractsAndOrders.product_contract();
  if (!isReg1172Dev2Target()) {
    return payload;
  }
  const customerDetailsId = Number(Responses.customer[0]?.lastCustomerDetailId);
  if (!Number.isFinite(customerDetailsId) || customerDetailsId <= 0) {
    throw new Error('[REG-1172] Missing lastCustomerDetailId on created customer');
  }
  const comm = await resolveReg1172CommunicationIdsForContract(Request, customerDetailsId);
  payload.basicParameters.communicationDataBillingId = comm.billingId;
  payload.basicParameters.communicationDataContractId = comm.contractId;
  return payload;
}

export const REG_1172_POD_COUNT = 4;

export type Reg1172Fx = Pick<
  baseFixture,
  'Request' | 'GeneratePayload' | 'Responses' | 'Endpoints' | 'validateInvoice'
>;

export type Reg1172PerPodElectricityResult = {
  label: string;
  identifier: string;
  expected: boolean;
  actual: boolean;
  deactivationDate: string | null;
};

/** Same as [REG-991] `withElectricity(Product).spec.ts` — stable label on invoice/detailed-data rows. */
export function buildReg1172ElectricityPriceComponentPayload(
  GeneratePayload: baseFixture['GeneratePayload'],
): ReturnType<typeof GeneratePayload.productAndServices.electricity> {
  const payload = GeneratePayload.productAndServices.electricity();
  payload.name = 'WITH_ELECTRICITY_INVOICE';
  return payload;
}

/** [REG-991] profile post — `profile1Month` + `timeZone: CET`, optional anchor window for REG-1172 matrix. */
export async function postReg1172BillingByProfileAllPods(
  fx: Pick<Reg1172Fx, 'Request' | 'GeneratePayload' | 'Responses'>,
  anchor: Pdt2376BillingAnchor,
  podCount: number = REG_1172_POD_COUNT,
): Promise<void> {
  for (let podIndex = 0; podIndex < podCount; podIndex++) {
    const payload = await fx.GeneratePayload.energyData.profile1Month(podIndex, [
      { startDate: anchor.profileStart, endDate: anchor.profileEnd },
    ]);
    payload.timeZone = 'CET';
    const profiles = await fx.Request.post('billing-by-profile', { data: payload });
    await expect(profiles).CheckResponse();
    const profileData = await profiles.json();
    fx.Responses.dataByProfiles.push({
      id: profileData,
      periodFrom: payload.periodFrom,
      periodTo: payload.periodTo,
      periodType: payload.periodType,
    });
  }
}

/** [REG-991] billing run models + anchor dates from OPEN accounting period. */
export async function postReg1172VolumeElectricityBillingRun(
  fx: Pick<Reg1172Fx, 'Request' | 'GeneratePayload' | 'Responses' | 'Endpoints'>,
  anchor: Pdt2376BillingAnchor,
): Promise<void> {
  const billingPayload = await fx.GeneratePayload.billing.billingRun('CONTRACT', [
    'FOR_VOLUMES',
    'WITH_ELECTRICITY_INVOICE',
  ]);
  billingPayload.commonParameters.accountingPeriodId = anchor.accountingPeriodId;
  billingPayload.commonParameters.taxEventDate = anchor.taxEventDate;
  billingPayload.commonParameters.invoiceDate = anchor.invoiceDate;

  const billingRun = await fx.Request.post(fx.Endpoints.billingRun, { data: billingPayload });
  await expect(billingRun).CheckResponse();
  fx.Responses.billingRun.push(await billingRun.json());
}

/** [REG-991] `validateInvoice.checkInvoiceTabs()` — invoice tabs sanity before per-POD matrix assert. */
export async function validateReg1172InvoiceTabs(
  validateInvoice: baseFixture['validateInvoice'],
): Promise<void> {
  await validateInvoice.checkInvoiceTabs();
}

export type Reg1172PodElectricityAssertOutcome = {
  perPodResults: Reg1172PerPodElectricityResult[];
  perPodAttachment: Pdt2376PodElectricityDetailedAttachmentEntry[];
  summaryRows: Record<string, unknown>[];
  invoiceIds: number[];
  electricityCtx: Pdt2376ElectricityMatchContext;
};

/** REG-1172 matrix assert on top of [REG-991] electricity invoice (detailed-data per POD). */
export async function assertReg1172PodElectricityMatrix(args: {
  Request: FixtureRequest;
  Responses: baseFixture['Responses'];
  matrix: Pdt2376PodMatrixRow[];
  billingAnchor: Pdt2376BillingAnchor;
  jiraKey?: string;
}): Promise<Reg1172PodElectricityAssertOutcome> {
  const { Request, Responses, matrix, billingAnchor, jiraKey = 'REG-1172' } = args;
  const brid = normalizeBillingRunId(Responses.billingRun[Responses.billingRun.length - 1]);
  let invoiceIds = await listInvoiceIdsForBillingRun(Request, brid);

  const fromResponses = Responses.invoice.filter((id) => Number(id) > 0).map(Number);
  if (fromResponses.length > 0) {
    invoiceIds = fromResponses;
  }

  expect(invoiceIds.length, `[${jiraKey}] at least one invoice must be generated`).toBeGreaterThan(0);

  let electricityCtx = buildPdt2376ElectricityMatchContext(Responses.priceComponent);
  electricityCtx = await hydratePdt2376ElectricityMatchContextIfBareId(Request, electricityCtx);
  const aggregatedDetailedRows = await fetchAggregatedInvoiceDetailedData(Request, invoiceIds);

  const contractId = Responses.productContract[0].id;
  const cg = await Request.get(`product-contract/${contractId}?version=1`);
  await expect(cg).CheckResponse();
  const cj = (await cg.json()) as {
    contractPodsResponses?: Array<{ podId?: number; identifier?: string | number }>;
  };

  const meterReadingTo = billingAnchor.profileEnd;
  const electricityPcId = resolvePdt2376ElectricityPriceComponentId(Responses.priceComponent);
  const perPodAttachment: Pdt2376PodElectricityDetailedAttachmentEntry[] = [];
  const perPodResults: Reg1172PerPodElectricityResult[] = [];
  const summaryRows: Record<string, unknown>[] = [];

  for (let i = 0; i < REG_1172_POD_COUNT; i++) {
    const podId = Responses.pod[i]?.id;
    const cpRow = (cj.contractPodsResponses ?? []).find((p) => Number(p.podId) === Number(podId));
    const identifier = cpRow?.identifier != null ? String(cpRow.identifier) : String(podId);
    const spec = matrix[i];
    const expectedWithElectricity = computeReg1172WithElectricityEligibility(
      spec.deactivationDate,
      meterReadingTo,
    );

    const matchedElectricityRows = getMatchedElectricityRowsForPodCandidates(
      aggregatedDetailedRows,
      [identifier, String(podId)],
      electricityCtx,
    );
    const actualWithElectricity = matchedElectricityRows.length > 0;

    const { periodFrom, periodTo } = resolveAttachmentPeriodBounds(
      matchedElectricityRows,
      billingAnchor.profileStart,
      meterReadingTo,
    );

    perPodAttachment.push({
      podIdentifier: identifier,
      expected: expectedWithElectricity,
      actual: actualWithElectricity,
      matchedRows: summarizeMatchedRowsBrief(matchedElectricityRows),
      periodFrom,
      periodTo,
    });

    perPodResults.push({
      label: spec.label,
      identifier,
      expected: expectedWithElectricity,
      actual: actualWithElectricity,
      deactivationDate: spec.deactivationDate,
    });

    summaryRows.push({
      label: spec.label,
      identifier,
      activationDate: spec.activationDate,
      deactivationDate: spec.deactivationDate,
      meterReadingTo,
      expectedWithElectricity,
      actualWithElectricity,
    });

    expect(
      actualWithElectricity,
      `[${spec.label}] POD ${identifier}: electricity line expected=${expectedWithElectricity}` +
        ` (deactivation=${spec.deactivationDate ?? 'null'}, meterReadingTo=${meterReadingTo};` +
        ` electricityPcId=${electricityPcId ?? 'n/a'})`,
    ).toBe(expectedWithElectricity);
  }

  return { perPodResults, perPodAttachment, summaryRows, invoiceIds, electricityCtx };
}

export { resolvePdt2376BillingAnchor, applyPdt2376PodActivationMatrix };
export type { Pdt2376BillingAnchor, Pdt2376PodMatrixRow };

/** Day-level eligibility per REG-1172 Jira description. */
export function computeReg1172WithElectricityEligibility(
  deactivationDate: string | null,
  meterReadingTo: string,
): boolean {
  if (deactivationDate == null) return true;
  return deactivationDate >= meterReadingTo;
}

function lastDayOfMonth(year: number, month1to12: number): string {
  const d = new Date(Date.UTC(year, month1to12, 0));
  return d.toISOString().slice(0, 10);
}

function monthFirstFromYmd(endYmd: string): string {
  return `${endYmd.slice(0, 7)}-01`;
}

function prevMonthLastDay(fromYmd: string): string {
  const y = Number(fromYmd.slice(0, 4));
  const m = Number(fromYmd.slice(5, 7));
  return lastDayOfMonth(y, m - 1);
}

function nextMonthLastDay(fromYmd: string): string {
  const y = Number(fromYmd.slice(0, 4));
  const m = Number(fromYmd.slice(5, 7));
  return lastDayOfMonth(y, m + 1);
}

function monthsBeforeMonthEnd(fromYmd: string, monthsBack: number): string {
  const y = Number(fromYmd.slice(0, 4));
  const m = Number(fromYmd.slice(5, 7));
  const ref = new Date(Date.UTC(y, m - 1, 1));
  ref.setUTCMonth(ref.getUTCMonth() - monthsBack);
  const ny = ref.getUTCFullYear();
  const nm = ref.getUTCMonth() + 1;
  return lastDayOfMonth(ny, nm);
}

/**
 * Four PODs covering REG-1172 matrix intent (dynamic dates from `meterReadingTo` / invoice period end):
 * - P1: deactivated before meter reading to → no electricity
 * - P2: deactivated after meter reading to → electricity
 * - P3: deactivated on meter reading to (same day) → electricity
 * - P4: activated in invoice month, no deactivation → electricity
 */
export function buildReg1172PodMatrix(meterReadingTo: string): Pdt2376PodMatrixRow[] {
  const monthFirst = monthFirstFromYmd(meterReadingTo);
  const podActivationDate = monthFirstFromYmd(monthsBeforeMonthEnd(meterReadingTo, 3));

  const rows: Array<Omit<Pdt2376PodMatrixRow, 'expectedEligible'> & { deactivationDate: string | null }> = [
    { label: 'POD1', activationDate: podActivationDate, deactivationDate: prevMonthLastDay(meterReadingTo) },
    { label: 'POD2', activationDate: podActivationDate, deactivationDate: nextMonthLastDay(meterReadingTo) },
    { label: 'POD3', activationDate: podActivationDate, deactivationDate: meterReadingTo },
    { label: 'POD4', activationDate: monthFirst, deactivationDate: null },
  ];

  return rows.map((r) => ({
    ...r,
    expectedEligible: computeReg1172WithElectricityEligibility(r.deactivationDate, meterReadingTo),
  }));
}
