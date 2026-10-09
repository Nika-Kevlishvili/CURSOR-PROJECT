import { envVariables } from "../../../fixtures/envCashed";
import { randomGens } from "../../../utils/randomGens";

type TerminationEvent = 'EXPIRATION_OF_THE_CONTRACT_TERM' | 'EXPIRATION_OF_THE_NOTICE' | 'DEACTIVATION_OF_POINTS_OF_DELIVERY';
type AutoTerminationFrom = 'EVENT_DATE' | 'CONTRACT_END_DATE' | 'NOTICE_DATE';
type CalculateFrom = 'CONTRACT_END_DATE' | 'EVENT_DATE' | 'NOTICE_DATE';
type NoticeDueType = 'DAY' | 'MONTH' | 'YEAR';
type TerminationChannel = 'EMAIL' | 'SMS' | 'PHONE' | 'OTHER' | 'PAPER';

type TerminationPayload = {
    name: string;
    contractClauseNumber: string;
    autoTermination: boolean;
    autoTerminationFrom: AutoTerminationFrom;
    calculateFrom: CalculateFrom;
    noticeDue: boolean;
    noticeDueValueMin: string | number | null;
    noticeDueValueMax: string | number | null;
    noticeDueType: NoticeDueType;
    autoEmailNotification: boolean;
    terminationNotificationChannels: TerminationChannel[];
    additionalInfo: string | null;
    event: TerminationEvent;
    templateId: number;
}

export function termination_payload(): TerminationPayload {
    return {
        "name": randomGens.generateRandomString(true,false,10),
        "contractClauseNumber": "automation",
        "autoTermination": true,
        "autoTerminationFrom": "EVENT_DATE",
        "calculateFrom": "CONTRACT_END_DATE",
        "noticeDue": true,
        "noticeDueValueMin": "1",
        "noticeDueValueMax": null,
        "noticeDueType": "DAY",
        "autoEmailNotification": false,
        "terminationNotificationChannels": [
            "EMAIL",
            "SMS",
            "PHONE",
            "OTHER",
            "PAPER"
        ],
        "additionalInfo": null,
        "event": "EXPIRATION_OF_THE_CONTRACT_TERM",
        "templateId": envVariables.termination_email_template
    }
}