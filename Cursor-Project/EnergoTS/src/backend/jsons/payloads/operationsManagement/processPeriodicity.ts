import { envVariables } from "../../../fixtures/envCashed";
import { randomGens } from "../../../utils/randomGens";

type ProcessPeriodicity = {
    name: string,
    ignoreErrors: boolean,
    ignoreWarnings: boolean,
    processPeriodicityType: string,
    incompatibleProcesses: any,
    startAfterProcessId: null | number | string,
    calendarId: boolean | null,
    isWeekendsExcluded: boolean,
    isHolidaysExcluded: boolean,
    changeTo: any,
    startTimeIntervals: Array<{
        startTime: string | null,
        endTime: string | null,
        id: null | number
    }>,
    baseProcessPeriodicityPeriodOptionsDto: {
        processPeriodicityPeriodType: string,
        baseDayOfWeekAndPeriodOfYearDto: {
            daysOfWeek: Array<{
                week: string,
                days: string[],
                id: null | number
            }>,
            yearAround: boolean,
            periodsOfYear: any[]
        }
    }
}


export function process_periodicity(): ProcessPeriodicity {
    return {
        "name": "forRemainder",
        "ignoreErrors": false,
        "ignoreWarnings": false,
        "processPeriodicityType": "PERIODICAL",
        "incompatibleProcesses": [],
        "startAfterProcessId": null,
        "calendarId": null,
        "isWeekendsExcluded": false,
        "isHolidaysExcluded": false,
        "changeTo": null,
        "startTimeIntervals": [
            {
                "startTime": "06:00",
                "endTime": null,
                "id": null
            }
        ],
        "baseProcessPeriodicityPeriodOptionsDto": {
            "processPeriodicityPeriodType": "PERIOD_OF_YEAR",
            "baseDayOfWeekAndPeriodOfYearDto": {
                "daysOfWeek": [
                    {
                        "week": "FIRST_WEEK",
                        "days": [
                            "MONDAY",
                            "TUESDAY",
                            "WEDNESDAY",
                            "THURSDAY",
                            "FRIDAY",
                            "SATURDAY",
                            "SUNDAY"
                        ],
                        "id": null
                    },
                    {
                        "week": "SECOND_WEEK",
                        "days": [
                            "MONDAY",
                            "TUESDAY",
                            "WEDNESDAY",
                            "THURSDAY",
                            "FRIDAY",
                            "SATURDAY",
                            "SUNDAY"
                        ],
                        "id": null
                    },
                    {
                        "week": "THIRD_WEEK",
                        "days": [
                            "MONDAY",
                            "TUESDAY",
                            "WEDNESDAY",
                            "THURSDAY",
                            "FRIDAY",
                            "SATURDAY",
                            "SUNDAY"
                        ],
                        "id": null
                    },
                    {
                        "week": "FOURTH_WEEK",
                        "days": [
                            "MONDAY",
                            "TUESDAY",
                            "WEDNESDAY",
                            "THURSDAY",
                            "FRIDAY",
                            "SATURDAY",
                            "SUNDAY"
                        ],
                        "id": null
                    },
                    {
                        "week": "FIFTH_WEEK",
                        "days": [
                            "MONDAY",
                            "TUESDAY",
                            "WEDNESDAY",
                            "THURSDAY",
                            "FRIDAY",
                            "SATURDAY",
                            "SUNDAY"
                        ],
                        "id": null
                    },
                    {
                        "week": "LAST_WEEK",
                        "days": [
                            "MONDAY",
                            "TUESDAY",
                            "WEDNESDAY",
                            "THURSDAY",
                            "FRIDAY",
                            "SATURDAY",
                            "SUNDAY"
                        ],
                        "id": null
                    }
                ],
                "yearAround": true,
                "periodsOfYear": []
            }
        }
    }
}