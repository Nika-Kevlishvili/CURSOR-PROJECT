import { expect } from '@playwright/test';
import type { APIRequestContext } from '@playwright/test';
import { randomGens } from '../../utils/randomGens';
import { Endpoints } from '../../fixtures/constants/endpoints';

/** Synthetic Jira-style key for this standalone automation (no TC .md). */
export const ONLINE_PAY_JUL30_JIRA_KEY = 'ONLINE-PAY-JUL30';

/**
 * Fixed EasyPay confirm DATE parameter — July 30, 2026 10:11:00 (YYYYMMDDHHmmss).
 * NOT generated via randomGens.generateOnlinePaymentDate.
 */
export const FIXED_EASYPAY_PAYMENT_DATE = '20260730101100';

/** ISO date prefix expected on payment/list when confirm succeeds (STATUS 00). */
export const FIXED_EASYPAY_PAYMENT_DATE_ISO_PREFIX = '2026-07-30';

export const EASYPAY_LIABILITY_AMOUNT = 100;

function resolveNomenclatureId(value: unknown): number | null {
  if (value == null) return null;
  if (typeof value === 'number') return value;
  if (typeof value === 'object' && value !== null && 'id' in value) {
    const id = (value as { id?: unknown }).id;
    return typeof id === 'number' ? id : null;
  }
  return null;
}

export type EasyPayChannelSetupResult = {
  channelId: number;
  combineLiabilities: boolean;
};

export type EasyPayJuly30Fx = {
  Request: APIRequestContext;
  GeneratePayload: {
    customers: { customer_legal: () => Record<string, unknown> };
    receivablesManagement: {
      customer_liability: () => Record<string, unknown>;
      CheckCombinedOnChannel: () => Promise<void>;
    };
  };
  Responses: {
    customer: Array<Record<string, unknown>>;
    customerLiability: unknown[];
    collectionChannel: unknown[];
    payment: unknown[];
  };
  Endpoints: typeof Endpoints;
  OnlinePaymentUrl: string;
};

export type EasyPayInitResult = {
  tid: string;
  merchantId: string;
  checksumInit: string;
  amount: string;
  initStatus: string;
};

export type EasyPayConfirmResult = {
  confirmStatus: string;
  confirmBody: Record<string, unknown>;
  paymentDateParam: string;
};

export type EasyPayPaymentListResult = {
  paymentId: number;
  paymentDate: string;
  totalElements: number;
};

/** Returns the fixed July 30, 2026 EasyPay DATE string (yyyyMMddHHmmss). */
export function formatFixedEasyPayPaymentDate(): string {
  return FIXED_EASYPAY_PAYMENT_DATE;
}

export async function createEasyPayTestCustomer(fx: EasyPayJuly30Fx): Promise<void> {
  const customerRes = await fx.Request.post(fx.Endpoints.customer, {
    data: fx.GeneratePayload.customers.customer_legal(),
  });
  await expect(customerRes).CheckResponse();
  fx.Responses.customer.push(await customerRes.json());
}

export async function createEasyPayTestLiabilities(
  fx: EasyPayJuly30Fx,
  count: number,
  amount = EASYPAY_LIABILITY_AMOUNT,
): Promise<void> {
  for (let i = 0; i < count; i += 1) {
    const payload = fx.GeneratePayload.receivablesManagement.customer_liability();
    payload.initialAmount = amount;
    const liabilityRes = await fx.Request.post(fx.Endpoints.customerLiability, { data: payload });
    await expect(liabilityRes).CheckResponse();
    fx.Responses.customerLiability.push(await liabilityRes.json());
  }
}

export async function findAndEnsureEasyPayCombinedChannel(fx: EasyPayJuly30Fx): Promise<EasyPayChannelSetupResult> {
  const channelListingGet = await fx.Request.get(
    `${fx.Endpoints.collectionChannel}?page=0&size=25&searchBy=NAME&prompt=EasyPay&collectionChannelType=ONLINE`,
  );
  await expect(channelListingGet).CheckResponse();
  const listingBody = await channelListingGet.json();
  const channelId = listingBody.content[0].id as number;
  fx.Responses.collectionChannel.push(channelId);

  const channelGet = await fx.Request.get(`${fx.Endpoints.collectionChannel}/${channelId}`);
  await expect(channelGet).CheckResponse();
  const channelBody = (await channelGet.json()) as Record<string, unknown>;

  if (channelBody.combineLiabilities === true) {
    return { channelId, combineLiabilities: true };
  }

  const employee = channelBody.employee as Record<string, unknown> | null | undefined;
  const performerId =
    (channelBody.performerId as number | undefined) ??
    (employee?.id as number | undefined) ??
    null;

  if (performerId == null) {
    return { channelId, combineLiabilities: false };
  }

  const performerType =
    (channelBody.performerType as string | undefined) ??
    (employee?.performerType as string | undefined) ??
    'TAG';

  const updatePayload = {
    name: channelBody.name,
    type: channelBody.type,
    employee: channelBody.employee ?? null,
    collectionPartnerId: resolveNomenclatureId(channelBody.collectionPartnerId),
    numberOfIncomeAccount: channelBody.numberOfIncomeAccount,
    currencyId: resolveNomenclatureId(channelBody.currencyId),
    customerConditionType: channelBody.customerConditionType ?? 'ALL_CUSTOMERS',
    condition: channelBody.condition ?? null,
    listOfCustomers: channelBody.listOfCustomers ?? null,
    excludeLiabilitiesByPrefix: channelBody.excludeLiabilitiesByPrefix ?? [],
    excludeLiabilitiesByAmount: channelBody.excludeLiabilitiesByAmount ?? {
      lessThan: null,
      greaterThan: null,
    },
    priorityLiabilitiesByPrefix: channelBody.priorityLiabilitiesByPrefix ?? [],
    typeOfFile: channelBody.typeOfFile ?? null,
    bankIds: channelBody.bankIds ?? null,
    globalBank: channelBody.globalBank ?? null,
    dataSendingSchedule: channelBody.dataSendingSchedule ?? null,
    dataReceivingSchedule: channelBody.dataReceivingSchedule ?? null,
    numberOfWorkingDays: channelBody.numberOfWorkingDays ?? null,
    calendarId: resolveNomenclatureId(channelBody.calendarId),
    waitingPeriodToleranceInHours: channelBody.waitingPeriodToleranceInHours ?? null,
    folderForFileReceiving: channelBody.folderForFileReceiving ?? null,
    folderForFileSending: channelBody.folderForFileSending ?? null,
    emailForFileSending: channelBody.emailForFileSending ?? null,
    combineLiabilities: true,
    performerId,
    performerType,
  };

  const changeValue = await fx.Request.put(`${fx.Endpoints.collectionChannel}/${channelId}`, {
    data: updatePayload,
  });
  await expect(changeValue).CheckResponse();

  return { channelId, combineLiabilities: true };
}

/** Test payment-api default from application-test.properties (`easypay.merchant.id`). */
export const TEST_EASYPAY_MERCHANT_ID = '7000005';

export async function getEasyPayMerchantId(fx: EasyPayJuly30Fx): Promise<string> {
  if (process.env.EASYPAY_MERCHANT_ID) {
    return process.env.EASYPAY_MERCHANT_ID;
  }

  const configRes = await fx.Request.get(`${fx.Endpoints.systemConfiguration}`);
  if (configRes.ok()) {
    const responseBody = await configRes.json();
    if (responseBody?.easyPayMerchantId != null) {
      return String(responseBody.easyPayMerchantId);
    }
  }

  return TEST_EASYPAY_MERCHANT_ID;
}

export async function calculateEasyPayInitChecksum(
  fx: EasyPayJuly30Fx,
  customerNumber: string,
  tid: string,
  merchantId: string,
): Promise<string> {
  const request = await fx.Request.get(
    `${fx.OnlinePaymentUrl}epay/calculate-check-sum-init?IDN=${customerNumber}&TID=${tid}&MERCHANTID=${merchantId}&TYPE=BILLING`,
  );
  await expect(request).CheckResponse();
  return request.text();
}

export async function initEasyPayPayment(
  fx: EasyPayJuly30Fx,
  customerNumber: string,
  tid: string,
  merchantId: string,
  checksumInit: string,
): Promise<{ amount: string; initStatus: string; initBody: Record<string, unknown> }> {
  const request = await fx.Request.get(
    `${fx.OnlinePaymentUrl}epay/init-pay?IDN=${customerNumber}&TID=${tid}&MERCHANTID=${merchantId}&TYPE=BILLING&CHECKSUM=${checksumInit}`,
  );
  await expect(request).CheckResponse();
  const initBody = (await request.json()) as Record<string, unknown>;
  return {
    amount: String(initBody.AMOUNT ?? ''),
    initStatus: String(initBody.STATUS ?? ''),
    initBody,
  };
}

export async function calculateEasyPayConfirmChecksum(
  fx: EasyPayJuly30Fx,
  params: {
    paymentDate: string;
    merchantId: string;
    customerNumber: string;
    total: string;
    tid: string;
  },
): Promise<string> {
  const { paymentDate, merchantId, customerNumber, total, tid } = params;
  const request = await fx.Request.get(
    `${fx.OnlinePaymentUrl}epay/calculate-check-sum-confirm?DATE=${paymentDate}&TYPE=BILLING&MERCHANTID=${merchantId}&IDN=${customerNumber}&TOTAL=${total}&TID=${tid}`,
  );
  await expect(request).CheckResponse();
  return request.text();
}

export async function confirmEasyPayPayment(
  fx: EasyPayJuly30Fx,
  params: {
    paymentDate: string;
    merchantId: string;
    customerNumber: string;
    total: string;
    tid: string;
    checksumConfirm: string;
  },
): Promise<EasyPayConfirmResult> {
  const { paymentDate, merchantId, customerNumber, total, tid, checksumConfirm } = params;
  const request = await fx.Request.get(
    `${fx.OnlinePaymentUrl}epay/confirm-pay?DATE=${paymentDate}&TYPE=BILLING&MERCHANTID=${merchantId}&IDN=${customerNumber}&TOTAL=${total}&TID=${tid}&CHECKSUM=${checksumConfirm}`,
  );
  if (request.status() >= 400) {
    const errorText = await request.text();
    return {
      confirmStatus: `HTTP_${request.status()}`,
      confirmBody: { error: errorText },
      paymentDateParam: paymentDate,
    };
  }
  const confirmBody = (await request.json()) as Record<string, unknown>;
  return {
    confirmStatus: String(confirmBody.STATUS ?? ''),
    confirmBody,
    paymentDateParam: paymentDate,
  };
}

export async function runEasyPayInitFlow(fx: EasyPayJuly30Fx): Promise<EasyPayInitResult> {
  const customerNumber = String(fx.Responses.customer[0].customerNumber);
  const tid = randomGens.generateTID();
  const merchantId = await getEasyPayMerchantId(fx);
  const checksumInit = await calculateEasyPayInitChecksum(fx, customerNumber, tid, merchantId);
  const { amount, initStatus } = await initEasyPayPayment(fx, customerNumber, tid, merchantId, checksumInit);

  return { tid, merchantId, checksumInit, amount, initStatus };
}

export async function runEasyPayConfirmFlow(
  fx: EasyPayJuly30Fx,
  init: Pick<EasyPayInitResult, 'tid' | 'merchantId' | 'amount'>,
  paymentDate = formatFixedEasyPayPaymentDate(),
): Promise<EasyPayConfirmResult> {
  const customerNumber = String(fx.Responses.customer[0].customerNumber);
  const checksumConfirm = await calculateEasyPayConfirmChecksum(fx, {
    paymentDate,
    merchantId: init.merchantId,
    customerNumber,
    total: init.amount,
    tid: init.tid,
  });

  return confirmEasyPayPayment(fx, {
    paymentDate,
    merchantId: init.merchantId,
    customerNumber,
    total: init.amount,
    tid: init.tid,
    checksumConfirm,
  });
}

export async function validateEasyPayPaymentInList(
  fx: EasyPayJuly30Fx,
  customerIdentifier: string,
  expectedDatePrefix = FIXED_EASYPAY_PAYMENT_DATE_ISO_PREFIX,
): Promise<EasyPayPaymentListResult> {
  const payload = {
    page: 0,
    size: 25,
    prompt: customerIdentifier,
    searchFields: 'CUSTOMER_IDENTIFIER',
  };
  const paymentList = await fx.Request.post(`${fx.Endpoints.payment}/list`, { data: payload });
  await expect(paymentList).CheckResponse();
  const responseBody = await paymentList.json();
  expect(responseBody.totalElements).toBeGreaterThan(0);
  expect(responseBody.content[0]).toBeDefined();

  const paymentRow = responseBody.content[0] as { id?: number; paymentDate?: string };
  const paymentId = Number(paymentRow.id);
  const paymentDate = String(paymentRow.paymentDate ?? '');

  expect(paymentDate).toContain(expectedDatePrefix);

  fx.Responses.payment.push(paymentId);

  return {
    paymentId,
    paymentDate,
    totalElements: Number(responseBody.totalElements),
  };
}
