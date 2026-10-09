import { customerMassPayload } from '../../massImportPayloads/customerMpPayload';
import { APIRequestContext } from "playwright";
import { baseFixture } from '../../../fixtures/baseFixture';
import { randomGens } from '../../../utils/randomGens';

export class CustomerMassImportGenerator {
    private Request: APIRequestContext;
    private responses: baseFixture['Responses'];
    private templateName = 'CUSTOMERS'

    constructor(apiRequestContext: APIRequestContext, responses: baseFixture['Responses']) {
        this.Request = apiRequestContext;
        this.responses = responses;
    }

    public privateCustomerMP(){
        const payload = customerMassPayload()
        payload.customer_type.value = 'PRIVATE_CUSTOMER'
        
        payload.customer_business_activity.value = 'false'
        payload.customer_name.value = 'Private Customer Name'
        payload.customer_last_name.value = 'Last Name'


        return {payload, templateName: this.templateName }
    }

    public privateWbusinessMP(){
        const payload = customerMassPayload()
        payload.customer_type.value = 'PRIVATE_CUSTOMER'
        payload.customer_business_activity.value = 'true' 

        payload.customer_legal_form.value = 'АД'
        payload.customer_legal_form_transl.value = 'AD'
        payload.customer_main_activity_subject.value = 'playwright automation'
        payload.customer_ownership_form.value = 'Частна'
        payload.customer_economic_branch_ci.value = 'Логистика'

        payload.customer_name.value = 'Private Business Name'
        payload.customer_last_name.value = 'Business Last Name'

        payload.manager1_title.value = 'Г-жа'
        payload.manager_name_1.value = 'mass'
        payload.manager_surname_1.value = 'import'
        payload.manager1_job_position.value = '1234'
        payload.manager1_representation_method.value = 'Самостоятелно'
        
        return {payload, templateName: this.templateName }
    }

    public legalCustomerMP(){
        const payload = customerMassPayload()

        payload.customer_type.value = 'LEGAL_ENTITY'
        payload.customer_identifier.value = `MI${randomGens.generateRandomString(false, true, 10)}`

        payload.customer_legal_form.value = 'АД'
        payload.customer_legal_form_transl.value = 'AD'

        payload.customer_name.value = 'LEGAL CUSTOMER MASS IMPORT'
        payload.customer_name_transl.value = 'LEGAL CUSTOMER MASS IMPORT'

        payload.customer_ownership_form.value = 'Частна'
        payload.customer_economic_branch_ci.value = 'Логистика'

        payload.customer_main_activity_subject.value = 'playwright automation'

        payload.manager1_title.value = 'Г-жа'
        payload.manager_name_1.value = 'MASS'
        payload.manager_surname_1.value = 'IMPORT'
        payload.manager1_job_position.value = 'ABCD'
        payload.manager1_representation_method.value = 'Самостоятелно'
        
        return {payload, templateName: this.templateName }
    }
    
}