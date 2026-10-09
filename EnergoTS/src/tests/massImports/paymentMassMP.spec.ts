import * as fs from 'fs';
import * as iconv from 'iconv-lite';
import { test, expect } from '../../backend/fixtures/baseFixture';
import reportGenerator from '../../backend/utils/generateReport';
import { randomGens } from '../../backend/utils/randomGens';
import {
    PaymentMassImportGenerator,
    PAYMENT_PARTNER_AMOUNT_LENGTH,
    PAYMENT_PARTNER_PREFIX_LENGTH,
    PAYMENT_PARTNER_ROW_LENGTH,
} from '../../backend/mass-imports/generators/domains/paymentMP';

const FILE_ENCODING = 'win1251';

function readWindows1251(filePath: string): string {
    return iconv.decode(fs.readFileSync(filePath), FILE_ENCODING);
}

test.describe('Payment Mass Import Generation', { tag: '@massImport' }, () => {
    test('Generate bank partner txt file from billing preconditions', async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        MassImportGenerator,
        receivableValidations,
    }) => {
        test.setTimeout(8 * 60 * 1000);

        let invoicePrefix = '';
        let invoiceNumber = '';
        let invoiceAmount = 0;
        let customerNumber = '';
        let customerIdentifier = '';
        let customerName = 'AUTOMATION';
        let collectionChannelId = 0;

        await test.step('generate customer', async () => {
            const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
            await expect(customer).CheckResponse();
            Responses.customer.push(await customer.json());
        });

        await test.step('Create term', async () => {
            const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
            await expect(term).CheckResponse();
            Responses.terms.push(await term.json())
        });

        await test.step('Create POD', async () => {
            const podSettlement = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});
            await expect(podSettlement).CheckResponse();
            Responses.pod.push(await podSettlement.json());
        });

        await test.step('Create product', async () => {
            const product = await Request.post(Endpoints.product, {data: GeneratePayload.productAndServices.product()});
            await expect(product).CheckResponse();
            Responses.product.push(await product.json())
        });

        await test.step('Create contract', async () => {
            const contract = await Request.post(Endpoints.productContract, {data: await GeneratePayload.contractsAndOrders.product_contract()});
            await expect(contract).CheckResponse();
            Responses.productContract.push(await contract.json())
        });

        await test.step('Activate POD', async () => {
            const podActivation = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation()});
            await expect(podActivation).CheckResponse();
        });

        await test.step('Create billing run for manual invoice', async () => {
            const payload = await GeneratePayload.billing.manualInvoice();
            const createManualInvoice = await Request.post(Endpoints.billingRun, {data: payload});

            await expect(createManualInvoice).CheckResponse();
            
            const responseBody = await createManualInvoice.json();
            Responses.billingRun.push(responseBody);
        })

        await test.step('make the invoice real', async () => {
            await GeneratePayload.billing.waitForInvoiceGeneration()
        })

        await test.step('read invoice and customer values for mass import', async () => {
            const customerGet = await Request.get(`${Endpoints.customer}/${Responses.customer[0].id}?version=1`);
            await expect(customerGet).CheckResponse();
            const customerJson = await customerGet.json();
            customerNumber = String(customerJson.customerNumber);
            customerIdentifier = customerJson.customerIdentifier ?? customerJson.identifier;
            customerName = customerJson.businessCustomerDetails?.name ?? 'AUTOMATION';

            const invoiceGet = await Request.get(`${Endpoints.invoice}?id=${Responses.invoice[0]}`);
            await expect(invoiceGet).CheckResponse();
            const invoiceJson = await invoiceGet.json();
            invoiceAmount = invoiceJson.totalAmountIncludingVat;

            const findLiability = await Request.get(
                `${Endpoints.customerLiability}/list?page=0&size=25&columns=ID&direction=DESC&prompt=${customerIdentifier}&searchFields=CUSTOMER`,
            );
            await expect(findLiability).CheckResponse();
            const liabilityList = await findLiability.json();
            expect(liabilityList.content ?? []).toHaveLength(1);

            const liabilityGet = await Request.get(`${Endpoints.customerLiability}/${liabilityList.content[0].id}`);
            await expect(liabilityGet).CheckResponse();
            const liabilityJson = await liabilityGet.json();

            const resolvedInvoice = PaymentMassImportGenerator.resolveInvoiceParts({
                prefix: invoiceJson.prefix,
                invoiceNumber: invoiceJson.invoiceNumber,
                fullInvoiceNumber: liabilityJson.invoiceResponse?.invoiceNumber,
            });
            invoicePrefix = resolvedInvoice.invoicePrefix;
            invoiceNumber = resolvedInvoice.invoiceNumber;

            expect(customerNumber).toBeTruthy();
            expect(customerIdentifier).toBeTruthy();
            expect(invoiceJson.prefix).toBeTruthy();
            expect(invoiceNumber).toHaveLength(10);
            expect(invoicePrefix.trim()).toBeTruthy();
            expect(liabilityJson.invoiceResponse.id).toEqual(Responses.invoice[0]);
        });

        await test.step('create bank partner collection channel', async () => {
            const collectionChannel = await Request.post(Endpoints.collectionChannel, {
                data: GeneratePayload.receivablesManagement.bank_partner_collection_channel(),
            });
            await expect(collectionChannel).CheckResponse();
            const channelBody = await collectionChannel.json();
            collectionChannelId = channelBody.id ?? channelBody;
            Responses.collectionChannel.push(collectionChannelId);
        });

        let bankPartnerFilePath = '';

        await test.step('generate bank partner txt file', async () => {
            const paymentDate = new Date();
            const generator = new PaymentMassImportGenerator();
            const paymentRow = PaymentMassImportGenerator.buildBankPartnerRowsFromEntities({
                paymentDate,
                customerNumber,
                invoicePrefix: invoicePrefix.trim(),
                invoiceNumber,
                amount: invoiceAmount,
                customerName,
            });

            bankPartnerFilePath = generator.generateBankPartnerFile(
                {
                    iban: 'BG20STSA93000024288530',
                    currency: 'EUR',
                    openingBalanceDate: paymentDate,
                    openingBalance: invoiceAmount,
                    paymentDate,
                },
                [paymentRow],
            );

            const fileContent = readWindows1251(bankPartnerFilePath);
            const lines = fileContent.split(/\r?\n/);

            expect(lines[0]).toMatch(/^:20:\d{6}$/);
            expect(fileContent.indexOf(':20:')).toBeLessThan(fileContent.indexOf(':25:'));
            expect(fileContent).toContain(':25:BG20STSA93000024288530');
            expect(fileContent).toContain(':60F:');
            expect(fileContent).toContain(':61:');
            expect(fileContent).toContain(':86:');
            expect(fileContent).toContain('ЕНЕРГИЯ');
            expect(fileContent).toContain(customerNumber);
            expect(fileContent).toContain(`${invoicePrefix.trim()}${invoiceNumber}`);
        });

        await test.step('upload bank partner mass import and check report', async () => {
            const uploadResult = await MassImportGenerator.uploadPayment('bank', collectionChannelId);
            expect(uploadResult.result.success, uploadResult.result.message ?? undefined).toBe(true);
        });

        await test.step('find and validate created payment', async () => {
            const paymentList = await Request.post(`${Endpoints.payment}/list`, {
                data: {
                    page: 0,
                    size: 25,
                    prompt: customerIdentifier,
                    initialAmountFrom: null,
                    initialAmountTo: null,
                    currentAmountFrom: null,
                    currentAmountTo: null,
                    collectionChannelIds: [collectionChannelId],
                    blockedForOffsetting: null,
                    paymentDateFrom: null,
                    paymentDateTo: null,
                    columns: 'PAYMENT_NUMBER',
                    searchFields: 'CUSTOMER_IDENTIFIER',
                    direction: 'DESC',
                    exactMatch: false,
                },
            });
            await expect(paymentList).CheckResponse();
            const listBody = await paymentList.json();
            expect(listBody.content?.length ?? 0).toBeGreaterThan(0);

            const paymentId = listBody.content[0].id;
            Responses.payment.push(paymentId);

            await receivableValidations.paymentMassImportValidation({
                paymentId,
                customerId: Responses.customer[0].id,
                customerIdentifier,
                collectionChannelId,
                invoiceId: Responses.invoice[0],
                invoiceAmount,
            });
        });

        test.info().attach('bank-partner-txt', {
            body: readWindows1251(bankPartnerFilePath),
            contentType: 'text/plain',
        });

        test.info().attach('payment-mass-import responses', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('Generate payment partner txt file from billing preconditions', async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        MassImportGenerator,
        receivableValidations,
    }) => {
        test.setTimeout(8 * 60 * 1000);

        let invoicePrefix = '';
        let invoiceNumber = '';
        let invoiceAmount = 0;
        let customerNumber = '';
        let customerIdentifier = '';
        let collectionChannelId = 0;

        await test.step('generate customer', async () => {
            const payload = GeneratePayload.customers.customer_legal();
            payload.customerIdentifier = randomGens.generateRandomString(false, true, 10);
            const customer = await Request.post(Endpoints.customer, {data: payload});
            await expect(customer).CheckResponse();
            Responses.customer.push(await customer.json());
        });

        await test.step('Create term', async () => {
            const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
            await expect(term).CheckResponse();
            Responses.terms.push(await term.json())
        });

        await test.step('Create POD', async () => {
            const podSettlement = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});
            await expect(podSettlement).CheckResponse();
            Responses.pod.push(await podSettlement.json());
        });

        await test.step('Create product', async () => {
            const product = await Request.post(Endpoints.product, {data: GeneratePayload.productAndServices.product()});
            await expect(product).CheckResponse();
            Responses.product.push(await product.json())
        });

        await test.step('Create contract', async () => {
            const contract = await Request.post(Endpoints.productContract, {data: await GeneratePayload.contractsAndOrders.product_contract()});
            await expect(contract).CheckResponse();
            Responses.productContract.push(await contract.json())
        });

        await test.step('Activate POD', async () => {
            const podActivation = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation()});
            await expect(podActivation).CheckResponse();
        });

        await test.step('Create billing run for manual invoice', async () => {
            const payload = await GeneratePayload.billing.manualInvoice();
            const createManualInvoice = await Request.post(Endpoints.billingRun, {data: payload});

            await expect(createManualInvoice).CheckResponse();
            
            const responseBody = await createManualInvoice.json();
            Responses.billingRun.push(responseBody);
        })

        await test.step('make the invoice real', async () => {
            await GeneratePayload.billing.waitForInvoiceGeneration()
        })

        await test.step('read invoice and customer values for mass import', async () => {
            const customerGet = await Request.get(`${Endpoints.customer}/${Responses.customer[0].id}?version=1`);
            await expect(customerGet).CheckResponse();
            const customerJson = await customerGet.json();
            customerNumber = String(customerJson.customerNumber);
            customerIdentifier = customerJson.customerIdentifier ?? customerJson.identifier;

            const invoiceGet = await Request.get(`${Endpoints.invoice}?id=${Responses.invoice[0]}`);
            await expect(invoiceGet).CheckResponse();
            const invoiceJson = await invoiceGet.json();
            invoiceAmount = invoiceJson.totalAmountIncludingVat;

            const findLiability = await Request.get(
                `${Endpoints.customerLiability}/list?page=0&size=25&columns=ID&direction=DESC&prompt=${customerIdentifier}&searchFields=CUSTOMER`,
            );
            await expect(findLiability).CheckResponse();
            const liabilityList = await findLiability.json();
            expect(liabilityList.content ?? []).toHaveLength(1);

            const liabilityGet = await Request.get(`${Endpoints.customerLiability}/${liabilityList.content[0].id}`);
            await expect(liabilityGet).CheckResponse();
            const liabilityJson = await liabilityGet.json();

            const resolvedInvoice = PaymentMassImportGenerator.resolveInvoiceParts({
                prefix: invoiceJson.prefix,
                invoiceNumber: invoiceJson.invoiceNumber,
                fullInvoiceNumber: liabilityJson.invoiceResponse?.invoiceNumber,
            });
            invoicePrefix = resolvedInvoice.invoicePrefix;
            invoiceNumber = resolvedInvoice.invoiceNumber;

            expect(customerNumber).toBeTruthy();
            expect(customerIdentifier).toBeTruthy();
            expect(customerIdentifier).toHaveLength(10);
            expect(invoiceJson.prefix).toBeTruthy();
            expect(invoiceNumber).toHaveLength(10);
            expect(invoicePrefix.trim()).toBeTruthy();
            expect(liabilityJson.invoiceResponse.id).toEqual(Responses.invoice[0]);
        });

        await test.step('create payment partner collection channel', async () => {
            const collectionChannel = await Request.post(Endpoints.collectionChannel, {
                data: GeneratePayload.receivablesManagement.collection_channel(),
            });
            await expect(collectionChannel).CheckResponse();
            const channelBody = await collectionChannel.json();
            collectionChannelId = channelBody.id ?? channelBody;
            Responses.collectionChannel.push(collectionChannelId);
        });

        let paymentPartnerFilePath = '';

        await test.step('generate payment partner txt file', async () => {
            const generator = new PaymentMassImportGenerator();
            const row = PaymentMassImportGenerator.buildPaymentPartnerRowsFromEntities({
                customerNumber,
                customerIdentifier,
                invoicePrefix,
                invoiceNumber,
                amount: invoiceAmount,
            });

            paymentPartnerFilePath = generator.generatePaymentPartnerFile([row]);
            const fileContent = readWindows1251(paymentPartnerFilePath);
            const line = fileContent.replace(/\r?\n$/, '');

            expect(line).toHaveLength(PAYMENT_PARTNER_ROW_LENGTH);
            expect(line.slice(0, 16).trim()).toBe(PaymentMassImportGenerator.truncateField(customerNumber, 16));
            expect(line.slice(16, 26).trim()).toBe(PaymentMassImportGenerator.truncateField(customerIdentifier, 10));
            expect(line.slice(46, 46 + PAYMENT_PARTNER_PREFIX_LENGTH)).toBe(invoicePrefix);
            expect(line.slice(49, 59)).toBe(invoiceNumber);
            expect(line.slice(59, 59 + PAYMENT_PARTNER_AMOUNT_LENGTH).trim()).toBe(Number(invoiceAmount).toFixed(2));
        });

        await test.step('upload payment partner mass import and check report', async () => {
            const uploadResult = await MassImportGenerator.uploadPayment('paymentPartner', collectionChannelId);
            expect(uploadResult.result.success, uploadResult.result.message ?? undefined).toBe(true);
        });

        await test.step('find and validate created payment', async () => {
            const paymentList = await Request.post(`${Endpoints.payment}/list`, {
                data: {
                    page: 0,
                    size: 25,
                    prompt: customerIdentifier,
                    initialAmountFrom: null,
                    initialAmountTo: null,
                    currentAmountFrom: null,
                    currentAmountTo: null,
                    collectionChannelIds: [collectionChannelId],
                    blockedForOffsetting: null,
                    paymentDateFrom: null,
                    paymentDateTo: null,
                    columns: 'PAYMENT_NUMBER',
                    searchFields: 'CUSTOMER_IDENTIFIER',
                    direction: 'DESC',
                    exactMatch: false,
                },
            });
            await expect(paymentList).CheckResponse();
            const listBody = await paymentList.json();
            expect(listBody.content?.length ?? 0).toBeGreaterThan(0);

            const paymentId = listBody.content[0].id;
            Responses.payment.push(paymentId);

            await receivableValidations.paymentMassImportValidation({
                paymentId,
                customerId: Responses.customer[0].id,
                customerIdentifier,
                collectionChannelId,
                invoiceId: Responses.invoice[0],
                invoiceAmount,
            });
        });

        test.info().attach('payment-partner-txt', {
            body: readWindows1251(paymentPartnerFilePath),
            contentType: 'text/plain',
        });

        test.info().attach('payment-mass-import responses', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });
});
