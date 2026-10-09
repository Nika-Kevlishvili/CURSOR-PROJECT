import { productContractMassPayload } from '../../massImportPayloads/productContractMpPayload';
import { APIRequestContext } from "playwright";
import { baseFixture } from '../../../fixtures/baseFixture';

export class ProductContractMassImportGenerator {
    private Request: APIRequestContext;
    private responses: baseFixture['Responses'];
    private templateName = 'PRODUCT_CONTRACTS'

    constructor(apiRequestContext: APIRequestContext, responses: baseFixture['Responses']) {
        this.Request = apiRequestContext;
        this.responses = responses;
    }

    public createProductContractMP(){
        const payload = productContractMassPayload();
        return { payload, templateName: this.templateName };
    }

    public editProductContractMP(){
        const payload = productContractMassPayload();    
        return { payload, templateName: this.templateName };
    }

    public createNewVersionProductContractMP(){
        const payload = productContractMassPayload();
        return { payload, templateName: this.templateName };
    }
}