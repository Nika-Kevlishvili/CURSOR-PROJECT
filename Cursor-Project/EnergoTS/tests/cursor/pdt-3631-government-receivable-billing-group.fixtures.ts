/**
 * PDT-3631 — government-recipient customer receivable billing group.
 *
 * Bug/standalone automation (no Backend TC .md).
 * Environment: dev.
 * Run with: $env:BASE_URL='https://devapps.energo-pro.bg/backend/phoenix1-dev'
 * Also accepted: http://10.236.20.11:8091 (selects DEVAUTHAPI in fixtures/login.ts).
 *
 * Assertion follows Confluence "Customer receivable automatic creation (Government Compensation)"
 * page id 1077608452: Billing group = billing group from the invoice.
 * The reporter empty-group claim is not the expected result.
 *
 * Reference spec(s):
 * - Cursor-Project/EnergoTS/tests/cursor/PDT-3368-invoice-cancellation-government-compensations.spec.ts
 * - Cursor-Project/EnergoTS/tests/cursor/pdt-3368-invoice-cancellation-government-compensations.fixtures.ts
 * - Cursor-Project/EnergoTS/tests/cursor/GC-REG-government-compensation-process.spec.ts
 * - Cursor-Project/EnergoTS/tests/cursor/gc-reg-government-compensation-regression.fixtures.ts
 *
 * swagger_refresh: update-swagger-specs.ps1 exit 0 this session.
 * Schemas from Cursor-Project/config/swagger/dev/swagger-spec.json:
 * - GET /product-contract/{id} ProductContractResponse.billingGroups BillingGroupListingResponse { id, groupNumber }
 * - GET /invoice InvoiceResponse.contractBillingGroup ShortResponse { id, name }
 * - GET /customer-receivable/{id} CustomerReceivableResponse.billingGroupResponse
 *   BillingGroupListingResponse { id, groupNumber }; currentAmount number
 * - POST /invoice-cancellation CreateInvoiceCancellationRequest required templateId;
 *   invoices, fileId, taxEventDate. InvoiceCancellationResponse: invoiceCancellationId, processId
 */

import { expect } from '../../fixtures/baseFixture';
import {
  BILLING_TIMEOUT_MS,
  accountGcsOnRealDebit,
  assertNegativeApplyAttachments,
  extractPartyCustomerId,
  getCompensation,
  getCustomerReceivable,
  getInvoice,
  postCancelByInvoiceNumber,
  resolveOpenTaxEventDate,
  runDevVolCompContractAndBbpPrechain,
  shortId,
  toAmountNumber,
  waitInvoiceCancelled,
  type Pdt3368Fx,
  type RealDebitWithGc,
} from './pdt-3368-invoice-cancellation-government-compensations.fixtures';
import { DEV_PORTAL_BASE, buildDevInvoicePreviewLink } from './gc-reg-government-compensation-regression.fixtures';

export const PDT_3631_KEY = 'PDT-3631';
export const PDT_3631_TITLE = 'Billing group appears in the government receivable';

export {
  BILLING_TIMEOUT_MS,
  DEV_PORTAL_BASE,
  accountGcsOnRealDebit,
  assertNegativeApplyAttachments,
  buildDevInvoicePreviewLink,
  extractPartyCustomerId,
  getCompensation,
  getCustomerReceivable,
  getInvoice,
  postCancelByInvoiceNumber,
  resolveOpenTaxEventDate,
  runDevVolCompContractAndBbpPrechain,
  shortId,
  toAmountNumber,
  waitInvoiceCancelled,
};

export type Pdt3631Fx = Pdt3368Fx;
export type { RealDebitWithGc };

export type BillingGroupSnapshot = {
  id: number | null;
  groupNumber: string | null;
};

export type ContractBillingGroup = {
  id: number;
  groupNumber: string | null;
};

export type InvoiceContractBillingGroup = {
  id: number;
  name: string | null;
};

export function formatBillingGroupId(id: number | null | undefined): string {
  if (id == null || !Number.isFinite(id) || id <= 0) {
    return 'null';
  }
  return String(id);
}

function positiveId(value: unknown): number | null {
  const n = Number(value);
  if (!Number.isFinite(n) || n <= 0) {
    return null;
  }
  return n;
}

function textOrNull(value: unknown): string | null {
  if (value == null) {
    return null;
  }
  const text = String(value).trim();
  return text === '' ? null : text;
}

/** GET /product-contract/{id}?version=1 — BillingGroupListingResponse on billingGroups[]. */
export async function readContractBillingGroup(
  Request: Pdt3631Fx['Request'],
  contractId: number,
): Promise<ContractBillingGroup> {
  const res = await Request.get(`product-contract/${contractId}?version=1`);
  await expect(res).CheckResponse();
  const body = (await res.json()) as {
    billingGroups?: Array<{ id?: unknown; groupNumber?: unknown }>;
  };
  const groups = body.billingGroups ?? [];
  expect(groups.length, `product contract ${contractId} billingGroups`).toBeGreaterThan(0);
  const id = positiveId(groups[0]?.id);
  expect(
    id,
    `product contract ${contractId} billingGroups[0].id, got ${JSON.stringify(groups[0])}`,
  ).toBeTruthy();
  return {
    id: id as number,
    groupNumber: textOrNull(groups[0]?.groupNumber),
  };
}

/** InvoiceResponse.contractBillingGroup is ShortResponse { id, name }, not contractBillingGroupId. */
export function readInvoiceContractBillingGroup(
  invoice: Record<string, unknown>,
): InvoiceContractBillingGroup {
  const nested = invoice.contractBillingGroup as { id?: unknown; name?: unknown } | null | undefined;
  const id = positiveId(nested?.id);
  expect(
    id,
    `invoice.contractBillingGroup.id must be set (ShortResponse). got ${JSON.stringify(nested)}`,
  ).toBeTruthy();
  return {
    id: id as number,
    name: textOrNull(nested?.name),
  };
}

/** CustomerReceivableResponse.billingGroupResponse is BillingGroupListingResponse { id, groupNumber }. */
export function readReceivableBillingGroup(receivable: Record<string, unknown>): BillingGroupSnapshot {
  const bg = receivable.billingGroupResponse;
  if (bg == null || typeof bg !== 'object') {
    return { id: null, groupNumber: null };
  }
  const row = bg as { id?: unknown; groupNumber?: unknown };
  return {
    id: positiveId(row.id),
    groupNumber: textOrNull(row.groupNumber),
  };
}

/**
 * Confluence 1077608452: government-institution receivable billing group equals the invoice billing group.
 * A null receivable group fails this check. The message names both ids.
 */
export function expectGovernmentReceivableBillingGroup(
  phase: string,
  receivableId: number,
  receivableBillingGroupId: number | null,
  invoiceId: number,
  invoiceContractBillingGroupId: number,
): void {
  const receivableText = formatBillingGroupId(receivableBillingGroupId);
  const invoiceText = formatBillingGroupId(invoiceContractBillingGroupId);
  expect.soft(
    receivableBillingGroupId,
    `${phase}: government receivable billingGroupResponse.id (${receivableText}) must equal invoice contractBillingGroup.id (${invoiceText}). receivableId=${receivableId} invoiceId=${invoiceId}`,
  ).toBe(invoiceContractBillingGroupId);
}

export function devPortalLinks(opts: {
  invoiceId: number;
  receivableId: number;
  governmentCustomerId: number;
  billedCustomerId: number;
}): Record<string, string[]> {
  const extra: Record<string, string[]> = {};
  if (opts.invoiceId > 0) {
    extra.invoice = [buildDevInvoicePreviewLink(opts.invoiceId)];
  }
  if (opts.receivableId > 0) {
    extra.customerReceivable = [
      `${DEV_PORTAL_BASE}/customer-receivables/preview?id=${opts.receivableId}`,
    ];
  }
  const customers: string[] = [];
  if (opts.governmentCustomerId > 0) {
    customers.push(`${DEV_PORTAL_BASE}/customers/preview/basic?id=${opts.governmentCustomerId}`);
  }
  if (opts.billedCustomerId > 0 && opts.billedCustomerId !== opts.governmentCustomerId) {
    customers.push(`${DEV_PORTAL_BASE}/customers/preview/basic?id=${opts.billedCustomerId}`);
  }
  if (customers.length > 0) {
    extra.customer = customers;
  }
  return extra;
}
