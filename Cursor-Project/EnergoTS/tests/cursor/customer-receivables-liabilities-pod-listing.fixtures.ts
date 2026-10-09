/**
 * STANDALONE — customer Receivables/Liabilities listing `pods` / `address`.
 *
 * Reference spec(s):
 * - tests/cursor/PDT-3214-easypay-liabilities-receivables-accounting-period-rules.spec.ts (liability POST numeric id)
 * - tests/cursor/pdt-3223-invoice-json-price-component-order.fixtures.ts (billing-by-scales + waitForInvoiceGeneration)
 * - tests/cursor/pdt-3013-separate-pod-reversal-offset.fixtures.ts (two PODs, invoice detailed-data)
 * - tests/cursor/PDT-2529-rfd-data-model-happy-path.fixtures.ts (enableSeparateInvoiceForEachPod)
 * - tests/receivableManagement/manualLiabilityOffsetting.spec.ts (REG-832 deposit MLO: auto liability, MLO_L_R, deposit/job, MLO_D_L)
 * - tests/cursor/pdt-3206-contract-template-proxy-private-json.fixtures.ts (productParameters.contractType SUPPLY_ONLY + paymentGuarantee NO)
 * - tests/cursor/PHN-2798-get-contract-outgoing-document.spec.ts (product_contract contractType overwrite)
 *
 * Swagger (dev, refreshed 2026-08-19): POST /customer/{customerId}/customer-liability-and-receivable
 * DTO CustomerRelatedLiabilitiesAndReceivablesListRequest; row CustomerLiabilityAndReceivableListingMiddleResponse.
 * Listing HTTP in controller is 206 PARTIAL_CONTENT (not Swagger 200).
 */

import { expect } from '../../fixtures/baseFixture';
import type { baseFixture } from '../../fixtures/baseFixture';
import { randomGens } from '../../utils/randomGens';
import { scales as scalesPayloadTemplate } from '../../jsons/payloads/create/energyData/scales';
import { asPriceComponentId } from './pdt-2599-service-contract.fixtures';
import { enableSeparateInvoiceForEachPod } from './PDT-2529-rfd-data-model-happy-path.fixtures';

export type PodListingFx = Pick<
  baseFixture,
  'Request' | 'GeneratePayload' | 'Responses' | 'Endpoints' | 'Nomenclatures'
>;

export const GRID_OPERATOR_CODE = 'GIO';
export const MANUAL_AMOUNT = 10;
export const DEPOSIT_AMOUNT = 100;
export const VOLUME_TIMEOUT_MS = 30 * 60 * 1000;

export type ListingRow = {
  pods?: string | null;
  address?: string | null;
  idRaw?: number;
  id?: string;
  object?: string;
  billingGroup?: string | null;
  outgoingDocumentId?: string | number | null;
  outgoingDocumentType?: string | null;
};

export type ListingPage = {
  content?: ListingRow[];
  totalElements?: number;
};

export type ListingBody = {
  page: number | null;
  size: number | null;
  prompt: string | null;
  searchFields: string;
  blockedForPayments: boolean;
  blockedForReminders: boolean;
  blockedForInterest: boolean;
  blockedForOffsetting: boolean;
  blockedForDisconnection: boolean;
  showDeposits: boolean;
  showLiabilitiesAndReceivables: boolean;
  columns: string;
  direction: string;
};

export type PeriodWindow = {
  periodFrom: string;
  periodTo: string;
};

export type PodView = {
  podId: number;
  identifier: string;
  addressFragment: string | null;
};

export type ManualContractChain = {
  customerId: number;
  customerDetailsId: number;
  contractId: number;
  billingGroupId: number;
  pods: PodView[];
};

export type VolumeInvoiceChain = ManualContractChain & {
  invoiceIds: number[];
  billingRunId: number;
  period: PeriodWindow;
};

export function entityId(raw: unknown): number {
  if (typeof raw === 'number' && Number.isFinite(raw)) return raw;
  if (raw !== null && typeof raw === 'object' && 'id' in raw) {
    const n = Number((raw as { id: unknown }).id);
    if (Number.isFinite(n) && n > 0) return n;
  }
  throw new Error(`Could not resolve numeric id from ${JSON.stringify(raw)}`);
}

export function listingPath(customerId: number): string {
  return `customer/${customerId}/customer-liability-and-receivable`;
}

export function defaultListingBody(overrides: Partial<ListingBody> = {}): ListingBody {
  return {
    page: 0,
    size: 50,
    prompt: null,
    searchFields: 'ALL',
    blockedForPayments: false,
    blockedForReminders: false,
    blockedForInterest: false,
    blockedForOffsetting: false,
    blockedForDisconnection: false,
    showDeposits: false,
    showLiabilitiesAndReceivables: true,
    columns: 'ID',
    direction: 'DESC',
    ...overrides,
  };
}

export function computeCurrentMonthWindow(reference = new Date()): PeriodWindow {
  const year = reference.getUTCFullYear();
  const monthIndex = reference.getUTCMonth();
  const pad = (n: number) => String(n).padStart(2, '0');
  const lastDay = new Date(Date.UTC(year, monthIndex + 1, 0)).getUTCDate();
  return {
    periodFrom: `${year}-${pad(monthIndex + 1)}-01`,
    periodTo: `${year}-${pad(monthIndex + 1)}-${pad(lastDay)}`,
  };
}

export function extractErrorText(body: unknown): string {
  if (typeof body === 'string') return body;
  if (body && typeof body === 'object') {
    const rec = body as Record<string, unknown>;
    if (typeof rec.message === 'string' && rec.message) return rec.message;
    if (typeof rec.error === 'string' && rec.error) return rec.error;
    if (typeof rec.detail === 'string' && rec.detail) return rec.detail;
    return JSON.stringify(body);
  }
  return String(body ?? '');
}

function firstString(values: unknown[]): string | null {
  for (const value of values) {
    if (typeof value === 'string' && value.trim()) return value.trim();
  }
  return null;
}

export function extractPodAddressFragment(podJson: Record<string, unknown>): string | null {
  const address =
    (podJson.address as Record<string, unknown> | undefined) ??
    (podJson.addressResponse as Record<string, unknown> | undefined) ??
    (podJson.podAddress as Record<string, unknown> | undefined) ??
    {};
  const local =
    (address.localAddressData as Record<string, unknown> | undefined) ??
    (address.localAddress as Record<string, unknown> | undefined) ??
    {};
  return firstString([
    podJson.populatedPlaceName,
    address.populatedPlaceName,
    local.populatedPlaceName,
    address.streetName,
    local.streetName,
    podJson.streetName,
  ]);
}

export function findRowByIdRaw(content: ListingRow[] | undefined, idRaw: number): ListingRow | undefined {
  return (content ?? []).find((row) => Number(row.idRaw) === Number(idRaw));
}

export function findRowByOutgoingDocumentId(
  content: ListingRow[] | undefined,
  invoiceId: number,
): ListingRow | undefined {
  return (content ?? []).find((row) => String(row.outgoingDocumentId ?? '') === String(invoiceId));
}

export function findDepositRow(content: ListingRow[] | undefined, depositId: number): ListingRow | undefined {
  return (content ?? []).find(
    (row) => row.object === 'deposit' && Number(row.idRaw) === Number(depositId),
  );
}

export function isBlankBillingGroup(value: unknown): boolean {
  return value === null || value === undefined || value === '';
}

export async function postListing(
  fx: PodListingFx,
  customerId: number,
  body: Record<string, unknown>,
): Promise<{ status: number; json: ListingPage; rawText: string }> {
  const { Request } = fx;
  const res = await Request.post(listingPath(customerId), { data: body });
  const status = res.status();
  const rawText = await res.text();
  let json: ListingPage = {};
  try {
    json = rawText ? (JSON.parse(rawText) as ListingPage) : {};
  } catch {
    json = {};
  }
  return { status, json, rawText };
}

export async function postListingOk(
  fx: PodListingFx,
  customerId: number,
  body: Record<string, unknown> = defaultListingBody(),
): Promise<ListingPage> {
  const { Request } = fx;
  const res = await Request.post(listingPath(customerId), { data: body });
  await expect(res).CheckResponse();
  expect(res.status(), 'listing HTTP must be 206 PARTIAL_CONTENT').toBe(206);
  return (await res.json()) as ListingPage;
}

export async function createLegalCustomer(
  fx: PodListingFx,
): Promise<{ customerId: number; customerDetailsId: number }> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const res = await Request.post(Endpoints.customer, {
    data: GeneratePayload.customers.customer_legal(),
  });
  await expect(res).CheckResponse();
  const body = (await res.json()) as {
    id?: number;
    lastCustomerDetailId?: number;
    customerDetailsId?: number;
  };
  Responses.customer.push(body);
  const customerId = Number(body.id);
  expect(customerId, 'customer.customers.id').toBeGreaterThan(0);
  let customerDetailsId = Number(body.lastCustomerDetailId ?? body.customerDetailsId ?? 0);
  if (!customerDetailsId) {
    const getRes = await Request.get(`customer/${customerId}?version=1`);
    if (getRes.ok()) {
      const getJson = (await getRes.json()) as {
        lastCustomerDetailId?: number;
        customerDetailsId?: number;
      };
      customerDetailsId = Number(getJson.lastCustomerDetailId ?? getJson.customerDetailsId ?? 0);
    }
  }
  return { customerId, customerDetailsId };
}

export async function createLegalCustomerWithDistinctDetailId(
  fx: PodListingFx,
): Promise<{ customerId: number; customerDetailsId: number }> {
  for (let attempt = 0; attempt < 3; attempt += 1) {
    const created = await createLegalCustomer(fx);
    if (created.customerDetailsId > 0 && created.customerDetailsId !== created.customerId) {
      const body = fx.Responses.customer[fx.Responses.customer.length - 1];
      fx.Responses.customer.splice(0, fx.Responses.customer.length, body);
      return created;
    }
    fx.Responses.customer.pop();
  }
  throw new Error(
    'Setup failure: customerId and lastCustomerDetailId were equal after 3 creates. Cannot prove TC-BE-16 path mix-up.',
  );
}

export async function createManualLiability(
  fx: PodListingFx,
  opts: { initialAmount?: number; billingGroupId?: number | null } = {},
): Promise<number> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const payload = GeneratePayload.receivablesManagement.customer_liability();
  payload.initialAmount = opts.initialAmount ?? MANUAL_AMOUNT;
  payload.billingGroupId = opts.billingGroupId === undefined ? null : opts.billingGroupId;
  const res = await Request.post(Endpoints.customerLiability, { data: payload });
  await expect(res).CheckResponse();
  const id = (await res.json()) as number;
  expect(id, 'customer-liability POST body must be numeric id').toBeGreaterThan(0);
  Responses.customerLiability.push(id);
  return id;
}

export async function createManualReceivable(
  fx: PodListingFx,
  opts: { initialAmount?: number; billingGroupId?: number | null } = {},
): Promise<number> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const payload = GeneratePayload.receivablesManagement.customer_receivable();
  payload.initialAmount = opts.initialAmount ?? MANUAL_AMOUNT;
  payload.billingGroupId = opts.billingGroupId === undefined ? null : opts.billingGroupId;
  const res = await Request.post(Endpoints.customerReceivable, { data: payload });
  await expect(res).CheckResponse();
  const id = entityId(await res.json());
  Responses.customerReceivable.push(id);
  return id;
}

export async function createDeposit(fx: PodListingFx, initialAmount = DEPOSIT_AMOUNT): Promise<number> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const payload = GeneratePayload.receivablesManagement.deposit();
  payload.initialAmount = initialAmount;
  const res = await Request.post(Endpoints.deposit, { data: payload });
  await expect(res).CheckResponse();
  const id = entityId(await res.json());
  Responses.deposit.push(id);
  return id;
}

export async function getDepositAmounts(
  fx: PodListingFx,
  depositId: number,
): Promise<{ currentAmount: number; initialAmount: number; refunded: boolean }> {
  const { Request } = fx;
  const res = await Request.get(`deposit/${depositId}`);
  await expect(res).CheckResponse();
  const body = (await res.json()) as {
    currentAmount?: number;
    initialAmount?: number;
    refunded?: boolean;
  };
  return {
    currentAmount: Number(body.currentAmount),
    initialAmount: Number(body.initialAmount),
    refunded: Boolean(body.refunded),
  };
}

export async function reduceDepositViaMlo(fx: PodListingFx, depositId: number): Promise<number> {
  const { Request, GeneratePayload, Responses } = fx;
  expect(entityId(Responses.deposit[0]), 'Responses.deposit[0] must be the created deposit').toBe(
    depositId,
  );

  const customerGet = await Request.get(`customer/${entityId(Responses.customer[0])}`);
  await expect(customerGet).CheckResponse();
  const customerIdentifier = String(
    ((await customerGet.json()) as { identifier?: string }).identifier ?? '',
  );
  expect(customerIdentifier, 'customer identifier for deposit-liability search').toBeTruthy();

  const depositLiabilityList = await Request.get(
    `customer-liability/list?page=0&size=25&columns=ID&direction=DESC&prompt=${customerIdentifier}&searchFields=CUSTOMER`,
  );
  await expect(depositLiabilityList).CheckResponse();
  const listJson = (await depositLiabilityList.json()) as { content?: Array<{ id?: number }> };
  const autoDepositLiabilityId = Number(listJson.content?.[0]?.id);
  expect(autoDepositLiabilityId, 'auto-created deposit liability id').toBeGreaterThan(0);
  Responses.customerLiability.push(autoDepositLiabilityId);

  await createManualReceivable(fx, { initialAmount: DEPOSIT_AMOUNT });

  const payDepositLiability = await Request.post('manual-liability-offsetting', {
    data: await GeneratePayload.receivablesManagement.MLO_L_R(),
  });
  await expect(payDepositLiability).CheckResponse();
  Responses.manualLiabilityOffsetting.push(await payDepositLiability.json());

  await expect
    .poll(
      async () => {
        const job = await Request.post('deposit/job');
        return job.status();
      },
      { timeout: 60_000, intervals: [2000, 5000] },
    )
    .toBe(204);

  const ready = await getDepositAmounts(fx, depositId);
  expect(ready.currentAmount, 'deposit currentAmount equals initialAmount after job').toBe(
    ready.initialAmount,
  );

  const offsetLiabilityId = await createManualLiability(fx, { initialAmount: DEPOSIT_AMOUNT });

  const useDeposit = await Request.post('manual-liability-offsetting', {
    data: await GeneratePayload.receivablesManagement.MLO_D_L(),
  });
  await expect(useDeposit).CheckResponse();
  Responses.manualLiabilityOffsetting.push(await useDeposit.json());

  const after = await getDepositAmounts(fx, depositId);
  expect(after.currentAmount, 'deposit currentAmount must drop below initialAmount').toBeLessThan(
    after.initialAmount,
  );
  expect(after.refunded, 'deposit must not be refunded').toBe(false);
  return offsetLiabilityId;
}

async function createTermsAndPriceProduct(
  fx: PodListingFx,
  kind: 'settlement' | 'scale',
  scaleTariffId?: number,
): Promise<number> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;

  const term = await Request.post(Endpoints.terms, { data: GeneratePayload.productAndServices.term() });
  await expect(term).CheckResponse();
  Responses.terms.push(await term.json());

  if (kind === 'scale') {
    expect(scaleTariffId, 'scale tariff id').toBeGreaterThan(0);
    const pcPayload = GeneratePayload.productAndServices.scaleComponent();
    pcPayload.formulaRequest.expression = '100';
    pcPayload.formulaRequest.issuedSeparateInvoice = 'INVOICE_ONE';
    pcPayload.applicationModelRequest.volumesByScaleRequest.scaleIds = [scaleTariffId as number];
    const pc = await Request.post(Endpoints.priceComponent, { data: pcPayload });
    await expect(pc).CheckResponse();
    const pcId = asPriceComponentId(await pc.json());
    Responses.priceComponent.push(pcId);
  } else {
    const pc = await Request.post(Endpoints.priceComponent, {
      data: GeneratePayload.productAndServices.priceSettlement(),
    });
    await expect(pc).CheckResponse();
    Responses.priceComponent.push(asPriceComponentId(await pc.json()));
  }

  const productPayload = GeneratePayload.productAndServices.product();
  productPayload.contractTypes = ['SUPPLY_ONLY'];
  productPayload.paymentGuarantees = ['NO'];
  productPayload.availableForSale = true;
  productPayload.priceComponentIds = Responses.priceComponent.map((item) =>
    typeof item === 'number' ? item : asPriceComponentId(item),
  );
  productPayload.priceComponentGroupIds = [];
  const product = await Request.post(Endpoints.product, { data: productPayload });
  await expect(product).CheckResponse();
  const productBody = await product.json();
  Responses.product.push(productBody);
  return entityId(productBody);
}

function uniquePodIdentifier(workerIndex: number, iteration: number): string {
  const suffix = `${workerIndex}${String(iteration).padStart(5, '0')}`;
  const core = randomGens.generateUniqueIdentifier();
  return (`32X${core}${suffix}`).slice(0, 33);
}

export async function createSettlementPod(
  fx: PodListingFx,
  workerIndex: number,
  iteration: number,
): Promise<PodView> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const payload = GeneratePayload.pointsOfDelivery.pod_settlement() as Record<string, unknown>;
  payload.identifier = uniquePodIdentifier(workerIndex, iteration);
  const res = await Request.post(Endpoints.pod, { data: payload });
  await expect(res).CheckResponse();
  const created = (await res.json()) as Record<string, unknown>;
  const podId = Number(created.id);
  expect(podId, 'POD create id').toBeGreaterThan(0);
  const identifier = String(payload.identifier ?? created.identifier ?? '').trim();
  expect(identifier, 'POD identifier').toBeTruthy();
  Responses.pod.push({
    ...created,
    id: podId,
    identifier,
    podDetailId: created.podDetailId ?? created.lastPodDetailId,
    estimatedMonthlyAvgConsumption: Number(payload.estimatedMonthlyAvgConsumption ?? 1),
    type: payload.type ?? 'CONSUMER',
  } as (typeof Responses.pod)[number]);
  return { podId, identifier, addressFragment: null };
}

async function createScalePodWithMeter(fx: PodListingFx): Promise<PodView> {
  const { Request, GeneratePayload, Responses, Endpoints, Nomenclatures } = fx;
  const gridId = await Nomenclatures.grid_operator(GRID_OPERATOR_CODE);
  const measurementTypeId = await Nomenclatures.measurement_type(GRID_OPERATOR_CODE, gridId);
  const scaleCodeId = await Nomenclatures.scales_code('scaleCode');
  const scaleTariffId = await Nomenclatures.scales_tariff(GRID_OPERATOR_CODE);

  const podPayload = GeneratePayload.pointsOfDelivery.pod_slp();
  podPayload.gridOperatorId = gridId;
  podPayload.measurementTypeId = measurementTypeId;
  const podRes = await Request.post(Endpoints.pod, { data: podPayload });
  await expect(podRes).CheckResponse();
  const podCreated = (await podRes.json()) as { id?: number; podDetailId?: number };
  const podId = Number(podCreated.id);
  expect(podId, 'SLP POD id').toBeGreaterThan(0);
  const identifier = String(podPayload.identifier ?? '').trim();
  expect(identifier, 'SLP POD identifier').toBeTruthy();
  Responses.pod.push({
    ...podCreated,
    id: podId,
    identifier,
    podDetailId: podCreated.podDetailId,
    gridOperatorId: gridId,
    estimatedMonthlyAvgConsumption: Number(podPayload.estimatedMonthlyAvgConsumption ?? 0),
    type: podPayload.type ?? 'CONSUMER',
  } as (typeof Responses.pod)[number]);

  const meterPayload = GeneratePayload.pointsOfDelivery.meters() as {
    gridOperatorId: number | null;
    podId: number | null;
    meterScales: number[];
  };
  meterPayload.gridOperatorId = gridId;
  meterPayload.podId = podId;
  meterPayload.meterScales = [scaleCodeId, scaleTariffId];
  const meterRes = await Request.post(Endpoints.meters, { data: meterPayload });
  await expect(meterRes).CheckResponse();
  Responses.meters.push(await meterRes.json());

  return { podId, identifier, addressFragment: null };
}

export async function enrichPodView(fx: PodListingFx, pod: PodView): Promise<PodView> {
  const { Request } = fx;
  const res = await Request.get(`pod/${pod.podId}?version=1`);
  if (!res.ok()) {
    return pod;
  }
  const json = (await res.json()) as Record<string, unknown>;
  const identifier = String(json.identifier ?? pod.identifier).trim();
  return {
    podId: pod.podId,
    identifier,
    addressFragment: extractPodAddressFragment(json),
  };
}

async function createSignedProductContract(fx: PodListingFx, activationFrom?: string): Promise<number> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const contractPayload = await GeneratePayload.contractsAndOrders.product_contract();
  contractPayload.productParameters.contractType = 'SUPPLY_ONLY';
  contractPayload.productParameters.paymentGuarantee = 'NO';
  let monthlySum = 0;
  for (const pod of Responses.pod) {
    const row = pod as { type?: string; estimatedMonthlyAvgConsumption?: number };
    if ((row.type ?? 'CONSUMER') === 'CONSUMER') {
      monthlySum += Number(row.estimatedMonthlyAvgConsumption ?? 0);
    }
  }
  contractPayload.additionalParameters.estimatedTotalConsumptionUnderContractKwh =
    (monthlySum * 12) / 1000;
  const contract = await Request.post(Endpoints.productContract, { data: contractPayload });
  await expect(contract).CheckResponse();
  const contractBody = await contract.json();
  Responses.productContract.push(contractBody);
  const contractId = entityId(contractBody);

  for (let i = 0; i < Responses.pod.length; i += 1) {
    const activation = await Request.post('/contract-pods/manual', {
      data: await GeneratePayload.pointsOfDelivery.pod_activation(i, activationFrom),
    });
    await expect(activation).CheckResponse();
  }
  return contractId;
}

export async function getFirstBillingGroup(
  fx: PodListingFx,
): Promise<{ id: number; groupNumber?: string; sendingInvoice?: string; separateInvoiceForEachPod?: boolean }> {
  const { Request, Responses } = fx;
  const contractId = entityId(Responses.productContract[0]);
  const listRes = await Request.get(`billing-group/list/${contractId}`);
  if (listRes.ok()) {
    const listJson = (await listRes.json()) as {
      content?: Array<{ id?: number }>;
      id?: number;
    };
    const first = Array.isArray(listJson.content) ? listJson.content[0] : listJson;
    if (first?.id) {
      const bgRes = await Request.get(`billing-group/${first.id}`);
      await expect(bgRes).CheckResponse();
      return (await bgRes.json()) as { id: number; groupNumber?: string; sendingInvoice?: string; separateInvoiceForEachPod?: boolean };
    }
  }
  const contractGet = await Request.get(`product-contract/${contractId}?version=1`);
  await expect(contractGet).CheckResponse();
  const contractJson = (await contractGet.json()) as { billingGroups?: Array<{ id?: number }> };
  const billingGroupId = Number(contractJson.billingGroups?.[0]?.id);
  expect(billingGroupId, 'billing group id from product-contract').toBeGreaterThan(0);
  const bgRes = await Request.get(`billing-group/${billingGroupId}`);
  await expect(bgRes).CheckResponse();
  return (await bgRes.json()) as { id: number; groupNumber?: string; sendingInvoice?: string; separateInvoiceForEachPod?: boolean };
}

export async function setSeparateInvoiceForEachPod(fx: PodListingFx, enabled: boolean): Promise<number> {
  const { Request, GeneratePayload } = fx;
  if (enabled) {
    const enabledRes = await enableSeparateInvoiceForEachPod(Request, GeneratePayload);
    return enabledRes.billingGroupId;
  }
  const payload = await GeneratePayload.contractsAndOrders.editBillingGroup(0);
  payload.separateInvoiceForEachPod = false;
  const editRes = await Request.put(`billing-group/${payload.id}`, { data: payload });
  await expect(editRes).CheckResponse();
  return Number(payload.id);
}

export async function runManualContractChain(
  fx: PodListingFx,
  podCount: number,
  workerIndex = 0,
  separateInvoice: boolean | null = null,
): Promise<ManualContractChain> {
  const { customerId, customerDetailsId } = await createLegalCustomer(fx);
  await createTermsAndPriceProduct(fx, 'settlement');
  const pods: PodView[] = [];
  for (let i = 0; i < podCount; i += 1) {
    const created = await createSettlementPod(fx, workerIndex, i + 1);
    pods.push(await enrichPodView(fx, created));
  }
  const contractId = await createSignedProductContract(fx);
  if (separateInvoice !== null) {
    await setSeparateInvoiceForEachPod(fx, separateInvoice);
  }
  const billingGroup = await getFirstBillingGroup(fx);
  if (separateInvoice !== null) {
    expect(
      billingGroup.separateInvoiceForEachPod,
      `billing group separateInvoiceForEachPod must be ${separateInvoice}`,
    ).toBe(separateInvoice);
  }
  return {
    customerId,
    customerDetailsId,
    contractId,
    billingGroupId: billingGroup.id,
    pods,
  };
}

async function buildScaleBillingPayload(
  fx: PodListingFx,
  podIndex: number,
  period: PeriodWindow,
): Promise<Record<string, unknown>> {
  const { Request, Responses } = fx;
  const meterRaw = Responses.meters[podIndex];
  const meterId = typeof meterRaw === 'number' ? meterRaw : entityId(meterRaw);
  const meterGet = await Request.get(`meters/${meterId}`);
  await expect(meterGet).CheckResponse();
  const meterJson = (await meterGet.json()) as {
    number?: string;
    meterScales?: Array<{ id?: number }>;
  };

  type ScaleJson = {
    id?: number;
    scaleType?: string | null;
    scaleCode?: string | null;
    tariffOrScale?: string | null;
  };
  const scaleDetails: ScaleJson[] = [];
  for (const meterScale of meterJson.meterScales ?? []) {
    const scaleId = meterScale?.id;
    if (scaleId == null) continue;
    const scaleGet = await Request.get(`scales/${scaleId}`);
    await expect(scaleGet).CheckResponse();
    scaleDetails.push((await scaleGet.json()) as ScaleJson);
  }
  const scaleTariffJson =
    scaleDetails.find((s) => Boolean(s.tariffOrScale) && !s.scaleCode) ??
    scaleDetails.find((s) => Boolean(s.tariffOrScale));
  const scaleCodeJson =
    scaleDetails.find((s) => Boolean(s.scaleCode) && !s.tariffOrScale) ??
    scaleDetails.find((s) => Boolean(s.scaleCode));
  if (!scaleTariffJson?.tariffOrScale) {
    throw new Error(`meter ${meterId} has no tariff scale (scales=${JSON.stringify(scaleDetails)})`);
  }
  if (!scaleCodeJson?.scaleCode) {
    throw new Error(`meter ${meterId} has no code scale (scales=${JSON.stringify(scaleDetails)})`);
  }

  const podEntry = Responses.pod[podIndex] as { identifier?: string };
  const identifier = String(podEntry.identifier ?? '').trim();
  expect(identifier, `Responses.pod[${podIndex}] identifier`).toBeTruthy();

  const payload = scalesPayloadTemplate() as Record<string, unknown>;
  payload.identifier = identifier;
  payload.dateFrom = period.periodFrom;
  payload.dateTo = period.periodTo;
  payload.invoiceDate = `${period.periodFrom}T00:00:00.000Z`;
  payload.invoiceNumber = '1';
  payload.correction = false;
  payload.invoiceCorrection = null;
  payload.override = false;

  const rows = payload.billingByScalesTableCreateRequests as Array<Record<string, unknown>>;
  rows[0].periodFrom = period.periodFrom;
  rows[0].periodTo = period.periodTo;
  rows[0].meterNumber = meterJson.number;
  rows[0].scaleType = scaleTariffJson.scaleType;
  rows[0].tariffScale = scaleTariffJson.tariffOrScale;
  rows[0].volumes = '40';
  rows[0].index = 0;

  rows[1].periodFrom = period.periodFrom;
  rows[1].periodTo = period.periodTo;
  rows[1].meterNumber = meterJson.number;
  rows[1].scaleCode = scaleCodeJson.scaleCode;
  rows[1].scaleNumber = randomGens.getRandomEven().toString();
  rows[1].scaleType = scaleCodeJson.scaleType;
  rows[1].newMeterReading = '5';
  rows[1].oldMeterReading = '0';
  rows[1].difference = '5';
  rows[1].multiplier = '8';
  rows[1].totalVolumes = '40';
  rows[1].index = 1;

  payload.billingByScalesTableCreateRequests = [rows[0], rows[1]];
  return payload;
}

export async function getInvoiceDetailedPods(fx: PodListingFx, invoiceId: number): Promise<string[]> {
  const { Request } = fx;
  const res = await Request.get(`invoice/detailed-data?id=${invoiceId}&page=0&size=50`);
  await expect(res).CheckResponse();
  const body = (await res.json()) as { content?: Array<Record<string, unknown>> };
  const pods = new Set<string>();
  for (const row of body.content ?? []) {
    const pod = String(row.pointOfDelivery ?? '').trim();
    if (pod) pods.add(pod);
  }
  return [...pods];
}

export async function ensureLiabilityForInvoice(fx: PodListingFx, invoiceId: number): Promise<void> {
  const { Request } = fx;
  const res = await Request.post(`customer-liability/test/invoice/${invoiceId}`);
  if (res.ok()) {
    const raw = await res.json().catch(() => null);
    if (raw != null) {
      try {
        fx.Responses.customerLiability.push(entityId(raw));
      } catch {
        /* listing will still match by outgoingDocumentId */
      }
    }
  }
}

export async function runVolumeInvoiceChain(
  fx: PodListingFx,
  opts: { podCount: number; separateInvoice: boolean; expectedInvoiceCount: number },
): Promise<VolumeInvoiceChain> {
  const { Request, GeneratePayload, Responses, Endpoints, Nomenclatures } = fx;
  const period = computeCurrentMonthWindow();
  const { customerId, customerDetailsId } = await createLegalCustomer(fx);

  const scaleTariffId = await Nomenclatures.scales_tariff(GRID_OPERATOR_CODE);
  await createTermsAndPriceProduct(fx, 'scale', scaleTariffId);

  const pods: PodView[] = [];
  for (let i = 0; i < opts.podCount; i += 1) {
    const created = await createScalePodWithMeter(fx);
    pods.push(await enrichPodView(fx, created));
  }

  const contractId = await createSignedProductContract(fx, period.periodFrom);
  await setSeparateInvoiceForEachPod(fx, opts.separateInvoice);
  const billingGroup = await getFirstBillingGroup(fx);
  expect(
    billingGroup.separateInvoiceForEachPod,
    `separateInvoiceForEachPod must be ${opts.separateInvoice} before billing`,
  ).toBe(opts.separateInvoice);

  for (let i = 0; i < opts.podCount; i += 1) {
    const payload = await buildScaleBillingPayload(fx, i, period);
    const scalesRes = await Request.post(Endpoints.dataByScales, { data: payload });
    await expect(scalesRes).CheckResponse();
    Responses.dataByScales.push(await scalesRes.json());
  }

  const billingPayload = await GeneratePayload.billing.billingRun('CONTRACT', ['FOR_VOLUMES']);
  const billingRun = await Request.post(Endpoints.billingRun, { data: billingPayload });
  await expect(billingRun).CheckResponse();
  const billingRunId = entityId(await billingRun.json());
  Responses.billingRun.push({ id: billingRunId, invoiceNumbers: opts.expectedInvoiceCount });

  await GeneratePayload.billing.waitForInvoiceGeneration(true, true, opts.expectedInvoiceCount, 0);

  const invoiceIds = Responses.invoice.slice(-opts.expectedInvoiceCount).map((item) => entityId(item));
  expect(invoiceIds.length, `expected ${opts.expectedInvoiceCount} REAL invoice(s)`).toBe(
    opts.expectedInvoiceCount,
  );
  for (const invoiceId of invoiceIds) {
    const invoiceRes = await Request.get(`invoice?id=${invoiceId}`);
    await expect(invoiceRes).CheckResponse();
    const invoice = (await invoiceRes.json()) as { invoiceStatus?: string; status?: string };
    expect(
      invoice.invoiceStatus ?? invoice.status,
      `invoice ${invoiceId} must be REAL`,
    ).toBe('REAL');
  }

  if (opts.expectedInvoiceCount === 1 && opts.podCount > 1 && !opts.separateInvoice) {
    const detailedPods = await getInvoiceDetailedPods(fx, invoiceIds[0]);
    if (detailedPods.length < 2) {
      throw new Error(
        `Setup failure: combined invoice ${invoiceIds[0]} detailed-data has ${detailedPods.length} POD(s); need ≥2.`,
      );
    }
  }

  if (opts.separateInvoice && opts.podCount > 1) {
    const podsOnInvoices: string[] = [];
    for (const invoiceId of invoiceIds) {
      const detailedPods = await getInvoiceDetailedPods(fx, invoiceId);
      if (detailedPods.length !== 1) {
        throw new Error(
          `Setup failure: Separate Invoice is true but invoice ${invoiceId} detailed-data has ${detailedPods.length} POD(s). Not a listing defect.`,
        );
      }
      podsOnInvoices.push(detailedPods[0]);
    }
    if (new Set(podsOnInvoices).size !== opts.podCount) {
      throw new Error(
        `Setup failure: invoices did not split one-POD-each. PODS=${JSON.stringify(podsOnInvoices)}`,
      );
    }
  }

  for (const invoiceId of invoiceIds) {
    await ensureLiabilityForInvoice(fx, invoiceId);
  }

  return {
    customerId,
    customerDetailsId,
    contractId,
    billingGroupId: billingGroup.id,
    pods,
    invoiceIds,
    billingRunId,
    period,
  };
}
