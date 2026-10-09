/**
 * PDT-3127 — Government compensation deferred to PDF — invoice correction flow.
 *
 * Correction-specific helpers: volume correction draft after realized standard run,
 * forward invoice classification, and correction billing run creation.
 *
 * Shared helpers imported from PDT-3087 fixtures:
 * - createGovernmentCompensation, getCompensation, getInvoice
 * - assertCompensationUnlinked, assertCompensationLinkedAtPdf
 * - assertInvoiceCompensationApplied, assertInvoiceNoCompensationApplied
 * - startGeneratingAndWaitGenerated, startAccountingAndWaitCompleted
 * - terminateBillingRunAndWaitCancelled, pollPdfDocumentsPresent
 * - buildDevBillingRunPreviewLink, buildDevCompensationPreviewLink, buildDevInvoicePreviewLink
 * - resolveEntityIdentifier, resolveCurrencyName
 * - buildGovernmentCompensationMassImportBuffer, uploadGovernmentCompensationMassImport
 * - pollGovernmentCompensationMassImportComplete, findCompensationByNumber
 * - isNullishEntityRef, getBillingRunStatus
 *
 * Reference specs:
 * - tests/cursor/pdt-3087-government-compensation-defer-to-pdf.fixtures.ts
 * - tests/cursor/pdt-2915-invoice-correction-deleted-bbp.fixtures.ts (correctionBilling, profile helpers)
 * - tests/cursor/dev-volume-billing-two-compensations.fixtures.ts (prechain)
 *
 * Swagger (dev): PATCH /billing-run/start-billing, PATCH /billing-run/start-generating,
 * PATCH /billing-run/start-accounting, PATCH /billing-run/terminate,
 * POST /government-compensations (CompensationRequest), POST /billing-run (InvoiceCorrectionBillingRunRequest),
 * GET /billing-run/draft-invoices, GET /invoice, GET /government-compensations/{id}.
 */

import { test, expect } from '../../fixtures/baseFixture';
import { envVariables } from '../../fixtures/envCashed';
import {
  type DevVolCompFx,
  type DevVolCompPrechainResult,
  runDevVolCompContractAndBbpPrechain,
  DEV_PORTAL_BASE,
  buildDevBillingRunPreviewLink,
  buildDevCompensationPreviewLink,
  buildDevInvoicePreviewLink,
} from './dev-volume-billing-two-compensations.fixtures';
import {
  getBillingRunStatus,
} from './rps-pod-invoice-due-date.fixtures';
import {
  applyPdt2915FixedProfileVolume,
  postPdt2915BillingByProfile,
} from './pdt-2915-invoice-correction-deleted-bbp.fixtures';

export {
  runDevVolCompContractAndBbpPrechain,
  DEV_PORTAL_BASE,
  buildDevBillingRunPreviewLink,
  buildDevCompensationPreviewLink,
  buildDevInvoicePreviewLink,
  getBillingRunStatus,
};

export {
  createGovernmentCompensation,
  getCompensation,
  getInvoice,
  getDraftInvoiceIds,
  assertCompensationUnlinked,
  assertCompensationLinkedAtPdf,
  assertInvoiceCompensationApplied,
  assertInvoiceNoCompensationApplied,
  startGeneratingAndWaitGenerated,
  startAccountingAndWaitCompleted,
  terminateBillingRunAndWaitCancelled,
  pollPdfDocumentsPresent,
  isNullishEntityRef,
  resolveEntityIdentifier,
  resolveCurrencyName,
  buildGovernmentCompensationMassImportBuffer,
  uploadGovernmentCompensationMassImport,
  pollGovernmentCompensationMassImportComplete,
  findCompensationByNumber,
  createForVolumesDraftBillingRun,
  type CompensationView,
  type InvoiceView,
  type Pdt3087Fx,
  type FixtureFileUpload,
  BILLING_RUN_ROOT,
  COMP_ROOT,
} from './pdt-3087-government-compensation-defer-to-pdf.fixtures';

import {
  BILLING_RUN_ROOT,
  getDraftInvoiceIds,
  getInvoice,
  type InvoiceView,
  type Pdt3087Fx,
} from './pdt-3087-government-compensation-defer-to-pdf.fixtures';

export const PDT_3127_KEY = 'PDT-3127';
const GEN_POLL_MS = 3_000;
const REALIZE_MAX_MS = 10 * 60 * 1000;
const DRAFT_POLL_MS = 15_000;

/** Higher kWh for the second BBP posted after realize — volume delta drives correction drafts. */
export const PDT_3127_CORRECTION_KWH = 1400;

export type Pdt3127Fx = DevVolCompFx;

const LONG_POLL_MAX_MS = 15 * 60 * 1000;
const LONG_POLL_INTERVAL_MS = 15_000;

/**
 * POST a FOR_VOLUMES billing run, PATCH start-billing, then poll until DRAFT with ≥1 draft
 * invoice — same pattern as `startCorrectionBillingAndWaitDraft` but creates the run first.
 *
 * Replaces `createForVolumesDraftBillingRun` for reuse steps that exceed the 5-minute
 * `waitForInvoiceGeneration` timeout (TC-BE-9/10 terminate→reuse flows).
 */
export async function createForVolumesDraftBillingRunLongPoll(
  ctx: Pdt3087Fx,
): Promise<{ billingRunId: number; draftInvoiceIds: number[]; billingRunStatus: string }> {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;

  const billingPayload = await GeneratePayload.billing.billingRun('CONTRACT', ['FOR_VOLUMES']);
  const billingRun = await Request.post(Endpoints.billingRun, { data: billingPayload });
  await expect(billingRun).CheckResponse();
  const billingRaw = await billingRun.json();
  const billingRunId = asEntityId(billingRaw);
  Responses.billingRun.push(
    typeof billingRaw === 'object' && billingRaw !== null ? billingRaw : { id: billingRunId },
  );

  const startRes = await Request.patch(
    `${BILLING_RUN_ROOT}/start-billing?billingRunId=${billingRunId}`,
  );
  await expect(startRes).CheckResponse();

  const maxAttempts = Math.floor(LONG_POLL_MAX_MS / LONG_POLL_INTERVAL_MS);
  let lastStatus = '';
  let lastDraftIds: number[] = [];

  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    lastStatus = await getBillingRunStatus(Request, billingRunId);
    lastDraftIds = await getDraftInvoiceIds(Request, billingRunId);

    if (lastStatus === 'DRAFT' && lastDraftIds.length >= 1) {
      for (const id of lastDraftIds) {
        if (!Responses.invoice.includes(id as never)) {
          Responses.invoice.push(id);
        }
      }
      return { billingRunId, draftInvoiceIds: lastDraftIds, billingRunStatus: lastStatus };
    }

    if (['CANCELLED', 'COMPLETED', 'ERROR'].includes(lastStatus)) {
      break;
    }

    await new Promise((r) => setTimeout(r, LONG_POLL_INTERVAL_MS));
  }

  throw new Error(
    `[PDT-3127] FOR_VOLUMES billing run ${billingRunId} did not reach DRAFT with drafts ` +
      `after ${maxAttempts} attempts (~${LONG_POLL_MAX_MS / 60000} min). ` +
      `Last status=${lastStatus}, draftCount=${lastDraftIds.length}`,
  );
}

function asEntityId(raw: unknown): number {
  if (typeof raw === 'number' && Number.isFinite(raw) && raw > 0) return raw;
  if (raw && typeof raw === 'object' && 'id' in raw) {
    const n = Number((raw as { id: unknown }).id);
    if (Number.isFinite(n) && n > 0) return n;
  }
  const n = Number(raw);
  if (Number.isFinite(n) && n > 0) return n;
  throw new Error(`Expected numeric entity id, got: ${JSON.stringify(raw)}`);
}

/**
 * Post a second billing-by-profile with higher volume (PDT_3127_CORRECTION_KWH) on pod index 0
 * after the original invoice is realized. Without this volume delta, Dev produces zero correction
 * draft invoices because there is no volume correction data for the INVOICE_CORRECTION run.
 *
 * Pattern follows PDT-3072 (scale volume correction after invoice) and PDT-2915 (profile helpers).
 */
export async function postHigherVolumeBbpAfterRealize(
  ctx: Pdt3087Fx,
): Promise<{ correctionBbpId: number }> {
  const { Request, GeneratePayload, Responses } = ctx;

  const originalBbp = Responses.dataByProfiles[0];
  if (!originalBbp?.periodFrom || !originalBbp?.periodTo || !originalBbp?.periodType) {
    throw new Error(
      '[PDT-3127] postHigherVolumeBbpAfterRealize: Responses.dataByProfiles[0] must have ' +
        'periodFrom, periodTo, periodType from the original BBP prechain. ' +
        `Got: ${JSON.stringify(originalBbp)}`,
    );
  }

  const payload = (await GeneratePayload.energyData.profile15minute(0)) as Record<string, unknown>;
  payload.timeZone = 'CET';
  payload.profileId = envVariables.profiles;
  payload.periodFrom = originalBbp.periodFrom;
  payload.periodTo = originalBbp.periodTo;
  payload.periodType = originalBbp.periodType;
  applyPdt2915FixedProfileVolume(payload, PDT_3127_CORRECTION_KWH);

  const bbp = await postPdt2915BillingByProfile(
    Request,
    payload,
    'PDT-3127 correction BBP (higher volume)',
  );

  await GeneratePayload.energyData.uploadDataByProfileFile(bbp.id, 'MIN');

  Responses.dataByProfiles.push({
    id: bbp.id,
    periodFrom: String(payload.periodFrom),
    periodTo: String(payload.periodTo),
    periodType: payload.periodType,
  });

  return { correctionBbpId: bbp.id };
}

/**
 * Run the full standard FOR_VOLUMES billing run through accounting so the invoice
 * is REAL / accounted. Returns the original invoice id and invoice number.
 */
export async function runStandardBillingToAccounted(
  ctx: Pdt3087Fx,
): Promise<{ billingRunId: number; invoiceId: number; invoiceNumber: string }> {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;

  const billingPayload = await GeneratePayload.billing.billingRun('CONTRACT', ['FOR_VOLUMES']);
  const billingRun = await Request.post(Endpoints.billingRun, { data: billingPayload });
  await expect(billingRun).CheckResponse();
  const billingRaw = await billingRun.json();
  const billingRunId = asEntityId(billingRaw);
  Responses.billingRun.push(
    typeof billingRaw === 'object' && billingRaw !== null ? billingRaw : { id: billingRunId },
  );

  const billingIndex = Responses.billingRun.length - 1;
  await GeneratePayload.billing.waitForInvoiceGeneration(true, true, 1, billingIndex);

  const invoiceId = Responses.invoice[Responses.invoice.length - 1] as number;
  expect(invoiceId, 'original realized invoice id').toBeTruthy();

  const invoiceView = await getInvoice(Request, invoiceId);
  const invoiceNumber = String(invoiceView.invoiceNumber ?? '');
  expect(invoiceNumber, 'original invoice number').toBeTruthy();

  return { billingRunId, invoiceId, invoiceNumber };
}

/**
 * Create scale volume correction data and a correction billing run (INVOICE_CORRECTION).
 * Uses GeneratePayload.billing.correctionBilling which reads Responses.invoice[invoiceIndex].
 */
export async function createCorrectionBillingRun(
  ctx: Pdt3087Fx,
  invoiceIndex: number,
): Promise<number> {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;
  const correctionPayload = await GeneratePayload.billing.correctionBilling(
    invoiceIndex,
    false,
    true,
  );
  correctionPayload.invoiceCorrectionParameters.priceChange = false;
  correctionPayload.invoiceCorrectionParameters.volumeChange = true;

  const correction = await Request.post(Endpoints.billingRun, { data: correctionPayload });
  await expect(correction).CheckResponse();
  const correctionId = Number(await correction.json());
  Responses.billingRun.push(correctionId);
  return correctionId;
}

/**
 * Start-billing on a correction run and poll until DRAFT **with** draft invoices present.
 *
 * Unlike the previous implementation that polled status only then asserted drafts once,
 * this combines both checks in the polling loop (PDT-3072 pattern). If status reaches DRAFT
 * but drafts stay empty, the error message indicates a likely missing volume correction step.
 */
export async function startCorrectionBillingAndWaitDraft(
  ctx: Pdt3087Fx,
  correctionBillingRunId: number,
): Promise<{ draftInvoiceIds: number[]; billingRunStatus: string }> {
  const { Request, Responses } = ctx;
  const startRes = await Request.patch(
    `${BILLING_RUN_ROOT}/start-billing?billingRunId=${correctionBillingRunId}`,
  );
  await expect(startRes).CheckResponse();

  const maxAttempts = Math.floor(REALIZE_MAX_MS / DRAFT_POLL_MS);
  let lastStatus = '';
  let lastDraftIds: number[] = [];

  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    lastStatus = await getBillingRunStatus(Request, correctionBillingRunId);
    lastDraftIds = await getDraftInvoiceIds(Request, correctionBillingRunId);

    if (lastStatus === 'DRAFT' && lastDraftIds.length > 0) {
      for (const id of lastDraftIds) {
        if (!Responses.invoice.includes(id as never)) {
          Responses.invoice.push(id);
        }
      }
      return { draftInvoiceIds: lastDraftIds, billingRunStatus: lastStatus };
    }

    if (['CANCELLED', 'COMPLETED', 'ERROR'].includes(lastStatus)) {
      break;
    }

    await new Promise((r) => setTimeout(r, DRAFT_POLL_MS));
  }

  const hint = lastStatus === 'DRAFT' && lastDraftIds.length === 0
    ? ' — status reached DRAFT but no draft invoices produced. ' +
      'Likely cause: missing volume correction data (second higher-volume BBP) before correction billing run.'
    : '';

  throw new Error(
    `[PDT-3127] Correction billing run ${correctionBillingRunId} did not reach DRAFT with drafts ` +
      `after ${maxAttempts} attempts. Last status=${lastStatus}, draftCount=${lastDraftIds.length}${hint}`,
  );
}

export type CorrectionPrechainResult = DevVolCompPrechainResult & {
  originalBillingRunId: number;
  originalInvoiceId: number;
  originalInvoiceNumber: string;
  correctionBbpId: number;
  correctionBillingRunId: number;
  correctionDraftInvoiceIds: number[];
};

/**
 * Full prechain: contract+BBP → standard billing (accounted) → correction billing run → draft.
 * Each test gets its own isolated data.
 */
export async function runCorrectionPrechain(
  ctx: Pdt3087Fx,
): Promise<CorrectionPrechainResult> {
  const pre = await test.step(
    'Precondition: FOR_VOLUMES contract + BBP chain',
    async () => runDevVolCompContractAndBbpPrechain(ctx),
  );

  const original = await test.step(
    'Precondition: standard FOR_VOLUMES billing run → accounted',
    async () => runStandardBillingToAccounted(ctx),
  );

  const { correctionBbpId } = await test.step(
    'Precondition: post higher-volume BBP after realize (volume delta for correction)',
    async () => postHigherVolumeBbpAfterRealize(ctx),
  );

  const invoiceIndex = ctx.Responses.invoice.indexOf(original.invoiceId);
  expect(invoiceIndex, 'invoice index in Responses').toBeGreaterThanOrEqual(0);

  const correctionBillingRunId = await test.step(
    'Precondition: create INVOICE_CORRECTION billing run',
    async () => createCorrectionBillingRun(ctx, invoiceIndex),
  );

  const draft = await test.step(
    'Precondition: start-billing on correction → DRAFT',
    async () => startCorrectionBillingAndWaitDraft(ctx, correctionBillingRunId),
  );

  return {
    ...pre,
    originalBillingRunId: original.billingRunId,
    originalInvoiceId: original.invoiceId,
    originalInvoiceNumber: original.invoiceNumber,
    correctionBbpId,
    correctionBillingRunId,
    correctionDraftInvoiceIds: draft.draftInvoiceIds,
  };
}

/**
 * Classify correction draft invoices into forward invoice vs energy credit/debit notes.
 * Forward invoice: the one with invoiceDocumentType not CREDIT_NOTE/DEBIT_NOTE,
 * or min-ID when multiple non-credit types exist.
 */
export async function classifyCorrectionDraftInvoices(
  Request: Pdt3087Fx['Request'],
  draftInvoiceIds: number[],
): Promise<{
  forwardInvoiceId: number;
  forwardInvoice: InvoiceView;
  energyCreditNoteIds: number[];
  energyDebitNoteIds: number[];
  allDocs: Array<{ id: number; docType: string; totalAmountIncludingVat: number | string }>;
}> {
  const allDocs: Array<{ id: number; docType: string; totalAmountIncludingVat: number | string }> = [];
  const creditNotes: number[] = [];
  const debitNotes: number[] = [];
  const forwardCandidates: number[] = [];

  for (const id of draftInvoiceIds) {
    const inv = await getInvoice(Request, id);
    const docType = String(inv.invoiceDocumentType ?? 'INVOICE');
    allDocs.push({ id, docType, totalAmountIncludingVat: inv.totalAmountIncludingVat ?? 0 });

    if (docType === 'CREDIT_NOTE') {
      creditNotes.push(id);
    } else if (docType === 'DEBIT_NOTE') {
      debitNotes.push(id);
    } else {
      forwardCandidates.push(id);
    }
  }

  const forwardInvoiceId = forwardCandidates.length > 0
    ? Math.min(...forwardCandidates)
    : Math.min(...draftInvoiceIds);

  const forwardInvoice = await getInvoice(Request, forwardInvoiceId);

  return {
    forwardInvoiceId,
    forwardInvoice,
    energyCreditNoteIds: creditNotes,
    energyDebitNoteIds: debitNotes,
    allDocs,
  };
}