import { envVariables } from "../../../../fixtures/envCashed";
import { randomGens } from "../../../../utils/randomGens";

type PenaltyPayer = 'CUSTOMER' | 'SUPPLIER' | string;
type ContractType = 'PRODUCT_CONTRACT' | 'SERVICE_CONTRACT' | string;

interface ActionPayload {
    actionTypeId: number;
    noticeReceivingDate: string; // yyyy-mm-dd
    executionDate: string;       // yyyy-mm-dd
    penaltyClaimAmount: number | null;
    penaltyClaimAmountCurrencyId: number | null;
    penaltyPayer: PenaltyPayer;
    dontAllowAutomaticPenaltyClaim: boolean;
    penaltyId: number | null;
    terminationId: number | null;
    withoutAutomaticTermination: boolean;
    withoutPenalty: boolean;
    additionalInformation: string | null;
    customerId: number | null;
    contractId: number | null;
    contractType: ContractType;
    files: any[];
    pods: any[];
    templateId: number | null;
    emailTemplateId: number | null;
}

export function action(): ActionPayload {
    return {
        "actionTypeId": 1,
        "noticeReceivingDate": randomGens.generateTodaysDate('yyyy-mm-dd'),
        "executionDate": randomGens.generateTodaysDate('yyyy-mm-dd'),
        "penaltyClaimAmount": null,
        "penaltyClaimAmountCurrencyId": null,
        "penaltyPayer": "CUSTOMER",
        "dontAllowAutomaticPenaltyClaim": false,
        "penaltyId": null,
        "terminationId": null,
        "withoutAutomaticTermination": false,
        "withoutPenalty": false,
        "additionalInformation": null,
        "customerId": null,
        "contractId": null,
        "contractType": "PRODUCT_CONTRACT",
        "files": [],
        "pods": [],
        "templateId": envVariables.penalty_document_template,
        "emailTemplateId": envVariables.penalty_email_template
    }
}