import { envVariables } from "../../../fixtures/envCashed";
import { randomGens } from "../../../utils/randomGens";

// -------------------- Type Definitions --------------------
type ValueType = 'EXACT_AMOUNT' | 'PRICE_COMPONENT' | 'PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT';
type PaymentType = 'OBLIGATORY' | 'AT_LEAST_ONE';
type DateOfIssueType = 'MATCH_THE_INVOICE_DATE' | 'DATE_OF_THE_MONTH' | 'WORKING_DAYS_AFTER_INVOICE_DATE' | 'PERIODICAL';
type IssuingForTheMonthToCurrent = 'MINUS_ONE' | 'ZERO' | 'PLUS_ONE' | 'PLUS_TWO' | 'PLUS_TWELVE';
type DeductionFrom = 'FIRST_INVOICE_FOR_SAME_PERIOD' | 'FIRST_INVOICE_WITH_LONGER_PAYMENT_TERM';
type PeriodType = 'DAY_OF_WEEK_AND_PERIOD_OF_YEAR' | 'DAY_OF_MONTH';
type CalendarType = 'WORKING_DAYS' | 'CALENDAR_DAYS' | 'CERTAIN_DAYS';

type Month = 'JANUARY' | 'FEBRUARY' | 'MARCH' | 'APRIL' | 'MAY' | 'JUNE' | 
            'JULY' | 'AUGUST' | 'SEPTEMBER' | 'OCTOBER' | 'NOVEMBER' | 'DECEMBER';

type DayNumber = 'ONE' | 'TWO' | 'THREE' | 'FOUR' | 'FIVE' | 'SIX' | 'SEVEN' | 'EIGHT' | 'NINE' | 'TEN' |
                'ELEVEN' | 'TWELVE' | 'THIRTEEN' | 'FOURTEEN' | 'FIFTEEN' | 'SIXTEEN' | 'SEVENTEEN' | 'EIGHTEEN' | 'NINETEEN' | 'TWENTY' |
                'TWENTYONE' | 'TWENTYTWO' | 'TWENTYTHREE' | 'TWENTYFOUR' | 'TWENTYFIVE' | 'TWENTYSIX' | 'TWENTYSEVEN' | 'TWENTYEIGHT' | 'TWENTYNINE' | 'THIRTY' | 'THIRTYONE' | 'ALL_DAYS';

type DateOfMonth = {
    month: Month;
    monthNumbers: DayNumber[];
}

type DayOfWeekAndPeriodOfYearAndDateOfMonth = {
    periodType: PeriodType;
    dateOfMonths: DateOfMonth[];
}

type InterimAdvancePaymentTerm = {
    calendarType: CalendarType;
    name: string;
    value: number;
    valueFrom: number | null;
    valueTo: number | null;
    calendarId: number;
    excludeWeekends: boolean;
    excludeHolidays: boolean;
    dueDateChange: 'PREVIOUS_WORKING_DAY' | 'NEXT_WORKING_DAY' | null;
}

type InterimPayload = {
    name: string;
    valueType: ValueType;
    value: number | null;
    valueFrom: number | null;
    valueTo: number | null;
    priceComponentId: number | null;
    currencyId: number | null;
    paymentType: PaymentType;
    missingInvoice?: boolean;
    dateOfIssueType: DateOfIssueType;
    dateOfIssueValue: number | null;
    dateOfIssueValueFrom: number | null;
    dateOfIssueValueTo: number | null;
    issuingForTheMonthToCurrent: IssuingForTheMonthToCurrent;
    deductionFrom: DeductionFrom;
    matchesWithTermOfStandardInvoice: boolean;
    noInterestInOverdueDebt: boolean;
    dayOfWeekAndPeriodOfYearAndDateOfMonth: DayOfWeekAndPeriodOfYearAndDateOfMonth;
    interimAdvancePaymentTerm: InterimAdvancePaymentTerm;
}

export function interim(): InterimPayload { 
    return {
        "name": `INTERIM - ${randomGens.generateRandomString(true, false, 10)}`,
        "valueType": "EXACT_AMOUNT",
        "value": randomGens.getRandomEven(),
        "valueFrom": null,
        "valueTo": null,
        "priceComponentId": null,
        "currencyId": envVariables.currency,
        "paymentType": "OBLIGATORY",
        "dateOfIssueType": "PERIODICAL",
        "dateOfIssueValue": null,
        "dateOfIssueValueFrom": null,
        "dateOfIssueValueTo": null,
        "issuingForTheMonthToCurrent": "ZERO",
        "deductionFrom": "FIRST_INVOICE_FOR_SAME_PERIOD",
        "matchesWithTermOfStandardInvoice": false,
        "noInterestInOverdueDebt": false,
        "missingInvoice": false,
        "dayOfWeekAndPeriodOfYearAndDateOfMonth": {
            "periodType": "DAY_OF_MONTH",
            "dateOfMonths": [
                {
                    "month": "JANUARY",
                    "monthNumbers": [
                        "ONE",
                        "TWO",
                        "THREE",
                        "FOUR",
                        "FIVE",
                        "SIX",
                        "SEVEN",
                        "EIGHT",
                        "NINE",
                        "TEN",
                        "ELEVEN",
                        "TWELVE",
                        "THIRTEEN",
                        "FOURTEEN",
                        "FIFTEEN",
                        "SIXTEEN",
                        "SEVENTEEN",
                        "EIGHTEEN",
                        "NINETEEN",
                        "TWENTY",
                        "TWENTYONE",
                        "TWENTYTWO",
                        "TWENTYTHREE",
                        "TWENTYFOUR",
                        "TWENTYFIVE",
                        "TWENTYSIX",
                        "TWENTYSEVEN",
                        "TWENTYEIGHT",
                        "TWENTYNINE",
                        "THIRTY",
                        "THIRTYONE"
                    ]
                },
                {
                    "month": "FEBRUARY",
                    "monthNumbers": [
                        "ONE",
                        "TWO",
                        "THREE",
                        "FOUR",
                        "FIVE",
                        "SIX",
                        "SEVEN",
                        "EIGHT",
                        "NINE",
                        "TEN",
                        "ELEVEN",
                        "TWELVE",
                        "THIRTEEN",
                        "FOURTEEN",
                        "FIFTEEN",
                        "SIXTEEN",
                        "SEVENTEEN",
                        "EIGHTEEN",
                        "NINETEEN",
                        "TWENTY",
                        "TWENTYONE",
                        "TWENTYTWO",
                        "TWENTYTHREE",
                        "TWENTYFOUR",
                        "TWENTYFIVE",
                        "TWENTYSIX",
                        "TWENTYSEVEN",
                        "TWENTYEIGHT",
                        "TWENTYNINE"
                    ]
                },
                {
                    "month": "MARCH",
                    "monthNumbers": [
                        "ONE",
                        "TWO",
                        "THREE",
                        "FOUR",
                        "FIVE",
                        "SIX",
                        "SEVEN",
                        "EIGHT",
                        "NINE",
                        "TEN",
                        "ELEVEN",
                        "TWELVE",
                        "THIRTEEN",
                        "FOURTEEN",
                        "FIFTEEN",
                        "SIXTEEN",
                        "SEVENTEEN",
                        "EIGHTEEN",
                        "NINETEEN",
                        "TWENTY",
                        "TWENTYONE",
                        "TWENTYTWO",
                        "TWENTYTHREE",
                        "TWENTYFOUR",
                        "TWENTYFIVE",
                        "TWENTYSIX",
                        "TWENTYSEVEN",
                        "TWENTYEIGHT",
                        "TWENTYNINE",
                        "THIRTY",
                        "THIRTYONE"
                    ]
                },
                {
                    "month": "APRIL",
                    "monthNumbers": [
                        "ONE",
                        "TWO",
                        "THREE",
                        "FOUR",
                        "FIVE",
                        "SIX",
                        "SEVEN",
                        "EIGHT",
                        "NINE",
                        "TEN",
                        "ELEVEN",
                        "TWELVE",
                        "THIRTEEN",
                        "FOURTEEN",
                        "FIFTEEN",
                        "SIXTEEN",
                        "SEVENTEEN",
                        "EIGHTEEN",
                        "NINETEEN",
                        "TWENTY",
                        "TWENTYONE",
                        "TWENTYTWO",
                        "TWENTYTHREE",
                        "TWENTYFOUR",
                        "TWENTYFIVE",
                        "TWENTYSIX",
                        "TWENTYSEVEN",
                        "TWENTYEIGHT",
                        "TWENTYNINE",
                        "THIRTY"
                    ]
                },
                {
                    "month": "MAY",
                    "monthNumbers": [
                        "ONE",
                        "TWO",
                        "THREE",
                        "FOUR",
                        "FIVE",
                        "SIX",
                        "SEVEN",
                        "EIGHT",
                        "NINE",
                        "TEN",
                        "ELEVEN",
                        "TWELVE",
                        "THIRTEEN",
                        "FOURTEEN",
                        "FIFTEEN",
                        "SIXTEEN",
                        "SEVENTEEN",
                        "EIGHTEEN",
                        "NINETEEN",
                        "TWENTY",
                        "TWENTYONE",
                        "TWENTYTWO",
                        "TWENTYTHREE",
                        "TWENTYFOUR",
                        "TWENTYFIVE",
                        "TWENTYSIX",
                        "TWENTYSEVEN",
                        "TWENTYEIGHT",
                        "TWENTYNINE",
                        "THIRTY",
                        "THIRTYONE"
                    ]
                },
                {
                    "month": "JUNE",
                    "monthNumbers": [
                        "ONE",
                        "TWO",
                        "THREE",
                        "FOUR",
                        "FIVE",
                        "SIX",
                        "SEVEN",
                        "EIGHT",
                        "NINE",
                        "TEN",
                        "ELEVEN",
                        "TWELVE",
                        "THIRTEEN",
                        "FOURTEEN",
                        "FIFTEEN",
                        "SIXTEEN",
                        "SEVENTEEN",
                        "EIGHTEEN",
                        "NINETEEN",
                        "TWENTY",
                        "TWENTYONE",
                        "TWENTYTWO",
                        "TWENTYTHREE",
                        "TWENTYFOUR",
                        "TWENTYFIVE",
                        "TWENTYSIX",
                        "TWENTYSEVEN",
                        "TWENTYEIGHT",
                        "TWENTYNINE",
                        "THIRTY"
                    ]
                },
                {
                    "month": "JULY",
                    "monthNumbers": [
                        "ONE",
                        "TWO",
                        "THREE",
                        "FOUR",
                        "FIVE",
                        "SIX",
                        "SEVEN",
                        "EIGHT",
                        "NINE",
                        "TEN",
                        "ELEVEN",
                        "TWELVE",
                        "THIRTEEN",
                        "FOURTEEN",
                        "FIFTEEN",
                        "SIXTEEN",
                        "SEVENTEEN",
                        "EIGHTEEN",
                        "NINETEEN",
                        "TWENTY",
                        "TWENTYONE",
                        "TWENTYTWO",
                        "TWENTYTHREE",
                        "TWENTYFOUR",
                        "TWENTYFIVE",
                        "TWENTYSIX",
                        "TWENTYSEVEN",
                        "TWENTYEIGHT",
                        "TWENTYNINE",
                        "THIRTY",
                        "THIRTYONE"
                    ]
                },
                {
                    "month": "AUGUST",
                    "monthNumbers": [
                        "ONE",
                        "TWO",
                        "THREE",
                        "FOUR",
                        "FIVE",
                        "SIX",
                        "SEVEN",
                        "EIGHT",
                        "NINE",
                        "TEN",
                        "ELEVEN",
                        "TWELVE",
                        "THIRTEEN",
                        "FOURTEEN",
                        "FIFTEEN",
                        "SIXTEEN",
                        "SEVENTEEN",
                        "EIGHTEEN",
                        "NINETEEN",
                        "TWENTY",
                        "TWENTYONE",
                        "TWENTYTWO",
                        "TWENTYTHREE",
                        "TWENTYFOUR",
                        "TWENTYFIVE",
                        "TWENTYSIX",
                        "TWENTYSEVEN",
                        "TWENTYEIGHT",
                        "TWENTYNINE",
                        "THIRTY",
                        "THIRTYONE"
                    ]
                },
                {
                    "month": "SEPTEMBER",
                    "monthNumbers": [
                        "ONE",
                        "TWO",
                        "THREE",
                        "FOUR",
                        "FIVE",
                        "SIX",
                        "SEVEN",
                        "EIGHT",
                        "NINE",
                        "TEN",
                        "ELEVEN",
                        "TWELVE",
                        "THIRTEEN",
                        "FOURTEEN",
                        "FIFTEEN",
                        "SIXTEEN",
                        "SEVENTEEN",
                        "EIGHTEEN",
                        "NINETEEN",
                        "TWENTY",
                        "TWENTYONE",
                        "TWENTYTWO",
                        "TWENTYTHREE",
                        "TWENTYFOUR",
                        "TWENTYFIVE",
                        "TWENTYSIX",
                        "TWENTYSEVEN",
                        "TWENTYEIGHT",
                        "TWENTYNINE",
                        "THIRTY"
                    ]
                },
                {
                    "month": "OCTOBER",
                    "monthNumbers": [
                        "ONE",
                        "TWO",
                        "THREE",
                        "FOUR",
                        "FIVE",
                        "SIX",
                        "SEVEN",
                        "EIGHT",
                        "NINE",
                        "TEN",
                        "ELEVEN",
                        "TWELVE",
                        "THIRTEEN",
                        "FOURTEEN",
                        "FIFTEEN",
                        "SIXTEEN",
                        "SEVENTEEN",
                        "EIGHTEEN",
                        "NINETEEN",
                        "TWENTY",
                        "TWENTYONE",
                        "TWENTYTWO",
                        "TWENTYTHREE",
                        "TWENTYFOUR",
                        "TWENTYFIVE",
                        "TWENTYSIX",
                        "TWENTYSEVEN",
                        "TWENTYEIGHT",
                        "TWENTYNINE",
                        "THIRTY",
                        "THIRTYONE"
                    ]
                },
                {
                    "month": "NOVEMBER",
                    "monthNumbers": [
                        "ONE",
                        "TWO",
                        "THREE",
                        "FOUR",
                        "FIVE",
                        "SIX",
                        "SEVEN",
                        "EIGHT",
                        "NINE",
                        "TEN",
                        "ELEVEN",
                        "TWELVE",
                        "THIRTEEN",
                        "FOURTEEN",
                        "FIFTEEN",
                        "SIXTEEN",
                        "SEVENTEEN",
                        "EIGHTEEN",
                        "NINETEEN",
                        "TWENTY",
                        "TWENTYONE",
                        "TWENTYTWO",
                        "TWENTYTHREE",
                        "TWENTYFOUR",
                        "TWENTYFIVE",
                        "TWENTYSIX",
                        "TWENTYSEVEN",
                        "TWENTYEIGHT",
                        "TWENTYNINE",
                        "THIRTY"
                    ]
                },
                {
                    "month": "DECEMBER",
                    "monthNumbers": [
                        "ONE",
                        "TWO",
                        "THREE",
                        "FOUR",
                        "FIVE",
                        "SIX",
                        "SEVEN",
                        "EIGHT",
                        "NINE",
                        "TEN",
                        "ELEVEN",
                        "TWELVE",
                        "THIRTEEN",
                        "FOURTEEN",
                        "FIFTEEN",
                        "SIXTEEN",
                        "SEVENTEEN",
                        "EIGHTEEN",
                        "NINETEEN",
                        "TWENTY",
                        "TWENTYONE",
                        "TWENTYTWO",
                        "TWENTYTHREE",
                        "TWENTYFOUR",
                        "TWENTYFIVE",
                        "TWENTYSIX",
                        "TWENTYSEVEN",
                        "TWENTYEIGHT",
                        "TWENTYNINE",
                        "THIRTY",
                        "THIRTYONE"
                    ]
                }
            ]
        },
        "interimAdvancePaymentTerm": {
            "calendarType": "WORKING_DAYS",
            "name": "5 WORKING DAYS",
            "value": 1,
            "valueFrom": null,
            "valueTo": null,
            "calendarId": envVariables.calendar.id,
            "excludeWeekends": false,
            "excludeHolidays": false,
            "dueDateChange": null
        }
    }
}