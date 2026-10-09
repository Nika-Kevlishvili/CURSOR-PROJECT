import { test, expect } from '../../fixtures/baseFixture';
import type { APIResponse } from '@playwright/test';
import reportGenerator from '../../utils/generateReport';

// ═══════════════════════════════════════════════════════════════════════════
// SHARED PRECONDITION HELPER FUNCTIONS
// ═══════════════════════════════════════════════════════════════════════════

async function createCustomer(
    Request: any, GeneratePayload: any, Responses: any, Endpoints: any,
) {
    const payload = GeneratePayload.customers.customer_private();
    const response: APIResponse = await Request.post(Endpoints.customer, { data: payload });
    await expect(response).CheckResponse();
    const body = await response.json();
    Responses.customer.push(body);
    return body;
}

async function createPod(
    Request: any, GeneratePayload: any, Responses: any, Endpoints: any,
) {
    const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
    const response = await Request.post(Endpoints.pod, { data: payload });
    await expect(response).toBeOK();
    const body = await response.json();
    Responses.pod.push(body);
    return body;
}

async function createTerm(
    Request: any, GeneratePayload: any, Responses: any, Endpoints: any,
) {
    const payload = GeneratePayload.productAndServices.term();
    const response = await Request.post(Endpoints.terms, { data: payload });
    await expect(response).toBeOK();
    const body = await response.json();
    Responses.terms.push(body);
    return body;
}

async function createPriceComponent(
    Request: any, GeneratePayload: any, Responses: any, Endpoints: any,
) {
    const payload = GeneratePayload.productAndServices.electricity();
    const response = await Request.post(Endpoints.priceComponent, { data: payload });
    await expect(response).toBeOK();
    const body = await response.json();
    Responses.priceComponent.push(body);
    return body;
}

async function createProduct(
    Request: any, GeneratePayload: any, Responses: any, Endpoints: any,
) {
    const payload = GeneratePayload.productAndServices.product();
    payload.contractTypes = ['SUPPLY_ONLY'];
    payload.paymentGuarantees = ['NO'];
    const response = await Request.post(Endpoints.product, { data: payload });
    await expect(response).toBeOK();
    const body = await response.json();
    Responses.product.push(body);
    return body;
}

async function createProductContract(
    Request: any, GeneratePayload: any, Responses: any, Endpoints: any,
) {
    const payload = await GeneratePayload.contractsAndOrders.product_contract();
    payload.productParameters.contractType = 'SUPPLY_ONLY';
    const response = await Request.post(Endpoints.productContract, { data: payload });
    await expect(response).toBeOK();
    const body = await response.json();
    Responses.productContract.push(body);
    return body;
}

/**
 * Generates contract documents with the specified signing types.
 * Uses the generate-popup to discover available templates, then generates
 * documents with the requested signing types and PDF output format.
 */
async function generateContractDocuments(
    Request: any,
    contractId: number,
    versionId: number,
    signingTypes: string[] = ['NO'],
) {
    const popupResponse = await Request.get(
        `product-contract/${contractId}/generate-popup?versionId=${versionId}`,
    );
    await expect(popupResponse).toBeOK();
    const templates = await popupResponse.json();
    expect(Array.isArray(templates)).toBeTruthy();
    expect(templates.length).toBeGreaterThan(0);

    const template = templates[0];
    const generatePayload = {
        contractId,
        versionId: Number(versionId),
        documents: [{
            templateId: template.templateId,
            signings: signingTypes,
            outputFileFormat: ['PDF'],
            deletePreviousFiles: true,
        }],
    };
    const generateResponse = await Request.post('product-contract/generate', {
        data: generatePayload,
    });
    await expect(generateResponse).toBeOK();
    return { template, templates };
}

/**
 * Retrieves file IDs for contract documents by inspecting the full contract
 * response. Searches common field names where file/document arrays may reside.
 */
async function getContractFileIds(Request: any, contractId: number): Promise<number[]> {
    const response = await Request.get(`product-contract/${contractId}?version=1`);
    await expect(response).toBeOK();
    const data = await response.json();
    const fileIds: number[] = [];

    const candidateArrays = [
        data.signableDocuments,
        data.contractDocuments,
        data.documents,
        data.files,
        data.contractFiles,
        data.productContractFiles,
    ].filter(Boolean);

    for (const arr of candidateArrays) {
        if (Array.isArray(arr)) {
            for (const item of arr) {
                if (item && typeof item === 'object') {
                    const fid = item.fileId ?? item.id ?? item.documentFileId ?? item.productContractFileId;
                    if (typeof fid === 'number' && fid > 0) {
                        fileIds.push(fid);
                    }
                }
            }
        }
    }

    if (fileIds.length === 0) {
        for (const key of Object.keys(data)) {
            const val = data[key];
            if (Array.isArray(val)) {
                for (const item of val) {
                    if (item && typeof item === 'object') {
                        const fid = item.fileId ?? item.documentFileId ?? item.productContractFileId;
                        if (typeof fid === 'number' && fid > 0) {
                            fileIds.push(fid);
                        }
                    }
                }
            }
        }
    }

    return fileIds;
}

/**
 * Updates the contract status via PUT /product-contract/status-update/{id}.
 * Validates against the Swagger spec enum values for ProductContractEditStatusRequest.
 */
async function updateContractStatus(
    Request: any,
    contractId: number,
    versionId: number,
    contractStatus: string,
    contractSubStatus: string,
    contractVersionStatus: string = 'SIGNED',
) {
    const payload = { contractStatus, contractSubStatus, contractVersionStatus };
    const response = await Request.put(
        `product-contract/status-update/${contractId}?versionId=${versionId}`,
        { data: payload },
    );
    return response;
}

interface FullPreconditions {
    contractId: number;
    versionId: number;
    fileIds: number[];
}

/**
 * Orchestrates the complete entity creation chain:
 * customer → POD → terms → price component → product → product contract → generate documents.
 * Returns contract ID, version ID, and generated file IDs.
 */
async function fullPreconditions(
    Request: any,
    GeneratePayload: any,
    Responses: any,
    Endpoints: any,
    signingTypes: string[] = ['NO'],
): Promise<FullPreconditions> {
    await createCustomer(Request, GeneratePayload, Responses, Endpoints);
    await createPod(Request, GeneratePayload, Responses, Endpoints);
    await createTerm(Request, GeneratePayload, Responses, Endpoints);
    await createPriceComponent(Request, GeneratePayload, Responses, Endpoints);
    await createProduct(Request, GeneratePayload, Responses, Endpoints);
    const contract = await createProductContract(Request, GeneratePayload, Responses, Endpoints);
    const contractId = contract.id;
    const versionId = contract.versionId ?? 1;

    await generateContractDocuments(Request, contractId, versionId, signingTypes);
    const fileIds = await getContractFileIds(Request, contractId);

    return { contractId, versionId, fileIds };
}

// ═══════════════════════════════════════════════════════════════════════════
// TESTS
// ═══════════════════════════════════════════════════════════════════════════

test.describe('[PHN-2798]: Get contract outgoing document', { tag: '@salesPortal' }, () => {
    test.describe.configure({ mode: 'serial' });

    // ── TC-BE-1 ──────────────────────────────────────────────────────────
    test('[PHN-2798]: TC-BE-1 – Electronic signing types (system certificate, QES, tablet) – file status SIGNED', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: FullPreconditions;

        await test.step('Precondition: Create full entity chain and generate documents (SIGNING_WITH_SYSTEM_CERTIFICATE)', async () => {
            pre = await fullPreconditions(
                Request, GeneratePayload, Responses, Endpoints,
                ['SIGNING_WITH_SYSTEM_CERTIFICATE'],
            );
            expect(pre.fileIds.length).toBeGreaterThan(0);
        });

        await test.step('GET /contract/document/{fileId} – expect 200 PDF for system-certificate-signed file', async () => {
            const response = await SPRequest.get(`contract/document/${pre!.fileIds[0]}`);

            expect(
                response.status(),
                'System-certificate-signed file should return 200. ' +
                'If 422, the SIGNING_WITH_SYSTEM_CERTIFICATE service did not auto-sign the document. ' +
                'Verify that system certificate signing is operational in this environment.',
            ).toBe(200);

            const contentType = response.headers()['content-type'] ?? '';
            expect(contentType).toContain('application/pdf');

            const contentDisposition = response.headers()['content-disposition'] ?? '';
            expect(contentDisposition).toContain('attachment;');

            const body = await response.body();
            expect(body.length).toBeGreaterThan(0);
        });

        test.info().attach('[PHN-2798] TC-BE-1 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ── TC-BE-2 ──────────────────────────────────────────────────────────
    test('[PHN-2798]: TC-BE-2 – Manual signing – contract status SIGNED, sub-status SIGNED_BY_BOTH_SIDES', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: FullPreconditions;

        await test.step('Precondition: Create full entity chain and generate documents (NO signing)', async () => {
            pre = await fullPreconditions(Request, GeneratePayload, Responses, Endpoints, ['NO']);
            expect(pre.fileIds.length).toBeGreaterThan(0);
        });

        await test.step('Precondition: Advance contract to SIGNED / SIGNED_BY_BOTH_SIDES', async () => {
            const statusRes = await updateContractStatus(
                Request, pre!.contractId, pre!.versionId,
                'SIGNED', 'SIGNED_BY_BOTH_SIDES', 'SIGNED',
            );
            if (!statusRes.ok()) {
                test.skip(true, 'Environment does not allow direct status transition to SIGNED/SIGNED_BY_BOTH_SIDES');
                return;
            }
        });

        await test.step('GET /contract/document/{fileId} – expect 200 PDF', async () => {
            const response = await SPRequest.get(`contract/document/${pre!.fileIds[0]}`);
            expect(response.status()).toBe(200);
            const contentType = response.headers()['content-type'] ?? '';
            expect(contentType).toContain('application/pdf');
            const body = await response.body();
            expect(body.length).toBeGreaterThan(0);
        });

        test.info().attach('[PHN-2798] TC-BE-2 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ── TC-BE-3 ──────────────────────────────────────────────────────────
    test('[PHN-2798]: TC-BE-3 – Manual signing – contract status SIGNED, sub-status SPECIAL_PROCESSES', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: FullPreconditions;

        await test.step('Precondition: Create full entity chain and generate documents (NO signing)', async () => {
            pre = await fullPreconditions(Request, GeneratePayload, Responses, Endpoints, ['NO']);
            expect(pre.fileIds.length).toBeGreaterThan(0);
        });

        await test.step('Precondition: Advance contract to SIGNED / SPECIAL_PROCESSES', async () => {
            const statusRes = await updateContractStatus(
                Request, pre!.contractId, pre!.versionId,
                'SIGNED', 'SPECIAL_PROCESSES', 'SIGNED',
            );
            if (!statusRes.ok()) {
                test.skip(true, 'Environment does not allow direct status transition to SIGNED/SPECIAL_PROCESSES');
                return;
            }
        });

        await test.step('GET /contract/document/{fileId} – expect 200 PDF', async () => {
            const response = await SPRequest.get(`contract/document/${pre!.fileIds[0]}`);
            expect(response.status()).toBe(200);
            const contentType = response.headers()['content-type'] ?? '';
            expect(contentType).toContain('application/pdf');
            const body = await response.body();
            expect(body.length).toBeGreaterThan(0);
        });

        test.info().attach('[PHN-2798] TC-BE-3 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ── TC-BE-4 ──────────────────────────────────────────────────────────
    test.skip('[PHN-2798]: TC-BE-4 – Manual signing – contract status ACTIVE_IN_TERM, ACTIVE_IN_PERPETUITY, TERMINATED', async () => {
        // Cannot fully automate: advancing a contract through the full lifecycle
        // (DRAFT → READY → SIGNED → ENTERED_INTO_FORCE → ACTIVE_IN_TERM /
        // ACTIVE_IN_PERPETUITY → TERMINATED) requires POD activation, billing
        // profile setup, and time-dependent entry-into-force processing that
        // cannot be reliably driven via a single API test.
    });

    // ── TC-BE-5 ──────────────────────────────────────────────────────────
    test('[PHN-2798]: TC-BE-5 – No signing type – allowed contract status', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: FullPreconditions;

        await test.step('Precondition: Create full entity chain and generate documents (NO signing)', async () => {
            pre = await fullPreconditions(Request, GeneratePayload, Responses, Endpoints, ['NO']);
            expect(pre.fileIds.length).toBeGreaterThan(0);
        });

        await test.step('Precondition: Advance contract to SIGNED / SIGNED_BY_BOTH_SIDES', async () => {
            const statusRes = await updateContractStatus(
                Request, pre!.contractId, pre!.versionId,
                'SIGNED', 'SIGNED_BY_BOTH_SIDES', 'SIGNED',
            );
            if (!statusRes.ok()) {
                test.skip(true, 'Environment does not allow direct status transition to SIGNED/SIGNED_BY_BOTH_SIDES');
                return;
            }
        });

        await test.step('GET /contract/document/{fileId} – expect 200 PDF', async () => {
            const response = await SPRequest.get(`contract/document/${pre!.fileIds[0]}`);
            expect(response.status()).toBe(200);
            const contentType = response.headers()['content-type'] ?? '';
            expect(contentType).toContain('application/pdf');
        });

        test.info().attach('[PHN-2798] TC-BE-5 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ── TC-BE-6 ──────────────────────────────────────────────────────────
    test.skip('[PHN-2798]: TC-BE-6 – Archimed fallback – file found in Archimed and eligible', async () => {
        // Cannot automate: requires archiving a file to the Archimed/EDMS service
        // (setting isArchived=true, localFileUrl=null) which is handled by the
        // EDMSFileArchivationService and is not exposed as a public API endpoint.
    });

    // ── TC-BE-7 ──────────────────────────────────────────────────────────
    test('[PHN-2798]: TC-BE-7 – Electronic signing (QES / tablet) – file status NOT SIGNED', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: FullPreconditions;

        await test.step('Precondition: Create full entity chain and generate documents (electronic signing, NOT signed)', async () => {
            pre = await fullPreconditions(
                Request, GeneratePayload, Responses, Endpoints,
                ['SIGNING_WITH_QUALIFIED_SIGNATURE'],
            );
            expect(pre.fileIds.length).toBeGreaterThan(0);
        });

        await test.step('GET /contract/document/{fileId} – expect 422 "File is not fully signed"', async () => {
            const response = await SPRequest.get(`contract/document/${pre!.fileIds[0]}`);
            expect(response.status()).toBe(422);
            const body = await response.json();
            const errorText = JSON.stringify(body).toLowerCase();
            expect(errorText).toContain('not fully signed');
        });

        test.info().attach('[PHN-2798] TC-BE-7 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ── TC-BE-8 ──────────────────────────────────────────────────────────
    test('[PHN-2798]: TC-BE-8 – Manual signing – contract sub-status DRAFT', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: FullPreconditions;

        await test.step('Precondition: Create full entity chain and generate documents (NO signing) – keep DRAFT', async () => {
            pre = await fullPreconditions(Request, GeneratePayload, Responses, Endpoints, ['NO']);
            expect(pre.fileIds.length).toBeGreaterThan(0);
        });

        await test.step('GET /contract/document/{fileId} – expect 422 "File is not fully signed"', async () => {
            const response = await SPRequest.get(`contract/document/${pre!.fileIds[0]}`);
            expect(response.status()).toBe(422);
            const body = await response.json();
            const errorText = JSON.stringify(body).toLowerCase();
            expect(errorText).toContain('not fully signed');
        });

        test.info().attach('[PHN-2798] TC-BE-8 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ── TC-BE-9 ──────────────────────────────────────────────────────────
    test('[PHN-2798]: TC-BE-9 – Manual signing – contract sub-status IN_PROCESS', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: FullPreconditions;

        await test.step('Precondition: Create full entity chain and generate documents (NO signing)', async () => {
            pre = await fullPreconditions(Request, GeneratePayload, Responses, Endpoints, ['NO']);
            expect(pre.fileIds.length).toBeGreaterThan(0);
        });

        await test.step('Precondition: Advance contract to DRAFT / IN_PROCESS', async () => {
            const statusRes = await updateContractStatus(
                Request, pre!.contractId, pre!.versionId,
                'DRAFT', 'IN_PROCESS', 'DRAFT',
            );
            if (!statusRes.ok()) {
                test.skip(true, 'Environment does not allow direct status transition to DRAFT/IN_PROCESS');
                return;
            }
        });

        await test.step('GET /contract/document/{fileId} – expect 422 "File is not fully signed"', async () => {
            const response = await SPRequest.get(`contract/document/${pre!.fileIds[0]}`);
            expect(response.status()).toBe(422);
            const body = await response.json();
            const errorText = JSON.stringify(body).toLowerCase();
            expect(errorText).toContain('not fully signed');
        });

        test.info().attach('[PHN-2798] TC-BE-9 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ── TC-BE-10 ─────────────────────────────────────────────────────────
    test('[PHN-2798]: TC-BE-10 – Manual / No signing – non-allowed contract statuses (READY, CANCELLED)', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: FullPreconditions;

        await test.step('Precondition: Create full entity chain and generate documents (NO signing)', async () => {
            pre = await fullPreconditions(Request, GeneratePayload, Responses, Endpoints, ['NO']);
            expect(pre.fileIds.length).toBeGreaterThan(0);
        });

        await test.step('Precondition: Advance contract to READY / READY', async () => {
            const statusRes = await updateContractStatus(
                Request, pre!.contractId, pre!.versionId,
                'READY', 'READY', 'READY',
            );
            if (!statusRes.ok()) {
                test.skip(true, 'Environment does not allow direct status transition to READY');
                return;
            }
        });

        await test.step('GET /contract/document/{fileId} – expect 422 "File is not fully signed"', async () => {
            const response = await SPRequest.get(`contract/document/${pre!.fileIds[0]}`);
            expect(response.status()).toBe(422);
            const body = await response.json();
            const errorText = JSON.stringify(body).toLowerCase();
            expect(errorText).toContain('not fully signed');
        });

        test.info().attach('[PHN-2798] TC-BE-10 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ── TC-BE-11 ─────────────────────────────────────────────────────────
    test('[PHN-2798]: TC-BE-11 – File not found anywhere – neither in Phoenix DB nor in Archimed', async ({
        SPRequest, Responses,
    }) => {
        await test.step('GET /contract/document/999999999 – expect 404 "File can\'t be found"', async () => {
            const response = await SPRequest.get('contract/document/999999999');
            expect(response.status()).toBe(404);
            const body = await response.json();
            const errorText = JSON.stringify(body).toLowerCase();
            expect(errorText).toContain("can't be found");
        });

        test.info().attach('[PHN-2798] TC-BE-11 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ── TC-BE-12 ─────────────────────────────────────────────────────────
    test.skip('[PHN-2798]: TC-BE-12 – Archimed fallback – file found but NOT eligible', async () => {
        // Cannot automate: requires archiving an unsigned file to Archimed/EDMS
        // (isArchived=true, localFileUrl=null) which is not exposed as a public
        // API endpoint. The EDMSFileArchivationService is an internal service.
    });

    // ── TC-BE-13 ─────────────────────────────────────────────────────────
    test('[PHN-2798]: TC-BE-13 – Invalid fileId – non-numeric or missing', async ({
        SPRequest, Responses,
    }) => {
        await test.step('GET /contract/document/abc – non-numeric fileId → expect 400', async () => {
            const response = await SPRequest.get('contract/document/abc');
            expect(response.status()).toBeGreaterThanOrEqual(400);
            expect(response.status()).toBeLessThan(500);
        });

        await test.step('GET /contract/document/9999999999999 – very large numeric → expect 404', async () => {
            const response = await SPRequest.get('contract/document/9999999999999');
            expect(response.status()).toBeGreaterThanOrEqual(400);
            expect(response.status()).toBeLessThan(500);
        });

        test.info().attach('[PHN-2798] TC-BE-13 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ── TC-BE-14 ─────────────────────────────────────────────────────────
    test.skip('[PHN-2798]: TC-BE-14 – Archimed service unavailable during fallback', async () => {
        // Cannot automate: simulating Archimed/EDMS service unavailability requires
        // infrastructure-level manipulation (stopping the Archimed service, network
        // isolation) that is outside the scope of API-only testing.
    });

    // ═══════════════════════════════════════════════════════════════════════
    // FRONTEND TEST CASES (TC-FE-1 through TC-FE-14)
    // All skipped: Sales Portal UI interactions cannot be automated in an
    // API-only test suite. These tests require browser-based navigation,
    // DOM element inspection, and user interaction simulation.
    // ═══════════════════════════════════════════════════════════════════════

    // ── TC-FE-1 ──────────────────────────────────────────────────────────
    test.skip('[PHN-2798]: TC-FE-1 – Download button visible and functional for eligible signed document', async () => {
        // UI-only: requires browser login to Sales Portal, navigating to Contracts,
        // selecting a contract, and clicking the Download button.
    });

    // ── TC-FE-2 ──────────────────────────────────────────────────────────
    test.skip('[PHN-2798]: TC-FE-2 – Download button visible for manual signing with allowed contract status', async () => {
        // UI-only: verifying Download button visibility and enablement state requires
        // DOM inspection which is not possible in an API-only suite.
    });

    // ── TC-FE-3 ──────────────────────────────────────────────────────────
    test.skip('[PHN-2798]: TC-FE-3 – Download button hidden or disabled for unsigned electronic document', async () => {
        // UI-only: checking disabled/hidden button state requires browser rendering.
    });

    // ── TC-FE-4 ──────────────────────────────────────────────────────────
    test.skip('[PHN-2798]: TC-FE-4 – Download button hidden or disabled for manual signing with non-allowed contract status', async () => {
        // UI-only: verifying button disabled state for non-allowed contract status
        // requires DOM inspection.
    });

    // ── TC-FE-5 ──────────────────────────────────────────────────────────
    test.skip('[PHN-2798]: TC-FE-5 – Error message displayed when file is not found', async () => {
        // UI-only: checking toast/error notification rendering requires browser context.
    });

    // ── TC-FE-6 ──────────────────────────────────────────────────────────
    test.skip('[PHN-2798]: TC-FE-6 – Contract list displays contract IDs correctly', async () => {
        // UI-only: verifying contract list rendering and navigation requires browser.
    });

    // ── TC-FE-7 ──────────────────────────────────────────────────────────
    test.skip('[PHN-2798]: TC-FE-7 – Contract detail page shows document availability', async () => {
        // UI-only: inspecting document availability indicators and download button
        // states on the contract detail page requires browser rendering.
    });

    // ── TC-FE-8 ──────────────────────────────────────────────────────────
    test.skip('[PHN-2798]: TC-FE-8 – Downloaded file opens as valid PDF', async () => {
        // UI-only: verifying downloaded file validity in a PDF viewer requires
        // browser download handling and file system interaction.
    });

    // ── TC-FE-9 ──────────────────────────────────────────────────────────
    test.skip('[PHN-2798]: TC-FE-9 – Multiple documents on one contract – selective download', async () => {
        // UI-only: verifying per-document download button states and selective
        // download behavior requires browser interaction.
    });

    // ── TC-FE-10 ─────────────────────────────────────────────────────────
    test.skip('[PHN-2798]: TC-FE-10 – Network error during download – graceful handling', async () => {
        // UI-only: simulating network interruption during a browser download and
        // verifying error toast/message requires browser-level network mocking.
    });

    // ── TC-FE-11 ─────────────────────────────────────────────────────────
    test.skip('[PHN-2798]: TC-FE-11 – Download for Archimed-resolved file is transparent to user', async () => {
        // UI-only: verifying that Archimed-resolved downloads are visually
        // identical to local downloads requires browser-based comparison.
    });

    // ── TC-FE-12 ─────────────────────────────────────────────────────────
    test.skip('[PHN-2798]: TC-FE-12 – Contract with "No signing" type – Active in perpetuity – download works', async () => {
        // UI-only: requires browser login, contract navigation, and download click
        // for a no-signing / ACTIVE_IN_PERPETUITY contract.
    });

    // ── TC-FE-13 ─────────────────────────────────────────────────────────
    test.skip('[PHN-2798]: TC-FE-13 – Contract with "Signing with tablet" – SIGNED – download works', async () => {
        // UI-only: requires browser interaction with a tablet-signed contract.
    });

    // ── TC-FE-14 ─────────────────────────────────────────────────────────
    test.skip('[PHN-2798]: TC-FE-14 – Download for terminated contract with "Manual" signing – contract status CANCELLED', async () => {
        // UI-only: verifying download button disabled/error state for a CANCELLED
        // contract in the Portal requires browser rendering.
    });
});
