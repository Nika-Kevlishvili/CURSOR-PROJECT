import { expect } from '@playwright/test';
import {
  preparePdt2459LpfFromOverdueLiability,
  setupPdt2459ReceivablesBase,
  type Pdt2459Fx,
} from './pdt-2459-payment-reverse-lpf-offset.fixtures';

export const PDT_3219_KEY = 'PDT-3219';
export const PDT_3219_TITLE =
  'Templater: Reverse late payment fine document not generated only with specific customer';

export type Pdt3219Fx = Pdt2459Fx;

export type Pdt3219LpfDocumentSnapshot = {
  hasFileResponse: boolean;
  fileResponseCount: number;
  communicationCount: number;
  fileUrl: string | null;
  firstFileId: number | null;
  contractBillingGroupId: number | null;
};

export type Pdt3219ReversalResult = {
  originalLpfId: number;
  reversalLpfId: number;
  overdueLiabilityId: number;
  lpfAmount: number;
  customerIdentifier: string;
  originalDoc: Pdt3219LpfDocumentSnapshot;
  reversalDoc: Pdt3219LpfDocumentSnapshot;
};

/** Reuse PDT-2459 receivables base (customer + channel + package + daily interest). */
export async function setupPdt3219ReceivablesBase(fx: Pdt3219Fx): Promise<void> {
  await setupPdt2459ReceivablesBase(fx);
}

/**
 * Paid overdue manual liability (billingGroupId null by payload default) → due shift → LPF job.
 * This is the no-contract-billing-group path from PDT-3219.
 */
export async function preparePdt3219LpfWithoutBillingGroup(fx: Pdt3219Fx) {
  return preparePdt2459LpfFromOverdueLiability(fx);
}

export function readPdt3219DocumentSnapshot(body: Record<string, unknown>): Pdt3219LpfDocumentSnapshot {
  const fileResponse = (body.fileResponse ?? []) as Array<{ id?: number; name?: string }>;
  const communication = (body.communicationShortResponse ?? []) as Array<{ id?: number }>;
  const fileUrlRaw = body.fileUrl;
  const fileUrl =
    fileUrlRaw === null || fileUrlRaw === undefined || String(fileUrlRaw).trim() === ''
      ? null
      : String(fileUrlRaw);
  const billingGroupRaw = body.contractBillingGroupId;
  const contractBillingGroupId =
    billingGroupRaw === null || billingGroupRaw === undefined || billingGroupRaw === ''
      ? null
      : Number(billingGroupRaw);

  const firstFileId = fileResponse.length > 0 ? Number(fileResponse[0].id) : null;

  return {
    hasFileResponse: fileResponse.length > 0,
    fileResponseCount: fileResponse.length,
    communicationCount: communication.length,
    fileUrl,
    firstFileId: Number.isFinite(firstFileId) && (firstFileId as number) > 0 ? firstFileId : null,
    contractBillingGroupId:
      contractBillingGroupId !== null && Number.isFinite(contractBillingGroupId)
        ? contractBillingGroupId
        : null,
  };
}

export async function getPdt3219LpfDocumentSnapshot(
  fx: Pdt3219Fx,
  lpfId: number,
): Promise<{ body: Record<string, unknown>; snapshot: Pdt3219LpfDocumentSnapshot }> {
  const getRes = await fx.Request.get(`${fx.Endpoints.latePaymentFine}/${lpfId}`);
  await expect(getRes).CheckResponse();
  const body = (await getRes.json()) as Record<string, unknown>;
  return { body, snapshot: readPdt3219DocumentSnapshot(body) };
}

/**
 * POST /latePaymentFine/reverse/{id} — controller returns 206 PARTIAL_CONTENT with reversal id body.
 * Playwright response.ok() treats 206 as success, so CheckResponse is valid.
 */
export async function reversePdt3219LatePaymentFine(fx: Pdt3219Fx, lpfId: number): Promise<number> {
  const reverseRes = await fx.Request.post(`${fx.Endpoints.latePaymentFine}/reverse/${lpfId}`);
  await expect(reverseRes).CheckResponse();
  const reversalId = Number(await reverseRes.json());
  expect(reversalId, 'POST /latePaymentFine/reverse/{id} must return reversal LPF id').toBeGreaterThan(0);
  return reversalId;
}

export async function assertPdt3219ReversalTypeAndLink(
  fx: Pdt3219Fx,
  originalLpfId: number,
  reversalLpfId: number,
  lpfAmount: number,
): Promise<Record<string, unknown>> {
  const originalGet = await fx.Request.get(`${fx.Endpoints.latePaymentFine}/${originalLpfId}`);
  await expect(originalGet).CheckResponse();
  const originalBody = await originalGet.json();
  expect(Boolean(originalBody.reversed), 'Original LPF must be marked reversed').toBe(true);
  expect(
    Number(originalBody.latePaymentFineReversalShortResponse?.id),
    'Original LPF must link to reversal via latePaymentFineReversalShortResponse',
  ).toEqual(reversalLpfId);

  const reversalGet = await fx.Request.get(`${fx.Endpoints.latePaymentFine}/${reversalLpfId}`);
  await expect(reversalGet).CheckResponse();
  const reversalBody = (await reversalGet.json()) as Record<string, unknown>;
  expect(String(reversalBody.type), 'Reversal LPF type').toEqual('REVERSAL_OF_LATE_PAYMENT_FINE');
  expect(Number(reversalBody.amount), 'Reversal amount must negate original').toBeCloseTo(-lpfAmount, 2);
  expect(
    Number((reversalBody.parentLatePaymentFineShortResponse as { id?: number } | undefined)?.id),
    'Reversal must reference original LPF',
  ).toEqual(originalLpfId);

  fx.Responses.latePaymentFine.push(reversalBody);
  return reversalBody;
}

/**
 * Product expectation (Confluence Generate Late Payment File Templates):
 * reverse must create LPF document + email attachment visible on GET /latePaymentFine/{id}
 * via fileResponse / communicationShortResponse.
 */
export function assertPdt3219ReversalDocumentGenerated(
  snapshot: Pdt3219LpfDocumentSnapshot,
  reversalLpfId: number,
): void {
  expect(
    snapshot.contractBillingGroupId,
    `Reversal LPF ${reversalLpfId} must remain on no-billing-group path (contractBillingGroupId null)`,
  ).toBeNull();

  const hasDocumentEvidence =
    snapshot.hasFileResponse ||
    snapshot.communicationCount > 0 ||
    (snapshot.fileUrl !== null && snapshot.fileUrl.length > 0);

  expect(
    hasDocumentEvidence,
    `Reversal LPF ${reversalLpfId} must have generated document/email (fileResponse, communicationShortResponse, or fileUrl). ` +
      `Got fileResponseCount=${snapshot.fileResponseCount}, communicationCount=${snapshot.communicationCount}, fileUrl=${snapshot.fileUrl}. ` +
      `PDT-3219 bug: reverse succeeds but document/email list is empty when contractBillingGroupId is null.`,
  ).toBe(true);

  expect(
    snapshot.fileResponseCount,
    `Reversal LPF ${reversalLpfId} fileResponse must contain at least one PDF/email attachment`,
  ).toBeGreaterThan(0);
  expect(
    snapshot.communicationCount,
    `Reversal LPF ${reversalLpfId} communicationShortResponse must contain at least one email communication`,
  ).toBeGreaterThan(0);
}

/** Optional: download attachment by TemplateFileResponse id (Swagger GET /latePaymentFine/download-file/{id}). */
export async function assertPdt3219DownloadReversalAttachment(
  fx: Pdt3219Fx,
  fileId: number,
): Promise<void> {
  const downloadRes = await fx.Request.get(`${fx.Endpoints.latePaymentFine}/download-file/${fileId}`);
  await expect(downloadRes).CheckResponse();
  const body = await downloadRes.body();
  expect(body.byteLength, `download-file/${fileId} must return non-empty binary`).toBeGreaterThan(0);
}

export function buildPdt3219AttachmentSummary(result: Pdt3219ReversalResult): Record<string, unknown> {
  return {
    jiraKey: PDT_3219_KEY,
    customerIdentifier: result.customerIdentifier,
    overdueLiabilityId: result.overdueLiabilityId,
    originalLpfId: result.originalLpfId,
    reversalLpfId: result.reversalLpfId,
    lpfAmount: result.lpfAmount,
    originalDoc: result.originalDoc,
    reversalDoc: result.reversalDoc,
  };
}
