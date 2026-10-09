import { envVariables } from "../../../../fixtures/envCashed";
import { randomGens } from "../../../../utils/randomGens";

type Status = 'ACTIVE' | 'INACTIVE' | 'NEW';

type Calendar = {
    id: number;
    name: string;
    defaultSelection: boolean;
    status: Status;
    orderingId: number;
    weekends: string[];
    holidays: string[];
}

type InvoicePaymentTerm = {
    id: number | null;
    calendarName: string;
    calendarType: 'WORKING_DAYS' | 'CALENDAR_DAYS' | string;
    value: number;
    valueFrom: number | null;
    valueTo: number | null;
    excludeWeekends: boolean;
    excludeHolidays: boolean;
    dueDateChange: unknown;
    name: string;
    calendar: Calendar;
    excludes: unknown[];
    type: 'WORKING_DAYS' | 'CALENDAR_DAYS' | string;
    calendarId: number;
}

type Terms = {
    name: string;
    contractDeliveryActivationValue: number | null;
    contractDeliveryActivationType: string | null;
    contractDeliveryActivationAutoTermination: boolean;
    resigningDeadlineValue: number | null;
    resigningDeadlineType: string | null;
    supplyActivations: string[];
    supplyActivationExactDateStartDay: string | null;
    generalNoticePeriodValue: number | null;
    generalNoticePeriodType: string | null;
    noticeTermPeriodValue: number | null;
    noticeTermPeriodType: string | null;
    noticeTermDisconnectionPeriodValue: number | null;
    noticeTermDisconnectionPeriodType: string | null;
    contractEntryIntoForces: string[];
    contractEntryIntoForceFromExactDayOfMonthStartDay: string | null;
    waitForOldContractTermToExpires: unknown[];
    noInterestOnOverdueDebts: boolean;
    startsOfContractInitialTerms: string[];
    startDayOfInitialContractTerm: string | null;
    firstDayOfTheMonthOfInitialContractTerm: string | null;
    invoicePaymentTerms: InvoicePaymentTerm[];
}

export function terms(): Terms {
    return {
            "name": randomGens.generateRandomString(),
            "contractDeliveryActivationValue": null,
            "contractDeliveryActivationType": null,
            "contractDeliveryActivationAutoTermination": false,
            "resigningDeadlineValue": null,
            "resigningDeadlineType": null,
            "supplyActivations": [
                "MANUAL"
        ],
            "supplyActivationExactDateStartDay": null,
            "generalNoticePeriodValue": null,
            "generalNoticePeriodType": null,
            "noticeTermPeriodValue": null,
            "noticeTermPeriodType": null,
            "noticeTermDisconnectionPeriodValue": null,
            "noticeTermDisconnectionPeriodType": null,
            "contractEntryIntoForces": [
                "MANUAL"
        ],
            "contractEntryIntoForceFromExactDayOfMonthStartDay": null,
            "waitForOldContractTermToExpires": [],
            "noInterestOnOverdueDebts": false,
            "startsOfContractInitialTerms": [
                "MANUAL"
        ],
            "startDayOfInitialContractTerm": null,
            "firstDayOfTheMonthOfInitialContractTerm": null,
            "invoicePaymentTerms": [
            {
                "id": null,
                "calendarName": "automation calendar",
                "calendarType": "WORKING_DAYS",
                "value": 10,
                "valueFrom": null,
                "valueTo": null,
                "excludeWeekends": false,
                "excludeHolidays": false,
                "dueDateChange": null,
                "name": "10 WORKING_DAYS ( )",
                "calendar": {
                    "id": envVariables.calendar.id,
                    "name": "automation calendar",
                    "defaultSelection": true,
                    "status": "ACTIVE",
                    "orderingId": envVariables.calendar.orderingId,
                    "weekends": [
                        "MONDAY",
                        "TUESDAY",
                        "WEDNESDAY",
                        "THURSDAY",
                        "FRIDAY"
                    ],
                    "holidays": []
                },
                "excludes": [],
                "type": "WORKING_DAYS",
                "calendarId": envVariables.calendar.id
            }
        ]
    }
}
