/**
 * EXP-PARITY-03 — Interim issue-date rule (MATCH_THE_INVOICE_DATE) and exactly-once
 * interim deduction across multiple volume invoices.
 *
 * NAMING: `EXP-PARITY-03` is a PSEUDO-KEY placeholder — no Jira ticket yet. Rename
 * files, `EXP_PARITY_03_KEY`, and test titles once a real key exists.
 *
 * Parity intent matches EXP-PARITY-01/02: run unchanged on PreProd vs Experiment;
 * never branch on environment or URL. Snapshots use `./shared/exp-parity-snapshot.fixtures`.
 *
 * Swagger (`Cursor-Project/config/swagger/experiment/swagger-spec.json`, Rule 41):
 * - `CreateInterimAdvancePaymentRequest`: `valueType` EXACT_AMOUNT; `dateOfIssueType`
 *   MATCH_THE_INVOICE_DATE; `deductionFrom` FIRST_INVOICE_FOR_SAME_PERIOD.
 * - `StandardBillingParameters.applicationModelType`: FOR_VOLUMES,
 *   INTERIM_AND_ADVANCE_PAYMENT. `maxEndDate` (date) is optional; required for
 *   FOR_VOLUMES / combined runs, must be omitted on INTERIM-only create.
 * - `PATCH /billing-run/download-error-report/{id}?protocol=BILLING` → xlsx
 *   `invoice_number` / `error_message` (see exp-parity-02).
 * - `InvoiceDetailedDataResponse.deducted` (number) — REST observable deduction column.
 * - `InvoiceResponse.isDeducted` / `deductedForInvoiceId` are NOT in Swagger but exist
 *   on the Invoice entity (`Invoice.java`); read when present, do not assert field names
 *   from invented strings.
 *
 * DB / code evidence (Experiment, cited in spec comments):
 * - `billing_run.generate_run_interim`: `when 'MATCH_THE_INVOICE_DATE' then with_standard_invoice`
 * - `execute_interim_data_preparation_job_task`: `_with_other` when
 *   `cardinality(rec.application_model_type) > 1`
 * - `InvoiceRepository.getAllNotDeductedInterimSamePeriod` (862–904) and
 *   `getAllNotDeductedInterimLongPaymentTerm` (906–937): REAL, not deducted,
 *   `deductedForInvoiceId is null`, same slot, and
 *   `(select count(1) from DeductionInterimInvoice dii where dii.interimInvoiceId=i.id
 *   and dii.billingId=:billingRunId)<=0`. Java caller of these queries is not present
 *   in this sparse Phoenix checkout — parity spec asserts API outcomes only.
 *
 * Reference spec(s):
 * - tests/billing/Interim/interimCases.spec.ts (REG-718 / combined model ~line 287)
 * - tests/billing/forVolumes/deduction.spec.ts (REG-718 interim → volumes)
 * - tests/cursor/exp-parity-01-pulling-shared-pod-handover.fixtures.ts (billing poll)
 * - tests/cursor/exp-parity-02-price-component-date-coverage.fixtures.ts (error report)
 * - tests/cursor/pdt-3013-separate-pod-reversal-offset.fixtures.ts (2 PODs / separate invoice)
 * - tests/cursor/pdt-2891-connected-invoices.fixtures.ts (interim REAL + deduction)
 * - tests/cursor/PDT-2529-rfd-data-model-happy-path.fixtures.ts (separateInvoiceForEachPod)
 */

import { expect, test } from './cursor-test.fixtures';
import type { baseFixture } from './cursor-test.fixtures';
import { envVariables } from '../../fixtures/envCashed';
import {
  applyVolumeBillingAnchor,
  enableSeparateInvoiceForEachPod,
  profileDateRangeFromAnchor,
  termPayloadWithMinimalPaymentDays,
} from './PDT-2529-rfd-data-model-happy-path.fixtures';
import {
  resolvePdt2376BillingAnchor,
  type Pdt2376BillingAnchor,
} from './pdt-2376-volume-with-electricity.fixtures';
import {
  downloadBillingErrorRows,
  normalizedErrorShapes,
  type SlotErrorRow,
} from './exp-parity-02-price-component-date-coverage.fixtures';
import {
  applyExpParityCombinedProduct,
  applyExpParitySignedCombinedContract,
  postExpParitySignedProductContract,
} from './shared/exp-parity-contract.fixtures';
import { parityAmount } from './shared/exp-parity-snapshot.fixtures';
import {
  applyExpParityLocalAddress,
  postExpParityLegalCustomer,
  type ExpParityLocalAddressIds,
} from './shared/exp-parity-customer.fixtures';
import { randomGens } from '../../utils/randomGens';

/** Pseudo-key placeholder — replace once a real Jira key exists. */
export const EXP_PARITY_03_KEY = 'EXP-PARITY-03';

export const EXP_PARITY_03_CASE_13_ID = 'case-13-match-invoice-date-requires-standard-model';
export const EXP_PARITY_03_CASE_12_ID = 'case-12-interim-deducted-exactly-once';

export const EXP_PARITY_03_CASE_13_TITLE =
  'MATCH_THE_INVOICE_DATE interim is generated on a combined FOR_VOLUMES and INTERIM_AND_ADVANCE_PAYMENT billing run';
export const EXP_PARITY_03_CASE_12_TITLE =
  'A single interim invoice is deducted exactly once across multiple volume invoices in one run';

/** Pinned EXACT_AMOUNT (generator default is random). */
export const EXP_PARITY_03_INTERIM_EXCL_VAT = 100;
/** Volume price component expression — deterministic, unrelated to interim amount. Swagger `PriceComponentFormulaRequest.expression` is string. */
export const EXP_PARITY_03_VOLUME_PC_EXPRESSION = '50';

export const EXP_PARITY_03_INTERIM_INVOICE_TYPE = 'INTERIM_AND_ADVANCE_PAYMENT';
export const EXP_PARITY_03_VOLUME_INVOICE_TYPE = 'STANDARD';

export const EXP_PARITY_03_TEST_TIMEOUT_MS = 45 * 60 * 1000;
export const EXP_PARITY_03_DEDUCTION_TEST_TIMEOUT_MS = 55 * 60 * 1000;

const BILLING_RUN_ROOT = 'billing-run';
const POD_MANUAL_ACTIVATION_ROOT = 'contract-pods/manual';

const DRAFT_POLL_INTERVAL_MS = 20_000;
const DRAFT_MAX_WAIT_MS = 30 * 60 * 1000;
const BILLING_DATA_CRON_SETTLE_MS = 3 * 60 * 1000;

const BILLING_RUN_PENDING_STATUSES = new Set(['', 'INITIAL', 'IN_PROGRESS_DRAFT']);
const BILLING_RUN_TERMINAL_FAILURES = new Set(['DELETED', 'CANCELLED', 'IN_PROGRESS_TERMINATION']);

/** Two PODs for separate-invoice-per-POD volume split (Case 12). */
export const EXP_PARITY_03_POD_COUNT = 2;

export type ExpParity03Fx = Pick<
  baseFixture,
  'Request' | 'GeneratePayload' | 'Responses' | 'Endpoints'
>;

export type ExpParity03Request = ExpParity03Fx['Request'];

export type ExpParity03Prechain = {
  anchor: Pdt2376BillingAnchor;
  interimExclVat: number;
};

export type BillingRunObservation = {
  billingRunId: number;
  applicationModelTypes: string[];
  billingRunStatus: string;
  draftInvoiceIds: number[];
  interimDraftCount: number;
  volumeDraftCount: number;
  errorRows: SlotErrorRow[];
  errorShapes: string[];
};

export type InterimDeductionObservation = {
  interimInvoiceId: number;
  interimAmountInclVat: number | null;
  interimAmountExclVat: number | null;
  /** Runtime Invoice entity field — omitted from Swagger InvoiceResponse. */
  interimIsDeducted: boolean | null;
  interimDeductedForInvoiceId: number | null;
  volumeInvoiceIds: number[];
  perVolumeInvoiceDeduction: Array<{
    invoiceRole: string;
    invoiceType: string;
    invoiceDocumentType: string;
    totalDeductedFromDetailedData: number;
    hasDeductionLines: boolean;
  }>;
  totalDeductedAcrossVolumes: number;
  deductingVolumeInvoiceCount: number;
  connectedVolumeIdsFromInterim: number[];
};

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function asEntityId(raw: unknown): number {
  if (typeof raw === 'number' && Number.isFinite(raw)) {
    return raw;
  }
  if (raw && typeof raw === 'object' && 'id' in raw) {
    const value = Number((raw as { id?: unknown }).id);
    if (Number.isFinite(value)) {
      return value;
    }
  }
  const coerced = Number(raw);
  if (Number.isFinite(coerced)) {
    return coerced;
  }
  throw new Error(`Unable to read an entity id from ${JSON.stringify(raw)}`);
}

function nestedNumericId(raw: unknown): number | null {
  if (typeof raw === 'number' && Number.isFinite(raw) && raw > 0) {
    return raw;
  }
  if (raw && typeof raw === 'object' && 'id' in raw) {
    const value = Number((raw as { id?: unknown }).id);
    if (Number.isFinite(value) && value > 0) {
      return value;
    }
  }
  return null;
}

function pushPodResponse(Responses: ExpParity03Fx['Responses'], raw: unknown): number {
  const id = asEntityId(raw);
  Responses.pod.push(
    raw && typeof raw === 'object' ? { ...(raw as object), id } : { id },
  );
  return id;
}

async function resolvePodDetailId(
  Request: ExpParity03Fx['Request'],
  Responses: ExpParity03Fx['Responses'],
  podIndex: number,
): Promise<number> {
  const pod = Responses.pod[podIndex] as Record<string, unknown> | undefined;
  const fromCreate =
    nestedNumericId(pod?.podDetailId) ?? nestedNumericId(pod?.lastPodDetailId);
  if (fromCreate) {
    return fromCreate;
  }

  const podId = asEntityId(pod);
  const getRes = await Request.get(`pod/${podId}?version=1`);
  await expect(getRes).CheckResponse();
  const body = (await getRes.json()) as {
    podDetailId?: unknown;
    lastPodDetailId?: unknown;
    versions?: Array<{ podDetailId?: unknown }>;
  };
  const detailId =
    nestedNumericId(body.lastPodDetailId) ??
    nestedNumericId(body.podDetailId) ??
    nestedNumericId(body.versions?.[0]?.podDetailId);
  expect(detailId, `podDetailId for POD ${podId}`).toBeGreaterThan(0);
  return detailId as number;
}

/**
 * POD B is created after the contract exists. `pod_activation(1)` reads
 * `contractPodsResponses` — without a contract PUT that adds the new
 * `podDetailId` to billing group 0, `podDetailId` stays null and Experiment
 * returns "Point Of Delivery does not exists or is not ACTIVE anymore".
 * Same attach path as `pdt-3421` / `invoiceSlotSplitting.spec.ts`.
 */
async function attachPodToExistingContractBillingGroup(
  fx: ExpParity03Fx,
  podIndex: number,
): Promise<void> {
  const { Request, GeneratePayload, Responses } = fx;
  const podDetailId = await resolvePodDetailId(Request, Responses, podIndex);
  const contractId = asEntityId(Responses.productContract[0]);

  const contractPayload = await GeneratePayload.contractsAndOrders.product_contract(0, 0, 0);
  applyExpParitySignedCombinedContract(contractPayload);
  const editPayload = (await GeneratePayload.contractsAndOrders.edit_ProductContract(
    contractPayload as never,
  )) as Record<string, unknown> & {
    podRequests: unknown[];
    productContractPointOfDeliveries: unknown[];
    additionalParameters?: { estimatedTotalConsumptionUnderContractKwh?: number };
    basicParameters?: Record<string, unknown>;
  };

  const newPodInfo = await GeneratePayload.contractsAndOrders.addPodToBillingGroup(0, podDetailId);
  editPayload.podRequests.push(newPodInfo.addPod);
  editPayload.productContractPointOfDeliveries.push(
    newPodInfo.addPod.productContractPointOfDeliveries[0],
  );
  if (editPayload.additionalParameters) {
    editPayload.additionalParameters.estimatedTotalConsumptionUnderContractKwh =
      newPodInfo.estimatedConsumption;
  }

  const existingGet = await Request.get(`product-contract/${contractId}?version=1`);
  await expect(existingGet).CheckResponse();
  const existing = (await existingGet.json()) as {
    basicParameters?: { entryInForceDate?: unknown; signingDate?: unknown };
  };
  if (editPayload.basicParameters) {
    const status = String(
      editPayload.basicParameters.status ?? existing.basicParameters?.status ?? '',
    );
    const signingDate =
      existing.basicParameters?.signingDate ?? editPayload.basicParameters.signingDate;
    editPayload.basicParameters.signingDate = signingDate;
    // ProductContractDateService (Experiment):
    // SIGNED — entryInForceDate must be omitted or in the future (past → 400).
    // ACTIVE_IN_TERM — entryInForceDate is mandatory and must be today or past.
    const activeStatuses = new Set([
      'ENTERED_INTO_FORCE',
      'ACTIVE_IN_PERPETUITY',
      'ACTIVE_IN_TERM',
      'TERMINATED',
    ]);
    if (activeStatuses.has(status)) {
      editPayload.basicParameters.entryInForceDate =
        existing.basicParameters?.entryInForceDate ??
        signingDate ??
        new Date().toISOString().slice(0, 10);
    } else {
      editPayload.basicParameters.entryInForceDate = null;
    }
  }

  const editRes = await Request.put(
    `product-contract/${contractId}?versionId=1&changeFutureVersionsPods=false`,
    { data: editPayload },
  );
  await expect(editRes).CheckResponse();
}

function podSettlementPayloadUnique(
  GeneratePayload: ExpParity03Fx['GeneratePayload'],
  suffix: string,
  addressIds: ExpParityLocalAddressIds,
): Record<string, unknown> {
  const payload = GeneratePayload.pointsOfDelivery.pod_settlement() as Record<string, unknown>;
  const core = randomGens.generateUniqueIdentifier();
  payload.identifier = (`32X${core}${suffix}`).slice(0, 33);
  applyExpParityLocalAddress(payload, addressIds);
  return payload;
}

/**
 * IAP payload validated against `CreateInterimAdvancePaymentRequest` (Experiment Swagger).
 * `MATCH_THE_INVOICE_DATE` requires a standard invoice date — Experiment DB routes this
 * through `with_standard_invoice` in `billing_run.generate_run_interim`.
 */
export function buildMatchInvoiceDateExactAmountIap(
  GeneratePayload: ExpParity03Fx['GeneratePayload'],
  amountExclVat: number = EXP_PARITY_03_INTERIM_EXCL_VAT,
): Record<string, unknown> {
  const payload = GeneratePayload.productAndServices.interim() as Record<string, unknown>;
  payload.valueType = 'EXACT_AMOUNT';
  payload.value = amountExclVat;
  payload.currencyId = envVariables.currency;
  payload.paymentType = 'OBLIGATORY';
  payload.dateOfIssueType = 'MATCH_THE_INVOICE_DATE';
  payload.deductionFrom = 'FIRST_INVOICE_FOR_SAME_PERIOD';
  // Experiment validation (`CreateInterimAdvancePaymentRequest`): MATCH_THE_INVOICE_DATE
  // must set matchesWithTermOfStandardInvoice=true (reuse standard invoice term);
  // interimAdvancePaymentTerm stays null; dayOfWeekAndPeriodOfYearAndDateOfMonth is
  // PERIODICAL-only — stock interim() inherits it and triggers 400 if left in payload.
  payload.matchesWithTermOfStandardInvoice = true;
  payload.interimAdvancePaymentTerm = null;
  delete payload.dayOfWeekAndPeriodOfYearAndDateOfMonth;
  payload.dateOfIssueValue = null;
  payload.dateOfIssueValueFrom = null;
  payload.dateOfIssueValueTo = null;
  payload.valueFrom = null;
  payload.valueTo = null;
  // Swagger has no `missingInvoice`; stock generator sends it — keep for green parity with legacy specs.
  payload.missingInvoice = false;
  return payload;
}

/**
 * Standalone interim IAP (PERIODICAL) for REG-718-style deduction chains — does not require
 * a standard application model in the same billing run (unlike MATCH_THE_INVOICE_DATE).
 */
export function buildExactAmountSamePeriodIap(
  GeneratePayload: ExpParity03Fx['GeneratePayload'],
  amountExclVat: number = EXP_PARITY_03_INTERIM_EXCL_VAT,
): Record<string, unknown> {
  const payload = GeneratePayload.productAndServices.interim() as Record<string, unknown>;
  payload.valueType = 'EXACT_AMOUNT';
  payload.value = amountExclVat;
  payload.currencyId = envVariables.currency;
  payload.paymentType = 'OBLIGATORY';
  payload.dateOfIssueType = 'PERIODICAL';
  payload.deductionFrom = 'FIRST_INVOICE_FOR_SAME_PERIOD';
  payload.matchesWithTermOfStandardInvoice = false;
  return payload;
}

/** Customer → PC → term → POD → IAP → product → contract → activate → profile (1 month). */
export async function buildExpParity03SinglePodPrechain(fx: ExpParity03Fx): Promise<ExpParity03Prechain> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;

  const anchor = await resolvePdt2376BillingAnchor(Request);

  const customer = await test.step('Precondition: customer', async () =>
    postExpParityLegalCustomer(fx),
  );

  await test.step('Precondition: price component (FOR_VOLUMES)', async () => {
    const payload = GeneratePayload.productAndServices.priceSettlement();
    payload.formulaRequest.expression = EXP_PARITY_03_VOLUME_PC_EXPRESSION;
    const res = await Request.post(Endpoints.priceComponent, { data: payload });
    await expect(res).CheckResponse();
    Responses.priceComponent.push(await res.json());
  });

  await test.step('Precondition: term', async () => {
    const res = await Request.post(Endpoints.terms, {
      data: termPayloadWithMinimalPaymentDays(GeneratePayload),
    });
    await expect(res).CheckResponse();
    Responses.terms.push(await res.json());
  });

  await test.step('Precondition: POD', async () => {
    const podPayload = GeneratePayload.pointsOfDelivery.pod_settlement() as Record<string, unknown>;
    applyExpParityLocalAddress(podPayload, customer.addressIds);
    const res = await Request.post(Endpoints.pod, { data: podPayload });
    await expect(res).CheckResponse();
    Responses.pod.push(await res.json());
  });

  await test.step(
    `Precondition: IAP EXACT_AMOUNT ${EXP_PARITY_03_INTERIM_EXCL_VAT}, dateOfIssueType MATCH_THE_INVOICE_DATE`,
    async () => {
      const res = await Request.post(Endpoints.interim, {
        data: buildMatchInvoiceDateExactAmountIap(GeneratePayload),
      });
      await expect(res).CheckResponse();
      Responses.interim.push(await res.json());
    },
  );

  await test.step('Precondition: product + contract + POD activation', async () => {
    const product = await Request.post(Endpoints.product, {
      data: applyExpParityCombinedProduct(GeneratePayload.productAndServices.product()),
    });
    await expect(product).CheckResponse();
    Responses.product.push(await product.json());

    const contractPayload = await GeneratePayload.contractsAndOrders.product_contract();
    const contractBody = await postExpParitySignedProductContract(Request, contractPayload);
    Responses.productContract.push(contractBody);

    const podActivation = await Request.post(POD_MANUAL_ACTIVATION_ROOT, {
      data: await GeneratePayload.pointsOfDelivery.pod_activation(),
    });
    await expect(podActivation).CheckResponse();
  });

  await test.step('Precondition: billing-by-profile (ONE_MONTH)', async () => {
    const dateRanges = profileDateRangeFromAnchor(anchor);
    const payload = await GeneratePayload.energyData.profile1Month(0, dateRanges);
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
  });

  await test.step(
    `Precondition: settle ${Math.round(BILLING_DATA_CRON_SETTLE_MS / 1000)}s for volumes billing-data cron`,
    async () => {
      await sleep(BILLING_DATA_CRON_SETTLE_MS);
    },
  );

  return { anchor, interimExclVat: EXP_PARITY_03_INTERIM_EXCL_VAT };
}

function isTransientNetworkError(err: unknown): boolean {
  const msg = err instanceof Error ? `${err.message}\n${err.stack ?? ''}` : String(err);
  return /ETIMEDOUT|ECONNRESET|ECONNREFUSED|socket hang up|Timeout|timed out/i.test(msg);
}

async function withTransientNetworkRetry<T>(fn: () => Promise<T>, attempts = 5): Promise<T> {
  let last: unknown;
  for (let i = 1; i <= attempts; i++) {
    try {
      return await fn();
    } catch (err) {
      last = err;
      if (!isTransientNetworkError(err) || i === attempts) {
        throw err;
      }
      await sleep(800 * i + Math.floor(Math.random() * 1200));
    }
  }
  throw last;
}

export async function createStandardBillingRun(
  fx: ExpParity03Fx,
  applicationModelTypes: string[],
  anchor?: Pdt2376BillingAnchor,
): Promise<number> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const payload = await withTransientNetworkRetry(async () => {
    const next = await GeneratePayload.billing.billingRun('CONTRACT', applicationModelTypes);
    if (anchor) {
      applyVolumeBillingAnchor(next, anchor);
      // Swagger `StandardBillingParameters.maxEndDate` is optional. Experiment
      // rejects it on INTERIM-only create (`Max End Date must not be provided`).
      // Combined / volumes runs keep the volume-period bound.
      if (!applicationModelTypes.includes('FOR_VOLUMES')) {
        delete next.basicParameters.maxEndDate;
      }
    }
    return next;
  });
  const res = await withTransientNetworkRetry(async () => {
    const posted = await Request.post(Endpoints.billingRun, { data: payload });
    if (!posted.ok()) {
      await expect(posted).CheckResponse();
    }
    return posted;
  });
  await expect(res).CheckResponse();
  const billingRunId = asEntityId(await res.json());
  Responses.billingRun.push(billingRunId);
  return billingRunId;
}

async function readDraftInvoiceMeta(
  Request: ExpParity03Request,
  billingRunId: number,
): Promise<Array<{ id: number; invoiceType: string }>> {
  const rows: Array<{ id: number; invoiceType: string }> = [];
  const size = 25;
  for (let page = 0; page < 20; page++) {
    const res = await Request.get(
      `${BILLING_RUN_ROOT}/draft-invoices?id=${billingRunId}&page=${page}&size=${size}`,
    );
    await expect(res).CheckResponse();
    const body = (await res.json()) as {
      content?: Array<{ id?: number; invoiceType?: string }>;
      last?: boolean;
    };
    const chunk = body.content ?? [];
    for (const row of chunk) {
      const id = Number(row.id);
      if (Number.isFinite(id) && id > 0) {
        rows.push({ id, invoiceType: String(row.invoiceType ?? 'unknown') });
      }
    }
    if (body.last === true || chunk.length < size) {
      break;
    }
  }
  return rows;
}

/**
 * Start billing and poll until `commonParameters.status === 'DRAFT'` (terminal for
 * parity observation) or a hard failure status. Returns draft invoices + billing error report.
 */
export async function startBillingAndObserveDraft(
  Request: ExpParity03Request,
  billingRunId: number,
  applicationModelTypes: string[],
): Promise<BillingRunObservation> {
  let billingRunStatus = '';

  await test.step(`Action: start billing run ${billingRunId}`, async () => {
    const startRes = await Request.patch(
      `${BILLING_RUN_ROOT}/start-billing?billingRunId=${billingRunId}`,
    );
    await expect(startRes).CheckResponse();

    const deadline = Date.now() + DRAFT_MAX_WAIT_MS;
    while (Date.now() < deadline) {
      const statusRes = await Request.get(`${BILLING_RUN_ROOT}/${billingRunId}`);
      await expect(statusRes).CheckResponse();
      const body = (await statusRes.json()) as { commonParameters?: { status?: string } };
      billingRunStatus = String(body.commonParameters?.status ?? '');

      if (billingRunStatus === 'DRAFT') {
        return;
      }
      if (BILLING_RUN_TERMINAL_FAILURES.has(billingRunStatus)) {
        throw new Error(
          `Billing run ${billingRunId} reached terminal status ${billingRunStatus} before DRAFT.`,
        );
      }
      if (!BILLING_RUN_PENDING_STATUSES.has(billingRunStatus)) {
        console.log(
          `[${EXP_PARITY_03_KEY}] billing run ${billingRunId}: status ${billingRunStatus}, waiting for DRAFT`,
        );
      }
      await sleep(DRAFT_POLL_INTERVAL_MS);
    }

    throw new Error(
      `Billing run ${billingRunId} stayed in ${billingRunStatus || 'unknown'} — never reached DRAFT.`,
    );
  });

  const draftRows = await readDraftInvoiceMeta(Request, billingRunId);
  const draftInvoiceIds = draftRows.map((row) => row.id);
  const interimDraftCount = draftRows.filter(
    (row) => row.invoiceType === EXP_PARITY_03_INTERIM_INVOICE_TYPE,
  ).length;
  const volumeDraftCount = draftRows.filter(
    (row) => row.invoiceType === EXP_PARITY_03_VOLUME_INVOICE_TYPE,
  ).length;

  const errorRows = await test.step('Read: billing error report (protocol=BILLING)', async () =>
    downloadBillingErrorRows(Request, billingRunId));

  return {
    billingRunId,
    applicationModelTypes,
    billingRunStatus,
    draftInvoiceIds,
    interimDraftCount,
    volumeDraftCount,
    errorRows,
    errorShapes: normalizedErrorShapes(errorRows),
  };
}

export async function getInvoiceRecord(
  Request: ExpParity03Request,
  invoiceId: number,
): Promise<Record<string, unknown>> {
  const res = await Request.get(`invoice?id=${invoiceId}`);
  await expect(res).CheckResponse();
  return (await res.json()) as Record<string, unknown>;
}

/** Story V0.2: `connectedInvoices`; legacy fallback `debitCredits`. */
export function readConnectedInvoiceIds(body: Record<string, unknown>): number[] {
  const raw = (body.connectedInvoices ?? body.debitCredits) as
    | Array<{ id?: number }>
    | undefined;
  if (!Array.isArray(raw)) {
    return [];
  }
  return raw
    .map((row) => row?.id)
    .filter((id): id is number => typeof id === 'number' && Number.isFinite(id));
}

async function sumDetailedDataDeducted(
  Request: ExpParity03Request,
  invoiceId: number,
): Promise<{ totalDeducted: number; hasDeductionLines: boolean }> {
  let totalDeducted = 0;
  let hasDeductionLines = false;
  const size = 100;
  for (let page = 0; page < 10; page++) {
    const res = await Request.get(`invoice/detailed-data?id=${invoiceId}&page=${page}&size=${size}`);
    await expect(res).CheckResponse();
    const body = (await res.json()) as { content?: Array<Record<string, unknown>>; last?: boolean };
    const chunk = body.content ?? [];
    for (const row of chunk) {
      const deducted = parityAmount(row.deducted, 2);
      if (deducted != null && deducted !== 0) {
        hasDeductionLines = true;
        totalDeducted += Math.abs(deducted);
      }
      const value = parityAmount(row.value, 2);
      const pc = String(row.priceComponent ?? '').toUpperCase();
      if (value != null && value < 0 && pc.includes('INTERIM')) {
        hasDeductionLines = true;
        totalDeducted += Math.abs(value);
      }
    }
    if (body.last === true || chunk.length < size) {
      break;
    }
  }
  return { totalDeducted: Number(totalDeducted.toFixed(2)), hasDeductionLines };
}

/**
 * Case 12 chain: one POD → REAL interim → enable separate invoice per POD → second POD
 * + profiles → FOR_VOLUMES expecting two volume invoices in one run.
 */
export async function buildInterimRealThenTwoPodVolumesChain(
  fx: ExpParity03Fx,
): Promise<{
  anchor: Pdt2376BillingAnchor;
  interimBillingRunId: number;
  volumeBillingRunId: number;
  interimInvoiceId: number;
}> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const anchor = await resolvePdt2376BillingAnchor(Request);

  const customer = await test.step('Precondition: customer', async () =>
    postExpParityLegalCustomer(fx),
  );

  await test.step('Precondition: price component', async () => {
    const payload = GeneratePayload.productAndServices.priceSettlement();
    payload.formulaRequest.expression = EXP_PARITY_03_VOLUME_PC_EXPRESSION;
    const res = await Request.post(Endpoints.priceComponent, { data: payload });
    await expect(res).CheckResponse();
    Responses.priceComponent.push(await res.json());
  });

  await test.step('Precondition: term', async () => {
    const res = await Request.post(Endpoints.terms, {
      data: termPayloadWithMinimalPaymentDays(GeneratePayload),
    });
    await expect(res).CheckResponse();
    Responses.terms.push(await res.json());
  });

  await test.step('Precondition: first POD', async () => {
    const res = await Request.post(Endpoints.pod, {
      data: podSettlementPayloadUnique(GeneratePayload, 'A', customer.addressIds),
    });
    await expect(res).CheckResponse();
    pushPodResponse(Responses, await res.json());
  });

  await test.step(
    `Precondition: IAP EXACT_AMOUNT + PERIODICAL + FIRST_INVOICE_FOR_SAME_PERIOD`,
    async () => {
      const res = await Request.post(Endpoints.interim, {
        data: buildExactAmountSamePeriodIap(GeneratePayload),
      });
      await expect(res).CheckResponse();
      Responses.interim.push(await res.json());
    },
  );

  await test.step('Precondition: product + contract', async () => {
    const product = await Request.post(Endpoints.product, {
      data: applyExpParityCombinedProduct(GeneratePayload.productAndServices.product()),
    });
    await expect(product).CheckResponse();
    Responses.product.push(await product.json());

    const contractPayload = await GeneratePayload.contractsAndOrders.product_contract();
    const contractBody = await postExpParitySignedProductContract(Request, contractPayload);
    Responses.productContract.push(contractBody);
  });

  await test.step('Precondition: activate first POD', async () => {
    const res = await Request.post(POD_MANUAL_ACTIVATION_ROOT, {
      data: await GeneratePayload.pointsOfDelivery.pod_activation(0),
    });
    await expect(res).CheckResponse();
  });

  await test.step('Precondition: profile for POD 0 (interim month)', async () => {
    const dateRanges = profileDateRangeFromAnchor(anchor);
    const payload = await GeneratePayload.energyData.profile1Month(0, dateRanges);
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
  });

  let interimBillingRunId = 0;
  await test.step('Precondition: interim-only billing run', async () => {
    interimBillingRunId = await createStandardBillingRun(
      fx,
      ['INTERIM_AND_ADVANCE_PAYMENT'],
      anchor,
    );
    Responses.billingRun[Responses.billingRun.length - 1] = {
      id: interimBillingRunId,
      invoiceNumbers: 1,
    };
  });

  await test.step('Precondition: complete interim billing to REAL (REG-718 pattern)', async () => {
    await GeneratePayload.billing.waitForInvoiceGeneration(true, true, 1, Responses.billingRun.length - 1);
  });

  const interimInvoiceId = asEntityId(Responses.invoice[Responses.invoice.length - 1]);
  const interimBody = await getInvoiceRecord(Request, interimInvoiceId);
  expect(interimBody.invoiceStatus, 'interim invoice must be REAL before volumes run').toBe('REAL');
  expect(interimBody.invoiceType).toBe(EXP_PARITY_03_INTERIM_INVOICE_TYPE);

  await test.step(
    'Precondition: enable separateInvoiceForEachPod AFTER interim is REAL',
    async () => {
      const enabled = await enableSeparateInvoiceForEachPod(Request, GeneratePayload);
      const bgRes = await Request.get(`billing-group/${enabled.billingGroupId}`);
      await expect(bgRes).CheckResponse();
      const bgBody = (await bgRes.json()) as { separateInvoiceForEachPod?: boolean };
      expect(bgBody.separateInvoiceForEachPod).toBe(true);
    },
  );

  await test.step('Precondition: second POD + attach to contract + activation + profile', async () => {
    const podRes = await Request.post(Endpoints.pod, {
      data: podSettlementPayloadUnique(GeneratePayload, 'B', customer.addressIds),
    });
    await expect(podRes).CheckResponse();
    pushPodResponse(Responses, await podRes.json());
    await attachPodToExistingContractBillingGroup(fx, Responses.pod.length - 1);

    const activation = await Request.post(POD_MANUAL_ACTIVATION_ROOT, {
      data: await GeneratePayload.pointsOfDelivery.pod_activation(1),
    });
    await expect(activation).CheckResponse();

    const dateRanges = profileDateRangeFromAnchor(anchor);
    const payload = await GeneratePayload.energyData.profile1Month(1, dateRanges);
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
  });

  await test.step(
    `Precondition: settle ${Math.round(BILLING_DATA_CRON_SETTLE_MS / 1000)}s for volumes billing-data cron`,
    async () => {
      await sleep(BILLING_DATA_CRON_SETTLE_MS);
    },
  );

  let volumeBillingRunId = 0;
  await test.step('Action: FOR_VOLUMES billing run (two PODs, separate invoice each)', async () => {
    volumeBillingRunId = await createStandardBillingRun(fx, ['FOR_VOLUMES'], anchor);
    Responses.billingRun[Responses.billingRun.length - 1] = {
      id: volumeBillingRunId,
      invoiceNumbers: EXP_PARITY_03_POD_COUNT,
    };
  });

  await test.step('Action: complete volume billing to REAL', async () => {
    await GeneratePayload.billing.waitForInvoiceGeneration(
      true,
      true,
      EXP_PARITY_03_POD_COUNT,
      Responses.billingRun.length - 1,
    );
  });

  return { anchor, interimBillingRunId, volumeBillingRunId, interimInvoiceId };
}

export async function observeInterimDeductionAcrossVolumes(
  Request: ExpParity03Request,
  interimInvoiceId: number,
  volumeInvoiceIds: number[],
): Promise<InterimDeductionObservation> {
  const interimBody = await getInvoiceRecord(Request, interimInvoiceId);
  const interimAmountInclVat = parityAmount(interimBody.totalAmountIncludingVat, 2);
  const interimAmountExclVat = parityAmount(interimBody.totalAmountExcludingVat, 2);
  const interimIsDeducted =
    typeof interimBody.isDeducted === 'boolean' ? interimBody.isDeducted : null;
  const interimDeductedForInvoiceId =
    interimBody.deductedForInvoiceId != null ? Number(interimBody.deductedForInvoiceId) : null;

  const perVolumeInvoiceDeduction: InterimDeductionObservation['perVolumeInvoiceDeduction'] = [];
  let totalDeductedAcrossVolumes = 0;
  let deductingVolumeInvoiceCount = 0;

  for (let index = 0; index < volumeInvoiceIds.length; index++) {
    const invoiceId = volumeInvoiceIds[index];
    const body = await getInvoiceRecord(Request, invoiceId);
    const { totalDeducted, hasDeductionLines } = await sumDetailedDataDeducted(Request, invoiceId);
    if (totalDeducted > 0 || hasDeductionLines) {
      deductingVolumeInvoiceCount += 1;
    }
    totalDeductedAcrossVolumes += totalDeducted;
    perVolumeInvoiceDeduction.push({
      invoiceRole: `volumeInvoice${index + 1}`,
      invoiceType: String(body.invoiceType ?? 'unknown'),
      invoiceDocumentType: String(body.invoiceDocumentType ?? 'unknown'),
      totalDeductedFromDetailedData: totalDeducted,
      hasDeductionLines,
    });
  }

  return {
    interimInvoiceId,
    interimAmountInclVat,
    interimAmountExclVat,
    interimIsDeducted,
    interimDeductedForInvoiceId,
    volumeInvoiceIds,
    perVolumeInvoiceDeduction,
    totalDeductedAcrossVolumes: Number(totalDeductedAcrossVolumes.toFixed(2)),
    deductingVolumeInvoiceCount,
    connectedVolumeIdsFromInterim: readConnectedInvoiceIds(interimBody),
  };
}

export function resolveLastVolumeInvoiceIds(
  Responses: baseFixture['Responses'],
  expectedCount: number,
): number[] {
  const ids = Responses.invoice.slice(-expectedCount) as number[];
  expect(ids.length, `expected ${expectedCount} volume invoice id(s) in Responses.invoice`).toBe(
    expectedCount,
  );
  return ids;
}
