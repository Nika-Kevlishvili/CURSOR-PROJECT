import { APIRequestContext } from "playwright";
import { baseFixture } from "../fixtures/baseFixture";

import {expect} from '../fixtures/baseFixture';
import { randomGens } from "../utils/randomGens";

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
            expect(paymentJson.fullOffsetDate).toBe(paymentJson.paymentDate);
        };

        if(paymentJson.blockedForOffsetting === true) {
            expect(paymentJson.blockedForOffsettingFromDate).not.toBe(null);
            expect(paymentJson.blockedForOffsettingBlockingReasonId).not.toBe(null);
        }

        if(paymentJson.creationType === "AUTOMATIC") {
            expect((paymentJson.offsettingResponseList ?? []).length).toBeGreaterThan(0);
        }
    }
}