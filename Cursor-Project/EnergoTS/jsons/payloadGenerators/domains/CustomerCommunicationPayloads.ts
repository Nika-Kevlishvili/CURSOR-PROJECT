import { APIRequestContext } from "playwright";
import { baseFixture } from "../../../fixtures/baseFixture";
import { sms } from "../../payloads/create/customerCommunication/sms";

export class CustomerCommunicationPayloads {
    private Request: APIRequestContext;
    private responses: baseFixture['Responses'];

    constructor(apiRequestContext: APIRequestContext, responses: baseFixture['Responses']) {
        this.Request = apiRequestContext;
        this.responses = responses;
    }

    public sms() {
        const payload = sms();
        return payload;
    }


}