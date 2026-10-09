import { expect } from '@playwright/test';
import type { APIRequestContext } from '@playwright/test';
import { randomGens } from '../../utils/randomGens';
import { Endpoints } from '../../fixtures/constants/endpoints';

export const PDT_2459_OVERDUE_LIABILITY_AMOUNT = 50;

export type Pdt2459Fx = {
  Request: APIRequestContext;
  GeneratePayload: any;
  Responses: any;
  Endpoints: typeof Endpoints;
  receivableValidations?: { paymentValidation: (paymentId: number) => Promise<void> };
};

export type Pdt2459LpfSnapshot = {
  lpfId: number;
  lpfAmount: number;
  lpfLiabilityId: number;
  overdueLiabilityId: number;
  lpfPaymentId: number;
  reverseCustomerId: number;
  customerIdentifier: string;
  reversalLpfId: number;
  reversalLpfNumber: string;
};

export type Pdt2459ReversalLpfDetails = {
  reversalLpfId: number;
  reversalLpfNumber: string;
};

export type Pdt2459LpfPreparation = {
  lpfRecord: Record<string, unknown>;
  overdueLiabilityId: number;
  sourceLiabilityPaymentId: number;
};

/** TC steps 1–4: customer, collection channel, payment package, daily interest rate. */
export async function setupPdt2459ReceivablesBase(fx: Pdt2459Fx): Promise<void> {
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

  const interestRes = await fx.Request.post(fx.Endpoints.interestRate, {
    data: fx.GeneratePayload.receivablesManagement.dailyInterestRate(),
  });
  await expect(interestRes).CheckResponse();
  fx.Responses.interestRate.push(await interestRes.json());
}

/** TC step 2: manual liability with interest (one week before occurrence/due — REG-1049 dates). */
export async function createPdt2459OverdueLiability(
  fx: Pdt2459Fx,
  principal = PDT_2459_OVERDUE_LIABILITY_AMOUNT,
): Promise<number> {
  const payload = fx.GeneratePayload.receivablesManagement.customer_liability();
  payload.initialAmount = principal;
  payload.currentAmount = principal;
  payload.occurrenceDate = randomGens.generateOneWeekBeforeDate('dd-mm-yyyy');
  payload.dueDate = randomGens.generateOneWeekBeforeDate('dd-mm-yyyy');
  payload.applicableInterestRateId = fx.Responses.interestRate[0];

  const liabilityRes = await fx.Request.post(fx.Endpoints.customerLiability, { data: payload });
  await expect(liabilityRes).CheckResponse();
  const liabilityId = (await liabilityRes.json()) as number;
  fx.Responses.customerLiability.push(liabilityId);
  return liabilityId;
}

/**
 * Pay source liability for LPF setup without polluting Responses.payment[0].
 * paymentReverse() must target the LPF payment only.
 */
async function payPdt2459SourceLiabilityWithoutTracking(
  fx: Pdt2459Fx,
  principal: number,
): Promise<number> {
  const paymentPayload = await fx.GeneratePayload.receivablesManagement.payment();
  paymentPayload.initialAmount = principal;
  const paymentRes = await fx.Request.post(fx.Endpoints.payment, { data: paymentPayload });
  await expect(paymentRes).CheckResponse();
  return (await paymentRes.json()) as number;
}

function readPdt2459ParentLiabilityId(lpfBody: Record<string, unknown>): number {
  const parentShort = lpfBody.parentLiabilityShortResponse as { id?: number } | undefined;
  return Number(lpfBody.parentLiabilityId ?? parentShort?.id ?? -1);
}

/**
 * Pick the LPF whose linked liability has the lowest id for this overdue liability.
 * Auto payment offset targets the earliest LPF liability — tracking must match.
 */
export async function selectPdt2459LpfForOverdueLiability(
  fx: Pdt2459Fx,
  customerIdentifier: string,
  overdueLiabilityId: number,
): Promise<Record<string, unknown>> {
  const listRes = await fx.Request.get(
    `${fx.Endpoints.latePaymentFine}/list?page=0&size=50&prompt=${customerIdentifier}&type=LATE_PAYMENT_FINE&searchBy=CUSTOMER`,
  );
  await expect(listRes).CheckResponse();
  const listBody = await listRes.json();
  const rows = (listBody.content ?? []) as Array<{ id?: number }>;

  let bestLpf: Record<string, unknown> | undefined;
  let bestLiabilityId = Number.POSITIVE_INFINITY;

  for (const row of rows) {
    const lpfId = Number(row.id);
    if (!Number.isFinite(lpfId)) continue;

    const lpfGet = await fx.Request.get(`${fx.Endpoints.latePaymentFine}/${lpfId}`);
    await expect(lpfGet).CheckResponse();
    const lpfBody = (await lpfGet.json()) as Record<string, unknown>;

    if (readPdt2459ParentLiabilityId(lpfBody) !== overdueLiabilityId) continue;
    if (String(lpfBody.type ?? '') !== 'LATE_PAYMENT_FINE') continue;

    const liabilityId = await findPdt2459LpfLiabilityId(fx, customerIdentifier, lpfId);
    if (liabilityId < bestLiabilityId) {
      bestLiabilityId = liabilityId;
      bestLpf = { ...row, ...lpfBody, id: lpfId };
    }
  }

  expect(
    bestLpf,
    `LPF for overdue liability ${overdueLiabilityId} on customer ${customerIdentifier}`,
  ).toBeTruthy();
  return bestLpf!;
}

/** TC step 3: trigger LPF job once, then wait for LATE_PAYMENT_FINE tied to overdue liability. */
export async function waitForPdt2459LpfGeneration(
  fx: Pdt2459Fx,
  overdueLiabilityId: number,
): Promise<Record<string, unknown>> {
  const customerIdentifier = String(fx.Responses.customer[0].identifier);

  const lpfJob = await fx.Request.post(`${fx.Endpoints.latePaymentFine}/job`, {
    timeout: 2 * 60 * 1000,
  });
  await expect(lpfJob).CheckResponse();
  const jobBody = await lpfJob.json();
  expect(jobBody, 'LPF generation job must return success indicator').toBe(1);

  let lpf: Record<string, unknown> | undefined;
  await expect
    .poll(
      async () => {
        lpf = await selectPdt2459LpfForOverdueLiability(fx, customerIdentifier, overdueLiabilityId);
        return lpf?.id;
      },
      {
        message: `LPF not listed for overdue liability ${overdueLiabilityId} (PDT-2459 extended poll)`,
        timeout: 90_000,
        intervals: [1000, 2000, 5000],
      },
    )
    .toBeTruthy();

  fx.Responses.latePaymentFine.push(lpf!);
  return lpf!;
}

/**
 * REG-1049 / pdt-2187 pattern: past-due liability → full payment → due shift to yesterday → LPF job.
 * Source liability payment is not stored in Responses.payment so reverse targets LPF payment only.
 */
export async function preparePdt2459LpfFromOverdueLiability(
  fx: Pdt2459Fx,
  principal = PDT_2459_OVERDUE_LIABILITY_AMOUNT,
): Promise<Pdt2459LpfPreparation> {
  const overdueLiabilityId = await createPdt2459OverdueLiability(fx, principal);

  const sourceLiabilityPaymentId = await payPdt2459SourceLiabilityWithoutTracking(fx, principal);

  await expect
    .poll(
      async () => {
        const get = await fx.Request.get(`${fx.Endpoints.customerLiability}/${overdueLiabilityId}`);
        await expect(get).CheckResponse();
        const body = await get.json();
        return Number(body.currentAmount ?? -1);
      },
      {
        message: 'PDT-2459 source liability not fully offset before LPF job',
        timeout: 90_000,
        intervals: [2000, 5000],
      },
    )
    .toBe(0);

  const dueChange = await fx.Request.put(
    `${fx.Endpoints.customerLiability}/${overdueLiabilityId}/due-date-change?dueDate=${randomGens.generateYesterdaysDate('yyyy-mm-dd')}`,
  );
  await expect(dueChange).CheckResponse();

  const lpfRecord = await waitForPdt2459LpfGeneration(fx, overdueLiabilityId);

  return { lpfRecord, overdueLiabilityId, sourceLiabilityPaymentId };
}

export async function getPdt2459LpfDetails(
  fx: Pdt2459Fx,
  lpfId: number,
): Promise<{ lpfAmount: number; lpfNumber: string }> {
  const lpfGet = await fx.Request.get(`${fx.Endpoints.latePaymentFine}/${lpfId}`);
  await expect(lpfGet).CheckResponse();
  const body = await lpfGet.json();
  const lpfAmount = Number(body.amount ?? 0);
  expect(lpfAmount, 'LPF amount must be positive').toBeGreaterThan(0);
  return {
    lpfAmount,
    lpfNumber: String(body.latePaymentNumber ?? body.number ?? lpfId),
  };
}

/**
 * Resolve LPF-linked liability via GET detail — list DTO omits outgoingDocumentType / LPF refs
 * (CustomerLiabilityListingResponse vs CustomerLiabilityResponse).
 */
export async function findPdt2459LpfLiabilityId(
  fx: Pdt2459Fx,
  customerIdentifier: string,
  lpfId: number,
): Promise<number> {
  const listRes = await fx.Request.get(
    `${fx.Endpoints.customerLiability}/list?page=0&size=50&columns=ID&direction=DESC&prompt=${customerIdentifier}&searchFields=CUSTOMER`,
  );
  await expect(listRes).CheckResponse();
  const listBody = await listRes.json();
  const rows = (listBody.content ?? []) as Array<{ id?: number }>;

  for (const row of rows) {
    const liabilityId = Number(row.id);
    if (!Number.isFinite(liabilityId)) continue;
    const getRes = await fx.Request.get(`${fx.Endpoints.customerLiability}/${liabilityId}`);
    await expect(getRes).CheckResponse();
    const body = await getRes.json();
    const docType = String(body.outgoingDocumentType ?? '');
    const lpfRef = body.latePaymentFineShortResponse as { id?: number } | undefined;
    if (docType === 'LATE_PAYMENT_FINE' && Number(lpfRef?.id) === lpfId) {
      return liabilityId;
    }
  }

  expect(undefined, `LPF liability for latePaymentFine id ${lpfId}`).toBeTruthy();
  return 0;
}

export async function assertPdt2459LiabilityCurrentAmount(
  fx: Pdt2459Fx,
  liabilityId: number,
  expectedAmount: number,
  label: string,
): Promise<void> {
  const getRes = await fx.Request.get(`${fx.Endpoints.customerLiability}/${liabilityId}`);
  await expect(getRes).CheckResponse();
  const body = await getRes.json();
  expect(Number(body.currentAmount), label).toEqual(expectedAmount);
}

/** TC step 4: manual payment covering LPF liability amount only. */
export async function payPdt2459LpfLiability(
  fx: Pdt2459Fx,
  lpfAmount: number,
): Promise<number> {
  const paymentPayload = await fx.GeneratePayload.receivablesManagement.payment();
  paymentPayload.initialAmount = lpfAmount;
  const paymentRes = await fx.Request.post(fx.Endpoints.payment, { data: paymentPayload });
  await expect(paymentRes).CheckResponse();
  const paymentId = (await paymentRes.json()) as number;
  fx.Responses.payment.push(paymentId);

  if (fx.receivableValidations) {
    await fx.receivableValidations.paymentValidation(paymentId);
  }
  return paymentId;
}

/** TC step 6: second customer for payment reverse target. */
export async function createPdt2459ReverseCustomer(fx: Pdt2459Fx): Promise<number> {
  const customerRes = await fx.Request.post(fx.Endpoints.customer, {
    data: fx.GeneratePayload.customers.customer_legal(),
  });
  await expect(customerRes).CheckResponse();
  const body = await customerRes.json();
  fx.Responses.customer.push(body);
  expect(fx.Responses.customer.length).toBe(2);
  return body.id as number;
}

/** TC step 7: POST /payment/cancel (PaymentCancelRequest). */
export async function reversePdt2459Payment(fx: Pdt2459Fx): Promise<number> {
  const reverseRes = await fx.Request.post(`${fx.Endpoints.payment}/cancel`, {
    data: fx.GeneratePayload.receivablesManagement.paymentReverse(),
  });
  await expect(reverseRes).CheckResponse();
  const newPaymentId = (await reverseRes.json()) as number;
  fx.Responses.payment.push(newPaymentId);
  return newPaymentId;
}

export async function assertPdt2459PaymentReversed(fx: Pdt2459Fx, paymentId: number): Promise<void> {
  const getRes = await fx.Request.get(`${fx.Endpoints.payment}/${paymentId}`);
  await expect(getRes).CheckResponse();
  const body = await getRes.json();
  expect(body.status).toEqual('REVERSED');
}

/**
 * Payment reverse creates a new REVERSAL_OF_LATE_PAYMENT_FINE (negative amount) linked to the original.
 * Original stays type LATE_PAYMENT_FINE; latePaymentFineReversalShortResponse points to the new record.
 */
export async function assertPdt2459NewReversalLpfCreated(
  fx: Pdt2459Fx,
  originalLpfId: number,
  lpfAmount: number,
): Promise<Pdt2459ReversalLpfDetails> {
  const originalGet = await fx.Request.get(`${fx.Endpoints.latePaymentFine}/${originalLpfId}`);
  await expect(originalGet).CheckResponse();
  const originalBody = await originalGet.json();

  expect(String(originalBody.type), 'Original LPF type must remain LATE_PAYMENT_FINE').toEqual(
    'LATE_PAYMENT_FINE',
  );
  expect(
    originalBody.latePaymentFineReversalShortResponse,
    'Original LPF must link to newly created reversal via latePaymentFineReversalShortResponse',
  ).toBeTruthy();

  const reversalLpfId = Number(originalBody.latePaymentFineReversalShortResponse.id);
  expect(reversalLpfId, 'Reversal LPF id from latePaymentFineReversalShortResponse').toBeGreaterThan(0);
  expect(reversalLpfId, 'Reversal LPF must be a new record, not the original').not.toEqual(originalLpfId);

  const reversalGet = await fx.Request.get(`${fx.Endpoints.latePaymentFine}/${reversalLpfId}`);
  await expect(reversalGet).CheckResponse();
  const reversalBody = await reversalGet.json();

  expect(String(reversalBody.type), 'New reversal record type').toEqual('REVERSAL_OF_LATE_PAYMENT_FINE');
  expect(Number(reversalBody.amount), 'Reversal LPF amount must negate original LPF amount').toBeCloseTo(
    -lpfAmount,
    2,
  );
  expect(
    Number(reversalBody.parentLatePaymentFineShortResponse?.id),
    'Reversal LPF parentLatePaymentFineShortResponse must reference original LPF',
  ).toEqual(originalLpfId);

  const reversalLpfNumber = String(reversalBody.latePaymentNumber ?? reversalLpfId);
  fx.Responses.latePaymentFine.push(reversalBody);

  return { reversalLpfId, reversalLpfNumber };
}

/** Expected fix: LPF liability stays covered (currentAmount === 0) via receivable from reversal LPF. */
export async function assertPdt2459LpfLiabilityCoveredAfterReverse(
  fx: Pdt2459Fx,
  originalLpfId: number,
  lpfLiabilityId: number,
  reversalLpfId: number,
  lpfAmount: number,
): Promise<void> {
  const getRes = await fx.Request.get(`${fx.Endpoints.customerLiability}/${lpfLiabilityId}`);
  await expect(getRes).CheckResponse();
  const liabilityBody = await getRes.json();

  expect(String(liabilityBody.outgoingDocumentType), 'LPF liability outgoingDocumentType').toEqual(
    'LATE_PAYMENT_FINE',
  );
  expect(
    Number(liabilityBody.latePaymentFineShortResponse?.id),
    'LPF liability must reference original latePaymentFine id',
  ).toEqual(originalLpfId);
  expect(Number(liabilityBody.currentAmount), 'LPF liability currentAmount after reverse').toEqual(0);

  const offsettingList = (liabilityBody.customerLiabilityOffsettingReponseList ?? []) as Array<{
    id?: number;
    offsettingObjectType?: string;
    amount?: number;
  }>;
  expect(
    offsettingList.length,
    'LPF liability must have customerLiabilityOffsettingReponseList entries after reverse',
  ).toBeGreaterThan(0);
  const receivableOffset = offsettingList.find((item) => item.offsettingObjectType === 'RECEIVABLE');
  expect(
    receivableOffset,
    'LPF liability must be covered by RECEIVABLE offsetting (reversal receivable) after reverse',
  ).toBeTruthy();
  expect(Number(receivableOffset!.amount), 'Receivable offset amount must equal LPF amount').toBeCloseTo(
    lpfAmount,
    2,
  );

  const receivableId = Number(receivableOffset!.id);
  expect(receivableId, 'Receivable id from liability offsetting list').toBeGreaterThan(0);

  const receivableGet = await fx.Request.get(`${fx.Endpoints.customerReceivable}/${receivableId}`);
  await expect(receivableGet).CheckResponse();
  const receivableBody = await receivableGet.json();

  expect(String(receivableBody.outgoingDocumentType), 'Reversal receivable outgoingDocumentType').toEqual(
    'LATE_PAYMENT_FINE',
  );
  expect(
    Number(receivableBody.latePaymentFineShortResponse?.id),
    'Receivable must reference the new reversal LPF id',
  ).toEqual(reversalLpfId);
  expect(Number(receivableBody.initialAmount), 'Reversal receivable initialAmount').toBeCloseTo(lpfAmount, 2);
  expect(Number(receivableBody.currentAmount), 'Reversal receivable must be fully offset').toEqual(0);
}

export function buildPdt2459AttachmentSummary(snapshot: Pdt2459LpfSnapshot): Record<string, unknown> {
  return {
    jiraKey: 'PDT-2459',
    overdueLiabilityId: snapshot.overdueLiabilityId,
    lpfId: snapshot.lpfId,
    reversalLpfId: snapshot.reversalLpfId,
    reversalLpfNumber: snapshot.reversalLpfNumber,
    lpfLiabilityId: snapshot.lpfLiabilityId,
    lpfAmount: snapshot.lpfAmount,
    lpfPaymentId: snapshot.lpfPaymentId,
    reverseCustomerId: snapshot.reverseCustomerId,
    customerIdentifier: snapshot.customerIdentifier,
  };
}
