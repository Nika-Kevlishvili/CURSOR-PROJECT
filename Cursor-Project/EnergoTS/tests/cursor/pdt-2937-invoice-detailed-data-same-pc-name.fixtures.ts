/**
 * PDT-2937 — invoice third tab (detailed-data) must list all price component rows even when names match.
 *
 * Jira repro (Dev): 3 same-named volume settlement PCs (explicit `issuedSeparateInvoice = INVOICE_ONE` each) →
 * price component group (`priceComponentsList`) → terms + POD → product with **group only** (`priceComponentIds = []`,
 * `priceComponentGroupIds = [groupId]`) → contract + POD activation → BBP → FOR_VOLUMES invoice.
 *
 * Invoice detailed-data matches on `pc.name` (not displayName only) — all three PCs share `PDT_2937_SHARED_PC_NAME`.
 * All three PCs are byte-for-byte identical config (name, displayName, expression, settlement, INVOICE_ONE,
 *   profile 100% on `envVariables.profiles`) — only separate API entities (different DB ids).
 * Billing aligns `priceSettlement` 15-minute settlement slots with BBP periodicity: `profile15minute(0)` + CET +
 * `uploadDataByProfileFile(bbpId, 'MIN')` (REG-962 / forVolumes pattern), then default FOR_VOLUMES billing run.
 *
 * Reference specs:
 * - tests/billing/forVolumes/forVolumes.spec.ts (REG-962: profile15minute + MIN upload for settlement alignment)
 * - tests/billing/slotSplitting/invoiceSlotSplitting.spec.ts (BBP + billing run)
 * - tests/salesPortal/GET-PRODUCT-LIST-product-list.spec.ts (price component group via `priceComponentsList`)
 * - tests/cursor/pdt-2915-invoice-correction-deleted-bbp.fixtures.ts (contract prechain, BBP POST helper)
 * - tests/cursor/pdt-2599-service-contract.fixtures.ts (fetchPdt2599InvoiceDetailedRows)
 *
 * Swagger (dev): POST price-component-groups (`CreatePriceComponentGroupRequest`), GET invoice/detailed-data.
 */

import { test, expect } from '../../fixtures/baseFixture';
import type { baseFixture } from '../../fixtures/baseFixture';
import { envVariables } from '../../fixtures/envCashed';
import { asPriceComponentId } from './pdt-2599-service-contract.fixtures';
import { fetchPdt2599InvoiceDetailedRows } from './pdt-2599-service-contract.fixtures';
import type { Pdt2599InvoiceDetailedDataRow } from './pdt-2599-service-contract.fixtures';
import { postPdt2915BillingByProfile, resolvePdt2915PodIdentifier } from './pdt-2915-invoice-correction-deleted-bbp.fixtures';

export const PDT_2937_SHARED_PC_NAME = 'PDT-2937-Shared-Volume-PC-Name';
export const PDT_2937_PC_GROUP_NAME = 'PDT-2937-Shared-PC-Group';
export const PDT_2937_PC_EXPRESSION = '100';
export const PDT_2937_IDENTICAL_PC_COUNT = 3;
export const PDT_2937_EXPECTED_DETAILED_ROWS_PER_POD = 3;

export type Pdt2937Fx = Pick<baseFixture, 'Request' | 'GeneratePayload' | 'Responses' | 'Endpoints'>;

export type Pdt2937ContractPrechainResult = {
  podId: number;
  podIdentifier: string;
  priceComponentIds: number[];
  groupId: number;
  contractId: number;
};

export type Pdt2937ScenarioResult = Pdt2937ContractPrechainResult & {
  invoiceId: number;
};

/** Dev portal third tab — matches Jira example invoice link pattern. */
export function buildPdt2937InvoiceDetailedDataPreviewLink(invoiceId: number): string {
  return `http://10.236.20.11:8080/billing-run/invoices/preview/detailed-data?id=${invoiceId}`;
}

export function filterPdt2937DetailedRowsForPod(
  rows: Pdt2599InvoiceDetailedDataRow[],
  podIdentifier: string,
): Pdt2599InvoiceDetailedDataRow[] {
  const needle = podIdentifier.trim();
  return rows.filter((row) => String(row.pointOfDelivery ?? '').trim() === needle);
}

export function summarizePdt2937DetailedRowsForPod(
  rows: Pdt2599InvoiceDetailedDataRow[],
): Array<{
  priceComponent?: string;
  pointOfDelivery?: string;
  value?: number;
  unitPrice?: number;
}> {
  return rows.map((row) => ({
    priceComponent: row.priceComponent,
    pointOfDelivery: row.pointOfDelivery,
    value: row.value,
    unitPrice: row.unitPrice,
  }));
}

export function buildPdt2937IdenticalPriceComponentPayload(
  GeneratePayload: Pdt2937Fx['GeneratePayload'],
): ReturnType<Pdt2937Fx['GeneratePayload']['productAndServices']['priceSettlement']> {
  const payload = GeneratePayload.productAndServices.priceSettlement();
  payload.name = PDT_2937_SHARED_PC_NAME;
  payload.displayName = PDT_2937_SHARED_PC_NAME;
  payload.formulaRequest.expression = PDT_2937_PC_EXPRESSION;
  payload.formulaRequest.issuedSeparateInvoice = 'INVOICE_ONE';
  payload.applicationModelRequest.settlementPeriodsRequest.profiles = [
    { profileId: envVariables.profiles, percentage: 100 },
  ];
  return payload;
}

async function createPdt2937SameNamedPriceComponents(ctx: Pdt2937Fx): Promise<number[]> {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;
  const ids: number[] = [];

  for (let i = 0; i < PDT_2937_IDENTICAL_PC_COUNT; i++) {
    const payload = buildPdt2937IdenticalPriceComponentPayload(GeneratePayload);
    const res = await Request.post(Endpoints.priceComponent, { data: payload });
    await expect(res).CheckResponse();
    const raw = await res.json();
    const id = asPriceComponentId(raw);
    ids.push(id);
    Responses.priceComponent.push(id);
  }

  return ids;
}

async function createPdt2937PriceComponentGroup(ctx: Pdt2937Fx, priceComponentIds: number[]): Promise<number> {
  const { Request, Responses, Endpoints } = ctx;
  const groupPayload = {
    name: PDT_2937_PC_GROUP_NAME,
    priceComponentsList: priceComponentIds.map((priceComponentId) => ({ priceComponentId })),
  };
  const res = await Request.post(Endpoints.groupOfPriceComponents, { data: groupPayload });
  await expect(res).CheckResponse();
  const raw = await res.json();
  const groupId = asPriceComponentId(raw);
  expect(groupId, 'price component group id required').toBeGreaterThan(0);
  Responses.groupOfPriceComponents.push({ id: groupId });
  return groupId;
}

/** Contract prechain: Jira repro via PC group on product (all PCs INVOICE_ONE). */
export async function runPdt2937ContractPrechain(ctx: Pdt2937Fx): Promise<Pdt2937ContractPrechainResult> {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;

  await test.step('Precondition: customer', async () => {
    const customer = await Request.post(Endpoints.customer, {
      data: GeneratePayload.customers.customer_legal(),
    });
    await expect(customer).CheckResponse();
    Responses.customer.push(await customer.json());
  });

  const priceComponentIds = await test.step(
    'Precondition: three identical volume settlement price components (same name, expression, settlement; INVOICE_ONE)',
    async () => createPdt2937SameNamedPriceComponents(ctx),
  );

  const groupId = await test.step(
    'Precondition: price component group with all three same-named PCs',
    async () => createPdt2937PriceComponentGroup(ctx, priceComponentIds),
  );

  await test.step('Precondition: terms', async () => {
    const term = await Request.post(Endpoints.terms, { data: GeneratePayload.productAndServices.term() });
    await expect(term).CheckResponse();
    Responses.terms.push(await term.json());
  });

  await test.step('Precondition: POD', async () => {
    const pod = await Request.post(Endpoints.pod, {
      data: GeneratePayload.pointsOfDelivery.pod_settlement(),
    });
    await expect(pod).CheckResponse();
    Responses.pod.push(await pod.json());
  });

  await test.step('Precondition: product (price component group only)', async () => {
    const productPayload = GeneratePayload.productAndServices.product();
    productPayload.priceComponentIds = [];
    productPayload.priceComponentGroupIds = [groupId];
    const product = await Request.post(Endpoints.product, { data: productPayload });
    await expect(product).CheckResponse();
    Responses.product.push(await product.json());
  });

  let contractId = 0;
  await test.step('Precondition: product contract', async () => {
    const contract = await Request.post(Endpoints.productContract, {
      data: await GeneratePayload.contractsAndOrders.product_contract(),
    });
    await expect(contract).CheckResponse();
    const contractBody = await contract.json();
    Responses.productContract.push(contractBody);
    contractId = Number((contractBody as { id?: number }).id);
    expect(contractId, 'product contract id required for portal links').toBeGreaterThan(0);
  });

  await test.step('Precondition: activate POD on contract', async () => {
    const podActivation = await Request.post('/contract-pods/manual', {
      data: await GeneratePayload.pointsOfDelivery.pod_activation(0),
    });
    await expect(podActivation).CheckResponse();
  });

  const podId = Responses.pod[0].id as number;
  const podIdentifier = await resolvePdt2915PodIdentifier(Request, podId);

  return { podId, podIdentifier, priceComponentIds, groupId, contractId };
}

/** Post billing-by-profile and realize FOR_VOLUMES invoice (slotSplitting / PDT-2915 pattern). */
export async function runPdt2937BillingAndRealizeInvoice(
  ctx: Pdt2937Fx,
  _pre: Pdt2937ContractPrechainResult,
): Promise<number> {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;

  await test.step('Precondition: billing-by-profile for POD', async () => {
    const payload = await GeneratePayload.energyData.profile15minute(0);
    payload.timeZone = 'CET';
    payload.profileId = envVariables.profiles;
    const bbp = await postPdt2915BillingByProfile(Request, payload, 'PDT-2937 BBP');
    await GeneratePayload.energyData.uploadDataByProfileFile(bbp.id, 'MIN');
    Responses.dataByProfiles.push({
      id: bbp.id,
      periodFrom: payload.periodFrom,
      periodTo: payload.periodTo,
      periodType: payload.periodType,
    });
  });

  await test.step('Precondition: standard FOR_VOLUMES billing run', async () => {
    const billingPayload = await GeneratePayload.billing.billingRun('CONTRACT', ['FOR_VOLUMES']);
    const billingRun = await Request.post(Endpoints.billingRun, { data: billingPayload });
    await expect(billingRun).CheckResponse();
    Responses.billingRun.push(await billingRun.json());
  });

  await test.step('Precondition: realize standard invoice', async () => {
    await GeneratePayload.billing.waitForInvoiceGeneration(true, true, 1, 0);
  });

  return Responses.invoice[Responses.invoice.length - 1] as number;
}

export async function assertPdt2937AllSameNamedPcRowsOnDetailedData(
  Request: baseFixture['Request'],
  invoiceId: number,
  podIdentifier: string,
): Promise<{
  allRows: Pdt2599InvoiceDetailedDataRow[];
  rowsForPod: Pdt2599InvoiceDetailedDataRow[];
}> {
  const allRows = await fetchPdt2599InvoiceDetailedRows(Request, invoiceId);
  const rowsForPod = filterPdt2937DetailedRowsForPod(allRows, podIdentifier);

  expect(
    rowsForPod.length,
    `PDT-2937 TO-BE: invoice/detailed-data must return ${PDT_2937_EXPECTED_DETAILED_ROWS_PER_POD} rows for POD ${podIdentifier} when ${PDT_2937_EXPECTED_DETAILED_ROWS_PER_POD} same-named price components are on the invoice (AS-IS deduplicates by name → 1 row)`,
  ).toBe(PDT_2937_EXPECTED_DETAILED_ROWS_PER_POD);

  expect(rowsForPod.every((r) => String(r.priceComponent ?? '').trim() === PDT_2937_SHARED_PC_NAME)).toBe(true);

  return { allRows, rowsForPod };
}

export function buildPdt2937AttachmentSummary(args: Record<string, unknown>): Record<string, unknown> {
  return { jiraKey: 'PDT-2937', ...args };
}
