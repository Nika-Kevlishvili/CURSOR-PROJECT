/**
 * PDT-3212 helpers — Request for disconnection: next POD with highest consumption.
 *
 * Product intent (Jira PDT-3212 / PDT-3345 + Confluence):
 * - Page: Request for disconnection of the power supply - Create, ID 72155868
 *   https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/72155868/Request+for+disconnection+of+the+power+supply+-+Create
 * - Page: Phase 2 - Request for disconnection of the power supply - Create (2), ID 585697986
 *   https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/585697986/Phase+2+-+Request+for+disconnection+of+the+power+supply+-+Create+(2)
 * - AC: Highest consumption is determined ONLY among PODs displayed on THIS RFD
 *   Point of Delivery list for that billing group. Off-list PODs must not rank.
 * - Among listed: YES = highest invoice detailed-data totalVolumes (sum);
 *   if equal or no usable volumes, highest pod.id (identity is pod.pod.id).
 *
 * Tests encode TO-BE. Fail vs spec if remaining listed rows are all
 * isHighestConsumption=false after the volume winner leaves the list (documented AS-IS).
 *
 * Dev2 swagger this session (config/swagger/dev2/swagger-spec.json):
 * - GET /disconnection-of-power-supply-requests/load-customer-for-disconnection-power-supply
 *   query CustomersForDPSRequest required: conditionType, page, size, supplierType
 *   conditionType enum ALL_CUSTOMERS | CUSTOMERS_UNDER_CONDITIONS | LIST_OF_CUSTOMERS
 *   supplierType enum CURRENT | PREVIOUS
 *   Runtime CheckResponse: HTTP 206 (Playwright ok); spec documents 200
 * - GET .../view-pod-tab/{id} same CustomersForDPSRequest; CustomersForDPSResponse
 *   isHighestConsumption boolean, isChecked boolean, podId int64
 * - POST /disconnection-of-power-supply-requests DPSRequestsBaseRequest required:
 *   conditionType, gridOpRequestRegDate, gridOperatorId, reasonOfDisconnectionId,
 *   reminderForDisconnectionId, supplierType, validityPeriodFrom, validityPeriodTo
 *   podWithHighestConsumption boolean; allSelected boolean; disconnectionRequestsStatus
 *   DRAFT | EXECUTED | FEE_CHARGED
 * - POST /disconnection-of-power-supply DisconnectionOfPowerSupplyRequest
 *   required requestForDisconnectionId, saveType (DRAFT | EXECUTED)
 *   disconnectedRequest[] DisconnectionOfPowerSupplyDisconnectedRequest required:
 *   customerId, dateOfDisconnection, gridOperatorTaxesId, podId
 *
 * Never conditionType=ALL_CUSTOMERS or allSelected=true. LIST_OF_CUSTOMERS +
 * validityPeriodFrom=today, validityPeriodTo=last day of month. supplierType CURRENT.
 *
 * postExecutedDpsForExactPod does NOT fall back to another POD (PDT-3090
 * postExecutedDpsForPod retries first POD — forbidden for PDT-3212).
 *
 * Reference spec(s):
 * - tests/cursor/PDT-3090-rfd-highest-consumption-fallback.spec.ts
 * - tests/cursor/pdt-3090-rfd-highest-consumption-fallback.fixtures.ts
 * - tests/cursor/PDT-3421-rfd-highest-consumption-uncheck.spec.ts
 * - tests/cursor/pdt-3421-rfd-highest-consumption-uncheck.fixtures.ts
 * - tests/receivableManagement/requestForDisconnection.spec.ts
 */

import { expect } from './cursor-test.fixtures';
import { envVariables } from '../../fixtures/envCashed';
import {
  PDT_3090_BE11_TIMEOUT_MS,
  PDT_3090_BE13_TIMEOUT_MS,
  PDT_3090_BE2_TIMEOUT_MS,
  PDT_3090_LOAD_PODS_TIMEOUT_MS,
  addAndActivateExtraPodsOnSameBillingGroup,
  calendarMonthPeriod,
  createDraftRfdWithHighestFilterOn,
  createExecutedRfdWithHighestFilterOn,
  createSupplyChainWithSettlementPriceComponent,
  customerIdentifier,
  ensureExecutedReminder,
  entityId,
  isCheckedFlag,
  isHighestConsumptionFlag,
  listActiveGridOperatorTaxId,
  loadCustomersForDps,
  makeInvoiceLiabilityOverdue,
  pdt3090RelevantKeys,
  postBillingByProfileForPods,
  realizeForVolumesInvoice,
  resolveLowHighPods,
  rowPodId,
  rowsForPodIds,
  sumInvoiceTotalVolumesByPod,
  todayYmd,
  viewPodTab,
  type Pdt3090Fx,
  type Pdt3090PodRef,
  type Pdt3090ProfilePeriod,
  type Pdt3090DpsRow,
} from './pdt-3090-rfd-highest-consumption-fallback.fixtures';

export const PDT_3212_KEY = 'PDT-3212';
/** Exact Jira summary — trailing space is part of the ticket title. */
export const PDT_3212_TITLE =
  'Request for disconnection - next POD with highest consumption ';

export const PDT_3212_BE2_TIMEOUT_MS = PDT_3090_BE2_TIMEOUT_MS;
export const PDT_3212_BE13_TIMEOUT_MS = PDT_3090_BE13_TIMEOUT_MS;
export const PDT_3212_BE11_TIMEOUT_MS = PDT_3090_BE11_TIMEOUT_MS;
export const PDT_3212_LOAD_PODS_TIMEOUT_MS = PDT_3090_LOAD_PODS_TIMEOUT_MS;

/** Confluence Case 1 — POD1=1500, POD2=2000, POD3=4020 assigned by ascending pod.id. */
export const PDT_3212_CASE1_VOLUMES = [1500, 2000, 4020] as const;
/** Jira 3-POD example — A highest volume is min(pod.id) so remaining YES is not max id. */
export const PDT_3212_JIRA_VOLUMES = [3000, 2000, 1000] as const;
export const PDT_3212_KALINA_VOLUMES = [3000, 1000] as const;
/** A unique high; B and C equal lower — after A off-list, YES = max(pod.id) among listed. */
export const PDT_3212_EQUAL_REMAINING_VOLUMES = [4000, 1000, 1000] as const;

export type Pdt3212Fx = Pdt3090Fx;

export function pdt3212RelevantKeys(opts?: {
  includeInvoice?: boolean;
  includeRfd?: boolean;
}): ReturnType<typeof pdt3090RelevantKeys> {
  return pdt3090RelevantKeys({
    includeInvoice: opts?.includeInvoice ?? true,
    includeRfd: opts?.includeRfd,
  });
}

export function sortPodsById(pods: Pdt3090PodRef[]): Pdt3090PodRef[] {
  return [...pods].sort((a, b) => a.podId - b.podId);
}

export function expectedHighestConsumptionPodId(
  listedPodIds: number[],
  sumsByPodId: Record<number, number>,
): number {
  expect(listedPodIds.length, 'need listed POD ids to rank highest consumption').toBeGreaterThan(0);
  return listedPodIds.reduce((best, id) => {
    const vol = sumsByPodId[id] ?? 0;
    const bestVol = sumsByPodId[best] ?? 0;
    if (vol > bestVol) return id;
    if (vol === bestVol && id > best) return id;
    return best;
  });
}

export function listedFlagSummary(rows: Pdt3090DpsRow[], podIds: number[]): string {
  const listed = rowsForPodIds(rows, podIds);
  if (listed.length === 0) return 'none listed';
  return listed
    .map((row) => `${rowPodId(row)}:hc=${String(row.isHighestConsumption)}:chk=${String(row.isChecked)}`)
    .join(', ');
}

async function postFifteenMinuteProfileForPod(
  fx: Pdt3212Fx,
  pod: Pdt3090PodRef,
  value: number,
  period: Pdt3090ProfilePeriod,
): Promise<number> {
  const payload = (await fx.GeneratePayload.energyData.profile15minute(pod.index, [
    { startDate: period.startYmd, endDate: period.endYmd, amount: value },
  ])) as Record<string, unknown>;
  payload.timeZone = 'CET';
  payload.profileId = envVariables.profiles;
  payload.identifier = pod.podIdentifier;
  payload.warningAcceptedByUser = true;
  payload.periodFrom = period.periodFrom;
  payload.periodTo = period.periodTo;
  const res = await fx.Request.post(fx.Endpoints.profilesByMonth, { data: payload });
  await expect(res).CheckResponse();
  const bbpId = Number(await res.json());
  expect(bbpId, `FIFTEEN_MINUTES billing-by-profile id for ${pod.podIdentifier}`).toBeGreaterThan(0);
  await fx.GeneratePayload.energyData.uploadDataByProfileFile(bbpId, 'MIN');
  fx.Responses.dataByProfiles.push({
    id: bbpId,
    identifier: pod.podIdentifier,
    periodType: 'FIFTEEN_MINUTES',
    value,
  });
  return bbpId;
}

async function postOneMonthBillingByProfileForPod(
  fx: Pdt3212Fx,
  pod: Pdt3090PodRef,
  value: number,
  period: Pdt3090ProfilePeriod,
): Promise<{ ok: true; bbpId: number } | { ok: false; status: number }> {
  const payload = (await fx.GeneratePayload.energyData.profile1Month(pod.index)) as Record<
    string,
    unknown
  >;
  payload.timeZone = 'CET';
  payload.profileId = envVariables.profiles;
  payload.identifier = pod.podIdentifier;
  payload.periodType = 'ONE_MONTH';
  payload.warningAcceptedByUser = true;
  payload.periodFrom = period.periodFrom;
  payload.periodTo = period.periodTo;
  const entries = Array.isArray(payload.entries)
    ? (payload.entries as Array<Record<string, unknown>>)
    : [];
  if (entries.length > 0) {
    entries[0].value = value;
    entries[0].shiftedHour = false;
    entries[0].periodFrom = period.periodFrom;
  } else {
    payload.entries = [{ periodFrom: period.periodFrom, value, shiftedHour: false }];
  }
  const res = await fx.Request.post(fx.Endpoints.profilesByMonth, { data: payload });
  if (!res.ok()) return { ok: false, status: res.status() };
  await expect(res).CheckResponse();
  const bbpId = Number(await res.json());
  expect(bbpId, `ONE_MONTH billing-by-profile id for ${pod.podIdentifier}`).toBeGreaterThan(0);
  fx.Responses.dataByProfiles.push({
    id: bbpId,
    identifier: pod.podIdentifier,
    periodType: 'ONE_MONTH',
    value,
  });
  return { ok: true, bbpId };
}

/**
 * Post BillingByProfileCreateRequest for N PODs. Reuses PDT-3090 pair helper for the
 * first two, then matches that periodType for extras (never mix ONE_MONTH + FIFTEEN_MINUTES).
 */
export async function postBillingByProfileForVolumePods(
  fx: Pdt3212Fx,
  items: Array<{ pod: Pdt3090PodRef; value: number }>,
  period: Pdt3090ProfilePeriod = calendarMonthPeriod(0),
): Promise<{ periodType: 'ONE_MONTH' | 'FIFTEEN_MINUTES'; ids: number[] }> {
  expect(items.length, 'PDT-3212 invoice volume chain needs ≥2 PODs').toBeGreaterThanOrEqual(2);
  const first = await postBillingByProfileForPods(
    fx,
    items[0].pod,
    items[1].pod,
    { lowValue: items[0].value, highValue: items[1].value },
    period,
  );
  const ids = [first.lowBbpId, first.highBbpId];
  for (const item of items.slice(2)) {
    if (first.periodType === 'ONE_MONTH') {
      const month = await postOneMonthBillingByProfileForPod(fx, item.pod, item.value, period);
      expect(
        month.ok,
        `PDT-3212 ONE_MONTH billing-by-profile failed for extra POD ${item.pod.podIdentifier} ` +
          `(HTTP ${month.ok ? 0 : month.status}). Must not mix FIFTEEN_MINUTES on the same period.`,
      ).toBe(true);
      if (month.ok) ids.push(month.bbpId);
    } else {
      ids.push(await postFifteenMinuteProfileForPod(fx, item.pod, item.value, period));
    }
  }
  return { periodType: first.periodType, ids };
}

export async function readInvoiceVolumeSums(
  fx: Pdt3212Fx,
  invoiceId: number,
  pods: Pdt3090PodRef[],
): Promise<Record<number, number>> {
  const sums: Record<number, number> = {};
  for (const pod of pods) {
    sums[pod.podId] = await sumInvoiceTotalVolumesByPod(fx, invoiceId, pod.podIdentifier);
    expect(
      sums[pod.podId],
      `invoice detailed-data totalVolumes for ${pod.podIdentifier} (podId ${pod.podId}) must be > 0 so the POD stays listed`,
    ).toBeGreaterThan(0);
  }
  return sums;
}

export function assertInvoiceSumsMatchAssignedVolumes(
  pods: Pdt3090PodRef[],
  assigned: number[],
  sumsByPodId: Record<number, number>,
): void {
  expect(pods.length).toBe(assigned.length);
  for (let i = 0; i < pods.length; i += 1) {
    for (let j = i + 1; j < pods.length; j += 1) {
      const left = sumsByPodId[pods[i].podId] ?? 0;
      const right = sumsByPodId[pods[j].podId] ?? 0;
      if (assigned[i] > assigned[j]) {
        expect(
          left,
          `sum(${pods[i].podIdentifier}) must exceed sum(${pods[j].podIdentifier}) (assigned ${assigned[i]} > ${assigned[j]})`,
        ).toBeGreaterThan(right);
      } else if (assigned[i] < assigned[j]) {
        expect(
          left,
          `sum(${pods[i].podIdentifier}) must be below sum(${pods[j].podIdentifier}) (assigned ${assigned[i]} < ${assigned[j]})`,
        ).toBeLessThan(right);
      } else {
        expect(
          left,
          `sum(${pods[i].podIdentifier}) must equal sum(${pods[j].podIdentifier}) (assigned ${assigned[i]} = ${assigned[j]})`,
        ).toBe(right);
      }
    }
  }
}

/**
 * Settlement FOR_VOLUMES chain: 1 + extraCount PODs on one billing group, invoice
 * detailed-data volumes matching assigned profile values (by ascending pod.id).
 */
export async function createVolumeInvoiceChain(
  fx: Pdt3212Fx,
  extraPodCount: number,
  volumesByAscendingPodId: number[],
): Promise<{
  sorted: Pdt3090PodRef[];
  invoiceId: number;
  periodType: 'ONE_MONTH' | 'FIFTEEN_MINUTES';
  sumsByPodId: Record<number, number>;
}> {
  expect(volumesByAscendingPodId.length, 'volume list must match POD count').toBe(extraPodCount + 1);
  await createSupplyChainWithSettlementPriceComponent(fx);
  if (extraPodCount > 0) {
    await addAndActivateExtraPodsOnSameBillingGroup(fx, extraPodCount);
  }
  expect(fx.Responses.pod.length, `need ${extraPodCount + 1} PODs on one billing group`).toBeGreaterThanOrEqual(
    extraPodCount + 1,
  );
  const pair = await resolveLowHighPods(fx);
  const sorted = sortPodsById(pair.all);
  expect(sorted.length).toBe(volumesByAscendingPodId.length);
  const items = sorted.map((pod, index) => ({ pod, value: volumesByAscendingPodId[index] }));
  const posted = await postBillingByProfileForVolumePods(fx, items);
  const invoiceId = await realizeForVolumesInvoice(fx);
  const sumsByPodId = await readInvoiceVolumeSums(fx, invoiceId, sorted);
  assertInvoiceSumsMatchAssignedVolumes(sorted, volumesByAscendingPodId, sumsByPodId);
  await makeInvoiceLiabilityOverdue(fx, invoiceId);
  return { sorted, invoiceId, periodType: posted.periodType, sumsByPodId };
}

/**
 * EXECUTED DPS for the intended podId only. Must not retry Responses.pod[0]
 * (PDT-3090 postExecutedDpsForPod does that — wrong POD would invalidate PDT-3212).
 */
export async function postExecutedDpsForExactPod(
  fx: Pdt3212Fx,
  rfdId: number,
  podId: number,
  gridOperatorTaxesId: number,
): Promise<number> {
  expect(podId, 'PDT-3212 must disconnect the intended highest-volume POD').toBeGreaterThan(0);
  const payload = fx.GeneratePayload.receivablesManagement.disconnectionOfPowerSupply();
  payload.requestForDisconnectionId = rfdId;
  payload.saveType = 'EXECUTED';
  payload.disconnectedRequest[0].customerId = entityId(fx.Responses.customer[0]);
  payload.disconnectedRequest[0].podId = podId;
  payload.disconnectedRequest[0].gridOperatorTaxesId = gridOperatorTaxesId;
  payload.disconnectedRequest[0].dateOfDisconnection = todayYmd();
  const res = await fx.Request.post(fx.Endpoints.disconnectionOfPowerSupply, { data: payload });
  if (!res.ok()) {
    const body = await res.text();
    expect(
      false,
      `PDT-3212 postExecutedDpsForExactPod failed for exact podId=${podId} on RFD ${rfdId} ` +
        `(HTTP ${res.status()} ${body.slice(0, 500)}). Must not fall back to another POD.`,
    ).toBe(true);
  }
  await expect(res).CheckResponse();
  const dpsId = entityId(await res.json());
  fx.Responses.disconnectionOfPowerSupply.push(dpsId);
  expect(dpsId, 'POST /disconnection-of-power-supply id').toBeGreaterThan(0);
  return dpsId;
}

export async function disconnectExactPodViaExecutedRfd(
  fx: Pdt3212Fx,
  reminderId: number,
  podId: number,
): Promise<{ rfdId: number; dpsId: number; taxId: number }> {
  const rfdId = await createExecutedRfdWithHighestFilterOn(fx, reminderId);
  expect(rfdId, 'EXECUTED RFD with podWithHighestConsumption=true').toBeGreaterThan(0);
  const taxId = await listActiveGridOperatorTaxId(fx);
  const dpsId = await postExecutedDpsForExactPod(fx, rfdId, podId, taxId);
  return { rfdId, dpsId, taxId };
}

/**
 * Reload Load PODs after a POD left the list. Prefer the same reminder; if the
 * expected remaining PODs are missing, POST a new EXECUTED reminder on the same
 * unpaid liability (do not pay it off).
 */
export async function loadCustomersForDpsAfterOffList(
  fx: Pdt3212Fx,
  reminderId: number,
  expectedListedPodIds: number[],
): Promise<{ reminderId: number; load: { status: number; rows: Pdt3090DpsRow[] }; usedNewReminder: boolean }> {
  let activeReminderId = reminderId;
  let usedNewReminder = false;
  let load = await loadCustomersForDps(fx, activeReminderId);
  let listed = rowsForPodIds(load.rows, expectedListedPodIds);
  if (listed.length < expectedListedPodIds.length) {
    activeReminderId = await ensureExecutedReminder(fx);
    usedNewReminder = true;
    load = await loadCustomersForDps(fx, activeReminderId);
    listed = rowsForPodIds(load.rows, expectedListedPodIds);
  }
  return { reminderId: activeReminderId, load, usedNewReminder };
}

export function assertToBeHighestAmongListed(opts: {
  rows: Pdt3090DpsRow[];
  offListPodIds: number[];
  listedPodIds: number[];
  expectedYesPodId: number;
}): void {
  const { rows, offListPodIds, listedPodIds, expectedYesPodId } = opts;
  for (const podId of offListPodIds) {
    expect(
      rowsForPodIds(rows, [podId]).length,
      `podId ${podId} must NOT be listed (disconnected / already on executed RFD / off-list)`,
    ).toBe(0);
  }
  const listed = rowsForPodIds(rows, listedPodIds);
  expect(
    listed.length,
    `Load PODs must list remaining PODs ${listedPodIds.join(', ')}`,
  ).toBe(listedPodIds.length);
  const allRemainingFalse = listed.length > 0 && listed.every((row) => row.isHighestConsumption === false);
  expect(
    allRemainingFalse,
    `Fail vs spec (PDT-3212 TO-BE / Confluence 72155868): remaining listed PODs must not all be ` +
      `isHighestConsumption=false when the previous volume winner is off-list. ` +
      `listed=${listedFlagSummary(rows, listedPodIds)}; expected YES podId=${expectedYesPodId}`,
  ).toBe(false);
  const yesRows = listed.filter(isHighestConsumptionFlag);
  expect(yesRows.length, 'exactly one isHighestConsumption===true among remaining listed PODs').toBe(1);
  expect(rowPodId(yesRows[0]), 'YES row must be the listed volume (then pod.id) winner').toBe(
    expectedYesPodId,
  );
  for (const row of listed) {
    const id = rowPodId(row);
    if (id === expectedYesPodId) {
      expect(row.isHighestConsumption, `podId ${id} isHighestConsumption`).toBe(true);
    } else {
      expect(row.isHighestConsumption, `podId ${id} isHighestConsumption must be false`).toBe(false);
    }
  }
}

export {
  createDraftRfdWithHighestFilterOn,
  createExecutedRfdWithHighestFilterOn,
  customerIdentifier,
  ensureExecutedReminder,
  entityId,
  isCheckedFlag,
  isHighestConsumptionFlag,
  loadCustomersForDps,
  rowPodId,
  rowsForPodIds,
  viewPodTab,
};
