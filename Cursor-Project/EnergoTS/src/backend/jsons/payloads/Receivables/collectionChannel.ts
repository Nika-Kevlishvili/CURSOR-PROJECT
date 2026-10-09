import { envVariables } from "../../../fixtures/envCashed";
import { randomGens } from "../../../utils/randomGens";

type CollectionChannel = {
    name: string;
    type: string;
    employee: string | null;
    collectionPartnerId: string | number;
    numberOfIncomeAccount: string | number;
    currencyId: string | number;
    customerConditionType: string;
    condition: string | null;
    listOfCustomers: string[] | null;
    excludeLiabilitiesByPrefix: string | null;
    excludeLiabilitiesByAmount: {
        lessThan: number | null;
        greaterThan: number | null;
    };
    priorityLiabilitiesByPrefix: string | null;
    typeOfFile: string;
    bankIds: (string | number)[];
    globalBank: boolean | string | null;
    dataSendingSchedule: string | null;
    dataReceivingSchedule: string | null;
    numberOfWorkingDays: number | null;
    calendarId: string | number | null;
    waitingPeriodToleranceInHours: number | null;
    folderForFileReceiving: string | null;
    folderForFileSending: string | null;
    emailForFileSending: string | null;
    combineLiabilities: boolean;
};

function offlineCollectionChannel(typeOfFile: string): CollectionChannel {
    return {
        "name": `AUTOMATION${randomGens.generateCurrentTimeStamp()}`,
        "type": "OFFLINE",
        "employee": null,
        "collectionPartnerId": envVariables.collection_partner,
        "numberOfIncomeAccount": 1,
        "currencyId": envVariables.currency,
        "customerConditionType": "ALL_CUSTOMERS",
        "condition": null,
        "listOfCustomers": null,
        "excludeLiabilitiesByPrefix": null,
        "excludeLiabilitiesByAmount": {
            "lessThan": null,
            "greaterThan": null
        },
        "priorityLiabilitiesByPrefix": null,
        "typeOfFile": typeOfFile,
        "bankIds": [],
        "globalBank": null,
        "dataSendingSchedule": null,
        "dataReceivingSchedule": null,
        "numberOfWorkingDays": null,
        "calendarId": null,
        "waitingPeriodToleranceInHours": null,
        "folderForFileReceiving": null,
        "folderForFileSending": null,
        "emailForFileSending": null,
        "combineLiabilities": false
    }
}

export function collection_channel(): CollectionChannel {
    return offlineCollectionChannel("PAYMENT_PARTNER");
}

export function bank_partner_collection_channel(): CollectionChannel {
    const payload = offlineCollectionChannel("BANK_PARTNER");
    payload.bankIds = [envVariables.banks];
    payload.globalBank = false;
    return payload;
}

type CollectionChannelEasyPay = {
    name: string;
    type: string;
    employee: any;
    collectionPartnerId: string | number;
    numberOfIncomeAccount: string | number;
    currencyId: string | number;
    customerConditionType: string;
    condition: string | null;
    listOfCustomers: string[] | null;
    excludeLiabilitiesByPrefix: string [];
    excludeLiabilitiesByAmount: {
        lessThan: number | null;
        greaterThan: number | null;
    };
    priorityLiabilitiesByPrefix: string [];
    typeOfFile: string | null;
    bankIds: (string | number)[] | null;
    globalBank: string | null;
    dataSendingSchedule: string | null;
    dataReceivingSchedule: string | null;
    numberOfWorkingDays: number | null;
    calendarId: string | number | null;
    waitingPeriodToleranceInHours: number | null;
    folderForFileReceiving: string | null;
    folderForFileSending: string | null;
    emailForFileSending: string | null;
    combineLiabilities: boolean;
    performerId: number | null;
    performerType: string|null;
};

export function easyPayCollectionChannel(): CollectionChannelEasyPay {
    return {
        "name": "EasyPay",
        "type": "ONLINE",
        "employee": {
            "id": 1041,
            "performerType": "TAG",
            "name": "group-aaa",
            "nameBg": "group-aaa",
            "portalId": "888fa723-7eef-4b79-9b78-6744b0475057"
        },
        "collectionPartnerId": envVariables.collection_partner,
        "numberOfIncomeAccount": "1",
        "currencyId": envVariables.currency,
        "customerConditionType": "ALL_CUSTOMERS",
        "condition": null,
        "listOfCustomers": null,
        "excludeLiabilitiesByPrefix": [],
        "excludeLiabilitiesByAmount": {
            "lessThan": null,
            "greaterThan": null
        },
        "priorityLiabilitiesByPrefix": [],
        "typeOfFile": null,
        "bankIds": null,
        "globalBank": null,
        "dataSendingSchedule": null,
        "dataReceivingSchedule": null,
        "numberOfWorkingDays": null,
        "calendarId": null,
        "waitingPeriodToleranceInHours": null,
        "folderForFileReceiving": null,
        "folderForFileSending": null,
        "emailForFileSending": null,
        "combineLiabilities": true, /*this is the higlight*/
        "performerId": 1041,
        "performerType": "TAG"
    }
}

export function bulgarianPostChannel(): CollectionChannelEasyPay {
    return {
        "name": "BulgarianPost",
        "type": "ONLINE",
        "employee": null,
        "collectionPartnerId": envVariables.collection_partner,
        "numberOfIncomeAccount": "1",
        "currencyId": envVariables.currency,
        "customerConditionType": "ALL_CUSTOMERS",
        "condition": null,
        "listOfCustomers": null,
        "excludeLiabilitiesByPrefix": [],
        "excludeLiabilitiesByAmount": {
            "lessThan": null,
            "greaterThan": null
        },
        "priorityLiabilitiesByPrefix": [],
        "typeOfFile": null,
        "bankIds": [],
        "globalBank": null,
        "dataSendingSchedule": null,
        "dataReceivingSchedule": null,
        "numberOfWorkingDays": null,
        "calendarId": null,
        "waitingPeriodToleranceInHours": null,
        "folderForFileReceiving": null,
        "folderForFileSending": null,
        "emailForFileSending": null,
        "combineLiabilities": false, /*this is the higlight*/
        "performerId": null,
        "performerType": null
    }
}