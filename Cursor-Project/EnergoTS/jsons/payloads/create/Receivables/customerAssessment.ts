import { envVariables } from "../../../../fixtures/envCashed";
import { randomGens } from "../../../../utils/randomGens";

type AssessmentStatus = "FINAL" | "DRAFT";
type AssessmentValue = "YES" | "NO";

type Parameter = {
    parameterCriteriaId: number;
    parameterValue: string;
    parameterFinalAssessment: AssessmentValue;
    parameterAssessment: AssessmentValue;
};

type customerAssessment = {
    finalAssessment: AssessmentValue;
    typeId: string | number;
    additionalConditions: any[];
    newComment: string | null | number;
    customerId: string | number | null;
    files: any[];
    parameters: Parameter[];
    status: AssessmentStatus;
    taskIds: any[];
};

export function customer_assessment(): customerAssessment {
    return {
        "finalAssessment": "NO",
        "typeId": 1,
        "additionalConditions": [],
        "newComment": null,
        "customerId": null,
        "files": [],
        "parameters": [
            {
                "parameterCriteriaId": 1,
                "parameterValue": "16",
                "parameterFinalAssessment": "NO",
                "parameterAssessment": "NO"
            },
            {
                "parameterCriteriaId": 2,
                "parameterValue": "0",
                "parameterFinalAssessment": "YES",
                "parameterAssessment": "YES"
            },
            {
                "parameterCriteriaId": 6,
                "parameterValue": "0",
                "parameterFinalAssessment": "YES",
                "parameterAssessment": "YES"
            },
            {
                "parameterCriteriaId": 8,
                "parameterValue": "YES",
                "parameterFinalAssessment": "NO",
                "parameterAssessment": "NO"
            },
            {
                "parameterCriteriaId": 9,
                "parameterValue": "YES",
                "parameterFinalAssessment": "NO",
                "parameterAssessment": "NO"
            },
            {
                "parameterCriteriaId": 10,
                "parameterValue": "YES",
                "parameterFinalAssessment": "NO",
                "parameterAssessment": "NO"
            },
            {
                "parameterCriteriaId": 12,
                "parameterValue": "YES",
                "parameterFinalAssessment": "NO",
                "parameterAssessment": "NO"
            },
            {
                "parameterCriteriaId": 13,
                "parameterValue": "YES",
                "parameterFinalAssessment": "NO",
                "parameterAssessment": "NO"
            },
            {
                "parameterCriteriaId": 7,
                "parameterValue": "0",
                "parameterFinalAssessment": "YES",
                "parameterAssessment": "YES"
            }
        ],
        "status": "FINAL",
        "taskIds": []
    }
}