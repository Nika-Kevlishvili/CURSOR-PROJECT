/**
 * PDT-3206 helpers — contract document JSON top-level ProxyPrivate before Managers.
 *
 * Swagger (dev): ProxyEditRequest (camelCase create/update), ManagerProxyModel /
 * ContractDocumentModel (PascalCase inspect JSON). Product
 * GET /product-contract/{id}/document-json-test?versionId= (int32);
 * service twin versionId int64.
 *
 * Responses.product / Responses.service / Responses.priceComponent are numeric ids
 * so ContractsAndOrdersPayloads.product_contract / serviceContract can interpolate
 * them into GET paths (same pattern as PHN-362).
 *
 * Reference spec(s):
 * - tests/cursor/PDT-3179-reconnection-invoice-emails.spec.ts
 * - tests/cursor/PHN-2130-sales-portal-contract-update.spec.ts (buildProxyForCreate)
 * - tests/cursor/pdt-2599-service-contract.fixtures.ts
 * - jsons/payloadGenerators/domains/ContractsAndOrdersPayloads.ts
 */

import { expect } from '../../fixtures/baseFixture';
import type { baseFixture } from '../../fixtures/baseFixture';
import { randomGens } from '../../utils/randomGens';

export const PDT_3206_KEY = 'PDT-3206';
export const PDT_3206_TITLE = 'Changes in JSON for contract template';
export const PDT_3206_TEST_TIMEOUT_MS = 180_000;
export const PDT_3206_AA_TIMEOUT_MS = 300_000;

export const PDT_3206_EIGHT_KEYS = [
  'ProxyName',
  'ProxyNameTrsl',
  'PowerAttroneyNumber',
  'NotaryPublic',
  'NotaryPublicTrsl',
  'OperationArea',
  'OperationAreaTrsl',
  'RegistrationNumber',
] as const;

export type Pdt3206Fx = {
  Request: baseFixture['Request'];
  GeneratePayload: baseFixture['GeneratePayload'];
  Responses: baseFixture['Responses'];
  Endpoints: baseFixture['Endpoints'];
};

export type Pdt3206DocumentJson = {
  status: number;
  json: Record<string, unknown> | null;
  rawText: string;
};

export type Pdt3206ProxyOverrides = Record<string, unknown>;

function stripAddressCoords(addr: unknown): void {
  if (!addr || typeof addr !== 'object') return;
  const row = addr as Record<string, unknown>;
  delete row.latitude;
  delete row.longitude;
}

function stripCustomerCoords(payload: Record<string, unknown>): void {
  stripAddressCoords(payload.address);
  const comm = payload.communicationData;
  if (!Array.isArray(comm)) return;
  for (const entry of comm) {
    if (entry && typeof entry === 'object') {
      stripAddressCoords((entry as Record<string, unknown>).address);
    }
  }
}

const PDT_3206_CUSTOMER_IDENTIFIER_COLLISION_ATTEMPTS = 5;

function isPdt3206CustomerIdentifierCollision(status: number, body: string): boolean {
  return status === 400 && body.includes('already exists');
}

async function postPdt3206CustomerRetryingIdentifierCollision(
  fx: Pdt3206Fx,
  buildPayload: () => Record<string, unknown>,
) {
  const { Request, Endpoints } = fx;
  const maxAttempts = PDT_3206_CUSTOMER_IDENTIFIER_COLLISION_ATTEMPTS;
  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    const payload = buildPayload();
    const response = await Request.post(Endpoints.customer, { data: payload });
    if (response.ok()) {
      await expect(response).CheckResponse();
      return response;
    }
    const text = await response.text();
    if (isPdt3206CustomerIdentifierCollision(response.status(), text) && attempt < maxAttempts) {
      continue;
    }
    await expect(response).CheckResponse();
  }
  throw new Error('PDT-3206 customer POST: identifier collision retries exhausted');
}

export function asNumericId(stored: unknown, label: string): number {
  if (typeof stored === 'number' && Number.isFinite(stored)) return stored;
  if (stored !== null && typeof stored === 'object' && 'id' in stored) {
    const n = Number((stored as { id: unknown }).id);
    if (Number.isFinite(n)) return n;
  }
  throw new Error(`Could not resolve ${label} id from: ${JSON.stringify(stored)?.slice(0, 200)}`);
}

export function addDaysIso(isoDate: string, days: number): string {
  const d = new Date(`${isoDate}T12:00:00.000Z`);
  d.setUTCDate(d.getUTCDate() + days);
  return d.toISOString().slice(0, 10);
}

export function errorHaystack(status: number, text: string, json: unknown): string {
  return `${status} ${text} ${JSON.stringify(json ?? {})}`;
}

export function pdt3206ValidEgn(GeneratePayload: Pdt3206Fx['GeneratePayload']): string {
  return String(GeneratePayload.customers.customer_private().customerIdentifier);
}

export function pdt3206ValidUic(GeneratePayload: Pdt3206Fx['GeneratePayload']): string {
  return String(GeneratePayload.customers.customer_legal().customerIdentifier);
}

const AUTHORIZED_PROXY_OVERRIDE_KEYS = [
  'proxyAuthorizedByProxy',
  'authorizedProxyPowerOfAttorneyNumber',
  'authorizedProxyNotaryPublic',
  'authorizedProxyRegistrationNumber',
  'authorizedProxyAreaOfOperation',
  'authorizedProxyEmail',
  'authorizedProxyPhone',
  'authorizedProxyCustomerIdentifier',
  'authorizedProxyData',
  'authorizedProxyValidTill',
  'authorizedProxyForeignEntityPerson',
] as const;

function pdt3206ProxyPhone(): string {
  return `35988${randomGens.generateRandomString(false, true, 7)}`;
}

function hasAuthorizedProxyFields(src: Record<string, unknown>): boolean {
  return AUTHORIZED_PROXY_OVERRIDE_KEYS.some((key) => src[key] != null);
}

/**
 * Phoenix API ProxyEditRequest (camelCase). Not Sales Portal PUT schema.
 * Required: proxyForeignEntityPerson, proxyName, proxyCustomerIdentifier, proxyData,
 * notaryPublic, areaOfOperation. ProxyValidator also requires proxyEmail + proxyPhone.
 * Once any authorized-proxy field is present, runtime also requires a full set:
 * authorizedProxyForeignEntityPerson (never null — unboxing NPE → HV000028),
 * authorizedProxyCustomerIdentifier (valid EGN), authorizedProxyData (on or before today),
 * authorizedProxyEmail, authorizedProxyPhone.
 * Omit managerIds for PRIVATE_CUSTOMER.
 */
export function buildPdt3206Proxy(
  GeneratePayload: Pdt3206Fx['GeneratePayload'],
  overrides: Pdt3206ProxyOverrides = {},
): Record<string, unknown> {
  const proxy: Record<string, unknown> = {
    proxyForeignEntityPerson: false,
    proxyName: 'Ivan Petrov Proxy',
    proxyCustomerIdentifier: pdt3206ValidEgn(GeneratePayload),
    proxyPowerOfAttorneyNumber: 'POAMAIN001',
    proxyData: '2026-01-15',
    notaryPublic: 'Notary Main',
    registrationNumber: 'REGMAIN111',
    areaOfOperation: 'Sofia City',
    proxyEmail: randomGens.generateRandomEMail(),
    proxyPhone: pdt3206ProxyPhone(),
    ...overrides,
  };
  if (proxy.proxyEmail == null || proxy.proxyEmail === '') {
    proxy.proxyEmail = randomGens.generateRandomEMail();
  }
  if (proxy.proxyPhone == null || proxy.proxyPhone === '') {
    proxy.proxyPhone = pdt3206ProxyPhone();
  }
  if (hasAuthorizedProxyFields(overrides) || hasAuthorizedProxyFields(proxy)) {
    if (proxy.authorizedProxyForeignEntityPerson == null) {
      proxy.authorizedProxyForeignEntityPerson = false;
    }
    if (
      proxy.authorizedProxyCustomerIdentifier == null ||
      proxy.authorizedProxyCustomerIdentifier === ''
    ) {
      proxy.authorizedProxyCustomerIdentifier = pdt3206ValidEgn(GeneratePayload);
    }
    if (proxy.authorizedProxyData == null || proxy.authorizedProxyData === '') {
      proxy.authorizedProxyData = '2026-03-01';
    }
    if (proxy.authorizedProxyEmail == null || proxy.authorizedProxyEmail === '') {
      proxy.authorizedProxyEmail = randomGens.generateRandomEMail();
    }
    if (proxy.authorizedProxyPhone == null || proxy.authorizedProxyPhone === '') {
      proxy.authorizedProxyPhone = pdt3206ProxyPhone();
    }
  }
  if (!('managerIds' in overrides)) {
    delete proxy.managerIds;
  }
  return proxy;
}

export function toProxyEditRequest(src: Record<string, unknown>): Record<string, unknown> {
  const out: Record<string, unknown> = {
    proxyForeignEntityPerson: src.proxyForeignEntityPerson ?? false,
    proxyName: src.proxyName,
    proxyCustomerIdentifier: src.proxyCustomerIdentifier,
    proxyData: src.proxyData,
    notaryPublic: src.notaryPublic,
    areaOfOperation: src.areaOfOperation,
  };
  if (src.id != null) out.id = src.id;
  if (src.proxyPowerOfAttorneyNumber != null) {
    out.proxyPowerOfAttorneyNumber = src.proxyPowerOfAttorneyNumber;
  }
  if (src.registrationNumber != null) out.registrationNumber = src.registrationNumber;
  if (src.proxyEmail != null) out.proxyEmail = src.proxyEmail;
  if (src.proxyPhone != null) out.proxyPhone = src.proxyPhone;
  if (src.authorizedProxyEmail != null) out.authorizedProxyEmail = src.authorizedProxyEmail;
  if (src.authorizedProxyPhone != null) out.authorizedProxyPhone = src.authorizedProxyPhone;
  if (src.authorizedProxyForeignEntityPerson != null) {
    out.authorizedProxyForeignEntityPerson = src.authorizedProxyForeignEntityPerson;
  }
  if (src.authorizedProxyCustomerIdentifier != null) {
    out.authorizedProxyCustomerIdentifier = src.authorizedProxyCustomerIdentifier;
  }
  if (src.authorizedProxyData != null) out.authorizedProxyData = src.authorizedProxyData;
  if (src.authorizedProxyValidTill != null) {
    out.authorizedProxyValidTill = src.authorizedProxyValidTill;
  }
  if (src.proxyValidTill != null) out.proxyValidTill = src.proxyValidTill;
  if (src.proxyAuthorizedByProxy != null) out.proxyAuthorizedByProxy = src.proxyAuthorizedByProxy;
  if (src.authorizedProxyPowerOfAttorneyNumber != null) {
    out.authorizedProxyPowerOfAttorneyNumber = src.authorizedProxyPowerOfAttorneyNumber;
  }
  if (src.authorizedProxyNotaryPublic != null) {
    out.authorizedProxyNotaryPublic = src.authorizedProxyNotaryPublic;
  }
  if (src.authorizedProxyRegistrationNumber != null) {
    out.authorizedProxyRegistrationNumber = src.authorizedProxyRegistrationNumber;
  }
  if (src.authorizedProxyAreaOfOperation != null) {
    out.authorizedProxyAreaOfOperation = src.authorizedProxyAreaOfOperation;
  }
  const rawIds = src.managerIds;
  if (Array.isArray(rawIds) && rawIds.length) {
    const managerIds = rawIds
      .map((item) => {
        if (typeof item === 'number') return item;
        if (item && typeof item === 'object' && 'id' in item) return Number((item as { id: unknown }).id);
        return NaN;
      })
      .filter((n) => Number.isFinite(n));
    if (managerIds.length) out.managerIds = managerIds;
  }
  return out;
}

export function assertPdt3206ProxyPrivateBeforeManagers(rawText: string): void {
  const proxyIdx = rawText.indexOf('"ProxyPrivate"');
  const managersIdx = rawText.indexOf('"Managers"');
  expect(proxyIdx, 'JSON key ProxyPrivate must be present').toBeGreaterThanOrEqual(0);
  expect(managersIdx, 'JSON key Managers must be present').toBeGreaterThanOrEqual(0);
  expect(proxyIdx, 'ProxyPrivate must appear before Managers in raw JSON').toBeLessThan(managersIdx);
}

export function assertPdt3206EightKeys(obj: Record<string, unknown>): void {
  for (const key of PDT_3206_EIGHT_KEYS) {
    expect(obj, `ManagerProxyModel missing ${key}`).toHaveProperty(key);
  }
  expect(
    Object.prototype.hasOwnProperty.call(obj, 'PowerOfAttorneyNumber'),
    'JSON must not use PowerOfAttorneyNumber (correct spelling)',
  ).toBe(false);
}

export function proxyPrivateList(doc: Record<string, unknown> | null): Record<string, unknown>[] {
  const list = doc?.ProxyPrivate;
  return Array.isArray(list) ? (list as Record<string, unknown>[]) : [];
}

export function managersList(doc: Record<string, unknown> | null): Record<string, unknown>[] {
  const list = doc?.Managers;
  return Array.isArray(list) ? (list as Record<string, unknown>[]) : [];
}

export function findProxyNameInManagers(
  managers: Record<string, unknown>[],
  proxyName: string,
): boolean {
  for (const manager of managers) {
    const nested = manager.ProxyList;
    if (!Array.isArray(nested)) continue;
    for (const row of nested as Record<string, unknown>[]) {
      if (String(row.ProxyName ?? '') === proxyName) return true;
    }
  }
  return false;
}

async function postCatalogEntity(
  fx: Pdt3206Fx,
  path: string,
  data: unknown,
): Promise<unknown> {
  const { Request } = fx;
  const response = await Request.post(path, { data });
  await expect(response).CheckResponse();
  return response.json();
}

/** Term + electricity price + SUPPLY_ONLY product + POD. No customer. */
export async function setupPdt3206ProductBase(fx: Pdt3206Fx): Promise<void> {
  const { GeneratePayload, Responses, Endpoints } = fx;

  const termBody = await postCatalogEntity(fx, Endpoints.terms, GeneratePayload.productAndServices.term());
  Responses.terms.push(termBody);

  const priceBody = await postCatalogEntity(
    fx,
    Endpoints.priceComponent,
    GeneratePayload.productAndServices.electricity(),
  );
  Responses.priceComponent.push(asNumericId(priceBody, 'price component'));

  const productPayload = GeneratePayload.productAndServices.product();
  productPayload.contractTypes = ['SUPPLY_ONLY'];
  productPayload.paymentGuarantees = ['NO'];
  const productBody = await postCatalogEntity(fx, Endpoints.product, productPayload);
  Responses.product.push(asNumericId(productBody, 'product'));

  const podBody = await postCatalogEntity(
    fx,
    Endpoints.pod,
    GeneratePayload.pointsOfDelivery.pod_settlement(),
  );
  Responses.pod.push(podBody);
}

/**
 * Term + per-piece price (service-compatible; electricity AM is for product) + service + POD.
 * perPiece matches PDT-2599 so serviceContract() can set quantity from ranges.
 */
export async function setupPdt3206ServiceBase(fx: Pdt3206Fx): Promise<void> {
  const { GeneratePayload, Responses, Endpoints } = fx;

  const termBody = await postCatalogEntity(fx, Endpoints.terms, GeneratePayload.productAndServices.term());
  Responses.terms.push(termBody);

  const priceBody = await postCatalogEntity(
    fx,
    Endpoints.priceComponent,
    GeneratePayload.productAndServices.perPiece(),
  );
  Responses.priceComponent.push(asNumericId(priceBody, 'price component'));

  const serviceBody = await postCatalogEntity(
    fx,
    Endpoints.service,
    GeneratePayload.productAndServices.service(),
  );
  Responses.service.push(asNumericId(serviceBody, 'service'));

  const podBody = await postCatalogEntity(
    fx,
    Endpoints.pod,
    GeneratePayload.pointsOfDelivery.pod_settlement(),
  );
  Responses.pod.push(podBody);
}

async function hydrateCustomerFromGet(
  fx: Pdt3206Fx,
  customerId: number,
): Promise<Record<string, unknown>> {
  const { Request, Endpoints, Responses } = fx;
  const getRes = await Request.get(`${Endpoints.customer}/${customerId}`);
  await expect(getRes).CheckResponse();
  const view = (await getRes.json()) as Record<string, unknown>;
  const stored = Responses.customer[Responses.customer.length - 1] as Record<string, unknown>;
  stored.versionId = view.versionId ?? stored.versionId ?? 1;
  stored.managers = view.managers;
  stored.communicationData = view.communicationData;
  stored.customerType = view.customerType;
  stored.identifier = view.identifier ?? stored.identifier;
  return view;
}

export async function createPdt3206PrivateCustomer(fx: Pdt3206Fx): Promise<Record<string, unknown>> {
  const { GeneratePayload, Responses } = fx;
  const response = await postPdt3206CustomerRetryingIdentifierCollision(fx, () => {
    const payload = GeneratePayload.customers.customer_private() as Record<string, unknown>;
    stripCustomerCoords(payload);
    return payload;
  });
  const body = await response.json();
  Responses.customer.push(body);
  return hydrateCustomerFromGet(fx, asNumericId(body, 'customer'));
}

export async function createPdt3206LegalCustomer(
  fx: Pdt3206Fx,
  managerName = 'LEGALMGR',
  managerSurname = 'ONE',
): Promise<{ view: Record<string, unknown>; managerId: number }> {
  const { GeneratePayload, Responses } = fx;
  const response = await postPdt3206CustomerRetryingIdentifierCollision(fx, () => {
    const payload = GeneratePayload.customers.customer_legal() as Record<string, unknown>;
    stripCustomerCoords(payload);
    const managers = payload.managers as Record<string, unknown>[] | undefined;
    if (managers?.[0]) {
      managers[0].name = managerName;
      managers[0].surname = managerSurname;
      managers[0].jobPosition = 'CEO';
    }
    return payload;
  });
  const body = await response.json();
  Responses.customer.push(body);
  const view = await hydrateCustomerFromGet(fx, asNumericId(body, 'customer'));
  const mgr = ((view.managers as Record<string, unknown>[]) ?? [])[0];
  const managerId = Number(mgr?.id);
  expect(managerId, 'LEGAL_ENTITY customer must have managers[0].id').toBeGreaterThan(0);
  return { view, managerId };
}

export async function createPdt3206BaCustomer(
  fx: Pdt3206Fx,
): Promise<{ view: Record<string, unknown>; managerId: number }> {
  const { GeneratePayload, Responses } = fx;
  const response = await postPdt3206CustomerRetryingIdentifierCollision(fx, () => {
    const payload = GeneratePayload.customers.customer_private_business() as Record<string, unknown>;
    stripCustomerCoords(payload);
    const managers = payload.managers as Record<string, unknown>[] | undefined;
    if (managers?.[0]) {
      managers[0].name = 'BAMGR';
      managers[0].surname = 'ONE';
      managers[0].jobPosition = 'CEO';
    }
    return payload;
  });
  const body = await response.json();
  Responses.customer.push(body);
  const view = await hydrateCustomerFromGet(fx, asNumericId(body, 'customer'));
  expect(String(view.customerType)).toBe('PRIVATE_CUSTOMER');
  expect(view.businessActivity).toBe(true);
  const mgr = ((view.managers as Record<string, unknown>[]) ?? [])[0];
  const managerId = Number(mgr?.id);
  expect(managerId, 'BA-private customer must have managers[0].id').toBeGreaterThan(0);
  return { view, managerId };
}

function applyProductContractProxyAndType(
  payload: Record<string, unknown>,
  proxy: unknown[] | undefined,
  customerVersionId: unknown,
): void {
  const basic = payload.basicParameters as Record<string, unknown>;
  const productParameters = payload.productParameters as Record<string, unknown>;
  productParameters.contractType = 'SUPPLY_ONLY';
  productParameters.paymentGuarantee = 'NO';
  if (customerVersionId != null) basic.customerVersionId = customerVersionId;
  if (proxy !== undefined) basic.proxy = proxy;
  const additional = payload.additionalParameters as Record<string, unknown>;
  const estimated = additional?.estimatedTotalConsumptionUnderContractKwh;
  if (estimated == null || Number(estimated) === 0) {
    additional.estimatedTotalConsumptionUnderContractKwh = 100;
  }
}

export async function postPdt3206ProductContract(
  fx: Pdt3206Fx,
  opts: { proxy?: unknown[]; expectSuccess?: boolean } = {},
): Promise<{ status: number; text: string; json: unknown; id: number | null }> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const payload = (await GeneratePayload.contractsAndOrders.product_contract()) as Record<string, unknown>;
  applyProductContractProxyAndType(
    payload,
    opts.proxy,
    (Responses.customer[0] as { versionId?: number })?.versionId,
  );
  const response = await Request.post(Endpoints.productContract, { data: payload });
  const status = response.status();
  if (opts.expectSuccess === false) {
    const text = await response.text();
    let json: unknown = null;
    try {
      json = JSON.parse(text);
    } catch {
      json = null;
    }
    return { status, text, json, id: null };
  }
  await expect(response).CheckResponse();
  const json = await response.json();
  Responses.productContract.push(typeof json === 'number' ? { id: json } : json);
  return {
    status,
    text: JSON.stringify(json),
    json,
    id: asNumericId(json, 'product contract'),
  };
}

export async function postPdt3206ServiceContract(
  fx: Pdt3206Fx,
  opts: { proxy?: unknown[]; expectSuccess?: boolean } = {},
): Promise<{ status: number; text: string; json: unknown; id: number | null }> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const payload = (await GeneratePayload.contractsAndOrders.serviceContract()) as Record<string, unknown>;
  const basic = payload.basicParameters as Record<string, unknown>;
  const serviceParameters = payload.serviceParameters as Record<string, unknown>;
  serviceParameters.paymentGuarantee = 'NO';
  const versionId = (Responses.customer[0] as { versionId?: number })?.versionId;
  if (versionId != null) basic.customerVersionId = versionId;
  if (opts.proxy !== undefined) basic.proxy = opts.proxy;
  const response = await Request.post(Endpoints.serviceContract, { data: payload });
  const status = response.status();
  if (opts.expectSuccess === false) {
    const text = await response.text();
    let json: unknown = null;
    try {
      json = JSON.parse(text);
    } catch {
      json = null;
    }
    return { status, text, json, id: null };
  }
  await expect(response).CheckResponse();
  const json = await response.json();
  Responses.serviceContract.push(typeof json === 'number' ? { id: json } : json);
  return {
    status,
    text: JSON.stringify(json),
    json,
    id: asNumericId(typeof json === 'number' ? { id: json } : json, 'service contract'),
  };
}

export async function getPdt3206ProductContract(
  fx: Pdt3206Fx,
  contractId: number,
  versionId?: number,
): Promise<Record<string, unknown>> {
  const { Request, Endpoints } = fx;
  const url = `${Endpoints.productContract}/${contractId}`;
  const response =
    versionId == null
      ? await Request.get(url)
      : await Request.get(url, { params: { versionId } });
  await expect(response).CheckResponse();
  return (await response.json()) as Record<string, unknown>;
}

export async function getPdt3206ServiceContract(
  fx: Pdt3206Fx,
  contractId: number,
  versionId?: number,
): Promise<Record<string, unknown>> {
  const { Request, Endpoints } = fx;
  const url = `${Endpoints.serviceContract}/${contractId}`;
  const response =
    versionId == null
      ? await Request.get(url)
      : await Request.get(url, { params: { versionId } });
  await expect(response).CheckResponse();
  return (await response.json()) as Record<string, unknown>;
}

/**
 * PUT /product-contract/{id}?versionId=&changeFutureVersionsPods=
 * Body: ProductContractUpdateRequest (required podRequests, savingAsNewVersion, startDate,
 * basicParameters.type enum includes ADDITIONAL_AGREEMENT).
 * Built via edit_ProductContract then overrides. Keep generator employeeId 154 (Dev PUT requires it).
 */
export async function putPdt3206ProductContract(
  fx: Pdt3206Fx,
  opts: {
    versionId: number;
    savingAsNewVersion: boolean;
    changeFutureVersionsPods?: boolean;
    type?: string;
    startDate?: string;
    proxy?: unknown[];
  },
): Promise<{ status: number; json: unknown; text: string }> {
  const { Request, GeneratePayload, Endpoints } = fx;
  const generated = (await GeneratePayload.contractsAndOrders.product_contract()) as Record<
    string,
    unknown
  >;
  applyProductContractProxyAndType(
    generated,
    opts.proxy,
    (fx.Responses.customer[0] as { versionId?: number })?.versionId,
  );
  const editPayload = (await GeneratePayload.contractsAndOrders.edit_ProductContract(
    generated as never,
  )) as Record<string, unknown>;
  editPayload.savingAsNewVersion = opts.savingAsNewVersion;
  const basic = editPayload.basicParameters as Record<string, unknown>;
  if (opts.type) basic.type = opts.type;
  if (opts.startDate) editPayload.startDate = opts.startDate;
  if (opts.proxy !== undefined) basic.proxy = opts.proxy;
  const changeFuture = opts.changeFutureVersionsPods ?? false;
  const contractId = asNumericId(fx.Responses.productContract[0], 'product contract');
  const response = await Request.put(
    `${Endpoints.productContract}/${contractId}?versionId=${opts.versionId}&changeFutureVersionsPods=${changeFuture}`,
    { data: editPayload },
  );
  await expect(response).CheckResponse();
  const json = await response.json();
  return { status: response.status(), json, text: JSON.stringify(json) };
}

export async function getPdt3206DocumentJson(
  fx: Pdt3206Fx,
  kind: 'product' | 'service',
  id: number,
  versionId?: number,
): Promise<Pdt3206DocumentJson> {
  const { Request, Endpoints } = fx;
  const base = kind === 'product' ? Endpoints.productContract : Endpoints.serviceContract;
  const url = `${base}/${id}/document-json-test`;
  const response =
    versionId === undefined
      ? await Request.get(url)
      : await Request.get(url, { params: { versionId } });
  const status = response.status();
  const rawText = await response.text();
  let json: Record<string, unknown> | null = null;
  try {
    const parsed = JSON.parse(rawText) as unknown;
    json = parsed !== null && typeof parsed === 'object' ? (parsed as Record<string, unknown>) : null;
  } catch {
    json = null;
  }
  return { status, json, rawText };
}

/** Optional Word generate. Empty popup is not a failure (no template in env). */
export async function tryPdt3206Generate(
  fx: Pdt3206Fx,
  kind: 'product' | 'service',
  id: number,
  versionId: number,
): Promise<{ popupStatus: number; generateStatus?: number; skipped: boolean }> {
  const { Request, Endpoints } = fx;
  const base = kind === 'product' ? Endpoints.productContract : Endpoints.serviceContract;
  const popupRes = await Request.get(`${base}/${id}/generate-popup`, { params: { versionId } });
  const popupStatus = popupRes.status();
  const popupText = await popupRes.text();
  let popupJson: unknown = null;
  try {
    popupJson = JSON.parse(popupText);
  } catch {
    popupJson = null;
  }
  const rows = Array.isArray(popupJson)
    ? popupJson
    : Array.isArray((popupJson as { content?: unknown[] } | null)?.content)
      ? ((popupJson as { content: unknown[] }).content)
      : [];
  const first = rows[0] as { templateId?: number; outputFileFormat?: string[] } | undefined;
  if (!first?.templateId) {
    return { popupStatus, skipped: true };
  }
  const formats =
    Array.isArray(first.outputFileFormat) && first.outputFileFormat.includes('DOCX')
      ? ['DOCX']
      : first.outputFileFormat?.[0]
        ? [first.outputFileFormat[0]]
        : ['DOCX'];
  const genRes = await Request.post(`${base}/generate`, {
    data: {
      contractId: id,
      versionId,
      documents: [{ templateId: first.templateId, outputFileFormat: formats }],
    },
  });
  return { popupStatus, generateStatus: genRes.status(), skipped: false };
}

export function latestLogicalVersionId(contract: Record<string, unknown>): number {
  const versions = (contract.versions ?? []) as Array<{ versionId?: number }>;
  const ids = versions.map((v) => Number(v.versionId)).filter((n) => Number.isFinite(n));
  if (ids.length) return Math.max(...ids);
  const basic = (contract.basicParameters ?? {}) as { versionId?: number };
  if (basic.versionId != null) return Number(basic.versionId);
  return 1;
}

export function pdt3206RelevantKeys(kind: 'product' | 'service' | 'none'): string[] {
  if (kind === 'product') return ['customer', 'pod', 'product', 'productContract'];
  if (kind === 'service') return ['customer', 'pod', 'service', 'serviceContract'];
  return ['customer', 'pod'];
}
