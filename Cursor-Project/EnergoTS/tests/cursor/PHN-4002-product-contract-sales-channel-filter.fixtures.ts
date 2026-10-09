/**
 * PHN-4002 — Product contract listing sales-channel filter must use contract Employee
 * (product_contract.contract_details.employee_id → account_managers + account_manager_tags
 * + nomenclature.sales_channels.portal_tag_id), not customer.customer_account_managers.
 *
 * POST /product-contract ignores request employeeId when a session exists
 * (processEmployeeOnCreate stamps the logged-in AM). Intended Employee is set via
 * PUT /product-contract/employee-update/{id}?versionId=&employeeId=.
 *
 * Reference spec(s):
 * - tests/contractsAndOrders/productContract.spec.ts
 * - tests/cursor/pdt-3059-product-contract-new-version-individual-product.fixtures.ts
 */

import { expect } from '../../fixtures/baseFixture';
import type { baseFixture } from '../../fixtures/baseFixture';
import { envVariables } from '../../fixtures/envCashed';
import { randomGens } from '../../utils/randomGens';

export const PHN_4002_JIRA_KEY = 'PHN-4002';

// Dev2 evidence only — never assigned unless the same ids appear in API discovery:
// tagged AM 1000 (George Kikriashvili), untagged AM 154, sales channel 1017.

export type Phn4002Fx = {
  Request: baseFixture['Request'];
  GeneratePayload: baseFixture['GeneratePayload'];
  Responses: baseFixture['Responses'];
  Endpoints: baseFixture['Endpoints'];
};

export type Phn4002SalesChannel = {
  id: number;
  name?: string;
  portalTagId: number;
};

export type Phn4002AccountManager = {
  id: number;
  name?: string;
  userName?: string;
  displayName?: string;
  portalTagIds: number[];
};

export type Phn4002Discovery = {
  source: 'manager-tags-api';
  taggedChannel: Phn4002SalesChannel;
  taggedEmployee: Phn4002AccountManager;
  untaggedEmployee: Phn4002AccountManager;
  accountManagerTypeId: number;
  channelsWithPortalTag: Phn4002SalesChannel[];
};

export type Phn4002CreatedContract = {
  id: number;
  contractNumber: string;
  employeeId: number | null;
  customerId: number;
  customerAccountManagerId: number | null;
};

type PagedBody<T> = {
  content?: T[];
  last?: boolean;
  totalPages?: number;
  number?: number;
};

function asRecord(value: unknown): Record<string, unknown> {
  return value && typeof value === 'object' ? (value as Record<string, unknown>) : {};
}

export function uniquePhn4002Key(prefix: string): string {
  return `${prefix}${randomGens.generateUniqueIdentifier()}`;
}

export function accountManagerTypeIdFromEnv(): number {
  const raw = Number(envVariables.account_manager_types);
  if (!Number.isFinite(raw) || raw <= 0) {
    throw new Error('envVariables.account_manager_types is missing — run EnergoTS setup first.');
  }
  return raw;
}

export function normalizeIdList(raw: unknown): number[] {
  if (Array.isArray(raw)) {
    return raw.map((item) => Number(item)).filter((id) => Number.isFinite(id) && id > 0);
  }
  if (typeof raw === 'string') {
    return raw
      .replace(/[{}]/g, '')
      .split(',')
      .map((part) => Number(part.trim()))
      .filter((id) => Number.isFinite(id) && id > 0);
  }
  const single = Number(raw);
  return Number.isFinite(single) && single > 0 ? [single] : [];
}

export function extractEntityId(body: unknown): number {
  if (typeof body === 'number' && Number.isFinite(body) && body > 0) {
    return body;
  }
  const record = asRecord(body);
  const id = Number(record.id);
  if (!Number.isFinite(id) || id <= 0) {
    throw new Error(`Response did not include a numeric id: ${JSON.stringify(body).slice(0, 300)}`);
  }
  return id;
}

export function readContractNumber(detail: unknown, fallback = ''): string {
  const record = asRecord(detail);
  const basic = asRecord(record.basicParameters);
  const number = String(record.contractNumber ?? basic.contractNumber ?? fallback).trim();
  return number;
}

export function readPersistedEmployeeId(detail: unknown): number | null | undefined {
  const additional = asRecord(asRecord(detail).additionalParameters);
  if (!Object.prototype.hasOwnProperty.call(additional, 'employeeId')) {
    return undefined;
  }
  const raw = additional.employeeId;
  if (raw == null || raw === '') {
    return null;
  }
  const id = Number(raw);
  return Number.isFinite(id) && id > 0 ? id : null;
}

export function readLatestVersionId(detail: unknown): number {
  const record = asRecord(detail);
  const versions = Array.isArray(record.versions) ? record.versions : [];
  const maxFromVersions = versions.reduce((max, version) => {
    const id = Number(asRecord(version).versionId ?? asRecord(version).id);
    return Number.isFinite(id) && id > max ? id : max;
  }, 0);
  if (maxFromVersions > 0) {
    return maxFromVersions;
  }
  const basic = Number(asRecord(record.basicParameters).versionId);
  if (Number.isFinite(basic) && basic > 0) {
    return basic;
  }
  return 1;
}

export function listingRowMatches(
  rows: Array<{ id?: number; contractNumber?: string }> | undefined,
  contractId: number,
  contractNumber: string,
): { id?: number; contractNumber?: string } | undefined {
  const targetNumber = contractNumber.trim();
  return (
    rows?.find((row) => Number(row.id) === contractId) ??
    rows?.find((row) => String(row.contractNumber ?? '').trim() === targetNumber) ??
    rows?.find((row) => String(row.contractNumber ?? '').includes(targetNumber))
  );
}

export async function assertListingHttpSuccess(response: any): Promise<number> {
  await expect(response).CheckResponse();
  const status = response.status();
  expect(
    response.ok() || status === 206,
    `Listing/filter GET-or-POST must be HTTP 2xx including 206 PARTIAL_CONTENT, got ${status}`,
  ).toBeTruthy();
  return status;
}

async function getPaged<T>(
  Request: Phn4002Fx['Request'],
  path: string,
  params: Record<string, string | number | boolean>,
  maxPages = 15,
): Promise<T[]> {
  const all: T[] = [];
  for (let page = 0; page < maxPages; page += 1) {
    const response = await Request.get(path, { params: { ...params, page } });
    await assertListingHttpSuccess(response);
    const body = (await response.json()) as PagedBody<T>;
    const content = Array.isArray(body.content) ? body.content : [];
    all.push(...content);
    const totalPages = Number(body.totalPages ?? 1);
    if (body.last === true || page + 1 >= totalPages || content.length === 0) {
      break;
    }
  }
  return all;
}

export async function fetchActiveSalesChannelsWithPortalTags(
  Request: Phn4002Fx['Request'],
): Promise<Phn4002SalesChannel[]> {
  const rows = await getPaged<Record<string, unknown>>(Request, 'sales-channels', {
    statuses: 'ACTIVE',
    size: 200,
  });
  return rows
    .map((row) => {
      const portalTag = asRecord(row.portalTagResponse);
      const portalTagId = Number(portalTag.id);
      const id = Number(row.id);
      if (!Number.isFinite(id) || id <= 0 || !Number.isFinite(portalTagId) || portalTagId <= 0) {
        return null;
      }
      return {
        id,
        name: String(row.name ?? row.displayName ?? ''),
        portalTagId,
      } satisfies Phn4002SalesChannel;
    })
    .filter((row): row is Phn4002SalesChannel => row != null);
}

export async function fetchActiveAccountManagers(
  Request: Phn4002Fx['Request'],
): Promise<Array<{ id: number; name?: string; userName?: string; displayName?: string }>> {
  const rows = await getPaged<Record<string, unknown>>(Request, 'account-managers', {
    statuses: 'ACTIVE',
    size: 200,
  });
  return rows
    .map((row) => {
      const id = Number(row.id);
      if (!Number.isFinite(id) || id <= 0) return null;
      return {
        id,
        name: String(row.name ?? ''),
        userName: String(row.userName ?? ''),
        displayName: String(row.displayName ?? ''),
      };
    })
    .filter((row): row is { id: number; name?: string; userName?: string; displayName?: string } => row != null);
}

export async function fetchManagerPortalTags(
  Request: Phn4002Fx['Request'],
): Promise<Map<number, number[]>> {
  const rows = await getPaged<Record<string, unknown>>(Request, 'manager-tags/all', {
    withGroupPrefix: false,
    size: 200,
  });
  const byManagerId = new Map<number, number[]>();
  for (const row of rows) {
    if (String(row.performerType ?? '') !== 'MANAGER') continue;
    const id = Number(row.id);
    if (!Number.isFinite(id) || id <= 0) continue;
    byManagerId.set(id, normalizeIdList(row.portalTagIds));
  }
  return byManagerId;
}

export async function discoverSalesChannelEmployees(Request: Phn4002Fx['Request']): Promise<Phn4002Discovery> {
  const accountManagerTypeId = accountManagerTypeIdFromEnv();
  const channelsWithPortalTag = await fetchActiveSalesChannelsWithPortalTags(Request);
  const activeManagers = await fetchActiveAccountManagers(Request);
  const tagMap = await fetchManagerPortalTags(Request);

  const channelByPortalTag = new Map<number, Phn4002SalesChannel>();
  for (const channel of channelsWithPortalTag) {
    channelByPortalTag.set(channel.portalTagId, channel);
  }

  const enriched: Phn4002AccountManager[] = activeManagers.map((manager) => ({
    ...manager,
    portalTagIds: tagMap.get(manager.id) ?? [],
  }));

  const taggedEmployee = enriched.find((manager) =>
    manager.portalTagIds.some((tagId) => channelByPortalTag.has(tagId)),
  );
  const taggedChannel = taggedEmployee
    ? taggedEmployee.portalTagIds.map((tagId) => channelByPortalTag.get(tagId)).find((channel) => channel != null)
    : undefined;
  const untaggedEmployee = enriched.find(
    (manager) =>
      manager.id !== taggedEmployee?.id &&
      !manager.portalTagIds.some((tagId) => tagId === taggedChannel?.portalTagId),
  );

  if (!taggedEmployee || !taggedChannel || !untaggedEmployee) {
    throw new Error(
      '[PHN-4002] Fail-fast discovery: GET /sales-channels + GET /account-managers + GET /manager-tags/all ' +
        'did not yield a tagged employee, an untagged employee, and a sales channel with portalTagResponse. ' +
        `channelsWithPortalTag=${channelsWithPortalTag.length}, activeManagers=${activeManagers.length}, ` +
        `managersWithTags=${[...tagMap.values()].filter((ids) => ids.length > 0).length}, ` +
        `taggedEmployeeId=${taggedEmployee?.id ?? 'missing'}, taggedChannelId=${taggedChannel?.id ?? 'missing'}, ` +
        `untaggedEmployeeId=${untaggedEmployee?.id ?? 'missing'}. ` +
        'Portal sales-channel tags cannot be created via Phoenix API — fix env tag data, then re-run.',
    );
  }

  return {
    source: 'manager-tags-api',
    taggedChannel,
    taggedEmployee,
    untaggedEmployee,
    accountManagerTypeId,
    channelsWithPortalTag,
  };
}

export async function createSharedCatalog(fx: Phn4002Fx): Promise<{ productId: number; productPayload: unknown }> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;

  const termResponse = await Request.post(Endpoints.terms, { data: GeneratePayload.productAndServices.term() });
  await expect(termResponse).CheckResponse();
  Responses.terms.push(await termResponse.json());

  const priceResponse = await Request.post(Endpoints.priceComponent, {
    data: GeneratePayload.productAndServices.priceSettlement(),
  });
  await expect(priceResponse).CheckResponse();
  Responses.priceComponent.push(await priceResponse.json());

  const productPayload = GeneratePayload.productAndServices.product();
  const productResponse = await Request.post(Endpoints.product, { data: productPayload });
  await expect(productResponse).CheckResponse();
  const productBody = await productResponse.json();
  const productId = extractEntityId(productBody);
  Responses.product.push(productId);

  return { productId, productPayload };
}

export async function createPod(fx: Phn4002Fx): Promise<Record<string, unknown>> {
  const payload = fx.GeneratePayload.pointsOfDelivery.pod_settlement();
  const response = await fx.Request.post(fx.Endpoints.pod, { data: payload });
  await expect(response).CheckResponse();
  const json = (await response.json()) as Record<string, unknown>;
  fx.Responses.pod.push(json);
  return json;
}

export async function createCustomerWithAccountManager(
  fx: Phn4002Fx,
  accountManagerId: number,
  accountManagerTypeId: number,
): Promise<{ payload: Record<string, unknown>; customer: Record<string, unknown> }> {
  const payload = fx.GeneratePayload.customers.customer_legal() as Record<string, unknown>;
  payload.accountManagers = [
    {
      accountManagerId,
      accountManagerTypeId,
    },
  ];
  const response = await fx.Request.post(fx.Endpoints.customer, { data: payload });
  await expect(response).CheckResponse();
  const customer = (await response.json()) as Record<string, unknown>;
  fx.Responses.customer.push(customer);

  const customerId = extractEntityId(customer);
  const getResponse = await fx.Request.get(`customer/${customerId}?version=1`);
  await expect(getResponse).CheckResponse();
  const detail = (await getResponse.json()) as Record<string, unknown>;
  const assigned = Array.isArray(detail.accountManagers) ? detail.accountManagers : [];
  const assignedIds = assigned
    .map((row) => Number(asRecord(row).accountManagerId ?? asRecord(row).id))
    .filter((id) => Number.isFinite(id) && id > 0);
  if (assigned.length > 0) {
    expect(
      assignedIds.includes(accountManagerId),
      `GET customer ${customerId} accountManagers should include ${accountManagerId}, got ${assignedIds.join(',')}`,
    ).toBeTruthy();
  }

  return { payload, customer };
}

export async function getProductContractDetail(
  fx: Phn4002Fx,
  contractId: number,
  versionId?: number,
): Promise<Record<string, unknown>> {
  const params = versionId != null ? { versionId } : { versionId: 1 };
  const getResponse = await fx.Request.get(`${fx.Endpoints.productContract}/${contractId}`, { params });
  await expect(getResponse).CheckResponse();
  return (await getResponse.json()) as Record<string, unknown>;
}

export async function updateProductContractEmployee(
  fx: Phn4002Fx,
  contractId: number,
  versionId: number,
  employeeId: number,
): Promise<void> {
  const putResponse = await fx.Request.put(
    `${fx.Endpoints.productContract}/employee-update/${contractId}`,
    { params: { versionId, employeeId } },
  );
  await expect(putResponse).CheckResponse();
}

export async function createProductContractWithEmployee(
  fx: Phn4002Fx,
  opts: {
    customerIndex: number;
    podIndex: number;
    employeeId: number | null;
    customerAccountManagerId: number | null;
    uniqueKey: string;
  },
): Promise<Phn4002CreatedContract> {
  const payload = await fx.GeneratePayload.contractsAndOrders.product_contract(
    opts.customerIndex,
    0,
    opts.podIndex,
  );
  const customerId = extractEntityId(fx.Responses.customer[opts.customerIndex]);
  payload.basicParameters.customerId = customerId;
  payload.basicParameters.contractNumber = `PHN4002-${opts.uniqueKey}`;
  payload.additionalParameters.employeeId = opts.employeeId;
  // dealNumber Swagger pattern ^[0-9]*$ — leave generator null (letter-prefixed uniqueKey is invalid).
  // POST create ignores request employeeId when a session exists (processEmployeeOnCreate stamps logged-in AM).

  let response = await fx.Request.post(fx.Endpoints.productContract, { data: payload });
  if (!response.ok() && response.status() === 400) {
    delete (payload.basicParameters as { contractNumber?: string }).contractNumber;
    response = await fx.Request.post(fx.Endpoints.productContract, { data: payload });
  }
  await expect(response).CheckResponse();
  const json = (await response.json()) as Record<string, unknown>;
  fx.Responses.productContract.push(json);
  const id = extractEntityId(json);

  let detail = await getProductContractDetail(fx, id);
  const versionId = readLatestVersionId(detail);
  if (versionId !== 1) {
    detail = await getProductContractDetail(fx, id, versionId);
  }
  const contractNumber = readContractNumber(detail, String(payload.basicParameters.contractNumber ?? ''));
  if (!contractNumber) {
    throw new Error(`GET product-contract/${id} did not return contractNumber`);
  }

  const intendedEmployeeId = opts.employeeId;
  let persistedEmployeeId = readPersistedEmployeeId(detail);

  if (
    intendedEmployeeId != null &&
    Number.isFinite(intendedEmployeeId) &&
    intendedEmployeeId > 0 &&
    persistedEmployeeId !== intendedEmployeeId
  ) {
    await updateProductContractEmployee(fx, id, versionId, intendedEmployeeId);
    detail = await getProductContractDetail(fx, id, versionId);
    persistedEmployeeId = readPersistedEmployeeId(detail);
  }

  if (
    intendedEmployeeId != null &&
    Number.isFinite(intendedEmployeeId) &&
    intendedEmployeeId > 0 &&
    persistedEmployeeId !== intendedEmployeeId
  ) {
    throw new Error(
      `[PHN-4002] Fail-fast: product-contract ${id} employeeId is ${String(persistedEmployeeId)} after ` +
        `PUT product-contract/employee-update/${id}?versionId=${versionId}&employeeId=${intendedEmployeeId}. ` +
        'Create stamps the logged-in AM; update must persist the intended Employee.',
    );
  }

  return {
    id,
    contractNumber,
    employeeId: persistedEmployeeId === undefined ? null : persistedEmployeeId,
    customerId,
    customerAccountManagerId: opts.customerAccountManagerId,
  };
}

export async function listProductContracts(
  Request: Phn4002Fx['Request'],
  opts: {
    contractNumber: string;
    salesChannelIds?: number[];
    size?: number;
  },
): Promise<{
  status: number;
  content: Array<{ id?: number; contractNumber?: string }>;
  match?: { id?: number; contractNumber?: string };
}> {
  const data: Record<string, unknown> = {
    page: 0,
    size: opts.size ?? 20,
    prompt: opts.contractNumber.trim(),
    searchBy: 'CONTRACT_NUMBER',
    exactMatch: true,
  };
  if (opts.salesChannelIds != null) {
    data.salesChannelIds = opts.salesChannelIds;
  }

  const response = await Request.post('product-contract/list', { data });
  const status = await assertListingHttpSuccess(response);
  const body = (await response.json()) as PagedBody<{ id?: number; contractNumber?: string }>;
  const content = Array.isArray(body.content) ? body.content : [];
  return {
    status,
    content,
    match: listingRowMatches(content, -1, opts.contractNumber),
  };
}

export async function listProductContractById(
  Request: Phn4002Fx['Request'],
  contract: Phn4002CreatedContract,
  salesChannelIds?: number[],
): Promise<{
  status: number;
  content: Array<{ id?: number; contractNumber?: string }>;
  found: boolean;
  match?: { id?: number; contractNumber?: string };
}> {
  const listed = await listProductContracts(Request, {
    contractNumber: contract.contractNumber,
    salesChannelIds,
  });
  const match = listingRowMatches(listed.content, contract.id, contract.contractNumber);
  return {
    status: listed.status,
    content: listed.content,
    found: Boolean(match),
    match,
  };
}
