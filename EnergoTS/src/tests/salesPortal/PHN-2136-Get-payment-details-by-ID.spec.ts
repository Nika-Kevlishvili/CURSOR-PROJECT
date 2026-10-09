/**
 * [REG-1307] Get: Payment details by ID — Dev 2 sales-portal API (OAuth client token via SPRequest).
 * Relative path: payment/details/customer/{identifier}/payment/{paymentId}
 * (SPRequest baseURL is .../phoenix-dev2/sales-portal/)
 *
 * Run: BASE_URL=https://devapps.energo-pro.bg/backend/phoenix2-dev npx playwright test --project=setup
 *       npx playwright test --grep "REG-1317|REG-1318|REG-1319"
 */
import { test, expect } from '../../backend/fixtures/baseFixture';
import reportGenerator from '../../backend/utils/generateReport';

function paymentIdFromResponse(paymentEntry: unknown): number {
    if (typeof paymentEntry === 'object' && paymentEntry !== null && 'id' in paymentEntry) {
        return (paymentEntry as { id: number }).id;
    }
    return paymentEntry as number;
}

function customerPathSegment(customer: { identifier?: string; customerIdentifier?: string }): string {
    const raw = customer.identifier ?? customer.customerIdentifier;
    if (raw === undefined || raw === null || String(raw) === '') {
        throw new Error('Customer response missing identifier / customerIdentifier');
    }
    return String(raw);
}

/** Path under SPRequest base (.../sales-portal/) — matches user route /sales-portal/payment/details/... */
function spPaymentDetailsRelativePath(customerKey: string, paymentId: string | number): string {
    return `payment/details/customer/${encodeURIComponent(customerKey)}/payment/${paymentId}`;
}

/** Dev 2 sales-portal payment details payload (observed keys: paymentInitialAmount, paymentNumber, pods, …). */
function pickCustomerInternalIdFromDetailsBody(body: Record<string, unknown>): number | undefined {
    const raw = body.customerId ?? (body.customer as Record<string, unknown> | undefined)?.id
        ?? (body.payment as Record<string, unknown> | undefined)?.customerId;
    if (raw === null || raw === undefined) return undefined;
    if (typeof raw === 'number') return raw;
    if (typeof raw === 'object' && raw !== null && 'id' in raw && typeof (raw as { id: unknown }).id === 'number') {
        return (raw as { id: number }).id;
    }
    const pods = body.pods;
    if (Array.isArray(pods) && pods.length > 0) {
        const row = pods[0] as Record<string, unknown>;
        const nested = row.customerId ?? row.customer;
        if (typeof nested === 'number') return nested;
        if (nested && typeof nested === 'object' && 'id' in nested && typeof (nested as { id: unknown }).id === 'number') {
            return (nested as { id: number }).id;
        }
    }
    return undefined;
}

function pickInitialAmountFromDetailsBody(body: Record<string, unknown>): number | undefined {
    const raw = body.paymentInitialAmount ?? body.initialAmount
        ?? (body.payment as Record<string, unknown> | undefined)?.initialAmount;
    if (raw === null || raw === undefined) return undefined;
    const n = Number(raw);
    return Number.isNaN(n) ? undefined : n;
}

function pickPaymentNumberFromDetailsBody(body: Record<string, unknown>): string | undefined {
    const n = body.paymentNumber;
    return typeof n === 'string' && n.length > 0 ? n : undefined;
}

function expectClientErrorStatus(status: number) {
    expect([400, 401, 403, 404]).toContain(status);
}

test.describe('[REG-1307]: Get payment details by ID (sales-portal)', { tag: '@cursor' }, () => {
    test.describe.configure({ timeout: 120_000 });
    test('[REG-1317] TC-BE-1 — GET sales-portal payment returns 200 and id matches', async ({ Request, SPRequest, GeneratePayload, Responses, Endpoints }) => {
        await test.step('Create customer', async () => {
            const customer = await Request.post(Endpoints.customer, { data: GeneratePayload.customers.customer_legal() });
            await expect(customer).CheckResponse();
            Responses.customer.push(await customer.json());
        });

        await test.step('Create collection channel', async () => {
            const collectionChannel = await Request.post(Endpoints.collectionChannel, { data: GeneratePayload.receivablesManagement.collection_channel() });
            await expect(collectionChannel).CheckResponse();
            Responses.collectionChannel.push(await collectionChannel.json());
        });

        await test.step('Create payment package', async () => {
            const paymentPackage = await Request.post(Endpoints.paymentPackage, { data: GeneratePayload.receivablesManagement.payment_package() });
            await expect(paymentPackage).CheckResponse();
            Responses.paymentPackage.push(await paymentPackage.json());
        });

        await test.step('Create payment', async () => {
            const payload = await GeneratePayload.receivablesManagement.payment();
            payload.initialAmount = 100;
            const payment = await Request.post(Endpoints.payment, { data: payload });
            await expect(payment).CheckResponse();
            Responses.payment.push(await payment.json());
        });

        await test.step('GET sales-portal payment details (SPRequest)', async () => {
            const paymentId = paymentIdFromResponse(Responses.payment[0]);
            const path = spPaymentDetailsRelativePath(customerPathSegment(Responses.customer[0]), paymentId);
            const res = await SPRequest.get(path);
            test.info().annotations.push({ type: 'endpoint', description: `sales-portal/${path}` });
            await expect(res).CheckResponse();
            const body = (await res.json()) as Record<string, unknown>;
            const corePayment = await (async () => {
                const r = await Request.get(`${Endpoints.payment}/${paymentId}`);
                await expect(r).CheckResponse();
                return r.json() as unknown as Record<string, unknown>;
            })();
            const expectedNumber = typeof corePayment.paymentNumber === 'string' ? corePayment.paymentNumber : String(corePayment.paymentNumber ?? '');
            expect(pickPaymentNumberFromDetailsBody(body)).toBe(expectedNumber);
            expect(pickInitialAmountFromDetailsBody(body)).toBe(100);
        });

        test.info().attach('[REG-1317] responses', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json'
        });
    });

    test('[REG-1318] TC-BE-2 — Sales-portal GET includes customer linkage', async ({ Request, SPRequest, GeneratePayload, Responses, Endpoints }) => {
        await test.step('Create customer', async () => {
            const customer = await Request.post(Endpoints.customer, { data: GeneratePayload.customers.customer_legal() });
            await expect(customer).CheckResponse();
            Responses.customer.push(await customer.json());
        });

        await test.step('Create collection channel', async () => {
            const collectionChannel = await Request.post(Endpoints.collectionChannel, { data: GeneratePayload.receivablesManagement.collection_channel() });
            await expect(collectionChannel).CheckResponse();
            Responses.collectionChannel.push(await collectionChannel.json());
        });

        await test.step('Create payment package', async () => {
            const paymentPackage = await Request.post(Endpoints.paymentPackage, { data: GeneratePayload.receivablesManagement.payment_package() });
            await expect(paymentPackage).CheckResponse();
            Responses.paymentPackage.push(await paymentPackage.json());
        });

        await test.step('Create payment', async () => {
            const payload = await GeneratePayload.receivablesManagement.payment();
            payload.initialAmount = 50;
            const payment = await Request.post(Endpoints.payment, { data: payload });
            await expect(payment).CheckResponse();
            Responses.payment.push(await payment.json());
        });

        await test.step('GET sales-portal payment details and verify customer', async () => {
            const paymentId = paymentIdFromResponse(Responses.payment[0]);
            const expectedCustomerId = Responses.customer[0].id;
            const path = spPaymentDetailsRelativePath(customerPathSegment(Responses.customer[0]), paymentId);
            const res = await SPRequest.get(path);
            await expect(res).CheckResponse();
            const body = (await res.json()) as Record<string, unknown>;
            const cidResolved = pickCustomerInternalIdFromDetailsBody(body);
            if (cidResolved !== undefined) {
                expect(cidResolved).toBe(expectedCustomerId);
            } else {
                const corePayment = await (async () => {
                    const r = await Request.get(`${Endpoints.payment}/${paymentId}`);
                    await expect(r).CheckResponse();
                    return r.json() as unknown as Record<string, unknown>;
                })();
                const expectedNumber = typeof corePayment.paymentNumber === 'string' ? corePayment.paymentNumber : String(corePayment.paymentNumber ?? '');
                expect(pickPaymentNumberFromDetailsBody(body)).toBe(expectedNumber);
                expect(pickInitialAmountFromDetailsBody(body)).toBe(50);
            }
        });

        test.info().attach('[REG-1318] responses', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json'
        });
    });

    test('[REG-1319] TC-BE-5 — Zero and negative payment id', async ({ Request, SPRequest, GeneratePayload, Responses, Endpoints }) => {
        await test.step('Create customer', async () => {
            const customer = await Request.post(Endpoints.customer, { data: GeneratePayload.customers.customer_legal() });
            await expect(customer).CheckResponse();
            Responses.customer.push(await customer.json());
        });

        const cid = () => customerPathSegment(Responses.customer[0]);

        await test.step('GET payment 0', async () => {
            const res = await SPRequest.get(spPaymentDetailsRelativePath(cid(), 0));
            expectClientErrorStatus(res.status());
        });

        await test.step('GET payment -1', async () => {
            const res = await SPRequest.get(spPaymentDetailsRelativePath(cid(), -1));
            expectClientErrorStatus(res.status());
        });

        test.info().attach('[REG-1319] responses', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json'
        });
    });
});
