import { APIRequestContext } from '@playwright/test';
import { baseFixture} from "../../../fixtures/baseFixture";
import { customer_liability } from '../../payloads/Receivables/customerLiability';
import { customer_receivable } from '../../payloads/Receivables/customerReceivable';
import { deposit } from '../../payloads/Receivables/deposit';
import { deposit_max } from '../../payloads/Receivables/depositMax';
import { payment_package } from '../../payloads/Receivables/paymentPackage';
import { payment } from '../../payloads/Receivables/payment';
import { bulgarianPostChannel, collection_channel, bank_partner_collection_channel } from '../../payloads/Receivables/collectionChannel';
import { reminder } from '../../payloads/Receivables/Reminder';
import { mass_operation_for_blocking } from '../../payloads/Receivables/massOperationForBlocking';
import { MLO_D_L, MLO_NP_R, MLO_L_R } from '../../payloads/Receivables/mloCreate';
import { interestRate } from '../../payloads/Receivables/interestRate';
import { customer_assessment } from "../../payloads/Receivables/customerAssessment";
import { paymentReverse } from '../../payloads/Receivables/paymentReverse'
import { reminderForDisconnection } from '../../payloads/Receivables/reminderForDisconnection';
import { requestForDisconnection } from '../../payloads/Receivables/requestForDisconnection';
import { disconnectionOfPowerSupply } from '../../payloads/Receivables/disconnectionOfPowerSupply';
import {reconnectionOfPowerSupply} from '../../payloads/Receivables/reconnectionOfPowerSupply'
import {easyPayCollectionChannel} from '../../payloads/Receivables/collectionChannel'
import { CalculateRescheduling } from "../../payloads/Receivables/calculateRescheduling";
import { rescheduling } from "../../payloads/Receivables/rescheduling";
import {Cbgpayload} from '../../payloads/Receivables/cbg';
import { massImportGenerator } from "../../../mass-imports/generators/massImportGenerator";
import { envVariables } from '../../../fixtures/envCashed';
import { cancelationOfRequestForDisconnection } from "../../payloads/Receivables/cancelationOfRequestForDisconnection";

import {expect} from '../../../fixtures/baseFixture';
import { randomGens } from "../../../utils/randomGens";

export class ReceivablesManagementPayloads {
    private Request: APIRequestContext;
    private responses: baseFixture['Responses'];
    private fileUploadRequest: APIRequestContext;

    constructor(apiRequestContext: APIRequestContext, responses: baseFixture['Responses'], fileUploadRequest: APIRequestContext) {
        this.Request = apiRequestContext;
        this.responses = responses;
        this.fileUploadRequest = fileUploadRequest;
    }

    public customer_liability() {
        const payload = customer_liability();
        payload.customerId = this.responses.customer[0].id;

        return payload;
    }

    public overdueLiability() { /** Due date = Yesterday() */
        const payload = customer_liability();
        payload.customerId = this.responses.customer[0].id
        payload.initialAmount = 100
        payload.occurrenceDate = randomGens.generateYesterdaysDate("dd-mm-yyyy");
        payload.dueDate = randomGens.generateYesterdaysDate("dd-mm-yyyy");
        payload.applicableInterestRateId = this.responses.interestRate[0];

        return payload
    }

    public customer_receivable() {
        const payload = customer_receivable();
        payload.customerId = this.responses.customer[0].id;

        return payload;
    }

    public deposit() {
        const payload = deposit();
        payload.customerId = this.responses.customer[0].id;
        return payload;
    }

    public deposit_max() {
        const payload = deposit_max();
        return payload;
    }

    public async makeDepositReady() {
        const depositGet = await this.Request.get(`deposit/${this.responses.deposit[0]}`);
        expect(depositGet).CheckResponse();
        const depositAmount = (await depositGet.json() as { initialAmount: number }).initialAmount;
        const depostCurrentAmount = (await depositGet.json() as { currentAmount: number }).currentAmount;

        if(depostCurrentAmount === 0) {
        // find liability
            const depositLiability = await this.Request.get(`customer-liability/list?page=0&size=25&columns=ID&direction=DESC&prompt=${this.responses.customer[0].identifier}&searchFields=CUSTOMER&initialAmountFrom=${depositAmount}`);
            expect(depositLiability).CheckResponse();
            const data = await depositLiability.json();
            const liabilityId = data.content[0].id;
            //create receivable
            const payload = customer_receivable();
            payload.initialAmount = depositAmount;
            payload.customerId = this.responses.customer[0].id;
            const receivable = await this.Request.post(`customer-receivable`, {data: payload})
            expect(receivable).CheckResponse();
            const receivableId = await receivable.json();
            //get customer
            const customerGetResponse = await this.Request.get(`customer/${this.responses.customer[0].id}`);
            const customerGetData = await customerGetResponse.json();
            //config payload for MLO
            const MLO_payload = MLO_L_R();
            MLO_payload.liabilities[0].id = liabilityId;
            MLO_payload.liabilities[0].current_amount = depositAmount;
            MLO_payload.receivables[0].id = receivableId;
            MLO_payload.receivables[0].current_amount = depositAmount;
            MLO_payload.receivedLiabilities[0].id = liabilityId;
            MLO_payload.receivedReceivables[0].id = receivableId;
            MLO_payload.receivedOffsets[0].r_id = receivableId;
            MLO_payload.receivedOffsets[0].l_id = liabilityId;
            MLO_payload.receivedOffsets[0].offset_amount = depositAmount;
            MLO_payload.customerId = this.responses.customer[0].id;
            MLO_payload.customerDetailId = customerGetData.lastCustomerDetailId;
            MLO_payload.customerCommunicationDataId = customerGetData.communicationData[0].id;
            //post MLO
            const postMLO = await this.Request.post('manual-liability-offsetting', {data: MLO_payload});
            expect(postMLO).CheckResponse();
            //run deposit fulfill job
            const job = await expect.poll( async() => {
                const job1 = await this.Request.post("deposit/job");
                expect(job1).CheckResponse();
                return job1.status();
            },
            {
                timeout: 1000 * 60,
                intervals: [2000, 5000],
            }).toBe(204);
            //check deposit amount
            const getDeposit = await this.Request.get(`deposit/${this.responses.deposit[0]}`);
            await expect(getDeposit).CheckResponse();
            const dataAfterJob = await getDeposit.json();
            expect(dataAfterJob.currentAmount).toEqual(depositAmount);

            return console.log(`✅ Deposit is ready to use -> ${this.responses.deposit[0]}`);
        }
        else {
            return console.log(`✅ Deposit is already ready for offsetting -> ${this.responses.deposit[0]}`);
        }
    }

    public async payment(billingGroup: boolean = false, ourgoingDocument: boolean = false) {
        const payload = payment();
        if (billingGroup) {
            const productContractGet = await this.Request.get(`product-contract/${this.responses.productContract[0].id}`);
            const billingGroupId = (await productContractGet.json()).contractPodsResponses[0].billingGroupId;
            payload.contractBillingGroupId = billingGroupId;
        };
        if (ourgoingDocument){
            payload.invoiceId = this.responses.invoice[0];
            payload.outgoingDocumentType = 'INVOICE';
        }
        payload.paymentPackageId = this.responses.paymentPackage[0];
        payload.customerId = this.responses.customer[0].id;
        payload.collectionChannelId = this.responses.collectionChannel[0];
        return payload;
    }

    public paymentReverse() {
        const payload = paymentReverse();
        payload.customerId = this.responses.customer[1].id;
        payload.paymentId = this.responses.payment[0];
        return payload;
    }

    public payment_package() {
        const payload = payment_package();
        payload.channelId = this.responses.collectionChannel[0];

        return payload;
    }

    public collection_channel() {
        const payload = collection_channel();
        return payload;
    }

    public bank_partner_collection_channel() {
        const payload = bank_partner_collection_channel();
        return payload;
    }

    /** function helps to modify channel combine liabilities value without violation data */
    public async modifyChannel(channelId: number, combineLiabilityValue: boolean,): Promise<void> {
        const channelGet = await this.Request.get(`collection-channel/${channelId}`);
        expect(channelGet).CheckResponse();
        const responseBody = await channelGet.json();

        if (responseBody.combineLiabilities !== combineLiabilityValue) {
            const excludePrefixIds = (responseBody.excludeLiabilitiesByPrefix ?? []).map((item: { id: number }) => item.id);
            const priorityPrefixIds = (responseBody.priorityLiabilitiesByPrefix ?? []).map((item: { id: number }) => item.id);
            const bankIds = (responseBody.bankIds ?? []).map((item: { id: number }) => item.id);

            const payload = {
                name: responseBody.name,
                type: responseBody.type,
                performerId: responseBody.employee?.id ?? null,
                performerType: responseBody.employee?.performerType ?? null,
                collectionPartnerId: responseBody.collectionPartnerId?.id ?? responseBody.collectionPartnerId,
                numberOfIncomeAccount: String(responseBody.numberOfIncomeAccount),
                currencyId: responseBody.currencyId?.id ?? responseBody.currencyId,
                customerConditionType: responseBody.conditionType ?? responseBody.customerConditionType,
                condition: responseBody.condition ?? null,
                listOfCustomers: responseBody.listOfCustomers ?? null,
                excludeLiabilitiesByPrefix: excludePrefixIds,
                excludeLiabilitiesByAmount: {
                    lessThan: responseBody.lessThan ?? null,
                    greaterThan: responseBody.greaterThan ?? null,
                },
                priorityLiabilitiesByPrefix: priorityPrefixIds,
                typeOfFile: responseBody.typeOfFile ?? null,
                bankIds: bankIds,
                globalBank: responseBody.isGlobalBank ?? responseBody.globalBank ?? false,
                dataSendingSchedule: responseBody.dataSendingSchedule ?? null,
                dataReceivingSchedule: responseBody.dataReceivingSchedule ?? null,
                numberOfWorkingDays: responseBody.numberOfWorkingDays ?? null,
                calendarId: responseBody.calendarId?.id ?? responseBody.calendarId ?? null,
                waitingPeriodToleranceInHours: responseBody.waitingPeriodToleranceInHours ?? null,
                folderForFileReceiving: responseBody.folderForFileReceiving ?? null,
                folderForFileSending: responseBody.folderForFileSending ?? null,
                emailForFileSending: responseBody.emailForFileSending ?? null,
                combineLiabilities: combineLiabilityValue,
            };

            const changeValue = await this.Request.put(`collection-channel/${channelId}`, {data: payload});
            expect(changeValue).CheckResponse();
        }
    }

    public reminder() {
        const payload = reminder();

        // listOfCustomers should be a comma-separated string of customer identifiers (personal numbers)
        const customerIdentifiers = this.responses.customer.map(c => c.identifier).join(',');
        payload.listOfCustomers = customerIdentifiers;

        for (const i of this.responses.processPeriodicity) {
            // Handle both cases: processPeriodicity can be just an ID (number) or full object
            const periodicityId = typeof i === 'number' ? i : i.id;
            payload.periodicityIds.push(periodicityId);
        }

        return payload;
    }

    public reminderForDisconnection() {
        const payload = reminderForDisconnection();

        // listOfCustomers should be a comma-separated string of customer identifiers (personal numbers)
        const customerIdentifiers = this.responses.customer.map(c => c.identifier).join(',');
        payload.listOfCustomer = customerIdentifiers;

        // if(configuredBaseURL === 'https://devapps.energo-pro.bg/backend/phoenix2-dev') {
            
        // }
        
        return payload;
    }

    public async makeLiabilityOverdue() {
        let invoiceResponseBody = null
        let liabilityId = null

        if(this.responses.invoice.length > 0) {
            const invoiceGet = await this.Request.get(`invoice?id=${this.responses.invoice[0]}`);
            expect(invoiceGet).CheckResponse();
            invoiceResponseBody = await invoiceGet.json();

            if (invoiceResponseBody.liabilitiesAndReceivables.length > 0) {
                liabilityId = invoiceResponseBody.liabilitiesAndReceivables[0].id;
            }
        }

        const liabilities = await this.Request.get(`customer-liability/list?page=0&size=1&columns=ID&direction=DESC&prompt=${this.responses.customer[0].identifier}&searchFields=CUSTOMER`)
        liabilityId = (await liabilities.json()).content[0].id;
        expect(liabilityId).toBeDefined();

        const dueDateChange = await this.Request.put(`customer-liability/${liabilityId}/due-date-change?dueDate=${randomGens.generateYesterdaysDate('yyyy-mm-dd')}`);
        await expect(dueDateChange).CheckResponse();
    }

    public async offsetReminderForDisconnectionTime () {
        // Dev stores LocalDateTime.now().withHour(hour - 4) on a UTC clock.
        // Bulgarian local hour (UTC+3) therefore becomes exactly one hour ago, and the job
        // query is customerSendDate > now-1h, so that boundary is never selected.
        // Pass Tbilisi time (UTC+4) from 30 minutes ago: hour-4 is then 30 minutes ago,
        // inside (now-1h, now], and the next job run executes the reminder.
        const { hours, minutes } = this.getReminderJobSendTime();

        const reminderId = typeof this.responses.reminderForDisconnection[0] === 'number'
            ? this.responses.reminderForDisconnection[0]
            : this.responses.reminderForDisconnection[0].id;

        const invoiceGet = await this.Request.get(`invoice?id=${this.responses.invoice[0]}`);
        expect(invoiceGet).CheckResponse();
        const responseBody = await invoiceGet.json();

        let liabilityId = null

        if (responseBody.liabilitiesAndReceivables.length > 0) {
            liabilityId = responseBody.liabilitiesAndReceivables[0].id;
        }else{
            const liabilities = await this.Request.get(`customer-liability/list?page=0&size=1&columns=ID&direction=DESC&prompt=${this.responses.customer[0].identifier}&searchFields=CUSTOMER`)
            liabilityId = (await liabilities.json()).content[0].id;
        }
        expect(liabilityId).toBeDefined();

        const dueDateChange = await this.Request.put(`customer-liability/${liabilityId}/due-date-change?dueDate=${randomGens.generateYesterdaysDate('yyyy-mm-dd')}`);
        await expect(dueDateChange).CheckResponse();

        const timeOffset = await this.Request.put(`power-supply-disconnection-reminder/set-customer-send-time?reminderId=${reminderId}&hour=${hours}&minute=${minutes}`);
        await expect(timeOffset).CheckResponse();

        await this.Request.post('power-supply-disconnection-reminder/job');

        const maxDuration = 2 * 60 * 1000; // 5 minutes in ms
        const interval = 5 * 1000; // 5 seconds
        const maxAttempts = Math.floor(maxDuration / interval);

        for (let attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                const reminderForDisc = await this.Request.get(`power-supply-disconnection-reminder/${reminderId}`);
                const reminderfordiscJson = await reminderForDisc.json();
                
                if (reminderfordiscJson.reminderStatus !== 'EXECUTED') {
                    if (attempt % 5 === 0 || attempt === 1) {
                        console.log(`⏳ Attempt ${attempt}/${maxAttempts}: Waiting for reminder for disconnection to become executed...`);
                    }
                    // Wait and retry
                    if (attempt < maxAttempts) {
                        await new Promise(resolve => setTimeout(resolve, interval));
                    }
                    continue;
                }
                
                // Status is EXECUTED - success!
                console.log(`✅ Reminder for disconnection status is EXECUTED at attempt ${attempt}`);
                return;

            } catch (err) {
                console.error(`❌ Error checking reminder status at attempt ${attempt}:`, err);
                if (attempt === maxAttempts) {
                    throw new Error(`Failed to verify reminder status after ${maxAttempts} attempts: ${err}`);
                }
                // Wait before retrying after error
                await new Promise(resolve => setTimeout(resolve, interval));
            }
        }
        
        throw new Error("❌ Reminder for disconnection did not reach EXECUTED status within 2 minutes");
    }

    /** Returns the current local time in Tbilisi (Georgia, UTC+4). Use this when running locally. */
    public getGeorgianLocalTime(): { hours: number; minutes: number } {
        return this.zonedHourMinute('Asia/Tbilisi', new Date());
    }

    /**
     * Hour/minute for PUT /power-supply-disconnection-reminder/set-customer-send-time.
     * The API writes UTC now.withHour(hour - 4). Asia/Tbilisi is UTC+4, so this hour survives
     * that subtraction. Thirty minutes ago stays inside the job's open one-hour window.
     */
    public getReminderJobSendTime(): { hours: number; minutes: number } {
        return this.zonedHourMinute('Asia/Tbilisi', new Date(Date.now() - 30 * 60 * 1000));
    }

    private zonedHourMinute(timeZone: string, date: Date): { hours: number; minutes: number } {
        const parts = new Intl.DateTimeFormat('en-GB', {
            timeZone,
            hour: '2-digit',
            minute: '2-digit',
            hourCycle: 'h23',
        }).formatToParts(date);
        const hours = Number(parts.find((part) => part.type === 'hour')?.value);
        const minutes = Number(parts.find((part) => part.type === 'minute')?.value);
        return { hours, minutes };
    }

    /** Returns the current local time in Sofia (Bulgaria, EET/EEST). Use this for regression runs against the Bulgaria-hosted server. */
    public getBulgarianLocalTime(): { hours: number; minutes: number } {
        const now = new Date();
        const bulgarianTime = new Date(now.toLocaleString('en-US', { timeZone: 'Europe/Sofia' }));
        return {
            hours: bulgarianTime.getHours(),
            minutes: bulgarianTime.getMinutes(),
        };
    }

    public requestForDisconnection() {
        const payload = requestForDisconnection();
        payload.reminderForDisconnectionId = this.responses.reminderForDisconnection[0];
        return payload;
    }

    public cancellationOfRequestForDisconnection() {
        const payload = cancelationOfRequestForDisconnection();
        payload.table[0].requestForDisconnectionOfPowerSupplyId = this.responses.requestForDisconnection[0];
        payload.requestForDisconnectionOfThePowerSupplyId = this.responses.requestForDisconnection[0];
        payload.table[0].customerId = this.responses.customer[0].id;
        payload.table[0].podId = this.responses.pod[0].id;
        
      return payload;
    }

    public disconnectionOfPowerSupply() {
        const payload = disconnectionOfPowerSupply();
        payload.requestForDisconnectionId = this.responses.requestForDisconnection[0];
        payload.disconnectedRequest[0].customerId = this.responses.customer[0].id;
        payload.disconnectedRequest[0].podId = this.responses.pod[0].id;

        return payload;
    }

    public async changeTaxForGridOperator(newTaxForReconnection: number, newTaxForExpressReconnection: number) {
        const getTaxNomenclature = await this.Request.get(`tax-for-the-grid-operator/${envVariables.taxes_for_grid_operator}`);
        await expect(getTaxNomenclature).CheckResponse();
        
        const taxData = await getTaxNomenclature.json();
        
        const payload = {
            disconnectionType: taxData.disconnectionType,
            gridOperator: taxData.gridOperator,
            documentTemplateId: taxData.documentTemplateResponse.id,
            emailTemplateId: taxData.emailTemplateResponse.id,
            numberOfIncomeAccount: taxData.numberOfIncomeAccount,
            basisForIssuing: taxData.basisForIssuing,
            supplierType: taxData.supplierType,
            costCenterControllingOrder: taxData.costCenterControllingOrder,
            priceComponentOrPriceComponentGroupOrItem: taxData.priceComponentOrPriceComponentGroupOrItem,
            taxForReconnection: newTaxForReconnection,
            taxForExpressReconnection: newTaxForExpressReconnection,
            currency: taxData.currency,
            removeTaxInCancel: taxData.removeTaxInCancel,
            defaultForPodWithMeasurementTypeSlp: taxData.defaultForPodWithMeasurementTypeSlp,
            defaultForPodWithMeasurementTypeBySettlementPeriod: taxData.defaultForPodWithMeasurementTypeBySettlementPeriod,
            inactiveCheckbox: taxData.status === 'INACTIVE',
            status: taxData.status,
            defaultSelection: taxData.defaultSelection
        };
        
        const putResponse = await this.Request.put(`tax-for-the-grid-operator/${envVariables.taxes_for_grid_operator}`, { data: payload });
        await expect(putResponse).CheckResponse();
        
        return payload;
    }

    public async reconnectionOfPowerSupply() {

        const request = await this.Request.get(`reconnection-of-the-power-supply/table?gridOperatorId=${envVariables.grid_operator}&page=0&pageSize=100&searchBy=CUSTOMER_IDENTIFIER`);
        await expect(request).CheckResponse();
        const responseJson = await request.json();
        const preselectedObjects = responseJson.content.filter((x:any) => x.checked === true && x.unableToUncheck === true);

        const payload = reconnectionOfPowerSupply()
        if (preselectedObjects.length > 0) {
            payload.table = preselectedObjects.map((object:any) => ({   
                customerId: object.customerId,
                podId: object.podId,
                requestForDisconnectionOfPowerSupplyId: object.requestForDisconnectionId,
                cancellationReasonId: envVariables.reason_for_cancellation
            }));
        } else {
            payload.table[0].customerId = this.responses.customer[0].id;
            payload.table[0].podId = this.responses.pod[0].id;
            payload.table[0].requestForDisconnectionOfPowerSupplyId = this.responses.requestForDisconnection[0];
        }
        
        return payload
    }

    public interest_rate() {
        const payload = interestRate();
        return payload;
    }

    public dailyInterestRate() { /** For LPF test cases, easy to use */
        const payload = interestRate();
        payload.type = "DAILY";
        payload.interestRatePeriods[0].amountInPercent = 50;
        payload.interestRatePeriods[0].applicableInterestRate = 0.01;

        return payload
    }

    public latePaymentFine() {
        // This would be implemented when the corresponding payload is available
        throw new Error("Late payment fine payload not yet implemented");
    }

    public async waitForLPFGeneration(withoutJob: boolean) { /** This would check LPFs, based on customer, reserved in responses during test run */
        let responseBody: any;
        if(withoutJob == false) {
            await expect.poll( async() => {
                    const lpfJob = await this.Request.post(`latePaymentFine/job`, {timeout: 2 * 1000 * 60});
                    const responseBody = await lpfJob.json();
                    return responseBody;
                },
                {
                    message: `Failed to trigger LPF generation job or took more than 2 minutes`,
                    timeout: 2 * 1000 * 60,
                    intervals: [2000, 5000],
                }).toBe(1);

            await expect.poll( async() => {
                    const LPFs = await this.Request.get(`latePaymentFine/list?page=0&size=25&prompt=${this.responses.customer[0].identifier}&type=LATE_PAYMENT_FINE&searchBy=CUSTOMER`);
                    expect(LPFs).CheckResponse();
                    responseBody = await LPFs.json();
                    const totalElements = responseBody.totalElements;
                    return totalElements
                },
                {
                    message: `Failed to generate LPFs`,
                    timeout: 20_000,
                    intervals: [1000], 
                }   
            ).toBeGreaterThan(0)

            return responseBody 
        } else {
            await expect.poll( async() => {
                    const LPFs = await this.Request.get(`latePaymentFine/list?page=0&size=25&prompt=${this.responses.customer[0].identifier}&type=LATE_PAYMENT_FINE&searchBy=CUSTOMER`);
                    expect(LPFs).CheckResponse();
                    responseBody = await LPFs.json();
                    const totalElements = responseBody.totalElements;
                    return totalElements
                },
                {
                    message: `Failed to generate LPFs from EsayPay`,
                    timeout: 20_000,
                    intervals: [1000], 
                }   
            ).toBeGreaterThan(0)

            return responseBody 
        }
    }

    public defaultInterestCalculation() {
        // This would be implemented when the corresponding payload is available
        throw new Error("Default interest calculation payload not yet implemented");
    }

    public mass_operation_for_blocking() {
        const payload = mass_operation_for_blocking();
        payload.listOfCustomers = this.responses.customer[0].identifier;

        return payload;
    }

    public customer_assessment() {
        const payload = customer_assessment();
        payload.customerId = this.responses.customer[0].id;
        
        return payload;
    }

    public reschedulingCalculation(
        installmentCount: string, 
        reschedulingInterestType: "INTEREST_WITH_THE_FIRST_INSTALLMENT"|"INTEREST_WITH_LAST_INSTALLMENT"|"FIRST_INSTALLMENT_INTEREST_ONLY"|"INTEREST_WITH_EVERY_INSTALLMENT") {
        const payload = CalculateRescheduling();
        payload.reschedulingInterestType = reschedulingInterestType;
        payload.installmentCount = installmentCount;
        payload.liabilityIds = this.responses.customerLiability[0];

        return payload
    }

    public async rescheduling(
        installmentCount: string, 
        reschedulingInterestType: "INTEREST_WITH_THE_FIRST_INSTALLMENT"|"INTEREST_WITH_LAST_INSTALLMENT"|"FIRST_INSTALLMENT_INTEREST_ONLY"|"INTEREST_WITH_EVERY_INSTALLMENT"
    ) 
        {
        const calculationPayload = CalculateRescheduling();
        calculationPayload.reschedulingInterestType = reschedulingInterestType;
        calculationPayload.installmentCount = installmentCount;
        calculationPayload.liabilityIds[0] = this.responses.customerLiability[0];

        const calculation = await this.Request.post("rescheduling/calculate-rescheduling",{data: calculationPayload});
        await expect(calculation).CheckResponse();
        console.log(`✅ calculation has been successful`)
        const calcResponseBody = await calculation.json();

        const customerGet = await this.Request.get(`customer/${this.responses.customer[0].id}?`);
        await expect(customerGet).CheckResponse();
        const responseBodyCustomer = await customerGet.json();

        const customerId = responseBodyCustomer.customerId;
        const customerDetailId = responseBodyCustomer.lastCustomerDetailId;
        const customerCommunicationDataId = responseBodyCustomer.communicationData[0].id; /*can use in contract comm data as well*/
        

        const reschedulingPayload = rescheduling();

        reschedulingPayload.customerId = customerId;
        reschedulingPayload.customerDetailId = customerDetailId;
        reschedulingPayload.customerCommunicationDataId = customerCommunicationDataId;
        reschedulingPayload.customerCommunicationDataIdForContract = customerCommunicationDataId;

        reschedulingPayload.customerAssessmentId = this.responses.customerAssessment[0];
        reschedulingPayload.interestRateForInstalmentsId = this.responses.interestRate[0];
        reschedulingPayload.liabilityIdsForRescheduling[0] = this.responses.customerLiability[0];
        reschedulingPayload.reschedulingInterestType = reschedulingInterestType;
        reschedulingPayload.numberOfInstallment = installmentCount;

        reschedulingPayload.reschedulingLpfs[0].id = this.responses.customerLiability[0];
        reschedulingPayload.reschedulingLpfs[0].interest_default_currency = calcResponseBody.lpfs[0].interest_default_currency;
        reschedulingPayload.reschedulingLpfs[0].lpf_data.results = calcResponseBody.lpfs[0].lpf_data.results;
        reschedulingPayload.installments = calcResponseBody.installments;

        return reschedulingPayload
    }

    public objectionWithdrawal() {

    }

    public async MLO_D_L() {
        const payload = MLO_D_L()
        
        const customerGetResponse = await this.Request.get(`customer/${this.responses.customer[0].id}`);
        const customerGetData = await customerGetResponse.json();
        if (this.responses.customerLiability.length > 1) {
        
            // customerLiability[1] and deposit[0] might be just IDs or full objects
            const liabilityId = typeof this.responses.customerLiability[1] === 'number' 
                ? this.responses.customerLiability[1] 
                : this.responses.customerLiability[1].id;
            
            const depositId = typeof this.responses.deposit[0] === 'number'
                ? this.responses.deposit[0]
                : this.responses.deposit[0].id;
                
            // Fetch liability to get currency (liability[0] might be just an ID from listing)
            const liabilityGetResponse = await this.Request.get(`customer-liability/${typeof this.responses.customerLiability[0] === 'number' ? this.responses.customerLiability[0] : this.responses.customerLiability[0].id}`);
            const liabilityData = await liabilityGetResponse.json();
            const currencyId = liabilityData.currencyResponse.id;
            
            payload.customerId = this.responses.customer[0].id;
            payload.customerDetailId = customerGetData.lastCustomerDetailId;
            payload.customerCommunicationDataId = customerGetData.communicationData[0].id;
            
            payload.liabilities = [{
                id: liabilityId,
                current_amount: 100,
                currency_id: currencyId
            }];
            
            payload.deposits = [{
                id: depositId,
                current_amount: 100,
                currency_id: currencyId
            }];
            
            payload.receivedLiabilities = [{
                id: liabilityId,
                current_amount: 0,
                currency_id: currencyId
            }];
            
            payload.receivedDeposits = [{
                id: depositId,
                current_amount: 0,
                currency_id: currencyId
            }];
            
            payload.receivedOffsets = [{
                d_id: depositId,
                l_id: liabilityId,
                r_id: null,
                offset_amount: 100,
                currency_id: currencyId
            }];

            return payload;
        }
        else {
            // customerLiability[1] and deposit[0] might be just IDs or full objects
            const liabilityId = typeof this.responses.customerLiability[0] === 'number' 
                ? this.responses.customerLiability[0] 
                : this.responses.customerLiability[0].id;
            
            const depositId = typeof this.responses.deposit[0] === 'number'
                ? this.responses.deposit[0]
                : this.responses.deposit[0].id;
                
            // Fetch liability to get currency (liability[0] might be just an ID from listing)
            const liabilityGetResponse = await this.Request.get(`customer-liability/${typeof this.responses.customerLiability[0] === 'number' ? this.responses.customerLiability[0] : this.responses.customerLiability[0].id}`);
            const liabilityData = await liabilityGetResponse.json();
            const currencyId = liabilityData.currencyResponse.id;
            
            payload.customerId = this.responses.customer[0].id;
            payload.customerDetailId = customerGetData.lastCustomerDetailId;
            payload.customerCommunicationDataId = customerGetData.communicationData[0].id;
            
            payload.liabilities = [{
                id: liabilityId,
                current_amount: 100,
                currency_id: currencyId
            }];
            
            payload.deposits = [{
                id: depositId,
                current_amount: 100,
                currency_id: currencyId
            }];
            
            payload.receivedLiabilities = [{
                id: liabilityId,
                current_amount: 0,
                currency_id: currencyId
            }];
            
            payload.receivedDeposits = [{
                id: depositId,
                current_amount: 0,
                currency_id: currencyId
            }];
            
            payload.receivedOffsets = [{
                d_id: depositId,
                l_id: liabilityId,
                r_id: null,
                offset_amount: 100,
                currency_id: currencyId
            }];

            return payload;
        }
    }

    public async MLO_NP_R() {
        const payload = MLO_NP_R();
        const customerGetResponse = await this.Request.get(`customer/${this.responses.customer[0].id}`);
        const customerGetData = await customerGetResponse.json();
        
        // Handle both cases: payment can be just an ID (number) or full object
        const paymentId = typeof this.responses.payment[0] === 'number'
            ? this.responses.payment[0]
            : this.responses.payment[0].id;
            
        // Handle both cases: customerReceivable can be just an ID (number) or full object
        const receivableId = typeof this.responses.customerReceivable[0] === 'number'
            ? this.responses.customerReceivable[0]
            : this.responses.customerReceivable[0].id;
        
        // Fetch full payment object to get currencyId
        const paymentGetResponse = await this.Request.get(`payment/${paymentId}`);
        const paymentData = await paymentGetResponse.json();
        const currencyId = paymentData.currencyId.id;
        
        payload.customerId = this.responses.customer[0].id;
        payload.customerDetailId = customerGetData.lastCustomerDetailId;
        payload.customerCommunicationDataId = customerGetData.communicationData[0].id;
        
        // Update the existing array objects instead of replacing them
        payload.payments[0].id = paymentId;
        payload.payments[0].currency_id = currencyId;
        
        payload.receivables[0].id = receivableId;
        payload.receivables[0].currency_id = currencyId;
        
        payload.receivedPayments[0].id = paymentId;
        payload.receivedPayments[0].currency_id = currencyId;
        
        payload.receivedReceivables[0].id = receivableId;
        payload.receivedReceivables[0].currency_id = currencyId;
        
        payload.receivedOffsets[0].r_id = receivableId;
        payload.receivedOffsets[0].currency_id = currencyId;

        return payload;
    }

    public async MLO_L_R() {
        const payload = MLO_L_R()
        const customerGetResponse = await this.Request.get(`customer/${this.responses.customer[0].id}`);
        const customerGetData = await customerGetResponse.json();
        
        // Handle both cases: customerLiability can be just an ID (number) or full object
        const liabilityId = typeof this.responses.customerLiability[0] === 'number' 
            ? this.responses.customerLiability[0] 
            : this.responses.customerLiability[0].id;
            
        // Handle both cases: customerReceivable can be just an ID (number) or full object
        const receivableId = typeof this.responses.customerReceivable[0] === 'number'
            ? this.responses.customerReceivable[0]
            : this.responses.customerReceivable[0].id;
        
        // Fetch full liability object to get currencyId
        const liabilityGetResponse = await this.Request.get(`customer-liability/${liabilityId}`);
        const liabilityData = await liabilityGetResponse.json();
        const currencyId = liabilityData.currencyResponse.id;
        
        payload.customerId = this.responses.customer[0].id;
        payload.customerDetailId = customerGetData.lastCustomerDetailId;
        payload.customerCommunicationDataId = customerGetData.communicationData[0].id;
        
        // Update the existing array objects instead of replacing them
        payload.liabilities[0].id = liabilityId;
        payload.liabilities[0].currency_id = currencyId;
        
        payload.receivables[0].id = receivableId;
        payload.receivables[0].currency_id = currencyId;
        
        payload.receivedLiabilities[0].id = liabilityId;
        payload.receivedLiabilities[0].currency_id = currencyId;
        
        payload.receivedReceivables[0].id = receivableId;
        payload.receivedReceivables[0].currency_id = currencyId;
        
        payload.receivedOffsets[0].l_id = liabilityId;
        payload.receivedOffsets[0].r_id = receivableId;
        payload.receivedOffsets[0].currency_id = currencyId;

        return payload;
    }

    public async cbgCreate() {
        const payload = Cbgpayload();
        const podIdentifiers = []
        const uploadUrl = 'balancingGroupCoordinatorObjection/upload-file';
        const downloadUrl = 'balancingGroupCoordinatorObjection/template/download';

        for(let i = 0; i < this.responses.pod.length; i++){
            let identifier = await this.Request.get(`pod/${this.responses.pod[i].id}`)
            let identifierData = await identifier.json();
            podIdentifiers.push(identifierData.identifier);
        }

        const massImport = new massImportGenerator(this.Request, this.fileUploadRequest, this.responses, uploadUrl, downloadUrl, true);
        const res = await massImport.generateAndUpload(podIdentifiers);
        
        payload.fileId = res.uploadedFileId;
        return payload;
    }

    public async exchangeRateForMainCurrency() {/** we are checking what is main currency here and what is exchange rate */
        const currency = await this.Request.get(`currencies?statuses=ACTIVE&page=0&size=25`);
        expect(currency).CheckResponse();
        const responseBody = await currency.json();
        const selectedCurrency = responseBody.content.find((X:any) => X.defaultSelection === true);

        return selectedCurrency?.altCurrencyExchangeRate
    }
}