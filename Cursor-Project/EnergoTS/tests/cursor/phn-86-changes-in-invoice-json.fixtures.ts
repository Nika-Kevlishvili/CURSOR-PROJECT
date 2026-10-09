/**
 * PHN-86 — Changes in invoice JSON.
 *
 * MostUsedPaymentChannel is reporting.customer_indicators.payment_channel_name
 * for the invoice customer (GET /customer-indicators/{id} → paymentChannelName).
 * Preferences are the ACTIVE preference names on the invoice customer version
 * (GET /customer/{id}?version=1 → customerPreferences[].preferences.name).
 *
 * A newly created customer has no payment-channel indicator, so the billed
 * customer is an existing Dev2 customer that already has both. The test still
 * creates the invoice DOCUMENT template, product, contract, POD, profile, and
 * billing run.
 *
 * Reference spec(s):
 * - tests/cursor/dev-volume-invoice-custom-template.fixtures.ts
 *   (upload DOCX + POST /template + product.INVOICE_TEMPLATE + FOR_VOLUMES)
 * - tests/cursor/rps-pod-invoice-due-date.fixtures.ts (startAndCompleteBillingRun)
 * - tests/cursor/phn-3951-cancelled-interim-invoice-price-component.fixtures.ts
 *   (GET /invoice/download-document + PDF text)
 *
 * Swagger (dev2, refreshed):
 *   POST /template/upload-template-file?fileFormats=DOCX
 *   POST /template — TemplateCreateRequest
 *   GET  /customer/{id}?version=
 *   GET  /customer-indicators/{id} — CustomerIndicatorsResponse.paymentChannelName
 *   GET  /billing-run/generate-invoice-data?invoiceId= — BillingRunDocumentModelImpl
 *        MostUsedPaymentChannel (string), Preferences (string[])
 *   GET  /invoice/download-document?invoiceDocumentId=
 *   ProductCreateRequest.templateIds templateType = INVOICE_TEMPLATE
 */

import * as fs from 'fs';
import * as path from 'path';
import { expect, test } from '../../fixtures/baseFixture';
import type { baseFixture } from '../../fixtures/baseFixture';
import { envVariables } from '../../fixtures/envCashed';
import { postPdt2915BillingByProfile } from './pdt-2915-invoice-correction-deleted-bbp.fixtures';
import {
  downloadInvoiceDocumentBuffer,
  extractPdfText,
  pollInvoiceFileRefs,
  savePdfToTestResults,
} from './phn-3951-cancelled-interim-invoice-price-component.fixtures';
import { listInvoices, startAndCompleteBillingRun } from './rps-pod-invoice-due-date.fixtures';
import {
  monthStartFromPeriod,
  uploadAndCreateCustomInvoiceDocumentTemplate,
} from './dev-volume-invoice-custom-template.fixtures';

export const PHN_86_KEY = 'PHN-86';
export const PHN_86_TITLE = 'Changes in invoice JSON';

export const PHN_86_DEV2_PORTAL_BASE = 'https://devapps.energo-pro.bg/app/phoenix-dev2';

/**
 * Dev2 customers whose version 1 already has ACTIVE preferences, a communication
 * record, and reporting.customer_indicators.payment_channel_name (verified 2026-10-08).
 * The test re-reads both APIs and uses the first candidate that still qualifies.
 */
export const PHN_86_DEV2_CUSTOMER_CANDIDATES = [6000037, 6000075, 6000909, 6001436] as const;

export const PHN_86_TEMPLATE_DOCX_PATH = path.resolve(
  __dirname,
  'assets/phn-86/invoice-most-used-channel-preferences.docx',
);

export type Phn86Fx = Pick<
  baseFixture,
  'Request' | 'FileUploadRequest' | 'GeneratePayload' | 'Responses' | 'Endpoints'
>;

export type Phn86ExistingCustomer = {
  customerId: number;
  customerNumber: string;
  identifier: string;
  paymentChannelName: string;
  preferenceNames: string[];
};

export type Phn86Prechain = Phn86ExistingCustomer & {
  podId: number;
  productId: number;
  contractId: number;
  bbpId: number;
  periodFrom: string;
  periodTo: string;
  documentPeriod: string;
  invoiceTemplateId: number;
  invoiceTemplateName: string;
  uploadedFileId: number;
};

type CustomerPreferenceRow = {
  status?: string;
  preferences?: { name?: string | null };
};

type CustomerVersionBody = {
  customerId?: number;
  customerNumber?: string | number | null;
  identifier?: string | null;
  customerPreferences?: CustomerPreferenceRow[];
  communicationData?: Array<{ id?: number }>;
};

type CustomerIndicatorsBody = {
  paymentChannelName?: string | null;
};

export type Phn86InvoiceDocumentModel = {
  MostUsedPaymentChannel?: string | null;
  Preferences?: string[] | null;
  CustomerNumber?: string | null;
};

function asEntityId(raw: unknown): number {
  if (typeof raw === 'number' && Number.isFinite(raw) && raw > 0) {
    return raw;
  }
  if (raw && typeof raw === 'object' && 'id' in raw) {
    const n = Number((raw as { id: unknown }).id);
    if (Number.isFinite(n) && n > 0) {
      return n;
    }
  }
  throw new Error(`Expected numeric entity id, got: ${JSON.stringify(raw)}`);
}

export function assertPhn86RunsOnDev2(): void {
  const base = (process.env.BASE_URL || 'https://devapps.energo-pro.bg/backend/phoenix2-dev').trim();
  const isDev2 =
    base.includes('/backend/phoenix2-dev') ||
    base.includes('/app/phoenix-dev2') ||
    base.startsWith('http://10.236.20.31:8091');
  expect(
    isDev2,
    `PHN-86 must run against Dev2 (phoenix2-dev). BASE_URL=${base}`,
  ).toBe(true);
}

function activePreferenceNames(body: CustomerVersionBody): string[] {
  const names = (body.customerPreferences ?? [])
    .filter((row) => row.status === 'ACTIVE')
    .map((row) => (row.preferences?.name ?? '').trim())
    .filter((name) => name.length > 0);
  return Array.from(new Set(names));
}

/**
 * First Dev2 candidate that still has version-1 ACTIVE preferences, a
 * communication id (required by product_contract), and a non-blank
 * paymentChannelName.
 */
export async function resolvePhn86ExistingCustomer(
  Request: Phn86Fx['Request'],
): Promise<Phn86ExistingCustomer> {
  const skipped: string[] = [];

  for (const customerId of PHN_86_DEV2_CUSTOMER_CANDIDATES) {
    const customerRes = await Request.get(`customer/${customerId}?version=1`);
    if (customerRes.status() !== 200) {
      skipped.push(`${customerId}: GET customer HTTP ${customerRes.status()}`);
      continue;
    }
    const customer = (await customerRes.json()) as CustomerVersionBody;
    const preferenceNames = activePreferenceNames(customer);
    const communicationId = Number(customer.communicationData?.[0]?.id ?? 0);
    if (preferenceNames.length === 0 || !Number.isFinite(communicationId) || communicationId <= 0) {
      skipped.push(
        `${customerId}: preferences=${preferenceNames.length}, communicationId=${communicationId}`,
      );
      continue;
    }

    const indicatorsRes = await Request.get(`customer-indicators/${customerId}`);
    if (indicatorsRes.status() !== 200) {
      skipped.push(`${customerId}: GET customer-indicators HTTP ${indicatorsRes.status()}`);
      continue;
    }
    const indicators = (await indicatorsRes.json()) as CustomerIndicatorsBody;
    const paymentChannelName = (indicators.paymentChannelName ?? '').trim();
    if (!paymentChannelName) {
      skipped.push(`${customerId}: paymentChannelName empty`);
      continue;
    }

    return {
      customerId,
      customerNumber: String(customer.customerNumber ?? ''),
      identifier: String(customer.identifier ?? ''),
      paymentChannelName,
      preferenceNames,
    };
  }

  throw new Error(
    `No Dev2 candidate still has version-1 preferences, communication, and paymentChannelName. ${skipped.join(' | ')}`,
  );
}

export async function runPhn86VolumePrechain(
  fx: Phn86Fx,
  customer: Phn86ExistingCustomer,
  invoiceTemplateId: number,
): Promise<Omit<Phn86Prechain, keyof Phn86ExistingCustomer | 'invoiceTemplateId' | 'invoiceTemplateName' | 'uploadedFileId'>> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;

  Responses.customer.push({
    id: customer.customerId,
    identifier: customer.identifier,
    customerNumber: customer.customerNumber,
  });

  await test.step('Precondition: price component (priceSettlement, 15-min profile)', async () => {
    const payload = GeneratePayload.productAndServices.priceSettlement();
    payload.formulaRequest.expression = '100';
    payload.applicationModelRequest.settlementPeriodsRequest.profiles = [
      { profileId: envVariables.profiles, percentage: 100 },
    ];
    const price = await Request.post(Endpoints.priceComponent, { data: payload });
    await expect(price).CheckResponse();
    Responses.priceComponent.push(await price.json());
  });

  await test.step('Precondition: terms', async () => {
    const term = await Request.post(Endpoints.terms, {
      data: GeneratePayload.productAndServices.term(),
    });
    await expect(term).CheckResponse();
    Responses.terms.push(await term.json());
  });

  await test.step('Precondition: POD settlement', async () => {
    const pod = await Request.post(Endpoints.pod, {
      data: GeneratePayload.pointsOfDelivery.pod_settlement(),
    });
    await expect(pod).CheckResponse();
    Responses.pod.push(await pod.json());
  });

  let productId = 0;
  await test.step('Precondition: product with PHN-86 INVOICE_TEMPLATE', async () => {
    const productPayload = GeneratePayload.productAndServices.product();
    productPayload.templateIds = [
      {
        templateId: envVariables.invoice_email_template,
        templateType: 'EMAIL_TEMPLATE',
      },
      {
        templateId: invoiceTemplateId,
        templateType: 'INVOICE_TEMPLATE',
      },
      {
        templateId: envVariables.product_contract_template,
        templateType: 'CONTRACT_TEMPLATE',
      },
    ];
    const product = await Request.post(Endpoints.product, { data: productPayload });
    await expect(product).CheckResponse();
    const body = await product.json();
    Responses.product.push(body);
    productId = asEntityId(body);
  });

  let contractId = 0;
  await test.step('Precondition: product contract for the existing Dev2 customer', async () => {
    const contract = await Request.post(Endpoints.productContract, {
      data: await GeneratePayload.contractsAndOrders.product_contract(),
    });
    await expect(contract).CheckResponse();
    const body = await contract.json();
    Responses.productContract.push(body);
    contractId = asEntityId(body);
  });

  await test.step('Precondition: activate POD on contract', async () => {
    const podActivation = await Request.post('/contract-pods/manual', {
      data: await GeneratePayload.pointsOfDelivery.pod_activation(0),
    });
    await expect(podActivation).CheckResponse();
  });

  let bbpId = 0;
  let periodFrom = '';
  let periodTo = '';
  await test.step('Precondition: billing-by-profile (15-minute + CET + MIN upload)', async () => {
    const payload = await GeneratePayload.energyData.profile15minute(0);
    payload.timeZone = 'CET';
    payload.profileId = envVariables.profiles;
    const bbp = await postPdt2915BillingByProfile(Request, payload, 'PHN-86 BBP');
    await GeneratePayload.energyData.uploadDataByProfileFile(bbp.id, 'MIN');
    bbpId = bbp.id;
    periodFrom = String(payload.periodFrom);
    periodTo = String(payload.periodTo);
    Responses.dataByProfiles.push({
      id: bbp.id,
      periodFrom,
      periodTo,
      periodType: payload.periodType,
    });
  });

  return {
    podId: asEntityId(Responses.pod[0]),
    productId,
    contractId,
    bbpId,
    periodFrom,
    periodTo,
    documentPeriod: monthStartFromPeriod(periodFrom),
  };
}

export async function createPhn86InvoiceTemplate(fx: Phn86Fx): Promise<{
  templateId: number;
  templateName: string;
  fileId: number;
}> {
  expect(fs.existsSync(PHN_86_TEMPLATE_DOCX_PATH), `PHN-86 DOCX missing at ${PHN_86_TEMPLATE_DOCX_PATH}`).toBe(
    true,
  );
  return uploadAndCreateCustomInvoiceDocumentTemplate(fx, PHN_86_TEMPLATE_DOCX_PATH);
}

export async function completePhn86BillingRun(
  fx: Phn86Fx,
): Promise<{ billingRunId: number; invoiceId: number }> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const billingPayload = await GeneratePayload.billing.billingRun('CONTRACT', ['FOR_VOLUMES']);
  const billingRun = await Request.post(Endpoints.billingRun, { data: billingPayload });
  await expect(billingRun).CheckResponse();
  const billingRaw = await billingRun.json();
  const billingRunId = asEntityId(billingRaw);
  Responses.billingRun.push(
    typeof billingRaw === 'object' && billingRaw !== null ? billingRaw : { id: billingRunId },
  );

  await startAndCompleteBillingRun(Request, billingRunId);

  const invoices = await listInvoices(Request, billingRunId);
  expect(invoices.length, 'FOR_VOLUMES billing must produce at least one invoice').toBeGreaterThanOrEqual(1);
  const invoiceId = Number(invoices[0].id);
  expect(invoiceId, 'invoice listing id').toBeGreaterThan(0);
  Responses.invoice.push(invoiceId);
  return { billingRunId, invoiceId };
}

export async function fetchPhn86InvoiceDocumentModel(
  Request: Phn86Fx['Request'],
  invoiceId: number,
): Promise<Phn86InvoiceDocumentModel> {
  const res = await Request.get(`billing-run/generate-invoice-data?invoiceId=${invoiceId}`);
  await expect(res).CheckResponse();
  return (await res.json()) as Phn86InvoiceDocumentModel;
}

export function sameStringSet(actual: string[], expected: string[]): boolean {
  const normalize = (values: string[]) =>
    [...values].map((value) => value.trim()).filter((value) => value.length > 0).sort();
  const left = normalize(actual);
  const right = normalize(expected);
  return left.length === right.length && left.every((value, index) => value === right[index]);
}

export async function downloadPhn86InvoicePdf(
  Request: Phn86Fx['Request'],
  invoiceId: number,
): Promise<{ pdfPath: string; pdfText: string; extractor: string; invoiceDocumentId: number }> {
  const { files } = await pollInvoiceFileRefs(Request, invoiceId);
  expect(files.length, `invoice ${invoiceId} must have a generated document file`).toBeGreaterThan(0);
  const invoiceDocumentId = files[0].id;
  const buffer = await downloadInvoiceDocumentBuffer(Request, invoiceDocumentId);
  const pdfPath = savePdfToTestResults(buffer, `PHN-86-invoice-${invoiceId}.pdf`);
  const extracted = extractPdfText(pdfPath);
  return { pdfPath, pdfText: extracted.text, extractor: extracted.extractor, invoiceDocumentId };
}
