import { envVariables } from "../../../fixtures/envCashed";
import { randomGens } from "../../../utils/randomGens";

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

function daysBefore(isoDate: string, days: number): string {
    const [year, month, day] = isoDate.split('-').map(Number);
    const date = new Date(year, month - 1, day);
    date.setDate(date.getDate() - days);
    const yyyy = date.getFullYear();
    const mm = String(date.getMonth() + 1).padStart(2, '0');
    const dd = String(date.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
}

export function action(): ActionPayload {
    const executionDate = randomGens.generateTodaysDate('yyyy-mm-dd');
    return {
        "actionTypeId": 1,
        "noticeReceivingDate": daysBefore(executionDate, 20),
        "executionDate": executionDate,
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