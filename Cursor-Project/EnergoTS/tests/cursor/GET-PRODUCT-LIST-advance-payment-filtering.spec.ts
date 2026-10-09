import { test, expect } from '../../fixtures/baseFixture';
import reportGenerator from '../../utils/generateReport';
import { envVariables } from '../../fixtures/envCashed';
import { templates as TemplatesGenerator } from '../../jsons/payloads/create/nomenclatures/templates';

type ProductTemplateRefs = { templateId: number; templateType: 'EMAIL_TEMPLATE' | 'INVOICE_TEMPLATE' | 'CONTRACT_TEMPLATE' }[];

async function ensureProductTemplates(FileUploadRequest: any, Request: any): Promise<ProductTemplateRefs> {
    const generator = new TemplatesGenerator(FileUploadRequest, Request.raw);
    const ids = await generator.generateEveryTemplate();

    return [
        { templateId: ids.invoice_email_template, templateType: 'EMAIL_TEMPLATE' },
        { templateId: ids.invoice_document_template, templateType: 'INVOICE_TEMPLATE' },
        { templateId: ids.product_contract_template, templateType: 'CONTRACT_TEMPLATE' },
    ];
}

async function sharedTerm(Request: any, GeneratePayload: any, Responses: any, Endpoints: any) {
    const termPayload = GeneratePayload.productAndServices.term();
    const termResponse = await Request.post(Endpoints.terms, { data: termPayload });
    await expect(termResponse).toBeOK();
    Responses.terms.push(await termResponse.json());
}

async function sharedPrice(Request: any, GeneratePayload: any, Responses: any, Endpoints: any) {
    const pricePayload = GeneratePayload.productAndServices.electricity();
    const priceResponse = await Request.post(Endpoints.priceComponent, { data: pricePayload });
    await expect(priceResponse).toBeOK();
    Responses.priceComponent.push(await priceResponse.json());
}

async function createProductAndGetId(Request: any, GeneratePayload: any, Responses: any, Endpoints: any, templateIds: ProductTemplateRefs) {
    const payload = GeneratePayload.productAndServices.product();
    payload.contractTypes = ['SUPPLY_ONLY'];
    payload.paymentGuarantees = ['NO'];
    payload.templateIds = templateIds;
    const response = await Request.post(Endpoints.product, { data: payload });
    await expect(response).toBeOK();
    const json = await response.json();
    Responses.product.push(json.id ?? json);
    return { id: json.id, name: json.name };
}

async function assertProductInList(SPRequest: any, productId: number, expectedInList: boolean) {
    let found: any = undefined;

    // The list endpoint can be eventually consistent and/or paginated.
    // Scan a few pages and retry briefly before asserting.
    for (let attempt = 0; attempt < 3 && (expectedInList ? !found : true); attempt++) {
        for (let page = 0; page < 5; page++) {
            const response = await SPRequest.post('product/list', { data: { page, size: 200 } });
            await expect(response).toBeOK();
            const body = await response.json();
            found = body.content?.find((p: any) => String(p.id) === String(productId));
            if (found) break;
        }
        if (found) break;
        await new Promise((r) => setTimeout(r, 1500));
    }

    if (expectedInList) {
        expect(found).toBeTruthy();
    } else {
        expect(found).toBeFalsy();
    }
}

async function createIap(Request: any, GeneratePayload: any, Responses: any, Endpoints: any, mutate?: (p: any) => void) {
    const payload = GeneratePayload.productAndServices.interim();
    if (mutate) mutate(payload);
    const response = await Request.post(Endpoints.interim, { data: payload });
    await expect(response).toBeOK();
    const json = await response.json();
    Responses.interim.push(json.id ?? json);
    return json.id ?? json;
}

async function createIapGroup(Request: any, Responses: any, Endpoints: any, iapId: number, groupName?: string) {
    const payload = {
        name: groupName ?? `IAP-Group-${Date.now()}`,
        interimAdvancePaymentIds: [iapId],
    };
    const response = await Request.post(Endpoints.groupOfInterims, { data: payload });
    await expect(response).toBeOK();
    const json = await response.json();
    Responses.groupOfInterims.push({ id: json.id });
    return json.id;
}

test.describe('[REG-XXX]: GET product list | advance payment filtering', { tag: '@productAndServices' }, () => {
    test.describe.configure({ mode: 'serial' });

    test('[REG-XXX]: TC1 – no direct/group advance payment → product returned', async ({
        Request, SPRequest, GeneratePayload, Endpoints, Responses, FileUploadRequest,
    }) => {
        let product: { id: number; name: string };
        let templateIds: ProductTemplateRefs;

        await test.step('Precondition: Ensure product templates exist', async () => {
            templateIds = await ensureProductTemplates(FileUploadRequest, Request);
        });

        await test.step('Precondition: Create terms and price component', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Create product without IAP links', async () => {
            product = await createProductAndGetId(Request, GeneratePayload, Responses, Endpoints, templateIds);
        });

        await test.step('Verify product returned in list', async () => {
            await assertProductInList(SPRequest, product.id, true);
        });

        test.info().attach('[REG-XXX] TC1 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[REG-XXX]: TC2 – advance payment group linked (active) → product returned', async ({
        Request, SPRequest, GeneratePayload, Endpoints, Responses, FileUploadRequest,
    }) => {
        let product: { id: number; name: string };
        let templateIds: ProductTemplateRefs;

        await test.step('Precondition: Ensure product templates exist', async () => {
            templateIds = await ensureProductTemplates(FileUploadRequest, Request);
        });

        await test.step('Precondition: Create terms and price component', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Precondition: Create valid IAP', async () => {
            await createIap(Request, GeneratePayload, Responses, Endpoints, (p) => {
                p.paymentType = 'OBLIGATORY';
                p.valueType = 'EXACT_AMOUNT';
                p.value = 100;
                p.currencyId = envVariables.currency;
                p.matchesWithTermOfStandardInvoice = true;
                p.interimAdvancePaymentTerm = null;
                p.dateOfIssueType = 'MATCH_THE_INVOICE_DATE';
                p.dateOfIssueValue = null;
                p.dateOfIssueValueFrom = null;
                p.dateOfIssueValueTo = null;
                p.valueFrom = null;
                p.valueTo = null;
            });
        });

        await test.step('Precondition: Create IAP group', async () => {
            const iapId = typeof Responses.interim[0] === 'object' ? Responses.interim[0].id : Responses.interim[0];
            await createIapGroup(Request, Responses, Endpoints, iapId);
        });

        await test.step('Create product (group is taken from Responses.groupOfInterims)', async () => {
            product = await createProductAndGetId(Request, GeneratePayload, Responses, Endpoints, templateIds);
        });

        await test.step('Verify product returned in list', async () => {
            await assertProductInList(SPRequest, product.id, true);
        });

        test.info().attach('[REG-XXX] TC2 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[REG-XXX]: TC3 – advance payment linked directly to product → product returned', async ({
        Request, SPRequest, GeneratePayload, Endpoints, Responses, FileUploadRequest,
    }) => {
        let product: { id: number; name: string };
        let templateIds: ProductTemplateRefs;

        await test.step('Precondition: Ensure product templates exist', async () => {
            templateIds = await ensureProductTemplates(FileUploadRequest, Request);
        });

        await test.step('Precondition: Create terms and price component', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Precondition: Create valid direct IAP', async () => {
            await createIap(Request, GeneratePayload, Responses, Endpoints, (p) => {
                p.paymentType = 'OBLIGATORY';
                p.valueType = 'EXACT_AMOUNT';
                p.value = 100;
                p.currencyId = envVariables.currency;
                p.matchesWithTermOfStandardInvoice = true;
                p.interimAdvancePaymentTerm = null;
                p.dateOfIssueType = 'MATCH_THE_INVOICE_DATE';
                p.dateOfIssueValue = null;
                p.dateOfIssueValueFrom = null;
                p.dateOfIssueValueTo = null;
                p.valueFrom = null;
                p.valueTo = null;
            });
        });

        await test.step('Create product (direct IAP is taken from Responses.interim)', async () => {
            product = await createProductAndGetId(Request, GeneratePayload, Responses, Endpoints, templateIds);
        });

        await test.step('Verify product returned in list', async () => {
            await assertProductInList(SPRequest, product.id, true);
        });

        test.info().attach('[REG-XXX] TC3 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[REG-XXX]: TC5 – at least one selected (AT_LEAST_ONE) → product NOT returned', async ({
        Request, SPRequest, GeneratePayload, Endpoints, Responses, FileUploadRequest,
    }) => {
        let product: { id: number; name: string };
        let templateIds: ProductTemplateRefs;

        await test.step('Precondition: Ensure product templates exist', async () => {
            templateIds = await ensureProductTemplates(FileUploadRequest, Request);
        });

        await test.step('Precondition: Create terms and price component', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Precondition: Create IAP with AT_LEAST_ONE', async () => {
            await createIap(Request, GeneratePayload, Responses, Endpoints, (p) => {
                p.paymentType = 'AT_LEAST_ONE';
            });
        });

        await test.step('Create product with AT_LEAST_ONE IAP', async () => {
            product = await createProductAndGetId(Request, GeneratePayload, Responses, Endpoints, templateIds);
        });

        await test.step('Verify product excluded from list', async () => {
            await assertProductInList(SPRequest, product.id, false);
        });

        test.info().attach('[REG-XXX] TC5 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[REG-XXX]: TC22/23 – Custom payment term (term value present → returned, term value null → NOT returned)', async ({
        Request, SPRequest, GeneratePayload, Endpoints, Responses, FileUploadRequest,
    }) => {
        let validProduct: { id: number; name: string };
        let invalidProduct: { id: number; name: string };
        let templateIds: ProductTemplateRefs;

        await test.step('Precondition: Ensure product templates exist', async () => {
            templateIds = await ensureProductTemplates(FileUploadRequest, Request);
        });

        await test.step('Precondition: Create terms and price component', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Create product with valid custom payment term', async () => {
            await createIap(Request, GeneratePayload, Responses, Endpoints, (p) => {
                p.paymentType = 'OBLIGATORY';
                p.valueType = 'EXACT_AMOUNT';
                p.value = 100;
                p.currencyId = envVariables.currency;
                p.matchesWithTermOfStandardInvoice = false;
                p.interimAdvancePaymentTerm = {
                    calendarType: 'WORKING_DAYS',
                    name: 'TERM',
                    value: 5,
                    valueFrom: null,
                    valueTo: null,
                    calendarId: envVariables.calendar.id,
                    excludeWeekends: false,
                    excludeHolidays: false,
                    dueDateChange: null,
                };
                p.dateOfIssueType = 'MATCH_THE_INVOICE_DATE';
                p.dateOfIssueValue = null;
                p.dateOfIssueValueFrom = null;
                p.dateOfIssueValueTo = null;
            });
            validProduct = await createProductAndGetId(Request, GeneratePayload, Responses, Endpoints, templateIds);
        });

        await test.step('Reset IAP links and create product with invalid custom payment term (value null)', async () => {
            Responses.interim = [];
            await createIap(Request, GeneratePayload, Responses, Endpoints, (p) => {
                p.paymentType = 'OBLIGATORY';
                p.valueType = 'EXACT_AMOUNT';
                p.value = 100;
                p.currencyId = envVariables.currency;
                p.matchesWithTermOfStandardInvoice = false;
                p.interimAdvancePaymentTerm = {
                    calendarType: 'WORKING_DAYS',
                    name: 'TERM',
                    value: null,
                    valueFrom: null,
                    valueTo: null,
                    calendarId: envVariables.calendar.id,
                    excludeWeekends: false,
                    excludeHolidays: false,
                    dueDateChange: null,
                };
                p.dateOfIssueType = 'MATCH_THE_INVOICE_DATE';
                p.dateOfIssueValue = null;
                p.dateOfIssueValueFrom = null;
                p.dateOfIssueValueTo = null;
            });
            invalidProduct = await createProductAndGetId(Request, GeneratePayload, Responses, Endpoints, templateIds);
        });

        await test.step('Verify list inclusion/exclusion', async () => {
            await assertProductInList(SPRequest, validProduct.id, true);
            await assertProductInList(SPRequest, invalidProduct.id, false);
        });

        test.info().attach('[REG-XXX] TC22-23 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });
});

