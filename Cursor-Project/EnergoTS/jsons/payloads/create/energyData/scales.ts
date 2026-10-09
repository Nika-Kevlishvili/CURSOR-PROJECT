import { envVariables } from "../../../../fixtures/envCashed";
import { randomGens } from "../../../../utils/randomGens";

type BillingByScalesTableCreateRequest = {
    periodFrom: string;
    periodTo: string;
    meterNumber: string | number | null;
    scaleType: string | null;
    tariffScale: string | null;
    volumes?: string;
    unitPrice?: string;
    totalValue?: string;
    scaleNumber?: string | null;
    scaleCode?: string | null;
    newMeterReading?: string;
    oldMeterReading?: string;
    difference?: string;
    multiplier?: string;
    correction?: string | null;
    deducted?: string | null;
    totalVolumes?: string;
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
    scaleNumber?: string | null;
    scaleCode?: string | null;
    billingByScalesTableCreateRequests: BillingByScalesTableCreateRequest[];
    saveRecordForIntermediatePeriod: boolean;
    saveRecordForMeterReadings: boolean;
    override: boolean;
};

export function scales(): Scales {
    return {
        "identifier": null,
        "dateFrom": randomGens.generateMonthStartDate('yyyy-mm-dd'),
        "dateTo": randomGens.generateMonthEndDate('yyyy-mm-dd'),
        "billingPowerInKw": "1",
        "numberOfDays": 31,
        "invoiceNumber": "1",
        "invoiceDate": `${randomGens.generateMonthStartDate('yyyy-mm-dd')}T00:00:00.000Z`,
        "invoiceCorrection": null,
        "correction": false,
        "billingByScalesTableCreateRequests": [
            {
                "periodFrom": randomGens.generateMonthStartDate('yyyy-mm-dd'),
                "periodTo": randomGens.generateMonthEndDate('yyyy-mm-dd'),
                "meterNumber": null,
                "scaleType": null,
                "tariffScale": null,
                "volumes": randomGens.getRandomEven().toString(),
                "unitPrice": "1",
                "totalValue": "1",
                "index": 0
            },
            {
            "periodFrom": randomGens.generateMonthStartDate('yyyy-mm-dd'),
            "periodTo": randomGens.generateMonthHalfDate('yyyy-mm-dd'),
            "meterNumber": null,
            "scaleType": null,
            "tariffScale": null,
            "scaleNumber": null,
            "scaleCode": null,
            "newMeterReading": "20",
            "oldMeterReading": "10",
            "difference": "10",
            "multiplier": "2",
            "correction": null,
            "deducted": null,
            "totalVolumes": "20",
            "index": 1
        },
        {
            "periodFrom": randomGens.generateMonthHalfDatePlusOne('yyyy-mm-dd'),
            "periodTo": randomGens.generateMonthEndDate('yyyy-mm-dd'),
            "meterNumber": null,
            "scaleType": null,
            "tariffScale": null,
            "scaleNumber": null,
            "scaleCode": null,
            "newMeterReading": "30",
            "oldMeterReading": "20",
            "difference": "10",
            "multiplier": "2",
            "correction": null,
            "deducted": null,
            "totalVolumes": "20",
            "index": 2
        },
        
        ],
        "saveRecordForIntermediatePeriod": false,
        "saveRecordForMeterReadings": false,
        "override": false
    }
}