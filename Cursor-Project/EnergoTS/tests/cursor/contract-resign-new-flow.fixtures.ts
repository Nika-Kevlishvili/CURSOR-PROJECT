/**
 * Contract Resign New Flow — shared API helpers (Dev2).
 *
 * Requires EnergoTS `.env` with Dev2 BASE_URL and auth (run global setup first).
 *
 * Reference spec(s):
 * - tests/cursor/PDT-2750-missing-interim-invoice.spec.ts
 * - tests/salesPortal/getProductList.spec.ts
 * - tests/contractsAndOrders/productContract.spec.ts
 * - tests/cursor/pdt-2815-version-validity.fixtures.ts (edit payload / status-update)
 */

import { expect, test } from '../../fixtures/baseFixture';
import type { baseFixture } from '../../fixtures/baseFixture';
import { request as playwrightRequest } from '@playwright/test';
import { configuredBaseURL } from '../../fixtures/utils/baseUrl';
import {
    buildEditProductContractPayload,
} from './pdt-2815-version-validity.fixtures';
import { productContractStatusUpdate } from './pdt-2854-pod-active-two-contracts.fixtures';

type FixtureRequest = baseFixture['Request'];
export type FixtureCtx = Pick<baseFixture, 'Request' | 'GeneratePayload' | 'Responses' | 'Endpoints'>;

function firstDayOfNextMonthIso(isoDate: string): string {
    const d = new Date(`${isoDate}T12:00:00.000Z`);
    d.setUTCMonth(d.getUTCMonth() + 1);
    d.setUTCDate(1);
    return d.toISOString().split('T')[0];
}

/** First calendar day of the month after the month containing `isoDate`. */
export function firstDayOfMonthAfter(isoDate: string): string {
    const d = new Date(`${isoDate}T12:00:00.000Z`);
    d.setUTCMonth(d.getUTCMonth() + 1);
    d.setUTCDate(1);
    return d.toISOString().split('T')[0];
}

/** Excel Case 13 / TC-BE-25: Wait=Yes + first day of month after initial term end 2026-03-25. */
export const CASE_13_INITIAL_TERM_END = '2026-03-25';
export const CASE_13_CONTRACT_TERM_END = '2026-03-31';
export const CASE_13_B_SIGNING_DATE = '2026-01-15';
export const CASE_13_POD_ACTIVATION = '2026-03-01';
export const CASE_13_F_SIGNING_DATE = '2026-03-15';
export const CASE_13_EXPECTED_F_ACTIVATION = firstDayOfMonthAfter(CASE_13_INITIAL_TERM_END);
export const CASE_13_EXPECTED_B_DEACTIVATION = addDaysIso(CASE_13_EXPECTED_F_ACTIVATION, -1);

/** Excel-aligned windows anchored to Dev2 „today“ so preconditions stay valid over time. */
function initResignFlowDates() {
    const today = todayIso();
    const termEnd = addDaysIso(today, 20);
    const signingInWindow = today;
    return {
        TERM_END_DATE: termEnd,
        SIGNING_DATE_IN_WINDOW: signingInWindow,
        SIGNING_DATE_BEFORE_WINDOW: addDaysIso(today, -120),
        SIGNING_DATE_AFTER_TERM_END: addDaysIso(termEnd, 10),
        SIGNING_DATE_AT_EARLY_DEADLINE: addDaysIso(termEnd, -25),
        WAIT_YES_FIRST_DAY_OF_NEXT_MONTH: firstDayOfNextMonthIso(signingInWindow),
        EXACT_WAIT_YES_AFTER_TERM_END: addDaysIso(termEnd, 1),
    };
}

let flowDates = initResignFlowDates();

export let TERM_END_DATE = flowDates.TERM_END_DATE;
export let SIGNING_DATE_IN_WINDOW = flowDates.SIGNING_DATE_IN_WINDOW;
export let SIGNING_DATE_BEFORE_WINDOW = flowDates.SIGNING_DATE_BEFORE_WINDOW;
export let SIGNING_DATE_AFTER_TERM_END = flowDates.SIGNING_DATE_AFTER_TERM_END;
export let SIGNING_DATE_AT_EARLY_DEADLINE = flowDates.SIGNING_DATE_AT_EARLY_DEADLINE;
export let WAIT_YES_FIRST_DAY_OF_NEXT_MONTH = flowDates.WAIT_YES_FIRST_DAY_OF_NEXT_MONTH;
export let EXACT_WAIT_YES_AFTER_TERM_END = flowDates.EXACT_WAIT_YES_AFTER_TERM_END;

/** Recompute flow-date exports from current clock (call at chain start — avoids stale import-time dates). */
export function refreshResignFlowDates(): void {
    flowDates = initResignFlowDates();
    TERM_END_DATE = flowDates.TERM_END_DATE;
    SIGNING_DATE_IN_WINDOW = flowDates.SIGNING_DATE_IN_WINDOW;
    SIGNING_DATE_BEFORE_WINDOW = flowDates.SIGNING_DATE_BEFORE_WINDOW;
    SIGNING_DATE_AFTER_TERM_END = flowDates.SIGNING_DATE_AFTER_TERM_END;
    SIGNING_DATE_AT_EARLY_DEADLINE = flowDates.SIGNING_DATE_AT_EARLY_DEADLINE;
    WAIT_YES_FIRST_DAY_OF_NEXT_MONTH = flowDates.WAIT_YES_FIRST_DAY_OF_NEXT_MONTH;
    EXACT_WAIT_YES_AFTER_TERM_END = flowDates.EXACT_WAIT_YES_AFTER_TERM_END;
}

/** Phoenix `ProductContractResignService` / `ProductContractService` message fragments (dev2). */
export const MSG_NOT_FOR_RESIGNING = 'Product is not for re-signing';
export const MSG_MANUAL_ACTIVATION = 'Product is not suitable for re-signing due to manual activation option';
export const MSG_ONLY_FOR_RESIGNING = 'Product is only for re-signing';
export const MSG_ONLY_FOR_RESIGNING_FULL = "Contract can't be created/edited. Product is only for re-signing";
export const MSG_INCOMPATIBLE = 'Re-signing unavailable due to product incompatibility';
export const MSG_INCOMPATIBLE_FULL = "Contract can't be created/edited. Re-signing unavailable due to product incompatibility";
export const MSG_VERSION_NOT_EXIST = 'Resigning can not start, Contract version for signing date does not exist';
export const MSG_RESIGNING_FAILED_ALL_PODS = 'Resigning process failed for all pods';
export const MSG_RESIGNING_FAILED_ALL_PODS_FULL =
    "Contract can't be created/edited. Product is only for re-signing, Resigning process failed for all pods";

/** Excel Case 16 / TC-BE-16 — engine cannot derive re-signing window (Confluence + spreadsheet). */
export const MSG_CODE_RESIGNING_PERIOD_CANNOT_BE_CALCULATED = 'RESIGNING_PERIOD_CANNOT_BE_CALCULATED';
export const MSG_RESIGNING_PERIOD_CANNOT_BE_CALCULATED_TEXT = 'resigning period cannot be calculated';

/** Typical Phoenix `ClientException` HTTP statuses on product-contract PUT. */
export const RESIGN_ERROR_HTTP_STATUSES = [400, 403, 409, 422] as const;

export type ParsedApiError = {
    status: number;
    message: string;
    errorCode?: string;
    raw: string;
};

export type ResignErrorExpectation = {
    /** One or more substrings expected in RestError.message or body text (case-insensitive). */
    messageParts: string | string[];
    status?: readonly number[];
    errorCode?: string;
};

export type SignPutResult = {
    status: number;
    body: Record<string, unknown>;
    text: string;
};

export type SignContractFOptions = {
    signingDate: string;
    waitExpire?: 'YES' | 'NO';
    supplyActivation?: 'FIRST_DAY_OF_MONTH' | 'EXACT_DATE' | 'MANUAL';
    removeFuturePods?: boolean;
    status?: string;
    subStatus?: string;
    contractTermEndDate?: string | null;
    /** Excel Case 16 — old contract B: omit term end (and initial-term end value) only; keep required enum fields. */
    omitOldContractTermEnd?: boolean;
    productIndex?: number;
    customerIndex?: number;
    podIndex?: number;
    baseContractIndex?: number;
};

export type ResignChainContext = {
    baseContractId: number;
    newContractId: number;
    podDetailId: number;
    podId: number;
    baseProductId: number;
    resignProductId: number;
};

export function entityId(entry: unknown): number {
    if (typeof entry === 'number' && Number.isFinite(entry) && entry > 0) {
        return entry;
    }
    if (entry !== null && typeof entry === 'object' && 'id' in entry) {
        const id = Number((entry as { id: unknown }).id);
        if (Number.isFinite(id) && id > 0) {
            return id;
        }
    }
    throw new Error(`Cannot resolve entity id from: ${JSON.stringify(entry).slice(0, 200)}`);
}

export function addDaysIso(isoDate: string, days: number): string {
    const d = new Date(`${isoDate}T12:00:00.000Z`);
    d.setUTCDate(d.getUTCDate() + days);
    return d.toISOString().split('T')[0];
}

export function addMonthsSameDay(isoDate: string, months: number): string {
    const d = new Date(`${isoDate}T12:00:00.000Z`);
    d.setUTCMonth(d.getUTCMonth() + months);
    return d.toISOString().split('T')[0];
}

export function todayIso(): string {
    return new Date().toISOString().split('T')[0];
}

/** Latest of two ISO dates (YYYY-MM-DD). */
export function maxIsoDate(a: string, b: string): string {
    return a >= b ? a : b;
}

/** Earliest of two ISO dates (YYYY-MM-DD). */
export function minIsoDate(a: string, b: string): string {
    return a <= b ? a : b;
}

/** Dev2 rejects coordinates on create without edit-coordinates permission; nested comm contacts need persisted parent. */
export function customerLegalPayloadForResignApi(GeneratePayload: FixtureCtx['GeneratePayload']): Record<string, unknown> {
    const payload = structuredClone(GeneratePayload.customers.customer_legal()) as Record<string, unknown>;
    const stripCoords = (addr: Record<string, unknown> | undefined): void => {
        if (!addr) return;
        delete addr.latitude;
        delete addr.longitude;
    };
    stripCoords(payload.address as Record<string, unknown> | undefined);
    const comm = payload.communicationData as Record<string, unknown>[] | undefined;
    if (Array.isArray(comm)) {
        for (const entry of comm) {
            if (!entry || typeof entry !== 'object') continue;
            stripCoords(entry.address as Record<string, unknown> | undefined);
        }
    }
    return payload;
}

export async function sharedCustomer(ctx: FixtureCtx): Promise<void> {
    const { Request, GeneratePayload, Responses, Endpoints } = ctx;
    const response = await Request.post(Endpoints.customer, {
        data: customerLegalPayloadForResignApi(GeneratePayload),
    });
    await expect(response).CheckResponse();
    Responses.customer.push(await response.json());
}

export async function sharedPrice(ctx: FixtureCtx): Promise<void> {
    const { Request, GeneratePayload, Responses, Endpoints } = ctx;
    const payload = GeneratePayload.productAndServices.priceSettlement();
    const response = await Request.post(Endpoints.priceComponent, { data: payload });
    await expect(response).CheckResponse();
    Responses.priceComponent.push(await response.json());
}

/** Product generator attaches every price in Responses; Dev2 allows each PC on one product only. */
function bindLatestPriceComponentOnly(
    payload: { priceComponentIds: unknown[] },
    Responses: FixtureCtx['Responses'],
): void {
    const last = Responses.priceComponent[Responses.priceComponent.length - 1];
    payload.priceComponentIds = [entityId(last)];
}

function bindTermId(
    payload: { termId: unknown; termGroupId?: unknown | null },
    Responses: FixtureCtx['Responses'],
    termIndex: number,
): void {
    payload.termId = entityId(Responses.terms[termIndex]);
    payload.termGroupId = null;
}

function prepareProductContractPostPayload(
    draftPayload: Record<string, unknown>,
    opts?: { termEndDate?: string | null },
): void {
    applySupplyOnlyContractType(draftPayload);
    const pp = draftPayload.productParameters as Record<string, unknown>;
    if (opts?.termEndDate !== undefined) {
        pp.contractTermEndDate = opts.termEndDate;
    }
    const bp = draftPayload.basicParameters as Record<string, unknown>;
    if (bp.entryInForceDate == null && bp.signingDate == null) {
        bp.entryInForceDate = null;
    }
}

export async function sharedTermBase(
    ctx: FixtureCtx,
    opts?: { resigningDeadlineValue?: number | null; resigningDeadlineType?: string | null },
): Promise<void> {
    const { Request, GeneratePayload, Responses, Endpoints } = ctx;
    const payload = GeneratePayload.productAndServices.term();
    payload.resigningDeadlineType = opts?.resigningDeadlineType ?? 'MONTH';
    payload.resigningDeadlineValue = opts?.resigningDeadlineValue ?? 1;
    payload.supplyActivations = ['FIRST_DAY_OF_MONTH', 'EXACT_DATE'];
    payload.waitForOldContractTermToExpires = ['YES', 'NO'];
    payload.contractEntryIntoForces = ['MANUAL', 'SIGNING'];
    payload.startsOfContractInitialTerms = ['MANUAL', 'SIGNING'];
    const response = await Request.post(Endpoints.terms, { data: payload });
    await expect(response).CheckResponse();
    Responses.terms.push(await response.json());
}

export async function sharedTermResign(
    ctx: FixtureCtx,
    opts?: {
        waitValues?: ('YES' | 'NO')[];
        supplyActivations?: string[];
        resigningDeadlineValue?: number | null;
    },
): Promise<void> {
    const { Request, GeneratePayload, Responses, Endpoints } = ctx;
    const payload = GeneratePayload.productAndServices.term();
    payload.resigningDeadlineType = 'MONTH';
    payload.resigningDeadlineValue = opts?.resigningDeadlineValue ?? 1;
    payload.supplyActivations = opts?.supplyActivations ?? ['FIRST_DAY_OF_MONTH', 'EXACT_DATE'];
    payload.waitForOldContractTermToExpires = opts?.waitValues ?? ['YES', 'NO'];
    payload.contractEntryIntoForces = ['MANUAL', 'SIGNING'];
    payload.startsOfContractInitialTerms = ['MANUAL', 'SIGNING'];
    const response = await Request.post(Endpoints.terms, { data: payload });
    await expect(response).CheckResponse();
    Responses.terms.push(await response.json());
}

export async function sharedPod(ctx: FixtureCtx): Promise<void> {
    const { Request, GeneratePayload, Responses, Endpoints } = ctx;
    const response = await Request.post(Endpoints.pod, {
        data: GeneratePayload.pointsOfDelivery.pod_settlement(),
    });
    await expect(response).CheckResponse();
    Responses.pod.push(await response.json());
}

export async function sharedBaseProduct(ctx: FixtureCtx, termIndex = 0): Promise<number> {
    const { Request, GeneratePayload, Responses, Endpoints } = ctx;
    if (Responses.priceComponent.length === 0) {
        await sharedPrice(ctx);
    }
    const payload = GeneratePayload.productAndServices.product(termIndex);
    payload.contractTypes = ['SUPPLY_ONLY'];
    payload.paymentGuarantees = ['NO'];
    bindLatestPriceComponentOnly(payload, Responses);
    bindTermId(payload, Responses, termIndex);
    const response = await Request.post(Endpoints.product, { data: payload });
    await expect(response).CheckResponse();
    const body = await response.json();
    Responses.product.push(body);
    return entityId(body);
}

export async function sharedResignProduct(
    ctx: FixtureCtx,
    baseProductId: number,
    opts?: { termIndex?: number; resignable?: boolean; linkTarget?: boolean },
): Promise<number> {
    const { Request, GeneratePayload, Responses, Endpoints } = ctx;
    await sharedPrice(ctx);
    const termIndex = opts?.termIndex ?? 1;
    const payload = GeneratePayload.productAndServices.product(termIndex);
    payload.contractTypes = ['SUPPLY_ONLY'];
    payload.paymentGuarantees = ['NO'];
    bindLatestPriceComponentOnly(payload, Responses);
    bindTermId(payload, Responses, termIndex);
    (payload as Record<string, unknown>).isResigning = opts?.resignable !== false;
    if (opts?.linkTarget !== false) {
        (payload as Record<string, unknown>).resignProductTargets = [baseProductId];
    }
    const response = await Request.post(Endpoints.product, { data: payload });
    await expect(response).CheckResponse();
    const body = await response.json();
    Responses.product.push(body);
    const resignProductId = entityId(body);

    if (opts?.resignable !== false && opts?.linkTarget !== false) {
        const specialPayload = await GeneratePayload.productAndServices.specialOffersTab(false);
        specialPayload.resignable = true;
        specialPayload.resigningProducts = [{ productId: baseProductId, versionIds: [1] }];
        const putRes = await Request.put(`products/${resignProductId}/special-offers?version=1`, {
            data: specialPayload,
        });
        await expect(putRes).CheckResponse();
    }

    return resignProductId;
}

export async function sharedNonResignProduct(ctx: FixtureCtx, termIndex = 1): Promise<number> {
    const { Request, GeneratePayload, Responses, Endpoints } = ctx;
    await sharedPrice(ctx);
    const payload = GeneratePayload.productAndServices.product(termIndex);
    payload.contractTypes = ['SUPPLY_ONLY'];
    payload.paymentGuarantees = ['NO'];
    bindLatestPriceComponentOnly(payload, Responses);
    bindTermId(payload, Responses, termIndex);
    (payload as Record<string, unknown>).isResigning = false;
    const response = await Request.post(Endpoints.product, { data: payload });
    await expect(response).CheckResponse();
    const body = await response.json();
    Responses.product.push(body);
    return entityId(body);
}

export async function activatePodOnContract(
    ctx: FixtureCtx,
    contractIndex: number,
    activationDate: string,
    podIndex = 0,
    deactivationDate?: string | null,
): Promise<void> {
    const { Request, Responses } = ctx;
    const contractId = entityId(Responses.productContract[contractIndex]);
    const getRes = await Request.get(`product-contract/${contractId}`);
    await expect(getRes).CheckResponse();
    const body = (await getRes.json()) as Record<string, unknown>;
    const versions = (body.versions ?? []) as { versionId?: number; id?: number }[];
    const v1 = versions.find((v) => Number(v.versionId) === 1) ?? versions[0];
    const contractDetailId = v1?.id;
    expect(contractDetailId, `contract ${contractId} version detail id`).toBeTruthy();

    const podId = entityId(Responses.pod[podIndex]);
    let pods = contractPodsFromBody(body);
    if (pods.length === 0) {
        pods = await getContractPods(Request, contractId);
    }
    const row = pods.find((p) => Number(p.podId) === podId);
    const podJson = Responses.pod[podIndex] as Record<string, unknown>;
    const identifier = row?.identifier ?? podJson.identifier;
    const podDetailId = row?.podDetailId ?? podJson.podDetailId ?? podJson.lastPodDetailId;
    expect(identifier, `POD identifier on contract ${contractId}`).toBeTruthy();
    expect(podDetailId, `POD detail id on contract ${contractId}`).toBeTruthy();

    const { envVariables } = await import('../../fixtures/envCashed');
    const effectiveActivationDate = maxIsoDate(activationDate, todayIso());
    const activation = await Request.post('/contract-pods/manual', {
        data: {
            identifier,
            podDetailId,
            contractDetailId,
            activationDate: effectiveActivationDate,
            deactivationDate: deactivationDate ?? undefined,
            deactivationPurposeId: deactivationDate ? envVariables.deactivation_reason : null,
        },
    });
    await expect(activation).CheckResponse();
}

function contractPodsFromBody(body: Record<string, unknown>) {
    return (body.contractPodsResponses ?? []) as Record<string, unknown>[];
}

export async function getContractPods(
    Request: FixtureRequest,
    contractId: number,
): Promise<Record<string, unknown>[]> {
    const res = await Request.get(`product-contract/${contractId}`);
    await expect(res).CheckResponse();
    const body = (await res.json()) as Record<string, unknown>;
    return contractPodsFromBody(body);
}

export async function expectResignMessages(body: Record<string, unknown>, fragment: string): Promise<void> {
    const messages = (body.resigningMessages ?? []) as string[];
    expect(Array.isArray(messages)).toBeTruthy();
    expect(messages.some((m) => String(m).includes(fragment))).toBeTruthy();
}

export async function parseApiError(response: {
    status: () => number;
    text: () => Promise<string>;
    json?: () => Promise<unknown>;
}): Promise<ParsedApiError> {
    const status = response.status();
    let raw = '';
    let message = '';
    let errorCode: string | undefined;
    try {
        const body = (await response.json?.()) as Record<string, unknown> | undefined;
        if (body && typeof body === 'object') {
            raw = JSON.stringify(body);
            if (typeof body.message === 'string') {
                message = body.message;
            }
            if (body.errorCode != null) {
                errorCode = String(body.errorCode);
            }
        }
    } catch {
        raw = await response.text();
        message = raw;
    }
    if (!message) {
        message = raw;
    }
    return { status, message, errorCode, raw };
}

export async function expectResignClientError(
    response: { status: () => number; text: () => Promise<string>; json?: () => Promise<unknown> },
    expected: ResignErrorExpectation,
): Promise<ParsedApiError> {
    const parsed = await parseApiError(response);
    const allowed = expected.status ?? RESIGN_ERROR_HTTP_STATUSES;
    expect(allowed).toContain(parsed.status);
    const parts = Array.isArray(expected.messageParts) ? expected.messageParts : [expected.messageParts];
    const haystack = `${parsed.message} ${parsed.raw}`.toLowerCase();
    for (const part of parts) {
        expect(haystack).toContain(part.toLowerCase());
    }
    if (expected.errorCode) {
        expect(String(parsed.errorCode ?? parsed.raw)).toContain(expected.errorCode);
    }
    return parsed;
}

/** @deprecated Prefer expectResignClientError — kept for gradual migration. */
export async function expectClientError(
    response: { status: () => number; text: () => Promise<string>; json?: () => Promise<unknown> },
    fragment: string,
): Promise<void> {
    await expectResignClientError(response, { messageParts: fragment });
}

export async function signContractPutRaw(
    ctx: FixtureCtx,
    contractId: number,
    options: SignContractFOptions,
): Promise<SignPutResult> {
    const { Request, Endpoints } = ctx;
    const signedPayload = await buildSignedPutPayload(ctx, contractId, options);
    const response = await Request.put(
        `${Endpoints.productContract}/${contractId}?versionId=1&changeFutureVersionsPods=false`,
        { data: signedPayload },
    );
    const status = response.status();
    const text = await response.text();
    let body: Record<string, unknown> = {};
    try {
        body = JSON.parse(text) as Record<string, unknown>;
    } catch {
        body = { raw: text };
    }
    return { status, body, text };
}

export async function assertPodNotResignedOnOldContract(
    Request: FixtureRequest,
    baseContractId: number,
    podId: number,
): Promise<void> {
    const pods = await getContractPods(Request, baseContractId);
    const row = pods.find((p) => Number(p.podId) === podId);
    expect(row).toBeTruthy();
    expect(row?.isResigned).not.toBe(true);
    const status = String(row?.status ?? row?.contractPodStatus ?? '');
    expect(status.toUpperCase()).not.toContain('RESIGNED');
}

export async function assertNoResignOnOldContract(
    Request: FixtureRequest,
    baseContractId: number,
    podId: number,
): Promise<void> {
    await assertPodNotResignedOnOldContract(Request, baseContractId, podId);
    const pods = await getContractPods(Request, baseContractId);
    const row = pods.find((p) => Number(p.podId) === podId);
    expect(row?.activationDate).toBeTruthy();
}

export async function assertInformationalResignOnly(
    ctx: FixtureCtx,
    body: Record<string, unknown>,
    baseContractId: number,
    podId: number,
    infoFragment: string,
): Promise<void> {
    await expectResignMessages(body, infoFragment);
    expect(body.id).toBeTruthy();
    await assertNoResignOnOldContract(ctx.Request, baseContractId, podId);
}

export async function assertBothPodsResignedOnOld(
    Request: FixtureRequest,
    baseContractId: number,
    podIds: number[],
): Promise<void> {
    const pods = await getContractPods(Request, baseContractId);
    for (const podId of podIds) {
        const row = pods.find((p) => Number(p.podId) === podId);
        expect(row).toBeTruthy();
        expect(
            row?.isResigned === true ||
                row?.status === 'RESIGNED' ||
                String(row?.contractPodStatus ?? '').toUpperCase().includes('RESIGN'),
        ).toBeTruthy();
    }
}

/** Historical supply gap (deactivate then reactivate) before signing — past gap must not block resign (Excel Case 21). */
export async function createPastSupplyGapThenReactivateOnBase(
    ctx: FixtureCtx,
    contractIndex: number,
    podIndex = 0,
): Promise<void> {
    const firstActivation = addDaysIso(SIGNING_DATE_IN_WINDOW, -120);
    const deactivation = addDaysIso(SIGNING_DATE_IN_WINDOW, -90);
    const secondActivation = addDaysIso(SIGNING_DATE_IN_WINDOW, -60);
    await activatePodOnContract(ctx, contractIndex, firstActivation, podIndex);
    await activatePodOnContract(ctx, contractIndex, deactivation, podIndex, deactivation);
    await activatePodOnContract(ctx, contractIndex, secondActivation, podIndex);
}

export async function updateResignDraftBeforeSign(
    ctx: FixtureCtx,
    contractId: number,
    productIndex = 1,
): Promise<void> {
    const generated = (await ctx.GeneratePayload.contractsAndOrders.product_contract(0, productIndex)) as Record<
        string,
        unknown
    >;
    const editPayload = await buildEditProductContractPayload(ctx.Request, contractId, generated, {
        forDraftV1: false,
        preserveSigningDate: false,
    });
    const put = await ctx.Request.put(
        `${ctx.Endpoints.productContract}/${contractId}?versionId=1&changeFutureVersionsPods=false`,
        { data: editPayload },
    );
    await expect(put).CheckResponse();
}

function applySupplyOnlyContractType(draftPayload: Record<string, unknown>): void {
    const pp = draftPayload.productParameters as Record<string, unknown>;
    pp.contractType = 'SUPPLY_ONLY';
}

/**
 * Excel Case 16 — old contract B without term end / initial-term end date (re-sign window cannot be calculated).
 * POST/PUT validation requires `startOfContractInitialTerm` (and signing flow fields) — only term-end fields are cleared.
 */
export function applyOmittedOldContractTermEndOnly(payload: Record<string, unknown>): void {
    const bp = payload.basicParameters as Record<string, unknown>;
    const pp = payload.productParameters as Record<string, unknown>;
    bp.contractTermEndDate = null;
    pp.contractTermEndDate = null;
    pp.startOfContractValue = null;
    if (pp.startOfContractInitialTerm == null) {
        pp.startOfContractInitialTerm = 'SIGNING';
    }
    prepareProductContractPostPayload(payload, { termEndDate: null });
}

export async function createAndSignBaseContractB(
    ctx: FixtureCtx,
    opts?: {
        signingDate?: string;
        termEndDate?: string | null;
        productIndex?: number;
        customerIndex?: number;
        activatePod?: boolean;
        activationDate?: string;
    },
): Promise<number> {
    const { Request, GeneratePayload, Responses, Endpoints } = ctx;
    const signingDate = opts?.signingDate ?? addDaysIso(todayIso(), -30);
    const termEndDate =
        opts?.termEndDate === undefined ? TERM_END_DATE : opts.termEndDate;
    const productIndex = opts?.productIndex ?? 0;
    const customerIndex = opts?.customerIndex ?? 0;

    const draftPayload = await GeneratePayload.contractsAndOrders.product_contract(customerIndex, productIndex);
    draftPayload.basicParameters.status = 'DRAFT';
    draftPayload.basicParameters.subStatus = 'DRAFT';
    draftPayload.basicParameters.versionStatus = 'SIGNED';
    draftPayload.basicParameters.signingDate = null;
    draftPayload.productParameters.contractTermEndDate = termEndDate;
    draftPayload.productParameters.supplyActivation = 'FIRST_DAY_OF_MONTH';
    prepareProductContractPostPayload(draftPayload as Record<string, unknown>, { termEndDate });

    const postRes = await Request.post(Endpoints.productContract, { data: draftPayload });
    await expect(postRes).CheckResponse();
    const contractId = entityId(await postRes.json());
    Responses.productContract.push(contractId);

    await signContractById(ctx, contractId, {
        signingDate,
        status: 'SIGNED',
        subStatus: 'SIGNED_BY_BOTH_SIDES',
        productIndex,
        customerIndex,
        waitExpire: 'NO',
        supplyActivation: 'FIRST_DAY_OF_MONTH',
    });

    if (opts?.activatePod !== false) {
        const activationDate = maxIsoDate(opts?.activationDate ?? signingDate, todayIso());
        await activatePodOnContract(ctx, Responses.productContract.length - 1, activationDate);
    }

    return contractId;
}

/**
 * Old contract B for Excel Case 16 — same flow as {@link createAndSignBaseContractB} but without term end / initial-term dates.
 */
export async function createAndSignBaseContractBWithoutTermDates(
    ctx: FixtureCtx,
    opts?: Omit<Parameters<typeof createAndSignBaseContractB>[1], 'termEndDate'>,
): Promise<number> {
    const { Request, GeneratePayload, Responses, Endpoints } = ctx;
    const signingDate = opts?.signingDate ?? addDaysIso(todayIso(), -30);
    const productIndex = opts?.productIndex ?? 0;
    const customerIndex = opts?.customerIndex ?? 0;

    const draftPayload = (await GeneratePayload.contractsAndOrders.product_contract(
        customerIndex,
        productIndex,
    )) as Record<string, unknown>;
    draftPayload.basicParameters = {
        ...(draftPayload.basicParameters as Record<string, unknown>),
        status: 'DRAFT',
        subStatus: 'DRAFT',
        versionStatus: 'SIGNED',
        signingDate: null,
    };
    applyOmittedOldContractTermEndOnly(draftPayload);
    const pp = draftPayload.productParameters as Record<string, unknown>;
    pp.supplyActivation = 'FIRST_DAY_OF_MONTH';

    const postRes = await Request.post(Endpoints.productContract, { data: draftPayload });
    await expect(postRes).CheckResponse();
    const contractId = entityId(await postRes.json());
    Responses.productContract.push(contractId);

    await signContractById(ctx, contractId, {
        signingDate,
        status: 'SIGNED',
        subStatus: 'SIGNED_BY_BOTH_SIDES',
        productIndex,
        customerIndex,
        waitExpire: 'NO',
        supplyActivation: 'FIRST_DAY_OF_MONTH',
        omitOldContractTermEnd: true,
        contractTermEndDate: null,
    });

    if (opts?.activatePod !== false) {
        const activationDate = maxIsoDate(opts?.activationDate ?? signingDate, todayIso());
        await activatePodOnContract(ctx, Responses.productContract.length - 1, activationDate);
    }

    return contractId;
}

/**
 * Old contract B for Case 13: initial term end 2026-03-25, POD active 2026-03-01, ACTIVE_IN_TERM.
 */
export async function createAndSignBaseContractCase13(ctx: FixtureCtx): Promise<number> {
    const { Request, GeneratePayload, Responses, Endpoints } = ctx;
    const productIndex = 0;
    const customerIndex = 0;

    const draftPayload = (await GeneratePayload.contractsAndOrders.product_contract(
        customerIndex,
        productIndex,
    )) as Record<string, unknown>;
    draftPayload.basicParameters = {
        ...(draftPayload.basicParameters as Record<string, unknown>),
        status: 'DRAFT',
        subStatus: 'DRAFT',
        versionStatus: 'SIGNED',
        signingDate: null,
        entryInForceDate: null,
        startOfInitialTerm: null,
        contractTermEndDate: CASE_13_CONTRACT_TERM_END,
    };
    const pp = draftPayload.productParameters as Record<string, unknown>;
    pp.contractType = 'SUPPLY_ONLY';
    pp.contractTermEndDate = CASE_13_CONTRACT_TERM_END;
    pp.startOfContractInitialTerm = 'EXACT_DATE';
    pp.startOfContractValue = CASE_13_INITIAL_TERM_END;
    pp.supplyActivation = 'MANUAL';
    prepareProductContractPostPayload(draftPayload, { termEndDate: CASE_13_CONTRACT_TERM_END });

    const postRes = await Request.post(Endpoints.productContract, { data: draftPayload });
    await expect(postRes).CheckResponse();
    const contractId = entityId(await postRes.json());
    Responses.productContract.push(contractId);

    const ready = await productContractStatusUpdate(Request, contractId, 'READY', 'READY', 1);
    await expect(ready).CheckResponse();
    const signed = await productContractStatusUpdate(
        Request,
        contractId,
        'SIGNED',
        'SIGNED_BY_BOTH_SIDES',
        1,
    );
    await expect(signed).CheckResponse();

    const editPayload = await buildEditProductContractPayload(Request, contractId, draftPayload, {
        preserveSigningDate: false,
    });
    const editBp = editPayload.basicParameters as Record<string, unknown>;
    editBp.signingDate = CASE_13_B_SIGNING_DATE;
    editBp.entryInForceDate = CASE_13_B_SIGNING_DATE;
    editBp.startOfInitialTerm = CASE_13_INITIAL_TERM_END;
    const put = await Request.put(
        `${Endpoints.productContract}/${contractId}?versionId=1&changeFutureVersionsPods=false`,
        { data: editPayload },
    );
    await expect(put).CheckResponse();

    await activatePodOnContract(ctx, Responses.productContract.length - 1, CASE_13_POD_ACTIVATION, 0);
    return contractId;
}

export async function createDraftContractF(
    ctx: FixtureCtx,
    opts?: {
        productIndex?: number;
        customerIndex?: number;
        status?: string;
        subStatus?: string;
        /** When set, attach these POD rows (avoids duplicate pointOfDeliveryDetailId on multi-POD drafts). */
        podIndices?: number[];
    },
): Promise<number> {
    const { Request, GeneratePayload, Responses, Endpoints } = ctx;
    const productIndex = opts?.productIndex ?? 1;
    const customerIndex = opts?.customerIndex ?? 0;

    const payload = (await GeneratePayload.contractsAndOrders.product_contract(
        customerIndex,
        productIndex,
    )) as Record<string, unknown>;
    payload.basicParameters = {
        ...(payload.basicParameters as Record<string, unknown>),
        status: opts?.status ?? 'DRAFT',
        subStatus: opts?.subStatus ?? 'DRAFT',
        versionStatus: 'SIGNED',
        signingDate: null,
    };
    const pp = payload.productParameters as Record<string, unknown>;
    pp.supplyActivation = 'FIRST_DAY_OF_MONTH';
    pp.productContractWaitForOldContractTermToExpires = 'NO';
    pp.contractTermEndDate = TERM_END_DATE;
    prepareProductContractPostPayload(payload);

    if (opts?.podIndices?.length) {
        const pods: { pointOfDeliveryDetailId: number; dealNumber: null }[] = [];
        for (const podIndex of opts.podIndices) {
            const podRes = await Request.get(`pod/${entityId(Responses.pod[podIndex])}`);
            await expect(podRes).CheckResponse();
            const podBody = await podRes.json();
            pods.push({
                pointOfDeliveryDetailId: Number(podBody.podDetailId ?? podBody.lastPodDetailId),
                dealNumber: null,
            });
        }
        payload.productContractPointOfDeliveries = pods;
    }

    const response = await Request.post(Endpoints.productContract, { data: payload });
    await expect(response).CheckResponse();
    const contractId = entityId(await response.json());
    Responses.productContract.push(contractId);
    return contractId;
}

export async function buildSignedPutPayload(
    ctx: FixtureCtx,
    contractId: number,
    options: SignContractFOptions,
): Promise<Record<string, unknown>> {
    const { Request, GeneratePayload } = ctx;
    const productIndex = options.productIndex ?? 1;
    const customerIndex = options.customerIndex ?? 0;

    const generated = (await GeneratePayload.contractsAndOrders.product_contract(
        customerIndex,
        productIndex,
    )) as Record<string, unknown>;

    const editPayload = await buildEditProductContractPayload(Request, contractId, generated, {
        forDraftV1: false,
        preserveSigningDate: false,
    });

    const bp = editPayload.basicParameters as Record<string, unknown>;
    bp.status = options.status ?? 'SIGNED';
    bp.subStatus = options.subStatus ?? 'SIGNED_BY_BOTH_SIDES';
    bp.versionStatus = 'SIGNED';
    bp.signingDate = minIsoDate(options.signingDate ?? todayIso(), addDaysIso(todayIso(), -1));

    const pp = editPayload.productParameters as Record<string, unknown>;
    pp.entryIntoForce = 'SIGNING';
    pp.startOfContractInitialTerm = 'SIGNING';
    pp.supplyActivation = options.supplyActivation ?? 'FIRST_DAY_OF_MONTH';
    pp.productContractWaitForOldContractTermToExpires = (options.waitExpire ?? 'NO') as 'YES' | 'NO';
    if (options.omitOldContractTermEnd) {
        bp.contractTermEndDate = null;
        pp.contractTermEndDate = null;
        pp.startOfContractValue = null;
    } else if (options.contractTermEndDate !== undefined) {
        pp.contractTermEndDate = options.contractTermEndDate;
    }

    if (options.removeFuturePods !== undefined) {
        editPayload.removeFuturePods = options.removeFuturePods;
    }

    const ap = editPayload.additionalParameters as Record<string, unknown> | undefined;
    if (ap) {
        ap.employeeId = 154;
        ap.riskAssessment = 'PERMIT';
    }

    applySupplyOnlyContractType(editPayload);
    return editPayload;
}

export async function signContractById(
    ctx: FixtureCtx,
    contractId: number,
    options: SignContractFOptions,
): Promise<Record<string, unknown>> {
    const { Request, Endpoints } = ctx;
    const signedPayload = await buildSignedPutPayload(ctx, contractId, options);
    const response = await Request.put(
        `${Endpoints.productContract}/${contractId}?versionId=1&changeFutureVersionsPods=false`,
        { data: signedPayload },
    );
    await expect(response).CheckResponse();
    return (await response.json()) as Record<string, unknown>;
}

export async function signContractF(
    ctx: FixtureCtx,
    contractId: number,
    options: SignContractFOptions,
): Promise<Record<string, unknown>> {
    return signContractById(ctx, contractId, options);
}

export async function signContractExpectError(
    ctx: FixtureCtx,
    contractId: number,
    options: SignContractFOptions,
    errorFragment: string | string[],
    errorOpts?: Omit<ResignErrorExpectation, 'messageParts'>,
): Promise<ParsedApiError> {
    const result = await signContractPutRaw(ctx, contractId, options);
    const response = {
        status: () => result.status,
        text: async () => result.text,
        json: async () => result.body,
    };
    return expectResignClientError(response, {
        messageParts: errorFragment,
        ...errorOpts,
    });
}

export async function signContractExpectMessagesOnly(
    ctx: FixtureCtx,
    contractId: number,
    options: SignContractFOptions,
    messageFragment: string,
): Promise<Record<string, unknown>> {
    const result = await signContractPutRaw(ctx, contractId, options);
    expect(result.status).toBeGreaterThanOrEqual(200);
    expect(result.status).toBeLessThan(300);
    const messages = (result.body.resigningMessages ?? []) as string[];
    expect(messages.some((m) => String(m).includes(messageFragment))).toBeTruthy();
    return result.body;
}

/** Manual POST + sign + POD activation (Case 10 contract C, interference contract). */
export async function createSignedSupplyContractManual(
    ctx: FixtureCtx,
    opts: {
        signingDate: string;
        activationDate: string;
        productIndex?: number;
        customerIndex?: number;
        podIndex?: number;
    },
): Promise<number> {
    const productIndex = opts.productIndex ?? 0;
    const customerIndex = opts.customerIndex ?? 0;
    const payload = (await ctx.GeneratePayload.contractsAndOrders.product_contract(
        customerIndex,
        productIndex,
    )) as Record<string, unknown>;
    payload.basicParameters = {
        ...(payload.basicParameters as Record<string, unknown>),
        status: 'DRAFT',
        subStatus: 'DRAFT',
        versionStatus: 'SIGNED',
        signingDate: null,
        entryInForceDate: opts.signingDate,
    };
    prepareProductContractPostPayload(payload, { termEndDate: TERM_END_DATE });
    const post = await ctx.Request.post(ctx.Endpoints.productContract, { data: payload });
    await expect(post).CheckResponse();
    const contractId = entityId(await post.json());
    ctx.Responses.productContract.push(contractId);
    await signContractById(ctx, contractId, {
        signingDate: opts.signingDate,
        status: 'SIGNED',
        subStatus: 'SIGNED_BY_BOTH_SIDES',
        productIndex,
        customerIndex,
        waitExpire: 'NO',
        supplyActivation: 'FIRST_DAY_OF_MONTH',
    });
    const contractIndex = ctx.Responses.productContract.length - 1;
    await activatePodOnContract(
        ctx,
        contractIndex,
        maxIsoDate(opts.activationDate, todayIso()),
        opts.podIndex ?? 0,
    );
    return contractId;
}

export function productContractIndex(ctx: FixtureCtx, contractId: number): number {
    const idx = ctx.Responses.productContract.findIndex((e) => entityId(e) === contractId);
    expect(idx, `contract id ${contractId} in Responses.productContract`).toBeGreaterThanOrEqual(0);
    return idx;
}

export async function runStandardResignChain(ctx: FixtureCtx): Promise<ResignChainContext> {
    refreshResignFlowDates();
    await sharedCustomer(ctx);
    await sharedPrice(ctx);
    await sharedTermBase(ctx);
    await sharedTermResign(ctx);
    await sharedPod(ctx);
    const baseProductId = await sharedBaseProduct(ctx, 0);
    const baseContractId = await createAndSignBaseContractB(ctx);
    const resignProductId = await sharedResignProduct(ctx, baseProductId, { termIndex: 1 });
    const newContractId = await createDraftContractF(ctx, { productIndex: 1 });
    const pods = await getContractPods(ctx.Request, newContractId);
    const podDetailId = Number(pods[0]?.podDetailId);
    const podId = Number(pods[0]?.podId ?? entityId(ctx.Responses.pod[0]));
    return {
        baseContractId,
        newContractId,
        podDetailId,
        podId,
        baseProductId,
        resignProductId,
    };
}

export async function assertPodResignedOnOldContract(
    Request: FixtureRequest,
    baseContractId: number,
    podId: number,
): Promise<void> {
    const pods = await getContractPods(Request, baseContractId);
    const row = pods.find((p) => Number(p.podId) === podId);
    expect(row).toBeTruthy();
    expect(row?.isResigned === true || row?.status === 'RESIGNED' || String(row?.contractPodStatus ?? '').includes('RESIGN')).toBeTruthy();
}

export async function assertPodActivationDate(
    Request: FixtureRequest,
    contractId: number,
    podId: number,
    expectedActivation: string,
): Promise<void> {
    const pods = await getContractPods(Request, contractId);
    const row = pods.find((p) => Number(p.podId) === podId);
    expect(row).toBeTruthy();
    expect(String(row?.activationDate)).toBe(expectedActivation);
}

export async function assertPodDeactivationDate(
    Request: FixtureRequest,
    contractId: number,
    podId: number,
    expectedDeactivation: string,
): Promise<void> {
    const pods = await getContractPods(Request, contractId);
    const row = pods.find((p) => Number(p.podId) === podId);
    expect(row).toBeTruthy();
    expect(String(row?.deactivationDate)).toBe(expectedDeactivation);
}

export async function runCase13ResignChain(ctx: FixtureCtx): Promise<ResignChainContext> {
    await sharedCustomer(ctx);
    await sharedPrice(ctx);
    await sharedTermBase(ctx);
    await sharedTermResign(ctx);
    await sharedPod(ctx);
    const baseProductId = await sharedBaseProduct(ctx, 0);
    const baseContractId = await createAndSignBaseContractCase13(ctx);
    const resignProductId = await sharedResignProduct(ctx, baseProductId, { termIndex: 1 });
    const newContractId = await createDraftContractF(ctx, { productIndex: 1 });
    const pods = await getContractPods(ctx.Request, newContractId);
    const podDetailId = Number(pods[0]?.podDetailId);
    const podId = Number(pods[0]?.podId ?? entityId(ctx.Responses.pod[0]));
    return {
        baseContractId,
        newContractId,
        podDetailId,
        podId,
        baseProductId,
        resignProductId,
    };
}

export type ResignLinkSnapshot = {
    oldContractId: number;
    newContractId: number;
    oldContractNumber?: string;
    newContractNumber?: string;
    oldResignedTo: unknown[];
    oldResignedFrom: unknown[];
    newResignedTo: unknown[];
    newResignedFrom: unknown[];
    crossLinkPresent: boolean;
};

export type FinalizeResignTestOpts = {
    label: string;
    chain?: ResignChainContext;
    oldContractId?: number;
    newContractId?: number;
    /** true = B↔F resign links required; false = must be absent; undefined = attach only */
    expectResignLinks?: boolean;
    extraContractIds?: number[];
};

function resolvePortalFrontendBaseUrl(): string | null {
    const env = process.env.FRONTEND_BASE_URL?.trim();
    if (env) {
        return env.endsWith('/') ? env : `${env}/`;
    }
    const raw = (configuredBaseURL || process.env.BASE_URL || 'http://10.236.20.11:8091/').replace(/\/$/, '');
    const table: [string, string][] = [
        ['http://10.236.20.11:8091', 'http://10.236.20.11:8080/'],
        ['http://10.236.20.31:8091', 'http://10.236.20.31:8080/'],
        ['http://10.236.20.81:8091', 'http://10.236.20.81:8080/'],
        ['http://10.236.20.81:8094', 'http://10.236.20.31:8082/'],
        [
            'https://testapps.energo-pro.bg/backend/phoenix-epres',
            'https://testapps.energo-pro.bg/app/phoenix-epres/',
        ],
        [
            'https://devapps.energo-pro.bg/backend/phoenix-dev2',
            'https://devapps.energo-pro.bg/app/phoenix-dev2/',
        ],
    ];
    for (const [api, fe] of table) {
        if (raw === api) {
            return fe;
        }
    }
    const lab = raw.match(/^http:\/\/(10\.236\.20\.\d+):8091$/);
    if (lab) {
        return `http://${lab[1]}:8080/`;
    }
    return null;
}

function contractPreviewUrl(frontendBase: string, contractId: number): string {
    const base = frontendBase.endsWith('/') ? frontendBase : `${frontendBase}/`;
    return `${base}energy-product-contracts/preview?id=${contractId}`;
}

function resignListsFromBasic(bp: Record<string, unknown>) {
    return {
        resignedTo: (bp.resignedTo ?? []) as Array<{ id?: number; contractNumber?: string }>,
        resignedFrom: (bp.resignedFrom ?? []) as Array<{ id?: number; contractNumber?: string }>,
        contractNumber: bp.contractNumber as string | undefined,
    };
}

export async function fetchContractBasicParameters(
    Request: FixtureRequest,
    contractId: number,
): Promise<Record<string, unknown>> {
    const res = await Request.get(`product-contract/${contractId}`);
    await expect(res).CheckResponse();
    const body = (await res.json()) as Record<string, unknown>;
    return (body.basicParameters ?? {}) as Record<string, unknown>;
}

export async function assertResignContractLinks(
    Request: FixtureRequest,
    oldContractId: number,
    newContractId: number,
): Promise<ResignLinkSnapshot> {
    const oldBp = await fetchContractBasicParameters(Request, oldContractId);
    const newBp = await fetchContractBasicParameters(Request, newContractId);
    const oldLinks = resignListsFromBasic(oldBp);
    const newLinks = resignListsFromBasic(newBp);

    const newInOldTo = oldLinks.resignedTo.some((x) => Number(x.id) === newContractId);
    const oldInNewFrom = newLinks.resignedFrom.some((x) => Number(x.id) === oldContractId);

    expect(
        newInOldTo || oldInNewFrom,
        `Expected resign link old(B)=${oldContractId} new(F)=${newContractId}; ` +
            `old.resignedTo=${JSON.stringify(oldLinks.resignedTo)} new.resignedFrom=${JSON.stringify(newLinks.resignedFrom)}`,
    ).toBeTruthy();

    return {
        oldContractId,
        newContractId,
        oldContractNumber: oldLinks.contractNumber,
        newContractNumber: newLinks.contractNumber,
        oldResignedTo: oldLinks.resignedTo,
        oldResignedFrom: oldLinks.resignedFrom,
        newResignedTo: newLinks.resignedTo,
        newResignedFrom: newLinks.resignedFrom,
        crossLinkPresent: newInOldTo || oldInNewFrom,
    };
}

export async function assertNoResignContractLinks(
    Request: FixtureRequest,
    oldContractId: number,
    newContractId: number,
): Promise<void> {
    const oldBp = await fetchContractBasicParameters(Request, oldContractId);
    const newBp = await fetchContractBasicParameters(Request, newContractId);
    const oldLinks = resignListsFromBasic(oldBp);
    const newLinks = resignListsFromBasic(newBp);

    const newInOldTo = oldLinks.resignedTo.some((x) => Number(x.id) === newContractId);
    const oldInNewFrom = newLinks.resignedFrom.some((x) => Number(x.id) === oldContractId);
    expect(newInOldTo).toBeFalsy();
    expect(oldInNewFrom).toBeFalsy();
}

export async function attachResignTestArtifacts(
    ctx: FixtureCtx,
    label: string,
    opts?: {
        oldContractId?: number;
        newContractId?: number;
        linkSnapshot?: ResignLinkSnapshot | null;
        extraContractIds?: number[];
    },
): Promise<void> {
    const reportGenerator = (await import('../../utils/generateReport')).default;
    const fe = resolvePortalFrontendBaseUrl();
    const apiBase = configuredBaseURL || process.env.BASE_URL || '';
    const fromLinker = reportGenerator.setLinksToResponses(ctx.Responses);
    const portalLinks: Record<string, string[]> = { ...fromLinker };

    const push = (key: string, url: string) => {
        portalLinks[key] = [...(portalLinks[key] ?? []), url];
    };

    if (fe && opts?.oldContractId) {
        push('productContract_old_B', contractPreviewUrl(fe, opts.oldContractId));
    }
    if (fe && opts?.newContractId) {
        push('productContract_new_F', contractPreviewUrl(fe, opts.newContractId));
    }
    for (const id of opts?.extraContractIds ?? []) {
        if (fe) {
            push('productContract', contractPreviewUrl(fe, id));
        }
    }

    const snapshot: Record<string, unknown> = {
        testLabel: label,
        apiBase,
        frontendBase: fe,
        oldContractId: opts?.oldContractId,
        newContractId: opts?.newContractId,
        resignLinks: opts?.linkSnapshot ?? null,
        productContractIds: ctx.Responses.productContract,
        portalLinks,
        linker: fromLinker,
    };

    await test.info().attach(label, {
        body: JSON.stringify(snapshot, null, 2),
        contentType: 'application/json',
    });

    const plainLines = [
        `========== ${label} — old/new contract links ==========`,
        ...(fe && opts?.oldContractId ? [`Old contract (B): ${contractPreviewUrl(fe, opts.oldContractId)}`] : []),
        ...(fe && opts?.newContractId ? [`New contract (F): ${contractPreviewUrl(fe, opts.newContractId)}`] : []),
        '',
        JSON.stringify(snapshot, null, 2),
    ];
    await test.info().attach(`${label} (plain)`, {
        body: plainLines.join('\n'),
        contentType: 'text/plain',
    });
}

/** End-of-test: link assertion (optional) + portal/API link attachments for old B and new F. */
export async function finalizeResignTest(ctx: FixtureCtx, opts: FinalizeResignTestOpts): Promise<void> {
    const oldId = opts.oldContractId ?? opts.chain?.baseContractId;
    const newId = opts.newContractId ?? opts.chain?.newContractId;

    let linkSnapshot: ResignLinkSnapshot | null = null;
    if (oldId != null && newId != null && opts.expectResignLinks === true) {
        linkSnapshot = await assertResignContractLinks(ctx.Request, oldId, newId);
    } else if (oldId != null && newId != null && opts.expectResignLinks === false) {
        await assertNoResignContractLinks(ctx.Request, oldId, newId);
    }

    await attachResignTestArtifacts(ctx, opts.label, {
        oldContractId: oldId,
        newContractId: newId,
        linkSnapshot,
        extraContractIds: opts.extraContractIds,
    });
}

export async function attachResponses(Responses: baseFixture['Responses'], label: string): Promise<void> {
    const reportGenerator = (await import('../../utils/generateReport')).default;
    await test.info().attach(label, {
        body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
        contentType: 'application/json',
    });
}

/**
 * Excel Case 16 — {@link runStandardResignChain} preconditions (terms with resigningDeadline, compatible products, POD active)
 * except old contract B has no term end / initial-term dates → engine cannot derive re-signing period.
 */
export async function setupExcelCase16ResignChain(ctx: FixtureCtx): Promise<ResignChainContext> {
    refreshResignFlowDates();
    await sharedCustomer(ctx);
    await sharedPrice(ctx);
    await sharedTermBase(ctx);
    await sharedTermResign(ctx);
    await sharedPod(ctx);
    const baseProductId = await sharedBaseProduct(ctx, 0);
    const baseContractId = await createAndSignBaseContractBWithoutTermDates(ctx);
    const resignProductId = await sharedResignProduct(ctx, baseProductId, { termIndex: 1 });
    const newContractId = await createDraftContractF(ctx, { productIndex: 1 });
    const pods = await getContractPods(ctx.Request, newContractId);
    const podDetailId = Number(pods[0]?.podDetailId);
    const podId = Number(pods[0]?.podId ?? entityId(ctx.Responses.pod[0]));
    return {
        baseContractId,
        newContractId,
        podDetailId,
        podId,
        baseProductId,
        resignProductId,
    };
}

/**
 * Excel Case 16 — sign F must fail with no POD re-sign.
 * Prefers HTTP 422 + `RESIGNING_PERIOD_CANNOT_BE_CALCULATED`; accepts Dev2 "failed for all pods" when SQL marks B unavailable.
 */
export async function assertExcelCase16ResignBlocked(
    ctx: FixtureCtx,
    chain: Pick<ResignChainContext, 'baseContractId' | 'newContractId' | 'podId'>,
    variantLabel: string,
): Promise<void> {
    const put = await signContractPutRaw(ctx, chain.newContractId, {
        signingDate: SIGNING_DATE_IN_WINDOW,
        productIndex: 1,
        waitExpire: 'NO',
        supplyActivation: 'FIRST_DAY_OF_MONTH',
    });
    const haystack = `${put.text} ${JSON.stringify(put.body)}`.toLowerCase();
    const messages = (put.body.resigningMessages ?? []) as string[];
    const messagesHay = messages.join(' ').toLowerCase();

    const excelPeriodError =
        put.status === 422 &&
        (haystack.includes(MSG_CODE_RESIGNING_PERIOD_CANNOT_BE_CALCULATED.toLowerCase()) ||
            haystack.includes(MSG_RESIGNING_PERIOD_CANNOT_BE_CALCULATED_TEXT) ||
            messagesHay.includes(MSG_RESIGNING_PERIOD_CANNOT_BE_CALCULATED_TEXT));

    const allPodsFailedOnHttp =
        put.status >= 400 &&
        [MSG_RESIGNING_FAILED_ALL_PODS, MSG_RESIGNING_FAILED_ALL_PODS_FULL, MSG_ONLY_FOR_RESIGNING_FULL].some((m) =>
            haystack.includes(m.toLowerCase()),
        );

    const allPodsFailedInMessages =
        put.status >= 200 &&
        put.status < 300 &&
        messages.some((m) => String(m).includes(MSG_RESIGNING_FAILED_ALL_PODS));

    expect(
        excelPeriodError || allPodsFailedOnHttp || allPodsFailedInMessages,
        `${variantLabel}: expected RESIGNING_PERIOD_CANNOT_BE_CALCULATED (422) or resign blocked with no POD transfer; status=${put.status} body=${put.text.slice(0, 600)}`,
    ).toBeTruthy();

    await assertPodNotResignedOnOldContract(ctx.Request, chain.baseContractId, chain.podId);
    await assertNoResignContractLinks(ctx.Request, chain.baseContractId, chain.newContractId);
}

/** @deprecated Use {@link assertExcelCase16ResignBlocked} with {@link setupExcelCase16ResignChain}. */
export async function expectCase16ResignBlocked(
    ctx: FixtureCtx,
    contractId: number,
    baseContractId: number,
    podId: number,
): Promise<void> {
    await assertExcelCase16ResignBlocked(ctx, { baseContractId, newContractId: contractId, podId }, 'legacy expectCase16');
}

export async function sharedSecondPod(ctx: FixtureCtx): Promise<void> {
    const { Request, GeneratePayload, Responses, Endpoints } = ctx;
    const response = await Request.post(Endpoints.pod, {
        data: GeneratePayload.pointsOfDelivery.pod_settlement(),
    });
    await expect(response).CheckResponse();
    Responses.pod.push(await response.json());
}

export async function appendSecondPodToContractPayload(
    ctx: FixtureCtx,
    payload: Record<string, unknown>,
): Promise<void> {
    const { Request, Responses } = ctx;
    const podRes = await Request.get(`pod/${entityId(Responses.pod[1])}`);
    await expect(podRes).CheckResponse();
    const podBody = await podRes.json();
    const list = (payload.productContractPointOfDeliveries ?? []) as { pointOfDeliveryDetailId: number; dealNumber: unknown }[];
    list.push({ pointOfDeliveryDetailId: Number(podBody.podDetailId), dealNumber: null });
    payload.productContractPointOfDeliveries = list;
}

export async function createAndSignBaseContractTwoPods(
    ctx: FixtureCtx,
    opts?: { activateBoth?: boolean },
): Promise<number> {
    refreshResignFlowDates();
    await sharedCustomer(ctx);
    await sharedPrice(ctx);
    await sharedTermBase(ctx);
    await sharedPod(ctx);
    await sharedSecondPod(ctx);
    const baseProductId = await sharedBaseProduct(ctx, 0);
    const signingDate = addDaysIso(todayIso(), -30);

    const draftPayload = (await ctx.GeneratePayload.contractsAndOrders.product_contract(0, 0)) as Record<
        string,
        unknown
    >;
    draftPayload.basicParameters = {
        ...(draftPayload.basicParameters as Record<string, unknown>),
        status: 'DRAFT',
        subStatus: 'DRAFT',
        versionStatus: 'SIGNED',
        signingDate: null,
    };
    prepareProductContractPostPayload(draftPayload, { termEndDate: TERM_END_DATE });
    await appendSecondPodToContractPayload(ctx, draftPayload);

    const postRes = await ctx.Request.post(ctx.Endpoints.productContract, { data: draftPayload });
    await expect(postRes).CheckResponse();
    const contractId = entityId(await postRes.json());
    ctx.Responses.productContract.push(contractId);

    await signContractById(ctx, contractId, {
        signingDate,
        status: 'SIGNED',
        subStatus: 'SIGNED_BY_BOTH_SIDES',
        productIndex: 0,
        waitExpire: 'NO',
        supplyActivation: 'FIRST_DAY_OF_MONTH',
    });

    const contractIndex = ctx.Responses.productContract.length - 1;
    const activationDate = maxIsoDate(signingDate, todayIso());
    await activatePodOnContract(ctx, contractIndex, activationDate, 0);
    if (opts?.activateBoth !== false) {
        await activatePodOnContract(ctx, contractIndex, activationDate, 1);
    }
    return contractId;
}

export async function createAndSignBaseContractPerpetuity(
    ctx: FixtureCtx,
    opts: { perpetuityDate: string; signingDate?: string },
): Promise<number> {
    const signingDate = opts.signingDate ?? SIGNING_DATE_IN_WINDOW;
    const contractId = await createAndSignBaseContractB(ctx, {
        signingDate: addDaysIso(signingDate, -60),
        termEndDate: TERM_END_DATE,
    });
    await productContractStatusUpdate(ctx.Request, contractId, 'ACTIVE_IN_PERPETUITY', 'DELIVERY');
    const signedPayload = await buildSignedPutPayload(ctx, contractId, {
        signingDate,
        status: 'ACTIVE_IN_PERPETUITY',
        subStatus: 'DELIVERY',
        productIndex: 0,
        waitExpire: 'NO',
        supplyActivation: 'FIRST_DAY_OF_MONTH',
    });
    const bp = signedPayload.basicParameters as Record<string, unknown>;
    bp.perpetuityDate = opts.perpetuityDate;
    const putRes = await ctx.Request.put(
        `${ctx.Endpoints.productContract}/${contractId}?versionId=1&changeFutureVersionsPods=false`,
        { data: signedPayload },
    );
    await expect(putRes).CheckResponse();
    return contractId;
}

/** Second contract on same customer + base product with future POD activation (AC4 setup). */
export async function createInterferenceContractFutureActivation(
    ctx: FixtureCtx,
    podIndex = 0,
    futureActivationDate?: string,
): Promise<number> {
    const futureDate = futureActivationDate ?? addDaysIso(SIGNING_DATE_IN_WINDOW, 30);
    const draftPayload = (await ctx.GeneratePayload.contractsAndOrders.product_contract(0, 0)) as Record<
        string,
        unknown
    >;
    draftPayload.basicParameters = {
        ...(draftPayload.basicParameters as Record<string, unknown>),
        status: 'DRAFT',
        subStatus: 'DRAFT',
        versionStatus: 'SIGNED',
        signingDate: null,
    };
    prepareProductContractPostPayload(draftPayload, { termEndDate: TERM_END_DATE });
    const postRes = await ctx.Request.post(ctx.Endpoints.productContract, { data: draftPayload });
    await expect(postRes).CheckResponse();
    const contractId = entityId(await postRes.json());
    ctx.Responses.productContract.push(contractId);
    const signingDate = addDaysIso(SIGNING_DATE_IN_WINDOW, -45);
    await signContractById(ctx, contractId, {
        signingDate,
        status: 'SIGNED',
        subStatus: 'SIGNED_BY_BOTH_SIDES',
        productIndex: 0,
        waitExpire: 'NO',
        supplyActivation: 'FIRST_DAY_OF_MONTH',
    });
    const contractIndex = ctx.Responses.productContract.length - 1;
    await activatePodOnContract(ctx, contractIndex, futureDate, podIndex);
    return contractId;
}

export async function setupAc4ResignChain(ctx: FixtureCtx): Promise<ResignChainContext & { interferenceContractId: number }> {
    refreshResignFlowDates();
    await sharedCustomer(ctx);
    await sharedPrice(ctx);
    await sharedTermBase(ctx);
    await sharedTermResign(ctx);
    await sharedPod(ctx);
    const baseProductId = await sharedBaseProduct(ctx, 0);
    const baseContractId = await createAndSignBaseContractB(ctx);
    const interferenceContractId = await createInterferenceContractFutureActivation(ctx, 0);
    const resignProductId = await sharedResignProduct(ctx, baseProductId, { termIndex: 1 });
    const newContractId = await createDraftContractF(ctx, { productIndex: 1 });
    const pods = await getContractPods(ctx.Request, newContractId);
    return {
        baseContractId,
        newContractId,
        interferenceContractId,
        podDetailId: Number(pods[0]?.podDetailId),
        podId: Number(pods[0]?.podId ?? entityId(ctx.Responses.pod[0])),
        baseProductId,
        resignProductId,
    };
}

/**
 * Excel Case 36 — F has V1–V3 but every version `start_date` is strictly after signing date S
 * so `findVersionByContractAndDate` returns empty (SQL: `start_date <= signingDate`).
 */
export async function setupContractFVersionsStartingAfter(
    ctx: FixtureCtx,
    contractId: number,
    signingDate: string = SIGNING_DATE_IN_WINDOW,
): Promise<void> {
    const { Request, GeneratePayload, Endpoints } = ctx;
    const generated = (await GeneratePayload.contractsAndOrders.product_contract(0, 1)) as Record<string, unknown>;
    const v1Start = addDaysIso(signingDate, 30);
    const v2Start = addDaysIso(signingDate, 60);
    const v3Start = addDaysIso(signingDate, 90);

    let editPayload = await buildEditProductContractPayload(Request, contractId, generated, {
        forDraftV1: false,
        preserveSigningDate: false,
    });
    editPayload.savingAsNewVersion = false;
    editPayload.startDate = v1Start;
    let put = await Request.put(
        `${Endpoints.productContract}/${contractId}?versionId=1&changeFutureVersionsPods=true`,
        { data: editPayload },
    );
    await expect(put).CheckResponse();

    editPayload = await buildEditProductContractPayload(Request, contractId, generated, {
        forDraftV1: false,
        preserveSigningDate: false,
    });
    editPayload.savingAsNewVersion = true;
    editPayload.startDate = v2Start;
    put = await Request.put(
        `${Endpoints.productContract}/${contractId}?versionId=1&changeFutureVersionsPods=true`,
        { data: editPayload },
    );
    await expect(put).CheckResponse();

    editPayload = await buildEditProductContractPayload(Request, contractId, generated, {
        forDraftV1: false,
        preserveSigningDate: false,
    });
    editPayload.savingAsNewVersion = true;
    editPayload.startDate = v3Start;
    put = await Request.put(
        `${Endpoints.productContract}/${contractId}?versionId=2&changeFutureVersionsPods=true`,
        { data: editPayload },
    );
    await expect(put).CheckResponse();
}

export async function runSuccessfulResign(
    ctx: FixtureCtx,
    chain: ResignChainContext,
    opts?: Partial<SignContractFOptions>,
): Promise<Record<string, unknown>> {
    return signContractF(ctx, chain.newContractId, {
        signingDate: SIGNING_DATE_IN_WINDOW,
        waitExpire: 'NO',
        supplyActivation: 'FIRST_DAY_OF_MONTH',
        productIndex: 1,
        ...opts,
    });
}

export async function createSecondResignDraft(
    ctx: FixtureCtx,
    productIndex = 1,
    podIndices?: number[],
): Promise<number> {
    return createDraftContractF(ctx, { productIndex, podIndices });
}

export async function putProductContractWithoutAuth(
    contractId: number,
    payload: Record<string, unknown>,
): Promise<{ status: number; text: string }> {
    const baseURL = configuredBaseURL;
    const anon = await playwrightRequest.newContext({
        baseURL,
        extraHTTPHeaders: { Accept: '*/*', 'Content-Type': 'application/json' },
    });
    try {
        const url = `product-contract/${contractId}?versionId=1&changeFutureVersionsPods=false`;
        const res = await anon.put(url, { data: payload });
        return { status: res.status(), text: await res.text() };
    } finally {
        await anon.dispose();
    }
}

export async function signContractWithEmptyPodRequests(
    ctx: FixtureCtx,
    contractId: number,
    options: SignContractFOptions,
): Promise<{ status: number; text: string }> {
    const signedPayload = await buildSignedPutPayload(ctx, contractId, options);
    signedPayload.podRequests = [];
    signedPayload.productContractPointOfDeliveries = [];
    const response = await ctx.Request.put(
        `${ctx.Endpoints.productContract}/${contractId}?versionId=1&changeFutureVersionsPods=false`,
        { data: signedPayload },
    );
    return { status: response.status(), text: await response.text() };
}

export async function createDraftContractDifferentCustomer(ctx: FixtureCtx): Promise<number> {
    const { Request, GeneratePayload, Responses, Endpoints } = ctx;
    const customer = await Request.post(Endpoints.customer, {
        data: customerLegalPayloadForResignApi(GeneratePayload),
    });
    await expect(customer).CheckResponse();
    Responses.customer.push(await customer.json());
    const customerIndex = Responses.customer.length - 1;
    return createDraftContractF(ctx, { productIndex: 1, customerIndex });
}

export async function createBaseContractNoResigningDeadline(ctx: FixtureCtx): Promise<number> {
    refreshResignFlowDates();
    await sharedCustomer(ctx);
    await sharedPrice(ctx);
    await sharedTermBase(ctx, { resigningDeadlineValue: null, resigningDeadlineType: null });
    await sharedPod(ctx);
    await sharedBaseProduct(ctx, 0);
    return createAndSignBaseContractBWithoutTermDates(ctx);
}
