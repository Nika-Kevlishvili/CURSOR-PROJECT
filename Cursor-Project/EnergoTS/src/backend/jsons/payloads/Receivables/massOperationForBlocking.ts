import { envVariables } from "../../../fixtures/envCashed";
import { randomGens } from "../../../utils/randomGens";


type ReceivableBlockingType = "CUSTOMER_LIABILITY" | "CUSTOMER_RECEIVABLE" | "PAYMENT";
type ReceivableBlockingConditionType = "LIST_OF_CUSTOMERS" | "ALL_CUSTOMERS" | "CUSTOMERS_UNDER_CONDITIONS";
type ReasonType =
  | "BLOCKED_FOR_PAYMENT"
  | "BLOCKED_FOR_REMINDER_LETTERS"
  | "BLOCKED_FOR_CALC_LATE_PAYMENT_FINES_INTERESTS"
  | "BLOCKED_FOR_LIABILITIES_OFFSETTING"
  | "BLOCKED_FOR_SUPPLY_TERMINATION";
type RequestReceivableBlockingStatus = "DRAFT" | "EXECUTED";

type BlockingDetails = {
  fromDate: string | any;
  toDate: string | null;
  reasonId: string | number | null;
  additionalInformation: string | null;
  reasonType: ReasonType;
};

type MassOperationForBlocking = {
  name: string;
  receivableBlockingTypes: ReceivableBlockingType[];
  receivableBlockingConditionType: ReceivableBlockingConditionType;
  prefixNomenclatureIds: string[] | null;
  conditions: any;
  listOfCustomers: string[] | null;
  exclusionByAmount: any;
  isBlockForPayment: boolean;
  blockingForPayment: BlockingDetails | null;
  isBlockForReminderLetters: boolean;
  blockingForReminderLetters: BlockingDetails | null;
  isBlockForCalculation: boolean;
  blockingForCalculation: BlockingDetails | null;
  isBlockForLiabilitiesOffsetting: boolean;
  blockingForLiabilitiesOffsetting: BlockingDetails | null;
  isBlockForSupplyTermination: boolean;
  blockingForSupplyTermination: BlockingDetails | null;
  requestReceivableBlockingStatus: RequestReceivableBlockingStatus;
  taskIds: string[];
};

export function mass_operation_for_blocking(): MassOperationForBlocking {
    return {
        "name": `MOFB${randomGens.generateCurrentTimeStamp()}`,
        "receivableBlockingTypes": [
            "CUSTOMER_LIABILITY",
            "CUSTOMER_RECEIVABLE",
            "PAYMENT"
        ],
        "receivableBlockingConditionType": "LIST_OF_CUSTOMERS",
        "prefixNomenclatureIds": null,
        "conditions": null,
        "listOfCustomers": null,
        "exclusionByAmount": null,
        "isBlockForPayment": false,
        "blockingForPayment": {
            "fromDate": null,
            "toDate": null,
            "reasonId": envVariables.blocking_reason,
            "additionalInformation": null,
            "reasonType": "BLOCKED_FOR_PAYMENT"
        },
        "isBlockForReminderLetters": false,
        "blockingForReminderLetters": {
            "fromDate": null,
            "toDate": null,
            "reasonId": envVariables.blocking_reason,
            "additionalInformation": null,
            "reasonType": "BLOCKED_FOR_REMINDER_LETTERS"
        },
        "isBlockForCalculation": false,
        "blockingForCalculation": {
            "fromDate": null,
            "toDate": null,
            "reasonId": envVariables.blocking_reason,
            "additionalInformation": null,
            "reasonType": "BLOCKED_FOR_CALC_LATE_PAYMENT_FINES_INTERESTS"
        },
        "isBlockForLiabilitiesOffsetting": false,
        "blockingForLiabilitiesOffsetting": {
            "fromDate": null,
            "toDate": null,
            "reasonId": envVariables.blocking_reason,
            "additionalInformation": null,
            "reasonType": "BLOCKED_FOR_LIABILITIES_OFFSETTING"
        },
        "isBlockForSupplyTermination": false,
        "blockingForSupplyTermination": {
            "fromDate": null,
            "toDate": null,
            "reasonId": envVariables.blocking_reason,
            "additionalInformation": null,
            "reasonType": "BLOCKED_FOR_SUPPLY_TERMINATION"
        },
        "requestReceivableBlockingStatus": "EXECUTED",
        "taskIds": []
    }
}