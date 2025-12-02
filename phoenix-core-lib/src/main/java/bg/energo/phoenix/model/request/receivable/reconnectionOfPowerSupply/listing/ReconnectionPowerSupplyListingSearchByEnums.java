package bg.energo.phoenix.model.request.receivable.reconnectionOfPowerSupply.listing;

import lombok.Getter;

public enum ReconnectionPowerSupplyListingSearchByEnums {
    ALL("ALL"),
    NUMBER("NUMBER"),
    DISCONNECTION_REQUEST_NUMBER("DISCONNECTION_REQUEST_NUMBER"),
    CUSTOMER_IDENTIFIER("CUSTOMER_IDENTIFIER"),
    CUSTOMER_NAME("CUSTOMER_NAME"),
    POD_IDENTIFIER("POD_IDENTIFIER");

    @Getter
    private final String value;

    ReconnectionPowerSupplyListingSearchByEnums(String value) {
        this.value = value;
    }
}
