/**
 * PDT-2915 — volume-only INVOICE_CORRECTION with DELETED billing_by_profile (non-zero delta vs ACTIVE).
 *
 * Dev-reproducible flow: contract prechain → BBP1 (800 kWh) + BBP2 (1000 kWh) → DELETE BBP1 **before** billing →
 * FOR_VOLUMES realize (BBP2 on invoice; BBP1 DELETED) → volume-only correction → record draft outcome.
 *
 * Reference specs:
 * - tests/billing/correction/correctionCases.spec.ts (`correctionBilling(0, false, true)`, FOR_VOLUMES chain)
 * - tests/cursor/PDT-2529-rfd-data-model-happy-path.fixtures.ts (`POST billing-by-profile`)
 * - tests/cursor/pdt-2872-minimal-interim-payment.fixtures.ts (billing-run poll / draft-invoices)
 *
 * Swagger (dev): POST/DELETE/GET `/billing-by-profile`, POST `/billing-run`, GET `/billing-run/{id}`,
 * GET `/billing-run/draft-invoices`, PATCH `/billing-run/start-billing`.
 */

import { test, expect } from '../../fixtures/baseFixture';
import type { baseFixture } from '../../fixtures/baseFixture';

/** ACTIVE profile (BBP2) — drives realized invoice. */
export const PDT2915_PROFILE_KWH = 1000;
/** DELETED profile (BBP1) — lower volume so delta vs ACTIVE is visible in portal/correction. */
export const PDT2915_DELETED_PROFILE_KWH = 800;
export const BILLING_RUN_ROOT = 'billing-run';
const DRAFT_POLL_MS = 15_000;
const DRAFT_MAX_MS = 10 * 60 * 1000;

export type Pdt2915Fx = Pick<baseFixture, 'Request' | 'GeneratePayload' | 'Responses' | 'Endpoints'>;

export type Pdt2915BbpRecord = {
  id: number;
  status?: string;
  invoiced?: boolean;
  totalValue: number;
};

export type Pdt2915DeleteAttempt = {
  attempted: boolean;
  ok: boolean;
  status?: number;
  bodySnippet?: string;
  limitation?: string;
};

export type Pdt2915ContractPrechainResult = {
  podId: number;
  podIdentifier: string;
};

export type Pdt2915DeletedBbpScenarioResult = {
  podIdentifier: string;
  firstBbp: Pdt2915BbpRecord;
  secondBbp: Pdt2915BbpRecord;
  deleteAttempt: Pdt2915DeleteAttempt;
  activeTotal: number;
  deletedTotal: number;
  originalInvoiceId: number;
  firstProfilePayload: Record<string, unknown>;
};

export type Pdt2915CorrectionDraftPoll = {
  correctionBillingRunId: number;
  billingRunSnapshot: Record<string, unknown>;
  draftInvoiceIds: number[];
  draftInvoiceCount: number;
};

/** Set kWh on profile POST payload `entries[0].value`. */
export function applyPdt2915FixedProfileVolume(payload: Record<string, unknown>, kwh: number = PDT2915_PROFILE_KWH): void {
  const entries = payload.entries as Array<Record<string, unknown>> | undefined;
  if (entries?.[0]) {
    entries[0].value = kwh;
  }
}

export function readPdt2915ProfileKwhFromPayload(payload: Record<string, unknown>): number {
  const entries = payload.entries as Array<{ value?: number | string }> | undefined;
  return Number(entries?.[0]?.value ?? PDT2915_PROFILE_KWH);
}

/** Deep-clone billing-by-profile POST payload for a second row on the same POD/period. */
export function clonePdt2915ProfilePayload(source: Record<string, unknown>): Record<string, unknown> {
  return JSON.parse(JSON.stringify(source)) as Record<string, unknown>;
}

export async function buildPdt2915ProfilePayload(
  GeneratePayload: baseFixture['GeneratePayload'],
  podIndex = 0,
): Promise<Record<string, unknown>> {
  const payload = (await GeneratePayload.energyData.profile1Month(podIndex)) as Record<string, unknown>;
  payload.timeZone = 'CET';
  applyPdt2915FixedProfileVolume(payload);
  return payload;
}

export async function postPdt2915BillingByProfile(
  Request: baseFixture['Request'],
  payload: Record<string, unknown>,
  label: string,
): Promise<Pdt2915BbpRecord> {
  const res = await Request.post('billing-by-profile', { data: payload });
  await expect(res).CheckResponse();
  const id = Number(await res.json());
  return { id, totalValue: readPdt2915ProfileKwhFromPayload(payload) };
}

/**
 * DELETE `/billing-by-profile/{id}` (Swagger dev: delete_51).
 * When invoiced=true blocks delete, returns `ok: false` and a documented limitation string.
 */
export async function deletePdt2915BillingByProfile(
  Request: baseFixture['Request'],
  bbpId: number,
): Promise<Pdt2915DeleteAttempt> {
  const res = await Request.delete(`billing-by-profile/${bbpId}`);
  const status = res.status();
  const text = await res.text().catch(() => '');
  if (res.ok()) {
    return { attempted: true, ok: true, status };
  }
  const limitation =
    status === 400 || status === 403 || status === 409
      ? `DELETE billing-by-profile blocked (HTTP ${status}) when profile was used on a realized invoice — cannot reproduce DELETED+invoiced BBP on Dev via API.`
      : `DELETE billing-by-profile failed (HTTP ${status}).`;
  return {
    attempted: true,
    ok: false,
    status,
    bodySnippet: text.slice(0, 500),
    limitation,
  };
}

export async function resolvePdt2915PodIdentifier(Request: baseFixture['Request'], podId: number): Promise<string> {
  const podRes = await Request.get(`pod/${podId}?version=1`);
  await expect(podRes).CheckResponse();
  const podJson = (await podRes.json()) as { identifier?: string };
  const identifier = podJson.identifier?.trim();
  expect(identifier, 'POD identifier required for billing-by-profile filter assertions').toBeTruthy();
  return identifier!;
}

export async function fetchPdt2915BillingByProfileTotal(
  Request: baseFixture['Request'],
  bbpId: number,
  periodFrom: string,
  periodTo: string,
): Promise<number> {
  const res = await Request.get(
    `billing-by-profile/${bbpId}?periodFrom=${encodeURIComponent(periodFrom)}&periodTo=${encodeURIComponent(periodTo)}`,
  );
  await expect(res).CheckResponse();
  const body = (await res.json()) as { entries?: Array<{ value?: number | string }> };
  const values = (body.entries ?? []).map((e) => Number(e.value ?? 0));
  return values.reduce((a, b) => a + b, 0);
}

export type Pdt2915FilterRow = {
  id?: number;
  status?: string;
  invoiced?: boolean;
};

/** POST `/billing-by-profile/filter` — list profiles for POD identifier. */
export async function listPdt2915BillingByProfilesForPod(
  Request: baseFixture['Request'],
  podIdentifier: string,
): Promise<Pdt2915FilterRow[]> {
  const res = await Request.post('billing-by-profile/filter', {
    data: { page: 0, size: 50, prompt: podIdentifier },
  });
  await expect(res).CheckResponse();
  const body = (await res.json()) as { content?: Pdt2915FilterRow[] };
  return body.content ?? [];
}

/** Contract prechain through POD activation — no billing-by-profile or billing run yet. */
export async function runPdt2915ContractPrechain(ctx: Pdt2915Fx): Promise<Pdt2915ContractPrechainResult> {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;

  await test.step('Precondition: customer', async () => {
    const customer = await Request.post(Endpoints.customer, {
      data: GeneratePayload.customers.customer_legal(),
    });
    await expect(customer).CheckResponse();
    Responses.customer.push(await customer.json());
  });

  await test.step('Precondition: price component (volume settlement)', async () => {
    const payload = GeneratePayload.productAndServices.priceSettlement();
    payload.formulaRequest.expression = '200';
    const price = await Request.post(Endpoints.priceComponent, { data: payload });
    await expect(price).CheckResponse();
    Responses.priceComponent.push(await price.json());
  });

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

  await test.step('Precondition: product', async () => {
    const product = await Request.post(Endpoints.product, { data: GeneratePayload.productAndServices.product() });
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

  await test.step('Precondition: activate POD on contract', async () => {
    const podActivation = await Request.post('/contract-pods/manual', {
      data: await GeneratePayload.pointsOfDelivery.pod_activation(0),
    });
    await expect(podActivation).CheckResponse();
  });

  const podId = Responses.pod[0].id as number;
  const podIdentifier = await resolvePdt2915PodIdentifier(Request, podId);
  return { podId, podIdentifier };
}

/** FOR_VOLUMES billing run + realized invoice (after BBP rows exist). */
export async function runPdt2915ForVolumesBillingAndRealize(ctx: Pdt2915Fx): Promise<number> {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;

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

/**
 * BBP1 (lower kWh) + BBP2 (1000 kWh) → DELETE BBP1 before billing → realize on BBP2 → delta checks.
 */
export async function setupPdt2915DeletedBbpBeforeBillingScenario(
  ctx: Pdt2915Fx,
  pre: Pdt2915ContractPrechainResult,
): Promise<Pdt2915DeletedBbpScenarioResult> {
  const { Request, GeneratePayload, Responses } = ctx;

  let firstPayload!: Record<string, unknown>;
  let firstBbp!: Pdt2915BbpRecord;
  let secondBbp!: Pdt2915BbpRecord;

  await test.step('Precondition: first billing-by-profile (BBP1, lower kWh — later DELETED)', async () => {
    firstPayload = await buildPdt2915ProfilePayload(GeneratePayload, 0);
    applyPdt2915FixedProfileVolume(firstPayload, PDT2915_DELETED_PROFILE_KWH);
    firstBbp = await postPdt2915BillingByProfile(Request, firstPayload, 'BBP1');
    Responses.dataByProfiles.push({
      id: firstBbp.id,
      periodFrom: firstPayload.periodFrom,
      periodTo: firstPayload.periodTo,
      periodType: firstPayload.periodType,
    });
  });

  await test.step('Precondition: second billing-by-profile (BBP2, ACTIVE volume 1000 kWh)', async () => {
    const secondPayload = clonePdt2915ProfilePayload(firstPayload);
    applyPdt2915FixedProfileVolume(secondPayload);
    secondBbp = await postPdt2915BillingByProfile(Request, secondPayload, 'BBP2');
  });

  const deleteAttempt = await test.step('Precondition: DELETE BBP1 before billing run', async () =>
    deletePdt2915BillingByProfile(Request, firstBbp.id),
  );

  expect(
    deleteAttempt.ok,
    deleteAttempt.limitation ?? `DELETE billing-by-profile/${firstBbp.id} must succeed before invoicing`,
  ).toBe(true);

  const originalInvoiceId = await runPdt2915ForVolumesBillingAndRealize(ctx);

  const periodFrom = String(firstPayload.periodFrom);
  const periodTo = String(firstPayload.periodTo);

  let activeTotal = 0;
  let deletedTotal = 0;
  await test.step('Verify BBP1 DELETED, BBP2 ACTIVE, non-zero volume delta', async () => {
    const rows = await listPdt2915BillingByProfilesForPod(Request, pre.podIdentifier);
    const deletedRow = rows.find((r) => r.id === firstBbp.id);
    const activeRow = rows.find((r) => r.id === secondBbp.id);
    expect(deletedRow?.status, 'BBP1 should be DELETED after pre-billing DELETE').toBe('DELETED');
    expect(activeRow?.status, 'BBP2 should remain ACTIVE on realized invoice').toBe('ACTIVE');

    deletedTotal = await fetchPdt2915BillingByProfileTotal(Request, firstBbp.id, periodFrom, periodTo);
    activeTotal = await fetchPdt2915BillingByProfileTotal(Request, secondBbp.id, periodFrom, periodTo);
    expect(deletedTotal).toBe(PDT2915_DELETED_PROFILE_KWH);
    expect(activeTotal).toBe(PDT2915_PROFILE_KWH);
    expect(activeTotal - deletedTotal, 'DELETED vs ACTIVE profile totals must differ').toBe(
      PDT2915_PROFILE_KWH - PDT2915_DELETED_PROFILE_KWH,
    );
  });

  return {
    podIdentifier: pre.podIdentifier,
    firstBbp,
    secondBbp,
    deleteAttempt,
    activeTotal,
    deletedTotal,
    originalInvoiceId,
    firstProfilePayload: firstPayload,
  };
}

/** Minimal chain: contract + one BBP + realized invoice (for DELETE guard test). */
export async function runPdt2915SingleBbpInvoicedPrechain(ctx: Pdt2915Fx): Promise<{
  bbpId: number;
  originalInvoiceId: number;
}> {
  await runPdt2915ContractPrechain(ctx);
  const { Request, GeneratePayload, Responses } = ctx;

  let bbpId = 0;
  await test.step('Precondition: billing-by-profile', async () => {
    const payload = await buildPdt2915ProfilePayload(GeneratePayload, 0);
    const bbp = await postPdt2915BillingByProfile(Request, payload, 'invoiced BBP');
    bbpId = bbp.id;
    Responses.dataByProfiles.push({
      id: bbp.id,
      periodFrom: payload.periodFrom,
      periodTo: payload.periodTo,
      periodType: payload.periodType,
    });
  });

  const originalInvoiceId = await runPdt2915ForVolumesBillingAndRealize(ctx);
  return { bbpId, originalInvoiceId };
}

/** API guard: DELETE invoiced billing-by-profile returns HTTP 400 (BillingByProfileService). */
export async function assertPdt2915DeleteInvoicedBbpBlocked(
  Request: baseFixture['Request'],
  bbpId: number,
): Promise<Pdt2915DeleteAttempt> {
  const deleteAttempt = await deletePdt2915BillingByProfile(Request, bbpId);
  expect(deleteAttempt.ok, 'DELETE must fail when profile is on a realized invoice').toBe(false);
  expect(deleteAttempt.status, 'Blocked delete should return HTTP 400').toBe(400);
  return deleteAttempt;
}

/** Volume-only correction — `correctionBilling(0, false, true)` per correctionCases REG-703 / PDT-2915. */
export async function createPdt2915VolumeCorrectionBillingRun(ctx: Pdt2915Fx): Promise<number> {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;
  const correctionPayload = await GeneratePayload.billing.correctionBilling(0, false, true);
  // Generator default leaves priceChange=true; PDT-2915 requires volume-only (no price change).
  correctionPayload.invoiceCorrectionParameters.priceChange = false;
  const correction = await Request.post(Endpoints.billingRun, { data: correctionPayload });
  await expect(correction).CheckResponse();
  const correctionId = Number(await correction.json());
  Responses.billingRun.push(correctionId);
  return correctionId;
}

export async function pollPdt2915CorrectionDraftInvoices(
  Request: baseFixture['Request'],
  correctionBillingRunId: number,
): Promise<Pdt2915CorrectionDraftPoll> {
  const startBilling = await Request.patch(`${BILLING_RUN_ROOT}/start-billing?billingRunId=${correctionBillingRunId}`);
  await expect(startBilling).CheckResponse();

  const maxAttempts = Math.floor(DRAFT_MAX_MS / DRAFT_POLL_MS);
  let lastBilling: Record<string, unknown> = {};
  let lastDraftIds: number[] = [];
  let lastCount = -1;

  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    const runRes = await Request.get(`${BILLING_RUN_ROOT}/${correctionBillingRunId}`);
    await expect(runRes).CheckResponse();
    lastBilling = (await runRes.json()) as Record<string, unknown>;

    const drafts = await Request.get(
      `${BILLING_RUN_ROOT}/draft-invoices?id=${correctionBillingRunId}&page=0&size=50`,
    );
    await expect(drafts).CheckResponse();
    const dj = (await drafts.json()) as { content?: Array<{ id?: number }> };
    lastDraftIds = (dj.content ?? [])
      .map((x) => Number(x.id))
      .filter((n) => Number.isFinite(n) && n > 0);
    lastCount = lastDraftIds.length;

    const cp = lastBilling.commonParameters as { status?: string } | undefined;
    if (cp?.status === 'DRAFT') {
      return {
        correctionBillingRunId,
        billingRunSnapshot: lastBilling,
        draftInvoiceIds: lastDraftIds,
        draftInvoiceCount: lastCount,
      };
    }
    await new Promise((r) => setTimeout(r, DRAFT_POLL_MS));
  }

  throw new Error(
    `[PDT-2915] Correction billing run ${correctionBillingRunId} did not reach DRAFT; last drafts=${lastCount} snapshot=${JSON.stringify(lastBilling.commonParameters ?? {})}`,
  );
}

async function invoiceDetailedRowsForPod(
  Request: baseFixture['Request'],
  invoiceId: number,
  podIdentifier: string,
): Promise<Array<Record<string, unknown>>> {
  const res = await Request.get(`invoice/detailed-data?page=0&size=100&id=${invoiceId}`);
  await expect(res).CheckResponse();
  const body = (await res.json()) as { content?: Array<Record<string, unknown>> };
  const rows = body.content ?? [];
  return rows.filter((r) => {
    const pod = String(r.pointOfDelivery ?? r.podIdentifier ?? '').trim();
    return pod === podIdentifier;
  });
}

/**
 * Non-zero delta run: record correction draft outcome when DELETED BBP total ≠ ACTIVE (observational).
 */
export async function recordPdt2915NonZeroDeltaCorrectionOutcome(
  Request: baseFixture['Request'],
  poll: Pdt2915CorrectionDraftPoll,
  podIdentifier: string,
  deletedTotal: number,
  activeTotal: number,
): Promise<{ volumeDeltaKwh: number; draftInvoiceCount: number; podLineCountOnDrafts: number }> {
  const volumeDeltaKwh = activeTotal - deletedTotal;
  expect(volumeDeltaKwh, 'Scenario requires non-zero volume delta').toBeGreaterThan(0);

  let podLineCountOnDrafts = 0;
  for (const invoiceId of poll.draftInvoiceIds) {
    const rows = await invoiceDetailedRowsForPod(Request, invoiceId, podIdentifier);
    podLineCountOnDrafts += rows.length;
  }

  return {
    volumeDeltaKwh,
    draftInvoiceCount: poll.draftInvoiceCount,
    podLineCountOnDrafts,
  };
}

/**
 * TO-BE (PDT-2915): POD with DELETED BBP + zero delta vs ACTIVE must not appear on correction credit/debit drafts.
 * Uses draft-invoices + invoice/detailed-data (Dev API surface).
 */
export async function assertPdt2915ZeroDeltaDeletedPodExcludedFromCorrectionDrafts(
  Request: baseFixture['Request'],
  poll: Pdt2915CorrectionDraftPoll,
  podIdentifier: string,
): Promise<void> {
  const bugMessage =
    'PDT-2915 AS-IS: zero-delta DELETED billing_by_profile must not generate correction credit/debit draft lines for the POD';

  expect(poll.draftInvoiceCount, `${bugMessage} — expected no draft invoices on correction run`).toBe(0);

  for (const invoiceId of poll.draftInvoiceIds) {
    const rows = await invoiceDetailedRowsForPod(Request, invoiceId, podIdentifier);
    expect(
      rows.length,
      `${bugMessage} — draft invoice ${invoiceId} must not contain lines for POD ${podIdentifier}`,
    ).toBe(0);
  }
}

export function buildPdt2915AttachmentSummary(args: Record<string, unknown>): Record<string, unknown> {
  return { jiraKey: 'PDT-2915', ...args };
}

/** GET `/billing-run/{id}` — Swagger `BillingRunResponse.invoiceCorrectionBillingRunParametersResponse`. */
export function resolvePdt2915InvoiceCorrectionParameters(
  billingRunSnapshot: Record<string, unknown>,
): Record<string, unknown> | undefined {
  const responseShape = billingRunSnapshot.invoiceCorrectionBillingRunParametersResponse;
  if (responseShape && typeof responseShape === 'object') {
    return responseShape as Record<string, unknown>;
  }
  const requestShape = billingRunSnapshot.invoiceCorrectionParameters;
  if (requestShape && typeof requestShape === 'object') {
    return requestShape as Record<string, unknown>;
  }
  return undefined;
}

/** Response may use `priceChange` (request) or `priceChanges` (Swagger InvoiceCorrectionBillingRunParametersResponse). */
export function isPdt2915VolumeOnlyCorrection(icp: Record<string, unknown> | undefined): boolean {
  if (!icp) return false;
  const volumeChange = icp.volumeChange === true;
  const priceChange = icp.priceChange === true || icp.priceChanges === true;
  return volumeChange && !priceChange;
}
