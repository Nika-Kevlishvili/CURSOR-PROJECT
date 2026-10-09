/**
 * EXP-PARITY-04 — PreProd vs Experiment billing-run object lock parity.
 *
 * NAMING: `EXP-PARITY-04` is a PSEUDO-KEY placeholder (no Jira ticket yet).
 *
 * Probe strategy
 * ---------------
 * Primary lock probe: `PUT /product-contract/{id}?versionId=&changeFutureVersionsPods=`
 * (`@WithLockValid(entityType = "energy-product-contracts")` on
 * `ProductContractController.update`).
 *
 * `run_main_data_preparation_status` is a DB column
 * (`billing_run.run_main_data_preparation_status`) — it is **not** exposed on
 * `BillingRunResponse` in Experiment Swagger (refreshed this session). Window
 * detection therefore uses behavioural proxies:
 *   * `commonParameters.status === 'IN_PROGRESS_DRAFT'` while start-billing runs
 *   * first contract `PUT` returning non-2xx **before** status reaches `DRAFT`
 *     ⇒ prep finished + `standard_billing_stage_21_lock_objects` applied
 *
 * Wiki (pages 143163393 / 585729954) vs Experiment runtime:
 *   * Wiki: lock starts when the run starts (two stages) but changes still apply
 *     until lock completes.
 *   * Experiment: `standard_billing_stage_21_lock_objects` at END of volumes prep
 *     sets `run_main_data_preparation_status = FINISHED` then locks.
 *   * Window 1 (before prep FINISHED) is an open parity question — record only.
 *   * Window 2 (after prep FINISHED, still `IN_PROGRESS_DRAFT`) — hard assert.
 *
 * Swagger (`Cursor-Project/config/swagger/experiment/swagger-spec.json`, Rule 41):
 * - `PATCH /billing-run/start-billing?billingRunId=`
 * - `PATCH /billing-run/terminate?billingRunId=`
 * - `GET /billing-run/{id}` → `BillingRunResponse.commonParameters.status`
 *   enum includes `IN_PROGRESS_DRAFT` and `DRAFT`.
 * - `PUT /product-contract/{id}` query: `versionId`, `changeFutureVersionsPods`;
 *   body `ProductContractUpdateRequest`.
 * - Additional editable families for lock-list comparison (Test 2):
 *   `PUT /customer/{id}`, `/pod/{id}`, `/products/{id}`, `/price-components/{id}`,
 *   `/iap/{id}`, `/discounts/{id}`.
 *
 * Reference spec(s):
 * - tests/cursor/exp-parity-01-pulling-shared-pod-handover.fixtures.ts (volumes prechain)
 * - tests/cursor/pdt-3087-government-compensation-defer-to-pdf.fixtures.ts (terminate)
 * - tests/cursor/pdt-3206-contract-template-proxy-private-json.fixtures.ts (contract PUT)
 */

import { expect, test } from './cursor-test.fixtures';
import type { baseFixture } from './cursor-test.fixtures';
import {
  postWholeMonthFifteenMinuteProfile,
  settleBillingDataPreparationCron,
  expParityPreviousMonthWindow,
  type ExpParityMonthWindow,
} from './exp-parity-01-pulling-shared-pod-handover.fixtures';
import {
  applyExpParityCombinedProduct,
  postExpParitySignedProductContract,
} from './shared/exp-parity-contract.fixtures';
import {
  applyExpParityLocalAddress,
  postExpParityLegalCustomer,
  type ExpParityLocalAddressIds,
} from './shared/exp-parity-customer.fixtures';
import { terminateBillingRunAndWaitCancelled } from './pdt-3087-government-compensation-defer-to-pdf.fixtures';

export const EXP_PARITY_04_KEY = 'EXP-PARITY-04';

export const EXP_PARITY_04_CASE_9_ID = 'case-09-contract-edit-during-data-prep';
export const EXP_PARITY_04_CASE_10_ID = 'case-10-locked-object-set-vs-wiki';

export const EXP_PARITY_04_CASE_9_TITLE =
  'Contract edit is rejected while a billing run holds the object lock during data preparation';

export const EXP_PARITY_04_CASE_10_TITLE =
  'Locked object set after data preparation compared with the documented lock list';

/** Per-test timeout: prechain + cron settle + async billing pipeline + probes. */
export const EXP_PARITY_04_TEST_TIMEOUT_MS = 75 * 60 * 1000;

const BILLING_RUN_ROOT = 'billing-run';
const POD_MANUAL_ACTIVATION_ROOT = 'contract-pods/manual';

/** Poll interval while racing the ~2 min volumes data-prep cron. */
const LOCK_PROBE_POLL_MS = 5_000;
/** Max wait for `IN_PROGRESS_DRAFT` → `DRAFT` (Experiment async stages). */
const DRAFT_MAX_WAIT_MS = 30 * 60 * 1000;

const BILLING_RUN_TERMINAL_FAILURES = new Set([
  'DELETED',
  'CANCELLED',
  'IN_PROGRESS_TERMINATION',
]);

export type ExpParity04Fx = Pick<
  baseFixture,
  'Request' | 'GeneratePayload' | 'Responses' | 'Endpoints'
>;

export type ExpParity04Request = ExpParity04Fx['Request'];

/** Editable entity families exposed as Swagger `PUT` endpoints. */
export type LockProbeFamily =
  | 'contract'
  | 'customer'
  | 'pod'
  | 'product'
  | 'priceComponent'
  | 'iap'
  | 'discount';

export type LockProbeMoment = 'window1' | 'prepFinished' | 'draft';

export type LockProbeResult = {
  family: LockProbeFamily;
  moment: LockProbeMoment;
  billingRunStatus: string;
  httpStatus: number;
  rejected: boolean;
  errorShape: Record<string, unknown>;
};

export type ExpParity04Prechain = {
  window: ExpParityMonthWindow;
  customerId: number;
  customerIdentifier: string;
  podId: number;
  productId: number;
  contractId: number;
  contractNumber: string;
  contractVersionId: number;
  priceComponentId: number;
  billingByProfileId: number;
  /** Present only when `withDiscountAndIap` was requested (Case 10). */
  discountId: number | null;
  iapId: number | null;
  addressIds: ExpParityLocalAddressIds;
};

export type BillingRunLockWindowOutcome = {
  billingRunId: number;
  finalStatus: string;
  window1Probes: LockProbeResult[];
  prepFinishedProbes: LockProbeResult[];
  draftProbes: LockProbeResult[];
  /** True when at least one contract probe was rejected while still IN_PROGRESS_DRAFT. */
  prepFinishedLockObserved: boolean;
  /** True when prep appeared to finish before any window-1 (pre-lock) probe landed. */
  prepFinishedBeforeWindow1: boolean;
};

function asEntityId(raw: unknown): number {
  if (typeof raw === 'number' && Number.isFinite(raw) && raw > 0) {
    return raw;
  }
  if (raw && typeof raw === 'object' && 'id' in raw) {
    const value = Number((raw as { id?: unknown }).id);
    if (Number.isFinite(value) && value > 0) {
      return value;
    }
  }
  throw new Error(`Unable to read an entity id from ${JSON.stringify(raw)}`);
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

/** Normalize error JSON for snapshot diff — no free-text hard matching. */
export function normalizeLockErrorShape(body: unknown, status: number): Record<string, unknown> {
  if (body === null || body === undefined) {
    return { httpStatus: status, bodyKind: 'empty' };
  }
  if (typeof body !== 'object') {
    return { httpStatus: status, bodyKind: typeof body };
  }
  const o = body as Record<string, unknown>;
  const messages = o.messages ?? o.message ?? o.errorMessage;
  return {
    httpStatus: status,
    errorCode: o.errorCode ?? o.code ?? o.status ?? null,
    hasMessages: messages != null,
    messageCount: Array.isArray(messages) ? messages.length : messages != null ? 1 : 0,
    fieldErrorKeys:
      o.fieldErrors && typeof o.fieldErrors === 'object'
        ? Object.keys(o.fieldErrors as Record<string, unknown>).sort()
        : null,
    violationCount: Array.isArray(o.violations) ? o.violations.length : null,
  };
}

async function parseResponseBody(response: {
  status(): number;
  text(): Promise<string>;
}): Promise<{ status: number; json: unknown; text: string }> {
  const status = response.status();
  const text = await response.text().catch(() => '');
  let json: unknown = null;
  if (text) {
    try {
      json = JSON.parse(text);
    } catch {
      json = { rawText: text.slice(0, 500) };
    }
  }
  return { status, json, text };
}

export async function getBillingRunCommonStatus(
  Request: ExpParity04Request,
  billingRunId: number,
): Promise<string> {
  const res = await Request.get(`${BILLING_RUN_ROOT}/${billingRunId}`);
  await expect(res).CheckResponse();
  const body = (await res.json()) as { commonParameters?: { status?: string } };
  return String(body.commonParameters?.status ?? '');
}

export async function resolveContractVersionId(
  Request: ExpParity04Request,
  contractId: number,
): Promise<number> {
  const res = await Request.get(`product-contract/${contractId}?version=1`);
  await expect(res).CheckResponse();
  const body = (await res.json()) as {
    basicParameters?: { versionId?: number };
    versions?: Array<{ versionId?: number }>;
  };
  const fromBasic = body.basicParameters?.versionId;
  if (fromBasic != null && Number.isFinite(fromBasic)) {
    return Number(fromBasic);
  }
  const versions = body.versions ?? [];
  const max = versions.reduce((m, v) => Math.max(m, Number(v.versionId ?? 0)), 0);
  if (max > 0) {
    return max;
  }
  return 1;
}

async function buildContractEditPayload(
  fx: ExpParity04Fx,
  contractId: number,
): Promise<{ versionId: number; payload: Record<string, unknown> }> {
  const { Request, GeneratePayload } = fx;
  const versionId = await resolveContractVersionId(Request, contractId);
  const generated = (await GeneratePayload.contractsAndOrders.product_contract(
    0,
    0,
    0,
  )) as Record<string, unknown>;
  const editPayload = (await GeneratePayload.contractsAndOrders.edit_ProductContract(
    generated as never,
  )) as Record<string, unknown>;
  editPayload.savingAsNewVersion = false;
  return { versionId, payload: editPayload };
}

async function probeContractEdit(
  fx: ExpParity04Fx,
  chain: ExpParity04Prechain,
  moment: LockProbeMoment,
  billingRunStatus: string,
): Promise<LockProbeResult> {
  const { Request, Endpoints } = fx;
  const { versionId, payload } = await buildContractEditPayload(fx, chain.contractId);
  const response = await Request.put(
    `${Endpoints.productContract}/${chain.contractId}?versionId=${versionId}&changeFutureVersionsPods=false`,
    { data: payload },
  );
  const parsed = await parseResponseBody(response);
  const rejected = parsed.status < 200 || parsed.status >= 300;
  return {
    family: 'contract',
    moment,
    billingRunStatus,
    httpStatus: parsed.status,
    rejected,
    errorShape: normalizeLockErrorShape(parsed.json, parsed.status),
  };
}

async function probeCustomerEdit(
  fx: ExpParity04Fx,
  chain: ExpParity04Prechain,
  moment: LockProbeMoment,
  billingRunStatus: string,
): Promise<LockProbeResult> {
  const { Request, GeneratePayload, Endpoints } = fx;
  const getRes = await Request.get(`${Endpoints.customer}/${chain.customerId}`);
  await expect(getRes).CheckResponse();
  const existing = (await getRes.json()) as Record<string, unknown>;
  const template = GeneratePayload.customers.customer_legal() as Record<string, unknown>;
  const versionId = Number(
    (existing as { customerDetailsVersion?: number }).customerDetailsVersion ?? 1,
  );
  const editPayload = {
    ...template,
    customerDetailsVersion: versionId,
    updateExistingVersion: true,
    customerType: existing.customerType ?? template.customerType,
    customerIdentifier: existing.identifier ?? chain.customerIdentifier,
    foreign: existing.foreign ?? template.foreign,
    businessCustomerDetails: existing.businessCustomerDetails ?? template.businessCustomerDetails,
    address: existing.address ?? template.address,
    communicationData: existing.communicationData ?? template.communicationData,
    accountManagers: existing.accountManagers ?? template.accountManagers,
    customerEditContractRequests: [],
  };
  applyExpParityLocalAddress(editPayload as Record<string, unknown>, chain.addressIds);
  const response = await Request.put(`${Endpoints.customer}/${chain.customerId}`, {
    data: editPayload,
  });
  const parsed = await parseResponseBody(response);
  return {
    family: 'customer',
    moment,
    billingRunStatus,
    httpStatus: parsed.status,
    rejected: parsed.status < 200 || parsed.status >= 300,
    errorShape: normalizeLockErrorShape(parsed.json, parsed.status),
  };
}

async function probePodEdit(
  fx: ExpParity04Fx,
  chain: ExpParity04Prechain,
  moment: LockProbeMoment,
  billingRunStatus: string,
): Promise<LockProbeResult> {
  const { Request, GeneratePayload, Endpoints } = fx;
  const getRes = await Request.get(`${Endpoints.pod}/${chain.podId}?version=1`);
  await expect(getRes).CheckResponse();
  const podJson = (await getRes.json()) as {
    identifier?: string;
    name?: string;
    gridOperatorId?: number;
    versionId?: number;
    versions?: { version?: number }[];
  };
  const editPayload = GeneratePayload.pointsOfDelivery.pod_settlement() as Record<string, unknown>;
  applyExpParityLocalAddress(editPayload, chain.addressIds);
  editPayload.identifier = String(podJson.identifier ?? editPayload.identifier);
  editPayload.name = String(podJson.name ?? editPayload.name);
  editPayload.updateExistingVersion = true;
  editPayload.versionId = podJson.versionId ?? podJson.versions?.[0]?.version ?? 1;
  if (podJson.gridOperatorId != null) {
    editPayload.gridOperatorId = podJson.gridOperatorId;
  }
  const response = await Request.put(`${Endpoints.pod}/${chain.podId}`, { data: editPayload });
  const parsed = await parseResponseBody(response);
  return {
    family: 'pod',
    moment,
    billingRunStatus,
    httpStatus: parsed.status,
    rejected: parsed.status < 200 || parsed.status >= 300,
    errorShape: normalizeLockErrorShape(parsed.json, parsed.status),
  };
}

async function probeProductEdit(
  fx: ExpParity04Fx,
  chain: ExpParity04Prechain,
  moment: LockProbeMoment,
  billingRunStatus: string,
): Promise<LockProbeResult> {
  const { Request, Endpoints } = fx;
  const getRes = await Request.get(`${Endpoints.product}/${chain.productId}`);
  await expect(getRes).CheckResponse();
  const existing = (await getRes.json()) as Record<string, unknown>;
  const productTerms = Array.isArray(existing.productTerms)
    ? existing.productTerms
    : Array.isArray(existing.productContractTerms)
      ? existing.productContractTerms
      : [];
  const editPayload = {
    ...existing,
    productTerms,
    updateExistingVersion: true,
  };
  const response = await Request.put(`${Endpoints.product}/${chain.productId}`, {
    data: editPayload,
  });
  const parsed = await parseResponseBody(response);
  return {
    family: 'product',
    moment,
    billingRunStatus,
    httpStatus: parsed.status,
    rejected: parsed.status < 200 || parsed.status >= 300,
    errorShape: normalizeLockErrorShape(parsed.json, parsed.status),
  };
}

async function probePriceComponentEdit(
  fx: ExpParity04Fx,
  chain: ExpParity04Prechain,
  moment: LockProbeMoment,
  billingRunStatus: string,
): Promise<LockProbeResult> {
  const { Request, Endpoints } = fx;
  const getRes = await Request.get(`${Endpoints.priceComponent}/${chain.priceComponentId}`);
  await expect(getRes).CheckResponse();
  const existing = (await getRes.json()) as Record<string, unknown>;
  const editPayload = { ...existing };
  const response = await Request.put(`${Endpoints.priceComponent}/${chain.priceComponentId}`, {
    data: editPayload,
  });
  const parsed = await parseResponseBody(response);
  return {
    family: 'priceComponent',
    moment,
    billingRunStatus,
    httpStatus: parsed.status,
    rejected: parsed.status < 200 || parsed.status >= 300,
    errorShape: normalizeLockErrorShape(parsed.json, parsed.status),
  };
}

async function probeIapEdit(
  fx: ExpParity04Fx,
  chain: ExpParity04Prechain,
  moment: LockProbeMoment,
  billingRunStatus: string,
): Promise<LockProbeResult> {
  if (chain.iapId == null) {
    return {
      family: 'iap',
      moment,
      billingRunStatus,
      httpStatus: 0,
      rejected: false,
      errorShape: { skipped: true, reason: 'no_iap_in_prechain' },
    };
  }
  const { Request, Endpoints } = fx;
  const getRes = await Request.get(`${Endpoints.interim}/${chain.iapId}`);
  await expect(getRes).CheckResponse();
  const existing = (await getRes.json()) as Record<string, unknown>;
  const response = await Request.put(`${Endpoints.interim}/${chain.iapId}`, { data: existing });
  const parsed = await parseResponseBody(response);
  return {
    family: 'iap',
    moment,
    billingRunStatus,
    httpStatus: parsed.status,
    rejected: parsed.status < 200 || parsed.status >= 300,
    errorShape: normalizeLockErrorShape(parsed.json, parsed.status),
  };
}

async function probeDiscountEdit(
  fx: ExpParity04Fx,
  chain: ExpParity04Prechain,
  moment: LockProbeMoment,
  billingRunStatus: string,
): Promise<LockProbeResult> {
  if (chain.discountId == null) {
    return {
      family: 'discount',
      moment,
      billingRunStatus,
      httpStatus: 0,
      rejected: false,
      errorShape: { skipped: true, reason: 'no_discount_in_prechain' },
    };
  }
  const { Request, Endpoints } = fx;
  const getRes = await Request.get(`${Endpoints.discount}/${chain.discountId}`);
  await expect(getRes).CheckResponse();
  const existing = (await getRes.json()) as Record<string, unknown>;
  const response = await Request.put(`${Endpoints.discount}/${chain.discountId}`, {
    data: existing,
  });
  const parsed = await parseResponseBody(response);
  return {
    family: 'discount',
    moment,
    billingRunStatus,
    httpStatus: parsed.status,
    rejected: parsed.status < 200 || parsed.status >= 300,
    errorShape: normalizeLockErrorShape(parsed.json, parsed.status),
  };
}

/** Run all Swagger-discovered PUT probes (contract required; others when prechain created them). */
export async function probeAllLockFamilies(
  fx: ExpParity04Fx,
  chain: ExpParity04Prechain,
  moment: LockProbeMoment,
  billingRunStatus: string,
): Promise<LockProbeResult[]> {
  return [
    await probeContractEdit(fx, chain, moment, billingRunStatus),
    await probeCustomerEdit(fx, chain, moment, billingRunStatus),
    await probePodEdit(fx, chain, moment, billingRunStatus),
    await probeProductEdit(fx, chain, moment, billingRunStatus),
    await probePriceComponentEdit(fx, chain, moment, billingRunStatus),
    await probeIapEdit(fx, chain, moment, billingRunStatus),
    await probeDiscountEdit(fx, chain, moment, billingRunStatus),
  ];
}

/**
 * Single-contract FOR_VOLUMES prechain: customer, POD, product+PC, contract,
 * POD activation, whole-month 15-min profile, cron settle.
 */
export async function buildExpParity04VolumesPrechain(
  fx: ExpParity04Fx,
  options: { withDiscountAndIap: boolean },
): Promise<ExpParity04Prechain> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const window = expParityPreviousMonthWindow();

  let customerId = 0;
  let customerIdentifier = '';
  const customer = await test.step('Precondition: legal customer', async () => {
    const created = await postExpParityLegalCustomer(fx);
    customerId = created.id;
    customerIdentifier = created.identifier;
    return created;
  });

  let priceComponentId = 0;
  await test.step('Precondition: settlement price component', async () => {
    const payload = GeneratePayload.productAndServices.priceSettlement();
    const res = await Request.post(Endpoints.priceComponent, { data: payload });
    await expect(res).CheckResponse();
    const raw = await res.json();
    Responses.priceComponent.push(raw);
    priceComponentId = asEntityId(raw);
  });

  await test.step('Precondition: term', async () => {
    const res = await Request.post(Endpoints.terms, {
      data: GeneratePayload.productAndServices.term(),
    });
    await expect(res).CheckResponse();
    Responses.terms.push(await res.json());
  });

  let podId = 0;
  await test.step('Precondition: settlement POD', async () => {
    const podPayload = GeneratePayload.pointsOfDelivery.pod_settlement() as Record<string, unknown>;
    applyExpParityLocalAddress(podPayload, customer.addressIds);
    const res = await Request.post(Endpoints.pod, { data: podPayload });
    await expect(res).CheckResponse();
    const body = (await res.json()) as { id?: number };
    Responses.pod.push(body);
    podId = asEntityId(body);
  });

  let discountId: number | null = null;
  let iapId: number | null = null;
  if (options.withDiscountAndIap) {
    await test.step('Precondition: 50% discount (lock-list probe entity)', async () => {
      const discountPayload = await GeneratePayload.energyData.discount();
      const res = await Request.post(Endpoints.discount, { data: discountPayload });
      await expect(res).CheckResponse();
      const raw = await res.json();
      Responses.discount.push(raw);
      discountId = asEntityId(raw);
    });

    await test.step('Precondition: interim advance payment (lock-list probe entity)', async () => {
      const payload = GeneratePayload.productAndServices.interim();
      payload.value = 50;
      const res = await Request.post(Endpoints.interim, { data: payload });
      await expect(res).CheckResponse();
      const raw = await res.json();
      Responses.interim.push(raw);
      iapId = asEntityId(raw);
    });
  }

  let productId = 0;
  await test.step('Precondition: product (settlement PC)', async () => {
    const payload = applyExpParityCombinedProduct(GeneratePayload.productAndServices.product(0));
    payload.priceComponentIds = [Responses.priceComponent[0]];
    payload.priceComponentGroupIds = [];
    payload.interimAdvancePayments = iapId == null ? [] : [iapId];
    payload.interimAdvancePaymentGroups = [];
    const res = await Request.post(Endpoints.product, { data: payload });
    await expect(res).CheckResponse();
    const raw = await res.json();
    Responses.product.push(raw);
    productId = asEntityId(raw);
  });

  let contractId = 0;
  await test.step('Precondition: product contract', async () => {
    const payload = await GeneratePayload.contractsAndOrders.product_contract(0, 0, 0);
    if (iapId == null) {
      payload.productParameters.interimAdvancePayments = [];
    }
    const raw = await postExpParitySignedProductContract(Request, payload);
    Responses.productContract.push(raw);
    contractId = asEntityId(raw);
  });

  await test.step(
    `Precondition: POD activation for previous month (${window.start}..${window.end})`,
    async () => {
      const activation = await GeneratePayload.pointsOfDelivery.pod_activation(
        0,
        window.start,
        window.end,
        0,
      );
      const res = await Request.post(POD_MANUAL_ACTIVATION_ROOT, { data: activation });
      await expect(res).CheckResponse();
    },
  );

  const profile = await postWholeMonthFifteenMinuteProfile(fx, window);
  await settleBillingDataPreparationCron();

  const contractVersionId = await resolveContractVersionId(Request, contractId);
  const contractRes = await Request.get(`product-contract/${contractId}?version=1`);
  await expect(contractRes).CheckResponse();
  const contractBody = (await contractRes.json()) as {
    basicParameters?: { contractNumber?: string | number };
  };
  const contractNumber = String(contractBody.basicParameters?.contractNumber ?? '');

  return {
    window,
    customerId,
    customerIdentifier,
    podId,
    productId,
    contractId,
    contractNumber,
    contractVersionId,
    priceComponentId,
    billingByProfileId: profile.billingByProfileId,
    discountId,
    iapId,
    addressIds: customer.addressIds,
  };
}

/** CONTRACT-level STANDARD_BILLING / FOR_VOLUMES run listing the contract number. */
export async function createExpParity04ContractVolumesRun(
  fx: ExpParity04Fx,
  contractNumber: string,
): Promise<number> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const payload = await GeneratePayload.billing.billingRun('CONTRACT', ['FOR_VOLUMES'], 0);
  payload.listOfCustomersContractsOrPOD = contractNumber;
  const res = await Request.post(Endpoints.billingRun, { data: payload });
  await expect(res).CheckResponse();
  const billingRunId = asEntityId(await res.json());
  Responses.billingRun.push(billingRunId);
  return billingRunId;
}

/**
 * Start billing and race the data-prep cron, probing contract edit in two windows:
 * Window 1 — before first rejection while `IN_PROGRESS_DRAFT` (parity record only).
 * Window 2 — first rejection while still `IN_PROGRESS_DRAFT` (prep FINISHED proxy).
 */
export async function startBillingAndProbeContractLockWindows(
  fx: ExpParity04Fx,
  chain: ExpParity04Prechain,
  billingRunId: number,
): Promise<BillingRunLockWindowOutcome> {
  const { Request } = fx;
  const window1Probes: LockProbeResult[] = [];
  let prepFinishedProbes: LockProbeResult[] = [];
  const draftProbes: LockProbeResult[] = [];
  let prepFinishedLockObserved = false;
  let prepFinishedBeforeWindow1 = false;
  let finalStatus = '';

  await test.step(`Action: PATCH start-billing for run ${billingRunId}`, async () => {
    const startRes = await Request.patch(
      `${BILLING_RUN_ROOT}/start-billing?billingRunId=${billingRunId}`,
    );
    await expect(startRes).CheckResponse();
  });

  const deadline = Date.now() + DRAFT_MAX_WAIT_MS;
  let pollCount = 0;

  while (Date.now() < deadline) {
    finalStatus = await getBillingRunCommonStatus(Request, billingRunId);
    pollCount += 1;

    if (BILLING_RUN_TERMINAL_FAILURES.has(finalStatus)) {
      throw new Error(
        `Billing run ${billingRunId} reached terminal status ${finalStatus} during lock-window race.`,
      );
    }

    if (finalStatus === 'DRAFT') {
      const draftProbe = await probeContractEdit(fx, chain, 'draft', finalStatus);
      draftProbes.push(draftProbe);
      break;
    }

    if (finalStatus === 'IN_PROGRESS_DRAFT') {
      const probe = await probeContractEdit(fx, chain, prepFinishedLockObserved ? 'prepFinished' : 'window1', finalStatus);

      if (probe.rejected) {
        if (!prepFinishedLockObserved) {
          if (window1Probes.length === 0) {
            prepFinishedBeforeWindow1 = true;
          }
          prepFinishedLockObserved = true;
          prepFinishedProbes = [{ ...probe, moment: 'prepFinished' }];
        }
      } else if (!prepFinishedLockObserved) {
        window1Probes.push({ ...probe, moment: 'window1' });
      }
    }

    await sleep(LOCK_PROBE_POLL_MS);
  }

  if (finalStatus !== 'DRAFT') {
    throw new Error(
      `Billing run ${billingRunId} did not reach DRAFT within ${DRAFT_MAX_WAIT_MS}ms ` +
        `(last status=${finalStatus || 'unknown'}, polls=${pollCount}).`,
    );
  }

  return {
    billingRunId,
    finalStatus,
    window1Probes,
    prepFinishedProbes,
    draftProbes,
    prepFinishedLockObserved,
    prepFinishedBeforeWindow1,
  };
}

/**
 * Probe all lock families at prep-FINISHED (first contract rejection while
 * IN_PROGRESS_DRAFT) and again at DRAFT; terminate the run so locks are released.
 */
export async function startBillingProbeLockListAndTerminate(
  fx: ExpParity04Fx,
  chain: ExpParity04Prechain,
  billingRunId: number,
): Promise<{
  prepFinishedProbes: LockProbeResult[];
  draftProbes: LockProbeResult[];
  finalStatus: string;
  terminated: boolean;
  terminationStatus: string | null;
}> {
  const { Request } = fx;
  let prepFinishedProbes: LockProbeResult[] = [];
  let draftProbes: LockProbeResult[] = [];
  let finalStatus = '';
  let prepCaptured = false;

  await test.step(`Action: PATCH start-billing for run ${billingRunId}`, async () => {
    const startRes = await Request.patch(
      `${BILLING_RUN_ROOT}/start-billing?billingRunId=${billingRunId}`,
    );
    await expect(startRes).CheckResponse();
  });

  const deadline = Date.now() + DRAFT_MAX_WAIT_MS;
  while (Date.now() < deadline) {
    finalStatus = await getBillingRunCommonStatus(Request, billingRunId);

    if (BILLING_RUN_TERMINAL_FAILURES.has(finalStatus)) {
      throw new Error(`Billing run ${billingRunId} terminal status ${finalStatus}.`);
    }

    if (finalStatus === 'DRAFT') {
      draftProbes = await probeAllLockFamilies(fx, chain, 'draft', finalStatus);
      break;
    }

    if (finalStatus === 'IN_PROGRESS_DRAFT' && !prepCaptured) {
      const contractProbe = await probeContractEdit(fx, chain, 'prepFinished', finalStatus);
      if (contractProbe.rejected) {
        prepFinishedProbes = await probeAllLockFamilies(fx, chain, 'prepFinished', finalStatus);
        prepCaptured = true;
      }
    }

    await sleep(LOCK_PROBE_POLL_MS);
  }

  if (finalStatus !== 'DRAFT') {
    throw new Error(`Billing run ${billingRunId} never reached DRAFT (last=${finalStatus}).`);
  }

  let terminationStatus: string | null = null;
  let terminated = false;
  await test.step('Cleanup: terminate billing run to release per-run locks', async () => {
    terminationStatus = await terminateBillingRunAndWaitCancelled(Request, billingRunId);
    terminated = terminationStatus === 'CANCELLED';
  });

  return {
    prepFinishedProbes,
    draftProbes,
    finalStatus,
    terminated,
    terminationStatus,
  };
}

/** Snapshot-friendly projection of probe rows (no volatile ids). */
export function parityLockProbeFacts(probe: LockProbeResult): Record<string, unknown> {
  return {
    family: probe.family,
    moment: probe.moment,
    billingRunStatus: probe.billingRunStatus,
    httpStatus: probe.httpStatus,
    rejected: probe.rejected,
    errorShape: probe.errorShape,
  };
}
