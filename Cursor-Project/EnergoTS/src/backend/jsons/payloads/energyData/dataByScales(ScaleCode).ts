import { envVariables } from "../../../fixtures/envCashed";
import { randomGens } from "../../../utils/randomGens";

type BillingByScalesTableCreateRequest = {
    periodFrom: string;
    periodTo: string;
    meterNumber: string | number | null;
    scaleType: string | null;
    scaleNumber: string | null;
    scaleCode: string | null;
    newMeterReading: string;
    oldMeterReading: string;
    difference: string;
    multiplier: string;
    correction: string | null;
    deducted: string | null;
    totalVolumes: string;
    index: number;
};

type Scales = {
    identifier: string | number | null;
    dateFrom: string;
    dateTo: string;
    billingPowerInKw: string;
    numberOfDays: number;
    invoiceNumber: string;
    invoiceDate: string;
    invoiceCorrection: string | null;
    correction: boolean;
    basisForIssuingTheInvoice: string | null;
    billingByScalesTableCreateRequests: BillingByScalesTableCreateRequest[];
    saveRecordForIntermediatePeriod: boolean;
    saveRecordForMeterReadings: boolean;
    override: boolean;
};

export function scaleCode(): Scales {
    return {
    "identifier": null,
    "dateFrom": randomGens.generateMonthStartDate('yyyy-mm-dd'),
    "dateTo": randomGens.generateMonthEndDate('yyyy-mm-dd'),
    "billingPowerInKw": "2",
    "numberOfDays": 31,
    "invoiceNumber": "2",
    "invoiceDate": `${randomGens.generateMonthStartDate('yyyy-mm-dd')}T00:00:00.000Z`,
    "invoiceCorrection": null,
    "correction": false,
    "basisForIssuingTheInvoice": null,
    "billingByScalesTableCreateRequests": [
        {
            "periodFrom": randomGens.generateMonthStartDate('yyyy-mm-dd'),
            "periodTo": randomGens.generateMonthEndDate('yyyy-mm-dd'),
            "meterNumber": null, //from meter
            "scaleNumber": "2",
            "scaleCode": null, //from scale nomenclature
            "scaleType": null, //from scale nomenclature
            "newMeterReading": "5", 
            "oldMeterReading": "0", 
            "difference": "5",
            "multiplier": "8",
            "correction": null,
            "deducted": null,
            "totalVolumes": "40",
            "index": 0
        }
    ],
    "saveRecordForIntermediatePeriod": false,
    "saveRecordForMeterReadings": false,
    "override": false
}
}