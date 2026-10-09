import { APIRequestContext } from "playwright";
import { baseFixture } from "../fixtures/baseFixture";

import {expect} from '../fixtures/baseFixture';
import { randomGens } from "../utils/randomGens";
import { Endpoints } from "../fixtures/constants/endpoints";

export class receivableValidations {
    private Request: APIRequestContext;
    private responses: baseFixture['Responses'];

    constructor(apiRequestContext: APIRequestContext, responses: baseFixture['Responses']) {
        this.Request = apiRequestContext;
        this.responses = responses;
    }

    public async paymentValidation(paymentId: number) {
        const status = ["ACTIVE", "DELETED", "REVERSED"];
        const creationType = ["AUTOMATIC", "MANUAL", null];

        const payment = await this.Request.get(`payment/${paymentId}`);
        const paymentJson = await payment.json();

        expect(paymentJson.id).toBeDefined();
        expect(paymentJson.paymentNumber).toMatch(/^Payment\d+$/);
        expect(paymentJson.paymentDate).not.toBe(null);
        expect(paymentJson.initialAmount).not.toBe(0);
        expect(paymentJson.collectionChannelId.id).not.toBe(null);
        expect(paymentJson.currencyId.id).not.toBe(null);
        expect(paymentJson.paymentPackageId).not.toBe(null);
        expect(paymentJson.accountPeriodId.id).not.toBe(null);
        expect(paymentJson.accountPeriodId.status).toBe('OPEN')
        expect(new Date(paymentJson.accountPeriodId.startDate).getTime()).toBeLessThan(new Date(randomGens.generateTodaysDate('yyyy-mm-dd')).getTime());
        expect(new Date(paymentJson.accountPeriodId.endDate).getTime()).toBeGreaterThan(new Date(randomGens.generateTodaysDate('yyyy-mm-dd')).getTime());
        expect(paymentJson.customerId.id).not.toBe(null);
        expect(status).toContain(paymentJson.status);
        expect(creationType).toContain(paymentJson.creationType)

        if(paymentJson.initialAmount > 0) {
            expect(paymentJson.currentAmount).toBe(0);
            expect((paymentJson.offsettingResponseList??[]).length).toBeGreaterThan(0);
            expect(String(paymentJson.fullOffsetDate).slice(0, 10)).toBe(String(paymentJson.paymentDate).slice(0, 10));
        };

        if(paymentJson.blockedForOffsetting === true) {
            expect(paymentJson.blockedForOffsettingFromDate).not.toBe(null);
            expect(paymentJson.blockedForOffsettingBlockingReasonId).not.toBe(null);
        }

        if(paymentJson.creationType === "AUTOMATIC") {
            expect((paymentJson.offsettingResponseList ?? []).length).toBeGreaterThan(0);
        }
    }

    public async paymentMassImportValidation(params: {
        paymentId: number;
        customerId: number;
        customerIdentifier: string;
        collectionChannelId: number;
        invoiceId: number;
        invoiceAmount: number;
        paymentDate?: string;
    }) {
        const payment = await this.Request.get(`payment/${params.paymentId}`);
        await expect(payment).CheckResponse();
        const paymentJson = await payment.json();

        await this.paymentValidation(params.paymentId);

        expect(paymentJson.customerId.id).toBe(params.customerId);
        expect(String(paymentJson.customerId.personalNumber)).toBe(String(params.customerIdentifier));
        expect(paymentJson.collectionChannelId.id).toBe(params.collectionChannelId);
        expect(Math.abs(Number(paymentJson.initialAmount))).toBeCloseTo(Number(params.invoiceAmount), 2);

        const expectedDate = (params.paymentDate ?? randomGens.generateTodaysDate('yyyy-mm-dd')).slice(0, 10);
        expect(String(paymentJson.paymentDate).slice(0, 10)).toBe(expectedDate);

        if (paymentJson.invoiceId != null) {
            const linkedInvoiceId =
                typeof paymentJson.invoiceId === 'object' ? paymentJson.invoiceId.id : paymentJson.invoiceId;
            expect(linkedInvoiceId).toBe(params.invoiceId);
            expect(paymentJson.outgoingDocumentType).toBe('INVOICE');
        }

        expect(paymentJson.status).toBe('ACTIVE');
        expect(paymentJson.isOnlinePayment).toBe(false);

        return paymentJson;
    }

    public async LatePaymentFineValidation(LatePaymentFineId: number) {

        const type = [ "LATE_PAYMENT_FINE" , "REVERSAL_OF_LATE_PAYMENT_FINE"]

        const get = await this.Request.get(`latePaymentFine/${LatePaymentFineId}`);
        const LPFJson = await get.json();

        expect(LPFJson.id).toBeTruthy();
        expect(LPFJson.latePaymentNumber).toMatch(/^ЕЛС-\d{10}$/);
        expect(type).toContain(LPFJson.type);
        expect(LPFJson.customerShortResponse.id).toBe(this.responses.customer[0].id);
        expect(LPFJson.currencyShortResponse).toBeTruthy();
        expect(LPFJson.issuerVersionId).toBeTruthy();
        expect(LPFJson.createDate.split('T')[0]).toBe(LPFJson.logicalDate);

        if (LPFJson.templateShortResponse) {
            expect(LPFJson.fileResponse.length, "❌ File was not generated for LPF ❌").toBeGreaterThan(0);
        }

        if(LPFJson.amount < 0) {
            expect(LPFJson.type).toBe("REVERSAL_OF_LATE_PAYMENT_FINE");
            expect(LPFJson.reversed).toBeTruthy();
        }
        else {
            expect(LPFJson.type).toBe("LATE_PAYMENT_FINE")
        }

        /* Parent liability check*/
        const parentLiability = await this.Request.get(`${Endpoints.customerLiability}/${LPFJson.parentLiabilityShortResponse.id}`);
        expect(parentLiability).CheckResponse();
        const liabilityResponse = await parentLiability.json();

        /** Billing group existance and connection to invoice*/
        if(liabilityResponse.billingGroupResponse !== null) {
            expect(liabilityResponse.billingGroupResponse).toBeTruthy()
            expect(LPFJson.contractBillingGroupShortResponse).toBeTruthy()
            expect(LPFJson.contractBillingGroupShortResponse.id).toBe(liabilityResponse.billingGroupResponse.id);
            expect(LPFJson.contractBillingGroupShortResponse.name).toBe(liabilityResponse.billingGroupResponse.groupNumber);   
        }

        if(liabilityResponse.invoiceResponse !== null) {
            expect(LPFJson.tableResponse.invoiceId).toBe(liabilityResponse.invoiceResponse.id);
            expect(liabilityResponse.invoiceResponse.invoiceNumber).toContain(LPFJson.tableResponse.invoiceNumber);
        }
    }
}
