import { envVariables } from "../../../fixtures/envCashed";
import { randomGens } from "../../../utils/randomGens";

type ReschedulingLpf = {
  id: number;
  lpf_data: {
    results: LpfResult[];
  };
  interest_default_currency: number;
};

type LpfResult = {
  fee: number | null;
  period_from: string;
  period_to: string;
  currency_id: number;
  applicable_interest_rate: number;
  interval_amount: number;
  calculated_interest: number;
};

type Installment = {
  fee: number;
  due_date: string;
  inst_num: number;
  inst_name: string;
  inst_amount: number;
  interest_amount: number;
  principal_amount: number;
};

type TemplateRequest = {
  templateId: number;
  signings: string[];
  outputFileFormat: string[];
};

type ReschedulingType = {
  numberOfInstallment: string;
  amountOfTheInstallment: string | null;
  currencyId: number;
  interestRateForInstalmentsId: number;
  installmentDueDayOfTheMonth: string;
  reschedulingInterestType: string;

  customerId: number;
  customerDetailId: number;
  customerCommunicationDataId: number;
  customerCommunicationDataIdForContract: number;
  customerAssessmentId: number;

  taskIds: number[];
  saveAndExecute: boolean;

  liabilityIdsForRescheduling: number[];

  files: any[];

  templateIds: number[];

  templateRequests: TemplateRequest[];

  reschedulingLpfs: ReschedulingLpf[];

  installments: Installment[];

  reschedulingStatus: string;
};

export function rescheduling(): ReschedulingType {
    return {
        "numberOfInstallment": "3",
        "amountOfTheInstallment": null,
        "currencyId": envVariables.currency,
        "interestRateForInstalmentsId": envVariables.interest_rate,
        "installmentDueDayOfTheMonth": "3",
        "reschedulingInterestType": "INTEREST_WITH_THE_FIRST_INSTALLMENT",
        "customerId": 6033167,
        "customerDetailId": 35260,
        "customerCommunicationDataId": 35226,
        "customerCommunicationDataIdForContract": 35226,
        "customerAssessmentId": 1092,
        "taskIds": [],
        "saveAndExecute": false,
        "liabilityIdsForRescheduling": [
            21406
        ],
        "files": [],
        "templateIds": [
            envVariables.rescheduling_document_template
        ],
        "templateRequests": [
            {
                "templateId": envVariables.rescheduling_document_template,
                "signings": [
                    "NO"
                ],
                "outputFileFormat": [
                    "DOCX",
                    "PDF"
                ]
            }
        ],
        "reschedulingLpfs": [
            {
                "id": 21406,
                "lpf_data": {
                    "results": [
                        {
                            "fee": null,
                            "period_from": "2026-01-22",
                            "period_to": "2026-03-02",
                            "currency_id": 1001,
                            "applicable_interest_rate": 11.91,
                            "interval_amount": 133.2,
                            "calculated_interest": 1.76268
                        },
                        {
                            "fee": null,
                            "period_from": "2026-03-03",
                            "period_to": "2026-03-11",
                            "currency_id": 1001,
                            "applicable_interest_rate": 11.91,
                            "interval_amount": 44.8,
                            "calculated_interest": 0.13339
                        },
                        {
                            "fee": null,
                            "period_from": "2026-03-12",
                            "period_to": "2026-04-03",
                            "currency_id": 1001,
                            "applicable_interest_rate": 11.91,
                            "interval_amount": 13.12,
                            "calculated_interest": 0.09983
                        },
                        {
                            "fee": null,
                            "period_from": "2026-03-12",
                            "period_to": "2026-05-03",
                            "currency_id": 1001,
                            "applicable_interest_rate": 11.91,
                            "interval_amount": 15.84,
                            "calculated_interest": 0.27774
                        },
                        {
                            "fee": null,
                            "period_from": "2026-03-12",
                            "period_to": "2026-06-03",
                            "currency_id": 1001,
                            "applicable_interest_rate": 11.91,
                            "interval_amount": 15.84,
                            "calculated_interest": 0.44019
                        }
                    ]
                },
                "interest_default_currency": 2.72
            }
        ],
        "installments": [
            {
                "fee": 0,
                "due_date": "2026-04-03",
                "inst_num": 1,
                "inst_name": "inst-1",
                "inst_amount": 15.84,
                "interest_amount": 2.72,
                "principal_amount": 13.12
            },
            {
                "fee": 0,
                "due_date": "2026-05-03",
                "inst_num": 2,
                "inst_name": "inst-2",
                "inst_amount": 15.84,
                "interest_amount": 0,
                "principal_amount": 15.84
            },
            {
                "fee": 0,
                "due_date": "2026-06-03",
                "inst_num": 3,
                "inst_name": "inst-3",
                "inst_amount": 15.84,
                "interest_amount": 0,
                "principal_amount": 15.84
            }
        ],
        "reschedulingStatus": "EXECUTED"
    }
}