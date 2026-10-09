import * as fs from 'fs';
import * as path from 'path';
import { expect } from '@playwright/test';
import type { APIRequestContext } from '@playwright/test';
import { randomGens } from '../../utils/randomGens';
import { Endpoints } from '../../fixtures/constants/endpoints';
import { startBillingRun } from './pdt-2872-minimal-interim-payment.fixtures';
import {
  resolvePdt2376BillingAnchor,
  type Pdt2376BillingAnchor,
} from './pdt-2376-volume-with-electricity.fixtures';

/** Dev UI preview (ResponseLinker mapping for API base `http://10.236.20.11:8091`). */
export function collectionChannelPreviewUrl(channelId: number): string {
  return `http://10.236.20.11:8080/collection-channel/preview?id=${channelId}`;
}

export function bankCollectionChannelPreviewUrl(channelId: number): string {
  return collectionChannelPreviewUrl(channelId);
}

/** Dev Phoenix share root — `application-dev.properties` `app.cfg.sharedFolderPath`. */
export const PDT2187_DEV_COLLECTION_FOLDER_ROOT =
  process.env.PDT2187_COLLECTION_FOLDER_ROOT?.trim() || '/Phxsign_140/collection_channel_dev/';

function resolvePdt2187ExportDirs(): { exportDir: string; bankDir: string } {
  const custom = process.env.PDT2187_EXPORT_DIR?.trim();
  if (custom) {
    const exportDir = custom.replace(/\\/g, '/').replace(/\/?$/, '/');
    return { exportDir, bankDir: `${exportDir}bank/` };
  }
  const stamp = randomGens.generateCurrentTimeStamp();
  const exportDir = `${PDT2187_DEV_COLLECTION_FOLDER_ROOT.replace(/\/?$/, '/')}/pdt2187-automation-${stamp}/`;
  return { exportDir, bankDir: `${exportDir}bank/` };
}

/** Payment Partner fixed-width row length (User Story V 0.2). */
export const PAYMENT_PARTNER_ROW_LENGTH = 423;

export const TEST_EXPORT_LIABILITIES_JOB = `${Endpoints.collectionChannel}/test-export-liabilities-job`;

/** Expected cols/refs from Payment_Partner_Export_PDT_2187.md (User Story AC traceability). */
export const PDT2187_TC_EXPECT = {
  external: { prefix3: '000', number10: '0222357185', documentDate: '15.06.2025' },
  invoice: { prefix3: '250', number10: '0000123456', documentDate: '20.05.2025' },
  lpf: { prefix3: 'LPF', number10: '00001234', documentDate: '01.07.2025' },
  resShort: { prefix3: 'RES', number10: '0000001065' },
  resLong: { prefix3: 'RES', number10: '4567890123' },
  action: { prefix3: 'ACT', number10: '0000999888' },
  deposit: { prefix3: 'DEP', number10: '0000555666', documentDate: '01.10.2025' },
  bankExternalDate: '20250615',
  bankResShortRef: 'RES0000001065',
} as const;

export const PP_COL = {
  prefix: { start: 338, len: 3 },
  number: { start: 341, len: 10 },
  documentDate: { start: 351, len: 10 },
  dueDate: { start: 361, len: 10 },
  initialAmount: { start: 371, len: 16 },
  currentAmount: { start: 387, len: 16 },
  trailer: { start: 403, len: 16 },
} as const;

export type PaymentPartnerRowSlice = {
  prefix: string;
  number: string;
  documentDate: string;
  dueDate: string;
  initialAmount: string;
  currentAmount: string;
  trailer: string;
};

export type DocReferenceExpectation = {
  prefix3: string;
  number10: string;
  documentDate?: string;
};

export type Pdt2187CustomerRef = {
  id: number;
  identifier: string;
  customerNumber: string;
  body: Record<string, unknown>;
};

export type Pdt2187BankExport = {
  exportDir: string;
  exportFilePath: string;
  csvLines: string[];
};

export type Pdt2187Pack = {
  collectionChannelId: number;
  bankChannelId: number | null;
  exportDir: string;
  exportFilePath: string;
  lines: string[];
  customers: {
    main: Pdt2187CustomerRef;
    resShort: Pdt2187CustomerRef;
    resLong: Pdt2187CustomerRef;
    custA: Pdt2187CustomerRef;
    custB: Pdt2187CustomerRef;
  };
  liabilityIds: {
    mainExternal: number;
    mainNoExternal: number;
    custA: number;
    custB: number;
  };
  expected: {
    resShortReschedulingNum10: string;
    resLongReschedulingNum10: string;
    mainInvoice: DocReferenceExpectation;
    mainLpf: DocReferenceExpectation;
    mainAction: DocReferenceExpectation;
    mainDeposit: DocReferenceExpectation;
  };
  bank: Pdt2187BankExport | null;
};

type Fx = {
  Request: APIRequestContext;
  GeneratePayload: any;
  Responses: any;
  Endpoints: typeof Endpoints;
};

type PeriodRange = { startDate: string; endDate: string };

let cachedBillingAnchor: Pdt2376BillingAnchor | null = null;

async function billingAnchor(fx: Fx): Promise<Pdt2376BillingAnchor> {
  if (!cachedBillingAnchor) {
    cachedBillingAnchor = await resolvePdt2376BillingAnchor(fx.Request);
  }
  return cachedBillingAnchor;
}

function periodMonthOffset(periodEndYmd: string, monthsBack: number): PeriodRange {
  const y = Number(periodEndYmd.slice(0, 4));
  const m = Number(periodEndYmd.slice(5, 7));
  const ref = new Date(Date.UTC(y, m - 1, 1));
  ref.setUTCMonth(ref.getUTCMonth() - monthsBack);
  const end = new Date(Date.UTC(ref.getUTCFullYear(), ref.getUTCMonth() + 1, 0))
    .toISOString()
    .slice(0, 10);
  const start = `${end.slice(0, 7)}-01`;
  return { startDate: start, endDate: end };
}

export function sliceField(line: string, start1: number, len: number): string {
  return line.substring(start1 - 1, start1 - 1 + len);
}

export function parsePaymentPartnerRow(line: string): PaymentPartnerRowSlice {
  expect(line.length).toBe(PAYMENT_PARTNER_ROW_LENGTH);
  return {
    prefix: sliceField(line, PP_COL.prefix.start, PP_COL.prefix.len),
    number: sliceField(line, PP_COL.number.start, PP_COL.number.len),
    documentDate: sliceField(line, PP_COL.documentDate.start, PP_COL.documentDate.len).trim(),
    dueDate: sliceField(line, PP_COL.dueDate.start, PP_COL.dueDate.len).trim(),
    initialAmount: sliceField(line, PP_COL.initialAmount.start, PP_COL.initialAmount.len).trim(),
    currentAmount: sliceField(line, PP_COL.currentAmount.start, PP_COL.currentAmount.len).trim(),
    trailer: sliceField(line, PP_COL.trailer.start, PP_COL.trailer.len).trim(),
  };
}

export function rowCustomerNumber(line: string): string {
  return line.substring(0, 16).trim();
}

export function linesForCustomer(lines: string[], customerNumber: string): string[] {
  return lines.filter((l) => l.length === PAYMENT_PARTNER_ROW_LENGTH && rowCustomerNumber(l) === customerNumber);
}

export function combinedReference(slice: PaymentPartnerRowSlice): string {
  return `${slice.prefix.trim()}${slice.number.trim()}`;
}

export function splitDocumentNumberForExport(docNumber: string): { prefix3: string; number10: string } {
  const clean = docNumber.replace(/-/g, '');
  const prefix3 = clean.length > 3 ? clean.substring(0, 3) : clean.padEnd(3, ' ');
  const rest = clean.length > 3 ? clean.substring(3) : '';
  const digits = rest.replace(/\D/g, '');
  const number10 =
    digits.length > 10 ? digits.substring(digits.length - 10) : digits.padStart(10, '0');
  return { prefix3, number10 };
}

export function digitsLast10(value: string): string {
  const digits = value.replace(/\D/g, '');
  if (!digits) return '0000000000';
  return digits.length > 10 ? digits.substring(digits.length - 10) : digits.padStart(10, '0');
}

export function toPaymentPartnerDisplayDate(ddMmYyyy: string): string {
  const [d, m, y] = ddMmYyyy.split('-');
  return `${d}.${m}.${y}`;
}

export function findRowByRef(
  lines: string[],
  customerNumber: string,
  prefix3: string,
  number10: string,
): string | undefined {
  return linesForCustomer(lines, customerNumber).find((l) => {
    const s = parsePaymentPartnerRow(l);
    return s.prefix.trim() === prefix3.trim() && s.number.trim() === number10.trim();
  });
}

export function bankCsvField(line: string, index1: number): string {
  return (line.split(';')[index1 - 1] ?? '').trim();
}

async function withCustomerAtZero<T>(
  fx: Fx,
  customerBody: Record<string, unknown>,
  fn: () => Promise<T>,
): Promise<T> {
  const saved = {
    customer: [...fx.Responses.customer],
    customerLiability: [...fx.Responses.customerLiability],
    interestRate: [...fx.Responses.interestRate],
    customerAssessment: [...fx.Responses.customerAssessment],
    rescheduling: [...fx.Responses.rescheduling],
    pod: [...fx.Responses.pod],
    productContract: [...fx.Responses.productContract],
    billingRun: [...fx.Responses.billingRun],
    invoice: [...fx.Responses.invoice],
    dataByProfiles: [...fx.Responses.dataByProfiles],
    deposit: [...fx.Responses.deposit],
    penalty: [...fx.Responses.penalty],
    termination: [...fx.Responses.termination],
  };
  fx.Responses.customer = [customerBody];
  fx.Responses.customerLiability = [];
  fx.Responses.interestRate = [];
  fx.Responses.customerAssessment = [];
  fx.Responses.rescheduling = [];
  fx.Responses.pod = [];
  fx.Responses.productContract = [];
  fx.Responses.billingRun = [];
  fx.Responses.invoice = [];
  fx.Responses.dataByProfiles = [];
  fx.Responses.deposit = [];
  fx.Responses.penalty = [];
  fx.Responses.termination = [];
  try {
    return await fn();
  } finally {
    Object.assign(fx.Responses, saved);
  }
}

async function createPrivateCustomer(fx: Fx): Promise<Pdt2187CustomerRef> {
  const res = await fx.Request.post(fx.Endpoints.customer, {
    data: fx.GeneratePayload.customers.customer_private(),
  });
  await expect(res).CheckResponse();
  const body = await res.json();
  fx.Responses.customer.push(body);
  return {
    id: body.id,
    identifier: body.identifier,
    customerNumber: String(body.customerNumber),
    body,
  };
}

/** TC shared steps 2–4: terms → price component → product (catalog once per pack). */
async function createSharedCatalog(fx: Fx): Promise<void> {
  const termRes = await fx.Request.post(fx.Endpoints.terms, {
    data: fx.GeneratePayload.productAndServices.term(),
  });
  await expect(termRes).CheckResponse();
  fx.Responses.terms.push(await termRes.json());

  const pricePayload = fx.GeneratePayload.productAndServices.priceSettlement();
  pricePayload.formulaRequest.expression = '100';
  const priceRes = await fx.Request.post(fx.Endpoints.priceComponent, { data: pricePayload });
  await expect(priceRes).CheckResponse();
  fx.Responses.priceComponent.push(await priceRes.json());

  const penaltyRes = await fx.Request.post(fx.Endpoints.penalty, {
    data: fx.GeneratePayload.productAndServices.penalty(),
  });
  await expect(penaltyRes).CheckResponse();
  fx.Responses.penalty.push(await penaltyRes.json());

  const terminationRes = await fx.Request.post(fx.Endpoints.termination, {
    data: fx.GeneratePayload.productAndServices.termination('EXPIRATION_OF_THE_NOTICE'),
  });
  await expect(terminationRes).CheckResponse();
  fx.Responses.termination.push(await terminationRes.json());

  const productRes = await fx.Request.post(fx.Endpoints.product, {
    data: fx.GeneratePayload.productAndServices.product(),
  });
  await expect(productRes).CheckResponse();
  fx.Responses.product.push(await productRes.json());
}

type ContractBootstrapOpts = {
  status?: string;
  subStatus?: string;
  entryInForceDate?: string;
};

/** TC steps 6–8: POD + contract + activation + billing-by-profile for period. */
async function setupContractAndProfile(
  fx: Fx,
  period: PeriodRange,
  contractOpts?: ContractBootstrapOpts,
): Promise<void> {
  const podRes = await fx.Request.post(fx.Endpoints.pod, {
    data: fx.GeneratePayload.pointsOfDelivery.pod_settlement(),
  });
  await expect(podRes).CheckResponse();
  fx.Responses.pod.push(await podRes.json());

  const contractPayload = await fx.GeneratePayload.contractsAndOrders.product_contract(0, 0, 0);
  if (contractOpts?.status) contractPayload.basicParameters.status = contractOpts.status;
  if (contractOpts?.subStatus) contractPayload.basicParameters.subStatus = contractOpts.subStatus;
  if (contractOpts?.entryInForceDate) {
    contractPayload.basicParameters.entryInForceDate = contractOpts.entryInForceDate;
  }
  const contractRes = await fx.Request.post(fx.Endpoints.productContract, { data: contractPayload });
  await expect(contractRes).CheckResponse();
  fx.Responses.productContract.push(await contractRes.json());

  const activation = await fx.Request.post('/contract-pods/manual', {
    data: await fx.GeneratePayload.pointsOfDelivery.pod_activation(0),
  });
  await expect(activation).CheckResponse();

  const profilePayload = await fx.GeneratePayload.energyData.profile1Month(0, [
    { startDate: period.startDate, endDate: period.endDate },
  ]);
  profilePayload.timeZone = 'CET';
  const bbpRes = await fx.Request.post('billing-by-profile', { data: profilePayload });
  await expect(bbpRes).CheckResponse();
  fx.Responses.dataByProfiles.push(await bbpRes.json());
}

async function runForVolumesBillingAndRealize(
  fx: Fx,
  opts: { invoiceDate?: string; taxEventDate?: string },
): Promise<number> {
  const anchor = await billingAnchor(fx);
  const payload = await fx.GeneratePayload.billing.billingRun('CONTRACT', ['FOR_VOLUMES']);
  payload.commonParameters.accountingPeriodId = anchor.accountingPeriodId;
  payload.commonParameters.invoiceDate = opts.invoiceDate ?? anchor.invoiceDate;
  payload.commonParameters.taxEventDate = opts.taxEventDate ?? anchor.taxEventDate;
  const billingRes = await fx.Request.post(fx.Endpoints.billingRun, { data: payload });
  await expect(billingRes).CheckResponse();
  const billingRunId = await billingRes.json();
  fx.Responses.billingRun.push(billingRunId);
  await startBillingRun(fx.Request, billingRunId as number);
  await fx.GeneratePayload.billing.waitForInvoiceGeneration(true, true, 1, 0);
  const invoiceId = fx.Responses.invoice[fx.Responses.invoice.length - 1] as number;
  expect(invoiceId, 'invoice id after billing run').toBeTruthy();
  return invoiceId;
}

async function invoiceDocExpectation(fx: Fx, invoiceId: number): Promise<DocReferenceExpectation> {
  const invGet = await fx.Request.get(`${fx.Endpoints.invoice}?id=${invoiceId}`);
  await expect(invGet).CheckResponse();
  const inv = await invGet.json();
  const invoiceNumber = String(inv.invoiceNumber ?? '');
  expect(invoiceNumber.length, 'invoiceNumber from billing').toBeGreaterThan(0);
  const { prefix3, number10 } = splitDocumentNumberForExport(invoiceNumber);
  const rawDate = String(inv.invoiceDate ?? '').split('T')[0];
  let documentDate: string | undefined;
  if (/^\d{4}-\d{2}-\d{2}$/.test(rawDate)) {
    const [y, m, d] = rawDate.split('-');
    documentDate = `${d}.${m}.${y}`;
  }
  return { prefix3: prefix3.trim(), number10, documentDate };
}

async function createReschedulingOnFirstCustomer(fx: Fx): Promise<string> {
  const interestRes = await fx.Request.post(fx.Endpoints.interestRate, {
    data: { ...fx.GeneratePayload.receivablesManagement.dailyInterestRate(), type: 'YEARLY' },
  });
  await expect(interestRes).CheckResponse();
  fx.Responses.interestRate.push(await interestRes.json());

  const liabilityPayload = fx.GeneratePayload.receivablesManagement.customer_liability();
  liabilityPayload.initialAmount = 250;
  liabilityPayload.currentAmount = 250;
  liabilityPayload.occurrenceDate = randomGens.generateOneWeekBeforeDate('dd-mm-yyyy');
  liabilityPayload.dueDate = randomGens.generateYesterdaysDate('dd-mm-yyyy');
  liabilityPayload.applicableInterestRateId = fx.Responses.interestRate[fx.Responses.interestRate.length - 1];
  const liabilityRes = await fx.Request.post(fx.Endpoints.customerLiability, { data: liabilityPayload });
  await expect(liabilityRes).CheckResponse();
  fx.Responses.customerLiability.push(await liabilityRes.json());

  const assessmentRes = await fx.Request.post(fx.Endpoints.customerAssessment, {
    data: fx.GeneratePayload.receivablesManagement.customer_assessment(),
  });
  await expect(assessmentRes).CheckResponse();
  fx.Responses.customerAssessment.push(await assessmentRes.json());

  const reschedPayload = await fx.GeneratePayload.receivablesManagement.rescheduling(
    '3',
    'INTEREST_WITH_THE_FIRST_INSTALLMENT',
  );
  const reschedRes = await fx.Request.post(fx.Endpoints.rescheduling, { data: reschedPayload });
  await expect(reschedRes).CheckResponse();
  const reschedulingId = await reschedRes.json();
  fx.Responses.rescheduling.push(reschedulingId);
  const get = await fx.Request.get(`${fx.Endpoints.rescheduling}/${reschedulingId}`);
  await expect(get).CheckResponse();
  const body = await get.json();
  return digitsLast10(String(body.reschedulingNumber ?? body.number ?? reschedulingId));
}

async function ensurePaymentPackage(fx: Fx): Promise<void> {
  if ((fx.Responses.paymentPackage ?? []).length > 0) return;
  const res = await fx.Request.post(fx.Endpoints.paymentPackage, {
    data: fx.GeneratePayload.receivablesManagement.payment_package(),
  });
  await expect(res).CheckResponse();
  fx.Responses.paymentPackage.push(await res.json());
}

/**
 * REG-1049 pattern: past-due manual liability + interest → full payment (current_amount=0,
 * due_date < full_offset_date) → POST latePaymentFine/job. Invoice liability stays open for export.
 */
async function setupLpfViaPaidManualLiability(
  fx: Fx,
  lpfPrincipal = 100,
): Promise<DocReferenceExpectation> {
  const interestPayload = fx.GeneratePayload.receivablesManagement.interest_rate();
  interestPayload.type = 'DAILY';
  interestPayload.interestRatePeriods[0].amountInPercent = lpfPrincipal;
  interestPayload.interestRatePeriods[0].applicableInterestRate = 0.01;
  const interestRes = await fx.Request.post(fx.Endpoints.interestRate, { data: interestPayload });
  await expect(interestRes).CheckResponse();
  const interestId = await interestRes.json();
  fx.Responses.interestRate.push(interestId);

  const liabilityPayload = fx.GeneratePayload.receivablesManagement.customer_liability();
  liabilityPayload.initialAmount = lpfPrincipal;
  liabilityPayload.currentAmount = lpfPrincipal;
  liabilityPayload.occurrenceDate = randomGens.generateOneWeekBeforeDate('dd-mm-yyyy');
  liabilityPayload.dueDate = randomGens.generateOneWeekBeforeDate('dd-mm-yyyy');
  liabilityPayload.applicableInterestRateId = interestId;
  const liabRes = await fx.Request.post(fx.Endpoints.customerLiability, { data: liabilityPayload });
  await expect(liabRes).CheckResponse();
  const liabilityId = (await liabRes.json()) as number;
  fx.Responses.customerLiability.push(liabilityId);

  await ensurePaymentPackage(fx);
  const paymentPayload = await fx.GeneratePayload.receivablesManagement.payment();
  paymentPayload.initialAmount = lpfPrincipal;
  const paymentRes = await fx.Request.post(fx.Endpoints.payment, { data: paymentPayload });
  await expect(paymentRes).CheckResponse();
  fx.Responses.payment.push(await paymentRes.json());

  await expect
    .poll(
      async () => {
        const get = await fx.Request.get(`${fx.Endpoints.customerLiability}/${liabilityId}`);
        await expect(get).CheckResponse();
        const body = await get.json();
        return Number(body.currentAmount ?? -1);
      },
      { message: 'LPF source liability not fully offset before job', timeout: 90_000, intervals: [2000, 5000] },
    )
    .toBe(0);

  const dueChange = await fx.Request.put(
    `${fx.Endpoints.customerLiability}/${liabilityId}/due-date-change?dueDate=${randomGens.generateYesterdaysDate('yyyy-mm-dd')}`,
  );
  await expect(dueChange).CheckResponse();

  const lpfList = await fx.GeneratePayload.receivablesManagement.waitForLPFGeneration(false);
  const lpf = lpfList?.content?.[0] as Record<string, unknown> | undefined;
  expect(lpf, 'LPF after paid-late liability + job').toBeTruthy();
  const lpfNumber = String(lpf!.latePaymentNumber ?? lpf!.number ?? 'LPF-00001234');
  const { prefix3, number10 } = splitDocumentNumberForExport(lpfNumber);
  const rawLogical = String(lpf!.logicalDate ?? '').split('T')[0];
  let documentDate = PDT2187_TC_EXPECT.lpf.documentDate;
  if (/^\d{4}-\d{2}-\d{2}$/.test(rawLogical)) {
    const [y, m, d] = rawLogical.split('-');
    documentDate = `${d}.${m}.${y}`;
  }
  return { prefix3: prefix3.trim().slice(0, 3), number10, documentDate };
}

/** Invoice from billing (export) + separate paid-late liability for LPF job eligibility. */
async function setupMainInvoiceAndLpf(
  fx: Fx,
  main: Pdt2187CustomerRef,
): Promise<{ mainInvoice: DocReferenceExpectation; mainLpf: DocReferenceExpectation }> {
  return withCustomerAtZero(fx, main.body, async () => {
    const anchor = await billingAnchor(fx);
    await setupContractAndProfile(fx, periodMonthOffset(anchor.invoicePeriodTo, 0));
    const invoiceId = await runForVolumesBillingAndRealize(fx, {});
    const mainInvoice = await invoiceDocExpectation(fx, invoiceId);
    const mainLpf = await setupLpfViaPaidManualLiability(fx, 100);
    return { mainInvoice, mainLpf };
  });
}

async function setupMainAction(fx: Fx, main: Pdt2187CustomerRef): Promise<DocReferenceExpectation> {
  const catalogSnapshot = {
    penalty: [...fx.Responses.penalty],
    termination: [...fx.Responses.termination],
    product: [...fx.Responses.product],
    terms: [...fx.Responses.terms],
    priceComponent: [...fx.Responses.priceComponent],
  };
  return withCustomerAtZero(fx, main.body, async () => {
    Object.assign(fx.Responses, catalogSnapshot);
    const anchor = await billingAnchor(fx);
    await setupContractAndProfile(fx, periodMonthOffset(anchor.invoicePeriodTo, 0), {
      status: 'ENTERED_INTO_FORCE',
      subStatus: 'AWAITING_ACTIVATION',
      entryInForceDate: randomGens.generateTodaysDate('yyyy-mm-dd'),
    });
    const contractId = fx.Responses.productContract[0]?.id as number;
    expect(contractId, 'contract for action').toBeTruthy();
    const actionPayload = await fx.GeneratePayload.contractsAndOrders.action();
    actionPayload.customerId = main.id;
    actionPayload.contractId = contractId;
    actionPayload.contractType = 'PRODUCT_CONTRACT';
    const actionRes = await fx.Request.post(fx.Endpoints.action, { data: actionPayload });
    await expect(actionRes).CheckResponse();
    const actionRaw = await actionRes.json();
    const actionId =
      typeof actionRaw === 'number'
        ? actionRaw
        : Number(
            (actionRaw as { id?: number }).id ?? (actionRaw as { actionId?: number }).actionId,
          );
    expect(Number.isFinite(actionId), 'POST /actions must return numeric action id').toBeTruthy();
    const get = await fx.Request.get(`${fx.Endpoints.action}/${actionId}`);
    await expect(get).CheckResponse();
    const body = await get.json();
    const actionNumber = String(body.actionNumber ?? '');
    expect(actionNumber.length).toBeGreaterThan(0);
    const split = splitDocumentNumberForExport(actionNumber);
    return { prefix3: split.prefix3.trim(), number10: split.number10 };
  });
}

async function setupMainDeposit(fx: Fx, main: Pdt2187CustomerRef): Promise<DocReferenceExpectation> {
  const payload = fx.GeneratePayload.receivablesManagement.deposit();
  payload.customerId = main.id;
  payload.initialAmount = 120;
  payload.paymentDeadline = '01-10-2025';
  const res = await fx.Request.post(fx.Endpoints.deposit, { data: payload });
  await expect(res).CheckResponse();
  const depositId = await res.json();
  const get = await fx.Request.post(`${fx.Endpoints.deposit}/job`);
  await expect(get).CheckResponse();
  const depGet = await fx.Request.get(`${fx.Endpoints.deposit}/${depositId}`);
  await expect(depGet).CheckResponse();
  const body = await depGet.json();
  const depositNumber = String(body.depositNumber ?? body.number ?? 'DEP-0000555666');
  const split = splitDocumentNumberForExport(depositNumber);
  return {
    prefix3: split.prefix3.trim().slice(0, 3),
    number10: split.number10,
    documentDate: PDT2187_TC_EXPECT.deposit.documentDate,
  };
}

async function setupResShortWithBillingAndRescheduling(fx: Fx, customer: Pdt2187CustomerRef): Promise<string> {
  return withCustomerAtZero(fx, customer.body, async () => {
    const anchor = await billingAnchor(fx);
    await setupContractAndProfile(fx, periodMonthOffset(anchor.invoicePeriodTo, 0));
    await runForVolumesBillingAndRealize(fx, {});
    return createReschedulingOnFirstCustomer(fx);
  });
}

async function setupResLongWithBillingAndRescheduling(fx: Fx, customer: Pdt2187CustomerRef): Promise<string> {
  return withCustomerAtZero(fx, customer.body, async () => {
    const anchor = await billingAnchor(fx);
    await setupContractAndProfile(fx, periodMonthOffset(anchor.invoicePeriodTo, 0));
    await runForVolumesBillingAndRealize(fx, {});
    return createReschedulingOnFirstCustomer(fx);
  });
}

async function postManualLiability(
  fx: Fx,
  customerId: number,
  opts: {
    outgoingDocumentFromExternalSystem?: string;
    occurrenceDate: string;
    dueDate: string;
    initialAmount?: number;
    directDebit?: boolean;
    bankId?: number;
  },
): Promise<number> {
  const payload: Record<string, unknown> = {
    ...fx.GeneratePayload.receivablesManagement.customer_liability(),
    customerId,
    occurrenceDate: opts.occurrenceDate,
    dueDate: opts.dueDate,
    initialAmount: opts.initialAmount ?? 100.5,
    currentAmount: opts.initialAmount ?? 100.5,
  };
  if (opts.directDebit != null) payload.directDebit = opts.directDebit;
  if (opts.bankId != null) payload.bankId = opts.bankId;
  if (opts.outgoingDocumentFromExternalSystem) {
    payload.outgoingDocumentFromExternalSystem = opts.outgoingDocumentFromExternalSystem;
  }
  const res = await fx.Request.post(fx.Endpoints.customerLiability, { data: payload });
  await expect(res).CheckResponse();
  const id = await res.json();
  fx.Responses.customerLiability.push(id);
  return id as number;
}

async function createCollectionChannel(
  fx: Fx,
  listIdentifiers: string[],
  exportDir: string,
  typeOfFile: 'PAYMENT_PARTNER' | 'BANK_PARTNER',
): Promise<number> {
  const payload = fx.GeneratePayload.receivablesManagement.collection_channel();
  payload.customerConditionType = 'LIST_OF_CUSTOMERS';
  payload.listOfCustomers = listIdentifiers.join(',');
  payload.typeOfFile = typeOfFile;
  payload.type = 'OFFLINE';
  payload.folderForFileSending = exportDir;
  payload.dataSendingSchedule = 'FREQ=DAILY;BYHOUR=0;BYMINUTE=0';
  if (typeOfFile === 'BANK_PARTNER') {
    payload.globalBank = true;
    payload.bankIds = [];
  }
  const res = await fx.Request.post(fx.Endpoints.collectionChannel, { data: payload });
  await expect(res).CheckResponse();
  const id = await res.json();
  fx.Responses.collectionChannel.push(id);
  return id as number;
}

function readNewestFile(dir: string, ext: string): { filePath: string; lines: string[] } {
  if (!fs.existsSync(dir)) {
    throw new Error(`Export directory missing: ${dir}. Set PDT2187_EXPORT_DIR.`);
  }
  const files = fs
    .readdirSync(dir)
    .filter((f) => f.toLowerCase().endsWith(ext))
    .map((f) => ({ f, mtime: fs.statSync(path.join(dir, f)).mtimeMs }))
    .sort((a, b) => b.mtime - a.mtime);
  if (!files.length) throw new Error(`No ${ext} files in ${dir}`);
  const filePath = path.join(dir, files[0].f);
  const raw = fs.readFileSync(filePath, 'utf8');
  return {
    filePath,
    lines: raw.split(/\r?\n/).map((l) => l.replace(/\r$/, '')).filter((l) => l.length > 0),
  };
}

async function triggerExportJob(fx: Fx): Promise<void> {
  const exportRes = await fx.Request.post(TEST_EXPORT_LIABILITIES_JOB);
  await expect(exportRes).CheckResponse();
  await new Promise((r) => setTimeout(r, 5000));
}

function assertRequiredExportRows(pack: Pdt2187Pack): void {
  const { lines } = pack;
  const cn = pack.customers;
  const missing: string[] = [];
  if (!findRowByRef(lines, cn.main.customerNumber, PDT2187_TC_EXPECT.external.prefix3, PDT2187_TC_EXPECT.external.number10)) {
    missing.push('TC-BE-1 external');
  }
  if (!findRowByRef(lines, cn.custA.customerNumber, PDT2187_TC_EXPECT.external.prefix3, PDT2187_TC_EXPECT.external.number10)) {
    missing.push('TC-BE-9 CUST-A');
  }
  if (linesForCustomer(lines, cn.custB.customerNumber).length > 0) missing.push('TC-BE-9 CUST-B must be absent');
  if (
    !findRowByRef(lines, cn.resShort.customerNumber, PDT2187_TC_EXPECT.resShort.prefix3, pack.expected.resShortReschedulingNum10)
  ) {
    missing.push('TC-BE-3 RES-SHORT');
  }
  if (
    !findRowByRef(lines, cn.resLong.customerNumber, PDT2187_TC_EXPECT.resLong.prefix3, pack.expected.resLongReschedulingNum10)
  ) {
    missing.push('TC-BE-7 RES-LONG');
  }
  if (
    !findRowByRef(lines, cn.main.customerNumber, pack.expected.mainInvoice.prefix3, pack.expected.mainInvoice.number10)
  ) {
    missing.push('TC-BE-4 invoice');
  }
  if (!findRowByRef(lines, cn.main.customerNumber, pack.expected.mainLpf.prefix3, pack.expected.mainLpf.number10)) {
    missing.push('TC-BE-2 LPF');
  }
  if (!findRowByRef(lines, cn.main.customerNumber, pack.expected.mainAction.prefix3, pack.expected.mainAction.number10)) {
    missing.push('TC-BE-8 action');
  }
  if (!findRowByRef(lines, cn.main.customerNumber, pack.expected.mainDeposit.prefix3, pack.expected.mainDeposit.number10)) {
    missing.push('TC-BE-10 deposit');
  }
  if (missing.length) {
    throw new Error(`[PDT-2187] Export missing required rows: ${missing.join('; ')}`);
  }
}

/**
 * Full shared pack — TC steps A–F + bank B1, single export job.
 */
export async function buildPdt2187SharedPack(fx: Fx): Promise<Pdt2187Pack> {
  cachedBillingAnchor = null;
  const { exportDir, bankDir } = resolvePdt2187ExportDirs();
  // folderForFileSending must be a Phoenix Dev share path, not Windows %TEMP% (see PDT2187_DEV_COLLECTION_FOLDER_ROOT).
  if (process.env.PDT2187_EXPORT_DIR?.trim()) {
    fs.mkdirSync(exportDir, { recursive: true });
    fs.mkdirSync(bankDir, { recursive: true });
  }

  await createSharedCatalog(fx);

  const main = await createPrivateCustomer(fx);
  const resShort = await createPrivateCustomer(fx);
  const resLong = await createPrivateCustomer(fx);
  const custA = await createPrivateCustomer(fx);
  const custB = await createPrivateCustomer(fx);

  const channelList = [main.identifier, resShort.identifier, resLong.identifier, custA.identifier];
  const channelId = await createCollectionChannel(fx, channelList, exportDir, 'PAYMENT_PARTNER');

  const { mainInvoice, mainLpf } = await setupMainInvoiceAndLpf(fx, main);
  const mainAction = await setupMainAction(fx, main);
  const mainDeposit = await setupMainDeposit(fx, main);
  const resShortReschedulingNum10 = await setupResShortWithBillingAndRescheduling(fx, resShort);
  const resLongReschedulingNum10 = await setupResLongWithBillingAndRescheduling(fx, resLong);

  const bankChannelId = await createCollectionChannel(
    fx,
    [main.identifier, resShort.identifier],
    bankDir,
    'BANK_PARTNER',
  );

  const mainExternal = await postManualLiability(fx, main.id, {
    outgoingDocumentFromExternalSystem: '222357185',
    occurrenceDate: '15-06-2025',
    dueDate: randomGens.generateTodaysDate('dd-mm-yyyy'),
    directDebit: true,
    bankId: 1000,
  });
  const mainNoExternal = await postManualLiability(fx, main.id, {
    occurrenceDate: '01-08-2025',
    dueDate: randomGens.generateTodaysDate('dd-mm-yyyy'),
    initialAmount: 50.25,
  });
  const liabilityCustA = await postManualLiability(fx, custA.id, {
    outgoingDocumentFromExternalSystem: '222357185',
    occurrenceDate: '01-09-2025',
    dueDate: randomGens.generateTodaysDate('dd-mm-yyyy'),
  });
  const liabilityCustB = await postManualLiability(fx, custB.id, {
    outgoingDocumentFromExternalSystem: '111222333',
    occurrenceDate: '02-09-2025',
    dueDate: randomGens.generateTodaysDate('dd-mm-yyyy'),
  });

  await triggerExportJob(fx);

  const { filePath, lines: rawLines } = readNewestFile(exportDir, '.txt');
  const lines = rawLines.filter((l) => l.length >= PAYMENT_PARTNER_ROW_LENGTH);

  const csv = readNewestFile(bankDir, '.csv');
  const bank: Pdt2187BankExport = {
    exportDir: bankDir,
    exportFilePath: csv.filePath,
    csvLines: csv.lines,
  };

  const pack: Pdt2187Pack = {
    collectionChannelId: channelId,
    bankChannelId,
    exportDir,
    exportFilePath: filePath,
    lines,
    customers: { main, resShort, resLong, custA, custB },
    liabilityIds: {
      mainExternal,
      mainNoExternal,
      custA: liabilityCustA,
      custB: liabilityCustB,
    },
    expected: {
      resShortReschedulingNum10,
      resLongReschedulingNum10,
      mainInvoice,
      mainLpf,
      mainAction,
      mainDeposit,
    },
    bank,
  };

  assertRequiredExportRows(pack);
  return pack;
}
