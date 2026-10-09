import { expect } from '@playwright/test';
import type { APIRequestContext } from '@playwright/test';
import { randomGens } from '../../utils/randomGens';
import { Endpoints } from '../../fixtures/constants/endpoints';

export type Pdt2960Fx = {
  Request: APIRequestContext;
  GeneratePayload: any;
  Responses: any;
  Endpoints: typeof Endpoints;
  receivableValidations?: { paymentValidation: (paymentId: number) => Promise<void> };
};

export type Pdt2960ProformaChainResult = {
  goodsOrderId: number;
  proformaInvoiceId: number;
  customerLiabilityId: number;
  liabilityAmount: number;
  dueDateBefore: string;
  customerIdentifier: string;
};

export function resolveGoodsOrderId(goodsOrderEntry: unknown): number {
  if (typeof goodsOrderEntry === 'number') return goodsOrderEntry;
  const id = (goodsOrderEntry as { id?: number })?.id;
  expect(id, 'goods order id must be present on Responses.goodsOrder entry').toBeTruthy();
  return Number(id);
}

export function normalizeDateOnly(value: unknown): string {
  const raw = String(value ?? '').trim();
  if (!raw) return '';
  return raw.split('T')[0];
}

const LONG_RUNNING_PATCH_TIMEOUT_MS = 5 * 60 * 1000;
const LONG_RUNNING_PATCH_MAX_ATTEMPTS = 3;
const LONG_RUNNING_PATCH_BACKOFF_MS = [2000, 4000];

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function isRetryablePatchError(error: unknown): boolean {
  const message = String((error as Error)?.message ?? error ?? '').toLowerCase();
  return (
    message.includes('socket hang up') ||
    message.includes('econnreset') ||
    message.includes('timeout') ||
    message.includes('etimedout')
  );
}

/** PATCH goods-order long-running steps (start-generating / start-accounting) with extended timeout + retry. */
export async function patchGoodsOrderLongRunning(fx: Pdt2960Fx, path: string): Promise<void> {
  let lastError: unknown;

  for (let attempt = 1; attempt <= LONG_RUNNING_PATCH_MAX_ATTEMPTS; attempt++) {
    try {
      const response = await fx.Request.patch(path, {
        timeout: LONG_RUNNING_PATCH_TIMEOUT_MS,
      });
      await expect(response).CheckResponse();
      return;
    } catch (error) {
      lastError = error;
      if (attempt < LONG_RUNNING_PATCH_MAX_ATTEMPTS && isRetryablePatchError(error)) {
        await sleep(LONG_RUNNING_PATCH_BACKOFF_MS[attempt - 1] ?? 4000);
        continue;
      }
      throw error;
    }
  }

  throw lastError;
}

/** Customer, collection channel, payment package (receivables base). */
export async function setupPdt2960ReceivablesBase(fx: Pdt2960Fx): Promise<void> {
  const customerRes = await fx.Request.post(fx.Endpoints.customer, {
    data: fx.GeneratePayload.customers.customer_legal(),
  });
  await expect(customerRes).CheckResponse();
  fx.Responses.customer.push(await customerRes.json());

  const channelRes = await fx.Request.post(fx.Endpoints.collectionChannel, {
    data: fx.GeneratePayload.receivablesManagement.collection_channel(),
  });
  await expect(channelRes).CheckResponse();
  fx.Responses.collectionChannel.push(await channelRes.json());

  const packageRes = await fx.Request.post(fx.Endpoints.paymentPackage, {
    data: fx.GeneratePayload.receivablesManagement.payment_package(),
  });
  await expect(packageRes).CheckResponse();
  fx.Responses.paymentPackage.push(await packageRes.json());
}

export async function createPdt2960Goods(fx: Pdt2960Fx): Promise<void> {
  const goodsRes = await fx.Request.post(fx.Endpoints.goods, {
    data: fx.GeneratePayload.productAndServices.goods(),
  });
  await expect(goodsRes).CheckResponse();
  fx.Responses.goods.push(await goodsRes.json());
}

export async function createPdt2960GoodsOrder(fx: Pdt2960Fx): Promise<number> {
  const orderRes = await fx.Request.post(fx.Endpoints.goodsOrder, {
    data: await fx.GeneratePayload.contractsAndOrders.goodsOrder(),
  });
  await expect(orderRes).CheckResponse();
  const body = await orderRes.json();
  fx.Responses.goodsOrder.push(body);
  return resolveGoodsOrderId(body);
}

/**
 * Proforma path: issue-proforma-invoice → start-generating → start-accounting.
 * Does NOT call issue-invoice — keeps PROFORMA_INVOICE for paid-proforma batch.
 */
export async function runPdt2960ProformaAccountingChain(
  fx: Pdt2960Fx,
  goodsOrderId: number,
): Promise<number> {
  const proformaRes = await fx.Request.post(
    `${fx.Endpoints.goodsOrder}/${goodsOrderId}/issue-proforma-invoice`,
  );
  await expect(proformaRes).CheckResponse();

  await patchGoodsOrderLongRunning(
    fx,
    `${fx.Endpoints.goodsOrder}/${goodsOrderId}/start-generating`,
  );

  await patchGoodsOrderLongRunning(
    fx,
    `${fx.Endpoints.goodsOrder}/${goodsOrderId}/start-accounting`,
  );

  const getOrderRes = await fx.Request.get(`${fx.Endpoints.goodsOrder}/${goodsOrderId}?version=1`);
  await expect(getOrderRes).CheckResponse();
  const orderBody = await getOrderRes.json();
  const proformaInvoiceId = Number(orderBody?.basicParametersResponse?.invoice?.id);
  expect(proformaInvoiceId, 'proforma invoice id on goods order v1').toBeGreaterThan(0);
  fx.Responses.invoice.push(proformaInvoiceId);
  return proformaInvoiceId;
}

async function findPdt2960LiabilityForInvoice(
  fx: Pdt2960Fx,
  customerIdentifier: string,
  proformaInvoiceId: number,
): Promise<
  { liabilityId: number; dueDate: string; currentAmount: number; initialAmount: number } | null
> {
  const listRes = await fx.Request.get(
    `${fx.Endpoints.customerLiability}/list?page=0&size=50&columns=ID&direction=DESC&prompt=${customerIdentifier}&searchFields=CUSTOMER`,
  );
  await expect(listRes).CheckResponse();
  const listBody = await listRes.json();
  const rows = (listBody.content ?? []) as Array<{ id?: number }>;

  for (const row of rows) {
    const liabilityId = Number(row.id);
    if (!Number.isFinite(liabilityId)) continue;

    const detailRes = await fx.Request.get(`${fx.Endpoints.customerLiability}/${liabilityId}`);
    await expect(detailRes).CheckResponse();
    const detail = await detailRes.json();
    const linkedInvoiceId = Number(
      detail.invoiceId ?? detail.invoiceResponse?.id ?? detail.invoiceShortResponse?.id,
    );

    if (linkedInvoiceId === proformaInvoiceId) {
      return {
        liabilityId,
        dueDate: normalizeDateOnly(detail.dueDate),
        currentAmount: Number(detail.currentAmount ?? 0),
        initialAmount: Number(detail.initialAmount ?? 0),
      };
    }
  }

  return null;
}

/** Resolve liability linked to proforma invoice via list + detail GET. */
export async function resolvePdt2960LiabilityForInvoice(
  fx: Pdt2960Fx,
  customerIdentifier: string,
  proformaInvoiceId: number,
): Promise<{ liabilityId: number; dueDate: string; currentAmount: number; initialAmount: number }> {
  const resolved = await findPdt2960LiabilityForInvoice(fx, customerIdentifier, proformaInvoiceId);
  expect(resolved, `liability for proforma invoice ${proformaInvoiceId}`).toBeTruthy();
  fx.Responses.customerLiability.push(resolved!.liabilityId);
  return resolved!;
}

/** Poll until proforma liability appears after accounting (async liability creation). */
export async function pollPdt2960LiabilityForInvoice(
  fx: Pdt2960Fx,
  customerIdentifier: string,
  proformaInvoiceId: number,
): Promise<{ liabilityId: number; dueDate: string; currentAmount: number; initialAmount: number }> {
  let resolved: {
    liabilityId: number;
    dueDate: string;
    currentAmount: number;
    initialAmount: number;
  } | null = null;

  await expect
    .poll(
      async () => {
        resolved = await findPdt2960LiabilityForInvoice(fx, customerIdentifier, proformaInvoiceId);
        return resolved?.liabilityId ?? 0;
      },
      {
        message: `PDT-2960 liability for proforma invoice ${proformaInvoiceId}`,
        timeout: 90_000,
        intervals: [2000, 5000],
      },
    )
    .toBeGreaterThan(0);

  fx.Responses.customerLiability.push(resolved!.liabilityId);
  return resolved!;
}

export async function getPdt2960LiabilityDueDate(
  fx: Pdt2960Fx,
  liabilityId: number,
): Promise<string> {
  const getRes = await fx.Request.get(`${fx.Endpoints.customerLiability}/${liabilityId}`);
  await expect(getRes).CheckResponse();
  const body = await getRes.json();
  return normalizeDateOnly(body.dueDate);
}

export async function payPdt2960LiabilityFullAmount(
  fx: Pdt2960Fx,
  amount: number,
  proformaInvoiceId: number,
): Promise<{ paymentId: number; paymentDate: string }> {
  const paymentPayload = await fx.GeneratePayload.receivablesManagement.payment();
  paymentPayload.initialAmount = amount;
  paymentPayload.paymentDate = randomGens.generateTodaysDate('yyyy-mm-dd');
  paymentPayload.invoiceId = proformaInvoiceId;
  paymentPayload.outgoingDocumentType = 'INVOICE';

  const paymentRes = await fx.Request.post(fx.Endpoints.payment, { data: paymentPayload });
  await expect(paymentRes).CheckResponse();
  const paymentId = (await paymentRes.json()) as number;
  fx.Responses.payment.push(paymentId);

  if (fx.receivableValidations) {
    await fx.receivableValidations.paymentValidation(paymentId);
  }

  return { paymentId, paymentDate: normalizeDateOnly(paymentPayload.paymentDate) };
}

export async function pollPdt2960LiabilityFullyPaid(
  fx: Pdt2960Fx,
  liabilityId: number,
): Promise<void> {
  await expect
    .poll(
      async () => {
        const getRes = await fx.Request.get(`${fx.Endpoints.customerLiability}/${liabilityId}`);
        await expect(getRes).CheckResponse();
        const body = await getRes.json();
        return Number(body.currentAmount ?? -1);
      },
      {
        message: 'PDT-2960 proforma liability must be fully offset after payment',
        timeout: 90_000,
        intervals: [2000, 5000],
      },
    )
    .toBe(0);
}

export async function triggerPdt2960PaidProformaBatch(fx: Pdt2960Fx): Promise<string[]> {
  const batchRes = await fx.Request.get(`${fx.Endpoints.goodsOrder}/test-paid-order-invoices`);
  await expect(batchRes).CheckResponse();
  const body = await batchRes.json();
  return Array.isArray(body) ? body.map(String) : [];
}

/**
 * AS-IS defect reproduction: batch syncs liability due_date from invoice payment_deadline (= current_date).
 * PASS while bug is open. When fixed, flip to expect(dueDateAfter).toBe(dueDateBefore).
 */
export function assertPdt2960AsIsDueDateChangedAfterPaidProformaBatch(
  dueDateBefore: string,
  dueDateAfter: string,
  paymentDate: string,
): void {
  const today = normalizeDateOnly(randomGens.generateTodaysDate('yyyy-mm-dd'));

  expect(dueDateAfter, 'post-batch due date must be present').toBeTruthy();
  expect(dueDateAfter, 'AS-IS: liability due date must change after paid-proforma batch').not.toBe(
    dueDateBefore,
  );
  expect(
    dueDateAfter,
    'AS-IS: liability due date should equal payment/processing date (current_date)',
  ).toBe(today);
  expect(dueDateAfter, 'AS-IS: liability due date should match payment date').toBe(paymentDate);
}

export function buildPdt2960AttachmentSummary(
  snapshot: Pdt2960ProformaChainResult & {
    paymentId: number;
    paymentDate: string;
    dueDateAfter: string;
    batchInvoiceNumbers: string[];
  },
): Record<string, unknown> {
  return {
    jiraKey: 'PDT-2960',
    goodsOrderId: snapshot.goodsOrderId,
    proformaInvoiceId: snapshot.proformaInvoiceId,
    customerLiabilityId: snapshot.customerLiabilityId,
    liabilityAmount: snapshot.liabilityAmount,
    dueDateBefore: snapshot.dueDateBefore,
    dueDateAfter: snapshot.dueDateAfter,
    paymentId: snapshot.paymentId,
    paymentDate: snapshot.paymentDate,
    customerIdentifier: snapshot.customerIdentifier,
    batchInvoiceNumbers: snapshot.batchInvoiceNumbers,
    assertionMode: 'AS-IS (PASS while bug open; flip when fixed)',
  };
}
