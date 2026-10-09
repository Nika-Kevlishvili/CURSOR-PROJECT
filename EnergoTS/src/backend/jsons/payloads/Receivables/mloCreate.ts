import { envVariables } from "../../../fixtures/envCashed";
import { randomGens } from "../../../utils/randomGens";

export function MLO_L_R()
{
  return {
    "date": randomGens.generateUtcDate('yyyy-mm-dd'),
    "customerId": null,
    "customerDetailId": null,
    "customerCommunicationDataId": null,
    "liabilities": [
      {
        "id": null,
        "currency_id": envVariables.currency,
        "current_amount": 100
      }
    ],
    "receivables": [
      {
        "id": null,
        "currency_id": envVariables.currency,
        "current_amount": 100
      }
    ],
    "deposits": [],
    "payments": [],
    "receivedLiabilities": [
      {
        "id": null,
        "currency_id": envVariables.currency,
        "current_amount": 0
      }
    ],
    "receivedReceivables": [
      {
        "id": null,
        "currency_id": envVariables.currency,
        "current_amount": 0
      }
    ],
    "receivedDeposits": null,
    "receivedOffsets": [
      {
        "d_id": null,
        "l_id": null,
        "r_id": null,
        "currency_id": envVariables.currency,
        "offset_amount": 100
      }
    ],
    "templateIds": []
  }
};

export function MLO_D_L() 
{
  return {
    "date": randomGens.generateYesterdaysDate('yyyy-mm-dd'),
    "customerId": null,
    "customerDetailId": null,
    "customerCommunicationDataId": null,
    "liabilities": [
      {
        "id": null,
        "currency_id": envVariables.currency,
        "current_amount": 100
      }
    ],
    "receivables": [],
    "deposits": [
      {
        "id": null,
        "currency_id": envVariables.currency,
        "current_amount": 100
      }
    ],
    "payments": [],
    "receivedLiabilities": [
      {
        "id": null,
        "currency_id": envVariables.currency,
        "current_amount": 0
      }
    ],
    "receivedReceivables": null,
    "receivedDeposits": [
      {
        "id": null,
        "currency_id": envVariables.currency,
        "current_amount": 0
      }
    ],
    "receivedOffsets": [
      {
        "d_id": null,
        "l_id": null,
        "r_id": null,
        "currency_id": envVariables.currency,
        "offset_amount": 100
      }
    ],
    "templateIds": []
  };
}
export function MLO_NP_R()
{
  return {
    "date": randomGens.generateYesterdaysDate('yyyy-mm-dd'),
    "customerId": null,
    "customerDetailId": null,
    "customerCommunicationDataId": null,
    "liabilities": [],
    "receivables": [
      {
        "id": null,
        "currency_id": envVariables.currency,
        "current_amount": 100
      }
    ],
    "deposits": [],
    "payments": [
      {
        "id": null,
        "currency_id": envVariables.currency,
        "current_amount": -100
      }
    ],
    "receivedLiabilities": null,
    "receivedReceivables": [
      {
        "id": null,
        "currency_id": envVariables.currency,
        "current_amount": 0
      }
    ],
    "receivedDeposits": null,
    "receivedOffsets": [
      {
        "d_id": null,
        "l_id": null,
        "r_id": null,
        "currency_id": envVariables.currency,
        "offset_amount": 100
      }
    ],
    "receivedPayments": [
      {
        "id": null,
        "currency_id": envVariables.currency,
        "current_amount": 0
      }
    ],
    "templateIds": []
  }
}