import { APIRequestContext } from "playwright";
import { baseFixture } from "../../../fixtures/baseFixture";
import { customerCoordinatePayload } from '../../payloads/salesPortal/SPCoordinates';

export class salesPortalPayloads {
    private Request: APIRequestContext;
    private responses: baseFixture['Responses'];

    constructor(apiRequestContext: APIRequestContext, responses: baseFixture['Responses']) {
        this.Request = apiRequestContext;
        this.responses = responses;
    }

    public customerCoordinates() {
        const payload = customerCoordinatePayload();
        return payload;
    }

}