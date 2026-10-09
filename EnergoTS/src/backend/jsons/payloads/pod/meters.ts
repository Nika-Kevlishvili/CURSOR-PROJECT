import { envVariables } from "../../../fixtures/envCashed";
import { randomGens } from "../../../utils/randomGens";

export function meters() {
  return {
    "number": `meters${randomGens.generateCurrentTimeStamp()}`,
    "gridOperatorId": null,
    "installmentDate": '2000-12-01',
    "removeDate": "2040-04-01",
    "podId": null,
    "meterScales": [
      null
    ],
    "warningAcceptedByUser": false
  }
}