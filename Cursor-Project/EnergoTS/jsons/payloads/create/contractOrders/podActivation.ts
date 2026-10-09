import { envVariables } from "../../../../fixtures/envCashed";
import { randomGens } from "../../../../utils/randomGens";

type PodActivation = {
    identifier: string | number | null;
    podDetailId: string | number | null;
    contractDetailId: string | number | null;
    activationDate: string;
    deactivationDate: string | null;
    deactivationPurposeId: string | number;
};

export function pod_activation(): PodActivation {
    return{
            "identifier": null,
            "podDetailId": null,
            "contractDetailId": null,
            "activationDate": randomGens.generateMonthStartDate('yyyy-mm-dd'),
            "deactivationDate": null,
            "deactivationPurposeId": envVariables.deactivation_reason,
    }
}