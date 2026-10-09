import { APIRequestContext } from "playwright";
import { baseFixture } from "../../../fixtures/baseFixture";
import { forVolumes } from '../../payloads/create/billing/forVolumes';
import { manualInvoice } from '../../payloads/create/billing/manualInvoice';
import { debitNote } from '../../payloads/create/billing/debitNote';
import { creditNote } from '../../payloads/create/billing/creditNote';
import { Manualinterim } from '../../payloads/create/billing/manualoInterim';
import { periodicBilling } from '../../payloads/create/billing/periodicBilling';
import { reversal } from "../../payloads/create/billing/reversal";
import { invoiceCorrection } from "../../payloads/create/billing/correction";
import { loadResponsesFromFile, updateFileWithInvoiceIds } from "../../../fixtures/utils/stashResponses";
import { invoiceCancellation } from "../../payloads/create/billing/invoiceCancelation";

import {expect} from '../../../fixtures/baseFixture';

type billingApplicationLevels = 'CUSTOMER' | 'CONTRACT' | 'POD';
type applicationModelType = ('FOR_VOLUMES' | 'OVER_TIME_PERIODICAL' | 'OVER_TIME_ONE_TIME' | 'PER_PIECE' | 'INTERIM_AND_ADVANCE_PAYMENT' | 'WITH_ELECTRICITY_INVOICE')[];

export class BillingPayloads {
    private Request: APIRequestContext;
    private responses: baseFixture['Responses'];

    constructor(apiRequestContext: APIRequestContext, responses: baseFixture['Responses']) {
        this.Request = apiRequestContext;
        this.responses = responses;
    }

    public async waitForInvoiceGeneration(startBilling: boolean = true, completeBillingRun: boolean = true, numberOfInvoices: number = 1, billingIndex: number = 0) {
        if (!startBilling) {
            return
        }

        const rawBillingRun = this.responses.billingRun?.[billingIndex];
        
        // Handle both formats: {id: 123, invoiceNumbers: 4} or just 123
        let billingRunId: number;
        let expectedInvoices: number = numberOfInvoices;
        
        if (typeof rawBillingRun === 'object' && rawBillingRun !== null && 'id' in rawBillingRun) {
            billingRunId = (rawBillingRun as any).id;
            // Use invoiceNumbers from object if available, otherwise use parameter
            expectedInvoices = (rawBillingRun as any).invoiceNumbers ?? numberOfInvoices;
        } else {
            billingRunId = rawBillingRun;
        }

        if (!billingRunId) {
            throw new Error(`❌ billingRun ID not found in responses at index ${billingIndex}. Ensure billing run was created before calling this method.`);
        }
        
        const maxDuration = 7 * 60 * 1000; // 7 minutes in ms
        const interval = 15 * 1000; // 15 seconds
        const maxAttempts = Math.floor(maxDuration / interval);

        await this.Request.patch(`billing-run/start-billing?billingRunId=${billingRunId}`);

        for (let attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                const commonParams = await this.Request.get(`billing-run/${billingRunId}`);
                const commonParamsJson = await commonParams.json();
                const isDraft = commonParamsJson.commonParameters.status === 'DRAFT';
                
                if (!isDraft) {
                    if (attempt % 5 === 0 || attempt === 1) {
                        console.log(`⏳ Attempt ${attempt}/${maxAttempts}: Waiting for billing run to reach DRAFT status...`);
                    }
                    if (attempt < maxAttempts) {
                        await new Promise(resolve => setTimeout(resolve, interval));
                    }
                    continue;
                }


                const draftJsonRes = await this.Request.get(`billing-run/draft-invoices?id=${billingRunId}&page=0&size=25`);
                const draftJson = await draftJsonRes.json();
                const invoiceCount = draftJson?.content?.length || 0;

                const getBillingRun = await this.Request.get(`billing-run/${billingRunId}`);
                const billingRunJson = await getBillingRun.json();

                if (invoiceCount === expectedInvoices) {
                    console.log(`✅ Found ${invoiceCount} invoice(s) at attempt ${attempt}`);
                    if (completeBillingRun) {
                        await this.Request.patch(`billing-run/start-generating?billingRunId=${billingRunId}`);
                        
                        // Poll for GENERATED status
                        const generationMaxDuration = 2 * 60 * 1000; // 2 minutes for PDF generation
                        const generationInterval = 3000; // 3 seconds
                        const generationMaxAttempts = Math.floor(generationMaxDuration / generationInterval);
                        
                        let isGenerated = false;
                        for (let genAttempt = 1; genAttempt <= generationMaxAttempts; genAttempt++) {
                            const checkBillingRun = await this.Request.get(`billing-run/${billingRunId}`);
                            const checkBillingRunJson = await checkBillingRun.json();
                            
                            if (checkBillingRunJson.commonParameters.status === 'GENERATED') {
                                console.log(`✅ Billing run ${billingRunId} has been generated successfully at attempt ${genAttempt}.`);
                                isGenerated = true;
                                break;
                            }
                            
                            if (genAttempt % 5 === 0 || genAttempt === 1) {
                                console.log(`⏳ PDF Generation attempt ${genAttempt}/${generationMaxAttempts}: Status is ${checkBillingRunJson.commonParameters.status}, waiting for GENERATED...`);
                            }
                            
                            if (genAttempt < generationMaxAttempts) {
                                await new Promise(resolve => setTimeout(resolve, generationInterval));
                            }
                        }
                        
                        if (!isGenerated) {
                            const finalCheck = await this.Request.get(`billing-run/${billingRunId}`);
                            const finalCheckJson = await finalCheck.json();
                            throw new Error(`❌ PDF Generation failed - billing run status is ${finalCheckJson.commonParameters.status}, expected GENERATED after ${generationMaxAttempts} attempts`);
                        }

                        await this.Request.patch(`billing-run/start-accounting?billingRunId=${billingRunId}`);
                        
                        // Poll for COMPLETED status
                        const accountingMaxDuration = 2 * 60 * 1000; // 2 minutes for accounting
                        const accountingInterval = 3000; // 3 seconds
                        const accountingMaxAttempts = Math.floor(accountingMaxDuration / accountingInterval);
                        
                        let isCompleted = false;
                        for (let accAttempt = 1; accAttempt <= accountingMaxAttempts; accAttempt++) {
                            const checkBillingRun = await this.Request.get(`billing-run/${billingRunId}`);
                            const checkBillingRunJson = await checkBillingRun.json();
                            
                            if (checkBillingRunJson.commonParameters.status === 'COMPLETED') {
                                console.log(`✅ Billing run ${billingRunId} has been completed successfully at attempt ${accAttempt}.`);
                                isCompleted = true;
                                break;
                            }
                            
                            if (accAttempt % 5 === 0 || accAttempt === 1) {
                                console.log(`⏳ Accounting attempt ${accAttempt}/${accountingMaxAttempts}: Status is ${checkBillingRunJson.commonParameters.status}, waiting for COMPLETED...`);
                            }
                            
                            if (accAttempt < accountingMaxAttempts) {
                                await new Promise(resolve => setTimeout(resolve, accountingInterval));
                            }
                        }
                        
                        if (!isCompleted) {
                            const finalCheck = await this.Request.get(`billing-run/${billingRunId}`);
                            const finalCheckJson = await finalCheck.json();
                            throw new Error(`❌ Accounting failed - billing run status is ${finalCheckJson.commonParameters.status}, expected COMPLETED after ${accountingMaxAttempts} attempts`);
                        }
                    }
                    
                    for (let i = 0; i < draftJson.content.length; i++) {
                        this.responses.invoice.push(draftJson.content[i].id);
                    }
                    return;
                }

                
                if (attempt % 5 === 0 || attempt === 1) {
                    console.log(`⏳ Attempt ${attempt}/${maxAttempts}`);
                }

            } catch (err) {
                // Re-throw validation errors immediately (don't retry)
                if (err instanceof Error && err.message.includes('❌')) {
                    throw err;
                }
                
                // For other errors, only log on final attempt
                if (attempt === maxAttempts) {
                    console.error(`❌ Final attempt failed:`, err);
                    throw err;
                }
            }

            // Wait 15s before next attempt (except after the last one)
            if (attempt < maxAttempts) {
                await new Promise(resolve => setTimeout(resolve, interval));
            }
        }

        throw new Error("❌ Invoice not generated within 5 minutes");
    }

    public async waitForInvoiceGenerationParallel(
        fileName: string,
        completeBillingRun: boolean = true
    ) {
        // Load stashed responses file
        const testCasesData = loadResponsesFromFile(fileName);
        
        // Handle both old format (single object) and new format (array)
        const testCasesArray = Array.isArray(testCasesData) ? testCasesData : [testCasesData];
        
        // Extract all billing run IDs with their expected invoice counts from all test cases
        const allBillingRunIds: { id: number; testCaseIndex: number; invoiceNumbers: number }[] = [];
        
        for (let i = 0; i < testCasesArray.length; i++) {
            const testCase = testCasesArray[i];
            if (testCase.billingRun && Array.isArray(testCase.billingRun)) {
                for (const rawBillingRun of testCase.billingRun) {
                    let billingRunId: number;
                    let invoiceNumbers: number = 1; // Default to 1 invoice
                    
                    // Handle both formats: {id: 123, invoiceNumbers: 4} or just 123
                    if (typeof rawBillingRun === 'object' && rawBillingRun !== null && 'id' in rawBillingRun) {
                        billingRunId = (rawBillingRun as any).id;
                        invoiceNumbers = (rawBillingRun as any).invoiceNumbers ?? 1;
                    } else {
                        billingRunId = rawBillingRun;
                    }
                    
                    if (billingRunId) {
                        allBillingRunIds.push({ id: billingRunId, testCaseIndex: i, invoiceNumbers });
                    }
                }
            }
        }
        
        if (allBillingRunIds.length === 0) {
            throw new Error(`❌ No billing runs found in file ${fileName}`);
        }

        // Check statuses and filter out already-completed billing runs
        const statusChecks = await Promise.all(
            allBillingRunIds.map(async ({ id }) => {
                const res = await this.Request.get(`billing-run/${id}`);
                const json = await res.json();
                return json.commonParameters?.status as string;
            })
        );

        const toProcess = allBillingRunIds.filter((_, i) => statusChecks[i] !== 'COMPLETED');

        if (toProcess.length === 0) {
            console.log(`ℹ️ All billing runs in ${fileName} are already COMPLETED, skipping.`);
            return [];
        }

        console.log(`🚀 Starting ${toProcess.length} billing runs in parallel...`);
        
        // Start all billing runs in parallel
        await Promise.all(
            toProcess.map(({ id }) =>
                this.Request.patch(`billing-run/start-billing?billingRunId=${id}`)
            )
        );
        
        console.log(`✅ All ${toProcess.length} billing runs started`);
        
        // Wait for all billing runs to complete in parallel (don't fail fast)
        const results = await Promise.allSettled(
            toProcess.map(({ id, testCaseIndex, invoiceNumbers }) =>
                this.waitForSingleBillingRun(id, testCaseIndex, completeBillingRun, invoiceNumbers)
            )
        );
        
        // Separate successful and failed billing runs
        const successful: any[] = [];
        const failed: { testCaseIndex: number; billingRunId: number; error: string }[] = [];
        
        results.forEach((result, index) => {
            if (result.status === 'fulfilled') {
                successful.push(result.value);
            } else {
                const { id, testCaseIndex } = toProcess[index];
                failed.push({
                    testCaseIndex,
                    billingRunId: id,
                    error: result.reason?.message || 'Unknown error'
                });
            }
        });
        
        // Log summary
        console.log(`\n${'='.repeat(80)}`);
        console.log(`📊 Billing Run Summary: ${successful.length} successful, ${failed.length} failed out of ${toProcess.length} total`);
        console.log(`${'='.repeat(80)}`);
        
        if (successful.length > 0) {
            console.log(`\n✅ Successful billing runs (${successful.length}):`);
            successful.forEach((result) => {
                console.log(`   [Test Case ${result.testCaseIndex}] Billing run ${result.billingRunId} - ${result.invoiceIds.length} invoice(s) generated (expected: ${result.expectedInvoices})`);
            });

            // Persist invoice IDs back to the stashed file so subsequent tests can use them
            const invoicesByTestCase = new Map<number, number[]>();
            for (const result of successful) {
                if (!invoicesByTestCase.has(result.testCaseIndex)) {
                    invoicesByTestCase.set(result.testCaseIndex, []);
                }
                invoicesByTestCase.get(result.testCaseIndex)!.push(...result.invoiceIds);
            }
            updateFileWithInvoiceIds(fileName, invoicesByTestCase);
        }
        
        if (failed.length > 0) {
            console.log(`\n❌ Failed billing runs (${failed.length}):`);
            failed.forEach((failure) => {
                console.log(`   [Test Case ${failure.testCaseIndex}] Billing run ${failure.billingRunId} - ${failure.error}`);
            });
            console.log(`${'='.repeat(80)}\n`);
            
            // Throw error with comprehensive failure information
            const failedIds = failed.map(f => `[Test Case ${f.testCaseIndex}] Billing run ${f.billingRunId}`).join(', ');
            throw new Error(`❌ ${failed.length}/${toProcess.length} billing runs failed: ${failedIds}`);
        }
        
        console.log(`${'='.repeat(80)}\n`);
        
        return successful;
    }
    
    private async waitForSingleBillingRun(
        billingRunId: number,
        testCaseIndex: number,
        completeBillingRun: boolean,
        numberOfInvoices: number
    ) {
        const maxDuration = 7 * 60 * 1000; // 7 minutes in ms
        const interval = 15 * 1000; // 15 seconds
        const maxAttempts = Math.floor(maxDuration / interval);
        
        for (let attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                const commonParams = await this.Request.get(`billing-run/${billingRunId}`);
                const commonParamsJson = await commonParams.json();
                const isDraft = commonParamsJson.commonParameters.status === 'DRAFT';
                
                if (!isDraft) {
                    if (attempt % 5 === 0 || attempt === 1) {
                        console.log(`⏳ [Test Case ${testCaseIndex}] Billing run ${billingRunId} - Attempt ${attempt}/${maxAttempts}: Waiting for billing run to reach DRAFT status...`);
                    }
                    if (attempt < maxAttempts) {
                        await new Promise(resolve => setTimeout(resolve, interval));
                    }
                    continue;
                }
                
                const draftJsonRes = await this.Request.get(`billing-run/draft-invoices?id=${billingRunId}&page=0&size=25`);
                const draftJson = await draftJsonRes.json();
                const invoiceCount = draftJson?.content?.length || 0;
                
                if (invoiceCount === numberOfInvoices) {
                    console.log(`✅ [Test Case ${testCaseIndex}] Billing run ${billingRunId} - Found ${invoiceCount} invoice(s) at attempt ${attempt}`);
                    
                    if (completeBillingRun) {
                        // Start PDF generation
                        await this.Request.patch(`billing-run/start-generating?billingRunId=${billingRunId}`);
                        
                        // Poll for GENERATED status
                        const generationMaxDuration = 2 * 60 * 1000;
                        const generationInterval = 3000;
                        const generationMaxAttempts = Math.floor(generationMaxDuration / generationInterval);
                        
                        let isGenerated = false;
                        for (let genAttempt = 1; genAttempt <= generationMaxAttempts; genAttempt++) {
                            const checkBillingRun = await this.Request.get(`billing-run/${billingRunId}`);
                            const checkBillingRunJson = await checkBillingRun.json();
                            
                            if (checkBillingRunJson.commonParameters.status === 'GENERATED') {
                                console.log(`✅ [Test Case ${testCaseIndex}] Billing run ${billingRunId} - Generated at attempt ${genAttempt}`);
                                isGenerated = true;
                                break;
                            }
                            
                            if (genAttempt % 5 === 0 || genAttempt === 1) {
                                console.log(`⏳ [Test Case ${testCaseIndex}] Billing run ${billingRunId} - PDF Generation attempt ${genAttempt}/${generationMaxAttempts}: Status is ${checkBillingRunJson.commonParameters.status}, waiting for GENERATED...`);
                            }
                            
                            if (genAttempt < generationMaxAttempts) {
                                await new Promise(resolve => setTimeout(resolve, generationInterval));
                            }
                        }
                        
                        if (!isGenerated) {
                            const finalCheck = await this.Request.get(`billing-run/${billingRunId}`);
                            const finalCheckJson = await finalCheck.json();
                            throw new Error(`❌ [Test Case ${testCaseIndex}] PDF Generation failed - billing run ${billingRunId} status is ${finalCheckJson.commonParameters.status}`);
                        }
                        
                        // Start accounting
                        await this.Request.patch(`billing-run/start-accounting?billingRunId=${billingRunId}`);
                        
                        // Poll for COMPLETED status
                        const accountingMaxDuration = 2 * 60 * 1000;
                        const accountingInterval = 3000;
                        const accountingMaxAttempts = Math.floor(accountingMaxDuration / accountingInterval);
                        
                        let isCompleted = false;
                        for (let accAttempt = 1; accAttempt <= accountingMaxAttempts; accAttempt++) {
                            const checkBillingRun = await this.Request.get(`billing-run/${billingRunId}`);
                            const checkBillingRunJson = await checkBillingRun.json();
                            
                            if (checkBillingRunJson.commonParameters.status === 'COMPLETED') {
                                console.log(`✅ [Test Case ${testCaseIndex}] Billing run ${billingRunId} - Completed at attempt ${accAttempt}`);
                                isCompleted = true;
                                break;
                            }
                            
                            if (accAttempt % 5 === 0 || accAttempt === 1) {
                                console.log(`⏳ [Test Case ${testCaseIndex}] Billing run ${billingRunId} - Accounting attempt ${accAttempt}/${accountingMaxAttempts}: Status is ${checkBillingRunJson.commonParameters.status}, waiting for COMPLETED...`);
                            }
                            
                            if (accAttempt < accountingMaxAttempts) {
                                await new Promise(resolve => setTimeout(resolve, accountingInterval));
                            }
                        }
                        
                        if (!isCompleted) {
                            const finalCheck = await this.Request.get(`billing-run/${billingRunId}`);
                            const finalCheckJson = await finalCheck.json();
                            throw new Error(`❌ [Test Case ${testCaseIndex}] Accounting failed - billing run ${billingRunId} status is ${finalCheckJson.commonParameters.status}`);
                        }
                    }
                    
                    // Collect invoice IDs
                    const invoiceIds = draftJson.content.map((invoice: any) => invoice.id);
                    
                    return {
                        billingRunId,
                        testCaseIndex,
                        invoiceIds,
                        expectedInvoices: numberOfInvoices,
                        status: 'success'
                    };
                }
                
            } catch (err) {
                if (err instanceof Error && err.message.includes('❌')) {
                    throw err;
                }
                
                if (attempt === maxAttempts) {
                    console.error(`❌ [Test Case ${testCaseIndex}] Billing run ${billingRunId} - Final attempt failed:`, err);
                    throw err;
                }
            }
            
            if (attempt < maxAttempts) {
                await new Promise(resolve => setTimeout(resolve, interval));
            }
        }
        
        throw new Error(`❌ [Test Case ${testCaseIndex}] Billing run ${billingRunId} - Invoice not generated within 7 minutes`);
    }
    
    public async billingRun(applicationLevel: billingApplicationLevels = 'CONTRACT', applicationModelType?: applicationModelType, customerIndex: number = 0, contractIndex: number = 0, podIndex: number = 0) {
        const payload = forVolumes();
        const finalModelType = applicationModelType || ['FOR_VOLUMES'];
        payload.basicParameters.applicationModelType = finalModelType;

        if(finalModelType.includes('FOR_VOLUMES')) {
            payload.basicParameters.maxEndDate = '2060-12-31'
        }

        if (applicationLevel === 'CUSTOMER') {
            payload.basicParameters.billingApplicationLevel = 'CUSTOMER'
            payload.basicParameters.listOfCustomersContractsOrPOD = this.responses.customer?.[customerIndex].identifier

        }else if (applicationLevel === 'CONTRACT') {
            let contractget;

            if(this.responses.productContract.length > 0){
                contractget = await this.Request.get(`product-contract/${this.responses.productContract[contractIndex].id}?version=1`);
            }else{
                contractget = await this.Request.get(`service-contract/${this.responses.serviceContract[contractIndex]}?version=1`);
            }
            const contractjson = await contractget.json();
            payload.basicParameters.listOfCustomersContractsOrPOD = await contractjson.basicParameters.contractNumber;

        }else if (applicationLevel === 'POD') {
            payload.basicParameters.billingApplicationLevel = 'POD'
            const podget = await this.Request.get(`pod/${this.responses.pod[podIndex].id}?version=1`);
            const podjson = await podget.json();
            payload.basicParameters.listOfCustomersContractsOrPOD = await podjson.identifier;
        }

        return payload;
    }

    public async manualInvoice() {
        const payload = manualInvoice();

        const getCustomer = await this.Request.get(`customer/${this.responses.customer[0].id}?version=1`);
        const getCustomerResponse = await getCustomer.json();

        payload.manualInvoiceParameters.manualInvoiceBasicDataParameters.customerDetailId = getCustomerResponse.customerDetailsId;
        payload.manualInvoiceParameters.manualInvoiceBasicDataParameters.invoiceCommunicationDataId = getCustomerResponse.communicationData[0].id;

        if(this.responses.productContract.length > 0){
            const contractget = await this.Request.get(`product-contract/${this.responses.productContract[0].id}?version=1`);
            const contractjson = await contractget.json();
            const billingGroup = contractjson.contractPodsResponses[0].billingGroupId;

            payload.manualInvoiceParameters.manualInvoiceBasicDataParameters.billingGroupId = billingGroup
            payload.manualInvoiceParameters.manualInvoiceBasicDataParameters.contractOrderId = contractjson.basicParameters.id;
            payload.manualInvoiceParameters.manualInvoiceBasicDataParameters.prefixType = null;
            payload.manualInvoiceParameters.manualInvoiceBasicDataParameters.contractOrderType = 'PRODUCT_CONTRACT';
        }

        return payload;
    }

    public debitNote() {
        const payload = debitNote();
        return payload;
    }

    public creditNote() {
        const payload = creditNote();
        return payload;
    }
    
    public Manualinterim() {
        const payload = Manualinterim();
        return payload;
    }

    public periodicBilling() {
        const payload = periodicBilling();
        return payload;
    }

    public async reversalBilling(invoiceIndex: number = 0) {
        const payload = reversal();
        const invoiceGet = await this.Request.get(`invoice?id=${this.responses.invoice[invoiceIndex]}`);
        const invoiceNumber = (await invoiceGet.json()).invoiceNumber
        payload.invoiceReversalParameters.listOfInvoices = `${invoiceNumber}`
        return payload;
    }

    public async correctionBilling(invoiceIndex: number = 0, priceChange: boolean = false, volumeChange: boolean = false) {
        const payload = invoiceCorrection();
        if(priceChange){
            payload.invoiceCorrectionParameters.priceChange = true;
        }

        if(volumeChange){
            payload.invoiceCorrectionParameters.volumeChange = true;
        }
        
        const invoiceGet = await this.Request.get(`invoice?id=${this.responses.invoice[invoiceIndex]}`);
        const invoiceNumber = (await invoiceGet.json()).invoiceNumber
        payload.invoiceCorrectionParameters.listOfInvoices = `${invoiceNumber}`
        return payload;
    }

    public async generateConditions() {
        const payload = await this.billingRun();
        const product = this.responses.product[0];

        const getproduct = await this.Request.get(`products/${product}`);
        const productjson = await getproduct.json();
        const productType = productjson.productType.id;

        const getCustomer = await this.Request.get(`customer/${this.responses.customer[0].id}?version=1`);
        const customerjson = await getCustomer.json();
        const customerType = customerjson.customerType;
        const customerSegment = customerjson.customerSegments[0].segment.id;
        const customerNumber = customerjson.customerNumber;

        const pod = await this.Request.get(`pod/${this.responses.pod[0].id}?version=1`);
        const podjson = await pod.json();

        const podGridOp = podjson.gridOperatorId;
        const podType = podjson.type;
        const podMeasurementType = podjson.measurementType;
        const podVoltageLevel = podjson.voltageLevel;
        const podConsumptionPurpose = podjson.consumptionPurpose;

        const contractGet = await this.Request.get(`product-contract/${this.responses.productContract[0].id}?version=1`);
        const contractjson = await contractGet.json();
        const contractType = contractjson.productParameters.contractType;

        const condition = `$CONTRACT$=$PRODUCT_CONTRACT$AND$PRODUCT$=$${product}$AND$PRODUCT_TYPE$=$${productType}$AND$CUSTOMER_TYPE$=$${customerType}$AND$POD_GRID_OP$=$${podGridOp}$AND$POD_TYPE$=$${podType}$AND$CONTRACT_TYPE$=$${contractType}$AND$POD_MEASUREMENT_TYPE$=$${podMeasurementType}$AND$VOLTAGE_LEVEL$=$${podVoltageLevel}$AND$PURPOSE_OF_CONSUMPTION$=$${podConsumptionPurpose}$AND$INTERIM_ADVANCE_PAYMENT$=$NO$AND$CUSTOMER_SEGMENT$=$${customerSegment}$AND$CUSTOMER_NUMBER$=$${customerNumber}$`;
        return condition;
    }

    public async invoiceCancellation() {
        const customerGetResponse = await this.Request.get(`customer/${this.responses.customer[0].id}`);
        expect(customerGetResponse).CheckResponse()
        const customerGetData = await customerGetResponse.json();
        const payloadForGet = {
            "page": 0,
            "size": 25
        }
        const invoice = await this.Request.post(`customer/${customerGetData.lastCustomerDetailId}/customer-invoices`, {data: payloadForGet});
        expect(invoice).CheckResponse()
        const responseData = await invoice.json();
        const invoiceNumber = responseData.content[0].invoiceNumber.split('-')[1];
        this.responses.invoice.push(responseData.content[0].invoiceId);

        const payload = invoiceCancellation();
        payload.invoices = invoiceNumber;

        return payload
    }

    public async waitForInvoiceStatus(ID: string, status: string) {
        await expect.poll( async() => {
            const get = await this.Request.get(`invoice?id=${ID}`);
            expect(get).CheckResponse();
            const responseBody = await get.json();

            return responseBody.invoiceStatus
        }, 
        {
            message: `❌ Invoice status is not -> ${status}`,
            timeout: 60000,
            intervals: [2000, 5000]
        }
        ).toBe(status)
        console.log(`✅ Invoice status is correct, with status -> ${status}. object ID -> ${ID}`)
    }
}