import { APIRequestContext } from "playwright";
import { baseFixture } from "../../../fixtures/baseFixture";

export class MasterDataPayloads {
    private Request: APIRequestContext;
    private responses: baseFixture['Responses'];

    constructor(apiRequestContext: APIRequestContext, responses: baseFixture['Responses']) {
        this.Request = apiRequestContext;
        this.responses = responses;
    }

    public companyDetails() {
        // This would be implemented when the corresponding payload is available
        throw new Error("Company details payload not yet implemented");
    }

    public nomenclature() {
        // This would be implemented when the corresponding payload is available
        throw new Error("Nomenclature payload not yet implemented");
    }
}