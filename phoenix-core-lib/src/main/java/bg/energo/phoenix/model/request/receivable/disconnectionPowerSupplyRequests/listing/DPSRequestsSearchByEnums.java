package bg.energo.phoenix.model.request.receivable.disconnectionPowerSupplyRequests.listing;

import lombok.Getter;

public enum DPSRequestsSearchByEnums {
    ALL("ALL"),
    NUMBER("DISCONNECTION_REQUEST_NUMBER"),
    NUMBER_OF_PODS("NUMBER_OF_PODS");

    @Getter
    private final String value;

    DPSRequestsSearchByEnums(String value) {
        this.value = value;
    }
}
