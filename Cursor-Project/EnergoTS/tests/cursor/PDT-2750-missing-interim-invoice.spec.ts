import { test, expect } from '../../fixtures/baseFixture';
import reportGenerator from '../../utils/generateReport';

/**
 * PDT-2750 — API-only regression (EnergoTS).
 *
 * Two **product contracts** for the same customer: predecessor **A** (base product, volume PC only)
 * and successor **B** (re-sign catalog product with volume + interim PCs). Re-sign is modeled with
 * backend product payload fields `isResigning` + `resignProductTargets` (no sales-portal endpoints),
 * **terminate predecessor A**, POD activation on **B**, then billing:
 * FOR_VOLUMES → REAL volume invoice on **A only**, then successor chain: contract **B**,
 * terminate **A**, POD on **B**, FOR_VOLUMES on **B**, then INTERIM_AND_ADVANCE_PAYMENT on **B**
 * (PERCENT_FROM_PREVIOUS / `missingInvoice`, `priceComponentId = null` on Dev).
 *
 * Preconditions through IAP match `tests/billing/Interim/interimCases.spec.ts` [REG-1154] (volume + interim PCs,
 * IAP PERCENT_FROM_PREVIOUS). Uses `billingRunEntryId()` because `Responses.billingRun[]` may hold numeric ids.
 *
 * Reference specs:
 * - `tests/billing/Interim/interimCases.spec.ts`
 * - `tests/pointOfDelivery/statusChangeContractLifeSycle.spec.ts` (terminate / status-update semantics)
 * - `tests/salesPortal/getProductList.spec.ts` (re-signing payload fields on product)
 *
 * Product **B**: (1) second `POST /terms` + `product(1)` — reusing `terms[0]` caused `400` *term not available*.
 * (2) **New** volume + interim price components and a **second** IAP — PCs/IAP from Product A are not reusable
 * on another product (`not available for adding`). Contract **B** is built while `Responses.interim` is scoped
 * to the successor IAP only so `product_contract` does not attach both IAPs to B.
 */

/**
 * POST `/billing-run` often returns a **bare number** id (`asBillingRunId` in `pdt-2599-service-contract.fixtures.ts`).
 * `waitForInvoiceGeneration` supports that. Assertions must not use `.id` on a primitive — use this helper.
 */
function billingRunEntryId(entry: unknown): number {
    if (typeof entry === 'number' && Number.isFinite(entry) && entry > 0) {
        return entry;
    }
    if (entry !== null && typeof entry === 'object') {
        const e = entry as Record<string, unknown>;
        const cp = e.commonParameters as Record<string, unknown> | undefined;
        const id = Number(e.id ?? cp?.id);
        if (Number.isFinite(id) && id > 0) {
            return id;
        }
    }
    throw new Error(`Cannot resolve billing run id from: ${JSON.stringify(entry).slice(0, 240)}`);
}

function resolveSignedProductContractVersionId(contractBody: Record<string, unknown>): number {
    const versions = contractBody?.versions;
    if (!Array.isArray(versions) || versions.length === 0) {
        return 1;
    }
    const signed = versions.find((v: unknown) => {
        if (!v || typeof v !== 'object') return false;
        const o = v as Record<string, unknown>;
        const bp = o.basicParameters as Record<string, unknown> | undefined;
        return (
            o.contractVersionStatus === 'SIGNED' ||
            bp?.contractVersionStatus === 'SIGNED' ||
            o.status === 'SIGNED'
        );
    }) as Record<string, unknown> | undefined;

    const vid = signed?.versionId ?? signed?.id;
    if (typeof vid === 'number' && Number.isFinite(vid) && vid > 0) {
        return vid;
    }
    const first = versions[0] as Record<string, unknown>;
    const fid = first?.versionId ?? first?.id;
    if (typeof fid === 'number' && Number.isFinite(fid) && fid > 0) {
        return fid;
    }
    return 1;
}

function entityId(entry: unknown): number {
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

async function waitUntilBillingRunStatus(
    Request: any,
    billingRunId: number,
    targetStatus: string,
    timeoutMs = 30 * 60 * 1000,
    intervalMs = 15000,
): Promise<void> {
    const billingRunRoot = 'billing-run';
    const deadline = Date.now() + timeoutMs;
    let lastStatus: string | undefined;
    let attempt = 0;

    while (Date.now() < deadline) {
        attempt += 1;
        const runRes = await Request.get(`${billingRunRoot}/${billingRunId}`);
        await (expect(runRes) as any).CheckResponse();
        const body = (await runRes.json()) as Record<string, unknown>;
        const cp = body.commonParameters as { status?: string } | undefined;
        lastStatus = cp?.status;
        if (attempt === 1 || attempt % 5 === 0) {
            console.log(
                `[waitUntilBillingRunStatus] billingRunId=${billingRunId} attempt=${attempt} status=${lastStatus ?? '(missing)'}`,
            );
        }
        if (lastStatus === targetStatus) {
            console.log(
                `[waitUntilBillingRunStatus] billingRunId=${billingRunId} reached target status=${targetStatus} at attempt=${attempt}`,
            );
            return;
        }
        await new Promise((r) => setTimeout(r, intervalMs));
    }

    throw new Error(
        `[waitUntilBillingRunStatus] Billing run ${billingRunId}: timed out after ${timeoutMs}ms waiting for status "${targetStatus}"; last commonParameters.status was "${lastStatus ?? '(missing)'}"`,
    );
}

test.describe('[PDT-2750]: Missing interim invoice – billing API chain', { tag: ['@billing', '@pdt-2750'] }, () => {
    test.describe.configure({ mode: 'serial' });

    test(
        '[PDT-2750]: TC-BE-13 (API chain) — two contracts, backend re-sign flags, terminate A, volume then interim on B',
        async ({ Request, GeneratePayload, Responses, Endpoints }) => {
            // Three billing pipelines (each can poll several minutes).
            test.setTimeout(30 * 60 * 1000);

            await test.step('Precondition: customer', async () => {
                const customer = await Request.post(Endpoints.customer, {
                    data: GeneratePayload.customers.customer_legal(),
                });
                await expect(customer).CheckResponse();
                Responses.customer.push(await customer.json());
            });

            await test.step('Precondition: price component (volume)', async () => {
                const payload = GeneratePayload.productAndServices.priceSettlement();
                payload.formulaRequest.expression = 50;
                const price = await Request.post(Endpoints.priceComponent, { data: payload });
                await expect(price).CheckResponse();
                Responses.priceComponent.push(await price.json());
            });

            await test.step('Precondition: price component (interim)', async () => {
                const payload = GeneratePayload.productAndServices.priceSettlement();
                payload.formulaRequest.expression = 10;
                const price = await Request.post(Endpoints.priceComponent, { data: payload });
                await expect(price).CheckResponse();
                Responses.priceComponent.push(await price.json());
            });

            await test.step('Precondition: term', async () => {
                const term = await Request.post(Endpoints.terms, {
                    data: GeneratePayload.productAndServices.term(),
                });
                await expect(term).CheckResponse();
                Responses.terms.push(await term.json());
            });

            await test.step('Precondition: POD', async () => {
                const podSettlement = await Request.post(Endpoints.pod, {
                    data: GeneratePayload.pointsOfDelivery.pod_settlement(),
                });
                await expect(podSettlement).CheckResponse();
                Responses.pod.push(await podSettlement.json());
            });

            await test.step('Precondition: IAP interim — PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT', async () => {
                const payload = GeneratePayload.productAndServices.interim();
                payload.valueType = 'PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT';
                payload.value = 50;
                payload.missingInvoice = true;
                // Dev API (ILLEGAL_ARGUMENTS_PROVIDED): priceComponentId must not be sent for this valueType.
                payload.priceComponentId = null;

                const interim = await Request.post(Endpoints.interim, { data: payload });
                await expect(interim).CheckResponse();
                Responses.interim.push(await interim.json());
            });

            await test.step('Product A (base): volume price component only', async () => {
                const payload = GeneratePayload.productAndServices.product();
                payload.priceComponentIds = [Responses.priceComponent[0]];
                const product = await Request.post(Endpoints.product, { data: payload });
                await expect(product).CheckResponse();
                Responses.product.push(await product.json());
            });

            await test.step('Contract A: POST productContract (customer 0, product 0)', async () => {
                const contract = await Request.post(Endpoints.productContract, {
                    data: await GeneratePayload.contractsAndOrders.product_contract(0, 0),
                });
                await expect(contract).CheckResponse();
                Responses.productContract.push(await contract.json());
            });

            await test.step('Activate POD on contract A', async () => {
                const podActivation = await Request.post('/contract-pods/manual', {
                    data: await GeneratePayload.pointsOfDelivery.pod_activation(0, undefined, undefined, 0),
                });
                await expect(podActivation).CheckResponse();
            });

            await test.step('Precondition: billing-by-profile energy data', async () => {
                const payload = await GeneratePayload.energyData.profile1Month();
                payload.timeZone = 'CET';

                const profiles = await Request.post('billing-by-profile', { data: payload });
                await expect(profiles).CheckResponse();
                const profileData = await profiles.json();
                Responses.dataByProfiles.push({
                    id: profileData,
                    periodFrom: payload.periodFrom,
                    periodTo: payload.periodTo,
                    periodType: payload.periodType,
                });
            });

            await test.step('Action: FOR_VOLUMES billing run (contract A)', async () => {
                const billingPayload = await GeneratePayload.billing.billingRun('CONTRACT', ['FOR_VOLUMES']);
                const billingRun = await Request.post(Endpoints.billingRun, { data: billingPayload });
                await expect(billingRun).CheckResponse();
                Responses.billingRun.push(await billingRun.json());
            });

            await test.step(
                'Action: run volume billing to completion — volume invoice REAL (billingRun index 0, predecessor A)',
                async () => {
                    await GeneratePayload.billing.waitForInvoiceGeneration(true, true, 1, 0);
                },
            );

            // Second term: first term is not valid for a second product with extra PCs + IAP (400 term not available).
            await test.step('Precondition: second term (for successor product B)', async () => {
                const termPayload = GeneratePayload.productAndServices.term();
                termPayload.supplyActivations = ['FIRST_DAY_OF_MONTH'];
                termPayload.waitForOldContractTermToExpires = ['NO'];
                termPayload.contractEntryIntoForces = ['SIGNING'];
                termPayload.startsOfContractInitialTerms = ['SIGNING'];
                const term = await Request.post(Endpoints.terms, { data: termPayload });
                await expect(term).CheckResponse();
                Responses.terms.push(await term.json());
            });

            // Price components + IAP from A cannot be linked to Product B (400 not available for adding).
            await test.step('Precondition: duplicate volume + interim PCs and second IAP (for Product B only)', async () => {
                let payload = GeneratePayload.productAndServices.priceSettlement();
                payload.formulaRequest.expression = 50;
                let price = await Request.post(Endpoints.priceComponent, { data: payload });
                await expect(price).CheckResponse();
                Responses.priceComponent.push(await price.json());

                payload = GeneratePayload.productAndServices.priceSettlement();
                payload.formulaRequest.expression = 10;
                price = await Request.post(Endpoints.priceComponent, { data: payload });
                await expect(price).CheckResponse();
                Responses.priceComponent.push(await price.json());

                const iapPayload = GeneratePayload.productAndServices.interim();
                iapPayload.valueType = 'PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT';
                iapPayload.value = 50;
                iapPayload.missingInvoice = true;
                iapPayload.priceComponentId = null;

                const interim2 = await Request.post(Endpoints.interim, { data: iapPayload });
                await expect(interim2).CheckResponse();
                Responses.interim.push(await interim2.json());
            });

            await test.step('Product B (re-sign / successor catalog): volume + interim price components', async () => {
                const volB = entityId(Responses.priceComponent[2]);
                const interimPcB = entityId(Responses.priceComponent[3]);
                const iapB = entityId(Responses.interim[1]);
                const baseProductId = entityId(Responses.product[0]);

                const payload = GeneratePayload.productAndServices.product(1);
                payload.priceComponentIds = [volB, interimPcB];
                payload.interimAdvancePayments = [iapB];
                (payload as any).isResigning = true;
                (payload as any).resignProductTargets = [baseProductId];

                const product = await Request.post(Endpoints.product, { data: payload });
                await expect(product).CheckResponse();
                Responses.product.push(await product.json());
            });

            await test.step('Check Product B exists before contract B draft->signed conversion', async () => {
                const successorId = entityId(Responses.product[1]);
                const successorGet = await Request.get(`${Endpoints.product}/${successorId}?version=1`);
                await expect(successorGet).CheckResponse();
            });

            await test.step('Contract B: POST in DRAFT (status) then edit -> SIGNED_BY_BOTH_SIDES', async () => {
                // `product_contract` attaches every entry in `Responses.interim`; scope to successor IAP only for B.
                const allInterims = [...Responses.interim];
                Responses.interim.length = 0;
                Responses.interim.push(allInterims[1]);

                try {
                    const draftPayload = await GeneratePayload.contractsAndOrders.product_contract(0, 1);
                    const signingDate = new Date().toISOString().slice(0, 10);
                    // Backend requires first version to be VALID/SIGNED, but contract status may start as DRAFT.
                    draftPayload.basicParameters.status = 'DRAFT';
                    draftPayload.basicParameters.subStatus = 'DRAFT';
                    draftPayload.basicParameters.versionStatus = 'SIGNED';
                    draftPayload.basicParameters.signingDate = null;
                    draftPayload.productParameters.entryIntoForce = 'SIGNING';
                    draftPayload.productParameters.startOfContractInitialTerm = 'SIGNING';
                    draftPayload.productParameters.supplyActivation = 'FIRST_DAY_OF_MONTH';
                    (draftPayload.productParameters as any).productContractWaitForOldContractTermToExpires = 'NO';
                    const contract = await Request.post(Endpoints.productContract, { data: draftPayload });
                    await expect(contract).CheckResponse();
                    const contractBId = entityId(await contract.json());
                    Responses.productContract.push(contractBId);

                    const getB = await Request.get(`${Endpoints.productContract}/${contractBId}?version=1`);
                    await expect(getB).CheckResponse();
                    const contractB = (await getB.json()) as Record<string, unknown>;
                    const contractPodsResponses = (contractB.contractPodsResponses as Record<string, unknown>[] | undefined) ?? [];
                    const podRequests = Array.from(
                        contractPodsResponses.reduce((acc, pod) => {
                            const bg = Number(pod.billingGroupId);
                            if (!acc.has(bg)) acc.set(bg, []);
                            acc.get(bg)!.push({
                                pointOfDeliveryDetailId: Number(pod.podDetailId),
                                dealNumber: (pod.dealNumber as string | null) ?? null,
                            });
                            return acc;
                        }, new Map<number, { pointOfDeliveryDetailId: number; dealNumber: string | null }[]>()),
                    ).map(([billingGroupId, productContractPointOfDeliveries]) => ({
                        billingGroupId,
                        productContractPointOfDeliveries,
                    }));

                    const signedPayload = (await GeneratePayload.contractsAndOrders.product_contract(0, 1)) as any;
                    signedPayload.basicParameters.status = 'SIGNED';
                    signedPayload.basicParameters.subStatus = 'SIGNED_BY_BOTH_SIDES';
                    signedPayload.basicParameters.versionStatus = 'SIGNED';
                    signedPayload.basicParameters.signingDate = signingDate;
                    signedPayload.productParameters.entryIntoForce = 'SIGNING';
                    signedPayload.productParameters.startOfContractInitialTerm = 'SIGNING';
                    signedPayload.productParameters.supplyActivation = 'FIRST_DAY_OF_MONTH';
                    (signedPayload.productParameters as any).productContractWaitForOldContractTermToExpires = 'NO';
                    (signedPayload.additionalParameters as any).employeeId = 154;
                    (signedPayload.additionalParameters as any).riskAssessment = 'PERMIT';
                    signedPayload.savingAsNewVersion = false;
                    signedPayload.startDate =
                        ((contractB.versions as Record<string, unknown>[] | undefined)?.[0]?.startDate as string) ?? signingDate;
                    signedPayload.podRequests = podRequests;
                    signedPayload.productContractPointOfDeliveries = contractPodsResponses.map((pod) => ({
                        pointOfDeliveryDetailId: Number(pod.podDetailId),
                        dealNumber: (pod.dealNumber as string | null) ?? null,
                    }));

                    const putB = await Request.put(
                        `${Endpoints.productContract}/${contractBId}?versionId=1&changeFutureVersionsPods=false`,
                        { data: signedPayload },
                    );
                    await expect(putB).CheckResponse();
                } finally {
                    Responses.interim.length = 0;
                    for (const row of allInterims) {
                        Responses.interim.push(row);
                    }
                }
            });

            await test.step('Validate re-signing relation between A and B contracts', async () => {
                const contractAId = entityId(Responses.productContract[0]);
                const contractBId = entityId(Responses.productContract[1]);

                const getA = await Request.get(`${Endpoints.productContract}/${contractAId}`);
                await expect(getA).CheckResponse();
                const bodyA = (await getA.json()) as Record<string, unknown>;

                const getB = await Request.get(`${Endpoints.productContract}/${contractBId}`);
                await expect(getB).CheckResponse();
                const bodyB = (await getB.json()) as Record<string, unknown>;

                const basicA = (bodyA.basicParameters ?? {}) as Record<string, unknown>;
                const basicB = (bodyB.basicParameters ?? {}) as Record<string, unknown>;
                const resignedToA = basicA.resignedTo;
                const resignedFromB = basicB.resignedFrom;

                const hasResignLink =
                    (Array.isArray(resignedToA) && resignedToA.length > 0) ||
                    (Array.isArray(resignedFromB) && resignedFromB.length > 0);

                expect(hasResignLink).toBeTruthy();
            });

            await test.step('Ensure contract B is in SIGNED/SIGNED_BY_BOTH_SIDES before POD activation', async () => {
                const contractBId = entityId(Responses.productContract[1]);
                const getB = await Request.get(`${Endpoints.productContract}/${contractBId}`);
                await expect(getB).CheckResponse();
                const contractB = (await getB.json()) as Record<string, unknown>;
                const versionId = resolveSignedProductContractVersionId(contractB);
                const payload = {
                    contractStatus: 'SIGNED',
                    contractSubStatus: 'SIGNED_BY_BOTH_SIDES',
                    contractVersionStatus: 'SIGNED',
                };
                const setSigned = await Request.put(
                    `${Endpoints.productContract}/status-update/${contractBId}?versionId=${versionId}`,
                    { data: payload },
                );
                await expect(setSigned).CheckResponse();
            });

            await test.step('Activate POD on contract B', async () => {
                const contractBId = entityId(Responses.productContract[1]);
                const getB = await Request.get(`${Endpoints.productContract}/${contractBId}`);
                await expect(getB).CheckResponse();
                const contractB = (await getB.json()) as Record<string, unknown>;
                const versions = (contractB.versions as Record<string, unknown>[] | undefined) ?? [];
                const signedVersion =
                    versions.find((v) => {
                        const bp = v.basicParameters as Record<string, unknown> | undefined;
                        return (
                            v.contractVersionStatus === 'SIGNED' ||
                            bp?.contractVersionStatus === 'SIGNED' ||
                            v.status === 'SIGNED'
                        );
                    }) ?? versions[0];
                const contractPods = (contractB.contractPodsResponses as Record<string, unknown>[] | undefined) ?? [];
                const targetPodId = Responses.pod[0].id;
                const podRow = contractPods.find((p) => Number(p.podId) === Number(targetPodId)) ?? contractPods[0];
                const activationDate = new Date().toISOString().slice(0, 10);
                const activationPayload = {
                    identifier: String(podRow.identifier),
                    podDetailId: Number(podRow.podDetailId),
                    contractDetailId: Number((signedVersion as Record<string, unknown>).id),
                    activationDate,
                    deactivationDate: null,
                    deactivationPurposeId: 2,
                };
                const podActivation = await Request.post('/contract-pods/manual', { data: activationPayload });
                await expect(podActivation).CheckResponse();
            });

            await test.step('Action: INTERIM_AND_ADVANCE_PAYMENT billing run (contract B)', async () => {
                const billingPayload = await GeneratePayload.billing.billingRun(
                    'CONTRACT',
                    ['INTERIM_AND_ADVANCE_PAYMENT'],
                    0,
                    1,
                );
                const billingRun = await Request.post(Endpoints.billingRun, { data: billingPayload });
                await expect(billingRun).CheckResponse();
                Responses.billingRun.push(await billingRun.json());
                const interimBillingRunId = billingRunEntryId(Responses.billingRun[1]);
                console.log(`[PDT-2750] created INTERIM billing run for B: billingRunId=${interimBillingRunId}`);
                // INITIAL -> DRAFT requires explicit start-billing.
                const startInterim = await Request.patch(`billing-run/start-billing?billingRunId=${interimBillingRunId}`);
                await (expect(startInterim) as any).CheckResponse();
                await waitUntilBillingRunStatus(Request, interimBillingRunId, 'DRAFT', 30 * 60 * 1000, 15000);
            });

            await test.step(
                'Action: run interim billing on B to completion after DRAFT reached (billingRun index 1)',
                async () => {
                    await GeneratePayload.billing.waitForInvoiceGeneration(true, true, 1, 1);
                },
            );

            await test.step('Assert: two contracts, two billing runs, distinct run ids, invoices', async () => {
                expect(Responses.productContract.length).toBe(2);
                expect(Responses.billingRun.length).toBeGreaterThanOrEqual(2);
                const id0 = billingRunEntryId(Responses.billingRun[0]);
                const id1 = billingRunEntryId(Responses.billingRun[1]);
                expect(id0).toBeTruthy();
                expect(id1).toBeTruthy();
                expect(id0).not.toEqual(id1);
                expect(Responses.invoice?.length).toBeGreaterThanOrEqual(2);
            });

            test.info().attach('[PDT-2750] created entities', {
                body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                contentType: 'application/json',
            });
        },
    );
});
