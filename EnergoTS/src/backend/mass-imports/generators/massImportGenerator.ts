import * as ExcelJS from 'exceljs';
import { APIRequestContext } from "playwright";
import { baseFixture } from '../../fixtures/baseFixture';
import fs from 'fs';
import path from 'path';

import { CustomerMassImportGenerator } from './domains/customerMP';
import { ProductContractMassImportGenerator } from './domains/productContractMP';
import { PaymentMassImportGenerator } from './domains/paymentMP';
import { SupplyActionDeactivationMassImportGenerator } from './domains/supplyActionDeactivationMP';
import { randomGens } from '../../utils/randomGens';

export class massImportGenerator{
    protected Request: APIRequestContext;
    protected FileUploadRequest: APIRequestContext;
    protected responses: baseFixture['Responses'];
    public readonly uploadUrl: string ;
    public readonly downloadUrl: string ;
    public vertical: boolean = false;
    public readonly CustomerMassImportGenerator: CustomerMassImportGenerator;
    public readonly ProductContractMassImportGenerator: ProductContractMassImportGenerator;
    public readonly PaymentMassImportGenerator: PaymentMassImportGenerator;
    public readonly SupplyActionDeactivationMassImportGenerator: SupplyActionDeactivationMassImportGenerator;

    constructor(
        apiRequestContext: APIRequestContext,
        fileUploadContext: APIRequestContext,
        responses: baseFixture['Responses'],
        uploadUrl: string = '',
        downloadUrl: string = '',
        vertical: boolean = false,
    ) {
        this.Request = apiRequestContext;
        this.FileUploadRequest = fileUploadContext;
        this.responses = responses;
        this.uploadUrl = uploadUrl;
        this.downloadUrl = downloadUrl;
        this.vertical = vertical; // default to horizontal, can be set to true for vertical templates

        this.CustomerMassImportGenerator = new CustomerMassImportGenerator(apiRequestContext, responses);
        this.ProductContractMassImportGenerator = new ProductContractMassImportGenerator(apiRequestContext, responses);
        this.PaymentMassImportGenerator = new PaymentMassImportGenerator();
        this.SupplyActionDeactivationMassImportGenerator = new SupplyActionDeactivationMassImportGenerator(apiRequestContext, responses);
    }
    
    private async populateExcelTemplate(payload: any, templatePath: string, outputPath: string) {
        try {
            const workbook = new ExcelJS.Workbook();
            await workbook.xlsx.readFile(templatePath);
            
            const worksheet = workbook.getWorksheet(1);
            
            if (!worksheet) {
                throw new Error('No worksheet found in the template');
            }

            if (this.vertical) {
                const values = Array.isArray(payload) ? payload : Object.values(payload);
                values.forEach((value, index) => {
                    const cell = worksheet.getCell(`A${index + 2}`);
                    cell.value = value;
                });
                await workbook.xlsx.writeFile(outputPath);
                return;
            }

            Object.keys(payload).forEach(key => {
                const field = payload[key];
                if (field && field.cellNumber && field.value !== undefined && field.value !== null && field.value !== '') {
                    try {
                        const cell = worksheet.getCell(field.cellNumber);
                        cell.value = field.value;
                    } catch (error) {
                        console.warn(`Failed to set cell ${field.cellNumber} for field ${key}:`, error);
                    }
                }
            });

            await workbook.xlsx.writeFile(outputPath);            
        } catch (error) {
            console.error('Error populating Excel template:', error);
            throw error;
        }
    }

    private ensureOutputDirectory(outputPath: string) {
        const fs = require('fs');
        if (!fs.existsSync(outputPath)) {
            fs.mkdirSync(outputPath, { recursive: true });
        }
    }

    private async downloadTemplate(templateUrl: string) {
        const templateResponse = await this.Request.get(templateUrl);
        
        if (!templateResponse.ok()) {
            throw new Error(`Failed to download template: ${templateResponse.status()} ${templateResponse.statusText()}`);
        }
        
        const outputDir = path.resolve(__dirname, '../output');
        this.ensureOutputDirectory(outputDir);
        
        const templateFileName = `${randomGens.generateRandomString()}-template.xlsx`;
        const templatePath = path.join(outputDir, templateFileName);
        
        const templateBuffer = await templateResponse.body();
        fs.writeFileSync(templatePath, templateBuffer);

        return {templatePath, outputDir}
    }

    private notificationPattern(templateName: string): RegExp {
        const rawTemplate = templateName.toUpperCase();
        // Normalize template name to singular (strip trailing S if present)
        const singularTemplate = rawTemplate.endsWith('S') ? rawTemplate.slice(0, -1) : rawTemplate;
        return new RegExp(`^(?:PROCESS_)?${singularTemplate}S?_MASS_IMPORT_COMPLETED$`);
    }

    private async latestMatchingNotificationId(templateName: string): Promise<number> {
        const pattern = this.notificationPattern(templateName);
        const notification = await this.Request.get('notifications?size=10&page=0');
        const body = await notification.json();
        const notifications = Array.isArray(body?.content) ? body.content : [body];
        const ids = notifications
            .filter((n: any) => n?.notificationType && pattern.test(String(n.notificationType).toUpperCase()))
            .map((n: any) => Number(n.id) || 0);
        return ids.length ? Math.max(...ids) : 0;
    }

    private async checkReport(templateName: string, afterNotificationId = 0) {
        const pattern = this.notificationPattern(templateName);

        // Poll for matching notification (check last 10 notifications, retry up to 30 seconds)
        const maxRetries = 30;
        const retryDelay = 1000; // 1 second
        let first: any = null;
        
        for (let attempt = 0; attempt < maxRetries; attempt++) {
            const notification = await this.Request.get('notifications?size=10&page=0');
            const body = await notification.json();
            const notifications = Array.isArray(body?.content) ? body.content : [body];

            // Ignore completions that already existed before this upload
            const matchingNotification = notifications.find((n: any) =>
                n?.notificationType
                && pattern.test(String(n.notificationType).toUpperCase())
                && (afterNotificationId <= 0 || Number(n.id) > afterNotificationId)
            );

            if (matchingNotification) {
                first = matchingNotification;
                break;
            }

            // Wait before retrying
            if (attempt < maxRetries - 1) {
                await new Promise(resolve => setTimeout(resolve, retryDelay));
            }
        }

        if (!first || !first.notificationType) {
            return { 
                success: false, 
                message: `No matching notification found for pattern ${pattern} after ${maxRetries} attempts` 
            };
        }

        // Continue with report download using first.entityId
        let reportFilePath: string | null = null;
        let reportResult = { success: false, message: null as string | null };

        const reportFile = await this.Request.get(
            `process/${first.entityId}/report/download?multiSheetExcelType=MASS_IMPORT_ERROR_REPORT`
        );

        if (reportFile.ok()) {
            const reportsDir = path.resolve(__dirname, '../reports');
            this.ensureOutputDirectory(reportsDir);
            const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
            const reportFileName = `mass-import-report-${timestamp}.xlsx`;
            reportFilePath = path.join(reportsDir, reportFileName);
            const reportBuffer = await reportFile.body();
            fs.writeFileSync(reportFilePath, reportBuffer);

            try {
                const workbook = new ExcelJS.Workbook();
                await workbook.xlsx.readFile(reportFilePath);
                const worksheet = workbook.getWorksheet(1);
                if (worksheet) {
                    const errors: string[] = [];
                    const lastRow = worksheet.actualRowCount || worksheet.rowCount;
                    for (let row = 2; row <= lastRow; row++) {
                        const cellValue = worksheet.getCell(`D${row}`).value;
                        if (cellValue != null && cellValue !== '') {
                            errors.push(String(cellValue));
                        }
                    }
                    if (errors.length === 0) {
                        reportResult = { success: true, message: null };
                    } else {
                        reportResult = { success: false, message: `Mass import ERROR: ${errors.join(' | ')}` };
                    }
                } else {
                    reportResult = { success: false, message: 'Worksheet missing in report file' };
                }
            } catch (e) {
                reportResult = { 
                    success: false, 
                    message: `Error parsing report file: ${(e as Error).message}` 
                };
            }
        } else {
            reportResult = { 
                success: false, 
                message: `Failed to download report: ${reportFile.status()} ${reportFile.statusText()}` 
            };
        }

        if (reportFilePath && fs.existsSync(reportFilePath)) {
            fs.unlinkSync(reportFilePath);
        }
        return reportResult;
    }

    public async generateAndUpload(payload: any) {
        const wrapped = payload
            && typeof payload === 'object'
            && !Array.isArray(payload)
            && payload.payload
            && typeof payload.payload === 'object'
            && !Array.isArray(payload.payload);
        const excelPayload = wrapped ? payload.payload : payload;
        const downloadUrl = this.downloadUrl || payload?.downloadUrl;
        const uploadUrl = this.uploadUrl || payload?.uploadUrl;
        if (!downloadUrl || !uploadUrl) {
            throw new Error('Mass import download or upload URL is not set');
        }

        const templateName = typeof payload === 'object' && !Array.isArray(payload) && payload.templateName
            ? payload.templateName
            : uploadUrl.split('/')[0].replace(/([A-Z])/g, '_$1').toUpperCase();

        const template = await this.downloadTemplate(downloadUrl);
        
        const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
        const populatedFileName = `${randomGens.generateRandomString()}-populated-${timestamp}.xlsx`;
        const populatedFilePath = path.join(template.outputDir, populatedFileName);
        await this.populateExcelTemplate(excelPayload, template.templatePath, populatedFilePath);


        if (!fs.existsSync(populatedFilePath)) {
            throw new Error(`File not found: ${populatedFilePath}`);
        }
        
        const stats = fs.statSync(populatedFilePath);
        if (stats.size === 0) {
            throw new Error('Populated XLSX file is empty (0 bytes)');
        }

        const fileBuffer = fs.readFileSync(populatedFilePath);
        const afterNotificationId = await this.latestMatchingNotificationId(templateName);
        const uploadFile = await this.FileUploadRequest.post(uploadUrl, {
            multipart: {
                file: {
                    name: path.basename(populatedFilePath),
                    mimeType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
                    buffer: fileBuffer
                },
                _boundaryProbe: 'ping'
            }
        });

        try {
            if (fs.existsSync(template.templatePath)) {
                fs.unlinkSync(template.templatePath);
            }
            if (fs.existsSync(populatedFilePath)) {
                fs.unlinkSync(populatedFilePath);
            }
        } catch (cleanupError) {
            console.warn('Failed to cleanup files:', cleanupError);
        }

        let rawBody: string;
        try {
            rawBody = await uploadFile.text();
        } catch (e) {
            rawBody = '';
        }
        
        let uploadResponse: any;
        if (rawBody && /^[{\[]/.test(rawBody.trim())) {
            try {
                uploadResponse = JSON.parse(rawBody);
            } catch (parseErr) {
                uploadResponse = { parseError: (parseErr as Error).message, raw: rawBody };
            }
        } else {
            uploadResponse = { raw: rawBody };
        }

        if (!uploadFile.ok()) {
            console.error('Upload failed status:', uploadFile.status(), uploadFile.statusText());
            console.error('Upload diagnostics:', {
                url: uploadFile.url(),
                fileName: path.basename(populatedFilePath),
                fileSize: stats.size,
                response: uploadResponse,
            });
            throw new Error(`Upload failed ${uploadFile.status()} ${uploadFile.statusText()}`);
        }

        const reportResult = await this.checkReport(templateName, afterNotificationId);
        return {result: reportResult, uploadedFileId: uploadResponse?.id || null};
    }

    /**
     * Uploads an already generated payment mass-import txt file (bank.txt or paymentPartner.txt),
     * then waits for process completion and checks the error report.
     */
    public async uploadPayment(
        fileType: 'bank' | 'paymentPartner',
        collectionChannelId: number | string,
    ) {
        const fileName = fileType === 'bank' ? 'bank.txt' : 'paymentPartner.txt';
        const filePath = path.resolve(__dirname, '../output', fileName);

        if (!fs.existsSync(filePath)) {
            throw new Error(`Payment mass import file not found: ${filePath}. Generate it first.`);
        }

        const stats = fs.statSync(filePath);
        if (stats.size === 0) {
            throw new Error(`Payment mass import file is empty (0 bytes): ${filePath}`);
        }

        const today = randomGens.generateTodaysDate('yyyy-mm-dd');
        const uploadUrl =
            `mass-import/PAYMENT/files/upload?collectionChannelId=${collectionChannelId}&date=${today}`;
        const fileBuffer = fs.readFileSync(filePath);

        const uploadFile = await this.FileUploadRequest.post(uploadUrl, {
            multipart: {
                file: {
                    name: fileName,
                    mimeType: 'text/plain',
                    buffer: fileBuffer,
                },
                _boundaryProbe: 'ping',
            },
        });

        let rawBody: string;
        try {
            rawBody = await uploadFile.text();
        } catch {
            rawBody = '';
        }

        let uploadResponse: any;
        if (rawBody && /^[{\[]/.test(rawBody.trim())) {
            try {
                uploadResponse = JSON.parse(rawBody);
            } catch (parseErr) {
                uploadResponse = { parseError: (parseErr as Error).message, raw: rawBody };
            }
        } else {
            uploadResponse = { raw: rawBody };
        }

        if (!uploadFile.ok()) {
            console.error('Payment mass import upload failed:', uploadFile.status(), uploadFile.statusText());
            console.error('Upload diagnostics:', {
                url: uploadFile.url(),
                fileName,
                fileSize: stats.size,
                collectionChannelId,
                date: today,
                response: uploadResponse,
            });
            throw new Error(
                `Payment mass import upload failed ${uploadFile.status()} ${uploadFile.statusText()}`,
            );
        }

        const reportResult = await this.checkReport('PAYMENT');
        return {
            result: reportResult,
            uploadedFileId: uploadResponse?.id || null,
            fileName,
            collectionChannelId,
            date: today,
        };
    }
}