import { APIRequestContext } from "playwright";
import { baseFixture } from "../../../fixtures/baseFixture";
import { process_periodicity } from '../../payloads/create/operationsManagement/processPeriodicity';
import { depositEmailTemplate } from '../../payloads/create/operationsManagement/depositEmailTemplate';
import { invoiceCancellationTemplate } from '../../payloads/create/operationsManagement/invoiceCancellationTemplate';
import { invoiceDocumentTemplate } from '../../payloads/create/operationsManagement/invoiceDocumentTemplate';
import { invoiceEmailTemplate } from '../../payloads/create/operationsManagement/invoiceEmailTemplate';
import { penaltyDocumentTemplate } from '../../payloads/create/operationsManagement/penaltyDocumentTemplate';
import { penaltyEmailTemplate } from '../../payloads/create/operationsManagement/penaltyEmailTemplate';
import { productContractBilingual } from '../../payloads/create/operationsManagement/productContractBilingual';
import { productContractBulgarian } from '../../payloads/create/operationsManagement/productContractBulgarian';
import { reminderDisconnectionEmailTemplate } from '../../payloads/create/operationsManagement/reminderDisconnectionEmailTemplate';
import { reminderDisconnectionSMSTemplate } from '../../payloads/create/operationsManagement/reminderDisconnectionSMSTemplate';
import { reminderDocumentTemplate } from '../../payloads/create/operationsManagement/reminderDocumentTemplate';
import { reminderEmailTemplate } from '../../payloads/create/operationsManagement/reminderEmailTemplate';
import { reminderSMSTemplate } from '../../payloads/create/operationsManagement/reminderSMSTemplate';
import { serviceContractBilingual } from '../../payloads/create/operationsManagement/serviceContractBilingual';
import { serviceContractBulgarian } from '../../payloads/create/operationsManagement/serviceContractBulgarian';
import { terminationEmailTemplate } from '../../payloads/create/operationsManagement/terminationEmailTemplate';

export class OperationsManagementPayloads {
    private Request: APIRequestContext;
    private responses: baseFixture['Responses'];

    constructor(apiRequestContext: APIRequestContext, responses: baseFixture['Responses']) {
        this.Request = apiRequestContext
        this.responses = responses
    }

    public tasks() {

    }

    public activities() {
        
    }

    public processes() {
        
    }

    public async process_periodicity() {
        const payload = process_periodicity();
        return payload;
    }

    public depositEmailTemplate() {
        const payload = depositEmailTemplate();
        return payload;
    }

    public invoiceCancellationTemplate() {
        const payload = invoiceCancellationTemplate();
        return payload;
    }

    public invoiceDocumentTemplate() {
        const payload = invoiceDocumentTemplate();
        return payload;
    }

    public invoiceEmailTemplate() {
        const payload = invoiceEmailTemplate();
        return payload;
    }

    public penaltyDocumentTemplate() {
        const payload = penaltyDocumentTemplate();
        return payload;
    }

    public penaltyEmailTemplate() {
        const payload = penaltyEmailTemplate();
        return payload;
    }

    public productContractBilingual() {
        const payload = productContractBilingual();
        return payload;
    }

    public productContractBulgarian() {
        const payload = productContractBulgarian();
        return payload;
    }

    public reminderDisconnectionEmailTemplate() {
        const payload = reminderDisconnectionEmailTemplate();
        return payload;
    }

    public reminderDisconnectionSMSTemplate() {
        const payload = reminderDisconnectionSMSTemplate();
        return payload;
    }

    public reminderDocumentTemplate() {
        const payload = reminderDocumentTemplate();
        return payload;
    }

    public reminderEmailTemplate() {
        const payload = reminderEmailTemplate();
        return payload;
    }

    public reminderSMSTemplate() {
        const payload = reminderSMSTemplate();
        return payload;
    }

    public serviceContractBilingual() {
        const payload = serviceContractBilingual();
        return payload;
    }

    public serviceContractBulgarian() {
        const payload = serviceContractBulgarian();
        return payload;
    }

    public terminationEmailTemplate() {
        const payload = terminationEmailTemplate();
        return payload;
    }

    public qesSigningDocuments() {
        
    }
}