import { test, expect } from '../../../fixtures/baseFixture';

/** Nomenclature profile "Измерено количество" — timezone EET. */
const EET_MEASURED_PROFILE_ID = 81;

function pad(value: number): string {
    return String(value).padStart(2, '0');
}

function formatLocalDateTime(date: Date): string {
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:00`;
}

/** October 2026 15-minute wall clock, plus the EET fallback hour on 25 Oct. */
function octoberFifteenMinuteSlots(): Array<{ periodFrom: string; shiftedHour: boolean }> {
    const slots: Array<{ periodFrom: string; shiftedHour: boolean }> = [];
    const cursor = new Date(2026, 9, 1, 0, 0, 0);
    const end = new Date(2026, 9, 31, 23, 45, 0);
    while (cursor.getTime() <= end.getTime()) {
        slots.push({ periodFrom: formatLocalDateTime(cursor), shiftedHour: false });
        cursor.setMinutes(cursor.getMinutes() + 15);
    }
    for (const minute of [0, 15, 30, 45]) {
        slots.push({
            periodFrom: `2026-10-25T03:${pad(minute)}:00`,
            shiftedHour: true,
        });
    }
    return slots;
}

test('EET formula price parameter with EET 15-minute profile', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
}) => {
    test.setTimeout(15 * 60 * 1000);
    const slots = octoberFifteenMinuteSlots();

    await test.step('Precondition: existing legal customer', async () => {
        const customer = await Request.get('customer/6048311?version=1');
        await expect(customer).CheckResponse();
        const body = await customer.json();
        Responses.customer.push({ id: body.id ?? 6048311, customerDetailsId: body.customerDetailsId });
    });

    await test.step('Precondition: settlement POD', async () => {
        const pod = await Request.post(Endpoints.pod, { data: GeneratePayload.pointsOfDelivery.pod_settlement() });
        await expect(pod).CheckResponse();
        Responses.pod.push(await pod.json());
    });

    await test.step('Precondition: term', async () => {
        const term = await Request.post(Endpoints.terms, { data: GeneratePayload.productAndServices.term() });
        await expect(term).CheckResponse();
        Responses.terms.push(await term.json());
    });

    let priceParameterId = 0;
    await test.step('Precondition: EET 15-minute price parameter for October 2026', async () => {
        const created = await Request.post(Endpoints.priceParameters, {
            data: {
                name: `EET15FORMULA${Date.now()}`,
                periodType: 'FIFTEEN_MINUTES',
                timeZone: 'EET',
                priceParameterDetails: [],
            },
        });
        await expect(created).CheckResponse();
        const body = await created.json();
        priceParameterId = typeof body === 'number' ? body : body.id;
        Responses.priceParameters.push(priceParameterId);

        const locked = await Request.post(`locks/acquire?entityType=price-parameters&entityId=${priceParameterId}`);
        await expect(locked).CheckResponse();
        const updated = await Request.put(`${Endpoints.priceParameters}/${priceParameterId}`, {
            data: {
                versionId: 1,
                name: `EET15FORMULA${priceParameterId}`,
                newVersion: false,
                periodType: 'FIFTEEN_MINUTES',
                priceParameterDetails: slots.map((slot) => ({
                    periodFrom: slot.periodFrom,
                    price: 100,
                    shiftedHour: slot.shiftedHour,
                })),
            },
        });
        await expect(updated).CheckResponse();
    });

    await test.step('Precondition: settlement price component formula uses the EET price parameter', async () => {
        const payload = GeneratePayload.productAndServices.priceSettlement();
        payload.name = `EET15FORMULA${Date.now()}`;
        payload.displayName = payload.name;
        payload.formulaRequest.expression = `$${priceParameterId}$`;
        payload.formulaRequest.priceParameterIds = [priceParameterId];
        payload.applicationModelRequest.settlementPeriodsRequest.timeZone = 'EET';
        payload.applicationModelRequest.settlementPeriodsRequest.profiles = [
            { profileId: EET_MEASURED_PROFILE_ID, percentage: 100 },
        ];
        const created = await Request.post(Endpoints.priceComponent, { data: payload });
        await expect(created).CheckResponse();
        const body = await created.json();
        Responses.priceComponent.push(typeof body === 'number' ? body : body.id);
    });

    await test.step('Precondition: product', async () => {
        const product = await Request.post(Endpoints.product, { data: GeneratePayload.productAndServices.product() });
        await expect(product).CheckResponse();
        const body = await product.json();
        Responses.product.push(typeof body === 'number' ? body : body.id);
    });

    await test.step('Precondition: contract', async () => {
        const payload = await GeneratePayload.contractsAndOrders.product_contract();
        payload.productParameters.contractType = 'WITHOUT_SUPPLY';
        const contract = await Request.post(Endpoints.productContract, { data: payload });
        await expect(contract).CheckResponse();
        Responses.productContract.push(await contract.json());
    });

    await test.step('Precondition: activate POD from 1 Oct 2026', async () => {
        const activation = await Request.post('/contract-pods/manual', {
            data: await GeneratePayload.pointsOfDelivery.pod_activation(0, '2026-10-01'),
        });
        await expect(activation).CheckResponse();
    });

    await test.step('Precondition: EET 15-minute profile for October 2026', async () => {
        const podGet = await Request.get(`pod/${Responses.pod[0].id}?version=1`);
        await expect(podGet).CheckResponse();
        const podJson = await podGet.json();
        const profile = await Request.post('billing-by-profile', {
            data: {
                identifier: podJson.identifier,
                periodType: 'FIFTEEN_MINUTES',
                profileId: EET_MEASURED_PROFILE_ID,
                periodFrom: '2026-10-01T00:00:00',
                periodTo: '2026-10-31T00:00:00',
                warningAcceptedByUser: true,
                entries: [],
            },
        });
        await expect(profile).CheckResponse();
        const profileBody = await profile.json();
        const profileId = typeof profileBody === 'number' ? profileBody : profileBody.id;
        Responses.dataByProfiles.push(profileId);

        const locked = await Request.post(`locks/acquire?entityType=data-by-profiles&entityId=${profileId}`);
        await expect(locked).CheckResponse();
        const filled = await Request.put(`billing-by-profile/${profileId}`, {
            data: {
                entries: slots.map((slot) => ({
                    periodFrom: slot.periodFrom,
                    value: 100,
                    shiftedHour: slot.shiftedHour,
                })),
            },
        });
        await expect(filled).CheckResponse();
    });

    let billingRunId = 0;
    await test.step('Action: create and start standard billing', async () => {
        const billingRun = await Request.post(Endpoints.billingRun, {
            data: await GeneratePayload.billing.billingRun(),
        });
        await expect(billingRun).CheckResponse();
        const billingBody = await billingRun.json();
        billingRunId = billingBody.id ?? billingBody;
        Responses.billingRun.push(billingBody);
        console.log(`EET formula case billing run id: ${billingRunId}`);

        const started = await Request.patch(`billing-run/start-billing?billingRunId=${billingRunId}`);
        await expect(started).CheckResponse();

        const deadline = Date.now() + 8 * 60 * 1000;
        let status = '';
        while (Date.now() < deadline) {
            const statusRes = await Request.get(`billing-run/${billingRunId}`);
            await expect(statusRes).CheckResponse();
            status = (await statusRes.json()).commonParameters?.status ?? '';
            if (status === 'DRAFT') {
                break;
            }
            await new Promise((resolve) => setTimeout(resolve, 15000));
        }
        expect(status, `billing run ${billingRunId} did not reach DRAFT`).toBe('DRAFT');
    });

    await test.step('Check: draft invoice exists and formula error is absent', async () => {
        const drafts = await Request.get(`billing-run/draft-invoices?id=${billingRunId}&page=0&size=25`);
        await expect(drafts).CheckResponse();
        const draftBody = await drafts.json();
        const invoiceIds = (draftBody.content ?? []).map((row: { id: number }) => row.id);
        console.log(`EET formula case draft invoices: ${invoiceIds.join(',') || 'none'}`);
        expect(invoiceIds.length, 'expected a draft invoice for the EET profile').toBeGreaterThan(0);

        const errorReport = await Request.get(`billing-run/download-error-report/${billingRunId}?protocol=BILLING`);
        const errorText = await errorReport.text();
        expect(errorText, errorText.slice(0, 500)).not.toContain('resolved formula was not found');
        expect(errorText).not.toContain('dimensions does not match');
    });
});
