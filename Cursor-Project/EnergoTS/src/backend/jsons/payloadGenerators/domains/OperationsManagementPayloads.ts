import { APIRequestContext } from "playwright";
import { baseFixture } from "../../../fixtures/baseFixture";
import { process_periodicity } from '../../payloads/operationsManagement/processPeriodicity';
import { depositEmailTemplate } from '../../payloads/operationsManagement/depositEmailTemplate';
import { invoiceCancellationTemplate } from '../../payloads/operationsManagement/invoiceCancellationTemplate';
import { invoiceDocumentTemplate } from '../../payloads/operationsManagement/invoiceDocumentTemplate';
import { invoiceEmailTemplate } from '../../payloads/operationsManagement/invoiceEmailTemplate';
import { penaltyDocumentTemplate } from '../../payloads/operationsManagement/penaltyDocumentTemplate';
import { penaltyEmailTemplate } from '../../payloads/operationsManagement/penaltyEmailTemplate';
import { productContractBilingual } from '../../payloads/operationsManagement/productContractBilingual';
import { productContractBulgarian } from '../../payloads/operationsManagement/productContractBulgarian';
import { reminderDisconnectionEmailTemplate } from '../../payloads/operationsManagement/reminderDisconnectionEmailTemplate';
import { reminderDisconnectionSMSTemplate } from '../../payloads/operationsManagement/RFDST';
import { reminderDocumentTemplate } from '../../payloads/operationsManagement/reminderDocumentTemplate';
import { reminderEmailTemplate } from '../../payloads/operationsManagement/reminderEmailTemplate';
import { reminderSMSTemplate } from '../../payloads/operationsManagement/reminderSMSTemplate';
import { serviceContractBilingual } from '../../payloads/operationsManagement/serviceContractBilingual';
import { serviceContractBulgarian } from '../../payloads/operationsManagement/serviceContractBulgarian';
import { terminationEmailTemplate } from '../../payloads/operationsManagement/terminationEmailTemplate';

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