/**
 * Pulling (REG-964) expected-result helpers.
 *
 * Spec: Confluence "Rules for pulling" (page 256049175) + Jira REG-965..REG-972.
 * Runtime checks use GET /invoice, /invoice/detailed-data, /invoice/summary-data,
 * GET /billing-run/{id}, GET /billing-run/draft-invoices (same paths as billingValidator).
 *
 * REG-969 data setup lives in pullingDataPrep.REG_969().
 */
import { expect, test } from '../../../backend/fixtures/baseFixture';
import type { baseFixture } from '../../../backend/fixtures/baseFixture';

type PullingApi = Pick<baseFixture, 'Request' | 'Endpoints'>;
type PullingCtx = PullingApi & Pick<baseFixture, 'Responses'>;

type InvoiceDetailedRow = {
    pointOfDelivery?: string | null;
    unitPrice?: string | number | null;
    totalVolumes?: string | number | null;
    value?: string | number | null;
    priceComponent?: string | null;
};

export function asId(value: unknown): number {
    if (typeof value === 'number' && Number.isFinite(value)) return value;
    if (typeof value === 'string' && value.trim() !== '' && Number.isFinite(Number(value))) {
        return Number(value);
    }
    if (value && typeof value === 'object' && 'id' in value) {
        return asId((value as { id: unknown }).id);
    }
    throw new Error(`Cannot resolve numeric id from ${JSON.stringify(value)}`);
}

export function amount(value: string | number | null | undefined): number {
    return Number(value ?? 0);
}

export async function getPodIdentifier(api: PullingApi, podRef: unknown): Promise<string> {
    const res = await api.Request.get(`${api.Endpoints.pod}/${asId(podRef)}?versionId=1`);
    await expect(res).CheckResponse();
    const body = await res.json();
    expect(body.identifier, 'POD identifier missing').toBeTruthy();
    return String(body.identifier);
}

export async function getInvoice(api: PullingApi, invoiceId: unknown): Promise<any> {
    const res = await api.Request.get(`${api.Endpoints.invoice}?id=${asId(invoiceId)}`);
    await expect(res).CheckResponse();
    return res.json();
}

export async function getInvoiceDetailedRows(api: PullingApi, invoiceId: unknown): Promise<InvoiceDetailedRow[]> {
    const res = await api.Request.get(
        `${api.Endpoints.invoice}/detailed-data?id=${asId(invoiceId)}&page=0&size=100`
    );
    await expect(res).CheckResponse();
    return ((await res.json()).content ?? []) as InvoiceDetailedRow[];
}

export async function getInvoiceSummaryRows(api: PullingApi, invoiceId: unknown): Promise<any[]> {
    const res = await api.Request.get(
        `${api.Endpoints.invoice}/summary-data?id=${asId(invoiceId)}&page=0&size=25`
    );
    await expect(res).CheckResponse();
    return (await res.json()).content ?? [];
}

export async function getBillingRun(api: PullingApi, billingRunRef: unknown): Promise<any> {
    const res = await api.Request.get(`${api.Endpoints.billingRun}/${asId(billingRunRef)}`);
    await expect(res).CheckResponse();
    return res.json();
}

export async function getDraftInvoiceCount(api: PullingApi, billingRunRef: unknown): Promise<number> {
    const res = await api.Request.get(
        `${api.Endpoints.billingRun}/draft-invoices?id=${asId(billingRunRef)}&page=0&size=25`
    );
    await expect(res).CheckResponse();
    const body = await res.json();
    return body.content?.length ?? 0;
}

export async function getPriceFormula(api: PullingApi, priceComponentRef: unknown): Promise<number> {
    const res = await api.Request.get(`${api.Endpoints.priceComponent}/${asId(priceComponentRef)}`);
    await expect(res).CheckResponse();
    const body = await res.json();
    const formula = Number(body.priceFormula);
    expect(Number.isFinite(formula), `priceFormula missing on price-components/${asId(priceComponentRef)}`).toBe(true);
    return formula;
}

export async function collectPodsOnInvoices(api: PullingApi, invoiceIds: unknown[]): Promise<Set<string>> {
    const pods = new Set<string>();
    for (const invoiceId of invoiceIds) {
        const rows = await getInvoiceDetailedRows(api, invoiceId);
        for (const row of rows) {
            const pod = String(row.pointOfDelivery ?? '').trim();
            if (pod) pods.add(pod);
        }
    }
    return pods;
}

export async function assertNoInvoicesGenerated(
    ctx: PullingCtx,
    reason: string
): Promise<void> {
    const invoiceCount = ctx.Responses.invoice?.length ?? 0;
    expect(invoiceCount, reason).toBe(0);

    const billingRuns = ctx.Responses.billingRun ?? [];
    expect(billingRuns.length, `${reason} — no billing run stashed`).toBeGreaterThan(0);

    for (const raw of billingRuns) {
        const runId = asId(raw);
        const draftCount = await getDraftInvoiceCount(ctx, runId);
        expect(
            draftCount,
            `${reason} (draft-invoices for billing run ${runId})`
        ).toBe(0);
    }
}

/**
 * REG-965 / REG-971: no activation gap → invoice on the latest (second) contract,
 * priced with that contract's product / price component.
 */
export async function assertPulledIntoLatestContract(
    ctx: PullingCtx,
    opts: {
        expectedInvoiceCount?: number;
        latestContractIndex?: number;
        latestProductIndex?: number;
        latestPriceComponentIndex?: number;
        includedPodIndexes?: number[];
        excludedPodIndexes?: number[];
    } = {}
): Promise<void> {
    const expectedInvoiceCount = opts.expectedInvoiceCount ?? 1;
    const latestContractIndex = opts.latestContractIndex ?? 1;
    const latestProductIndex = opts.latestProductIndex ?? 1;
    const latestPriceComponentIndex = opts.latestPriceComponentIndex ?? 1;

    const invoiceIds = ctx.Responses.invoice ?? [];
    expect(invoiceIds.length, 'Expected pulled invoice(s) on the latest contract').toBe(expectedInvoiceCount);

    const expectedContractId = asId(ctx.Responses.productContract[latestContractIndex]);
    const expectedProductId = asId(ctx.Responses.product[latestProductIndex]);
    const expectedUnitPrice = await getPriceFormula(ctx, ctx.Responses.priceComponent[latestPriceComponentIndex]);

    const includedPods = await Promise.all(
        (opts.includedPodIndexes ?? [0]).map((i) => getPodIdentifier(ctx, ctx.Responses.pod[i]))
    );
    const excludedPods = await Promise.all(
        (opts.excludedPodIndexes ?? []).map((i) => getPodIdentifier(ctx, ctx.Responses.pod[i]))
    );

    const foundPods = new Set<string>();
    const foundContractIds = new Set<number>();
    const foundProductIds = new Set<number>();

    for (const invoiceId of invoiceIds) {
        const invoice = await getInvoice(ctx, invoiceId);
        expect(invoice.invoiceStatus, `invoice ${invoiceId} status`).toBe('REAL');
        expect(invoice.productContract?.id, `invoice ${invoiceId} must have a product contract`).toBeTruthy();
        foundContractIds.add(asId(invoice.productContract));
        if (invoice.product?.id != null) foundProductIds.add(asId(invoice.product));

        const rows = await getInvoiceDetailedRows(ctx, invoiceId);
        expect(rows.length, `invoice ${invoiceId} detailed-data is empty`).toBeGreaterThan(0);

        const energyRows = rows.filter((row) => amount(row.totalVolumes) !== 0 || amount(row.value) !== 0);
        expect(energyRows.length, `invoice ${invoiceId} has no billed energy lines`).toBeGreaterThan(0);

        for (const row of energyRows) {
            expect(
                amount(row.unitPrice),
                `invoice ${invoiceId} unitPrice must match latest-contract priceFormula ${expectedUnitPrice}`
            ).toBeCloseTo(expectedUnitPrice, 4);
        }

        const summaryRows = await getInvoiceSummaryRows(ctx, invoiceId);
        expect(summaryRows.length, `invoice ${invoiceId} summary-data is empty`).toBeGreaterThan(0);
        for (const row of summaryRows) {
            if (amount(row.unitPrice) !== 0) {
                expect(amount(row.unitPrice)).toBeCloseTo(expectedUnitPrice, 4);
            }
        }

        for (const row of rows) {
            const pod = String(row.pointOfDelivery ?? '').trim();
            if (pod) foundPods.add(pod);
        }
    }

    expect(
        foundContractIds.has(expectedContractId),
        `Expected invoice on latest contract ${expectedContractId}, found [${[...foundContractIds].join(', ')}]`
    ).toBe(true);
    expect(foundContractIds.has(asId(ctx.Responses.productContract[0]))).toBe(false);

    if (foundProductIds.size > 0) {
        expect(
            foundProductIds.has(expectedProductId),
            `Expected latest-contract product ${expectedProductId}, found [${[...foundProductIds].join(', ')}]`
        ).toBe(true);
    }

    for (const pod of includedPods) {
        expect(foundPods.has(pod), `Expected POD ${pod} on pulled invoice detailed-data; found [${[...foundPods].join(', ')}]`).toBe(true);
    }
    for (const pod of excludedPods) {
        expect(foundPods.has(pod), `POD ${pod} should not be on pulled invoice detailed-data; found [${[...foundPods].join(', ')}]`).toBe(false);
    }
}

/**
 * REG-966: customer/contract levels invoice all PODs from the pulled-to contract;
 * POD level invoices only the selected (pulled) POD.
 */
export async function assertCustomerContractPodLevelPulling(ctx: PullingCtx): Promise<void> {
    const invoiceIds = ctx.Responses.invoice ?? [];
    expect(invoiceIds.length, 'REG-966 expected one invoice per billing run (contract, customer, POD)').toBe(3);
    expect(ctx.Responses.billingRun?.length, 'REG-966 expected 3 billing runs').toBe(3);

    const pullingPod = await getPodIdentifier(ctx, ctx.Responses.pod[0]);
    const extraPod = await getPodIdentifier(ctx, ctx.Responses.pod[1]);
    const latestContractId = asId(ctx.Responses.productContract[1]);

    const invoicesByRun = new Map<number, unknown[]>();
    for (const invoiceId of invoiceIds) {
        const invoice = await getInvoice(ctx, invoiceId);
        const runId = asId(invoice.billingRun);
        const list = invoicesByRun.get(runId) ?? [];
        list.push(invoiceId);
        invoicesByRun.set(runId, list);
    }

    for (const raw of ctx.Responses.billingRun) {
        const runId = asId(raw);
        const billingRun = await getBillingRun(ctx, runId);
        const level = billingRun.standardBillingRunParameters?.applicationLevel as 'CUSTOMER' | 'CONTRACT' | 'POD';
        expect(['CUSTOMER', 'CONTRACT', 'POD'], `billing run ${runId} applicationLevel`).toContain(level);
        const runInvoiceIds = invoicesByRun.get(runId) ?? [];
        expect(runInvoiceIds.length, `billing run ${runId} (${level}) produced no invoices`).toBe(1);

        const pods = await collectPodsOnInvoices(ctx, runInvoiceIds);
        const invoice = await getInvoice(ctx, runInvoiceIds[0]);

        if (level === 'POD') {
            expect(pods.has(pullingPod), `[POD] expected pulling POD ${pullingPod}; found [${[...pods].join(', ')}]`).toBe(true);
            expect(pods.has(extraPod), `[POD] extra POD ${extraPod} must not be invoiced`).toBe(false);
            continue;
        }

        expect(
            pods.has(pullingPod),
            `[${level}] expected pulled POD ${pullingPod}; found [${[...pods].join(', ')}]`
        ).toBe(true);
        expect(
            pods.has(extraPod),
            `[${level}] expected extra POD from pulled-to contract ${extraPod}; found [${[...pods].join(', ')}]`
        ).toBe(true);

        if (level === 'CONTRACT' && invoice.productContract?.id != null) {
            expect(asId(invoice.productContract), `[CONTRACT] invoice must be on pulled-to contract`).toBe(latestContractId);
        }
    }
}

/**
 * REG-972: two successful pulling chains → invoice POD1 on contract 2, POD2 on contract 3.
 */
export async function assertTwoPullingChains(ctx: PullingCtx): Promise<void> {
    const invoiceIds = ctx.Responses.invoice ?? [];
    expect(invoiceIds.length, 'REG-972 expected 2 invoices (one per pulling chain)').toBe(2);

    const pod1 = await getPodIdentifier(ctx, ctx.Responses.pod[0]);
    const pod2 = await getPodIdentifier(ctx, ctx.Responses.pod[1]);
    const contract2 = asId(ctx.Responses.productContract[1]);
    const contract3 = asId(ctx.Responses.productContract[2]);
    const contract1 = asId(ctx.Responses.productContract[0]);

    const podsByContract = new Map<number, Set<string>>();

    for (const invoiceId of invoiceIds) {
        const invoice = await getInvoice(ctx, invoiceId);
        expect(invoice.productContract?.id, `invoice ${invoiceId} missing productContract`).toBeTruthy();
        const contractId = asId(invoice.productContract);
        expect(contractId, 'REG-972 must not invoice the first (source) contract').not.toBe(contract1);

        const rows = await getInvoiceDetailedRows(ctx, invoiceId);
        const pods = podsByContract.get(contractId) ?? new Set<string>();
        for (const row of rows) {
            const pod = String(row.pointOfDelivery ?? '').trim();
            if (pod) pods.add(pod);
        }
        podsByContract.set(contractId, pods);
    }

    expect(podsByContract.has(contract2), `Missing invoice for contract 2 (${contract2})`).toBe(true);
    expect(podsByContract.has(contract3), `Missing invoice for contract 3 (${contract3})`).toBe(true);
    expect(
        podsByContract.get(contract2)!.has(pod1),
        `Contract 2 invoice must include POD 1 (${pod1}); found [${[...podsByContract.get(contract2)!].join(', ')}]`
    ).toBe(true);
    expect(
        podsByContract.get(contract2)!.has(pod2),
        `Contract 2 invoice must not include POD 2 (${pod2})`
    ).toBe(false);
    expect(
        podsByContract.get(contract3)!.has(pod2),
        `Contract 3 invoice must include POD 2 (${pod2}); found [${[...podsByContract.get(contract3)!].join(', ')}]`
    ).toBe(true);
    expect(
        podsByContract.get(contract3)!.has(pod1),
        `Contract 3 invoice must not include POD 1 (${pod1})`
    ).toBe(false);
}

export async function assertInvoiceForPodsOnly(
    ctx: PullingCtx,
    opts: {
        expectedInvoiceCount?: number;
        includedPodIndexes: number[];
        excludedPodIndexes: number[];
        contractIndex?: number;
    }
): Promise<void> {
    const expectedInvoiceCount = opts.expectedInvoiceCount ?? 1;
    const invoiceIds = ctx.Responses.invoice ?? [];
    expect(invoiceIds.length, 'Unexpected invoice count').toBe(expectedInvoiceCount);

    const included = await Promise.all(
        opts.includedPodIndexes.map((i) => getPodIdentifier(ctx, ctx.Responses.pod[i]))
    );
    const excluded = await Promise.all(
        opts.excludedPodIndexes.map((i) => getPodIdentifier(ctx, ctx.Responses.pod[i]))
    );
    const foundPods = await collectPodsOnInvoices(ctx, invoiceIds);

    for (const pod of included) {
        expect(foundPods.has(pod), `Expected POD ${pod}; found [${[...foundPods].join(', ')}]`).toBe(true);
    }
    for (const pod of excluded) {
        expect(foundPods.has(pod), `POD ${pod} should not be invoiced; found [${[...foundPods].join(', ')}]`).toBe(false);
    }

    if (opts.contractIndex != null) {
        const expectedContractId = asId(ctx.Responses.productContract[opts.contractIndex]);
        for (const invoiceId of invoiceIds) {
            const invoice = await getInvoice(ctx, invoiceId);
            expect(invoice.invoiceStatus, `invoice ${invoiceId} status`).toBe('REAL');
            expect(asId(invoice.productContract)).toBe(expectedContractId);
        }
    } else {
        for (const invoiceId of invoiceIds) {
            const invoice = await getInvoice(ctx, invoiceId);
            expect(invoice.invoiceStatus, `invoice ${invoiceId} status`).toBe('REAL');
        }
    }
}

export async function runPullingCheck(
    title: string,
    fn: () => Promise<void>
): Promise<void> {
    await test.step(title, fn);
}

async function getPodVersion(api: PullingApi, podId: unknown, versionId: number): Promise<any> {
    const res = await api.Request.get(`${api.Endpoints.pod}/${asId(podId)}?versionId=${versionId}`);
    await expect(res).CheckResponse();
    return res.json();
}

export async function assertPodVersionsHaveDifferentMeasurementTypes(ctx: PullingCtx): Promise<void> {
    const podId = asId(ctx.Responses.pod[0]);
    const version1 = await getPodVersion(ctx, podId, 1);
    const version2 = await getPodVersion(ctx, podId, 2);

    expect(version1.id, 'Both versions must be the same POD').toBe(version2.id);
    expect(version1.identifier).toBe(version2.identifier);
    expect(Number(version1.versionId)).toBe(1);
    expect(Number(version2.versionId)).toBe(2);
    expect(version1.measurementType, 'Version 1 (contract 1) must stay SETTLEMENT_PERIOD').toBe('SETTLEMENT_PERIOD');
    expect(version2.measurementType, 'Version 2 (contract 2) must be SLP').toBe('SLP');
    expect(version1.settlementPeriod).toBe(true);
    expect(version2.slp).toBe(true);

    const contract1Res = await ctx.Request.get(`${ctx.Endpoints.productContract}/${asId(ctx.Responses.productContract[0])}`);
    await expect(contract1Res).CheckResponse();
    const contract2Res = await ctx.Request.get(`${ctx.Endpoints.productContract}/${asId(ctx.Responses.productContract[1])}`);
    await expect(contract2Res).CheckResponse();
    const contract1Pod = (await contract1Res.json()).contractPodsResponses?.[0];
    const contract2Pod = (await contract2Res.json()).contractPodsResponses?.[0];

    expect(asId(contract1Pod?.podId), 'Both contracts must use the same POD').toBe(asId(contract2Pod?.podId));
    expect(asId(contract1Pod?.podDetailId), 'Contract 2 must use a different POD version than contract 1').not.toBe(
        asId(contract2Pod?.podDetailId)
    );
    expect(Number(contract1Pod?.versionId), 'Contract 1 must use POD version 1').toBe(1);
    expect(Number(contract2Pod?.versionId), 'Contract 2 must use POD version 2').toBe(2);
    expect(contract1Pod?.podMeasurementType).toBe('SETTLEMENT_PERIOD');
    expect(contract2Pod?.podMeasurementType).toBe('SLP');
}
