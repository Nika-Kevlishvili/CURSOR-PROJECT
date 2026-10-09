/**
 * PDT-3409 helpers — DRAFT reconnection POD table (unpaid executed-RFD POD must stay visible).
 *
 * Two PODs, same grid operator:
 * - Unpaid target: one PDT-3179 chain (runPdt3179ReceivableChain + createExecutedRfd +
 *   createExecutedDps). Those helpers correctly use Responses[0]. Snapshot ids, then
 *   do NOT put this POD in POST table.
 * - Dummy POD: created from scratch in this file. createSupplyChain / createExecutedRfd /
 *   createExecutedDps / customerIdentifier always bind to [0], so a second 3179 chain
 *   would attach to the unpaid customer. Dummy supply uses last indices (at(-1)) and
 *   overrides generator fields that default to customer[0].
 *
 * POST DRAFT: GET /table by dummy CUSTOMER_IDENTIFIER only, map that row, saveAs DRAFT.
 * CREATE requires a non-empty table; dummy satisfies that without saving the unpaid POD.
 * Swagger ReconnectionPodRequest does not require cancellationReasonId — omit unless POST 400.
 *
 * Phase 1 (wiki 75923727): unpaid executed-RFD POD stays visible and unchecked on
 * table/view-temporary. Runtime may drop unchecked rows (isChecked = reconnection_pod_id
 * is not null). Missing unpaid row is a FAIL, not success.
 *
 * Origin/dev listing SQL (cite in P1–P4 checks):
 * - CREATE GET /table (tableByGridOperator): date gate = last EXECUTED reconnection_date
 *   < last EXECUTED disconnection_date. No disconnected=true. No hide-unpaid AND.
 * - DRAFT GET /table/view (viewTable): date gate = last EXECUTED reconnection_date
 *   < last RFD execution_date. Hide: reconnection_pod_id is not null OR liability_amount = 0.
 * - DRAFT GET /table/view-temporary (viewTableTemporary): same execution_date gate,
 *   no hide AND (Sep 4 origin/dev mismatch vs view).
 * - `disconnected` is not in reconnection listing SQL. PointOfDeliveryResponse swagger
 *   has no `disconnected` property; GET /pod still records the runtime field.
 *
 * Swagger (dev, refreshed this session — update-swagger-specs.ps1 all envs OK):
 * - POST /reconnection-of-the-power-supply ReconnectionOfThePowerSupplyBaseRequest
 *   required: gridOperatorId, saveAs enum DRAFT | EXECUTED | SAVE
 *   table[] items = ReconnectionPodRequest: customerId, podId,
 *   requestForDisconnectionOfPowerSupplyId, cancellationReasonId (not required)
 * - PUT /reconnection-of-the-power-supply?reconnectionId= (update_7)
 *   ReconnectionOfThePowerSupplyEditRequest required: gridOperatorId, saveAs
 *   table[] = ReconnectionPodRequest; existingPodChangeRequest optional
 *   (requires cancellationReasonId + reconnectionPodId — omit unless 400)
 * - GET /table tableByGridOperator → PageCreateReconnectionTableResponse
 *   ReconnectionTableListingRequest: gridOperatorId required; searchBy CUSTOMER_IDENTIFIER
 *   CreateReconnectionTableResponse.requestForDisconnectionId maps to POST
 *   requestForDisconnectionOfPowerSupplyId
 * - GET /table/view viewTable → PageTableViewResponse
 * - GET /table/view-temporary viewTableTemporary → TableViewResponse[]
 *   ReconnectionTablePreviewRequest (page, pageSize, prompt, searchBy) + reconnectionId
 * - GET /pod/{id} PointOfDeliveryResponse; optional query versionId (fixtures also use version=1)
 * - JSON field is `checked` (not isChecked). Do not send `checked` on POST.
 *
 * Reference spec(s):
 * - tests/cursor/PDT-3179-reconnection-invoice-emails.spec.ts
 * - tests/cursor/pdt-3179-reconnection-invoice-emails.fixtures.ts
 * - tests/cursor/PDT-2861-rfd-pod-reconnection-fk.spec.ts
 * - tests/cursor/PDT-2880-pod-disconnected-banner.spec.ts
 */

import { expect } from '../../fixtures/baseFixture';
import { envVariables } from '../../fixtures/envCashed';
import { randomGens } from '../../utils/randomGens';
import {
  PDT_3179_MANUAL_LIABILITY_AMOUNT,
  PDT_3179_RFD_POST_TIMEOUT_MS,
  asNumber,
  createExecutedDps,
  createExecutedRfd,
  customerIdentifier,
  entityId,
  getLiability,
  listingContent,
  monthStartYmd,
  nestedId,
  readHttpBody,
  resolveContractBillingGroupId,
  resolvePodIdentifier,
  runPdt3179ReceivableChain,
  todayYmd,
  type Pdt3179Fx,
} from './pdt-3179-reconnection-invoice-emails.fixtures';

export const PDT_3409_KEY = 'PDT-3409';
/** Exact Jira summary — including the ticket typo "Dissappearing". */
export const PDT_3409_TITLE =
  'Reconnection of power supply - DRAFT status - Dissappearing PODs';
/** Two 3179-scale chains (unpaid + dummy) plus optional EXECUTED reconnection (~40 min). */
export const PDT_3409_TEST_TIMEOUT_MS = 40 * 60 * 1000;

export type Pdt3409Fx = Pdt3179Fx;

/** One disconnected POD side of the PDT-3409 pair (unpaid target or dummy CREATE row). */
export type Pdt3409Party = {
  customerId: number;
  customerIdent: string;
  podId: number;
  podIdentifier: string;
  rfdId: number;
  dpsId: number;
  liabilityId: number;
  gridOperatorId: number;
};

export type Pdt3409DraftChain = {
  unpaid: Pdt3409Party;
  dummy: Pdt3409Party;
  reconnectionId: number;
  gridOperatorId: number;
};

const UNPAID_HIDE_BUG =
  'PDT-3409 / Phase 1 wiki 75923727: unpaid executed-RFD POD must stay visible and unchecked; hide-if-unpaid is a bug';

export function pdt3409RelevantKeys(): Array<
  'customer' | 'pod' | 'product' | 'productContract' | 'customerLiability'
> {
  return ['customer', 'pod', 'product', 'productContract', 'customerLiability'];
}

export function lastEntry<T>(arr: T[], label: string): T {
  expect(arr.length, `${label} must exist`).toBeGreaterThan(0);
  return arr[arr.length - 1];
}

export function customerIdentOf(customer: unknown): string {
  return String((customer as { identifier?: unknown } | undefined)?.identifier ?? '');
}

export function listingRows(body: unknown): Record<string, unknown>[] {
  if (Array.isArray(body)) return body as Record<string, unknown>[];
  if (body && typeof body === 'object' && Array.isArray((body as { content?: unknown }).content)) {
    return (body as { content: Record<string, unknown>[] }).content;
  }
  return [];
}

export function findPodTableRow(
  rows: Record<string, unknown>[],
  opts: { podId: number; podIdentifier: string },
): Record<string, unknown> | undefined {
  return rows.find((row) => {
    const id = Number(row.podId);
    const ident = String(row.podIdentifier ?? '');
    return id === opts.podId || ident === opts.podIdentifier;
  });
}

/** Swagger TableViewResponse / CreateReconnectionTableResponse field is `checked` (not Java isChecked). */
export function rowChecked(row: Record<string, unknown> | undefined): boolean | undefined {
  if (!row) return undefined;
  return typeof row.checked === 'boolean' ? row.checked : undefined;
}

export function rowMatchesCreatedPodAndRfd(
  row: Record<string, unknown> | undefined,
  party: { podId: number; rfdId: number },
): boolean {
  if (!row) return false;
  return Number(row.podId) === party.podId && Number(row.requestForDisconnectionId) === party.rfdId;
}

export function unpaidPodVisibleUnchecked(
  row: Record<string, unknown> | undefined,
): boolean {
  return Boolean(row) && rowChecked(row) === false;
}

export function missingUnpaidPodMessage(endpoint: string, podIdentifier: string): string {
  return (
    `${UNPAID_HIDE_BUG}. Endpoint ${endpoint} did not return POD ${podIdentifier}. ` +
    'Do not treat a missing unpaid row as success.'
  );
}

/** Dev 400: "cancellation reason is mandatory" / "not found with idnull" — not only camelCase. */
export function isCancellationReasonRequiredMessage(body: string): boolean {
  return /cancellation\s*reason/i.test(body);
}

function requireReasonForCancellationOnDev(context: string): number {
  const id = asNumber(envVariables.reason_for_cancellation);
  expect(
    id,
    `${context}: envVariables.reason_for_cancellation is required on Dev after HTTP 400 ` +
      `(cancellation reason is mandatory / not found with id null). Current value is ${id || 'missing/0'}.`,
  ).toBeGreaterThan(0);
  return id;
}

/**
 * GET listing: Swagger nests ReconnectionTable*Request as query object `request`.
 * Try request.* first, then flat keys (same pattern as PDT-3179 DPS table/executed).
 * reconnectionId is a sibling query param on view* operations, not inside the request DTO.
 */
export async function getReconnectionTable(
  fx: Pdt3409Fx,
  path: string,
  query: Record<string, string | number>,
  reconnectionId?: number,
): Promise<{ status: number; body: unknown }> {
  const nested: Record<string, string | number> = {};
  for (const [key, value] of Object.entries(query)) {
    nested[`request.${key}`] = value;
  }
  if (reconnectionId != null) nested.reconnectionId = reconnectionId;

  let res = await fx.Request.get(path, { params: nested });
  if (!res.ok()) {
    const flat: Record<string, string | number> = { ...query };
    if (reconnectionId != null) flat.reconnectionId = reconnectionId;
    res = await fx.Request.get(path, { params: flat });
  }
  await expect(res).CheckResponse();
  return { status: res.status(), body: await res.json() };
}

/** Same GET as getReconnectionTable but does not CheckResponse — P1–P4 must all record. */
export async function tryGetReconnectionTable(
  fx: Pdt3409Fx,
  path: string,
  query: Record<string, string | number>,
  reconnectionId?: number,
): Promise<{ ok: boolean; status: number; body: unknown; text: string }> {
  const nested: Record<string, string | number> = {};
  for (const [key, value] of Object.entries(query)) {
    nested[`request.${key}`] = value;
  }
  if (reconnectionId != null) nested.reconnectionId = reconnectionId;

  let res = await fx.Request.get(path, { params: nested });
  if (!res.ok()) {
    const flat: Record<string, string | number> = { ...query };
    if (reconnectionId != null) flat.reconnectionId = reconnectionId;
    res = await fx.Request.get(path, { params: flat });
  }
  const parsed = await readHttpBody(res);
  return {
    ok: parsed.status >= 200 && parsed.status < 300,
    status: parsed.status,
    body: parsed.json ?? parsed.text,
    text: parsed.text.slice(0, 400),
  };
}

export type Pdt3409ListingProbe = {
  ok: boolean;
  status: number;
  present: boolean;
  checked: boolean | undefined;
  matches: boolean;
  visibleUnchecked: boolean;
  row: Record<string, unknown> | undefined;
  rowCount: number;
};

export function probePartyListing(
  result: { ok: boolean; status: number; body: unknown },
  party: { podId: number; podIdentifier: string; rfdId: number },
): Pdt3409ListingProbe {
  const rows = result.ok ? listingRows(result.body) : [];
  const row = findPodTableRow(rows, party);
  return {
    ok: result.ok,
    status: result.status,
    present: Boolean(row),
    checked: rowChecked(row),
    matches: rowMatchesCreatedPodAndRfd(row, party),
    visibleUnchecked: unpaidPodVisibleUnchecked(row),
    row,
    rowCount: rows.length,
  };
}

export function formatPartyProbe(
  label: string,
  probe: Pdt3409ListingProbe,
  party: { podIdentifier: string },
): string {
  if (!probe.ok) {
    return `${label} HTTP ${probe.status} (listing failed).`;
  }
  if (!probe.present) {
    return (
      `${label} POD ${party.podIdentifier} missing (${probe.rowCount} rows). ` +
      'Do not treat a missing unpaid row as success.'
    );
  }
  return (
    `${label} POD ${party.podIdentifier} present, checked=${String(probe.checked)}, ` +
    `matchesCreatedPodAndRfd=${probe.matches}.`
  );
}

export async function getPodDisconnectedField(
  fx: Pdt3409Fx,
  podId: number,
): Promise<{ disconnected: unknown; path: string; status: number; body: Record<string, unknown> | null }> {
  const paths = [`${fx.Endpoints.pod}/${podId}`, `${fx.Endpoints.pod}/${podId}?version=1`];
  for (const path of paths) {
    const res = await fx.Request.get(path);
    const parsed = await readHttpBody(res);
    if (parsed.status >= 200 && parsed.status < 300) {
      const body = parsed.json;
      return {
        disconnected: body && typeof body === 'object' ? (body as { disconnected?: unknown }).disconnected : undefined,
        path,
        status: parsed.status,
        body,
      };
    }
  }
  return { disconnected: undefined, path: paths[0], status: 0, body: null };
}

export type Pdt3409ExecuteResult = {
  method: 'PUT' | 'POST' | 'NONE';
  ok: boolean;
  status: number;
  reconnectionId: number | null;
  text: string;
};

function buildExecutedReconnectionPayload(
  fx: Pdt3409Fx,
  gridOperatorId: number,
  dummy: Pdt3409Party,
  row: Record<string, unknown>,
  includeCancellationReason: boolean,
): Record<string, unknown> {
  const payload = fx.GeneratePayload.receivablesManagement.reconnectionOfPowerSupply() as Record<
    string,
    unknown
  >;
  payload.gridOperatorId = gridOperatorId;
  payload.saveAs = 'EXECUTED';
  payload.fileIds = [];
  payload.templateIds = [];
  payload.table = [
    mapGetTableRowToPodRequest(row, {
      customerId: dummy.customerId,
      podId: dummy.podId,
      rfdId: dummy.rfdId,
    }, { includeCancellationReason }),
  ];
  return payload;
}

function reconnectionIdFromHttpJson(json: unknown, fallback: number): number {
  try {
    return entityId(json);
  } catch {
    return fallback;
  }
}

/**
 * DRAFT → EXECUTED for the dummy POD already on the DRAFT.
 * Prefer PUT /reconnection-of-the-power-supply?reconnectionId= saveAs=EXECUTED with dummy table.
 * If PUT fails, POST a new reconnection saveAs=EXECUTED with only the dummy mapped row.
 * Does not POST an empty table. Omits cancellationReasonId unless HTTP 400 mentions it.
 * Does not CheckResponse so P1–P3 remain recorded if execute fails.
 */
export async function executeDummyReconnection(
  fx: Pdt3409Fx,
  chain: Pdt3409DraftChain,
  dummyCreateRow: Record<string, unknown> | undefined,
): Promise<Pdt3409ExecuteResult> {
  if (!dummyCreateRow) {
    return {
      method: 'NONE',
      ok: false,
      status: 0,
      reconnectionId: null,
      text: 'Dummy CREATE /table row missing; refused empty table (do not POST empty table).',
    };
  }

  const putUrl = `${fx.Endpoints.reconnectionOfPowerSupply}?reconnectionId=${chain.reconnectionId}`;
  let payload = buildExecutedReconnectionPayload(
    fx,
    chain.gridOperatorId,
    chain.dummy,
    dummyCreateRow,
    false,
  );
  let res = await fx.Request.put(putUrl, { data: payload });
  let parsed = await readHttpBody(res);
  if (parsed.status === 400) {
    const hay = parsed.text;
    const cancellationReasonId = isCancellationReasonRequiredMessage(hay)
      ? requireReasonForCancellationOnDev('PUT reconnection saveAs=EXECUTED')
      : asNumber(envVariables.reason_for_cancellation);
    if (isCancellationReasonRequiredMessage(hay)) {
      payload = buildExecutedReconnectionPayload(
        fx,
        chain.gridOperatorId,
        chain.dummy,
        dummyCreateRow,
        true,
      );
      res = await fx.Request.put(putUrl, { data: payload });
      parsed = await readHttpBody(res);
    }
    if (parsed.status === 400 && /existingPod|reconnectionPod/i.test(parsed.text)) {
      const viewPath = `${fx.Endpoints.reconnectionOfPowerSupply}/table/view`;
      const viewDummy = await tryGetReconnectionTable(fx, viewPath, {
        page: 0,
        pageSize: 50,
        prompt: chain.dummy.customerIdent,
        searchBy: 'CUSTOMER_IDENTIFIER',
      }, chain.reconnectionId);
      const viewRow = probePartyListing(viewDummy, chain.dummy).row;
      const reconnectionPodId = asNumber(viewRow?.reconnectionPodId);
      if (reconnectionPodId > 0 && cancellationReasonId > 0) {
        payload = buildExecutedReconnectionPayload(
          fx,
          chain.gridOperatorId,
          chain.dummy,
          dummyCreateRow,
          true,
        );
        payload.existingPodChangeRequest = [
          { reconnectionPodId, cancellationReasonId },
        ];
        res = await fx.Request.put(putUrl, { data: payload });
        parsed = await readHttpBody(res);
      }
    }
  }
  if (parsed.status >= 200 && parsed.status < 300) {
    return {
      method: 'PUT',
      ok: true,
      status: parsed.status,
      reconnectionId: reconnectionIdFromHttpJson(parsed.json, chain.reconnectionId),
      text: '',
    };
  }

  const putFailText = parsed.text.slice(0, 400);
  payload = buildExecutedReconnectionPayload(
    fx,
    chain.gridOperatorId,
    chain.dummy,
    dummyCreateRow,
    false,
  );
  res = await fx.Request.post(fx.Endpoints.reconnectionOfPowerSupply, { data: payload });
  parsed = await readHttpBody(res);
  if (parsed.status === 400) {
    if (isCancellationReasonRequiredMessage(parsed.text)) {
      requireReasonForCancellationOnDev('POST reconnection saveAs=EXECUTED');
      payload = buildExecutedReconnectionPayload(
        fx,
        chain.gridOperatorId,
        chain.dummy,
        dummyCreateRow,
        true,
      );
      res = await fx.Request.post(fx.Endpoints.reconnectionOfPowerSupply, { data: payload });
      parsed = await readHttpBody(res);
    }
  }
  if (parsed.status >= 200 && parsed.status < 300) {
    const reconnectionId = reconnectionIdFromHttpJson(parsed.json, 0);
    if (reconnectionId > 0) {
      fx.Responses.reconnectionOfPowerSupply.push(reconnectionId);
    }
    return {
      method: 'POST',
      ok: reconnectionId > 0,
      status: parsed.status,
      reconnectionId: reconnectionId > 0 ? reconnectionId : null,
      text: '',
    };
  }
  return {
    method: 'POST',
    ok: false,
    status: parsed.status,
    reconnectionId: null,
    text: `PUT HTTP failed (${putFailText}); POST HTTP ${parsed.status} ${parsed.text.slice(0, 400)}`,
  };
}

export async function resolveGridOperatorIdForPod(
  fx: Pdt3409Fx,
  pod: unknown,
): Promise<number> {
  const podId = entityId(pod);
  const getRes = await fx.Request.get(`${fx.Endpoints.pod}/${podId}?version=1`);
  await expect(getRes).CheckResponse();
  const body = (await getRes.json()) as Record<string, unknown>;
  const fromPod = nestedId(body.gridOperatorId) ?? asNumber(body.gridOperatorId);
  const fromCreate = nestedId((pod as Record<string, unknown> | undefined)?.gridOperatorId);
  const id = fromPod || fromCreate || asNumber(envVariables.grid_operator);
  expect(id, 'gridOperatorId for reconnection table').toBeGreaterThan(0);
  return id;
}

export async function resolvePodIdentifierForPod(fx: Pdt3409Fx, pod: unknown): Promise<string> {
  const created = String((pod as { identifier?: unknown } | undefined)?.identifier ?? '');
  if (created.length > 0) return created;
  const podId = entityId(pod);
  const getRes = await fx.Request.get(`${fx.Endpoints.pod}/${podId}`);
  await expect(getRes).CheckResponse();
  const identifier = String(((await getRes.json()) as { identifier?: unknown }).identifier ?? '');
  expect(identifier.length, 'POD identifier').toBeGreaterThan(0);
  return identifier;
}

/**
 * Map GET /table CreateReconnectionTableResponse → POST ReconnectionPodRequest.
 * GET field is `requestForDisconnectionId`; POST field is
 * `requestForDisconnectionOfPowerSupplyId`. Do not copy `checked`.
 * cancellationReasonId is not required in Swagger — omit unless caller asks.
 */
export function mapGetTableRowToPodRequest(
  row: Record<string, unknown>,
  fallback: { customerId: number; podId: number; rfdId: number },
  opts: { includeCancellationReason?: boolean } = {},
): Record<string, unknown> {
  const customerId =
    row.customerId != null ? entityId(row.customerId) : fallback.customerId;
  const podId = row.podId != null ? entityId(row.podId) : fallback.podId;
  const rfdId =
    row.requestForDisconnectionId != null
      ? entityId(row.requestForDisconnectionId)
      : fallback.rfdId;
  const item: Record<string, unknown> = {
    customerId,
    podId,
    requestForDisconnectionOfPowerSupplyId: rfdId,
  };
  if (opts.includeCancellationReason) {
    item.cancellationReasonId = requireReasonForCancellationOnDev(
      'ReconnectionPodRequest.cancellationReasonId',
    );
  }
  return item;
}

async function snapshotParty(
  fx: Pdt3409Fx,
  opts: {
    customer: unknown;
    pod: unknown;
    rfdId: number;
    dpsId: number;
    liabilityId: number;
  },
): Promise<Pdt3409Party> {
  return {
    customerId: entityId(opts.customer),
    customerIdent: customerIdentOf(opts.customer),
    podId: entityId(opts.pod),
    podIdentifier: await resolvePodIdentifierForPod(fx, opts.pod),
    rfdId: opts.rfdId,
    dpsId: opts.dpsId,
    liabilityId: opts.liabilityId,
    gridOperatorId: await resolveGridOperatorIdForPod(fx, opts.pod),
  };
}

/**
 * Dummy supply from scratch. Mirrors createSupplyChain but uses last indices and
 * overrides product_contract.basicParameters.customerId (generator always writes customer[0]).
 */
async function createDummySupplyChain(fx: Pdt3409Fx): Promise<void> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;

  const term = await Request.post(Endpoints.terms, { data: GeneratePayload.productAndServices.term() });
  await expect(term).CheckResponse();
  Responses.terms.push(await term.json());

  const electricity = await Request.post(
    Endpoints.priceComponent,
    { data: GeneratePayload.productAndServices.electricity() },
  );
  await expect(electricity).CheckResponse();
  Responses.priceComponent.push(await electricity.json());

  const productPayload = GeneratePayload.productAndServices.product() as Record<string, unknown>;
  productPayload.contractTypes = ['SUPPLY_ONLY'];
  productPayload.paymentGuarantees = ['NO'];
  // product() defaults termIndex=0 (unpaid term). POST /products field is termId
  // (Swagger ProductCreateRequest; backend validates as basicSettings.termId).
  productPayload.termId = entityId(lastEntry(Responses.terms, 'dummy term'));
  productPayload.priceComponentIds = [entityId(lastEntry(Responses.priceComponent, 'dummy price component'))];
  productPayload.interimAdvancePayments = [];
  productPayload.interimAdvancePaymentGroups = [];
  const product = await Request.post(Endpoints.product, { data: productPayload });
  await expect(product).CheckResponse();
  Responses.product.push(await product.json());

  const customerPayload = GeneratePayload.customers.customer_legal() as Record<string, unknown>;
  const customer = await Request.post(Endpoints.customer, { data: customerPayload });
  await expect(customer).CheckResponse();
  Responses.customer.push(await customer.json());

  const pod = await Request.post(Endpoints.pod, { data: GeneratePayload.pointsOfDelivery.pod_settlement() });
  await expect(pod).CheckResponse();
  const podBody = await pod.json();
  Responses.pod.push(podBody);

  const lastCustomerIdx = Responses.customer.length - 1;
  const lastProductIdx = Responses.product.length - 1;
  const lastPodIdx = Responses.pod.length - 1;
  const contractPayload = await GeneratePayload.contractsAndOrders.product_contract(
    lastCustomerIdx,
    lastProductIdx,
    lastPodIdx,
  );
  (contractPayload.basicParameters as Record<string, unknown>).customerId = entityId(
    lastEntry(Responses.customer, 'dummy customer'),
  );
  (contractPayload.productParameters as Record<string, unknown>).contractType = 'SUPPLY_ONLY';
  const monthly =
    asNumber((podBody as { estimatedMonthlyAvgConsumption?: unknown }).estimatedMonthlyAvgConsumption) || 1;
  contractPayload.additionalParameters.estimatedTotalConsumptionUnderContractKwh = (monthly * 12) / 1000;
  const contract = await Request.post(Endpoints.productContract, { data: contractPayload });
  await expect(contract).CheckResponse();
  Responses.productContract.push(await contract.json());

  const lastContractIdx = Responses.productContract.length - 1;
  const activation = await Request.post('/contract-pods/manual', {
    data: await GeneratePayload.pointsOfDelivery.pod_activation(
      lastPodIdx,
      monthStartYmd(),
      undefined,
      lastContractIdx,
    ),
  });
  await expect(activation).CheckResponse();
}

/** Last contract + last customer — generator customer_liability() binds customer[0]. */
async function createDummyOverdueLiability(fx: Pdt3409Fx): Promise<number> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const customer = lastEntry(Responses.customer, 'dummy customer');
  const contractId = entityId(lastEntry(Responses.productContract, 'dummy product contract'));
  const contractGet = await Request.get(`${Endpoints.productContract}/${contractId}?version=1`);
  await expect(contractGet).CheckResponse();
  const contractBody = (await contractGet.json()) as Record<string, unknown>;
  const billingGroupId = resolveContractBillingGroupId(contractBody);
  expect(billingGroupId, 'dummy contract billingGroupId').toBeGreaterThan(0);

  const yesterday = randomGens.generateYesterdaysDate('dd-mm-yyyy');
  const payload = GeneratePayload.receivablesManagement.customer_liability();
  payload.customerId = entityId(customer);
  payload.billingGroupId = billingGroupId;
  payload.initialAmount = PDT_3179_MANUAL_LIABILITY_AMOUNT;
  payload.dueDate = yesterday;
  payload.occurrenceDate = yesterday;

  const post = await Request.post(Endpoints.customerLiability, { data: payload });
  await expect(post).CheckResponse();
  const liabilityId = entityId(await post.json());
  Responses.customerLiability.push(liabilityId);

  const getLiab = await Request.get(`${Endpoints.customerLiability}/${liabilityId}`);
  await expect(getLiab).CheckResponse();
  const liab = (await getLiab.json()) as Record<string, unknown>;
  expect(asNumber(liab.currentAmount), 'dummy liability currentAmount > 0').toBeGreaterThan(0);
  return liabilityId;
}

async function reminderStatusOf(fx: Pdt3409Fx, reminderId: number): Promise<string | undefined> {
  try {
    const reminderGet = await fx.Request.get(`power-supply-disconnection-reminder/${reminderId}`, {
      timeout: 3_000,
    });
    if (!reminderGet.ok()) return undefined;
    return ((await reminderGet.json()) as { reminderStatus?: string }).reminderStatus;
  } catch {
    return undefined;
  }
}

/** Last customer identifier only — generator reminderForDisconnection joins every customer. */
async function createDummyExecutedReminder(fx: Pdt3409Fx): Promise<number> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const identifier = customerIdentOf(lastEntry(Responses.customer, 'dummy customer'));
  expect(identifier.length, 'dummy reminder customerList').toBeGreaterThan(0);
  const payload = GeneratePayload.receivablesManagement.reminderForDisconnection();
  payload.customerList = identifier;
  payload.customerFilterType = 'INCLUDED';
  payload.documentTemplateId = null;
  const res = await Request.post(Endpoints.reminderForDisconnection, { data: payload });
  await expect(res).CheckResponse();
  const reminderId = entityId(await res.json());
  Responses.reminderForDisconnection.push(reminderId);

  const now = new Date();
  const georgianTime = new Date(now.toLocaleString('en-US', { timeZone: 'Asia/Tbilisi' }));
  const timeOffset = await Request.put(
    `power-supply-disconnection-reminder/set-customer-send-time?reminderId=${reminderId}&hour=${georgianTime.getHours()}&minute=${georgianTime.getMinutes()}`,
    { timeout: 5_000 },
  );
  await expect(timeOffset).CheckResponse();

  const deadline = Date.now() + 20_000;
  while (Date.now() < deadline) {
    if ((await reminderStatusOf(fx, reminderId)) === 'EXECUTED') return reminderId;
    await new Promise((r) => setTimeout(r, 1_000));
  }
  return reminderId;
}

async function resolveDummyReminderIdForRfd(fx: Pdt3409Fx, createdId: number): Promise<number> {
  if ((await reminderStatusOf(fx, createdId)) === 'EXECUTED') return createdId;
  const listRes = await fx.Request.get(
    `${fx.Endpoints.reminderForDisconnection}/list?page=0&size=5&statuses=EXECUTED&direction=DESC`,
    { timeout: 8_000 },
  );
  await expect(listRes).CheckResponse();
  const rows = listingContent(await listRes.json());
  expect(rows.length, 'need at least one EXECUTED reminder for dummy RFD').toBeGreaterThan(0);
  return entityId(rows[0]);
}

/** Last customer / POD / contract / liability — 3179 fetchCheckedPodRowForRfd uses [0]. */
async function fetchDummyCheckedPodRowForRfd(fx: Pdt3409Fx): Promise<Record<string, unknown>> {
  const customer = lastEntry(fx.Responses.customer, 'dummy customer') as Record<string, unknown>;
  const pod = lastEntry(fx.Responses.pod, 'dummy pod') as Record<string, unknown>;
  const podId = entityId(pod);
  const customerId = entityId(customer);
  const identifier = customerIdentOf(customer);
  const contractId = entityId(lastEntry(fx.Responses.productContract, 'dummy product contract'));
  const liabilityId = entityId(lastEntry(fx.Responses.customerLiability, 'dummy liability'));

  const getRes = await fx.Request.get(`${fx.Endpoints.pod}/${podId}?version=1`);
  await expect(getRes).CheckResponse();
  const body = (await getRes.json()) as Record<string, unknown>;
  const versions = Array.isArray(body.versions) ? (body.versions as Record<string, unknown>[]) : [];
  const contractGet = await fx.Request.get(`${fx.Endpoints.productContract}/${contractId}?version=1`);
  await expect(contractGet).CheckResponse();
  const contractBody = (await contractGet.json()) as Record<string, unknown>;
  const contractPods = Array.isArray(contractBody.contractPodsResponses)
    ? (contractBody.contractPodsResponses as Record<string, unknown>[])
    : [];
  const podDetailId =
    nestedId(pod.podDetailId) ??
    nestedId(body.podDetailId) ??
    nestedId(body.lastPodDetailId) ??
    nestedId(versions[0]?.podDetailId) ??
    nestedId(contractPods[0]?.podDetailId) ??
    nestedId(contractPods[0]?.pointOfDeliveryDetailId);
  expect(podDetailId, 'dummy podDetailId').toBeGreaterThan(0);

  const liab = await getLiability(fx, liabilityId);
  const liabilityNumber = String(liab.number ?? '');
  expect(liabilityNumber.length, 'dummy GET /customer-liability number').toBeGreaterThan(0);
  const amount = asNumber(liab.currentAmount) || PDT_3179_MANUAL_LIABILITY_AMOUNT;
  const currencyName = String(
    (liab.currencyResponse as { name?: unknown } | undefined)?.name ?? 'BGN',
  ).replace(/-/g, ' ');
  const liabilityToken = `${amount}-${currencyName}-${liabilityNumber}`;
  const bp = (contractBody.basicParameters ?? {}) as Record<string, unknown>;
  const contractNumber = String(bp.contractNumber ?? contractId);
  const bg = Array.isArray(contractBody.billingGroups)
    ? (contractBody.billingGroups[0] as Record<string, unknown>)
    : undefined;
  const billingGroupLabel = String(bg?.groupNumber ?? bg?.number ?? nestedId(bg) ?? '');

  return {
    podId,
    customerId,
    podIdentifier: String(body.identifier ?? pod.identifier ?? ''),
    isChecked: true,
    isHighestConsumption: false,
    existingCustomerReceivables: true,
    gridOperatorId: nestedId(body.gridOperatorId) ?? envVariables.grid_operator,
    podDetailId,
    customerNumber: identifier,
    customers: identifier,
    contracts: contractNumber,
    billingGroups: billingGroupLabel || contractNumber,
    liabilitiesInBillingGroup: liabilityToken,
    liabilitiesInPod: liabilityToken,
    liabilityAmountCustomer: amount,
  };
}

async function createDummyExecutedRfd(fx: Pdt3409Fx, reminderId: number): Promise<number> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const identifier = customerIdentOf(lastEntry(Responses.customer, 'dummy customer'));
  expect(identifier.length, 'dummy RFD listOfCustomer').toBeGreaterThan(0);
  const podRow = await fetchDummyCheckedPodRowForRfd(fx);
  const payload = GeneratePayload.receivablesManagement.requestForDisconnection();
  payload.reminderForDisconnectionId = await resolveDummyReminderIdForRfd(fx, reminderId);
  payload.gridOpRequestRegDate = todayYmd();
  payload.disconnectionRequestsStatus = 'EXECUTED';
  payload.supplierType = 'CURRENT';
  payload.conditionType = 'LIST_OF_CUSTOMERS';
  payload.listOfCustomer = identifier;
  payload.allSelected = false;
  payload.pods = [podRow];
  payload.podWithHighestConsumption = false;
  payload.templateIds = [];
  const res = await Request.post(Endpoints.requestForDisconnection, {
    data: payload,
    timeout: PDT_3179_RFD_POST_TIMEOUT_MS,
  });
  await expect(res).CheckResponse();
  const requestId = entityId(await res.json());
  Responses.requestForDisconnection.push(requestId);
  return requestId;
}

/**
 * Last customer / POD / RFD — 3179 createExecutedDps and postDpsRaw both write [0].
 */
async function createDummyExecutedDps(
  fx: Pdt3409Fx,
  opts: { taxId: number; rfdId: number },
): Promise<{ status: number; id: number | null; text: string }> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const payload = GeneratePayload.receivablesManagement.disconnectionOfPowerSupply();
  payload.requestForDisconnectionId = opts.rfdId;
  payload.saveType = 'EXECUTED';
  payload.disconnectedRequest[0].customerId = entityId(lastEntry(Responses.customer, 'dummy customer'));
  payload.disconnectedRequest[0].podId = entityId(lastEntry(Responses.pod, 'dummy pod'));
  payload.disconnectedRequest[0].gridOperatorTaxesId = opts.taxId;
  payload.disconnectedRequest[0].expressReconnection = false;
  payload.disconnectedRequest[0].dateOfDisconnection = todayYmd();
  const res = await Request.post(Endpoints.disconnectionOfPowerSupply, { data: payload });
  const status = res.status();
  if (status >= 200 && status < 300) {
    await expect(res).CheckResponse();
    const body = await res.json();
    const id = entityId(body);
    Responses.disconnectionOfPowerSupply.push(id);
    return { status, id, text: '' };
  }
  const text = await res.text();
  return { status, id: null, text };
}

async function createDummyDisconnectedPod(
  fx: Pdt3409Fx,
  unpaidGridOperatorId: number,
): Promise<Pdt3409Party> {
  await createDummySupplyChain(fx);
  const liabilityId = await createDummyOverdueLiability(fx);
  const reminderId = await createDummyExecutedReminder(fx);
  const rfdId = await createDummyExecutedRfd(fx, reminderId);
  const taxId = Number(envVariables.taxes_for_grid_operator);
  expect(taxId, 'envVariables.taxes_for_grid_operator for dummy EXECUTED DPS').toBeGreaterThan(0);
  const dps = await createDummyExecutedDps(fx, { taxId, rfdId });
  expect(
    dps.status,
    `Dummy EXECUTED DPS is required so the dummy POD is disconnected. HTTP ${dps.status} ${dps.text.slice(0, 400)}`,
  ).toBeGreaterThanOrEqual(200);
  expect(dps.status).toBeLessThan(300);
  expect(dps.id, 'dummy DPS id').toBeTruthy();

  const dummy = await snapshotParty(fx, {
    customer: lastEntry(fx.Responses.customer, 'dummy customer'),
    pod: lastEntry(fx.Responses.pod, 'dummy pod'),
    rfdId,
    dpsId: dps.id as number,
    liabilityId,
  });
  expect(
    dummy.gridOperatorId,
    'dummy POD grid operator must match unpaid (same GET /table grid)',
  ).toBe(unpaidGridOperatorId);
  expect(dummy.customerId, 'dummy customer must not be unpaid customer[0]').not.toBe(
    entityId(fx.Responses.customer[0]),
  );
  expect(dummy.podId, 'dummy POD must not be unpaid pod[0]').not.toBe(entityId(fx.Responses.pod[0]));
  return dummy;
}

/**
 * GET /table by dummy identifier, map only that row, POST DRAFT. Unpaid POD is not in table.
 * Prefer omitting cancellationReasonId (not required). Retry with it only if CREATE returns 400.
 */
export async function createDraftReconnectionWithDummyTable(
  fx: Pdt3409Fx,
  gridOperatorId: number,
  dummy: Pdt3409Party,
): Promise<number> {
  const payload = fx.GeneratePayload.receivablesManagement.reconnectionOfPowerSupply() as Record<
    string,
    unknown
  >;
  payload.gridOperatorId = gridOperatorId;
  payload.saveAs = 'DRAFT';
  payload.fileIds = [];
  payload.templateIds = [];

  const tablePath = `${fx.Endpoints.reconnectionOfPowerSupply}/table`;
  const { body } = await getReconnectionTable(fx, tablePath, {
    gridOperatorId,
    page: 0,
    pageSize: 50,
    prompt: dummy.customerIdent,
    searchBy: 'CUSTOMER_IDENTIFIER',
  });
  const row = findPodTableRow(listingRows(body), dummy);
  expect(
    row,
    `CREATE POST /reconnection-of-the-power-supply requires a non-empty table (DRAFT included). ` +
      `GET ${tablePath} did not return dummy POD ${dummy.podIdentifier} after EXECUTED RFD + EXECUTED DPS. ` +
      `Do not POST an empty table and do not fall back to the unpaid POD.`,
  ).toBeTruthy();
  expect(Number(row!.podId), 'CREATE table row must be dummy POD, not unpaid').toBe(dummy.podId);

  const fallback = { customerId: dummy.customerId, podId: dummy.podId, rfdId: dummy.rfdId };
  payload.table = [mapGetTableRowToPodRequest(row as Record<string, unknown>, fallback)];

  let res = await fx.Request.post(fx.Endpoints.reconnectionOfPowerSupply, { data: payload });
  if (res.status() === 400) {
    const hay = await res.text();
    if (isCancellationReasonRequiredMessage(hay)) {
      requireReasonForCancellationOnDev('POST DRAFT reconnection');
      payload.table = [
        mapGetTableRowToPodRequest(row as Record<string, unknown>, fallback, {
          includeCancellationReason: true,
        }),
      ];
      res = await fx.Request.post(fx.Endpoints.reconnectionOfPowerSupply, { data: payload });
    }
  }
  await expect(res).CheckResponse();
  const reconnectionId = entityId(await res.json());
  fx.Responses.reconnectionOfPowerSupply.push(reconnectionId);
  return reconnectionId;
}

/**
 * Unpaid 3179 chain (uses [0]) + dummy disconnected POD (last ids) + DRAFT reconnection
 * whose POST table contains only the dummy row. Unpaid POD is never saved on the DRAFT.
 */
export async function runPdt3409DraftReconnectionChain(fx: Pdt3409Fx): Promise<Pdt3409DraftChain> {
  await runPdt3179ReceivableChain(fx);

  const unpaidRfdId = await createExecutedRfd(fx, { status: 'EXECUTED' });
  const taxId = Number(envVariables.taxes_for_grid_operator);
  expect(taxId, 'envVariables.taxes_for_grid_operator for unpaid EXECUTED DPS').toBeGreaterThan(0);
  const unpaidDps = await createExecutedDps(fx, { taxId, express: false, requestId: unpaidRfdId });
  expect(
    unpaidDps.status,
    `EXECUTED DPS is required so the unpaid POD is disconnected. HTTP ${unpaidDps.status} ${unpaidDps.text.slice(0, 400)}`,
  ).toBeGreaterThanOrEqual(200);
  expect(unpaidDps.status).toBeLessThan(300);
  expect(unpaidDps.id, 'unpaid DPS id').toBeTruthy();

  const unpaid = await snapshotParty(fx, {
    customer: fx.Responses.customer[0],
    pod: fx.Responses.pod[0],
    rfdId: unpaidRfdId,
    dpsId: unpaidDps.id as number,
    liabilityId: entityId(fx.Responses.customerLiability[0]),
  });
  expect(unpaid.customerIdent.length, 'unpaid customer identifier').toBeGreaterThan(0);
  expect(unpaid.customerIdent, 'unpaid snapshot matches customer[0]').toBe(customerIdentifier(fx.Responses));
  expect(unpaid.podIdentifier.length, 'unpaid POD identifier').toBeGreaterThan(0);
  expect(unpaid.podIdentifier, 'unpaid snapshot matches resolvePodIdentifier [0]').toBe(
    await resolvePodIdentifier(fx),
  );

  const dummy = await createDummyDisconnectedPod(fx, unpaid.gridOperatorId);
  expect(dummy.customerIdent, 'dummy identifier must differ from unpaid').not.toBe(unpaid.customerIdent);
  expect(dummy.podId, 'dummy POD id must differ from unpaid').not.toBe(unpaid.podId);

  const reconnectionId = await createDraftReconnectionWithDummyTable(fx, unpaid.gridOperatorId, dummy);

  return {
    unpaid,
    dummy,
    reconnectionId,
    gridOperatorId: unpaid.gridOperatorId,
  };
}
