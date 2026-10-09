/**
 * PDT-3177 — Payment packages from mass import must auto-lock after payments are created.
 *
 * Closest feasible API path for the bank / offline mass-import lock bug:
 * - OFFLINE collection channel
 * - `POST mass-import/PAYMENT/files/upload` with a minimal MT940-style bank file (`:20:` / `:25:` / `:61:` / `:86:`)
 * - Intentionally omit `:86:` sub-tags so processing adds a warning after payment create
 *   (`AbstractTxtMassImportProcessService` sets `processed_record_info.success=false` on warnings)
 * - Documented expected: package `lockStatus` becomes `LOCKED` once payments from the import exist
 *
 * Reference spec(s):
 * - tests/cursor/PDT-2931-skip-risklist-product-contract-mass-import.fixtures.ts (FileUploadRequest + process poll)
 * - tests/receivableManagement/Payment/offlinePaymentCreateAndOffsetting.spec.ts (channel + package pattern)
 * - tests/receivableManagement/paymentPackage.spec.ts (payment package create/get)
 *
 * Swagger (test): `/mass-import/{domainType}/files/upload` domainType=PAYMENT;
 * `/payment-package/{id}` lockStatus enum LOCKED|UNLOCKED; `/payment-package/list`.
 */

import { expect } from '../../fixtures/baseFixture';
import type { baseFixture } from '../../fixtures/baseFixture';
import { Endpoints } from '../../fixtures/constants/endpoints';
import { randomGens } from '../../utils/randomGens';

export type Pdt3177Fx = {
  Request: baseFixture['Request'];
  FileUploadRequest: baseFixture['FileUploadRequest'];
  GeneratePayload: baseFixture['GeneratePayload'];
  Responses: baseFixture['Responses'];
  Endpoints: typeof Endpoints;
};

export const PDT_3177_KEY = 'PDT-3177';
export const MASS_IMPORT_PAYMENT_UPLOAD = 'mass-import/PAYMENT/files/upload';
export const PAYMENT_MI_PROCESS_NAME = /PROCESS_PAYMENT_MASS_IMPORT/;
export const PAYMENT_MI_NOTIFICATION = /PROCESS_PAYMENT_MASS_IMPORT_(COMPLETED|ERROR)/;

const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

export function paymentDateYyyyMmDd(base: Date = new Date()): string {
  const yyyy = base.getFullYear();
  const mm = String(base.getMonth() + 1).padStart(2, '0');
  const dd = String(base.getDate()).padStart(2, '0');
  return `${yyyy}-${mm}-${dd}`;
}

/** `:20:` / `:61:` value-date fragment `yyMMdd` for today. */
export function paymentDateYyMmDd(base: Date = new Date()): string {
  const yy = String(base.getFullYear()).slice(-2);
  const mm = String(base.getMonth() + 1).padStart(2, '0');
  const dd = String(base.getDate()).padStart(2, '0');
  return `${yy}${mm}${dd}`;
}

/**
 * Minimal bank partner (MT940-like) file.
 * Empty `:86:` (no ?20–?32 sub-tags) → warning during process → same success=false path as mixed-doc warnings.
 */
export function buildMinimalBankPartnerMassImportFile(opts: {
  paymentDate: Date;
  accountIdentification: string;
  amount?: string;
}): string {
  const tag20 = paymentDateYyyyMmDd(opts.paymentDate).replace(/-/g, '');
  const yyMmDd = paymentDateYyMmDd(opts.paymentDate);
  const amount = opts.amount ?? '10,00';
  // :61: YYMMDD + MMDD + C + amount + N + type/ref (charAt(10) must be C)
  const field61 = `${yyMmDd}${yyMmDd.slice(2)}C${amount}NTRFNONREF`;
  return [
    `:20:${tag20}`,
    `:25:${opts.accountIdentification}`,
    `:61:${field61}`,
    ':86:',
    '',
  ].join('\n');
}

export async function createOfflineCollectionChannel(fx: Pdt3177Fx): Promise<number> {
  const payload = fx.GeneratePayload.receivablesManagement.collection_channel();
  payload.type = 'OFFLINE';
  payload.typeOfFile = 'PAYMENT_PARTNER';
  const res = await fx.Request.post(fx.Endpoints.collectionChannel, { data: payload });
  await expect(res).CheckResponse();
  const channelId = Number(await res.json());
  expect(channelId, 'collection channel id').toBeGreaterThan(0);
  fx.Responses.collectionChannel.push(channelId);
  return channelId;
}

export async function listPaymentMassImportProcessIds(Request: Pdt3177Fx['Request']): Promise<number[]> {
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
    .filter(
      (process) =>
        PAYMENT_MI_PROCESS_NAME.test(String(process.name ?? '')) ||
        String(process.processType ?? '') === 'PROCESS_PAYMENT_MASS_IMPORT',
    )
    .map((process) => Number(process.id))
    .filter((id) => Number.isFinite(id) && id > 0);
}

export async function resolvePaymentMassImportProcessId(
  Request: Pdt3177Fx['Request'],
  processIdsBeforeUpload: ReadonlySet<number>,
  maxAttempts = 60,
  delayMs = 2000,
): Promise<number> {
  for (let attempt = 0; attempt < maxAttempts; attempt++) {
    const processIdsNow = await listPaymentMassImportProcessIds(Request);
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
            n?.notificationType && PAYMENT_MI_NOTIFICATION.test(String(n.notificationType)),
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
  throw new Error('No new PROCESS_PAYMENT_MASS_IMPORT process found after upload');
}

export async function pollProcessUntilCompleted(
  Request: Pdt3177Fx['Request'],
  processId: number,
  timeoutMs = 10 * 60 * 1000,
  intervalMs = 3000,
): Promise<{ status: string; processType?: string; name?: string }> {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    const res = await Request.get(`process/${processId}`);
    await expect(res).CheckResponse();
    const body = (await res.json()) as { status?: string; processType?: string; name?: string };
    if (body.status === 'COMPLETED') {
      return { status: body.status, processType: body.processType, name: body.name };
    }
    if (body.status === 'CANCELED') {
      throw new Error(`Process ${processId} was CANCELED`);
    }
    await sleep(intervalMs);
  }
  throw new Error(`Process ${processId} did not reach COMPLETED within ${timeoutMs}ms`);
}

export async function uploadPaymentMassImportBankFile(
  FileUploadRequest: Pdt3177Fx['FileUploadRequest'],
  opts: { paymentDate: string; collectionChannelId: number; fileContent: string },
): Promise<{ status: number; bodyText: string }> {
  const url =
    `${MASS_IMPORT_PAYMENT_UPLOAD}` +
    `?date=${encodeURIComponent(opts.paymentDate)}` +
    `&collectionChannelId=${opts.collectionChannelId}`;
  const upload = await FileUploadRequest.post(url, {
    multipart: {
      file: {
        name: `pdt-3177-bank-${randomGens.generateCurrentTimeStamp()}.txt`,
        mimeType: 'text/plain',
        buffer: Buffer.from(opts.fileContent, 'utf-8'),
      },
    },
  });
  const bodyText = await upload.text();
  return { status: upload.status(), bodyText };
}

export async function findPaymentPackageForChannel(
  Request: Pdt3177Fx['Request'],
  collectionChannelId: number,
  paymentDate: string,
): Promise<{ id: number; lockStatus?: string }> {
  const listRes = await Request.post(`${Endpoints.paymentPackage}/list`, {
    data: {
      page: 0,
      size: 25,
      collectionChannelIds: [collectionChannelId],
      fromDate: paymentDate,
      toDate: paymentDate,
      sortingField: 'NUMBER',
      direction: 'DESC',
    },
  });
  await expect(listRes).CheckResponse();
  const body = (await listRes.json()) as {
    content?: Array<{ id?: number; lockStatus?: string }>;
  };
  const row = (body.content ?? []).find((p) => Number(p.id) > 0);
  expect(row?.id, `payment package for collection channel ${collectionChannelId}`).toBeTruthy();
  return { id: Number(row!.id), lockStatus: row!.lockStatus };
}

export async function getPaymentPackageDetail(
  Request: Pdt3177Fx['Request'],
  packageId: number,
): Promise<{ id: number; lockStatus: string }> {
  const res = await Request.get(`${Endpoints.paymentPackage}/${packageId}`);
  await expect(res).CheckResponse();
  const body = (await res.json()) as { id?: number; lockStatus?: string };
  expect(Number(body.id), 'payment package detail id').toBe(packageId);
  expect(body.lockStatus, 'payment package lockStatus present').toBeTruthy();
  return { id: Number(body.id), lockStatus: String(body.lockStatus) };
}

/** Confirm at least one payment exists for the package (bug scenario keeps payments + UNLOCKED). */
export async function countPaymentsForPackage(
  Request: Pdt3177Fx['Request'],
  packageId: number,
  paymentDate: string,
): Promise<number> {
  const res = await Request.post(`${Endpoints.payment}/list`, {
    data: {
      page: 0,
      size: 25,
      prompt: String(packageId),
      searchFields: 'PAYMENT_PACKAGE',
      paymentDateFrom: paymentDate,
      paymentDateTo: paymentDate,
      columns: 'PAYMENT_PACKAGE',
      direction: 'DESC',
    },
  });
  await expect(res).CheckResponse();
  const body = (await res.json()) as { content?: unknown[]; totalElements?: number };
  if (typeof body.totalElements === 'number') {
    return body.totalElements;
  }
  return Array.isArray(body.content) ? body.content.length : 0;
}

export async function runOfflineBankPaymentMassImport(
  fx: Pdt3177Fx,
  opts?: { paymentDate?: Date },
): Promise<{
  collectionChannelId: number;
  processId: number;
  paymentPackageId: number;
  paymentDate: string;
  paymentCount: number;
  lockStatus: string;
}> {
  const paymentDateObj = opts?.paymentDate ?? new Date();
  const paymentDate = paymentDateYyyyMmDd(paymentDateObj);

  const collectionChannelId = await createOfflineCollectionChannel(fx);
  const fileContent = buildMinimalBankPartnerMassImportFile({
    paymentDate: paymentDateObj,
    accountIdentification: '1',
  });

  const processIdsBefore = new Set(await listPaymentMassImportProcessIds(fx.Request));
  const upload = await uploadPaymentMassImportBankFile(fx.FileUploadRequest, {
    paymentDate,
    collectionChannelId,
    fileContent,
  });
  expect(upload.status, `payment mass import upload accepted (body=${upload.bodyText})`).toBe(202);

  const processId = await resolvePaymentMassImportProcessId(fx.Request, processIdsBefore);
  await pollProcessUntilCompleted(fx.Request, processId);

  const listed = await findPaymentPackageForChannel(fx.Request, collectionChannelId, paymentDate);
  const detail = await getPaymentPackageDetail(fx.Request, listed.id);
  fx.Responses.paymentPackage.push(detail.id);

  const paymentCount = await countPaymentsForPackage(fx.Request, detail.id, paymentDate);
  expect(paymentCount, 'mass import must create at least one payment before lock assertion').toBeGreaterThan(
    0,
  );

  return {
    collectionChannelId,
    processId,
    paymentPackageId: detail.id,
    paymentDate,
    paymentCount,
    lockStatus: detail.lockStatus,
  };
}
