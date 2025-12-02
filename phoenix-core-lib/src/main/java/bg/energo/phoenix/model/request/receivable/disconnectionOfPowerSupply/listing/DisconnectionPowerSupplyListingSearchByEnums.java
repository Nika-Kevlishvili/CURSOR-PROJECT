package bg.energo.phoenix.model.request.receivable.disconnectionOfPowerSupply.listing;

import lombok.Getter;

public enum DisconnectionPowerSupplyListingSearchByEnums {
    ALL("ALL"),
    NUMBER("NUMBER"),
    DISCONNECTION_REQUEST_NUMBER("DISCONNECTION_REQUEST_NUMBER"),
    CUSTOMER_IDENTIFIER("CUSTOMER_IDENTIFIER"),
    CUSTOMER_NAME("CUSTOMER_NAME"),
    POD_IDENTIFIER("POD_IDENTIFIER");

    @Getter
    private final String value;

    DisconnectionPowerSupplyListingSearchByEnums(String value) {
        this.value = value;
    }
}
