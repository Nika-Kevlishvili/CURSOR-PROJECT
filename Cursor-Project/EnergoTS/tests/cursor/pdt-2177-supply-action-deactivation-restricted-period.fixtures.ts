/**
 * PDT-2177 — Information-of-deactivation mass import must report billing/invoice restricted period
 * (not the misleading "Respective Contract version..." message) when contract lock date exists.
 *
 * Lock induction (DB-aligned with ProductContractRepository.getContractLockDate):
 * - Prefer STANDARD FOR_VOLUMES invoice (durable after COMPLETED). DB column: meter_reading_period_to.
 * - GET /invoice (InvoiceResponse) exposes that value as **meterReadingTo** (not meterReadingPeriodTo).
 *   meterReadingPeriodTo appears on listing/view DTOs only.
 * - Interim invoices typically leave meter reading "to" null → no lock → onComplete penalty path.
 * - Pattern: tests/billing/Restriction/restrictionForVolume(Percenage).spec.ts + BillingPayloads.waitForInvoiceGeneration
 *
 * Reference spec(s):
 * - tests/cursor/pdt-2854-pod-active-two-contracts.fixtures.ts (vertical supply MI)
 * - tests/billing/Restriction/restrictionForVolume(Percenage).spec.ts (FOR_VOLUMES + meter period)
 * - tests/cursor/PDT-2931-skip-risklist-product-contract-mass-import.fixtures.ts (process poll + error report)
 * - Phoenix ActionSupplyActivationService.deactivateWithActionNew (origin/dev2 fix)
 *
 * Target env: Dev2 — BASE_URL=https://devapps.energo-pro.bg/backend/phoenix2-dev
 */

import * as ExcelJS from 'exceljs';
import { expect, test } from '../../fixtures/baseFixture';
import type { baseFixture } from '../../fixtures/baseFixture';
import { randomGens } from '../../utils/randomGens';
import {
  downloadMassImportErrorReport,
  formatMassImportErrorReportForAttach,
  loadMassImportErrorReportSnapshot,
  pollProcessUntilCompleted,
  type MassImportErrorReportSnapshot,
  type MassImportReportRow,
} from './PDT-2931-skip-risklist-product-contract-mass-import.fixtures';

type FixtureRequest = baseFixture['Request'];
type FixtureFileUpload = baseFixture['FileUploadRequest'];
type Pdt2177Fx = Pick<baseFixture, 'Request' | 'GeneratePayload' | 'Responses' | 'Endpoints'>;

/** Swagger DomainType path segment for Information of deactivation. */
export const PDT_2177_DOMAIN_TYPE = 'SUPPLY_ACTION_DEACTIVATIONS';
export const PDT_2177_UPLOAD_BASE = `mass-import/${PDT_2177_DOMAIN_TYPE}/files/upload`;
export const PDT_2177_TEMPLATE_URL = `mass-import/${PDT_2177_DOMAIN_TYPE}/template/download`;

/** Fixed Dev2 message after e0fe000c4 — substring match is enough. */
export const RESTRICTED_PERIOD_SNIPPET = 'restricted period by invoice/billing';
export const RESTRICTED_PERIOD_FULL_SNIPPET =
  'deactivation unable for this pod due to restricted period by invoice/billing';

/** Pre-fix misleading message — must NOT appear after fix. */
export const OLD_MISLEADING_CONTRACT_VERSION_SNIPPET =
  'Respective Contract version for this pod is not found';

/** Seen when restriction did NOT fire and onComplete ran (previous Dev2 failure). */
export const PENALTY_ONCOMPLETE_SNIPPET = "Valid penalty can't be found";

const SUPPLY_ACTION_DEACTIVATION_MI_NOTIFICATION =
  /PROCESS_SUPPLY_ACTION_DEACTIVATION_MASS_IMPORT_(COMPLETED|ERROR)/;

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

export function isoToday(): string {
  return new Date().toISOString().split('T')[0];
}

export function addDaysIso(baseIso: string, days: number): string {
  const d = new Date(`${baseIso}T12:00:00.000Z`);
  d.setUTCDate(d.getUTCDate() + days);
  return d.toISOString().split('T')[0];
}

export function entityId(body: unknown): number {
  if (typeof body === 'number' && Number.isFinite(body)) {
    return body;
  }
  if (body && typeof body === 'object' && 'id' in body) {
    const id = Number((body as { id?: unknown }).id);
    if (Number.isFinite(id) && id > 0) {
      return id;
    }
  }
  throw new Error(`Unable to resolve entity id from ${JSON.stringify(body)?.slice(0, 200)}`);
}

function dateOnly(value: unknown): string | null {
  if (value == null || value === '') {
    return null;
  }
  const text = String(value).slice(0, 10);
  return /^\d{4}-\d{2}-\d{2}$/.test(text) ? text : null;
}

export async function resolvePodIdentifier(
  Request: FixtureRequest,
  Endpoints: baseFixture['Endpoints'],
  Responses: baseFixture['Responses'],
): Promise<string> {
  const podBody = Responses.pod[0] as { id?: number; identifier?: string } | undefined;
  const fromCreate = String(podBody?.identifier ?? '').trim();
  if (fromCreate) {
    return fromCreate;
  }
  const podId = entityId(podBody);
  const podGet = await Request.get(`${Endpoints.pod}/${podId}`);
  await expect(podGet).CheckResponse();
  const detail = (await podGet.json()) as { identifier?: string };
  const identifier = String(detail.identifier ?? '').trim();
  expect(identifier, 'POD identifier required for SUPPLY_ACTION_DEACTIVATIONS upload').toBeTruthy();
  return identifier;
}

export type ContractLockEvidence = {
  contractId: number;
  invoiceId: number;
  billingRunId: number;
  /**
   * DB-aligned lock date (ISO yyyy-MM-dd).
   * Source: InvoiceResponse.meterReadingTo (= DB meter_reading_period_to), with listing fallbacks.
   */
  lockDateIso: string;
  /** Which response field supplied lockDateIso. */
  lockDateField: string;
  activationDateIso: string;
  /** Deactivation date: activationDate <= date < lockDate (EDIT_LOCKED-safe). */
  deactivationDateIso: string;
  invoiceSnapshot: Record<string, unknown>;
};

const LOCK_DATE_CANDIDATE_KEYS = [
  // GET /invoice — InvoiceResponse (Swagger Dev2)
  'meterReadingTo',
  // Listing / billing-run invoice view DTOs
  'meterReadingPeriodTo',
  'meter_reading_period_to',
] as const;

function collectDateLikeFields(source: Record<string, unknown>): Record<string, string> {
  const out: Record<string, string> = {};
  for (const [key, value] of Object.entries(source)) {
    const d = dateOnly(value);
    if (d) {
      out[key] = d;
    }
  }
  return out;
}

/**
 * Resolve lock date from InvoiceResponse first (meterReadingTo), then listing-style aliases.
 * Optionally enrich from invoice/listing when GET /invoice has no meter reading "to".
 */
export async function resolveInvoiceLockDateIso(
  Request: FixtureRequest,
  invoiceId: number,
  invoice: Record<string, unknown>,
  billingRunId?: number,
): Promise<{ lockDateIso: string; lockDateField: string; sources: Record<string, unknown> }> {
  const sources: Record<string, unknown> = { invoiceGet: invoice };

  for (const key of LOCK_DATE_CANDIDATE_KEYS) {
    const d = dateOnly(invoice[key]);
    if (d) {
      return { lockDateIso: d, lockDateField: `invoice.${key}`, sources };
    }
  }

  // Fallback: invoice/listing often exposes meterReadingPeriodTo
  const invoiceNumber = String(invoice.invoiceNumber ?? '').trim();
  const listingPayload: Record<string, unknown> = {
    page: 0,
    size: 25,
  };
  if (invoiceNumber) {
    listingPayload.searchBy = 'INVOICE_NUMBER';
    listingPayload.prompt = invoiceNumber;
  } else {
    listingPayload.searchBy = 'ID';
    listingPayload.prompt = String(invoiceId);
  }
  const listingRes = await Request.post('invoice/listing', { data: listingPayload });
  if (listingRes.ok()) {
    const listingBody = (await listingRes.json()) as {
      content?: Array<Record<string, unknown>>;
    };
    const row =
      (listingBody.content ?? []).find((r) => Number(r.id) === invoiceId) ??
      (listingBody.content ?? [])[0];
    if (row) {
      sources.invoiceListing = row;
      for (const key of LOCK_DATE_CANDIDATE_KEYS) {
        const d = dateOnly(row[key]);
        if (d) {
          return { lockDateIso: d, lockDateField: `invoiceListing.${key}`, sources };
        }
      }
    }
  }

  if (billingRunId && billingRunId > 0) {
    const viewRes = await Request.get(
      `billing-run/draft-invoices?id=${billingRunId}&page=0&size=25`,
    );
    if (viewRes.ok()) {
      const viewBody = (await viewRes.json()) as { content?: Array<Record<string, unknown>> };
      const row =
        (viewBody.content ?? []).find((r) => Number(r.id) === invoiceId) ??
        (viewBody.content ?? [])[0];
      if (row) {
        sources.billingRunDraftInvoices = row;
        for (const key of LOCK_DATE_CANDIDATE_KEYS) {
          const d = dateOnly(row[key]);
          if (d) {
            return { lockDateIso: d, lockDateField: `draftInvoices.${key}`, sources };
          }
        }
      }
    }
  }

  const dateLike = {
    invoiceGet: collectDateLikeFields(invoice),
    invoiceListing: collectDateLikeFields(
      (sources.invoiceListing as Record<string, unknown> | undefined) ?? {},
    ),
    draftInvoices: collectDateLikeFields(
      (sources.billingRunDraftInvoices as Record<string, unknown> | undefined) ?? {},
    ),
  };
  throw new Error(
    `Could not resolve contract lock date (DB meter_reading_period_to) for invoice ${invoiceId}. ` +
      `Tried InvoiceResponse.meterReadingTo then listing meterReadingPeriodTo. ` +
      `Date-like fields seen: ${JSON.stringify(dateLike)}`,
  );
}

/**
 * Choose deactivation date that:
 * 1) is on/after POD activation (so deactivateWithActionNew date filter keeps the model), and
 * 2) is strictly before meter_reading_period_to (so isRestrictedForAutomationsPod returns true with EDIT_LOCKED).
 */
export function pickRestrictedDeactivationDate(
  activationDateIso: string,
  lockDateIso: string,
): string {
  expect(
    activationDateIso < lockDateIso,
    `activationDate (${activationDateIso}) must be before lockDate/meterReadingTo (${lockDateIso}) so a restricted deactivation date exists`,
  ).toBeTruthy();

  const dayBeforeLock = addDaysIso(lockDateIso, -1);
  if (dayBeforeLock >= activationDateIso) {
    return dayBeforeLock;
  }
  // Extremely short window — use activation date only if still before lock (equality already ruled out).
  expect(
    activationDateIso < lockDateIso,
    `no valid deactivation window between activation ${activationDateIso} and lock ${lockDateIso}`,
  ).toBeTruthy();
  return activationDateIso;
}

/**
 * Create contract + activated POD + STANDARD FOR_VOLUMES invoice.
 * Lock date from InvoiceResponse.meterReadingTo (= DB meter_reading_period_to used by getContractLockDate).
 */
export async function induceContractLockViaStandardVolumeInvoice(
  fx: Pdt2177Fx,
): Promise<ContractLockEvidence> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;

  const activationDateIso = randomGens.generateMonthStartDate('yyyy-mm-dd');

  await test.step('Precondition: customer', async () => {
    const customer = await Request.post(Endpoints.customer, {
      data: GeneratePayload.customers.customer_legal(),
    });
    await expect(customer).CheckResponse();
    Responses.customer.push(await customer.json());
  });

  await test.step('Precondition: price component (volume settlement)', async () => {
    const price = await Request.post(Endpoints.priceComponent, {
      data: GeneratePayload.productAndServices.priceSettlement(),
    });
    await expect(price).CheckResponse();
    Responses.priceComponent.push(await price.json());
  });

  await test.step('Precondition: term', async () => {
    const term = await Request.post(Endpoints.terms, {
      data: GeneratePayload.productAndServices.term(),
    });
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
    const product = await Request.post(Endpoints.product, {
      data: GeneratePayload.productAndServices.product(),
    });
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

  await test.step('Precondition: activate POD (month start)', async () => {
    const podActivation = await Request.post('/contract-pods/manual', {
      data: await GeneratePayload.pointsOfDelivery.pod_activation(0, activationDateIso),
    });
    await expect(podActivation).CheckResponse();
  });

  await test.step('Precondition: billing-by-profile (current month)', async () => {
    const payload = await GeneratePayload.energyData.profile1Month();
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

  let billingRunId = 0;
  await test.step('Precondition: STANDARD FOR_VOLUMES billing run', async () => {
    const billingPayload = await GeneratePayload.billing.billingRun('CONTRACT', ['FOR_VOLUMES']);
    const billingRun = await Request.post(Endpoints.billingRun, { data: billingPayload });
    await expect(billingRun).CheckResponse();
    const body = await billingRun.json();
    billingRunId = entityId(body);
    Responses.billingRun.push(body);
  });

  await test.step('Precondition: generate + complete billing (realize STANDARD invoice)', async () => {
    await GeneratePayload.billing.waitForInvoiceGeneration(true, true, 1, 0);
  });

  const invoiceId = entityId(Responses.invoice[Responses.invoice.length - 1]);
  expect(invoiceId, 'realized STANDARD invoice id').toBeGreaterThan(0);

  const invoiceRes = await Request.get(`invoice?id=${invoiceId}`);
  await expect(invoiceRes).CheckResponse();
  const invoice = (await invoiceRes.json()) as Record<string, unknown>;

  const resolved = await resolveInvoiceLockDateIso(Request, invoiceId, invoice, billingRunId);
  const deactivationDateIso = pickRestrictedDeactivationDate(
    activationDateIso,
    resolved.lockDateIso,
  );

  console.log(
    `[PDT-2177] lock from ${resolved.lockDateField}=${resolved.lockDateIso} ` +
      `invoiceType=${String(invoice.invoiceType)} invoiceDate=${String(invoice.invoiceDate)}`,
  );

  return {
    contractId: entityId(Responses.productContract[0]),
    invoiceId,
    billingRunId,
    lockDateIso: resolved.lockDateIso,
    lockDateField: resolved.lockDateField,
    activationDateIso,
    deactivationDateIso,
    invoiceSnapshot: invoice,
  };
}

/** @deprecated Use induceContractLockViaStandardVolumeInvoice — interim path does not set meter_reading_period_to. */
export async function induceContractLockViaInterimBilling(
  fx: Pdt2177Fx,
): Promise<ContractLockEvidence> {
  return induceContractLockViaStandardVolumeInvoice(fx);
}

export async function listSupplyActionDeactivationProcessIds(
  Request: FixtureRequest,
): Promise<number[]> {
  const res = await Request.get('process', {
    params: {
      page: 0,
      size: 50,
      sortBy: 'ID',
      sortDirection: 'DESC',
    },
  });
  if (!res.ok()) {
    return [];
  }
  const body = (await res.json()) as {
    content?: Array<{ id?: number; name?: string; processType?: string }>;
  };
  return (body.content ?? [])
    .filter((process) => {
      const name = String(process.name ?? '').toUpperCase();
      const type = String(process.processType ?? '').toUpperCase();
      return (
        name.includes('SUPPLY_ACTION_DEACTIVATION') ||
        type.includes('SUPPLY_ACTION_DEACTIVATION')
      );
    })
    .map((process) => Number(process.id))
    .filter((id) => Number.isFinite(id) && id > 0);
}

export async function resolveSupplyActionDeactivationProcessId(
  Request: FixtureRequest,
  processIdsBeforeUpload: ReadonlySet<number>,
  maxAttempts = 60,
  delayMs = 2000,
): Promise<number> {
  for (let attempt = 0; attempt < maxAttempts; attempt++) {
    const processIdsNow = await listSupplyActionDeactivationProcessIds(Request);
    const newProcessIds = processIdsNow.filter((id) => !processIdsBeforeUpload.has(id));
    if (newProcessIds.length > 0) {
      return Math.max(...newProcessIds);
    }

    const notification = await Request.get('notifications?size=30&page=0');
    if (notification.ok()) {
      const body = await notification.json();
      const notifications = Array.isArray(body?.content) ? body.content : [body];
      const notificationIds = notifications
        .filter(
          (n: { notificationType?: string; entityId?: number }) =>
            n?.notificationType &&
            SUPPLY_ACTION_DEACTIVATION_MI_NOTIFICATION.test(String(n.notificationType)),
        )
        .map((n: { entityId?: number }) => Number(n.entityId))
        .filter((id) => Number.isFinite(id) && id > 0 && !processIdsBeforeUpload.has(id));
      if (notificationIds.length > 0) {
        return Math.max(...notificationIds);
      }
    }

    if (attempt < maxAttempts - 1) {
      await sleep(delayMs);
    }
  }
  throw new Error('No new PROCESS_SUPPLY_ACTION_DEACTIVATION_MASS_IMPORT process found after upload');
}

/** Build vertical SUPPLY_ACTION_DEACTIVATIONS workbook (column A = POD identifier), matching PDT-2854. */
export async function buildSupplyActionDeactivationWorkbook(
  Request: FixtureRequest,
  podIdentifier: string,
): Promise<Buffer> {
  const templateRes = await Request.get(PDT_2177_TEMPLATE_URL);
  await expect(templateRes).CheckResponse();
  const templateBuffer = await templateRes.body();

  const workbook = new ExcelJS.Workbook();
  await workbook.xlsx.load(templateBuffer as unknown as ExcelJS.Buffer);
  const worksheet = workbook.worksheets[0];
  expect(worksheet, 'SUPPLY_ACTION_DEACTIVATIONS template worksheet').toBeTruthy();
  worksheet!.getCell('A2').value = podIdentifier;

  const out = await workbook.xlsx.writeBuffer();
  return Buffer.from(out);
}

export async function uploadSupplyActionDeactivationFile(
  FileUploadRequest: FixtureFileUpload,
  fileBuffer: Buffer,
  deactivationDateIso: string,
): Promise<{ status: number; bodyText: string }> {
  const uploadUrl = `${PDT_2177_UPLOAD_BASE}?date=${encodeURIComponent(deactivationDateIso)}`;
  const upload = await FileUploadRequest.post(uploadUrl, {
    multipart: {
      file: {
        name: `pdt-2177-supply-action-deactivation-${randomGens.generateCurrentTimeStamp()}.xlsx`,
        mimeType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        buffer: fileBuffer,
      },
    },
  });
  const bodyText = await upload.text();
  return { status: upload.status(), bodyText };
}

/**
 * Upload Information-of-deactivation MI → resolve process → poll COMPLETED → parse error report.
 * Restriction must fail in deactivateWithActionNew (row processing), not in onComplete.
 */
export async function runSupplyActionDeactivationMassImportFlow(
  Request: FixtureRequest,
  FileUploadRequest: FixtureFileUpload,
  _Responses: baseFixture['Responses'],
  podIdentifier: string,
  deactivationDateIso: string,
): Promise<{
  processId: number;
  processStatus: string;
  reportSnapshot: MassImportErrorReportSnapshot;
  uploadStatus: number;
}> {
  const processIdsBefore = new Set(await listSupplyActionDeactivationProcessIds(Request));

  const fileBuffer = await buildSupplyActionDeactivationWorkbook(Request, podIdentifier);
  const upload = await uploadSupplyActionDeactivationFile(
    FileUploadRequest,
    fileBuffer,
    deactivationDateIso,
  );
  expect(
    upload.status,
    `SUPPLY_ACTION_DEACTIVATIONS upload accepted (body=${upload.bodyText.slice(0, 300)})`,
  ).toBeGreaterThanOrEqual(200);
  expect(upload.status).toBeLessThan(300);

  const processId = await resolveSupplyActionDeactivationProcessId(Request, processIdsBefore);
  const process = await pollProcessUntilCompleted(Request, processId);
  const reportBuffer = await downloadMassImportErrorReport(Request, processId);
  const reportSnapshot = await loadMassImportErrorReportSnapshot(reportBuffer);

  await test.info().attach(`[PDT-2177] process ${processId} — MASS_IMPORT_ERROR_REPORT`, {
    body: formatMassImportErrorReportForAttach(reportSnapshot),
    contentType: 'text/plain; charset=utf-8',
  });

  return {
    processId,
    processStatus: process.status,
    reportSnapshot,
    uploadStatus: upload.status,
  };
}

export function combinedErrorText(rows: MassImportReportRow[]): string {
  return rows.map((r) => r.errors ?? '').join('\n');
}

/** Fixed Dev2 behavior: restricted-period message present; old misleading + onComplete penalty absent. */
export function assertRestrictedPeriodErrorInReport(snapshot: MassImportErrorReportSnapshot): {
  passed: boolean;
  expectedResult: string;
  actualResult: string;
} {
  expect(
    snapshot.failedRows.length,
    `expected at least one failed MI row; ${snapshot.summary}`,
  ).toBeGreaterThan(0);

  const combined = combinedErrorText(snapshot.failedRows);
  const hasRestricted =
    combined.toLowerCase().includes(RESTRICTED_PERIOD_SNIPPET.toLowerCase()) ||
    combined.toLowerCase().includes(RESTRICTED_PERIOD_FULL_SNIPPET.toLowerCase());
  const hasOldMisleading = combined
    .toLowerCase()
    .includes(OLD_MISLEADING_CONTRACT_VERSION_SNIPPET.toLowerCase());
  const hasPenaltyOnComplete = combined.toLowerCase().includes(PENALTY_ONCOMPLETE_SNIPPET.toLowerCase());

  expect(
    hasPenaltyOnComplete,
    `restriction did not fire — got onComplete penalty path (lock missing?). Errors: ${combined.slice(0, 500)}`,
  ).toBeFalsy();
  expect(
    hasRestricted,
    `error report must contain restricted-period message (got: ${combined.slice(0, 500)})`,
  ).toBeTruthy();
  expect(
    hasOldMisleading,
    `error report must NOT contain old misleading message (got: ${combined.slice(0, 500)})`,
  ).toBeFalsy();

  return {
    passed: hasRestricted && !hasOldMisleading && !hasPenaltyOnComplete,
    expectedResult:
      'MASS_IMPORT_ERROR_REPORT contains "restricted period by invoice/billing" and does not contain "Respective Contract version for this pod is not found" (nor onComplete penalty).',
    actualResult: snapshot.summary,
  };
}
