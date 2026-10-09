/**
 * [PHN-4377] Dev2 reproduction of standard-billing failure
 * Invalid date 'SEPTEMBER 31'.
 *
 * Contract payment term is Certain day = 31. Billing-run invoice date is
 * 28 September 2026 and invoice due date is ACCORDING_TO_THE_CONTRACT.
 * On Test 2 ES that combination throws inside
 * BillingPaymentTermDayCalculationService.calculateDeadline (LocalDate.of).
 *
 * Environment: Dev2
 * - API that answers on this network: https://devapps.energo-pro.bg/backend/phoenix2-dev
 *   (same service as http://10.236.20.11:8092, the dev2 Swagger host).
 *   https://devapps.energo-pro.bg/backend/phoenix-dev2 returns 404 for these routes.
 * - Portal: https://devapps.energo-pro.bg/app/phoenix2-dev/
 *
 * Reference spec(s):
 * - tests/cursor/SLR-18167-dev2-volumes-billing-replica.spec.ts
 * - tests/cursor/slr-18167-dev2-volumes-billing-replica.fixtures.ts
 *
 * Swagger (dev2, Cursor-Project/config/swagger/dev2/swagger-spec.json):
 * - CreateInvoicePaymentTermRequest.calendarType enum includes CERTAIN_DAYS
 * - BillingRunCommonParameters.invoiceDueDate enum includes ACCORDING_TO_THE_CONTRACT
 * - PATCH /billing-run/start-billing?billingRunId=
 * - PATCH /billing-run/download-error-report/{id}?protocol=BILLING
 * - GET /billing-run/draft-invoices?id=
 */

import * as fs from 'fs';
import * as path from 'path';
import { request as playwrightRequest } from '@playwright/test';
import { expect, test } from '../../fixtures/baseFixture';
import type { baseFixture } from '../../fixtures/baseFixture';
import { envVariables } from '../../fixtures/envCashed';
import { getToken } from '../../fixtures/utils/auth';
import { nomenclatures } from '../../jsons/payloads/create/nomenclatures/nomenclatures';
import { templates } from '../../jsons/payloads/create/nomenclatures/templates';
import * as ExcelJS from 'exceljs';
import {
  createSlr18167ForVolumesBillingRun,
  getDraftInvoiceIds,
  runSlr18167ContractScaleAndBbpPrechain,
  type Slr18167BillingDates,
  type Slr18167Fx,
  type Slr18167PrechainResult,
} from './slr-18167-dev2-volumes-billing-replica.fixtures';

export const PHN_4377_KEY = 'PHN-4377';
export const PHN_4377_TITLE =
  "Billing run - Invoice is not created - ExceptionMessage: [Invalid date 'SEPTEMBER 31']";

export const PHN_4377_API_BASE = 'https://devapps.energo-pro.bg/backend/phoenix2-dev';
export const PHN_4377_PORTAL_BASE = 'https://devapps.energo-pro.bg/app/phoenix2-dev';

export const PHN_4377_INVOICE_DATE = '2026-09-28';
export const PHN_4377_TAX_EVENT_DATE = '2026-09-27';
/** Keeps the February scale and March profile inside the billed window. */
export const PHN_4377_MAX_END_DATE = '2026-04-30';
export const PHN_4377_CERTAIN_DAY = 31;
export const PHN_4377_BUG_MESSAGE = "Invalid date 'SEPTEMBER 31'";

const BILLING_RUN_ROOT = 'billing-run';
const POLL_MS = 15_000;
const POLL_MAX_MS = 12 * 60 * 1000;

export type Phn4377Fx = Slr18167Fx;

type TermPayload = {
  invoicePaymentTerms: Array<{
    calendarType: string;
    type: string;
    value: number;
    valueFrom: number | null;
    valueTo: number | null;
    name: string;
  }>;
};

type ContractPayload = {
  productParameters: {
    invoicePaymentTermValue: number | null;
    invoicePaymentTermId?: number | null;
  };
};

/**
 * The shared volumes prechain posts terms() then product_contract().
 * Override those two generators so the saved term is CERTAIN_DAYS 31.
 */
export function forceCertainDay31(GeneratePayload: baseFixture['GeneratePayload']): void {
  const products = GeneratePayload.productAndServices;
  const originalTerm = products.term.bind(products);
  products.term = () => {
    const payload = originalTerm() as TermPayload;
    const term = payload.invoicePaymentTerms[0];
    term.calendarType = 'CERTAIN_DAYS';
    term.type = 'CERTAIN_DAYS';
    term.value = PHN_4377_CERTAIN_DAY;
    term.valueFrom = null;
    term.valueTo = null;
    term.name = '31 day of the month';
    return payload;
  };

  const contracts = GeneratePayload.contractsAndOrders;
  const originalContract = contracts.product_contract.bind(contracts);
  contracts.product_contract = async (...args: unknown[]) => {
    const payload = (await originalContract(...(args as []))) as ContractPayload;
    payload.productParameters.invoicePaymentTermValue = PHN_4377_CERTAIN_DAY;
    return payload;
  };

  const customers = GeneratePayload.customers;
  const originalCustomer = customers.customer_legal.bind(customers);
  customers.customer_legal = () => {
    const payload = originalCustomer() as {
      address?: { number?: string | null };
      addressTransl?: Record<string, unknown>;
    };
    payload.addressTransl = {
      foreign: false,
      number: payload.address?.number ?? '1',
      additionalInformation: null,
      block: null,
      entrance: null,
      floor: null,
      apartment: null,
      mailbox: null,
      localAddressData: {
        country: 'BULGARIA',
        region: 'REGION',
        municipality: 'MUNICIPALITY',
        populatedPlace: 'CITY',
        zipCode: '1000',
        district: 'DISTRICT',
        residentialArea: 'QUARTER',
        street: 'STREET',
        streetType: 'STREET',
        residentialAreaType: 'QUARTER',
      },
    };
    return payload;
  };
}

export async function readContractPaymentTerm(
  Request: Phn4377Fx['Request'],
  contractId: number,
): Promise<{ value: number | null; calendarType: string | null; termId: number | null }> {
  const contractGet = await Request.get(`product-contract/${contractId}?version=1`);
  await expect(contractGet).CheckResponse();
  const body = (await contractGet.json()) as {
    productParameters?: {
      invoicePaymentTermValue?: number | null;
      invoicePaymentTerm?: { id?: number; termId?: number; calendarType?: string };
    };
  };
  const parameters = body.productParameters;
  const value = parameters?.invoicePaymentTermValue ?? null;
  const term = parameters?.invoicePaymentTerm;
  return {
    value: value == null ? null : Number(value),
    calendarType: term?.calendarType ?? null,
    termId: term?.id ?? term?.termId ?? null,
  };
}

/**
 * Cached envVariables.json was not built against phoenix2-dev (calendar 1123 is 404 there).
 * Rebuild nomenclature and template ids on Dev2 and write them into the shared object
 * that payload generators read at call time.
 */
export async function refreshDev2ReferenceData(): Promise<void> {
  const headers = {
    Accept: '*/*',
    Authorization: `Bearer ${getToken()}`,
  };
  const jsonContext = await playwrightRequest.newContext({
    baseURL: `${PHN_4377_API_BASE}/`,
    ignoreHTTPSErrors: true,
    extraHTTPHeaders: { ...headers, 'Content-Type': 'application/json' },
  });
  const uploadContext = await playwrightRequest.newContext({
    baseURL: `${PHN_4377_API_BASE}/`,
    ignoreHTTPSErrors: true,
    extraHTTPHeaders: headers,
  });
  try {
    const templateData = await new templates(uploadContext, jsonContext).generateEveryTemplate();
    const nomenclatureData = await new nomenclatures(jsonContext).generateNomenclatures(templateData);
    const fresh = { ...nomenclatureData, ...templateData };
    for (const key of Object.keys(envVariables)) {
      delete envVariables[key];
    }
    Object.assign(envVariables, fresh);
    const envFile = path.resolve(__dirname, '../../fixtures/envVariables.json');
    fs.writeFileSync(envFile, JSON.stringify(fresh, null, 4), 'utf-8');
  } finally {
    await jsonContext.dispose();
    await uploadContext.dispose();
  }
}

function ymd(value: string): string {
  return value.slice(0, 10);
}

export async function resolveSeptemberBillingDates(
  Request: Phn4377Fx['Request'],
): Promise<Slr18167BillingDates & { openPeriods: string[] }> {
  const listed = await Request.get(
    'accounting-period?page=0&size=100&status=OPEN&sortBy=END_DATE&direction=DESC',
  );
  await expect(listed).CheckResponse();
  const page = (await listed.json()) as { content?: Array<Record<string, unknown>> };
  const open = (page.content ?? [])
    .map((row) => ({
      id: Number(row.accountPeriodId ?? row.id),
      name: String(row.name ?? ''),
      startDate: ymd(String(row.startDate ?? '')),
      endDate: ymd(String(row.endDate ?? '')),
    }))
    .filter((row) => Number.isFinite(row.id) && row.id > 0);

  const september = open.find(
    (period) =>
      period.startDate <= PHN_4377_TAX_EVENT_DATE && period.endDate >= PHN_4377_INVOICE_DATE,
  );

  return {
    datesAdapted: !september,
    invoiceDate: september ? PHN_4377_INVOICE_DATE : null,
    taxEventDate: september ? PHN_4377_TAX_EVENT_DATE : null,
    maxEndDate: PHN_4377_MAX_END_DATE,
    accountingPeriodId: september?.id ?? null,
    openPeriodName: september ? september.name || `${september.startDate}..${september.endDate}` : null,
    openPeriods: open.map((period) => `${period.id}:${period.name}:${period.startDate}..${period.endDate}`),
  };
}

export async function runPhn4377Prechain(ctx: Phn4377Fx): Promise<Slr18167PrechainResult> {
  forceCertainDay31(ctx.GeneratePayload);
  return runSlr18167ContractScaleAndBbpPrechain(ctx);
}

export async function startBillingAndCollectOutcome(
  Request: Phn4377Fx['Request'],
  billingRunId: number,
): Promise<{ status: string; draftInvoiceIds: number[]; errorMessages: string[] }> {
  const startRes = await Request.patch(
    `${BILLING_RUN_ROOT}/start-billing?billingRunId=${billingRunId}`,
  );
  await expect(startRes).CheckResponse();

  const waiting = new Set(['', 'INITIAL', 'IN_PROGRESS_DRAFT', 'IN_PROGRESS', 'PROCESSING', 'NEW', 'CREATED']);
  const maxAttempts = Math.max(1, Math.ceil(POLL_MAX_MS / POLL_MS));
  let status = '';
  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    const statusRes = await Request.get(`${BILLING_RUN_ROOT}/${billingRunId}`);
    await expect(statusRes).CheckResponse();
    const body = (await statusRes.json()) as { commonParameters?: { status?: string } };
    status = String(body.commonParameters?.status ?? '');
    if (!waiting.has(status)) {
      break;
    }
    if (attempt < maxAttempts) {
      await new Promise((resolve) => setTimeout(resolve, POLL_MS));
    }
  }

  const draftInvoiceIds = status === 'DRAFT' || status === 'COMPLETED' || status === 'GENERATED'
    ? await getDraftInvoiceIds(Request, billingRunId)
    : [];
  const errorMessages = await downloadBillingErrorMessages(Request, billingRunId);
  return { status, draftInvoiceIds, errorMessages };
}

async function downloadBillingErrorMessages(
  Request: Phn4377Fx['Request'],
  billingRunId: number,
): Promise<string[]> {
  const res = await Request.patch(
    `${BILLING_RUN_ROOT}/download-error-report/${billingRunId}?protocol=BILLING`,
  );
  if (res.status() >= 400) {
    return [`error-report HTTP ${res.status()}`];
  }
  await expect(res).CheckResponse();
  const buffer = await res.body();
  const workbook = new ExcelJS.Workbook();
  await workbook.xlsx.load(buffer as unknown as ExcelJS.Buffer);
  const sheet = workbook.worksheets[0];
  if (!sheet) {
    return [];
  }
  const messages: string[] = [];
  sheet.eachRow((row, rowNumber) => {
    if (rowNumber === 1) {
      return;
    }
    const errorMessage = String(row.getCell(2).value ?? '').trim();
    if (errorMessage) {
      messages.push(errorMessage);
    }
  });
  return messages;
}

export function buildPhn4377Links(ids: {
  customerId: number;
  podId: number;
  contractId: number;
  billingRunId: number;
  productId: number;
}): Record<string, string> {
  const root = `${PHN_4377_PORTAL_BASE}/`;
  return {
    customer: `${root}customers/preview/basic?id=${ids.customerId}`,
    pod: `${root}points-of-delivery/preview?id=${ids.podId}`,
    productContract: `${root}energy-product-contracts/preview?id=${ids.contractId}`,
    billingRun: `${root}billing-run/preview/basic-parameters?id=${ids.billingRunId}&type=STANDARD_BILLING`,
    product: `${root}products/preview?id=${ids.productId}`,
  };
}

export async function createPhn4377BillingRun(
  ctx: Phn4377Fx,
  contractNumber: string,
  dates: Slr18167BillingDates,
): Promise<{ billingRunId: number; billingPayload: Record<string, unknown> }> {
  return test.step('Create STANDARD_BILLING FOR_VOLUMES with September invoice date', async () =>
    createSlr18167ForVolumesBillingRun(ctx, contractNumber, dates),
  );
}
