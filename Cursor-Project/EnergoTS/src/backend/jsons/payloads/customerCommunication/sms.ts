import { envVariables } from "../../../fixtures/envCashed";
import { randomGens } from "../../../utils/randomGens";

type SmsPayload = {
	communicationAsInstitution: boolean;
	topicOfCommunicationId: number;
	dateAndTime: string | any;
	exchangeCodeId: number;
	customerPhoneNumber: string;
	communicationType: 'INCOMING' | 'OUTGOING';
	smsBody: string;
	customerDetailId: number;
	saveAs: 'SAVE_AS_DRAFT' | 'SAVE_AND_SEND' | string;
	customerCommunicationId: number;
	relatedCustomerIds: number[];
	communicationFileIds: number[];
	templateId: number | null;
}

export function sms(): SmsPayload {
	return {
		"communicationAsInstitution": true,
		"topicOfCommunicationId": envVariables.topic_of_communication,
		"dateAndTime": null,
		"exchangeCodeId": 11,
		"customerPhoneNumber": "4555",
		"communicationType": "OUTGOING",
		"smsBody": "Description " + randomGens.generateRandomString(true, false, 30),
		"customerDetailId": 947928,
		"saveAs": "SAVE_AS_DRAFT",
		"customerCommunicationId": 898899,
		"relatedCustomerIds": [],
		"communicationFileIds": [],
		"templateId": null
	}
}