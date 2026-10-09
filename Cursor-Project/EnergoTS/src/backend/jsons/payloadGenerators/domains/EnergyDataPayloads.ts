
import { APIRequestContext } from "playwright";
import { baseFixture } from "../../../fixtures/baseFixture";
import { profile15minute } from '../../payloads/energyData/profile15Minute';
import { profile1Day } from '../../payloads/energyData/profile1Day';
import { profile1Hour } from '../../payloads/energyData/profile1Hour';
import { profile1Month } from '../../payloads/energyData/profile1Month';
import { scales } from '../../payloads/energyData/scales';
import { discount } from '../../payloads/energyData/discount';
import { compensation } from '../../payloads/energyData/compensation';
import { scaleCode } from '../../payloads/energyData/dataByScales(ScaleCode)';
import excelGens from "../../../mass-imports/generators/domains/priceParameterMP";
import { randomUUID } from 'crypto';
import * as fs from 'fs';
import * as os from 'os';
import * as path from 'path';
import {randomGens} from '../../../utils/randomGens';

type ProfileDateRange = {
    startDate: string;
    endDate: string;
    amount: number;
};

export class EnergyDataPayloads {
    private Request: APIRequestContext;
    private fileUploadRequest: APIRequestContext;
    private responses: baseFixture['Responses'];
    private generatedProfileFiles = new Map<string, string>();

    constructor(apiRequestContext: APIRequestContext, responses: baseFixture['Responses'], fileUploadRequest: APIRequestContext) {
        this.Request = apiRequestContext;
        this.fileUploadRequest = fileUploadRequest;
        this.responses = responses;
    }

    private async generateProfileWithExcel(
        payload: any,
        podIndex: number,
        dateRanges: ProfileDateRange[] | undefined,
        generatorMethod: 'generateFifteenMinutes' | 'generateHours' | 'generateDays',
        endTimeHours?: number,
        endTimeMinutes?: number
    ) {
        const podget = await this.Request.get(`pod/${this.responses.pod[podIndex].id}?version=1`);
        const podjson = await podget.json();
        payload.identifier = podjson.identifier;
        
        let defaultEndDate: string;
        if (endTimeHours !== undefined || endTimeMinutes !== undefined) {
            const endDate = new Date(payload.periodTo.split('T')[0]);
            endDate.setHours(endTimeHours ?? 23, endTimeMinutes ?? 0, 0, 0);
            defaultEndDate = endDate.toISOString();
        } else {
            defaultEndDate = payload.periodTo.split('T')[0];
        }
        
        const ranges = dateRanges && dateRanges.length > 0 
            ? dateRanges 
            : [{ startDate: payload.periodFrom.split('T')[0], endDate: defaultEndDate, amount: 100 }];
        
        if (dateRanges && dateRanges.length > 0) {
            payload.periodFrom = dateRanges[0].startDate;
            payload.periodTo = dateRanges[dateRanges.length - 1].endDate;
        }
        
        const profileTypeByGenerator = {
            generateFifteenMinutes: 'MIN',
            generateHours: 'HOUR',
            generateDays: 'DAY',
        } as const;
        const profileType = profileTypeByGenerator[generatorMethod];
        const outputPath = path.join(
            os.tmpdir(),
            `energo-profile-${profileType.toLowerCase()}-${process.pid}-${randomUUID()}.xlsx`
        );
        const generator = new excelGens(ranges, { outputPath });
        await generator[generatorMethod]();
        this.generatedProfileFiles.set(profileType, outputPath);
        
        return payload;
    }

    public async profile15minute(podIndex: number = 0, dateRanges?: ProfileDateRange[]) {
        return this.generateProfileWithExcel(
            { ...profile15minute },
            podIndex,
            dateRanges,
            'generateFifteenMinutes',
            23,
            45
        );
    }

    public async profile1Day(podIndex: number = 0, dateRanges?: ProfileDateRange[]) {
        return this.generateProfileWithExcel(
            { ...profile1Day },
            podIndex,
            dateRanges,
            'generateDays'
        );
    }

    public async profile1Hour(podIndex: number = 0, dateRanges?: ProfileDateRange[]) {
        return this.generateProfileWithExcel(
            { ...profile1Hour },
            podIndex,
            dateRanges,
            'generateHours',
            23,
            0
        );
    }

    public async profile1Month(podIndex: number = 0, dateRanges?: { startDate: string; endDate: string }[]) {
        const payload = profile1Month();
        const podget = await this.Request.get(`pod/${this.responses.pod[podIndex].id}?version=1`);
        const podjson = await podget.json();

        payload.identifier = podjson.identifier;
        
        if (dateRanges && dateRanges.length > 0) {
            payload.periodFrom = `${dateRanges[0].startDate}T00:00:00.000Z`;
            payload.periodTo = `${dateRanges[0].endDate}T00:00:00.000Z`;
            payload.entries[0].periodFrom = `${dateRanges[0].startDate}T00:00:00.000Z`;
        }
        
        return payload;
    }

    public async uploadDataByProfileFile(dataByProfileId: number, profileType: string) {
        let filename = '';
        let periodTypeParam = '';
        
        if (profileType === 'MIN') {
            filename = '15min.xlsx';
            periodTypeParam = 'FIFTEEN_MINUTES';
        } else if (profileType === 'HOUR') {
            filename = '1h.xlsx';
            periodTypeParam = 'ONE_HOUR';
        } else if (profileType === 'DAY') {
            filename = '1D.xlsx';
            periodTypeParam = 'ONE_DAY';
        }

        const generatedFile = this.generatedProfileFiles.get(profileType);
        const filePath = generatedFile ?? filename;

        try {
            const fileBuffer = fs.readFileSync(filePath);
            const fileUpload = await this.fileUploadRequest.post(`billing-by-profile/import?periodType=${periodTypeParam}&billingByProfileId=${dataByProfileId}`, {
                multipart: {
                    file: {
                        name: filename,
                        mimeType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
                        buffer: fileBuffer
                    }
                }
            });
            if (!fileUpload.ok()) {
                throw new Error(`File upload failed: ${fileUpload.status()} ${await fileUpload.text()}`);
            }
        } finally {
            if (generatedFile) {
                fs.rmSync(generatedFile, { force: true });
                this.generatedProfileFiles.delete(profileType);
            }
        }
    }

    public scales() {
        const payload = scales();
        return payload;
    }

    public async scaleTariff(podIndex: number = 0) {
        const meterGet = await this.Request.get(`meters/${this.responses.meters[0]}?versionId=1`)
        const meterJsonBody = await meterGet.json();
        const scale = await this.Request.get(`scales/${meterJsonBody.meterScales[0].id}`)
        const scaleJsonBody = await scale.json();
        const scaleTariffResponse = await this.Request.get(`scales/${meterJsonBody.meterScales[1].id}`)
        const scaleJsonBodyTariff = await scaleTariffResponse.json();
        const podGet = await this.Request.get(`pod/${(this.responses.pod[podIndex].id)}?versionId=1`)
        const podJsonBody = await podGet.json();

        const payload = scales();
        //first row - tariff (only tariffScale, no scaleCode)
        payload.identifier = podJsonBody.identifier;
        payload.billingByScalesTableCreateRequests[0].meterNumber = meterJsonBody.number;
        payload.billingByScalesTableCreateRequests[0].scaleType = scaleJsonBodyTariff.scaleType;
        payload.billingByScalesTableCreateRequests[0].tariffScale = scaleJsonBodyTariff.tariffOrScale;
        //second row - scale code (only scaleCode, no tariffScale - they are mutually exclusive)
        payload.billingByScalesTableCreateRequests[1].meterNumber = meterJsonBody.number;
        payload.billingByScalesTableCreateRequests[1].scaleCode = scaleJsonBody.scaleCode;
        payload.billingByScalesTableCreateRequests[1].scaleNumber = randomGens.getRandomEven().toString();
        payload.billingByScalesTableCreateRequests[1].scaleType = scaleJsonBody.scaleType;
        
        //third row - scale code (only scaleCode, no tariffScale - they are mutually exclusive)
        payload.billingByScalesTableCreateRequests[2].meterNumber = meterJsonBody.number;
        payload.billingByScalesTableCreateRequests[2].scaleCode = scaleJsonBody.scaleCode;
        payload.billingByScalesTableCreateRequests[2].scaleType = scaleJsonBody.scaleType;
        payload.billingByScalesTableCreateRequests[2].scaleNumber = randomGens.getRandomEven().toString();

        return payload;
    }
    
    public async scaleCode(podIndex: number = 0, rangeFrom?: string, rangeTo?: string) {
        const meterGet = await this.Request.get(`meters/${this.responses.meters[0]}?versionId=1`)
        const meterJsonBody = await meterGet.json();
        const scale = await this.Request.get(`scales/${meterJsonBody.meterScales[0].id}`)
        const scaleJsonBody = await scale.json();
        const podGet = await this.Request.get(`pod/${(this.responses.pod[podIndex].id)}?versionId=1`)
        const podJsonBody = await podGet.json();
        const payload = scaleCode();

        payload.identifier = podJsonBody.identifier;
        payload.billingByScalesTableCreateRequests[0].meterNumber = meterJsonBody.number;
        payload.billingByScalesTableCreateRequests[0].scaleCode = scaleJsonBody.scaleCode;
        payload.billingByScalesTableCreateRequests[0].scaleType = scaleJsonBody.scaleType;
        
        const oldMeterReading = randomGens.getRandomEven();
        const newMeterReading = oldMeterReading + randomGens.getRandomOdd();
        const difference = newMeterReading - oldMeterReading;
        const multiplier = randomGens.getRandomEven();
        const total = difference * multiplier;
        payload.billingByScalesTableCreateRequests[0].multiplier = multiplier.toString();
        payload.billingByScalesTableCreateRequests[0].difference = difference.toString();
        payload.billingByScalesTableCreateRequests[0].oldMeterReading = oldMeterReading.toString();
        payload.billingByScalesTableCreateRequests[0].newMeterReading = newMeterReading.toString();
        payload.billingByScalesTableCreateRequests[0].totalVolumes = total.toString();

        if (rangeFrom && rangeTo) {
            payload.dateFrom = rangeFrom;
            payload.dateTo = rangeTo;
            payload.billingByScalesTableCreateRequests[0].periodFrom = rangeFrom;
            payload.billingByScalesTableCreateRequests[0].periodTo = rangeTo;
        }

        return payload;
    }

    
     public async scaleZero(podIndex: number = 0) {
        const meterGet = await this.Request.get(`meters/${this.responses.meters[0]}?versionId=1`)
        const meterJsonBody = await meterGet.json();
        const scale = await this.Request.get(`scales/${meterJsonBody.meterScales[0].id}`)
        const scaleJsonBody = await scale.json();
        const scaleTariffResponse = await this.Request.get(`scales/${meterJsonBody.meterScales[1].id}`)
        const scaleJsonBodyTariff = await scaleTariffResponse.json();
        const podGet = await this.Request.get(`pod/${(this.responses.pod[podIndex].id)}?versionId=1`)
        const podJsonBody = await podGet.json();

        const payload = scales();
        //first row - tariff (only tariffScale, no scaleCode)
        payload.identifier = podJsonBody.identifier;
        payload.billingByScalesTableCreateRequests[0].meterNumber = meterJsonBody.number;
        payload.billingByScalesTableCreateRequests[0].scaleType = scaleJsonBodyTariff.scaleType;
        payload.billingByScalesTableCreateRequests[0].tariffScale = scaleJsonBodyTariff.tariffOrScale;

        //second row - scale code (only scaleCode, no tariffScale - they are mutually exclusive)
        payload.billingByScalesTableCreateRequests[1].multiplier = "0";
        payload.billingByScalesTableCreateRequests[1].totalVolumes = "0";
        payload.billingByScalesTableCreateRequests[1].meterNumber = meterJsonBody.number;
        payload.billingByScalesTableCreateRequests[1].scaleCode = scaleJsonBody.scaleCode;
        payload.billingByScalesTableCreateRequests[1].scaleNumber = randomGens.getRandomEven().toString();
        payload.billingByScalesTableCreateRequests[1].scaleType = scaleJsonBody.scaleType;
        
        //third row - scale code (only scaleCode, no tariffScale - they are mutually exclusive)
        payload.billingByScalesTableCreateRequests[2].multiplier = "0";
        payload.billingByScalesTableCreateRequests[2].totalVolumes = "0";
        payload.billingByScalesTableCreateRequests[2].meterNumber = meterJsonBody.number;
        payload.billingByScalesTableCreateRequests[2].scaleCode = scaleJsonBody.scaleCode;
        payload.billingByScalesTableCreateRequests[2].scaleType = scaleJsonBody.scaleType;
        payload.billingByScalesTableCreateRequests[2].scaleNumber = randomGens.getRandomEven().toString();

        return payload;
    }

    public async discount() {
        const payload = discount();
        const podGet = await this.Request.get(`pod/${(this.responses.pod[0].id)}?versionId=1`)
        const podJsonBody = await podGet.json();

        const getCustomer = await this.Request.get(`customer/${this.responses.customer[0].id}?version=1`);
        const getCustomerResponse = await getCustomer.json();
        payload.customerId = getCustomerResponse.customerId;
        payload.pointOfDeliveryIds = [podJsonBody.id];
        console.log(JSON.stringify(payload, null, 2));
        return payload;

    }

    public compensation() {
        const payload = compensation();
        payload.customerId = this.responses.customer[0].id;
        payload.podId = this.responses.pod[0].id;
        payload.recipientId = this.responses.customer[1].id;
        return payload;
    }
}