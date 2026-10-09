import { envVariables } from "../../../../fixtures/envCashed";
import { randomGens } from "../../../../utils/randomGens";

type PodSettlementAddressLocalData = {
    countryId: number;
    regionId: number;
    municipalityId: number;
    residentialAreaType: string | null;
    streetType: string | null;
    populatedPlaceId: number;
    zipCodeId: number;
    residentialAreaTypeTrsl: string | null;
    streetTypeTrsl: string | null;
    countryTrsl: string;
    regionTrsl: string;
    municipalityTrsl: string;
    populatedPlaceTrsl: string;
    zipCodeTrsl: string;
    districtTrsl: string | null;
    residentialAreaTrsl: string | null;
    streetTrsl: string | null;
    districtId: number | null;
    residentialAreaId: number | null;
    streetId: number | null;
    measurementTypeId: number | null;
}

type PodSettlementAddressRequest = {
    foreign: boolean;
    foreignAddressData: unknown;
    localAddressData: PodSettlementAddressLocalData;
    number: string | null;
    additionalInformation: string | null;
    block: string | null;
    entrance: string | null;
    floor: string | null;
    apartment: string | null;
    mailbox: string | null;
}

type PodSettlementPayload = {
    identifier: string;
    name: string;
    additionalIdentifier: string | null;
    gridOperatorId: number;
    type: string;
    estimatedMonthlyAvgConsumption: string;
    consumptionPurpose: string | null;
    voltageLevel: string;
    customerIdentifierByGridOperator: string | null;
    customerNumberByGridOperator: string | null;
    settlementPeriod: boolean;
    providedPower: number | null;
    multiplier: number | null;
    addressRequest: PodSettlementAddressRequest;
    impossibleToDisconnect: boolean;
    blockedDisconnection: boolean;
    blockedBilling: boolean;
    blockedBillingRequest: unknown;
    blockedDisconnectionRequest: unknown;
    customerIdentifier: string | null;
    balancingGroupCoordinatorId: number;
    userTypeId: number;
    measurementTypeId: number | null;
    podAdditionalParameters: unknown;
    slp: boolean;
}

export function pod_settlement(): PodSettlementPayload {
    return {
        "identifier": `32X${randomGens.generateRandomString(true,false,10)}${randomGens.generateRandomString(false,true,10)}`,
        "name": "SETTLEMENT POD",
        "additionalIdentifier": null,
        "gridOperatorId": envVariables.grid_operator,
        "type": "CONSUMER",
        "estimatedMonthlyAvgConsumption": "1",
        "consumptionPurpose": "NON_HOUSEHOLD",
        "voltageLevel": "LOW",
        "customerIdentifierByGridOperator": null,
        "customerNumberByGridOperator": null,
        "settlementPeriod": true,
        "providedPower": null,
        "multiplier": null,
        "addressRequest": {
            "foreign": false,
            "foreignAddressData": null,
            "localAddressData": {
                "countryId": envVariables.countries,
                "regionId": envVariables.regions,
                "municipalityId": envVariables.municipalities,
                "residentialAreaType": null,
                "streetType": null,
                "populatedPlaceId": envVariables.population_places,
                "zipCodeId": envVariables.zip_codes,
                "residentialAreaTypeTrsl": null,
                "streetTypeTrsl": null,
                "countryTrsl": "BALGARIYA",
                "regionTrsl": "TARGOVISHTE",
                "municipalityTrsl": "ANTONOVO",
                "populatedPlaceTrsl": "DABRAVITSA",
                "zipCodeTrsl": "7997",
                "districtTrsl": null,
                "residentialAreaTrsl": null,
                "streetTrsl": null,
                "districtId": null,
                "residentialAreaId": null,
                "streetId": null,
                "measurementTypeId": null
            },
            "number": null,
            "additionalInformation": null,
            "block": null,
            "entrance": null,
            "floor": null,
            "apartment": null,
            "mailbox": null
        },
        "impossibleToDisconnect": false,
        "blockedDisconnection": false,
        "blockedBilling": false,
        "blockedBillingRequest": null,
        "blockedDisconnectionRequest": null,
        "customerIdentifier": null,
        "balancingGroupCoordinatorId": envVariables.balancing_group_coordinator,
        "userTypeId": envVariables.type_of_user,
        "measurementTypeId": null,
        "podAdditionalParameters": null,
        "slp": false
    }
}