import { test, expect } from '../../fixtures/baseFixture';
import { envVariables } from '../../fixtures/envCashed';
import {
  addDaysIso,
  applyPdt2599PerPieceDriversToServiceParameters,
  asBillingRunId,
  asPriceComponentId,
  alignPerPieceSecondSignedWindowBillingPayload,
  assertPdt2599InvoiceBilledAmountMatchesSignedVersionPriceComponent,
  assertPdt2599InvoiceNetMatchesExpected,
  assertSignedVersionEndDateChain,
  attachPdt2599CreatedEntities,
  attachPdt2599CreatedEntitiesForBilling,
  attachResponseLinkerAfterTest,
  buildPdt2599BillingRunPayload,
  buildServiceContractEditPayloadFromDetail,
  extractBillingInvoiceLinkedVersionId,
  extractFirstContractPriceComponentFormulaVariableId,
  assertPdt2599InvoiceLinksToExpectedContractVersionStrict,
  readPdt2599SignedVersionBillingRateScalar,
  readPdt2599BillingPriceComponentPrimaryFormulaVariableId,
  isSignedContractVersionStatus,
  pickLatestLogicalVersionId,
  pickFormulaFromThirdTab,
  pickFormulaDeepFromRecord,
  resolvePdt2599ContractFormulaForPost,
  pickFormulaFromNestedFormulaVariablesArrays,
  pickValidBillingDateFromBillingCommon,
  pickValidBillingDateFromBillingCommonWithinAccountingPeriod,
  pollPerPieceBillingRunUntilDraftWithInvoices,
  resolveAccountingPeriodIdForBillingDate,
  resolvePdt2599SecondSignedSliceStartForOpenAccountingPeriod,
  readPdt2599ContractVersionQuantityAndFormulaValue,
  PDT2599_OVER_TIME_ONE_TIME_PRIMARY_DRIVER,
  PDT2599_OVER_TIME_ONE_TIME_SECONDARY_DRIVER,
  PDT2599_OVER_TIME_ONE_TIME_TERTIARY_DRIVER,
  PDT2599_OVER_TIME_PERIODICAL_PRIMARY_DRIVER,
  PDT2599_OVER_TIME_PERIODICAL_SECONDARY_DRIVER,
  PDT2599_OVER_TIME_PERIODICAL_TERTIARY_DRIVER,
  PDT2599_PER_PIECE_PRIMARY_DRIVER,
  PDT2599_PER_PIECE_SECONDARY_DRIVER,
  PDT2599_PER_PIECE_V1_QUANTITY,
  PDT2599_PER_PIECE_V2_QUANTITY,
  pdt2599ExpectedOverTimeNet,
  pdt2599ExpectedPerPieceNet,
  runPdt2599SignedServiceContractBillingChain,
  runPdt2599SignedServiceContractChain,
} from './pdt-2599-service-contract.fixtures';

const PDT2599_FORMULA_VARIABLE_DEEP_SEARCH_MAX_DEPTH = 18;

/** Ids that must not be mistaken for price `formulaVariableId` during deep search (term rows look like formula rows). */
function pdt2599RejectIdsForFormulaDeepSearch(detail: Record<string, unknown>): Set<number> {
  const reject = new Set<number>();
  const sp = (detail.serviceParameters ?? {}) as Record<string, unknown>;
  for (const key of ['contractTermId', 'invoicePaymentTermId'] as const) {
    const v = sp[key];
    if (v != null && v !== '') {
      const n = typeof v === 'number' ? v : Number(v);
      if (Number.isFinite(n)) reject.add(n);
    }
  }
  const third = (detail.thirdPageTabs ?? {}) as Record<string, unknown>;
  for (const arrKey of ['serviceContractTerms', 'invoicePaymentTerms'] as const) {
    const arr = third[arrKey];
    if (!Array.isArray(arr)) continue;
    for (const row of arr) {
      if (row && typeof row === 'object' && 'id' in (row as object)) {
        const raw = (row as { id: unknown }).id;
        if (raw == null || raw === '') continue;
        const n = typeof raw === 'number' ? raw : Number(raw);
        if (Number.isFinite(n)) reject.add(n);
      }
    }
  }
  return reject;
}

function pdt2599AcceptFormulaVariableCandidate(n: number | null, rejectIds: Set<number>): number | null {
  if (n == null || !Number.isFinite(n)) return null;
  return rejectIds.has(n) ? null : n;
}

function pdt2599NormalizeNumericId(raw: unknown): number | null {
  if (raw == null || raw === '') return null;
  const n = typeof raw === 'number' ? raw : Number(raw);
  return Number.isFinite(n) ? n : null;
}

function pdt2599RowHasFormulaValueSignals(o: Record<string, unknown>): boolean {
  return (
    typeof o.value === 'number' ||
    (typeof o.valueFrom === 'number' && typeof o.valueTo === 'number')
  );
}

/**
 * DFS for the first finite formula driver id nested in API payloads (unknown shape).
 * Prefers `formulaVariableId`; then Phoenix `FormulaVariablePayload` (`variable` string + `id`); then
 * `formulaVariableId ?? id` when value signals exist; then `id` on formula-like rows.
 * Skips ids in `rejectIds` (e.g. contract term / invoice term) so deep search cannot pick those.
 * Guards: max depth and WeakSet for object cycles.
 */
function findFirstNumericFormulaVariableIdDeep(
  value: unknown,
  maxDepth: number,
  rejectIds: Set<number>,
  seen: WeakSet<object> | undefined = undefined,
): number | null {
  if (maxDepth < 0 || value == null) return null;
  const cycle = seen ?? new WeakSet<object>();
  if (Array.isArray(value)) {
    for (const el of value) {
      const r = findFirstNumericFormulaVariableIdDeep(el, maxDepth - 1, rejectIds, cycle);
      if (r != null) return r;
    }
    return null;
  }
  if (typeof value !== 'object') return null;
  const o = value as Record<string, unknown>;
  if (cycle.has(o)) return null;
  cycle.add(o);
  const hit =
    pdt2599AcceptFormulaVariableCandidate(pdt2599NormalizeNumericId(o.formulaVariableId), rejectIds) ??
    (typeof o.variable === 'string' && o.id != null
      ? pdt2599AcceptFormulaVariableCandidate(pdt2599NormalizeNumericId(o.id), rejectIds)
      : null);
  if (hit != null) return hit;
  if (pdt2599RowHasFormulaValueSignals(o)) {
    const n = pdt2599NormalizeNumericId(o.formulaVariableId ?? o.id);
    const v = pdt2599AcceptFormulaVariableCandidate(n, rejectIds);
    if (v != null) return v;
  }
  for (const k of Object.keys(o)) {
    if (k === 'formulaVariableId') continue;
    const r = findFirstNumericFormulaVariableIdDeep(o[k], maxDepth - 1, rejectIds, cycle);
    if (r != null) return r;
  }
  return null;
}

/** Fixture-aligned deep pick; returns null if the id is a known term/catalog id. */
function pdt2599FormulaVariableIdFromPickFormulaDeep(
  root: Record<string, unknown>,
  rejectIds: Set<number>,
): number | null {
  const p = pickFormulaDeepFromRecord(root);
  if (!p) return null;
  return rejectIds.has(p.formulaVariableId) ? null : p.formulaVariableId;
}

async function loadServiceContractDetail(
  Request: Parameters<typeof runPdt2599SignedServiceContractChain>[0]['Request'],
  Endpoints: Parameters<typeof runPdt2599SignedServiceContractChain>[0]['Endpoints'],
  contractId: number,
  queryVersionId?: number,
): Promise<Record<string, unknown>> {
  const res = await Request.get(`${Endpoints.serviceContract}/${contractId}`, {
    params: queryVersionId != null ? { versionId: queryVersionId } : undefined,
  });
  await expect(res).CheckResponse();
  return (await res.json()) as Record<string, unknown>;
}

/** Optional debug: draft invoice may expose linked logical version id (not used as pass/fail). */
function pdt2599AttachLinkedVersionIdDebug(inv: Record<string, unknown>, label: string): void {
  const linked = extractBillingInvoiceLinkedVersionId(inv);
  if (linked != null) {
    void test.info().attach(`[PDT-2599 debug] ${label} linked version id (informational only)`, {
      body: String(linked),
      contentType: 'text/plain; charset=utf-8',
    });
  }
}

/** Merge `GET third-tab-fields` into `thirdPageTabs` so edit payloads get `formulaVariables` (preview often omits formulas). */
async function loadServiceContractDetailForEdit(
  Request: Parameters<typeof runPdt2599SignedServiceContractChain>[0]['Request'],
  Endpoints: Parameters<typeof runPdt2599SignedServiceContractChain>[0]['Endpoints'],
  contractId: number,
  /** When performing in-place `PUT ...?versionId=X`, pass `X` so `podsEditList` IDs match that version (Phoenix `DomainEntityNotFoundException` otherwise). */
  queryVersionId?: number,
): Promise<Record<string, unknown>> {
  const detail = await loadServiceContractDetail(Request, Endpoints, contractId, queryVersionId);
  const sid = (detail.basicParameters as { serviceDetailId?: number } | undefined)?.serviceDetailId;
  if (!sid) return detail;
  const tRes = await Request.get(`${Endpoints.serviceContract}/third-tab-fields`, {
    params: { serviceDetailId: sid },
  });
  await expect(tRes).CheckResponse();
  const tab = (await tRes.json()) as Record<string, unknown>;
  const prev = (detail.thirdPageTabs ?? {}) as Record<string, unknown>;
  return { ...detail, thirdPageTabs: { ...prev, ...tab } };
}

function pdtSortedSignedStartDates(detail: Record<string, unknown>): string[] {
  const rows = ((detail.versions ?? []) as { startDate?: string; status?: unknown }[]) ?? [];
  return [...rows]
    .filter((v) => v.startDate && isSignedContractVersionStatus(v.status))
    .sort((a, b) => String(a.startDate).localeCompare(String(b.startDate)))
    .map((v) => String(v.startDate));
}

async function getFirstDraftInvoiceStubId(
  Request: Parameters<typeof runPdt2599SignedServiceContractChain>[0]['Request'],
  billingRunRoot: string,
  brid: number,
): Promise<number> {
  const drafts = await Request.get(`${billingRunRoot}/draft-invoices?id=${brid}&page=0&size=25`);
  await expect(drafts).CheckResponse();
  const dj = (await drafts.json()) as { content?: Array<{ id?: number }> };
  expect(dj.content?.length).toBeGreaterThan(0);
  return dj.content![0].id as number;
}

async function pdt2599ReadInterimAdvancePaymentContractFields(
  Request: Parameters<typeof runPdt2599SignedServiceContractChain>[0]['Request'],
  interimAdvancePaymentId: number,
): Promise<{ interimAdvancePaymentId: number; termValue: number }> {
  const interimGet = await Request.get(`iap/${interimAdvancePaymentId}?version=1`);
  await expect(interimGet).CheckResponse();
  const interimJson = (await interimGet.json()) as {
    id?: number;
    interimAdvancePaymentTerm?: { value?: number };
  };
  const id = Number(interimJson.id ?? interimAdvancePaymentId);
  const termValue = Number(interimJson.interimAdvancePaymentTerm?.value);
  expect(Number.isFinite(id)).toBeTruthy();
  expect(Number.isFinite(termValue)).toBeTruthy();
  return { interimAdvancePaymentId: id, termValue };
}

/** PUT one Signed/Draft slice with explicit `serviceParameters.interimAdvancePaymentsRequests` (Swagger `ServiceContractInterimAdvancePaymentsRequest`). */
async function pdt2599PutServiceContractVersionInterimAdvanceRequests(
  Request: Parameters<typeof runPdt2599SignedServiceContractChain>[0]['Request'],
  Endpoints: Parameters<typeof runPdt2599SignedServiceContractChain>[0]['Endpoints'],
  contractId: number,
  putParamsVersionId: number,
  savingAsNewVersion: boolean,
  editOpts: { contractVersionStatus: string; startDate?: string },
  interimRows: Array<{ value: number; interimAdvancePaymentId: number; termValue: number }>,
): Promise<void> {
  const detail = await loadServiceContractDetailForEdit(Request, Endpoints, contractId, putParamsVersionId);
  const basic = detail.basicParameters as { startDate?: string };
  const versions = (detail.versions as { versionId?: number; startDate?: string }[]) ?? [];
  const rowStart = versions.find((v) => v.versionId === putParamsVersionId)?.startDate;
  const sliceStart =
    editOpts.startDate ??
    (basic.startDate ? String(basic.startDate) : undefined) ??
    (rowStart ? String(rowStart) : undefined);
  if (!sliceStart) {
    throw new Error(
      `[PDT-2599] Cannot resolve startDate for interim PUT (contract ${contractId}, versionId ${putParamsVersionId}).`,
    );
  }
  const p = buildServiceContractEditPayloadFromDetail(detail, {
    savingAsNewVersion,
    contractVersionStatus: editOpts.contractVersionStatus,
    startDate: sliceStart,
  });
  const sp = p.serviceParameters as Record<string, unknown>;
  sp.interimAdvancePaymentsRequests = interimRows.map((r) => ({
    issueDate: null,
    value: r.value,
    interimAdvancePaymentId: r.interimAdvancePaymentId,
    termValue: r.termValue,
    contractFormulas: [],
  }));
  await expect(
    await Request.put(`${Endpoints.serviceContract}/${contractId}`, {
      params: { versionId: putParamsVersionId },
      data: { ...p, serviceParameters: sp },
    }),
  ).CheckResponse();
}

/** Single-row interim amount from preview/edit-shaped payloads (tolerates nested term objects). */
function pdt2599InterimNumericAmountFromRow(row: Record<string, unknown>): number | null {
  const pick = (v: unknown): number | null => (typeof v === 'number' && Number.isFinite(v) ? v : null);
  const direct = pick(row.value) ?? pick(row.amount) ?? pick(row.amountExcludingVat);
  if (direct != null) return direct;
  const iapTerms = row.iapTerms;
  if (iapTerms && typeof iapTerms === 'object') {
    const t = pdt2599InterimNumericAmountFromRow(iapTerms as Record<string, unknown>);
    if (t != null) return t;
  }
  const term = row.interimAdvancePaymentTerm;
  if (term && typeof term === 'object') {
    const tv = pick((term as Record<string, unknown>).value);
    if (tv != null) return tv;
  }
  return null;
}

function pdt2599CollectInterimLikeRowsFromServiceParameters(sp: Record<string, unknown>): Record<string, unknown>[] {
  const out: Record<string, unknown>[] = [];
  for (const k of ['interimAdvancePayments', 'interimAdvancePaymentsRequests', 'interimAdvancePaymentsList'] as const) {
    const arr = sp[k];
    if (!Array.isArray(arr)) continue;
    for (const el of arr) {
      if (el && typeof el === 'object') out.push(el as Record<string, unknown>);
    }
  }
  return out;
}

/**
 * Fallback: depth-first search under `serviceParameters` for an object with matching `interimAdvancePaymentId` and a usable amount field.
 */
function pdt2599FindInterimAmountDeepInServiceParameters(
  sp: Record<string, unknown>,
  interimAdvancePaymentId: number,
  maxDepth = 14,
): number | null {
  const want = Number(interimAdvancePaymentId);
  const seen = new WeakSet<object>();
  function walk(node: unknown, depth: number): number | null {
    if (depth < 0 || node == null) return null;
    if (Array.isArray(node)) {
      for (const el of node) {
        const h = walk(el, depth - 1);
        if (h != null) return h;
      }
      return null;
    }
    if (typeof node !== 'object') return null;
    const o = node as Record<string, unknown>;
    if (seen.has(o)) return null;
    seen.add(o);
    const iap = pdt2599NormalizeNumericId(o.interimAdvancePaymentId);
    if (iap === want) {
      const amt = pdt2599InterimNumericAmountFromRow(o);
      if (amt != null) return amt;
    }
    for (const k of Object.keys(o)) {
      const h = walk(o[k], depth - 1);
      if (h != null) return h;
    }
    return null;
  }
  return walk(sp, maxDepth);
}

/**
 * Reads persisted interim advance **amount** for one logical contract version via `GET /service-contract/{id}?versionId=…`.
 * Tries `serviceParameters` list keys first, then deep search for `interimAdvancePaymentId` + value-like fields.
 */
async function pdt2599ReadInterimAdvanceAmountForContractVersion(
  Request: Parameters<typeof runPdt2599SignedServiceContractChain>[0]['Request'],
  Endpoints: Parameters<typeof runPdt2599SignedServiceContractChain>[0]['Endpoints'],
  contractId: number,
  queryVersionId: number,
  interimAdvancePaymentId: number,
): Promise<number> {
  const detail = await loadServiceContractDetail(Request, Endpoints, contractId, queryVersionId);
  const sp = (detail.serviceParameters ?? {}) as Record<string, unknown>;
  const want = Number(interimAdvancePaymentId);
  const rows = pdt2599CollectInterimLikeRowsFromServiceParameters(sp);
  const matched =
    rows.find((r) => pdt2599NormalizeNumericId(r.interimAdvancePaymentId) === want) ??
    (rows.length === 1 ? rows[0] : undefined);
  let amount = matched != null ? pdt2599InterimNumericAmountFromRow(matched) : null;
  if (amount == null) amount = pdt2599FindInterimAmountDeepInServiceParameters(sp, want);
  expect(
    amount,
    `[PDT-2599] interim amount missing (contract ${contractId}, versionId ${queryVersionId}, iap ${want})`,
  ).not.toBeNull();
  expect(Number.isFinite(amount as number)).toBe(true);
  return amount as number;
}

function pdt2599ResponsesPriceComponentIds(
  Responses: Parameters<typeof runPdt2599SignedServiceContractChain>[0]['Responses'],
): number[] {
  return Responses.priceComponent.map((pc) => asPriceComponentId(pc));
}

/**
 * Two Signed slices with optional distinct formula drivers.
 * For PER_PIECE, pass `signedVersionQuantities` so v1/v2 differ by `serviceParameters.quantity`;
 * billing uses `invoiceDate` in-window; assert invoice net against that version's persisted quantity × formula value.
 */
async function applyPdt2599BillingTwoSignedDriverGraph(
  Request: Parameters<typeof runPdt2599SignedServiceContractChain>[0]['Request'],
  Endpoints: Parameters<typeof runPdt2599SignedServiceContractChain>[0]['Endpoints'],
  contractId: number,
  primaryDriver: number,
  secondaryDriver: number,
  opts?: {
    fallbackPriceComponentId?: number;
    fallbackPriceComponentIds?: number[];
    /** When set, both PUTs set `serviceParameters.quantity` (v1 first Signed, v2 second Signed). */
    signedVersionQuantities?: { v1: number; v2: number };
  },
): Promise<{ driverOverridesApplied: boolean }> {
  let detail = await loadServiceContractDetailForEdit(Request, Endpoints, contractId, 1);
  const rejectFormulaIds = pdt2599RejectIdsForFormulaDeepSearch(detail);
  const c = String((detail.basicParameters as { creationDate?: string }).creationDate);
  const spLoad = (detail.serviceParameters ?? {}) as {
    priceComponents?: { formulaVariableId?: number }[];
    contractFormulas?: { formulaVariableId?: number }[];
  };
  const pcs = (spLoad.priceComponents ?? []) as { formulaVariableId?: number }[];
  const persistedFormulas = spLoad.contractFormulas ?? [];
  let fv: number | undefined;
  if (persistedFormulas.length > 0 && persistedFormulas[0]?.formulaVariableId != null) {
    const n = Number(persistedFormulas[0].formulaVariableId);
    if (Number.isFinite(n) && !rejectFormulaIds.has(n)) {
      fv = n;
    }
  }
  if ((fv == null || !Number.isFinite(fv)) && pcs.length > 0 && pcs[0]?.formulaVariableId != null) {
    const n = Number(pcs[0].formulaVariableId);
    if (Number.isFinite(n) && !rejectFormulaIds.has(n)) {
      fv = n;
    }
  }
  if (fv == null || !Number.isFinite(fv)) {
    const depth = PDT2599_FORMULA_VARIABLE_DEEP_SEARCH_MAX_DEPTH;
    let picked = pickFormulaFromThirdTab((detail.thirdPageTabs ?? {}) as Record<string, unknown>);
    if (!picked) {
      picked = pickFormulaFromNestedFormulaVariablesArrays((detail.thirdPageTabs ?? {}) as Record<string, unknown>);
    }
    if (picked) {
      const n = Number(picked.formulaVariableId);
      if (Number.isFinite(n) && !rejectFormulaIds.has(n)) {
        fv = n;
      }
    }
    if (fv == null || !Number.isFinite(fv)) {
      const idList =
        opts?.fallbackPriceComponentIds && opts.fallbackPriceComponentIds.length > 0
          ? opts.fallbackPriceComponentIds
          : opts?.fallbackPriceComponentId != null
            ? [opts.fallbackPriceComponentId]
            : [];
      if (idList.length > 0 && (fv == null || !Number.isFinite(fv))) {
        for (const pcid of idList) {
          const pcRes = await Request.get(`${Endpoints.priceComponent}/${pcid}`);
          await expect(pcRes).CheckResponse();
          const pcJson = (await pcRes.json()) as Record<string, unknown>;
          const fromPickPc = pdt2599FormulaVariableIdFromPickFormulaDeep(pcJson, rejectFormulaIds);
          if (fromPickPc != null) {
            fv = fromPickPc;
            break;
          }
          const fromPc = findFirstNumericFormulaVariableIdDeep(pcJson, depth, rejectFormulaIds);
          if (fromPc != null) {
            fv = fromPc;
            break;
          }
        }
      }
      if (fv == null || !Number.isFinite(fv)) {
        const tabs = (detail.thirdPageTabs ?? {}) as Record<string, unknown>;
        let tabPick = pickFormulaFromThirdTab(tabs) ?? pickFormulaFromNestedFormulaVariablesArrays(tabs);
        if (tabPick) {
          const n = Number(tabPick.formulaVariableId);
          if (Number.isFinite(n) && !rejectFormulaIds.has(n)) {
            fv = n;
          }
        }
      }
      if (idList.length > 0 && (fv == null || !Number.isFinite(fv))) {
        const sid = (detail.basicParameters as { serviceDetailId?: number } | undefined)?.serviceDetailId;
        expect(sid).toBeTruthy();
        const tRes = await Request.get(`${Endpoints.serviceContract}/third-tab-fields`, {
          params: { serviceDetailId: sid },
        });
        await expect(tRes).CheckResponse();
        const thirdFresh = (await tRes.json()) as Record<string, unknown>;
        let pickedThird =
          pickFormulaFromThirdTab(thirdFresh) ?? pickFormulaFromNestedFormulaVariablesArrays(thirdFresh);
        if (pickedThird) {
          const n = Number(pickedThird.formulaVariableId);
          if (Number.isFinite(n) && !rejectFormulaIds.has(n)) {
            fv = n;
          }
        }
        if (fv == null || !Number.isFinite(fv)) {
          const primaryStored = { id: idList[idList.length - 1] };
          const extraStored = idList.slice(0, -1).map((id) => ({ id }));
          const fromResolve = await resolvePdt2599ContractFormulaForPost(
            Request,
            Endpoints,
            thirdFresh,
            primaryStored,
            extraStored,
          );
          if (fromResolve?.formulaVariableId != null) {
            const n = Number(fromResolve.formulaVariableId);
            if (Number.isFinite(n) && !rejectFormulaIds.has(n)) {
              fv = n;
            }
          }
          const persistedFv = persistedFormulas[0]?.formulaVariableId;
          if ((fv == null || !Number.isFinite(fv)) && persistedFv != null) {
            const n = Number(persistedFv);
            if (Number.isFinite(n) && !rejectFormulaIds.has(n)) {
              fv = n;
            }
          }
          if (fv == null || !Number.isFinite(fv)) {
            const fromPickThird = pdt2599FormulaVariableIdFromPickFormulaDeep(thirdFresh, rejectFormulaIds);
            if (fromPickThird != null) {
              fv = fromPickThird;
            } else {
              const fromDeepThird = findFirstNumericFormulaVariableIdDeep(thirdFresh, depth, rejectFormulaIds);
              if (fromDeepThird != null) {
                fv = fromDeepThird;
              } else {
                for (const pcid of idList) {
                  const pcRes = await Request.get(`${Endpoints.priceComponent}/${pcid}`);
                  await expect(pcRes).CheckResponse();
                  const pcJson = (await pcRes.json()) as Record<string, unknown>;
                  const fromPickPc = pdt2599FormulaVariableIdFromPickFormulaDeep(pcJson, rejectFormulaIds);
                  if (fromPickPc != null) {
                    fv = fromPickPc;
                    break;
                  }
                  const fromPc = findFirstNumericFormulaVariableIdDeep(pcJson, depth, rejectFormulaIds);
                  if (fromPc != null) {
                    fv = fromPc;
                    break;
                  }
                }
              }
            }
          }
        }
      } else if (fv == null || !Number.isFinite(fv)) {
        expect(picked).toBeTruthy();
        fv = undefined;
      }
    }
  }

  let pV1 = buildServiceContractEditPayloadFromDetail(detail, {
    savingAsNewVersion: false,
    contractVersionStatus: 'SIGNED',
  });
  const sp1 = pV1.serviceParameters as { contractFormulas: { formulaVariableId: number; value: number }[] };
  const payloadFv = sp1.contractFormulas?.[0]?.formulaVariableId;
  if ((fv == null || !Number.isFinite(fv)) && payloadFv != null) {
    const n = Number(payloadFv);
    if (Number.isFinite(n) && !rejectFormulaIds.has(n)) {
      fv = n;
    }
  }

  const applyDrivers = fv != null && Number.isFinite(fv);
  if (!applyDrivers) {
    test.info().annotations.push({
      type: 'warning',
      description:
        'PDT-2599 applyPdt2599BillingTwoSignedDriverGraph: PER_PIECE driver override skipped — formulaVariableId unavailable after resolver (null), detail.contractFormulas, edit payload fallbacks, deep search (third-tab/contract/detail), and price-component GET fallbacks.',
    });
  } else {
    sp1.contractFormulas = applyPdt2599PerPieceDriversToServiceParameters(sp1.contractFormulas, fv!, primaryDriver);
  }
  if (opts?.signedVersionQuantities) {
    (sp1 as { quantity?: number | null }).quantity = opts.signedVersionQuantities.v1;
  }
  await expect(await Request.put(`${Endpoints.serviceContract}/${contractId}`, { params: { versionId: 1 }, data: { ...pV1, serviceParameters: sp1 } }))
    .CheckResponse();

  detail = await loadServiceContractDetailForEdit(Request, Endpoints, contractId);
  const secondSignedStart = await resolvePdt2599SecondSignedSliceStartForOpenAccountingPeriod(Request, c);
  let pV2 = buildServiceContractEditPayloadFromDetail(detail, {
    savingAsNewVersion: true,
    contractVersionStatus: 'SIGNED',
    startDate: secondSignedStart,
  });
  const sp2 = pV2.serviceParameters as { contractFormulas: { formulaVariableId: number; value: number }[] };
  if (applyDrivers) {
    sp2.contractFormulas = applyPdt2599PerPieceDriversToServiceParameters(sp2.contractFormulas, fv!, secondaryDriver);
  }
  if (opts?.signedVersionQuantities) {
    (sp2 as { quantity?: number | null }).quantity = opts.signedVersionQuantities.v2;
  }

  await expect(
    await Request.put(`${Endpoints.serviceContract}/${contractId}`, {
      params: { versionId: pickLatestLogicalVersionId(detail.versions as { versionId: number }[]) },
      data: { ...pV2, serviceParameters: sp2 },
    }),
  ).CheckResponse();
  return { driverOverridesApplied: applyDrivers };
}

test.describe.serial('[PDT-2599]: BE Service Contract versioning', { tag: '@contractsAndOrders' }, () => {
  test.describe.configure({ timeout: 60_000 });

  test("[PDT-2599] TC-BE-1: BE - Service Contract - The system doesn't consider the contract version status", async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    const { contractId } = await runPdt2599SignedServiceContractChain({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
    });

    await test.step('GET /service-contract/{id} — verify first Signed version & dates', async () => {
      const getRes = await Request.get(`${Endpoints.serviceContract}/${contractId}`);
      await expect(getRes).CheckResponse();
      const body = await getRes.json();

      const creationDate = body.basicParameters?.creationDate;
      expect(creationDate).toBeTruthy();
      expect(isSignedContractVersionStatus(body.basicParameters?.contractVersionStatus)).toBe(true);

      const versions = body.versions ?? [];
      expect(versions.length).toBe(1);

      const first = versions[0];
      expect(isSignedContractVersionStatus(first.status)).toBe(true);
      expect(first.startDate).toBe(creationDate);
      expect(first.endDate == null || first.endDate === '').toBeTruthy();
    });

    attachPdt2599CreatedEntities(Responses, contractId);
  });

  test('[PDT-2599] TC-BE-2: POST /service-contract/list — created contract appears when searching by contract number', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    const { contractId } = await runPdt2599SignedServiceContractChain({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
    });

    await test.step('POST /service-contract/list with CONTRACT_NUMBER prompt', async () => {
      const getRes = await Request.get(`${Endpoints.serviceContract}/${contractId}`);
      await expect(getRes).CheckResponse();
      const detail = await getRes.json();
      const contractNumber = detail.basicParameters?.contractNumber;
      expect(contractNumber).toBeTruthy();

      const listRes = await Request.post(`${Endpoints.serviceContract}/list`, {
        data: {
          page: 0,
          size: 20,
          searchBy: 'CONTRACT_NUMBER',
          prompt: contractNumber,
        },
      });
      await expect(listRes).CheckResponse();
      const page = await listRes.json();
      const content = page.content ?? [];
      expect(Array.isArray(content)).toBeTruthy();
      const ids = content.map((row: { id?: number }) => row.id);
      expect(ids).toContain(contractId);
    });

    attachPdt2599CreatedEntities(Responses, contractId);
  });

  test('[PDT-2599] TC-BE-3: GET /service-contract/{id} — non-existent id returns 400 or 404', async ({
    Request,
    Endpoints,
    Responses,
  }) => {
    await test.step('GET unlikely id', async () => {
      const res = await Request.get(`${Endpoints.serviceContract}/99999999999999999`);
      expect([400, 404]).toContain(res.status());
    });
    attachResponseLinkerAfterTest(Responses);
  });

  test('[PDT-2599] TC-BE-4: GET /service-contract/{id} — invalid versionId returns error', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    const { contractId } = await runPdt2599SignedServiceContractChain({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
    });

    await test.step('GET with non-existent versionId', async () => {
      const res = await Request.get(`${Endpoints.serviceContract}/${contractId}`, {
        params: { versionId: 9999999999999 },
      });
      expect([400, 404]).toContain(res.status());
    });

    attachPdt2599CreatedEntities(Responses, contractId);
  });

  test('[PDT-2599] TC-BE-5: GET /service-contract/{id} — first version row has versionId 1', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    const { contractId } = await runPdt2599SignedServiceContractChain({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
    });

    await test.step('Assert ServiceContractVersions.versionId', async () => {
      const getRes = await Request.get(`${Endpoints.serviceContract}/${contractId}`);
      await expect(getRes).CheckResponse();
      const body = await getRes.json();
      const first = (body.versions ?? [])[0];
      expect(first).toBeTruthy();
      expect(first.versionId).toBe(1);
    });

    attachPdt2599CreatedEntities(Responses, contractId);
  });

  test('[PDT-2599] TC-BE-6: GET /service-contract/third-tab-fields — missing serviceDetailId rejected', async ({
    Request,
    Endpoints,
    Responses,
  }) => {
    await test.step('Omit required query param serviceDetailId', async () => {
      const res = await Request.get(`${Endpoints.serviceContract}/third-tab-fields`);
      expect([400, 404, 500]).toContain(res.status());
    });
    attachResponseLinkerAfterTest(Responses);
  });

  test('[PDT-2599] TC-BE-7: duplicate Valid start date on new version → 4xx (EN message)', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    const { contractId } = await runPdt2599SignedServiceContractChain({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
    });
    const detail = await loadServiceContractDetailForEdit(Request, Endpoints, contractId);
    const creationDate = String((detail.basicParameters as { creationDate?: string }).creationDate);
    const payload = buildServiceContractEditPayloadFromDetail(detail, {
      savingAsNewVersion: true,
      startDate: creationDate,
      contractVersionStatus: 'SIGNED',
    });
    const putRes = await Request.put(`${Endpoints.serviceContract}/${contractId}`, {
      params: { versionId: 1 },
      data: payload,
    });
    expect(putRes.status(), await putRes.text()).toBeGreaterThanOrEqual(400);
    const err = await putRes.text();
    expect(err.toLowerCase()).toMatch(/contract version already has provided start date|already exists/);
    attachPdt2599CreatedEntities(Responses, contractId);
  });

  test('[PDT-2599] TC-BE-8: three Signed versions — end dates = day before next start', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    const { contractId } = await runPdt2599SignedServiceContractChain({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
    });
    let detail = await loadServiceContractDetailForEdit(Request, Endpoints, contractId);
    const c = String((detail.basicParameters as { creationDate?: string }).creationDate);
    const d2 = addDaysIso(c, 400);
    const d3 = addDaysIso(c, 800);

    let putRes = await Request.put(`${Endpoints.serviceContract}/${contractId}`, {
      params: { versionId: pickLatestLogicalVersionId(detail.versions as { versionId: number }[]) },
      data: buildServiceContractEditPayloadFromDetail(detail, {
        savingAsNewVersion: true,
        startDate: d2,
        contractVersionStatus: 'SIGNED',
      }),
    });
    await expect(putRes).CheckResponse();

    detail = await loadServiceContractDetailForEdit(Request, Endpoints, contractId);
    putRes = await Request.put(`${Endpoints.serviceContract}/${contractId}`, {
      params: { versionId: pickLatestLogicalVersionId(detail.versions as { versionId: number }[]) },
      data: buildServiceContractEditPayloadFromDetail(detail, {
        savingAsNewVersion: true,
        startDate: d3,
        contractVersionStatus: 'SIGNED',
      }),
    });
    await expect(putRes).CheckResponse();

    detail = await loadServiceContractDetailForEdit(Request, Endpoints, contractId);
    assertSignedVersionEndDateChain((detail.versions as Record<string, unknown>[]) ?? []);
    attachPdt2599CreatedEntities(Responses, contractId);
  });

  test('[PDT-2599] TC-BE-9: insert Signed between two Signed — neighbour end dates recalc', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    const { contractId } = await runPdt2599SignedServiceContractChain({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
    });
    let detail = await loadServiceContractDetailForEdit(Request, Endpoints, contractId);
    const c = String((detail.basicParameters as { creationDate?: string }).creationDate);
    const far = addDaysIso(c, 500);

    let putRes = await Request.put(`${Endpoints.serviceContract}/${contractId}`, {
      params: { versionId: 1 },
      data: buildServiceContractEditPayloadFromDetail(detail, {
        savingAsNewVersion: true,
        startDate: far,
        contractVersionStatus: 'SIGNED',
      }),
    });
    await expect(putRes).CheckResponse();

    detail = await loadServiceContractDetailForEdit(Request, Endpoints, contractId);
    const between = addDaysIso(c, 200);
    putRes = await Request.put(`${Endpoints.serviceContract}/${contractId}`, {
      params: { versionId: pickLatestLogicalVersionId(detail.versions as { versionId: number }[]) },
      data: buildServiceContractEditPayloadFromDetail(detail, {
        savingAsNewVersion: true,
        startDate: between,
        contractVersionStatus: 'SIGNED',
      }),
    });
    await expect(putRes).CheckResponse();

    detail = await loadServiceContractDetailForEdit(Request, Endpoints, contractId);
    assertSignedVersionEndDateChain((detail.versions as Record<string, unknown>[]) ?? []);
    attachPdt2599CreatedEntities(Responses, contractId);
  });

  test('[PDT-2599] TC-BE-10: versionId ordinals 1..n after out-of-order chronological inserts', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    const { contractId } = await runPdt2599SignedServiceContractChain({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
    });
    let detail = await loadServiceContractDetailForEdit(Request, Endpoints, contractId);
    const c = String((detail.basicParameters as { creationDate?: string }).creationDate);

    let putRes = await Request.put(`${Endpoints.serviceContract}/${contractId}`, {
      params: { versionId: 1 },
      data: buildServiceContractEditPayloadFromDetail(detail, {
        savingAsNewVersion: true,
        startDate: addDaysIso(c, 500),
        contractVersionStatus: 'SIGNED',
      }),
    });
    await expect(putRes).CheckResponse();

    detail = await loadServiceContractDetailForEdit(Request, Endpoints, contractId);
    putRes = await Request.put(`${Endpoints.serviceContract}/${contractId}`, {
      params: { versionId: pickLatestLogicalVersionId(detail.versions as { versionId: number }[]) },
      data: buildServiceContractEditPayloadFromDetail(detail, {
        savingAsNewVersion: true,
        startDate: addDaysIso(c, 200),
        contractVersionStatus: 'SIGNED',
      }),
    });
    await expect(putRes).CheckResponse();

    detail = await loadServiceContractDetailForEdit(Request, Endpoints, contractId);
    const ids = ((detail.versions as { versionId: number }[]) ?? []).map((v) => v.versionId).sort((a, b) => a - b);
    expect(ids).toEqual([1, 2, 3]);
    attachPdt2599CreatedEntities(Responses, contractId);
  });

  test('[PDT-2599] TC-BE-11: add non-first Draft — distinct start, open end', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    const { contractId } = await runPdt2599SignedServiceContractChain({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
    });
    let detail = await loadServiceContractDetailForEdit(Request, Endpoints, contractId);
    const c = String((detail.basicParameters as { creationDate?: string }).creationDate);
    const draftStart = addDaysIso(c, 120);
    const putRes = await Request.put(`${Endpoints.serviceContract}/${contractId}`, {
      params: { versionId: 1 },
      data: buildServiceContractEditPayloadFromDetail(detail, {
        savingAsNewVersion: true,
        startDate: draftStart,
        contractVersionStatus: 'DRAFT',
      }),
    });
    await expect(putRes).CheckResponse();

    detail = await loadServiceContractDetailForEdit(Request, Endpoints, contractId);
    const draftRow = ((detail.versions as Record<string, unknown>[]) ?? []).find(
      (v) => v.versionId === 2,
    );
    expect(draftRow).toBeTruthy();
    /** API may surface Not-Valid rows as DRAFT / READY / etc. — not `Valid` / `SIGNED`. */
    expect(isSignedContractVersionStatus(draftRow!.status)).toBe(false);
    expect(draftRow!.endDate == null || draftRow!.endDate === '').toBeTruthy();
    attachPdt2599CreatedEntities(Responses, contractId);
  });

  test('[PDT-2599] TC-BE-12: Draft duplicate start date → 4xx', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    const { contractId } = await runPdt2599SignedServiceContractChain({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
    });
    const detail = await loadServiceContractDetailForEdit(Request, Endpoints, contractId);
    const creationDate = String((detail.basicParameters as { creationDate?: string }).creationDate);
    const payload = buildServiceContractEditPayloadFromDetail(detail, {
      savingAsNewVersion: true,
      startDate: creationDate,
      contractVersionStatus: 'DRAFT',
    });
    const putRes = await Request.put(`${Endpoints.serviceContract}/${contractId}`, {
      params: { versionId: 1 },
      data: payload,
    });
    expect(putRes.status()).toBeGreaterThanOrEqual(400);
    attachPdt2599CreatedEntities(Responses, contractId);
  });

  test('[PDT-2599] TC-BE-13: second Signed start earlier than first → 4xx', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    const { contractId } = await runPdt2599SignedServiceContractChain({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
    });
    let detail = await loadServiceContractDetailForEdit(Request, Endpoints, contractId);
    const c = String((detail.basicParameters as { creationDate?: string }).creationDate);
    let putRes = await Request.put(`${Endpoints.serviceContract}/${contractId}`, {
      params: { versionId: 1 },
      data: buildServiceContractEditPayloadFromDetail(detail, {
        savingAsNewVersion: true,
        startDate: addDaysIso(c, 300),
        contractVersionStatus: 'SIGNED',
      }),
    });
    await expect(putRes).CheckResponse();

    detail = await loadServiceContractDetailForEdit(Request, Endpoints, contractId, 2);
    const evilStart = addDaysIso(c, -5);
    putRes = await Request.put(`${Endpoints.serviceContract}/${contractId}`, {
      params: { versionId: 2 },
      data: buildServiceContractEditPayloadFromDetail(detail, {
        savingAsNewVersion: false,
        startDate: evilStart,
        contractVersionStatus: 'SIGNED',
      }),
    });
    expect(putRes.status()).toBeGreaterThanOrEqual(400);
    const t = await putRes.text();
    expect(t.toLowerCase()).toMatch(/first version|earlier than the previous|after the start date of the first version/);
    attachPdt2599CreatedEntities(Responses, contractId);
  });

  test('[PDT-2599] TC-BE-14: edit middle Signed start — chain stays coherent', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    const { contractId } = await runPdt2599SignedServiceContractChain({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
    });
    let detail = await loadServiceContractDetailForEdit(Request, Endpoints, contractId);
    const c = String((detail.basicParameters as { creationDate?: string }).creationDate);

    let putRes = await Request.put(`${Endpoints.serviceContract}/${contractId}`, {
      params: { versionId: 1 },
      data: buildServiceContractEditPayloadFromDetail(detail, {
        savingAsNewVersion: true,
        startDate: addDaysIso(c, 200),
        contractVersionStatus: 'SIGNED',
      }),
    });
    await expect(putRes).CheckResponse();

    detail = await loadServiceContractDetailForEdit(Request, Endpoints, contractId);
    putRes = await Request.put(`${Endpoints.serviceContract}/${contractId}`, {
      params: { versionId: 1 },
      data: buildServiceContractEditPayloadFromDetail(detail, {
        savingAsNewVersion: true,
        startDate: addDaysIso(c, 600),
        contractVersionStatus: 'SIGNED',
      }),
    });
    await expect(putRes).CheckResponse();

    detail = await loadServiceContractDetailForEdit(Request, Endpoints, contractId, 2);
    const putRes2 = await Request.put(`${Endpoints.serviceContract}/${contractId}`, {
      params: { versionId: 2 },
      data: buildServiceContractEditPayloadFromDetail(detail, {
        savingAsNewVersion: false,
        startDate: addDaysIso(c, 350),
        contractVersionStatus: 'SIGNED',
      }),
    });
    await expect(putRes2).CheckResponse();

    detail = await loadServiceContractDetailForEdit(Request, Endpoints, contractId);
    assertSignedVersionEndDateChain((detail.versions as Record<string, unknown>[]) ?? []);
    attachPdt2599CreatedEntities(Responses, contractId);
  });

  test('[PDT-2599] TC-BE-15: first Signed cannot change start date (in-place PUT)', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    const { contractId } = await runPdt2599SignedServiceContractChain({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
    });
    const detail = await loadServiceContractDetailForEdit(Request, Endpoints, contractId, 1);
    const c = String((detail.basicParameters as { creationDate?: string }).creationDate);
    const putRes = await Request.put(`${Endpoints.serviceContract}/${contractId}`, {
      params: { versionId: 1 },
      data: buildServiceContractEditPayloadFromDetail(detail, {
        savingAsNewVersion: false,
        startDate: addDaysIso(c, 10),
        contractVersionStatus: 'SIGNED',
      }),
    });
    expect(putRes.status()).toBeGreaterThanOrEqual(400);
    const body = await putRes.text();
    expect(body.toLowerCase()).toMatch(/start date must not be changed|startdate-/);
    attachPdt2599CreatedEntities(Responses, contractId);
  });

  test('[PDT-2599] TC-BE-16: status-update v1 to CANCELLED rejected', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    const { contractId } = await runPdt2599SignedServiceContractChain({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
    });
    const detail = await loadServiceContractDetailForEdit(Request, Endpoints, contractId);
    const basic = detail.basicParameters as Record<string, unknown>;
    const putRes = await Request.put(`${Endpoints.serviceContract}/status-update/${contractId}`, {
      params: { versionId: 1 },
      data: {
        contractStatus: basic.contractStatus,
        contractSubStatus: basic.detailsSubStatus,
        contractVersionStatus: 'CANCELLED',
      },
    });
    expect(putRes.ok()).toBeFalsy();
    const after = await loadServiceContractDetail(Request, Endpoints, contractId);
    const v1 = ((after.versions as { versionId: number; status?: string }[]) ?? []).find((v) => v.versionId === 1);
    expect(isSignedContractVersionStatus(v1?.status)).toBe(true);
    attachPdt2599CreatedEntities(Responses, contractId);
  });

  test('[PDT-2599] TC-BE-17: non-first Draft → READY via status-update (when chain allows)', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    const { contractId } = await runPdt2599SignedServiceContractChain({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
    });
    let detail = await loadServiceContractDetailForEdit(Request, Endpoints, contractId);
    const c = String((detail.basicParameters as { creationDate?: string }).creationDate);
    let putRes = await Request.put(`${Endpoints.serviceContract}/${contractId}`, {
      params: { versionId: 1 },
      data: buildServiceContractEditPayloadFromDetail(detail, {
        savingAsNewVersion: true,
        startDate: addDaysIso(c, 90),
        contractVersionStatus: 'DRAFT',
      }),
    });
    await expect(putRes).CheckResponse();

    detail = await loadServiceContractDetailForEdit(Request, Endpoints, contractId);
    const basic = detail.basicParameters as Record<string, unknown>;
    const contractSubStatus = (basic.subStatus ?? basic.detailsSubStatus) as string;
    const statusRes = await Request.put(`${Endpoints.serviceContract}/status-update/${contractId}`, {
      params: { versionId: 2 },
      data: {
        contractStatus: basic.contractStatus,
        contractSubStatus,
        contractVersionStatus: 'READY',
      },
    });
    if (!statusRes.ok()) {
      test.info().annotations.push({
        type: 'note',
        description: `TC-BE-17: status-update returned ${statusRes.status()}: ${await statusRes.text().then((t) => t.slice(0, 400))}`,
      });
    }
    expect([200, 201].includes(statusRes.status())).toBeTruthy();
    const v2View = await loadServiceContractDetail(Request, Endpoints, contractId, 2);
    const v2Basic = v2View.basicParameters as { contractVersionStatus?: string };
    expect(v2Basic.contractVersionStatus).toBe('READY');
    attachPdt2599CreatedEntities(Responses, contractId);
  });

  test('[PDT-2599] TC-BE-18: GET versions — Signed group sorted by start; new version ids monotonic', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    const { contractId } = await runPdt2599SignedServiceContractChain({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
    });
    let detail = await loadServiceContractDetailForEdit(Request, Endpoints, contractId);
    const c = String((detail.basicParameters as { creationDate?: string }).creationDate);

    let putRes = await Request.put(`${Endpoints.serviceContract}/${contractId}`, {
      params: { versionId: 1 },
      data: buildServiceContractEditPayloadFromDetail(detail, {
        savingAsNewVersion: true,
        startDate: addDaysIso(c, 700),
        contractVersionStatus: 'SIGNED',
      }),
    });
    await expect(putRes).CheckResponse();

    detail = await loadServiceContractDetailForEdit(Request, Endpoints, contractId);
    putRes = await Request.put(`${Endpoints.serviceContract}/${contractId}`, {
      params: { versionId: pickLatestLogicalVersionId(detail.versions as { versionId: number }[]) },
      data: buildServiceContractEditPayloadFromDetail(detail, {
        savingAsNewVersion: true,
        startDate: addDaysIso(c, 400),
        contractVersionStatus: 'DRAFT',
      }),
    });
    await expect(putRes).CheckResponse();

    detail = await loadServiceContractDetailForEdit(Request, Endpoints, contractId);
    const versions = (detail.versions as Record<string, unknown>[]) ?? [];
    const canonical = [...versions].sort((a, b) => {
      const ga = isSignedContractVersionStatus(a.status) ? 0 : 1;
      const gb = isSignedContractVersionStatus(b.status) ? 0 : 1;
      if (ga !== gb) return ga - gb;
      return String(a.startDate).localeCompare(String(b.startDate));
    });
    expect(versions.map((v) => v.versionId)).toEqual(canonical.map((v) => v.versionId));
    const maxId = Math.max(...versions.map((v) => Number(v.versionId)));
    expect(maxId).toBe(versions.length);
    attachPdt2599CreatedEntities(Responses, contractId);
  });

  test('[PDT-2599] TC-BE-19: validation error surfaces message text (sample from duplicate-date)', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    const { contractId } = await runPdt2599SignedServiceContractChain({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
    });
    const detail = await loadServiceContractDetailForEdit(Request, Endpoints, contractId);
    const creationDate = String((detail.basicParameters as { creationDate?: string }).creationDate);
    const putRes = await Request.put(`${Endpoints.serviceContract}/${contractId}`, {
      params: { versionId: 1 },
      data: buildServiceContractEditPayloadFromDetail(detail, {
        savingAsNewVersion: true,
        startDate: creationDate,
        contractVersionStatus: 'SIGNED',
      }),
    });
    const txt = await putRes.text();
    expect(putRes.status()).toBeGreaterThanOrEqual(400);
    expect(txt.length).toBeGreaterThan(5);
    attachPdt2599CreatedEntities(Responses, contractId);
  });

  test('[PDT-2599] TC-BE-20: after Signed date edit, Draft row stays open-ended', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    const { contractId } = await runPdt2599SignedServiceContractChain({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
    });
    let detail = await loadServiceContractDetailForEdit(Request, Endpoints, contractId);
    const c = String((detail.basicParameters as { creationDate?: string }).creationDate);

    let putRes = await Request.put(`${Endpoints.serviceContract}/${contractId}`, {
      params: { versionId: 1 },
      data: buildServiceContractEditPayloadFromDetail(detail, {
        savingAsNewVersion: true,
        startDate: addDaysIso(c, 250),
        contractVersionStatus: 'SIGNED',
      }),
    });
    await expect(putRes).CheckResponse();

    detail = await loadServiceContractDetailForEdit(Request, Endpoints, contractId);
    putRes = await Request.put(`${Endpoints.serviceContract}/${contractId}`, {
      params: { versionId: 1 },
      data: buildServiceContractEditPayloadFromDetail(detail, {
        savingAsNewVersion: true,
        startDate: addDaysIso(c, 400),
        contractVersionStatus: 'DRAFT',
      }),
    });
    await expect(putRes).CheckResponse();

    detail = await loadServiceContractDetailForEdit(Request, Endpoints, contractId, 2);
    putRes = await Request.put(`${Endpoints.serviceContract}/${contractId}`, {
      params: { versionId: 2 },
      data: buildServiceContractEditPayloadFromDetail(detail, {
        savingAsNewVersion: false,
        startDate: addDaysIso(c, 280),
        contractVersionStatus: 'SIGNED',
      }),
    });
    await expect(putRes).CheckResponse();

    detail = await loadServiceContractDetailForEdit(Request, Endpoints, contractId);
    const draft = ((detail.versions as Record<string, unknown>[]) ?? []).find((v) => v.versionId === 3);
    expect(isSignedContractVersionStatus(draft?.status)).toBe(false);
    expect(draft!.endDate == null || draft!.endDate === '').toBeTruthy();
    attachPdt2599CreatedEntities(Responses, contractId);
  });

  test('[PDT-2599] TC-BE-21: PUT with unknown versionId → 4xx, GET unchanged', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    const { contractId } = await runPdt2599SignedServiceContractChain({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
    });
    const before = await loadServiceContractDetailForEdit(Request, Endpoints, contractId);
    const snap = JSON.stringify(before.versions);
    const detail = before;
    const putRes = await Request.put(`${Endpoints.serviceContract}/${contractId}`, {
      params: { versionId: 99_999 },
      data: buildServiceContractEditPayloadFromDetail(detail, {
        savingAsNewVersion: false,
        contractVersionStatus: 'SIGNED',
      }),
    });
    expect(putRes.status()).toBeGreaterThanOrEqual(400);
    const after = await loadServiceContractDetail(Request, Endpoints, contractId);
    expect(JSON.stringify(after.versions)).toBe(snap);
    attachPdt2599CreatedEntities(Responses, contractId);
  });

  test('[PDT-2599] TC-BE-22: new Signed start strictly before first version start → 4xx', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    const { contractId } = await runPdt2599SignedServiceContractChain({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
    });
    const detail = await loadServiceContractDetailForEdit(Request, Endpoints, contractId);
    const c = String((detail.basicParameters as { creationDate?: string }).creationDate);
    const putRes = await Request.put(`${Endpoints.serviceContract}/${contractId}`, {
      params: { versionId: 1 },
      data: buildServiceContractEditPayloadFromDetail(detail, {
        savingAsNewVersion: true,
        startDate: addDaysIso(c, -10),
        contractVersionStatus: 'SIGNED',
      }),
    });
    expect(putRes.status()).toBeGreaterThanOrEqual(400);
    attachPdt2599CreatedEntities(Responses, contractId);
  });

  test('[PDT-2599] TC-BE-23: invalid contractVersionStatus in PUT body → client error', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    const { contractId } = await runPdt2599SignedServiceContractChain({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
    });
    const detail = await loadServiceContractDetailForEdit(Request, Endpoints, contractId);
    const payload = buildServiceContractEditPayloadFromDetail(detail, {
      savingAsNewVersion: false,
      contractVersionStatus: 'SIGNED',
    }) as { basicParameters: Record<string, unknown> };
    payload.basicParameters.contractVersionStatus = 'NOT_A_STATUS_ENUM';
    const putRes = await Request.put(`${Endpoints.serviceContract}/${contractId}`, {
      params: { versionId: 1 },
      data: payload,
    });
    expect([400, 415, 422].includes(putRes.status())).toBeTruthy();
    attachPdt2599CreatedEntities(Responses, contractId);
  });
});

/** PER_PIECE: v1/v2 differ by quantity + formula drivers; pass/fail = invoice net ex-VAT matches `readPdt2599ContractVersionQuantityAndFormulaValue` × {@link pdt2599ExpectedPerPieceNet}. */
test.describe('[PDT-2599] PER_PIECE — billing', { tag: '@contractsAndOrders' }, () => {
  test.describe.configure({ mode: 'serial' });
  test.setTimeout(540_000);

    test('[PDT-2599] TC-BE-24: PER_PIECE — first Signed window — invoices issued (distinct drivers v1/v2)', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
    }) => {
      const fixtures = { Request, GeneratePayload, Responses, Endpoints };
      const chain = await runPdt2599SignedServiceContractBillingChain(fixtures, 'PER_PIECE');
      const contractId = chain.contractId;
      await test.step('Precondition: tariff drivers diverge across Signed boundaries', async () => {
        await applyPdt2599BillingTwoSignedDriverGraph(
          Request,
          Endpoints,
          contractId,
          PDT2599_PER_PIECE_PRIMARY_DRIVER,
          PDT2599_PER_PIECE_SECONDARY_DRIVER,
          {
            fallbackPriceComponentIds: pdt2599ResponsesPriceComponentIds(Responses),
            signedVersionQuantities: { v1: PDT2599_PER_PIECE_V1_QUANTITY, v2: PDT2599_PER_PIECE_V2_QUANTITY },
          },
        );
      });

      await test.step('POST first-window billing-run + drafts', async () => {
        const detailRo = await loadServiceContractDetail(Request, Endpoints, contractId);
        const signedStarts = pdtSortedSignedStartDates(detailRo);
        expect(signedStarts.length).toBeGreaterThanOrEqual(2);
        const w1Pick = pickValidBillingDateFromBillingCommon(signedStarts[0], signedStarts[1]);

        const payload = await buildPdt2599BillingRunPayload(Request, Responses.serviceContract, ['PER_PIECE'], {
          taxEventDate: w1Pick,
          invoiceDate: w1Pick,
        });

        const br = await Request.post(Endpoints.billingRun, { data: payload });
        await expect(br).CheckResponse();
        const brid = asBillingRunId(await br.json());
        const { draftInvoiceCount } = await pollPerPieceBillingRunUntilDraftWithInvoices(
          Request,
          Endpoints.billingRun,
          brid,
          { minimumInvoices: 1 },
        );
        expect(draftInvoiceCount).toBeGreaterThan(0);

        const iid = await getFirstDraftInvoiceStubId(Request, Endpoints.billingRun, brid);
        const iv = await Request.get(`invoice?id=${iid}`);
        await expect(iv).CheckResponse();

        const invJson = (await iv.json()) as Record<string, unknown>;
        const v1base = await readPdt2599ContractVersionQuantityAndFormulaValue(Request, Endpoints, contractId, 1);
        expect(v1base.quantity, 'TC-BE-24').not.toBeNull();
        expect(v1base.formulaValue, 'TC-BE-24').not.toBeNull();
        assertPdt2599InvoiceNetMatchesExpected(
          invJson,
          pdt2599ExpectedPerPieceNet(v1base.quantity!, v1base.formulaValue!),
          'TC-BE-24 PER_PIECE first Signed window (net ex-VAT = quantity × formula value)',
        );
        pdt2599AttachLinkedVersionIdDebug(invJson, 'TC-BE-24');
        attachPdt2599CreatedEntitiesForBilling(Responses, contractId, brid);
      });
    });

    test('[PDT-2599] TC-BE-25: PER_PIECE — second Signed window — billingDate aligns second driver fingerprints', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
    }) => {
      const fixtures = { Request, GeneratePayload, Responses, Endpoints };
      const chain = await runPdt2599SignedServiceContractBillingChain(fixtures, 'PER_PIECE');
      const contractId = chain.contractId;

      await test.step('Precondition: tariff drivers diverge across Signed boundaries', async () => {
        await applyPdt2599BillingTwoSignedDriverGraph(
          Request,
          Endpoints,
          contractId,
          PDT2599_PER_PIECE_PRIMARY_DRIVER,
          PDT2599_PER_PIECE_SECONDARY_DRIVER,
          {
            fallbackPriceComponentIds: pdt2599ResponsesPriceComponentIds(Responses),
            signedVersionQuantities: { v1: PDT2599_PER_PIECE_V1_QUANTITY, v2: PDT2599_PER_PIECE_V2_QUANTITY },
          },
        );
      });

      await test.step('POST second Signed window billing-run + drafts', async () => {
        const detailRo = await loadServiceContractDetail(Request, Endpoints, contractId);
        const signedStarts = pdtSortedSignedStartDates(detailRo);
        expect(signedStarts.length).toBeGreaterThanOrEqual(2);

        const w2Pick = await pickValidBillingDateFromBillingCommonWithinAccountingPeriod(
          Request,
          signedStarts[1],
          addDaysIso(signedStarts[1], 60),
        );
        const payload = await buildPdt2599BillingRunPayload(Request, Responses.serviceContract, ['PER_PIECE'], {
          taxEventDate: w2Pick,
          invoiceDate: w2Pick,
        });

        const br = await Request.post(Endpoints.billingRun, {
          data: alignPerPieceSecondSignedWindowBillingPayload(payload as unknown as Record<string, unknown>, w2Pick),
        });
        await expect(br).CheckResponse();
        const brid = asBillingRunId(await br.json());
        const { draftInvoiceCount } = await pollPerPieceBillingRunUntilDraftWithInvoices(
          Request,
          Endpoints.billingRun,
          brid,
          { minimumInvoices: 1 },
        );
        expect(draftInvoiceCount).toBeGreaterThan(0);

        const iid = await getFirstDraftInvoiceStubId(Request, Endpoints.billingRun, brid);
        const iv = await Request.get(`invoice?id=${iid}`);
        await expect(iv).CheckResponse();

        const invJson = (await iv.json()) as Record<string, unknown>;
        const v2base = await readPdt2599ContractVersionQuantityAndFormulaValue(Request, Endpoints, contractId, 2);
        expect(v2base.quantity, 'TC-BE-25').not.toBeNull();
        expect(v2base.formulaValue, 'TC-BE-25').not.toBeNull();
        assertPdt2599InvoiceNetMatchesExpected(
          invJson,
          pdt2599ExpectedPerPieceNet(v2base.quantity!, v2base.formulaValue!),
          'TC-BE-25 PER_PIECE second Signed window (net ex-VAT = quantity × formula value)',
        );
        pdt2599AttachLinkedVersionIdDebug(invJson, 'TC-BE-25');
        attachPdt2599CreatedEntitiesForBilling(Responses, contractId, brid);
      });
    });

    test('[PDT-2599] TC-BE-26: PER_PIECE — billing after Draft v3 start — links latest Signed v2', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
    }) => {
      const fixtures = { Request, GeneratePayload, Responses, Endpoints };
      const chain = await runPdt2599SignedServiceContractBillingChain(fixtures, 'PER_PIECE');
      const contractId = chain.contractId;
      /** Distinct from `PDT2599_PER_PIECE_V1_QUANTITY` / `PDT2599_PER_PIECE_V2_QUANTITY`; within default PER_PIECE tier [1, 100]. */
      const v3DraftQuantity = 45;

      await test.step('Precondition: v1/v2 Signed + distinct quantities; v3 Draft + distinct quantity', async () => {
        await applyPdt2599BillingTwoSignedDriverGraph(
          Request,
          Endpoints,
          contractId,
          PDT2599_PER_PIECE_PRIMARY_DRIVER,
          PDT2599_PER_PIECE_SECONDARY_DRIVER,
          {
            fallbackPriceComponentIds: pdt2599ResponsesPriceComponentIds(Responses),
            signedVersionQuantities: { v1: PDT2599_PER_PIECE_V1_QUANTITY, v2: PDT2599_PER_PIECE_V2_QUANTITY },
          },
        );

        let detail = await loadServiceContractDetailForEdit(Request, Endpoints, contractId);
        let signedStarts = pdtSortedSignedStartDates(await loadServiceContractDetail(Request, Endpoints, contractId));
        expect(signedStarts.length).toBeGreaterThanOrEqual(2);
        let v2Start = signedStarts[1];
        const apId = Number(envVariables.accounting_period);
        let apHi: string | undefined;
        if (Number.isFinite(apId)) {
          const apRes = await Request.get(`accounting-period/${apId}`);
          await expect(apRes).CheckResponse();
          const apJson = (await apRes.json()) as { endDate?: string };
          apHi = apJson.endDate?.slice(0, 10);
          if (apHi) {
            const maxV2StartForV3Billing = addDaysIso(apHi, -2);
            if (v2Start > maxV2StartForV3Billing) {
              const v1Start = signedStarts[0];
              const targetV2 = maxV2StartForV3Billing;
              const minV2AfterV1 = addDaysIso(v1Start, 1);
              if (targetV2 < minV2AfterV1) {
                throw new Error(
                  `[PDT-2599 TC-BE-26] Cannot place Draft v3 + billing inside AP ending ${apHi}: Signed v1=${v1Start}, v2=${v2Start} (apId=${apId}).`,
                );
              }
              const d2 = await loadServiceContractDetailForEdit(Request, Endpoints, contractId, 2);
              await expect(
                await Request.put(`${Endpoints.serviceContract}/${contractId}`, {
                  params: { versionId: 2 },
                  data: buildServiceContractEditPayloadFromDetail(d2, {
                    savingAsNewVersion: false,
                    startDate: targetV2,
                    contractVersionStatus: 'SIGNED',
                  }),
                }),
              ).CheckResponse();
              signedStarts = pdtSortedSignedStartDates(await loadServiceContractDetail(Request, Endpoints, contractId));
              v2Start = signedStarts[1];
            }
          }
        }

        const minV3Start = addDaysIso(v2Start, 1);
        let v3Start = addDaysIso(v2Start, 25);
        if (apHi) {
          const maxV3Start = addDaysIso(apHi, -1);
          if (minV3Start > maxV3Start) {
            throw new Error(
              `[PDT-2599 TC-BE-26] No calendar room for Draft v3 after v2=${v2Start} before AP end ${apHi}.`,
            );
          }
          if (v3Start > maxV3Start) v3Start = maxV3Start;
          if (v3Start < minV3Start) v3Start = minV3Start;
        }

        detail = await loadServiceContractDetailForEdit(Request, Endpoints, contractId);
        const pV3 = buildServiceContractEditPayloadFromDetail(detail, {
          savingAsNewVersion: true,
          startDate: v3Start,
          contractVersionStatus: 'DRAFT',
        });
        const sp3 = pV3.serviceParameters as {
          quantity?: number | null;
          contractFormulas: { formulaVariableId: number; value: number }[];
        };
        sp3.quantity = v3DraftQuantity;

        await expect(
          await Request.put(`${Endpoints.serviceContract}/${contractId}`, {
            params: { versionId: pickLatestLogicalVersionId(detail.versions as { versionId: number }[]) },
            data: { ...pV3, serviceParameters: sp3 },
          }),
        ).CheckResponse();

        const verify = await loadServiceContractDetail(Request, Endpoints, contractId);
        const rows = (verify.versions as { versionId: number; status?: string }[]) ?? [];
        expect(rows.length).toBe(3);
        const v3row = rows.find((v) => v.versionId === 3);
        expect(v3row).toBeTruthy();
        expect(isSignedContractVersionStatus(v3row!.status)).toBe(false);

        for (const [versionId, expectedQty] of [
          [1, PDT2599_PER_PIECE_V1_QUANTITY],
          [2, PDT2599_PER_PIECE_V2_QUANTITY],
          [3, v3DraftQuantity],
        ] as const) {
          const dv = await loadServiceContractDetail(Request, Endpoints, contractId, versionId);
          const q = Number((dv.serviceParameters as { quantity?: unknown } | undefined)?.quantity);
          expect(q).toBe(expectedQty);
        }

      });

      await test.step('POST billing-run: invoiceDate / taxEventDate after v3 start → draft invoices; linkage v2 when exposed', async () => {
        const detailRo = await loadServiceContractDetail(Request, Endpoints, contractId);
        const v3Row = ((detailRo.versions as { versionId: number; startDate?: string }[]) ?? []).find((v) => v.versionId === 3);
        expect(v3Row?.startDate).toBeTruthy();
        const v3StartIso = String(v3Row!.startDate);

        let billingPick = await pickValidBillingDateFromBillingCommonWithinAccountingPeriod(
          Request,
          addDaysIso(v3StartIso, 1),
          addDaysIso(v3StartIso, 120),
        );
        const apForBill = Number(envVariables.accounting_period);
        if (Number.isFinite(apForBill)) {
          const apBillRes = await Request.get(`accounting-period/${apForBill}`);
          await expect(apBillRes).CheckResponse();
          const apBillEnd = ((await apBillRes.json()) as { endDate?: string }).endDate?.slice(0, 10);
          if (apBillEnd && billingPick > apBillEnd) {
            billingPick = apBillEnd;
          }
        }
        expect(billingPick > v3StartIso).toBe(true);

        const payload = await buildPdt2599BillingRunPayload(Request, Responses.serviceContract, ['PER_PIECE'], {
          taxEventDate: billingPick,
          invoiceDate: billingPick,
        });

        const br = await Request.post(Endpoints.billingRun, {
          data: alignPerPieceSecondSignedWindowBillingPayload(payload as unknown as Record<string, unknown>, billingPick),
        });
        await expect(br).CheckResponse();
        const brid = asBillingRunId(await br.json());
        const { draftInvoiceCount } = await pollPerPieceBillingRunUntilDraftWithInvoices(
          Request,
          Endpoints.billingRun,
          brid,
          { minimumInvoices: 1 },
        );
        expect(draftInvoiceCount).toBeGreaterThan(0);

        const iid = await getFirstDraftInvoiceStubId(Request, Endpoints.billingRun, brid);
        const iv = await Request.get(`invoice?id=${iid}`);
        await expect(iv).CheckResponse();

        const invJson = (await iv.json()) as Record<string, unknown>;
        const v2base = await readPdt2599ContractVersionQuantityAndFormulaValue(Request, Endpoints, contractId, 2);
        expect(v2base.quantity, 'TC-BE-26').not.toBeNull();
        expect(v2base.formulaValue, 'TC-BE-26').not.toBeNull();
        assertPdt2599InvoiceNetMatchesExpected(
          invJson,
          pdt2599ExpectedPerPieceNet(v2base.quantity!, v2base.formulaValue!),
          'TC-BE-26 PER_PIECE billing after Draft v3: invoice must follow latest Signed v2 (not Draft v3 quantity/driver)',
        );
        pdt2599AttachLinkedVersionIdDebug(invJson, 'TC-BE-26');
        attachPdt2599CreatedEntitiesForBilling(Responses, contractId, brid);
      });
    });
  });

  test.describe('[PDT-2599] OVER_TIME_ONE_TIME — billing', { tag: '@contractsAndOrders' }, () => {
    test.describe.configure({ mode: 'serial' });
    test.setTimeout(540_000);

    test('[PDT-2599] TC-BE-27: OVER_TIME_ONE_TIME — first Signed window — invoices issued (distinct drivers v1/v2)', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
    }) => {
      const fixtures = { Request, GeneratePayload, Responses, Endpoints };
      const chain = await runPdt2599SignedServiceContractBillingChain(fixtures, 'OVER_TIME_ONE_TIME');
      const contractId = chain.contractId;

      await test.step('Precondition: tariff drivers diverge across Signed boundaries (one-time price component)', async () => {
        await applyPdt2599BillingTwoSignedDriverGraph(
          Request,
          Endpoints,
          contractId,
          PDT2599_OVER_TIME_ONE_TIME_PRIMARY_DRIVER,
          PDT2599_OVER_TIME_ONE_TIME_SECONDARY_DRIVER,
          { fallbackPriceComponentIds: pdt2599ResponsesPriceComponentIds(Responses) },
        );
      });

      await test.step('POST first-window billing-run + drafts', async () => {
        const detailRo = await loadServiceContractDetail(Request, Endpoints, contractId);
        const signedStarts = pdtSortedSignedStartDates(detailRo);
        expect(signedStarts.length).toBeGreaterThanOrEqual(2);
        const w1Pick = pickValidBillingDateFromBillingCommon(signedStarts[0], signedStarts[1]);

        const payload = await buildPdt2599BillingRunPayload(Request, Responses.serviceContract, ['OVER_TIME_ONE_TIME'], {
          taxEventDate: w1Pick,
          invoiceDate: w1Pick,
        });

        const br = await Request.post(Endpoints.billingRun, { data: payload });
        await expect(br).CheckResponse();
        const brid = asBillingRunId(await br.json());
        const { draftInvoiceCount } = await pollPerPieceBillingRunUntilDraftWithInvoices(
          Request,
          Endpoints.billingRun,
          brid,
          { minimumInvoices: 1 },
        );
        expect(draftInvoiceCount).toBeGreaterThan(0);

        const iid = await getFirstDraftInvoiceStubId(Request, Endpoints.billingRun, brid);
        const iv = await Request.get(`invoice?id=${iid}`);
        await expect(iv).CheckResponse();

        const invJson = (await iv.json()) as Record<string, unknown>;
        const billingPcId = asPriceComponentId(Responses.priceComponent[0]);
        await assertPdt2599InvoiceLinksToExpectedContractVersionStrict(invJson, Request, Endpoints, contractId, 1, 'TC-BE-27');
        await assertPdt2599InvoiceBilledAmountMatchesSignedVersionPriceComponent(
          Request,
          Endpoints,
          iid,
          contractId,
          1,
          PDT2599_OVER_TIME_ONE_TIME_PRIMARY_DRIVER,
          'TC-BE-27 OVER_TIME_ONE_TIME first Signed window (invoice line vs signed v1 price component)',
          { priceComponentId: billingPcId },
        );
        pdt2599AttachLinkedVersionIdDebug(invJson, 'TC-BE-27');
        attachPdt2599CreatedEntitiesForBilling(Responses, contractId, brid);
      });
    });

    test('[PDT-2599] TC-BE-28: OVER_TIME_ONE_TIME — second Signed window — billing aligns second driver', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
    }) => {
      const fixtures = { Request, GeneratePayload, Responses, Endpoints };
      const chain = await runPdt2599SignedServiceContractBillingChain(fixtures, 'OVER_TIME_ONE_TIME');
      const contractId = chain.contractId;

      await test.step('Precondition: tariff drivers diverge across Signed boundaries (one-time price component)', async () => {
        await applyPdt2599BillingTwoSignedDriverGraph(
          Request,
          Endpoints,
          contractId,
          PDT2599_OVER_TIME_ONE_TIME_PRIMARY_DRIVER,
          PDT2599_OVER_TIME_ONE_TIME_SECONDARY_DRIVER,
          { fallbackPriceComponentIds: pdt2599ResponsesPriceComponentIds(Responses) },
        );
      });

      await test.step('POST second Signed window billing-run + drafts', async () => {
        const detailRo = await loadServiceContractDetail(Request, Endpoints, contractId);
        const signedStarts = pdtSortedSignedStartDates(detailRo);
        expect(signedStarts.length).toBeGreaterThanOrEqual(2);

        const w2Pick = await pickValidBillingDateFromBillingCommonWithinAccountingPeriod(
          Request,
          signedStarts[1],
          addDaysIso(signedStarts[1], 60),
        );
        const payload = await buildPdt2599BillingRunPayload(Request, Responses.serviceContract, ['OVER_TIME_ONE_TIME'], {
          taxEventDate: w2Pick,
          invoiceDate: w2Pick,
        });

        const br = await Request.post(Endpoints.billingRun, {
          data: alignPerPieceSecondSignedWindowBillingPayload(payload as unknown as Record<string, unknown>, w2Pick),
        });
        await expect(br).CheckResponse();
        const brid = asBillingRunId(await br.json());
        const { draftInvoiceCount } = await pollPerPieceBillingRunUntilDraftWithInvoices(
          Request,
          Endpoints.billingRun,
          brid,
          { minimumInvoices: 1 },
        );
        expect(draftInvoiceCount).toBeGreaterThan(0);

        const iid = await getFirstDraftInvoiceStubId(Request, Endpoints.billingRun, brid);
        const iv = await Request.get(`invoice?id=${iid}`);
        await expect(iv).CheckResponse();

        const invJson = (await iv.json()) as Record<string, unknown>;
        const billingPcId = asPriceComponentId(Responses.priceComponent[0]);
        await assertPdt2599InvoiceLinksToExpectedContractVersionStrict(invJson, Request, Endpoints, contractId, 2, 'TC-BE-28');
        await assertPdt2599InvoiceBilledAmountMatchesSignedVersionPriceComponent(
          Request,
          Endpoints,
          iid,
          contractId,
          2,
          PDT2599_OVER_TIME_ONE_TIME_SECONDARY_DRIVER,
          'TC-BE-28 OVER_TIME_ONE_TIME second Signed window (invoice line vs signed v2 price component)',
          { priceComponentId: billingPcId },
        );
        pdt2599AttachLinkedVersionIdDebug(invJson, 'TC-BE-28');
        attachPdt2599CreatedEntitiesForBilling(Responses, contractId, brid);
      });
    });

    test('[PDT-2599] TC-BE-29: OVER_TIME_ONE_TIME — billing after Draft v3 start — links latest Signed v2', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
    }) => {
      const fixtures = { Request, GeneratePayload, Responses, Endpoints };
      const chain = await runPdt2599SignedServiceContractBillingChain(fixtures, 'OVER_TIME_ONE_TIME');
      const contractId = chain.contractId;

      await test.step('Precondition: v1/v2 Signed + distinct drivers; v3 Draft (no PER_PIECE quantity axis)', async () => {
        await applyPdt2599BillingTwoSignedDriverGraph(
          Request,
          Endpoints,
          contractId,
          PDT2599_OVER_TIME_ONE_TIME_PRIMARY_DRIVER,
          PDT2599_OVER_TIME_ONE_TIME_SECONDARY_DRIVER,
          { fallbackPriceComponentIds: pdt2599ResponsesPriceComponentIds(Responses) },
        );

        let detail = await loadServiceContractDetailForEdit(Request, Endpoints, contractId);
        let signedStarts = pdtSortedSignedStartDates(await loadServiceContractDetail(Request, Endpoints, contractId));
        expect(signedStarts.length).toBeGreaterThanOrEqual(2);
        let v2Start = signedStarts[1];
        const apId = Number(envVariables.accounting_period);
        let apHi: string | undefined;
        if (Number.isFinite(apId)) {
          const apRes = await Request.get(`accounting-period/${apId}`);
          await expect(apRes).CheckResponse();
          const apJson = (await apRes.json()) as { endDate?: string };
          apHi = apJson.endDate?.slice(0, 10);
          if (apHi) {
            const maxV2StartForV3Billing = addDaysIso(apHi, -2);
            if (v2Start > maxV2StartForV3Billing) {
              const v1Start = signedStarts[0];
              const targetV2 = maxV2StartForV3Billing;
              const minV2AfterV1 = addDaysIso(v1Start, 1);
              if (targetV2 < minV2AfterV1) {
                throw new Error(
                  `[PDT-2599 TC-BE-29] Cannot place Draft v3 + billing inside AP ending ${apHi}: Signed v1=${v1Start}, v2=${v2Start} (apId=${apId}).`,
                );
              }
              const d2 = await loadServiceContractDetailForEdit(Request, Endpoints, contractId, 2);
              await expect(
                await Request.put(`${Endpoints.serviceContract}/${contractId}`, {
                  params: { versionId: 2 },
                  data: buildServiceContractEditPayloadFromDetail(d2, {
                    savingAsNewVersion: false,
                    startDate: targetV2,
                    contractVersionStatus: 'SIGNED',
                  }),
                }),
              ).CheckResponse();
              signedStarts = pdtSortedSignedStartDates(await loadServiceContractDetail(Request, Endpoints, contractId));
              v2Start = signedStarts[1];
            }
          }
        }

        const minV3Start = addDaysIso(v2Start, 1);
        let v3Start = addDaysIso(v2Start, 25);
        if (apHi) {
          const maxV3Start = addDaysIso(apHi, -1);
          if (minV3Start > maxV3Start) {
            throw new Error(
              `[PDT-2599 TC-BE-29] No calendar room for Draft v3 after v2=${v2Start} before AP end ${apHi}.`,
            );
          }
          if (v3Start > maxV3Start) v3Start = maxV3Start;
          if (v3Start < minV3Start) v3Start = minV3Start;
        }

        detail = await loadServiceContractDetailForEdit(Request, Endpoints, contractId);
        const pV3 = buildServiceContractEditPayloadFromDetail(detail, {
          savingAsNewVersion: true,
          startDate: v3Start,
          contractVersionStatus: 'DRAFT',
        });
        const sp3 = pV3.serviceParameters as { contractFormulas: { formulaVariableId: number; value: number }[] };
        let fvRow29 = sp3.contractFormulas?.[0];
        if (fvRow29?.formulaVariableId == null || !Number.isFinite(fvRow29.formulaVariableId)) {
          let fvid: number | null = null;
          try {
            fvid = await readPdt2599BillingPriceComponentPrimaryFormulaVariableId(
              Request,
              Endpoints,
              asPriceComponentId(Responses.priceComponent[0]),
            );
          } catch {
            const d2 = await loadServiceContractDetailForEdit(Request, Endpoints, contractId, 2);
            fvid = extractFirstContractPriceComponentFormulaVariableId(d2);
            if (fvid == null) {
              const sid = (d2.basicParameters as { serviceDetailId?: number } | undefined)?.serviceDetailId;
              if (sid) {
                const tRes = await Request.get(`${Endpoints.serviceContract}/third-tab-fields`, {
                  params: { serviceDetailId: sid },
                });
                await expect(tRes).CheckResponse();
                const thirdFresh = (await tRes.json()) as Record<string, unknown>;
                const depth = PDT2599_FORMULA_VARIABLE_DEEP_SEARCH_MAX_DEPTH;
                const rejectIds = pdt2599RejectIdsForFormulaDeepSearch(d2);
                fvid =
                  findFirstNumericFormulaVariableIdDeep(thirdFresh, depth, rejectIds) ?? null;
              }
            }
          }
          expect(fvid, 'TC-BE-29 Draft v3 formulaVariableId').toBeTruthy();
          sp3.contractFormulas = [{ formulaVariableId: fvid!, value: 0 }];
          fvRow29 = sp3.contractFormulas[0];
        }
        expect(fvRow29?.formulaVariableId, 'TC-BE-29 Draft v3 formula row').toBeTruthy();
        sp3.contractFormulas = applyPdt2599PerPieceDriversToServiceParameters(
          sp3.contractFormulas,
          fvRow29!.formulaVariableId,
          PDT2599_OVER_TIME_ONE_TIME_TERTIARY_DRIVER,
        );

        await expect(
          await Request.put(`${Endpoints.serviceContract}/${contractId}`, {
            params: { versionId: pickLatestLogicalVersionId(detail.versions as { versionId: number }[]) },
            data: { ...pV3, serviceParameters: sp3 },
          }),
        ).CheckResponse();

        const verify = await loadServiceContractDetail(Request, Endpoints, contractId);
        const rows = (verify.versions as { versionId: number; status?: string }[]) ?? [];
        expect(rows.length).toBe(3);
        const v3row = rows.find((v) => v.versionId === 3);
        expect(v3row).toBeTruthy();
        expect(isSignedContractVersionStatus(v3row!.status)).toBe(false);

      });

      await test.step('POST billing-run: invoiceDate / taxEventDate after v3 start → draft invoices; linkage v2 when exposed', async () => {
        const detailRo = await loadServiceContractDetail(Request, Endpoints, contractId);
        const v3Row = ((detailRo.versions as { versionId: number; startDate?: string }[]) ?? []).find((v) => v.versionId === 3);
        expect(v3Row?.startDate).toBeTruthy();
        const v3StartIso = String(v3Row!.startDate);

        let billingPick = await pickValidBillingDateFromBillingCommonWithinAccountingPeriod(
          Request,
          addDaysIso(v3StartIso, 1),
          addDaysIso(v3StartIso, 120),
        );
        const apForBill = Number(envVariables.accounting_period);
        if (Number.isFinite(apForBill)) {
          const apBillRes = await Request.get(`accounting-period/${apForBill}`);
          await expect(apBillRes).CheckResponse();
          const apBillEnd = ((await apBillRes.json()) as { endDate?: string }).endDate?.slice(0, 10);
          if (apBillEnd && billingPick > apBillEnd) {
            billingPick = apBillEnd;
          }
        }
        expect(billingPick > v3StartIso).toBe(true);

        const payload = await buildPdt2599BillingRunPayload(Request, Responses.serviceContract, ['OVER_TIME_ONE_TIME'], {
          taxEventDate: billingPick,
          invoiceDate: billingPick,
        });

        const br = await Request.post(Endpoints.billingRun, {
          data: alignPerPieceSecondSignedWindowBillingPayload(payload as unknown as Record<string, unknown>, billingPick),
        });
        await expect(br).CheckResponse();
        const brid = asBillingRunId(await br.json());
        const { draftInvoiceCount } = await pollPerPieceBillingRunUntilDraftWithInvoices(
          Request,
          Endpoints.billingRun,
          brid,
          { minimumInvoices: 1 },
        );
        expect(draftInvoiceCount).toBeGreaterThan(0);

        const iid = await getFirstDraftInvoiceStubId(Request, Endpoints.billingRun, brid);
        const iv = await Request.get(`invoice?id=${iid}`);
        await expect(iv).CheckResponse();

        const invJson = (await iv.json()) as Record<string, unknown>;
        const billingPcId29 = asPriceComponentId(Responses.priceComponent[0]);
        const rateSignedV2 = await readPdt2599SignedVersionBillingRateScalar(
          Request,
          Endpoints,
          contractId,
          2,
          billingPcId29,
          PDT2599_OVER_TIME_ONE_TIME_SECONDARY_DRIVER,
        );
        expect(rateSignedV2, 'TC-BE-29: Signed v2 tariff must not be Draft v3 tertiary driver').not.toBe(
          PDT2599_OVER_TIME_ONE_TIME_TERTIARY_DRIVER,
        );
        await assertPdt2599InvoiceLinksToExpectedContractVersionStrict(invJson, Request, Endpoints, contractId, 2, 'TC-BE-29');
        await assertPdt2599InvoiceBilledAmountMatchesSignedVersionPriceComponent(
          Request,
          Endpoints,
          iid,
          contractId,
          2,
          PDT2599_OVER_TIME_ONE_TIME_SECONDARY_DRIVER,
          'TC-BE-29 OVER_TIME_ONE_TIME after Draft v3 (invoice line vs signed v2 price component, not Draft v3)',
          { priceComponentId: billingPcId29 },
        );
        pdt2599AttachLinkedVersionIdDebug(invJson, 'TC-BE-29');
        attachPdt2599CreatedEntitiesForBilling(Responses, contractId, brid);
      });
    });
  });

  test.describe('[PDT-2599] OVER_TIME_PERIODICAL — billing', { tag: '@contractsAndOrders' }, () => {
    test.describe.configure({ mode: 'serial' });
    test.setTimeout(540_000);

    test('[PDT-2599] TC-BE-30: OVER_TIME_PERIODICAL — first Signed window — invoices issued (distinct drivers v1/v2)', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
    }) => {
      const fixtures = { Request, GeneratePayload, Responses, Endpoints };
      const chain = await runPdt2599SignedServiceContractBillingChain(fixtures, 'OVER_TIME_PERIODICAL');
      const contractId = chain.contractId;

      await test.step('Precondition: tariff drivers diverge across Signed boundaries (periodical price component)', async () => {
        await applyPdt2599BillingTwoSignedDriverGraph(
          Request,
          Endpoints,
          contractId,
          PDT2599_OVER_TIME_PERIODICAL_PRIMARY_DRIVER,
          PDT2599_OVER_TIME_PERIODICAL_SECONDARY_DRIVER,
          { fallbackPriceComponentIds: pdt2599ResponsesPriceComponentIds(Responses) },
        );
      });

      await test.step('POST first-window billing-run + drafts', async () => {
        const detailRo = await loadServiceContractDetail(Request, Endpoints, contractId);
        const signedStarts = pdtSortedSignedStartDates(detailRo);
        expect(signedStarts.length).toBeGreaterThanOrEqual(2);
        const w1Pick = pickValidBillingDateFromBillingCommon(signedStarts[0], signedStarts[1]);

        const payload = await buildPdt2599BillingRunPayload(Request, Responses.serviceContract, ['OVER_TIME_PERIODICAL'], {
          taxEventDate: w1Pick,
          invoiceDate: w1Pick,
        });

        const br = await Request.post(Endpoints.billingRun, { data: payload });
        await expect(br).CheckResponse();
        const brid = asBillingRunId(await br.json());
        const { draftInvoiceCount } = await pollPerPieceBillingRunUntilDraftWithInvoices(
          Request,
          Endpoints.billingRun,
          brid,
          { minimumInvoices: 1 },
        );
        expect(draftInvoiceCount).toBeGreaterThan(0);

        const iid = await getFirstDraftInvoiceStubId(Request, Endpoints.billingRun, brid);
        const iv = await Request.get(`invoice?id=${iid}`);
        await expect(iv).CheckResponse();

        const invJson = (await iv.json()) as Record<string, unknown>;
        const billingPcId = asPriceComponentId(Responses.priceComponent[0]);
        await assertPdt2599InvoiceLinksToExpectedContractVersionStrict(invJson, Request, Endpoints, contractId, 1, 'TC-BE-30');
        await assertPdt2599InvoiceBilledAmountMatchesSignedVersionPriceComponent(
          Request,
          Endpoints,
          iid,
          contractId,
          1,
          PDT2599_OVER_TIME_PERIODICAL_PRIMARY_DRIVER,
          'TC-BE-30 OVER_TIME_PERIODICAL first Signed window (invoice line vs signed v1 price component)',
          { priceComponentId: billingPcId },
        );
        pdt2599AttachLinkedVersionIdDebug(invJson, 'TC-BE-30');
        attachPdt2599CreatedEntitiesForBilling(Responses, contractId, brid);
      });
    });

    test('[PDT-2599] TC-BE-31: OVER_TIME_PERIODICAL — second Signed window — billing aligns second driver', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
    }) => {
      const fixtures = { Request, GeneratePayload, Responses, Endpoints };
      const chain = await runPdt2599SignedServiceContractBillingChain(fixtures, 'OVER_TIME_PERIODICAL');
      const contractId = chain.contractId;

      await test.step('Precondition: tariff drivers diverge across Signed boundaries (periodical price component)', async () => {
        await applyPdt2599BillingTwoSignedDriverGraph(
          Request,
          Endpoints,
          contractId,
          PDT2599_OVER_TIME_PERIODICAL_PRIMARY_DRIVER,
          PDT2599_OVER_TIME_PERIODICAL_SECONDARY_DRIVER,
          { fallbackPriceComponentIds: pdt2599ResponsesPriceComponentIds(Responses) },
        );
      });

      await test.step('POST second Signed window billing-run + drafts', async () => {
        const detailRo = await loadServiceContractDetail(Request, Endpoints, contractId);
        const signedStarts = pdtSortedSignedStartDates(detailRo);
        expect(signedStarts.length).toBeGreaterThanOrEqual(2);

        const w2Pick = await pickValidBillingDateFromBillingCommonWithinAccountingPeriod(
          Request,
          signedStarts[1],
          addDaysIso(signedStarts[1], 60),
        );
        const payload = await buildPdt2599BillingRunPayload(Request, Responses.serviceContract, ['OVER_TIME_PERIODICAL'], {
          taxEventDate: w2Pick,
          invoiceDate: w2Pick,
        });

        const br = await Request.post(Endpoints.billingRun, {
          data: alignPerPieceSecondSignedWindowBillingPayload(payload as unknown as Record<string, unknown>, w2Pick),
        });
        await expect(br).CheckResponse();
        const brid = asBillingRunId(await br.json());
        const { draftInvoiceCount } = await pollPerPieceBillingRunUntilDraftWithInvoices(
          Request,
          Endpoints.billingRun,
          brid,
          { minimumInvoices: 1 },
        );
        expect(draftInvoiceCount).toBeGreaterThan(0);

        const iid = await getFirstDraftInvoiceStubId(Request, Endpoints.billingRun, brid);
        const iv = await Request.get(`invoice?id=${iid}`);
        await expect(iv).CheckResponse();

        const invJson = (await iv.json()) as Record<string, unknown>;
        const v2base = await readPdt2599ContractVersionQuantityAndFormulaValue(Request, Endpoints, contractId, 2);
        expect(v2base.formulaValue, 'TC-BE-31').not.toBeNull();
        assertPdt2599InvoiceNetMatchesExpected(
          invJson,
          pdt2599ExpectedOverTimeNet(v2base.formulaValue!),
          'TC-BE-31 OVER_TIME_PERIODICAL second Signed window (net ex-VAT = contract formula value)',
        );
        pdt2599AttachLinkedVersionIdDebug(invJson, 'TC-BE-31');
        attachPdt2599CreatedEntitiesForBilling(Responses, contractId, brid);
      });
    });

    test('[PDT-2599] TC-BE-32: OVER_TIME_PERIODICAL — billing after Draft v3 start — links latest Signed v2', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
    }) => {
      const fixtures = { Request, GeneratePayload, Responses, Endpoints };
      const chain = await runPdt2599SignedServiceContractBillingChain(fixtures, 'OVER_TIME_PERIODICAL');
      const contractId = chain.contractId;

      await test.step('Precondition: v1/v2 Signed + distinct drivers; v3 Draft', async () => {
        await applyPdt2599BillingTwoSignedDriverGraph(
          Request,
          Endpoints,
          contractId,
          PDT2599_OVER_TIME_PERIODICAL_PRIMARY_DRIVER,
          PDT2599_OVER_TIME_PERIODICAL_SECONDARY_DRIVER,
          { fallbackPriceComponentIds: pdt2599ResponsesPriceComponentIds(Responses) },
        );

        let detail = await loadServiceContractDetailForEdit(Request, Endpoints, contractId);
        let signedStarts = pdtSortedSignedStartDates(await loadServiceContractDetail(Request, Endpoints, contractId));
        expect(signedStarts.length).toBeGreaterThanOrEqual(2);
        let v2Start = signedStarts[1];
        const apId = Number(envVariables.accounting_period);
        let apHi: string | undefined;
        if (Number.isFinite(apId)) {
          const apRes = await Request.get(`accounting-period/${apId}`);
          await expect(apRes).CheckResponse();
          const apJson = (await apRes.json()) as { endDate?: string };
          apHi = apJson.endDate?.slice(0, 10);
          if (apHi) {
            const maxV2StartForV3Billing = addDaysIso(apHi, -2);
            if (v2Start > maxV2StartForV3Billing) {
              const v1Start = signedStarts[0];
              const targetV2 = maxV2StartForV3Billing;
              const minV2AfterV1 = addDaysIso(v1Start, 1);
              if (targetV2 < minV2AfterV1) {
                throw new Error(
                  `[PDT-2599 TC-BE-32] Cannot place Draft v3 + billing inside AP ending ${apHi}: Signed v1=${v1Start}, v2=${v2Start} (apId=${apId}).`,
                );
              }
              const d2 = await loadServiceContractDetailForEdit(Request, Endpoints, contractId, 2);
              await expect(
                await Request.put(`${Endpoints.serviceContract}/${contractId}`, {
                  params: { versionId: 2 },
                  data: buildServiceContractEditPayloadFromDetail(d2, {
                    savingAsNewVersion: false,
                    startDate: targetV2,
                    contractVersionStatus: 'SIGNED',
                  }),
                }),
              ).CheckResponse();
              signedStarts = pdtSortedSignedStartDates(await loadServiceContractDetail(Request, Endpoints, contractId));
              v2Start = signedStarts[1];
            }
          }
        }

        const minV3Start = addDaysIso(v2Start, 1);
        let v3Start = addDaysIso(v2Start, 25);
        if (apHi) {
          const maxV3Start = addDaysIso(apHi, -1);
          if (minV3Start > maxV3Start) {
            throw new Error(
              `[PDT-2599 TC-BE-32] No calendar room for Draft v3 after v2=${v2Start} before AP end ${apHi}.`,
            );
          }
          if (v3Start > maxV3Start) v3Start = maxV3Start;
          if (v3Start < minV3Start) v3Start = minV3Start;
        }

        detail = await loadServiceContractDetailForEdit(Request, Endpoints, contractId);
        const pV3 = buildServiceContractEditPayloadFromDetail(detail, {
          savingAsNewVersion: true,
          startDate: v3Start,
          contractVersionStatus: 'DRAFT',
        });
        const sp3 = pV3.serviceParameters as { contractFormulas: { formulaVariableId: number; value: number }[] };
        const fvRow32 = sp3.contractFormulas?.[0];
        expect(fvRow32?.formulaVariableId, 'TC-BE-32 Draft v3 formula row').toBeTruthy();
        sp3.contractFormulas = applyPdt2599PerPieceDriversToServiceParameters(
          sp3.contractFormulas,
          fvRow32!.formulaVariableId,
          PDT2599_OVER_TIME_PERIODICAL_TERTIARY_DRIVER,
        );

        await expect(
          await Request.put(`${Endpoints.serviceContract}/${contractId}`, {
            params: { versionId: pickLatestLogicalVersionId(detail.versions as { versionId: number }[]) },
            data: { ...pV3, serviceParameters: sp3 },
          }),
        ).CheckResponse();

        const verify = await loadServiceContractDetail(Request, Endpoints, contractId);
        const rows = (verify.versions as { versionId: number; status?: string }[]) ?? [];
        expect(rows.length).toBe(3);
        const v3row = rows.find((v) => v.versionId === 3);
        expect(v3row).toBeTruthy();
        expect(isSignedContractVersionStatus(v3row!.status)).toBe(false);

      });

      await test.step('POST billing-run: invoiceDate / taxEventDate after v3 start → draft invoices; linkage v2 when exposed', async () => {
        const detailRo = await loadServiceContractDetail(Request, Endpoints, contractId);
        const v3Row = ((detailRo.versions as { versionId: number; startDate?: string }[]) ?? []).find((v) => v.versionId === 3);
        expect(v3Row?.startDate).toBeTruthy();
        const v3StartIso = String(v3Row!.startDate);

        let billingPick = await pickValidBillingDateFromBillingCommonWithinAccountingPeriod(
          Request,
          addDaysIso(v3StartIso, 1),
          addDaysIso(v3StartIso, 120),
        );
        const apForBill = Number(envVariables.accounting_period);
        if (Number.isFinite(apForBill)) {
          const apBillRes = await Request.get(`accounting-period/${apForBill}`);
          await expect(apBillRes).CheckResponse();
          const apBillEnd = ((await apBillRes.json()) as { endDate?: string }).endDate?.slice(0, 10);
          if (apBillEnd && billingPick > apBillEnd) {
            billingPick = apBillEnd;
          }
        }
        expect(billingPick > v3StartIso).toBe(true);

        const payload = await buildPdt2599BillingRunPayload(Request, Responses.serviceContract, ['OVER_TIME_PERIODICAL'], {
          taxEventDate: billingPick,
          invoiceDate: billingPick,
        });

        const br = await Request.post(Endpoints.billingRun, {
          data: alignPerPieceSecondSignedWindowBillingPayload(payload as unknown as Record<string, unknown>, billingPick),
        });
        await expect(br).CheckResponse();
        const brid = asBillingRunId(await br.json());
        const { draftInvoiceCount } = await pollPerPieceBillingRunUntilDraftWithInvoices(
          Request,
          Endpoints.billingRun,
          brid,
          { minimumInvoices: 1 },
        );
        expect(draftInvoiceCount).toBeGreaterThan(0);

        const iid = await getFirstDraftInvoiceStubId(Request, Endpoints.billingRun, brid);
        const iv = await Request.get(`invoice?id=${iid}`);
        await expect(iv).CheckResponse();

        const invJson = (await iv.json()) as Record<string, unknown>;
        const v2base = await readPdt2599ContractVersionQuantityAndFormulaValue(Request, Endpoints, contractId, 2);
        expect(v2base.formulaValue, 'TC-BE-32').not.toBeNull();
        expect(v2base.formulaValue, 'TC-BE-32: invoice must not follow Draft v3 tertiary driver').not.toBe(
          PDT2599_OVER_TIME_PERIODICAL_TERTIARY_DRIVER,
        );
        assertPdt2599InvoiceNetMatchesExpected(
          invJson,
          pdt2599ExpectedOverTimeNet(v2base.formulaValue!),
          'TC-BE-32 OVER_TIME_PERIODICAL after Draft v3: billing follows latest Signed v2 (not tertiary Draft v3 price)',
        );
        pdt2599AttachLinkedVersionIdDebug(invJson, 'TC-BE-32');
        attachPdt2599CreatedEntitiesForBilling(Responses, contractId, brid);
      });
    });
  });

test.describe('[PDT-2599] INTERIM — billing', { tag: '@contractsAndOrders' }, () => {
  test.describe.configure({ mode: 'serial' });
  test.setTimeout(540_000);

  /** INTERIM: integer advance amounts per **version** (`56`, `92`, Draft `188`); billing chain POSTs three distinct IAP entities, registers them on the commercial service, and restores ids in `Responses.interim` for PUTs/GET reads. */
  const PDT2599_INTERIM_ADVANCE_AMOUNT_V1_SIGNED = 56;
  const PDT2599_INTERIM_ADVANCE_AMOUNT_V2_SIGNED = 92;
  const PDT2599_INTERIM_ADVANCE_AMOUNT_V3_DRAFT = 188;

  test('[PDT-2599] TC-BE-34: INTERIM — first Signed window — invoices issued (distinct interim amounts v1/v2)', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    const fixtures = { Request, GeneratePayload, Responses, Endpoints };
    const chain = await runPdt2599SignedServiceContractBillingChain(fixtures, 'INTERIM');
    const contractId = chain.contractId;
    /** Fixture seeds **three** distinct IAP entities on the commercial service (56 / 92 / 188); use A/B for Signed v1/v2. */
    expect(Responses.interim.length, 'INTERIM billing chain restores 3 IAP ids').toBe(3);
    const interimIdV1 = asPriceComponentId(Responses.interim[0]);
    const interimIdV2 = asPriceComponentId(Responses.interim[1]);
    expect(interimIdV1, 'TC-BE-34 IAP A').not.toBe(interimIdV2);

    await test.step('Precondition: second Signed slice (formula-neutral — interim contract rejects unrelated formulaVariableId overrides)', async () => {
      const detailRo = await loadServiceContractDetail(Request, Endpoints, contractId);
      const c = String((detailRo.basicParameters as { creationDate?: string }).creationDate);
      const secondSignedStart = await resolvePdt2599SecondSignedSliceStartForOpenAccountingPeriod(Request, c);
      const detail = await loadServiceContractDetailForEdit(Request, Endpoints, contractId);
      const pV2 = buildServiceContractEditPayloadFromDetail(detail, {
        savingAsNewVersion: true,
        startDate: secondSignedStart,
        contractVersionStatus: 'SIGNED',
      });
      await expect(
        await Request.put(`${Endpoints.serviceContract}/${contractId}`, {
          params: { versionId: pickLatestLogicalVersionId(detail.versions as { versionId: number }[]) },
          data: pV2,
        }),
      ).CheckResponse();
    });

    await test.step('Precondition: Signed v1 uses IAP A (+ term); Signed v2 uses IAP B (+ term) — amounts A≠B', async () => {
      const fieldsA = await pdt2599ReadInterimAdvancePaymentContractFields(Request, interimIdV1);
      const fieldsB = await pdt2599ReadInterimAdvancePaymentContractFields(Request, interimIdV2);

      await pdt2599PutServiceContractVersionInterimAdvanceRequests(
        Request,
        Endpoints,
        contractId,
        1,
        false,
        { contractVersionStatus: 'SIGNED' },
        [
          {
            interimAdvancePaymentId: fieldsA.interimAdvancePaymentId,
            termValue: fieldsA.termValue,
            value: PDT2599_INTERIM_ADVANCE_AMOUNT_V1_SIGNED,
          },
        ],
      );

      await pdt2599PutServiceContractVersionInterimAdvanceRequests(
        Request,
        Endpoints,
        contractId,
        2,
        false,
        { contractVersionStatus: 'SIGNED' },
        [
          {
            interimAdvancePaymentId: fieldsB.interimAdvancePaymentId,
            termValue: fieldsB.termValue,
            value: PDT2599_INTERIM_ADVANCE_AMOUNT_V2_SIGNED,
          },
        ],
      );

    });

    await test.step('Assert: GET by versionId shows persisted interim amounts v1=A, v2=B (A≠B)', async () => {
      const a = await pdt2599ReadInterimAdvanceAmountForContractVersion(
        Request,
        Endpoints,
        contractId,
        1,
        interimIdV1,
      );
      const b = await pdt2599ReadInterimAdvanceAmountForContractVersion(
        Request,
        Endpoints,
        contractId,
        2,
        interimIdV2,
      );
      expect(a, 'TC-BE-34 v1 interim amount').toBeCloseTo(PDT2599_INTERIM_ADVANCE_AMOUNT_V1_SIGNED, 2);
      expect(b, 'TC-BE-34 v2 interim amount').toBeCloseTo(PDT2599_INTERIM_ADVANCE_AMOUNT_V2_SIGNED, 2);
      expect(a, 'TC-BE-34 distinct v1/v2 interim').not.toBeCloseTo(b, 2);
    });

    await test.step('POST first-window billing-run + drafts (billing date in AP ∩ [v1Start, v2Start))', async () => {
      const detailRo = await loadServiceContractDetail(Request, Endpoints, contractId);
      const signedStarts = pdtSortedSignedStartDates(detailRo);
      expect(signedStarts.length).toBeGreaterThanOrEqual(2);
      const v1Start = signedStarts[0];
      const v2Start = signedStarts[1];

      /**
       * Last day strictly before v2 start is unambiguously in the first Signed slice.
       * `pickValidBillingDateFromBillingCommon` can return `v2Start` when the window is only 1–2 days wide (see `e <= s + 86_400_000` branch),
       * which breaks “first window” expectations and can yield no INTERIM draft rows.
       */
      const lastDayBeforeV2 = addDaysIso(v2Start, -1);
      expect(lastDayBeforeV2 >= v1Start).toBe(true);
      expect(lastDayBeforeV2 < v2Start).toBe(true);
      const w1Pick = lastDayBeforeV2;

      /** Accounting-period guard (same OPEN period as resolver uses for Signed v2). */
      const apIdGuard = Number(envVariables.accounting_period);
      if (Number.isFinite(apIdGuard)) {
        const apG = await Request.get(`accounting-period/${apIdGuard}`);
        await expect(apG).CheckResponse();
        const apJ = (await apG.json()) as { startDate?: string; endDate?: string };
        const loG = apJ.startDate?.slice(0, 10);
        const hiG = apJ.endDate?.slice(0, 10);
        if (!loG || !hiG) {
          throw new Error(`[PDT-2599 TC-BE-34] accounting-period ${apIdGuard} missing startDate/endDate`);
        }
        expect(w1Pick >= loG && w1Pick <= hiG).toBe(true);
      }

      expect(w1Pick < v2Start).toBe(true);

      const basePayload = await buildPdt2599BillingRunPayload(Request, Responses.serviceContract, ['INTERIM'], {
        taxEventDate: w1Pick,
        invoiceDate: w1Pick,
      });
      basePayload.commonParameters.accountingPeriodId = await resolveAccountingPeriodIdForBillingDate(Request, w1Pick);
      /** INTERIM billing rejects non-empty `maxEndDate` (400: must not be provided); drop template default from `forVolumes()`. */
      delete (basePayload.basicParameters as Record<string, unknown>).maxEndDate;

      const br = await Request.post(Endpoints.billingRun, { data: basePayload });
      await expect(br).CheckResponse();
      const brid = asBillingRunId(await br.json());
      const { draftInvoiceCount } = await pollPerPieceBillingRunUntilDraftWithInvoices(
        Request,
        Endpoints.billingRun,
        brid,
        { minimumInvoices: 1 },
      );
      expect(draftInvoiceCount).toBeGreaterThan(0);

      const iid = await getFirstDraftInvoiceStubId(Request, Endpoints.billingRun, brid);
      const iv = await Request.get(`invoice?id=${iid}`);
      await expect(iv).CheckResponse();

      const invJson = (await iv.json()) as Record<string, unknown>;
      assertPdt2599InvoiceNetMatchesExpected(
        invJson,
        PDT2599_INTERIM_ADVANCE_AMOUNT_V1_SIGNED,
        'TC-BE-34 INTERIM first Signed window (net ex-VAT = interim amount on logical v1)',
      );
      pdt2599AttachLinkedVersionIdDebug(invJson, 'TC-BE-34');
      attachPdt2599CreatedEntitiesForBilling(Responses, contractId, brid, {
        interimAdvancePaymentIdV1: interimIdV1,
        interimAdvancePaymentIdV2: interimIdV2,
      });
    });
  });

  test('[PDT-2599] TC-BE-35: INTERIM — second Signed window — billingDate in v2 slice (distinct interim amounts v1/v2)', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    const fixtures = { Request, GeneratePayload, Responses, Endpoints };
    const chain = await runPdt2599SignedServiceContractBillingChain(fixtures, 'INTERIM');
    const contractId = chain.contractId;
    expect(Responses.interim.length, 'INTERIM billing chain restores 3 IAP ids').toBe(3);
    const interimIdV1 = asPriceComponentId(Responses.interim[0]);
    const interimIdV2 = asPriceComponentId(Responses.interim[1]);
    expect(interimIdV1, 'TC-BE-35 IAP A').not.toBe(interimIdV2);

    await test.step('Precondition: second Signed slice (formula-neutral — interim contract rejects unrelated formulaVariableId overrides)', async () => {
      const detailRo = await loadServiceContractDetail(Request, Endpoints, contractId);
      const c = String((detailRo.basicParameters as { creationDate?: string }).creationDate);
      const secondSignedStart = await resolvePdt2599SecondSignedSliceStartForOpenAccountingPeriod(Request, c);
      const detail = await loadServiceContractDetailForEdit(Request, Endpoints, contractId);
      const pV2 = buildServiceContractEditPayloadFromDetail(detail, {
        savingAsNewVersion: true,
        startDate: secondSignedStart,
        contractVersionStatus: 'SIGNED',
      });
      await expect(
        await Request.put(`${Endpoints.serviceContract}/${contractId}`, {
          params: { versionId: pickLatestLogicalVersionId(detail.versions as { versionId: number }[]) },
          data: pV2,
        }),
      ).CheckResponse();
    });

    await test.step('Precondition: Signed v1 uses IAP A (+ term); Signed v2 uses IAP B (+ term) — amounts A≠B', async () => {
      const fieldsA = await pdt2599ReadInterimAdvancePaymentContractFields(Request, interimIdV1);
      const fieldsB = await pdt2599ReadInterimAdvancePaymentContractFields(Request, interimIdV2);

      await pdt2599PutServiceContractVersionInterimAdvanceRequests(
        Request,
        Endpoints,
        contractId,
        1,
        false,
        { contractVersionStatus: 'SIGNED' },
        [
          {
            interimAdvancePaymentId: fieldsA.interimAdvancePaymentId,
            termValue: fieldsA.termValue,
            value: PDT2599_INTERIM_ADVANCE_AMOUNT_V1_SIGNED,
          },
        ],
      );

      await pdt2599PutServiceContractVersionInterimAdvanceRequests(
        Request,
        Endpoints,
        contractId,
        2,
        false,
        { contractVersionStatus: 'SIGNED' },
        [
          {
            interimAdvancePaymentId: fieldsB.interimAdvancePaymentId,
            termValue: fieldsB.termValue,
            value: PDT2599_INTERIM_ADVANCE_AMOUNT_V2_SIGNED,
          },
        ],
      );

    });

    await test.step('Assert: GET by versionId shows persisted interim amounts v1=A, v2=B (A≠B)', async () => {
      const a = await pdt2599ReadInterimAdvanceAmountForContractVersion(
        Request,
        Endpoints,
        contractId,
        1,
        interimIdV1,
      );
      const b = await pdt2599ReadInterimAdvanceAmountForContractVersion(
        Request,
        Endpoints,
        contractId,
        2,
        interimIdV2,
      );
      expect(a, 'TC-BE-35 v1 interim amount').toBeCloseTo(PDT2599_INTERIM_ADVANCE_AMOUNT_V1_SIGNED, 2);
      expect(b, 'TC-BE-35 v2 interim amount').toBeCloseTo(PDT2599_INTERIM_ADVANCE_AMOUNT_V2_SIGNED, 2);
      expect(a, 'TC-BE-35 distinct v1/v2 interim').not.toBeCloseTo(b, 2);
    });

    await test.step('POST second Signed window billing-run + drafts (billing date in AP ∩ [v2Start, …))', async () => {
      const detailRo = await loadServiceContractDetail(Request, Endpoints, contractId);
      const signedStarts = pdtSortedSignedStartDates(detailRo);
      expect(signedStarts.length).toBeGreaterThanOrEqual(2);
      const v2Start = signedStarts[1];

      const w2Pick = await pickValidBillingDateFromBillingCommonWithinAccountingPeriod(
        Request,
        v2Start,
        addDaysIso(v2Start, 60),
      );
      const basePayload = await buildPdt2599BillingRunPayload(Request, Responses.serviceContract, ['INTERIM'], {
        taxEventDate: w2Pick,
        invoiceDate: w2Pick,
      });
      basePayload.commonParameters.accountingPeriodId = await resolveAccountingPeriodIdForBillingDate(Request, w2Pick);
      delete (basePayload.basicParameters as Record<string, unknown>).maxEndDate;
      const payload = alignPerPieceSecondSignedWindowBillingPayload(
        basePayload as unknown as Record<string, unknown>,
        w2Pick,
      );

      const br = await Request.post(Endpoints.billingRun, {
        data: payload,
      });
      await expect(br).CheckResponse();
      const brid = asBillingRunId(await br.json());
      const { draftInvoiceCount } = await pollPerPieceBillingRunUntilDraftWithInvoices(
        Request,
        Endpoints.billingRun,
        brid,
        { minimumInvoices: 1 },
      );
      expect(draftInvoiceCount).toBeGreaterThan(0);

      const iid = await getFirstDraftInvoiceStubId(Request, Endpoints.billingRun, brid);
      const iv = await Request.get(`invoice?id=${iid}`);
      await expect(iv).CheckResponse();

      const invJson = (await iv.json()) as Record<string, unknown>;
      assertPdt2599InvoiceNetMatchesExpected(
        invJson,
        PDT2599_INTERIM_ADVANCE_AMOUNT_V2_SIGNED,
        'TC-BE-35 INTERIM second Signed window (net ex-VAT = interim amount on logical v2)',
      );
      pdt2599AttachLinkedVersionIdDebug(invJson, 'TC-BE-35');
      attachPdt2599CreatedEntitiesForBilling(Responses, contractId, brid, {
        interimAdvancePaymentIdV1: interimIdV1,
        interimAdvancePaymentIdV2: interimIdV2,
      });
    });
  });

  test('[PDT-2599] TC-BE-33: INTERIM — billing after Draft v3 start — links latest Signed v2 (interim amounts differ per version)', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    const fixtures = { Request, GeneratePayload, Responses, Endpoints };
    const chain = await runPdt2599SignedServiceContractBillingChain(fixtures, 'INTERIM');
    const contractId = chain.contractId;
    expect(Responses.interim.length, 'INTERIM billing chain restores 3 IAP ids').toBe(3);
    /** A/B Signed, C Draft — fixture order matches PDT2599 interim integer amounts [56, 92, 188]. */
    const interimIdV1 = asPriceComponentId(Responses.interim[0]);
    const interimIdV2 = asPriceComponentId(Responses.interim[1]);
    const interimIdV3 = asPriceComponentId(Responses.interim[2]);
    expect(new Set([interimIdV1, interimIdV2, interimIdV3]).size).toBe(3);

    await test.step('Precondition: second Signed slice (formula-neutral — interim contract rejects unrelated formulaVariableId overrides)', async () => {
      const detailRo = await loadServiceContractDetail(Request, Endpoints, contractId);
      const c = String((detailRo.basicParameters as { creationDate?: string }).creationDate);
      const secondSignedStart = await resolvePdt2599SecondSignedSliceStartForOpenAccountingPeriod(Request, c);
      const detail = await loadServiceContractDetailForEdit(Request, Endpoints, contractId);
      const pV2 = buildServiceContractEditPayloadFromDetail(detail, {
        savingAsNewVersion: true,
        startDate: secondSignedStart,
        contractVersionStatus: 'SIGNED',
      });
      await expect(
        await Request.put(`${Endpoints.serviceContract}/${contractId}`, {
          params: { versionId: pickLatestLogicalVersionId(detail.versions as { versionId: number }[]) },
          data: pV2,
        }),
      ).CheckResponse();
    });

    await test.step('Precondition: shift Signed v2 start if needed + distinct interim amounts per version + Draft v3', async () => {
      let detail = await loadServiceContractDetailForEdit(Request, Endpoints, contractId);
      let signedStarts = pdtSortedSignedStartDates(await loadServiceContractDetail(Request, Endpoints, contractId));
      expect(signedStarts.length).toBeGreaterThanOrEqual(2);
      let v2Start = signedStarts[1];
      const apId = Number(envVariables.accounting_period);
      let apHi: string | undefined;
      if (Number.isFinite(apId)) {
        const apRes = await Request.get(`accounting-period/${apId}`);
        await expect(apRes).CheckResponse();
        const apJson = (await apRes.json()) as { endDate?: string };
        apHi = apJson.endDate?.slice(0, 10);
        if (apHi) {
          const maxV2StartForV3Billing = addDaysIso(apHi, -2);
          if (v2Start > maxV2StartForV3Billing) {
            const v1Start = signedStarts[0];
            const targetV2 = maxV2StartForV3Billing;
            const minV2AfterV1 = addDaysIso(v1Start, 1);
            if (targetV2 < minV2AfterV1) {
              throw new Error(
                `[PDT-2599 TC-BE-33] Cannot place Draft v3 + billing inside AP ending ${apHi}: Signed v1=${v1Start}, v2=${v2Start} (apId=${apId}).`,
              );
            }
            const d2 = await loadServiceContractDetailForEdit(Request, Endpoints, contractId, 2);
            await expect(
              await Request.put(`${Endpoints.serviceContract}/${contractId}`, {
                params: { versionId: 2 },
                data: buildServiceContractEditPayloadFromDetail(d2, {
                  savingAsNewVersion: false,
                  startDate: targetV2,
                  contractVersionStatus: 'SIGNED',
                }),
              }),
            ).CheckResponse();
            signedStarts = pdtSortedSignedStartDates(await loadServiceContractDetail(Request, Endpoints, contractId));
            v2Start = signedStarts[1];
          }
        }
      }

      const fieldsA = await pdt2599ReadInterimAdvancePaymentContractFields(Request, interimIdV1);
      const fieldsB = await pdt2599ReadInterimAdvancePaymentContractFields(Request, interimIdV2);
      const fieldsC = await pdt2599ReadInterimAdvancePaymentContractFields(Request, interimIdV3);

      await pdt2599PutServiceContractVersionInterimAdvanceRequests(
        Request,
        Endpoints,
        contractId,
        1,
        false,
        { contractVersionStatus: 'SIGNED' },
        [
          {
            interimAdvancePaymentId: fieldsA.interimAdvancePaymentId,
            termValue: fieldsA.termValue,
            value: PDT2599_INTERIM_ADVANCE_AMOUNT_V1_SIGNED,
          },
        ],
      );

      await pdt2599PutServiceContractVersionInterimAdvanceRequests(
        Request,
        Endpoints,
        contractId,
        2,
        false,
        { contractVersionStatus: 'SIGNED' },
        [
          {
            interimAdvancePaymentId: fieldsB.interimAdvancePaymentId,
            termValue: fieldsB.termValue,
            value: PDT2599_INTERIM_ADVANCE_AMOUNT_V2_SIGNED,
          },
        ],
      );

      const minV3Start = addDaysIso(v2Start, 1);
      let v3Start = addDaysIso(v2Start, 25);
      if (apHi) {
        const maxV3Start = addDaysIso(apHi, -1);
        if (minV3Start > maxV3Start) {
          throw new Error(
            `[PDT-2599 TC-BE-33] No calendar room for Draft v3 after v2=${v2Start} before AP end ${apHi}.`,
          );
        }
        if (v3Start > maxV3Start) v3Start = maxV3Start;
        if (v3Start < minV3Start) v3Start = minV3Start;
      }

      detail = await loadServiceContractDetailForEdit(Request, Endpoints, contractId);
      const pV3 = buildServiceContractEditPayloadFromDetail(detail, {
        savingAsNewVersion: true,
        startDate: v3Start,
        contractVersionStatus: 'DRAFT',
      });
      const sp3 = pV3.serviceParameters as Record<string, unknown>;
      sp3.interimAdvancePaymentsRequests = [
        {
          issueDate: null,
          value: PDT2599_INTERIM_ADVANCE_AMOUNT_V3_DRAFT,
          interimAdvancePaymentId: fieldsC.interimAdvancePaymentId,
          termValue: fieldsC.termValue,
          contractFormulas: [],
        },
      ];

      await expect(
        await Request.put(`${Endpoints.serviceContract}/${contractId}`, {
          params: { versionId: pickLatestLogicalVersionId(detail.versions as { versionId: number }[]) },
          data: { ...pV3, serviceParameters: sp3 },
        }),
      ).CheckResponse();

      const verify = await loadServiceContractDetail(Request, Endpoints, contractId);
      const rows = (verify.versions as { versionId: number; status?: string }[]) ?? [];
      expect(rows.length).toBe(3);
      const v3row = rows.find((v) => v.versionId === 3);
      expect(v3row).toBeTruthy();
      expect(isSignedContractVersionStatus(v3row!.status)).toBe(false);

      const interimA = await pdt2599ReadInterimAdvanceAmountForContractVersion(
        Request,
        Endpoints,
        contractId,
        1,
        interimIdV1,
      );
      const interimB = await pdt2599ReadInterimAdvanceAmountForContractVersion(
        Request,
        Endpoints,
        contractId,
        2,
        interimIdV2,
      );
      const interimC = await pdt2599ReadInterimAdvanceAmountForContractVersion(
        Request,
        Endpoints,
        contractId,
        3,
        interimIdV3,
      );
      expect(interimA, 'TC-BE-33 v1 interim amount').toBeCloseTo(PDT2599_INTERIM_ADVANCE_AMOUNT_V1_SIGNED, 2);
      expect(interimB, 'TC-BE-33 v2 interim amount').toBeCloseTo(PDT2599_INTERIM_ADVANCE_AMOUNT_V2_SIGNED, 2);
      expect(interimC, 'TC-BE-33 v3 interim amount').toBeCloseTo(PDT2599_INTERIM_ADVANCE_AMOUNT_V3_DRAFT, 2);
      expect(interimA, 'TC-BE-33 distinct v1/v2 interim').not.toBeCloseTo(interimB, 2);
      expect(interimB, 'TC-BE-33 distinct v2/v3 interim').not.toBeCloseTo(interimC, 2);

    });

    await test.step('POST billing-run: invoiceDate / taxEventDate after v3 start → draft invoices; linkage v2 when exposed', async () => {
      const detailRo = await loadServiceContractDetail(Request, Endpoints, contractId);
      const v3Row = ((detailRo.versions as { versionId: number; startDate?: string }[]) ?? []).find((v) => v.versionId === 3);
      expect(v3Row?.startDate).toBeTruthy();
      const v3StartIso = String(v3Row!.startDate);

      let billingPick = await pickValidBillingDateFromBillingCommonWithinAccountingPeriod(
        Request,
        addDaysIso(v3StartIso, 1),
        addDaysIso(v3StartIso, 120),
      );
      const apForBill = Number(envVariables.accounting_period);
      if (Number.isFinite(apForBill)) {
        const apBillRes = await Request.get(`accounting-period/${apForBill}`);
        await expect(apBillRes).CheckResponse();
        const apBillEnd = ((await apBillRes.json()) as { endDate?: string }).endDate?.slice(0, 10);
        if (apBillEnd && billingPick > apBillEnd) {
          billingPick = apBillEnd;
        }
      }
      expect(billingPick > v3StartIso).toBe(true);

      const baseBill = await buildPdt2599BillingRunPayload(Request, Responses.serviceContract, ['INTERIM'], {
        taxEventDate: billingPick,
        invoiceDate: billingPick,
      });
      baseBill.commonParameters.accountingPeriodId = await resolveAccountingPeriodIdForBillingDate(Request, billingPick);
      delete (baseBill.basicParameters as Record<string, unknown>).maxEndDate;
      const payload = alignPerPieceSecondSignedWindowBillingPayload(
        baseBill as unknown as Record<string, unknown>,
        billingPick,
      );

      const br = await Request.post(Endpoints.billingRun, {
        data: payload,
      });
      await expect(br).CheckResponse();
      const brid = asBillingRunId(await br.json());
      const { draftInvoiceCount } = await pollPerPieceBillingRunUntilDraftWithInvoices(
        Request,
        Endpoints.billingRun,
        brid,
        { minimumInvoices: 1 },
      );
      expect(draftInvoiceCount).toBeGreaterThan(0);

      const iid = await getFirstDraftInvoiceStubId(Request, Endpoints.billingRun, brid);
      const iv = await Request.get(`invoice?id=${iid}`);
      await expect(iv).CheckResponse();

      const invJson = (await iv.json()) as Record<string, unknown>;
      assertPdt2599InvoiceNetMatchesExpected(
        invJson,
        PDT2599_INTERIM_ADVANCE_AMOUNT_V2_SIGNED,
        'TC-BE-33 INTERIM after Draft v3: billing follows latest Signed v2 interim amount (not Draft v3 amount)',
      );
      pdt2599AttachLinkedVersionIdDebug(invJson, 'TC-BE-33');
      attachPdt2599CreatedEntitiesForBilling(Responses, contractId, brid, {
        interimAdvancePaymentIdV1: interimIdV1,
        interimAdvancePaymentIdV2: interimIdV2,
        interimAdvancePaymentIdV3Draft: interimIdV3,
      });
    });
  });
});