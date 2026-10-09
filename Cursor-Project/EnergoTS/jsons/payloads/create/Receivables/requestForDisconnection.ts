import { envVariables } from "../../../../fixtures/envCashed";
import { randomGens } from "../../../../utils/randomGens";

type SupplierType = "CURRENT" | "PREVIOUS";
type DisconnectionRequestsStatus = "EXECUTED" | "PENDING" | "CANCELLED";
type ConditionType = "ALL_CUSTOMERS" | "LIST_OF_CUSTOMERS" | "CUSTOMERS_UNDER_CONDITIONS";

type RequestForDisconnectionType = {
    supplierType: SupplierType;
    disconnectionRequestsStatus: DisconnectionRequestsStatus;
    gridOperatorId: number;
    reasonOfDisconnectionId: number;
    gridOpRequestRegDate: string;
    customerReminderLetterSentDate: string | null;
    gridOpDisconnectionFeePayDate: string | null;
    powerSupplyDisconnectionDate: string | null;
    liabilityAmountFrom: number | null;
    liabilityAmountTo: number | null;
    conditionType: ConditionType;
    condition: any | null;
    listOfCustomer: string | null;
    reminderForDisconnectionId: number | null;
    templateIds: number[];
    pods: any[];
    excludePodIds: number[];
    allSelected: boolean;
    podWithHighestConsumption: boolean;
    files: any[];
};

export function requestForDisconnection(): RequestForDisconnectionType {
    return {
        "supplierType": "CURRENT",
        "disconnectionRequestsStatus": "EXECUTED",
        "gridOperatorId": envVariables.grid_operator,
        "reasonOfDisconnectionId": envVariables.reason_for_disconnection,
        "gridOpRequestRegDate": "2026-01-23",
        "customerReminderLetterSentDate": null,
        "gridOpDisconnectionFeePayDate": null,
        "powerSupplyDisconnectionDate": null,
        "liabilityAmountFrom": null,
        "liabilityAmountTo": null,
        "conditionType": "ALL_CUSTOMERS",
        "condition": null,
        "listOfCustomer": null,
        "reminderForDisconnectionId": null,
        "templateIds": [
            envVariables.request_disconnection_document_template
        ],
        "pods": [],
        "excludePodIds": [],
        "allSelected": true,
        "podWithHighestConsumption": false,
        "files": []
    }
}