/**
 * PDT-3013 — automatic offsetting after invoice reversal when billing group has
 * "Separate invoice for each point of delivery" (2 PODs → 2 volume invoices).
 *
 * Reference specs:
 * - tests/cursor/PDT-2529-rfd-data-model-happy-path.fixtures.ts (separate invoice per POD, volume anchor)
 * - tests/cursor/pdt-2915-invoice-correction-deleted-bbp.fixtures.ts (FOR_VOLUMES realize chain)
 * - tests/billing/reversal/reversal.spec.ts (reversal billing run)
 */

import { expect } from '../../fixtures/baseFixture';
import type { baseFixture } from '../../fixtures/baseFixture';
import { configuredBaseURL } from '../../fixtures/utils/baseUrl';
import { randomGens } from '../../utils/randomGens';
import {
  applyVolumeBillingAnchor,
  enableSeparateInvoiceForEachPod,
  profileDateRangeFromAnchor,
  termPayloadWithMinimalPaymentDays,
} from './PDT-2529-rfd-data-model-happy-path.fixtures';
import { resolvePdt2376BillingAnchor, type Pdt2376BillingAnchor } from './pdt-2376-volume-with-electricity.fixtures';

/** Matches ticket repro: 2 PODs in billing group 0000 with separate invoice per POD. */
export const PDT3013_POD_COUNT = 2;

export type Pdt3013Environment = 'dev' | 'dev2' | 'test' | 'unknown';

/** Resolved from BASE_URL / configuredBaseURL (Dev, Dev2, Test). */
export function resolvePdt3013Environment(): Pdt3013Environment {
  const raw = (configuredBaseURL || process.env.BASE_URL || '').toLowerCase();
  if (raw.includes('testapps')) return 'test';
  if (raw.includes('phoenix-dev2') || raw.includes('dev2')) return 'dev2';
  if (
    raw.includes('10.236.20.11') ||
    raw.includes('10.236.20.81:8091') ||
    raw.includes(':8091')
  ) {
    return 'dev';
  }
  return 'unknown';
}

export type Pdt3013Fx = Pick<baseFixture, 'Request' | 'GeneratePayload' | 'Responses' | 'Endpoints'>;

export type Pdt3013PrepOptions = {
  workerIndex?: number;
  iteration?: number;
  /** Default true — PDT-3013 bug path. Set false for TC-BE-6 baseline. */
  separateInvoicePerPod?: boolean;
};

export type Pdt3013PrepResult = {
  billingGroupId: number;
  anchor: Pdt2376BillingAnchor;
  podIdentifiers: string[];
  separateInvoicePerPod: boolean;
};

export type Pdt3013ReversalOffsetSnapshot = {
  originalInvoiceId: number;
  originalLiabilityId: number;
  creditNoteInvoiceId: number;
  creditNoteReceivableId: number;
  receivableInitialAmount: number;
  receivableCurrentAmount: number;
  liabilityInitialAmount: number;
  liabilityCurrentAmount: number;
  receivableHasLiabilityOffset: boolean;
  liabilityHasReceivableOffset: boolean;
};

type OffsettingRow = {
  id?: number;
  offsettingObjectType?: string;
  offsettingObject?: string;
  amount?: number;
};

function liabilityOffsettingRows(liabilityBody: Record<string, unknown>): OffsettingRow[] {
  const list =
    liabilityBody.customerLiabilityOffsettingReponseList ??
    liabilityBody.customerOffsettingResponseList;
  return (list as OffsettingRow[] | undefined) ?? [];
}

function rowMatchesOffsetTarget(
  row: OffsettingRow,
  expectedType: string,
  expectedId: number,
): boolean {
  const type = row.offsettingObjectType ?? row.offsettingObject;
  return type === expectedType && row.id === expectedId;
}

function podSettlementPayloadUnique(
  GeneratePayload: baseFixture['GeneratePayload'],
  workerIndex: number,
  iteration: number,
): Record<string, unknown> {
  const payload = GeneratePayload.pointsOfDelivery.pod_settlement() as Record<string, unknown>;
  const suffix = `${workerIndex}${String(iteration).padStart(5, '0')}`;
  const core = randomGens.generateUniqueIdentifier();
  payload.identifier = (`32X${core}${suffix}`).slice(0, 33);
  return payload;
}

async function postBillingByProfileForPod(
  ctx: Pdt3013Fx,
  podIndex: number,
  anchor: Pdt2376BillingAnchor,
): Promise<void> {
  const { Request, GeneratePayload, Responses } = ctx;
  const dateRanges = profileDateRangeFromAnchor(anchor);
  const payload = await GeneratePayload.energyData.profile1Month(podIndex, dateRanges);
  payload.timeZone = 'CET';
  const profiles = await Request.post('billing-by-profile', { data: payload });
  await expect(profiles).CheckResponse();
  const profileData = await profiles.json();
  Responses.dataByProfiles.push({
    id: profileData,
    periodFrom: payload.periodFrom,
    periodTo: payload.periodTo,
    periodType: payload.periodType,
  });
}

async function resolvePodIdentifiers(Request: baseFixture['Request'], Responses: baseFixture['Responses']): Promise<string[]> {
  const identifiers: string[] = [];
  for (const pod of Responses.pod) {
    const podRes = await Request.get(`pod/${pod.id}?version=1`);
    await expect(podRes).CheckResponse();
    const podJson = (await podRes.json()) as { identifier?: string };
    if (podJson.identifier) identifiers.push(String(podJson.identifier));
  }
  return identifiers;
}

/**
 * Customer → 2 PODs → contract → activation → separate invoice per POD → billing-by-profile per POD.
 */
export async function runPdt3013ContractPrep(
  ctx: Pdt3013Fx,
  opts?: Pdt3013PrepOptions,
): Promise<Pdt3013PrepResult> {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;
  const workerIndex = opts?.workerIndex ?? 0;
  const iteration = opts?.iteration ?? 0;
  const separateInvoicePerPod = opts?.separateInvoicePerPod ?? true;

  const term = await Request.post(Endpoints.terms, {
    data: termPayloadWithMinimalPaymentDays(GeneratePayload),
  });
  await expect(term).CheckResponse();
  Responses.terms.push(await term.json());

  const pricePayload = GeneratePayload.productAndServices.priceSettlement();
  const price = await Request.post(Endpoints.priceComponent, { data: pricePayload });
  await expect(price).CheckResponse();
  Responses.priceComponent.push(await price.json());

  const product = await Request.post(Endpoints.product, { data: GeneratePayload.productAndServices.product() });
  await expect(product).CheckResponse();
  Responses.product.push(await product.json());

  const customer = await Request.post(Endpoints.customer, { data: GeneratePayload.customers.customer_legal() });
  await expect(customer).CheckResponse();
  Responses.customer.push(await customer.json());

  for (let i = 0; i < PDT3013_POD_COUNT; i++) {
    const podPayload = podSettlementPayloadUnique(GeneratePayload, workerIndex, iteration * 100 + i);
    const podSettlement = await Request.post(Endpoints.pod, { data: podPayload });
    await expect(podSettlement).CheckResponse();
    const podJson = (await podSettlement.json()) as Record<string, unknown>;
    if (!podJson.identifier) podJson.identifier = podPayload.identifier;
    Responses.pod.push(podJson as (typeof Responses.pod)[number]);
  }

  const contract = await Request.post(Endpoints.productContract, {
    data: await GeneratePayload.contractsAndOrders.product_contract(),
  });
  await expect(contract).CheckResponse();
  Responses.productContract.push(await contract.json());

  for (let i = 0; i < PDT3013_POD_COUNT; i++) {
    const podActivation = await Request.post('/contract-pods/manual', {
      data: await GeneratePayload.pointsOfDelivery.pod_activation(i),
    });
    await expect(podActivation).CheckResponse();
  }

  let billingGroupId: number;
  if (separateInvoicePerPod) {
    const enabled = await enableSeparateInvoiceForEachPod(Request, GeneratePayload);
    billingGroupId = enabled.billingGroupId;
    const billingGroupRes = await Request.get(`billing-group/${billingGroupId}`);
    await expect(billingGroupRes).CheckResponse();
    const billingGroupBody = (await billingGroupRes.json()) as { separateInvoiceForEachPod?: boolean };
    expect(
      billingGroupBody.separateInvoiceForEachPod,
      'Billing group must have separateInvoiceForEachPod=true (PDT-3013 precondition)',
    ).toBe(true);
  } else {
    const contractGet = await Request.get(`${Endpoints.productContract}/${Responses.productContract[0].id}?version=1`);
    await expect(contractGet).CheckResponse();
    const contractJson = await contractGet.json();
    billingGroupId = contractJson.billingGroups[0].id as number;
    const billingGroupRes = await Request.get(`billing-group/${billingGroupId}`);
    await expect(billingGroupRes).CheckResponse();
    const billingGroupBody = (await billingGroupRes.json()) as { separateInvoiceForEachPod?: boolean };
    expect(billingGroupBody.separateInvoiceForEachPod, 'Baseline TC-BE-6: flag must stay false').toBe(false);
  }

  const anchor = await resolvePdt2376BillingAnchor(Request);

  for (let i = 0; i < PDT3013_POD_COUNT; i++) {
    await postBillingByProfileForPod(ctx, i, anchor);
  }

  const podIdentifiers = await resolvePodIdentifiers(Request, Responses);
  return { billingGroupId, anchor, podIdentifiers, separateInvoicePerPod };
}

/**
 * POD on volume invoices is exposed on `GET invoice/detailed-data` as `pointOfDelivery`
 * (Swagger `InvoiceDetailedDataResponse`), not on `GET invoice?id=…`.
 */
export async function resolvePodIdentifierFromInvoiceDetailedData(
  Request: baseFixture['Request'],
  invoiceId: number,
): Promise<string> {
  const res = await Request.get(`invoice/detailed-data?page=0&size=100&id=${invoiceId}`);
  await expect(res).CheckResponse();
  const body = (await res.json()) as { content?: Array<Record<string, unknown>> };
  const rows = body.content ?? [];
  expect(rows.length, `Invoice ${invoiceId} detailed-data must have at least one row`).toBeGreaterThan(0);

  const podIds = new Set<string>();
  for (const row of rows) {
    const pod = String(row.pointOfDelivery ?? row.podIdentifier ?? '').trim();
    if (pod) podIds.add(pod);
  }
  expect(
    podIds.size,
    `Invoice ${invoiceId} detailed-data must reference exactly one POD (separate invoice per POD)`,
  ).toBe(1);
  return [...podIds][0];
}

/** FOR_VOLUMES billing with historical anchor — expects `expectedInvoiceCount` REAL invoices. */
export async function runPdt3013VolumeBillingAndRealize(
  ctx: Pdt3013Fx,
  anchor: Pdt2376BillingAnchor,
  expectedInvoiceCount: number = PDT3013_POD_COUNT,
): Promise<number[]> {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;

  const billingPayload = await GeneratePayload.billing.billingRun('CONTRACT', ['FOR_VOLUMES']);
  applyVolumeBillingAnchor(billingPayload, anchor);
  const billingRun = await Request.post(Endpoints.billingRun, { data: billingPayload });
  await expect(billingRun).CheckResponse();
  const billingRunId = await billingRun.json();
  Responses.billingRun.push({ id: billingRunId, invoiceNumbers: expectedInvoiceCount });

  await GeneratePayload.billing.waitForInvoiceGeneration(true, true, expectedInvoiceCount, 0);

  const invoiceIds = Responses.invoice.slice(-expectedInvoiceCount) as number[];
  expect(invoiceIds.length, `Expected ${expectedInvoiceCount} volume invoice(s)`).toBe(expectedInvoiceCount);

  if (expectedInvoiceCount > 1) {
    const podIdentifiersOnInvoices: string[] = [];
    for (const invoiceId of invoiceIds) {
      const invoiceRes = await Request.get(`invoice?id=${invoiceId}`);
      await expect(invoiceRes).CheckResponse();
      const invoice = await invoiceRes.json();
      expect(invoice.invoiceStatus, `Invoice ${invoiceId} must be REAL`).toBe('REAL');
      expect(invoice.invoiceDocumentType, `Invoice ${invoiceId} must be standard invoice`).toBe('INVOICE');
      podIdentifiersOnInvoices.push(await resolvePodIdentifierFromInvoiceDetailedData(Request, invoiceId));
    }
    expect(
      new Set(podIdentifiersOnInvoices).size,
      `Each volume invoice must map to a distinct POD; got ${podIdentifiersOnInvoices.join(', ')}`,
    ).toBe(expectedInvoiceCount);
  } else {
    const invoiceId = invoiceIds[0];
    const invoiceRes = await Request.get(`invoice?id=${invoiceId}`);
    await expect(invoiceRes).CheckResponse();
    const invoice = await invoiceRes.json();
    expect(invoice.invoiceStatus).toBe('REAL');
    expect(invoice.invoiceDocumentType).toBe('INVOICE');
  }

  return invoiceIds;
}

export async function assertPdt3013OtherLiabilityUnchanged(
  Request: baseFixture['Request'],
  Endpoints: baseFixture['Endpoints'],
  liabilityId: number,
  amountBefore: number,
): Promise<void> {
  const liabilityGet = await Request.get(`${Endpoints.customerLiability}/${liabilityId}`);
  await expect(liabilityGet).CheckResponse();
  const body = await liabilityGet.json();
  expect(
    Number(body.currentAmount),
    `Liability ${liabilityId} must remain unchanged (cross-POD isolation)`,
  ).toBe(amountBefore);
}

export async function readLiabilityCurrentAmount(
  Request: baseFixture['Request'],
  Endpoints: baseFixture['Endpoints'],
  liabilityId: number,
): Promise<number> {
  const liabilityGet = await Request.get(`${Endpoints.customerLiability}/${liabilityId}`);
  await expect(liabilityGet).CheckResponse();
  const body = await liabilityGet.json();
  return Number(body.currentAmount);
}

export function liabilityIdFromInvoice(invoiceBody: {
  liabilitiesAndReceivables?: Array<{ type?: string; id?: number }>;
}): number {
  const liability = (invoiceBody.liabilitiesAndReceivables ?? []).find((row) => row.type === 'LIABILITY');
  expect(liability?.id, 'Invoice must expose LIABILITY in liabilitiesAndReceivables').toBeTruthy();
  return Number(liability!.id);
}

export function receivableIdFromInvoice(invoiceBody: {
  liabilitiesAndReceivables?: Array<{ type?: string; id?: number }>;
}): number {
  const receivable = (invoiceBody.liabilitiesAndReceivables ?? []).find((row) => row.type === 'RECEIVABLE');
  expect(receivable?.id, 'Credit note must expose RECEIVABLE in liabilitiesAndReceivables').toBeTruthy();
  return Number(receivable!.id);
}

/** Reverse first volume invoice and realize credit note. */
export async function runPdt3013InvoiceReversal(
  ctx: Pdt3013Fx,
  originalInvoiceIndex: number,
): Promise<{ creditNoteInvoiceId: number }> {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;

  const payload = await GeneratePayload.billing.reversalBilling(originalInvoiceIndex);
  const reversal = await Request.post(Endpoints.billingRun, { data: payload });
  await expect(reversal).CheckResponse();
  const reversalId = await reversal.json();
  Responses.billingRun.push({ id: reversalId, invoiceNumbers: 1 });

  await GeneratePayload.billing.waitForInvoiceGeneration(true, true, 1, Responses.billingRun.length - 1);

  const creditNoteInvoiceId = Responses.invoice[Responses.invoice.length - 1] as number;
  const creditNoteRes = await Request.get(`invoice?id=${creditNoteInvoiceId}`);
  await expect(creditNoteRes).CheckResponse();
  const creditNote = await creditNoteRes.json();
  expect(creditNote.invoiceDocumentType, 'Reversal must produce CREDIT_NOTE').toBe('CREDIT_NOTE');

  return { creditNoteInvoiceId };
}

export async function collectPdt3013ReversalOffsetSnapshot(
  Request: baseFixture['Request'],
  Endpoints: baseFixture['Endpoints'],
  originalInvoiceId: number,
  creditNoteInvoiceId: number,
): Promise<Pdt3013ReversalOffsetSnapshot> {
  const originalRes = await Request.get(`invoice?id=${originalInvoiceId}`);
  await expect(originalRes).CheckResponse();
  const originalInvoice = await originalRes.json();
  const originalLiabilityId = liabilityIdFromInvoice(originalInvoice);

  const creditRes = await Request.get(`invoice?id=${creditNoteInvoiceId}`);
  await expect(creditRes).CheckResponse();
  const creditNote = await creditRes.json();
  const creditNoteReceivableId = receivableIdFromInvoice(creditNote);

  const receivableGet = await Request.get(`${Endpoints.customerReceivable}/${creditNoteReceivableId}`);
  await expect(receivableGet).CheckResponse();
  const receivableBody = await receivableGet.json();

  const liabilityGet = await Request.get(`${Endpoints.customerLiability}/${originalLiabilityId}`);
  await expect(liabilityGet).CheckResponse();
  const liabilityBody = await liabilityGet.json();

  const receivableHasLiabilityOffset = (receivableBody.customerOffsettingResponseList as OffsettingRow[] | undefined)?.some(
    (row) => rowMatchesOffsetTarget(row, 'LIABILITY', originalLiabilityId),
  ) ?? false;

  const liabilityHasReceivableOffset = liabilityOffsettingRows(liabilityBody as Record<string, unknown>).some(
    (row) => rowMatchesOffsetTarget(row, 'RECEIVABLE', creditNoteReceivableId),
  );

  return {
    originalInvoiceId,
    originalLiabilityId,
    creditNoteInvoiceId,
    creditNoteReceivableId,
    receivableInitialAmount: Number(receivableBody.initialAmount),
    receivableCurrentAmount: Number(receivableBody.currentAmount),
    liabilityInitialAmount: Number(liabilityBody.initialAmount),
    liabilityCurrentAmount: Number(liabilityBody.currentAmount),
    receivableHasLiabilityOffset,
    liabilityHasReceivableOffset,
  };
}

/**
 * Product rule (PDT-3013): credit-note receivable must auto-offset the reversed invoice liability for the same POD.
 */
export async function assertPdt3013AutomaticReceivableLiabilityOffset(
  snapshot: Pdt3013ReversalOffsetSnapshot,
): Promise<void> {
  expect(
    snapshot.receivableHasLiabilityOffset,
    `Receivable ${snapshot.creditNoteReceivableId} must list liability ${snapshot.originalLiabilityId} in customerOffsettingResponseList`,
  ).toBe(true);

  expect(
    snapshot.liabilityHasReceivableOffset,
    `Liability ${snapshot.originalLiabilityId} must list receivable ${snapshot.creditNoteReceivableId} in customerLiabilityOffsettingReponseList`,
  ).toBe(true);

  expect(
    snapshot.receivableCurrentAmount,
    `Credit-note receivable should be fully offset (initial=${snapshot.receivableInitialAmount}, current=${snapshot.receivableCurrentAmount})`,
  ).toBe(0);

  expect(
    snapshot.liabilityCurrentAmount,
    `Original invoice liability should be fully offset after reversal (initial=${snapshot.liabilityInitialAmount}, current=${snapshot.liabilityCurrentAmount})`,
  ).toBe(0);
}

export function buildPdt3013AttachmentSummary(
  prep: Pdt3013PrepResult,
  snapshot: Pdt3013ReversalOffsetSnapshot,
  responsesLinks: Record<string, unknown>,
): Record<string, unknown> {
  return {
    jiraKey: 'PDT-3013',
    environment: resolvePdt3013Environment(),
    separateInvoiceForEachPod: prep.separateInvoicePerPod,
    podCount: PDT3013_POD_COUNT,
    billingGroupId: prep.billingGroupId,
    podIdentifiers: prep.podIdentifiers,
    billingAnchor: prep.anchor,
    ...snapshot,
    responsesLinks,
  };
}
