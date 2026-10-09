import { APIRequestContext} from "@playwright/test";
import { depositEmailTemplate } from '../operationsManagement/depositEmailTemplate';
import { invoiceCancellationTemplate } from '../operationsManagement/invoiceCancellationTemplate';
import { invoiceDocumentTemplate } from '../operationsManagement/invoiceDocumentTemplate';
import { invoiceEmailTemplate } from '../operationsManagement/invoiceEmailTemplate';
import { penaltyDocumentTemplate } from '../operationsManagement/penaltyDocumentTemplate';
import { penaltyEmailTemplate } from '../operationsManagement/penaltyEmailTemplate';
import { productContractBulgarian } from '../operationsManagement/productContractBulgarian';
import { reminderDisconnectionEmailTemplate } from '../operationsManagement/reminderDisconnectionEmailTemplate';
import { reminderDisconnectionSMSTemplate } from '../operationsManagement/RFDST';
import { reminderDocumentTemplate } from '../operationsManagement/reminderDocumentTemplate';
import { reminderEmailTemplate } from '../operationsManagement/reminderEmailTemplate';
import { reminderSMSTemplate } from '../operationsManagement/reminderSMSTemplate';
import { serviceContractBilingual } from '../operationsManagement/serviceContractBilingual';
import { serviceContractBulgarian } from '../operationsManagement/serviceContractBulgarian';
import { terminationEmailTemplate } from '../operationsManagement/terminationEmailTemplate';
import { requestForDisconnectionDocumentTemplate } from '../operationsManagement/RFDDT';
import { reminderDisconnectionDocumentTemplate } from '../operationsManagement/REFDDT';
import { emailTemplate } from "../operationsManagement/emailTemplate";
import { emailDocumentTemplate } from "../operationsManagement/emailDocumentTemplate";
import { cbgDocumentTemplate } from "../operationsManagement/cbgDocumentTemplate";
import { cbgEmailTemplate } from "../operationsManagement/cbgEmailTemplate";
import { ReschedulingDocumentTemplate } from "../operationsManagement/reschedulinDocumentTemplate";
import {expect} from '../../../fixtures/baseFixture';
import * as fs from 'fs';
import path from 'path';

export class templates {
    private templateRequest: APIRequestContext;
    private request: APIRequestContext;
    private templatesData: any;


    private currentTemplate: string
    private currentTemplateUrlGet: string;
    private currentTemplateGetRequest: any
    private currentTemplateUrlPost: string;

    private currentPayload: any
    private currentFileName: string;

    constructor(templateRequest: APIRequestContext, normalRequest: APIRequestContext) {
        this.templateRequest = templateRequest;
        this.request = normalRequest;
        this.templatesData = {};

        this.currentTemplate = '';
        this.currentTemplateUrlGet = 'template/list';
        this.currentTemplateGetRequest = {}
        this.currentTemplateUrlPost = 'template';

        this.currentPayload = {};
        this.currentFileName = '';
    }

    private async checkTemplateFile(templateId: string) {
        const getTemplate = await this.request.get(`template/${templateId}`);
        const templateJson = await getTemplate.json();

        const getTemplateFile = await this.request.get(`template/${templateId}/download-template-file?id=${templateJson.file.id}`);
        if (!getTemplateFile.ok()) {
            const newfileId = this.uploadTemplateFile();
            const updateRes = await this.request.put(`template/${templateId}?versionId=1`, {data: this.currentPayload})
        }
    }

    private async uploadTemplateFile(format: string = 'DOCX', fileNameOverride?: string) {
        try {
            const fileToUpload = path.resolve(__dirname, `../../../fixtures/templatedocs/${fileNameOverride ?? this.currentFileName}`);

            if (!fs.existsSync(fileToUpload)) {
                throw new Error(`File not found: ${fileToUpload}`);
            }
            
            const fileStream = fs.createReadStream(fileToUpload);
            
            const response = await this.templateRequest.post(`template/upload-template-file?fileFormats=${format}`, {
                multipart: {
                    'file': fileStream
                }
            });
            

            if (!response.ok()) {
                const errorText = await response.text();
                console.error('❌ Upload failed:', response.status(), errorText);
                throw new Error(`Upload failed with status ${response.status()}: ${errorText}`);
            }

            const responseJson = await response.json();
            return responseJson.id;
        
        } catch (error) {
            console.error('❌ Error uploading template file:', error instanceof Error ? error.message : error);
            throw error;
        }
    }

    private async checkTemplate(fileFormat: string = 'DOCX') {
        // Snapshot instance vars before the first await so concurrent calls don't overwrite each other
        const template = this.currentTemplate;
        const templateGetRequest = this.currentTemplateGetRequest;
        const fileName = this.currentFileName;
        const payload = { ...this.currentPayload };

        let response = await this.request.post(this.currentTemplateUrlGet, { data: templateGetRequest });
        const responseText = await response.text();

        let responseData;
        try {
            responseData = JSON.parse(responseText);
        } catch (e) {
            throw new Error(`Failed to parse JSON response from ${this.currentTemplateUrlGet}. Status: ${response.status()}. Body: ${responseText.substring(0, 200)}`);
        }

        if (responseData.errorCode) {
            throw new Error(`API Error: ${responseData.errorCode} - ${responseData.message}`);
        }

        if (Array.isArray(responseData.content) && responseData.content.length === 0) {
            const uploadedFileId = await this.uploadTemplateFile(fileFormat, fileName);
            payload.fileId = uploadedFileId;

            const createRes = await this.request.post(this.currentTemplateUrlPost, { data: payload });
            expect(createRes).CheckResponse();
            const createdJson = await createRes.json();

            this.templatesData[template] = createdJson.id;
        } else if (responseData.content && responseData.content.length > 0) {
            this.templatesData[template] = responseData.content[0].id;
        } else {
            throw new Error(`Unexpected response format for template ${template}`);
        }
    }

    private async depositEmailTemplate() {
        this.currentTemplate = 'deposit_email_template',
        this.currentPayload = depositEmailTemplate();
        this.currentTemplateGetRequest = {
            "types": [
                "EMAIL"
            ],
            "templatePurposes": [
                "DEPOSIT"
            ],
            "statuses": ["ACTIVE"],
            "page": 0,
            "size": 1,
            "prompt": this.currentPayload.name
        }

        this.currentFileName = 'deposit_email_template.docx';
        await this.checkTemplate()
    }

    private async invoiceCancellationTemplate() {
        this.currentTemplate = 'invoice_cancellation_template',
        this.currentPayload = invoiceCancellationTemplate();
        this.currentTemplateGetRequest = {
            "types": [
                "DOCUMENT"
            ],
            "templatePurposes": [
                "INVOICE_CANCEL"
            ],
            "statuses": ["ACTIVE"],
            "page": 0,
            "size": 1,
            "prompt": this.currentPayload.name
        }

        this.currentFileName = 'invoiceCancellation.docx'
        await this.checkTemplate()
        
    }

    private async invoiceDocumentTemplate(prompt: string = '') {
        this.currentTemplate = 'invoice_document_template',
        this.currentPayload = invoiceDocumentTemplate();
        this.currentTemplateGetRequest = {
            "types": [
                "DOCUMENT"
            ],
            "templatePurposes": [
                "INVOICE"
            ],
            "statuses": ["ACTIVE"],
            "page": 0,
            "size": 1,
            "prompt": prompt ? prompt : this.currentPayload.name
        }

        this.currentFileName = 'invoiceDocument.docx'
        await this.checkTemplate()
        
    }

    private async invoiceEmailTemplate() {
        this.currentTemplate = 'invoice_email_template',
        this.currentPayload = invoiceEmailTemplate();
        this.currentTemplateGetRequest = {
            "types": [
                "EMAIL"
            ],
            "templatePurposes": [
                "INVOICE"
            ],
            "statuses": ["ACTIVE"],
            "page": 0,
            "size": 1,
            "prompt": this.currentPayload.name
        }

        this.currentFileName = 'invoiceEmail.docx'
        await this.checkTemplate()
        
    }

    private async penaltyDocumentTemplate() {
        this.currentTemplate = 'penalty_document_template',
        this.currentPayload = penaltyDocumentTemplate();
        this.currentTemplateGetRequest = {
            "types": [
                "DOCUMENT"
            ],
            "templatePurposes": [
                "PENALTY"
            ],
            "statuses": ["ACTIVE"],
            "page": 0,
            "size": 1,
            "prompt": this.currentPayload.name
        }

        this.currentFileName = 'penaltyDocument.docx'
        await this.checkTemplate()
        
    }

    private async penaltyEmailTemplate() {
        this.currentTemplate = 'penalty_email_template',
        this.currentPayload = penaltyEmailTemplate();
        this.currentTemplateGetRequest = {
            "types": [
                "EMAIL"
            ],
            "templatePurposes": [
                "PENALTY"
            ],
            "statuses": ["ACTIVE"],
            "page": 0,
            "size": 1,
            "prompt": this.currentPayload.name
        }

        this.currentFileName = 'penaltyEmail.docx'
        await this.checkTemplate()
        
    }

    private async productContractTemplate() {
        this.currentTemplate = 'product_contract_template',
        this.currentPayload = productContractBulgarian();
        this.currentTemplateGetRequest = {
            
            'languages': [
                "BULGARIAN"
            ],
            
            "types": [
                "DOCUMENT"
            ],
            "templatePurposes": [
                "PRODUCT"
            ],
            "statuses": ["ACTIVE"],
            "page": 0,
            "size": 1,
            "prompt": this.currentPayload.name
        }

        this.currentFileName = 'productContract.docx'
        await this.checkTemplate()
        
    }

    private async reminderDisconnectionEmailTemplate() {
        this.currentTemplate = 'reminder_disconnection_email_template';
        this.currentPayload = reminderDisconnectionEmailTemplate();
        this.currentTemplateGetRequest = {
            "types": ["EMAIL"],
            "templatePurposes": ["REMINDER_DISCONNECT_POWER"],
            "statuses": ["ACTIVE"],
            "page": 0,
            "size": 1,
            "prompt": this.currentPayload.name
        };
;
        this.currentFileName = 'reminderForDisconnectionEmail.docx';
        await this.checkTemplate();
    }

    private async reminderDisconnectionSMSTemplate() {
        this.currentTemplate = 'reminder_disconnection_sms_template';
        this.currentPayload = reminderDisconnectionSMSTemplate();
        this.currentTemplateGetRequest = {
            "types": ["SMS"],
            "templatePurposes": ["REMINDER_DISCONNECT_POWER"],
            "statuses": ["ACTIVE"],
            "page": 0,
            "size": 1,
            "prompt": this.currentPayload.name
        };
;
        this.currentFileName = 'reminderForDisconnectionSMS.docx';
        await this.checkTemplate();
    }

    private async reminderDisconnectionDocumentTemplate() {
        this.currentTemplate = 'reminder_disconnection_document_template';
        this.currentPayload = reminderDisconnectionDocumentTemplate();
        this.currentTemplateGetRequest = {
            "types": ["DOCUMENT"],
            "templatePurposes": ["REMINDER_DISCONNECT_POWER"],
            "statuses": ["ACTIVE"],
            "page": 0,
            "size": 1,
            "prompt": this.currentPayload.name
        };
;
        this.currentFileName = 'reminderForDisconnectionDocument.docx'
        await this.checkTemplate();
    }

    //currently there is no bulgarian template to be used, so we are using dummydoc here
    public async requestForDisconnectionDocumentTemplate() {
        this.currentTemplate = 'request_disconnection_document_template';
        this.currentPayload = requestForDisconnectionDocumentTemplate();
        this.currentTemplateGetRequest = {
            "types": ["DOCUMENT"],
            "templatePurposes": ["REQUEST_DISCONNECT_POWER"],
            "statuses": ["ACTIVE"],
            "page": 0,
            "size": 1,
            "prompt": this.currentPayload.name
        };
;
        this.currentFileName = 'dummydoc.docx'
        await this.checkTemplate();
    }

    //no bulgarian template file
    private async reminderDocumentTemplate() {
        this.currentTemplate = 'reminder_document_template';
        this.currentPayload = reminderDocumentTemplate();
        this.currentTemplateGetRequest = {
            "types": ["DOCUMENT"],
            "templatePurposes": ["REMINDER"],
            "statuses": ["ACTIVE"],
            "page": 0,
            "size": 1,
            "prompt": this.currentPayload.name
        };
;
        this.currentFileName = 'dummydoc.docx'
        await this.checkTemplate();
    }

    //no bulgarian template file
    private async reminderEmailTemplate() {
        this.currentTemplate = 'reminder_email_template';
        this.currentPayload = reminderEmailTemplate();
        this.currentTemplateGetRequest = {
            "types": ["EMAIL"],
            "templatePurposes": ["REMINDER"],
            "statuses": ["ACTIVE"],
            "page": 0,
            "size": 1,
            "prompt": this.currentPayload.name
        };
;
        this.currentFileName = 'dummydoc.docx'
        await this.checkTemplate();
    }

    //no bulgarian template file
    private async reminderSMSTemplate() {
        this.currentTemplate = 'reminder_sms_template';
        this.currentPayload = reminderSMSTemplate();
        this.currentTemplateGetRequest = {
            "types": ["SMS"],
            "templatePurposes": ["REMINDER"],
            "statuses": ["ACTIVE"],
            "page": 0,
            "size": 1,
            "prompt": this.currentPayload.name
        };
;
        this.currentFileName = 'dummydoc.docx'
        await this.checkTemplate();
    }

    private async serviceContractBilingualTemplate() {
        this.currentTemplate = 'service_contract_bilingual';
        this.currentPayload = serviceContractBilingual();
        this.currentTemplateGetRequest = {
            "types": ["DOCUMENT"],
            "templatePurposes": ["SERVICE"],
            "statuses": ["ACTIVE"],
            "page": 0,
            "size": 1,
            "prompt": this.currentPayload.name
        };
;
        this.currentFileName = 'serviceContractDocument.docx'
        await this.checkTemplate();
    }

    private async serviceContractBulgarianTemplate() {
        this.currentTemplate = 'service_contract_bulgarian';
        this.currentPayload = serviceContractBulgarian();
        this.currentTemplateGetRequest = {
            "types": ["DOCUMENT"],
            "templatePurposes": ["SERVICE"],
            "statuses": ["ACTIVE"],
            "page": 0,
            "size": 1,
            "prompt": this.currentPayload.name
        };
;
        this.currentFileName = 'serviceContractDocument.docx'
        await this.checkTemplate();
    }

    private async terminationEmailTemplate() {
        this.currentTemplate = 'termination_email_template';
        this.currentPayload = terminationEmailTemplate();
        this.currentTemplateGetRequest = {
            "types": ["EMAIL"],
            "templatePurposes": ["TERMINATION"],
            "statuses": ["ACTIVE"],
            "page": 0,
            "size": 1,
            "prompt": this.currentPayload.name
        };
;
        this.currentFileName = 'dummydoc.docx'
        await this.checkTemplate();
    }

    public async emailTemplate() {
        this.currentTemplate = 'email_template';
        this.currentPayload = emailTemplate();
        this.currentTemplateGetRequest = {
            "types": ["EMAIL"],
            "templatePurposes": ["EMAIL"],
            "statuses": ["ACTIVE"],
            "page": 0,
            "size": 1,
            "prompt": this.currentPayload.name
        };
        
        this.currentFileName = 'dummydoc.docx'
        await this.checkTemplate();
    }

    public async documentTemplate() {
        this.currentTemplate = 'email_document_template';
        this.currentPayload = emailDocumentTemplate();
        this.currentTemplateGetRequest = {
            "types": ["DOCUMENT"],
            "templatePurposes": ["EMAIL"],
            "statuses": ["ACTIVE"],
            "page": 0,
            "size": 1,
            "prompt": this.currentPayload.name
        };

        this.currentFileName = 'dummydoc.docx'
        await this.checkTemplate();
    }

    public async cbgDocumentTemplate() {
        this.currentTemplate = 'cbg_document_template';
        this.currentPayload = cbgDocumentTemplate();
        this.currentTemplateGetRequest = {
            "types": ["DOCUMENT"],
            "templatePurposes": ["OBJECTION_CHANGE_COORD"],
            "statuses": ["ACTIVE"],
            "page": 0,
            "size": 1,
            "prompt": this.currentPayload.name
        };
        
        this.currentFileName = 'CbgDocument.xlsx'
        await this.checkTemplate('XLSX');
    }

    public async cbgEmailTemplate() {
        this.currentTemplate = 'cbg_email_template';
        this.currentPayload = cbgEmailTemplate();
        this.currentTemplateGetRequest = {
            "types": ["EMAIL"],
            "templatePurposes": ["OBJECTION_CHANGE_COORD"],
            "statuses": ["ACTIVE"],
            "page": 0,
            "size": 1,
            "prompt": this.currentPayload.name
        };
        
        this.currentFileName = 'cbgEmail.docx'
        await this.checkTemplate();
    }

    public async reschedulingDocumentTemplate() {
        this.currentTemplate = 'rescheduling_document_template';
        this.currentPayload = ReschedulingDocumentTemplate();
        this.currentTemplateGetRequest = {
            "types": ["DOCUMENT"],
            "templatePurposes": ["RESCHEDULING"],
            "statuses": ["ACTIVE"],
            "page": 0,
            "size": 1,
            "prompt": this.currentPayload.name
        };

        this.currentFileName = 'reschedulingTemplate.docx'
        await this.checkTemplate();
    }

    public async generateEveryTemplate() {
        await Promise.all([
            this.depositEmailTemplate(),
            this.invoiceCancellationTemplate(),
            this.invoiceDocumentTemplate(),
            this.invoiceEmailTemplate(),
            this.penaltyDocumentTemplate(),
            this.penaltyEmailTemplate(),
            this.productContractTemplate(),
            this.reminderDocumentTemplate(),
            this.reminderEmailTemplate(),
            this.reminderSMSTemplate(),
            this.reminderDisconnectionEmailTemplate(),
            this.reminderDisconnectionSMSTemplate(),
            this.reminderDisconnectionDocumentTemplate(),
            this.serviceContractBilingualTemplate(),
            this.serviceContractBulgarianTemplate(),
            this.terminationEmailTemplate(),
            this.requestForDisconnectionDocumentTemplate(),
            this.emailTemplate(),
            this.documentTemplate(),
            this.cbgDocumentTemplate(),
            this.cbgEmailTemplate(),
            this.reschedulingDocumentTemplate(),
        ]);

        return this.templatesData;
    }
}